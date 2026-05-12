package com.reparaciones.models;

import java.time.LocalDateTime;

public class SolicitudStock {

    private int           idSol;
    private int           idCom;
    private String        tipoComponente;
    private int           idUsu;
    private String        nombreUsuario;
    private String        descripcion;
    private String        estado;
    private LocalDateTime fecha;

    public SolicitudStock() {}

    public SolicitudStock(int idSol, int idCom, String tipoComponente,
                          int idUsu, String nombreUsuario,
                          String descripcion, String estado, LocalDateTime fecha) {
        this.idSol          = idSol;
        this.idCom          = idCom;
        this.tipoComponente = tipoComponente;
        this.idUsu          = idUsu;
        this.nombreUsuario  = nombreUsuario;
        this.descripcion    = descripcion;
        this.estado         = estado;
        this.fecha          = fecha;
    }

    public int           getIdSol()          { return idSol; }
    public int           getIdCom()          { return idCom; }
    public String        getTipoComponente() { return tipoComponente; }
    public int           getIdUsu()          { return idUsu; }
    public String        getNombreUsuario()  { return nombreUsuario; }
    public String        getDescripcion()    { return descripcion; }
    public String        getEstado()         { return estado; }
    public LocalDateTime getFecha()          { return fecha; }

    public void setEstado(String estado) { this.estado = estado; }
}
