package br.edu.quoridor.modelo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.HashSet;

/** Retrato imutável de uma partida; listas e jogadores não podem ser alterados. */
public final class EstadoJogo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<Jogador> jogadores;
    private final List<Barreira> barreiras;
    private final int jogadorDaVez;
    private final Integer vencedor;
    private final SituacaoPartida situacao;
    private final Integer jogadorAbandono;

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
        this(jogadores, barreiras, jogadorDaVez, vencedor,
                vencedor == null ? SituacaoPartida.EM_ANDAMENTO : SituacaoPartida.ENCERRADA_POR_VITORIA, null);
    }

    public EstadoJogo(List<Jogador> jogadores, List<Barreira> barreiras, int jogadorDaVez,
                      Integer vencedor, SituacaoPartida situacao, Integer jogadorAbandono) {
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
        this.situacao = Objects.requireNonNull(situacao);
        if ((situacao == SituacaoPartida.ENCERRADA_POR_VITORIA) != (vencedor != null)
                || (situacao == SituacaoPartida.ENCERRADA_POR_ABANDONO) != (jogadorAbandono != null)
                || (jogadorAbandono != null && (jogadorAbandono < 1 || jogadorAbandono > 4))) {
            throw new IllegalArgumentException("Situação, vencedor e responsável pelo abandono inconsistentes.");
        }
        this.jogadorAbandono = jogadorAbandono;
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

    public SituacaoPartida getSituacao() { return situacao; }

    public Integer getJogadorAbandono() { return jogadorAbandono; }

    public boolean isPausada() { return situacao == SituacaoPartida.PAUSADA; }

    public boolean isFinalizada() {
        return situacao == SituacaoPartida.ENCERRADA_POR_VITORIA
                || situacao == SituacaoPartida.ENCERRADA_POR_ABANDONO;
    }
}
