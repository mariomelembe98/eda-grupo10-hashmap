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
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/** Interface de consola para as operações do parque. */
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
            int opcao = lerInteiro("Opcao: ");
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
                    default -> saida.println("Opcao invalida. Escolha um numero entre 0 e 14.");
                }
            } catch (UncheckedIOException e) {
                saida.println("Falha de persistencia: " + mensagem(e.getCause()));
                saida.println("A operacao nao foi confirmada. Se a substituicao ficou indeterminada, escolha 14 para reconciliar.");
            } catch (IllegalStateException e) {
                saida.println("Operacao indisponivel: " + mensagem(e));
                saida.println("Se a persistencia estiver bloqueada, escolha 14 para reconciliar.");
            } catch (IllegalArgumentException | NullPointerException e) {
                saida.println("Operacao rejeitada: " + mensagem(e));
            }
        }
        saida.println("Aplicacao encerrada.");
    }

    private void mostrarMenuPrincipal() {
        saida.println("\n=== Gestao do Parque de Estacionamento ===");
        saida.println("1. Cadastrar utilizador");
        saida.println("2. Cadastrar veiculo");
        saida.println("3. Cadastrar lugar");
        saida.println("4. Actualizar registo");
        saida.println("5. Veiculos de estudantes");
        saida.println("6. Veiculos de docentes/funcionarios");
        saida.println("7. Registar entrada");
        saida.println("8. Registar saida");
        saida.println("9. Veiculos actualmente estacionados");
        saida.println("10. Historico completo");
        saida.println("11. Estacionamentos por utilizador");
        saida.println("12. Lugares livres e ocupados");
        saida.println("13. Consultar registos existentes");
        saida.println("14. Reconciliar persistencia");
        saida.println("0. Sair");
    }

    private void submenuCadastroUtilizador() {
        while (executar) {
            saida.println("\n--- Cadastrar utilizador ---");
            saida.println("1. Estudante");
            saida.println("2. Docente/funcionario");
            saida.println("0. Voltar");
            int opcao = lerInteiro("Opcao: ");
            if (!executar || opcao == 0) return;
            if (opcao == 1) {
                String id = campo("ID"); if (!executar) return;
                String nome = campo("Nome"); if (!executar) return;
                String contacto = campo("Contacto"); if (!executar) return;
                String numero = campo("Numero de estudante"); if (!executar) return;
                String curso = campo("Curso"); if (!executar) return;
                service.cadastrarEstudante(new Estudante(id, nome, contacto, numero, curso));
                saida.println("Estudante cadastrado com sucesso.");
                return;
            }
            if (opcao == 2) {
                String id = campo("ID"); if (!executar) return;
                String nome = campo("Nome"); if (!executar) return;
                String contacto = campo("Contacto"); if (!executar) return;
                String numero = campo("Numero de funcionario"); if (!executar) return;
                String departamento = campo("Departamento"); if (!executar) return;
                service.cadastrarDocenteFuncionario(new DocenteFuncionario(id, nome, contacto, numero, departamento));
                saida.println("Docente/funcionario cadastrado com sucesso.");
                return;
            }
            saida.println("Opcao invalida.");
        }
    }

    private void cadastrarVeiculo() {
        String matricula = campo("Matricula"); if (!executar) return;
        String marca = campo("Marca"); if (!executar) return;
        String modelo = campo("Modelo"); if (!executar) return;
        String cor = campo("Cor"); if (!executar) return;
        String proprietario = campo("ID do proprietario"); if (!executar) return;
        service.cadastrarVeiculo(new Veiculo(matricula, marca, modelo, cor, proprietario));
        saida.println("Veiculo cadastrado com sucesso.");
    }

    private void cadastrarLugar() {
        String numero = campo("Numero do lugar"); if (!executar) return;
        String localizacao = campo("Localizacao"); if (!executar) return;
        TipoLugar tipo = lerTipoLugar(); if (!executar || tipo == null) return;
        service.cadastrarLugar(new LugarEstacionamento(numero, localizacao, tipo));
        saida.println("Lugar cadastrado com sucesso.");
    }

    private void submenuActualizacao() {
        while (executar) {
            saida.println("\n--- Actualizar registo ---");
            saida.println("1. Utilizador");
            saida.println("2. Veiculo");
            saida.println("3. Lugar");
            saida.println("0. Voltar");
            int opcao = lerInteiro("Opcao: ");
            if (!executar || opcao == 0) return;
            switch (opcao) {
                case 1 -> actualizarUtilizador();
                case 2 -> actualizarVeiculo();
                case 3 -> actualizarLugar();
                default -> saida.println("Opcao invalida.");
            }
            return;
        }
    }

    private void actualizarUtilizador() {
        String id = campo("ID do utilizador"); if (!executar) return;
        Utilizador anterior = service.consultarUtilizador(id);
        String nome = campo("Novo nome"); if (!executar) return;
        String contacto = campo("Novo contacto"); if (!executar) return;
        if (anterior.getTipo() == TipoUtilizador.ESTUDANTE) {
            Estudante estudante = (Estudante) anterior;
            String numero = campo("Numero de estudante"); if (!executar) return;
            String curso = campo("Curso"); if (!executar) return;
            service.actualizarUtilizador(new Estudante(id, nome, contacto, numero, curso));
        } else {
            DocenteFuncionario docente = (DocenteFuncionario) anterior;
            String numero = campo("Numero de funcionario"); if (!executar) return;
            String departamento = campo("Departamento"); if (!executar) return;
            service.actualizarUtilizador(new DocenteFuncionario(id, nome, contacto, numero, departamento));
        }
        saida.println("Utilizador actualizado com sucesso.");
    }

    private void actualizarVeiculo() {
        String matricula = campo("Matricula do veiculo"); if (!executar) return;
        Veiculo anterior = service.consultarVeiculo(matricula);
        String marca = campo("Nova marca"); if (!executar) return;
        String modelo = campo("Novo modelo"); if (!executar) return;
        String cor = campo("Nova cor"); if (!executar) return;
        service.actualizarVeiculo(new Veiculo(anterior.getMatricula(), marca, modelo, cor, anterior.getIdProprietario()));
        saida.println("Veiculo actualizado com sucesso.");
    }

    private void actualizarLugar() {
        String numero = campo("Numero do lugar"); if (!executar) return;
        service.consultarLugar(numero);
        String localizacao = campo("Nova localizacao"); if (!executar) return;
        TipoLugar tipo = lerTipoLugar(); if (!executar || tipo == null) return;
        service.actualizarLugar(new LugarEstacionamento(numero, localizacao, tipo));
        saida.println("Lugar actualizado com sucesso.");
    }

    private TipoLugar lerTipoLugar() {
        while (executar) {
            saida.println("Tipo do lugar: 1. Estudante  2. Docente/funcionario");
            int opcao = lerInteiro("Opcao: ");
            if (opcao == 1) return TipoLugar.ESTUDANTE;
            if (opcao == 2) return TipoLugar.DOCENTE_FUNCIONARIO;
            if (executar) saida.println("Opcao invalida.");
        }
        return null;
    }

    private void registarEntrada() {
        String matricula = campo("Matricula"); if (!executar) return;
        String lugar = campo("Numero do lugar"); if (!executar) return;
        Estacionamento estacionamento = service.registarEntrada(matricula, lugar);
        saida.println("Entrada registada: " + estacionamento.getIdentificador() + " em "
                + estacionamento.getDataHoraEntrada().format(FORMATO_DATA) + ".");
    }

    private void registarSaida() {
        String matricula = campo("Matricula"); if (!executar) return;
        Estacionamento estacionamento = service.registarSaida(matricula);
        saida.println("Saida registada: " + estacionamento.getIdentificador() + " em "
                + estacionamento.getDataHoraSaida().format(FORMATO_DATA) + ".");
    }

    private void listarEstacionados() {
        List<Veiculo> veiculos = service.listarVeiculosEstacionados();
        if (veiculos.isEmpty()) { saida.println("Nao ha veiculos estacionados."); return; }
        saida.println("Veiculos actualmente estacionados:");
        for (Veiculo veiculo : veiculos) {
            Estacionamento activo = service.consultarHistorico().stream()
                    .filter(e -> e.estaActivo() && e.getMatricula().equals(veiculo.getMatricula())).findFirst().orElse(null);
            saida.println("- " + veiculo.getMatricula() + " | proprietario " + veiculo.getIdProprietario()
                    + " | lugar " + (activo == null ? "?" : activo.getNumeroLugar()));
        }
    }

    private void listarVeiculos(List<Veiculo> veiculos, String titulo) {
        saida.println(titulo + ":");
        if (veiculos.isEmpty()) { saida.println("Nenhum veiculo encontrado."); return; }
        for (Veiculo veiculo : veiculos) imprimirVeiculo(veiculo);
    }

    private void imprimirVeiculo(Veiculo v) {
        saida.println("- " + v.getMatricula() + " | " + v.getMarca() + " " + v.getModelo()
                + " | " + v.getCor() + " | proprietario " + v.getIdProprietario());
    }

    private void listarHistorico(List<Estacionamento> historico, String titulo) {
        saida.println(titulo + ":");
        if (historico.isEmpty()) { saida.println("Sem registos de estacionamento."); return; }
        for (Estacionamento e : historico) {
            saida.println("- " + e.getIdentificador() + " | " + e.getMatricula() + " | utilizador "
                    + e.getIdProprietario() + " | lugar " + e.getNumeroLugar() + " | entrada "
                    + e.getDataHoraEntrada().format(FORMATO_DATA) + " | saida "
                    + (e.getDataHoraSaida() == null ? "activa" : e.getDataHoraSaida().format(FORMATO_DATA)));
        }
    }

    private void consultarPorUtilizador() {
        String id = campo("ID do utilizador"); if (!executar) return;
        listarHistorico(service.consultarEstacionamentosPorUtilizador(id), "Estacionamentos do utilizador " + id);
    }

    private void mostrarContagemLugares() {
        saida.println("Lugares livres: " + service.quantidadeLugaresLivres());
        saida.println("Lugares ocupados: " + service.quantidadeLugaresOcupados());
    }

    private void submenuConsultas() {
        while (executar) {
            saida.println("\n--- Consultar registos ---");
            saida.println("1. Utilizador por ID");
            saida.println("2. Veiculo por matricula");
            saida.println("3. Lugar por numero");
            saida.println("0. Voltar");
            int opcao = lerInteiro("Opcao: ");
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
                    LugarEstacionamento lugar = service.consultarLugar(numero);
                    saida.println("Lugar " + lugar.getNumero() + " | " + lugar.getLocalizacao() + " | " + rotulo(lugar.getTipo()));
                }
                default -> saida.println("Opcao invalida.");
            }
        }
    }

    private void imprimirUtilizador(Utilizador u) {
        saida.println("Utilizador " + u.getId() + " | " + u.getNome() + " | " + u.getContacto()
                + " | " + rotulo(u.getTipo()));
        if (u instanceof Estudante e) saida.println("Numero de estudante: " + e.getNumeroEstudante() + " | Curso: " + e.getCurso());
        if (u instanceof DocenteFuncionario d) saida.println("Numero de funcionario: " + d.getNumeroFuncionario()
                + " | Departamento: " + d.getDepartamento());
    }

    private String rotulo(TipoLugar tipo) { return tipo == TipoLugar.ESTUDANTE ? "Estudante" : "Docente/funcionario"; }
    private String rotulo(TipoUtilizador tipo) { return tipo == TipoUtilizador.ESTUDANTE ? "Estudante" : "Docente/funcionario"; }

    private void reconciliarPersistencia() {
        try {
            service.reconciliarPersistencia();
            saida.println("Persistencia reconciliada com sucesso.");
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            saida.println("Nao foi possivel reconciliar a persistencia: " + mensagem(e));
        }
    }

    private String campo(String rotulo) {
        while (executar) {
            String valor = lerLinha(rotulo + ": ");
            if (!executar) return "";
            if (!valor.isBlank()) return valor.trim();
            saida.println("Campo obrigatorio. Introduza um valor.");
        }
        return "";
    }

    private int lerInteiro(String prompt) {
        while (executar) {
            String valor = lerLinha(prompt);
            if (!executar) return -1;
            try { return Integer.parseInt(valor.trim()); }
            catch (NumberFormatException e) { saida.println("Introduza uma opcao numerica valida."); }
        }
        return -1;
    }

    private String lerLinha(String prompt) {
        saida.print(prompt);
        saida.flush();
        if (!entrada.hasNextLine()) { executar = false; return ""; }
        return entrada.nextLine();
    }

    private String mensagem(Throwable erro) {
        if (erro == null || erro.getMessage() == null || erro.getMessage().isBlank()) return "erro sem detalhe";
        return erro.getMessage();
    }
}
