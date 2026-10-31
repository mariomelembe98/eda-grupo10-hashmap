package mz.edu.eda.servico;

import mz.edu.eda.modelo.DocenteFuncionario;
import mz.edu.eda.modelo.Estacionamento;
import mz.edu.eda.modelo.Estudante;
import mz.edu.eda.modelo.LugarEstacionamento;
import mz.edu.eda.modelo.TipoLugar;
import mz.edu.eda.modelo.TipoUtilizador;
import mz.edu.eda.modelo.Utilizador;
import mz.edu.eda.modelo.Veiculo;
import mz.edu.eda.persistencia.RepositorioFicheiros;
import mz.edu.eda.tad.MeuHashMap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Serviço de aplicação para o registo e operação do parque de estacionamento. */
public final class ParqueService {
    private MeuHashMap<String, Utilizador> utilizadores = new MeuHashMap<>();
    private MeuHashMap<String, Veiculo> veiculos = new MeuHashMap<>();
    private MeuHashMap<String, LugarEstacionamento> lugares = new MeuHashMap<>();
    private MeuHashMap<String, Estacionamento> estacionamentos = new MeuHashMap<>();
    private MeuHashMap<String, String> estacionamentoActivoPorMatricula = new MeuHashMap<>();
    private MeuHashMap<String, String> matriculaPorLugarOcupado = new MeuHashMap<>();
    private final Clock clock;
    private final RepositorioFicheiros repositorio;
    private long sequenciaEstacionamento;
    private boolean persistenciaIndeterminada;

    public ParqueService() { this(Clock.systemDefaultZone(), null, (RepositorioFicheiros.Estado) null); }

    public ParqueService(Clock clock) {
        this(clock, null, (RepositorioFicheiros.Estado) null);
    }

    public ParqueService(RepositorioFicheiros repositorio) throws IOException {
        this(Clock.systemDefaultZone(), validarRepositorio(repositorio), carregar(repositorio));
    }

    public ParqueService(Clock clock, RepositorioFicheiros repositorio) throws IOException {
        this(clock, validarRepositorio(repositorio), carregar(repositorio));
    }

    private ParqueService(Clock clock, RepositorioFicheiros repositorio, RepositorioFicheiros.Estado estado) {
        this.clock = Objects.requireNonNull(clock, "Clock nao pode ser nulo");
        this.repositorio = repositorio;
        if (estado != null) aplicarEstadoValidado(estado);
    }

    private static RepositorioFicheiros validarRepositorio(RepositorioFicheiros repositorio) {
        return Objects.requireNonNull(repositorio, "Repositorio nao pode ser nulo");
    }

    private static RepositorioFicheiros.Estado carregar(RepositorioFicheiros repositorio) throws IOException {
        return validarRepositorio(repositorio).carregar();
    }

    public void cadastrarEstudante(Estudante estudante) { cadastrarUtilizador(estudante); }
    public void cadastrarDocenteFuncionario(DocenteFuncionario docente) { cadastrarUtilizador(docente); }

    private void cadastrarUtilizador(Utilizador utilizador) {
        garantirEstadoPersistido();
        Objects.requireNonNull(utilizador, "Utilizador nao pode ser nulo");
        if (utilizadores.contemChave(utilizador.getId()))
            throw new IllegalArgumentException("ID de utilizador ja registado: " + utilizador.getId());
        validarNumeroInstitucional(utilizador, utilizadores);
        RepositorioFicheiros.Estado actual = exportarEstado();
        List<Utilizador> novos = new ArrayList<>(actual.utilizadores());
        novos.add(utilizador);
        confirmarEstado(new RepositorioFicheiros.Estado(novos, actual.veiculos(), actual.lugares(),
                actual.estacionamentos(), sequenciaEstacionamento));
    }

