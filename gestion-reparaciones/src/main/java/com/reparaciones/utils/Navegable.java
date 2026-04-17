package com.reparaciones.utils;

import java.time.LocalDate;

/**
 * Callback que permite a una vista secundaria solicitar navegación a la vista
 * de reparaciones con un filtro de fecha y técnico pre-aplicado.
 *
 * @param tecnico nombre del técnico a filtrar, o {@code null} para todos.
 */
public interface Navegable {
    void navegarAReparaciones(LocalDate desde, LocalDate hasta, String tecnico);
}
