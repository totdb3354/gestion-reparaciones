package com.reparaciones.models;

import java.time.LocalDateTime;

/**
 * Modelo de solo lectura para la vista de la tabla de reparaciones.
 * Agrega datos de Reparacion, Tecnico, Componente y Reparacion_componente
 * en un único objeto listo para mostrar en la TableView.
 * tipoComponente puede ser null si es una asignación sin componente todavía.
 */
public class ReparacionResumen {

    private String        idRep;
    private long          imei;
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

    public ReparacionResumen(String idRep, long imei, String nombreTecnico,
                             LocalDateTime fechaAsig, LocalDateTime fechaFin,
                             String tipoComponente, String observaciones,
                             boolean esIncidencia, boolean esResuelto,
                             String incidencia, String idRepAnterior, int idTec,
                             int esSolicitud, String descripcionSolicitud) {
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
    }

    public String        getIdRep()           { return idRep; }
    public long          getImei()            { return imei; }
    public String        getNombreTecnico()   { return nombreTecnico; }
    public LocalDateTime getFechaAsig()       { return fechaAsig; }
    public LocalDateTime getFechaFin()        { return fechaFin; }
    /** true si es asignación pendiente — sin fecha fin ni componente todavía */
    public boolean       isPendiente()        { return fechaFin == null; }
    public String        getTipoComponente()  { return tipoComponente; }
    public String        getObservaciones()   { return observaciones; }
    public boolean       isEsIncidencia()     { return esIncidencia; }
    public boolean       isEsResuelto()       { return esResuelto; }
    public String        getIncidencia()      { return incidencia; }
    public String        getIdRepAnterior()   { return idRepAnterior; }
    public int           getIdTec()           { return idTec; }

    public int    getEsSolicitud()            { return esSolicitud; }
    public String getDescripcionSolicitud()   { return descripcionSolicitud; }

    public void setEsIncidencia(boolean esIncidencia)              { this.esIncidencia = esIncidencia; }
    public void setIncidencia(String incidencia)                   { this.incidencia = incidencia; }
    public void setEsResuelto(boolean esResuelto)                  { this.esResuelto = esResuelto; }
    public void setEsSolicitud(int esSolicitud)                    { this.esSolicitud = esSolicitud; }
    public void setDescripcionSolicitud(String descripcionSolicitud){ this.descripcionSolicitud = descripcionSolicitud; }
    public void setIdTec(int idTec)                                { this.idTec = idTec; }
    public void setNombreTecnico(String nombreTecnico)             { this.nombreTecnico = nombreTecnico; }
}