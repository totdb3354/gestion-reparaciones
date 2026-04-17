package com.reparaciones.dao;

import com.reparaciones.models.Tecnico;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TecnicoDAO {

    public List<Tecnico> getAll() throws SQLException {
        List<Tecnico> lista = new ArrayList<>();
        String sql = "SELECT * FROM Tecnico";
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Tecnico(
                    rs.getInt("ID_TEC"),
                    rs.getString("NOMBRE")
                ));
            }
        }
        return lista;
    }

    /** Devuelve solo los técnicos con ACTIVO = TRUE. */
    public List<Tecnico> getAllActivos() throws SQLException {
        List<Tecnico> lista = new ArrayList<>();
        String sql = "SELECT * FROM Tecnico WHERE ACTIVO = TRUE";
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Tecnico(
                    rs.getInt("ID_TEC"),
                    rs.getString("NOMBRE")
                ));
            }
        }
        return lista;
    }

    public void insertar(Tecnico t) throws SQLException {
        String sql = "INSERT INTO Tecnico (NOMBRE) VALUES (?)";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, t.getNombre());
            ps.executeUpdate();
        }
    }

    public void eliminar(int idTec) throws SQLException {
        String sql = "DELETE FROM Tecnico WHERE ID_TEC = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTec);
            ps.executeUpdate();
        }
    }
}