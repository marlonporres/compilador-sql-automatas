-- ==========================================
-- PRUEBAS DML VALIDAS
-- ==========================================

INSERTAR EN usuarios VALORES (1, 'Carlos');

INSERTAR EN usuarios VALORES (2, 'Maria');

INSERTAR EN usuarios VALORES (3, 'Jose');

SELECCIONAR * DESDE usuarios;

SELECCIONAR nombre DESDE usuarios DONDE id = 1;

SELECCIONAR nombre DESDE usuarios DONDE id >= 1;

SELECCIONAR nombre DESDE usuarios DONDE id <= 3;

SELECCIONAR nombre DESDE usuarios DONDE id != 2;

ACTUALIZAR usuarios ASIGNAR nombre = 'Pedro' DONDE id = 1;

BORRAR DESDE usuarios DONDE id = 3;


-- ==========================================
-- PRUEBAS DE ERRORES LEXICOS
-- ==========================================

INSERTAR EN usuarios VALORES (4, 'Ana');

SELECCIONAR nombre @ DESDE usuarios;

ACTUALIZAR usuarios ASIGNAR nombre = 'Luis' DONDE id ! 5;

INSERTAR EN usuarios VALORES (5, 99.8.7);

INSERTAR EN usuarios VALORES (6, 'Texto sin cerrar);