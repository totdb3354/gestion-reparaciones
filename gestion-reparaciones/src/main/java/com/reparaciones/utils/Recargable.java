package com.reparaciones.utils;

/** Controlador de vista principal que sabe recargarse y gestionar polling. */
public interface Recargable {
    void recargar();
    void detenerPolling();
}
