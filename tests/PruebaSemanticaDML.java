import java.util.List;

import lexer.ScannerSQL;
import lexer.Token;
import lexer.AnalizadorDML;
import semantic.ErrorCompilacion;
import semantic.ReporteCompilacion;
import semantic.SemanticaDML;
import semantic.TablaErrores;
import semantic.AnalizadorSemanticoDDL;
import symbol_table.CatalogoEsquema;
import parser.ColumnaDDL;
import parser.SentenciaDDL;
import parser.TipoSentenciaDDL;
import lexer.TipoToken;

/** Pruebas de la semantica DML y del reporte comun de errores. */
public class PruebaSemanticaDML {
    private static int total;
    private static int fallidas;

    public static void main(String[] args) {
        CatalogoEsquema catalogo = catalogoBase();

        pruebaInsertarValido(catalogo);
        pruebaTablaInexistente(catalogo);
        pruebaColumnaInexistente(catalogo);
        pruebaCantidadValores(catalogo);
        pruebaTipos(catalogo);
        pruebaSeleccionarYCondicion(catalogo);
        pruebaActualizar(catalogo);
        pruebaBorrar(catalogo);
        pruebaNoAnalizaSentenciaSintacticamenteInvalida(catalogo);
        pruebaReporteComun(catalogo);
        pruebaRecuperacionEnLaMismaLinea(catalogo);
        pruebaFechaBooleanoYNumeros();
        pruebaOperandosYAsignaciones(catalogo);
        pruebaSoloLiteralesEnValores(catalogo);

        System.out.println();
        System.out.println("Pruebas: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) {
            System.exit(1);
        }
    }

    private static void pruebaInsertarValido(CatalogoEsquema catalogo) {
        SemanticaDML sem = new SemanticaDML(catalogo);
        List<ErrorCompilacion> errores = sem.analizar(tokens(
                "INSERTAR EN usuarios VALORES (1, 'Carlos');"));
        probar("INSERTAR con cantidad y tipos correctos", errores.isEmpty());
    }

    private static void pruebaTablaInexistente(CatalogoEsquema catalogo) {
        List<ErrorCompilacion> errores = analizar(catalogo,
                "INSERTAR EN fantasmas VALORES (1, 'Ana');");
        probar("tabla inexistente da SEM-201", tieneCodigo(errores, "SEM-201"));
    }

    private static void pruebaColumnaInexistente(CatalogoEsquema catalogo) {
        List<ErrorCompilacion> errores = analizar(catalogo,
                "INSERTAR EN usuarios (id, correo) VALORES (1, 'Ana');");
        probar("columna inexistente da SEM-202", tieneCodigo(errores, "SEM-202"));
    }

    private static void pruebaCantidadValores(CatalogoEsquema catalogo) {
        List<ErrorCompilacion> errores = analizar(catalogo,
                "INSERTAR EN usuarios VALORES (1);\n"
                        + "INSERTAR EN usuarios (id) VALORES (2, 'Ana');");
        probar("cantidad incorrecta da SEM-203", cuentaCodigo(errores, "SEM-203") == 2);
    }

    private static void pruebaTipos(CatalogoEsquema catalogo) {
        List<ErrorCompilacion> errores = analizar(catalogo,
                "INSERTAR EN usuarios VALORES ('no es entero', 'Ana');\n"
                        + "INSERTAR EN productos VALORES ('no es entero', 4);");
        probar("tipos incompatibles dan SEM-204", cuentaCodigo(errores, "SEM-204") == 2);
    }

    private static void pruebaSeleccionarYCondicion(CatalogoEsquema catalogo) {
        List<ErrorCompilacion> valido = analizar(catalogo,
                "SELECCIONAR nombre DESDE usuarios DONDE id >= 1;");
        List<ErrorCompilacion> errores = analizar(catalogo,
                "SELECCIONAR correo DESDE usuarios;\n"
                        + "SELECCIONAR nombre DESDE usuarios DONDE nombre > 'Ana';");
        probar("SELECCIONAR valido no da errores", valido.isEmpty());
        probar("columna inexistente en SELECT da SEM-202", cuentaCodigo(errores, "SEM-202") >= 1);
        probar("operador numerico sobre TEXTO da SEM-205", tieneCodigo(errores, "SEM-205"));
    }

    private static void pruebaActualizar(CatalogoEsquema catalogo) {
        List<ErrorCompilacion> errores = analizar(catalogo,
                "ACTUALIZAR usuarios ASIGNAR nombre = 'Pedro' DONDE id = 1;\n"
                        + "ACTUALIZAR usuarios ASIGNAR fantasma = 1;");
        probar("ACTUALIZAR valido pasa", tieneCantidadCeroEnPrimera(errores, "SEM-202"));
        probar("columna inexistente en UPDATE da SEM-202", tieneCodigo(errores, "SEM-202"));
    }

    private static void pruebaBorrar(CatalogoEsquema catalogo) {
        List<ErrorCompilacion> errores = analizar(catalogo,
                "BORRAR DESDE usuarios DONDE fantasma = 1;");
        probar("columna inexistente en BORRAR da SEM-202", tieneCodigo(errores, "SEM-202"));
    }

