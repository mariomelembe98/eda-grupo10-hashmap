package mz.edu.eda.modelo;

import java.util.Objects;

public final class DocenteFuncionario extends Utilizador {
    private static final long serialVersionUID = 1L;
    private final String numeroFuncionario;
    private final String departamento;

    public DocenteFuncionario(String id, String nome, String contacto, String numeroFuncionario, String departamento) {
        super(id, nome, contacto);
        this.numeroFuncionario = validar(numeroFuncionario, "Numero de funcionario");
        this.departamento = validar(departamento, "Departamento");
    }

    private static String validar(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " nao pode ser nulo");
        if (valor.isBlank()) throw new IllegalArgumentException(campo + " nao pode estar vazio");
        return valor.trim();
    }

    public String getNumeroFuncionario() { return numeroFuncionario; }
    public String getDepartamento() { return departamento; }
    @Override public TipoUtilizador getTipo() { return TipoUtilizador.DOCENTE_FUNCIONARIO; }
}
