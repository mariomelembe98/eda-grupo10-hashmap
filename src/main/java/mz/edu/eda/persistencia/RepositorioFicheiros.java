package mz.edu.eda.persistencia;

import mz.edu.eda.modelo.Estacionamento;
import mz.edu.eda.modelo.LugarEstacionamento;
import mz.edu.eda.modelo.Utilizador;
import mz.edu.eda.modelo.Veiculo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Repositório local baseado em serialização de objectos Java. */
public final class RepositorioFicheiros {
    private static final Path CAMINHO_PADRAO = Path.of("dados", "parque.dat");
    private final Path caminho;
    private final Replacer replacer;
    private final Gravador gravador;
    private final CriadorTemporario criadorTemporario;

    public RepositorioFicheiros() { this(CAMINHO_PADRAO); }

    public RepositorioFicheiros(Path caminho) {
        this(caminho, (temporario, destino) -> Files.move(temporario, destino,
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING));
    }

    RepositorioFicheiros(Path caminho, Replacer replacer) {
        this(caminho, replacer, RepositorioFicheiros::escreverEstado);
    }

    RepositorioFicheiros(Path caminho, Replacer replacer, Gravador gravador) {
        this(caminho, replacer, gravador, Files::createTempFile);
    }

    RepositorioFicheiros(Path caminho, Replacer replacer, Gravador gravador, CriadorTemporario criadorTemporario) {
        this.caminho = Objects.requireNonNull(caminho, "Caminho nao pode ser nulo").toAbsolutePath().normalize();
        this.replacer = Objects.requireNonNull(replacer, "Operacao de substituicao nao pode ser nula");
        this.gravador = Objects.requireNonNull(gravador, "Gravador nao pode ser nulo");
        this.criadorTemporario = Objects.requireNonNull(criadorTemporario, "Criador de temporario nao pode ser nulo");
    }

    public Path getCaminho() { return caminho; }

    public Estado carregar() throws IOException {
        if (Files.notExists(caminho)) return Estado.vazio();
        try (ObjectInputStream entrada = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(caminho)))) {
            entrada.setObjectInputFilter(RepositorioFicheiros::filtrarClasse);
            Object objecto = entrada.readObject();
            if (!(objecto instanceof Estado estado)) throw new IOException("Formato de estado invalido");
            return estado;
        } catch (ClassNotFoundException | RuntimeException e) {
            throw new IOException("Nao foi possivel ler o ficheiro de estado", e);
        }
    }

    public void guardar(Estado estado) throws IOException {
        Objects.requireNonNull(estado, "Estado nao pode ser nulo");
        Path directorio = caminho.getParent();
        if (directorio != null) Files.createDirectories(directorio);
        Path temporario = criadorTemporario.criar(directorio, caminho.getFileName().toString(), ".tmp");
        boolean substituido = false;
        try {
            gravador.escrever(temporario, estado);
            try { replacer.substituir(temporario, caminho); }
            catch (IOException e) { throw new FalhaSubstituicaoException("Falha durante a substituicao; resultado pode ser indeterminado", e); }
            substituido = true;
        } finally {
            if (!substituido) {
                try { Files.deleteIfExists(temporario); }
                catch (IOException ignorada) { /* Preserve the original write or replacement failure. */ }
            }
        }
    }

    @FunctionalInterface
    interface Replacer { void substituir(Path temporario, Path destino) throws IOException; }

    @FunctionalInterface
    interface Gravador { void escrever(Path temporario, Estado estado) throws IOException; }

    @FunctionalInterface
    interface CriadorTemporario { Path criar(Path directorio, String prefixo, String sufixo) throws IOException; }

    private static void escreverEstado(Path temporario, Estado estado) throws IOException {
        try (ObjectOutputStream saida = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(temporario)))) {
            saida.writeObject(estado);
            saida.flush();
        }
    }

    public static final class FalhaSubstituicaoException extends IOException {
        private static final long serialVersionUID = 1L;
        public FalhaSubstituicaoException(String mensagem, IOException causa) { super(mensagem, causa); }
    }

    private static ObjectInputFilter.Status filtrarClasse(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 30 || info.references() > 1_000_000 || info.streamBytes() > 100_000_000)
            return ObjectInputFilter.Status.REJECTED;
        Class<?> classe = info.serialClass();
        if (classe == null) return ObjectInputFilter.Status.UNDECIDED;
        while (classe.isArray()) classe = classe.getComponentType();
        String nome = classe.getName();
        if (classe.isPrimitive() || nome.startsWith("mz.edu.eda.modelo.")
                || nome.equals("java.lang.String") || nome.equals("java.lang.Enum")
                || nome.equals("java.lang.Object") || nome.equals("java.util.ArrayList")
                || nome.equals("java.time.Ser") || nome.equals("java.time.LocalDateTime")
                || nome.equals(Estado.class.getName())) return ObjectInputFilter.Status.ALLOWED;
        return ObjectInputFilter.Status.REJECTED;
    }

    /** Snapshot serializável dos registos; não contém o TAD nem índices derivados. */
    public static final class Estado implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final ArrayList<Utilizador> utilizadores;
        private final ArrayList<Veiculo> veiculos;
        private final ArrayList<LugarEstacionamento> lugares;
        private final ArrayList<Estacionamento> estacionamentos;
        private final long sequenciaEstacionamento;

        public Estado(List<Utilizador> utilizadores, List<Veiculo> veiculos,
                      List<LugarEstacionamento> lugares, List<Estacionamento> estacionamentos,
                      long sequenciaEstacionamento) {
            this.utilizadores = new ArrayList<>(Objects.requireNonNull(utilizadores));
            this.veiculos = new ArrayList<>(Objects.requireNonNull(veiculos));
            this.lugares = new ArrayList<>(Objects.requireNonNull(lugares));
            this.estacionamentos = new ArrayList<>(Objects.requireNonNull(estacionamentos));
            if (sequenciaEstacionamento < 0) throw new IllegalArgumentException("Sequencia negativa");
            this.sequenciaEstacionamento = sequenciaEstacionamento;
        }

        public List<Utilizador> utilizadores() { return List.copyOf(utilizadores); }
        public List<Veiculo> veiculos() { return List.copyOf(veiculos); }
        public List<LugarEstacionamento> lugares() { return List.copyOf(lugares); }
        public List<Estacionamento> estacionamentos() { return List.copyOf(estacionamentos); }
        public long sequenciaEstacionamento() { return sequenciaEstacionamento; }

        public static Estado vazio() { return new Estado(List.of(), List.of(), List.of(), List.of(), 0); }
    }
}
