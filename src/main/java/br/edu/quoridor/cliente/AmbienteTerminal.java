package br.edu.quoridor.cliente;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Detecção feita uma vez: capacidade ANSI e descritores reais, não apenas Console. */
final class AmbienteTerminal {
    private AmbienteTerminal() { }

    static boolean interativo() {
        String sistema = System.getProperty("os.name");
        return detectar(System.getProperty("quoridor.terminal", "auto"), System.getenv(),
                sistema, System.console() != null, () -> descritoresDeTerminal(sistema));
    }

    static boolean detectar(String modo, Map<String, String> ambiente, String sistema,
                             boolean consoleJava, BooleanSupplier descritoresDeTerminal) {
        switch (modo.toLowerCase(Locale.ROOT)) {
            case "ansi": return true; // Escolha explícita de um terminal ANSI pelo usuário.
            case "texto": return false;
            case "auto": break;
            default: throw new IllegalArgumentException("Use -Dquoridor.terminal=auto, ansi ou texto.");
        }
        if ("dumb".equalsIgnoreCase(ambiente.get("TERM"))
                || Boolean.parseBoolean(ambiente.getOrDefault("CI", "false"))) { return false; }
        String term = ambiente.getOrDefault("TERM", "").toLowerCase(Locale.ROOT);
        boolean capacidade = "vscode".equalsIgnoreCase(ambiente.get("TERM_PROGRAM"))
                || ambiente.containsKey("WT_SESSION") || ambiente.containsKey("ANSICON")
                || "ON".equalsIgnoreCase(ambiente.get("ConEmuANSI"))
                || term.startsWith("xterm") || term.startsWith("screen") || term.startsWith("vt")
                || (!sistema.startsWith("Windows") && consoleJava);
        // Variáveis de terminal também são herdadas por processos redirecionados.
        return capacidade && descritoresDeTerminal.getAsBoolean();
    }

    private static boolean descritoresDeTerminal(String sistema) {
        Process processo = null;
        try {
            ProcessBuilder comando = sistema.startsWith("Windows")
                    ? new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command",
                        "if ([Console]::IsInputRedirected -or [Console]::IsOutputRedirected) { exit 1 }; exit 0")
                    : new ProcessBuilder("sh", "-c", "test -t 0 && test -t 1");
            // Herda os descritores reais; não lê comandos nem imprime no painel.
            processo = comando.inheritIO().start();
            return processo.waitFor(3, TimeUnit.SECONDS) && processo.exitValue() == 0;
        } catch (IOException indisponivel) {
            return false;
        } catch (InterruptedException interrompido) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (processo != null && processo.isAlive()) { processo.destroyForcibly(); }
        }
    }
}
