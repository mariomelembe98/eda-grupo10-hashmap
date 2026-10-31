package mz.edu.eda.ui;

import mz.edu.eda.modelo.DocenteFuncionario;
import mz.edu.eda.modelo.Estacionamento;
import mz.edu.eda.modelo.Estudante;
import mz.edu.eda.modelo.LugarEstacionamento;
import mz.edu.eda.modelo.TipoLugar;
import mz.edu.eda.modelo.TipoUtilizador;
import mz.edu.eda.modelo.Utilizador;
import mz.edu.eda.modelo.Veiculo;
import mz.edu.eda.servico.ParqueService;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

/** Interface textual para as operações de gestão do parque. */
public final class MenuConsola {
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ParqueService service;
    private final Scanner entrada;
    private final PrintStream saida;
    private boolean executar = true;

    public MenuConsola(ParqueService service, Scanner entrada, PrintStream saida) {
        this.service = Objects.requireNonNull(service, "Servico nao pode ser nulo");
        this.entrada = Objects.requireNonNull(entrada, "Scanner nao pode ser nulo");
        this.saida = Objects.requireNonNull(saida, "Saida nao pode ser nula");
    }

    public void executar() {
        while (executar) {
            mostrarMenuPrincipal();
            int opcao = lerInteiro("Seleccione uma opcao (0-14): ");
            if (!executar) break;
            try {
                switch (opcao) {
                    case 0 -> executar = false;
                    case 1 -> submenuCadastroUtilizador();
                    case 2 -> cadastrarVeiculo();
                    case 3 -> cadastrarLugar();
                    case 4 -> submenuActualizacao();
                    case 5 -> listarVeiculos(service.listarVeiculosDeEstudantes(), "Veiculos de estudantes");
                    case 6 -> listarVeiculos(service.listarVeiculosDeDocentesFuncionarios(), "Veiculos de docentes/funcionarios");
                    case 7 -> registarEntrada();
                    case 8 -> registarSaida();
                    case 9 -> listarEstacionados();
                    case 10 -> listarHistorico(service.consultarHistorico(), "Historico completo");
                    case 11 -> consultarPorUtilizador();
                    case 12 -> mostrarContagemLugares();
                    case 13 -> submenuConsultas();
                    case 14 -> reconciliarPersistencia();
                    default -> saida.println("ERRO: Opcao invalida. Escolha um numero entre 0 e 14.");
                }
            } catch (UncheckedIOException e) {
                saida.println("ERRO: Falha de persistencia: " + mensagem(e.getCause()));
                saida.println("A operacao nao foi confirmada. Se a substituicao ficou indeterminada, escolha 14 para reconciliar.");
            } catch (IllegalStateException e) {
                saida.println("AVISO: Operacao indisponivel: " + mensagem(e));
                saida.println("Se a persistencia estiver bloqueada, escolha 14 para reconciliar.");
            } catch (IllegalArgumentException | NullPointerException e) {
                saida.println("ERRO: Operacao rejeitada: " + mensagem(e));
            }
        }
        saida.println("Aplicacao encerrada.");
    }

    private void mostrarMenuPrincipal() {
        int livres = service.quantidadeLugaresLivres();
        int ocupados = service.quantidadeLugaresOcupados();
        saida.println();
        saida.println("==========================================================");
        saida.println("       SISTEMA DE GESTAO DO PARQUE DE ESTACIONAMENTO");
        saida.println("                         EDA - GRUPO 10");
        saida.println("==========================================================");
        saida.printf("Ocupacao actual: %s | %d livres%n", formatarLugaresOcupados(ocupados), livres);
        saida.println();
        saida.println("GESTAO DE REGISTOS");
        saida.println("----------------------------------------------------------");
        saida.println(" [1] Cadastrar utilizador");
        saida.println(" [2] Cadastrar veiculo");
        saida.println(" [3] Cadastrar lugar");
        saida.println(" [4] Actualizar registo");
        saida.println();
        saida.println("CONSULTAS DE VEICULOS");
        saida.println("----------------------------------------------------------");
        saida.println(" [5] Veiculos de estudantes");
        saida.println(" [6] Veiculos de docentes/funcionarios");
        saida.println();
        saida.println("MOVIMENTOS");
        saida.println("----------------------------------------------------------");
        saida.println(" [7] Registar entrada");
        saida.println(" [8] Registar saida");
        saida.println();
        saida.println("CONSULTAS E RELATORIOS");
        saida.println("----------------------------------------------------------");
        saida.println(" [9] Veiculos actualmente estacionados");
        saida.println("[10] Historico completo");
        saida.println("[11] Historico por utilizador");
        saida.println("[12] Lugares livres e ocupados");
        saida.println("[13] Consultar registos");
        saida.println();
        saida.println("MANUTENCAO");
        saida.println("----------------------------------------------------------");
        saida.println("[14] Reconciliar persistencia");
        saida.println();
        saida.println("----------------------------------------------------------");
        saida.println(" [0] Sair");
        saida.println("==========================================================");
    }

