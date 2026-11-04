package br.edu.quoridor.cliente;

import br.edu.quoridor.modelo.*;
import br.edu.quoridor.rmi.JogoRemoto;
import br.edu.quoridor.rmi.ServicoJogo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ClienteIntegracaoTest {
    private Registry registro;
    private ServicoJogo servico;
    private JogoRemoto remoto;

    @BeforeEach
    void iniciar() throws Exception {
        registro = LocateRegistry.createRegistry(0);
        servico = new ServicoJogo(new Random() {
            @Override
            public int nextInt(int limite) { return 0; }
        });
        registro.bind("Quoridor", servico);
        remoto = (JogoRemoto) ((Registry) UnicastRemoteObject.toStub(registro)).lookup("Quoridor");
    }

    @AfterEach
    void encerrar() throws Exception {
        try {
            if (servico != null) { servico.close(); }
        } finally {
            if (registro != null) { UnicastRemoteObject.unexportObject(registro, true); }
        }
    }

    @Test
    void quatroClientesJogamUmaPartidaCompletaPelaMesmaReferenciaRemota() throws Exception {
        Cliente[] clientes = new Cliente[4];
        StringWriter[] saidas = new StringWriter[4];
        for (int i = 0; i < 4; i++) {
            saidas[i] = new StringWriter();
            clientes[i] = cliente(saidas[i], "");
            assertTrue(clientes[i].iniciarSessao("Aluno " + (i + 1), ""));
        }
        for (int rodada = 0; rodada <= 8; rodada++) {
            clientes[0].executarComando("mover " + (rodada + 1) + " 4");
            if (rodada == 8) { break; }
            clientes[1].executarComando("mover " + (rodada % 2 == 0 ? 4 : 5) + " 9");
            clientes[2].executarComando("mover 9 " + (rodada % 2 == 0 ? 6 : 5));
            clientes[3].executarComando("mover " + (rodada % 2 == 0 ? 4 : 5) + " 1");
        }
        assertEquals(Integer.valueOf(1), remoto.consultarEstado().jogo().getVencedor());
        for (int i = 0; i < 4; i++) {
            clientes[i].atualizar(false);
            assertTrue(saidas[i].toString().contains("Vencedor: jogador 1"));
            assertFalse(clientes[i].executarComando("sair"));
        }
    }

    @Test
    void entradasInvalidasEErrosDoServidorSaoMostradosSemAlterarPartida() throws Exception {
        StringWriter saida = new StringWriter();
        Cliente cliente = cliente(saida, "");
        assertTrue(cliente.iniciarSessao("Aluno", ""));
        cliente.executarComando("mover 2 5");
        assertTrue(saida.toString().contains("Aguardando o registro"));
        for (int i = 0; i < 3; i++) { remoto.registrar(); }
        cliente.executarComando("mover 10 2");
        cliente.executarComando("voar");
        cliente.executarComando("mover 3 5");
        assertTrue(saida.toString().contains("Coordenadas devem ser inteiros"));
        assertTrue(saida.toString().contains("Comando desconhecido"));
        assertTrue(saida.toString().contains("Movimento inválido"));
        assertEquals(new Posicao(0, 4), remoto.consultarEstado().jogo().getJogadores().get(0).getPosicao());
        cliente.executarComando("barreira H 4 4");
        assertEquals(new Barreira(3, 3, Orientacao.HORIZONTAL), remoto.consultarEstado().jogo().getBarreiras().get(0));
        cliente.executarComando("mover 2 5");
        assertTrue(saida.toString().contains("jogador da vez"));
    }

    @Test
    void quintoClienteRecebeRecusaEReconexaoMantemIdentidade() throws Exception {
        Registro primeiro = remoto.registrar();
        for (int i = 0; i < 3; i++) { remoto.registrar(); }
        StringWriter saida = new StringWriter();
        assertFalse(cliente(saida, "").iniciarSessao("Quinto", ""));
        assertTrue(saida.toString().contains("quatro jogadores"));
        Cliente reconectado = cliente(saida, "");
        assertTrue(reconectado.iniciarSessao("Retorno", primeiro.token()));
        reconectado.executarComando("mover 2 5");
        assertEquals(4, remoto.consultarEstado().jogadoresRegistrados());
        assertEquals(new Posicao(1, 4), remoto.consultarEstado().jogo().getJogadores().get(0).getPosicao());
    }

    @Test
    void tokenInvalidoENomeVazioNaoCriamRegistro() throws Exception {
        StringWriter saida = new StringWriter();
        Cliente cliente = cliente(saida, "");
        assertFalse(cliente.iniciarSessao(" ", ""));
        assertFalse(cliente.iniciarSessao("Aluno", "invalido"));
        assertTrue(saida.toString().contains("Nome inválido"));
        assertTrue(saida.toString().contains("Token de sessão inválido"));
        assertEquals(0, remoto.consultarEstado().jogadoresRegistrados());
    }

    @Test
    void atualizacaoSoRedesenhaQuandoConteudoMuda() throws Exception {
        StringWriter saida = new StringWriter();
        Cliente cliente = cliente(saida, "");
        assertTrue(cliente.iniciarSessao("Aluno", ""));
        int tamanho = saida.toString().length();
        cliente.atualizar(false);
        cliente.atualizar(false);
        assertEquals(tamanho, saida.toString().length());
        remoto.registrar();
        cliente.atualizar(false);
        assertTrue(saida.toString().contains("Registrados: 2/4"));
        assertTrue(saida.toString().length() > tamanho);
    }

    @Test
    void entradaRedirecionadaProcessaComandosESaiSemEsperarNovaLinha() throws Exception {
        StringWriter saida = new StringWriter();
        cliente(saida, "Aluno\n\najuda\ncomando errado\ntabuleiro\nsair\n").executar();
        assertTrue(saida.toString().contains("Bem-vindo, Aluno"));
        assertTrue(saida.toString().contains("H (L,C):"));
        assertTrue(saida.toString().contains("Comando desconhecido"));
        assertTrue(saida.toString().contains("Cliente encerrado"));
    }

    @Test
    void fimDaEntradaEncerraCliente() throws Exception {
        StringWriter saida = new StringWriter();
        cliente(saida, "Aluno\n\n").executar();
        assertTrue(saida.toString().contains("Cliente encerrado"));
    }

    @Test
    void sessaoExibeSomenteTokenDoProprioJogador() throws Exception {
        Registro primeiro = remoto.registrar();
        StringWriter saida = new StringWriter();
        Cliente cliente = cliente(saida, "");
        assertTrue(cliente.iniciarSessao("Aluno", ""));
        cliente.executarComando("sessao");
        assertFalse(saida.toString().contains(primeiro.token()));
        assertTrue(saida.toString().contains("Seu token:"));
    }

    @Test
    void servidorDesligadoPropagaFalhaDeComunicacao() throws Exception {
        Cliente cliente = cliente(new StringWriter(), "");
        assertTrue(cliente.iniciarSessao("Aluno", ""));
        servico.close();
        assertThrows(RemoteException.class, () -> cliente.atualizar(false));
    }

    private Cliente cliente(StringWriter saida, String entrada) {
        return new Cliente(remoto, new BufferedReader(new StringReader(entrada)), new PrintWriter(saida), false);
    }
}
