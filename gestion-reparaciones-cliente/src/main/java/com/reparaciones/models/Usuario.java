package com.reparaciones.models;

/**
 * Usuario autenticado del sistema.
 * <p>El campo {@code rol} puede ser {@code "ADMIN"} o {@code "TECNICO"}.
 * {@code idTec} es nullable: los técnicos siempre lo tienen; los admins
 * que también reparan pueden tenerlo asignado en BD.</p>
 * <p>{@code nombreTecnico} y {@code activo} solo se rellenan en consultas
 * con JOIN sobre la tabla Tecnico (p. ej. {@code getUsuariosTecnicos}).
 * En login se dejan {@code null} / {@code true} respectivamente.</p>
 */
public class Usuario {

    /** Clave primaria (ID_USU en BD). */
    private final int idUsu;

    /** Nombre de inicio de sesión. */
    private final String nombreUsuario;

    /** Rol del usuario: {@code "ADMIN"} o {@code "TECNICO"}. */
    private final String rol;

    /** ID del técnico asociado, o {@code null} si no aplica. */
    private final Integer idTec;

    /**
     * Nombre visible del técnico asociado; {@code null} si la consulta
     * no incluye JOIN con la tabla Tecnico.
     */
    private final String nombreTecnico;

    /**
     * {@code true} si la cuenta está activa.
     * En login siempre es {@code true} porque la BD solo devuelve activos.
     */
    private final boolean activo;

    /**
     * Constructor completo — usado en {@code getUsuariosTecnicos} (JOIN con Tecnico).
     *
     * @param idUsu          clave primaria del usuario
     * @param nombreUsuario  nombre de inicio de sesión
     * @param rol            {@code "ADMIN"} o {@code "TECNICO"}
     * @param idTec          ID del técnico asociado, o {@code null}
     * @param nombreTecnico  nombre visible del técnico, o {@code null}
     * @param activo         {@code true} si la cuenta está activa
     */
    public Usuario(int idUsu, String nombreUsuario, String rol, Integer idTec,
                   String nombreTecnico, boolean activo) {
        this.idUsu         = idUsu;
        this.nombreUsuario = nombreUsuario;
        this.rol           = rol;
        this.idTec         = idTec;
        this.nombreTecnico = nombreTecnico;
        this.activo        = activo;
    }

    /**
     * Constructor de login — {@code nombreTecnico} desconocido, {@code activo=true}
     * (la BD ya filtra inactivos en la consulta de autenticación).
     *
     * @param idUsu         clave primaria del usuario
     * @param nombreUsuario nombre de inicio de sesión
     * @param rol           {@code "ADMIN"} o {@code "TECNICO"}
     * @param idTec         ID del técnico asociado, o {@code null}
     */
    public Usuario(int idUsu, String nombreUsuario, String rol, Integer idTec) {
        this(idUsu, nombreUsuario, rol, idTec, null, true);
    }

    /** @return clave primaria del usuario */
    public int getIdUsu() { return idUsu; }

    /** @return nombre de inicio de sesión */
    public String getNombreUsuario() { return nombreUsuario; }

    /** @return rol del usuario: {@code "ADMIN"} o {@code "TECNICO"} */
    public String getRol() { return rol; }

    /** @return ID del técnico asociado, o {@code null} */
    public Integer getIdTec() { return idTec; }

    /** @return nombre visible del técnico, o {@code null} si no se hizo JOIN */
    public String getNombreTecnico() { return nombreTecnico; }

    /** @return {@code true} si la cuenta está activa */
    public boolean isActivo() { return activo; }

    /** @return {@code true} si el rol es {@code "ADMIN"} */
    public boolean esAdmin() { return "ADMIN".equals(rol); }
}
