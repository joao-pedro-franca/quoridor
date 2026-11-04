package br.edu.quoridor.rmi;

import br.edu.quoridor.modelo.EstadoServico;
import br.edu.quoridor.modelo.Orientacao;
import br.edu.quoridor.modelo.Registro;
import br.edu.quoridor.modelo.ResultadoAcao;

import java.rmi.Remote;
import java.rmi.RemoteException;

/** Contrato dos clientes. Somente o servidor escolhe IDs e altera a partida. */
public interface JogoRemoto extends Remote {
    Registro registrar() throws RemoteException;

    /** Recupera o mesmo registro sem ocupar outra vaga; exige o token original. */
    Registro reconectar(String token) throws RemoteException;

    EstadoServico consultarEstado() throws RemoteException;

    ResultadoAcao mover(String token, int linha, int coluna) throws RemoteException;

    ResultadoAcao colocarBarreira(String token, int linha, int coluna, Orientacao orientacao)
            throws RemoteException;
}
