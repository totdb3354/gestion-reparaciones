package com.reparaciones.models;

/**
 * Punto de datos para el gráfico de estadísticas de reparaciones.
 * <p>Cada instancia representa el número de reparaciones finalizadas
 * por un técnico concreto en un periodo determinado.
 * El formato de {@code periodo} depende de la granularidad elegida:</p>
 * <ul>
 *   <li>Día → {@code "yyyy-MM-dd"} (ISO 8601)</li>
 *   <li>Semana → {@code "yyyy-Www"} (año-semana ISO)</li>
 *   <li>Mes → {@code "yyyy-MM"}</li>
 *   <li>Año → {@code "yyyy"}</li>
 * </ul>
 *
 * @see com.reparaciones.dao.ReparacionDAO
 */
public class PuntoEstadistica {

    /** Nombre del técnico al que pertenece este punto. */
    private final String nombreTecnico;

    /** Etiqueta del periodo (formato depende de granularidad). */
    private final String periodo;

    /** Número de reparaciones finalizadas en el periodo. */
    private final int cantidad;

    /**
     * @param nombreTecnico nombre del técnico
     * @param periodo       etiqueta del periodo (formato según granularidad)
     * @param cantidad      número de reparaciones finalizadas
     */
    public PuntoEstadistica(String nombreTecnico, String periodo, int cantidad) {
        this.nombreTecnico = nombreTecnico;
        this.periodo       = periodo;
        this.cantidad      = cantidad;
    }

    /** @return nombre del técnico */
    public String getNombreTecnico() { return nombreTecnico; }

    /** @return etiqueta del periodo */
    public String getPeriodo() { return periodo; }

    /** @return número de reparaciones finalizadas */
    public int getCantidad() { return cantidad; }
}
