import java.util.List;

import lexer.ScannerSQL;
import lexer.Token;
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
