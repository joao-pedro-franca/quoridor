package br.edu.quoridor.rmi;

import br.edu.quoridor.jogo.Partida;
import br.edu.quoridor.modelo.*;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;

/** Uma partida privada; chamadas RMI e monitor usam o mesmo monitor de exclusão. */
public final class ServicoJogo implements JogoRemoto, AutoCloseable {
    private static final long SILENCIO = TimeUnit.SECONDS.toNanos(10);
    private static final long RECONEXAO = TimeUnit.SECONDS.toNanos(180);
    private final Partida partida;
    private final Map<String, Sessao> sessoes = new HashMap<>();
    private final LongSupplier relogio;
    private final ScheduledExecutorService monitor;
    private boolean encerrado;

    private static final class Sessao {
        final int id;
        long ultimoHeartbeat;
        long desconectadoEm;
        boolean conectado = true;
        Sessao(int id, long agora) { this.id = id; ultimoHeartbeat = agora; }
    }

    public ServicoJogo() throws RemoteException {
        this(RandomGenerator.getDefault());
    }

    public ServicoJogo(RandomGenerator aleatorio) throws RemoteException {
        this(aleatorio, System::nanoTime);
    }

    /** Relógio monotônico em nanos, controlável nos testes sem aguardar três minutos. */
    public ServicoJogo(RandomGenerator aleatorio, LongSupplier relogio) throws RemoteException {
        this(aleatorio, relogio, Executors.newSingleThreadScheduledExecutor(tarefa -> {
            Thread thread = new Thread(tarefa, "monitor-presenca-quoridor");
            thread.setDaemon(true);
            return thread;
        }));
    }

    ServicoJogo(RandomGenerator aleatorio, LongSupplier relogio,
                ScheduledExecutorService monitor) throws RemoteException {
        this.monitor = Objects.requireNonNull(monitor);
        try {
            partida = new Partida(aleatorio);
            this.relogio = Objects.requireNonNull(relogio);
            UnicastRemoteObject.exportObject(this, 0);
            monitor.scheduleWithFixedDelay(this::verificarConexoes, 250, 250, TimeUnit.MILLISECONDS);
        } catch (RemoteException | RuntimeException falha) {
            monitor.shutdownNow();
            try { UnicastRemoteObject.unexportObject(this, true); }
            catch (NoSuchObjectException naoExportado) { /* Nada a liberar. */ }
            throw falha;
        }
    }

    @Override
    public synchronized Registro registrar() throws RemoteException {
        long agora = relogio.getAsLong();
        verificarConexoes(agora);
        if (partida.getEstado().isFinalizada()) {
            return recusarRegistro("A partida já terminou.");
        }
        if (sessoes.size() == 4) {
            return recusarRegistro("A partida já possui quatro jogadores. Use seu token para reconectar.");
        }
        String token;
        do { token = UUID.randomUUID().toString(); } while (sessoes.containsKey(token));
        Sessao sessao = new Sessao(sessoes.size() + 1, agora);
        sessoes.put(token, sessao);
        return new Registro(true, sessao.id, token, "Jogador " + sessao.id + " registrado.");
    }

    @Override
    public synchronized Registro reconectar(String token) throws RemoteException {
        long agora = relogio.getAsLong();
        verificarConexoes(agora);
        Sessao sessao = sessoes.get(token);
        if (sessao == null) { return recusarRegistro("Token de sessão inválido."); }
        if (partida.getEstado().getSituacao() == SituacaoPartida.ENCERRADA_POR_ABANDONO) {
            return recusarRegistro("A partida terminou por abandono e não pode ser retomada.");
        }
        if (!partida.getEstado().isFinalizada()) {
            sessao.conectado = true;
            sessao.ultimoHeartbeat = agora;
            sessao.desconectadoEm = 0;
            if (sessoes.values().stream().allMatch(s -> s.conectado)) { partida.retomar(); }
        }
        return new Registro(true, sessao.id, token, "Jogador " + sessao.id + " reconectou. Sessão recuperada.");
    }

    @Override
    public synchronized ResultadoAcao heartbeat(String token) throws RemoteException {
        long agora = relogio.getAsLong();
        verificarConexoes(agora);
        Sessao sessao = sessoes.get(token);
        if (sessao == null) { return erro("Token de sessão inválido."); }
        if (partida.getEstado().isFinalizada()) { return erro("A partida já terminou."); }
        if (!sessao.conectado) { return erro("Sessão desconectada. Reinicie e use reconectar com seu token."); }
        sessao.ultimoHeartbeat = agora;
        return new ResultadoAcao(true, "Heartbeat recebido.");
    }

