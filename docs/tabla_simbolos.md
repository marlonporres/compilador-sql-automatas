# Tabla de Símbolos — Fase 1

**Módulo del Integrante 2.**
Paquete `symbol_table`. Depende únicamente de `lexer.Token` y `lexer.TipoToken`.

---

## 1. Qué es y qué resuelve

La tabla de símbolos es la estructura donde el compilador guarda **los nombres que
el usuario declara o usa** en la entrada: nombres de tablas y de columnas. El
analizador léxico produce un flujo de tokens; de ese flujo, la tabla de símbolos
se queda solo con los identificadores y les asocia la información que las fases
posteriores van a necesitar (posición, categoría, tipo de dato).

En la Fase 1 el análisis es únicamente léxico, así que la tabla responde a tres
preguntas:

1. ¿Qué identificadores distintos aparecen en la entrada?
2. ¿Dónde apareció cada uno por primera vez (línea y columna)?
3. ¿Cuántas veces se usó cada uno?

---

## 2. Archivos

| Archivo | Rol |
|---|---|
| `src/symbol_table/Simbolo.java` | Una entrada de la tabla: un identificador y sus atributos. |
| `src/symbol_table/TablaSimbolos.java` | La estructura: inserción, búsqueda, existencia, duplicados, impresión. |
| `src/symbol_table/CategoriaSimbolo.java` | Enum `TABLA`, `COLUMNA`, `DESCONOCIDA`. |
| `src/symbol_table/PruebaTablaSimbolos.java` | Pruebas aisladas del módulo (50 verificaciones). |
| `tests/PruebaFase1.java` | Regresiones del scanner y de su integración real con la tabla y Main. |

---

## 3. Decisiones de diseño

### 3.1 Estructura interna: `LinkedHashMap<String, Simbolo>`

- **HashMap** → inserción, búsqueda y comprobación de existencia en **O(1) promedio**.
  Una lista o arreglo obligaría a recorrer todo para detectar un duplicado: O(n)
  por cada identificador, O(n²) en el archivo completo.
- **Linked** → conserva el **orden de aparición**. La tabla impresa se puede leer
  de arriba abajo contra el archivo `.sql` de prueba, lo que hace la demostración
  verificable a simple vista.

### 3.2 La clave es el nombre normalizado a mayúsculas

SQL no distingue mayúsculas en los identificadores: `usuarios`, `Usuarios` y
`USUARIOS` nombran la misma tabla. Por eso la identidad del símbolo es
`Simbolo.normalizar(nombre)` (recorte de espacios + mayúsculas con `Locale.ROOT`), mientras que el
campo `nombre` conserva el **lexema original** tal como se escribió la primera
vez. Así la prevención de duplicados es correcta sin perder el texto real de la
entrada.

### 3.3 Regla de duplicados

Un identificador ocupa **una sola fila**. Si vuelve a aparecer:

- no se inserta una fila nueva,
- se incrementa `apariciones`,
- se conservan la **línea y columna de la primera** aparición.

`insertar(...)` devuelve `false` ante un duplicado (rechazo explícito).
`registrar(...)` es el método que usa el analizador: inserta si es nuevo, cuenta
si ya estaba.

### 3.4 Qué NO entra a la tabla

Solo se registran los tokens de tipo `IDENTIFICADOR`. Quedan fuera:

- **palabras reservadas** — ya están en el catálogo fijo del scanner, no las declara el usuario;
- **tipos de dato** (`TIPO_ENTERO`, `TIPO_TEXTO`, …) — mismo motivo;
- **literales** (`42`, `'hola'`) — son valores, no nombres;
- **tokens `ERROR`** — no son identificadores válidos.

Esta regla vive en el método privado `esInsertable(Token)`, así que hay un único
punto donde se decide.

### 3.5 Ámbitos: fuera del alcance de la Fase 1

Distinguir ámbitos exige saber la estructura de la sentencia, y eso es análisis
sintáctico. En la Fase 1 la tabla es de un solo ámbito global. Los campos
`categoria` y `tipoDato` ya existen y nacen en `DESCONOCIDA` / `DESCONOCIDO`;
se refinan con `actualizarCategoria(...)` y `actualizarTipoDato(...)` cuando la
Fase 2 pueda determinarlos.

En la fase 2 los ambitos ya se manejan en `CatalogoEsquema` (cada columna esta
dentro de su tabla). Esta tabla sigue guardando los nombres que vio el scanner y
la semantica DDL les pone la categoria con `marcarDeclarado(...)`. Ver
[Simbolos y semantica DDL](semantica_ddl.md).

---

## 4. API pública de `TablaSimbolos`

### Inserción
| Método | Devuelve |
|---|---|
| `insertar(Simbolo s)` | `true` si se insertó, `false` si ya existía |
| `insertar(Token t)` | `true` solo si es un `IDENTIFICADOR` nuevo |
| `registrar(Token t)` | el `Simbolo` (nuevo o existente con una aparición más), o `null` si el token no es registrable |
| `registrarTodos(List<Token>)` | cuántos símbolos **nuevos** se crearon |

