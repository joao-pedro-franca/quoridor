package br.edu.quoridor.cliente;

import java.io.PrintWriter;

/** Um único escritor atualiza o painel superior e preserva a linha de entrada. */
final class TerminalCLI {
    private static final int LINHAS_PAINEL = 23;
    private final PrintWriter saida;
    private final boolean interativo;
    private String ultimoPainel;

    TerminalCLI(PrintWriter saida, boolean interativo) {
        this.saida = saida;
        this.interativo = interativo;
    }

    void mostrar(String conteudo, String mensagem, boolean forcar) {
        StringBuilder painel = new StringBuilder("Quoridor RMI | Comandos: ajuda, tabuleiro, sessao, sair\n");
        String[] linhas = conteudo.split("\n");
        for (int i = 0; i < LINHAS_PAINEL - 2; i++) {
            painel.append(i < linhas.length ? limpar(linhas[i]) : "").append('\n');
        }
        painel.append(limpar(mensagem));
        String novoPainel = painel.toString();
        if (!forcar && novoPainel.equals(ultimoPainel)) {
            return;
        }
        if (!interativo) {
            saida.println(novoPainel);
        } else {
            boolean primeiro = ultimoPainel == null;
            saida.print(primeiro ? "\033[2J\033[H" : "\0337\033[H");
            String[] exibidas = novoPainel.split("\n", -1);
            for (int i = 0; i < exibidas.length; i++) {
                saida.print("\033[2K" + exibidas[i]);
                if (i < exibidas.length - 1) {
                    saida.print("\r\n");
                }
            }
            if (primeiro) {
                saida.print("\r\n> ");
            } else {
                saida.print("\0338");
            }
        }
        saida.flush();
        ultimoPainel = novoPainel;
    }

    void novoPrompt() {
        if (interativo) {
            saida.print("\033[24;1H\033[2K> ");
        } else {
            saida.print("> ");
        }
        saida.flush();
    }

    void finalizar() {
        if (interativo) {
            saida.print("\033[24;1H\033[2K");
        }
        saida.println("Cliente encerrado. Sua vaga permanece reservada para reconexão.");
        saida.flush();
    }

    private static String limpar(String texto) {
        StringBuilder limpo = new StringBuilder();
        for (int i = 0; i < texto.length() && limpo.length() < 78; i++) {
            if (!Character.isISOControl(texto.charAt(i))) {
                limpo.append(texto.charAt(i));
            }
        }
        return limpo.toString();
    }
}
