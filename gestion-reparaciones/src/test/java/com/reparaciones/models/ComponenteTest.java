package com.reparaciones.models;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ComponenteTest {

    private Componente componente(int stock, int stockMinimo) {
        return new Componente(1, "Batería iPhone 13", LocalDateTime.now(),
                stock, stockMinimo, LocalDateTime.now());
    }

    @Test
    void getStock_devuelveValorCorrecto() {
        assertEquals(5, componente(5, 2).getStock());
    }

    @Test
    void setStock_actualizaCorrectamente() {
        Componente c = componente(5, 2);
        c.setStock(10);
        assertEquals(10, c.getStock());
    }

    @Test
    void setStockMinimo_actualizaCorrectamente() {
        Componente c = componente(5, 2);
        c.setStockMinimo(3);
        assertEquals(3, c.getStockMinimo());
    }

    @Test
    void enCamino_porDefecto_esZero() {
        assertEquals(0, componente(5, 2).getEnCamino());
    }

    @Test
    void setEnCamino_actualizaCorrectamente() {
        Componente c = componente(5, 2);
        c.setEnCamino(3);
        assertEquals(3, c.getEnCamino());
    }

    @Test
    void toString_devuelveNombreTipo() {
        Componente c = componente(5, 2);
        assertEquals("Batería iPhone 13", c.toString());
    }
}
