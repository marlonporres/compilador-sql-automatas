import java.util.List;

import lexer.ScannerSQL;
import lexer.Token;
import parser.AnalizadorSintacticoDDL;
import parser.CursorTokens;
import parser.ResultadoSintacticoDDL;
import parser.SentenciaDDL;
import parser.TipoSentenciaDDL;

/** Pruebas del núcleo de cursor y del análisis sintáctico DDL de fase 2. */
public final class PruebaParserDDL {

    private static int total;
    private static int fallidas;

    private PruebaParserDDL() {
    }

    public static void main(String[] args) {
        cursorNoExponeLaListaInterna();
        aceptaDDLCompleto();
        rechazaEstructuraIncompleta();
        recuperaDespuesDeError();
        System.out.println("Pruebas Parser DDL: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) {
            System.exit(1);
        }
    }

    private static void cursorNoExponeLaListaInterna() {
        List<Token> tokens = tokens("CREAR TABLA t (id ENTERO);");
        CursorTokens cursor = new CursorTokens(tokens);
        probar("cursor inicia en el primer token", cursor.ver().getLexema().equals("CREAR"));
        probar("cursor coincide y avanza", cursor.coincidir(lexer.TipoToken.CREAR)
                && cursor.ver().getLexema().equals("TABLA"));
        probar("cursor llega al EOF", avanzarHastaEOF(cursor) && cursor.estaAlFinal());
        probar("copiarTokens devuelve otra lista", cursor.copiarTokens() != tokens);
    }

    private static void aceptaDDLCompleto() {
        ResultadoSintacticoDDL resultado = analizar(
                "CREAR TABLA usuarios (id ENTERO, nombre TEXTO, activo BOOLEANO);"
                        + "ALTERAR TABLA usuarios AGREGAR edad ENTERO;"
                        + "ELIMINAR TABLA usuarios;"
                        + "TRUNCAR TABLA usuarios;"
                        + "RENOMBRAR TABLA usuarios A clientes;");
        probar("acepta cinco sentencias DDL", resultado.esValido()
                && resultado.getSentencias().size() == 5);
        SentenciaDDL crear = resultado.getSentencias().get(0);
        probar("CREAR conserva tabla y columnas", crear.getTipo() == TipoSentenciaDDL.CREAR_TABLA
                && crear.getNombreTabla().equals("usuarios")
                && crear.getColumnas().size() == 3
                && crear.getColumnas().get(1).getTipoDato() == lexer.TipoToken.TIPO_TEXTO);
        probar("comandos simples conservan su tipo", resultado.getSentencias().get(1).getTipo()
                == TipoSentenciaDDL.ALTERAR_TABLA
                && resultado.getSentencias().get(4).getTipo() == TipoSentenciaDDL.RENOMBRAR_TABLA);
    }

    private static void rechazaEstructuraIncompleta() {
        ResultadoSintacticoDDL resultado = analizar("CREAR TABLA usuarios (id); ALTERAR usuarios;");
        probar("reporta errores de columna y TABLA", !resultado.esValido()
                && resultado.getErrores().size() >= 2);
        probar("los errores tienen fase y posición", resultado.getErrores().stream()
                .allMatch(error -> error.getFase().equals("SINTACTICA")
                        && error.getLinea() >= 1 && error.getColumna() >= 1));
    }

    private static void recuperaDespuesDeError() {
        ResultadoSintacticoDDL resultado = analizar(
                "CREAR TABLA usuarios (id ENTERO,);"
                        + "CREAR TABLA roles (id ENTERO);");
        probar("continúa después de una sentencia inválida", resultado.getErrores().size() == 1
                && resultado.getSentencias().size() == 1
                && resultado.getSentencias().get(0).getNombreTabla().equals("roles"));
    }

    private static ResultadoSintacticoDDL analizar(String entrada) {
        return new AnalizadorSintacticoDDL().analizar(tokens(entrada));
    }

    private static List<Token> tokens(String entrada) {
        ScannerSQL scanner = new ScannerSQL();
        scanner.analizar(entrada);
        return scanner.getTokens();
    }

    private static boolean avanzarHastaEOF(CursorTokens cursor) {
        while (!cursor.estaAlFinal()) {
            cursor.avanzar();
        }
        return cursor.ver().getTipo() == lexer.TipoToken.FIN_DE_ARCHIVO;
    }

    private static void probar(String descripcion, boolean correcta) {
        total++;
        if (!correcta) {
            fallidas++;
            System.out.println("FALLA: " + descripcion);
        }
    }
}
