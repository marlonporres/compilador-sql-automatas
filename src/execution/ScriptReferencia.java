package execution;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Delimita SQL básico respetando cadenas, identificadores y comentarios. */
final class ScriptReferencia {
    record Fragmento(String sql, int linea, int columna) { }
    private static final String ID = "[\\p{L}_][\\p{L}_0-9]*";
    private static final String TIPO = "(?:INT(?:EGER)?|BIGINT|SMALLINT|TINYINT|TEXT|VARCHAR|CHARACTER\\s+VARYING"
            + "|DECIMAL|NUMERIC|DECFLOAT|BOOLEAN|DATE)(?:\\s*\\(\\s*\\d+\\s*(?:,\\s*\\d+\\s*)?\\))?";
    private ScriptReferencia() { }

    static List<Fragmento> separar(String fuente) {
        List<Fragmento> resultado = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean cadena = false;
        boolean identificador = false;
        boolean comentarioLinea = false;
        boolean comentarioBloque = false;
        boolean contenido = false;
        int linea = 1;
        int columna = 1;
        int inicioLinea = 1;
        int inicioColumna = 1;
        for (int i = 0; i < fuente.length(); i++) {
            char c = fuente.charAt(i);
            if (i == 0 && c == '\uFEFF') continue;
            char siguiente = i + 1 < fuente.length() ? fuente.charAt(i + 1) : '\0';
            if (comentarioLinea) {
                if (c == '\n' || c == '\r') { comentarioLinea = false; actual.append(' '); }
            } else if (comentarioBloque) {
                if (c == '*' && siguiente == '/') { comentarioBloque = false; i++; columna++; actual.append(' '); }
            } else if (!cadena && !identificador && c == '-' && siguiente == '-') {
                comentarioLinea = true; i++; columna++;
            } else if (!cadena && !identificador && c == '/' && siguiente == '*') {
                comentarioBloque = true; i++; columna++;
            } else if (!cadena && !identificador && c == ';') {
                if (contenido) resultado.add(new Fragmento(actual.toString().strip(), inicioLinea, inicioColumna));
                actual.setLength(0);
                contenido = false;
            } else {
                if (!contenido && !Character.isWhitespace(c)) {
                    inicioLinea = linea; inicioColumna = columna;
                    contenido = true;
                }
                actual.append(c);
                if (c == '\'' && !identificador) {
                    if (cadena && siguiente == '\'') { actual.append(siguiente); i++; columna++; }
                    else cadena = !cadena;
                } else if (c == '"' && !cadena) {
                    if (identificador && siguiente == '"') { actual.append(siguiente); i++; columna++; }
                    else identificador = !identificador;
                }
            }
            if (c == '\r') { linea++; columna = 1; }
            else if (c == '\n') {
                if (i == 0 || fuente.charAt(i - 1) != '\r') linea++;
                columna = 1;
            } else columna++;
        }
        if (cadena || identificador || comentarioBloque) throw new IllegalArgumentException("SQL de referencia: cadena, identificador o comentario sin cerrar.");
        if (contenido) throw new IllegalArgumentException("SQL de referencia: falta ';' al final de la sentencia.");
        return List.copyOf(resultado);
    }

    static void validar(String sql) {
        // Enmascarar valores impide interpretar sus palabras como comandos.
        String simple = enmascarar(sql).strip();
        String columna = ID + "\\s+" + TIPO;
        String nombres = ID + "(?:\\s*,\\s*" + ID + ")*";
        String literal = "(?:\\?|TRUE|FALSE|NULL|[+-]?\\d+(?:\\.\\d+)?)";
        String condicion = "(?:\\s+WHERE\\s+[\\w\\s?().,<>=!+*-]+)?";
        String orden = "(?:\\s+ORDER\\s+BY\\s+" + ID + "(?:\\s+(?:ASC|DESC))?(?:\\s*,\\s*"
                + ID + "(?:\\s+(?:ASC|DESC))?)*)?";
        boolean valido = coincide(simple, "CREATE\\s+TABLE\\s+" + ID + "\\s*\\(\\s*" + columna + "(?:\\s*,\\s*" + columna + ")*\\s*\\)")
                || coincide(simple, "DROP\\s+TABLE\\s+" + ID)
                || coincide(simple, "TRUNCATE\\s+TABLE\\s+" + ID)
                || coincide(simple, "ALTER\\s+TABLE\\s+" + ID + "\\s+(?:ADD(?:\\s+COLUMN)?\\s+" + columna
                        + "|ALTER(?:\\s+COLUMN)?\\s+" + ID + "\\s+(?:SET\\s+DATA\\s+TYPE\\s+)?" + TIPO
                        + "|DROP(?:\\s+COLUMN)?\\s+" + ID + "|RENAME\\s+TO\\s+" + ID + ")")
                || coincide(simple, "INSERT\\s+INTO\\s+" + ID + "(?:\\s*\\(\\s*" + nombres + "\\s*\\))?\\s+VALUES\\s*\\(\\s*"
                        + literal + "(?:\\s*,\\s*" + literal + ")*\\s*\\)")
                || coincide(simple, "SELECT\\s+(?:\\*|" + nombres + ")\\s+FROM\\s+" + ID + condicion + orden)
                || coincide(simple, "DELETE\\s+FROM\\s+" + ID + condicion)
                || coincide(simple, "UPDATE\\s+" + ID + "\\s+SET\\s+" + ID + "\\s*=\\s*" + literal
                        + "(?:\\s*,\\s*" + ID + "\\s*=\\s*" + literal + ")*" + condicion);
        if (!valido || Pattern.compile("(?i)\\b(?:CALL|RUNSCRIPT|BACKUP|SCRIPT|SHUTDOWN|UNION|JOIN|EXISTS|SELECT)\\b")
                .matcher(simple.replaceFirst("(?i)^SELECT\\b", "").replaceFirst("(?i)(ALTER.*)SET DATA TYPE", "$1")).find()
                || Pattern.compile("(?i)\\b(?!AND\\b|OR\\b|NOT\\b|VALUES\\b|TABLE\\b|INTO\\b)" + ID + "\\s*\\(")
                        .matcher(simple.replaceFirst("(?i)^CREATE\\s+TABLE\\s+" + ID, "CREATE TABLE ")
                                .replaceFirst("(?i)^INSERT\\s+INTO\\s+" + ID, "INSERT INTO ")
                                .replaceAll("(?i)\\b(?:NUMERIC|DECIMAL|DECFLOAT|VARCHAR|VARYING)\\s*\\([^)]*\\)", "TIPO")).find()) {
            throw new IllegalArgumentException("SQL de referencia: solo se admiten CREATE/ALTER/DROP/TRUNCATE TABLE, INSERT VALUES, SELECT, UPDATE y DELETE básicos.");
        }
    }

    private static boolean coincide(String texto, String patron) {
        return Pattern.compile(patron, Pattern.CASE_INSENSITIVE | Pattern.DOTALL
                | Pattern.UNICODE_CHARACTER_CLASS).matcher(texto).matches();
    }

    private static String enmascarar(String sql) {
        StringBuilder simple = new StringBuilder();
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c != '\'' && c != '"') { simple.append(c); continue; }
            simple.append(c == '\'' ? "?" : "identificador_citado");
            char cierre = c;
            for (i++; i < sql.length(); i++) {
                if (sql.charAt(i) != cierre) continue;
                if (i + 1 < sql.length() && sql.charAt(i + 1) == cierre) i++;
                else break;
            }
        }
        return simple.toString();
    }
}
