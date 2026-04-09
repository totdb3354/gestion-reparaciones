-- ══════════════════════════════════════════════════════════════════════════════
-- datos_prueba.sql  —  Reset completo + SKUs + datos de prueba
-- Ejecutar después de crear_bd.sql. Se puede relanzar cuantas veces se quiera.
-- ══════════════════════════════════════════════════════════════════════════════

USE reparaciones;

SET SQL_SAFE_UPDATES  = 0;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE Compra_componente;
TRUNCATE TABLE TipoCambio;
TRUNCATE TABLE Reparacion_componente;
TRUNCATE TABLE Reparacion;
TRUNCATE TABLE Usuario;
TRUNCATE TABLE Telefono;
TRUNCATE TABLE Tecnico;
TRUNCATE TABLE Proveedor;
TRUNCATE TABLE Componente;

SET FOREIGN_KEY_CHECKS = 1;

-- ── Otros ─────────────────────────────────────────────────────────────────────
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi8', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi8plus', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroise2020', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroix', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroixr', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroixs', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroixsmax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi11', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi11pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi11promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi12', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi12mini', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi12pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi12promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi13', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi13mini', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi13pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi13promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi14', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi14plus', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi14pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi14promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi15', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi15plus', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi15pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi15promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi16', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi16e', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi16pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi16promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('otroi16plus', 5, 2);

-- ── Batería ───────────────────────────────────────────────────────────────────
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati8', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati8plus', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('batise2020', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('batix', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('batixr', 2, 5);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('batixs', 1, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('batixsmax', 3, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati11', 2, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati11pro', 1, 3);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati11promax', 3, 6);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati12', 2, 3);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati12mini', 1, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati12pro', 2, 5);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati12promax', 3, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati13', 7, 3);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati13mini', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati13pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati13promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati14', 10, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati14plus', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati14pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati14promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati15', 12, 5);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati15plus', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati15pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati15promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati16', 14, 5);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati16e', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati16pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati16promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('bati16plus', 5, 2);

-- ── Pantalla ──────────────────────────────────────────────────────────────────
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi11negraic', 1, 3);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi11pronegraic', 2, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi11promaxnegraic', 3, 5);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi12negraic', 2, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi12mininegraic', 1, 3);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi12promaxnegraic', 3, 5);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi13negraic', 8, 3);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi13mininegraic', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi13pronegraic', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi13promaxnegraic', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi14negraic', 10, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi14pronegraic', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi14plusnegraic', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi14promaxnegraic', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi15negraic', 12, 5);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi15plusnegraic', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi15pronegraic', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi15promaxnegraic', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi16negraic', 15, 5);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi16enegraic', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi16pronegraic', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi16promaxnegraic', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('lcdi16plusnegraic', 5, 2);

