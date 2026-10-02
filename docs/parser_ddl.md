# Analizador sintáctico DDL — Fase 2

Este módulo toma la lista de tokens producida por `ScannerSQL` y construye
sentencias DDL inmutables. La validación de nombres existentes, duplicados,
ámbitos y efectos sobre el esquema corresponde al análisis semántico.

## Gramática implementada

```text
sentencia       ::= crear | alterar | eliminar | truncar | renombrar
crear           ::= CREAR TABLA identificador '(' columna (',' columna)* ')'
alterar         ::= ALTERAR TABLA identificador
eliminar        ::= ELIMINAR TABLA identificador
truncar         ::= TRUNCAR TABLA identificador
renombrar       ::= RENOMBRAR TABLA identificador
columna         ::= identificador tipo_dato
tipo_dato       ::= ENTERO | TEXTO | FECHA | DECIMAL | BOOLEANO
programa        ::= (sentencia ';')* EOF
```

En esta iteración `ALTERAR` y `RENOMBRAR` reconocen la forma mínima que ya
existe en el catálogo léxico. La forma que modifique columnas o especifique un
nuevo nombre debe acordarse con el módulo semántico antes de extender los
tokens y la gramática.

## API

```java
ScannerSQL scanner = new ScannerSQL();
scanner.analizar(entrada);
ResultadoSintacticoDDL resultado = new AnalizadorSintacticoDDL()
        .analizar(scanner.getTokens());

resultado.getSentencias(); // solo sentencias sintácticamente válidas
resultado.getErrores();    // fase, código, mensaje, lexema, línea y columna
```

`CursorTokens` protege la lista de entrada, permite mirar sin consumir y
recupera hasta `;` o `EOF`. Cuando una sentencia falla, el analizador la
descarta y puede continuar con la siguiente para reportar más de un error.

## Pruebas

Desde la raíz del proyecto:

```sh
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d out \
  src/lexer/*.java src/parser/*.java tests/PruebaParserDDL.java
java -cp out PruebaParserDDL
```

La prueba cubre el cursor, los cinco comandos DDL, columnas y tipos de datos,
errores estructurales, posiciones y recuperación después de una sentencia
inválida.
