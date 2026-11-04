package br.edu.quoridor.rmi;

import br.edu.quoridor.modelo.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class HeartbeatTest {
    private final AtomicLong nanos = new AtomicLong();
    private ScheduledExecutorService monitor;
    private ServicoJogo servico;
    private final List<Registro> registros = new ArrayList<>();

    private Random sorteio() {
        return new Random() { @Override public int nextInt(int limite) { return 0; } };
    }

    @BeforeEach
    void iniciar() throws Exception {
        monitor = Executors.newSingleThreadScheduledExecutor();
        servico = new ServicoJogo(sorteio(), nanos::get, monitor);
    }

    @AfterEach
    void encerrar() throws Exception {
        servico.close();
        assertTrue(monitor.awaitTermination(3, TimeUnit.SECONDS));
    }

    private void registrarQuatro() throws RemoteException {
        for (int i = 0; i < 4; i++) { registros.add(servico.registrar()); }
    }
    private String token(int id) { return registros.get(id - 1).token(); }
    private EstadoServico estado() throws RemoteException { return servico.consultarEstado(); }
    private PresencaJogador presenca(int id) throws RemoteException {
        return estado().presencas().stream().filter(p -> p.jogadorId() == id).findFirst().orElseThrow();
    }
    /** Avança em passos menores que dez segundos, mantendo somente os IDs indicados ativos. */
    private void avancar(long segundos, int... ativos) throws RemoteException {
        while (segundos > 0) {
            long passo = Math.min(3, segundos);
            nanos.addAndGet(TimeUnit.SECONDS.toNanos(passo));
            for (int id : ativos) { assertTrue(servico.heartbeat(token(id)).sucesso()); }
            servico.verificarConexoes();
            segundos -= passo;
        }
    }

    @Test
    void heartbeatValidoNaoAlteraJogo() throws Exception {
        registrarQuatro();
        EstadoJogo antes = estado().jogo();
        assertTrue(servico.heartbeat(token(1)).sucesso());
        assertSame(antes, estado().jogo());
        assertEquals(new PresencaJogador(1, true, 0), presenca(1));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalido", "1"})
    void novosMetodosRecusamTokensInvalidos(String token) throws Exception {
        registrarQuatro();
        EstadoServico antes = estado();
        assertFalse(servico.heartbeat(token).sucesso());
        assertFalse(servico.desconectar(token).sucesso());
        assertEquals(antes, estado());
    }

    @Test
    void ativosSemFazerJogadasPermanecemConectados() throws Exception {
        registrarQuatro();
        EstadoJogo antes = estado().jogo();
        avancar(210, 1, 2, 3, 4);
        assertSame(antes, estado().jogo());
        assertTrue(estado().presencas().stream().allMatch(PresencaJogador::conectado));
    }

    @Test
    void detectaNoLimiteDeDezSegundosENaoAntes() throws Exception {
        registrarQuatro();
        nanos.set(TimeUnit.SECONDS.toNanos(10) - 1);
        servico.verificarConexoes();
        assertFalse(estado().jogo().isPausada());
        nanos.incrementAndGet();
        servico.verificarConexoes();
        assertTrue(estado().jogo().isPausada());
        assertEquals(new PresencaJogador(1, false, 180), presenca(1));
        assertEquals(4, estado().presencas().stream().filter(p -> !p.conectado()).count());
    }

    @Test
    void saidaNotificadaPausaImediatamenteEPreservaTodosOsDadosDoJogo() throws Exception {
        registrarQuatro();
        assertTrue(servico.colocarBarreira(token(1), 2, 3, Orientacao.HORIZONTAL).sucesso());
        EstadoJogo antes = estado().jogo();
        assertTrue(servico.desconectar(token(2)).sucesso());
        EstadoJogo depois = estado().jogo();
        assertEquals(SituacaoPartida.PAUSADA, depois.getSituacao());
        assertSame(antes.getJogadores().get(0), depois.getJogadores().get(0));
        assertEquals(antes.getBarreiras(), depois.getBarreiras());
        assertEquals(antes.getJogadorDaVez(), depois.getJogadorDaVez());
        assertEquals(180, presenca(2).segundosRestantes());
        EstadoServico pausado = estado();
        assertFalse(servico.mover(token(2), 4, 7).sucesso());
        assertFalse(servico.colocarBarreira(token(2), 5, 5, Orientacao.VERTICAL).sucesso());
        assertEquals(pausado, estado());
    }

    @Test
    void consultasSaidasRepetidasEHeartbeatsIndevidosNaoReiniciamPrazo() throws Exception {
        registrarQuatro();
        servico.desconectar(token(2));
        avancar(30, 1, 3, 4);
        assertEquals(150, presenca(2).segundosRestantes());
        EstadoServico antes = estado();
        assertFalse(servico.heartbeat(token(2)).sucesso());
        servico.desconectar(token(2));
        for (int i = 0; i < 10; i++) { assertEquals(antes, estado()); }
        assertEquals(150, presenca(2).segundosRestantes());
    }

    @Test
    void reconectarAntesDoPrazoRestauraMesmoIdEstoqueETurno() throws Exception {
        registrarQuatro();
        servico.colocarBarreira(token(1), 2, 3, Orientacao.HORIZONTAL);
        EstadoJogo antes = estado().jogo();
        servico.desconectar(token(1));
        avancar(179, 2, 3, 4);
        nanos.addAndGet(TimeUnit.SECONDS.toNanos(1) - 1);
        Registro retorno = servico.reconectar(token(1));
        assertTrue(retorno.sucesso());
        assertEquals(1, retorno.jogadorId());
        assertEquals(token(1), retorno.token());
        assertEquals(SituacaoPartida.EM_ANDAMENTO, estado().jogo().getSituacao());
        assertEquals(antes.getJogadorDaVez(), estado().jogo().getJogadorDaVez());
        assertEquals(antes.getBarreiras(), estado().jogo().getBarreiras());
        assertEquals(4, estado().jogo().getJogadores().get(0).getBarreirasDisponiveis());
        assertEquals(4, estado().jogadoresRegistrados());
        assertFalse(servico.registrar().sucesso());
        assertTrue(presenca(1).conectado());
        avancar(5, 1, 2, 3, 4);
        assertFalse(estado().jogo().isFinalizada());
    }

    @Test
    void expiraExatamenteEm180SegundosSemVencedorESemRetomada() throws Exception {
        registrarQuatro();
        servico.desconectar(token(2));
        avancar(179, 1, 3, 4);
        assertFalse(estado().jogo().isFinalizada());
        nanos.addAndGet(TimeUnit.SECONDS.toNanos(1));
        assertFalse(servico.reconectar(token(2)).sucesso());
        EstadoServico fim = estado();
        assertTrue(fim.jogo().isFinalizada());
        assertEquals(SituacaoPartida.ENCERRADA_POR_ABANDONO, fim.jogo().getSituacao());
        assertNull(fim.jogo().getVencedor());
        assertEquals(Integer.valueOf(2), fim.jogo().getJogadorAbandono());
        assertEquals(0, presenca(2).segundosRestantes());
        for (int id = 1; id <= 4; id++) {
            assertFalse(servico.mover(token(id), 1, 4).sucesso());
            assertFalse(servico.colocarBarreira(token(id), 4, 4, Orientacao.HORIZONTAL).sucesso());
            assertFalse(servico.heartbeat(token(id)).sucesso());
            assertFalse(servico.reconectar(token(id)).sucesso());
        }
        assertFalse(servico.registrar().sucesso());
        assertEquals(fim, estado());
    }

    @Test
    void doisAusentesTemPrazosIndependentesESomenteUltimoRetornoRetoma() throws Exception {
        registrarQuatro();
        servico.desconectar(token(1));
        avancar(30, 2, 3, 4);
        servico.desconectar(token(3));
        assertEquals(150, presenca(1).segundosRestantes());
        assertEquals(180, presenca(3).segundosRestantes());
        assertTrue(servico.reconectar(token(1)).sucesso());
        assertTrue(estado().jogo().isPausada());
        assertTrue(servico.reconectar(token(3)).sucesso());
        assertFalse(estado().jogo().isPausada());
        assertEquals(1, estado().jogo().getJogadorDaVez());
    }

    @Test
    void primeiroPrazoExpiradoEncerraMesmoComOutroPrazoVigente() throws Exception {
        registrarQuatro();
        servico.desconectar(token(1));
        avancar(30, 2, 3, 4);
        servico.desconectar(token(3));
        avancar(149, 2, 4);
        nanos.addAndGet(TimeUnit.SECONDS.toNanos(1));
        servico.verificarConexoes();
        assertEquals(Integer.valueOf(1), estado().jogo().getJogadorAbandono());
        assertEquals(30, presenca(3).segundosRestantes());
    }

    @Test
    void naoHaTimeoutAntesDoRegistro() throws Exception {
        nanos.set(TimeUnit.DAYS.toNanos(1));
        servico.verificarConexoes();
        assertEquals(SituacaoPartida.EM_ANDAMENTO, estado().jogo().getSituacao());
        assertTrue(estado().presencas().isEmpty());
        Registro primeiro = servico.registrar();
        assertTrue(primeiro.sucesso());
        assertEquals(List.of(new PresencaJogador(1, true, 0)), estado().presencas());
        nanos.addAndGet(TimeUnit.SECONDS.toNanos(9));
        assertFalse(estado().jogo().isPausada());
    }

    @Test
    void naoDependeDeConsultaParaDetectarEEncerrar() throws Exception {
        servico.close();
        assertTrue(monitor.awaitTermination(3, TimeUnit.SECONDS));
        CountDownLatch atualizado = new CountDownLatch(1);
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1) {
            @Override protected void afterExecute(Runnable tarefa, Throwable falha) {
                if (nanos.get() >= TimeUnit.SECONDS.toNanos(190)) { atualizado.countDown(); }
            }
        };
        monitor = executor;
        servico = new ServicoJogo(sorteio(), nanos::get, monitor);
        registros.add(servico.registrar());
        nanos.set(TimeUnit.SECONDS.toNanos(190));
        assertTrue(atualizado.await(5, TimeUnit.SECONDS));
        assertTrue(estado().jogo().isFinalizada());
        assertNull(estado().jogo().getVencedor());
    }

    @Test
    void heartbeatTardioNaoReconecta() throws Exception {
        registrarQuatro();
        avancar(9, 1, 3, 4);
        nanos.addAndGet(TimeUnit.SECONDS.toNanos(1));
        assertFalse(servico.heartbeat(token(2)).sucesso());
        assertFalse(presenca(2).conectado());
        assertTrue(estado().jogo().isPausada());
        assertTrue(servico.reconectar(token(2)).sucesso());
        assertFalse(estado().jogo().isPausada());
    }

    @Test
    void reconexaoHeartbeatEMonitorConcorrentesAntesDoLimite() throws Exception {
        registrarQuatro();
        servico.desconectar(token(1));
        avancar(179, 2, 3, 4);
        nanos.addAndGet(TimeUnit.SECONDS.toNanos(1) - 1);
        ApoioRmi.simultaneamente(12, () -> {
            servico.verificarConexoes();
            assertTrue(servico.reconectar(token(1)).sucesso());
            assertTrue(servico.heartbeat(token(1)).sucesso());
            return servico.heartbeat(token(2));
        });
        assertFalse(estado().jogo().isFinalizada());
        assertFalse(estado().jogo().isPausada());
    }

    @Test
    void reconexoesNoLimiteNaoImpedemAbandonoAtomico() throws Exception {
        registrarQuatro();
        servico.desconectar(token(1));
        avancar(179, 2, 3, 4);
        nanos.addAndGet(TimeUnit.SECONDS.toNanos(1));
        ApoioRmi.simultaneamente(8, () -> {
            servico.verificarConexoes();
            assertFalse(servico.reconectar(token(1)).sucesso());
            return servico.heartbeat(token(1));
        });
        assertEquals(Integer.valueOf(1), estado().jogo().getJogadorAbandono());
    }

    @Test
    void closeEncerraMonitorEExportacaoDeFormaIdempotente() throws Exception {
        servico.close();
        servico.close();
        assertTrue(monitor.isShutdown());
        assertTrue(monitor.awaitTermination(3, TimeUnit.SECONDS));
        assertThrows(java.rmi.NoSuchObjectException.class,
                () -> java.rmi.server.UnicastRemoteObject.toStub(servico));
    }

    @Test
    void aritmeticaMonotonicaSuportaWrapDoNanoTime() throws Exception {
        AtomicLong tempo = new AtomicLong(Long.MAX_VALUE - TimeUnit.SECONDS.toNanos(5));
        try (ServicoJogo outro = new ServicoJogo(sorteio(), tempo::get)) {
            outro.registrar();
            tempo.addAndGet(TimeUnit.SECONDS.toNanos(10));
            assertTrue(outro.consultarEstado().jogo().isPausada());
            assertEquals(180, outro.consultarEstado().presencas().getFirst().segundosRestantes());
        }
    }

    @Test
    void vitoriaNaoIniciaContagemDeAbandono() throws Exception {
        registrarQuatro();
        for (int rodada = 0; rodada <= 8; rodada++) {
            assertTrue(servico.mover(token(1), rodada, 3).sucesso());
            if (rodada == 8) { break; }
            assertTrue(servico.mover(token(2), rodada % 2 == 0 ? 3 : 4, 8).sucesso());
            assertTrue(servico.mover(token(3), 8, rodada % 2 == 0 ? 5 : 4).sucesso());
            assertTrue(servico.mover(token(4), rodada % 2 == 0 ? 3 : 4, 0).sucesso());
        }
        EstadoJogo vencedor = estado().jogo();
        servico.desconectar(token(1));
        nanos.set(TimeUnit.DAYS.toNanos(1));
        servico.verificarConexoes();
        assertSame(vencedor, estado().jogo());
        assertEquals(SituacaoPartida.ENCERRADA_POR_VITORIA, vencedor.getSituacao());
        assertEquals(Integer.valueOf(1), vencedor.getVencedor());
        assertNull(vencedor.getJogadorAbandono());
    }
}
