package lexer;

import java.util.ArrayList;
import java.util.List;

import parser.AnalizadorSintacticoDML;
import parser.ErrorSintactico;

/**
 * Mantiene la entrada historica del analizador DML y su formato de mensajes.
 * La gramatica y la recuperacion se comparten con el parser de fase 2.
 */
public class AnalizadorDML {

    /** Valida una lista de tokens producida por ScannerSQL y devuelve sus errores. */
    public List<String> analizar(List<Token> entrada) {
        List<String> errores = new ArrayList<>();
        if (entrada == null) {
            errores.add("No se recibieron tokens para analizar.");
            return errores;
        }

        for (ErrorSintactico error : new AnalizadorSintacticoDML()
                .analizar(entrada).getErrores()) {
            errores.add("Error sintactico en linea " + error.getLinea()
                    + ", columna " + error.getColumna() + ": " + error.getMensaje());
        }
        return errores;
    }
}
