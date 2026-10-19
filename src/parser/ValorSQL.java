package parser;

import lexer.TipoToken;

/** Literal que aparece en una sentencia DML. */
public final class ValorSQL {

    private final TipoToken tipo;
    private final String lexema;
    private final int linea;
    private final int columna;

    public ValorSQL(TipoToken tipo, String lexema, int linea, int columna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
    }

    public TipoToken getTipo() {
        return tipo;
    }

    public String getLexema() {
        return lexema;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    @Override
    public String toString() {
        return tipo == TipoToken.LITERAL_CADENA ? "'" + lexema.replace("'", "''") + "'" : lexema;
    }
}
