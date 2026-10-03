# compilador-sql-automatas
Proyecto Final - Autómatas y Lenguajes Formales (Java)

## Demostración de fase 2

En Windows, haga doble clic en **DEMOSTRAR.cmd**. Compila el proyecto y abre
un menú para ejecutar las pruebas, mostrar un script válido, mostrar errores
y recuperación, o ver los tokens. Requiere un **JDK 21 o posterior**.

Desde PowerShell en la raíz, también puede ejecutar:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\demo.ps1 -Modo todo
```

El recorrido completo es archivo UTF-8 → scanner → parser DDL/DML →
semántica → catálogo de tablas/columnas y tabla de errores. La gramática
cubre los nueve comandos en español, cinco tipos y condiciones con Y/O/NO.
Las consultas se validan; su ejecución e interfaz corresponden a fase 3.

Use [el instructivo de demostración](docs/instructivo_demostracion.md) y
[la documentación técnica con diagramas](docs/fase2.md).

## Fase 1

Analizador léxico manual de SQL en español y tabla de símbolos global.
El flujo es archivo UTF-8 → `ScannerSQL.analizar` → `List<Token>` →
`TablaSimbolos.registrarTodos`. `Main` imprime tokens, errores y símbolos.
Esta fase no valida la sintaxis ni ejecuta SQL; las categorías y tipos de los
identificadores permanecen en `DESCONOCIDA` y `DESCONOCIDO`.

## Compilación y ejecución

Requiere **JDK 21 o posterior** y compila para Java 21. Desde la raíz:

```sh
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d out src/Main.java src/lexer/*.java src/parser/*.java src/symbol_table/*.java src/errors/*.java src/semantic/*.java src/compiler/*.java tests/*.java
java -cp out PruebaParserDDL
java -cp out Main tests/demo_fase2_valido.sql --fase2 --resumen
java -cp out Main tests/demo_fase2_errores.sql --fase2 --resumen
```

Los archivos históricos `tests/test_ddl.sql` y `tests/test_dml.sql` contienen
una sección válida y otra con **4 errores léxicos
intencionales**. Por ello, ejecutarlos completos debe reportar 4 errores y salir
con código `1`. La cadena sin cerrar consume hasta el final del archivo.

Códigos de salida de `Main`: `0` sin errores, `1` con errores del lenguaje,
`2` por argumentos incorrectos, problemas de lectura o fallo de ejecución.

`Main archivo.sql` conserva el modo léxico de fase 1. Agregue `--fase2`
para el análisis completo; `--resumen` omite la impresión de tokens y símbolos
léxicos. El ejemplo válido tiene 15 sentencias válidas y el catálogo final
clientes; el negativo tiene 1 error léxico, 1 sintáctico y 6 semánticos.

## Pruebas

Las ocho suites reúnen **390 comprobaciones**. En Windows, la opción 1 de
DEMOSTRAR.cmd las ejecuta todas. GitHub también las verifica con Java 21 en
cada publicación a `main` y en los pull requests dirigidos a esa rama.

Después de compilar:

```sh
java -cp out symbol_table.PruebaTablaSimbolos
java -cp out PruebaFase1
java -cp out PruebaParserDDL
java -cp out PruebaFase2
java -cp out PruebaAnalizadorDML
java -cp out PruebaSemanticaDDL
java -cp out PruebaSemanticaDML
java -cp out PruebaDelimitadores
```

- Tabla de símbolos: 50 comprobaciones aisladas.
- Fase 1: 79 comprobaciones de reservadas, identificadores, literales, operadores,
  errores, comentarios, posiciones, reutilización del scanner e integración.
  Ejecuta `Main` en procesos separados con DDL/DML válidos, archivos con errores,
  archivos vacíos, argumentos incorrectos y archivos ilegibles. Usa archivos temporales que elimina
  al terminar; no requiere dependencias ni generadores.

- Parser DDL: 10 comprobaciones aisladas.
- Fase 2: 80 comprobaciones de gramática, precedencia, ámbitos, tipos,
  atomicidad, recuperación, demos y códigos de salida reales.
- Las suites DML y semánticas del equipo comprueban sus APIs compatibles
  con el recorrido completo; se conservan los aportes de las cuatro personas.
- Delimitadores: 54 comprobaciones de `;`, comas y paréntesis ausentes o
  incorrectos, recuperación y delimitadores dentro de cadenas y comentarios.

Los mensajes identifican la falta de coma en listas de columnas, valores y
asignaciones. Las sentencias inválidas no pasan al análisis semántico.
La recuperación continúa después del siguiente `;` o llega al fin del archivo.

Todas deben terminar con código `0` y `Fallidas: 0`; un fallo produce
código `1`. Ejecute desde la raíz para encontrar `tests/`.

## Documentación

- [Especificación léxica](docs/especificacion_lexica.md)
- [Tabla de símbolos](docs/tabla_simbolos.md)
- [Analizador sintáctico DDL](docs/parser_ddl.md)
- [Fase 2 y diagramas](docs/fase2.md)
- [Símbolos y semántica DDL del equipo](docs/semantica_ddl.md)
- [Semántica DML y errores del equipo](docs/semantica_dml_errores.md)
- [Instructivo de demostración](docs/instructivo_demostracion.md)
