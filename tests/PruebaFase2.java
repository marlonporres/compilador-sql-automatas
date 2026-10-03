import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import compiler.CompiladorSQL;
import compiler.ResultadoCompilacion;
import errors.FaseError;
import lexer.ScannerSQL;
import lexer.TipoToken;
import lexer.Token;
import parser.AnalizadorSintactico;
import parser.CondicionSQL;
import parser.CursorTokens;
import parser.SentenciaDML;
import semantic.TipoDato;

/** Verifica el flujo completo de fase 2 y los contratos de recuperación/atomicidad. */
public final class PruebaFase2 {
    private static int total;
    private static int fallidas;
    private PruebaFase2() { }

    public static void main(String[] args) throws Exception {
        sintaxis();
        precedencia();
        esquemas();
        datos();
        posicionesYRecuperacion();
        demosYConsola();
        System.out.println("Pruebas Fase 2: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) System.exit(1);
    }

    private static ResultadoCompilacion analizar(String sql) {
        return new CompiladorSQL().analizar(sql);
    }
    private static boolean codigo(ResultadoCompilacion r, String codigo) {
        return r.errores().obtenerTodos().stream().anyMatch(e -> e.getCodigo().equals(codigo));
    }
    private static void probar(String nombre, boolean condicion) {
        total++;
        if (!condicion) { fallidas++; System.out.println("FALLA: " + nombre); }
    }

    private static void sintaxis() {
        List<String> invalidas = List.of(
                "CREAR t (id ENTERO);", "CREAR TABLA t ();", "CREAR TABLA t (id);",
                "CREAR TABLA t (id ENTERO,);", "CREAR TABLA t (id ENTERO;",
                "CREAR TABLA t (id ENTERO)", "ALTERAR TABLA t;",
                "ALTERAR TABLA t CAMBIAR id ENTERO;", "ALTERAR TABLA t AGREGAR;",
                "ALTERAR TABLA t MODIFICAR id;", "ALTERAR TABLA t QUITAR;",
                "ELIMINAR t;", "TRUNCAR TABLA;", "RENOMBRAR TABLA t;",
                "RENOMBRAR TABLA t A;", "SELECCIONAR DESDE t;",
                "SELECCIONAR * t;", "SELECCIONAR id, DESDE t;",
                "SELECCIONAR * DESDE t DONDE;", "SELECCIONAR * DESDE t DONDE id =;",
                "SELECCIONAR * DESDE t DONDE id;", "SELECCIONAR * DESDE t DONDE (id = 1;",
                "SELECCIONAR * DESDE t DONDE id = 1 Y;",
                "INSERTAR t VALORES (1);", "INSERTAR EN t VALORES ();",
                "INSERTAR EN t VALORES (1,);", "INSERTAR EN t (id,) VALORES (1);",
                "ACTUALIZAR t ASIGNAR;", "ACTUALIZAR t ASIGNAR id 1;",
                "ACTUALIZAR t ASIGNAR id = 1,;", "BORRAR t;", ";", "ALGO;");
        for (String sql : invalidas) {
            ResultadoCompilacion r = analizar(sql);
            probar("rechaza sintaxis " + sql, r.errores().contar(FaseError.SINTACTICA) > 0
                    && r.sentencias().isEmpty() && r.catalogo().tamano() == 0);
        }
        probar("vacío y comentarios son válidos", analizar("-- comentario\n/* bloque */").esValido());
        ScannerSQL scanner = new ScannerSQL();
        scanner.analizar("CREAR TABLA t (id ENTERO);");
        List<Token> sinEOF = new ArrayList<>(scanner.getTokens());
        sinEOF.remove(sinEOF.size() - 1);
        probar("dispatcher sin EOF explícito", new AnalizadorSintactico().analizar(sinEOF)
                .getSentencias().size() == 1);
        CursorTokens cursor = new CursorTokens(scanner.getTokens());
        while (!cursor.estaAlFinal()) cursor.avanzar();
        int posicion = cursor.posicion();
        cursor.avanzar();
        probar("EOF conserva posición y no avanza", cursor.posicion() == posicion
                && cursor.ver().getTipo() == TipoToken.FIN_DE_ARCHIVO);
    }

    private static void precedencia() {
        ResultadoCompilacion r = analizar("CREAR TABLA t (id ENTERO);"
                + "SELECCIONAR * DESDE t DONDE id = 1 O id = 2 Y NO id = 3;");
        CondicionSQL c = ((SentenciaDML) r.sentencias().get(1)).getCondicion();
        probar("NO > Y > O", r.esValido() && c.getOperador().equals("O")
                && c.getSegunda().getOperador().equals("Y")
                && c.getSegunda().getSegunda().getOperador().equals("NO"));
        r = analizar("CREAR TABLA t (id ENTERO);"
                + "SELECCIONAR * DESDE t DONDE (id = 1 O id = 2) Y id > 0;");
        c = ((SentenciaDML) r.sentencias().get(1)).getCondicion();
        probar("paréntesis cambian agrupación", c.getOperador().equals("Y")
                && c.getPrimera().getOperador().equals("O"));
    }

    private static void esquemas() {
        ResultadoCompilacion r = analizar("crear tabla T (id entero);"
                + "crear tabla Otra (id texto); seleccionar ID desde t;");
        probar("ámbitos y nombres insensibles a mayúsculas", r.esValido()
                && r.catalogo().buscar("t").buscarColumna("ID").tipo() == TipoDato.ENTERO
                && r.catalogo().buscar("OTRA").buscarColumna("id").tipo() == TipoDato.TEXTO);
        r = analizar("CREAR TABLA t (id ENTERO, ID TEXTO);");
        probar("CREAR inválido no modifica catálogo", codigo(r, "SEM-002") && r.catalogo().tamano() == 0);
        r = analizar("CREAR TABLA t (id ENTERO); CREAR TABLA T (id TEXTO);");
        probar("tabla duplicada conserva esquema original", codigo(r, "SEM-001")
                && r.catalogo().buscar("t").buscarColumna("id").tipo() == TipoDato.ENTERO);
        r = analizar("CREAR TABLA t (id ENTERO); ALTERAR TABLA t AGREGAR n TEXTO;"
                + "ALTERAR TABLA t MODIFICAR id DECIMAL; ALTERAR TABLA t QUITAR n;");
        probar("AGREGAR MODIFICAR QUITAR", r.esValido()
                && r.catalogo().buscar("t").obtenerColumnas().size() == 1
                && r.catalogo().buscar("t").buscarColumna("id").tipo() == TipoDato.DECIMAL);
        r = analizar("CREAR TABLA t (id ENTERO); ALTERAR TABLA t AGREGAR id TEXTO;"
                + "ALTERAR TABLA t MODIFICAR noExiste TEXTO; ALTERAR TABLA t QUITAR id;");
        probar("ALTERAR inválido conserva estado", codigo(r, "SEM-002") && codigo(r, "SEM-004")
                && codigo(r, "SEM-009") && r.catalogo().buscar("t").obtenerColumnas().size() == 1
                && r.catalogo().buscar("t").buscarColumna("id").tipo() == TipoDato.ENTERO);
        r = analizar("CREAR TABLA t (id ENTERO); RENOMBRAR TABLA t A nueva;"
                + "SELECCIONAR * DESDE nueva; SELECCIONAR * DESDE t;");
        probar("RENOMBRAR mueve ámbito", codigo(r, "SEM-003") && r.catalogo().buscar("t") == null
                && r.catalogo().buscar("nueva") != null && r.sentenciasValidas() == 3);
        r = analizar("CREAR TABLA t (id ENTERO); CREAR TABLA ocupada (id ENTERO);"
                + "RENOMBRAR TABLA t A ocupada;");
        probar("RENOMBRAR inválido no pierde tablas", codigo(r, "SEM-001")
                && r.catalogo().buscar("t") != null && r.catalogo().tamano() == 2);
        r = analizar("CREAR TABLA t (id ENTERO); TRUNCAR TABLA t; SELECCIONAR * DESDE t;");
        probar("TRUNCAR conserva esquema", r.esValido() && r.catalogo().tamano() == 1);
        r = analizar("CREAR TABLA t (id ENTERO); ELIMINAR TABLA t; SELECCIONAR * DESDE t;");
        probar("ELIMINAR y orden secuencial", codigo(r, "SEM-003") && r.catalogo().tamano() == 0);
        for (String sql : List.of("ALTERAR TABLA t AGREGAR id ENTERO;", "TRUNCAR TABLA t;",
                "ELIMINAR TABLA t;", "RENOMBRAR TABLA t A nueva;")) {
            probar("DDL exige tabla existente " + sql, codigo(analizar(sql), "SEM-003"));
        }
    }

    private static void datos() {
        String base = "CREAR TABLA t (id ENTERO, n TEXTO, monto DECIMAL,"
                + "nacimiento FECHA, activo BOOLEANO);";
        ResultadoCompilacion r = analizar(base
                + "INSERTAR EN t VALORES (1, 'Ana', 10, '2024-02-29', VERDADERO);"
                + "INSERTAR EN t (n, id) VALORES ('Luis', 2);"
                + "ACTUALIZAR t ASIGNAR activo = falso, monto = 5.5 DONDE id >= 1;"
                + "BORRAR DESDE t DONDE nacimiento < '2025-01-01';");
        probar("DML completo con cinco tipos y columnas opcionales", r.esValido()
                && r.sentenciasValidas() == 5);
        for (String sql : List.of(
                "INSERTAR EN t VALORES (1, 'Ana');",
                "INSERTAR EN t (id, id) VALORES (1, 2);",
                "INSERTAR EN t (fantasma) VALORES (1);",
                "INSERTAR EN t (id) VALORES ('uno');",
                "INSERTAR EN t (id) VALORES (1.5);",
                "INSERTAR EN t (nacimiento) VALORES ('2023-02-29');",
                "INSERTAR EN t (activo) VALORES (1);",
                "ACTUALIZAR t ASIGNAR n = 4;",
                "ACTUALIZAR t ASIGNAR id = 1, ID = 2;",
                "SELECCIONAR fantasma DESDE t;",
                "SELECCIONAR * DESDE t DONDE id = 'texto';",
                "SELECCIONAR * DESDE t DONDE fantasma = 1;",
                "SELECCIONAR * DESDE t DONDE activo > VERDADERO;",
                "SELECCIONAR * DESDE t DONDE nacimiento = '2023-02-29';",
                "SELECCIONAR * DESDE inexistente;")) {
            r = analizar(base + sql);
            probar("rechaza semántica " + sql, r.errores().contar(FaseError.SEMANTICA) > 0
                    && r.errores().contar(FaseError.SINTACTICA) == 0);
        }
        probar("comparación decimal entero compatible", analizar(base
                + "SELECCIONAR * DESDE t DONDE monto = id;").esValido());
        CompiladorSQL compilador = new CompiladorSQL();
        compilador.analizar(base);
        probar("cada análisis reinicia catálogo", codigo(compilador.analizar(
                "SELECCIONAR * DESDE t;"), "SEM-003"));
    }

    private static void posicionesYRecuperacion() {
        ResultadoCompilacion r = analizar("CREAR TABLA t (id ENTERO);\nSELECCIONAR fantasma DESDE t;");
        probar("posición exacta de columna inexistente", r.errores().obtenerTodos().get(0).getLinea() == 2
                && r.errores().obtenerTodos().get(0).getColumna() == 13);
        r = analizar("CREAR TABLA t (id ENTERO)\n");
        probar("EOF real conserva línea y columna", r.errores().obtenerTodos().get(0).getLinea() == 2
                && r.errores().obtenerTodos().get(0).getColumna() == 1);
        r = analizar("CREAR TABLA mala (id); CREAR TABLA buena (id ENTERO);"
                + "INSERTAR EN buena VALORES (@); SELECCIONAR * DESDE buena;");
        probar("recupera léxico y sintaxis sin cascada", r.errores().contar(FaseError.LEXICA) == 1
                && r.errores().contar(FaseError.SINTACTICA) == 1
                && r.sentenciasValidas() == 2 && r.catalogo().tamano() == 1);
        r = analizar("CREAR TABLA t (id ENTERO); SELECCIONAR * DESDE t DONDE ;");
        probar("DONDE inválido no llega a semántica", r.sentencias().size() == 1 && r.sentenciasValidas() == 1);
        probar("copias inmutables", inmutable(analizar("CREAR TABLA t (id ENTERO);").sentencias()));
    }
    private static boolean inmutable(List<?> lista) {
        try { lista.clear(); return false; } catch (UnsupportedOperationException e) { return true; }
    }

    private static void demosYConsola() throws Exception {
        String valida = Files.readString(Path.of("tests/demo_fase2_valido.sql"), StandardCharsets.UTF_8);
        String invalida = Files.readString(Path.of("tests/demo_fase2_errores.sql"), StandardCharsets.UTF_8);
        ResultadoCompilacion r = analizar(valida);
        probar("demo válida y catálogo final", r.esValido() && r.sentencias().size() == 15
                && r.catalogo().tamano() == 1 && r.catalogo().buscar("clientes") != null
                && r.catalogo().buscar("clientes").obtenerColumnas().size() == 5);
        r = analizar(invalida);
        probar("demo negativa contiene tres fases", r.errores().contar(FaseError.LEXICA) == 1
                && r.errores().contar(FaseError.SINTACTICA) == 1
                && r.errores().contar(FaseError.SEMANTICA) == 6
                && r.catalogo().buscar("recuperada") != null);
        proceso(0, "ANALISIS COMPLETO SIN ERRORES", "tests/demo_fase2_valido.sql", "--fase2", "--resumen");
        proceso(1, "SE ENCONTRARON ERRORES", "tests/demo_fase2_errores.sql", "--fase2");
        proceso(2, "Uso:", "tests/demo_fase2_valido.sql", "--invalido");
        proceso(2, "ERROR AL LEER EL ARCHIVO:", "tests/no-existe-fase2.sql", "--fase2");
    }

    private static void proceso(int codigo, String esperado, String... args) throws Exception {
        List<String> comando = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                "-cp", System.getProperty("java.class.path"), "Main"));
        comando.addAll(List.of(args));
        Path salida = Files.createTempFile("fase2-salida-", ".txt");
        try {
            Process proceso = new ProcessBuilder(comando).redirectErrorStream(true)
                    .redirectOutput(salida.toFile()).start();
            if (!proceso.waitFor(20, TimeUnit.SECONDS)) {
                proceso.destroyForcibly().waitFor();
                probar("Main termina", false);
                return;
            }
            String texto = Files.readString(salida, StandardCharsets.UTF_8);
            probar("Main salida " + codigo, proceso.exitValue() == codigo && texto.contains(esperado)
                    && (codigo == 2 || texto.contains("TABLA DE SIMBOLOS SEMANTICA")));
        } finally { Files.deleteIfExists(salida); }
    }
}
