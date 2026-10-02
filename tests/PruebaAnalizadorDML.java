import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import lexer.AnalizadorDML;
import lexer.ScannerSQL;
import lexer.Token;

public class PruebaAnalizadorDML {
    private static int total;
    private static int fallidas;

    public static void main(String[] args) throws Exception {
        String archivo = Files.readString(Path.of("tests/test_dml.sql"), StandardCharsets.UTF_8);
        int corte = archivo.indexOf("-- PRUEBAS DE ERRORES LEXICOS");
        comprobar("se encontro la seccion DML valida", corte >= 0);
        comprobar("se aceptan las sentencias DML del ejemplo", analizar(archivo.substring(0, corte)).isEmpty());

        validar("SELECCIONAR id, nombre DESDE usuarios DONDE id >= 1 Y nombre != 'Ana';");
        validar("INSERTAR EN usuarios (id, nombre) VALORES (1, 'Ana');");
        validar("ACTUALIZAR usuarios ASIGNAR nombre = 'Luis', id = 2 DONDE id = 1;");
        validar("BORRAR DESDE usuarios DONDE id = 2 O id = 3;");

        invalido("SELECCIONAR DESDE usuarios;");
        invalido("INSERTAR EN usuarios VALORES (1,);");
        invalido("ACTUALIZAR usuarios ASIGNAR nombre DONDE id = 1;");
        invalido("BORRAR usuarios;");

        List<String> errores = analizar("SELECCIONAR * DESDE;");
        comprobar("el error incluye linea y columna", errores.size() == 1
                && errores.getFirst().contains("linea 1, columna"));
        System.out.println("Pruebas Analizador DML: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) {
            System.exit(1);
        }
    }

    private static void validar(String entrada) {
        comprobar("valido: " + entrada, analizar(entrada).isEmpty());
    }

    private static void invalido(String entrada) {
        comprobar("invalido: " + entrada, !analizar(entrada).isEmpty());
    }

    private static List<String> analizar(String entrada) {
        ScannerSQL scanner = new ScannerSQL();
        scanner.analizar(entrada);
        List<Token> tokens = scanner.getTokens();
        return new AnalizadorDML().analizar(tokens);
    }

    private static void comprobar(String descripcion, boolean condicion) {
        total++;
        if (!condicion) {
            fallidas++;
            System.out.println("FALLA: " + descripcion);
        }
    }
}