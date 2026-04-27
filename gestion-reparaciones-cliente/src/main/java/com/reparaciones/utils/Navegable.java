package com.reparaciones.utils;

import java.time.LocalDate;

/**
 * Callback que permite a una vista secundaria solicitar navegación a la vista
 * de reparaciones con un filtro de fecha y técnico pre-aplicado.
 * <p>Lo implementa {@code MainController} como lambda y se inyecta en
 * {@code EstadisticasController} mediante {@code setNavegacion()}. Al hacer
 * clic en un vértice del gráfico, el controlador invoca este callback para
 * navegar directamente a la pestaña de historial con los filtros del punto
 * seleccionado.</p>
 */
public interface Navegable {

    /**
     * Navega a la vista de historial de reparaciones con los filtros indicados ya aplicados.
     *
     * @param desde   fecha de inicio del filtro (inclusive)
     * @param hasta   fecha de fin del filtro (inclusive)
     * @param tecnico nombre del técnico a filtrar, o {@code null} para mostrar todos
     */
    void navegarAReparaciones(LocalDate desde, LocalDate hasta, String tecnico);
}
