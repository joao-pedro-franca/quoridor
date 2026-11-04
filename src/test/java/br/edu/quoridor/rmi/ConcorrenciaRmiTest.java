package br.edu.quoridor.rmi;

import br.edu.quoridor.modelo.Orientacao;
import br.edu.quoridor.modelo.Posicao;
import br.edu.quoridor.modelo.Registro;
import br.edu.quoridor.modelo.ResultadoAcao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static br.edu.quoridor.rmi.ApoioRmi.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class ConcorrenciaRmiTest {
    @Test
    void registrosSimultaneosPreenchemExatamenteQuatroVagas() throws Exception {
        try (ServicoJogo servico = novoServico()) {
            var resultados = simultaneamente(12, servico::registrar);
            var aceitos = resultados.stream().filter(Registro::sucesso).toList();
            assertEquals(4, aceitos.size());
            assertEquals(4, aceitos.stream().map(Registro::jogadorId).distinct().count());
            assertEquals(4, aceitos.stream().map(Registro::token).distinct().count());
            assertEquals(java.util.Set.of(1, 2, 3, 4),
                    aceitos.stream().map(Registro::jogadorId).collect(java.util.stream.Collectors.toSet()));
            assertEquals(4, servico.consultarEstado().jogadoresRegistrados());
        }
    }

    @Test
    void movimentosSimultaneosNaoAplicamDuasAcoesNoMesmoTurno() throws Exception {
        try (ServicoJogo servico = novoServico()) {
            String token = registrarQuatro(servico).get(0).token();
            var resultados = simultaneamente(12, () -> servico.mover(token, 1, 4));
            assertEquals(1, resultados.stream().filter(ResultadoAcao::sucesso).count());
            var estado = servico.consultarEstado().jogo();
            assertEquals(new Posicao(1, 4), estado.getJogadores().get(0).getPosicao());
            assertEquals(2, estado.getJogadorDaVez());
        }
    }

    @Test
    void barreirasSimultaneasEConsultasNaoExibemAtualizacaoParcial() throws Exception {
        try (ServicoJogo servico = novoServico()) {
            String token = registrarQuatro(servico).get(0).token();
            var resultados = simultaneamente(12, () -> {
                ResultadoAcao resultado = servico.colocarBarreira(token, 3, 3, Orientacao.HORIZONTAL);
                for (int i = 0; i < 20; i++) {
                    var consulta = servico.consultarEstado();
                    var jogo = consulta.jogo();
                    int estoqueTotal = jogo.getJogadores().stream().mapToInt(j -> j.getBarreirasDisponiveis()).sum();
                    assertEquals(4, consulta.jogadoresRegistrados());
                    assertEquals(20, estoqueTotal + jogo.getBarreiras().size());
                    assertEquals(jogo.getBarreiras().isEmpty() ? 1 : 2, jogo.getJogadorDaVez());
                }
                return resultado;
            });
            assertEquals(1, resultados.stream().filter(ResultadoAcao::sucesso).count());
            var estado = servico.consultarEstado().jogo();
            assertEquals(1, estado.getBarreiras().size());
            assertEquals(4, estado.getJogadores().get(0).getBarreirasDisponiveis());
        }
    }
}
