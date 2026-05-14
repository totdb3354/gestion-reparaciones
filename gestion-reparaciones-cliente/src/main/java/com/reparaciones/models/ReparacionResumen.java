package com.reparaciones.models;

import java.time.LocalDateTime;

/**
 * Proyección de solo lectura para la tabla de reparaciones.
 * <p>Agrega en un único objeto datos de las tablas {@code Reparacion},
 * {@code Tecnico}, {@code Componente} y {@code Reparacion_componente},
 * listos para mostrar en la {@code TableView} sin consultas adicionales.</p>
 * <p>{@code tipoComponente} puede ser {@code null} si la reparación
 * todavía no tiene componente asignado (asignación pendiente).</p>
 *
 * @see com.reparaciones.dao.ReparacionDAO
 */
public class ReparacionResumen {

    private String        idRep;
    private String        imei;
    private String        nombreTecnico;
    private LocalDateTime fechaAsig;
    private LocalDateTime fechaFin;
    private String        tipoComponente;
    private String        observaciones;
    private boolean       esIncidencia;
    private boolean       esResuelto;
    private String        incidencia;
    private String        idRepAnterior;
    private int           idTec;
    private int           esSolicitud;
    private String        descripcionSolicitud;
    private String        estadoSolicitud;
    private String        tipoSolicitud;
    private int           stockSolicitud;
    private boolean       enCaminoSolicitud;
    private String        tiposSolicitud;
    private LocalDateTime updatedAt;

    /**
     * Constructor completo — llamado desde {@code ReparacionDAO.getResumenes()}.
     *
     * @param idRep                clave primaria de la reparación
     * @param imei                 IMEI del dispositivo
     * @param nombreTecnico        nombre del técnico asignado
     * @param fechaAsig            fecha de asignación
     * @param fechaFin             fecha de finalización, o {@code null} si pendiente
     * @param tipoComponente       tipo del componente usado, o {@code null}
     * @param observaciones        notas libres del técnico
     * @param esIncidencia         {@code true} si se registró como incidencia
     * @param esResuelto           {@code true} si la incidencia está resuelta
     * @param incidencia           descripción de la incidencia, o {@code null}
     * @param idRepAnterior        ID de la reparación origen si es reincidencia
     * @param idTec                ID del técnico asignado
     * @param esSolicitud          {@code 1} si es una solicitud pendiente de componente
     * @param descripcionSolicitud descripción de la solicitud, o {@code null}
     * @param updatedAt            última actualización del registro
     */
    public ReparacionResumen(String idRep, String imei, String nombreTecnico,
                             LocalDateTime fechaAsig, LocalDateTime fechaFin,
                             String tipoComponente, String observaciones,
                             boolean esIncidencia, boolean esResuelto,
                             String incidencia, String idRepAnterior, int idTec,
                             int esSolicitud, String descripcionSolicitud,
                             LocalDateTime updatedAt) {
        this.idRep                = idRep;
        this.imei                 = imei;
        this.nombreTecnico        = nombreTecnico;
        this.fechaAsig            = fechaAsig;
        this.fechaFin             = fechaFin;
        this.tipoComponente       = tipoComponente;
        this.observaciones        = observaciones;
        this.esIncidencia         = esIncidencia;
        this.esResuelto           = esResuelto;
        this.incidencia           = incidencia;
        this.idRepAnterior        = idRepAnterior;
        this.idTec                = idTec;
        this.esSolicitud          = esSolicitud;
        this.descripcionSolicitud = descripcionSolicitud;
        this.updatedAt            = updatedAt;
    }

    /** @return clave primaria de la reparación */
    public String getIdRep() { return idRep; }

    /** @return IMEI del dispositivo */
    public String getImei() { return imei; }

    /** @return nombre del técnico asignado */
    public String getNombreTecnico() { return nombreTecnico; }

    /** @return fecha de asignación */
    public LocalDateTime getFechaAsig() { return fechaAsig; }

    /** @return fecha de finalización, o {@code null} si pendiente */
    public LocalDateTime getFechaFin() { return fechaFin; }

    /**
     * Indica si la reparación está pendiente de finalizar.
     *
     * @return {@code true} si {@code fechaFin} es {@code null}
     */
    public boolean isPendiente() { return fechaFin == null; }

    /** @return tipo del componente usado, o {@code null} si no hay componente */
    public String getTipoComponente() { return tipoComponente; }

    /** @return notas libres del técnico */
    public String getObservaciones() { return observaciones; }

    /** @return {@code true} si se registró como incidencia */
    public boolean isEsIncidencia() { return esIncidencia; }

    /** @return {@code true} si la incidencia está resuelta */
    public boolean isEsResuelto() { return esResuelto; }

    /** @return descripción de la incidencia, o {@code null} */
    public String getIncidencia() { return incidencia; }

    /** @return ID de la reparación origen si es reincidencia, o {@code null} */
    public String getIdRepAnterior() { return idRepAnterior; }

    /** @return ID del técnico asignado */
    public int getIdTec() { return idTec; }

    /** @return {@code 1} si es una solicitud pendiente de componente, {@code 0} si no */
    public int getEsSolicitud() { return esSolicitud; }

    /** @return descripción de la solicitud de componente, o {@code null} */
    public String getDescripcionSolicitud() { return descripcionSolicitud; }

    /** @return última actualización del registro en BD */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setEsIncidencia(boolean esIncidencia)               { this.esIncidencia = esIncidencia; }
    public void setIncidencia(String incidencia)                    { this.incidencia = incidencia; }
    public void setEsResuelto(boolean esResuelto)                   { this.esResuelto = esResuelto; }
    public void setEsSolicitud(int esSolicitud)                     { this.esSolicitud = esSolicitud; }
    public void setDescripcionSolicitud(String descripcionSolicitud) { this.descripcionSolicitud = descripcionSolicitud; }
    public void setIdTec(int idTec)                                 { this.idTec = idTec; }
    public void setNombreTecnico(String nombreTecnico)              { this.nombreTecnico = nombreTecnico; }
    public void setEstadoSolicitud(String estadoSolicitud)          { this.estadoSolicitud = estadoSolicitud; }
    public void setTipoSolicitud(String tipoSolicitud)              { this.tipoSolicitud = tipoSolicitud; }
    public void setStockSolicitud(int stockSolicitud)               { this.stockSolicitud = stockSolicitud; }
    public void setEnCaminoSolicitud(boolean enCaminoSolicitud)     { this.enCaminoSolicitud = enCaminoSolicitud; }
    public void setTiposSolicitud(String tiposSolicitud)            { this.tiposSolicitud = tiposSolicitud; }

    public String  getEstadoSolicitud()    { return estadoSolicitud; }
    public String  getTipoSolicitud()      { return tipoSolicitud; }
    public int     getStockSolicitud()     { return stockSolicitud; }
    public boolean isEnCaminoSolicitud()   { return enCaminoSolicitud; }
    public String  getTiposSolicitud()     { return tiposSolicitud; }
}
