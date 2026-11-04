package br.edu.quoridor.rmi;

import br.edu.quoridor.modelo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static br.edu.quoridor.rmi.ApoioRmi.*;
import static org.junit.jupiter.api.Assertions.*;

class ServicoJogoTest {
    private ServicoJogo servico;

    @BeforeEach
    void iniciar() throws Exception {
        servico = novoServico();
    }

    @AfterEach
    void encerrar() {
        if (servico != null) {
            servico.close();
        }
    }

    @Test
    void consultaInicialIncluiJogoEQuantidadeDeRegistros() throws Exception {
        EstadoServico estado = servico.consultarEstado();
        assertEquals(0, estado.jogadoresRegistrados());
        assertEquals(4, estado.jogo().getJogadores().size());
        assertEquals(1, estado.jogo().getJogadorDaVez());
        assertNull(estado.jogo().getVencedor());
        assertTrue(estado.jogo().getBarreiras().isEmpty());
        assertFalse(estado.jogo().isFinalizada());
    }

    @Test
    void registraQuatroJogadoresComIdsETokensDistintos() throws Exception {
        var tokens = new HashSet<String>();
        for (int id = 1; id <= 4; id++) {
            Registro registro = servico.registrar();
            assertTrue(registro.sucesso());
            assertEquals(id, registro.jogadorId());
            assertEquals(4, UUID.fromString(registro.token()).version());
            assertTrue(tokens.add(registro.token()));
            assertEquals(id, servico.consultarEstado().jogadoresRegistrados());
        }
    }

