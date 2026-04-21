package com.reparaciones.dao;

import com.reparaciones.models.Componente;
import com.reparaciones.models.PuntoStock;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de la tabla {@code Componente}.
 * <p>Gestiona el stock de componentes de reparación. Los componentes de tipo
 * {@code "otro*"} no se gestionan desde el programa (p. ej. "otro teclado"):
 * se excluyen de las consultas de stock pero siguen referenciables en reparaciones.</p>
 * <p>Las operaciones de escritura que afectan al stock se hacen con transacciones
 * para garantizar consistencia con los movimientos de pedidos y reparaciones.</p>
 *
 * @role ADMIN (gestión completa); TECNICO (lectura de stock en formulario de reparación)
 */
public class ComponenteDAO {

    /**
     * Devuelve todos los componentes sin ningún filtro.
     *
     * @return lista completa de componentes
     * @throws SQLException si falla la consulta
     */
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

    /**
     * Devuelve los componentes gestionados (excluye tipos {@code "otro*"}) con
     * información calculada de unidades en camino y último pedido.
     * <p>Usado en la vista de stock para mostrar la situación real de cada componente.</p>
     *
     * @return lista de componentes gestionados con {@code enCamino} y {@code ultimoPedido} rellenos
     * @throws SQLException si falla la consulta
     */
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

    /**
     * Devuelve los componentes cuyo stock está en o por debajo del mínimo.
     * <p>Usado para mostrar alertas de stock bajo en el panel de administración.</p>
     *
     * @return lista de componentes con {@code stock <= stockMinimo}
     * @throws SQLException si falla la consulta
     */
    public List<Componente> getStockBajo() throws SQLException {
        List<Componente> lista = new ArrayList<>();
        String sql = "SELECT * FROM Componente WHERE STOCK <= STOCK_MINIMO AND ACTIVO = TRUE";
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /**
     * Devuelve los chasis disponibles (stock {@literal >} 0) que coinciden con el color dado.
     * <p>El color se busca como sufijo del tipo: p. ej. {@code "negro"} encuentra
     * {@code "chai13negro"}, {@code "chai14negro"}, etc.</p>
     *
     * @param color sufijo de color del chasis (p. ej. {@code "negro"}, {@code "blanco"})
     * @return lista de chasis con stock disponible del color indicado
     * @throws SQLException si falla la consulta
     */
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

    /**
     * Inserta un nuevo componente en BD.
     *
     * @param c componente a insertar (se usan {@code tipo}, {@code stock} y {@code stockMinimo})
     * @throws SQLException si falla el insert
     */
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

    /**
     * Actualiza tipo, stock y stock mínimo de un componente con control de concurrencia
     * optimista (compara {@code UPDATED_AT}).
     *
     * @param c componente con los nuevos valores y el {@code updatedAt} leído previamente
     * @throws SQLException                               si falla el update
     * @throws com.reparaciones.utils.StaleDataException si el registro fue modificado
     *                                                    por otro usuario desde la última lectura
     */
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

    /**
     * Actualiza únicamente el stock mínimo de un componente.
     *
     * @param idCom      ID del componente
     * @param stockMinimo nuevo valor del umbral mínimo
     * @throws SQLException si falla el update
     */
    public void setStockMinimo(int idCom, int stockMinimo) throws SQLException {
        String sql = "UPDATE Componente SET STOCK_MINIMO = ? WHERE ID_COM = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, stockMinimo);
            ps.setInt(2, idCom);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina físicamente el componente de BD.
     *
     * @param idCom ID del componente a eliminar
     * @throws SQLException si falla el delete
     */
    public void eliminar(int idCom) throws SQLException {
        String sql = "DELETE FROM Componente WHERE ID_COM = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCom);
            ps.executeUpdate();
        }
    }

    /**
     * Incrementa (o decrementa si negativo) el stock de un componente.
     * <p>Usa {@code STOCK + ?} para ser seguro ante concurrencia.</p>
     *
     * @param idCom   ID del componente
     * @param cantidad delta a aplicar (positivo suma, negativo resta)
     * @throws SQLException si falla el update
     */
    public void actualizarStock(int idCom, int cantidad) throws SQLException {
        String sql = "UPDATE Componente SET STOCK = STOCK + ? WHERE ID_COM = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idCom);
            ps.executeUpdate();
        }
    }

