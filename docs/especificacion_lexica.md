# Especificación Léxica — Fase 1

## 1. Objetivo

Definir los lexemas y tokens reconocidos por el analizador léxico del compilador SQL en español.

---

## 2. Alfabeto

El lenguaje utiliza:

- Letras.
- Dígitos del 0 al 9.
- Guion bajo: `_`
- Operadores: `=`, `>`, `<`, `!`
- Delimitadores: `,`, `;`, `(`, `)`, `*`
- Punto decimal: `.`
- Comilla simple: `'`
- Guion y barra para comentarios: `--`, `/* ... */`.
- Espacios, tabulaciones y saltos de línea.

---

## 3. Palabras reservadas

### DDL
`CREAR`, `TABLA`, `ALTERAR`, `ELIMINAR`, `TRUNCAR`, `RENOMBRAR`

### DML
`SELECCIONAR`, `INSERTAR`, `ACTUALIZAR`, `BORRAR`, `DESDE`, `DONDE`, `EN`, `VALORES`, `ASIGNAR`

### Tipos de dato
`ENTERO`, `TEXTO`, `FECHA`, `DECIMAL`, `BOOLEANO`

### Operadores lógicos
`Y`, `O`, `NO`

Las palabras reservadas no distinguen entre mayúsculas y minúsculas ni dependen
de la configuración regional. Su lexema se entrega en mayúsculas.

---

## 4. Identificadores

Representan nombres definidos por el usuario, como tablas y columnas.

Regla:

`letra o _` seguido de `letras, dígitos o _`

Las letras se reconocen con `Character.isLetter` de Java; los dígitos son `0` a `9`.
El token conserva el texto original del identificador. La tabla de símbolos
unifica sus apariciones sin distinguir mayúsculas y conserva la primera posición.
Palabras no incluidas en el catálogo, como `NULO` o `LLAVE`, son identificadores;
no se incorporan como nuevas reservadas.

Ejemplos válidos:

`usuarios`
`nombre_usuario`
`tabla1`
`_temporal`

---

## 5. Literales

### Entero
Uno o más dígitos.

Ejemplo: `25`

### Decimal
Dígitos, un punto y más dígitos.

Ejemplo: `85.50`

### Cadena
Texto delimitado por comillas simples.

Ejemplo: `'Carlos'`

El lexema del token contiene el texto sin las comillas. Se conserva el comportamiento
existente: la cadena puede atravesar saltos de línea y termina en la siguiente
comilla simple. No hay escapes de comillas. Si no se encuentra el cierre, se genera
un `ERROR` desde la posición de apertura y se consume hasta el fin del archivo.

---

## 6. Operadores

Relacionales:

`>`
`<`
`>=`
`<=`
`!=`
`<>`

`=` → `OP_IGUAL`, tanto en condiciones como después de `ASIGNAR`.
La distinción por contexto corresponde a una fase posterior.

`==` no pertenece al catálogo y genera un token `ERROR`. No existe `OP_ASIGNACION`.
`>` y `<` producen `OP_MAYOR` y `OP_MENOR`; `>=` y `<=` producen
`OP_MAYOR_IGUAL` y `OP_MENOR_IGUAL`; `!=` y `<>` producen `OP_DIFERENTE`.

---

## 7. Delimitadores

`,` coma
`;` punto y coma
`(` paréntesis izquierdo
`)` paréntesis derecho
`*` asterisco

---

## 8. Comentarios

Comentario de línea:

`-- comentario`

Comentario de bloque:

`/* comentario */`

Los comentarios cerrados son ignorados por el analizador léxico. El comentario
de línea termina en el salto de línea o en EOF. El comentario de bloque termina
en el primer `*/` (sin anidamiento); si falta el cierre, genera `ERROR` con el
texto completo desde `/*` y la posición de apertura.

---

## 9. Espacios en blanco

Espacios, tabulaciones y saltos de línea sirven como separadores y no generan tokens.

Las posiciones comienzan en línea 1, columna 1. LF, CRLF y CR cuentan como un salto
de línea, incluso dentro de cadenas y comentarios. Una tabulación avanza una columna.

---

## 10. Errores léxicos

Todo símbolo o secuencia que no cumpla las reglas anteriores genera un token `ERROR`.

Ejemplos:

`85.5.2`
`25.`
`==`
`!`
`@`
`'cadena sin cerrar`
`/* comentario sin cerrar`

Los números mal formados se consumen como una secuencia de dígitos y puntos.
Los caracteres desconocidos producen errores individuales. El scanner continúa
cuando queda entrada y agrega exactamente un `FIN_DE_ARCHIVO` con lexema `EOF`,
incluso tras un error o una entrada vacía. Los errores no entran a la tabla de símbolos.

---

## 11. Ejemplos válidos

```sql
CREAR TABLA estudiantes (id ENTERO, nombre TEXTO);

INSERTAR EN estudiantes VALORES (1, 'Carlos');

SELECCIONAR * DESDE estudiantes DONDE id >= 1;
```
