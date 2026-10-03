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
| `SEM-203` | Cantidad de columnas/valores incorrecta o columna repetida en INSERTAR. |
| `SEM-204` | El tipo del valor no es compatible con la columna. |
| `SEM-205` | Se usa una comparacion ordenada (`>`, `<`, `>=`, `<=`) sobre un tipo no numerico. |

### Compatibilidad de literales

El scanner actual produce tres clases de literales DML: entero, decimal y
cadena. Un `ENTERO` es compatible con `ENTERO` y puede asignarse a `DECIMAL`;
un decimal solo es compatible con `DECIMAL`; una cadena es compatible con
`TEXTO`. Un identificador usado como valor se resuelve como una columna de la
misma tabla y se compara su tipo.

`FECHA` y `BOOLEANO` existen como tipos declarativos del lenguaje, pero el
scanner actual no define literales dedicados para esos tipos. Por ello este
modulo no inventa tokens nuevos ni cambia el scanner: una fecha o booleano debe
llegar con la representación que acuerde el equipo en el contrato DML.

## Condiciones

Se valida que la columna de la izquierda exista y que el valor de la derecha
sea compatible. `=` y `!=` aceptan tipos compatibles. Las comparaciones
ordenadas requieren un tipo numerico. Las condiciones se revisan en
`DONDE` de `SELECCIONAR`, `ACTUALIZAR` y `BORRAR`, respetando que la sintaxis ya
fue validada por `lexer.AnalizadorDML`.

## Recuperacion y dependencia de sintaxis

`SemanticaDML` llama al `AnalizadorDML` existente cuando recibe solo tokens.
Si una sentencia contiene un error sintactico, se identifica su linea y se
omite esa sentencia del analisis semantico. Asi una estructura mal formada no
se trata como una sentencia semanticamente valida.

Cuando `Main` ya tenga la lista de errores sintacticos, puede usar la sobrecarga
`analizar(tokens, erroresSintacticos)` para reutilizar el resultado del parser.

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
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d out \
  src/lexer/*.java src/parser/*.java src/symbol_table/*.java \
  src/semantic/*.java tests/PruebaSemanticaDML.java

java -cp out PruebaSemanticaDML
```

La prueba cubre tabla inexistente, columna inexistente, cantidad de valores,
tipos incompatibles, condiciones, `ACTUALIZAR`, `BORRAR`, omision de sentencias
sintacticamente invalidas y formato comun del diagnostico.
