package br.edu.quoridor.jogo;

import br.edu.quoridor.modelo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static br.edu.quoridor.jogo.Cenarios.*;
import static org.junit.jupiter.api.Assertions.*;

class BarreirasTest {
    @ParameterizedTest
    @EnumSource(Orientacao.class)
    void colocaBarreiraEConsomeApenasEstoqueDoAutor(Orientacao orientacao) {
        Partida partida = centro();
        EstadoJogo antes = partida.getEstado();
        Barreira barreira = new Barreira(3, 3, orientacao);
        assertTrue(partida.colocarBarreira(1, barreira).sucesso());
        EstadoJogo depois = partida.getEstado();
        assertEquals(List.of(barreira), depois.getBarreiras());
        assertEquals(4, depois.getJogadores().get(0).getBarreirasDisponiveis());
        for (int i = 1; i < 4; i++) {
            assertEquals(5, depois.getJogadores().get(i).getBarreirasDisponiveis());
        }
        assertEquals(2, depois.getJogadorDaVez());
        assertTrue(antes.getBarreiras().isEmpty());
        assertEquals(5, antes.getJogadores().get(0).getBarreirasDisponiveis());
    }

    @ParameterizedTest
    @CsvSource({"0,0", "0,7", "7,0", "7,7"})
    void aceitaAncorasNosQuatroExtremos(int linha, int coluna) {
        assertTrue(centro().colocarBarreira(1, linha, coluna, Orientacao.HORIZONTAL).sucesso());
        assertTrue(centro().colocarBarreira(1, linha, coluna, Orientacao.VERTICAL).sucesso());
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "8,0", "0,-1", "0,8", "2147483647,0", "0,-2147483648"})
    void rejeitaAncoraInvalidaSemConsumirEstoqueOuTurno(int linha, int coluna) {
        Partida partida = centro();
        EstadoJogo antes = partida.getEstado();
        assertFalse(partida.colocarBarreira(1, linha, coluna, Orientacao.HORIZONTAL).sucesso());
        assertSame(antes, partida.getEstado());
    }

    @Test
    void rejeitaBarreiraOuOrientacaoNula() {
        Partida partida = centro();
        EstadoJogo antes = partida.getEstado();
        assertFalse(partida.colocarBarreira(1, null).sucesso());
        assertFalse(partida.colocarBarreira(1, 0, 0, null).sucesso());
        assertSame(antes, partida.getEstado());
    }

    @ParameterizedTest
    @CsvSource({"3,2", "3,3", "3,4"})
    void rejeitaSobreposicaoHorizontalTotalOuParcial(int linha, int coluna) {
        Partida partida = centro(h(3, 3));
        EstadoJogo antes = partida.getEstado();
        assertFalse(partida.colocarBarreira(1, h(linha, coluna)).sucesso());
        assertSame(antes, partida.getEstado());
    }

    @ParameterizedTest
    @CsvSource({"2,3", "3,3", "4,3"})
    void rejeitaSobreposicaoVerticalTotalOuParcial(int linha, int coluna) {
        Partida partida = centro(v(3, 3));
        EstadoJogo antes = partida.getEstado();
        assertFalse(partida.colocarBarreira(1, v(linha, coluna)).sucesso());
        assertSame(antes, partida.getEstado());
    }

    @Test
    void rejeitaCruzamentosNasDuasOrdens() {
        assertFalse(centro(h(3, 3)).colocarBarreira(1, v(3, 3)).sucesso());
        assertFalse(centro(v(3, 3)).colocarBarreira(1, h(3, 3)).sucesso());
    }

    @Test
    void permiteExtremidadesEncostadasEEncontrosEmT() {
        assertTrue(centro(h(3, 3)).colocarBarreira(1, h(3, 5)).sucesso());
        assertTrue(centro(h(3, 3)).colocarBarreira(1, h(3, 1)).sucesso());
        assertTrue(centro(v(3, 3)).colocarBarreira(1, v(5, 3)).sucesso());
        assertTrue(centro(v(3, 3)).colocarBarreira(1, v(1, 3)).sucesso());
        assertTrue(centro(h(3, 3)).colocarBarreira(1, v(2, 4)).sucesso());
    }

    @ParameterizedTest
    @EnumSource(Orientacao.class)
    void bloqueiaExatamenteDoisSegmentosNosDoisSentidos(Orientacao orientacao) {
        List<Barreira> barreiras = List.of(new Barreira(3, 3, orientacao));
        for (int linha = 0; linha < 9; linha++) {
            for (int coluna = 0; coluna < 9; coluna++) {
                Posicao a = p(linha, coluna);
                if (linha < 8) {
                    boolean esperado = orientacao == Orientacao.HORIZONTAL && linha == 3
                            && (coluna == 3 || coluna == 4);
                    assertEquals(esperado, Regras.passagemBloqueada(barreiras, a, p(linha + 1, coluna)));
                    assertEquals(esperado, Regras.passagemBloqueada(barreiras, p(linha + 1, coluna), a));
                }
                if (coluna < 8) {
                    boolean esperado = orientacao == Orientacao.VERTICAL && coluna == 3
                            && (linha == 3 || linha == 4);
                    assertEquals(esperado, Regras.passagemBloqueada(barreiras, a, p(linha, coluna + 1)));
                    assertEquals(esperado, Regras.passagemBloqueada(barreiras, p(linha, coluna + 1), a));
                }
            }
        }
    }

    @Test
    void estoqueZeroImpedeColocacaoMasPermiteMovimento() {
        EstadoJogo base = centro().getEstado();
        var jogadores = new ArrayList<>(base.getJogadores());
        jogadores.set(0, new Jogador(1, p(4, 4), Objetivo.LINHA_8, 0));
        Partida partida = new Partida(new EstadoJogo(jogadores, List.of(), 1, null));
        EstadoJogo antes = partida.getEstado();
        assertFalse(partida.colocarBarreira(1, h(3, 3)).sucesso());
        assertSame(antes, partida.getEstado());
        assertTrue(partida.mover(1, 5, 4).sucesso());
        assertEquals(0, partida.getEstado().getJogadores().get(0).getBarreirasDisponiveis());
    }

    @Test
    void consomeCincoBarreirasEmTurnosReaisERejeitaASexta() {
        Partida partida = new Partida(new EstadoJogo());
        Barreira[] barreiras = {h(2, 0), h(2, 2), h(2, 4), h(2, 6), h(4, 0)};
        for (int i = 0; i < barreiras.length; i++) {
            assertTrue(partida.colocarBarreira(1, barreiras[i]).sucesso());
            assertEquals(4 - i, partida.getEstado().getJogadores().get(0).getBarreirasDisponiveis());
            assertTrue(partida.mover(2, 4, i % 2 == 0 ? 7 : 8).sucesso());
            assertTrue(partida.mover(3, i % 2 == 0 ? 7 : 8, 4).sucesso());
            assertTrue(partida.mover(4, 4, i % 2 == 0 ? 1 : 0).sucesso());
        }
        EstadoJogo antes = partida.getEstado();
        assertFalse(partida.colocarBarreira(1, h(6, 0)).sucesso());
        assertSame(antes, partida.getEstado());
    }
}
