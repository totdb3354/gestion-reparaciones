package com.reparaciones.dao;

import com.reparaciones.models.Componente;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ComponenteDAO {

    public List<Componente> getAll() throws SQLException {
        List<Componente> lista = new ArrayList<>();
        String sql = "SELECT * FROM Componente";
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Componente> getStockBajo() throws SQLException {
        List<Componente> lista = new ArrayList<>();
        String sql = "SELECT * FROM Componente WHERE STOCK <= STOCK_MINIMO";
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Componente> getChasisPorColor(String color) throws SQLException {
        List<Componente> lista = new ArrayList<>();
        String sql = "SELECT * FROM Componente WHERE TIPO LIKE 'cha%' AND TIPO LIKE ? AND STOCK > 0";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + color);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public void insertar(Componente c) throws SQLException {
        String sql = "INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES (?, ?, ?)";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getTipo());
            ps.setInt(2, c.getStock());
            ps.setInt(3, c.getStockMinimo());
            ps.executeUpdate();
        }
    }

    public void actualizar(Componente c) throws SQLException {
        String sql = "UPDATE Componente SET TIPO=?, STOCK=?, STOCK_MINIMO=? WHERE ID_COM=?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getTipo());
            ps.setInt(2, c.getStock());
            ps.setInt(3, c.getStockMinimo());
            ps.setInt(4, c.getIdCom());
            ps.executeUpdate();
        }
    }

    public void setStockMinimo(int idCom, int stockMinimo) throws SQLException {
        String sql = "UPDATE Componente SET STOCK_MINIMO = ? WHERE ID_COM = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, stockMinimo);
            ps.setInt(2, idCom);
            ps.executeUpdate();
        }
    }

    public void eliminar(int idCom) throws SQLException {
        String sql = "DELETE FROM Componente WHERE ID_COM = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCom);
            ps.executeUpdate();
        }
    }

    public void actualizarStock(int idCom, int cantidad) throws SQLException {
        String sql = "UPDATE Componente SET STOCK = STOCK + ? WHERE ID_COM = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idCom);
            ps.executeUpdate();
        }
    }

    private Componente mapear(ResultSet rs) throws SQLException {
        return new Componente(
                rs.getInt("ID_COM"),
                rs.getString("TIPO"),
                rs.getTimestamp("FECHA_REGISTRO").toLocalDateTime(),
                rs.getInt("STOCK"),
                rs.getInt("STOCK_MINIMO"));
    }

    /**
     * Devuelve todos los componentes agrupados por prefijo de tipo.
     * Prefijos reales (sin la 'i' de iPhone que va después):
     * bat, cha, g, cam, lcd, mc, otro
     * Estructura SKU: [prefijo] + i + [modelo] + [color opcional]
     */
    public Map<String, List<Componente>> getAgrupadosPorTipo() throws SQLException {
        Map<String, List<Componente>> mapa = new LinkedHashMap<>();
        // Orden fijo de las 7 filas — prefijos reales sin la 'i' de iPhone
        for (String prefijo : List.of("bat", "cha", "g", "mc", "cam", "lcd", "otro")) {
            mapa.put(prefijo, new ArrayList<>());
        }
        String sql = "SELECT * FROM Componente ORDER BY TIPO ASC";
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Componente c = mapear(rs);
                for (String prefijo : mapa.keySet()) {
                    if (c.getTipo().toLowerCase().startsWith(prefijo)) {
                        mapa.get(prefijo).add(c);
                        break;
                    }
                }
            }
        }
        return mapa;
    }
}