-- ── Cámara ────────────────────────────────────────────────────────────────────
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami8', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami8plus', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('camise2020', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('camix', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('camixr', 2, 3);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('camixs', 1, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('camixsmax', 3, 5);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami11', 1, 3);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami11pro', 2, 5);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami11promax', 3, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami12', 2, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami12mini', 1, 3);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami12pro', 3, 5);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami12promax', 2, 3);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami13', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami13mini', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami13pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami13promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami14', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami14plus', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami14pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami14promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami15', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami15plus', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami15pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami15promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami16', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami16e', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami16pro', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami16promax', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('cami16plus', 5, 2);

-- ── Chasis ────────────────────────────────────────────────────────────────────
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai8silver', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai8gold', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai8spacegrey', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai8productred', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai8plussilver', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai8plusgold', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai8plusspacegrey', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai8plusproductred', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaise2020black', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaise2020white', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaise2020productred', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixsilver', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixspacegrey', 0, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixssilver', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixsspacegrey', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixsgold', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixsmaxsilver', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixsmaxspacegrey', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixsmaxgold', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixrproductred', 1, 3);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixryellow', 2, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixrwhite', 3, 5);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixrcoral', 1, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixrblack', 2, 3);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chaixrblue', 3, 4);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11black', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11green', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11yellow', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11purple', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11productred', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11white', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11progold', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11prospacegray', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11prosilver', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11promidnightgreen', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11promaxgold', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11promaxspacegray', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11promaxsilver', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai11promaxmidnightgreen', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12black', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12white', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12productred', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12green', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12blue', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12purple', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12miniblack', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12miniwhite', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12miniproductred', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12minigreen', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12miniblue', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12minipurple', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12prosilver', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12prographite', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12progold', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12propacificblue', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12promaxsilver', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12promaxgraphite', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12promaxgold', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai12promaxpacificblue', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13productred', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13starlight', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13midnight', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13pink', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13blue', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13green', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13miniproductred', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13ministarlight', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13minimidnight', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13miniblue', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13minipink', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13minigreen', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13prographite', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13progold', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13prosilver', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13prosierrablue', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13proalpinegreen', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13promaxgraphite', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13promaxgold', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13promaxsilver', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13promaxsierrablue', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai13promaxalpinegreen', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14midnight', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14purple', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14starlight', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14productred', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14blue', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14yellow', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14plusmidnight', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14pluspurple', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14plusstarlight', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14plusproductred', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14plusblue', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14plusyellow', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14prospaceblack', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14prosilver', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14progold', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14prodeeppurple', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14promaxspaceblack', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14promaxsilver', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14promaxgold', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai14promaxdeeppurple', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15black', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15blue', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15green', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15yellow', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15pink', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15plusblack', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15plusblue', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15plusgreen', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15plusyellow', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15pluspink', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15problacktitanium', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15prowhitetitanium', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15probluetitanium', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15pronaturaltitanium', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15promaxblacktitanium', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15promaxwhitetitanium', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15promaxbluetitanium', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('chai15promaxnaturaltitanium', 5, 2);

-- ── Glass ─────────────────────────────────────────────────────────────────────
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi6sblanca', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi6snegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi6splusblanca', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi6splusnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi7blanca', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi7negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi7plusblanca', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi7plusnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi8blanca', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi8negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi8plusblanca', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi8plusnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gixnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gixrnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gixsmaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi11negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi11pronegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi11promaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi12negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi12mininegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi12promaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi13negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi13mininegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi13promaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi14negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi14plusnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi14pronegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi14promaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi15negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi15plusnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi15pronegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi15promaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi16negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi16plusnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi16pronegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi16enegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('gi16promaxnegra', 5, 2);

-- ── Marco ─────────────────────────────────────────────────────────────────────
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mcixnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mcixsnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mcixsmaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci11pronegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci11promaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci12negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci12mininegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci12promaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci13negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci13mininegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci13pronegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci13promaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci14negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci14plusnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci14pronegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci14promaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci15negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci15plusnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci15pronegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci15promaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci16negra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci16promaxnegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci16pronegra', 5, 2);
INSERT INTO Componente (TIPO, STOCK, STOCK_MINIMO) VALUES ('mci16enegra', 5, 2);

-- ── Técnicos ──────────────────────────────────────────────────────────────────
INSERT INTO Tecnico (ID_TEC, NOMBRE) VALUES (1, 'Ángelo');
INSERT INTO Tecnico (ID_TEC, NOMBRE) VALUES (2, 'Daniel');
INSERT INTO Tecnico (ID_TEC, NOMBRE) VALUES (3, 'Admin');

-- ── Usuarios ──────────────────────────────────────────────────────────────────
INSERT INTO Usuario (NOMBRE_USUARIO, PASSWORD, ROL, ID_TEC)
VALUES ('admin',  '$2a$10$89OoWr1AD1dbESqCVGhGVOBWILu0ld117qcCSS68z1dH4k/A7MjBu', 'ADMIN',   3);
INSERT INTO Usuario (NOMBRE_USUARIO, PASSWORD, ROL, ID_TEC)
VALUES ('angelo', '$2a$10$WwWX69tAaqxGhUmwD6Rlj.Z0k3deSmIwpidcGBNnAhHD.wlAraVRa', 'TECNICO', 1);
INSERT INTO Usuario (NOMBRE_USUARIO, PASSWORD, ROL, ID_TEC)
VALUES ('daniel', '$2a$10$WwWX69tAaqxGhUmwD6Rlj.Z0k3deSmIwpidcGBNnAhHD.wlAraVRa', 'TECNICO', 2);

