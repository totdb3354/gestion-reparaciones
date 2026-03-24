package com.reparaciones.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
     * ── MIGRACIÓN FUTURA A POSTGRESQL + REST ────────────────────────────────────
     *
     * Cuando el proyecto migre a una arquitectura REST (back separado del front),
     * esta clase DAO desaparece: la conexión a BD pasa a ser responsabilidad del
     * servidor (Spring Boot, Quarkus, etc.), que gestiona un pool de conexiones
     * (HikariCP) y expone endpoints HTTP. Las credenciales se configuran entonces
     * como variables de entorno en el servidor o mediante un gestor de secretos
     * (AWS Secrets Manager, HashiCorp Vault, etc.).
     * En ese escenario exponer credenciales en el código sería un riesgo crítico.
     */

    private static final String URL      = "jdbc:mysql://127.0.0.1:3306/reparaciones";
    private static final String USUARIO  = "root";
    private static final String PASSWORD = "2017";

    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }
}