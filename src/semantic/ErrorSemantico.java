package semantic;

//Error de la parte semantica. Lleva los mismos datos que ErrorSintactico
//(fase, codigo, mensaje, elemento, linea y columna) para poder mostrarlos
//juntos en el reporte de errores
public class ErrorSemantico {

    public static final String FASE = "SEMANTICA";

    private final String codigo;
    private final String mensaje;
    private final String elemento;   //la tabla o columna del error, las columnas van como tabla.columna
    private final int linea;
    private final int columna;

    public ErrorSemantico(String codigo, String mensaje, String elemento, int linea, int columna) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.elemento = elemento == null ? "" : elemento;
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

    public String getElemento() {
        return elemento;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    @Override
    public String toString() {
        return String.format("[%s][%s] %s (elemento: \"%s\", L:%d C:%d)",
                FASE, codigo, mensaje, elemento, linea, columna);
    }
}
