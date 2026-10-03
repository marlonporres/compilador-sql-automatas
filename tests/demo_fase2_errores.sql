-- Los errores siguientes son intencionales y deben producir salida 1.
CREAR TABLA usuarios (id ENTERO, nombre TEXTO);
INSERTAR EN usuarios VALORES (1, 'Ana');
-- Semánticos: columna inexistente, cantidad y tipo de valores.
SELECCIONAR inexistente DESDE usuarios;
INSERTAR EN usuarios VALORES (2);
ACTUALIZAR usuarios ASIGNAR id = 'texto' DONDE id = 1;
-- Sintáctico: falta el tipo de dato.
CREAR TABLA rota (dato);
-- Léxico: carácter no reconocido. Se descarta esta sentencia.
INSERTAR EN usuarios VALORES (3, @);
-- Semánticos: duplicado, destino ocupado y comparación incompatible.
CREAR TABLA usuarios (otro ENTERO);
CREAR TABLA ocupada (id ENTERO);
RENOMBRAR TABLA usuarios A ocupada;
SELECCIONAR * DESDE usuarios DONDE id = 'texto';
-- Recuperación: esta sentencia debe aparecer y ser válida.
CREAR TABLA recuperada (id ENTERO);
SELECCIONAR * DESDE recuperada;
