package br.edu.quoridor.modelo;

import java.io.Serial;
import java.io.Serializable;

/** Prazo calculado pelo servidor. Não contém token nem instante do relógio monotônico. */
public record PresencaJogador(int jogadorId, boolean conectado, long segundosRestantes)
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
