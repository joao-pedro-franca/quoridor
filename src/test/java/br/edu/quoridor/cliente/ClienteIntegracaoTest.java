package br.edu.quoridor.cliente;

import br.edu.quoridor.modelo.*;
import br.edu.quoridor.rmi.JogoRemoto;
import br.edu.quoridor.rmi.ServicoJogo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
    private int colocacoes;

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
        cliente.executarComando("barreira H 4,5 4,5");
        cliente.executarComando("S");
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
        assertTrue(saida.toString().contains("H: separa as linhas"));
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
        JogoRemoto contando = new JogoRemoto() {
            public ResultadoAcao heartbeat(String token) throws RemoteException { return remoto.heartbeat(token); }
            public ResultadoAcao desconectar(String token) throws RemoteException { return remoto.desconectar(token); }
            public Registro registrar() throws RemoteException { return remoto.registrar(); }
            public Registro reconectar(String token) throws RemoteException { return remoto.reconectar(token); }
            public EstadoServico consultarEstado() throws RemoteException { return remoto.consultarEstado(); }
            public ResultadoAcao mover(String token, int linha, int coluna) throws RemoteException {
                return remoto.mover(token, linha, coluna);
            }
            public ResultadoAcao colocarBarreira(String token, int linha, int coluna, Orientacao orientacao)
                    throws RemoteException {
                colocacoes++;
                return remoto.colocarBarreira(token, linha, coluna, orientacao);
            }
        };
        return new Cliente(contando, new BufferedReader(new StringReader(entrada)), new PrintWriter(saida), false);
    }

    @ParameterizedTest
    @EnumSource(Orientacao.class)
    void previaNaoEnviaJogadaEAConfirmacaoEnviaUmaVez(Orientacao orientacao) throws Exception {
        StringWriter saida = new StringWriter();
        Cliente cliente = cliente(saida, "");
        assertTrue(cliente.iniciarSessao("Aluno", ""));
        for (int i = 0; i < 3; i++) { remoto.registrar(); }
        EstadoJogo antes = servico.consultarEstado().jogo();
        String letra = orientacao == Orientacao.HORIZONTAL ? "h" : "v";
        cliente.executarComando("barreira " + letra + " 3,4 4,5");
        assertEquals(0, colocacoes);
        assertSame(antes, servico.consultarEstado().jogo());
        assertTrue(saida.toString().contains("Confirmar posicionamento? (S/N)"));
        assertTrue(saida.toString().contains("*"));
        cliente.executarComando("s");
        assertEquals(1, colocacoes);
        assertEquals(new Barreira(2, 3, orientacao), remoto.consultarEstado().jogo().getBarreiras().get(0));
        assertEquals(4, remoto.consultarEstado().jogo().getJogadores().get(0).getBarreirasDisponiveis());
        assertEquals(2, remoto.consultarEstado().jogo().getJogadorDaVez());
        assertTrue(saida.toString().contains("OK: Barreira colocada"));
    }

    @Test
    void cancelarNaoEnviaJogadaNemConsomeEstoqueOuTurno() throws Exception {
        Cliente cliente = cliente(new StringWriter(), "");
        assertTrue(cliente.iniciarSessao("Aluno", ""));
        for (int i = 0; i < 3; i++) { remoto.registrar(); }
        EstadoJogo antes = servico.consultarEstado().jogo();
        cliente.executarComando("barreira h 3,4 4,5");
        cliente.executarComando("n");
        assertEquals(0, colocacoes);
        assertSame(antes, servico.consultarEstado().jogo());
        cliente.executarComando("mover 2 5");
        assertEquals(new Posicao(1, 4), remoto.consultarEstado().jogo().getJogadores().get(0).getPosicao());
    }

    @Test
    void respostaInvalidaMantemPropostaEnquantoAtualizacoesContinuam() throws Exception {
        StringWriter saida = new StringWriter();
        Cliente cliente = cliente(saida, "");
        assertTrue(cliente.iniciarSessao("Aluno", ""));
        cliente.executarComando("barreira h 3,4 4,5");
        cliente.executarComando("talvez");
        remoto.registrar();
        cliente.atualizar(false);
        assertEquals(0, colocacoes);
        String atual = saida.toString().substring(saida.toString().lastIndexOf("Quoridor RMI"));
        assertTrue(atual.contains("Registrados: 2/4"));
        assertTrue(atual.contains("*******"));
        assertTrue(atual.contains("Confirmar posicionamento? (S/N)"));
        cliente.executarComando("N");
    }

    @Test
    void confirmacaoDePropostaIlegalExibeRecusaDoServidor() throws Exception {
        StringWriter saida = new StringWriter();
        Cliente cliente = cliente(saida, "");
        assertTrue(cliente.iniciarSessao("Aluno", ""));
        for (int i = 0; i < 3; i++) { remoto.registrar(); }
        remoto.colocarBarreira(remoto.reconectar(extrairToken(saida.toString())).token(), 2, 3, Orientacao.HORIZONTAL);
        EstadoJogo antes = servico.consultarEstado().jogo();
        // O jogador 1 já gastou seu turno; a confirmação ainda deve ser julgada pelo servidor.
        cliente.executarComando("barreira v 3,4 4,5");
        cliente.executarComando("S");
        assertEquals(1, colocacoes);
        assertSame(antes, servico.consultarEstado().jogo());
        assertTrue(saida.toString().contains("Erro:"));
    }

    private String extrairToken(String texto) {
        return texto.substring(texto.indexOf("Seu token: ") + "Seu token: ".length()).split(" ")[0];
    }

    @Test
    void eofComPropostaPendenteCancelaSemEnviarAcao() throws Exception {
        StringWriter saida = new StringWriter();
        cliente(saida, "Aluno\n\nbarreira h 3,4 4,5\n").executar();
        assertEquals(0, colocacoes);
        assertTrue(remoto.consultarEstado().jogo().getBarreiras().isEmpty());
        assertTrue(saida.toString().contains("Cliente encerrado"));
    }
}
