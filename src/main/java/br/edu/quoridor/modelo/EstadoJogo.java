package br.edu.quoridor.modelo;

import java.util.List;

/** Estado inicial imutável de uma partida de quatro jogadores. */
public final class EstadoJogo {
    private final List<Jogador> jogadores;
    private final List<Barreira> barreiras;
    private final int jogadorDaVez;

    public EstadoJogo() {
        jogadores = List.of(
                new Jogador(1, new Posicao(0, 4), Objetivo.LINHA_8),
                new Jogador(2, new Posicao(4, 8), Objetivo.COLUNA_0),
                new Jogador(3, new Posicao(8, 4), Objetivo.LINHA_0),
                new Jogador(4, new Posicao(4, 0), Objetivo.COLUNA_8)
        );
        barreiras = List.of();
        jogadorDaVez = 1;
    }

    public List<Jogador> getJogadores() {
        return jogadores;
    }

    public List<Barreira> getBarreiras() {
        return barreiras;
    }

    public int getJogadorDaVez() {
        return jogadorDaVez;
    }
}
