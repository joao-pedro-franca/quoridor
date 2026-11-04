package br.edu.quoridor.modelo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstadoJogoTest {
    @Test
    void copiaListasRecebidasSemExporMutabilidade() {
        var jogadores = new ArrayList<>(new EstadoJogo().getJogadores());
        var barreiras = new ArrayList<Barreira>();
        EstadoJogo estado = new EstadoJogo(jogadores, barreiras, 2, null);
        jogadores.clear();
        barreiras.add(new Barreira(0, 0, Orientacao.HORIZONTAL));
        assertEquals(4, estado.getJogadores().size());
        assertTrue(estado.getBarreiras().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> estado.getJogadores().clear());
        assertThrows(UnsupportedOperationException.class, () -> estado.getBarreiras().clear());
    }

    @Test
    void rejeitaEstoqueNegativoOuAcimaDeCinco() {
        assertThrows(IllegalArgumentException.class,
                () -> new Jogador(1, new Posicao(0, 4), Objetivo.LINHA_8, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new Jogador(1, new Posicao(0, 4), Objetivo.LINHA_8, 6));
    }

    @Test
    void rejeitaQuantidadeInvalidaDeJogadores() {
        assertThrows(IllegalArgumentException.class, () -> new EstadoJogo(List.of(), List.of(), 1, null));
    }

    @Test
    void rejeitaIdsDuplicadosOuForaDaOrdem() {
        var jogadores = new ArrayList<>(new EstadoJogo().getJogadores());
        jogadores.set(1, jogadores.get(0));
        assertThrows(IllegalArgumentException.class, () -> new EstadoJogo(jogadores, List.of(), 1, null));
    }

    @Test
    void rejeitaDoisPeoesNaMesmaCasa() {
        var jogadores = new ArrayList<>(new EstadoJogo().getJogadores());
        jogadores.set(1, new Jogador(2, new Posicao(0, 4), Objetivo.COLUNA_0));
        assertThrows(IllegalArgumentException.class, () -> new EstadoJogo(jogadores, List.of(), 1, null));
    }

    @Test
    void rejeitaTurnoEVencedorInexistentes() {
        List<Jogador> jogadores = new EstadoJogo().getJogadores();
        assertThrows(IllegalArgumentException.class, () -> new EstadoJogo(jogadores, List.of(), 0, null));
        assertThrows(IllegalArgumentException.class, () -> new EstadoJogo(jogadores, List.of(), 5, null));
        assertThrows(IllegalArgumentException.class, () -> new EstadoJogo(jogadores, List.of(), 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new EstadoJogo(jogadores, List.of(), 1, 5));
    }
    @Test
    void representaAbandonoSemInventarVencedorEPreservaConstrutorAnterior() {
        EstadoJogo inicial = new EstadoJogo();
        assertEquals(SituacaoPartida.EM_ANDAMENTO, inicial.getSituacao());
        EstadoJogo abandono = new EstadoJogo(inicial.getJogadores(), java.util.List.of(), 3,
                null, SituacaoPartida.ENCERRADA_POR_ABANDONO, 2);
        assertTrue(abandono.isFinalizada());
        assertFalse(abandono.isPausada());
        assertNull(abandono.getVencedor());
        assertEquals(Integer.valueOf(2), abandono.getJogadorAbandono());
        EstadoJogo vitoria = new EstadoJogo(inicial.getJogadores(), java.util.List.of(), 1, 1);
        assertEquals(SituacaoPartida.ENCERRADA_POR_VITORIA, vitoria.getSituacao());
    }

    @Test
    void rejeitaCombinacoesInconsistentesDeSituacaoEVencedor() {
        var jogadores = new EstadoJogo().getJogadores();
        assertThrows(IllegalArgumentException.class, () -> new EstadoJogo(jogadores,
                java.util.List.of(), 1, 2, SituacaoPartida.PAUSADA, null));
        assertThrows(IllegalArgumentException.class, () -> new EstadoJogo(jogadores,
                java.util.List.of(), 1, null, SituacaoPartida.ENCERRADA_POR_ABANDONO, null));
        assertThrows(IllegalArgumentException.class, () -> new EstadoJogo(jogadores,
                java.util.List.of(), 1, null, SituacaoPartida.ENCERRADA_POR_ABANDONO, 5));
    }

}
