package mz.edu.eda.modelo;

import java.io.Serializable;
import java.util.Objects;

public final class Veiculo implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String matricula;
    private final String marca;
    private final String modelo;
    private final String cor;
    private final String idProprietario;

    public Veiculo(String matricula, String marca, String modelo, String cor, String idProprietario) {
        this.matricula = validar(matricula, "Matricula");
        this.marca = validar(marca, "Marca");
        this.modelo = validar(modelo, "Modelo");
        this.cor = validar(cor, "Cor");
        this.idProprietario = validar(idProprietario, "ID do proprietario");
    }

    private static String validar(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " nao pode ser nulo");
        if (valor.isBlank()) throw new IllegalArgumentException(campo + " nao pode estar vazio");
        return valor.trim();
    }

    public String getMatricula() { return matricula; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getCor() { return cor; }
    public String getIdProprietario() { return idProprietario; }
}
