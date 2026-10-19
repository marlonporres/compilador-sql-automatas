import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import lexer.ScannerSQL;
import lexer.TipoToken;
import lexer.Token;
import symbol_table.CategoriaSimbolo;
import symbol_table.Simbolo;
import symbol_table.TablaSimbolos;

/** Regresiones del contrato léxico y del recorrido real archivo -> Main -> tabla. */
public class PruebaFase1 {
    private static int total;
    private static int fallidas;

    public static void main(String[] args) throws Exception {
        reservadas();
        literalesYOperadores();
        posicionesYComentarios();
        integracion();
        archivosYMain();
        System.out.println("Pruebas Fase 1: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) System.exit(1);
    }

    private static void reservadas() {
        String palabras = "crear tabla alterar eliminar truncar renombrar seleccionar insertar "
                + "actualizar borrar desde donde en valores asignar entero texto fecha decimal booleano y o no";
        String tipos = "CREAR TABLA ALTERAR ELIMINAR TRUNCAR RENOMBRAR SELECCIONAR INSERTAR "
                + "ACTUALIZAR BORRAR DESDE DONDE EN VALORES ASIGNAR TIPO_ENTERO TIPO_TEXTO "
                + "TIPO_FECHA TIPO_DECIMAL TIPO_BOOLEANO OP_Y OP_O OP_NO";
        tipos(palabras, tipos);
        Locale anterior = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            tipos(palabras, tipos);
            TablaSimbolos tabla = tabla("id ID Id");
            probar("identificadores independientes de la configuración regional",
                    tabla.tamano() == 1 && tabla.buscar("id").getApariciones() == 3);
        } finally {
            Locale.setDefault(anterior);
        }
        tipos("usuarios nombre_usuario tabla1 _temporal año insertar2 NULO LLAVE PRIMARIA FORANEA",
                "IDENTIFICADOR IDENTIFICADOR IDENTIFICADOR IDENTIFICADOR IDENTIFICADOR "
                + "IDENTIFICADOR IDENTIFICADOR IDENTIFICADOR IDENTIFICADOR IDENTIFICADOR");
        token(analizar("CrEaR MiTabla").get(0), TipoToken.CREAR, "CREAR", 1, 1);
        token(analizar("CrEaR MiTabla").get(1), TipoToken.IDENTIFICADOR, "MiTabla", 1, 7);
    }

    private static void literalesYOperadores() {
        tipos("0 25 85.50 0001 '' 'Carlos' '/* texto -- */'",
                "LITERAL_ENTERO LITERAL_ENTERO LITERAL_DECIMAL LITERAL_ENTERO "
                + "LITERAL_CADENA LITERAL_CADENA LITERAL_CADENA");
        tipos("= > < >= <= != <> , ; ( ) *", "OP_IGUAL OP_MAYOR OP_MENOR OP_MAYOR_IGUAL "
                + "OP_MENOR_IGUAL OP_DIFERENTE OP_DIFERENTE COMA PUNTO_Y_COMA PARENTESIS_IZQ PARENTESIS_DER ASTERISCO");
        tipos("==", "ERROR");
        tipos("85.5.2 25. 1..2 ! @ . / - + \u0661", "ERROR ERROR ERROR ERROR ERROR ERROR ERROR ERROR ERROR ERROR");
        tipos("'sin cerrar", "ERROR");
        tipos("/* sin cerrar", "ERROR");
        tipos("/*", "ERROR");
        tipos("@ nombre; ! otro", "ERROR IDENTIFICADOR PUNTO_Y_COMA ERROR IDENTIFICADOR");
        token(analizar("85.5.2").get(0), TipoToken.ERROR, "85.5.2", 1, 1);
        token(analizar("'Carlos'").get(0), TipoToken.LITERAL_CADENA, "Carlos", 1, 1);
        token(analizar("'O''Brien' id").get(0), TipoToken.LITERAL_CADENA, "O'Brien", 1, 1);
        token(analizar("'O''Brien' id").get(1), TipoToken.IDENTIFICADOR, "id", 1, 12);
        tipos("'texto'' sin cierre", "ERROR");
    }

