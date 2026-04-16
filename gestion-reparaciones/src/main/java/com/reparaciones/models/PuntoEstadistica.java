package com.reparaciones.models;

/**
 * Un punto de datos para el gráfico de estadísticas de reparaciones.
 * Representa el número de reparaciones finalizadas por un técnico en un periodo.
 */
public class PuntoEstadistica {

    private final String nombreTecnico;
    private final String periodo;   // "2026-04-15", "2026-W15", "2026-04" según granularidad
    private final int    cantidad;

    public PuntoEstadistica(String nombreTecnico, String periodo, int cantidad) {
        this.nombreTecnico = nombreTecnico;
        this.periodo       = periodo;
        this.cantidad      = cantidad;
    }

    public String getNombreTecnico() { return nombreTecnico; }
    public String getPeriodo()       { return periodo; }
    public int    getCantidad()      { return cantidad; }
}
