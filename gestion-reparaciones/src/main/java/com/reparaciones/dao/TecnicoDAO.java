package com.reparaciones.dao;

import com.reparaciones.models.Tecnico;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla {@code Tecnico}.
 * <p>Proporciona operaciones CRUD básicas sobre técnicos.
 * La activación/desactivación de cuentas se gestiona en {@link UsuarioDAO}.</p>
 *
 * @role ADMIN
 */
public class TecnicoDAO {

    /**
     * Devuelve todos los técnicos, activos e inactivos.
     *
     * @return lista completa de técnicos
     * @throws SQLException si falla la consulta
     */
    public List<Tecnico> getAll() throws SQLException {
        List<Tecnico> lista = new ArrayList<>();
        String sql = "SELECT * FROM Tecnico";
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Tecnico(
                    rs.getInt("ID_TEC"),
                    rs.getString("NOMBRE"),
                    rs.getBoolean("ACTIVO")
                ));
            }
        }
        return lista;
    }

    /**
     * Devuelve solo los técnicos activos ({@code ACTIVO = TRUE}).
     * <p>Usado en el gráfico de estadísticas para excluir técnicos inactivos
     * de las líneas individuales.</p>
     *
     * @return lista de técnicos activos
     * @throws SQLException si falla la consulta
     */
    public List<Tecnico> getAllActivos() throws SQLException {
        List<Tecnico> lista = new ArrayList<>();
        String sql = "SELECT * FROM Tecnico WHERE ACTIVO = TRUE";
        try (Connection con = Conexion.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Tecnico(
                    rs.getInt("ID_TEC"),
                    rs.getString("NOMBRE"),
                    true
                ));
            }
        }
        return lista;
    }

    /**
     * Inserta un nuevo técnico en BD.
     * <p>Usar preferentemente {@link UsuarioDAO#registrarTecnico} que crea
     * técnico y usuario en una sola transacción.</p>
     *
     * @param t técnico a insertar (se usa solo {@code nombre})
     * @throws SQLException si falla el insert
     */
    public void insertar(Tecnico t) throws SQLException {
        String sql = "INSERT INTO Tecnico (NOMBRE) VALUES (?)";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, t.getNombre());
            ps.executeUpdate();
        }
    }

    /**
     * Elimina el técnico de BD.
     * <p>Solo llamar cuando el técnico no tiene reparaciones asociadas.
     * Preferir {@link UsuarioDAO#eliminarTecnico} que borra también el usuario
     * en transacción.</p>
     *
     * @param idTec ID del técnico a eliminar
     * @throws SQLException si falla el delete
     */
    public void eliminar(int idTec) throws SQLException {
        String sql = "DELETE FROM Tecnico WHERE ID_TEC = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTec);
            ps.executeUpdate();
        }
    }
}