package com.reparaciones.models;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ReparacionResumenTest {

    private ReparacionResumen reparacion(LocalDateTime fechaFin) {
        return new ReparacionResumen(
                "REP-001", "123456789012345", "Daniel García",
                LocalDateTime.now(), fechaFin,
                "Batería", "Sin observaciones",
                false, false, null, null, 1, 0, null, LocalDateTime.now());
    }

    @Test
    void isPendiente_sinFechaFin_esTrue() {
        assertTrue(reparacion(null).isPendiente());
    }

    @Test
    void isPendiente_conFechaFin_esFalse() {
        assertFalse(reparacion(LocalDateTime.now()).isPendiente());
    }

    @Test
    void isEsIncidencia_porDefecto_esFalse() {
        assertFalse(reparacion(null).isEsIncidencia());
    }

    @Test
    void getIdRep_devuelveValorCorrecto() {
        assertEquals("REP-001", reparacion(null).getIdRep());
    }

    @Test
    void getImei_devuelveValorCorrecto() {
        assertEquals("123456789012345", reparacion(null).getImei());
    }

    @Test
    void tipoComponente_nullable_devuelveNull() {
        ReparacionResumen r = new ReparacionResumen(
                "REP-002", "123456789012345", "Daniel García",
                LocalDateTime.now(), null,
                null, null,
                false, false, null, null, 1, 0, null, LocalDateTime.now());
        assertNull(r.getTipoComponente());
    }
}
