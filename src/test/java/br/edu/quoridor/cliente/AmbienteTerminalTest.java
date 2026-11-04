package br.edu.quoridor.cliente;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AmbienteTerminalTest {
    @Test void vscodeComConsoleNuloUsaAnsiSeDescritoresSaoInterativos() {
        assertTrue(AmbienteTerminal.detectar("auto", Map.of("TERM_PROGRAM", "vscode"), "Windows 11", false, () -> true));
    }
    @Test void variavelVscodeNaoForcaAnsiQuandoSaidaOuEntradaEhRedirecionada() {
        assertFalse(AmbienteTerminal.detectar("auto", Map.of("TERM_PROGRAM", "vscode"), "Windows 11", false, () -> false));
    }
    @Test void windowsTerminalTambemEhReconhecido() {
        assertTrue(AmbienteTerminal.detectar("auto", Map.of("WT_SESSION", "sessao"), "Windows 11", false, () -> true));
    }
    @Test void termDumbNaoRecebeAnsi() {
        assertFalse(AmbienteTerminal.detectar("auto", Map.of("TERM_PROGRAM", "vscode", "TERM", "dumb"),
                "Windows 11", true, () -> true));
    }
    @Test void ciUsaTexto() {
        assertFalse(AmbienteTerminal.detectar("auto", Map.of("TERM_PROGRAM", "vscode", "CI", "true"),
                "Windows 11", true, () -> true));
    }
    @Test void terminalDesconhecidoNaoRecebeControles() {
        assertFalse(AmbienteTerminal.detectar("auto", Map.of(), "Windows 11", false,
                () -> { fail("Não precisa sondar sem indicação de suporte ANSI."); return true; }));
    }
    @Test void modoTextoDispensaSondagem() {
        assertFalse(AmbienteTerminal.detectar("texto", Map.of("TERM_PROGRAM", "vscode"), "Windows 11", false,
                () -> { fail("Modo explícito não deve sondar."); return true; }));
    }
    @Test void usuarioPodeSelecionarTerminalAnsiExplicitamente() {
        assertTrue(AmbienteTerminal.detectar("ansi", Map.of(), "Windows 11", false,
                () -> { fail("Modo explícito não deve sondar."); return false; }));
    }
    @Test void modoInvalidoProduzErroClaro() {
        assertThrows(IllegalArgumentException.class,
                () -> AmbienteTerminal.detectar("invalido", Map.of(), "Windows 11", false, () -> true));
    }
    @Test void terminalPosixEhReconhecido() {
        assertTrue(AmbienteTerminal.detectar("auto", Map.of("TERM", "xterm-256color"), "Linux", false, () -> true));
    }

    @Test @Timeout(15)
    void processoRedirecionadoHerdandoVscodePermaneceEmTexto() throws Exception {
        String executavel = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        String cp = Path.of("target/classes").toAbsolutePath() + java.io.File.pathSeparator
                + Path.of("target/test-classes").toAbsolutePath();
        ProcessBuilder comando = new ProcessBuilder(executavel, "-Dquoridor.terminal=auto", "-cp", cp,
                AmbienteTerminalTest.class.getName()).redirectErrorStream(true);
        comando.environment().put("TERM_PROGRAM", "vscode");
        comando.environment().remove("CI");
        comando.environment().remove("TERM");
        Process processo = comando.start();
        try {
            assertTrue(processo.waitFor(10, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(0, processo.exitValue());
            assertEquals("false", new String(processo.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).strip());
        } finally {
            processo.destroyForcibly();
            assertTrue(processo.waitFor(3, java.util.concurrent.TimeUnit.SECONDS));
        }
    }

    public static void main(String[] args) { System.out.println(AmbienteTerminal.interativo()); }
}