    private void submenuCadastroUtilizador() {
        while (executar) {
            cabecalho("CADASTRO > UTILIZADOR");
            saida.println(" [1] Estudante");
            saida.println(" [2] Docente/funcionario");
            saida.println(" [0] Voltar ao menu principal");
            int opcao = lerInteiro("Seleccione o tipo: ");
            if (!executar || opcao == 0) return;
            if (opcao == 1) {
                String id = campo("ID"); if (!executar) return;
                String nome = campo("Nome"); if (!executar) return;
                String contacto = campo("Contacto"); if (!executar) return;
                String numero = campo("Numero de estudante"); if (!executar) return;
                String curso = campo("Curso"); if (!executar) return;
                service.cadastrarEstudante(new Estudante(id, nome, contacto, numero, curso));
                sucesso("Estudante cadastrado com sucesso.");
                return;
            }
            if (opcao == 2) {
                String id = campo("ID"); if (!executar) return;
                String nome = campo("Nome"); if (!executar) return;
                String contacto = campo("Contacto"); if (!executar) return;
                String numero = campo("Numero de funcionario"); if (!executar) return;
                String departamento = campo("Departamento"); if (!executar) return;
                service.cadastrarDocenteFuncionario(new DocenteFuncionario(id, nome, contacto, numero, departamento));
                sucesso("Docente/funcionario cadastrado com sucesso.");
                return;
            }
            saida.println("ERRO: Opcao invalida.");
        }
    }

    private void cadastrarVeiculo() {
        cabecalho("GESTAO DE REGISTOS > CADASTRAR VEICULO");
        String matricula = campo("Matricula"); if (!executar) return;
        String marca = campo("Marca"); if (!executar) return;
        String modelo = campo("Modelo"); if (!executar) return;
        String cor = campo("Cor"); if (!executar) return;
        String proprietario = campo("ID do proprietario"); if (!executar) return;
        service.cadastrarVeiculo(new Veiculo(matricula, marca, modelo, cor, proprietario));
        sucesso("Veiculo cadastrado com sucesso.");
    }

    private void cadastrarLugar() {
        cabecalho("GESTAO DE REGISTOS > CADASTRAR LUGAR");
        String numero = campo("Numero do lugar"); if (!executar) return;
        String localizacao = campo("Localizacao"); if (!executar) return;
        TipoLugar tipo = lerTipoLugar(); if (!executar || tipo == null) return;
        service.cadastrarLugar(new LugarEstacionamento(numero, localizacao, tipo));
        sucesso("Lugar cadastrado com sucesso.");
    }

    private void submenuActualizacao() {
        while (executar) {
            cabecalho("GESTAO > ACTUALIZAR REGISTO");
            saida.println(" [1] Utilizador");
            saida.println(" [2] Veiculo");
            saida.println(" [3] Lugar");
            saida.println(" [0] Voltar ao menu principal");
            int opcao = lerInteiro("Seleccione o tipo de registo: ");
            if (!executar || opcao == 0) return;
            switch (opcao) {
                case 1 -> actualizarUtilizador();
                case 2 -> actualizarVeiculo();
                case 3 -> actualizarLugar();
                default -> saida.println("ERRO: Opcao invalida.");
            }
            return;
        }
    }

    private void actualizarUtilizador() {
        cabecalho("GESTAO > ACTUALIZAR UTILIZADOR");
        String id = campo("ID do utilizador"); if (!executar) return;
        Utilizador anterior = service.consultarUtilizador(id);
        String nome = campo("Novo nome"); if (!executar) return;
        String contacto = campo("Novo contacto"); if (!executar) return;
        if (anterior.getTipo() == TipoUtilizador.ESTUDANTE) {
            String numero = campo("Numero de estudante"); if (!executar) return;
            String curso = campo("Curso"); if (!executar) return;
            service.actualizarUtilizador(new Estudante(id, nome, contacto, numero, curso));
        } else {
            String numero = campo("Numero de funcionario"); if (!executar) return;
            String departamento = campo("Departamento"); if (!executar) return;
            service.actualizarUtilizador(new DocenteFuncionario(id, nome, contacto, numero, departamento));
        }
        sucesso("Utilizador actualizado com sucesso.");
    }

