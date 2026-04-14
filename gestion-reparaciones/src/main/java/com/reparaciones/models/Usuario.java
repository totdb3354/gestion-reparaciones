package com.reparaciones.models;

/**
 * Modelo de usuario del sistema.
 * ROL puede ser ADMIN o TECNICO.
 * idTec es nullable — los admins que reparan también tienen ID_TEC asignado en BD.
 * nombreTecnico y activo se rellenan solo en consultas que hacen JOIN con Tecnico
 * (p.ej. getUsuariosTecnicos). En login se dejan null/true por defecto.
 */
public class Usuario {

    private final int     idUsu;
    private final String  nombreUsuario;
    private final String  rol;
    private final Integer idTec;
    private final String  nombreTecnico; // null si no se hizo JOIN
    private final boolean activo;        // true por defecto en login

    /** Constructor completo — usado en getUsuariosTecnicos (JOIN con Tecnico). */
    public Usuario(int idUsu, String nombreUsuario, String rol, Integer idTec,
                   String nombreTecnico, boolean activo) {
        this.idUsu         = idUsu;
        this.nombreUsuario = nombreUsuario;
        this.rol           = rol;
        this.idTec         = idTec;
        this.nombreTecnico = nombreTecnico;
        this.activo        = activo;
    }

    /** Constructor de login — nombreTecnico desconocido, activo=true (ya validado en BD). */
    public Usuario(int idUsu, String nombreUsuario, String rol, Integer idTec) {
        this(idUsu, nombreUsuario, rol, idTec, null, true);
    }

    public int     getIdUsu()          { return idUsu; }
    public String  getNombreUsuario()  { return nombreUsuario; }
    public String  getRol()            { return rol; }
    public Integer getIdTec()          { return idTec; }
    public String  getNombreTecnico()  { return nombreTecnico; }
    public boolean isActivo()          { return activo; }
    public boolean esAdmin()           { return "ADMIN".equals(rol); }
}