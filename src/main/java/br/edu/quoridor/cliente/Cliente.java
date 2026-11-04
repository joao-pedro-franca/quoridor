package br.edu.quoridor.cliente;

import br.edu.quoridor.modelo.EstadoServico;
import br.edu.quoridor.modelo.Registro;
import br.edu.quoridor.modelo.ResultadoAcao;
import br.edu.quoridor.modelo.Barreira;
import br.edu.quoridor.rmi.JogoRemoto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Cliente de terminal: converte comandos e consulta o servidor, sem aplicar regras. */
public final class Cliente {
    private static final long INTERVALO_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final String AJUDA = """
            ajuda                       Mostra esta ajuda.
            tabuleiro                   Consulta e mostra o estado atual.
            mover LINHA COLUNA          Casas de 1 a 9.
            barreira H L,L+1 C,C+1      Pares consecutivos de 1 a 9.
            barreira V L,L+1 C,C+1      Pares consecutivos de 1 a 9.
            sessao                      Mostra somente o seu token.
            sair                        Encerra este cliente, preservando a vaga.

            Exemplo: mover 2 5
            Exemplo: barreira H 3,4 4,5
            H: separa as linhas do primeiro par; ocupa as colunas do segundo.
            V: separa as colunas do segundo par; ocupa as linhas do primeiro.
            A proposta aparece como *. Confirme com S ou cancele com N.
            A legalidade de toda jogada é decidida pelo servidor.
            """;

    private final JogoRemoto jogo;
    private final BufferedReader entrada;
    private final PrintWriter saida;
    private final TerminalCLI terminal;
    private Registro registro;
    private EstadoServico estado;
    private String desenho;
    private String mensagem = "";
    private ComandoCLI barreiraPendente;

    Cliente(JogoRemoto jogo, BufferedReader entrada, PrintWriter saida, boolean interativo) {
        this.jogo = jogo;
        this.entrada = entrada;
        this.saida = saida;
        terminal = new TerminalCLI(saida, interativo);
    }

    public static void main(String[] args) {
        try {
            if (args.length > 2) {
                throw new IllegalArgumentException("Uso: Cliente [HOST [PORTA]]");
            }
            String host = args.length >= 1 ? args[0] : "127.0.0.1";
            int porta = args.length == 2 ? Integer.parseInt(args[1]) : 1099;
            if (host.isBlank() || porta < 1 || porta > 65535) {
                throw new IllegalArgumentException("Informe um host e uma porta de 1 a 65535.");
            }
            // Limita a espera de respostas e do handshake no RMI do JDK, sem sockets próprios.
            System.setProperty("sun.rmi.transport.tcp.responseTimeout",
                    System.getProperty("sun.rmi.transport.tcp.responseTimeout", "5000"));
            System.setProperty("sun.rmi.transport.tcp.handshakeTimeout",
                    System.getProperty("sun.rmi.transport.tcp.handshakeTimeout", "5000"));
            JogoRemoto jogo = (JogoRemoto) LocateRegistry.getRegistry(host, porta).lookup("Quoridor");
            BufferedReader entrada = new BufferedReader(System.console() != null
                    ? System.console().reader() : new InputStreamReader(System.in));
            new Cliente(jogo, entrada, new PrintWriter(System.out, true), System.console() != null).executar();
        } catch (NumberFormatException invalido) {
            System.err.println("Porta inválida. Use um número de 1 a 65535.");
        } catch (IllegalArgumentException invalido) {
            System.err.println(invalido.getMessage());
        } catch (NotBoundException ausente) {
            System.err.println("Serviço Quoridor não encontrado. Inicie o servidor primeiro.");
        } catch (RemoteException indisponivel) {
            System.err.println("Não foi possível conectar ao servidor. Confira o host, a porta e se ele está ligado.");
        } catch (IOException falha) {
            System.err.println("Não foi possível ler o terminal: " + falha.getMessage());
        }
    }

    void executar() throws IOException {
        saida.print("Seu nome: ");
        saida.flush();
        String nome = entrada.readLine();
        if (nome == null) {
            return;
        }
        saida.print("Token para reconectar (Enter para novo jogador): ");
        saida.flush();
        String token = entrada.readLine();
        if (token == null || !iniciarSessao(nome, token)) {
            return;
        }
        executarComandos();
    }

    boolean iniciarSessao(String nome, String token) throws RemoteException {
        if (nome.isBlank() || nome.length() > 30 || nome.chars().anyMatch(Character::isISOControl)) {
            saida.println("Nome inválido. Use de 1 a 30 caracteres visíveis.");
            return false;
        }
        registro = token.isBlank() ? jogo.registrar() : jogo.reconectar(token.strip());
        if (!registro.sucesso()) {
            saida.println(registro.mensagem());
            return false;
        }
        saida.println("Bem-vindo, " + nome.strip() + "! Você é o jogador " + registro.jogadorId() + ".");
        mensagem = "Seu token: " + registro.token() + " (guarde para reconectar).";
        atualizar(true);
        return true;
    }

