package mz.edu.eda.persistencia;

import mz.edu.eda.modelo.Estacionamento;
import mz.edu.eda.modelo.Estudante;
import mz.edu.eda.modelo.LugarEstacionamento;
import mz.edu.eda.modelo.TipoLugar;
import mz.edu.eda.modelo.Veiculo;
import mz.edu.eda.servico.ParqueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioFicheirosTest {
    @TempDir Path pasta;

    private Clock clock() { return Clock.fixed(Instant.parse("2026-10-31T08:00:00Z"), ZoneOffset.UTC); }
    private void preparar(ParqueService service) {
        service.cadastrarEstudante(new Estudante("U1", "Ana", "841234567", "E1", "Informatica"));
        service.cadastrarVeiculo(new Veiculo("AAA-111", "Toyota", "Corolla", "Branco", "U1"));
        service.cadastrarLugar(new LugarEstacionamento("L1", "Bloco A", TipoLugar.ESTUDANTE));
    }

    @Test void persisteTodosOsRegistosEReconstroiIndicesDepoisDoReinicio() throws IOException {
        RepositorioFicheiros repo = new RepositorioFicheiros(pasta.resolve("parque.dat"));
        ParqueService primeiro = new ParqueService(clock(), repo);
        preparar(primeiro);
        primeiro.registarEntrada("AAA-111", "L1");

        ParqueService reiniciado = new ParqueService(clock(), repo);
        assertEquals("Ana", reiniciado.consultarUtilizador("U1").getNome());
        assertEquals("Toyota", reiniciado.consultarVeiculo("AAA-111").getMarca());
        assertEquals("Bloco A", reiniciado.consultarLugar("L1").getLocalizacao());
        assertEquals(1, reiniciado.consultarHistorico().size());
        assertTrue(reiniciado.consultarHistorico().getFirst().estaActivo());
        assertEquals(1, reiniciado.quantidadeLugaresOcupados());
        assertEquals(0, reiniciado.quantidadeLugaresLivres());
        assertThrows(IllegalArgumentException.class, () -> reiniciado.registarEntrada("AAA-111", "L1"));
        assertThrows(IllegalArgumentException.class, () -> reiniciado.registarEntrada("AAA-111", "L1"));
    }

    @Test void preservaHistoricoConcluidoEActivoESequencia() throws IOException {
        RepositorioFicheiros repo = new RepositorioFicheiros(pasta.resolve("parque.dat"));
        ParqueService service = new ParqueService(clock(), repo);
        service.cadastrarEstudante(new Estudante("U1", "Ana", "841", "E1", "Curso"));
        service.cadastrarVeiculo(new Veiculo("AAA-111", "Toyota", "Corolla", "Branco", "U1"));
        service.cadastrarLugar(new LugarEstacionamento("L1", "Bloco A", TipoLugar.ESTUDANTE));
        service.cadastrarLugar(new LugarEstacionamento("L2", "Bloco B", TipoLugar.ESTUDANTE));
        service.registarEntrada("AAA-111", "L1");
        service.registarSaida("AAA-111");
        Estacionamento activo = service.registarEntrada("AAA-111", "L2");

        ParqueService reiniciado = new ParqueService(clock(), repo);
        assertEquals(2, reiniciado.consultarHistorico().size());
        assertFalse(reiniciado.consultarHistorico().getFirst().estaActivo());
        assertTrue(reiniciado.consultarHistorico().getLast().estaActivo());
        assertEquals("L2", activo.getNumeroLugar());
        assertEquals("EST-2", reiniciado.registarSaida("AAA-111").getIdentificador());
    }

    @Test void ficheiroInexistenteProduzServicoVazio() throws IOException {
        ParqueService service = new ParqueService(clock(), new RepositorioFicheiros(pasta.resolve("ausente.dat")));
        assertTrue(service.consultarHistorico().isEmpty());
        assertEquals(0, service.quantidadeLugaresLivres());
        preparar(service);
        assertTrue(Files.exists(pasta.resolve("ausente.dat")));
    }

    @Test void rejeitaFicheiroCorrompidoOuObjectoDeTipoInesperado() throws IOException {
        Path ficheiro = pasta.resolve("corrompido.dat");
        Files.write(ficheiro, new byte[]{1, 2, 3, 4});
        RepositorioFicheiros repo = new RepositorioFicheiros(ficheiro);
        assertThrows(IOException.class, repo::carregar);
        assertThrows(IOException.class, () -> new ParqueService(clock(), repo));
    }

    @Test void falhaNaSubstituicaoMantemFicheiroAnteriorIntacto() throws IOException {
        Path destino = pasta.resolve("parque.dat");
        RepositorioFicheiros operacional = new RepositorioFicheiros(destino);
        operacional.guardar(RepositorioFicheiros.Estado.vazio());
        byte[] conteudoAnterior = Files.readAllBytes(destino);
        RepositorioFicheiros comFalhaNaSubstituicao = new RepositorioFicheiros(destino,
                (temporario, alvo) -> { throw new IOException("falha simulada na substituicao"); });
        assertThrows(IOException.class, () -> comFalhaNaSubstituicao.guardar(RepositorioFicheiros.Estado.vazio()));
        assertArrayEquals(conteudoAnterior, Files.readAllBytes(destino));
    }

    @Test void estadoInconsistenteNaoEActivado() throws IOException {
        RepositorioFicheiros repo = new RepositorioFicheiros(pasta.resolve("parque.dat"));
        ParqueService service = new ParqueService(clock(), repo);
        preparar(service);
        RepositorioFicheiros.Estado invalido = new RepositorioFicheiros.Estado(
                service.exportarEstado().utilizadores(), service.exportarEstado().veiculos(),
                service.exportarEstado().lugares(),
                java.util.List.of(new Estacionamento("P1", "INEXISTENTE", "U1", "L1",
                        java.time.LocalDateTime.now(clock()), null)), 1);
        assertThrows(IllegalArgumentException.class, () -> service.restaurarEstado(invalido));
        assertEquals("AAA-111", service.consultarVeiculo("AAA-111").getMatricula());
    }
}
