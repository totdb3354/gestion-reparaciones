package com.reparaciones.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProveedorTest {

    @Test
    void getIdProv_devuelveValorCorrecto() {
        Proveedor p = new Proveedor(7, "iFixit", true, "EUR");
        assertEquals(7, p.getIdProv());
    }

    @Test
    void getNombre_devuelveValorCorrecto() {
        Proveedor p = new Proveedor(1, "iFixit", true, "EUR");
        assertEquals("iFixit", p.getNombre());
    }

    @Test
    void isActivo_true_devuelveTrue() {
        assertTrue(new Proveedor(1, "iFixit", true, "EUR").isActivo());
    }

    @Test
    void isActivo_false_devuelveFalse() {
        assertFalse(new Proveedor(1, "iFixit", false, "EUR").isActivo());
    }

    @Test
    void setActivo_actualizaCorrectamente() {
        Proveedor p = new Proveedor(1, "iFixit", true, "EUR");
        p.setActivo(false);
        assertFalse(p.isActivo());
    }

    @Test
    void setNombre_actualizaCorrectamente() {
        Proveedor p = new Proveedor(1, "iFixit", true, "EUR");
        p.setNombre("MobileSentrix");
        assertEquals("MobileSentrix", p.getNombre());
    }

    @Test
    void toString_devuelveNombre() {
        Proveedor p = new Proveedor(1, "iFixit", true, "EUR");
        assertEquals("iFixit", p.toString());
    }
}
