package parser;

/** Diagnóstico estructurado producido por el analizador sintáctico. */
public final class ErrorSintactico {

    public static final String FASE = "SINTACTICA";

    private final String codigo;
    private final String mensaje;
    private final String lexema;
    private final int linea;
    private final int columna;

    public ErrorSintactico(String codigo, String mensaje, String lexema, int linea, int columna) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.lexema = lexema == null ? "" : lexema;
        this.linea = linea;
        this.columna = columna;
    }

    public String getFase() {
        return FASE;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getMensaje() {
        return mensaje;
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
        return String.format("[%s][%s] %s (lexema: \"%s\", L:%d C:%d)",
                FASE, codigo, mensaje, lexema, linea, columna);
    }
}
