package br.edu.quoridor.cliente;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TerminalCLITest {
    @Test
    void vscodeAtivaPainelMesmoSemSystemConsole() {
        assertTrue(TerminalCLI.usarPainel(false, Map.of("TERM_PROGRAM", "vscode"), "auto"));
        assertTrue(TerminalCLI.usarPainel(false, Map.of("WT_SESSION", "terminal-windows"), "auto"));
        assertTrue(TerminalCLI.usarPainel(false, Map.of("TERM", "xterm-256color"), "auto"));
    }

    @Test
    void logsETestesPodemUsarModoTextoSemEscapes() {
        assertFalse(TerminalCLI.usarPainel(false, Map.of("TERM_PROGRAM", "vscode"), "texto"));
        assertFalse(TerminalCLI.usarPainel(false, Map.of("CI", "true", "TERM_PROGRAM", "vscode"), "auto"));
        assertFalse(TerminalCLI.usarPainel(false, Map.of("TERM", "dumb"), "auto"));
        assertFalse(TerminalCLI.usarPainel(false, Map.of(), "auto"));
    }

    @Test
    void usuarioPodeForcarPainelOuTexto() {
        assertTrue(TerminalCLI.usarPainel(false, Map.of(), "painel"));
        assertFalse(TerminalCLI.usarPainel(true, Map.of("TERM_PROGRAM", "vscode"), "texto"));
        assertThrows(IllegalArgumentException.class,
                () -> TerminalCLI.usarPainel(false, Map.of(), "desconhecido"));
    }

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
    void quatroEntradasDeJogadoresAtualizamMesmoPainelNoModoAnsi() {
        StringWriter texto = new StringWriter();
        TerminalCLI terminal = new TerminalCLI(new PrintWriter(texto), true);
        terminal.mostrar("Registrados: 1/4", "Aguardando", true);
        assertTrue(texto.toString().startsWith("\033[2J\033[H"));
        for (int jogadores = 2; jogadores <= 4; jogadores++) {
            texto.getBuffer().setLength(0);
            terminal.mostrar("Registrados: " + jogadores + "/4", "Aguardando", false);
            String atualizacao = texto.toString();
            assertTrue(atualizacao.startsWith("\0337\033[H"));
            assertTrue(atualizacao.endsWith("\0338"));
            assertFalse(atualizacao.contains("\033[2J"));
            assertTrue(atualizacao.contains("Registrados: " + jogadores + "/4"));
            assertFalse(atualizacao.contains("Registrados: " + (jogadores - 1) + "/4"));
        }
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
