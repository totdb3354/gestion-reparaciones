package com.reparaciones.dao;

import com.reparaciones.models.Tecnico;
import com.reparaciones.utils.ApiClient;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de la tabla {@code Tecnico} vía API REST.
 * <p>Proporciona operaciones CRUD básicas sobre técnicos.
 * La activación/desactivación de cuentas se gestiona en {@link UsuarioDAO}.</p>
 *
 * @role ADMIN
 */
public class TecnicoDAO {

    /**
     * Devuelve todos los técnicos, activos e inactivos.
     *
     * @return lista completa de técnicos
     * @throws SQLException si falla la llamada al servidor
     */
    public List<Tecnico> getAll() throws SQLException {
        return ApiClient.getList("/api/tecnicos", Tecnico.class);
    }

    /**
     * Devuelve solo los técnicos activos ({@code ACTIVO = TRUE}).
     *
     * @return lista de técnicos activos
     * @throws SQLException si falla la llamada al servidor
     */
    public List<Tecnico> getAllActivos() throws SQLException {
        return ApiClient.getList("/api/tecnicos/activos", Tecnico.class);
    }

    /**
     * Inserta un nuevo técnico.
     * <p>Usar preferentemente {@link UsuarioDAO#registrarTecnico} que crea
     * técnico y usuario en una sola transacción.</p>
     *
     * @param t técnico a insertar (se usa solo {@code nombre})
     * @throws SQLException si falla la llamada al servidor
     */
    public void insertar(Tecnico t) throws SQLException {
        ApiClient.post("/api/tecnicos", Map.of("nombre", t.getNombre()));
    }

    /**
     * Elimina el técnico.
     * <p>Solo llamar cuando el técnico no tiene reparaciones asociadas.
     * Preferir {@link UsuarioDAO#eliminarTecnico} que borra también el usuario
     * en transacción.</p>
     *
     * @param idTec ID del técnico a eliminar
     * @throws SQLException si falla la llamada al servidor
     */
    public void eliminar(int idTec) throws SQLException {
        ApiClient.delete("/api/tecnicos/" + idTec);
    }
}