-- ── Teléfonos ─────────────────────────────────────────────────────────────────
-- 987654321000003 → reparación normal
-- 345234532340002 → incidencia abierta
-- 122323525560001 → incidencia resuelta
-- 111111111111111 → pendiente Ángelo (sin historial)
-- 222222222222222 → pendiente Daniel  (sin historial)
-- 333333333333333 → pendiente Ángelo con solicitud de pieza
INSERT INTO Telefono (IMEI) VALUES ('987654321000003');
INSERT INTO Telefono (IMEI) VALUES ('345234532340002');
INSERT INTO Telefono (IMEI) VALUES ('122323525560001');
INSERT INTO Telefono (IMEI) VALUES ('111111111111111');
INSERT INTO Telefono (IMEI) VALUES ('222222222222222');
INSERT INTO Telefono (IMEI) VALUES ('333333333333333');

-- ── Escenario 1: Reparación normal sin incidencia ─────────────────────────────
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('R20260210_1', '2026-02-10 09:00:00', '2026-02-10 10:00:00', '987654321000003', 1, NULL);
INSERT INTO Reparacion_componente (ID_REP, ID_COM, ES_REUTILIZADO, ES_INCIDENCIA, ES_RESUELTO, INCIDENCIA, OBSERVACIONES, ES_SOLICITUD, DESCRIPCION_SOLICITUD, CANTIDAD)
VALUES ('R20260210_1', 36, FALSE, FALSE, FALSE, NULL, 'Batería sustituida correctamente', 0, NULL, 1);

-- ── Escenario 2: Incidencia abierta ──────────────────────────────────────────
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('R20260211_1', '2026-02-11 10:00:00', '2026-02-11 11:00:00', '345234532340002', 2, NULL);
INSERT INTO Reparacion_componente (ID_REP, ID_COM, ES_REUTILIZADO, ES_INCIDENCIA, ES_RESUELTO, INCIDENCIA, OBSERVACIONES, ES_SOLICITUD, DESCRIPCION_SOLICITUD, CANTIDAD)
VALUES ('R20260211_1', 52, FALSE, TRUE, FALSE, 'No se ha pegado bien la batería', 'El dispositivo se reinicia', 0, NULL, 1);

-- ── Escenario 3: Incidencia resuelta ─────────────────────────────────────────
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('R20260215_1', '2026-02-15 08:00:00', '2026-02-15 09:00:00', '122323525560001', 1, NULL);
INSERT INTO Reparacion_componente (ID_REP, ID_COM, ES_REUTILIZADO, ES_INCIDENCIA, ES_RESUELTO, INCIDENCIA, OBSERVACIONES, ES_SOLICITUD, DESCRIPCION_SOLICITUD, CANTIDAD)
VALUES ('R20260215_1', 70, FALSE, TRUE, TRUE, 'Pantalla no encendía tras la reparación', 'Pantalla instalada pero sin retroiluminación', 0, NULL, 1);

INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('R20260216_1', '2026-02-16 09:00:00', '2026-02-16 10:00:00', '122323525560001', 1, 'R20260215_1');
INSERT INTO Reparacion_componente (ID_REP, ID_COM, ES_REUTILIZADO, ES_INCIDENCIA, ES_RESUELTO, INCIDENCIA, OBSERVACIONES, ES_SOLICITUD, DESCRIPCION_SOLICITUD, CANTIDAD)
VALUES ('R20260216_1', 70, FALSE, FALSE, FALSE, NULL, 'Incidencia resuelta, pantalla recolocada y retroiluminación restaurada', 0, NULL, 1);

-- ── Escenario 4 (bis): Componente roto durante instalación — se usaron 2 unidades ──
-- Primera pantalla se fisuró al encajar el conector; se instaló una segunda
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('R20260301_1', '2026-03-01 09:00:00', '2026-03-01 11:30:00', '987654321000003', 2, NULL);
INSERT INTO Reparacion_componente (ID_REP, ID_COM, ES_REUTILIZADO, ES_INCIDENCIA, ES_RESUELTO, INCIDENCIA, OBSERVACIONES, ES_SOLICITUD, DESCRIPCION_SOLICITUD, CANTIDAD)
VALUES ('R20260301_1', 70, FALSE, FALSE, FALSE, NULL, 'Primera pantalla fisurada al encajar conector; se instaló segunda unidad sin incidencias', 0, NULL, 2);