    private void actualizarVeiculo() {
        cabecalho("GESTAO > ACTUALIZAR VEICULO");
        String matricula = campo("Matricula do veiculo"); if (!executar) return;
        Veiculo anterior = service.consultarVeiculo(matricula);
        String marca = campo("Nova marca"); if (!executar) return;
        String modelo = campo("Novo modelo"); if (!executar) return;
        String cor = campo("Nova cor"); if (!executar) return;
        service.actualizarVeiculo(new Veiculo(anterior.getMatricula(), marca, modelo, cor, anterior.getIdProprietario()));
        sucesso("Veiculo actualizado com sucesso.");
    }

    private void actualizarLugar() {
        cabecalho("GESTAO > ACTUALIZAR LUGAR");
        String numero = campo("Numero do lugar"); if (!executar) return;
        service.consultarLugar(numero);
        String localizacao = campo("Nova localizacao"); if (!executar) return;
        TipoLugar tipo = lerTipoLugar(); if (!executar || tipo == null) return;
        service.actualizarLugar(new LugarEstacionamento(numero, localizacao, tipo));
        sucesso("Lugar actualizado com sucesso.");
    }

    private TipoLugar lerTipoLugar() {
        while (executar) {
            saida.println("Tipo do lugar: [1] Estudante  [2] Docente/funcionario");
            int opcao = lerInteiro("Seleccione o tipo: ");
            if (opcao == 1) return TipoLugar.ESTUDANTE;
            if (opcao == 2) return TipoLugar.DOCENTE_FUNCIONARIO;
            if (executar) saida.println("ERRO: Opcao invalida.");
        }
        return null;
    }

    private void registarEntrada() {
        cabecalho("MOVIMENTOS > REGISTAR ENTRADA");
        String matricula = campo("Matricula"); if (!executar) return;
        String lugar = campo("Numero do lugar"); if (!executar) return;
        Estacionamento estacionamento = service.registarEntrada(matricula, lugar);
        sucesso("Entrada registada: " + estacionamento.getIdentificador() + " em "
                + estacionamento.getDataHoraEntrada().format(FORMATO_DATA) + ".");
    }

    private void registarSaida() {
        cabecalho("MOVIMENTOS > REGISTAR SAIDA");
        String matricula = campo("Matricula"); if (!executar) return;
        Estacionamento estacionamento = service.registarSaida(matricula);
        sucesso("Saida registada: " + estacionamento.getIdentificador() + " em "
                + estacionamento.getDataHoraSaida().format(FORMATO_DATA) + ".");
    }

    private void listarEstacionados() {
        cabecalho("CONSULTAS > VEICULOS ESTACIONADOS");
        List<Estacionamento> activos = new ArrayList<>(service.consultarHistorico().stream()
                .filter(Estacionamento::estaActivo)
                .sorted(Comparator.comparing(Estacionamento::getMatricula))
                .toList());
        if (activos.isEmpty()) {
            saida.println("Nao ha veiculos estacionados.");
            return;
        }
        List<List<String>> linhas = new ArrayList<>();
        for (Estacionamento activo : activos) {
            Veiculo veiculo = service.consultarVeiculo(activo.getMatricula());
            linhas.add(List.of(veiculo.getMatricula(), veiculo.getIdProprietario(),
                    activo.getNumeroLugar(), activo.getDataHoraEntrada().format(FORMATO_DATA)));
        }
        FormatadorConsola.imprimirTabela(saida,
                List.of("Matricula", "Proprietario", "Lugar", "Entrada"), linhas, 14, 18, 14, 16);
        quantidade("veiculo estacionado", activos.size());
    }

    private void listarVeiculos(List<Veiculo> veiculos, String titulo) {
        cabecalho("CONSULTAS > " + titulo.toUpperCase(Locale.ROOT));
        saida.println(titulo + ":");
        if (veiculos.isEmpty()) {
            saida.println("Nenhum veiculo encontrado.");
            return;
        }
        List<List<String>> linhas = new ArrayList<>();
        for (Veiculo veiculo : veiculos)
            linhas.add(List.of(veiculo.getMatricula(), veiculo.getMarca() + " " + veiculo.getModelo(),
                    veiculo.getCor(), veiculo.getIdProprietario()));
        FormatadorConsola.imprimirTabela(saida,
                List.of("Matricula", "Marca / modelo", "Cor", "Proprietario"), linhas, 16, 24, 16, 20);
        quantidade("veiculo", veiculos.size());
    }

