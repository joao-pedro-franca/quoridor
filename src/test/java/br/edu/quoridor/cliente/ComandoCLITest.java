package br.edu.quoridor.cliente;

import br.edu.quoridor.modelo.Orientacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ComandoCLITest {
    @ParameterizedTest
    @CsvSource({"ajuda,AJUDA", "tabuleiro,TABULEIRO", "sessao,SESSAO", "sair,SAIR"})
    void interpretaComandosSemArgumentos(String texto, ComandoCLI.Tipo tipo) {
        assertEquals(tipo, ComandoCLI.interpretar(texto).tipo());
    }

    @Test
    void converteTodasAsCasasDeUmANoveParaZeroAOito() {
        for (int linha = 1; linha <= 9; linha++) {
            for (int coluna = 1; coluna <= 9; coluna++) {
                var comando = ComandoCLI.interpretar("mover " + linha + " " + coluna);
                assertEquals(ComandoCLI.Tipo.MOVER, comando.tipo());
                assertEquals(linha - 1, comando.linha());
                assertEquals(coluna - 1, comando.coluna());
            }
        }
    }

    @Test
    void converteTodasAsAncorasENasDuasOrientacoes() {
        for (String orientacao : new String[]{"H", "V"}) {
            for (int linha = 1; linha <= 8; linha++) {
                for (int coluna = 1; coluna <= 8; coluna++) {
                    var comando = ComandoCLI.interpretar("barreira " + orientacao + " " + linha + " " + coluna);
                    assertEquals(ComandoCLI.Tipo.BARREIRA, comando.tipo());
                    assertEquals(linha - 1, comando.linha());
                    assertEquals(coluna - 1, comando.coluna());
                    assertEquals(orientacao.equals("H") ? Orientacao.HORIZONTAL : Orientacao.VERTICAL,
                            comando.orientacao());
                }
            }
        }
    }

    @Test
    void aceitaMaiusculasEspacosETabulacoes() {
        var comando = ComandoCLI.interpretar("  BaRrEiRa\t v  3   4  ");
        assertEquals(new ComandoCLI(ComandoCLI.Tipo.BARREIRA, 2, 3, Orientacao.VERTICAL), comando);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "pular", "ajuda extra", "sair agora", "mover", "mover 1",
            "mover 1 2 3", "mover 0 1", "mover 1 10", "mover -1 2", "mover 1.5 2",
            "mover a b", "mover 999999999999999999999 1", "barreira", "barreira H 1",
            "barreira H 1 2 3", "barreira X 1 2", "barreira H 0 1", "barreira V 8 9",
            "barreira H 9 8", "barreira V 1 -1", "barreira H a 1"})
    void recusaEntradasInvalidasComMensagem(String texto) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ComandoCLI.interpretar(texto));
        assertFalse(erro.getMessage().isBlank());
    }
}