    public void cadastrarVeiculo(Veiculo veiculo) {
        garantirEstadoPersistido();
        Objects.requireNonNull(veiculo, "Veiculo nao pode ser nulo");
        if (!utilizadores.contemChave(veiculo.getIdProprietario()))
            throw new IllegalArgumentException("Proprietario nao registado: " + veiculo.getIdProprietario());
        if (veiculos.contemChave(veiculo.getMatricula()))
            throw new IllegalArgumentException("Matricula ja registada: " + veiculo.getMatricula());
        RepositorioFicheiros.Estado actual = exportarEstado();
        List<Veiculo> novos = new ArrayList<>(actual.veiculos());
        novos.add(veiculo);
        confirmarEstado(new RepositorioFicheiros.Estado(actual.utilizadores(), novos, actual.lugares(),
                actual.estacionamentos(), sequenciaEstacionamento));
    }

    public void cadastrarLugar(LugarEstacionamento lugar) {
        garantirEstadoPersistido();
        Objects.requireNonNull(lugar, "Lugar nao pode ser nulo");
        if (lugares.contemChave(lugar.getNumero()))
            throw new IllegalArgumentException("Numero de lugar ja registado: " + lugar.getNumero());
        RepositorioFicheiros.Estado actual = exportarEstado();
        List<LugarEstacionamento> novos = new ArrayList<>(actual.lugares());
        novos.add(lugar);
        confirmarEstado(new RepositorioFicheiros.Estado(actual.utilizadores(), actual.veiculos(), novos,
                actual.estacionamentos(), sequenciaEstacionamento));
    }

    public void actualizarUtilizador(Utilizador actualizado) {
        garantirEstadoPersistido();
        Objects.requireNonNull(actualizado, "Utilizador nao pode ser nulo");
        Utilizador existente = exigir(utilizadores.obter(actualizado.getId()), "Utilizador nao encontrado: " + actualizado.getId());
        if (existente.getTipo() != actualizado.getTipo())
            throw new IllegalArgumentException("Nao e permitido alterar o tipo do utilizador");
        validarNumeroInstitucional(actualizado, utilizadores);
        if (!utilizadorCompatívelComEstacionamentos(actualizado))
            throw new IllegalArgumentException("A actualizacao tornaria incompatível um estacionamento activo");
        RepositorioFicheiros.Estado actual = exportarEstado();
        List<Utilizador> novos = substituirPorId(actual.utilizadores(), Utilizador::getId, actualizado);
        confirmarEstado(new RepositorioFicheiros.Estado(novos, actual.veiculos(), actual.lugares(),
                actual.estacionamentos(), sequenciaEstacionamento));
    }

    private boolean utilizadorCompatívelComEstacionamentos(Utilizador utilizador) {
        for (String matricula : veiculos.chaves()) {
            Veiculo veiculo = veiculos.obter(matricula);
            if (veiculo.getIdProprietario().equals(utilizador.getId())
                    && estacionamentoActivoPorMatricula.contemChave(matricula)) {
                Estacionamento activo = estacionamentos.obter(estacionamentoActivoPorMatricula.obter(matricula));
                LugarEstacionamento lugar = lugares.obter(activo.getNumeroLugar());
                if (!compatível(utilizador.getTipo(), lugar.getTipo())) return false;
            }
        }
        return true;
    }

    public void actualizarVeiculo(Veiculo actualizado) {
        garantirEstadoPersistido();
        Objects.requireNonNull(actualizado, "Veiculo nao pode ser nulo");
        Veiculo existente = exigir(veiculos.obter(actualizado.getMatricula()), "Veiculo nao encontrado: " + actualizado.getMatricula());
        if (!utilizadores.contemChave(actualizado.getIdProprietario()))
            throw new IllegalArgumentException("Proprietario nao registado: " + actualizado.getIdProprietario());
        if (estacionamentoActivoPorMatricula.contemChave(existente.getMatricula())
                && !existente.getIdProprietario().equals(actualizado.getIdProprietario()))
            throw new IllegalArgumentException("Nao e permitido transferir um veiculo com estacionamento activo");
        RepositorioFicheiros.Estado actual = exportarEstado();
        List<Veiculo> novos = substituirPorId(actual.veiculos(), Veiculo::getMatricula, actualizado);
        confirmarEstado(new RepositorioFicheiros.Estado(actual.utilizadores(), novos, actual.lugares(),
                actual.estacionamentos(), sequenciaEstacionamento));
    }

