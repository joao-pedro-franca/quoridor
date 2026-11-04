package br.edu.quoridor.jogo;

import br.edu.quoridor.modelo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static br.edu.quoridor.jogo.Cenarios.*;
import static org.junit.jupiter.api.Assertions.*;

class CaminhosTest {
    @Test
    void tabuleiroInicialTemCaminhoParaOsQuatroObjetivos() {
        for (Jogador jogador : new EstadoJogo().getJogadores()) {
            assertTrue(Regras.temCaminho(jogador, List.of()));
        }
    }

    @Test
    void bfsEncontraDesvioAteUltimaColuna() {
        List<Barreira> barreiras = List.of(h(4, 0), h(4, 2), h(4, 4), h(4, 6));
        for (Jogador jogador : new EstadoJogo().getJogadores()) {
            assertTrue(Regras.temCaminho(jogador, barreiras));
        }
    }

    @Test
    void bfsIgnoraPeoesMesmoQuandoNaoHaMovimentoLegalImediato() {
        Partida partida = partida(p(4, 0), p(4, 1), p(4, 2), p(4, 3),
                h(3, 0), h(3, 2), h(3, 4), h(3, 6),
                h(4, 0), h(4, 2), h(4, 4), h(4, 6));
        EstadoJogo estado = partida.getEstado();
        assertTrue(Regras.movimentosValidos(estado, 1).isEmpty());
        for (Jogador jogador : estado.getJogadores()) {
            assertTrue(Regras.temCaminho(jogador, estado.getBarreiras()));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "1,0,0,0,0,0,1",
            "2,0,8,0,7,0,6",
            "3,8,8,7,7,7,6",
            "4,8,0,7,0,7,1"
    })
    void rejeitaBarreiraQueCercaQualquerAdversario(int alvo, int linha, int coluna,
                                                int lh, int ch, int lv, int cv) {
        var jogadores = new ArrayList<>(new EstadoJogo().getJogadores());
        Jogador jogador = jogadores.get(alvo - 1);
        jogadores.set(alvo - 1, new Jogador(alvo, p(linha, coluna), jogador.getObjetivo()));
        Barreira existente = h(lh, ch);
        Barreira nova = v(lv, cv);
        int autor = alvo % 4 + 1;
        Partida partida = new Partida(new EstadoJogo(jogadores, List.of(existente), autor, null));
        EstadoJogo antes = partida.getEstado();
        assertFalse(Regras.barreirasConflitam(existente, nova));
        for (Jogador participante : jogadores) {
            assertTrue(Regras.temCaminho(participante, List.of(existente)));
            assertEquals(participante.getId() != alvo,
                    Regras.temCaminho(participante, List.of(existente, nova)));
        }
        assertFalse(partida.colocarBarreira(autor, nova).sucesso());
        assertSame(antes, partida.getEstado());
        assertEquals(5, partida.getEstado().getJogadores().get(autor - 1).getBarreirasDisponiveis());
    }

    @Test
    void tambemRejeitaBarreiraQueBloqueiaOProprioAutor() {
        Partida partida = partida(p(0, 0), p(4, 8), p(8, 4), p(4, 0), h(0, 0));
        EstadoJogo antes = partida.getEstado();
        assertFalse(partida.colocarBarreira(1, v(0, 1)).sucesso());
        assertSame(antes, partida.getEstado());
    }

    @Test
    void aceitaBarreiraQueRestringeMasNaoEliminaCaminho() {
        Partida partida = partida(p(0, 0), p(4, 8), p(8, 4), p(4, 0), h(0, 0));
        assertTrue(partida.colocarBarreira(1, v(1, 1)).sucesso());
        EstadoJogo depois = partida.getEstado();
        for (Jogador jogador : depois.getJogadores()) {
            assertTrue(Regras.temCaminho(jogador, depois.getBarreiras()));
        }
    }

    @Test
    void consultaDeBarreiraNaoAlteraListasDoEstado() {
        EstadoJogo estado = new EstadoJogo();
        assertTrue(Regras.podeColocarBarreira(estado, h(3, 3)));
        assertTrue(estado.getBarreiras().isEmpty());
        assertEquals(5, estado.getJogadores().get(0).getBarreirasDisponiveis());
    }
}
