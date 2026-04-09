package com.reparaciones.dao;

import com.reparaciones.models.Reparacion;
import com.reparaciones.models.ReparacionResumen;
import com.reparaciones.models.FilaReparacion;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReparacionDAO {

    // ─── Queries base ─────────────────────────────────────────────────────────

    private static final String SQL_RESUMEN = """
            SELECT r.ID_REP, r.IMEI, r.FECHA_ASIG, r.FECHA_FIN, r.ID_TEC,
                   t.NOMBRE AS nombre_tecnico,
                   c.TIPO AS tipo_componente,
                   rc.OBSERVACIONES,
                   rc.ES_INCIDENCIA,
                   rc.ES_RESUELTO,
                   rc.INCIDENCIA,
                   rc.ES_SOLICITUD,
                   rc.DESCRIPCION_SOLICITUD,
                   r.ID_REP_ANTERIOR AS id_rep_nueva
            FROM Reparacion r
            JOIN Tecnico t ON r.ID_TEC = t.ID_TEC
            LEFT JOIN Reparacion_componente rc ON r.ID_REP = rc.ID_REP
            LEFT JOIN Componente c ON rc.ID_COM = c.ID_COM
            """;

    private static final String ORDER_BY_ID = """
             ORDER BY SUBSTRING_INDEX(r.ID_REP, '_', 1),
                      CAST(SUBSTRING_INDEX(r.ID_REP, '_', -1) AS UNSIGNED) ASC
            """;

    private static final String SQL_ASIGNACIONES = """
            SELECT r.ID_REP, r.IMEI, r.FECHA_ASIG, r.FECHA_FIN, r.ID_TEC,
                   t.NOMBRE AS nombre_tecnico,
                   r.ID_REP_ANTERIOR AS id_rep_nueva,
                   EXISTS (
                       SELECT 1 FROM Reparacion r2
                       JOIN Reparacion_componente rc2 ON r2.ID_REP = rc2.ID_REP
                       WHERE r2.IMEI = r.IMEI
                         AND r2.ID_TEC = r.ID_TEC
                         AND rc2.ES_INCIDENCIA = TRUE
                         AND rc2.ES_RESUELTO = FALSE
                   ) AS ES_INCIDENCIA,
                   EXISTS (
                       SELECT 1 FROM Reparacion_componente rc
                       WHERE rc.ID_REP = r.ID_REP
                         AND rc.ES_SOLICITUD = 1
                   ) AS TIENE_SOLICITUD
            FROM Reparacion r
            JOIN Tecnico t ON r.ID_TEC = t.ID_TEC
            WHERE r.ID_REP LIKE 'A%'
            """;

    // ─── Métodos de lectura ───────────────────────────────────────────────────

    public List<ReparacionResumen> getReparacionesResumen() throws SQLException {
        List<ReparacionResumen> lista = new ArrayList<>();
        String sql = SQL_RESUMEN + " WHERE r.ID_REP LIKE 'R%'" + ORDER_BY_ID;
        try (Connection con = Conexion.getConexion();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                lista.add(mapearResumen(rs));
        }
        return lista;
    }

    public List<ReparacionResumen> getReparacionesPorTecnico(int idTec) throws SQLException {
        List<ReparacionResumen> lista = new ArrayList<>();
        String sql = SQL_RESUMEN + " WHERE r.ID_REP LIKE 'R%' AND r.ID_TEC = ?" + ORDER_BY_ID;
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTec);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                lista.add(mapearResumen(rs));
        }
        return lista;
    }

    public List<ReparacionResumen> getResumenPorImei(String imei) throws SQLException {
        List<ReparacionResumen> lista = new ArrayList<>();
        String sql = SQL_RESUMEN + " WHERE r.IMEI = ? AND r.ID_REP LIKE 'R%'" + ORDER_BY_ID;
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, imei);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                lista.add(mapearResumen(rs));
        }
        return lista;
    }

    public List<ReparacionResumen> getAsignaciones() throws SQLException {
        List<ReparacionResumen> lista = new ArrayList<>();
        String sql = SQL_ASIGNACIONES + " ORDER BY r.FECHA_ASIG ASC";
        try (Connection con = Conexion.getConexion();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                lista.add(mapearAsignacion(rs));
        }
        return lista;
    }

    public List<ReparacionResumen> getAsignacionesPorTecnico(int idTec) throws SQLException {
        List<ReparacionResumen> lista = new ArrayList<>();
        String sql = SQL_ASIGNACIONES + " AND r.ID_TEC = ? ORDER BY r.FECHA_ASIG ASC";
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTec);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                lista.add(mapearAsignacion(rs));
        }
        return lista;
    }

    public List<FilaReparacion> getSolicitudesPorAsignacion(String idAsignacion) throws SQLException {
        List<FilaReparacion> lista = new ArrayList<>();
        String sql = """
                SELECT rc.ID_COM, rc.DESCRIPCION_SOLICITUD
                FROM Reparacion_componente rc
                WHERE rc.ID_REP = ? AND rc.ES_SOLICITUD = 1
                """;
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idAsignacion);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                lista.add(new FilaReparacion(
                        rs.getInt("ID_COM"), 0, false, null, null,
                        true, rs.getString("DESCRIPCION_SOLICITUD")));
        }
        return lista;
    }

    public List<Reparacion> getAll() throws SQLException {
        List<Reparacion> lista = new ArrayList<>();
        String sql = "SELECT * FROM Reparacion";
        try (Connection con = Conexion.getConexion();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Reparacion> getByImei(String imei) throws SQLException {
        List<Reparacion> lista = new ArrayList<>();
        String sql = "SELECT * FROM Reparacion WHERE IMEI = ?";
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, imei);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                lista.add(mapear(rs));
        }
        return lista;
    }

    public int countByImei(String imei) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Reparacion WHERE IMEI = ?";
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, imei);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
    }

    // ─── Métodos de escritura ─────────────────────────────────────────────────

    public String insertar(Reparacion r) throws SQLException {
        String idRep = generarId();
        String sql = "INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idRep);
            ps.setTimestamp(2, Timestamp.valueOf(r.getFechaAsig()));
            if (r.getFechaFin() != null)
                ps.setTimestamp(3, Timestamp.valueOf(r.getFechaFin()));
            else
                ps.setNull(3, Types.TIMESTAMP);
            ps.setString(4, r.getImei());
            ps.setInt(5, r.getIdTec());
            ps.executeUpdate();
        }
        return idRep;
    }

    public String insertarAsignacion(String imei, int idTec) throws SQLException {
        String idAsig = generarIdAsignacion();
        String sql = "INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC) VALUES (?, NOW(), NULL, ?, ?)";
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idAsig);
            ps.setString(2, imei);
            ps.setInt(3, idTec);
            ps.executeUpdate();
        }
        return idAsig;
    }

    public void eliminarAsignacion(String idAsig) throws SQLException {
        String sqlGetImei    = "SELECT IMEI FROM Reparacion WHERE ID_REP = ?";
        String sqlBorrarComp = "DELETE FROM Reparacion_componente WHERE ID_REP = ?";
        String sqlBorrar     = "DELETE FROM Reparacion WHERE ID_REP = ?";
        String sqlContarImei = "SELECT COUNT(*) FROM Reparacion WHERE IMEI = ?";
        String sqlBorrarTel  = "DELETE FROM Telefono WHERE IMEI = ?";

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                String imei = null;
                try (PreparedStatement ps = con.prepareStatement(sqlGetImei)) {
                    ps.setString(1, idAsig);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next())
                        imei = rs.getString("IMEI");
                }
                try (PreparedStatement ps = con.prepareStatement(sqlBorrarComp)) {
                    ps.setString(1, idAsig);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(sqlBorrar)) {
                    ps.setString(1, idAsig);
                    ps.executeUpdate();
                }
                if (imei != null) {
                    try (PreparedStatement ps = con.prepareStatement(sqlContarImei)) {
                        ps.setString(1, imei);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next() && rs.getInt(1) == 0) {
                            try (PreparedStatement psDel = con.prepareStatement(sqlBorrarTel)) {
                                psDel.setString(1, imei);
                                psDel.executeUpdate();
                            }
                        }
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void completar(String idRep) throws SQLException {
        String sql = "UPDATE Reparacion SET FECHA_FIN = NOW() WHERE ID_REP = ?";
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idRep);
            ps.executeUpdate();
        }
    }

    // ─── Generadores de ID ────────────────────────────────────────────────────

    public String generarId() throws SQLException {
        String fechaHoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefijo = "R" + fechaHoy + "_";
        String sql = "SELECT MAX(CAST(SUBSTRING_INDEX(ID_REP, '_', -1) AS UNSIGNED)) " +
                     "FROM Reparacion WHERE ID_REP LIKE ?";
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, prefijo + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int max = rs.getInt(1); // 0 si no hay ninguno (rs devuelve NULL -> getInt = 0)
                return prefijo + (max + 1);
            }
        }
        return prefijo + "1";
    }

    public String generarIdAsignacion() throws SQLException {
        String fechaHoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefijo = "A" + fechaHoy + "_";
        String sql = "SELECT MAX(CAST(SUBSTRING_INDEX(ID_REP, '_', -1) AS UNSIGNED)) " +
                     "FROM Reparacion WHERE ID_REP LIKE ?";
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, prefijo + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int max = rs.getInt(1);
                return prefijo + (max + 1);
            }
        }
        return prefijo + "1";
    }

    public void actualizarTecnico(String idRep, int idTec) throws SQLException {
        String sql = "UPDATE Reparacion SET ID_TEC = ? WHERE ID_REP = ?";
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTec);
            ps.setString(2, idRep);
            ps.executeUpdate();
        }
    }

    // ─── Edición de reparación ────────────────────────────────────────────────

    /** Datos del R* necesarios para abrir el modal de edición. */
    public static class DetalleEdicion {
        public final String imei;
        public final int    idTec;
        public final int    idCom;
        public final boolean esReutilizado;
        public final String  observacion;
        public DetalleEdicion(String imei, int idTec, int idCom,
                              boolean esReutilizado, String observacion) {
            this.imei = imei; this.idTec = idTec; this.idCom = idCom;
            this.esReutilizado = esReutilizado; this.observacion = observacion;
        }
    }

    public DetalleEdicion getDetalleEdicion(String idRep) throws SQLException {
        String sql = """
                SELECT r.IMEI, r.ID_TEC, rc.ID_COM, rc.ES_REUTILIZADO, rc.OBSERVACIONES
                FROM Reparacion r
                JOIN Reparacion_componente rc ON r.ID_REP = rc.ID_REP
                WHERE r.ID_REP = ? AND rc.ES_SOLICITUD = 0
                """;
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idRep);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return new DetalleEdicion(
                        rs.getString("IMEI"), rs.getInt("ID_TEC"),
                        rs.getInt("ID_COM"), rs.getBoolean("ES_REUTILIZADO"),
                        rs.getString("OBSERVACIONES"));
        }
        return null;
    }

    public void editarReparacion(String idRep, int idComNuevo, boolean esReutilizadoNuevo,
            String observacionNueva, boolean piezaViejaRota, int nNuevas) throws SQLException {
        String sqlGetActual  = """
                SELECT rc.ID_COM, rc.ES_REUTILIZADO
                FROM Reparacion_componente rc
                WHERE rc.ID_REP = ? AND rc.ES_SOLICITUD = 0
                """;
        String sqlDevolver   = "UPDATE Componente SET STOCK = STOCK + 1 WHERE ID_COM = ? AND TIPO NOT LIKE 'otro%'";
        String sqlDescontar  = "UPDATE Componente SET STOCK = STOCK - ? WHERE ID_COM = ? AND TIPO NOT LIKE 'otro%'";
        String sqlUpdateComp = """
                UPDATE Reparacion_componente
                SET ID_COM = ?, ES_REUTILIZADO = ?, OBSERVACIONES = ?
                WHERE ID_REP = ? AND ES_SOLICITUD = 0
                """;
        String sqlUpdateFecha = "UPDATE Reparacion SET FECHA_FIN = NOW() WHERE ID_REP = ?";

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                // 1. Leer estado actual
                int     idComActual  = -1;
                boolean eraReutilizado = false;
                try (PreparedStatement ps = con.prepareStatement(sqlGetActual)) {
                    ps.setString(1, idRep);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        idComActual    = rs.getInt("ID_COM");
                        eraReutilizado = rs.getBoolean("ES_REUTILIZADO");
                    }
                }
                // 2. Devolver stock viejo (solo si era normal y no rota)
                if (!eraReutilizado && !piezaViejaRota) {
                    try (PreparedStatement ps = con.prepareStatement(sqlDevolver)) {
                        ps.setInt(1, idComActual);
                        ps.executeUpdate();
                    }
                }
                // 3. Descontar stock nuevo (solo si no es reutilizada)
                if (!esReutilizadoNuevo && nNuevas > 0) {
                    try (PreparedStatement ps = con.prepareStatement(sqlDescontar)) {
                        ps.setInt(1, nNuevas);
                        ps.setInt(2, idComNuevo);
                        ps.executeUpdate();
                    }
                }
                // 4. Actualizar componente
                try (PreparedStatement ps = con.prepareStatement(sqlUpdateComp)) {
                    ps.setInt(1, idComNuevo);
                    ps.setBoolean(2, esReutilizadoNuevo);
                    ps.setString(3, observacionNueva);
                    ps.setString(4, idRep);
                    ps.executeUpdate();
                }
                // 5. Actualizar fecha
                try (PreparedStatement ps = con.prepareStatement(sqlUpdateFecha)) {
                    ps.setString(1, idRep);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ─── Utilidades ───────────────────────────────────────────────────────────

    public String getReferenciadora(String idRep) throws SQLException {
        String sql = "SELECT ID_REP FROM Reparacion WHERE ID_REP_ANTERIOR = ?";
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idRep);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("ID_REP");
        }
        return null;
    }

    /** Devuelve los ID_COM ya reparados para un IMEI, excluyendo el R* que se está editando. */
    public Set<Integer> getIdComsYaReparados(String imei, String idRepExcluir) throws SQLException {
        String sql = """
                SELECT DISTINCT rc.ID_COM
                FROM Reparacion r
                JOIN Reparacion_componente rc ON r.ID_REP = rc.ID_REP
                WHERE r.IMEI = ? AND r.ID_REP LIKE 'R%'
                  AND r.ID_REP != ? AND rc.ES_SOLICITUD = 0
                """;
        Set<Integer> ids = new HashSet<>();
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, imei);
            ps.setString(2, idRepExcluir);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("ID_COM"));
        }
        return ids;
    }

    public void eliminar(String idRep) throws SQLException {
        String sqlGetInfo    = "SELECT ID_REP_ANTERIOR, IMEI FROM Reparacion WHERE ID_REP = ?";
        String sqlGetComps   = """
                SELECT ID_COM, COUNT(*) AS UNIDADES
                FROM Reparacion_componente
                WHERE ID_REP = ? AND ES_REUTILIZADO = FALSE AND ES_SOLICITUD = 0
                GROUP BY ID_COM
                """;
        String sqlDevolverStock = "UPDATE Componente SET STOCK = STOCK + ? WHERE ID_COM = ? AND TIPO NOT LIKE 'otro%'";
        String sqlComp       = "DELETE FROM Reparacion_componente WHERE ID_REP = ?";
        String sqlRep        = "DELETE FROM Reparacion WHERE ID_REP = ?";
        String sqlRevertir   = "UPDATE Reparacion_componente SET ES_RESUELTO = FALSE WHERE ID_REP = ?";
        String sqlContarImei = "SELECT COUNT(*) FROM Reparacion WHERE IMEI = ?";
        String sqlBorrarTel  = "DELETE FROM Telefono WHERE IMEI = ?";

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                // 1. Leer IMEI e ID_REP_ANTERIOR antes de borrar
                String idRepAnterior = null;
                String imei = null;
                try (PreparedStatement ps = con.prepareStatement(sqlGetInfo)) {
                    ps.setString(1, idRep);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        idRepAnterior = rs.getString("ID_REP_ANTERIOR");
                        imei = rs.getString("IMEI");
                    }
                }
                // 2. Devolver stock: solo componentes reales (no reutilizados, no solicitudes)
                try (PreparedStatement psGet = con.prepareStatement(sqlGetComps);
                     PreparedStatement psUpd = con.prepareStatement(sqlDevolverStock)) {
                    psGet.setString(1, idRep);
                    ResultSet rs = psGet.executeQuery();
                    while (rs.next()) {
                        psUpd.setInt(1, rs.getInt("UNIDADES"));
                        psUpd.setInt(2, rs.getInt("ID_COM"));
                        psUpd.executeUpdate();
                    }
                }
                // 4. Borrar componentes y reparación
                try (PreparedStatement ps = con.prepareStatement(sqlComp)) {
                    ps.setString(1, idRep);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(sqlRep)) {
                    ps.setString(1, idRep);
                    ps.executeUpdate();
                }
                // 5. Revertir incidencia de la reparación anterior si la había
                if (idRepAnterior != null) {
                    try (PreparedStatement ps = con.prepareStatement(sqlRevertir)) {
                        ps.setString(1, idRepAnterior);
                        ps.executeUpdate();
                    }
                }
                // 6. Si ya no quedan reparaciones con ese IMEI, borrar el Telefono
                if (imei != null) {
                    try (PreparedStatement ps = con.prepareStatement(sqlContarImei)) {
                        ps.setString(1, imei);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next() && rs.getInt(1) == 0) {
                            try (PreparedStatement psDel = con.prepareStatement(sqlBorrarTel)) {
                                psDel.setString(1, imei);
                                psDel.executeUpdate();
                            }
                        }
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void insertarCompleta(List<FilaReparacion> filas, String imei, int idTec,
            String idRepAnterior, String idAsignacion) throws SQLException {

        List<FilaReparacion> filasNormales   = new ArrayList<>();
        List<FilaReparacion> filasSolicitud  = new ArrayList<>();
        for (FilaReparacion f : filas) {
            if (f.isEsSolicitud()) filasSolicitud.add(f);
            else                   filasNormales.add(f);
        }

        String sqlRep = """
                INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
                VALUES (?, NOW(), NOW(), ?, ?, ?)
                """;
        String sqlComp = """
                INSERT INTO Reparacion_componente
                (ID_REP, ID_COM, ES_REUTILIZADO, ES_INCIDENCIA, ES_RESUELTO, INCIDENCIA, OBSERVACIONES, CANTIDAD)
                VALUES (?, ?, ?, FALSE, FALSE, NULL, ?, ?)
                """;
        String sqlCompSolicitud = """
                INSERT INTO Reparacion_componente
                (ID_REP, ID_COM, ES_REUTILIZADO, ES_INCIDENCIA, ES_RESUELTO, ES_SOLICITUD, DESCRIPCION_SOLICITUD)
                VALUES (?, ?, FALSE, FALSE, FALSE, 1, ?)
                """;
        String sqlStock    = "UPDATE Componente SET STOCK = STOCK - ? WHERE ID_COM = ? AND TIPO NOT LIKE 'otro%'";
        String sqlResolver = "UPDATE Reparacion_componente SET ES_RESUELTO = TRUE WHERE ID_REP = ?";
        String sqlBorrarAsig = "DELETE FROM Reparacion WHERE ID_REP = ?";

        // Calcular base del contador solo si hay filas normales
        String fechaHoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int contadorBase = 0;
        if (!filasNormales.isEmpty()) {
            String sqlCount = "SELECT COUNT(*) FROM Reparacion WHERE ID_REP LIKE ?";
            try (Connection con = Conexion.getConexion();
                    PreparedStatement ps = con.prepareStatement(sqlCount)) {
                ps.setString(1, "R" + fechaHoy + "_%");
                ResultSet rs = ps.executeQuery();
                contadorBase = rs.next() ? rs.getInt(1) : 0;
            }
        }

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                // ── Filas normales → nuevos R* ────────────────────────────────
                int indice = 0;
                for (FilaReparacion fila : filasNormales) {
                    String nuevoId = "R" + fechaHoy + "_" + (contadorBase + 1 + indice++);
                    try (PreparedStatement ps = con.prepareStatement(sqlRep)) {
                        ps.setString(1, nuevoId);
                        ps.setString(2, imei);
                        ps.setInt(3, idTec);
                        if (idRepAnterior != null) ps.setString(4, idRepAnterior);
                        else                       ps.setNull(4, Types.VARCHAR);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = con.prepareStatement(sqlComp)) {
                        ps.setString(1, nuevoId);
                        ps.setInt(2, fila.getIdCom());
                        ps.setBoolean(3, fila.isReutilizado());
                        ps.setString(4, fila.getObservacion());
                        ps.setInt(5, fila.getCantidad() > 0 ? fila.getCantidad() : 1);
                        ps.executeUpdate();
                    }
                    if (!fila.isReutilizado() && fila.getCantidad() > 0
                            && !fila.getPrefijo().equals("otro")) {
                        try (PreparedStatement ps = con.prepareStatement(sqlStock)) {
                            ps.setInt(1, fila.getCantidad());
                            ps.setInt(2, fila.getIdCom());
                            ps.executeUpdate();
                        }
                    }
                }

                // ── Limpiar solicitudes previas de la A* (se reemplazan) ─────
                if (idAsignacion != null) {
                    String sqlLimpiarSol =
                            "DELETE FROM Reparacion_componente WHERE ID_REP = ? AND ES_SOLICITUD = 1";
                    try (PreparedStatement ps = con.prepareStatement(sqlLimpiarSol)) {
                        ps.setString(1, idAsignacion);
                        ps.executeUpdate();
                    }
                }

                // ── Filas solicitud → Reparacion_componente sobre A* ──────────
                if (idAsignacion != null) {
                    for (FilaReparacion fila : filasSolicitud) {
                        try (PreparedStatement ps = con.prepareStatement(sqlCompSolicitud)) {
                            ps.setString(1, idAsignacion);
                            ps.setInt(2, fila.getIdCom());
                            if (fila.getDescripcionSolicitud() != null)
                                ps.setString(3, fila.getDescripcionSolicitud());
                            else
                                ps.setNull(3, Types.LONGVARCHAR);
                            ps.executeUpdate();
                        }
                    }
                }

                // ── Resolver incidencia anterior si hubo filas normales ───────
                if (idRepAnterior != null && !filasNormales.isEmpty()) {
                    try (PreparedStatement ps = con.prepareStatement(sqlResolver)) {
                        ps.setString(1, idRepAnterior);
                        ps.executeUpdate();
                    }
                }

                // ── Borrar A* solo si no quedan solicitudes pendientes ────────
                if (idAsignacion != null && filasSolicitud.isEmpty()) {
                    try (PreparedStatement ps = con.prepareStatement(sqlBorrarAsig)) {
                        ps.setString(1, idAsignacion);
                        ps.executeUpdate();
                    }
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ─── Mapeos ───────────────────────────────────────────────────────────────

    private ReparacionResumen mapearResumen(ResultSet rs) throws SQLException {
        Timestamp tsAsig = rs.getTimestamp("FECHA_ASIG");
        Timestamp tsFin = rs.getTimestamp("FECHA_FIN");
        return new ReparacionResumen(
                rs.getString("ID_REP"),
                rs.getString("IMEI"),
                rs.getString("nombre_tecnico"),
                tsAsig != null ? tsAsig.toLocalDateTime() : null,
                tsFin != null ? tsFin.toLocalDateTime() : null,
                rs.getString("tipo_componente"),
                rs.getString("OBSERVACIONES"),
                rs.getBoolean("ES_INCIDENCIA"),
                rs.getBoolean("ES_RESUELTO"),
                rs.getString("INCIDENCIA"),
                rs.getString("id_rep_nueva"),
                rs.getInt("ID_TEC"),
                rs.getInt("ES_SOLICITUD"),
                rs.getString("DESCRIPCION_SOLICITUD"));
    }

    private ReparacionResumen mapearAsignacion(ResultSet rs) throws SQLException {
        Timestamp tsAsig = rs.getTimestamp("FECHA_ASIG");
        Timestamp tsFin = rs.getTimestamp("FECHA_FIN");
        return new ReparacionResumen(
                rs.getString("ID_REP"),
                rs.getString("IMEI"),
                rs.getString("nombre_tecnico"),
                tsAsig != null ? tsAsig.toLocalDateTime() : null,
                tsFin != null ? tsFin.toLocalDateTime() : null,
                null,
                null,
                rs.getBoolean("ES_INCIDENCIA"),
                false,
                null,
                rs.getString("id_rep_nueva"),
                rs.getInt("ID_TEC"),
                rs.getBoolean("TIENE_SOLICITUD") ? 1 : 0,
                null);
    }

    private Reparacion mapear(ResultSet rs) throws SQLException {
        Timestamp tsAsig = rs.getTimestamp("FECHA_ASIG");
        Timestamp tsFin = rs.getTimestamp("FECHA_FIN");
        return new Reparacion(
                rs.getString("ID_REP"),
                tsAsig != null ? tsAsig.toLocalDateTime() : null,
                tsFin != null ? tsFin.toLocalDateTime() : null,
                rs.getString("IMEI"),
                rs.getInt("ID_TEC"));
    }

    public void marcarIncidenciaYAsignar(String idRep, String comentario,
            String imei, int idTec) throws SQLException {
        String sqlIncidencia = "UPDATE Reparacion_componente " +
                "SET ES_INCIDENCIA = TRUE, INCIDENCIA = ? WHERE ID_REP = ?";
        String sqlAsignacion = "INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC) " +
                "VALUES (?, NOW(), NULL, ?, ?)";

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sqlIncidencia)) {
                    ps.setString(1, comentario);
                    ps.setString(2, idRep);
                    ps.executeUpdate();
                }
                String idAsig = generarIdAsignacion();
                try (PreparedStatement ps = con.prepareStatement(sqlAsignacion)) {
                    ps.setString(1, idAsig);
                    ps.setString(2, imei);
                    ps.setInt(3, idTec);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public boolean existeAsignacionParaTecnico(String imei, int idTec) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM Reparacion
                WHERE IMEI = ? AND ID_TEC = ? AND ID_REP LIKE 'A%'
                """;
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, imei);
            ps.setInt(2, idTec);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    public String getIncidenciaActivaPorImei(String imei) throws SQLException {
        String sql = """
                SELECT r.ID_REP FROM Reparacion r
                JOIN Reparacion_componente rc ON r.ID_REP = rc.ID_REP
                WHERE r.IMEI = ?
                  AND rc.ES_INCIDENCIA = TRUE
                  AND rc.ES_RESUELTO = FALSE
                LIMIT 1
                """;
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, imei);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("ID_REP");
        }
        return null;
    }

    public void borrarIncidenciaPorImei(String imei) throws SQLException {
        String sql = """
                UPDATE Reparacion_componente SET ES_INCIDENCIA = FALSE
                WHERE ID_REP = (
                    SELECT idRep FROM (
                        SELECT r.ID_REP AS idRep
                        FROM Reparacion r
                        JOIN Reparacion_componente rc ON r.ID_REP = rc.ID_REP
                        WHERE r.IMEI = ?
                          AND rc.ES_INCIDENCIA = TRUE
                          AND rc.ES_RESUELTO = FALSE
                        LIMIT 1
                    ) AS subquery
                )
                """;
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, imei);
            int filasAfectadas = ps.executeUpdate();
            System.out.println(">>> borrarIncidenciaPorImei — IMEI=" + imei
                    + " | filas afectadas=" + filasAfectadas);
        }
    }
}