    private void listarHistorico(List<Estacionamento> historico, String titulo) {
        cabecalho("CONSULTAS > " + titulo.toUpperCase(Locale.ROOT));
        saida.println(titulo + ":");
        if (historico.isEmpty()) {
            saida.println("Sem registos de estacionamento.");
            return;
        }
        List<List<String>> linhas = new ArrayList<>();
        for (Estacionamento estacionamento : historico) {
            String saidaRegisto = estacionamento.getDataHoraSaida() == null ? "Activo"
                    : estacionamento.getDataHoraSaida().format(FORMATO_DATA);
            linhas.add(List.of(estacionamento.getIdentificador(), estacionamento.getMatricula(),
                    estacionamento.getIdProprietario(), estacionamento.getNumeroLugar(),
                    estacionamento.getDataHoraEntrada().format(FORMATO_DATA), saidaRegisto));
        }
        FormatadorConsola.imprimirTabela(saida,
                List.of("ID", "Matricula", "Utilizador", "Lugar", "Entrada", "Saida"), linhas,
                14, 16, 18, 14, 16, 16);
        quantidade("registo", historico.size());
    }

    private void consultarPorUtilizador() {
        cabecalho("CONSULTAS > HISTORICO POR UTILIZADOR");
        String id = campo("ID do utilizador"); if (!executar) return;
        listarHistorico(service.consultarEstacionamentosPorUtilizador(id), "Estacionamentos do utilizador " + id);
    }

    private void mostrarContagemLugares() {
        cabecalho("CONSULTAS > ESTADO DO PARQUE");
        int livres = service.quantidadeLugaresLivres();
        int ocupados = service.quantidadeLugaresOcupados();
        List<LugarEstacionamento> lugares = new ArrayList<>(service.exportarEstado().lugares());
        int total = lugares.size();
        saida.println("Total de lugares cadastrados: " + total);
        saida.println("Lugares livres: " + livres);
        saida.println("Lugares ocupados: " + ocupados);
        if (total == 0) {
            saida.println("Taxa de ocupacao: N/D (nao existem lugares cadastrados).");
            saida.println("Nao existem lugares cadastrados.");
            return;
        }
        double taxa = (ocupados * 100.0) / total;
        saida.printf(Locale.ROOT, "Taxa de ocupacao: %.1f%%%n", taxa);

        lugares.sort(Comparator.comparing(LugarEstacionamento::getNumero));
        List<Estacionamento> activos = new ArrayList<>(service.consultarHistorico().stream()
                .filter(Estacionamento::estaActivo).toList());
        activos.sort(Comparator.comparing(Estacionamento::getNumeroLugar));
        List<List<String>> linhas = new ArrayList<>();
        int indiceActivo = 0;
        for (LugarEstacionamento lugar : lugares) {
            while (indiceActivo < activos.size()
                    && activos.get(indiceActivo).getNumeroLugar().compareTo(lugar.getNumero()) < 0)
                indiceActivo++;
            boolean ocupado = indiceActivo < activos.size()
                    && activos.get(indiceActivo).getNumeroLugar().equals(lugar.getNumero());
            String estado = ocupado ? "[O] Ocupado" : "[L] Livre";
            linhas.add(List.of(lugar.getNumero(), estado, lugar.getLocalizacao(), rotulo(lugar.getTipo())));
        }
        FormatadorConsola.imprimirTabela(saida,
                List.of("Lugar", "Estado", "Localizacao", "Tipo"), linhas, 16, 14, 28, 22);
    }

    private void submenuConsultas() {
        while (executar) {
            cabecalho("CONSULTAS > REGISTOS");
            saida.println(" [1] Utilizador por ID");
            saida.println(" [2] Veiculo por matricula");
            saida.println(" [3] Lugar por numero");
            saida.println(" [0] Voltar ao menu principal");
            int opcao = lerInteiro("Seleccione o tipo de consulta: ");
            if (!executar || opcao == 0) return;
            switch (opcao) {
                case 1 -> {
                    String id = campo("ID do utilizador"); if (!executar) return;
                    imprimirUtilizador(service.consultarUtilizador(id));
                }
                case 2 -> {
                    String matricula = campo("Matricula"); if (!executar) return;
                    imprimirVeiculo(service.consultarVeiculo(matricula));
                }
                case 3 -> {
                    String numero = campo("Numero do lugar"); if (!executar) return;
                    imprimirLugar(service.consultarLugar(numero));
                }
                default -> saida.println("ERRO: Opcao invalida.");
            }
        }
    }

