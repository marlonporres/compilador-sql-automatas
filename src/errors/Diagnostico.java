package errors;

/** Diagnóstico uniforme para las tres fases del compilador. */
public final class Diagnostico {

    private final FaseError fase;
    private final String codigo;
    private final String mensaje;
    private final String lexema;
    private final int linea;
    private final int columna;

    public Diagnostico(FaseError fase, String codigo, String mensaje,
            String lexema, int linea, int columna) {
        this.fase = fase;
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.lexema = lexema == null ? "" : lexema;
        this.linea = linea;
        this.columna = columna;
    }

    public FaseError getFase() {
        return fase;
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
        return String.format("[%s][%s] %s | Lexema: \"%s\" | L:%d C:%d",
                fase, codigo, mensaje, lexema, linea, columna);
    }
}
