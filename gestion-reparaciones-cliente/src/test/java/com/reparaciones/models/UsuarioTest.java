package com.reparaciones.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void esAdmin_conRolAdmin_devuelveTrue() {
        Usuario u = new Usuario(1, "admin", "ADMIN", 1);
        assertTrue(u.esAdmin());
    }

    @Test
    void esAdmin_conRolTecnico_devuelveFalse() {
        Usuario u = new Usuario(2, "daniel", "TECNICO", 2);
        assertFalse(u.esAdmin());
    }

    @Test
    void isActivo_porDefectoEnConstructorLogin_esTrue() {
        Usuario u = new Usuario(1, "admin", "ADMIN", 1);
        assertTrue(u.isActivo());
    }

    @Test
    void isActivo_constructorCompleto_respetaValor() {
        Usuario activo   = new Usuario(1, "daniel", "TECNICO", 1, "Daniel García", true);
        Usuario inactivo = new Usuario(2, "angelo", "TECNICO", 2, "Angelo López", false);
        assertTrue(activo.isActivo());
        assertFalse(inactivo.isActivo());
    }

    @Test
    void getNombreTecnico_constructorLogin_esNull() {
        Usuario u = new Usuario(1, "admin", "ADMIN", 1);
        assertNull(u.getNombreTecnico());
    }

    @Test
    void getNombreTecnico_constructorCompleto_devuelveNombre() {
        Usuario u = new Usuario(1, "daniel", "TECNICO", 1, "Daniel García", true);
        assertEquals("Daniel García", u.getNombreTecnico());
    }
}
