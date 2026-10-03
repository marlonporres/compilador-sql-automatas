package semantic;

import java.util.Objects;

/** Diagnóstico común para las fases léxica, sintáctica y semántica. */
public final class ErrorCompilacion {
    private final String fase;
    private final String codigo;
    private final String mensaje;
    private final String elemento;
    private final int linea;
    private final int columna;

    public ErrorCompilacion(String fase, String codigo, String mensaje,
            String elemento, int linea, int columna) {
        this.fase = fase == null ? "DESCONOCIDA" : fase;
        this.codigo = Objects.requireNonNullElse(codigo, "GEN-000");
        this.mensaje = Objects.requireNonNullElse(mensaje, "");
        this.elemento = Objects.requireNonNullElse(elemento, "");
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
