-- ══════════════════════════════════════════════════════════════════════════════
-- crear_bd.sql  —  Reset completo + creación desde cero
-- Se puede relanzar cuantas veces se quiera — borra y recrea todo.
-- ══════════════════════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS reparaciones
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE reparaciones;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS Reparacion_componente;
DROP TABLE IF EXISTS Compra_componente;
DROP TABLE IF EXISTS Reparacion;
DROP TABLE IF EXISTS Usuario;
DROP TABLE IF EXISTS TipoCambio;
DROP TABLE IF EXISTS Proveedor;
DROP TABLE IF EXISTS Componente;
DROP TABLE IF EXISTS Telefono;
DROP TABLE IF EXISTS Tecnico;

SET FOREIGN_KEY_CHECKS = 1;

-- ── Tablas base (sin dependencias) ────────────────────────────────────────────

CREATE TABLE Componente (
    ID_COM         INT          NOT NULL AUTO_INCREMENT,
    TIPO           VARCHAR(100) NOT NULL UNIQUE,
    STOCK          INT          NOT NULL DEFAULT 0,
    STOCK_MINIMO   INT          NOT NULL DEFAULT 0,
    ACTIVO         BOOLEAN      NOT NULL DEFAULT TRUE,
    FECHA_REGISTRO TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (ID_COM)
);

CREATE TABLE Tecnico (
    ID_TEC  INT          NOT NULL AUTO_INCREMENT,
    NOMBRE  VARCHAR(100) NOT NULL,
    ACTIVO  BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (ID_TEC)
);

CREATE TABLE Telefono (
    IMEI  VARCHAR(15) NOT NULL,
    PRIMARY KEY (IMEI)
);

-- ── Tablas con dependencias ────────────────────────────────────────────────────

CREATE TABLE Usuario (
    ID_USU         INT          NOT NULL AUTO_INCREMENT,
    NOMBRE_USUARIO VARCHAR(50)  NOT NULL UNIQUE,
    PASSWORD       VARCHAR(255) NOT NULL,
    ROL            ENUM('ADMIN','TECNICO') NOT NULL,
    ID_TEC         INT          NULL,
    PRIMARY KEY (ID_USU),
    CONSTRAINT fk_usuario_tecnico FOREIGN KEY (ID_TEC) REFERENCES Tecnico (ID_TEC)
);

CREATE TABLE Reparacion (
    ID_REP          VARCHAR(30)  NOT NULL,
    FECHA_ASIG      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FECHA_FIN       DATETIME,
    IMEI            VARCHAR(15)  NOT NULL,
    ID_TEC          INT          NOT NULL,
    ID_REP_ANTERIOR VARCHAR(30),
    UPDATED_AT      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (ID_REP),
    CONSTRAINT fk_rep_telefono  FOREIGN KEY (IMEI)            REFERENCES Telefono  (IMEI),
    CONSTRAINT fk_rep_tecnico   FOREIGN KEY (ID_TEC)          REFERENCES Tecnico   (ID_TEC),
    CONSTRAINT fk_rep_anterior  FOREIGN KEY (ID_REP_ANTERIOR) REFERENCES Reparacion(ID_REP)
);

-- ── Tablas de stock ───────────────────────────────────────────────────────────

CREATE TABLE Proveedor (
    ID_PROV  INT          NOT NULL AUTO_INCREMENT,
    NOMBRE   VARCHAR(100) NOT NULL,
    ACTIVO   BOOLEAN      NOT NULL DEFAULT TRUE,
    DIVISA   VARCHAR(3)   NOT NULL DEFAULT 'EUR',
    PRIMARY KEY (ID_PROV)
);

CREATE TABLE TipoCambio (
    DIVISA   VARCHAR(3)    NOT NULL,
    FECHA    DATE          NOT NULL,
    TASA     DECIMAL(10,6) NOT NULL,
    PRIMARY KEY (DIVISA, FECHA)
);

CREATE TABLE Compra_componente (
    ID_COMPRA            INT           NOT NULL AUTO_INCREMENT,
    ID_COM               INT           NOT NULL,
    ID_PROV              INT           NOT NULL,
    CANTIDAD             INT           NOT NULL,
    CANTIDAD_RECIBIDA    INT,
    ES_URGENTE           BOOLEAN       NOT NULL DEFAULT FALSE,
    FECHA_PEDIDO         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FECHA_LLEGADA        DATETIME,
    PRECIO_UNIDAD_PEDIDO DECIMAL(10,2) NOT NULL,
    DIVISA               VARCHAR(3)    NOT NULL DEFAULT 'EUR',
    PRECIO_EUR           DECIMAL(10,2) NOT NULL,
    ESTADO               ENUM('pendiente','recibido','parcial','cancelado') NOT NULL DEFAULT 'pendiente',
    UPDATED_AT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (ID_COMPRA),
    CONSTRAINT fk_compra_componente FOREIGN KEY (ID_COM)  REFERENCES Componente (ID_COM),
    CONSTRAINT fk_compra_proveedor  FOREIGN KEY (ID_PROV) REFERENCES Proveedor  (ID_PROV)
);

-- ── Tablas de reparaciones ────────────────────────────────────────────────────

CREATE TABLE Reparacion_componente (
    ID_RC                INT          NOT NULL AUTO_INCREMENT,
    ID_REP               VARCHAR(30)  NOT NULL,
    ID_COM               INT,
    ES_REUTILIZADO       BOOLEAN      NOT NULL DEFAULT FALSE,
    ES_INCIDENCIA        BOOLEAN      NOT NULL DEFAULT FALSE,
    ES_RESUELTO          BOOLEAN      NOT NULL DEFAULT FALSE,
    INCIDENCIA           TEXT,
    OBSERVACIONES        TEXT,
    ES_SOLICITUD         BOOLEAN      NOT NULL DEFAULT FALSE,
    DESCRIPCION_SOLICITUD TEXT,
    ESTADO_SOLICITUD     ENUM('PENDIENTE','GESTIONADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
    CANTIDAD             INT          NOT NULL DEFAULT 1,
    UPDATED_AT           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (ID_RC),
    CONSTRAINT fk_rc_reparacion  FOREIGN KEY (ID_REP) REFERENCES Reparacion (ID_REP),
    CONSTRAINT fk_rc_componente  FOREIGN KEY (ID_COM) REFERENCES Componente  (ID_COM)
);