    /**
     * Reconstruye la evolución de stock por componente y periodo.
     * <p>Calcula el stock estimado al final de cada periodo como:
     * {@code stock_actual - suma(eventos_posteriores_al_periodo)},
     * donde los eventos son entradas ({@code Compra_componente} recibida)
     * y salidas ({@code Reparacion_componente} no reutilizada con reparación finalizada).</p>
     *
     * @param granularidad {@code "dia"}, {@code "semana"}, {@code "mes"} o {@code "ano"}
     * @param desde        inicio del rango de fechas
     * @param hasta        fin del rango de fechas
     * @return lista de puntos ordenados por componente y periodo
     * @throws SQLException si falla la consulta
     */
    public List<PuntoStock> getEvolucionStock(String granularidad,
                                               java.time.LocalDate desde,
                                               java.time.LocalDate hasta) throws SQLException {
        String fmt = switch (granularidad) {
            case "dia"    -> "%Y-%m-%d";
            case "semana" -> "%x-W%v";
            case "ano"    -> "%Y";
            default       -> "%Y-%m";
        };

        // Periodos que delimitan el rango en el mismo formato
        String periodoDesde = formatPeriodo(desde, granularidad);
        String periodoHasta = formatPeriodo(hasta, granularidad);

        String sql = """
            WITH todos_eventos AS (
                SELECT ID_COM,
                       DATE_FORMAT(FECHA_LLEGADA, '""" + fmt + """
') AS periodo,
                       COALESCE(CANTIDAD_RECIBIDA, 0) AS delta
                FROM Compra_componente
                WHERE ESTADO IN ('recibido','parcial') AND FECHA_LLEGADA IS NOT NULL

                UNION ALL

                SELECT rc.ID_COM,
                       DATE_FORMAT(r.FECHA_FIN, '""" + fmt + """
') AS periodo,
                       -rc.CANTIDAD AS delta
                FROM Reparacion_componente rc
                JOIN Reparacion r ON r.ID_REP = rc.ID_REP
                WHERE r.FECHA_FIN IS NOT NULL
                  AND rc.ES_REUTILIZADO = FALSE
                  AND rc.ID_COM IS NOT NULL
            ),
            delta_por_periodo AS (
                SELECT ID_COM, periodo, SUM(delta) AS delta
                FROM todos_eventos
                GROUP BY ID_COM, periodo
            ),
            periodos_en_rango AS (
                SELECT DISTINCT ID_COM, periodo
                FROM todos_eventos
                WHERE periodo BETWEEN ? AND ?
            )
            SELECT p.ID_COM, c.TIPO, p.periodo,
                   c.STOCK - COALESCE((
                       SELECT SUM(d.delta)
                       FROM delta_por_periodo d
                       WHERE d.ID_COM = p.ID_COM AND d.periodo > p.periodo
                   ), 0) AS stock_estimado,
                   c.STOCK_MINIMO
            FROM periodos_en_rango p
            JOIN Componente c ON c.ID_COM = p.ID_COM
            WHERE c.TIPO NOT LIKE 'otro%'
            ORDER BY c.TIPO, p.periodo
            """;

        List<PuntoStock> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, periodoDesde);
            ps.setString(2, periodoHasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new PuntoStock(
                            rs.getString("periodo"),
                            rs.getString("TIPO"),
                            rs.getInt("stock_estimado"),
                            rs.getInt("STOCK_MINIMO")));
                }
            }
        }
        return lista;
    }

    private String formatPeriodo(java.time.LocalDate fecha, String granularidad) {
        return switch (granularidad) {
            case "dia"    -> fecha.format(java.time.format.DateTimeFormatter.ISO_DATE);
            case "semana" -> String.format("%d-W%02d",
                    fecha.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR),
                    fecha.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case "ano"    -> String.valueOf(fecha.getYear());
            default       -> fecha.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        };
    }

    /**
     * Activa o desactiva un componente.
     * <p>Los componentes desactivados se excluyen del conteo de stock y de las alertas.</p>
     *
     * @param idCom  ID del componente
     * @param activo {@code true} para activar, {@code false} para desactivar
     * @throws SQLException si falla el update
     */
    public void setActivo(int idCom, boolean activo) throws SQLException {
        String sql = "UPDATE Componente SET ACTIVO = ? WHERE ID_COM = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, idCom);
            ps.executeUpdate();
        }
    }

    /** Mapea una fila del {@code ResultSet} a un {@link com.reparaciones.models.Componente}. */
    private Componente mapear(ResultSet rs) throws SQLException {
        return new Componente(
                rs.getInt("ID_COM"),
                rs.getString("TIPO"),
                rs.getTimestamp("FECHA_REGISTRO").toLocalDateTime(),
                rs.getInt("STOCK"),
                rs.getInt("STOCK_MINIMO"),
                rs.getBoolean("ACTIVO"),
                rs.getTimestamp("UPDATED_AT").toLocalDateTime());
    }

    /**
     * Devuelve todos los componentes agrupados por prefijo de tipo en un mapa ordenado.
     * <p>Los 7 prefijos posibles son: {@code bat}, {@code cha}, {@code g}, {@code mc},
     * {@code cam}, {@code lcd}, {@code otro}. La estructura del SKU completo es
     * {@code [prefijo] + "i" + [modelo] + [color opcional]} (p. ej. {@code "bati13"}).</p>
     *
     * @return mapa ordenado: prefijo → lista de componentes
     * @throws SQLException si falla la consulta
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