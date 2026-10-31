package mz.edu.eda.modelo;

import java.io.Serializable;
import java.util.Objects;

public abstract class Utilizador implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String nome;
    private final String contacto;

    protected Utilizador(String id, String nome, String contacto) {
        this.id = obrigatorio(id, "ID");
        this.nome = obrigatorio(nome, "Nome");
        this.contacto = obrigatorio(contacto, "Contacto");
    }

    private static String obrigatorio(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " nao pode ser nulo");
        if (valor.isBlank()) throw new IllegalArgumentException(campo + " nao pode estar vazio");
        return valor.trim();
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getContacto() { return contacto; }
    public abstract TipoUtilizador getTipo();
}
