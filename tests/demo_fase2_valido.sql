-- Demostración válida: cubre los nueve comandos y los cinco tipos de dato.
CREAR TABLA usuarios (
    id ENTERO,
    nombre TEXTO,
    salario DECIMAL,
    fecha_nacimiento FECHA,
    activo BOOLEANO
);
CREAR TABLA productos (id ENTERO, precio DECIMAL);

INSERTAR EN usuarios VALORES (1, 'Carlos', 1250.50, '2000-02-29', VERDADERO);
SELECCIONAR nombre, salario DESDE usuarios
    DONDE salario >= 1000 Y NO (activo = FALSO) O id = 1;
ACTUALIZAR usuarios ASIGNAR salario = 1500, activo = FALSO DONDE id = 1;
BORRAR DESDE usuarios DONDE id <> 99;

ALTERAR TABLA usuarios AGREGAR correo TEXTO;
INSERTAR EN usuarios (id, nombre, correo) VALORES (2, 'María', 'maria@ejemplo.com');
ALTERAR TABLA usuarios MODIFICAR salario ENTERO;
ACTUALIZAR usuarios ASIGNAR salario = 1600 DONDE id = 2;
ALTERAR TABLA usuarios QUITAR correo;
RENOMBRAR TABLA usuarios A clientes;

SELECCIONAR * DESDE clientes DONDE fecha_nacimiento >= '1999-01-01';
TRUNCAR TABLA clientes;
ELIMINAR TABLA productos;
-- Catálogo final: clientes, con cinco columnas. No se almacenan filas.
