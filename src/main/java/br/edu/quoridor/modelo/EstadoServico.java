package br.edu.quoridor.modelo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.List;

/** Consulta pública e imutável: reúne jogo e registros sem divulgar credenciais. */
public record EstadoServico(EstadoJogo jogo, int jogadoresRegistrados,
                            List<PresencaJogador> presencas) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public EstadoServico(EstadoJogo jogo, int jogadoresRegistrados) {
        this(jogo, jogadoresRegistrados, List.of());
    }

    public EstadoServico {
        presencas = List.copyOf(presencas);
        Objects.requireNonNull(jogo, "O estado do jogo é obrigatório.");
        if (jogadoresRegistrados < 0 || jogadoresRegistrados > 4) {
            throw new IllegalArgumentException("A quantidade de registros deve estar entre 0 e 4.");
        }
    }
}
