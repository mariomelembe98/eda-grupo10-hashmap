package mz.edu.eda.ui;

import mz.edu.eda.modelo.DocenteFuncionario;
import mz.edu.eda.modelo.Estudante;
import mz.edu.eda.modelo.LugarEstacionamento;
import mz.edu.eda.modelo.TipoLugar;
import mz.edu.eda.modelo.Veiculo;
import mz.edu.eda.servico.ParqueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class MenuConsolaTest {
    @TempDir Path pasta;

    private String executar(ParqueService service, String comandos) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(buffer); Scanner in = new Scanner(new StringReader(comandos))) {
            new MenuConsola(service, in, out).executar();
        }
        return buffer.toString();
    }

    @Test void navegaRegistaEntradaSaidaListagensEConsultas() throws Exception {
        ParqueService service = new ParqueService(new mz.edu.eda.persistencia.RepositorioFicheiros(pasta.resolve("dados.dat")));
        String comandos = "1\n1\nU1\nAna\n841\nE1\nInformatica\n"
                + "2\nAAA-111\nToyota\nCorolla\nBranco\nU1\n"
                + "3\nL1\nBloco A\n1\n7\nAAA-111\nL1\n9\n10\n11\nU1\n12\n"
                + "8\nAAA-111\n10\n13\n1\nU1\n0\n13\n2\nAAA-111\n0\n13\n3\nL1\n0\n0\n";
        String output = executar(service, comandos);
        assertTrue(output.contains("Estudante cadastrado com sucesso."));
        assertTrue(output.contains("Veiculo cadastrado com sucesso."));
        assertTrue(output.contains("Lugar cadastrado com sucesso."));
        assertTrue(output.contains("Entrada registada: EST-1"));
        assertTrue(output.contains("Saida registada: EST-1"));
        assertTrue(output.contains("Lugares livres: 0"));
        assertTrue(output.contains("Lugares ocupados: 1"));
        assertTrue(output.contains("Aplicacao encerrada."));
        assertEquals(1, service.consultarHistorico().size());
        assertFalse(service.consultarHistorico().getFirst().estaActivo());
    }

    @Test void aceitaDocenteEListaVeiculosPorCategoria() {
        ParqueService service = new ParqueService();
        String comandos = "1\n2\nD1\nDalia\n821\nF1\nMatematica\n"
                + "2\nMZ-222\nHonda\nFit\nAzul\nD1\n6\n0\n";
        String output = executar(service, comandos);
        assertTrue(output.contains("Docente/funcionario cadastrado com sucesso."));
        assertTrue(output.contains("Veiculos de docentes/funcionarios:"));
        assertTrue(output.contains("MZ-222"));
    }

    @Test void actualizaUtilizadorVeiculoELugar() {
        ParqueService service = new ParqueService();
        service.cadastrarEstudante(new Estudante("U1", "Ana", "841", "E1", "Curso"));
        service.cadastrarVeiculo(new Veiculo("AAA-111", "Toyota", "Corolla", "Branco", "U1"));
        service.cadastrarLugar(new LugarEstacionamento("L1", "Bloco A", TipoLugar.ESTUDANTE));
        String comandos = "4\n1\nU1\nAna Maria\n842\nE1\nCurso Novo\n"
                + "4\n2\nAAA-111\nToyota\nYaris\nAzul\n"
                + "4\n3\nL1\nBloco B\n1\n0\n";
        String output = executar(service, comandos);
        assertTrue(output.contains("Utilizador actualizado com sucesso."));
        assertTrue(output.contains("Veiculo actualizado com sucesso."));
        assertTrue(output.contains("Lugar actualizado com sucesso."));
        assertEquals("Ana Maria", service.consultarUtilizador("U1").getNome());
        assertEquals("Yaris", service.consultarVeiculo("AAA-111").getModelo());
        assertEquals("Bloco B", service.consultarLugar("L1").getLocalizacao());
    }

    @Test void rejeitaOpcoesInvalidasECamposVaziosSemEncerrar() {
        ParqueService service = new ParqueService();
        service.cadastrarEstudante(new Estudante("U1", "Ana", "841", "E1", "Curso"));
        String output = executar(service, "x\n99\n2\n\nAAA-111\nToyota\nYaris\nBranco\nU1\n0\n");
        assertTrue(output.contains("Introduza uma opcao numerica valida."));
        assertTrue(output.contains("Opcao invalida."));
        assertTrue(output.contains("Campo obrigatorio."));
        assertTrue(output.contains("Veiculo cadastrado com sucesso."));
        assertTrue(output.contains("Aplicacao encerrada."));
    }

    @Test void erroDeRegraEApresentadoSemStackTrace() {
        ParqueService service = new ParqueService();
        String output = executar(service, "7\nNAO-EXISTE\nL1\n0\n");
        assertTrue(output.contains("Operacao rejeitada: Veiculo nao registado"));
        assertFalse(output.contains("Exception"));
        assertTrue(output.contains("Aplicacao encerrada."));
    }

    @Test void fimDaEntradaEncerraMenuSemExcepcao() {
        String output = executar(new ParqueService(), "");
        assertTrue(output.contains("Aplicacao encerrada."));
    }
}