    @Test
    void recusaQuintoJogadorSemDivulgarCredenciais() throws Exception {
        registrarQuatro(servico);
        EstadoServico antes = servico.consultarEstado();
        Registro quinto = servico.registrar();
        assertFalse(quinto.sucesso());
        assertEquals(0, quinto.jogadorId());
        assertNull(quinto.token());
        assertTrue(quinto.mensagem().contains("quatro"));
        assertEquals(antes, servico.consultarEstado());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"inexistente", "1", "00000000-0000-0000-0000-000000000000"})
    void rejeitaTokensInvalidosEmTodasAsAcoes(String token) throws Exception {
        registrarQuatro(servico);
        EstadoServico antes = servico.consultarEstado();
        ResultadoAcao movimento = servico.mover(token, 1, 4);
        assertFalse(movimento.sucesso());
        assertTrue(movimento.mensagem().contains("Token"));
        assertFalse(servico.colocarBarreira(token, 3, 3, Orientacao.HORIZONTAL).sucesso());
        Registro reconexao = servico.reconectar(token);
        assertFalse(reconexao.sucesso());
        assertEquals(0, reconexao.jogadorId());
        assertNull(reconexao.token());
        assertEquals(antes, servico.consultarEstado());
    }

    @Test
    void aguardaQuatroParticipantesAntesDePermitirQualquerJogada() throws Exception {
        String token = servico.registrar().token();
        for (int quantidade = 1; quantidade < 4; quantidade++) {
            EstadoServico antes = servico.consultarEstado();
            ResultadoAcao resultado = servico.mover(token, 1, 4);
            assertFalse(resultado.sucesso());
            assertTrue(resultado.mensagem().contains("Aguardando"));
            assertFalse(servico.colocarBarreira(token, 3, 3, Orientacao.HORIZONTAL).sucesso());
            assertEquals(antes, servico.consultarEstado());
            servico.registrar();
        }
        assertTrue(servico.mover(token, 1, 4).sucesso());
    }

    @Test
    void tokenDoSegundoJogadorNaoPodeMoverOPrimeiro() throws Exception {
        List<Registro> registros = registrarQuatro(servico);
        EstadoServico antes = servico.consultarEstado();
        assertFalse(servico.mover(registros.get(1).token(), 1, 4).sucesso());
        assertFalse(servico.colocarBarreira(registros.get(1).token(), 3, 3, Orientacao.HORIZONTAL).sucesso());
        assertEquals(antes, servico.consultarEstado());
        assertTrue(servico.mover(registros.get(0).token(), 1, 4).sucesso());
        assertEquals(new Posicao(1, 4), servico.consultarEstado().jogo().getJogadores().get(0).getPosicao());
    }

    @Test
    void movimentoValidoAtualizaPosicaoETurno() throws Exception {
        var registros = registrarQuatro(servico);
        assertTrue(servico.mover(registros.get(0).token(), 1, 4).sucesso());
        EstadoJogo estado = servico.consultarEstado().jogo();
        assertEquals(new Posicao(1, 4), estado.getJogadores().get(0).getPosicao());
        assertEquals(2, estado.getJogadorDaVez());
        assertEquals(5, estado.getJogadores().get(0).getBarreirasDisponiveis());
    }

    @Test
    void movimentoInvalidoMantemEstadoETurno() throws Exception {
        String token = registrarQuatro(servico).get(0).token();
        EstadoServico antes = servico.consultarEstado();
        assertFalse(servico.mover(token, 2, 4).sucesso());
        assertFalse(servico.mover(token, -1, 4).sucesso());
        assertFalse(servico.mover(token, 0, 4).sucesso());
        assertEquals(antes, servico.consultarEstado());
    }

    @Test
    void barreiraValidaConsomeEstoqueEAplicaRegrasExistentes() throws Exception {
        var registros = registrarQuatro(servico);
        assertTrue(servico.colocarBarreira(registros.get(0).token(), 3, 3, Orientacao.HORIZONTAL).sucesso());
        EstadoServico antes = servico.consultarEstado();
        assertEquals(List.of(new Barreira(3, 3, Orientacao.HORIZONTAL)), antes.jogo().getBarreiras());
        assertEquals(4, antes.jogo().getJogadores().get(0).getBarreirasDisponiveis());
        assertEquals(2, antes.jogo().getJogadorDaVez());
        assertFalse(servico.colocarBarreira(registros.get(1).token(), 3, 4, Orientacao.HORIZONTAL).sucesso());
        assertFalse(servico.colocarBarreira(registros.get(1).token(), 3, 3, Orientacao.VERTICAL).sucesso());
        assertFalse(servico.colocarBarreira(registros.get(1).token(), 8, 3, Orientacao.VERTICAL).sucesso());
        assertFalse(servico.colocarBarreira(registros.get(1).token(), 3, 3, null).sucesso());
        assertEquals(antes, servico.consultarEstado());
    }

    @Test
    void reconectaMesmoJogadorSemCriarVagaNova() throws Exception {
        var registros = registrarQuatro(servico);
        String token = registros.get(0).token();
        EstadoServico antes = servico.consultarEstado();
        Registro reconexao = servico.reconectar(token);
        assertTrue(reconexao.sucesso());
        assertEquals(1, reconexao.jogadorId());
        assertEquals(token, reconexao.token());
        assertEquals(antes, servico.consultarEstado());
        assertTrue(servico.mover(reconexao.token(), 1, 4).sucesso());
        assertFalse(servico.registrar().sucesso());
    }

    @Test
    void reconectaAntesDoQuartoRegistroSemAvancarContagem() throws Exception {
        Registro registro = servico.registrar();
        assertTrue(servico.reconectar(registro.token()).sucesso());
        assertTrue(servico.reconectar(registro.token()).sucesso());
        assertEquals(1, servico.consultarEstado().jogadoresRegistrados());
        assertEquals(2, servico.registrar().jogadorId());
    }

    @Test
    void consultasAnterioresPermanecemConsistentesEImutaveis() throws Exception {
        EstadoServico vazio = servico.consultarEstado();
        var registros = registrarQuatro(servico);
        EstadoServico antes = servico.consultarEstado();
        assertTrue(servico.mover(registros.get(0).token(), 1, 4).sucesso());
        assertEquals(0, vazio.jogadoresRegistrados());
        assertEquals(4, antes.jogadoresRegistrados());
        assertEquals(new Posicao(0, 4), antes.jogo().getJogadores().get(0).getPosicao());
        assertEquals(1, antes.jogo().getJogadorDaVez());
        assertThrows(UnsupportedOperationException.class, () -> antes.jogo().getJogadores().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> antes.jogo().getBarreiras().add(new Barreira(0, 0, Orientacao.HORIZONTAL)));
    }

    @Test
    void partidaCompletaPeloServicoRejeitaAcoesDepoisDaVitoria() throws Exception {
        var registros = registrarQuatro(servico);
        // Jogador 1 usa a coluna 3; os demais alternam casas sem bloquear esse trajeto.
        for (int rodada = 0; rodada <= 8; rodada++) {
            assertTrue(servico.mover(registros.get(0).token(), rodada, 3).sucesso());
            if (rodada == 8) {
                break;
            }
            assertTrue(servico.mover(registros.get(1).token(), rodada % 2 == 0 ? 3 : 4, 8).sucesso());
            assertTrue(servico.mover(registros.get(2).token(), 8, rodada % 2 == 0 ? 5 : 4).sucesso());
            assertTrue(servico.mover(registros.get(3).token(), rodada % 2 == 0 ? 3 : 4, 0).sucesso());
        }
        EstadoServico finalizado = servico.consultarEstado();
        assertTrue(finalizado.jogo().isFinalizada());
        assertEquals(Integer.valueOf(1), finalizado.jogo().getVencedor());
        for (Registro registro : registros) {
            assertFalse(servico.mover(registro.token(), 7, 3).sucesso());
            assertFalse(servico.colocarBarreira(registro.token(), 3, 3, Orientacao.HORIZONTAL).sucesso());
            assertTrue(servico.reconectar(registro.token()).sucesso());
            assertEquals(finalizado, servico.consultarEstado());
        }
    }
}
