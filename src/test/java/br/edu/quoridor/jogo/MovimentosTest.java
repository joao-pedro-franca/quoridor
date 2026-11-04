package br.edu.quoridor.jogo;

import br.edu.quoridor.modelo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static br.edu.quoridor.jogo.Cenarios.*;
import static org.junit.jupiter.api.Assertions.*;

class MovimentosTest {
    @ParameterizedTest
    @CsvSource({"3,4", "5,4", "4,3", "4,5"})
    void moveUmaCasaOrtogonal(int linha, int coluna) {
        Partida partida = centro();
        assertTrue(partida.mover(1, linha, coluna).sucesso());
        assertEquals(p(linha, coluna), partida.getEstado().getJogadores().get(0).getPosicao());
        assertEquals(2, partida.getEstado().getJogadorDaVez());
        assertEquals(5, partida.getEstado().getJogadores().get(0).getBarreirasDisponiveis());
    }

    @ParameterizedTest
    @CsvSource({"4,4", "2,4", "6,4", "4,2", "4,6", "3,3", "3,5", "5,3", "5,5"})
    void rejeitaMovimentoSemPeaoQueJustifiqueSaltoOuDiagonal(int linha, int coluna) {
        Partida partida = centro();
        EstadoJogo antes = partida.getEstado();
        assertFalse(partida.mover(1, linha, coluna).sucesso());
        assertSame(antes, partida.getEstado());
    }

    @ParameterizedTest
    @CsvSource({"-1,4", "9,4", "4,-1", "4,9", "-2147483648,0", "2147483647,0"})
    void rejeitaCoordenadasForaDoTabuleiroSemAlterarEstado(int linha, int coluna) {
        Partida partida = centro();
        EstadoJogo antes = partida.getEstado();
        assertFalse(partida.mover(1, linha, coluna).sucesso());
        assertSame(antes, partida.getEstado());
    }

    @Test
    void movimentosGeradosNosCantosFicamDentroDoTabuleiro() {
        Partida partida = partida(p(0, 0), p(0, 8), p(8, 0), p(8, 8));
        assertEquals(java.util.Set.of(p(1, 0), p(0, 1)),
                Regras.movimentosValidos(partida.getEstado(), 1));
    }

    @Test
    void barreirasBloqueiamMovimentoNasQuatroDirecoes() {
        assertFalse(centro(h(3, 4)).mover(1, 3, 4).sucesso());
        assertFalse(centro(h(4, 4)).mover(1, 5, 4).sucesso());
        assertFalse(centro(v(4, 3)).mover(1, 4, 3).sucesso());
        assertFalse(centro(v(4, 4)).mover(1, 4, 5).sucesso());
    }

    @ParameterizedTest
    @CsvSource({"3,4,2,4", "5,4,6,4", "4,3,4,2", "4,5,4,6"})
    void saltaUmAdversarioNasQuatroDirecoes(int la, int ca, int ld, int cd) {
        Partida partida = partida(p(4, 4), p(la, ca), p(8, 0), p(0, 0));
        assertTrue(partida.mover(1, ld, cd).sucesso());
        assertEquals(p(la, ca), partida.getEstado().getJogadores().get(1).getPosicao());
    }

    @Test
    void naoOcupaCasaDeAdversario() {
        Partida partida = partida(p(4, 4), p(3, 4), p(8, 0), p(0, 0));
        assertFalse(partida.mover(1, 3, 4).sucesso());
    }

    @Test
    void naoSaltaNemDesviaSeHaBarreiraAntesDoAdversario() {
        Partida partida = partida(p(4, 4), p(3, 4), p(8, 0), p(0, 0), h(3, 4));
        assertFalse(partida.mover(1, 2, 4).sucesso());
        assertFalse(partida.mover(1, 3, 3).sucesso());
        assertFalse(partida.mover(1, 3, 5).sucesso());
    }

    @Test
    void saltoDiretoDisponivelProibeDiagonais() {
        Partida partida = partida(p(4, 4), p(3, 4), p(8, 0), p(0, 0));
        assertFalse(partida.mover(1, 3, 3).sucesso());
        assertFalse(partida.mover(1, 3, 5).sucesso());
        assertTrue(partida.mover(1, 2, 4).sucesso());
    }

    @ParameterizedTest
    @CsvSource({"3,3", "3,5"})
    void barreiraAtrasDoAdversarioPermiteDiagonais(int linha, int coluna) {
        Partida partida = partida(p(4, 4), p(3, 4), p(8, 0), p(0, 0), h(2, 4));
        assertFalse(partida.mover(1, 2, 4).sucesso());
        assertTrue(partida.mover(1, linha, coluna).sucesso());
    }

    @ParameterizedTest
    @CsvSource({"0,3", "0,5"})
    void bordaAtrasDoAdversarioPermiteDiagonais(int linha, int coluna) {
        Partida partida = partida(p(1, 4), p(0, 4), p(8, 0), p(0, 0));
        assertTrue(partida.mover(1, linha, coluna).sucesso());
    }

    @ParameterizedTest
    @CsvSource({"3,3", "3,5"})
    void segundoPeaoImpedeSaltoDiretoEPermiteDiagonal(int linha, int coluna) {
        Partida partida = partida(p(4, 4), p(3, 4), p(2, 4), p(0, 0));
        EstadoJogo antes = partida.getEstado();
        assertFalse(partida.mover(1, 2, 4).sucesso());
        assertFalse(partida.mover(1, 1, 4).sucesso());
        assertSame(antes, partida.getEstado());
        assertTrue(partida.mover(1, linha, coluna).sucesso());
    }

    @Test
    void diagonalNaoAtravessaBarreiraLateral() {
        Partida partida = partida(p(4, 4), p(3, 4), p(8, 0), p(0, 0), h(2, 4), v(2, 3));
        assertFalse(partida.mover(1, 3, 3).sucesso());
        assertTrue(partida.mover(1, 3, 5).sucesso());
    }

    @Test
    void diagonalNaoOcupaNemSaltaOutroPeao() {
        Partida partida = partida(p(4, 4), p(3, 4), p(2, 4), p(3, 3));
        assertFalse(partida.mover(1, 3, 3).sucesso());
        assertFalse(partida.mover(1, 3, 2).sucesso());
        assertTrue(partida.mover(1, 3, 5).sucesso());
    }

    @Test
    void variosAdversariosAdjacentesSaoAvaliadosSeparadamente() {
        Partida partida = partida(p(4, 4), p(3, 4), p(4, 5), p(5, 4));
        assertEquals(java.util.Set.of(p(2, 4), p(4, 6), p(6, 4), p(4, 3)),
                Regras.movimentosValidos(partida.getEstado(), 1));
    }

    @Test
    void diagonaisNaDirecaoHorizontalRespeitamBarreiras() {
        Partida partida = partida(p(4, 4), p(4, 5), p(8, 0), p(0, 0), v(4, 5), h(3, 5));
        assertFalse(partida.mover(1, 4, 6).sucesso());
        assertFalse(partida.mover(1, 3, 5).sucesso());
        assertTrue(partida.mover(1, 5, 5).sucesso());
    }

    @Test
    void rejeitaDestinoNuloESetDeMovimentosNaoPodeSerAlterado() {
        Partida partida = centro();
        assertFalse(partida.mover(1, (Posicao) null).sucesso());
        assertThrows(UnsupportedOperationException.class,
                () -> Regras.movimentosValidos(partida.getEstado(), 1).clear());
        assertTrue(Regras.movimentosValidos(partida.getEstado(), 0).isEmpty());
    }
}
