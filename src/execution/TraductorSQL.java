package execution;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lexer.TipoToken;
import lexer.Token;
import parser.ColumnaDDL;
import parser.CondicionSQL;
import parser.SentenciaDDL;
import parser.SentenciaDML;
import parser.SentenciaSQL;
import parser.ValorSQL;

/** Traduce el árbol validado; nunca sustituye palabras dentro del texto fuente. */
final class TraductorSQL {
    record Plan(String sql, List<Object> parametros, String comando) { }
    private final List<Object> parametros = new ArrayList<>();

    Plan traducir(SentenciaSQL sentencia) {
        parametros.clear();
        String sql;
        String comando;
        if (sentencia instanceof SentenciaDDL ddl) {
            comando = ddl.getTipo().name().replace('_', ' ');
            sql = ddl(ddl);
        } else if (sentencia instanceof SentenciaDML dml) {
            comando = dml.getTipo().name();
            sql = dml(dml);
        } else throw new IllegalArgumentException("Sentencia no soportada");
        return new Plan(sql, List.copyOf(parametros), comando);
    }

    static String identificador(String nombre) {
        return "\"" + nombre.toLowerCase(Locale.ROOT).replace("\"", "\"\"") + "\"";
    }

    private static String columna(ColumnaDDL columna) {
        return identificador(columna.getNombre()) + " " + tipo(columna.getTipoDato());
    }

    private static String tipo(TipoToken tipo) {
        return switch (tipo) {
            case TIPO_ENTERO -> "NUMERIC(1000,0)";
            case TIPO_DECIMAL -> "DECFLOAT(1000)";
            case TIPO_TEXTO -> "CHARACTER VARYING";
            case TIPO_FECHA -> "DATE";
            case TIPO_BOOLEANO -> "BOOLEAN";
            default -> throw new IllegalArgumentException("Tipo no soportado: " + tipo);
        };
    }

    private static String ddl(SentenciaDDL sentencia) {
        String tabla = identificador(sentencia.getNombreTabla());
        return switch (sentencia.getTipo()) {
            case CREAR_TABLA -> "CREATE TABLE " + tabla + " ("
                    + String.join(", ", sentencia.getColumnas().stream()
                            .map(TraductorSQL::columna).toList()) + ")";
            case ELIMINAR_TABLA -> "DROP TABLE " + tabla;
            case TRUNCAR_TABLA -> "TRUNCATE TABLE " + tabla;
            case RENOMBRAR_TABLA -> "ALTER TABLE " + tabla + " RENAME TO "
                    + identificador(sentencia.getNuevoNombre().getLexema());
            case ALTERAR_TABLA -> "ALTER TABLE " + tabla + " "
                    + switch (sentencia.getOperacion()) {
                        case "AGREGAR" -> "ADD COLUMN " + columna(sentencia.getColumnas().getFirst());
                        case "MODIFICAR" -> "ALTER COLUMN " + columna(sentencia.getColumnas().getFirst());
                        case "QUITAR" -> "DROP COLUMN "
                                + identificador(sentencia.getNuevoNombre().getLexema());
                        default -> throw new IllegalArgumentException("ALTERAR no soportado");
                    };
        };
    }

    private String dml(SentenciaDML sentencia) {
        String tabla = identificador(sentencia.getNombreTabla());
        String sql = switch (sentencia.getTipo()) {
            case SELECCIONAR -> "SELECT " + (sentencia.usaTodasLasColumnas() ? "*"
                    : nombres(sentencia.getColumnas())) + " FROM " + tabla;
            case INSERTAR -> "INSERT INTO " + tabla
                    + (sentencia.getColumnasInsertar().isEmpty() ? ""
                            : " (" + nombres(sentencia.getColumnasInsertar()) + ")")
                    + " VALUES (" + String.join(", ", sentencia.getValores().stream()
                            .map(this::valor).toList()) + ")";
            case ACTUALIZAR -> "UPDATE " + tabla + " SET "
                    + String.join(", ", sentencia.getAsignaciones().stream()
                            .map(a -> identificador(a.getNombreColumna()) + " = " + valor(a.getValor()))
                            .toList());
            case BORRAR -> "DELETE FROM " + tabla;
        };
        if (sentencia.getCondicion() != null) sql += " WHERE " + condicion(sentencia.getCondicion());
        return sql;
    }

    private static String nombres(List<Token> tokens) {
        return String.join(", ", tokens.stream().map(t -> identificador(t.getLexema())).toList());
    }

    private String valor(ValorSQL valor) { return parametro(valor.getTipo(), valor.getLexema()); }

    private String operando(Token token) {
        if (token.getTipo() == TipoToken.IDENTIFICADOR && !CondicionSQL.esBooleano(token)) {
            return identificador(token.getLexema());
        }
        return parametro(token.getTipo(), token.getLexema());
    }

    private String parametro(TipoToken tipo, String lexema) {
        Object valor = switch (tipo) {
            case LITERAL_ENTERO, LITERAL_DECIMAL -> numero(lexema);
            case IDENTIFICADOR -> lexema.equalsIgnoreCase("VERDADERO");
            case LITERAL_CADENA -> lexema;
            default -> throw new IllegalArgumentException("Literal no soportado: " + tipo);
        };
        parametros.add(valor);
        return "?";
    }

    private static BigDecimal numero(String lexema) {
        BigDecimal numero = new BigDecimal(lexema);
        if (numero.precision() > 1000) {
            throw new IllegalArgumentException("El motor admite hasta 1000 dígitos significativos;"
                    + " se detiene la ejecución para evitar redondeos silenciosos.");
        }
        return numero;
    }

    private String condicion(CondicionSQL condicion) {
        if (condicion.esComparacion()) {
            return "(" + operando(condicion.getIzquierdo()) + " " + condicion.getOperador()
                    + " " + operando(condicion.getDerecho()) + ")";
        }
        if (condicion.getSegunda() == null) return "(NOT " + condicion(condicion.getPrimera()) + ")";
        String operador = condicion.getOperador().equals("Y") ? "AND" : "OR";
        return "(" + condicion(condicion.getPrimera()) + " " + operador + " "
                + condicion(condicion.getSegunda()) + ")";
    }
}
