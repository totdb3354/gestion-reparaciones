USE reparaciones;

SET SQL_SAFE_UPDATES = 0;

UPDATE Reparacion SET ID_REP_ANTERIOR = NULL;
DELETE FROM Reparacion_componente;
DELETE FROM Reparacion;
DELETE FROM Usuario;
DELETE FROM Tecnico;
DELETE FROM Telefono;

UPDATE Componente SET STOCK = 5, STOCK_MINIMO = 2;

-- Técnicos
INSERT INTO Tecnico (ID_TEC, NOMBRE) VALUES (1, 'Ángelo');
INSERT INTO Tecnico (ID_TEC, NOMBRE) VALUES (2, 'Daniel');
INSERT INTO Tecnico (ID_TEC, NOMBRE) VALUES (3, 'Admin');

-- Teléfonos
-- 987654321000003 → reparación normal
-- 345234532340002 → incidencia abierta
-- 122323525560001 → incidencia resuelta
-- 111111111111111 → pendiente Ángelo (teléfono nuevo, sin historial)
-- 222222222222222 → pendiente Daniel  (teléfono nuevo, sin historial)
INSERT INTO Telefono (IMEI) VALUES (987654321000003);
INSERT INTO Telefono (IMEI) VALUES (345234532340002);
INSERT INTO Telefono (IMEI) VALUES (122323525560001);
INSERT INTO Telefono (IMEI) VALUES (111111111111111);
INSERT INTO Telefono (IMEI) VALUES (222222222222222);

-- ── Escenario 1: Reparación normal sin incidencia ──────────────────────────
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('R20260210_1', '2026-02-10 09:00:00', '2026-02-10 10:00:00', 987654321000003, 1, NULL);

INSERT INTO Reparacion_componente (ID_REP, ID_COM, ES_REUTILIZADO, ES_INCIDENCIA, ES_RESUELTO, INCIDENCIA, OBSERVACIONES)
VALUES ('R20260210_1', 36, FALSE, FALSE, FALSE, NULL, 'Batería sustituida correctamente');

-- ── Escenario 2: Incidencia abierta ───────────────────────────────────────
-- ES_INCIDENCIA=TRUE, ES_RESUELTO=FALSE, ninguna reparación posterior apunta a esta
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('R20260211_1', '2026-02-11 10:00:00', '2026-02-11 11:00:00', 345234532340002, 2, NULL);

INSERT INTO Reparacion_componente (ID_REP, ID_COM, ES_REUTILIZADO, ES_INCIDENCIA, ES_RESUELTO, INCIDENCIA, OBSERVACIONES)
VALUES ('R20260211_1', 52, FALSE, TRUE, FALSE, 'No se ha pegado bien la batería', 'El dispositivo se reinicia');

-- ── Escenario 3: Incidencia resuelta ──────────────────────────────────────
-- R3: reparación con incidencia, marcada como resuelta (ES_RESUELTO=TRUE)
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('R20260215_1', '2026-02-15 08:00:00', '2026-02-15 09:00:00', 122323525560001, 1, NULL);

INSERT INTO Reparacion_componente (ID_REP, ID_COM, ES_REUTILIZADO, ES_INCIDENCIA, ES_RESUELTO, INCIDENCIA, OBSERVACIONES)
VALUES ('R20260215_1', 70, FALSE, TRUE, TRUE, 'Pantalla no encendía tras la reparación', 'Pantalla instalada pero sin retroiluminación');

-- R4: reparación que resuelve la incidencia de R3 — apunta a R3 via ID_REP_ANTERIOR
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('R20260216_1', '2026-02-16 09:00:00', '2026-02-16 10:00:00', 122323525560001, 1, 'R20260215_1');

INSERT INTO Reparacion_componente (ID_REP, ID_COM, ES_REUTILIZADO, ES_INCIDENCIA, ES_RESUELTO, INCIDENCIA, OBSERVACIONES)
VALUES ('R20260216_1', 70, FALSE, FALSE, FALSE, NULL, 'Incidencia resuelta, pantalla recolocada y retroiluminación restaurada');

-- ── Escenario 4: Asignaciones pendientes ───────────────────────────────────
-- A1: Ángelo, teléfono nuevo → fila blanca en pendientes
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('A20260313_1', '2026-03-13 09:00:00', NULL, 111111111111111, 1, NULL);

-- A2: Daniel, teléfono nuevo → fila blanca en pendientes
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('A20260313_2', '2026-03-13 09:30:00', NULL, 222222222222222, 2, NULL);

-- A3: Daniel, mismo IMEI que R20260211_1 (incidencia abierta) → fila roja en pendientes
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('A20260313_3', '2026-03-13 10:00:00', NULL, 345234532340002, 2, NULL);

-- Usuarios
INSERT INTO Usuario (NOMBRE_USUARIO, PASSWORD, ROL, ID_TEC)
VALUES ('admin', '$2a$10$89OoWr1AD1dbESqCVGhGVOBWILu0ld117qcCSS68z1dH4k/A7MjBu', 'ADMIN', 3);

INSERT INTO Usuario (NOMBRE_USUARIO, PASSWORD, ROL, ID_TEC)
VALUES ('angelo', '$2a$10$WwWX69tAaqxGhUmwD6Rlj.Z0k3deSmIwpidcGBNnAhHD.wlAraVRa', 'TECNICO', 1);

INSERT INTO Usuario (NOMBRE_USUARIO, PASSWORD, ROL, ID_TEC)
VALUES ('daniel', '$2a$10$WwWX69tAaqxGhUmwD6Rlj.Z0k3deSmIwpidcGBNnAhHD.wlAraVRa', 'TECNICO', 2);

SET SQL_SAFE_UPDATES = 1;
