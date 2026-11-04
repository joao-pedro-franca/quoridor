package br.edu.quoridor.cliente;

import br.edu.quoridor.modelo.*;
import br.edu.quoridor.rmi.JogoRemoto;
import br.edu.quoridor.rmi.ServicoJogo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Roteiros de aceitação: comandos reais da CLI, lookup e RPC, desde o estado inicial. */
@Timeout(30)
class AceitacaoJogoTest {
    private Registry registro;
    private ServicoJogo servico;
    private final JogoRemoto[] remotos = new JogoRemoto[4];
    private final Registro[] sessoes = new Registro[4];
    private final Cliente[] clientes = new Cliente[4];
    private final StringWriter[] saidas = new StringWriter[4];

    @BeforeEach
    void iniciarQuatroParticipantes() throws Exception {
        registro = LocateRegistry.createRegistry(0);
        servico = new ServicoJogo(new Random() {
            @Override
            public int nextInt(int limite) { return 0; }
        });
        registro.bind("Quoridor", servico);
        Registry descoberta = (Registry) UnicastRemoteObject.toStub(registro);
        for (int i = 0; i < 4; i++) {
            remotos[i] = (JogoRemoto) descoberta.lookup("Quoridor");
            sessoes[i] = remotos[i].registrar();
            assertTrue(sessoes[i].sucesso());
            assertEquals(i + 1, sessoes[i].jogadorId());
            saidas[i] = new StringWriter();
            clientes[i] = new Cliente(remotos[i], new BufferedReader(new StringReader("")),
                    new PrintWriter(saidas[i]), false);
            assertTrue(clientes[i].iniciarSessao("Aluno " + (i + 1), sessoes[i].token()));
        }
        verificarConsultasConsistentes();
    }

    @AfterEach
    void liberarRmi() throws Exception {
        try {
            if (servico != null) { servico.close(); }
        } finally {
            if (registro != null) { UnicastRemoteObject.unexportObject(registro, true); }
        }
    }

    @Test
    void partidaCompletaComBarreirasTerminaNaVitoriaDoJogadorDois() throws Exception {
        // Primeira rodada: a horizontal e a vertical não interceptam a rota do jogador 2.
        propor(1, "barreira h 3,4 4,5");
        comando(1, "N", "Posicionamento cancelado");
        colocar(1, "barreira h 3,4 4,5", new Barreira(2, 3, Orientacao.HORIZONTAL));
        mover(2, 4, 9);

        // O jogador 3 continua com seu turno e estoque após sobreposição e cruzamento.
        propor(3, "barreira h 3,4 4,5");
        confirmar(3, false);
        propor(3, "barreira v 3,4 4,5");
        confirmar(3, false);
        colocar(3, "barreira v 6,7 6,7", new Barreira(5, 5, Orientacao.VERTICAL));
        mover(4, 6, 1);

        // O jogador 2 percorre a linha 4 da CLI até a coluna 1 (objetivo coluna interna 0).
        // Os demais alternam duas casas; nenhuma ação passa a vez artificialmente.
        for (int rodada = 1; rodada <= 8; rodada++) {
            mover(1, 1, rodada % 2 == 1 ? 4 : 5);
            mover(2, 4, 9 - rodada);
            if (rodada < 8) {
                mover(3, 9, rodada % 2 == 1 ? 6 : 5);
                mover(4, rodada % 2 == 1 ? 5 : 6, 1);
            }
            verificarConsultasConsistentes();
        }

        EstadoServico finalizado = remotos[0].consultarEstado();
        assertEquals(Integer.valueOf(2), finalizado.jogo().getVencedor());
        assertTrue(finalizado.jogo().isFinalizada());
        assertEquals(new Posicao(3, 0), finalizado.jogo().getJogadores().get(1).getPosicao());
        assertEquals(2, finalizado.jogo().getBarreiras().size());
        assertEquals(List.of(4, 5, 4, 5), finalizado.jogo().getJogadores().stream()
                .map(Jogador::getBarreirasDisponiveis).toList());

        for (int id = 1; id <= 4; id++) {
            comando(id, "tabuleiro", "Vencedor: jogador 2");
            comando(id, "mover 4 2", "Erro: A partida já terminou");
            propor(id, "barreira h 7,8 1,2");
            confirmar(id, false);
            compararEstados(finalizado, remotos[id - 1].consultarEstado());
        }
    }

    @Test
    void cercamentoConstruidoPorJogadasReaisEhRecusadoSemEfeitos() throws Exception {
        // O jogador 1 começa em (0,4). A borda e estas barreiras deixariam apenas
        // as casas (0,3) e (0,4), sem saída para a linha 8. Não há cruzamentos.
        colocar(1, "barreira h 1,2 4,5", new Barreira(0, 3, Orientacao.HORIZONTAL));
        colocar(2, "barreira v 1,2 3,4", new Barreira(0, 2, Orientacao.VERTICAL));
        propor(3, "barreira v 1,2 5,6");
        confirmar(3, false);
        assertEquals(List.of(4, 4, 5, 5), remotos[0].consultarEstado().jogo().getJogadores().stream()
                .map(Jogador::getBarreirasDisponiveis).toList());

        // A rejeição permite outra ação do jogador 3 e o peão 1 ainda pode escapar.
        colocar(3, "barreira h 7,8 7,8", new Barreira(6, 6, Orientacao.HORIZONTAL));
        mover(4, 6, 1);
        mover(1, 1, 6);
        verificarConsultasConsistentes();
    }

