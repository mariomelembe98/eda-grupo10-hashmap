package mz.edu.eda.ui;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Formata tabelas de texto sem sequências ANSI nem dependências externas. */
final class FormatadorConsola {
    private static final String AUSENTE = "N/D";

    private FormatadorConsola() { }

    static void imprimirTabela(PrintStream saida, List<String> cabecalhos,
                               List<? extends List<String>> linhas, int... largurasMaximas) {
        Objects.requireNonNull(saida, "Saida nao pode ser nula");
        Objects.requireNonNull(cabecalhos, "Cabecalhos nao podem ser nulos");
        Objects.requireNonNull(linhas, "Linhas nao podem ser nulas");
        Objects.requireNonNull(largurasMaximas, "Larguras nao podem ser nulas");
        if (cabecalhos.isEmpty()) throw new IllegalArgumentException("A tabela deve ter pelo menos uma coluna");
        if (largurasMaximas.length != cabecalhos.size())
            throw new IllegalArgumentException("Deve ser indicada uma largura maxima por coluna");
        for (int largura : largurasMaximas)
            if (largura <= 0) throw new IllegalArgumentException("As larguras maximas devem ser positivas");

        int colunas = cabecalhos.size();
        int[] larguras = new int[colunas];
        List<List<String>> valores = new ArrayList<>(linhas.size());
        for (List<String> linha : linhas) {
            List<String> preenchida = new ArrayList<>(colunas);
            for (int coluna = 0; coluna < colunas; coluna++) {
                String valor = linha != null && coluna < linha.size() ? normalizar(linha.get(coluna)) : AUSENTE;
                preenchida.add(valor);
            }
            valores.add(preenchida);
        }
        for (int coluna = 0; coluna < colunas; coluna++) {
            int largura = Math.min(largurasMaximas[coluna], normalizar(cabecalhos.get(coluna)).length());
            for (List<String> linha : valores) {
                for (String segmento : linha.get(coluna).split("\\R", -1))
                    largura = Math.max(largura, Math.min(largurasMaximas[coluna], segmento.length()));
            }
            larguras[coluna] = Math.max(1, largura);
        }

        List<List<String>> cabecalhosQuebrados = new ArrayList<>(colunas);
        int alturaCabecalho = 1;
        for (int coluna = 0; coluna < colunas; coluna++) {
            List<String> quebrado = quebrar(normalizar(cabecalhos.get(coluna)), larguras[coluna]);
            cabecalhosQuebrados.add(quebrado);
            alturaCabecalho = Math.max(alturaCabecalho, quebrado.size());
        }
        for (int linhaVisual = 0; linhaVisual < alturaCabecalho; linhaVisual++) {
            List<String> valoresVisuais = new ArrayList<>(colunas);
            for (int coluna = 0; coluna < colunas; coluna++)
                valoresVisuais.add(linhaVisual < cabecalhosQuebrados.get(coluna).size()
                        ? cabecalhosQuebrados.get(coluna).get(linhaVisual) : "");
            imprimirLinha(saida, valoresVisuais, larguras);
        }
        imprimirSeparador(saida, larguras);
        for (List<String> linha : valores) {
            List<List<String>> celulas = new ArrayList<>(colunas);
            int altura = 1;
            for (int coluna = 0; coluna < colunas; coluna++) {
                List<String> quebrada = quebrar(linha.get(coluna), larguras[coluna]);
                celulas.add(quebrada);
                altura = Math.max(altura, quebrada.size());
            }
            for (int linhaVisual = 0; linhaVisual < altura; linhaVisual++) {
                List<String> valoresVisuais = new ArrayList<>(colunas);
                for (int coluna = 0; coluna < colunas; coluna++)
                    valoresVisuais.add(linhaVisual < celulas.get(coluna).size()
                            ? celulas.get(coluna).get(linhaVisual) : "");
                imprimirLinha(saida, valoresVisuais, larguras);
            }
        }
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? AUSENTE : valor;
    }

    private static List<String> quebrar(String texto, int largura) {
        List<String> resultado = new ArrayList<>();
        for (String linha : texto.split("\\R", -1)) {
            StringBuilder actual = new StringBuilder();
            for (String palavra : linha.trim().split("\\s+")) {
                if (palavra.isEmpty()) continue;
                if (actual.length() > 0 && actual.length() + 1 + palavra.length() <= largura) {
                    actual.append(' ').append(palavra);
                } else {
                    if (actual.length() > 0) {
                        resultado.add(actual.toString());
                        actual.setLength(0);
                    }
                    int inicio = 0;
                    while (palavra.codePointCount(inicio, palavra.length()) > largura) {
                        int fim = palavra.offsetByCodePoints(inicio, largura);
                        resultado.add(palavra.substring(inicio, fim));
                        inicio = fim;
                    }
                    actual.append(palavra.substring(inicio));
                }
            }
            resultado.add(actual.toString());
        }
        return resultado.isEmpty() ? List.of("") : resultado;
    }

    private static void imprimirLinha(PrintStream saida, List<String> valores, int[] larguras) {
        StringBuilder linha = new StringBuilder("| ");
        for (int coluna = 0; coluna < larguras.length; coluna++) {
            String valor = valores.get(coluna);
            linha.append(valor);
            linha.append(" ".repeat(Math.max(0, larguras[coluna] - valor.length())));
            linha.append(" | ");
        }
        saida.println(linha.toString().stripTrailing());
    }

    private static void imprimirSeparador(PrintStream saida, int[] larguras) {
        StringBuilder linha = new StringBuilder("+-");
        for (int largura : larguras) linha.append("-".repeat(largura)).append("-+");
        saida.println(linha);
    }
}
