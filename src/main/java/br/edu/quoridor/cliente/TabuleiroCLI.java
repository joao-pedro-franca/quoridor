package br.edu.quoridor.cliente;

import br.edu.quoridor.modelo.*;

import java.util.Arrays;

/** Desenha um snapshot usando ASCII; não decide quais jogadas são permitidas. */
public final class TabuleiroCLI {
    private TabuleiroCLI() {
    }

    public static String desenhar(EstadoServico estado, int jogadorId) {
        char[][] grade = new char[17][37];
        for (char[] linha : grade) {
            Arrays.fill(linha, ' ');
        }
        for (int linha = 0; linha < 9; linha++) {
            for (int coluna = 0; coluna < 9; coluna++) {
                grade[2 * linha][2 + 4 * coluna] = '.';
            }
        }
        for (Barreira barreira : estado.jogo().getBarreiras()) {
            if (barreira.orientacao() == Orientacao.HORIZONTAL) {
                for (int segmento = 1; segmento <= 7; segmento++) {
                    grade[2 * barreira.linha() + 1][4 * barreira.coluna() + segmento] = '=';
                }
            } else {
                for (int segmento = 0; segmento <= 2; segmento++) {
                    grade[2 * barreira.linha() + segmento][4 * barreira.coluna() + 4] = '|';
                }
            }
        }
        for (Jogador jogador : estado.jogo().getJogadores()) {
            Posicao posicao = jogador.getPosicao();
            grade[2 * posicao.linha()][2 + 4 * posicao.coluna()] = (char) ('0' + jogador.getId());
        }
        StringBuilder texto = new StringBuilder("    1   2   3   4   5   6   7   8   9\n");
        for (int linha = 0; linha < grade.length; linha++) {
            texto.append(linha % 2 == 0 ? (linha / 2 + 1) + " " : "  ");
            texto.append(grade[linha]).append('\n');
        }
        texto.append("Registrados: ").append(estado.jogadoresRegistrados()).append("/4 | Turno: jogador ")
                .append(estado.jogo().getJogadorDaVez()).append('\n');
        texto.append("Barreiras:");
        for (Jogador jogador : estado.jogo().getJogadores()) {
            texto.append("  ").append(jogador.getId()).append('=').append(jogador.getBarreirasDisponiveis());
        }
        texto.append("\nVocê é o jogador ").append(jogadorId).append(". ");
        if (estado.jogo().isFinalizada()) {
            texto.append("Vencedor: jogador ").append(estado.jogo().getVencedor()).append('.');
        } else if (estado.jogadoresRegistrados() < 4) {
            texto.append("Aguardando os demais jogadores.");
        } else if (estado.jogo().getJogadorDaVez() == jogadorId) {
            texto.append("É a sua vez!");
        } else {
            texto.append("Não é a sua vez.");
        }
        return texto.toString();
    }
}
