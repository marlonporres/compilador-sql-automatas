-- ==========================================
-- PRUEBAS DDL VALIDAS
-- ==========================================

CREAR TABLA usuarios (
    id ENTERO,
    nombre TEXTO,
    fecha_nacimiento FECHA,
    salario DECIMAL,
    activo BOOLEANO
);

CREAR TABLA productos (
    id ENTERO,
    descripcion TEXTO,
    precio DECIMAL
);

ALTERAR TABLA usuarios;

RENOMBRAR TABLA productos;

TRUNCAR TABLA usuarios;

ELIMINAR TABLA productos;


-- ==========================================
-- PRUEBAS DE ERRORES LEXICOS
-- ==========================================

CREAR TABLA prueba (
    numero 85.5.2,
    simbolo !,
    texto 'cadena sin cerrar
);

CREAR TABLA otra_prueba (
    dato @
);