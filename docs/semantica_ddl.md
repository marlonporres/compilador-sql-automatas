# Simbolos y semantica DDL (Fase 2)

Modulo de la Persona 3. Esta en los paquetes `symbol_table` (el catalogo) y
`semantic` (el analizador). Usa los tokens del scanner y las sentencias del
parser DDL de la Persona 1.

## 1. Que hace

El parser DDL solo revisa que las sentencias esten bien escritas. Por ejemplo
acepta `CREAR TABLA usuarios (...)` dos veces o `ELIMINAR TABLA pedidos` aunque
pedidos nunca se haya creado. Este modulo revisa que tengan sentido:

- Guarda un catalogo con las tablas que existen, sus columnas, el tipo de cada
  columna y su ambito.
- Revisa cada sentencia DDL en orden contra el catalogo y solo si esta bien
  hace el cambio.
- Deja consultas del catalogo para que la semantica DML (Persona 4) pueda
  revisar tablas, columnas y tipos.
- Le pone categoria y tipo a los nombres de la tabla de simbolos de la fase 1
  cuando quedan bien declarados.

No se guardan filas ni se ejecuta nada, por eso TRUNCAR solo revisa que la tabla
exista y deja el esquema igual.

## 2. Archivos

| Archivo | Para que |
|---|---|
| `src/symbol_table/CatalogoEsquema.java` | El catalogo: crear, eliminar, renombrar y las consultas |
| `src/symbol_table/TablaDeclarada.java` | Una tabla con sus columnas en orden |
| `src/symbol_table/ColumnaDeclarada.java` | Una columna con su tipo y su ambito |
| `src/symbol_table/TablaSimbolos.java` | La de la fase 1, se le agrego `marcarDeclarado` |
| `src/semantic/AnalizadorSemanticoDDL.java` | Las reglas de CREAR, ALTERAR, ELIMINAR, TRUNCAR y RENOMBRAR |
| `src/semantic/ErrorSemantico.java` | El error semantico |
| `tests/PruebaSemanticaDDL.java` | Pruebas (48) |
| `tests/demo_fase2.sql` | Script de demostracion con casos de los 4 |

## 3. Como funciona

### Nombres que vio el scanner vs tablas declaradas

La tabla de simbolos de la fase 1 guarda todos los identificadores que salen en
el archivo, aunque la sentencia este mala. El catalogo solo guarda lo que la
semantica acepto. Por eso en el script de demostracion `pedidos` sale en la
tabla de simbolos pero no en el catalogo (su CREAR tenia una columna repetida),
y en la tabla de simbolos se queda como DESCONOCIDA.

### Ambito

Las tablas estan en el ambito GLOBAL y cada columna esta en el ambito de su
tabla. Asi `usuarios.id` y `productos.id` son dos columnas distintas y cada una
tiene su tipo. Lo de `tabla.columna` es solo para mostrarlo y para los mensajes,
no se agrego el punto al lenguaje.

Igual que en la fase 1, los nombres se comparan en mayusculas (`usuarios` y
`USUARIOS` son la misma tabla).

Las columnas se guardan en el orden en que se declararon porque INSERTAR
necesita ese orden para comparar los valores.

### Sin cambios a medias

Primero se revisa toda la sentencia y se juntan todos los errores. Solo si no
hubo ninguno se cambia el catalogo. Entonces un CREAR TABLA con una columna
repetida no deja la tabla creada a medias.

### En orden

El catalogo empieza vacio y va cambiando con cada sentencia valida. Despues de
`ELIMINAR TABLA temporal` ya no se puede usar temporal.

## 4. Diagramas

### Tabla - columnas

```mermaid
classDiagram
    class CatalogoEsquema {
        -Map tablas
        +crearTabla()
        +eliminarTabla()
        +renombrarTabla()
        +agregarColumna()
        +eliminarColumna()
        +existeTabla()
        +existeColumna()
        +tipoDeColumna()
        +columnasDe()
    }
    class TablaDeclarada {
        -String nombre
        -int linea
        -int columna
        +getAmbito() GLOBAL
        +getColumnas()
    }
    class ColumnaDeclarada {
        -String nombre
        -TipoToken tipoDato
        -String ambito
        -int linea
        -int columna
        +getIdentidad()
    }
    CatalogoEsquema "1" o-- "*" TablaDeclarada
    TablaDeclarada "1" *-- "1..*" ColumnaDeclarada
```

