package br.edu.quoridor.modelo;

/** Coordenadas imutáveis de uma casa do tabuleiro 9x9. */
public record Posicao(int linha, int coluna) {
    public static final int TAMANHO_TABULEIRO = 9;

    public Posicao {
        if (linha < 0 || linha >= TAMANHO_TABULEIRO
                || coluna < 0 || coluna >= TAMANHO_TABULEIRO) {
            throw new IllegalArgumentException("Linha e coluna devem estar entre 0 e 8.");
        }
    }
}
