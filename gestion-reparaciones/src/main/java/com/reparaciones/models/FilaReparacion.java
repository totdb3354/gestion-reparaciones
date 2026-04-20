package com.reparaciones.models;

/**
 * DTO inmutable que representa una línea del formulario de finalización
 * de reparación antes de persistirse en BD.
 * <p>Cada fila corresponde a un componente seleccionado por el técnico,
 * con sus opciones de reutilización y solicitud de reposición de stock.</p>
 *
 * @see com.reparaciones.dao.ReparacionDAO
 */
public class FilaReparacion {

    /** ID del componente seleccionado. */
    private final int idCom;

    /** Número de unidades utilizadas (máximo 1 en el flujo actual). */
    private final int cantidad;

    /** {@code true} si el componente se extrajo de una reparación anterior y no resta stock. */
    private final boolean reutilizado;

    /** Notas libres del técnico sobre este componente. */
    private final String observacion;

    /** Prefijo del SKU del componente (identifica el modelo de teléfono). */
    private final String prefijo;

    /** {@code true} si el técnico solicita reponer este componente en el siguiente pedido. */
    private final boolean esSolicitud;

    /** Descripción de la solicitud de reposición, o {@code null} si no es solicitud. */
    private final String descripcionSolicitud;

    /**
     * @param idCom                ID del componente
     * @param cantidad             unidades utilizadas
     * @param reutilizado          {@code true} si no resta stock
     * @param observacion          notas del técnico
     * @param prefijo              prefijo del SKU
     * @param esSolicitud          {@code true} si solicita reposición
     * @param descripcionSolicitud descripción de la solicitud, o {@code null}
     */
    public FilaReparacion(int idCom, int cantidad, boolean reutilizado,
                          String observacion, String prefijo,
                          boolean esSolicitud, String descripcionSolicitud) {
        this.idCom                = idCom;
        this.cantidad             = cantidad;
        this.reutilizado          = reutilizado;
        this.observacion          = observacion;
        this.prefijo              = prefijo;
        this.esSolicitud          = esSolicitud;
        this.descripcionSolicitud = descripcionSolicitud;
    }

    /** @return ID del componente */
    public int getIdCom() { return idCom; }

    /** @return unidades utilizadas */
    public int getCantidad() { return cantidad; }

    /** @return {@code true} si no resta stock (componente reutilizado) */
    public boolean isReutilizado() { return reutilizado; }

    /** @return notas del técnico sobre este componente */
    public String getObservacion() { return observacion; }

    /** @return prefijo del SKU del componente */
    public String getPrefijo() { return prefijo; }

    /** @return {@code true} si solicita reposición de stock */
    public boolean isEsSolicitud() { return esSolicitud; }

    /** @return descripción de la solicitud, o {@code null} */
    public String getDescripcionSolicitud() { return descripcionSolicitud; }
}
