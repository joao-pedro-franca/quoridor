package br.edu.quoridor.servidor;

import br.edu.quoridor.rmi.ServicoJogo;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CountDownLatch;

/** Inicialização e encerramento do único servidor de demonstração em localhost. */
public final class Servidor implements AutoCloseable {
    private static final int PORTA = 1099;
    private static final String NOME = "Quoridor";

    private final Registry registro;
    private final boolean registroCriado;
    private ServicoJogo servico;
    private boolean publicado;
    private boolean encerrado;

    private Servidor() throws RemoteException, AlreadyBoundException {
        Registry encontrado;
        boolean criado;
        try {
            encontrado = LocateRegistry.createRegistry(PORTA);
            criado = true;
        } catch (ExportException portaOcupada) {
            encontrado = LocateRegistry.getRegistry("127.0.0.1", PORTA);
            try {
                encontrado.list(); // getRegistry sozinho não verifica se existe um registro real.
            } catch (RemoteException indisponivel) {
                throw new RemoteException("Porta 1099 ocupada ou registro RMI local indisponível.", indisponivel);
            }
            criado = false;
        }
        registro = encontrado;
        registroCriado = criado;
        try {
            servico = new ServicoJogo();
            registro.bind(NOME, servico); // Não substitui outro servidor já publicado.
            publicado = true;
        } catch (RemoteException | AlreadyBoundException falha) {
            close();
            throw falha;
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        try (Servidor servidor = new Servidor()) {
            Runtime.getRuntime().addShutdownHook(new Thread(servidor::close, "encerrar-quoridor"));
            System.out.println("Servidor Quoridor RMI funcionando em rmi://127.0.0.1:1099/Quoridor");
            System.out.println("Aguardando quatro jogadores. Para encerrar, pressione Ctrl+C.");
            new CountDownLatch(1).await();
        } catch (AlreadyBoundException ocupado) {
            System.err.println("Não foi possível publicar: já existe um serviço chamado Quoridor na porta 1099.");
        } catch (RemoteException falha) {
            System.err.println("Não foi possível iniciar o servidor RMI: " + falha.getMessage());
        } catch (InterruptedException interrompido) {
            Thread.currentThread().interrupt();
            System.err.println("Servidor interrompido e encerrado.");
        }
    }

    @Override
    public synchronized void close() {
        if (encerrado) {
            return;
        }
        encerrado = true;
        if (publicado) {
            try {
                registro.unbind(NOME);
            } catch (RemoteException | NotBoundException falha) {
                System.err.println("Não foi possível remover a publicação: " + falha.getMessage());
            }
        }
        if (servico != null) {
            servico.close();
        }
        if (registroCriado) {
            try {
                UnicastRemoteObject.unexportObject(registro, true);
            } catch (RemoteException jaEncerrado) {
                // O registro criado por este processo já foi liberado.
            }
        }
    }
}
