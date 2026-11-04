package br.edu.quoridor.modelo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Barreira imutável com âncora entre 0 e 7 em cada eixo.
 * A horizontal separa as linhas l e l+1 nas colunas c e c+1.
 * A vertical separa as colunas c e c+1 nas linhas l e l+1.
 * Nesta etapa, somente as coordenadas e a orientação são validadas.
 */
public record Barreira(int linha, int coluna, Orientacao orientacao) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public Barreira {
        if (linha < 0 || linha >= Posicao.TAMANHO_TABULEIRO - 1
                || coluna < 0 || coluna >= Posicao.TAMANHO_TABULEIRO - 1) {
            throw new IllegalArgumentException("A âncora deve estar entre 0 e 7 em cada eixo.");
        }
        Objects.requireNonNull(orientacao, "A orientação é obrigatória.");
    }
}