    private static void posicionesYComentarios() {
        for (String salto : List.of("\n", "\r\n", "\r")) {
            List<Token> tokens = analizar("-- ignorado" + salto + "\t/* x" + salto + " */ id");
            probar("comentarios no producen tokens", tokens.size() == 2);
            token(tokens.get(0), TipoToken.IDENTIFICADOR, "id", 3, 5);
            token(tokens.get(1), TipoToken.FIN_DE_ARCHIVO, "EOF", 3, 7);
            tokens = analizar("'a" + salto + "b' id");
            token(tokens.get(0), TipoToken.LITERAL_CADENA, "a" + salto + "b", 1, 1);
            token(tokens.get(1), TipoToken.IDENTIFICADOR, "id", 2, 4);
            token(tokens.get(2), TipoToken.FIN_DE_ARCHIVO, "EOF", 2, 6);
            tokens = analizar(" " + salto + " 'a" + salto + "b");
            token(tokens.get(0), TipoToken.ERROR, "a" + salto + "b", 2, 2);
            token(tokens.get(1), TipoToken.FIN_DE_ARCHIVO, "EOF", 3, 2);
            tokens = analizar(" " + salto + " /* x" + salto + "y");
            token(tokens.get(0), TipoToken.ERROR, "/* x" + salto + "y", 2, 2);
            token(tokens.getLast(), TipoToken.FIN_DE_ARCHIVO, "EOF", 3, 2);
        }
        tipos("/**/-- fin", "");
        token(analizar("").get(0), TipoToken.FIN_DE_ARCHIVO, "EOF", 1, 1);
        token(analizar(" \t\n").get(0), TipoToken.FIN_DE_ARCHIVO, "EOF", 2, 1);
        token(analizar("\uFEFFCREAR").get(0), TipoToken.CREAR, "CREAR", 1, 1);
        token(analizar("\uFEFF").get(0), TipoToken.FIN_DE_ARCHIVO, "EOF", 1, 1);
        tipos("id\uFEFFotro", "IDENTIFICADOR ERROR IDENTIFICADOR");
    }

    private static void integracion() {
        TablaSimbolos tabla = tabla("CREAR TABLA usuarios (id ENTERO, correo TEXTO, edad ENTERO);\n"
                + "SELECCIONAR correo DESDE USUARIOS DONDE edad > 18; @");
        probar("solo cuatro identificadores distintos", tabla.tamano() == 4);
        List<Simbolo> simbolos = tabla.obtenerTodos();
        String[] nombres = {"usuarios", "id", "correo", "edad"};
        int[] columnas = {13, 23, 34, 48};
        int[] apariciones = {2, 1, 2, 2};
        for (int i = 0; i < nombres.length; i++) {
            Simbolo s = simbolos.get(i);
            probar("atributos del símbolo " + nombres[i], s.getNombre().equals(nombres[i])
                    && s.getId() == i + 1 && s.getLinea() == 1 && s.getColumna() == columnas[i]
                    && s.getApariciones() == apariciones[i]
                    && s.getCategoria() == CategoriaSimbolo.DESCONOCIDA
                    && s.getTipoDato().equals(Simbolo.TIPO_DATO_DESCONOCIDO));
        }
        simbolos.clear();
        probar("modificar la copia no borra la tabla", tabla.tamano() == 4);
        ScannerSQL scanner = new ScannerSQL();
        scanner.analizar("primero");
        List<Token> anteriores = scanner.getTokens();
        scanner.getTokens().clear();
        probar("getTokens protege la lista interna", scanner.getTokens().size() == 2);
        scanner.analizar("segundo");
        probar("cada análisis reemplaza los tokens", scanner.getTokens().size() == 2
                && scanner.getTokens().get(0).getLexema().equals("segundo"));
        probar("la copia anterior permanece estable", anteriores.get(0).getLexema().equals("primero"));
    }

