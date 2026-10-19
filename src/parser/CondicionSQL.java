package parser;

import java.util.ArrayList;
import java.util.List;

import lexer.Token;

/** Árbol de condiciones: comparación, Y, O y NO; conserva tipos y posiciones. */
public final class CondicionSQL {
    private final String operador;
    private final Token izquierdo;
    private final Token derecho;
    private final CondicionSQL primera;
    private final CondicionSQL segunda;

    private CondicionSQL(String operador, Token izquierdo, Token derecho,
            CondicionSQL primera, CondicionSQL segunda) {
        this.operador = operador;
        this.izquierdo = izquierdo;
        this.derecho = derecho;
        this.primera = primera;
        this.segunda = segunda;
    }

    public static CondicionSQL comparar(Token izquierda, Token operador, Token derecha) {
        return new CondicionSQL(operador.getLexema(), izquierda, derecha, null, null);
    }

    public static CondicionSQL unir(String operador, CondicionSQL izquierda, CondicionSQL derecha) {
        return new CondicionSQL(operador, null, null, izquierda, derecha);
    }

    public static CondicionSQL negar(CondicionSQL condicion) {
        return new CondicionSQL("NO", null, null, condicion, null);
    }

    public boolean esComparacion() { return izquierdo != null; }
    public String getOperador() { return operador; }
    public Token getIzquierdo() { return izquierdo; }
    public Token getDerecho() { return derecho; }
    public CondicionSQL getPrimera() { return primera; }
    public CondicionSQL getSegunda() { return segunda; }

    public String getTexto() {
        if (esComparacion()) return texto(izquierdo) + " " + operador + " " + texto(derecho);
        if (segunda == null) return "NO (" + primera.getTexto() + ")";
        return "(" + primera.getTexto() + " " + operador + " " + segunda.getTexto() + ")";
    }

    private static String texto(Token token) {
        return token.getTipo() == lexer.TipoToken.LITERAL_CADENA
                ? "'" + token.getLexema().replace("'", "''") + "'" : token.getLexema();
    }

    public List<Token> getReferencias() {
        List<Token> referencias = new ArrayList<>();
        if (esComparacion()) {
            if (izquierdo.getTipo() == lexer.TipoToken.IDENTIFICADOR
                    && !esBooleano(izquierdo)) referencias.add(izquierdo);
            if (derecho.getTipo() == lexer.TipoToken.IDENTIFICADOR
                    && !esBooleano(derecho)) referencias.add(derecho);
        } else {
            referencias.addAll(primera.getReferencias());
            if (segunda != null) referencias.addAll(segunda.getReferencias());
        }
        return List.copyOf(referencias);
    }

    public static boolean esBooleano(Token token) {
        return token.getTipo() == lexer.TipoToken.IDENTIFICADOR
                && (token.getLexema().equalsIgnoreCase("VERDADERO")
                || token.getLexema().equalsIgnoreCase("FALSO"));
    }
}
