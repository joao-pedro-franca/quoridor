package br.edu.quoridor.cliente;

import br.edu.quoridor.modelo.Posicao;
import br.edu.quoridor.rmi.ServicoJogo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Exercita os mains em JVMs independentes, dirigidas por pipes em vez de pessoas. */
@Timeout(45)
class ClientesProcessosTest {
    private Registry registro;
    private ServicoJogo servico;
    private int porta;
    private final List<ProcessoCliente> processos = new ArrayList<>();

    @BeforeEach
    void publicarEmPortaLivreSemUsar1099() throws Exception {
        for (int tentativa = 0; tentativa < 20; tentativa++) {
            porta = ThreadLocalRandom.current().nextInt(20000, 60000);
            try {
                registro = LocateRegistry.createRegistry(porta);
                break;
            } catch (ExportException ocupada) {
                // A tentativa de bind do próprio RMI evita sondagem com sockets.
            }
        }
        assertNotNull(registro, "Não foi possível obter uma porta para o registro de teste.");
        servico = new ServicoJogo(new Random() {
            @Override
            public int nextInt(int limite) { return 0; }
        });
        registro.bind("Quoridor", servico);
    }

    @AfterEach
    void encerrarProcessosERecursosRmi() throws Exception {
        try {
            for (ProcessoCliente processo : processos) { processo.close(); }
        } finally {
            try {
                if (servico != null) { servico.close(); }
            } finally {
                if (registro != null) { UnicastRemoteObject.unexportObject(registro, true); }
            }
        }
    }

    @Test
    void quatroJvmsRecebemAtualizacoesSemDigitarEQuintaEhRecusada() throws Exception {
        for (int i = 0; i < 4; i++) {
            ProcessoCliente processo = novoCliente("Aluno " + (i + 1), "");
            processo.aguardar("Seu token:");
        }
        assertEquals(4, servico.consultarEstado().jogadoresRegistrados());
        // Nenhuma linha nova precisa ser digitada para os clientes perceberem o quarto registro.
        for (ProcessoCliente processo : processos) { processo.aguardar("Registrados: 4/4"); }
        processos.get(0).enviar("mover 2 5");
        processos.get(0).aguardar("OK:");
        assertEquals(new Posicao(1, 4), servico.consultarEstado().jogo().getJogadores().get(0).getPosicao());
        for (int i = 1; i < 4; i++) { processos.get(i).aguardar("Turno: jogador 2"); }
        processos.get(1).enviar("barreira V 4 4");
        processos.get(1).aguardar("OK:");
        assertEquals(1, servico.consultarEstado().jogo().getBarreiras().size());
        ProcessoCliente quinto = novoCliente("Quinto", "");
        quinto.aguardar("quatro jogadores");
        assertTrue(quinto.processo.waitFor(10, TimeUnit.SECONDS));
        assertEquals(4, servico.consultarEstado().jogadoresRegistrados());
        for (int i = 0; i < 4; i++) {
            processos.get(i).enviar("sair");
            assertTrue(processos.get(i).processo.waitFor(10, TimeUnit.SECONDS));
            assertEquals(0, processos.get(i).processo.exitValue());
        }
    }

    @Test
    void perdaDeConexaoEncerraMesmoComLeituraDeTerminalPendente() throws Exception {
        ProcessoCliente processo = novoCliente("Aluno", "");
        processo.aguardar("Seu token:");
        servico.close();
        processo.aguardar("Conex");
        assertTrue(processo.processo.waitFor(10, TimeUnit.SECONDS));
        assertEquals(0, processo.processo.exitValue());
    }

    @Test
    void novaJvmReconectaUsandoTokenSemConsumirOutraVaga() throws Exception {
        var primeiro = servico.registrar();
        ProcessoCliente processo = novoCliente("Retorno", primeiro.token());
        processo.aguardar("jogador 1");
        processo.aguardar("Seu token:");
        assertEquals(1, servico.consultarEstado().jogadoresRegistrados());
        processo.enviar("sair");
        assertTrue(processo.processo.waitFor(10, TimeUnit.SECONDS));
    }

    private ProcessoCliente novoCliente(String nome, String token) throws IOException {
        String executavel = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executavel);
        Process processo = new ProcessBuilder(java.toString(), "-cp", Path.of("target/classes").toAbsolutePath().toString(),
                Cliente.class.getName(), "127.0.0.1", Integer.toString(porta)).redirectErrorStream(true).start();
        ProcessoCliente cliente = new ProcessoCliente(processo);
        processos.add(cliente);
        cliente.enviar(nome);
        cliente.enviar(token);
        return cliente;
    }

    private static final class ProcessoCliente implements AutoCloseable {
        private final Process processo;
        private final PrintWriter entrada;
        private final LinkedBlockingQueue<String> linhas = new LinkedBlockingQueue<>();
        private String observadas = "";

        private ProcessoCliente(Process processo) {
            this.processo = processo;
            entrada = new PrintWriter(new OutputStreamWriter(processo.getOutputStream()), true);
            Thread leitor = new Thread(() -> {
                try (var saida = new BufferedReader(new InputStreamReader(processo.getInputStream()))) {
                    String linha;
                    while ((linha = saida.readLine()) != null) { linhas.add(linha); }
                } catch (IOException encerrado) {
                    // Pipes são fechados ao destruir um processo na limpeza.
                }
            }, "saida-cliente-teste");
            leitor.setDaemon(true);
            leitor.start();
        }

        private void enviar(String comando) {
            entrada.println(comando);
        }

        private void aguardar(String trecho) throws InterruptedException {
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!observadas.contains(trecho)) {
                long restante = limite - System.nanoTime();
                assertTrue(restante > 0, "O processo não exibiu a informação esperada: " + trecho);
                String linha = linhas.poll(restante, TimeUnit.NANOSECONDS);
                assertNotNull(linha, "O processo não exibiu a informação esperada: " + trecho);
                observadas += linha + "\n";
            }
        }

        @Override
        public void close() throws InterruptedException {
            entrada.close(); // EOF também deve encerrar o laço do cliente.
            if (!processo.waitFor(5, TimeUnit.SECONDS)) {
                processo.destroyForcibly();
                assertTrue(processo.waitFor(5, TimeUnit.SECONDS));
            }
        }
    }
}