    private static void archivosYMain() throws Exception {
        for (String archivo : List.of("tests/test_ddl.sql", "tests/test_dml.sql")) {
            String entrada = Files.readString(Path.of(archivo), StandardCharsets.UTF_8);
            int corte = entrada.indexOf("-- PRUEBAS DE ERRORES LEXICOS");
            probar("sección negativa identificable en " + archivo, corte >= 0);
            String valida = entrada.substring(0, corte);
            probar("DDL/DML válidos sin errores: " + archivo, errores(analizar(valida)) == 0);
            probar("cuatro errores intencionales: " + archivo, errores(analizar(entrada)) == 4);
            TablaSimbolos tabla = tabla(valida);
            probar("identificadores esperados: " + archivo, tabla.tamano() == (archivo.contains("ddl") ? 9 : 3));
            probar("INSERTAR no entra como identificador", !tabla.existe("INSERTAR"));
            if (archivo.contains("dml")) {
                probar("conteos DML reales", tabla.buscar("usuarios").getApariciones() == 10
                        && tabla.buscar("nombre").getApariciones() == 5
                        && tabla.buscar("id").getApariciones() == 6);
            } else {
                probar("conteos DDL reales", tabla.buscar("usuarios").getApariciones() == 3
                        && tabla.buscar("productos").getApariciones() == 3
                        && tabla.buscar("id").getApariciones() == 2);
            }
            Path temporal = Files.createTempFile("fase1-valida-", ".sql");
            try {
                Files.writeString(temporal, valida, StandardCharsets.UTF_8);
                proceso(0, "No se encontraron errores lexicos.", temporal.toString());
            } finally {
                Files.deleteIfExists(temporal);
            }
            proceso(1, "Errores lexicos: 4", archivo);
        }
        proceso(0, "Uso:", "--ayuda");
        proceso(2, "Uso:", "tests/test_ddl.sql", "tests/test_dml.sql");
        Path inexistente = Files.createTempFile("fase1-inexistente-", ".sql");
        try {
            proceso(0, "No se encontraron errores lexicos.", inexistente.toString());
        } finally {
            Files.deleteIfExists(inexistente);
        }
        proceso(2, "ERROR AL LEER EL ARCHIVO:", inexistente.toString());
        proceso(2, "ERROR AL LEER EL ARCHIVO:", ".");
    }

    private static void proceso(int codigo, String mensaje, String... args) throws Exception {
        // Probar Main en otro proceso permite verificar los códigos de salida reales.
        List<String> comando = new java.util.ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"), "Main"));
        comando.addAll(List.of(args));
        Path salida = Files.createTempFile("fase1-salida-", ".txt");
        try {
            Process proceso = new ProcessBuilder(comando).redirectErrorStream(true)
                    .redirectOutput(salida.toFile()).start();
            boolean termino = proceso.waitFor(20, TimeUnit.SECONDS);
            if (!termino) {
                proceso.destroyForcibly().waitFor();
                probar("Main termina sin bloquearse", false);
                return;
            }
            String texto = Files.readString(salida, StandardCharsets.UTF_8);
            boolean tablaVisible = codigo == 2 || mensaje.equals("Uso:") || (texto.contains("TABLA DE SIMBOLOS")
                    && texto.contains("FIN_DE_ARCHIVO") && texto.contains("RESULTADO:"));
            probar("Main: " + mensaje,
                    proceso.exitValue() == codigo && texto.contains(mensaje) && tablaVisible);
        } finally {
            Files.deleteIfExists(salida);
        }
    }

    private static List<Token> analizar(String entrada) {
        ScannerSQL scanner = new ScannerSQL();
        scanner.analizar(entrada);
        return scanner.getTokens();
    }

    private static TablaSimbolos tabla(String entrada) {
        TablaSimbolos tabla = new TablaSimbolos();
        tabla.registrarTodos(analizar(entrada));
        return tabla;
    }

    private static long errores(List<Token> tokens) {
        return tokens.stream().filter(t -> t.getTipo() == TipoToken.ERROR).count();
    }

    private static void tipos(String entrada, String esperados) {
        String actual = String.join(" ", analizar(entrada).stream().map(t -> t.getTipo().name()).toList());
        probar("tipos de " + entrada.replace("\n", "\\n"),
                actual.equals((esperados.isEmpty() ? "" : esperados + " ") + "FIN_DE_ARCHIVO"));
    }

    private static void token(Token token, TipoToken tipo, String lexema, int fila, int columna) {
        probar("token " + tipo + " en " + fila + ":" + columna,
                token.getTipo() == tipo && token.getLexema().equals(lexema)
                && token.getlinea() == fila && token.getcolumna() == columna);
    }

    private static void probar(String descripcion, boolean condicion) {
        total++;
        if (!condicion) {
            fallidas++;
            System.out.println("FALLA: " + descripcion);
        }
    }
}
