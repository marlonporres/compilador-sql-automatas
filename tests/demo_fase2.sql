-- ==========================================
-- SCRIPT DE DEMOSTRACION FASE 2
-- ==========================================
-- Tiene casos de los 4 integrantes, con errores intencionales
-- Las sentencias se revisan en orden y el catalogo empieza vacio
-- En cada caso puse lo que deberia salir


-- ------------------------------------------
-- Persona 1: sintaxis DDL
-- ------------------------------------------

-- bien, crea usuarios
CREAR TABLA usuarios (
    id ENTERO,
    nombre TEXTO,
    fecha_nacimiento FECHA,
    salario DECIMAL,
    activo BOOLEANO
);

-- error sintactico: a la columna le falta el tipo
CREAR TABLA incompleta (codigo);


-- ------------------------------------------
-- Persona 3: simbolos y semantica DDL
-- ------------------------------------------

-- bien, productos.id es otra columna diferente a usuarios.id
CREAR TABLA productos (
    id TEXTO,
    descripcion TEXTO,
    precio DECIMAL
);

-- SEM-001 usuarios ya existe en el recorrido completo
CREAR TABLA usuarios (correo TEXTO);

-- SEM-002 id esta repetida, pedidos no se crea
CREAR TABLA pedidos (
    id ENTERO,
    total DECIMAL,
    id FECHA
);

-- bien, truncar no cambia la tabla
TRUNCAR TABLA usuarios;

-- SEM-003 pedidos no existe porque no se creo
TRUNCAR TABLA pedidos;

-- bien, modifica el tipo y renombra con la forma completa
ALTERAR TABLA usuarios MODIFICAR salario DECIMAL;
RENOMBRAR TABLA productos A articulos;
RENOMBRAR TABLA articulos A productos;

-- bien, se crea temporal y se elimina
CREAR TABLA temporal (dato ENTERO);
ELIMINAR TABLA temporal;

-- SEM-003 temporal ya se elimino
ELIMINAR TABLA temporal;


-- ------------------------------------------
-- Persona 2: sintaxis DML
-- ------------------------------------------

-- bien
INSERTAR EN usuarios VALORES (1, 'Carlos', '2000-01-15', 1500.50, VERDADERO);
SELECCIONAR * DESDE usuarios;
SELECCIONAR nombre DESDE usuarios DONDE id >= 1 Y NO (salario < 1000 O activo = FALSO);
ACTUALIZAR usuarios ASIGNAR nombre = 'Pedro' DONDE id = 1;
BORRAR DESDE usuarios DONDE id = 1;

-- error sintactico: falta DESDE
SELECCIONAR nombre usuarios;

-- error sintactico: falta el valor despues del =
BORRAR DESDE usuarios DONDE id = ;


-- ------------------------------------------
-- Persona 4: semantica DML
-- ------------------------------------------

-- error: clientes no existe
SELECCIONAR * DESDE clientes;

-- error: usuarios no tiene correo
SELECCIONAR correo DESDE usuarios;

-- error: usuarios tiene 5 columnas y aqui van 2 valores
INSERTAR EN usuarios VALORES (2, 'Maria');

-- error: id es ENTERO
INSERTAR EN usuarios VALORES ('dos', 'Maria', '2001-02-02', 900.00, FALSO);

-- error: temporal ya se elimino
SELECCIONAR * DESDE temporal;


-- ------------------------------------------
-- Lexico (fase 1)
-- ------------------------------------------

-- error lexico: @ no es del lenguaje
SELECCIONAR nombre @ DESDE usuarios;