-- ── Escenario 4: Asignaciones pendientes ─────────────────────────────────────
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('A20260313_1', '2026-03-13 09:00:00', NULL, '111111111111111', 1, NULL);

INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('A20260313_2', '2026-03-13 09:30:00', NULL, '222222222222222', 2, NULL);

INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('A20260313_3', '2026-03-13 10:00:00', NULL, '345234532340002', 2, NULL);

-- ── Escenario 5: Asignación con solicitud de pieza ────────────────────────────
INSERT INTO Reparacion (ID_REP, FECHA_ASIG, FECHA_FIN, IMEI, ID_TEC, ID_REP_ANTERIOR)
VALUES ('A20260318_1', '2026-03-18 11:00:00', NULL, '333333333333333', 1, NULL);
INSERT INTO Reparacion_componente (ID_REP, ID_COM, ES_REUTILIZADO, ES_INCIDENCIA, ES_RESUELTO, INCIDENCIA, OBSERVACIONES, ES_SOLICITUD, DESCRIPCION_SOLICITUD, CANTIDAD)
VALUES ('A20260318_1', 36, FALSE, FALSE, FALSE, NULL, NULL, 1, 'La batería del proveedor habitual está agotada. Pendiente de reposición.', 1);

-- ── Proveedores ───────────────────────────────────────────────────────────────
INSERT INTO Proveedor (NOMBRE, ACTIVO) VALUES ('iPartsBuy', TRUE);
INSERT INTO Proveedor (NOMBRE, ACTIVO) VALUES ('Mobile Wholesale EU', TRUE);
INSERT INTO Proveedor (NOMBRE, ACTIVO) VALUES ('Fix Phone Parts', FALSE);

-- ── Pedidos de componentes ────────────────────────────────────────────────────
-- ID_COM=52 bati14pro (32 otros + 20 baterías = ID_COM 52 = bati14pro)
-- ID_COM=79 lcdi15pronegraic (32+31 baterías = 63 inicio LCD; 79-63+1=17ª lcd = lcdi15pronegraic)
-- ID_COM=46 bati13          (32+14=46)
-- ID_COM=83 lcdi16pronegraic (63+20=83)

-- Pedido 1: urgente, pendiente (bati14pro × 20 u., EUR)
INSERT INTO Compra_componente
    (ID_COM, ID_PROV, CANTIDAD, ES_URGENTE, FECHA_PEDIDO,
     PRECIO_UNIDAD_PEDIDO, DIVISA, PRECIO_EUR, ESTADO)
VALUES (52, 1, 20, TRUE, '2026-04-01 09:00:00', 8.50, 'EUR', 8.50, 'pendiente');

-- Pedido 2: recibido completo (lcdi15pronegraic × 5 u., USD)
INSERT INTO Compra_componente
    (ID_COM, ID_PROV, CANTIDAD, ES_URGENTE, FECHA_PEDIDO,
     PRECIO_UNIDAD_PEDIDO, DIVISA, PRECIO_EUR,
     ESTADO, FECHA_LLEGADA, CANTIDAD_RECIBIDA, OBSERVACION_LLEGADA)
VALUES (79, 2, 5, FALSE, '2026-03-15 10:00:00',
        18.00, 'USD', 16.56,
        'recibido', '2026-03-22 16:30:00', 5, 'Pedido recibido sin incidencias.');

-- Pedido 3: cancelado (bati13 × 10 u., EUR)
INSERT INTO Compra_componente
    (ID_COM, ID_PROV, CANTIDAD, ES_URGENTE, FECHA_PEDIDO,
     PRECIO_UNIDAD_PEDIDO, DIVISA, PRECIO_EUR, ESTADO)
VALUES (46, 3, 10, FALSE, '2026-03-20 11:00:00', 7.00, 'EUR', 7.00, 'cancelado');

-- Pedido 4: urgente, pendiente (lcdi16pronegraic × 3 u., USD)
INSERT INTO Compra_componente
    (ID_COM, ID_PROV, CANTIDAD, ES_URGENTE, FECHA_PEDIDO,
     PRECIO_UNIDAD_PEDIDO, DIVISA, PRECIO_EUR, ESTADO)
VALUES (83, 1, 3, TRUE, '2026-04-07 14:00:00', 22.00, 'USD', 20.24, 'pendiente');

SET SQL_SAFE_UPDATES = 1;
