package com.reparaciones;

import com.reparaciones.models.Usuario;

/**
 * Almacén estático del usuario autenticado durante la sesión activa.
 * <p>Se rellena en {@link #iniciar(Usuario)} tras un login correcto y se limpia en
 * {@link #cerrar()} al cerrar sesión. Cualquier controlador puede acceder a los datos
 * del usuario sin necesidad de inyección o paso explícito de parámetros.</p>
 *
 * <p><b>Nota:</b> este patrón de estado global es adecuado para una aplicación de
 * escritorio monousuario. En una arquitectura cliente-servidor se reemplazaría por
 * un token de sesión en el contexto HTTP.</p>
 */
public class Sesion {

    private static Usuario usuarioActual;

    /**
     * Inicia la sesión guardando el usuario autenticado.
     *
     * @param usuario usuario devuelto por el login exitoso
     */
    public static void iniciar(Usuario usuario) { usuarioActual = usuario; }

    /** Cierra la sesión borrando el usuario almacenado. */
    public static void cerrar() { usuarioActual = null; }

    /** @return usuario actualmente autenticado, o {@code null} si no hay sesión */
    public static Usuario getUsuario() { return usuarioActual; }

    /** @return {@code true} si el usuario en sesión tiene rol {@code "ADMIN"} */
    public static boolean esAdmin() { return usuarioActual != null && usuarioActual.esAdmin(); }

    /** @return ID del técnico del usuario en sesión, o {@code null} si no tiene técnico asociado */
    public static Integer getIdTec() { return usuarioActual != null ? usuarioActual.getIdTec() : null; }

    /** @return {@code true} si hay un usuario autenticado */
    public static boolean haySession() { return usuarioActual != null; }
}
