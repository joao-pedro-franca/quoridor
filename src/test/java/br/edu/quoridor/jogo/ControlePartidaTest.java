package br.edu.quoridor.jogo;

import br.edu.quoridor.modelo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Random;

import static br.edu.quoridor.jogo.Cenarios.*;
import static org.junit.jupiter.api.Assertions.*;

class ControlePartidaTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void sorteioPodeSelecionarQualquerJogador(int sorteado) {
        Random aleatorio = new Random() {
            @Override
            public int nextInt(int limite) {
                assertEquals(4, limite);
                return sorteado;
            }
        };
        Partida partida = new Partida(aleatorio);
        assertEquals(sorteado + 1, partida.getEstado().getJogadorDaVez());
        assertFalse(partida.getEstado().isFinalizada());
        assertNull(partida.getEstado().getVencedor());
    }

    @Test
    void mesmaSementeReproduzSorteio() {
        assertEquals(new Partida(new Random(42)).getEstado().getJogadorDaVez(),
                new Partida(new Random(42)).getEstado().getJogadorDaVez());
    }

    @Test
    void alternaCircularmenteOsQuatroTurnos() {
        Partida partida = new Partida(new EstadoJogo());
        assertTrue(partida.mover(1, 1, 4).sucesso());
        assertEquals(2, partida.getEstado().getJogadorDaVez());
        assertTrue(partida.colocarBarreira(2, h(3, 3)).sucesso());
        assertEquals(3, partida.getEstado().getJogadorDaVez());
        assertTrue(partida.mover(3, 7, 4).sucesso());
        assertEquals(4, partida.getEstado().getJogadorDaVez());
        assertTrue(partida.colocarBarreira(4, v(5, 5)).sucesso());
        assertEquals(1, partida.getEstado().getJogadorDaVez());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 2, 3, 4, 5})
    void rejeitaAcoesDeQuemNaoEstaNoTurno(int jogador) {
        Partida partida = new Partida(new EstadoJogo());
        EstadoJogo antes = partida.getEstado();
        assertFalse(partida.mover(jogador, 1, 4).sucesso());
        assertFalse(partida.colocarBarreira(jogador, h(3, 3)).sucesso());
        assertSame(antes, partida.getEstado());
    }

    @Test
    void acaoInvalidaPermiteNovaTentativaDoMesmoJogador() {
        Partida partida = new Partida(new EstadoJogo());
        EstadoJogo antes = partida.getEstado();
        assertFalse(partida.mover(1, 5, 4).sucesso());
        assertSame(antes, partida.getEstado());
        assertTrue(partida.mover(1, 1, 4).sucesso());
    }

    @ParameterizedTest
    @CsvSource({"1,7,2,8,2", "2,2,1,2,0", "3,1,2,0,2", "4,2,7,2,8"})
    void detectaVitoriaNosQuatroObjetivosEEncerraPartida(int id, int li, int ci, int lf, int cf) {
        var jogadores = new ArrayList<>(new EstadoJogo().getJogadores());
        Jogador vencedor = jogadores.get(id - 1);
        jogadores.set(id - 1, new Jogador(id, p(li, ci), vencedor.getObjetivo()));
        Partida partida = new Partida(new EstadoJogo(jogadores, java.util.List.of(), id, null));
        assertTrue(partida.mover(id, lf, cf).sucesso());
        EstadoJogo finalizado = partida.getEstado();
        assertTrue(finalizado.isFinalizada());
        assertEquals(Integer.valueOf(id), finalizado.getVencedor());
        assertEquals(id, finalizado.getJogadorDaVez());
        assertEquals(p(lf, cf), finalizado.getJogadores().get(id - 1).getPosicao());
        for (int jogador = 1; jogador <= 4; jogador++) {
            assertFalse(partida.mover(jogador, li, ci).sucesso());
            assertFalse(partida.colocarBarreira(jogador, h(3, 3)).sucesso());
            assertTrue(Regras.movimentosValidos(finalizado, jogador).isEmpty());
            assertSame(finalizado, partida.getEstado());
        }
    }

    @Test
    void saltoTambemPodeAlcancarObjetivoEVencer() {
        Partida partida = partida(p(6, 2), p(7, 2), p(8, 4), p(4, 0));
        assertTrue(partida.mover(1, 8, 2).sucesso());
        assertEquals(Integer.valueOf(1), partida.getEstado().getVencedor());
    }

    @Test
    void diagonalTambemPodeAlcancarObjetivoEVencer() {
        Partida partida = partida(p(7, 2), p(8, 2), p(8, 4), p(4, 0));
        assertTrue(partida.mover(1, 8, 1).sucesso());
        assertEquals(Integer.valueOf(1), partida.getEstado().getVencedor());
    }

    @Test
    void retratoAnteriorPermaneceImutavelDepoisDeMovimento() {
        Partida partida = new Partida(new EstadoJogo());
        EstadoJogo antes = partida.getEstado();
        assertTrue(partida.mover(1, 1, 4).sucesso());
        assertEquals(p(0, 4), antes.getJogadores().get(0).getPosicao());
        assertEquals(1, antes.getJogadorDaVez());
        assertNull(antes.getVencedor());
        assertNotSame(antes, partida.getEstado());
    }
}
