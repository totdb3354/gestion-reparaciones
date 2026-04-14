package com.reparaciones.dao;

import com.reparaciones.models.Usuario;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    /**
     * Busca el usuario por nombre y verifica la password con BCrypt.
     * Devuelve el Usuario si las credenciales son correctas, null si no.
     * Rechaza el login si el técnico asociado está desactivado (ACTIVO = false).
     */
    public Usuario login(String nombreUsuario, String password) throws SQLException {
        String sql = """
                SELECT u.ID_USU, u.NOMBRE_USUARIO, u.PASSWORD, u.ROL, u.ID_TEC,
                       t.ACTIVO
                FROM Usuario u
                LEFT JOIN Tecnico t ON u.ID_TEC = t.ID_TEC
                WHERE u.NOMBRE_USUARIO = ?
                """;
        try (Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("PASSWORD");
                if (BCrypt.checkpw(password, hash)) {
                    boolean activo = rs.getBoolean("ACTIVO");
                    if (!activo) return null; // técnico desactivado — login denegado
                    int idUsu    = rs.getInt("ID_USU");
                    String rol   = rs.getString("ROL");
                    int idTecRaw = rs.getInt("ID_TEC");
                    Integer idTec = rs.wasNull() ? null : idTecRaw;
                    return new Usuario(idUsu, nombreUsuario, rol, idTec);
                }
            }
        }
        return null;
    }

    /**
     * Devuelve todos los técnicos (con credenciales) para la tabla de gestión.
     * Hace JOIN con Tecnico para incluir nombre visible y estado activo/inactivo.
     */
    public List<Usuario> getUsuariosTecnicos() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = """
                SELECT u.ID_USU, u.NOMBRE_USUARIO, u.ROL, u.ID_TEC,
                       t.NOMBRE AS nombre_tecnico, t.ACTIVO
                FROM Usuario u
                JOIN Tecnico t ON u.ID_TEC = t.ID_TEC
                WHERE u.ROL = 'TECNICO'
                ORDER BY t.NOMBRE
                """;
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Usuario(
                        rs.getInt("ID_USU"),
                        rs.getString("NOMBRE_USUARIO"),
                        rs.getString("ROL"),
                        rs.getInt("ID_TEC"),
                        rs.getString("nombre_tecnico"),
                        rs.getBoolean("ACTIVO")));
            }
        }
        return lista;
    }

    /** Desactiva el técnico (ACTIVO = false). No afecta sus reparaciones históricas. */
    public void desactivarTecnico(int idTec) throws SQLException {
        String sql = "UPDATE Tecnico SET ACTIVO = FALSE WHERE ID_TEC = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTec);
            ps.executeUpdate();
        }
    }

    /** Reactiva el técnico (ACTIVO = true). */
    public void activarTecnico(int idTec) throws SQLException {
        String sql = "UPDATE Tecnico SET ACTIVO = TRUE WHERE ID_TEC = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTec);
            ps.executeUpdate();
        }
    }

    /**
     * Comprueba si el técnico tiene alguna reparación asociada (histórica o activa).
     * Si las tiene, no se puede eliminar.
     */
    public boolean tieneReparaciones(int idTec) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Reparacion WHERE ID_TEC = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTec);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /**
     * Elimina completamente el técnico: borra Usuario y Tecnico en transacción.
     * Solo llamar si tieneReparaciones() devuelve false.
     */
    public void eliminarTecnico(int idUsu, int idTec) throws SQLException {
        String sqlUsu = "DELETE FROM Usuario WHERE ID_USU = ?";
        String sqlTec = "DELETE FROM Tecnico WHERE ID_TEC = ?";
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sqlUsu)) {
                    ps.setInt(1, idUsu);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(sqlTec)) {
                    ps.setInt(1, idTec);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * Registra un nuevo técnico — inserta en Tecnico y en Usuario en transacción.
     * Si falla cualquiera de los dos inserts, se hace rollback de ambos.
     */
    public void registrarTecnico(String nombreTecnico, String nombreUsuario, String password) throws SQLException {
        String sqlTecnico = "INSERT INTO Tecnico (NOMBRE) VALUES (?)";
        String sqlUsuario = "INSERT INTO Usuario (NOMBRE_USUARIO, PASSWORD, ROL, ID_TEC) VALUES (?, ?, 'TECNICO', ?)";

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                int idTec;
                try (PreparedStatement ps = con.prepareStatement(sqlTecnico, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombreTecnico);
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    idTec = rs.getInt(1);
                }

                try (PreparedStatement ps = con.prepareStatement(sqlUsuario)) {
                    ps.setString(1, nombreUsuario);
                    ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
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
}