Asi queda el esquema despues de la parte de la Persona 3 en el script de
demostracion. Las dos `id` son columnas distintas con tipo distinto:

```mermaid
erDiagram
    usuarios {
        ENTERO id
        TEXTO nombre
        FECHA fecha_nacimiento
        DECIMAL salario
        BOOLEANO activo
    }
    productos {
        TEXTO id
        TEXTO descripcion
        DECIMAL precio
    }
```

### Recorrido

```mermaid
flowchart TD
    A[Archivo SQL] --> B[ScannerSQL]
    B --> C[Tokens]
    C --> D[TablaSimbolos]
    C --> E[Parser DDL]
    E -->|mal escrita| X[Error sintactico]
    E -->|bien escrita| F[AnalizadorSemanticoDDL]
    F --> G{Tiene errores?}
    G -->|si| H[Error semantico, el catalogo no cambia]
    G -->|no| I[Se cambia el CatalogoEsquema]
    I --> J[Se marca en TablaSimbolos]
    I --> K[La semantica DML consulta el catalogo]
```

## 5. Reglas

| Sentencia | Que se revisa | Que cambia |
|---|---|---|
| CREAR TABLA | que la tabla no exista (SEM-101) y que no se repitan columnas (SEM-102) | se crea la tabla con sus columnas |
| ELIMINAR TABLA | que exista (SEM-103) | se quita la tabla con sus columnas |
| TRUNCAR TABLA | que exista (SEM-103) | nada |
| ALTERAR TABLA | que exista (SEM-103) | nada por ahora (ver seccion 8) |
| RENOMBRAR TABLA | que exista (SEM-103) | nada por ahora (ver seccion 8) |

Para cuando el parser traiga la forma completa ya estan hechos y probados:

- `renombrarTabla`: la tabla debe existir (SEM-103) y el nombre nuevo no (SEM-101).
  Las columnas se quedan igual pero cambian de ambito.
- `agregarColumna`: la tabla debe existir (SEM-103) y no tener ya esa columna (SEM-102).
- `eliminarColumna`: la tabla debe existir (SEM-103), tener la columna (SEM-104)
  y no quedarse sin columnas (SEM-105).

## 6. Errores

Cada error tiene fase (SEMANTICA), codigo, mensaje, elemento, linea y columna,
igual que `ErrorSintactico`. Si el error es de la tabla se usa la posicion de la
sentencia y si es de una columna la posicion de esa columna.

| Codigo | Cuando sale |
|---|---|
| SEM-101 | ya existe una tabla con ese nombre |
| SEM-102 | columna repetida |
| SEM-103 | la tabla no existe |
| SEM-104 | la columna no existe en esa tabla |
| SEM-105 | la tabla se quedaria sin columnas |

Ejemplo:

```text
[SEMANTICA][SEM-102] La columna se declara mas de una vez (elemento: "pedidos.id", L:44 C:5)
```

Use SEM-1xx para DDL, la Persona 4 puede usar SEM-2xx para DML. Si la Persona 4
cambia el formato comun de errores se ajusta `ErrorSemantico`.

## 7. Como se usa

### En Main

```java
TablaSimbolos tabla = new TablaSimbolos();
tabla.registrarTodos(tokens);

CatalogoEsquema catalogo = new CatalogoEsquema();   //el mismo para DDL y DML
AnalizadorSemanticoDDL semanticaDDL = new AnalizadorSemanticoDDL(catalogo, tabla);

//por cada sentencia que paso la sintaxis, en el orden del archivo:
List<ErrorSemantico> errores = semanticaDDL.analizarSentencia(sentencia);
//las DML van a la semantica de la Persona 4 con el mismo catalogo

catalogo.imprimir();
tabla.imprimir();
```

