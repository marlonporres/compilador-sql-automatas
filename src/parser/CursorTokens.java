package parser;

import java.util.ArrayList;
import java.util.List;

import lexer.TipoToken;
import lexer.Token;

/**
 * Cursor seguro para recorrer el flujo de tokens producido por ScannerSQL.
 *
 * <p>El cursor no modifica la lista recibida y siempre puede consultar un token
 * de fin de archivo. Eso permite que los analizadores DDL y DML compartan el
 * mismo contrato sin depender de índices ni de una implementación concreta de
 * la lista.</p>
 */
public class CursorTokens {

    private final List<Token> tokens;
    private int posicion;

    public CursorTokens(List<Token> tokens) {
        this.tokens = tokens == null ? List.of() : List.copyOf(tokens);
        this.posicion = 0;
    }

    public boolean estaAlFinal() {
        return posicion >= tokens.size() || ver().getTipo() == TipoToken.FIN_DE_ARCHIVO;
    }

    public boolean haySiguiente() {
        return !estaAlFinal();
    }

    public int posicion() {
        return posicion;
    }

    public Token ver() {
        return ver(0);
    }

    public Token ver(int desplazamiento) {
        int indice = posicion + desplazamiento;
        if (indice < 0 || indice >= tokens.size()) {
            return tokenEOF();
        }
        return tokens.get(indice);
    }

    public Token avanzar() {
        Token actual = ver();
        if (!estaAlFinal()) {
            posicion++;
        }
        return actual;
    }

    public boolean comprobar(TipoToken tipo) {
        return tipo != null && ver().getTipo() == tipo;
    }

    public boolean coincidir(TipoToken tipo) {
        if (comprobar(tipo)) {
            avanzar();
            return true;
        }
        return false;
    }

    public List<Token> copiarTokens() {
        return new ArrayList<>(tokens);
    }

    /** Consume tokens hasta el siguiente punto y coma o el fin de archivo. */
    public void recuperarHastaFinDeSentencia() {
        while (!estaAlFinal() && !coincidir(TipoToken.PUNTO_Y_COMA)) {
            avanzar();
        }
    }

    private Token tokenEOF() {
        if (!tokens.isEmpty()) {
            Token ultimo = tokens.get(tokens.size() - 1);
            if (ultimo.getTipo() == TipoToken.FIN_DE_ARCHIVO) return ultimo;
            return new Token(TipoToken.FIN_DE_ARCHIVO, "EOF", ultimo.getlinea(),
                    ultimo.getcolumna() + ultimo.getLexema().length());
        }
        return new Token(TipoToken.FIN_DE_ARCHIVO, "EOF", 1, 1);
    }
}
