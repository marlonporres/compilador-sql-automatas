package parser;

import java.util.ArrayList;
import java.util.List;

import errors.Diagnostico;
import errors.FaseError;
import lexer.TipoToken;
import lexer.Token;

/** Dispatcher que analiza un script con sentencias DDL y DML mezcladas. */
public final class AnalizadorSintactico {

    public ResultadoSintactico analizar(List<Token> tokens) {
        List<SentenciaSQL> sentencias = new ArrayList<>();
        List<Diagnostico> errores = new ArrayList<>();
        List<Token> actual = new ArrayList<>();

        if (tokens != null) {
            for (Token token : tokens) {
                if (token.getTipo() == TipoToken.FIN_DE_ARCHIVO) {
                    if (!actual.isEmpty()) {
                        actual.add(token);
                        analizarSentencia(actual, sentencias, errores);
                        actual.clear();
                    }
                    break;
                }
                actual.add(token);
                if (token.getTipo() == TipoToken.PUNTO_Y_COMA) {
                    analizarSentencia(actual, sentencias, errores);
                    actual.clear();
                }
            }
        }
        if (!actual.isEmpty()) analizarSentencia(actual, sentencias, errores);

        return new ResultadoSintactico(sentencias, errores);
    }

    private void analizarSentencia(List<Token> tokens, List<SentenciaSQL> sentencias,
            List<Diagnostico> errores) {
        if (tokens.isEmpty()) {
            return;
        }
        // La tabla común ya registra los errores léxicos. Una sentencia con
        // ERROR se descarta completa para evitar diagnósticos en cascada.
        if (tokens.stream().anyMatch(token -> token.getTipo() == TipoToken.ERROR)) return;
        List<Token> entrada = new ArrayList<>(tokens);
        Token ultimo = entrada.get(entrada.size() - 1);
        if (ultimo.getTipo() != TipoToken.FIN_DE_ARCHIVO) {
            entrada.add(new Token(TipoToken.FIN_DE_ARCHIVO, "EOF",
                    ultimo.getlinea(), ultimo.getcolumna() + ultimo.getLexema().length()));
        }

        TipoToken inicio = entrada.get(0).getTipo();
        if (esDDL(inicio)) {
            ResultadoSintacticoDDL resultado = new AnalizadorSintacticoDDL().analizar(entrada);
            sentencias.addAll(resultado.getSentencias());
            resultado.getErrores().forEach(error -> errores.add(convertir(error)));
        } else if (esDML(inicio)) {
            ResultadoSintacticoDML resultado = new AnalizadorSintacticoDML().analizar(entrada);
            sentencias.addAll(resultado.getSentencias());
            resultado.getErrores().forEach(error -> errores.add(convertir(error)));
        } else {
            Token token = entrada.get(0);
            errores.add(new Diagnostico(FaseError.SINTACTICA, "SYN-000",
                    "Se esperaba una sentencia DDL o DML", token.getLexema(),
                    token.getlinea(), token.getcolumna()));
        }
    }

    private static Diagnostico convertir(ErrorSintactico error) {
        return new Diagnostico(FaseError.SINTACTICA, error.getCodigo(),
                error.getMensaje(), error.getLexema(), error.getLinea(), error.getColumna());
    }

    private static boolean esDDL(TipoToken tipo) {
        return tipo == TipoToken.CREAR || tipo == TipoToken.ALTERAR
                || tipo == TipoToken.ELIMINAR || tipo == TipoToken.TRUNCAR
                || tipo == TipoToken.RENOMBRAR;
    }

    private static boolean esDML(TipoToken tipo) {
        return tipo == TipoToken.SELECCIONAR || tipo == TipoToken.INSERTAR
                || tipo == TipoToken.ACTUALIZAR || tipo == TipoToken.BORRAR;
    }
}
