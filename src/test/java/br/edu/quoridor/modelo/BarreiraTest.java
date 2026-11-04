package br.edu.quoridor.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BarreiraTest {
    @Test
    void aceitaTodasAsAncorasNasDuasOrientacoes() {
        for (Orientacao orientacao : Orientacao.values()) {
            for (int linha = 0; linha < 8; linha++) {
                for (int coluna = 0; coluna < 8; coluna++) {
                    Barreira barreira = new Barreira(linha, coluna, orientacao);
                    assertEquals(linha, barreira.linha());
                    assertEquals(coluna, barreira.coluna());
                    assertEquals(orientacao, barreira.orientacao());
                }
            }
        }
    }

    @Test
    void rejeitaLinhaDaAncoraForaDoIntervalo() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Barreira(-1, 0, Orientacao.HORIZONTAL)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Barreira(8, 0, Orientacao.VERTICAL))
        );
    }

    @Test
    void rejeitaColunaDaAncoraForaDoIntervalo() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Barreira(0, -1, Orientacao.VERTICAL)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Barreira(0, 8, Orientacao.HORIZONTAL))
        );
    }

    @Test
    void rejeitaOrientacaoNula() {
        assertThrows(NullPointerException.class, () -> new Barreira(0, 0, null));
    }

    @Test
    void comparaBarreirasPelaAncoraEOrientacao() {
        assertEquals(new Barreira(2, 3, Orientacao.HORIZONTAL),
                new Barreira(2, 3, Orientacao.HORIZONTAL));
        assertNotEquals(new Barreira(2, 3, Orientacao.HORIZONTAL),
                new Barreira(2, 3, Orientacao.VERTICAL));
    }
}
