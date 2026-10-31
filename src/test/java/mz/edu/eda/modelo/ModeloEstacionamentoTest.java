package mz.edu.eda.modelo;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ModeloEstacionamentoTest {
    @Test void criaEstudanteComTipoEIdentificacao() {
        Estudante estudante = new Estudante("U1", " Ana ", "841234567", "E10", "Informatica");
        assertEquals("U1", estudante.getId());
        assertEquals("Ana", estudante.getNome());
        assertEquals("E10", estudante.getNumeroEstudante());
        assertEquals(TipoUtilizador.ESTUDANTE, estudante.getTipo());
        assertTrue(estudante instanceof java.io.Serializable);
    }

    @Test void criaDocenteFuncionarioComTipoEIdentificacao() {
        DocenteFuncionario docente = new DocenteFuncionario("U2", "Bia", "821234567", "F20", "Engenharia");
        assertEquals("F20", docente.getNumeroFuncionario());
        assertEquals("Engenharia", docente.getDepartamento());
        assertEquals(TipoUtilizador.DOCENTE_FUNCIONARIO, docente.getTipo());
    }

    @Test void veiculoMantemIdDoProprietario() {
        Veiculo veiculo = new Veiculo("ABC-123-MP", "Toyota", "Corolla", "Branco", "U1");
        assertEquals("U1", veiculo.getIdProprietario());
        assertEquals("ABC-123-MP", veiculo.getMatricula());
    }

    @Test void criaLugarComTipo() {
        LugarEstacionamento lugar = new LugarEstacionamento("E01", "Bloco A", TipoLugar.ESTUDANTE);
        assertEquals("E01", lugar.getNumero());
        assertEquals(TipoLugar.ESTUDANTE, lugar.getTipo());
    }

    @Test void registaEstacionamentoActivoEEncerrado() {
        LocalDateTime entrada = LocalDateTime.of(2026, 10, 31, 8, 0);
        Estacionamento activo = new Estacionamento("P1", "ABC-123-MP", "U1", "E01", entrada, null);
        Estacionamento encerrado = new Estacionamento("P2", "ABC-123-MP", "U1", "E01", entrada, entrada.plusHours(2));
        assertTrue(activo.estaActivo());
        assertFalse(encerrado.estaActivo());
        assertEquals("P1", activo.getIdentificador());
    }

    @Test void rejeitaCamposObrigatoriosNulosOuEmBranco() {
        assertThrows(NullPointerException.class, () -> new Estudante(null, "Ana", "841", "E1", "Curso"));
        assertThrows(IllegalArgumentException.class, () -> new Estudante("U1", " ", "841", "E1", "Curso"));
        assertThrows(NullPointerException.class, () -> new DocenteFuncionario("U1", "Bia", "841", null, "Depto"));
        assertThrows(IllegalArgumentException.class, () -> new Veiculo(" ", "Toyota", "Yaris", "Azul", "U1"));
        assertThrows(NullPointerException.class, () -> new LugarEstacionamento("L1", "Bloco A", null));
        assertThrows(NullPointerException.class, () -> new Estacionamento("P1", "ABC", "U1", "L1", null, null));
    }

    @Test void rejeitaDataDeSaidaAnteriorAEntrada() {
        LocalDateTime entrada = LocalDateTime.of(2026, 10, 31, 10, 0);
        assertThrows(IllegalArgumentException.class, () ->
                new Estacionamento("P1", "ABC-123-MP", "U1", "E01", entrada, entrada.minusSeconds(1)));
    }
}
