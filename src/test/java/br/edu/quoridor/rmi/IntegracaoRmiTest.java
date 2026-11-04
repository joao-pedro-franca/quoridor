package br.edu.quoridor.rmi;

import br.edu.quoridor.modelo.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static br.edu.quoridor.rmi.ApoioRmi.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class IntegracaoRmiTest {
    private Registry registro;
    private ServicoJogo servico;
    private JogoRemoto remoto;

    @BeforeEach
    void publicarEmRegistroRealComPortaTemporaria() throws Exception {
        // Porta 0: o RMI escolhe uma porta livre, sem disputar a 1099 de demonstração.
        registro = LocateRegistry.createRegistry(0);
        servico = novoServico();
        registro.bind("Quoridor", servico);
        Registry registroRemoto = (Registry) UnicastRemoteObject.toStub(registro);
        remoto = (JogoRemoto) registroRemoto.lookup("Quoridor");
        assertNotSame(servico, remoto);
    }

    @AfterEach
    void limparRecursos() throws Exception {
        try {
            if (registro != null) {
                // Libera também a publicação; o registro inteiro pertence a este teste.
                UnicastRemoteObject.unexportObject(registro, true);
            }
        } finally {
            if (servico != null) {
                servico.close();
            }
        }
    }

    @Test
    void lookupRegistraMoveERecebeCopiasSerializadas() throws Exception {
        var registros = registrarQuatro(remoto);
        EstadoServico antes = remoto.consultarEstado();
        assertEquals(4, antes.jogadoresRegistrados());
        assertNotSame(servico.consultarEstado().jogo(), antes.jogo());
        assertNotSame(servico.consultarEstado().jogo().getJogadores().get(0), antes.jogo().getJogadores().get(0));
        assertTrue(remoto.mover(registros.get(0).token(), 1, 4).sucesso());
        assertTrue(remoto.colocarBarreira(registros.get(1).token(), 3, 3, Orientacao.VERTICAL).sucesso());
        EstadoServico depois = remoto.consultarEstado();
        assertEquals(new Posicao(1, 4), depois.jogo().getJogadores().get(0).getPosicao());
        assertEquals(new Barreira(3, 3, Orientacao.VERTICAL), depois.jogo().getBarreiras().get(0));
        assertEquals(4, depois.jogo().getJogadores().get(1).getBarreirasDisponiveis());
        assertEquals(Objetivo.COLUNA_0, depois.jogo().getJogadores().get(1).getObjetivo());
        assertEquals(3, depois.jogo().getJogadorDaVez());
        assertNull(depois.jogo().getVencedor());
        assertEquals(new Posicao(0, 4), antes.jogo().getJogadores().get(0).getPosicao());
        assertTrue(antes.jogo().getBarreiras().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> depois.jogo().getJogadores().clear());
        assertThrows(UnsupportedOperationException.class, () -> depois.jogo().getBarreiras().clear());
        assertEquals(4, servico.consultarEstado().jogo().getJogadores().size());
    }

    @Test
    void recusasEReconexaoTambemAtravessamRmi() throws Exception {
        Registro primeiro = remoto.registrar();
        assertFalse(remoto.mover(primeiro.token(), 1, 4).sucesso());
        assertFalse(remoto.reconectar("invalido").sucesso());
        for (int i = 0; i < 3; i++) {
            assertTrue(remoto.registrar().sucesso());
        }
        assertFalse(remoto.registrar().sucesso());
        assertFalse(remoto.mover("invalido", 1, 4).sucesso());
        Registro reconexao = remoto.reconectar(primeiro.token());
        assertEquals(primeiro.jogadorId(), reconexao.jogadorId());
        assertEquals(primeiro.token(), reconexao.token());
        assertTrue(remoto.mover(reconexao.token(), 1, 4).sucesso());
        assertEquals(4, remoto.consultarEstado().jogadoresRegistrados());
    }

    @Test
    void chamadasRemotasConcorrentesAplicamUmaUnicaBarreira() throws Exception {
        String token = registrarQuatro(remoto).get(0).token();
        var resultados = simultaneamente(8,
                () -> remoto.colocarBarreira(token, 3, 3, Orientacao.HORIZONTAL));
        assertEquals(1, resultados.stream().filter(ResultadoAcao::sucesso).count());
        EstadoServico estado = remoto.consultarEstado();
        assertEquals(1, estado.jogo().getBarreiras().size());
        assertEquals(4, estado.jogo().getJogadores().get(0).getBarreirasDisponiveis());
        assertEquals(2, estado.jogo().getJogadorDaVez());
    }

    @Test
    void removerExportacaoImpedeNovasChamadasRemotas() {
        servico.close();
        assertThrows(RemoteException.class, () -> remoto.consultarEstado());
    }
}
