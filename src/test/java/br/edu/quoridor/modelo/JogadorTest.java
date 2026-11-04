package br.edu.quoridor.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JogadorTest {
    @Test
    void iniciaComCincoBarreiras() {
        Jogador jogador = new Jogador(1, new Posicao(0, 4), Objetivo.LINHA_8);
        assertEquals(5, jogador.getBarreirasDisponiveis());
    }

    @Test
    void rejeitaIdentificadorForaDoIntervalo() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Jogador(0, new Posicao(0, 4), Objetivo.LINHA_8)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Jogador(5, new Posicao(0, 4), Objetivo.LINHA_8))
        );
    }

    @Test
    void rejeitaPosicaoNula() {
        assertThrows(NullPointerException.class, () -> new Jogador(1, null, Objetivo.LINHA_8));
    }

    @Test
    void rejeitaObjetivoNulo() {
        assertThrows(NullPointerException.class, () -> new Jogador(1, new Posicao(0, 4), null));
    }
}
