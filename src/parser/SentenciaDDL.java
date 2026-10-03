package parser;

import java.util.List;

import lexer.TipoToken;
import lexer.Token;

/**
 * Representación inmutable de una sentencia DDL aceptada sintácticamente.
 * La semántica (existencia de tablas, duplicados y tipos) se valida después.
 */
public final class SentenciaDDL implements SentenciaSQL {

    private final TipoSentenciaDDL tipo;
    private final String nombreTabla;
    private final List<ColumnaDDL> columnas;
    private final int linea;
    private final int columna;
    private final Token tabla;
    private final String operacion;
    private final Token nuevoNombre;

    public SentenciaDDL(TipoSentenciaDDL tipo, String nombreTabla,
            List<ColumnaDDL> columnas, int linea, int columna) {
        this(tipo, new Token(TipoToken.IDENTIFICADOR, nombreTabla, linea, columna),
                columnas, linea, columna, "", null);
    }

    public SentenciaDDL(TipoSentenciaDDL tipo, Token tabla, List<ColumnaDDL> columnas,
            int linea, int columna, String operacion, Token nuevoNombre) {
        String nombreTabla = tabla.getLexema();
        if (tipo == null || nombreTabla == null || nombreTabla.isBlank()) {
            throw new IllegalArgumentException("Una sentencia DDL necesita tipo y tabla");
        }
        this.tipo = tipo;
        this.nombreTabla = nombreTabla;
        this.columnas = List.copyOf(columnas == null ? List.of() : columnas);
        this.linea = linea;
        this.columna = columna;
        this.tabla = tabla;
        this.operacion = operacion;
        this.nuevoNombre = nuevoNombre;
    }

    public Token getTabla() { return tabla; }
    public String getOperacion() { return operacion; }
    public Token getNuevoNombre() { return nuevoNombre; }

    public TipoSentenciaDDL getTipo() {
        return tipo;
    }

    public String getNombreTabla() {
        return nombreTabla;
    }

    public List<ColumnaDDL> getColumnas() {
        return columnas;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    @Override
    public String toString() {
        return tipo + " " + nombreTabla
                + (operacion.isEmpty() ? "" : " " + operacion)
                + (columnas.isEmpty() ? "" : " " + columnas)
                + (nuevoNombre == null ? "" : (tipo == TipoSentenciaDDL.RENOMBRAR_TABLA
                        ? " A " : " ") + nuevoNombre.getLexema());
    }
}
