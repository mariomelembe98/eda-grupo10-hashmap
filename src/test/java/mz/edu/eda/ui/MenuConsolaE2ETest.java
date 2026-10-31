package mz.edu.eda.ui;

import mz.edu.eda.persistencia.RepositorioFicheiros;
import mz.edu.eda.servico.ParqueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class MenuConsolaE2ETest {
    @TempDir Path pasta;

    @Test void fluxoCompletoPelaConsolaSobreviveAoReinicio() throws Exception {
        Path ficheiro = pasta.resolve("parque-e2e.dat");
        RepositorioFicheiros repositorio = new RepositorioFicheiros(ficheiro);
        ParqueService servico = new ParqueService(repositorio);
        assertTrue(servico.exportarEstado().utilizadores().isEmpty());

        String comandos = "1\n1\nU-E\nEstudante\n841\nE1\nInformatica\n"
                + "1\n2\nU-D\nDocente\n842\nF1\nMatematica\n"
                + "2\nAAA-111\nToyota\nYaris\nAzul\nU-E\n"
                + "2\nBBB-222\nHonda\nFit\nBranco\nU-D\n"
                + "3\nE-01\nBloco A\n1\n3\nD-01\nBloco B\n2\n"
                + "7\nAAA-111\nD-01\n7\nAAA-111\nE-01\n7\nAAA-111\nE-01\n"
                + "7\nBBB-222\nD-01\n9\n12\n8\nAAA-111\n10\n11\nU-E\n0\n";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream saida = new PrintStream(buffer);
             Scanner entrada = new Scanner(new StringReader(comandos))) {
            new MenuConsola(servico, entrada, saida).executar();
        }

        String apresentacao = buffer.toString();
        assertTrue(apresentacao.contains("Estudante cadastrado com sucesso."));
        assertTrue(apresentacao.contains("Docente/funcionario cadastrado com sucesso."));
        assertTrue(apresentacao.contains("Entrada registada: EST-1"));
        assertTrue(apresentacao.contains("Entrada registada: EST-2"));
        assertTrue(apresentacao.contains("Operacao rejeitada: Tipo de utilizador incompatível com o lugar"));
        assertTrue(apresentacao.contains("Operacao rejeitada: Veiculo ja tem estacionamento activo"));
        assertTrue(apresentacao.contains("Lugares livres: 0"));
        assertTrue(apresentacao.contains("Lugares ocupados: 2"));
        assertTrue(apresentacao.contains("Historico completo:"));
        assertTrue(apresentacao.contains("Estacionamentos do utilizador U-E:"));
        assertTrue(apresentacao.contains("Aplicacao encerrada."));

        ParqueService reiniciado = new ParqueService(repositorio);
        assertEquals(2, reiniciado.exportarEstado().utilizadores().size());
        assertEquals(2, reiniciado.exportarEstado().veiculos().size());
        assertEquals(2, reiniciado.exportarEstado().lugares().size());
        assertEquals(2, reiniciado.consultarHistorico().size());
        assertFalse(reiniciado.consultarHistorico().getFirst().estaActivo());
        assertTrue(reiniciado.consultarHistorico().getLast().estaActivo());
        assertEquals(1, reiniciado.consultarEstacionamentosPorUtilizador("U-E").size());
        assertEquals(1, reiniciado.quantidadeLugaresLivres());
        assertEquals(1, reiniciado.quantidadeLugaresOcupados());
        ByteArrayOutputStream segundaSaida = new ByteArrayOutputStream();
        try (PrintStream saida = new PrintStream(segundaSaida);
             Scanner entrada = new Scanner(new StringReader("7\nAAA-111\nE-01\n0\n"))) {
            new MenuConsola(reiniciado, entrada, saida).executar();
        }
        assertTrue(segundaSaida.toString().contains("Entrada registada: EST-3"));

        ParqueService segundoReinicio = new ParqueService(repositorio);
        assertEquals(3, segundoReinicio.consultarHistorico().size());
        assertEquals(2, segundoReinicio.quantidadeLugaresOcupados());
        assertEquals(0, segundoReinicio.quantidadeLugaresLivres());
        assertEquals("EST-3", segundoReinicio.consultarHistorico().getLast().getIdentificador());
    }
}