    public void actualizarLugar(LugarEstacionamento actualizado) {
        garantirEstadoPersistido();
        Objects.requireNonNull(actualizado, "Lugar nao pode ser nulo");
        exigir(lugares.obter(actualizado.getNumero()), "Lugar nao encontrado: " + actualizado.getNumero());
        String matricula = matriculaPorLugarOcupado.obter(actualizado.getNumero());
        if (matricula != null) {
            Veiculo veiculo = veiculos.obter(matricula);
            Utilizador proprietario = utilizadores.obter(veiculo.getIdProprietario());
            if (!compatível(proprietario.getTipo(), actualizado.getTipo()))
                throw new IllegalArgumentException("O novo tipo de lugar e incompatível com o veiculo estacionado");
        }
        RepositorioFicheiros.Estado actual = exportarEstado();
        List<LugarEstacionamento> novos = substituirPorId(actual.lugares(), LugarEstacionamento::getNumero, actualizado);
        confirmarEstado(new RepositorioFicheiros.Estado(actual.utilizadores(), actual.veiculos(), novos,
                actual.estacionamentos(), sequenciaEstacionamento));
    }

    public List<Veiculo> listarVeiculosDeEstudantes() { return listarVeiculosPorTipo(TipoUtilizador.ESTUDANTE); }
    public List<Veiculo> listarVeiculosDeDocentesFuncionarios() { return listarVeiculosPorTipo(TipoUtilizador.DOCENTE_FUNCIONARIO); }

    private List<Veiculo> listarVeiculosPorTipo(TipoUtilizador tipo) {
        List<Veiculo> resultado = new ArrayList<>();
        for (String matricula : veiculos.chaves()) {
            Veiculo veiculo = veiculos.obter(matricula);
            Utilizador dono = utilizadores.obter(veiculo.getIdProprietario());
            if (dono.getTipo() == tipo) resultado.add(veiculo);
        }
        resultado.sort(Comparator.comparing(Veiculo::getMatricula));
        return List.copyOf(resultado);
    }

    public Estacionamento registarEntrada(String matricula, String numeroLugar) {
        garantirEstadoPersistido();
        exigirTexto(matricula, "Matricula");
        exigirTexto(numeroLugar, "Numero de lugar");
        Veiculo veiculo = exigir(veiculos.obter(matricula), "Veiculo nao registado: " + matricula);
        LugarEstacionamento lugar = exigir(lugares.obter(numeroLugar), "Lugar nao registado: " + numeroLugar);
        if (estacionamentoActivoPorMatricula.contemChave(matricula))
            throw new IllegalArgumentException("Veiculo ja tem estacionamento activo: " + matricula);
        if (matriculaPorLugarOcupado.contemChave(numeroLugar))
            throw new IllegalArgumentException("Lugar ja esta ocupado: " + numeroLugar);
        Utilizador proprietario = utilizadores.obter(veiculo.getIdProprietario());
        if (!compatível(proprietario.getTipo(), lugar.getTipo()))
            throw new IllegalArgumentException("Tipo de utilizador incompatível com o lugar");
        LocalDateTime agora = LocalDateTime.now(clock);
        long novaSequencia = sequenciaEstacionamento + 1;
        String id = "EST-" + novaSequencia;
        Estacionamento registo = new Estacionamento(id, matricula, veiculo.getIdProprietario(), numeroLugar, agora, null);
        RepositorioFicheiros.Estado actual = exportarEstado();
        List<Estacionamento> novos = new ArrayList<>(actual.estacionamentos());
        novos.add(registo);
        confirmarEstado(new RepositorioFicheiros.Estado(actual.utilizadores(), actual.veiculos(), actual.lugares(),
                novos, novaSequencia));
        return registo;
    }

