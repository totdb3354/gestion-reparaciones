-- ══════════════════════════════════════════════════════════════════════════════
-- datos_prueba_stock.sql  —  Genera 18 meses de entradas y salidas de stock
-- Idempotente: usa IF NOT EXISTS / INSERT IGNORE, se puede relanzar sin duplicar.
-- ══════════════════════════════════════════════════════════════════════════════

USE reparaciones;

DROP PROCEDURE IF EXISTS gen_datos_stock;
DELIMITER $$

CREATE PROCEDURE gen_datos_stock()
BEGIN
    DECLARE i        INT DEFAULT 0;
    DECLARE j        INT DEFAULT 0;
    DECLARE mes_base DATE;
    DECLARE fecha_ll DATETIME;
    DECLARE id_prov  INT;
    DECLARE id_tec   INT;
    DECLARE id_bat13 INT;
    DECLARE id_bat14 INT;
    DECLARE id_lcd13 INT;
    DECLARE id_lcd14 INT;
    DECLARE id_cha13 INT;
    DECLARE id_rep   VARCHAR(30);
    DECLARE n_reps   INT;
    DECLARE dia      INT;

    -- ── Proveedor ────────────────────────────────────────────────────────────
    SELECT ID_PROV INTO id_prov FROM Proveedor LIMIT 1;
    IF id_prov IS NULL THEN
        INSERT INTO Proveedor (NOMBRE) VALUES ('Proveedor Prueba');
        SET id_prov = LAST_INSERT_ID();
    END IF;

    -- ── Técnico ──────────────────────────────────────────────────────────────
    SELECT ID_TEC INTO id_tec FROM Tecnico WHERE ACTIVO = TRUE LIMIT 1;
    IF id_tec IS NULL THEN
        INSERT INTO Tecnico (NOMBRE, ACTIVO) VALUES ('Técnico Prueba', TRUE);
        SET id_tec = LAST_INSERT_ID();
    END IF;

    -- ── Teléfono de apoyo (IMEI ficticio) ────────────────────────────────────
    INSERT IGNORE INTO Telefono (IMEI) VALUES ('000000000000001');

    -- ── Componentes (crea solo si no existen) ────────────────────────────────
    SELECT ID_COM INTO id_bat13 FROM Componente WHERE TIPO = 'bati13' LIMIT 1;
    IF id_bat13 IS NULL THEN
        INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati13', 30, 5);
        SET id_bat13 = LAST_INSERT_ID();
    END IF;

    SELECT ID_COM INTO id_bat14 FROM Componente WHERE TIPO = 'bati14' LIMIT 1;
    IF id_bat14 IS NULL THEN
        INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati14', 20, 4);
        SET id_bat14 = LAST_INSERT_ID();
    END IF;

    SELECT ID_COM INTO id_lcd13 FROM Componente WHERE TIPO = 'lcdi13' LIMIT 1;
    IF id_lcd13 IS NULL THEN
        INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi13', 15, 3);
        SET id_lcd13 = LAST_INSERT_ID();
    END IF;

    SELECT ID_COM INTO id_lcd14 FROM Componente WHERE TIPO = 'lcdi14' LIMIT 1;
    IF id_lcd14 IS NULL THEN
        INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi14', 12, 3);
        SET id_lcd14 = LAST_INSERT_ID();
    END IF;

    SELECT ID_COM INTO id_cha13 FROM Componente WHERE TIPO = 'chai13negro' LIMIT 1;
    IF id_cha13 IS NULL THEN
        INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13negro', 8, 2);
        SET id_cha13 = LAST_INSERT_ID();
    END IF;

    -- ── Bucle principal: 18 meses hacia atrás ────────────────────────────────
    WHILE i < 18 DO
        -- Primer día del mes correspondiente
        SET mes_base = DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL (17 - i) MONTH), '%Y-%m-01');
        -- Fecha de llegada: un día aleatorio en los primeros 10 días del mes
        SET fecha_ll = DATE_ADD(mes_base, INTERVAL FLOOR(RAND() * 10) DAY);

        -- ── Compras recibidas (entradas de stock) ─────────────────────────
        INSERT INTO Compra_componente
            (ID_COM, ID_PROV, CANTIDAD, CANTIDAD_RECIBIDA, ESTADO,
             FECHA_PEDIDO, FECHA_LLEGADA, PRECIO_UNIDAD_PEDIDO, PRECIO_EUR)
        VALUES
            (id_bat13, id_prov, 12, 12, 'recibido',
             DATE_SUB(fecha_ll, INTERVAL 7 DAY), fecha_ll, 8.50,  8.50),
            (id_bat14, id_prov, 10, 10, 'recibido',
             DATE_SUB(fecha_ll, INTERVAL 7 DAY), fecha_ll, 9.00,  9.00),
            (id_lcd13, id_prov,  6,  6, 'recibido',
             DATE_SUB(fecha_ll, INTERVAL 7 DAY), fecha_ll, 22.00, 22.00),
            (id_lcd14, id_prov,  5,  5, 'recibido',
             DATE_SUB(fecha_ll, INTERVAL 7 DAY), fecha_ll, 25.00, 25.00),
            (id_cha13, id_prov,  4,  4, 'recibido',
             DATE_SUB(fecha_ll, INTERVAL 7 DAY), fecha_ll, 15.00, 15.00);

        -- ── Reparaciones finalizadas (salidas de stock): entre 3 y 7 por mes ─
        SET n_reps = 3 + FLOOR(RAND() * 5);
        SET j = 0;
        WHILE j < n_reps DO
            SET dia    = 2 + FLOOR(RAND() * 26);
            SET id_rep = CONCAT('TST-', DATE_FORMAT(mes_base, '%Y%m'), '-', LPAD(j + 1, 3, '0'));

            IF NOT EXISTS (SELECT 1 FROM Reparacion WHERE ID_REP = id_rep) THEN
                INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC)
                VALUES (id_rep,
                        DATE_ADD(mes_base, INTERVAL dia - 1 DAY),
                        DATE_ADD(mes_base, INTERVAL dia     DAY),
                        '000000000000001',
                        id_tec);

                -- Batería: en el 80 % de reparaciones iPhone 13, 70 % iPhone 14
                IF RAND() < 0.80 THEN
                    INSERT INTO Reparacion_componente (ID_REP, ID_COM, CANTIDAD, ES_REUTILIZADO)
                    VALUES (id_rep, id_bat13, 1, FALSE);
                END IF;
                IF RAND() < 0.70 THEN
                    INSERT INTO Reparacion_componente (ID_REP, ID_COM, CANTIDAD, ES_REUTILIZADO)
                    VALUES (id_rep, id_bat14, 1, FALSE);
                END IF;

                -- LCD: ~40 % de las reparaciones
                IF RAND() < 0.40 THEN
                    INSERT INTO Reparacion_componente (ID_REP, ID_COM, CANTIDAD, ES_REUTILIZADO)
                    VALUES (id_rep, id_lcd13, 1, FALSE);
                END IF;
                IF RAND() < 0.35 THEN
                    INSERT INTO Reparacion_componente (ID_REP, ID_COM, CANTIDAD, ES_REUTILIZADO)
                    VALUES (id_rep, id_lcd14, 1, FALSE);
                END IF;

                -- Chasis: ~15 % de las reparaciones
                IF RAND() < 0.15 THEN
                    INSERT INTO Reparacion_componente (ID_REP, ID_COM, CANTIDAD, ES_REUTILIZADO)
                    VALUES (id_rep, id_cha13, 1, FALSE);
                END IF;
            END IF;

            SET j = j + 1;
        END WHILE;

        SET i = i + 1;
    END WHILE;
