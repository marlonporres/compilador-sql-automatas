import java.util.List;

import compiler.CompiladorSQL;
import compiler.ResultadoCompilacion;
import errors.FaseError;
import semantic.TipoDato;

/** Regresiones de delimitadores y recuperacion sin ejecutar sentencias invalidas. */
public final class PruebaDelimitadores {
    private static final String BASE = "CREAR TABLA t (id ENTERO, nombre TEXTO);";
    private static final String CONTINUACION = "CREAR TABLA recuperada (id ENTERO);"
            + "SELECCIONAR id DESDE recuperada;";
    private static int total;
    private static int fallidas;

    private PruebaDelimitadores() { }

    public static void main(String[] args) {
        puntosYComas();
        comas();
        parentesis();
        delimitadoresDentroDeCadenasYComentarios();
        System.out.println("Pruebas Delimitadores: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) System.exit(1);
    }

    private static void puntosYComas() {
        List<String> sinTerminador = List.of(
                "CREAR TABLA otra (id ENTERO)",
                "ALTERAR TABLA t AGREGAR edad ENTERO",
                "ELIMINAR TABLA t",
                "TRUNCAR TABLA t",
                "RENOMBRAR TABLA t A nueva",
                "SELECCIONAR * DESDE t",
                "INSERTAR EN t VALORES (1, 'Ana')",
                "ACTUALIZAR t ASIGNAR nombre = 'Ana'",
                "BORRAR DESDE t DONDE id = 1");
        for (String sql : sinTerminador) {
            ResultadoCompilacion r = analizar(BASE + sql);
            probar("punto y coma obligatorio al EOF: " + sql,
                    soloBaseValida(r) && r.errores().contar(FaseError.SINTACTICA) == 1);

            // Sin ';' no existe una frontera entre las dos sentencias iniciales.
            // La recuperacion descarta ambas hasta el siguiente ';' y continua.
            r = analizar(BASE + sql + "\nCREAR TABLA perdida (id ENTERO);" + CONTINUACION);
            probar("detecta punto y coma ausente entre sentencias: " + sql,
                    recuperacionValida(r) && r.catalogo().buscar("perdida") == null
                    && r.errores().contar(FaseError.SINTACTICA) == 1);
        }
    }

    private static void comas() {
        List<String> sinComa = List.of(
                "CREAR TABLA otra (id ENTERO nombre TEXTO);",
                "SELECCIONAR id nombre DESDE t;",
                "INSERTAR EN t (id nombre) VALORES (1, 'Ana');",
                "INSERTAR EN t VALORES (1 'Ana');",
                "ACTUALIZAR t ASIGNAR id = 1 nombre = 'Ana';");
        for (String sql : sinComa) verificarInvalidaYRecuperacion("coma ausente", sql);
    }

    private static void parentesis() {
        List<String> invalidas = List.of(
                "CREAR TABLA otra id ENTERO);",
                "CREAR TABLA otra (id ENTERO;",
                "CREAR TABLA otra (id ENTERO));",
                "INSERTAR EN t (id, nombre VALORES (1, 'Ana');",
                "INSERTAR EN t (id, nombre)) VALORES (1, 'Ana');",
                "INSERTAR EN t VALORES 1, 'Ana');",
                "INSERTAR EN t VALORES (1, 'Ana';",
                "INSERTAR EN t VALORES (1, 'Ana'));",
                "SELECCIONAR * DESDE t DONDE (id = 1;",
                "SELECCIONAR * DESDE t DONDE ((id = 1) Y id > 0;",
                "SELECCIONAR * DESDE t DONDE id = 1);",
                "SELECCIONAR * DESDE t DONDE ();");
        for (String sql : invalidas) verificarInvalidaYRecuperacion("parentesis invalido", sql);
    }

    private static void delimitadoresDentroDeCadenasYComentarios() {
        ResultadoCompilacion r = analizar(BASE
                + "INSERTAR EN t VALORES (1, 'texto; con, (parentesis)');"
                + "SELECCIONAR nombre DESDE t;");
        probar("delimitadores dentro de una cadena no separan sentencias",
                r.esValido() && r.sentencias().size() == 3 && r.sentenciasValidas() == 3
                && baseConservada(r));

        r = analizar(BASE + "/* ; , ( ) */ INSERTAR EN t VALORES (1, 'Ana');"
                + "-- ; , ( )\nSELECCIONAR * DESDE t DONDE ((id = 1) O NO (id > 2));");
        probar("comentarios y parentesis anidados validos no generan falsos errores",
                r.esValido() && r.sentencias().size() == 3 && r.sentenciasValidas() == 3
                && baseConservada(r));
    }

    private static void verificarInvalidaYRecuperacion(String tipo, String sql) {
        ResultadoCompilacion r = analizar(BASE + sql);
        probar(tipo + " no llega a semantica: " + sql, soloBaseValida(r));
        r = analizar(BASE + sql + CONTINUACION);
        probar(tipo + " permite continuar tras ';': " + sql, recuperacionValida(r));
    }

    private static boolean soloBaseValida(ResultadoCompilacion r) {
        return diagnosticoSintacticoAislado(r) && r.sentencias().size() == 1
                && r.sentenciasValidas() == 1 && r.catalogo().tamano() == 1
                && baseConservada(r);
    }

    private static boolean recuperacionValida(ResultadoCompilacion r) {
        return diagnosticoSintacticoAislado(r) && r.sentencias().size() == 3
                && r.sentenciasValidas() == 3 && r.catalogo().tamano() == 2
                && r.catalogo().buscar("recuperada") != null
                && r.catalogo().buscar("recuperada").obtenerColumnas().size() == 1
                && baseConservada(r);
    }

    private static boolean diagnosticoSintacticoAislado(ResultadoCompilacion r) {
        return r.errores().contar(FaseError.SINTACTICA) > 0
                && r.errores().contar(FaseError.LEXICA) == 0
                && r.errores().contar(FaseError.SEMANTICA) == 0;
    }

    private static boolean baseConservada(ResultadoCompilacion r) {
        var tabla = r.catalogo().buscar("t");
        return tabla != null && tabla.obtenerColumnas().size() == 2
                && tabla.buscarColumna("id") != null
                && tabla.buscarColumna("id").tipo() == TipoDato.ENTERO
                && tabla.buscarColumna("nombre") != null
                && tabla.buscarColumna("nombre").tipo() == TipoDato.TEXTO;
    }

    private static ResultadoCompilacion analizar(String sql) {
        return new CompiladorSQL().analizar(sql);
    }

    private static void probar(String nombre, boolean condicion) {
        total++;
        if (!condicion) {
            fallidas++;
            System.out.println("FALLA: " + nombre);
        }
    }
}