    public Estacionamento registarSaida(String matricula) {
        garantirEstadoPersistido();
        exigirTexto(matricula, "Matricula");
        String id = estacionamentoActivoPorMatricula.obter(matricula);
        if (id == null) throw new IllegalArgumentException("Veiculo sem estacionamento activo: " + matricula);
        Estacionamento activo = estacionamentos.obter(id);
        LocalDateTime saida = LocalDateTime.now(clock);
        if (saida.isBefore(activo.getDataHoraEntrada()))
            throw new IllegalStateException("Clock produziu uma data anterior a entrada");
        Estacionamento concluido = new Estacionamento(activo.getIdentificador(), activo.getMatricula(),
                activo.getIdProprietario(), activo.getNumeroLugar(), activo.getDataHoraEntrada(), saida);
        RepositorioFicheiros.Estado actual = exportarEstado();
        List<Estacionamento> novos = substituirPorId(actual.estacionamentos(), Estacionamento::getIdentificador, concluido);
        confirmarEstado(new RepositorioFicheiros.Estado(actual.utilizadores(), actual.veiculos(), actual.lugares(),
                novos, sequenciaEstacionamento));
        return concluido;
    }

    public List<Veiculo> listarVeiculosEstacionados() {
        List<Veiculo> resultado = new ArrayList<>();
        for (String matricula : estacionamentoActivoPorMatricula.chaves()) resultado.add(veiculos.obter(matricula));
        resultado.sort(Comparator.comparing(Veiculo::getMatricula));
        return List.copyOf(resultado);
    }

    public List<Estacionamento> consultarHistorico() { return listarEstacionamentos(estacionamentos.chaves()); }

    public List<Estacionamento> consultarEstacionamentosPorUtilizador(String idUtilizador) {
        exigirTexto(idUtilizador, "ID do utilizador");
        if (!utilizadores.contemChave(idUtilizador)) throw new IllegalArgumentException("Utilizador nao encontrado: " + idUtilizador);
        List<String> ids = new ArrayList<>();
        for (String id : estacionamentos.chaves())
            if (estacionamentos.obter(id).getIdProprietario().equals(idUtilizador)) ids.add(id);
        return listarEstacionamentos(ids);
    }

    private List<Estacionamento> listarEstacionamentos(List<String> ids) {
        List<Estacionamento> resultado = new ArrayList<>();
        for (String id : ids) resultado.add(estacionamentos.obter(id));
        resultado.sort(Comparator.comparing(Estacionamento::getDataHoraEntrada).thenComparing(Estacionamento::getIdentificador));
        return List.copyOf(resultado);
    }

    public int quantidadeLugaresLivres() { return lugares.tamanho() - matriculaPorLugarOcupado.tamanho(); }
    public int quantidadeLugaresOcupados() { return matriculaPorLugarOcupado.tamanho(); }

    public Utilizador consultarUtilizador(String id) { return exigir(utilizadores.obter(id), "Utilizador nao encontrado: " + id); }
    public Veiculo consultarVeiculo(String matricula) { return exigir(veiculos.obter(matricula), "Veiculo nao encontrado: " + matricula); }
    public LugarEstacionamento consultarLugar(String numero) { return exigir(lugares.obter(numero), "Lugar nao encontrado: " + numero); }

    /** Exporta um snapshot independente dos índices derivados e do TAD usado internamente. */
    public RepositorioFicheiros.Estado exportarEstado() {
        return new RepositorioFicheiros.Estado(valores(utilizadores), valores(veiculos), valores(lugares),
                valores(estacionamentos), sequenciaEstacionamento);
    }

    /** Valida o snapshot integralmente antes de substituir os registos e reconstruir os índices. */
    public void restaurarEstado(RepositorioFicheiros.Estado estado) {
        garantirEstadoPersistido();
        confirmarEstado(Objects.requireNonNull(estado, "Estado nao pode ser nulo"));
    }

