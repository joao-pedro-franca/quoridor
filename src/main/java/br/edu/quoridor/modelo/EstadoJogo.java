package br.edu.quoridor.modelo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.HashSet;

/** Retrato imutável de uma partida; listas e jogadores não podem ser alterados. */
public final class EstadoJogo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<Jogador> jogadores;
    private final List<Barreira> barreiras;
    private final int jogadorDaVez;
    private final Integer vencedor;

    public EstadoJogo() {
        this(List.of(
                new Jogador(1, new Posicao(0, 4), Objetivo.LINHA_8),
                new Jogador(2, new Posicao(4, 8), Objetivo.COLUNA_0),
                new Jogador(3, new Posicao(8, 4), Objetivo.LINHA_0),
                new Jogador(4, new Posicao(4, 0), Objetivo.COLUNA_8)
        ), List.of(), 1, null);
    }

    /** Copia os dados recebidos. A validação das jogadas pertence ao motor de regras. */
    public EstadoJogo(List<Jogador> jogadores, List<Barreira> barreiras,
                      int jogadorDaVez, Integer vencedor) {
        this.jogadores = List.copyOf(jogadores);
        this.barreiras = List.copyOf(barreiras);
        if (this.jogadores.size() != 4) {
            throw new IllegalArgumentException("A partida deve ter quatro jogadores.");
        }
        var posicoes = new HashSet<Posicao>();
        for (int indice = 0; indice < 4; indice++) {
            Jogador jogador = this.jogadores.get(indice);
            if (jogador.getId() != indice + 1 || !posicoes.add(jogador.getPosicao())) {
                throw new IllegalArgumentException("Jogadores devem ter IDs de 1 a 4, em ordem, e casas distintas.");
            }
        }
        if (jogadorDaVez < 1 || jogadorDaVez > 4
                || (vencedor != null && (vencedor < 1 || vencedor > 4))) {
            throw new IllegalArgumentException("Jogador da vez e vencedor devem identificar um jogador válido.");
        }
        this.jogadorDaVez = jogadorDaVez;
        this.vencedor = vencedor;
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

    /** Retorna null enquanto não houver vencedor. */
    public Integer getVencedor() {
        return vencedor;
    }

    public boolean isFinalizada() {
        return vencedor != null;
    }
}
