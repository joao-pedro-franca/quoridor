package br.edu.quoridor.modelo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Consulta pública e imutável: reúne jogo e registros sem divulgar credenciais. */
public record EstadoServico(EstadoJogo jogo, int jogadoresRegistrados) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public EstadoServico {
        Objects.requireNonNull(jogo, "O estado do jogo é obrigatório.");
        if (jogadoresRegistrados < 0 || jogadoresRegistrados > 4) {
            throw new IllegalArgumentException("A quantidade de registros deve estar entre 0 e 4.");
        }
    }
}
