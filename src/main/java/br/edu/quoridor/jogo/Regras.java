package br.edu.quoridor.jogo;

import br.edu.quoridor.modelo.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Regras puras: consultam os dados, sem modificar o estado da partida. */
public final class Regras {
    private static final int[][] DIRECOES = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private Regras() {
    }

    public static Set<Posicao> movimentosValidos(EstadoJogo estado, int jogadorId) {
        if (estado.isFinalizada() || jogadorId < 1 || jogadorId > 4) {
            return Set.of();
        }
        Posicao origem = estado.getJogadores().get(jogadorId - 1).getPosicao();
        Set<Posicao> destinos = new HashSet<>();
        for (int[] direcao : DIRECOES) {
            Posicao vizinha = deslocar(origem, direcao[0], direcao[1]);
            if (vizinha == null || passagemBloqueada(estado.getBarreiras(), origem, vizinha)) {
                continue;
            }
            if (!ocupada(estado, vizinha)) {
                destinos.add(vizinha);
            } else {
                adicionarSaltos(estado, vizinha, direcao[0], direcao[1], destinos);
            }
        }
        return Set.copyOf(destinos);
    }

    private static void adicionarSaltos(EstadoJogo estado, Posicao adversario,
                                        int dl, int dc, Set<Posicao> destinos) {
        Posicao atras = deslocar(adversario, dl, dc);
        if (destinoLivre(estado, adversario, atras)) {
            destinos.add(atras);
            return; // Salto direto disponível: não permite desvio diagonal por esse peão.
        }
        // O salto direto está impedido: examina apenas as duas casas laterais.
        // Nunca continua saltando sobre um segundo peão.
        Posicao lateralA = deslocar(adversario, dc, dl);
        Posicao lateralB = deslocar(adversario, -dc, -dl);
        if (destinoLivre(estado, adversario, lateralA)) {
            destinos.add(lateralA);
        }
        if (destinoLivre(estado, adversario, lateralB)) {
            destinos.add(lateralB);
        }
    }

    private static boolean destinoLivre(EstadoJogo estado, Posicao origem, Posicao destino) {
        return destino != null && !ocupada(estado, destino)
                && !passagemBloqueada(estado.getBarreiras(), origem, destino);
    }

    private static boolean ocupada(EstadoJogo estado, Posicao posicao) {
        return estado.getJogadores().stream().anyMatch(j -> j.getPosicao().equals(posicao));
    }

    /** Aresta entre duas casas ortogonalmente adjacentes. Compartilhada por movimentos e BFS. */
    static boolean passagemBloqueada(List<Barreira> barreiras, Posicao a, Posicao b) {
        if (Math.abs(a.linha() - b.linha()) + Math.abs(a.coluna() - b.coluna()) != 1) {
            throw new IllegalArgumentException("A passagem deve ligar duas casas vizinhas.");
        }
        for (Barreira barreira : barreiras) {
            if (a.linha() != b.linha() && barreira.orientacao() == Orientacao.HORIZONTAL
                    && barreira.linha() == Math.min(a.linha(), b.linha())
                    && a.coluna() >= barreira.coluna() && a.coluna() <= barreira.coluna() + 1) {
                return true;
            }
            if (a.coluna() != b.coluna() && barreira.orientacao() == Orientacao.VERTICAL
                    && barreira.coluna() == Math.min(a.coluna(), b.coluna())
                    && a.linha() >= barreira.linha() && a.linha() <= barreira.linha() + 1) {
                return true;
            }
        }
        return false;
    }

    public static boolean barreirasConflitam(Barreira a, Barreira b) {
        if (a.orientacao() != b.orientacao()) {
            return a.linha() == b.linha() && a.coluna() == b.coluna();
        }
        if (a.orientacao() == Orientacao.HORIZONTAL) {
            return a.linha() == b.linha() && Math.abs(a.coluna() - b.coluna()) <= 1;
        }
        return a.coluna() == b.coluna() && Math.abs(a.linha() - b.linha()) <= 1;
    }

    /** Simula a nova barreira em uma lista local antes de verificar os quatro caminhos. */
    public static boolean podeColocarBarreira(EstadoJogo estado, Barreira nova) {
        if (nova == null || estado.getBarreiras().stream().anyMatch(b -> barreirasConflitam(b, nova))) {
            return false;
        }
        List<Barreira> simuladas = new ArrayList<>(estado.getBarreiras());
        simuladas.add(nova);
        return estado.getJogadores().stream().allMatch(j -> temCaminho(j, simuladas));
    }

    /** BFS ignora peões e visita somente casas conectadas por passagens sem barreiras. */
    public static boolean temCaminho(Jogador jogador, List<Barreira> barreiras) {
        var fila = new ArrayDeque<Posicao>();
        boolean[][] visitadas = new boolean[9][9];
        Posicao inicio = jogador.getPosicao();
        fila.add(inicio);
        visitadas[inicio.linha()][inicio.coluna()] = true;
        while (!fila.isEmpty()) {
            Posicao atual = fila.remove();
            if (alcancouObjetivo(jogador.getObjetivo(), atual)) {
                return true;
            }
            for (int[] direcao : DIRECOES) {
                Posicao vizinha = deslocar(atual, direcao[0], direcao[1]);
                if (vizinha != null && !visitadas[vizinha.linha()][vizinha.coluna()]
                        && !passagemBloqueada(barreiras, atual, vizinha)) {
                    visitadas[vizinha.linha()][vizinha.coluna()] = true;
                    fila.add(vizinha);
                }
            }
        }
        return false;
    }

    public static boolean alcancouObjetivo(Objetivo objetivo, Posicao posicao) {
        return switch (objetivo) {
            case LINHA_8 -> posicao.linha() == 8;
            case COLUNA_0 -> posicao.coluna() == 0;
            case LINHA_0 -> posicao.linha() == 0;
            case COLUNA_8 -> posicao.coluna() == 8;
        };
    }

    static boolean dentroTabuleiro(int linha, int coluna) {
        return linha >= 0 && linha < Posicao.TAMANHO_TABULEIRO
                && coluna >= 0 && coluna < Posicao.TAMANHO_TABULEIRO;
    }

    private static Posicao deslocar(Posicao origem, int dl, int dc) {
        int linha = origem.linha() + dl;
        int coluna = origem.coluna() + dc;
        return dentroTabuleiro(linha, coluna) ? new Posicao(linha, coluna) : null;
    }
}
