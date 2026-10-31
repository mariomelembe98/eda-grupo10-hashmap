package mz.edu.eda.tad;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class MeuHashMapTest {
    @Test void inserirConsultarActualizarERemover() {
        Mapa<String, Integer> mapa = new MeuHashMap<>();
        assertTrue(mapa.estaVazio());
        assertNull(mapa.colocar("A", 1));
        assertEquals(1, mapa.colocar("A", 2));
        assertEquals(2, mapa.obter("A"));
        assertEquals(1, mapa.tamanho());
        assertTrue(mapa.contemChave("A"));
        assertEquals(2, mapa.remover("A"));
        assertFalse(mapa.contemChave("A"));
        assertNull(mapa.remover("A"));
    }

    record ChaveColisao(int id) {
        @Override public int hashCode() { return 7; }
    }

    @Test void colisoesSaoResolvidas() {
        Mapa<ChaveColisao, String> mapa = new MeuHashMap<>(2);
        for (int i = 0; i < 30; i++) mapa.colocar(new ChaveColisao(i), "v" + i);
        for (int i = 0; i < 30; i++) assertEquals("v" + i, mapa.obter(new ChaveColisao(i)));
        assertEquals("v15", mapa.remover(new ChaveColisao(15)));
        assertEquals("v16", mapa.obter(new ChaveColisao(16)));
    }

    @Test void redimensionarPreservaRegistos() {
        Mapa<Integer, Integer> mapa = new MeuHashMap<>(1);
        for (int i = 0; i < 1000; i++) mapa.colocar(i, i * 2);
        assertEquals(1000, mapa.tamanho());
        for (int i = 0; i < 1000; i++) assertEquals(i * 2, mapa.obter(i));
        assertEquals(1000, new HashSet<>(mapa.chaves()).size());
        assertEquals(1000, mapa.valores().size());
        mapa.limpar();
        assertTrue(mapa.estaVazio());
    }

    @Test void validaArgumentos() {
        Mapa<String, String> mapa = new MeuHashMap<>();
        assertThrows(IllegalArgumentException.class, () -> new MeuHashMap<>(0));
        assertThrows(NullPointerException.class, () -> mapa.colocar(null, "v"));
        assertThrows(NullPointerException.class, () -> mapa.colocar("c", null));
        assertThrows(NullPointerException.class, () -> mapa.obter(null));
    }
}
