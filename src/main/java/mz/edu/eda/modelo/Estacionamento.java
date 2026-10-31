package mz.edu.eda.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public final class Estacionamento implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String identificador;
    private final String matricula;
    private final String idProprietario;
    private final String numeroLugar;
    private final LocalDateTime dataHoraEntrada;
    private final LocalDateTime dataHoraSaida;

    public Estacionamento(String identificador, String matricula, String idProprietario,
                          String numeroLugar, LocalDateTime dataHoraEntrada, LocalDateTime dataHoraSaida) {
        this.identificador = validar(identificador, "Identificador");
        this.matricula = validar(matricula, "Matricula");
        this.idProprietario = validar(idProprietario, "ID do proprietario");
        this.numeroLugar = validar(numeroLugar, "Numero do lugar");
        this.dataHoraEntrada = Objects.requireNonNull(dataHoraEntrada, "Data/hora de entrada nao pode ser nula");
        if (dataHoraSaida != null && dataHoraSaida.isBefore(dataHoraEntrada))
            throw new IllegalArgumentException("Data/hora de saida nao pode ser anterior a entrada");
        this.dataHoraSaida = dataHoraSaida;
    }

    private static String validar(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " nao pode ser nulo");
        if (valor.isBlank()) throw new IllegalArgumentException(campo + " nao pode estar vazio");
        return valor.trim();
    }

    public String getIdentificador() { return identificador; }
    public String getMatricula() { return matricula; }
    public String getIdProprietario() { return idProprietario; }
    public String getNumeroLugar() { return numeroLugar; }
    public LocalDateTime getDataHoraEntrada() { return dataHoraEntrada; }
    public LocalDateTime getDataHoraSaida() { return dataHoraSaida; }
    public boolean estaActivo() { return dataHoraSaida == null; }
}