END $$

DELIMITER ;

CALL gen_datos_stock();
DROP PROCEDURE IF EXISTS gen_datos_stock;

-- ── Ajustar STOCK para que sea coherente con los eventos generados ────────────
-- stock_real = suma de todas las entradas recibidas - todas las salidas por reparación
-- Así la reconstrucción histórica parte de 0 antes del primer evento y sube correctamente.
UPDATE Componente c
SET STOCK = (
    SELECT COALESCE(SUM(delta), 0)
    FROM (
        SELECT cc.ID_COM AS id, COALESCE(cc.CANTIDAD_RECIBIDA, 0) AS delta
        FROM Compra_componente cc
        WHERE cc.ESTADO IN ('recibido','parcial') AND cc.FECHA_LLEGADA IS NOT NULL

        UNION ALL

        SELECT rc.ID_COM AS id, -rc.CANTIDAD AS delta
        FROM Reparacion_componente rc
        JOIN Reparacion r ON r.ID_REP = rc.ID_REP
        WHERE r.FECHA_FIN IS NOT NULL
          AND rc.ES_REUTILIZADO = FALSE
          AND rc.ID_COM IS NOT NULL
    ) e
    WHERE e.id = c.ID_COM
)
WHERE c.TIPO IN ('bati13','bati14','lcdi13','lcdi14','chai13negro');
