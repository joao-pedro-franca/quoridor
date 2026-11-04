package br.edu.quoridor.cliente;

import br.edu.quoridor.modelo.*;

import java.util.Arrays;
import java.util.ArrayList;

/** Desenha um snapshot usando ASCII; não decide quais jogadas são permitidas. */
public final class TabuleiroCLI {
    private TabuleiroCLI() {
    }

    public static String desenhar(EstadoServico estado, int jogadorId) {
        return desenhar(estado, jogadorId, null);
    }

    /** Monta um snapshot temporário. A proposta não consome estoque nem altera o turno. */
    public static String previsualizar(EstadoServico estado, int jogadorId, Barreira proposta) {
        var jogo = estado.jogo();
        var barreiras = new ArrayList<>(jogo.getBarreiras());
        barreiras.add(proposta);
        var temporario = new EstadoJogo(jogo.getJogadores(), barreiras,
                jogo.getJogadorDaVez(), jogo.getVencedor(), jogo.getSituacao(), jogo.getJogadorAbandono());
        return desenhar(new EstadoServico(temporario, estado.jogadoresRegistrados(), estado.presencas()), jogadorId, proposta);
    }

    private static String desenhar(EstadoServico estado, int jogadorId, Barreira proposta) {
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
                    grade[2 * barreira.linha() + 1][4 * barreira.coluna() + segmento] = barreira.equals(proposta) ? '*' : '=';
                }
            } else {
                for (int segmento = 0; segmento <= 2; segmento++) {
                    grade[2 * barreira.linha() + segmento][4 * barreira.coluna() + 4] = barreira.equals(proposta) ? '*' : '|';
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
        if (estado.jogo().isPausada()) {
            texto.append("PARTIDA PAUSADA | Aguardando reconexão. Turno: ")
                    .append(estado.jogo().getJogadorDaVez()).append('\n');
        } else if (estado.jogo().getSituacao() == SituacaoPartida.ENCERRADA_POR_ABANDONO) {
            texto.append("PARTIDA ENCERRADA | Resultado: sem vencedor\n");
        } else {
        texto.append("Registrados: ").append(estado.jogadoresRegistrados()).append("/4 | Turno: jogador ")
                .append(estado.jogo().getJogadorDaVez()).append('\n');
        }
        texto.append("Barreiras:");
        for (Jogador jogador : estado.jogo().getJogadores()) {
            texto.append("  ").append(jogador.getId()).append('=').append(jogador.getBarreirasDisponiveis());
        }
        if (estado.jogo().isPausada()) {
            texto.append("\nFora: ");
            for (PresencaJogador presenca : estado.presencas()) {
                if (!presenca.conectado()) {
                    long segundos = presenca.segundosRestantes();
                    texto.append("Jogador ").append(presenca.jogadorId()).append(" (")
                            .append(String.format("%02d:%02d", segundos / 60, segundos % 60)).append(") ");
                }
            }
            return texto.toString();
        }
        if (estado.jogo().getSituacao() == SituacaoPartida.ENCERRADA_POR_ABANDONO) {
            texto.append("\nMotivo: jogador ").append(estado.jogo().getJogadorAbandono())
                    .append(" não reconectou dentro de 3 minutos.");
            return texto.toString();
        }
        if (proposta != null) {
            int linha = proposta.linha() + 1;
            int coluna = proposta.coluna() + 1;
            texto.append('\n').append(proposta.orientacao() == Orientacao.HORIZONTAL
                    ? "H: separa linhas " + linha + " e " + (linha + 1) + "; ocupa colunas " + coluna + " e " + (coluna + 1) + "."
                    : "V: separa colunas " + coluna + " e " + (coluna + 1) + "; ocupa linhas " + linha + " e " + (linha + 1) + ".");
            return texto.toString();
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
