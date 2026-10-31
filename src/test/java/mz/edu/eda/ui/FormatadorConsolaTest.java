package mz.edu.eda.ui;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FormatadorConsolaTest {
    private String formatar(List<String> cabecalhos, List<? extends List<String>> linhas, int... larguras) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream saida = new PrintStream(buffer)) {
            FormatadorConsola.imprimirTabela(saida, cabecalhos, linhas, larguras);
        }
        return buffer.toString();
    }

    @Test
    void alinhaCabecalhosEValores() {
        String tabela = formatar(List.of("ID", "Nome"), List.of(
                List.of("U1", "Ana"), List.of("U-EST-02", "Nome comprido")), 12, 18);
        String[] linhas = tabela.split("\\R");
        assertEquals(linhas[0].indexOf("ID"), linhas[2].indexOf("U1"));
        assertEquals(linhas[0].indexOf("Nome"), linhas[2].indexOf("Ana"));
        assertTrue(linhas[1].contains("-+-"));
    }

    @Test
    void quebraTextosLongosSemOsTruncar() {
        String valor = "ABCDEFGHIJKLMNO";
        String tabela = formatar(List.of("Matricula"), List.of(List.of(valor)), 5);
        assertTrue(tabela.contains("Matri"));
        assertTrue(tabela.contains("cula"));
        assertTrue(tabela.contains("ABCDE"));
        assertTrue(tabela.contains("FGHIJ"));
        assertTrue(tabela.contains("KLMNO"));
    }

    @Test
    void apresentaValoresNulosOuEmFaltaComoNaoDisponiveis() {
        String tabela = formatar(List.of("A", "B", "C"), List.of(Arrays.asList(null, " ")), 8, 8, 8);
        assertEquals(3, tabela.split("N/D", -1).length - 1);
        assertTrue(tabela.contains("| N/D"));
    }

    @Test
    void rejeitaConfiguracaoDeColunasInvalida() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream saida = new PrintStream(buffer)) {
            assertThrows(IllegalArgumentException.class,
                    () -> FormatadorConsola.imprimirTabela(saida, List.of("A", "B"), List.of(), 8));
            assertThrows(IllegalArgumentException.class,
                    () -> FormatadorConsola.imprimirTabela(saida, List.of("A"), List.of(), 0));
        }
    }
}
