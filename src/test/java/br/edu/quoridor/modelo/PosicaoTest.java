package br.edu.quoridor.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PosicaoTest {
    @Test
    void aceitaTodasAsCasasDoTabuleiro() {
        for (int linha = 0; linha < 9; linha++) {
            for (int coluna = 0; coluna < 9; coluna++) {
                Posicao posicao = new Posicao(linha, coluna);
                assertEquals(linha, posicao.linha());
                assertEquals(coluna, posicao.coluna());
            }
        }
    }

    @Test
    void rejeitaLinhaForaDoTabuleiro() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new Posicao(-1, 4)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Posicao(9, 4))
        );
    }

    @Test
    void rejeitaColunaForaDoTabuleiro() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new Posicao(4, -1)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Posicao(4, 9))
        );
    }

    @Test
    void comparaPosicoesPelasCoordenadas() {
        assertEquals(new Posicao(3, 4), new Posicao(3, 4));
        assertNotEquals(new Posicao(3, 4), new Posicao(4, 3));
    }
}
