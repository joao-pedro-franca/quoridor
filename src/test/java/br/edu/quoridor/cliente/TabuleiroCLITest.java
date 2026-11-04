package br.edu.quoridor.cliente;

import br.edu.quoridor.modelo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TabuleiroCLITest {
    @Test
    void mostraCoordenadasQuatroPeoesECasasVazias() {
        String[] linhas = TabuleiroCLI.desenhar(new EstadoServico(new EstadoJogo(), 0), 1).split("\n");
        assertEquals("    1   2   3   4   5   6   7   8   9", linhas[0]);
        for (int linha = 0; linha < 9; linha++) {
            assertTrue(linhas[2 * linha + 1].startsWith((linha + 1) + " "));
        }
        assertEquals('1', linhas[1].charAt(20));
        assertEquals('2', linhas[9].charAt(36));
        assertEquals('3', linhas[17].charAt(20));
        assertEquals('4', linhas[9].charAt(4));
        assertEquals('.', linhas[9].charAt(20));
    }

    @ParameterizedTest
    @CsvSource({"0,0", "3,4", "7,7"})
    void mostraDoisSegmentosHorizontaisNaAncoraCorreta(int linha, int coluna) {
        String[] desenho = desenharBarreira(new Barreira(linha, coluna, Orientacao.HORIZONTAL));
        assertEquals("=======", desenho[2 * linha + 2].substring(4 * coluna + 3, 4 * coluna + 10));
        assertEquals(7, java.util.Arrays.stream(desenho).limit(18)
                .flatMapToInt(String::chars).filter(c -> c == '=').count());
    }

    @ParameterizedTest
    @CsvSource({"0,0", "3,4", "7,7"})
    void mostraDoisSegmentosVerticaisNaAncoraCorreta(int linha, int coluna) {
        String[] desenho = desenharBarreira(new Barreira(linha, coluna, Orientacao.VERTICAL));
        for (int segmento = 0; segmento <= 2; segmento++) {
            assertEquals('|', desenho[2 * linha + segmento + 1].charAt(4 * coluna + 6));
        }
        // Conta somente as linhas do tabuleiro, excluindo o separador do resumo de turnos.
        long segmentos = java.util.Arrays.stream(desenho).limit(18)
                .flatMapToInt(String::chars).filter(c -> c == '|').count();
        assertEquals(3, segmentos);
    }

    @Test
    void mostraEstoqueTurnoEsperaEVezDoJogador() {
        var jogadores = new ArrayList<>(new EstadoJogo().getJogadores());
        jogadores.set(0, new Jogador(1, new Posicao(0, 4), Objetivo.LINHA_8, 4));
        EstadoJogo jogo = new EstadoJogo(jogadores, List.of(), 2, null);
        assertTrue(TabuleiroCLI.desenhar(new EstadoServico(jogo, 3), 1).contains("Aguardando"));
        String texto = TabuleiroCLI.desenhar(new EstadoServico(jogo, 4), 1);
        assertTrue(texto.contains("Turno: jogador 2"));
        assertTrue(texto.contains("1=4  2=5  3=5  4=5"));
        assertTrue(texto.contains("Não é a sua vez"));
        assertTrue(TabuleiroCLI.desenhar(new EstadoServico(jogo, 4), 2).contains("É a sua vez"));
    }

    @Test
    void mostraVencedorSemAnunciarNovoTurnoDisponivel() {
        EstadoJogo inicial = new EstadoJogo();
        var jogadores = new ArrayList<>(inicial.getJogadores());
        jogadores.set(0, new Jogador(1, new Posicao(8, 3), Objetivo.LINHA_8));
        EstadoJogo finalizado = new EstadoJogo(jogadores, List.of(), 1, 1);
        String texto = TabuleiroCLI.desenhar(new EstadoServico(finalizado, 4), 1);
        assertTrue(texto.contains("Vencedor: jogador 1"));
        assertFalse(texto.contains("É a sua vez"));
    }

    private String[] desenharBarreira(Barreira barreira) {
        EstadoJogo inicial = new EstadoJogo();
        EstadoJogo jogo = new EstadoJogo(inicial.getJogadores(), List.of(barreira), 1, null);
        return TabuleiroCLI.desenhar(new EstadoServico(jogo, 4), 1).split("\n");
    }

    @ParameterizedTest
    @CsvSource({"0,0", "2,3", "7,7"})
    void previaHorizontalDestacaSomentePropostaSemMudarEstado(int linha, int coluna) {
        EstadoServico estado = new EstadoServico(new EstadoJogo(), 4);
        String antes = TabuleiroCLI.desenhar(estado, 1);
        String[] previa = TabuleiroCLI.previsualizar(estado, 1,
                new Barreira(linha, coluna, Orientacao.HORIZONTAL)).split("\n");
        assertEquals("*******", previa[2 * linha + 2].substring(4 * coluna + 3, 4 * coluna + 10));
        assertTrue(String.join("\n", previa).contains("H: separa linhas " + (linha + 1) + " e " + (linha + 2)));
        assertEquals(antes, TabuleiroCLI.desenhar(estado, 1));
        assertTrue(estado.jogo().getBarreiras().isEmpty());
        assertEquals(5, estado.jogo().getJogadores().get(0).getBarreirasDisponiveis());
    }

    @ParameterizedTest
    @CsvSource({"0,0", "2,3", "7,7"})
    void previaVerticalPreservaBarreiraExistenteEDestacaProposta(int linha, int coluna) {
        EstadoJogo inicial = new EstadoJogo();
        EstadoServico estado = new EstadoServico(new EstadoJogo(inicial.getJogadores(),
                List.of(new Barreira(5, 0, Orientacao.HORIZONTAL)), 1, null), 4);
        String[] previa = TabuleiroCLI.previsualizar(estado, 1,
                new Barreira(linha, coluna, Orientacao.VERTICAL)).split("\n");
        for (int segmento = 0; segmento <= 2; segmento++) {
            assertEquals('*', previa[2 * linha + segmento + 1].charAt(4 * coluna + 6));
        }
        assertTrue(String.join("\n", previa).contains("V: separa colunas " + (coluna + 1) + " e " + (coluna + 2)));
        assertTrue(String.join("\n", previa).contains("======="));
        assertEquals(1, estado.jogo().getBarreiras().size());
    }
}