    @Test
    void propostaDesatualizadaNaoReservaLocalNemSeSobrepoeAoEstadoOficial() throws Exception {
        propor(1, "barreira h 3,4 4,5");
        // Outra conexão da mesma sessão faz uma ação legal; a prévia não reserva o turno.
        JogoRemoto reconexao = (JogoRemoto) ((Registry) UnicastRemoteObject.toStub(registro)).lookup("Quoridor");
        assertTrue(reconexao.reconectar(sessoes[0].token()).sucesso());
        assertTrue(reconexao.mover(sessoes[0].token(), 1, 4).sucesso());
        colocar(2, "barreira h 3,4 4,5", new Barreira(2, 3, Orientacao.HORIZONTAL));
        mover(3, 8, 5);
        mover(4, 6, 1);
        assertEquals(1, remotos[0].consultarEstado().jogo().getJogadorDaVez());

        // Mesmo sendo novamente o turno 1, a confirmação deve falhar: o local foi ocupado.
        clientes[0].atualizar(false);
        confirmar(1, false);
        assertEquals(5, remotos[0].consultarEstado().jogo().getJogadores().get(0).getBarreirasDisponiveis());
        mover(1, 3, 5);
        verificarConsultasConsistentes();
    }

    private void propor(int id, String texto) throws Exception {
        EstadoServico antes = remotos[id - 1].consultarEstado();
        comando(id, texto, "Confirmar posicionamento? (S/N)");
        compararEstados(antes, remotos[id - 1].consultarEstado());
    }

    private void confirmar(int id, boolean aceita) throws Exception {
        EstadoServico antes = remotos[id - 1].consultarEstado();
        comando(id, "S", aceita ? "OK: Barreira colocada" : "Erro:");
        if (!aceita) {
            compararEstados(antes, remotos[id - 1].consultarEstado());
        }
    }

    private void colocar(int id, String texto, Barreira esperada) throws Exception {
        EstadoJogo antes = remotos[id - 1].consultarEstado().jogo();
        assertEquals(id, antes.getJogadorDaVez());
        propor(id, texto);
        confirmar(id, true);
        EstadoJogo depois = remotos[id - 1].consultarEstado().jogo();
        assertEquals(esperada, depois.getBarreiras().getLast());
        assertEquals(antes.getJogadores().get(id - 1).getBarreirasDisponiveis() - 1,
                depois.getJogadores().get(id - 1).getBarreirasDisponiveis());
        assertEquals(id % 4 + 1, depois.getJogadorDaVez());
    }

    private void mover(int id, int linha, int coluna) throws Exception {
        assertEquals(id, remotos[id - 1].consultarEstado().jogo().getJogadorDaVez());
        comando(id, "mover " + linha + " " + coluna, "OK:");
        assertEquals(new Posicao(linha - 1, coluna - 1),
                remotos[id - 1].consultarEstado().jogo().getJogadores().get(id - 1).getPosicao());
    }

    private void comando(int id, String texto, String respostaEsperada) throws Exception {
        // Limpa o histórico para uma resposta antiga não satisfazer uma nova verificação.
        saidas[id - 1].getBuffer().setLength(0);
        assertTrue(clientes[id - 1].executarComando(texto));
        assertTrue(saidas[id - 1].toString().contains(respostaEsperada),
                "Resposta inesperada para jogador " + id + ", comando " + texto);
    }

    private void verificarConsultasConsistentes() throws Exception {
        EstadoServico esperado = servico.consultarEstado();
        assertEquals(4, esperado.jogadoresRegistrados());
        assertEquals(20, esperado.jogo().getBarreiras().size() + esperado.jogo().getJogadores().stream()
                .mapToInt(Jogador::getBarreirasDisponiveis).sum());
        for (JogoRemoto remoto : remotos) {
            compararEstados(esperado, remoto.consultarEstado());
        }
    }

    private void compararEstados(EstadoServico esperado, EstadoServico recebido) {
        assertEquals(esperado.jogadoresRegistrados(), recebido.jogadoresRegistrados());
        EstadoJogo a = esperado.jogo();
        EstadoJogo b = recebido.jogo();
        assertEquals(a.getJogadorDaVez(), b.getJogadorDaVez());
        assertEquals(a.getVencedor(), b.getVencedor());
        assertEquals(a.isFinalizada(), b.isFinalizada());
        assertEquals(a.getBarreiras(), b.getBarreiras());
        assertEquals(a.getJogadores().size(), b.getJogadores().size());
        for (int i = 0; i < a.getJogadores().size(); i++) {
            Jogador original = a.getJogadores().get(i);
            Jogador copia = b.getJogadores().get(i);
            assertEquals(original.getId(), copia.getId());
            assertEquals(original.getPosicao(), copia.getPosicao());
            assertEquals(original.getObjetivo(), copia.getObjetivo());
            assertEquals(original.getBarreirasDisponiveis(), copia.getBarreirasDisponiveis());
        }
    }
}