    private void aplicarEstadoValidado(RepositorioFicheiros.Estado estado) {
        Objects.requireNonNull(estado, "Estado nao pode ser nulo");
        MeuHashMap<String, Utilizador> novosUtilizadores = new MeuHashMap<>();
        MeuHashMap<String, Veiculo> novosVeiculos = new MeuHashMap<>();
        MeuHashMap<String, LugarEstacionamento> novosLugares = new MeuHashMap<>();
        MeuHashMap<String, Estacionamento> novosEstacionamentos = new MeuHashMap<>();
        MeuHashMap<String, String> novosActivos = new MeuHashMap<>();
        MeuHashMap<String, String> novosOcupados = new MeuHashMap<>();
        for (Utilizador utilizador : estado.utilizadores()) {
            exigir(utilizador, "Utilizador nulo no estado");
            validarNumeroInstitucional(utilizador, novosUtilizadores);
            if (novosUtilizadores.colocar(utilizador.getId(), utilizador) != null)
                throw new IllegalArgumentException("ID de utilizador duplicado no estado");
        }
        for (Veiculo veiculo : estado.veiculos()) {
            exigir(veiculo, "Veiculo nulo no estado");
            if (!novosUtilizadores.contemChave(veiculo.getIdProprietario()))
                throw new IllegalArgumentException("Veiculo com proprietario inexistente no estado");
            if (novosVeiculos.colocar(veiculo.getMatricula(), veiculo) != null)
                throw new IllegalArgumentException("Matricula duplicada no estado");
        }
        for (LugarEstacionamento lugar : estado.lugares()) {
            exigir(lugar, "Lugar nulo no estado");
            if (novosLugares.colocar(lugar.getNumero(), lugar) != null)
                throw new IllegalArgumentException("Numero de lugar duplicado no estado");
        }
        long maiorIdentificador = 0;
        for (Estacionamento estacionamento : estado.estacionamentos()) {
            exigir(estacionamento, "Estacionamento nulo no estado");
            if (!novosVeiculos.contemChave(estacionamento.getMatricula())
                    || !novosUtilizadores.contemChave(estacionamento.getIdProprietario())
                    || !novosLugares.contemChave(estacionamento.getNumeroLugar()))
                throw new IllegalArgumentException("Estacionamento com referencia inexistente no estado");
            if (novosEstacionamentos.colocar(estacionamento.getIdentificador(), estacionamento) != null)
                throw new IllegalArgumentException("Identificador de estacionamento duplicado no estado");
            if (estacionamento.getIdentificador().startsWith("EST-")) {
                try { maiorIdentificador = Math.max(maiorIdentificador, Long.parseLong(estacionamento.getIdentificador().substring(4))); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Identificador de estacionamento invalido", e); }
            }
            if (estacionamento.estaActivo()) {
                Veiculo veiculo = novosVeiculos.obter(estacionamento.getMatricula());
                LugarEstacionamento lugar = novosLugares.obter(estacionamento.getNumeroLugar());
                Utilizador donoActual = novosUtilizadores.obter(veiculo.getIdProprietario());
                if (!veiculo.getIdProprietario().equals(estacionamento.getIdProprietario())
                        || !compatível(donoActual.getTipo(), lugar.getTipo()))
                    throw new IllegalArgumentException("Estacionamento activo inconsistente no estado");
                if (novosActivos.colocar(estacionamento.getMatricula(), estacionamento.getIdentificador()) != null)
                    throw new IllegalArgumentException("Veiculo com mais de um estacionamento activo");
                if (novosOcupados.colocar(estacionamento.getNumeroLugar(), estacionamento.getMatricula()) != null)
                    throw new IllegalArgumentException("Lugar ocupado por mais de um estacionamento activo");
            }
        }
        if (estado.sequenciaEstacionamento() < maiorIdentificador)
            throw new IllegalArgumentException("Sequencia de estacionamentos inconsistente");
        utilizadores = novosUtilizadores;
        veiculos = novosVeiculos;
        lugares = novosLugares;
        estacionamentos = novosEstacionamentos;
        estacionamentoActivoPorMatricula = novosActivos;
        matriculaPorLugarOcupado = novosOcupados;
        sequenciaEstacionamento = estado.sequenciaEstacionamento();
    }

    private void confirmarEstado(RepositorioFicheiros.Estado candidato) {
        ParqueService preparado = new ParqueService(clock);
        preparado.aplicarEstadoValidado(candidato);
        if (repositorio != null) {
            try { repositorio.guardar(candidato); }
            catch (RepositorioFicheiros.FalhaSubstituicaoException e) {
                persistenciaIndeterminada = true;
                throw new UncheckedIOException("Resultado da substituicao do ficheiro e indeterminado; reconcilie o servico", e);
            } catch (IOException e) {
                throw new UncheckedIOException("Falha ao guardar os dados do parque", e);
            }
        }
        activar(preparado);
    }

    private void activar(ParqueService preparado) {
        utilizadores = preparado.utilizadores;
        veiculos = preparado.veiculos;
        lugares = preparado.lugares;
        estacionamentos = preparado.estacionamentos;
        estacionamentoActivoPorMatricula = preparado.estacionamentoActivoPorMatricula;
        matriculaPorLugarOcupado = preparado.matriculaPorLugarOcupado;
        sequenciaEstacionamento = preparado.sequenciaEstacionamento;
    }

    /** Recarrega o ficheiro depois de uma falha cuja substituição possa ter sido concluída. */
    public void reconciliarPersistencia() throws IOException {
        if (repositorio == null) throw new IllegalStateException("Servico sem repositorio configurado");
        RepositorioFicheiros.Estado estado = repositorio.carregar();
        ParqueService preparado = new ParqueService(clock);
        preparado.aplicarEstadoValidado(estado);
        activar(preparado);
        persistenciaIndeterminada = false;
    }

    private void garantirEstadoPersistido() {
        if (persistenciaIndeterminada)
            throw new IllegalStateException("Persistencia indeterminada; execute reconciliarPersistencia antes de novas mutacoes");
    }

    private static <K, V> List<V> valores(MeuHashMap<K, V> mapa) {
        List<V> resultado = new ArrayList<>(mapa.tamanho());
        for (K chave : mapa.chaves()) resultado.add(mapa.obter(chave));
        return resultado;
    }

    private static <T> List<T> substituirPorId(List<T> elementos, java.util.function.Function<T, String> id,
                                               T actualizado) {
        List<T> resultado = new ArrayList<>(elementos.size());
        boolean encontrado = false;
        for (T elemento : elementos) {
            if (id.apply(elemento).equals(id.apply(actualizado))) {
                resultado.add(actualizado);
                encontrado = true;
            } else resultado.add(elemento);
        }
        if (!encontrado) throw new IllegalArgumentException("Registo a actualizar nao encontrado");
        return resultado;
    }

    private static void validarNumeroInstitucional(Utilizador candidato,
                                                    MeuHashMap<String, Utilizador> existentes) {
        for (String id : existentes.chaves()) {
            Utilizador existente = existentes.obter(id);
            if (existente.getId().equals(candidato.getId())) continue;
            if (candidato instanceof Estudante novo && existente instanceof Estudante actual
                    && actual.getNumeroEstudante().equals(novo.getNumeroEstudante()))
                throw new IllegalArgumentException("Numero de estudante ja registado: " + novo.getNumeroEstudante());
            if (candidato instanceof DocenteFuncionario novo && existente instanceof DocenteFuncionario actual
                    && actual.getNumeroFuncionario().equals(novo.getNumeroFuncionario()))
                throw new IllegalArgumentException("Numero de funcionario ja registado: " + novo.getNumeroFuncionario());
        }
    }

    private static boolean compatível(TipoUtilizador utilizador, TipoLugar lugar) {
        return utilizador == TipoUtilizador.DOCENTE_FUNCIONARIO || lugar == TipoLugar.ESTUDANTE;
    }

    private static void exigirTexto(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " nao pode ser nulo");
        if (valor.isBlank()) throw new IllegalArgumentException(campo + " nao pode estar vazio");
    }

    private static <T> T exigir(T valor, String mensagem) {
        if (valor == null) throw new IllegalArgumentException(mensagem);
        return valor;
    }
}
