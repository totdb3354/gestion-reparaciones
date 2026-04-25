package com.reparaciones.utils;

/**
 * Excepción de control de concurrencia optimista.
 * <p>Se lanza cuando un {@code UPDATE} con comprobación de {@code UPDATED_AT}
 * no afecta a ninguna fila, lo que indica que otro usuario modificó el registro
 * entre la última lectura y el intento de escritura.</p>
 * <p>El controlador que la recibe debe informar al usuario y pedirle que
 * recargue los datos antes de reintentar la operación.</p>
 */
public class StaleDataException extends java.sql.SQLException {

    /**
     * @param mensaje descripción del conflicto, mostrable directamente al usuario
     */
    public StaleDataException(String mensaje) {
        super(mensaje);
    }
}
