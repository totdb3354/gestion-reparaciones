package com.reparaciones.dao;

import com.reparaciones.models.CompraComponente;
import com.reparaciones.models.CompraComponente.Estado;
import com.reparaciones.utils.StaleDataException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompraComponenteDAO {

    private static final String SQL_BASE = """
            SELECT cc.*, c.TIPO AS tipo_componente, p.NOMBRE AS nombre_proveedor
            FROM Compra_componente cc
            JOIN Componente c ON cc.ID_COM  = c.ID_COM
            JOIN Proveedor  p ON cc.ID_PROV = p.ID_PROV
            """;

    // ─── Lectura ──────────────────────────────────────────────────────────────

    public List<CompraComponente> getAll() throws SQLException {
        List<CompraComponente> lista = new ArrayList<>();
        String sql = SQL_BASE + " ORDER BY cc.FECHA_PEDIDO DESC, cc.ID_COMPRA DESC";
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<CompraComponente> getPendientes() throws SQLException {
        List<CompraComponente> lista = new ArrayList<>();
        String sql = SQL_BASE + " WHERE cc.ESTADO = 'pendiente' ORDER BY cc.ES_URGENTE DESC, cc.FECHA_PEDIDO ASC";
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ─── Insertar ─────────────────────────────────────────────────────────────

    public void insertar(int idCom, int idProv, int cantidad, boolean esUrgente,
                         double precioUnidad, String divisa, double precioEur) throws SQLException {
        String sql = """
                INSERT INTO Compra_componente
                    (ID_COM, ID_PROV, CANTIDAD, ES_URGENTE,
                     PRECIO_UNIDAD_PEDIDO, DIVISA, PRECIO_EUR, ESTADO)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'pendiente')
                """;
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCom);
            ps.setInt(2, idProv);
            ps.setInt(3, cantidad);
            ps.setBoolean(4, esUrgente);
            ps.setDouble(5, precioUnidad);
            ps.setString(6, divisa);
            ps.setDouble(7, precioEur);
            ps.executeUpdate();
        }
    }

    // ─── Editar pedido ────────────────────────────────────────────────────────

    public void editar(CompraComponente pedido, int idProv, int cantidad, boolean esUrgente,
                       double precioUnidad, String divisa, double precioEur)
            throws SQLException, StaleDataException {
        String sql = """
                UPDATE Compra_componente
                SET ID_PROV = ?, CANTIDAD = ?, ES_URGENTE = ?,
                    PRECIO_UNIDAD_PEDIDO = ?, DIVISA = ?, PRECIO_EUR = ?
                WHERE ID_COMPRA = ? AND UPDATED_AT = ?
                """;
        String sqlStock = """
                UPDATE Componente SET STOCK = STOCK + ?
                WHERE ID_COM = (SELECT ID_COM FROM Compra_componente WHERE ID_COMPRA = ?)
                """;
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, idProv);
                    ps.setInt(2, cantidad);
                    ps.setBoolean(3, esUrgente);
                    ps.setDouble(4, precioUnidad);
                    ps.setString(5, divisa);
                    ps.setDouble(6, precioEur);
                    ps.setInt(7, pedido.getIdCompra());
                    ps.setTimestamp(8, Timestamp.valueOf(pedido.getUpdatedAt()));
                    if (ps.executeUpdate() == 0)
                        throw new StaleDataException(
                                "El pedido fue modificado por otro usuario. Recarga los datos e inténtalo de nuevo.");
                }
                // Si el pedido ya estaba recibido, ajustar stock por la diferencia
                if (pedido.getEstado() == Estado.recibido) {
                    int diferencia = cantidad - pedido.getCantidad();
                    if (diferencia != 0) {
                        if (diferencia < 0) {
                            String sqlCheckStock = """
                                    SELECT c.STOCK FROM Componente c
                                    JOIN Compra_componente cc ON cc.ID_COM = c.ID_COM
                                    WHERE cc.ID_COMPRA = ?
                                    """;
                            try (PreparedStatement ps = con.prepareStatement(sqlCheckStock)) {
                                ps.setInt(1, pedido.getIdCompra());
                                ResultSet rs = ps.executeQuery();
                                if (rs.next() && rs.getInt("STOCK") + diferencia < 0)
                                    throw new SQLException(
                                            "Stock insuficiente: reducir la cantidad en " + Math.abs(diferencia) +
                                            " dejaría el stock en negativo.");
                            }
                        }
                        try (PreparedStatement ps = con.prepareStatement(sqlStock)) {
                            ps.setInt(1, diferencia);
                            ps.setInt(2, pedido.getIdCompra());
                            ps.executeUpdate();
                        }
                    }
                }
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // ─── Confirmar llegada ────────────────────────────────────────────────────

    public void confirmarRecibido(CompraComponente pedido)
            throws SQLException, StaleDataException {
        String sqlUpdate = """
                UPDATE Compra_componente
                SET ESTADO = 'recibido', FECHA_LLEGADA = NOW(),
                    CANTIDAD_RECIBIDA = CANTIDAD
                WHERE ID_COMPRA = ? AND UPDATED_AT = ?
                """;
        String sqlStock = """
                UPDATE Componente SET STOCK = STOCK + (
                    SELECT CANTIDAD FROM Compra_componente WHERE ID_COMPRA = ?
                ) WHERE ID_COM = (
                    SELECT ID_COM FROM Compra_componente WHERE ID_COMPRA = ?
                )
                """;
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    ps.setInt(1, pedido.getIdCompra());
                    ps.setTimestamp(2, Timestamp.valueOf(pedido.getUpdatedAt()));
                    rows = ps.executeUpdate();
                }
                if (rows == 0) throw new StaleDataException(
                        "El pedido fue modificado por otro usuario. Recarga los datos e inténtalo de nuevo.");
                try (PreparedStatement ps = con.prepareStatement(sqlStock)) {
                    ps.setInt(1, pedido.getIdCompra());
                    ps.setInt(2, pedido.getIdCompra());
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void confirmarAlterado(CompraComponente pedido, int cantidadRecibida)
            throws SQLException, StaleDataException {
        String sqlUpdate = """
                UPDATE Compra_componente
                SET ESTADO = 'alterado', FECHA_LLEGADA = NOW(),
                    CANTIDAD_RECIBIDA = ?
                WHERE ID_COMPRA = ? AND ESTADO IN ('pendiente','parcial') AND UPDATED_AT = ?
                """;
        String sqlStock = """
                UPDATE Componente SET STOCK = STOCK + ?
                WHERE ID_COM = (SELECT ID_COM FROM Compra_componente WHERE ID_COMPRA = ?)
                """;
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    ps.setInt(1, cantidadRecibida);
                    ps.setInt(2, pedido.getIdCompra());
                    ps.setTimestamp(3, Timestamp.valueOf(pedido.getUpdatedAt()));
                    rows = ps.executeUpdate();
                }
                if (rows == 0) throw new StaleDataException(
                        "El pedido fue modificado por otro usuario. Recarga los datos e inténtalo de nuevo.");
                try (PreparedStatement ps = con.prepareStatement(sqlStock)) {
                    ps.setInt(1, cantidadRecibida);
                    ps.setInt(2, pedido.getIdCompra());
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ─── Recepción parcial ────────────────────────────────────────────────────

    /** Llegan X de Y unidades. Se suman X al stock, el pedido queda como 'parcial' (abierto). */
    public void confirmarParcial(CompraComponente pedido, int cantidadRecibida)
            throws SQLException, StaleDataException {
        String sqlUpdate = """
                UPDATE Compra_componente
                SET ESTADO = 'parcial', FECHA_LLEGADA = NOW(),
                    CANTIDAD_RECIBIDA = ?
                WHERE ID_COMPRA = ? AND UPDATED_AT = ?
                """;
        String sqlStock = """
                UPDATE Componente SET STOCK = STOCK + ?
                WHERE ID_COM = (SELECT ID_COM FROM Compra_componente WHERE ID_COMPRA = ?)
                """;
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    ps.setInt(1, cantidadRecibida);
                    ps.setInt(2, pedido.getIdCompra());
                    ps.setTimestamp(3, Timestamp.valueOf(pedido.getUpdatedAt()));
                    rows = ps.executeUpdate();
                }
                if (rows == 0) throw new StaleDataException(
                        "El pedido fue modificado por otro usuario. Recarga los datos e inténtalo de nuevo.");
                try (PreparedStatement ps = con.prepareStatement(sqlStock)) {
                    ps.setInt(1, cantidadRecibida);
                    ps.setInt(2, pedido.getIdCompra());
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Llegan las unidades restantes de un pedido parcial. Se suman al stock y se cierra como 'recibido'. */
    public void recibirResto(CompraComponente pedido, int cantidadExtra)
            throws SQLException, StaleDataException {
        String sqlUpdate = """
                UPDATE Compra_componente
                SET ESTADO = 'recibido',
                    CANTIDAD_RECIBIDA = CANTIDAD_RECIBIDA + ?
                WHERE ID_COMPRA = ? AND ESTADO = 'parcial' AND UPDATED_AT = ?
                """;
        String sqlStock = """
                UPDATE Componente SET STOCK = STOCK + ?
                WHERE ID_COM = (SELECT ID_COM FROM Compra_componente WHERE ID_COMPRA = ?)
                """;
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    ps.setInt(1, cantidadExtra);
                    ps.setInt(2, pedido.getIdCompra());
                    ps.setTimestamp(3, Timestamp.valueOf(pedido.getUpdatedAt()));
                    rows = ps.executeUpdate();
                }
                if (rows == 0) throw new StaleDataException(
                        "El pedido fue modificado por otro usuario. Recarga los datos e inténtalo de nuevo.");
                try (PreparedStatement ps = con.prepareStatement(sqlStock)) {
                    ps.setInt(1, cantidadExtra);
                    ps.setInt(2, pedido.getIdCompra());
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ─── Cancelar ─────────────────────────────────────────────────────────────

    public void cancelar(CompraComponente pedido) throws SQLException, StaleDataException {
        String sql = "UPDATE Compra_componente SET ESTADO = 'cancelado' WHERE ID_COMPRA = ? AND ESTADO = 'pendiente' AND UPDATED_AT = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pedido.getIdCompra());
            ps.setTimestamp(2, Timestamp.valueOf(pedido.getUpdatedAt()));
            if (ps.executeUpdate() == 0)
                throw new StaleDataException(
                        "El pedido fue modificado por otro usuario. Recarga los datos e inténtalo de nuevo.");
        }
    }

    // ─── Devolver ─────────────────────────────────────────────────────────────

    public void devolver(CompraComponente pedido, int cantidadDevuelta) throws SQLException, StaleDataException {
        String sqlStock = """
                SELECT c.STOCK FROM Componente c
                JOIN Compra_componente cc ON cc.ID_COM = c.ID_COM
                WHERE cc.ID_COMPRA = ?
                """;
        String sqlUpdate = "UPDATE Compra_componente SET ESTADO = 'devuelto' WHERE ID_COMPRA = ? AND UPDATED_AT = ?";
        String sqlDesc   = """
                UPDATE Componente SET STOCK = STOCK - ?
                WHERE ID_COM = (SELECT ID_COM FROM Compra_componente WHERE ID_COMPRA = ?)
                """;
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                int stockActual;
                try (PreparedStatement ps = con.prepareStatement(sqlStock)) {
                    ps.setInt(1, pedido.getIdCompra());
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) throw new SQLException("Pedido no encontrado.");
                    stockActual = rs.getInt(1);
                }
                if (cantidadDevuelta > stockActual) {
                    throw new SQLException(
                            "No se puede devolver " + cantidadDevuelta + " ud(s): " +
                            "el stock actual del componente es " + stockActual + ".");
                }
                int rows;
                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    ps.setInt(1, pedido.getIdCompra());
                    ps.setTimestamp(2, Timestamp.valueOf(pedido.getUpdatedAt()));
                    rows = ps.executeUpdate();
                }
                if (rows == 0) throw new StaleDataException(
                        "El pedido fue modificado por otro usuario. Recarga los datos e inténtalo de nuevo.");
                try (PreparedStatement ps = con.prepareStatement(sqlDesc)) {
                    ps.setInt(1, cantidadDevuelta);
                    ps.setInt(2, pedido.getIdCompra());
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ─── Consultas auxiliares ─────────────────────────────────────────────────

    public int getCantidadPendientePorComponente(int idCom) throws SQLException {
        String sql = "SELECT COALESCE(SUM(CANTIDAD), 0) FROM Compra_componente WHERE ID_COM = ? AND ESTADO = 'pendiente'";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCom);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ─── Mapeo ────────────────────────────────────────────────────────────────

    private CompraComponente mapear(ResultSet rs) throws SQLException {
        Timestamp tsLlegada = rs.getTimestamp("FECHA_LLEGADA");
        return new CompraComponente(
                rs.getInt("ID_COMPRA"),
                rs.getInt("ID_COM"),
                rs.getString("tipo_componente"),
                rs.getInt("ID_PROV"),
                rs.getString("nombre_proveedor"),
                rs.getInt("CANTIDAD"),
                rs.getObject("CANTIDAD_RECIBIDA") != null ? rs.getInt("CANTIDAD_RECIBIDA") : null,
                rs.getBoolean("ES_URGENTE"),
                rs.getTimestamp("FECHA_PEDIDO").toLocalDateTime(),
                tsLlegada != null ? tsLlegada.toLocalDateTime() : null,
                rs.getDouble("PRECIO_UNIDAD_PEDIDO"),
                rs.getString("DIVISA"),
                rs.getDouble("PRECIO_EUR"),
                Estado.valueOf(rs.getString("ESTADO")),
                rs.getTimestamp("UPDATED_AT").toLocalDateTime()
        );
    }
}
