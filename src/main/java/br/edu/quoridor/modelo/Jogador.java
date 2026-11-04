package br.edu.quoridor.modelo;

import java.util.Objects;

/** Dados imutáveis de um jogador em um instante da partida. */
public final class Jogador {
    public static final int BARREIRAS_INICIAIS = 5;

    private final int id;
    private final Posicao posicao;
    private final Objetivo objetivo;
    private final int barreirasDisponiveis;

    public Jogador(int id, Posicao posicao, Objetivo objetivo) {
        this(id, posicao, objetivo, BARREIRAS_INICIAIS);
    }

    public Jogador(int id, Posicao posicao, Objetivo objetivo, int barreirasDisponiveis) {
        if (id < 1 || id > 4) {
            throw new IllegalArgumentException("O identificador do jogador deve estar entre 1 e 4.");
        }
        if (barreirasDisponiveis < 0 || barreirasDisponiveis > BARREIRAS_INICIAIS) {
            throw new IllegalArgumentException("O estoque deve estar entre 0 e 5.");
        }
        this.id = id;
        this.posicao = Objects.requireNonNull(posicao, "A posição é obrigatória.");
        this.objetivo = Objects.requireNonNull(objetivo, "O objetivo é obrigatório.");
        this.barreirasDisponiveis = barreirasDisponiveis;
    }

    public int getId() {
        return id;
    }

    public Posicao getPosicao() {
        return posicao;
    }

    public Objetivo getObjetivo() {
        return objetivo;
    }

    public int getBarreirasDisponiveis() {
        return barreirasDisponiveis;
    }
}
