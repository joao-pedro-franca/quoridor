package br.edu.quoridor.modelo;

import java.util.Objects;

/** Dados imutáveis de um jogador no início da partida. */
public final class Jogador {
    public static final int BARREIRAS_INICIAIS = 5;

    private final int id;
    private final Posicao posicao;
    private final Objetivo objetivo;
    private final int barreirasDisponiveis;

    public Jogador(int id, Posicao posicao, Objetivo objetivo) {
        if (id < 1 || id > 4) {
            throw new IllegalArgumentException("O identificador do jogador deve estar entre 1 e 4.");
        }
        this.id = id;
        this.posicao = Objects.requireNonNull(posicao, "A posição é obrigatória.");
        this.objetivo = Objects.requireNonNull(objetivo, "O objetivo é obrigatório.");
        this.barreirasDisponiveis = BARREIRAS_INICIAIS;
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
