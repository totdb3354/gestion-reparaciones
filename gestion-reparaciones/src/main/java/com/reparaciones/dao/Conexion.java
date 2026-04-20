package com.reparaciones.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fábrica de conexiones JDBC a la base de datos MySQL local.
 * <p>Las credenciales están hardcodeadas porque la BD solo escucha en
 * {@code 127.0.0.1}, por lo que nadie externo puede conectarse aunque
 * las conozca. Ver comentario en el código para las opciones seguras
 * de cara a una migración a producción.</p>
 *
 * <p><b>Nota:</b> cada llamada a {@link #getConexion()} abre una nueva
 * conexión; no hay pool. Aceptable para uso de escritorio monousuario;
 * reemplazar por HikariCP al migrar a servidor.</p>
 */
public class Conexion {

    /*
     * ── CREDENCIALES HARDCODEADAS (válido solo para desarrollo local) ──────────
     *
     * La URL apunta a localhost, por lo que las credenciales expuestas en el
     * repositorio no suponen un riesgo real: nadie externo puede conectarse
     * a esta base de datos aunque las conozca.
     *
     * ── CÓMO HACERLO BIEN EN PRODUCCIÓN ────────────────────────────────────────
     *
     * Opción A — Fichero de propiedades externo (no versionado):
     *
     *   1. Crear src/main/resources/db.properties con:
     *          db.url=jdbc:mysql://host:3306/reparaciones
     *          db.usuario=root
     *          db.password=2017
     *
     *   2. Añadir db.properties al .gitignore para que nunca se suba al repo.
     *      Incluir un db.properties.example con valores de plantilla como
     *      documentación para quien clone el proyecto.
     *
     *   3. Leerlo aquí:
     *          Properties props = new Properties();
     *          props.load(Conexion.class.getResourceAsStream("/db.properties"));
     *          URL      = props.getProperty("db.url");
     *          USUARIO  = props.getProperty("db.usuario");
     *          PASSWORD = props.getProperty("db.password");
     *
     * Opción B — Variables de entorno (habitual en servidores y contenedores):
     *
     *          URL      = System.getenv("DB_URL");
     *          USUARIO  = System.getenv("DB_USER");
     *          PASSWORD = System.getenv("DB_PASSWORD");
     *
     * ── MIGRACIÓN FUTURA A MARIADB EN VM CLOUD + REST ───────────────────────────
     *
     * Cuando el proyecto migre a una arquitectura REST (back separado del front),
     * esta clase DAO desaparece: la conexión a MariaDB pasa a ser responsabilidad
     * del backend (Spring Boot u otro), alojado en la misma VM cloud que MariaDB.
     * El backend gestiona un pool de conexiones (HikariCP) y expone endpoints HTTP.
     * MariaDB es compatible con el esquema MySQL actual — la migración requiere
     * cambios mínimos. Las credenciales se configuran como variables de entorno
     * en la VM o mediante un gestor de secretos.
     * En ese escenario exponer credenciales en el código sería un riesgo crítico.
     */

    private static final String URL      = "jdbc:mysql://127.0.0.1:3306/reparaciones";
    private static final String USUARIO  = "root";
    private static final String PASSWORD = "2017";

    /**
     * Abre y devuelve una nueva conexión JDBC.
     * <p>El llamador es responsable de cerrarla (idealmente con try-with-resources).</p>
     *
     * @return conexión activa a la BD
     * @throws SQLException si el driver no puede conectarse
     */
    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }
}