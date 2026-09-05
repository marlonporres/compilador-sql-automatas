package lexer;

/** Resultado léxico inmutable con la posición inicial del lexema, desde línea y columna 1. */
public class Token {
    private final TipoToken tipo;
    private final String lexema;
    private final int linea;
    private final int columna;

    public Token(TipoToken tipo, String lexema, int linea, int columna) {
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

    public int getlinea() {
        return linea;
    }

    public int getcolumna() {
        return columna;
    }

    @Override
    public String toString() {
        return String.format("<%-15s | Lexema: %-15s | L:%-3d C:%-3d>", tipo, "\"" + lexema + "\"", linea, columna);
    }

}
