package com.reparaciones.models;

public class FilaReparacion {

    private final int     idCom;
    private final int     cantidad;
    private final boolean reutilizado;
    private final String  observacion;
    private final String  prefijo;
    private final boolean esSolicitud;
    private final String  descripcionSolicitud;

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

    public int     getIdCom()                 { return idCom; }
    public int     getCantidad()              { return cantidad; }
    public boolean isReutilizado()            { return reutilizado; }
    public String  getObservacion()           { return observacion; }
    public String  getPrefijo()               { return prefijo; }
    public boolean isEsSolicitud()            { return esSolicitud; }
    public String  getDescripcionSolicitud()  { return descripcionSolicitud; }
}