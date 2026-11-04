package br.edu.quoridor.modelo;

import java.io.Serial;
import java.io.Serializable;

/** Credencial individual. Em caso de recusa, jogadorId é 0 e token é null. */
public record Registro(boolean sucesso, int jogadorId, String token, String mensagem)
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
