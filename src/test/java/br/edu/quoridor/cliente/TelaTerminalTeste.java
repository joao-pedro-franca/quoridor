package br.edu.quoridor.cliente;

import java.util.Arrays;

/** Emula somente os controles emitidos pelo cliente, em uma tela de 28 por 80. */
final class TelaTerminalTeste {
    private final char[][] tela = new char[28][80];
    private int linha, coluna, salvaLinha, salvaColuna;
    private int topo, fundo = 27;
    TelaTerminalTeste() { limpar(); }
    void receber(String texto) {
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == 27) {
                char comando = texto.charAt(++i);
                if (comando == '7') { salvaLinha = linha; salvaColuna = coluna; }
                else if (comando == '8') { linha = salvaLinha; coluna = salvaColuna; }
                else if (comando == '[') {
                    int inicio = ++i;
                    while (texto.charAt(i) < '@' || texto.charAt(i) > '~') { i++; }
                    controle(texto.substring(inicio, i), texto.charAt(i));
                }
            } else if (c == '\r') { coluna = 0; }
            else if (c == '\n') { avancar(); }
            else {
                if (coluna == 80) { coluna = 0; avancar(); }
                tela[linha][coluna++] = c;
            }
        }
    }
    private void controle(String argumentos, char comando) {
        String[] p = argumentos.split(";", -1);
        switch (comando) {
            case 'H' -> { linha = numero(p, 0, 1) - 1; coluna = numero(p, 1, 1) - 1; }
            case 'r' -> { topo = numero(p, 0, 1) - 1; fundo = numero(p, 1, 28) - 1; linha = coluna = 0; }
            case 'K' -> Arrays.fill(tela[linha], ' ');
            case 'J' -> {
                if (argumentos.equals("2")) { limpar(); }
                else {
                    Arrays.fill(tela[linha], coluna, 80, ' ');
                    for (int l = linha + 1; l < 28; l++) { Arrays.fill(tela[l], ' '); }
                }
            }
            default -> { /* Modos de tela alternativa não afetam as verificações. */ }
        }
    }
    private int numero(String[] p, int indice, int padrao) {
        return indice >= p.length || p[indice].isEmpty() ? padrao : Integer.parseInt(p[indice]);
    }
    private void limpar() { for (char[] l : tela) { Arrays.fill(l, ' '); } }
    private void avancar() {
        if (linha == fundo) {
            for (int l = topo; l < fundo; l++) { System.arraycopy(tela[l + 1], 0, tela[l], 0, 80); }
            Arrays.fill(tela[fundo], ' ');
        } else { linha = Math.min(27, linha + 1); }
    }
    String linha(int n) { return new String(tela[n - 1]).stripTrailing(); }
    String painel() { return String.join("\n", java.util.stream.IntStream.rangeClosed(1, 25).mapToObj(this::linha).toList()); }
    int linhaCursor() { return linha + 1; }
    int colunaCursor() { return coluna + 1; }
}
