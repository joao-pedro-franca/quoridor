package br.edu.quoridor.jogo;

import br.edu.quoridor.modelo.EstadoJogo;

/** Mantém o estado inicial; as ações do jogo serão implementadas em outra etapa. */
public final class Partida {
    private final EstadoJogo estado;

    public Partida() {
        estado = new EstadoJogo();
    }

    public EstadoJogo getEstado() {
        return estado;
    }
}