    private static void pruebaNoAnalizaSentenciaSintacticamenteInvalida(CatalogoEsquema catalogo) {
        ScannerSQL scanner = new ScannerSQL();
        scanner.analizar("INSERTAR EN usuarios VALORES (1, );\n"
                + "INSERTAR EN usuarios VALORES (2, 'Ana');");
        List<Token> tokens = scanner.getTokens();
        SemanticaDML sem = new SemanticaDML(catalogo);
        List<ErrorCompilacion> erroresSintacticos = sem.analizar(tokens);
        probar("una sentencia sintacticamente invalida no genera error semantico propio",
                erroresSintacticos.isEmpty());
    }

    private static void pruebaReporteComun(CatalogoEsquema catalogo) {
        TablaErrores tabla = new TablaErrores();
        SemanticaDML sem = new SemanticaDML(catalogo, tabla);
        List<ErrorCompilacion> errores = sem.analizar(tokens(
                "INSERTAR EN fantasmas VALORES (1, 'Ana');"));
        String reporte = ReporteCompilacion.generar(tabla);
        probar("el reporte conserva fase, codigo y posicion", errores.size() == 1
                && reporte.contains("[SEMANTICA][SEM-201]")
                && reporte.contains("L:1 C:"));
    }

    private static void pruebaRecuperacionEnLaMismaLinea(CatalogoEsquema catalogo) {
        String script = "INSERTAR EN usuarios VALORES (1,); "
                + "SELECCIONAR fantasma DESDE usuarios;";
        List<ErrorCompilacion> errores = analizar(catalogo, script);
        probar("error sintactico no oculta error semantico posterior en la misma linea",
                errores.size() == 1 && tieneCodigo(errores, "SEM-202")
                && errores.getFirst().getLinea() == 1
                && errores.getFirst().getElemento().equals("fantasma"));

        List<Token> entrada = tokens(script);
        errores = new SemanticaDML(catalogo).analizar(entrada,
                new AnalizadorDML().analizar(entrada));
        probar("sobrecarga con errores sintacticos conserva la otra sentencia de la misma linea",
                errores.size() == 1 && tieneCodigo(errores, "SEM-202")
                && errores.getFirst().getElemento().equals("fantasma"));
    }

    private static void pruebaFechaBooleanoYNumeros() {
        CatalogoEsquema catalogo = catalogoCincoTipos();
        probar("INSERTAR admite fecha real, VERDADERO y falso con cinco tipos",
                analizar(catalogo,
                        "INSERTAR EN datos VALORES (1, 'Ana', 10, '2024-02-29', VERDADERO);"
                        + "INSERTAR EN datos VALORES (2, 'Luis', 10.5, '2025-01-01', falso);")
                        .isEmpty());

        for (String fecha : List.of("2023-02-29", "2024-02-30", "2024-2-01")) {
            List<ErrorCompilacion> errores = analizar(catalogo,
                    "INSERTAR EN datos (nacimiento) VALORES ('" + fecha + "');");
            probar("FECHA rechaza calendario o formato invalido: " + fecha,
                    errores.size() == 1 && tieneCodigo(errores, "SEM-204"));
        }

        probar("BOOLEANO rechaza un numero",
                tieneCodigo(analizar(catalogo,
                        "ACTUALIZAR datos ASIGNAR activo = 1;"), "SEM-204"));
        probar("BOOLEANO distingue literal booleano de cadena",
                tieneCodigo(analizar(catalogo,
                        "ACTUALIZAR datos ASIGNAR activo = 'VERDADERO';"), "SEM-204"));
        probar("FECHA permite comparar fechas ISO con orden",
                analizar(catalogo, "SELECCIONAR * DESDE datos DONDE "
                        + "nacimiento >= '2024-02-29' Y nacimiento < '2026-01-01';").isEmpty());
        probar("una condicion FECHA tambien valida el calendario",
                tieneCodigo(analizar(catalogo, "SELECCIONAR * DESDE datos "
                        + "DONDE nacimiento = '2023-02-29';"), "SEM-204"));
        probar("condiciones admiten igualdad y desigualdad BOOLEANO",
                analizar(catalogo, "BORRAR DESDE datos DONDE "
                        + "activo = VERDADERO O activo != FALSO;").isEmpty());
        probar("comparacion ordenada BOOLEANO da SEM-205",
                tieneCodigo(analizar(catalogo, "SELECCIONAR * DESDE datos "
                        + "DONDE activo > FALSO;"), "SEM-205"));
        probar("condiciones numericas admiten ENTERO y DECIMAL en ambos lados",
                analizar(catalogo, "SELECCIONAR * DESDE datos "
                        + "DONDE id < precio Y precio >= id;").isEmpty());
    }

