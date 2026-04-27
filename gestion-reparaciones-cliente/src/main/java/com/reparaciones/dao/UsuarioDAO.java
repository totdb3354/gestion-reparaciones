package com.reparaciones.dao;

import com.google.gson.JsonObject;
import com.reparaciones.models.Usuario;
import com.reparaciones.utils.ApiClient;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de usuarios del sistema vía API REST.
 * <p>Gestiona autenticación, altas, bajas lógicas y eliminaciones
 * definitivas de cuentas de técnicos. Los administradores no se gestionan desde
 * aquí; sus cuentas se crean directamente en BD.</p>
 *
 * @role ADMIN (gestión de cuentas); login accesible a todos los roles
 */
public class UsuarioDAO {

    /**
     * Autentica al usuario contra el servidor y guarda el JWT de sesión.
     * <p>Si las credenciales son incorrectas el servidor devuelve 401
     * y este método retorna {@code null}. Cualquier otro error (red caída,
     * servidor no disponible) se propaga como {@link SQLException}.</p>
     *
     * @param nombreUsuario nombre de inicio de sesión
     * @param password      contraseña en texto plano
     * @return el {@link Usuario} autenticado, o {@code null} si las credenciales son incorrectas
     * @throws SQLException si falla la comunicación con el servidor
     */
    public Usuario login(String nombreUsuario, String password) throws SQLException {
        try {
            JsonObject resp = ApiClient.post(
                    "/api/auth/login",
                    Map.of("usuario", nombreUsuario, "password", password),
                    JsonObject.class);
            if (resp == null) return null;
            ApiClient.setToken(resp.get("token").getAsString());
            int idUsu     = resp.get("idUsu").getAsInt();
            String rol    = resp.get("rol").getAsString();
            Integer idTec = resp.get("idTec").isJsonNull() ? null : resp.get("idTec").getAsInt();
            return new Usuario(idUsu, nombreUsuario, rol, idTec);
        } catch (SQLException e) {
            // 401 en el contexto de login significa credenciales incorrectas, no sesión expirada
            if (e.getMessage() != null && e.getMessage().startsWith("Sesión expirada")) return null;
            throw e;
        }
    }

    /**
     * Devuelve todos los usuarios con rol {@code TECNICO} para la tabla de gestión.
     *
     * @return lista de técnicos ordenada alfabéticamente por nombre
     * @throws SQLException si falla la llamada al servidor
     */
    public List<Usuario> getUsuariosTecnicos() throws SQLException {
        return ApiClient.getList("/api/usuarios/tecnicos", Usuario.class);
    }

    /**
     * Desactiva la cuenta del técnico.
     *
     * @param idTec ID del técnico a desactivar
     * @throws SQLException si falla la llamada al servidor
     */
    public void desactivarTecnico(int idTec) throws SQLException {
        ApiClient.patch("/api/usuarios/tecnicos/" + idTec + "/desactivar", null);
    }

    /**
     * Reactiva la cuenta del técnico.
     *
     * @param idTec ID del técnico a reactivar
     * @throws SQLException si falla la llamada al servidor
     */
    public void activarTecnico(int idTec) throws SQLException {
        ApiClient.patch("/api/usuarios/tecnicos/" + idTec + "/activar", null);
    }

    /**
     * Comprueba si el técnico tiene alguna reparación asociada.
     *
     * @param idTec ID del técnico a comprobar
     * @return {@code true} si existe al menos una reparación para este técnico
     * @throws SQLException si falla la llamada al servidor
     */
    public boolean tieneReparaciones(int idTec) throws SQLException {
        return ApiClient.getBoolean("/api/usuarios/tecnicos/" + idTec + "/tiene-reparaciones");
    }

    /**
     * Elimina completamente el técnico: borra usuario y técnico en una sola transacción.
     * <p>Solo llamar cuando {@link #tieneReparaciones(int)} devuelve {@code false}.</p>
     *
     * @param idUsu ID del usuario a eliminar
     * @param idTec ID del técnico a eliminar
     * @throws SQLException si falla la llamada al servidor
     */
    public void eliminarTecnico(int idUsu, int idTec) throws SQLException {
        ApiClient.delete("/api/usuarios/tecnicos/" + idTec + "?idUsu=" + idUsu);
    }

    /**
     * Registra un nuevo técnico: crea técnico y usuario en una sola transacción.
     * <p>El hash de la contraseña lo realiza el servidor.</p>
     *
     * @param nombreTecnico  nombre visible del técnico
     * @param nombreUsuario  nombre de inicio de sesión
     * @param password       contraseña en texto plano
     * @throws SQLException si el nombre de usuario ya existe o falla la transacción
     */
    public void registrarTecnico(String nombreTecnico, String nombreUsuario, String password) throws SQLException {
        ApiClient.post("/api/usuarios/tecnicos", Map.of(
                "nombreTecnico", nombreTecnico,
                "nombreUsuario", nombreUsuario,
                "password",      password));
    }
}
