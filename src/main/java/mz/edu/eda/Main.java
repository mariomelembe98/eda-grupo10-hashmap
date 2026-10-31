package mz.edu.eda;

import mz.edu.eda.persistencia.RepositorioFicheiros;
import mz.edu.eda.servico.ParqueService;
import mz.edu.eda.ui.MenuConsola;

import java.io.IOException;
import java.util.Scanner;

public final class Main {
    public static void main(String[] args) {
        try {
            ParqueService service = new ParqueService(new RepositorioFicheiros());
            new MenuConsola(service, new Scanner(System.in), System.out).executar();
        } catch (IOException e) {
            System.err.println("Nao foi possivel carregar os dados do parque: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Os dados persistidos do parque sao inconsistentes: " + e.getMessage());
        }
    }
}
