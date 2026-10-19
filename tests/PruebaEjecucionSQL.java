import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import execution.InformeEjecucion;
import execution.ResultadoComparacion;
import execution.ResultadoEjecucion;
import execution.ServicioEjecucion;

/** Pruebas reales sobre H2; verifican efectos, aislamiento y comparación. */
public final class PruebaEjecucionSQL {
    private static int total;
    private static int fallidas;
    private static final ServicioEjecucion SERVICIO = new ServicioEjecucion();
    private PruebaEjecucionSQL() { }

    public static void main(String[] args) throws Exception {
        flujoCompleto();
        tiposYLiterales();
        archivoConBOM();
        aislamientoYBloqueo();
        comparar();
        compararIdentificadores();
        erroresReferencia();
        inmutabilidad();
        System.out.println("Pruebas ejecución SQL: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) System.exit(1);
    }

    private static void probar(String nombre, boolean condicion) {
        total++;
        if (!condicion) { fallidas++; System.out.println("FALLA: " + nombre); }
    }

    private static String leer(String nombre) throws Exception {
        return Files.readString(Path.of("tests", nombre), StandardCharsets.UTF_8);
    }

    private static ResultadoEjecucion ultimaConsulta(InformeEjecucion informe) {
        List<ResultadoEjecucion> consultas = informe.resultados().stream()
                .filter(ResultadoEjecucion::esConsulta).toList();
        if (consultas.isEmpty()) return new ResultadoEjecucion(0, "", 0, 0, "", List.of(), List.of(), 0, "sin consulta");
        return consultas.getLast();
    }

    private static void flujoCompleto() throws Exception {
        InformeEjecucion informe = SERVICIO.ejecutar(leer("demo_fase3.sql"));
        probar("demo fase 3 válida", informe.esExitoso());
        if (!informe.esExitoso()) System.out.println(informe);
        probar("la demo ejecuta las diecinueve sentencias", informe.resultados().size() == 19);
        probar("el truncado deja vacía la consulta final", ultimaConsulta(informe).filas().isEmpty());
        probar("se devuelve SQL traducido", informe.resultados().stream().allMatch(r -> !r.sql().isBlank()));
        probar("sentencias tienen posición", informe.resultados().stream().allMatch(r -> r.linea() > 0 && r.columna() > 0));
        ResultadoComparacion comparacion = SERVICIO.comparar(leer("demo_fase3.sql"), leer("demo_fase3_referencia.sql"));
        probar("demo coincide con SQL de referencia", comparacion.coincide());
        if (!comparacion.coincide()) System.out.println(comparacion.detalles() + " " + comparacion.referencia());
    }

    private static void tiposYLiterales() {
        String fuente = "CREAR TABLA Datos (id ENTERO, contenido TEXTO, valor DECIMAL, nacimiento FECHA, activo BOOLEANO);"
                + "INSERTAR EN datos VALORES (123456789012345678901234567890, 'SELECCIONAR; BORRAR -- /* */',"
                + "0.123456789012345678901234567890, '2024-02-29', VERDADERO);"
                + "INSERTAR EN datos (id, contenido) VALORES (2, 'O''Brien');"
                + "SELECCIONAR * DESDE DATOS;";
        InformeEjecucion informe = SERVICIO.ejecutar(fuente);
        probar("cinco tipos y nombres sin distinción de mayúsculas", informe.esExitoso());
        if (!informe.esExitoso()) System.out.println(informe);
        List<List<Object>> filas = ultimaConsulta(informe).filas();
        probar("dos inserciones reales", filas.size() == 2);
        if (filas.size() == 2) {
            probar("enteros grandes sin pérdida", filas.getFirst().getFirst().toString().equals("123456789012345678901234567890"));
            probar("texto SQL se conserva literal", filas.getFirst().get(1).equals("SELECCIONAR; BORRAR -- /* */"));
            probar("decimal exacto", new BigDecimal(filas.getFirst().get(2).toString()).compareTo(new BigDecimal("0.123456789012345678901234567890")) == 0);
            probar("fecha real", filas.getFirst().get(3).toString().equals("2024-02-29"));
            probar("booleano real", filas.getFirst().get(4).equals(Boolean.TRUE));
            probar("apóstrofe escapado", filas.get(1).get(1).equals("O'Brien"));
            probar("columnas omitidas producen SQL NULL", filas.get(1).get(2) == null && filas.get(1).get(3) == null && filas.get(1).get(4) == null);
        }
        InformeEjecucion precedencia = SERVICIO.ejecutar("CREAR TABLA t (id ENTERO, activo BOOLEANO);"
                + "INSERTAR EN t VALORES (1, VERDADERO);INSERTAR EN t VALORES (2, FALSO);"
                + "INSERTAR EN t VALORES (3, FALSO);"
                + "SELECCIONAR id DESDE t DONDE 1 = id O id = 2 Y NO (activo = VERDADERO);"
                + "SELECCIONAR id DESDE t DONDE (1 = id O id = 2) Y activo = VERDADERO;");
        probar("precedencia y literales a izquierda ejecutan", precedencia.esExitoso());
        List<ResultadoEjecucion> consultas = precedencia.resultados().stream().filter(ResultadoEjecucion::esConsulta).toList();
        probar("Y tiene prioridad sobre O", consultas.size() == 2 && consultas.getFirst().filas().size() == 2);
        probar("paréntesis cambian resultado", consultas.size() == 2 && consultas.getLast().filas().size() == 1);
        InformeEjecucion fechas = SERVICIO.ejecutar("CREAR TABLA t (f FECHA);INSERTAR EN t VALORES ('2024-02-29');"
                + "SELECCIONAR * DESDE t DONDE '2024-01-01' < f;");
        probar("comparación fecha con literal en izquierda", fechas.esExitoso() && ultimaConsulta(fechas).filas().size() == 1);
        InformeEjecucion inyeccion = SERVICIO.ejecutar("CREAR TABLA t (v TEXTO);"
                + "INSERTAR EN t VALORES ('x''); DROP TABLE t; --');SELECCIONAR * DESDE t;");
        probar("SQL incrustado con apóstrofe permanece como dato", inyeccion.esExitoso()
                && ultimaConsulta(inyeccion).filas().size() == 1
                && ultimaConsulta(inyeccion).filas().getFirst().getFirst().equals("x'); DROP TABLE t; --"));
        probar("SQL generado mantiene parámetros separados", inyeccion.resultados().size() == 3
                && inyeccion.resultados().get(1).sql().contains("?")
                && !inyeccion.resultados().get(1).sql().contains("DROP TABLE"));
        InformeEjecucion operadores = SERVICIO.ejecutar("CREAR TABLA t (id ENTERO, activo BOOLEANO);"
                + "INSERTAR EN t VALORES (2, VERDADERO);"
                + "SELECCIONAR * DESDE t DONDE id >= 2 Y id <= 2 Y id != 3 Y id <> 4 Y VERDADERO = activo;");
        probar("comparadores completos y booleano en izquierda", operadores.esExitoso() && ultimaConsulta(operadores).filas().size() == 1);
    }

    private static void aislamientoYBloqueo() {
        String valido = "CREAR TABLA t (id ENTERO);INSERTAR EN t VALORES (1);SELECCIONAR * DESDE t;";
        InformeEjecucion a = SERVICIO.ejecutar(valido);
        InformeEjecucion b = SERVICIO.ejecutar(valido);
        probar("cada ejecución comienza vacía", a.esExitoso() && b.esExitoso() && ultimaConsulta(b).filas().size() == 1);
        for (String invalido : List.of("CREAR TABLA t (id ENTERO);INSERTAR EN t VALORES ('texto');",
                "CREAR TABLA t (id ENTERO);INSERTAR EN t VALORES (1)",
                "CREAR TABLA t (id ENTERO);@;", "SELECCIONAR * DESDE inexistente;")) {
            InformeEjecucion informe = SERVICIO.ejecutar(invalido);
            probar("bloqueo de script con error: " + invalido, !informe.esExitoso() && informe.resultados().isEmpty());
        }
        probar("vacío no exitoso", !SERVICIO.ejecutar("").esExitoso());
        probar("null no exitoso", !SERVICIO.ejecutar(null).esExitoso());
        InformeEjecucion fallo = SERVICIO.ejecutar("CREAR TABLA t (id TEXTO);INSERTAR EN t VALORES ('abc');"
                + "ALTERAR TABLA t MODIFICAR id ENTERO;SELECCIONAR * DESDE t;");
        probar("error de datos en ALTERAR se informa", !fallo.esExitoso() && fallo.resultados().size() == 3);
        probar("error conserva posición de sentencia", fallo.resultados().size() == 3 && fallo.resultados().getLast().linea() == 1
                && fallo.resultados().getLast().columna() > 1 && fallo.resultados().getLast().error().contains("EXEC-001"));
        InformeEjecucion limite = SERVICIO.ejecutar("CREAR TABLA t (id ENTERO);\nINSERTAR EN t VALORES (" + "1".repeat(1001) + ");");
        probar("no redondear silenciosamente fuera de precisión", !limite.esExitoso() && limite.resultados().size() == 2
                && limite.resultados().getLast().linea() == 2 && limite.resultados().getLast().error().contains("1000"));
    }

    private static void archivoConBOM() throws Exception {
        Path archivo = Files.createTempFile("fase3-bom-", ".sql");
        try {
            Files.writeString(archivo, "\uFEFFCREAR TABLA t (id ENTERO);"
                    + "INSERTAR EN t VALORES (1);SELECCIONAR * DESDE t;", StandardCharsets.UTF_8);
            String fuente = Files.readString(archivo, StandardCharsets.UTF_8);
            InformeEjecucion informe = SERVICIO.ejecutar(fuente);
            probar("archivo UTF-8 con BOM inicial ejecuta y conserva posición 1:1", informe.esExitoso()
                    && ultimaConsulta(informe).filas().size() == 1
                    && informe.resultados().getFirst().linea() == 1
                    && informe.resultados().getFirst().columna() == 1);
            probar("archivo con BOM coincide con referencia SQL también con BOM", SERVICIO.comparar(fuente,
                    "\uFEFFCREATE TABLE t (id INT);INSERT INTO t VALUES (1);SELECT * FROM t;").coincide());
        } finally {
            Files.deleteIfExists(archivo);
        }
    }

    private static void comparar() {
        String fuente = "CREAR TABLA t (id ENTERO, nombre TEXTO);"
                + "INSERTAR EN t (id) VALORES (2);INSERTAR EN t VALORES (1, 'a');INSERTAR EN t VALORES (1, 'a');"
                + "SELECCIONAR * DESDE t;";
        String referencia = "CREATE TABLE t (id INT, nombre VARCHAR(100));"
                + "INSERT INTO t VALUES (1, 'a');INSERT INTO t VALUES (1, 'a');INSERT INTO t (id) VALUES (2);"
                + "SELECT * FROM t ORDER BY id;";
        ResultadoComparacion igual = SERVICIO.comparar(fuente, referencia);
        probar("orden de filas, NULL y tipos numéricos normalizados", igual.coincide());
        if (!igual.coincide()) System.out.println(igual.detalles() + " " + igual.referencia());
        probar("repeticiones cuentan", !SERVICIO.comparar(fuente,
                referencia.replace("INSERT INTO t VALUES (1, 'a');INSERT INTO t VALUES (1, 'a');", "INSERT INTO t VALUES (1, 'a');")).coincide());
        probar("diferencia en valores detectada", !SERVICIO.comparar(fuente, referencia.replace("'a'", "'b'")).coincide());
        probar("esquema sin consultas también se compara", !SERVICIO.comparar("CREAR TABLA t (id ENTERO);", "CREATE TABLE t (id VARCHAR(20));").coincide());
        probar("tablas de más detectadas", !SERVICIO.comparar("CREAR TABLA t (id ENTERO);", "CREATE TABLE t (id INT);CREATE TABLE extra (id INT);").coincide());
        probar("datos finales sin consultas también se comparan", !SERVICIO.comparar("CREAR TABLA t (id ENTERO);INSERTAR EN t VALORES (1);",
                "CREATE TABLE t (id INT);INSERT INTO t VALUES (2);").coincide());
        probar("decimal normalizado", SERVICIO.comparar("CREAR TABLA t (v DECIMAL);INSERTAR EN t VALORES (1.20);SELECCIONAR * DESDE t;",
                "CREATE TABLE t (v DECIMAL(20,2));INSERT INTO t VALUES (1.2);SELECT * FROM t;").coincide());
        probar("consulta vacía conserva columnas y puede coincidir", SERVICIO.comparar("CREAR TABLA t (id ENTERO);SELECCIONAR * DESDE t;",
                "CREATE TABLE t (id INT);SELECT * FROM t;").coincide());
        probar("nombres de columnas diferentes se detectan", !SERVICIO.comparar("CREAR TABLA t (id ENTERO);SELECCIONAR * DESDE t;",
                "CREATE TABLE t (otro INT);SELECT * FROM t;").coincide());
        probar("todas las filas borradas comparan esquema final", SERVICIO.comparar("CREAR TABLA t (id ENTERO);INSERTAR EN t VALORES (1);BORRAR DESDE t;",
                "CREATE TABLE t (id INT);INSERT INTO t VALUES (1);DELETE FROM t;").coincide());
        probar("DROP en ambos deja catálogo vacío", SERVICIO.comparar("CREAR TABLA t (id ENTERO);ELIMINAR TABLA t;",
                "CREATE TABLE t (id INT);DROP TABLE t;").coincide());
    }

    private static void erroresReferencia() {
        String fuente = "CREAR TABLA t (v TEXTO);INSERTAR EN t VALORES ('a;--b/*c*/');SELECCIONAR * DESDE t;";
        probar("separador dentro de cadenas y comentarios", SERVICIO.comparar(fuente,
                "-- inicio;\nCREATE TABLE t (v TEXT);/* ; bloque */INSERT INTO t VALUES ('a;--b/*c*/');SELECT * FROM t;").coincide());
        probar("referencia admite apóstrofe SQL escapado", SERVICIO.comparar("CREAR TABLA t (v TEXTO);INSERTAR EN t VALORES ('O''Brien;');SELECCIONAR * DESDE t;",
                "CREATE TABLE t (v TEXT);INSERT INTO t VALUES ('O''Brien;');SELECT * FROM t;").coincide());
        probar("referencia admite BOM inicial", SERVICIO.comparar("CREAR TABLA t (id ENTERO);",
                "\uFEFFCREATE TABLE t (id INT);").coincide());
        String largo = "a;O'Brien -- /* */ ".repeat(3000);
        String literal = "'" + largo.replace("'", "''") + "'";
        probar("referencia larga respeta comillas y delimitadores", SERVICIO.comparar(
                "CREAR TABLA t (v TEXTO);INSERTAR EN t VALORES (" + literal + ");SELECCIONAR * DESDE t;",
                "CREATE TABLE t (v TEXT);INSERT INTO t VALUES (" + literal + ");SELECT * FROM t;").coincide());
        ResultadoComparacion crlf = SERVICIO.comparar("CREAR TABLA t (id ENTERO);",
                "CREATE TABLE t (id INT);\r\nINSERT INTO ausente VALUES (1);");
        probar("posición de referencia después de CRLF", !crlf.coincide() && crlf.referencia().getLast().linea() == 2
                && crlf.referencia().getLast().columna() == 1);
        probar("comparación explica causa y posición del error", crlf.detalles().stream()
                .anyMatch(d -> d.contains("línea 2, columna 1") && d.contains("EXEC-001")));
        ResultadoComparacion cr = SERVICIO.comparar("CREAR TABLA t (id ENTERO);",
                "CREATE TABLE t (id INT);\r-- comentario\rINSERT INTO ausente VALUES (1);");
        probar("posición de referencia después de CR", !cr.coincide() && cr.referencia().getLast().linea() == 3
                && cr.referencia().getLast().columna() == 1);
        String simple = "CREAR TABLA t (id ENTERO);";
        for (String referencia : List.of("", "CREATE TABLE t (id INT)", "CREATE TABLE t (id INT);SELECT * FROM inexistente;",
                "CREATE TABLE t (id INT);INSERT INTO t VALUES ('abc');", "CALL 1;", "RUNSCRIPT FROM 'x';",
                "CREATE ALIAS X AS 'x';", "SELECT * FROM CSVREAD('x');", "CREATE TABLE t (id INT);/*sin cerrar")) {
            probar("referencia inválida no coincide: " + referencia, !SERVICIO.comparar(simple, referencia).coincide());
        }
        probar("dos scripts vacíos nunca coinciden", !SERVICIO.comparar("", "").coincide());
        probar("error original no ejecuta referencia", SERVICIO.comparar("SELECCIONAR * DESDE noexiste;", "CREATE TABLE t (id INT);").referencia().isEmpty());
    }

    private static void compararIdentificadores() {
        String fuente = "CREAR TABLA t_a (uno ENTERO);CREAR TABLA tba (dos TEXTO);"
                + "INSERTAR EN t_a VALORES (7);INSERTAR EN tba VALORES ('dato');"
                + "SELECCIONAR * DESDE t_a;SELECCIONAR * DESDE tba;";
        String referencia = "CREATE TABLE t_a (uno INT);CREATE TABLE tba (dos TEXT);"
                + "INSERT INTO t_a VALUES (7);INSERT INTO tba VALUES ('dato');"
                + "SELECT * FROM t_a;SELECT * FROM tba;";
        probar("guion bajo no mezcla columnas de tablas al comparar", SERVICIO.comparar(fuente, referencia).coincide());
        probar("diferencias reales se conservan con guion bajo", !SERVICIO.comparar(fuente,
                referencia.replace("uno", "distinto")).coincide());
        String espanol = "CREAR TABLA niños (año ENTERO, descripción TEXTO);"
                + "INSERTAR EN niños VALORES (2026, 'matrícula');"
                + "SELECCIONAR año, descripción DESDE niños DONDE año = 2026 Y descripción = 'matrícula';";
        String sql = "CREATE TABLE niños (año INT, descripción TEXT);"
                + "INSERT INTO niños VALUES (2026, 'matrícula');"
                + "SELECT año, descripción FROM niños WHERE año = 2026 AND descripción = 'matrícula';";
        probar("referencia acepta identificadores españoles en tabla columnas y condiciones",
                SERVICIO.comparar(espanol, sql).coincide());
        probar("referencia con identificadores españoles detecta datos distintos", !SERVICIO.comparar(espanol,
                sql.replace("matrícula", "otro")).coincide());
    }

    private static void inmutabilidad() {
        List<Object> fila = new ArrayList<>(Arrays.asList(1, null));
        List<List<Object>> filas = new ArrayList<>();
        filas.add(fila);
        ResultadoEjecucion resultado = new ResultadoEjecucion(1, "SELECT", 1, 1, "", List.of("a", "b"), filas, -1, null);
        fila.set(0, 2); filas.clear();
        probar("DTO copia filas y admite null", resultado.filas().size() == 1 && resultado.filas().getFirst().getFirst().equals(1)
                && resultado.filas().getFirst().get(1) == null && resultado.esExitoso());
        boolean bloqueado = false;
        try { resultado.filas().getFirst().set(0, 3); } catch (UnsupportedOperationException e) { bloqueado = true; }
        probar("DTO inmutable", bloqueado);
    }
}
