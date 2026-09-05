# compilador-sql-automatas
Proyecto Final - Autómatas y Lenguajes Formales (Java)

## Fase 1

Analizador léxico manual de SQL en español y tabla de símbolos global.
El flujo es archivo UTF-8 → `ScannerSQL.analizar` → `List<Token>` →
`TablaSimbolos.registrarTodos`. `Main` imprime tokens, errores y símbolos.
Esta fase no valida la sintaxis ni ejecuta SQL; las categorías y tipos de los
identificadores permanecen en `DESCONOCIDA` y `DESCONOCIDO`.

## Compilación y ejecución

Requiere **JDK 21**. Desde la raíz del repositorio:

```sh
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d out src/Main.java src/lexer/*.java src/symbol_table/*.java tests/PruebaFase1.java
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
```

- Tabla de símbolos: 50 comprobaciones aisladas.
- Fase 1: 79 comprobaciones de reservadas, identificadores, literales, operadores,
  errores, comentarios, posiciones, reutilización del scanner e integración.
  Ejecuta `Main` en procesos separados con DDL/DML válidos, archivos con errores,
  archivos vacíos, argumentos incorrectos y archivos ilegibles. Usa archivos temporales que elimina
  al terminar; no requiere dependencias ni generadores.

Ambas pruebas deben terminar con código `0` y `Fallidas: 0`; un fallo produce
código `1`. `PruebaFase1` debe ejecutarse desde la raíz para encontrar `tests/`.

## Documentación

- [Especificación léxica](docs/especificacion_lexica.md)
- [Tabla de símbolos](docs/tabla_simbolos.md)
