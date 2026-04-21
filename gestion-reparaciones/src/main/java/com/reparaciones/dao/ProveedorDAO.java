package com.reparaciones.dao;

import com.reparaciones.models.Proveedor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla {@code Proveedor}.
 * <p>Un proveedor puede desactivarse lógicamente si tiene pedidos históricos
 * (no se puede eliminar). Si no tiene pedidos, se puede borrar físicamente.</p>
 *
 * @role ADMIN
 */
public class ProveedorDAO {

    // ─── Lectura ──────────────────────────────────────────────────────────────

    /**
     * Devuelve todos los proveedores, activos e inactivos, ordenados por nombre.
     *
     * @return lista completa de proveedores
     * @throws SQLException si falla la consulta
     */
    public List<Proveedor> getAll() throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT * FROM Proveedor ORDER BY NOMBRE ASC";
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /**
     * Devuelve solo los proveedores activos, ordenados por nombre.
     * <p>Usado en los desplegables de nuevos pedidos para ocultar inactivos.</p>
     *
     * @return lista de proveedores activos
     * @throws SQLException si falla la consulta
     */
    public List<Proveedor> getActivos() throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT * FROM Proveedor WHERE ACTIVO = TRUE ORDER BY NOMBRE ASC";
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /**
     * Comprueba si el proveedor tiene pedidos históricos.
     * <p>Si devuelve {@code true}, solo puede desactivarse con {@link #setActivo};
     * no puede eliminarse físicamente.</p>
     *
     * @param idProv ID del proveedor a comprobar
     * @return {@code true} si existe al menos un pedido para este proveedor
     * @throws SQLException si falla la consulta
     */
    public boolean tienePedidos(int idProv) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Compra_componente WHERE ID_PROV = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProv);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // ─── Escritura ────────────────────────────────────────────────────────────

    /**
     * Inserta un nuevo proveedor activo en BD.
     *
     * @param nombre nombre comercial del proveedor
     * @throws SQLException si falla el insert
     */
    public void insertar(String nombre) throws SQLException {
        String sql = "INSERT INTO Proveedor (NOMBRE) VALUES (?)";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        }
    }

    /**
     * Activa o desactiva un proveedor.
     *
     * @param idProv ID del proveedor
     * @param activo {@code true} para activar, {@code false} para desactivar
     * @throws SQLException si falla el update
     */
    public void setActivo(int idProv, boolean activo) throws SQLException {
        String sql = "UPDATE Proveedor SET ACTIVO = ? WHERE ID_PROV = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, idProv);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina físicamente el proveedor de BD.
     * <p>Solo llamar cuando {@link #tienePedidos(int)} devuelve {@code false}.</p>
     *
     * @param idProv ID del proveedor a eliminar
     * @throws SQLException si falla el delete
     */
    public void borrar(int idProv) throws SQLException {
        String sql = "DELETE FROM Proveedor WHERE ID_PROV = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProv);
            ps.executeUpdate();
        }
    }

    // ─── Mapeo ────────────────────────────────────────────────────────────────

    /**
     * Actualiza la divisa habitual de un proveedor.
     *
     * @param idProv ID del proveedor
     * @param divisa código de divisa ("EUR", "USD", …)
     * @throws SQLException si falla el update
     */
    public void setDivisa(int idProv, String divisa) throws SQLException {
        String sql = "UPDATE Proveedor SET DIVISA = ? WHERE ID_PROV = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, divisa);
            ps.setInt(2, idProv);
            ps.executeUpdate();
        }
    }

    /** Mapea una fila del {@code ResultSet} a un {@link com.reparaciones.models.Proveedor}. */
    private Proveedor mapear(ResultSet rs) throws SQLException {
        return new Proveedor(
                rs.getInt("ID_PROV"),
                rs.getString("NOMBRE"),
                rs.getBoolean("ACTIVO"),
                rs.getString("DIVISA")
        );
    }
}
