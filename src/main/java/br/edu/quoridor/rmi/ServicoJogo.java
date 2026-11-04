package br.edu.quoridor.rmi;

import br.edu.quoridor.jogo.Partida;
import br.edu.quoridor.modelo.EstadoServico;
import br.edu.quoridor.modelo.Orientacao;
import br.edu.quoridor.modelo.Registro;
import br.edu.quoridor.modelo.ResultadoAcao;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;

/** Uma partida privada; o monitor do serviço protege toda consulta e ação. */
public final class ServicoJogo implements JogoRemoto, AutoCloseable {
    private final Partida partida;
    private final Map<String, Integer> sessoes = new HashMap<>();

    public ServicoJogo() throws RemoteException {
        this(RandomGenerator.getDefault());
    }

    /** A origem controlada afeta apenas o sorteio do turno, nunca os tokens. */
    public ServicoJogo(RandomGenerator aleatorio) throws RemoteException {
        partida = new Partida(aleatorio);
        UnicastRemoteObject.exportObject(this, 0);
    }

    @Override
    public synchronized Registro registrar() throws RemoteException {
        if (sessoes.size() == 4) {
            return new Registro(false, 0, null, "A partida já possui quatro jogadores. Use seu token para reconectar.");
        }
        String token;
        do {
            token = UUID.randomUUID().toString();
        } while (sessoes.containsKey(token));
        int jogadorId = sessoes.size() + 1;
        sessoes.put(token, jogadorId);
        return new Registro(true, jogadorId, token, "Jogador " + jogadorId + " registrado.");
    }

    @Override
    public synchronized Registro reconectar(String token) throws RemoteException {
        Integer jogadorId = sessoes.get(token);
        if (jogadorId == null) {
            return new Registro(false, 0, null, "Token de sessão inválido.");
        }
        return new Registro(true, jogadorId, token, "Sessão recuperada. A vaga foi preservada.");
    }

    @Override
    public synchronized EstadoServico consultarEstado() throws RemoteException {
        return new EstadoServico(partida.getEstado(), sessoes.size());
    }

    @Override
    public synchronized ResultadoAcao mover(String token, int linha, int coluna) throws RemoteException {
        ResultadoAcao impedimento = validarSessao(token);
        if (impedimento != null) {
            return impedimento;
        }
        return partida.mover(sessoes.get(token), linha, coluna);
    }

    @Override
    public synchronized ResultadoAcao colocarBarreira(String token, int linha, int coluna,
                                                       Orientacao orientacao) throws RemoteException {
        ResultadoAcao impedimento = validarSessao(token);
        if (impedimento != null) {
            return impedimento;
        }
        return partida.colocarBarreira(sessoes.get(token), linha, coluna, orientacao);
    }

    private ResultadoAcao validarSessao(String token) {
        if (!sessoes.containsKey(token)) {
            return new ResultadoAcao(false, "Token de sessão inválido.");
        }
        if (sessoes.size() < 4) {
            return new ResultadoAcao(false, "Aguardando o registro dos quatro jogadores.");
        }
        return null;
    }

    /** Libera os recursos RMI. Não pertence à interface remota. */
    @Override
    public synchronized void close() {
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException jaEncerrado) {
            // Permite limpeza tanto pelo fluxo normal quanto pelo gancho de encerramento.
        }
    }
}
