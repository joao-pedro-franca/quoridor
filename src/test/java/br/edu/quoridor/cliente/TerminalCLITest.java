package br.edu.quoridor.cliente;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class TerminalCLITest {
    @Test
    void naoRedesenhaPainelInalterado() {
        StringWriter texto = new StringWriter();
        TerminalCLI terminal = new TerminalCLI(new PrintWriter(texto), false);
        terminal.mostrar("Tabuleiro", "Aguardando", false);
        String primeira = texto.toString();
        terminal.mostrar("Tabuleiro", "Aguardando", false);
        assertEquals(primeira, texto.toString());
        terminal.mostrar("Tabuleiro atualizado", "Sua vez", false);
        assertTrue(texto.toString().length() > primeira.length());
        assertFalse(texto.toString().contains("\033"));
    }

    @Test
    void atualizacaoInterativaSalvaERestauraCursorSemReescreverPrompt() {
        StringWriter texto = new StringWriter();
        TerminalCLI terminal = new TerminalCLI(new PrintWriter(texto), true);
        terminal.mostrar("Tabuleiro", "Aguardando", true);
        texto.getBuffer().setLength(0);
        terminal.mostrar("Tabuleiro novo", "Sua vez", false);
        assertTrue(texto.toString().startsWith("\0337\033[H"));
        assertTrue(texto.toString().endsWith("\0338"));
        assertFalse(texto.toString().contains("> "));
    }

    @Test
    void removeControlesDeTextoDoPainel() {
        StringWriter texto = new StringWriter();
        TerminalCLI terminal = new TerminalCLI(new PrintWriter(texto), false);
        terminal.mostrar("Tabuleiro", "Erro\r\n\033: entrada inválida", true);
        assertTrue(texto.toString().contains("Erro: entrada inválida"));
        assertFalse(texto.toString().contains("\033"));
    }
}
