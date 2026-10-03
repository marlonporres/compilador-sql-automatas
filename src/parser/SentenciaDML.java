package parser;

import java.util.List;
import lexer.Token;

/** Sentencia DML inmutable; los tokens guardan posiciones para la semántica. */
public final class SentenciaDML implements SentenciaSQL {
    private final TipoSentenciaDML tipo;
    private final Token tabla;
    private final List<Token> columnas;
    private final boolean todasLasColumnas;
    private final List<Token> columnasInsertar;
    private final List<ValorSQL> valores;
    private final List<AsignacionSQL> asignaciones;
    private final CondicionSQL condicion;
    private final int linea;
    private final int columna;

    public SentenciaDML(TipoSentenciaDML tipo, Token tabla,
            List<Token> columnas, boolean todasLasColumnas,
            List<Token> columnasInsertar, List<ValorSQL> valores,
            List<AsignacionSQL> asignaciones, CondicionSQL condicion,
            int linea, int columna) {
        this.tipo = tipo;
        this.tabla = tabla;
        this.columnas = List.copyOf(columnas);
        this.todasLasColumnas = todasLasColumnas;
        this.columnasInsertar = List.copyOf(columnasInsertar);
        this.valores = List.copyOf(valores);
        this.asignaciones = List.copyOf(asignaciones);
        this.condicion = condicion;
        this.linea = linea;
        this.columna = columna;
    }

    public TipoSentenciaDML getTipo() { return tipo; }
    public Token getTabla() { return tabla; }
    @Override public String getNombreTabla() { return tabla.getLexema(); }
    public List<Token> getColumnas() { return columnas; }
    public boolean usaTodasLasColumnas() { return todasLasColumnas; }
    public List<Token> getColumnasInsertar() { return columnasInsertar; }
    public List<ValorSQL> getValores() { return valores; }
    public List<AsignacionSQL> getAsignaciones() { return asignaciones; }
    public CondicionSQL getCondicion() { return condicion; }
    @Override public int getLinea() { return linea; }
    @Override public int getColumna() { return columna; }
    @Override public String toString() {
        String contenido = switch (tipo) {
            case SELECCIONAR -> (todasLasColumnas ? "*" : nombres(columnas))
                    + " DESDE " + getNombreTabla();
            case INSERTAR -> "EN " + getNombreTabla()
                    + (columnasInsertar.isEmpty() ? "" : " (" + nombres(columnasInsertar) + ")")
                    + " VALORES " + valores;
            case ACTUALIZAR -> getNombreTabla() + " ASIGNAR " + asignaciones;
            case BORRAR -> "DESDE " + getNombreTabla();
        };
        return tipo + " " + contenido
                + (condicion == null ? "" : " DONDE " + condicion.getTexto());
    }
    private static String nombres(List<Token> lista) {
        return String.join(", ", lista.stream().map(Token::getLexema).toList());
    }
}
