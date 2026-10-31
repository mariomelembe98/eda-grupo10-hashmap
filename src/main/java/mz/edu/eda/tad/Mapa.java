package mz.edu.eda.tad;

import java.util.List;

/** Contrato do TAD mapa: associa uma chave unica a um valor. */
public interface Mapa<K, V> {
    V colocar(K chave, V valor);
    V obter(K chave);
    V remover(K chave);
    boolean contemChave(K chave);
    int tamanho();
    boolean estaVazio();
    void limpar();
    List<K> chaves();
    List<V> valores();
}
