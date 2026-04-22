package com.reparaciones.models;

public class ReparacionComponente {

    private int    idRc;
    private String idRep;
    private int    idCom;
    private boolean esReutilizado;
    private boolean esIncidencia;
    private boolean esResuelto;
    private String  incidencia;
    private String  observaciones;
    private boolean esSolicitud;
    private String  descripcionSolicitud;
    private String  estadoSolicitud;

    public ReparacionComponente(int idRc, String idRep, int idCom,
                                boolean esReutilizado, boolean esIncidencia,
                                boolean esResuelto, String incidencia,
                                String observaciones, boolean esSolicitud,
                                String descripcionSolicitud, String estadoSolicitud) {
        this.idRc                 = idRc;
        this.idRep                = idRep;
        this.idCom                = idCom;
        this.esReutilizado        = esReutilizado;
        this.esIncidencia         = esIncidencia;
        this.esResuelto           = esResuelto;
        this.incidencia           = incidencia;
        this.observaciones        = observaciones;
        this.esSolicitud          = esSolicitud;
        this.descripcionSolicitud = descripcionSolicitud;
        this.estadoSolicitud      = estadoSolicitud;
    }

    public int     getIdRc()                { return idRc; }
    public String  getIdRep()               { return idRep; }
    public int     getIdCom()               { return idCom; }
    public boolean isEsReutilizado()        { return esReutilizado; }
    public boolean isEsIncidencia()         { return esIncidencia; }
    public boolean isEsResuelto()           { return esResuelto; }
    public String  getIncidencia()          { return incidencia; }
    public String  getObservaciones()       { return observaciones; }
    public boolean isEsSolicitud()          { return esSolicitud; }
    public String  getDescripcionSolicitud(){ return descripcionSolicitud; }
    public String  getEstadoSolicitud()     { return estadoSolicitud; }

    public void setIdRc(int idRc)                               { this.idRc = idRc; }
    public void setIdRep(String idRep)                         { this.idRep = idRep; }
    public void setIdCom(int idCom)                            { this.idCom = idCom; }
    public void setEsReutilizado(boolean esReutilizado)        { this.esReutilizado = esReutilizado; }
    public void setEsIncidencia(boolean esIncidencia)          { this.esIncidencia = esIncidencia; }
    public void setEsResuelto(boolean esResuelto)              { this.esResuelto = esResuelto; }
    public void setIncidencia(String incidencia)               { this.incidencia = incidencia; }
    public void setObservaciones(String observaciones)         { this.observaciones = observaciones; }
    public void setEsSolicitud(boolean esSolicitud)            { this.esSolicitud = esSolicitud; }
    public void setDescripcionSolicitud(String d)              { this.descripcionSolicitud = d; }
    public void setEstadoSolicitud(String estadoSolicitud)     { this.estadoSolicitud = estadoSolicitud; }
}
