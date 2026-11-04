package br.edu.quoridor.jogo;

import br.edu.quoridor.modelo.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PartidaTest {
    @Test
    void iniciaComQuatroJogadoresNasPosicoesCorretas() {
        List<Jogador> jogadores = new Partida().getEstado().getJogadores();
        assertEquals(4, jogadores.size());
        List<Posicao> posicoes = List.of(
                new Posicao(0, 4), new Posicao(4, 8),
                new Posicao(8, 4), new Posicao(4, 0));
        for (int indice = 0; indice < 4; indice++) {
            assertEquals(indice + 1, jogadores.get(indice).getId());
            assertEquals(posicoes.get(indice), jogadores.get(indice).getPosicao());
        }
    }

    @Test
    void defineOsObjetivosDosQuatroJogadores() {
        List<Jogador> jogadores = new Partida().getEstado().getJogadores();
        assertAll(
                () -> assertEquals(Objetivo.LINHA_8, jogadores.get(0).getObjetivo()),
                () -> assertEquals(Objetivo.COLUNA_0, jogadores.get(1).getObjetivo()),
                () -> assertEquals(Objetivo.LINHA_0, jogadores.get(2).getObjetivo()),
                () -> assertEquals(Objetivo.COLUNA_8, jogadores.get(3).getObjetivo())
        );
    }

    @Test
    void todosOsJogadoresComecamComCincoBarreiras() {
        for (Jogador jogador : new Partida().getEstado().getJogadores()) {
            assertEquals(5, jogador.getBarreirasDisponiveis());
        }
    }

    @Test
    void iniciaSemBarreirasNoTabuleiroENoTurnoDoJogadorUm() {
        EstadoJogo estado = new Partida(new EstadoJogo()).getEstado();
        assertTrue(estado.getBarreiras().isEmpty());
        assertEquals(1, estado.getJogadorDaVez());
    }

    @Test
    void impedeAlteracaoDasColecoesExpostas() {
        EstadoJogo estado = new Partida().getEstado();
        assertAll(
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> estado.getJogadores().remove(0)),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> estado.getJogadores().set(0, estado.getJogadores().get(1))),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> estado.getBarreiras().add(new Barreira(0, 0, Orientacao.HORIZONTAL)))
        );
        assertEquals(4, estado.getJogadores().size());
        assertTrue(estado.getBarreiras().isEmpty());
    }
}
