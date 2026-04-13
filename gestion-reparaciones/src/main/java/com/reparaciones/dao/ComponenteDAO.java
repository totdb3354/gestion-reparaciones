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

    /** Stock actual: excluye 'otro*' porque su cantidad no se gestiona en el programa. */
    public List<Componente> getAllGestionados() throws SQLException {
        List<Componente> lista = new ArrayList<>();
        String sql = """
                SELECT c.*,
                       COALESCE(SUM(CASE WHEN cc.ESTADO IN ('pendiente','parcial')
                                        THEN cc.CANTIDAD - COALESCE(cc.CANTIDAD_RECIBIDA, 0) END), 0) AS en_camino,
                       MAX(cc.FECHA_PEDIDO) AS ultimo_pedido
                FROM Componente c
                LEFT JOIN Compra_componente cc ON cc.ID_COM = c.ID_COM
                WHERE c.TIPO NOT LIKE 'otro%'
                GROUP BY c.ID_COM
                """;
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Componente c = mapear(rs);
                c.setEnCamino(rs.getInt("en_camino"));
                Timestamp ts = rs.getTimestamp("ultimo_pedido");
                c.setUltimoPedido(ts != null ? ts.toLocalDateTime() : null);
                lista.add(c);
            }
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

    public void actualizar(Componente c) throws SQLException, com.reparaciones.utils.StaleDataException {
        String sql = "UPDATE Componente SET TIPO=?, STOCK=?, STOCK_MINIMO=? WHERE ID_COM=? AND UPDATED_AT=?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getTipo());
            ps.setInt(2, c.getStock());
            ps.setInt(3, c.getStockMinimo());
            ps.setInt(4, c.getIdCom());
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(c.getUpdatedAt()));
            if (ps.executeUpdate() == 0)
                throw new com.reparaciones.utils.StaleDataException(
                        "El componente fue modificado por otro usuario. Recarga los datos e inténtalo de nuevo.");
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
                rs.getInt("STOCK_MINIMO"),
                rs.getTimestamp("UPDATED_AT").toLocalDateTime());
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