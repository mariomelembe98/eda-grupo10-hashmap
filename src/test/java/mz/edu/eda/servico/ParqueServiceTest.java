package mz.edu.eda.servico;

import mz.edu.eda.modelo.DocenteFuncionario;
import mz.edu.eda.modelo.Estacionamento;
import mz.edu.eda.modelo.Estudante;
import mz.edu.eda.modelo.LugarEstacionamento;
import mz.edu.eda.modelo.TipoLugar;
import mz.edu.eda.modelo.Veiculo;
import mz.edu.eda.persistencia.RepositorioFicheiros;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class ParqueServiceTest {
    private ParqueService service;

    @BeforeEach void preparar() {
        service = new ParqueService(Clock.fixed(Instant.parse("2026-10-31T08:00:00Z"), ZoneOffset.UTC));
    }

    private void cadastrarEstudanteECarro(String id, String matricula, String lugar) {
        service.cadastrarEstudante(new Estudante(id, "Ana " + id, "841234567", "E" + id, "Informatica"));
        service.cadastrarVeiculo(new Veiculo(matricula, "Toyota", "Corolla", "Branco", id));
        service.cadastrarLugar(new LugarEstacionamento(lugar, "Bloco A", TipoLugar.ESTUDANTE));
    }

    @Test void cadastrosValidosERejeicaoDeIdentificadoresDuplicados() {
        cadastrarEstudanteECarro("U1", "AAA-111", "L1");
        assertDoesNotThrow(() -> service.cadastrarDocenteFuncionario(
                new DocenteFuncionario("U2", "Bia", "821234567", "F2", "Engenharia")));
        assertThrows(IllegalArgumentException.class, () -> service.cadastrarEstudante(
                new Estudante("U1", "Outro", "841", "E2", "Curso")));
        assertThrows(IllegalArgumentException.class, () -> service.cadastrarVeiculo(
                new Veiculo("AAA-111", "Honda", "Fit", "Azul", "U1")));
        assertThrows(IllegalArgumentException.class, () -> service.cadastrarLugar(
                new LugarEstacionamento("L1", "Bloco B", TipoLugar.ESTUDANTE)));
    }

    @Test void rejeitaVeiculoComProprietarioInexistente() {
        assertThrows(IllegalArgumentException.class, () -> service.cadastrarVeiculo(
                new Veiculo("AAA-111", "Toyota", "Corolla", "Branco", "inexistente")));
    }

    @Test void entradaValidaActualizaListasEContagens() {
        cadastrarEstudanteECarro("U1", "AAA-111", "L1");
        Estacionamento entrada = service.registarEntrada("AAA-111", "L1");
        assertEquals("AAA-111", entrada.getMatricula());
        assertTrue(entrada.estaActivo());
        assertEquals(1, service.quantidadeLugaresOcupados());
        assertEquals(0, service.quantidadeLugaresLivres());
        assertEquals("AAA-111", service.listarVeiculosEstacionados().getFirst().getMatricula());
    }

    @Test void impedeLugarOcupadoEEntradaDuplicadaDoVeiculo() {
        service.cadastrarEstudante(new Estudante("U1", "Ana", "841", "E1", "Curso"));
        service.cadastrarEstudante(new Estudante("U2", "Bia", "842", "E2", "Curso"));
        service.cadastrarVeiculo(new Veiculo("AAA-111", "Toyota", "Corolla", "Branco", "U1"));
        service.cadastrarVeiculo(new Veiculo("BBB-222", "Honda", "Fit", "Azul", "U2"));
        service.cadastrarLugar(new LugarEstacionamento("L1", "Bloco A", TipoLugar.ESTUDANTE));
        service.cadastrarLugar(new LugarEstacionamento("L2", "Bloco A", TipoLugar.ESTUDANTE));
        service.registarEntrada("AAA-111", "L1");
        assertThrows(IllegalArgumentException.class, () -> service.registarEntrada("AAA-111", "L2"));
        assertThrows(IllegalArgumentException.class, () -> service.registarEntrada("BBB-222", "L1"));
    }

    @Test void estudanteNaoPodeOcuparLugarReservado() {
        service.cadastrarEstudante(new Estudante("U1", "Ana", "841", "E1", "Curso"));
        service.cadastrarVeiculo(new Veiculo("AAA-111", "Toyota", "Corolla", "Branco", "U1"));
        service.cadastrarLugar(new LugarEstacionamento("D1", "Bloco B", TipoLugar.DOCENTE_FUNCIONARIO));
        assertThrows(IllegalArgumentException.class, () -> service.registarEntrada("AAA-111", "D1"));
    }

    @Test void saidaLibertaLugarEConservaHistoricoEConsultaPorUtilizador() {
        cadastrarEstudanteECarro("U1", "AAA-111", "L1");
        service.registarEntrada("AAA-111", "L1");
        Estacionamento saida = service.registarSaida("AAA-111");
        assertFalse(saida.estaActivo());
        assertEquals(1, service.quantidadeLugaresLivres());
        assertEquals(0, service.quantidadeLugaresOcupados());
        assertThrows(IllegalArgumentException.class, () -> service.registarSaida("AAA-111"));
        assertEquals(1, service.consultarHistorico().size());
        assertEquals("U1", service.consultarEstacionamentosPorUtilizador("U1").getFirst().getIdProprietario());
        assertEquals("EST-2", service.registarEntrada("AAA-111", "L1").getIdentificador());
    }

    @Test void ordenaMatriculasPorTipoDeProprietario() {
        service.cadastrarEstudante(new Estudante("E1", "Ana", "841", "E1", "Curso"));
        service.cadastrarEstudante(new Estudante("E2", "Bia", "842", "E2", "Curso"));
        service.cadastrarDocenteFuncionario(new DocenteFuncionario("D1", "Caio", "843", "F1", "Depto"));
        service.cadastrarVeiculo(new Veiculo("ZZZ-999", "Toyota", "A", "Branco", "E1"));
        service.cadastrarVeiculo(new Veiculo("AAA-111", "Honda", "B", "Azul", "E2"));
        service.cadastrarVeiculo(new Veiculo("MMM-555", "Nissan", "C", "Preto", "D1"));
        assertEquals("AAA-111", service.listarVeiculosDeEstudantes().getFirst().getMatricula());
        assertEquals("MMM-555", service.listarVeiculosDeDocentesFuncionarios().getFirst().getMatricula());
    }

    @Test void actualizacoesNaoInvalidamEstacionamentosActivos() {
        cadastrarEstudanteECarro("U1", "AAA-111", "L1");
        service.cadastrarEstudante(new Estudante("U2", "Bia", "842", "E2", "Curso"));
        service.cadastrarLugar(new LugarEstacionamento("D1", "Bloco B", TipoLugar.DOCENTE_FUNCIONARIO));
        service.registarEntrada("AAA-111", "L1");
        assertThrows(IllegalArgumentException.class, () -> service.actualizarVeiculo(
                new Veiculo("AAA-111", "Toyota", "Corolla", "Branco", "U2")));
        assertThrows(IllegalArgumentException.class, () -> service.actualizarLugar(
                new LugarEstacionamento("L1", "Bloco A", TipoLugar.DOCENTE_FUNCIONARIO)));
        assertThrows(IllegalArgumentException.class, () -> service.actualizarUtilizador(
                new DocenteFuncionario("U1", "Ana", "841", "F1", "Depto")));
        assertEquals("U1", service.consultarVeiculo("AAA-111").getIdProprietario());
        assertEquals(TipoLugar.ESTUDANTE, service.consultarLugar("L1").getTipo());
    }

    @Test void actualizacoesValidasSubstituemEntidadesSemMudarIdentificadores() {
        cadastrarEstudanteECarro("U1", "AAA-111", "L1");
        service.actualizarUtilizador(new Estudante("U1", "Ana Maria", "841000000", "EU1", "Matematica"));
        service.actualizarVeiculo(new Veiculo("AAA-111", "Toyota", "Yaris", "Azul", "U1"));
        service.actualizarLugar(new LugarEstacionamento("L1", "Bloco C", TipoLugar.ESTUDANTE));
        assertEquals("Ana Maria", service.consultarUtilizador("U1").getNome());
        assertEquals("Yaris", service.consultarVeiculo("AAA-111").getModelo());
        assertEquals("Bloco C", service.consultarLugar("L1").getLocalizacao());
    }

    @Test void numerosInstitucionaisSaoUnicosDentroDaCategoria() {
        service.cadastrarEstudante(new Estudante("U1", "Ana", "841", "NUM-1", "Curso"));
        service.cadastrarDocenteFuncionario(new DocenteFuncionario("U2", "Bia", "842", "NUM-1", "Depto"));
        assertThrows(IllegalArgumentException.class, () -> service.cadastrarEstudante(
                new Estudante("U3", "Caio", "843", "NUM-1", "Curso")));
        assertThrows(IllegalArgumentException.class, () -> service.cadastrarDocenteFuncionario(
                new DocenteFuncionario("U4", "Dina", "844", "NUM-1", "Depto")));
        assertEquals(2, service.exportarEstado().utilizadores().size());
    }

    @Test void actualizacaoNaoPodeDuplicarNumeroInstitucional() {
        service.cadastrarEstudante(new Estudante("U1", "Ana", "841", "E1", "Curso"));
        service.cadastrarEstudante(new Estudante("U2", "Bia", "842", "E2", "Curso"));
        service.cadastrarDocenteFuncionario(new DocenteFuncionario("D1", "Caio", "843", "F1", "Depto"));
        service.cadastrarDocenteFuncionario(new DocenteFuncionario("D2", "Dina", "844", "F2", "Depto"));
        assertThrows(IllegalArgumentException.class, () -> service.actualizarUtilizador(
                new Estudante("U1", "Ana actualizada", "841", "E2", "Curso")));
        assertThrows(IllegalArgumentException.class, () -> service.actualizarUtilizador(
                new DocenteFuncionario("D1", "Caio actualizado", "843", "F2", "Depto")));
        assertEquals("Ana", service.consultarUtilizador("U1").getNome());
        assertEquals("E1", ((Estudante) service.consultarUtilizador("U1")).getNumeroEstudante());
        assertEquals("F1", ((DocenteFuncionario) service.consultarUtilizador("D1")).getNumeroFuncionario());
    }

    @Test void restauracaoRejeitaNumeroInstitucionalDuplicadoSemAlterarEstadoActivo() {
        service.cadastrarEstudante(new Estudante("U1", "Ana", "841", "E1", "Curso"));
        RepositorioFicheiros.Estado actual = service.exportarEstado();
        var utilizadoresInvalidos = new java.util.ArrayList<>(actual.utilizadores());
        utilizadoresInvalidos.add(new Estudante("U2", "Bia", "842", "E1", "Outro curso"));
        var invalido = new RepositorioFicheiros.Estado(utilizadoresInvalidos, actual.veiculos(), actual.lugares(),
                actual.estacionamentos(), actual.sequenciaEstacionamento());
        assertThrows(IllegalArgumentException.class, () -> service.restaurarEstado(invalido));
        assertEquals(1, service.exportarEstado().utilizadores().size());
        assertEquals("Ana", service.consultarUtilizador("U1").getNome());

        service.cadastrarDocenteFuncionario(new DocenteFuncionario("D1", "Caio", "843", "F1", "Depto"));
        RepositorioFicheiros.Estado comDocente = service.exportarEstado();
        var docentesInvalidos = new java.util.ArrayList<>(comDocente.utilizadores());
        docentesInvalidos.add(new DocenteFuncionario("D2", "Dina", "844", "F1", "Outro depto"));
        var estadoDocenteInvalido = new RepositorioFicheiros.Estado(docentesInvalidos, comDocente.veiculos(),
                comDocente.lugares(), comDocente.estacionamentos(), comDocente.sequenciaEstacionamento());
        assertThrows(IllegalArgumentException.class, () -> service.restaurarEstado(estadoDocenteInvalido));
        assertEquals(2, service.exportarEstado().utilizadores().size());
    }
}
