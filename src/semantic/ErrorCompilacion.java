package semantic;

/** Diagnostico comun del compilador. */
public final class ErrorCompilacion {
    private final String fase;
    private final String codigo;
    private final String mensaje;
    private final String elemento;
    private final int linea;
    private final int columna;

    public ErrorCompilacion(String fase, String codigo, String mensaje,
            String elemento, int linea, int columna) {
        this.fase = fase == null ? "" : fase;
        this.codigo = codigo == null ? "" : codigo;
        this.mensaje = mensaje == null ? "" : mensaje;
        this.elemento = elemento == null ? "" : elemento;
        this.linea = linea;
        this.columna = columna;
    }

    public String getFase() { return fase; }
    public String getCodigo() { return codigo; }
    public String getMensaje() { return mensaje; }
    public String getElemento() { return elemento; }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }

    @Override
    public String toString() {
        return String.format("[%s][%s] %s (elemento: \"%s\", L:%d C:%d)",
                fase, codigo, mensaje, elemento, linea, columna);
    }
}
