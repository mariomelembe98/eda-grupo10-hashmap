package mz.edu.eda.modelo;

import java.io.Serializable;
import java.util.Objects;

public final class LugarEstacionamento implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String numero;
    private final String localizacao;
    private final TipoLugar tipo;

    public LugarEstacionamento(String numero, String localizacao, TipoLugar tipo) {
        this.numero = validar(numero, "Numero do lugar");
        this.localizacao = validar(localizacao, "Localizacao");
        this.tipo = Objects.requireNonNull(tipo, "Tipo do lugar nao pode ser nulo");
    }

    private static String validar(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " nao pode ser nulo");
        if (valor.isBlank()) throw new IllegalArgumentException(campo + " nao pode estar vazio");
        return valor.trim();
    }

    public String getNumero() { return numero; }
    public String getLocalizacao() { return localizacao; }
    public TipoLugar getTipo() { return tipo; }
}
