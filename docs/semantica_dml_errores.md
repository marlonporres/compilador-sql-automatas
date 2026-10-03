# Semantica DML y tabla comun de errores — Persona 4

## Objetivo

Validar semanticamente `SELECCIONAR`, `INSERTAR`, `ACTUALIZAR` y `BORRAR` sin
modificar datos ni ejecutar consultas. El modulo usa el mismo
`symbol_table.CatalogoEsquema` que construye la semantica DDL.

## Archivos de esta contribucion

- `src/semantic/SemanticaDML.java`: validacion de tablas, columnas, valores,
  asignaciones y condiciones.
- `src/semantic/ErrorCompilacion.java`: diagnostico comun con fase, codigo,
  mensaje, elemento, linea y columna.
- `src/semantic/TablaErrores.java`: coleccion comun de diagnosticos.
- `src/semantic/ReporteCompilacion.java`: presentacion unificada.
- `tests/PruebaSemanticaDML.java`: pruebas de aceptacion de la Persona 4.

`CatalogoEsquema`, `TablaDeclarada` y `ColumnaDeclarada` ya pertenecen al
modulo de simbolos/semantica DDL del proyecto y se consumen, no se duplican.

## Reglas implementadas

| Codigo | Regla |
|---|---|
| `SEM-201` | La tabla referenciada no existe. |
| `SEM-202` | La columna referenciada no existe. |
| `SEM-203` | Cantidad de columnas/valores incorrecta o columna repetida en INSERTAR o ACTUALIZAR. |
| `SEM-204` | El tipo del valor no es compatible con la columna. |
| `SEM-205` | Se usa una comparación ordenada (`>`, `<`, `>=`, `<=`) sobre TEXTO o BOOLEANO. |

### Compatibilidad de literales

El scanner actual produce tres clases de literales DML: entero, decimal y
cadena. Un `ENTERO` es compatible con `ENTERO` y puede asignarse a `DECIMAL`;
un decimal solo es compatible con `DECIMAL`; una cadena es compatible con
`TEXTO`. En INSERTAR y asignaciones se aceptan literales, no referencias a
columnas como valores. Las columnas pueden aparecer como operandos de condiciones.

`FECHA` acepta cadenas ISO `'AAAA-MM-DD'` con una fecha real de calendario.
`BOOLEANO` acepta `VERDADERO` y `FALSO` sin comillas. Se interpretan de forma
contextual, conservando el scanner de la fase 1.

## Condiciones

Se validan ambos operandos, sean columnas o literales. `=`, `!=` y `<>` aceptan
tipos compatibles. Las comparaciones ordenadas permiten números y fechas.
Las condiciones en `DONDE` de SELECCIONAR, ACTUALIZAR y BORRAR respetan
paréntesis y la precedencia `NO`, `Y`, `O`.

## Recuperacion y dependencia de sintaxis

`SemanticaDML` usa el parser común y valida únicamente los AST DML aceptados.
Las sentencias inválidas se descartan por sus límites, nunca por compartir
línea con otra sentencia. Así puede recuperarse después del siguiente `;`.

La sobrecarga `analizar(tokens, erroresSintacticos)` conserva la firma de la
contribución original y verifica la sintaxis con el parser. El recorrido
completo de `Main --fase2` usa `CompiladorSQL`, `AnalizadorSemantico` y la tabla
`errors.TablaErrores`. La API del equipo adapta los diagnósticos a SEM-201–205
y conserva `semantic.TablaErrores`; ambas rutas comparten las reglas de tipos.

## Tabla comun de errores

Todos los diagnosticos de este modulo se guardan como `ErrorCompilacion` con:

- fase (`SEMANTICA`),
- codigo,
- mensaje,
- elemento implicado,
- linea,
- columna.

`TablaErrores` tambien permite incorporar errores lexicos mediante
`agregarLexico(Token)`, usando el formato comun. Los errores de otras fases se
pueden agregar como `ErrorCompilacion` sin duplicar otra estructura.

## Flujo

```text
Tokens
  |
  v
AnalizadorDML (sintaxis)
  |-- error --> se omite la sentencia
  |
  v
Sentencia DML valida
  |
  v
SemanticaDML
  |
  +--> CatalogoEsquema
  |      +--> tabla
  |      +--> columnas
  |      +--> tipos
  |
  +--> ErrorCompilacion
           |
           v
      TablaErrores
           |
           v
   ReporteCompilacion
```

No se ejecutan consultas ni se almacenan filas. El catalogo solo representa el
esquema que ya fue aceptado por la semantica DDL.

## Pruebas

Desde la raiz del proyecto:

```sh
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d out src/lexer/*.java src/parser/*.java src/symbol_table/*.java src/errors/*.java src/semantic/*.java tests/PruebaSemanticaDML.java

java -cp out PruebaSemanticaDML
```

Las 32 comprobaciones cubren tabla inexistente, columna inexistente, cantidad de valores,
tipos incompatibles, condiciones, `ACTUALIZAR`, `BORRAR`, omision de sentencias
sintacticamente invalidas y formato comun del diagnostico.
