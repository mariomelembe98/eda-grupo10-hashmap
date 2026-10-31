package mz.edu.eda;

import mz.edu.eda.tad.Mapa;
import mz.edu.eda.tad.MeuHashMap;

/** Demonstração isolada do TAD HashMap desenvolvido pelo grupo. */
public final class DemonstracaoHashMap {
    private DemonstracaoHashMap() { }

    public static void main(String[] args) {
        Mapa<String, String> veiculos = new MeuHashMap<>();
        veiculos.colocar("ABC-123-MP", "Toyota Corolla");
        veiculos.colocar("AAD-456-MP", "Honda Fit");
        System.out.println("Veiculos registados: " + veiculos.tamanho());
        System.out.println("ABC-123-MP: " + veiculos.obter("ABC-123-MP"));
        veiculos.colocar("ABC-123-MP", "Toyota Corolla actualizado");
        System.out.println("Actualizado: " + veiculos.obter("ABC-123-MP"));
        veiculos.remover("AAD-456-MP");
        System.out.println("Apos remocao: " + veiculos.tamanho());
    }
}
