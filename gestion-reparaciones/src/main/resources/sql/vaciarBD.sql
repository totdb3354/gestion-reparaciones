USE reparaciones;

SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- Vaciar datos operativos
TRUNCATE TABLE Reparacion_componente;
TRUNCATE TABLE Reparacion;
TRUNCATE TABLE Telefono;

-- Resetear stock de componentes
UPDATE Componente SET STOCK = 5, STOCK_MINIMO = 2;

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;   