### Consulta
| Método | Devuelve |
|---|---|
| `buscar(String nombre)` | el `Simbolo`, o `null` |
| `existe(String nombre)` | `true` / `false` (insensible a mayúsculas) |
| `tamano()` | número de símbolos |
| `estaVacia()` | `true` / `false` |
| `obtenerTodos()` | copia de la lista, en orden de aparición; los objetos `Simbolo` son compartidos |

### Modificación
`eliminar(String)`, `actualizarTipoDato(String, String)`,
`actualizarTipoDato(String, TipoToken)`, `actualizarCategoria(String, CategoriaSimbolo)`,
`limpiar()`.

Fase 2: `marcarDeclarado(String, CategoriaSimbolo, TipoToken)` le pone categoria
y tipo a un nombre cuando se declara bien. Si la misma columna esta en varias
tablas con tipos distintos queda `VARIOS`.

### Salida
`toString()` devuelve la tabla formateada; `imprimir()` la envía a consola.

---

## 5. Atributos de `Simbolo`

| Campo | Descripción |
|---|---|
| `id` | correlativo de inserción (1, 2, 3…) |
| `nombre` | lexema original de la primera aparición |
| `clave` | nombre normalizado; es la identidad del símbolo |
| `tipoToken` | siempre `IDENTIFICADOR` en la Fase 1 |
| `categoria` | `TABLA` / `COLUMNA` / `DESCONOCIDA` |
| `tipoDato` | `ENTERO`, `TEXTO`, … o `DESCONOCIDO` |
| `linea`, `columna` | posición de la **primera** aparición |
| `apariciones` | veces que se encontró el identificador |

`equals` y `hashCode` se basan en `clave`: dos símbolos son iguales si nombran lo mismo.

---

## 6. Cómo probar el módulo de forma aislada

La prueba **no depende del scanner**: construye los tokens a mano, como si
vinieran del analizador léxico.

```bash
javac --release 21 -encoding UTF-8 -d out src/lexer/TipoToken.java src/lexer/Token.java src/symbol_table/*.java
```

```bash
java -cp out symbol_table.PruebaTablaSimbolos
```

Cubre: tabla vacía, inserción, rechazo de duplicados, duplicados con distinta
capitalización, búsqueda y existencia, conteo de apariciones, conservación de la
primera posición, filtrado de no-identificadores, recorrido de una lista completa
de tokens, orden de aparición, ids correlativos, actualización de categoría y
tipo de dato, eliminación, limpieza y entradas nulas. El proceso termina con
código de salida `1` si algo falla. `PruebaFase1` verifica además que modificar
la copia de la lista no borra entradas de la tabla.

**Estado actual del módulo aislado: 50 verificaciones, 0 fallidas.**

---

## 7. Cómo se integra (para `Main.java`)

```java
ScannerSQL scanner = new ScannerSQL();
scanner.analizar(contenidoDelArchivo);

TablaSimbolos tabla = new TablaSimbolos();
tabla.registrarTodos(scanner.getTokens());
tabla.imprimir();
```

O token por token, si se prefiere recorrer el flujo una sola vez:

```java
for (Token t : tokens) {
    System.out.println(t);
    tabla.registrar(t);   // ignora por sí solo todo lo que no sea IDENTIFICADOR
}
tabla.imprimir();
```

La integración ya está implementada en `Main`. `ScannerSQL.getTokens()` devuelve
una copia de los tokens y cada llamada a `analizar` reinicia la lista del scanner.
La posición de `Token` se consulta con los métodos existentes `getlinea()` y
`getcolumna()`; `Simbolo` expone `getLinea()` y `getColumna()`.

---

## 8. Ejemplo de salida

Entrada analizada:

```sql
CREAR TABLA usuarios (id ENTERO, correo TEXTO, edad ENTERO);
SELECCIONAR correo DESDE usuarios DONDE edad > 18;
```

La prueba aislada `ejemploDeSalida()` construye estos tokens y asigna las categorías
y tipos manualmente mediante los métodos de actualización, para demostrar la API.
`Main` no deduce esos atributos: su salida mantiene `DESCONOCIDA` / `DESCONOCIDO`.
Salida del ejemplo aislado:

```
+-----+----------------------+---------------+--------------+--------------+-------+-------+-------------+
| ID  | NOMBRE               | TOKEN         | CATEGORIA    | TIPO DATO    | LINEA | COL   | APARICION   |
+-----+----------------------+---------------+--------------+--------------+-------+-------+-------------+
| 1   | usuarios             | IDENTIFICADOR | TABLA        | DESCONOCIDO  | 1     | 13    | 2           |
| 2   | id                   | IDENTIFICADOR | COLUMNA      | ENTERO       | 1     | 23    | 1           |
| 3   | correo               | IDENTIFICADOR | COLUMNA      | TEXTO        | 1     | 34    | 2           |
| 4   | edad                 | IDENTIFICADOR | COLUMNA      | ENTERO       | 1     | 48    | 2           |
+-----+----------------------+---------------+--------------+--------------+-------+-------+-------------+
Total de símbolos: 4
```

Se lee así: `usuarios` es una tabla, apareció por primera vez en la línea 1
columna 13 y se usó 2 veces (en el `CREAR` y en el `SELECCIONAR`). Las palabras
reservadas `CREAR`, `TABLA`, `SELECCIONAR`, `DESDE`, `DONDE`, el literal `18` y
todos los delimitadores fueron correctamente descartados por la tabla.
