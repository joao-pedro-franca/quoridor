package br.edu.quoridor.rmi;

import br.edu.quoridor.modelo.Registro;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

final class ApoioRmi {
    private ApoioRmi() {
    }

    static ServicoJogo novoServico() throws RemoteException {
        return new ServicoJogo(new Random() {
            @Override
            public int nextInt(int limite) {
                return 0; // Turno inicial do jogador 1; tokens continuam imprevisíveis.
            }
        });
    }

    static List<Registro> registrarQuatro(JogoRemoto jogo) throws RemoteException {
        List<Registro> registros = new ArrayList<>();
        for (int id = 1; id <= 4; id++) {
            Registro registro = jogo.registrar();
            assertTrue(registro.sucesso());
            assertEquals(id, registro.jogadorId());
            registros.add(registro);
        }
        return registros;
    }

    static <T> List<T> simultaneamente(int quantidade, Callable<T> acao) throws Exception {
        CountDownLatch prontos = new CountDownLatch(quantidade);
        CountDownLatch iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(quantidade)) {
            List<Future<T>> pendentes = new ArrayList<>();
            try {
                for (int i = 0; i < quantidade; i++) {
                    pendentes.add(executor.submit(() -> {
                        prontos.countDown();
                        if (!iniciar.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Tempo de início concorrente excedido.");
                        }
                        return acao.call();
                    }));
                }
                assertTrue(prontos.await(10, TimeUnit.SECONDS));
            } finally {
                iniciar.countDown();
            }
            List<T> resultados = new ArrayList<>();
            for (Future<T> pendente : pendentes) {
                resultados.add(pendente.get(10, TimeUnit.SECONDS));
            }
            return resultados;
        }
    }
}