    /** Um único laço faz RPC e escreve na tela; somente a leitura de linhas fica em daemon. */
    private void executarComandos() {
        var linhas = new LinkedBlockingQueue<Entrada>();
        Thread leitor = new Thread(() -> lerLinhas(linhas), "entrada-quoridor");
        leitor.setDaemon(true); // A leitura nativa do console pode não responder a interrupt().
        leitor.start();
        long proximaConsulta = System.nanoTime() + INTERVALO_NANOS;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                long restante = proximaConsulta - System.nanoTime();
                if (restante <= 0) {
                    if (!estado.jogo().isFinalizada()) {
                        atualizar(false);
                    }
                    proximaConsulta = System.nanoTime() + INTERVALO_NANOS;
                    restante = INTERVALO_NANOS;
                }
                Entrada recebida = linhas.poll(restante, TimeUnit.NANOSECONDS);
                if (recebida == null) {
                    continue;
                }
                if (recebida.erro() != null) {
                    mostrarMensagem("Falha na leitura do terminal.");
                    break;
                }
                if (recebida.linha() == null) { // Fim da entrada redirecionada ou Ctrl+Z no Windows.
                    break;
                }
                terminal.novoPrompt();
                if (!executarComando(recebida.linha())) {
                    break;
                }
            }
        } catch (RemoteException desconectado) {
            mostrarMensagem("Conexão perdida. Reinicie o cliente e use seu token para reconectar.");
        } catch (InterruptedException interrompido) {
            Thread.currentThread().interrupt();
        } finally {
            leitor.interrupt();
            terminal.finalizar();
        }
    }

    private void lerLinhas(LinkedBlockingQueue<Entrada> linhas) {
        try {
            String linha;
            while (!Thread.currentThread().isInterrupted() && (linha = entrada.readLine()) != null) {
                linhas.add(new Entrada(linha, null));
            }
            linhas.add(new Entrada(null, null));
        } catch (IOException falha) {
            linhas.add(new Entrada(null, falha));
        }
    }

    boolean executarComando(String texto) throws RemoteException {
        if (barreiraPendente != null) {
            return confirmarBarreira(texto);
        }
        ComandoCLI comando;
        try {
            comando = ComandoCLI.interpretar(texto);
        } catch (IllegalArgumentException invalido) {
            mostrarMensagem(invalido.getMessage());
            return true;
        }
        switch (comando.tipo()) {
            case SAIR -> { return false; }
            case AJUDA -> terminal.mostrar(AJUDA, "Use tabuleiro para voltar ao jogo.", true);
            case SESSAO -> mostrarMensagem("Seu token: " + registro.token());
            case TABULEIRO -> atualizar(true);
            case MOVER -> mostrarResultado(jogo.mover(registro.token(), comando.linha(), comando.coluna()));
            case BARREIRA -> {
                barreiraPendente = comando;
                mensagem = "Proposta: * | Confirmar posicionamento? (S/N)";
                atualizar(true);
            }
        }
        return true;
    }

    private boolean confirmarBarreira(String texto) throws RemoteException {
        String resposta = texto.strip();
        if (resposta.equalsIgnoreCase("sair")) {
            barreiraPendente = null;
            return false;
        }
        if (resposta.equalsIgnoreCase("N")) {
            barreiraPendente = null;
            mensagem = "Posicionamento cancelado. Nenhuma jogada foi enviada.";
            atualizar(true);
        } else if (resposta.equalsIgnoreCase("S")) {
            ComandoCLI comando = barreiraPendente;
            barreiraPendente = null;
            mostrarResultado(jogo.colocarBarreira(registro.token(), comando.linha(), comando.coluna(), comando.orientacao()));
        } else {
            mostrarMensagem("Responda S ou N. Confirmar posicionamento? (S/N)");
        }
        return true;
    }

    private void mostrarResultado(ResultadoAcao resultado) throws RemoteException {
        mensagem = (resultado.sucesso() ? "OK: " : "Erro: ") + resultado.mensagem();
        atualizar(true);
    }

    void atualizar(boolean forcar) throws RemoteException {
        EstadoServico novo = jogo.consultarEstado();
        String novoDesenho = barreiraPendente == null ? TabuleiroCLI.desenhar(novo, registro.jogadorId())
                : TabuleiroCLI.previsualizar(novo, registro.jogadorId(), new Barreira(
                        barreiraPendente.linha(), barreiraPendente.coluna(), barreiraPendente.orientacao()));
        estado = novo;
        if (forcar || !novoDesenho.equals(desenho)) {
            desenho = novoDesenho;
            terminal.mostrar(desenho, mensagem, forcar);
        }
    }

    private void mostrarMensagem(String texto) {
        mensagem = texto;
        terminal.mostrar(desenho, mensagem, true);
    }

    private record Entrada(String linha, IOException erro) {
    }
}
