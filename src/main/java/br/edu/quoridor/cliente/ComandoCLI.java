package br.edu.quoridor.cliente;

import br.edu.quoridor.modelo.Orientacao;

import java.util.Locale;

/** Interpreta somente a sintaxe; a legalidade das jogadas pertence ao servidor. */
record ComandoCLI(Tipo tipo, int linha, int coluna, Orientacao orientacao) {
    enum Tipo { AJUDA, TABULEIRO, MOVER, BARREIRA, SESSAO, SAIR }

    static ComandoCLI interpretar(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Digite um comando. Use ajuda para ver as opções.");
        }
        String[] partes = texto.strip().toLowerCase(Locale.ROOT).split("\\s+");
        return switch (partes[0]) {
            case "ajuda" -> simples(Tipo.AJUDA, partes);
            case "tabuleiro" -> simples(Tipo.TABULEIRO, partes);
            case "sessao" -> simples(Tipo.SESSAO, partes);
            case "sair" -> simples(Tipo.SAIR, partes);
            case "mover" -> {
                exigirQuantidade(partes, 3, "mover LINHA COLUNA");
                yield new ComandoCLI(Tipo.MOVER, coordenada(partes[1], 9), coordenada(partes[2], 9), null);
            }
            case "barreira" -> {
                exigirQuantidade(partes, 4, "barreira H|V LINHA,LINHA COLUNA,COLUNA (ex.: barreira H 3,4 4,5)");
                Orientacao orientacao = switch (partes[1]) {
                    case "h" -> Orientacao.HORIZONTAL;
                    case "v" -> Orientacao.VERTICAL;
                    default -> throw new IllegalArgumentException("Orientação inválida. Use H ou V.");
                };
                yield new ComandoCLI(Tipo.BARREIRA, inicioPar(partes[2]),
                        inicioPar(partes[3]), orientacao);
            }
            default -> throw new IllegalArgumentException("Comando desconhecido. Use ajuda.");
        };
    }

    private static ComandoCLI simples(Tipo tipo, String[] partes) {
        exigirQuantidade(partes, 1, partes[0]);
        return new ComandoCLI(tipo, 0, 0, null);
    }

    private static void exigirQuantidade(String[] partes, int quantidade, String uso) {
        if (partes.length != quantidade) {
            throw new IllegalArgumentException("Uso: " + uso);
        }
    }

    private static int coordenada(String texto, int limite) {
        try {
            int valor = Integer.parseInt(texto);
            if (valor >= 1 && valor <= limite) {
                return valor - 1;
            }
        } catch (NumberFormatException invalido) {
            // Inclui números maiores que o intervalo de int.
        }
        throw new IllegalArgumentException("Coordenadas devem ser inteiros de 1 a " + limite + ".");
    }

    private static int inicioPar(String texto) {
        if (!texto.matches("[1-9],[1-9]")) {
            throw new IllegalArgumentException("Use pares no formato 3,4, com números de 1 a 9.");
        }
        int primeiro = coordenada(texto.substring(0, 1), 9);
        int segundo = coordenada(texto.substring(2), 9);
        if (segundo != primeiro + 1) {
            throw new IllegalArgumentException("Cada par deve ser consecutivo e crescente, como 3,4.");
        }
        return primeiro;
    }
}
