package br.edu.quoridor.jogo;

import br.edu.quoridor.modelo.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Aplica uma ação por turno, publicando um novo estado somente após a validação. */
public final class Partida {
    private EstadoJogo estado;

    public Partida() {
        this(RandomGenerator.getDefault());
    }

    /** Permite controlar o sorteio sem alterar o comportamento das regras. */
    public Partida(RandomGenerator aleatorio) {
        Objects.requireNonNull(aleatorio, "A origem de aleatoriedade é obrigatória.");
        EstadoJogo inicial = new EstadoJogo();
        estado = new EstadoJogo(inicial.getJogadores(), inicial.getBarreiras(),
                aleatorio.nextInt(4) + 1, null);
    }

    /** Entrada interna para cenários controlados, sem setters nem acesso mutável ao estado. */
    Partida(EstadoJogo estado) {
        this.estado = Objects.requireNonNull(estado);
    }

    public EstadoJogo getEstado() {
        return estado;
    }

    public ResultadoAcao mover(int jogadorId, int linha, int coluna) {
        if (!Regras.dentroTabuleiro(linha, coluna)) {
            return rejeitar("Destino fora do tabuleiro.");
        }
        return mover(jogadorId, new Posicao(linha, coluna));
    }

    public ResultadoAcao mover(int jogadorId, Posicao destino) {
        ResultadoAcao impedimento = validarTurno(jogadorId);
        if (impedimento != null) {
            return impedimento;
        }
        if (destino == null || !Regras.movimentosValidos(estado, jogadorId).contains(destino)) {
            return rejeitar("Movimento inválido.");
        }
        Jogador atual = estado.getJogadores().get(jogadorId - 1);
        var jogadores = new ArrayList<>(estado.getJogadores());
        jogadores.set(jogadorId - 1,
                new Jogador(jogadorId, destino, atual.getObjetivo(), atual.getBarreirasDisponiveis()));
        boolean venceu = Regras.alcancouObjetivo(atual.getObjetivo(), destino);
        estado = new EstadoJogo(jogadores, estado.getBarreiras(),
                venceu ? jogadorId : proximoJogador(), venceu ? jogadorId : null);
        return new ResultadoAcao(true, venceu ? "Jogador " + jogadorId + " venceu!" : "Peão movido.");
    }

    public ResultadoAcao colocarBarreira(int jogadorId, int linha, int coluna, Orientacao orientacao) {
        if (linha < 0 || linha > 7 || coluna < 0 || coluna > 7 || orientacao == null) {
            return rejeitar("Âncora ou orientação inválida.");
        }
        return colocarBarreira(jogadorId, new Barreira(linha, coluna, orientacao));
    }

    public ResultadoAcao colocarBarreira(int jogadorId, Barreira barreira) {
        ResultadoAcao impedimento = validarTurno(jogadorId);
        if (impedimento != null) {
            return impedimento;
        }
        Jogador atual = estado.getJogadores().get(jogadorId - 1);
        if (atual.getBarreirasDisponiveis() == 0) {
            return rejeitar("O jogador não possui barreiras disponíveis.");
        }
        if (!Regras.podeColocarBarreira(estado, barreira)) {
            return rejeitar("Barreira sobreposta, cruzada ou que elimina um caminho.");
        }
        var jogadores = new ArrayList<>(estado.getJogadores());
        jogadores.set(jogadorId - 1, new Jogador(jogadorId, atual.getPosicao(),
                atual.getObjetivo(), atual.getBarreirasDisponiveis() - 1));
        var barreiras = new ArrayList<>(estado.getBarreiras());
        barreiras.add(barreira);
        estado = new EstadoJogo(jogadores, barreiras, proximoJogador(), null);
        return new ResultadoAcao(true, "Barreira colocada.");
    }

    private ResultadoAcao validarTurno(int jogadorId) {
        if (estado.isFinalizada()) {
            return rejeitar("A partida já terminou.");
        }
        if (estado.isPausada()) {
            return rejeitar("Partida pausada. Aguardando reconexão.");
        }
        if (jogadorId != estado.getJogadorDaVez()) {
            return rejeitar("A ação deve ser realizada pelo jogador da vez.");
        }
        return null;
    }

    public void pausar() {
        if (!estado.isFinalizada() && !estado.isPausada()) {
            alterarSituacao(SituacaoPartida.PAUSADA, null);
        }
    }

    public void retomar() {
        if (estado.isPausada()) {
            alterarSituacao(SituacaoPartida.EM_ANDAMENTO, null);
        }
    }

    public void encerrarPorAbandono(int jogadorId) {
        if (!estado.isFinalizada()) {
            alterarSituacao(SituacaoPartida.ENCERRADA_POR_ABANDONO, jogadorId);
        }
    }

    private void alterarSituacao(SituacaoPartida situacao, Integer jogadorAbandono) {
        estado = new EstadoJogo(estado.getJogadores(), estado.getBarreiras(),
                estado.getJogadorDaVez(), null, situacao, jogadorAbandono);
    }

    private int proximoJogador() {
        return estado.getJogadorDaVez() % 4 + 1;
    }

    private ResultadoAcao rejeitar(String mensagem) {
        return new ResultadoAcao(false, mensagem);
    }
}
