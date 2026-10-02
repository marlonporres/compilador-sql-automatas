package parser;

import java.util.List;

/**
 * Representación inmutable de una sentencia DDL aceptada sintácticamente.
 * La semántica (existencia de tablas, duplicados y tipos) se valida después.
 */
public final class SentenciaDDL {

    private final TipoSentenciaDDL tipo;
    private final String nombreTabla;
    private final List<ColumnaDDL> columnas;
    private final int linea;
    private final int columna;

    public SentenciaDDL(TipoSentenciaDDL tipo, String nombreTabla,
            List<ColumnaDDL> columnas, int linea, int columna) {
        if (tipo == null || nombreTabla == null || nombreTabla.isBlank()) {
            throw new IllegalArgumentException("Una sentencia DDL necesita tipo y tabla");
        }
        this.tipo = tipo;
        this.nombreTabla = nombreTabla;
        this.columnas = List.copyOf(columnas == null ? List.of() : columnas);
        this.linea = linea;
        this.columna = columna;
    }

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
                + (columnas.isEmpty() ? "" : " " + columnas);
    }
}
