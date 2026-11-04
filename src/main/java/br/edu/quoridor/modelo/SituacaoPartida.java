package br.edu.quoridor.modelo;

/** Abandono é um término do sistema distribuído, sem atribuir vencedor. */
public enum SituacaoPartida {
    EM_ANDAMENTO,
    PAUSADA,
    ENCERRADA_POR_VITORIA,
    ENCERRADA_POR_ABANDONO
}
