package com.reparaciones.models;

/**
 * Representa a un técnico de reparaciones.
 * <p>Un técnico puede estar activo o inactivo (campo {@code ACTIVO} en BD).
 * Los inactivos no aparecen en el gráfico de estadísticas pero siguen
 * contando en el total acumulado.</p>
 */
public class Tecnico {

    /** Clave primaria (ID_TEC en BD). */
    private int idTec;

    /** Nombre visible del técnico. */
    private String nombre;

    /** {@code true} si el técnico está activo. */
    private boolean activo;

    /**
     * @param idTec  clave primaria del técnico
     * @param nombre nombre visible
     * @param activo {@code true} si está activo
     */
    public Tecnico(int idTec, String nombre, boolean activo) {
        this.idTec  = idTec;
        this.nombre = nombre;
        this.activo = activo;
    }

    /** @return clave primaria del técnico */
    public int getIdTec() { return idTec; }
    public void setIdTec(int idTec) { this.idTec = idTec; }

    /** @return nombre visible del técnico */
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    /** @return {@code true} si el técnico está activo */
    public boolean isActivo() { return activo; }

    /** Devuelve el nombre para uso en ComboBox y MenuButton. */
    @Override
    public String toString() { return nombre; }
}
