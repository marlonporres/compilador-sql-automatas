# compilador-sql-automatas
Proyecto Final - Autómatas y Lenguajes Formales (Java)

## Fase 1

Analizador léxico manual de SQL en español y tabla de símbolos global.
El flujo es archivo UTF-8 → `ScannerSQL.analizar` → `List<Token>` →
`TablaSimbolos.registrarTodos`. `Main` imprime tokens, errores y símbolos.
Esta fase no valida la sintaxis ni ejecuta SQL; las categorías y tipos de los
identificadores permanecen en `DESCONOCIDA` y `DESCONOCIDO`.

## Fase 2

Análisis semántico de identificación y clasificación de símbolos.
En esta etapa se determina si cada identificador representa una tabla, una
columna o permanece desconocido según el contexto de la sentencia SQL. La tabla
de símbolos ya está preparada para esta evolución mediante los valores
`TABLA`, `COLUMNA` y `DESCONOCIDA` de `CategoriaSimbolo`, y se espera completar
la validación del alcance, consistencia de referencias y tipos de dato en las
consultas y definiciones.

La intención de esta fase es resolver preguntas como:

- ¿Qué nombres corresponden a tablas y cuáles a columnas?
- ¿Una columna se usa antes de ser declarada o fuera de su contexto?
- ¿Los tipos de dato asignados a una columna son consistentes con la operación?

Esta etapa no ejecuta sentencias SQL ni modifica datos; su objetivo es validar
la semántica del programa fuente y preparar la base para fases posteriores del
compilador.

## Analizador sintactico DML

`lexer.AnalizadorDML` valida las sentencias `SELECCIONAR`, `INSERTAR`,
`ACTUALIZAR` y `BORRAR` a partir de los tokens de `ScannerSQL`. Incluye
condiciones con operadores relacionales y logicos; devuelve errores sintacticos
con linea y columna. Valida la estructura, pero no ejecuta consultas ni modifica
datos.

## Compilación y ejecución

Requiere **JDK 21**. Desde la raíz del repositorio:

```sh
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d out src/Main.java src/lexer/*.java src/symbol_table/*.java tests/PruebaFase1.java tests/PruebaAnalizadorDML.java
java -cp out Main tests/test_ddl.sql
java -cp out Main tests/test_dml.sql
```

Los dos archivos SQL contienen una sección válida y otra con **4 errores léxicos
intencionales**. Por ello, ejecutarlos completos debe reportar 4 errores y salir
con código `1`. La cadena sin cerrar consume hasta el final del archivo.

Códigos de salida de `Main`: `0` sin errores léxicos, `1` con errores léxicos,
`2` por argumentos incorrectos, problemas de lectura o fallo de ejecución.

## Pruebas

Después de compilar:

```sh
java -cp out symbol_table.PruebaTablaSimbolos
java -cp out PruebaFase1
java -cp out PruebaAnalizadorDML
```

- Tabla de símbolos: 50 comprobaciones aisladas.
- Fase 1: 79 comprobaciones de reservadas, identificadores, literales, operadores,
  errores, comentarios, posiciones, reutilización del scanner e integración.
  Ejecuta `Main` en procesos separados con DDL/DML válidos, archivos con errores,
  archivos vacíos, argumentos incorrectos y archivos ilegibles. Usa archivos temporales que elimina
  al terminar; no requiere dependencias ni generadores.
- Fase 2: la clasificación semántica de identificadores aún está en preparación.
  La tabla de símbolos ya admite categorías como `TABLA` y `COLUMNA`, y la siguiente
  etapa consiste en completar la validación del contexto, alcance y consistencia
  de referencias para distinguir correctamente cada símbolo dentro de las sentencias SQL.

Ambas pruebas deben terminar con código `0` y `Fallidas: 0`; un fallo produce
código `1`. `PruebaFase1` debe ejecutarse desde la raíz para encontrar `tests/`.

## Documentación

- [Especificación léxica](docs/especificacion_lexica.md)
- [Tabla de símbolos](docs/tabla_simbolos.md)
