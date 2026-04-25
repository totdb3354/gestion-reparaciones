package com.reparaciones.dao;

import com.reparaciones.models.Proveedor;
import com.reparaciones.utils.ApiClient;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de la tabla {@code Proveedor} vía API REST.
 * <p>Un proveedor puede desactivarse lógicamente si tiene pedidos históricos
 * (no se puede eliminar). Si no tiene pedidos, se puede borrar físicamente.</p>
 *
 * @role ADMIN
 */
public class ProveedorDAO {

    /**
     * Devuelve todos los proveedores, activos e inactivos, ordenados por nombre.
     *
     * @return lista completa de proveedores
     * @throws SQLException si falla la llamada al servidor
     */
    public List<Proveedor> getAll() throws SQLException {
        return ApiClient.getList("/api/proveedores", Proveedor.class);
    }

    /**
     * Devuelve solo los proveedores activos, ordenados por nombre.
     *
     * @return lista de proveedores activos
     * @throws SQLException si falla la llamada al servidor
     */
    public List<Proveedor> getActivos() throws SQLException {
        return ApiClient.getList("/api/proveedores/activos", Proveedor.class);
    }

    /**
     * Comprueba si el proveedor tiene pedidos históricos.
     *
     * @param idProv ID del proveedor a comprobar
     * @return {@code true} si existe al menos un pedido para este proveedor
     * @throws SQLException si falla la llamada al servidor
     */
    public boolean tienePedidos(int idProv) throws SQLException {
        return ApiClient.getBoolean("/api/proveedores/" + idProv + "/tiene-pedidos");
    }

    /**
     * Inserta un nuevo proveedor activo.
     *
     * @param nombre nombre comercial del proveedor
     * @throws SQLException si falla la llamada al servidor
     */
    public void insertar(String nombre) throws SQLException {
        ApiClient.post("/api/proveedores", Map.of("nombre", nombre));
    }

    /**
     * Activa o desactiva un proveedor.
     *
     * @param idProv ID del proveedor
     * @param activo {@code true} para activar, {@code false} para desactivar
     * @throws SQLException si falla la llamada al servidor
     */
    public void setActivo(int idProv, boolean activo) throws SQLException {
        ApiClient.patch("/api/proveedores/" + idProv + "/activo", Map.of("activo", activo));
    }

    /**
     * Elimina físicamente el proveedor.
     * <p>Solo llamar cuando {@link #tienePedidos(int)} devuelve {@code false}.</p>
     *
     * @param idProv ID del proveedor a eliminar
     * @throws SQLException si falla la llamada al servidor
     */
    public void borrar(int idProv) throws SQLException {
        ApiClient.delete("/api/proveedores/" + idProv);
    }

    /**
     * Actualiza la divisa habitual de un proveedor.
     *
     * @param idProv ID del proveedor
     * @param divisa código de divisa ("EUR", "USD", …)
     * @throws SQLException si falla la llamada al servidor
     */
    public void setDivisa(int idProv, String divisa) throws SQLException {
        ApiClient.patch("/api/proveedores/" + idProv + "/divisa", Map.of("divisa", divisa));
    }
}
