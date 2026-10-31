# Conexion y ejecucion DDL (Fase 3)

Modulo de la Persona 3. Esta en el paquete `execution`. Usa las sentencias que
deja el `CompiladorSQL` de las fases 1 y 2 y las ejecuta de verdad en una base H2.

## 1. Que hace

- Abre la conexion a la base que usamos todos en la fase 3 (`ConexionSQL`).
- Ejecuta los cinco comandos DDL: CREAR, ALTERAR, ELIMINAR, TRUNCAR y RENOMBRAR
  (`EjecutorDDL`).
- Devuelve cada resultado como un `ResultadoEjecucion` con el SQL generado, la
  linea y columna de la sentencia y el error si H2 la rechazo. La ventana recibe
  datos, no texto impreso.

No revisa sintaxis ni semantica, eso ya lo hizo el compilador. Solo se le deben
pasar sentencias de un analisis sin errores.

## 2. Archivos

| Archivo | Para que |
|---|---|
| `src/execution/ConexionSQL.java` | Abre la base H2 en memoria y fija la version del controlador |
| `src/execution/EjecutorDDL.java` | Ejecuta una `SentenciaDDL` sobre la conexion que le pasan |
| `tests/PruebaEjecutorDDL.java` | Pruebas (40) |

`TraductorSQL` y `ResultadoEjecucion` ya estaban en main, los uso tal cual.

## 3. Como funciona

### La conexion

El motor es H2 embebido: va dentro del mismo programa por JDBC, no hay que
instalar ni levantar ningun servidor. El controlador es `lib/h2-2.4.240.jar` y
`ConexionSQL.VERSION_H2` guarda esa version, la prueba revisa que el jar que
carga java sea ese.

Cada `ConexionSQL.abrir()` crea una base en memoria nueva con nombre propio, asi
cada script empieza sin tablas y dos ejecuciones no se mezclan. Al cerrar la
conexion la base se borra sola. Si falta el jar en el classpath da un mensaje
claro en vez del "No suitable driver" de java.

La conexion se abre una vez por script y se le pasa al EjecutorDDL y al
EjecutorDML de la Persona 4, asi un CREAR deja la tabla lista para el INSERTAR
que sigue. Los ejecutores no la abren ni la cierran.

### De espanol a SQL

`EjecutorDDL` le pide el SQL a `TraductorSQL`, el mismo que usa el DML, para que
los nombres y los tipos se conviertan igual en todo el script:

| Nuestro | SQL |
|---|---|
| CREAR TABLA | CREATE TABLE |
| ALTERAR TABLA ... AGREGAR / MODIFICAR / QUITAR | ALTER TABLE ... ADD / ALTER / DROP COLUMN |
| ELIMINAR TABLA | DROP TABLE |
| TRUNCAR TABLA | TRUNCATE TABLE |
| RENOMBRAR TABLA ... A | ALTER TABLE ... RENAME TO |

Los tipos quedan asi: ENTERO = NUMERIC(1000,0), DECIMAL = DECFLOAT(1000),
TEXTO = CHARACTER VARYING, FECHA = DATE, BOOLEANO = BOOLEAN. Los nombres van en
minusculas y entre comillas.

Los DDL no llevan valores, por eso se ejecutan con un `Statement` normal.

### Errores

Puede pasar que el compilador acepte una sentencia y la base no. Por ejemplo un
CREAR de una tabla que ya existe en la base, porque cada analisis empieza con el
catalogo vacio. En ese caso no se lanza la excepcion: se devuelve un
`ResultadoEjecucion` con `EXEC-DDL-001: <mensaje de H2>`, la linea y columna de
la sentencia y el SQL que fallo. Lo mismo si la conexion se cayo.

## 4. Como se usa

```java
try (Connection conexion = ConexionSQL.abrir()) {
    int numero = 0;
    for (SentenciaSQL s : resultado.sentencias()) {
        numero++;
        ResultadoEjecucion r;
        if (s instanceof SentenciaDDL ddl) {
            r = new EjecutorDDL().ejecutar(conexion, numero, ddl);
        } else {
            r = new EjecutorDML().ejecutar(conexion, numero, (SentenciaDML) s);
        }
        resultados.add(r);
        if (!r.esExitoso()) break;   //se para en la primera que falla
    }
}
```

Hoy `ServicioEjecucion` (el que usa la ventana) todavia ejecuta todo con su
propio metodo. La prueba revisa que mi ejecutor arma el mismo SQL DDL que el
servicio para `demo_fase3.sql`, asi que se puede cambiar el servicio para que
use `ConexionSQL`, `EjecutorDDL` y `EjecutorDML` sin que cambie lo que se ve.
Ese cambio lo acordamos en el equipo antes de hacerlo.

## 5. Pruebas

Desde la raiz del proyecto (en Linux/Mac el `;` del classpath va con `:`):

```
javac --release 21 -encoding UTF-8 -cp "lib/*" -d out @fuentes.txt
java -cp "out;lib/*" PruebaEjecutorDDL
```

`fuentes.txt` es la lista de todos los `.java` de `src` y `tests`.

Que se prueba:
- la conexion abre, la base empieza vacia, cada `abrir()` es otra base y el
  controlador es la version 2.4.240
- cada uno de los cinco DDL, revisando en los metadatos de H2 que la tabla,
  las columnas o los tipos de verdad cambiaron
- RENOMBRAR conserva las filas y TRUNCAR las borra pero deja la tabla
- un error de H2 vuelve con su codigo, linea, columna y SQL, sin caerse
- `tests/demo_fase3.sql` corre completo junto con el DML de la Persona 4

## 6. Explicacion para la exposicion

Mi parte es la que ejecuta de verdad las sentencias de definicion. Cuando el
compilador dice que el script no tiene errores, se abre una sola conexion a una
base H2 nueva y cada sentencia DDL pasa por el EjecutorDDL, que la traduce a SQL
y la ejecuta. Con la misma conexion la Persona 4 corre el DML, por eso las
tablas que creo ya estan cuando se insertan los datos. Si la base rechaza algo
se devuelve el error con la linea y columna para que la ventana lo muestre y se
detenga ahi.
