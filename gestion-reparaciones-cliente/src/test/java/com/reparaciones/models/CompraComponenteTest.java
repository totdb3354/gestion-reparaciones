package com.reparaciones.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompraComponenteTest {

    private CompraComponente pedido(double precioUd, double precioEur, int cantidad) {
        return new CompraComponente(
                1, 1, "Batería", 1, "Proveedor", cantidad, null,
                false, null, null,
                precioUd, "USD", precioEur,
                CompraComponente.Estado.pendiente, null);
    }

    @Test
    void getTotalPedido_calculaCorrecto() {
        CompraComponente p = pedido(10.0, 9.5, 3);
        assertEquals(30.0, p.getTotalPedido(), 0.001);
    }

    @Test
    void getTotalEur_calculaCorrecto() {
        CompraComponente p = pedido(10.0, 9.5, 3);
        assertEquals(28.5, p.getTotalEur(), 0.001);
    }

    @Test
    void getTotalPedido_conCantidadCero_esZero() {
        CompraComponente p = pedido(10.0, 9.5, 0);
        assertEquals(0.0, p.getTotalPedido(), 0.001);
    }

    @Test
    void getCantidadRecibida_nullable_devuelveNull() {
        CompraComponente p = pedido(10.0, 9.5, 5);
        assertNull(p.getCantidadRecibida());
    }

    @Test
    void estadoInicial_esPendiente() {
        CompraComponente p = pedido(10.0, 9.5, 2);
        assertEquals(CompraComponente.Estado.pendiente, p.getEstado());
    }

    @Test
    void setEstado_actualizaCorrectamente() {
        CompraComponente p = pedido(10.0, 9.5, 2);
        p.setEstado(CompraComponente.Estado.recibido);
        assertEquals(CompraComponente.Estado.recibido, p.getEstado());
    }
}
