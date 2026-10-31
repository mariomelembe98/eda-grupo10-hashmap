package mz.edu.eda.persistencia;

import mz.edu.eda.modelo.Estacionamento;
import mz.edu.eda.modelo.DocenteFuncionario;
import mz.edu.eda.modelo.Estudante;
import mz.edu.eda.modelo.LugarEstacionamento;
import mz.edu.eda.modelo.TipoLugar;
import mz.edu.eda.modelo.Utilizador;
import mz.edu.eda.modelo.Veiculo;
import mz.edu.eda.servico.ParqueService;
import mz.edu.eda.ui.MenuConsola;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ParqueServiceAtomicidadeTest {
    @TempDir Path pasta;

    private Clock clock() { return Clock.fixed(Instant.parse("2026-10-31T08:00:00Z"), ZoneOffset.UTC); }

    private ParqueService preparar(Path ficheiro, boolean entradaActiva) throws IOException {
        ParqueService service = new ParqueService(clock(), new RepositorioFicheiros(ficheiro));
        service.cadastrarEstudante(new Estudante("U1", "Ana", "841", "E1", "Curso"));
        service.cadastrarEstudante(new Estudante("U2", "Bia", "842", "E2", "Curso"));
        service.cadastrarVeiculo(new Veiculo("AAA-111", "Toyota", "Corolla", "Branco", "U1"));
        service.cadastrarLugar(new LugarEstacionamento("L1", "Bloco A", TipoLugar.ESTUDANTE));
        service.cadastrarLugar(new LugarEstacionamento("L2", "Bloco B", TipoLugar.ESTUDANTE));
        if (entradaActiva) service.registarEntrada("AAA-111", "L1");
        return service;
    }

    private void verificarFalhaAntesDaSubstituicao(Consumer<ParqueService> operacao, boolean entradaActiva) throws IOException {
        Path ficheiro = pasta.resolve("parque.dat");
        preparar(ficheiro, entradaActiva);
        AtomicBoolean falharEscrita = new AtomicBoolean(true);
        RepositorioFicheiros repoInstavel = new RepositorioFicheiros(ficheiro,
                (temporario, destino) -> Files.move(temporario, destino,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING),
                (temporario, estado) -> {
                    if (falharEscrita.get()) throw new IOException("falha simulada durante escrita");
                    try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temporario))) {
                        out.writeObject(estado);
                    }
                });
        ParqueService service = new ParqueService(clock(), repoInstavel);
        RepositorioFicheiros.Estado antes = service.exportarEstado();
        byte[] ficheiroAntes = Files.readAllBytes(ficheiro);
        int livresAntes = service.quantidadeLugaresLivres();
        int ocupadosAntes = service.quantidadeLugaresOcupados();

        UncheckedIOException erro = assertThrows(UncheckedIOException.class, () -> operacao.accept(service));
        assertInstanceOf(IOException.class, erro.getCause());
        assertEstadoIgual(antes, service.exportarEstado());
        assertArrayEquals(ficheiroAntes, Files.readAllBytes(ficheiro));
        assertEquals(livresAntes, service.quantidadeLugaresLivres());
        assertEquals(ocupadosAntes, service.quantidadeLugaresOcupados());

        falharEscrita.set(false);
        assertDoesNotThrow(() -> operacao.accept(service));
        ParqueService reiniciado = new ParqueService(clock(), new RepositorioFicheiros(ficheiro));
        assertEstadoIgual(service.exportarEstado(), reiniciado.exportarEstado());
    }

    @Test void falhaCadastroUtilizadorNaoAlteraEstadoNemFicheiroEPermiteRepetir() throws IOException {
        verificarFalhaAntesDaSubstituicao(s -> s.cadastrarEstudante(
                new Estudante("U3", "Carlos", "843", "E3", "Curso")), false);
    }

    @Test void falhaCadastroVeiculoNaoAlteraEstadoNemFicheiroEPermiteRepetir() throws IOException {
        verificarFalhaAntesDaSubstituicao(s -> s.cadastrarVeiculo(
                new Veiculo("BBB-222", "Honda", "Fit", "Azul", "U1")), false);
    }

    @Test void falhaCadastroLugarNaoAlteraEstadoNemFicheiroEPermiteRepetir() throws IOException {
        verificarFalhaAntesDaSubstituicao(s -> s.cadastrarLugar(
                new LugarEstacionamento("L3", "Bloco C", TipoLugar.ESTUDANTE)), false);
    }

    @Test void falhaActualizacaoUtilizadorNaoAlteraEstadoNemFicheiroEPermiteRepetir() throws IOException {
        verificarFalhaAntesDaSubstituicao(s -> s.actualizarUtilizador(
                new Estudante("U1", "Ana Maria", "841", "E1", "Curso")), false);
    }

    @Test void falhaActualizacaoVeiculoNaoAlteraEstadoNemFicheiroEPermiteRepetir() throws IOException {
        verificarFalhaAntesDaSubstituicao(s -> s.actualizarVeiculo(
                new Veiculo("AAA-111", "Toyota", "Yaris", "Azul", "U1")), false);
    }

    @Test void falhaActualizacaoLugarNaoAlteraEstadoNemFicheiroEPermiteRepetir() throws IOException {
        verificarFalhaAntesDaSubstituicao(s -> s.actualizarLugar(
                new LugarEstacionamento("L1", "Bloco C", TipoLugar.ESTUDANTE)), false);
    }

    @Test void falhaEntradaPreservaIndicesESequenciaEPermiteRepetir() throws IOException {
        verificarFalhaAntesDaSubstituicao(s -> {
            assertEquals("EST-1", s.registarEntrada("AAA-111", "L1").getIdentificador());
        }, false);
    }

    @Test void falhaSaidaPreservaIndicesEHistoricoEPermiteRepetir() throws IOException {
        verificarFalhaAntesDaSubstituicao(s -> s.registarSaida("AAA-111"), true);
    }

    @Test void falhaNaSubstituicaoBloqueiaMutacoesAteReconciliacao() throws IOException {
        Path ficheiro = pasta.resolve("substituicao.dat");
        preparar(ficheiro, false);
        AtomicBoolean falharSubstituicao = new AtomicBoolean(true);
        RepositorioFicheiros repoInstavel = new RepositorioFicheiros(ficheiro,
                (temporario, destino) -> {
                    if (falharSubstituicao.get()) throw new IOException("falha simulada durante substituicao");
                    Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                });
        ParqueService service = new ParqueService(clock(), repoInstavel);
        RepositorioFicheiros.Estado antes = service.exportarEstado();
        byte[] ficheiroAntes = Files.readAllBytes(ficheiro);
        UncheckedIOException erro = assertThrows(UncheckedIOException.class, () ->
                service.cadastrarEstudante(new Estudante("U3", "Carlos", "843", "E3", "Curso")));
        assertInstanceOf(RepositorioFicheiros.FalhaSubstituicaoException.class, erro.getCause());
        assertEstadoIgual(antes, service.exportarEstado());
        assertArrayEquals(ficheiroAntes, Files.readAllBytes(ficheiro));
        assertThrows(IllegalStateException.class, () -> service.cadastrarLugar(
                new LugarEstacionamento("L3", "Bloco C", TipoLugar.ESTUDANTE)));

        falharSubstituicao.set(false);
        service.reconciliarPersistencia();
        assertEstadoIgual(antes, service.exportarEstado());
        service.cadastrarEstudante(new Estudante("U3", "Carlos", "843", "E3", "Curso"));
        ParqueService reiniciado = new ParqueService(clock(), new RepositorioFicheiros(ficheiro));
        assertEstadoIgual(service.exportarEstado(), reiniciado.exportarEstado());
    }

    @Test void falhaReportadaDepoisDaSubstituicaoRequerReconciliacaoSemPrometerRollback() throws IOException {
        Path ficheiro = pasta.resolve("substituido-antes-da-excepcao.dat");
        preparar(ficheiro, false);
        AtomicBoolean falharDepoisDeMover = new AtomicBoolean(true);
        RepositorioFicheiros repo = new RepositorioFicheiros(ficheiro,
                (temporario, destino) -> {
                    Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    if (falharDepoisDeMover.get()) throw new IOException("falha reportada apos substituicao");
                });
        ParqueService service = new ParqueService(clock(), repo);
        RepositorioFicheiros.Estado antes = service.exportarEstado();
        assertThrows(UncheckedIOException.class, () -> service.cadastrarEstudante(
                new Estudante("U3", "Carlos", "843", "E3", "Curso")));
        assertEstadoIgual(antes, service.exportarEstado());
        assertThrows(IllegalStateException.class, () -> service.cadastrarLugar(
                new LugarEstacionamento("L3", "Bloco C", TipoLugar.ESTUDANTE)));

        falharDepoisDeMover.set(false);
        service.reconciliarPersistencia();
        assertEquals("Carlos", service.consultarUtilizador("U3").getNome());
        assertEquals(antes.sequenciaEstacionamento(), service.exportarEstado().sequenciaEstacionamento());
    }

    @Test void menuReconciliaServicoBloqueado() throws IOException {
        Path ficheiro = pasta.resolve("menu-reconciliar.dat");
        preparar(ficheiro, false);
        AtomicBoolean falhar = new AtomicBoolean(true);
        RepositorioFicheiros repo = new RepositorioFicheiros(ficheiro,
                (temporario, destino) -> {
                    if (falhar.get()) throw new IOException("falha simulada");
                    Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                });
        ParqueService service = new ParqueService(clock(), repo);
        assertThrows(UncheckedIOException.class, () -> service.cadastrarEstudante(
                new Estudante("U3", "Carlos", "843", "E3", "Curso")));
        falhar.set(false);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(buffer); Scanner in = new Scanner(new StringReader("14\n0\n"))) {
            new MenuConsola(service, in, out).executar();
        }
        assertTrue(buffer.toString().contains("Persistencia reconciliada com sucesso."));
        assertThrows(IllegalArgumentException.class, () -> service.consultarUtilizador("U3"));
    }

    @Test void menuMostraFalhaDePersistenciaSemStackTrace() throws IOException {
        Path ficheiro = pasta.resolve("menu-falha.dat");
        AtomicBoolean falhar = new AtomicBoolean(true);
        RepositorioFicheiros repo = new RepositorioFicheiros(ficheiro,
                (temporario, destino) -> Files.move(temporario, destino,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING),
                (temporario, estado) -> {
                    if (falhar.get()) throw new IOException("disco indisponivel");
                    try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temporario))) {
                        out.writeObject(estado);
                    }
                });
        ParqueService service = new ParqueService(clock(), repo);
        String comandos = "1\n1\nU1\nAna\n841\nE1\nCurso\n0\n";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(buffer); Scanner in = new Scanner(new StringReader(comandos))) {
            new MenuConsola(service, in, out).executar();
        }
        String output = buffer.toString();
        assertTrue(output.contains("Falha de persistencia: disco indisponivel"));
        assertTrue(output.contains("escolha 14 para reconciliar"));
        assertFalse(output.contains("java.io."));
        assertTrue(service.exportarEstado().utilizadores().isEmpty());
    }

    @Test void falhaAoCriarTemporarioNaoMudaMemoriaEPermiteRepetir() throws IOException {
        Path ficheiro = pasta.resolve("parque.dat");
        AtomicBoolean falharTemporario = new AtomicBoolean(true);
        RepositorioFicheiros repo = new RepositorioFicheiros(ficheiro,
                (temporario, destino) -> Files.move(temporario, destino,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING),
                (temporario, estado) -> {
                    try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temporario))) {
                        out.writeObject(estado);
                    }
                },
                (directorio, prefixo, sufixo) -> {
                    if (falharTemporario.get()) throw new IOException("falha simulada ao criar temporario");
                    return Files.createTempFile(directorio, prefixo, sufixo);
                });
        ParqueService service = new ParqueService(clock(), repo);
        RepositorioFicheiros.Estado antes = service.exportarEstado();
        assertThrows(UncheckedIOException.class, () -> service.cadastrarEstudante(
                new Estudante("U1", "Ana", "841", "E1", "Curso")));
        assertEstadoIgual(antes, service.exportarEstado());
        assertFalse(Files.exists(ficheiro));
        falharTemporario.set(false);
        service.cadastrarEstudante(new Estudante("U1", "Ana", "841", "E1", "Curso"));
        assertEquals("U1", new ParqueService(clock(), new RepositorioFicheiros(ficheiro)).consultarUtilizador("U1").getId());
    }

    private static void assertEstadoIgual(RepositorioFicheiros.Estado esperado, RepositorioFicheiros.Estado real) {
        assertEquals(esperado.utilizadores().stream().map(u -> u.getClass().getSimpleName() + "|" + u.getId()
                + "|" + u.getNome() + "|" + u.getContacto()
                + (u instanceof Estudante e ? "|" + e.getNumeroEstudante() + "|" + e.getCurso()
                : "|" + ((DocenteFuncionario) u).getNumeroFuncionario() + "|" + ((DocenteFuncionario) u).getDepartamento()))
                .sorted().toList(), real.utilizadores().stream().map(u -> u.getClass().getSimpleName() + "|" + u.getId()
                + "|" + u.getNome() + "|" + u.getContacto()
                + (u instanceof Estudante e ? "|" + e.getNumeroEstudante() + "|" + e.getCurso()
                : "|" + ((DocenteFuncionario) u).getNumeroFuncionario() + "|" + ((DocenteFuncionario) u).getDepartamento()))
                .sorted().toList());
        assertEquals(esperado.veiculos().stream().map(v -> v.getMatricula() + "|" + v.getMarca() + "|" + v.getModelo()
                + "|" + v.getCor() + "|" + v.getIdProprietario()).sorted().toList(), real.veiculos().stream()
                .map(v -> v.getMatricula() + "|" + v.getMarca() + "|" + v.getModelo() + "|" + v.getCor()
                        + "|" + v.getIdProprietario()).sorted().toList());
        assertEquals(esperado.lugares().stream().map(l -> l.getNumero() + "|" + l.getLocalizacao() + "|" + l.getTipo())
                .sorted().toList(), real.lugares().stream().map(l -> l.getNumero() + "|" + l.getLocalizacao()
                        + "|" + l.getTipo()).sorted().toList());
        assertEquals(esperado.estacionamentos().stream().map(ParqueServiceAtomicidadeTest::estacionamentoComoTexto)
                .sorted().toList(), real.estacionamentos().stream().map(ParqueServiceAtomicidadeTest::estacionamentoComoTexto)
                .sorted().toList());
        assertEquals(esperado.sequenciaEstacionamento(), real.sequenciaEstacionamento());
    }

    private static String estacionamentoComoTexto(Estacionamento e) {
        return e.getIdentificador() + "|" + e.getMatricula() + "|" + e.getIdProprietario() + "|"
                + e.getNumeroLugar() + "|" + e.getDataHoraEntrada() + "|" + e.getDataHoraSaida();
    }
}
