package com.reparaciones.dao;

import com.reparaciones.models.Proveedor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO {

    // ─── Lectura ──────────────────────────────────────────────────────────────

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

    public void insertar(String nombre) throws SQLException {
        String sql = "INSERT INTO Proveedor (NOMBRE) VALUES (?)";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        }
    }

    public void setActivo(int idProv, boolean activo) throws SQLException {
        String sql = "UPDATE Proveedor SET ACTIVO = ? WHERE ID_PROV = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, idProv);
            ps.executeUpdate();
        }
    }

    public void borrar(int idProv) throws SQLException {
        String sql = "DELETE FROM Proveedor WHERE ID_PROV = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProv);
            ps.executeUpdate();
        }
    }

    // ─── Mapeo ────────────────────────────────────────────────────────────────

    private Proveedor mapear(ResultSet rs) throws SQLException {
        return new Proveedor(
                rs.getInt("ID_PROV"),
                rs.getString("NOMBRE"),
                rs.getBoolean("ACTIVO")
        );
    }
}
