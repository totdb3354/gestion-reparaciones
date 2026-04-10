package com.reparaciones.utils;

/**
 * Se lanza cuando un UPDATE detecta que el registro fue modificado
 * por otro usuario desde que se cargó (optimistic locking).
 */
public class StaleDataException extends Exception {
    public StaleDataException(String mensaje) {
        super(mensaje);
    }
}
