package parser;

import lexer.Token;

/** Asignación con la posición exacta de la columna y del literal. */
public final class AsignacionSQL {
    private final Token columna;
    private final ValorSQL valor;

    public AsignacionSQL(Token columna, ValorSQL valor) {
        this.columna = columna;
        this.valor = valor;
    }

    public String getNombreColumna() { return columna.getLexema(); }
    public Token getColumna() { return columna; }
    public ValorSQL getValor() { return valor; }
    @Override public String toString() { return getNombreColumna() + " = " + valor; }
}
