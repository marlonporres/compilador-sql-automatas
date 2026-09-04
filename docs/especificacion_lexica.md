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

Las palabras reservadas no distinguen entre mayúsculas y minúsculas.

---

## 4. Identificadores

Representan nombres definidos por el usuario, como tablas y columnas.

Regla:

`letra o _` seguido de `letras, dígitos o _`

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

---

## 6. Operadores

Relacionales:

`>`
`<`
`>=`
`<=`
`!=`
`<>`

Actualmente el Scanner también reconoce:

`=` → asignación
`==` → igualdad

> La definición definitiva de `=` y `==` debe acordarse con el equipo antes de cerrar la Fase 1.

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

Los comentarios son ignorados por el analizador léxico.

---

## 9. Espacios en blanco

Espacios, tabulaciones y saltos de línea sirven como separadores y no generan tokens.

---

## 10. Errores léxicos

Todo símbolo o secuencia que no cumpla las reglas anteriores genera un token `ERROR`.

Ejemplos:

`85.5.2`
`!`
`'cadena sin cerrar`

---

## 11. Ejemplos válidos

```sql
CREAR TABLA estudiantes (id ENTERO, nombre TEXTO);

INSERTAR EN estudiantes VALORES (1, 'Carlos');

SELECCIONAR * DESDE estudiantes DONDE id >= 1;
