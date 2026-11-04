package br.edu.quoridor.modelo;

import java.io.Serial;
import java.io.Serializable;

/** Resultado de uma tentativa de ação, sem expor estado mutável. */
public record ResultadoAcao(boolean sucesso, String mensagem) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
