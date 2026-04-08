-- ══════════════════════════════════════════════════════════════════════════════
-- crear_bd.sql  —  Creación completa de la base de datos desde cero
-- Ejecutar como root en MySQL.
-- ══════════════════════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS reparaciones
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE reparaciones;

-- ── Tablas base (sin dependencias) ────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS Componente (
    ID_COM        INT            NOT NULL AUTO_INCREMENT,
    TIPO          VARCHAR(100)   NOT NULL UNIQUE,
    STOCK         INT            NOT NULL DEFAULT 0,
    STOCK_MINIMO  INT            NOT NULL DEFAULT 0,
    PRECIO_UNIDAD DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    FECHA_REGISTRO TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ID_COM)
);

CREATE TABLE IF NOT EXISTS Tecnico (
    ID_TEC  INT          NOT NULL AUTO_INCREMENT,
    NOMBRE  VARCHAR(100) NOT NULL,
    PRIMARY KEY (ID_TEC)
);

CREATE TABLE IF NOT EXISTS Telefono (
    IMEI  VARCHAR(15) NOT NULL,
    PRIMARY KEY (IMEI)
);

-- ── Tablas con dependencias ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS Usuario (
    ID_USUARIO     INT          NOT NULL AUTO_INCREMENT,
    NOMBRE_USUARIO VARCHAR(50)  NOT NULL UNIQUE,
    PASSWORD       VARCHAR(255) NOT NULL,
    ROL            ENUM('ADMIN','TECNICO') NOT NULL,
    ID_TEC         INT          NOT NULL,
    PRIMARY KEY (ID_USUARIO),
    CONSTRAINT fk_usuario_tecnico FOREIGN KEY (ID_TEC) REFERENCES Tecnico (ID_TEC)
);

CREATE TABLE IF NOT EXISTS Reparacion (
    ID_REP          VARCHAR(30)  NOT NULL,
    FECHA_ASIG      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FECHA_FIN       DATETIME,
    IMEI            VARCHAR(15)  NOT NULL,
    ID_TEC          INT          NOT NULL,
    ID_REP_ANTERIOR VARCHAR(30),
    PRIMARY KEY (ID_REP),
    CONSTRAINT fk_rep_telefono  FOREIGN KEY (IMEI)            REFERENCES Telefono  (IMEI),
    CONSTRAINT fk_rep_tecnico   FOREIGN KEY (ID_TEC)          REFERENCES Tecnico   (ID_TEC),
    CONSTRAINT fk_rep_anterior  FOREIGN KEY (ID_REP_ANTERIOR) REFERENCES Reparacion(ID_REP)
);

CREATE TABLE IF NOT EXISTS Reparacion_componente (
    ID_RC                INT          NOT NULL AUTO_INCREMENT,
    ID_REP               VARCHAR(30)  NOT NULL,
    ID_COM               INT,
    ES_REUTILIZADO       BOOLEAN      NOT NULL DEFAULT FALSE,
    ES_INCIDENCIA        BOOLEAN      NOT NULL DEFAULT FALSE,
    ES_RESUELTO          BOOLEAN      NOT NULL DEFAULT FALSE,
    INCIDENCIA           TEXT,
    OBSERVACIONES        TEXT,
    ES_SOLICITUD         TINYINT(1)   NOT NULL DEFAULT 0,
    DESCRIPCION_SOLICITUD TEXT,
    PRIMARY KEY (ID_RC),
    CONSTRAINT fk_rc_reparacion  FOREIGN KEY (ID_REP) REFERENCES Reparacion (ID_REP),
    CONSTRAINT fk_rc_componente  FOREIGN KEY (ID_COM) REFERENCES Componente  (ID_COM)
);
