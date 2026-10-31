package mz.edu.eda.modelo;

import java.util.Objects;

public final class Estudante extends Utilizador {
    private static final long serialVersionUID = 1L;
    private final String numeroEstudante;
    private final String curso;

    public Estudante(String id, String nome, String contacto, String numeroEstudante, String curso) {
        super(id, nome, contacto);
        this.numeroEstudante = validar(numeroEstudante, "Numero de estudante");
        this.curso = validar(curso, "Curso");
    }

    private static String validar(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " nao pode ser nulo");
        if (valor.isBlank()) throw new IllegalArgumentException(campo + " nao pode estar vazio");
        return valor.trim();
    }

    public String getNumeroEstudante() { return numeroEstudante; }
    public String getCurso() { return curso; }
    @Override public TipoUtilizador getTipo() { return TipoUtilizador.ESTUDANTE; }
}
