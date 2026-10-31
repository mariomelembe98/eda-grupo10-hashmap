package mz.edu.eda.tad;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Tabela hash generica com encadeamento separado e redimensionamento. */
public final class MeuHashMap<K, V> implements Mapa<K, V> {
    private static final int CAPACIDADE_INICIAL = 16;
    private static final double FACTOR_CARGA = 0.75;

    private static final class Entrada<K, V> {
        final K chave;
        V valor;
        Entrada<K, V> proxima;
        Entrada(K chave, V valor, Entrada<K, V> proxima) {
            this.chave = chave;
            this.valor = valor;
            this.proxima = proxima;
        }
    }

    private Entrada<K, V>[] tabela;
    private int tamanho;

    public MeuHashMap() { this(CAPACIDADE_INICIAL); }

    @SuppressWarnings("unchecked")
    public MeuHashMap(int capacidadeInicial) {
        if (capacidadeInicial <= 0) throw new IllegalArgumentException("Capacidade deve ser positiva");
        tabela = (Entrada<K, V>[]) new Entrada[capacidadeInicial];
    }

    private int indice(K chave, int capacidade) {
        int hash = chave.hashCode();
        hash ^= (hash >>> 16);
        return Math.floorMod(hash, capacidade);
    }

    private void validar(K chave, V valor) {
        Objects.requireNonNull(chave, "Chave nao pode ser nula");
        Objects.requireNonNull(valor, "Valor nao pode ser nulo");
    }

    @Override
    public V colocar(K chave, V valor) {
        validar(chave, valor);
        int i = indice(chave, tabela.length);
        for (Entrada<K, V> e = tabela[i]; e != null; e = e.proxima) {
            if (e.chave.equals(chave)) {
                V anterior = e.valor;
                e.valor = valor;
                return anterior;
            }
        }
        if ((tamanho + 1.0) / tabela.length > FACTOR_CARGA) {
            if (tabela.length > Integer.MAX_VALUE / 2) throw new IllegalStateException("Capacidade maxima atingida");
            redimensionar(tabela.length * 2);
            i = indice(chave, tabela.length);
        }
        tabela[i] = new Entrada<>(chave, valor, tabela[i]);
        tamanho++;
        return null;
    }

    @Override
    public V obter(K chave) {
        Objects.requireNonNull(chave, "Chave nao pode ser nula");
        int i = indice(chave, tabela.length);
        for (Entrada<K, V> e = tabela[i]; e != null; e = e.proxima) {
            if (e.chave.equals(chave)) return e.valor;
        }
        return null;
    }

    @Override
    public V remover(K chave) {
        Objects.requireNonNull(chave, "Chave nao pode ser nula");
        int i = indice(chave, tabela.length);
        Entrada<K, V> anterior = null;
        for (Entrada<K, V> e = tabela[i]; e != null; e = e.proxima) {
            if (e.chave.equals(chave)) {
                if (anterior == null) tabela[i] = e.proxima;
                else anterior.proxima = e.proxima;
                tamanho--;
                return e.valor;
            }
            anterior = e;
        }
        return null;
    }

    @Override public boolean contemChave(K chave) { return obter(chave) != null; }
    @Override public int tamanho() { return tamanho; }
    @Override public boolean estaVazio() { return tamanho == 0; }

    @Override
    public void limpar() {
        for (int i = 0; i < tabela.length; i++) tabela[i] = null;
        tamanho = 0;
    }

    @Override
    public List<K> chaves() {
        List<K> resultado = new ArrayList<>(tamanho);
        for (Entrada<K, V> bucket : tabela)
            for (Entrada<K, V> e = bucket; e != null; e = e.proxima) resultado.add(e.chave);
        return resultado;
    }

    @Override
    public List<V> valores() {
        List<V> resultado = new ArrayList<>(tamanho);
        for (Entrada<K, V> bucket : tabela)
            for (Entrada<K, V> e = bucket; e != null; e = e.proxima) resultado.add(e.valor);
        return resultado;
    }

    @SuppressWarnings("unchecked")
    private void redimensionar(int novaCapacidade) {
        Entrada<K, V>[] antiga = tabela;
        tabela = (Entrada<K, V>[]) new Entrada[novaCapacidade];
        for (Entrada<K, V> bucket : antiga) {
            Entrada<K, V> e = bucket;
            while (e != null) {
                Entrada<K, V> proxima = e.proxima;
                int i = indice(e.chave, novaCapacidade);
                e.proxima = tabela[i];
                tabela[i] = e;
                e = proxima;
            }
        }
    }
}
