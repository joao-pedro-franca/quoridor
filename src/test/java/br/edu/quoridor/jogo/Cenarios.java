package br.edu.quoridor.jogo;

import br.edu.quoridor.modelo.*;

import java.util.List;

/** Monta retratos imutáveis de teste pela entrada interna de Partida, sem reflexão. */
final class Cenarios {
    private Cenarios() {
    }

    static Posicao p(int linha, int coluna) {
        return new Posicao(linha, coluna);
    }

    static Barreira h(int linha, int coluna) {
        return new Barreira(linha, coluna, Orientacao.HORIZONTAL);
    }

    static Barreira v(int linha, int coluna) {
        return new Barreira(linha, coluna, Orientacao.VERTICAL);
    }

    static Partida centro(Barreira... barreiras) {
        return partida(p(4, 4), p(0, 8), p(8, 0), p(0, 0), barreiras);
    }

    static Partida partida(Posicao p1, Posicao p2, Posicao p3, Posicao p4, Barreira... barreiras) {
        return new Partida(new EstadoJogo(List.of(
                new Jogador(1, p1, Objetivo.LINHA_8),
                new Jogador(2, p2, Objetivo.COLUNA_0),
                new Jogador(3, p3, Objetivo.LINHA_0),
                new Jogador(4, p4, Objetivo.COLUNA_8)
        ), List.of(barreiras), 1, null));
    }
}
