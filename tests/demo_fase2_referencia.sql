-- Referencia independiente del ejemplo demo_fase2_valido.sql.
-- Se ejecuta en otra base vacía; no se genera desde el catálogo final.
CREATE TABLE usuarios (
    id INTEGER, nombre VARCHAR(200), salario DECIMAL(20,2),
    fecha_nacimiento DATE, activo BOOLEAN
);
CREATE TABLE productos (id INTEGER, precio DECIMAL(20,2));
INSERT INTO usuarios VALUES (1, 'Carlos', 1250.50, '2000-02-29', TRUE);
SELECT nombre, salario FROM usuarios
    WHERE salario >= 1000 AND NOT (activo = FALSE) OR id = 1;
UPDATE usuarios SET salario = 1500, activo = FALSE WHERE id = 1;
DELETE FROM usuarios WHERE id <> 99;
ALTER TABLE usuarios ADD COLUMN correo VARCHAR(200);
INSERT INTO usuarios (id, nombre, correo) VALUES (2, 'María', 'maria@ejemplo.com');
ALTER TABLE usuarios ALTER COLUMN salario INTEGER;
UPDATE usuarios SET salario = 1600 WHERE id = 2;
ALTER TABLE usuarios DROP COLUMN correo;
ALTER TABLE usuarios RENAME TO clientes;
SELECT * FROM clientes WHERE fecha_nacimiento >= '1999-01-01';
TRUNCATE TABLE clientes;
DROP TABLE productos;
