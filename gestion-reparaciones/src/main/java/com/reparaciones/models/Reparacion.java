package com.reparaciones.models;

import java.time.LocalDateTime;

public class Reparacion {

    private String        idRep;
    private LocalDateTime fechaAsig;
    private LocalDateTime fechaFin;
    private String        imei;
    private int           idTec;

    public Reparacion(String idRep, LocalDateTime fechaAsig, LocalDateTime fechaFin, String imei, int idTec) {
        this.idRep     = idRep;
        this.fechaAsig = fechaAsig;
        this.fechaFin  = fechaFin;
        this.imei      = imei;
        this.idTec     = idTec;
    }

    public String getIdRep()                    { return idRep; }
    public void setIdRep(String idRep)          { this.idRep = idRep; }

    public LocalDateTime getFechaAsig()                      { return fechaAsig; }
    public void setFechaAsig(LocalDateTime fechaAsig)        { this.fechaAsig = fechaAsig; }

    public LocalDateTime getFechaFin()                       { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin)          { this.fechaFin = fechaFin; }

    public String getImei()                     { return imei; }
    public void setImei(String imei)            { this.imei = imei; }

    public int getIdTec()                       { return idTec; }
    public void setIdTec(int idTec)             { this.idTec = idTec; }
}