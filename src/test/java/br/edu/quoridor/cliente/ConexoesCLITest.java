package br.edu.quoridor.cliente;

import br.edu.quoridor.modelo.*;
import br.edu.quoridor.rmi.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class ConexoesCLITest {
    private final AtomicLong nanos = new AtomicLong();
    private ServicoJogo servico;
    private JogoRemoto remoto;
    private final List<Registro> registros = new ArrayList<>();
    private final StringWriter saida = new StringWriter();
    private Cliente cliente;

    @BeforeEach
    void iniciar() throws Exception {
        servico = new ServicoJogo(new Random() {
            @Override public int nextInt(int limite) { return 0; }
        }, nanos::get);
        remoto = (JogoRemoto) UnicastRemoteObject.toStub(servico);
        for (int i = 0; i < 4; i++) { registros.add(remoto.registrar()); }
        cliente = new Cliente(remoto, new BufferedReader(new StringReader("")), new PrintWriter(saida), false);
        assertTrue(cliente.iniciarSessao("Aluno", registros.get(0).token()));
    }

    @AfterEach
    void encerrar() { servico.close(); }

    @Test
    void sairNotificaImediatamenteSemAlterarPosicoesOuTurno() throws Exception {
        var antes = remoto.consultarEstado().jogo();
        assertFalse(cliente.executarComando("sair"));
        var estado = remoto.consultarEstado();
        assertTrue(estado.jogo().isPausada());
        assertFalse(estado.presencas().getFirst().conectado());
        assertEquals(180, estado.presencas().getFirst().segundosRestantes());
        assertEquals(antes.getJogadorDaVez(), estado.jogo().getJogadorDaVez());
        assertEquals(antes.getJogadores().getFirst().getPosicao(), estado.jogo().getJogadores().getFirst().getPosicao());
    }

    @Test
    void sairDurantePreviaDescartaBarreiraENotifica() throws Exception {
        cliente.executarComando("barreira h 3,4 4,5");
        assertFalse(cliente.executarComando("sair"));
        var estado = remoto.consultarEstado();
        assertTrue(estado.jogo().isPausada());
        assertTrue(estado.jogo().getBarreiras().isEmpty());
        assertEquals(5, estado.jogo().getJogadores().getFirst().getBarreirasDisponiveis());
    }

    @Test
    void cliMostraPausaContagemERetomadaSemRedesenharEstadoIdentico() throws Exception {
        remoto.desconectar(registros.get(1).token());
        cliente.atualizar(false);
        assertTrue(saida.toString().contains("PARTIDA PAUSADA"));
        assertTrue(saida.toString().contains("Jogador 2 (03:00)"));
        nanos.set(TimeUnit.SECONDS.toNanos(5));
        cliente.atualizar(false);
        assertTrue(saida.toString().contains("Jogador 2 (02:55)"));
        int tamanho = saida.toString().length();
        cliente.atualizar(false);
        assertEquals(tamanho, saida.toString().length());
        remoto.reconectar(registros.get(1).token());
        cliente.atualizar(false);
        assertTrue(saida.toString().contains("2 reconectou!"));
        assertTrue(saida.toString().contains("Partida retomada"));
    }

    @Test
    void cliMostraMultiplosAusentesESemVencedorNoAbandono() throws Exception {
        remoto.desconectar(registros.get(1).token());
        remoto.desconectar(registros.get(2).token());
        cliente.atualizar(false);
        assertTrue(saida.toString().contains("Jogador 2 (03:00)"));
        assertTrue(saida.toString().contains("Jogador 3 (03:00)"));
        nanos.set(TimeUnit.SECONDS.toNanos(180));
        saida.getBuffer().setLength(0);
        cliente.atualizar(false);
        assertTrue(saida.toString().contains("PARTIDA ENCERRADA"));
        assertTrue(saida.toString().contains("sem vencedor"));
        assertTrue(saida.toString().contains("jogador 2 não reconectou"));
        assertFalse(saida.toString().contains("Vencedor: jogador"));
    }

    @Test
    void eofTambemNotificaAusencia() throws Exception {
        Cliente outro = new Cliente(remoto, new BufferedReader(new StringReader(
                "Aluno\n" + registros.get(0).token() + "\n")), new PrintWriter(saida), false);
        outro.executar();
        assertFalse(remoto.consultarEstado().presencas().getFirst().conectado());
    }

    @Test
    void sairContinuaPossivelSeServidorEstiverDesligado() throws Exception {
        servico.close();
        assertFalse(cliente.executarComando("sair"));
    }

    @Test
    void enviaHeartbeatsMesmoAguardandoTurnoEDigitacaoSemEnter() throws Exception {
        CountDownLatch heartbeats = new CountDownLatch(2);
        JogoRemoto contando = new JogoRemoto() {
            public Registro registrar() throws RemoteException { return remoto.registrar(); }
            public Registro reconectar(String token) throws RemoteException { return remoto.reconectar(token); }
            public EstadoServico consultarEstado() throws RemoteException { return remoto.consultarEstado(); }
            public ResultadoAcao heartbeat(String token) throws RemoteException {
                var resposta = remoto.heartbeat(token);
                if (resposta.sucesso()) { heartbeats.countDown(); }
                return resposta;
            }
            public ResultadoAcao desconectar(String token) throws RemoteException { return remoto.desconectar(token); }
            public ResultadoAcao mover(String token, int linha, int coluna) throws RemoteException {
                return remoto.mover(token, linha, coluna);
            }
            public ResultadoAcao colocarBarreira(String token, int linha, int coluna, Orientacao orientacao)
                    throws RemoteException { return remoto.colocarBarreira(token, linha, coluna, orientacao); }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (PipedWriter escritor = new PipedWriter(); PipedReader leitor = new PipedReader(escritor)) {
            Cliente ativo = new Cliente(contando, new BufferedReader(leitor), new PrintWriter(saida), false);
            Future<?> tarefa = executor.submit(() -> {
                try { ativo.executar(); } catch (IOException erro) { throw new UncheckedIOException(erro); }
            });
            escritor.write("Aluno\n" + registros.get(1).token() + "\nmover ");
            escritor.flush();
            assertTrue(heartbeats.await(12, TimeUnit.SECONDS));
            assertFalse(remoto.consultarEstado().jogo().isPausada());
            assertEquals(1, remoto.consultarEstado().jogo().getJogadorDaVez());
            assertEquals(new Posicao(4, 8), remoto.consultarEstado().jogo().getJogadores().get(1).getPosicao());
            escritor.write("5 8\nsair\n");
            escritor.flush();
            tarefa.get(5, TimeUnit.SECONDS);
            assertFalse(remoto.consultarEstado().presencas().get(1).conectado());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
    @Test
    void previaPreservaPausaEQuatroAusentesCabemNoPainel() throws Exception {
        cliente.executarComando("barreira h 3,4 4,5");
        for (Registro registro : registros) { remoto.desconectar(registro.token()); }
        saida.getBuffer().setLength(0);
        cliente.atualizar(false);
        assertTrue(saida.toString().contains("PARTIDA PAUSADA"));
        for (int id = 1; id <= 4; id++) {
            assertTrue(saida.toString().contains("Jogador " + id + " (03:00)"));
        }
        nanos.set(TimeUnit.SECONDS.toNanos(180));
        saida.getBuffer().setLength(0);
        cliente.atualizar(false);
        assertTrue(saida.toString().contains("PARTIDA ENCERRADA"));
        assertTrue(saida.toString().contains("sem vencedor"));
    }

}