Si solo hay DDL tambien se puede usar `analizar(lista)`. `getErrores()` devuelve
todos los errores que van saliendo.

### Consultas para la Persona 4

Todas ignoran mayusculas y no truenan si les llega null o un nombre que no existe.

- `existeTabla(tabla)`
- `existeColumna(tabla, columna)`: busca la columna dentro de esa tabla
- `buscarColumna(tabla, columna)`: la columna o null
- `tipoDeColumna(tabla, columna)`: `TIPO_ENTERO`, `TIPO_TEXTO`, etc. o null
- `columnasDe(tabla)`: las columnas en orden, vacia si la tabla no existe

Ejemplo para un INSERTAR:

```java
List<ColumnaDeclarada> columnas = catalogo.columnasDe("usuarios");
if (!catalogo.existeTabla("usuarios")) {
    //tabla no existe
} else if (valores.size() != columnas.size()) {
    //cantidad de valores incorrecta
} else {
    for (int i = 0; i < columnas.size(); i++) {
        TipoToken esperado = columnas.get(i).getTipoDato();
        //comparar con el valor i
    }
}
```

### Tabla de simbolos

`marcarDeclarado(nombre, categoria, tipo)` lo llama el analizador cuando algo se
declara bien. El nombre pasa de DESCONOCIDA a TABLA, o a COLUMNA con su tipo. Si
la misma columna esta en varias tablas con tipos distintos queda VARIOS (el
detalle de cada tabla esta en el catalogo). Lo que nunca se declaro bien sigue
DESCONOCIDA.

## 8. Pendiente con la Persona 1: ALTERAR y RENOMBRAR

Ahorita el parser acepta `ALTERAR TABLA t` y `RENOMBRAR TABLA t` sin decir que se
cambia ni el nombre nuevo, entonces solo se puede revisar que la tabla exista.
Propuesta:

```text
renombrar ::= RENOMBRAR TABLA identificador EN identificador
alterar   ::= ALTERAR TABLA identificador AGREGAR identificador tipo_dato
            | ALTERAR TABLA identificador ELIMINAR identificador
```

EN y ELIMINAR ya existen, solo habria que agregar AGREGAR al scanner. Tambien
`SentenciaDDL` tendria que traer el nombre nuevo o la columna. Con eso solo hay
que llamar a `renombrarTabla`, `agregarColumna` o `eliminarColumna`, que ya estan.
Si no da tiempo se queda la forma simple.

## 9. Pruebas

```sh
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d out src/lexer/*.java src/parser/*.java src/symbol_table/*.java src/semantic/*.java tests/PruebaSemanticaDDL.java
java -cp out PruebaSemanticaDDL
```

48 pruebas, 0 fallidas. Prueban crear tablas, la misma columna en dos tablas,
tablas y columnas repetidas, que no queden cambios a medias, ELIMINAR, TRUNCAR,
ALTERAR y RENOMBRAR, renombrar/agregar/eliminar columna, las consultas para DML,
el formato de los errores, un script pasado por el scanner y el parser de verdad
y la clasificacion de la tabla de simbolos. Al final imprime un ejemplo del
catalogo. Si algo falla termina con codigo 1.

## 10. Script de demostracion

`tests/demo_fase2.sql` tiene una parte por persona y en cada caso dice lo que
deberia salir. La parte de semantica DDL da:

```text
[SEMANTICA][SEM-101] Ya existe una tabla con ese nombre (elemento: "usuarios", L:38 C:1)
[SEMANTICA][SEM-102] La columna se declara mas de una vez (elemento: "pedidos.id", L:44 C:5)
[SEMANTICA][SEM-103] No existe una tabla con ese nombre (elemento: "pedidos", L:51 C:1)
[SEMANTICA][SEM-103] No existe una tabla con ese nombre (elemento: "temporal", L:62 C:1)
```

y el catalogo queda con usuarios (5 columnas) y productos (3 columnas).

Para que el script completo corra por Main falta que el parser mande cada
sentencia al parser DDL o al DML segun la primera palabra. Ahorita cada uno
marca error en las sentencias del otro. Eso lo ven la Persona 1 y la Persona 2.
