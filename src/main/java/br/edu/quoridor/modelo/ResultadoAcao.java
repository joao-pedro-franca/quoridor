package br.edu.quoridor.modelo;

/** Resultado de uma tentativa de ação, sem expor estado mutável. */
public record ResultadoAcao(boolean sucesso, String mensagem) {
}