    private static void pruebaOperandosYAsignaciones(CatalogoEsquema catalogo) {
        List<ErrorCompilacion> errores = analizar(catalogo,
                "SELECCIONAR * DESDE usuarios DONDE 1 = fantasma;");
        probar("literal a la izquierda no oculta columna inexistente a la derecha",
                errores.size() == 1 && tieneCodigo(errores, "SEM-202")
                && errores.getFirst().getElemento().equals("fantasma"));

        errores = analizar(catalogo,
                "ACTUALIZAR usuarios ASIGNAR nombre = 'Ana', NOMBRE = 'Luis';");
        probar("asignaciones duplicadas sin distinguir mayusculas dan SEM-203",
                errores.size() == 1 && tieneCodigo(errores, "SEM-203"));

        probar("condiciones anidadas validan ambos operandos y ramas logicas",
                analizar(catalogoCincoTipos(), "SELECCIONAR * DESDE datos DONDE "
                        + "NO ((id = precio O precio > 1) "
                        + "Y (activo = VERDADERO Y nombre != 'Luis'));").isEmpty());

        errores = analizar(catalogo, "SELECCIONAR * DESDE usuarios DONDE "
                + "(NO (izquierda = 1) Y (id = derecha O nombre = 'Ana'));");
        probar("condiciones anidadas detectan columnas de izquierda y derecha",
                cuentaCodigo(errores, "SEM-202") == 2
                && errores.stream().anyMatch(e -> e.getElemento().equals("izquierda"))
                && errores.stream().anyMatch(e -> e.getElemento().equals("derecha")));
    }

    private static void pruebaSoloLiteralesEnValores(CatalogoEsquema catalogo) {
        for (String script : List.of(
                "INSERTAR EN usuarios (id) VALORES (id);",
                "ACTUALIZAR usuarios ASIGNAR id = id;")) {
            List<Token> entrada = tokens(script);
            probar("valores no literales se rechazan antes de semantica: " + script,
                    !new AnalizadorDML().analizar(entrada).isEmpty()
                    && new SemanticaDML(catalogo).analizar(entrada).isEmpty());
        }
    }

    private static CatalogoEsquema catalogoCincoTipos() {
        CatalogoEsquema catalogo = new CatalogoEsquema();
        AnalizadorSemanticoDDL ddl = new AnalizadorSemanticoDDL(catalogo);
        ddl.analizarSentencia(new SentenciaDDL(TipoSentenciaDDL.CREAR_TABLA, "datos",
                List.of(new ColumnaDDL("id", TipoToken.TIPO_ENTERO, 1, 20),
                        new ColumnaDDL("nombre", TipoToken.TIPO_TEXTO, 1, 31),
                        new ColumnaDDL("precio", TipoToken.TIPO_DECIMAL, 1, 45),
                        new ColumnaDDL("nacimiento", TipoToken.TIPO_FECHA, 1, 61),
                        new ColumnaDDL("activo", TipoToken.TIPO_BOOLEANO, 1, 79)), 1, 1));
        return catalogo;
    }

    private static CatalogoEsquema catalogoBase() {
        CatalogoEsquema catalogo = new CatalogoEsquema();
        AnalizadorSemanticoDDL ddl = new AnalizadorSemanticoDDL(catalogo);
        ddl.analizarSentencia(new SentenciaDDL(TipoSentenciaDDL.CREAR_TABLA, "usuarios",
                List.of(new ColumnaDDL("id", TipoToken.TIPO_ENTERO, 1, 23),
                        new ColumnaDDL("nombre", TipoToken.TIPO_TEXTO, 1, 34)), 1, 1));
        ddl.analizarSentencia(new SentenciaDDL(TipoSentenciaDDL.CREAR_TABLA, "productos",
                List.of(new ColumnaDDL("id", TipoToken.TIPO_ENTERO, 2, 24),
                        new ColumnaDDL("precio", TipoToken.TIPO_DECIMAL, 2, 33)), 2, 1));
        return catalogo;
    }

    private static List<ErrorCompilacion> analizar(CatalogoEsquema catalogo, String script) {
        return new SemanticaDML(catalogo).analizar(tokens(script));
    }

    private static List<Token> tokens(String script) {
        ScannerSQL scanner = new ScannerSQL();
        scanner.analizar(script);
        return scanner.getTokens();
    }

    private static boolean tieneCodigo(List<ErrorCompilacion> errores, String codigo) {
        return cuentaCodigo(errores, codigo) > 0;
    }

    private static int cuentaCodigo(List<ErrorCompilacion> errores, String codigo) {
        int cuenta = 0;
        for (ErrorCompilacion error : errores) {
            if (codigo.equals(error.getCodigo())) {
                cuenta++;
            }
        }
        return cuenta;
    }

    private static boolean tieneCantidadCeroEnPrimera(List<ErrorCompilacion> errores, String codigo) {
        for (ErrorCompilacion error : errores) {
            if (codigo.equals(error.getCodigo()) && error.getLinea() == 1) {
                return false;
            }
        }
        return true;
    }

    private static void probar(String descripcion, boolean condicion) {
        total++;
        if (condicion) {
            System.out.println("  ok    " + descripcion);
        } else {
            fallidas++;
            System.out.println("  FALLA " + descripcion);
        }
    }
}
