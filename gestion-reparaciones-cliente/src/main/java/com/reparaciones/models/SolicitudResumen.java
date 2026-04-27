package com.reparaciones.models;

import java.time.LocalDateTime;

public class SolicitudResumen {

    private int           idRc;
    private String        idRep;
    private String        imei;
    private String        nombreTecnico;
    private int           idCom;
    private String        tipoComponente;
    private String        descripcion;
    private String        estado;
    private LocalDateTime fechaSolicitud;

    public SolicitudResumen(int idRc, String idRep, String imei,
                            String nombreTecnico, int idCom, String tipoComponente,
                            String descripcion, String estado, LocalDateTime fechaSolicitud) {
        this.idRc           = idRc;
        this.idRep          = idRep;
        this.imei           = imei;
        this.nombreTecnico  = nombreTecnico;
        this.idCom          = idCom;
        this.tipoComponente = tipoComponente;
        this.descripcion    = descripcion;
        this.estado         = estado;
        this.fechaSolicitud = fechaSolicitud;
    }

    public int           getIdRc()           { return idRc; }
    public String        getIdRep()          { return idRep; }
    public String        getImei()           { return imei; }
    public String        getNombreTecnico()  { return nombreTecnico; }
    public int           getIdCom()          { return idCom; }
    public String        getTipoComponente() { return tipoComponente; }
    public String        getDescripcion()    { return descripcion; }
    public String        getEstado()         { return estado; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }

    public void setEstado(String estado) { this.estado = estado; }
}