    private void imprimirUtilizador(Utilizador utilizador) {
        cabecalho("CONSULTAS > UTILIZADOR");
        List<List<String>> linhas = new ArrayList<>();
        linhas.add(List.of("ID", utilizador.getId()));
        linhas.add(List.of("Nome", utilizador.getNome()));
        linhas.add(List.of("Contacto", utilizador.getContacto()));
        linhas.add(List.of("Tipo", rotulo(utilizador.getTipo())));
        if (utilizador instanceof Estudante estudante) {
            linhas.add(List.of("Numero de estudante", estudante.getNumeroEstudante()));
            linhas.add(List.of("Curso", estudante.getCurso()));
        }
        if (utilizador instanceof DocenteFuncionario docente) {
            linhas.add(List.of("Numero de funcionario", docente.getNumeroFuncionario()));
            linhas.add(List.of("Departamento", docente.getDepartamento()));
        }
        FormatadorConsola.imprimirTabela(saida, List.of("Campo", "Valor"), linhas, 24, 40);
    }

    private void imprimirVeiculo(Veiculo veiculo) {
        cabecalho("CONSULTAS > VEICULO");
        FormatadorConsola.imprimirTabela(saida, List.of("Campo", "Valor"), List.of(
                List.of("Matricula", veiculo.getMatricula()),
                List.of("Marca", veiculo.getMarca()),
                List.of("Modelo", veiculo.getModelo()),
                List.of("Cor", veiculo.getCor()),
                List.of("Proprietario", veiculo.getIdProprietario())), 24, 40);
    }

    private void imprimirLugar(LugarEstacionamento lugar) {
        cabecalho("CONSULTAS > LUGAR");
        FormatadorConsola.imprimirTabela(saida, List.of("Campo", "Valor"), List.of(
                List.of("Numero", lugar.getNumero()),
                List.of("Localizacao", lugar.getLocalizacao()),
                List.of("Tipo", rotulo(lugar.getTipo()))), 24, 40);
    }

    private void reconciliarPersistencia() {
        cabecalho("MANUTENCAO > RECONCILIAR PERSISTENCIA");
        try {
            service.reconciliarPersistencia();
            sucesso("Persistencia reconciliada com sucesso.");
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            saida.println("ERRO: Nao foi possivel reconciliar a persistencia: " + mensagem(e));
        }
    }

    private void cabecalho(String titulo) {
        saida.println();
        saida.println("----------------------------------------------------------");
        saida.println(titulo);
        saida.println("----------------------------------------------------------");
    }

    private String formatarLugaresOcupados(int quantidade) {
        return quantidade + (quantidade == 1 ? " ocupado" : " ocupados");
    }

    private void quantidade(String entidade, int quantidade) {
        String descricao = switch (entidade) {
            case "veiculo estacionado" -> quantidade == 1 ? "veiculo estacionado" : "veiculos estacionados";
            case "veiculo" -> quantidade == 1 ? "veiculo" : "veiculos";
            case "registo" -> quantidade == 1 ? "registo" : "registos";
            default -> entidade;
        };
        saida.println(quantidade + " " + descricao + (quantidade == 1 ? " encontrado." : " encontrados."));
    }

    private void sucesso(String mensagem) { saida.println("SUCESSO: " + mensagem); }

    private String rotulo(TipoLugar tipo) {
        return tipo == TipoLugar.ESTUDANTE ? "Estudante" : "Docente/funcionario";
    }

    private String rotulo(TipoUtilizador tipo) {
        return tipo == TipoUtilizador.ESTUDANTE ? "Estudante" : "Docente/funcionario";
    }

    private String campo(String rotulo) {
        while (executar) {
            String valor = lerLinha(rotulo + ": ");
            if (!executar) return "";
            if (!valor.isBlank()) return valor.trim();
            saida.println("ERRO: Campo obrigatorio. Introduza um valor.");
        }
        return "";
    }

    private int lerInteiro(String prompt) {
        while (executar) {
            String valor = lerLinha(prompt);
            if (!executar) return -1;
            try {
                return Integer.parseInt(valor.trim());
            } catch (NumberFormatException e) {
                saida.println("ERRO: Introduza uma opcao numerica valida.");
            }
        }
        return -1;
    }

    private String lerLinha(String prompt) {
        saida.print(prompt);
        saida.flush();
        if (!entrada.hasNextLine()) {
            executar = false;
            return "";
        }
        return entrada.nextLine();
    }

    private String mensagem(Throwable erro) {
        if (erro == null || erro.getMessage() == null || erro.getMessage().isBlank()) return "erro sem detalhe";
        return erro.getMessage();
    }
}