    @Override
    public synchronized ResultadoAcao desconectar(String token) throws RemoteException {
        long agora = relogio.getAsLong();
        verificarConexoes(agora);
        Sessao sessao = sessoes.get(token);
        if (sessao == null) { return erro("Token de sessão inválido."); }
        if (!partida.getEstado().isFinalizada() && sessao.conectado) {
            marcarDesconectado(sessao, agora);
        }
        return new ResultadoAcao(true, "Desconexão notificada.");
    }

    @Override
    public synchronized EstadoServico consultarEstado() throws RemoteException {
        long agora = relogio.getAsLong();
        verificarConexoes(agora);
        List<PresencaJogador> presencas = sessoes.values().stream()
                .sorted(Comparator.comparingInt(s -> s.id))
                .map(s -> new PresencaJogador(s.id, s.conectado, s.conectado ? 0 : restantes(s, agora)))
                .toList();
        return new EstadoServico(partida.getEstado(), sessoes.size(), presencas);
    }

    private long restantes(Sessao sessao, long agora) {
        long nanos = Math.max(0, RECONEXAO - (agora - sessao.desconectadoEm));
        return (nanos + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1);
    }

    /** Chamado pelo único agendador e antes de cada operação; nunca depende de consultas. */
    synchronized void verificarConexoes() {
        verificarConexoes(relogio.getAsLong());
    }

    private void verificarConexoes(long agora) {
        if (encerrado || partida.getEstado().isFinalizada()) { return; }
        for (Sessao sessao : sessoes.values()) {
            if (sessao.conectado && agora - sessao.ultimoHeartbeat >= SILENCIO) {
                // Usa o instante em que os dez segundos completaram, sem alongar o prazo
                // caso o agendador ou uma chamada RMI só consiga o monitor mais tarde.
                marcarDesconectado(sessao, sessao.ultimoHeartbeat + SILENCIO);
            }
        }
        Sessao expirou = sessoes.values().stream()
                .filter(s -> !s.conectado && agora - s.desconectadoEm >= RECONEXAO)
                .max(Comparator.<Sessao>comparingLong(s -> agora - s.desconectadoEm)
                        .thenComparingInt(s -> -s.id)).orElse(null);
        if (expirou != null) { partida.encerrarPorAbandono(expirou.id); }
    }

    private void marcarDesconectado(Sessao sessao, long instante) {
        sessao.conectado = false;
        sessao.desconectadoEm = instante;
        partida.pausar();
    }

    @Override
    public synchronized ResultadoAcao mover(String token, int linha, int coluna) throws RemoteException {
        ResultadoAcao impedimento = validarSessao(token);
        return impedimento != null ? impedimento : partida.mover(sessoes.get(token).id, linha, coluna);
    }

    @Override
    public synchronized ResultadoAcao colocarBarreira(String token, int linha, int coluna,
                                                     Orientacao orientacao) throws RemoteException {
        ResultadoAcao impedimento = validarSessao(token);
        return impedimento != null ? impedimento
                : partida.colocarBarreira(sessoes.get(token).id, linha, coluna, orientacao);
    }

    private ResultadoAcao validarSessao(String token) {
        verificarConexoes();
        Sessao sessao = sessoes.get(token);
        if (sessao == null) { return erro("Token de sessão inválido."); }
        if (partida.getEstado().isFinalizada()) { return erro("A partida já terminou."); }
        if (partida.getEstado().isPausada()) { return erro("Partida pausada. Aguardando reconexão."); }
        if (sessoes.size() < 4) { return erro("Aguardando o registro dos quatro jogadores."); }
        return null;
    }

    private ResultadoAcao erro(String mensagem) { return new ResultadoAcao(false, mensagem); }
    private Registro recusarRegistro(String mensagem) { return new Registro(false, 0, null, mensagem); }

    @Override
    public synchronized void close() {
        if (encerrado) { return; }
        encerrado = true;
        monitor.shutdownNow();
        try { UnicastRemoteObject.unexportObject(this, true); }
        catch (NoSuchObjectException jaEncerrado) { /* Limpeza idempotente. */ }
    }
}
