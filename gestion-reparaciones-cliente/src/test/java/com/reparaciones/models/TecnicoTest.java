package com.reparaciones.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TecnicoTest {

    @Test
    void getIdTec_devuelveValorCorrecto() {
        assertEquals(3, new Tecnico(3, "Daniel García", true).getIdTec());
    }

    @Test
    void getNombre_devuelveValorCorrecto() {
        assertEquals("Daniel García", new Tecnico(3, "Daniel García", true).getNombre());
    }

    @Test
    void setIdTec_actualizaCorrectamente() {
        Tecnico t = new Tecnico(1, "Daniel García", true);
        t.setIdTec(5);
        assertEquals(5, t.getIdTec());
    }

    @Test
    void setNombre_actualizaCorrectamente() {
        Tecnico t = new Tecnico(1, "Daniel García", true);
        t.setNombre("Angelo López");
        assertEquals("Angelo López", t.getNombre());
    }

    @Test
    void toString_devuelveNombre() {
        assertEquals("Daniel García", new Tecnico(1, "Daniel García", true).toString());
    }
}
