package execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Resultado de una sentencia; las celdas nulas se conservan como SQL NULL. */
public record ResultadoEjecucion(int numero, String comando, int linea, int columna,
        String sql, List<String> columnas, List<List<Object>> filas,
        int filasAfectadas, String error) {
    public ResultadoEjecucion {
        columnas = List.copyOf(columnas);
        List<List<Object>> copia = new ArrayList<>();
        for (List<Object> fila : filas) {
            copia.add(Collections.unmodifiableList(new ArrayList<>(fila)));
        }
        filas = Collections.unmodifiableList(copia);
        error = error == null ? "" : error;
    }
    public boolean esConsulta() { return !columnas.isEmpty(); }
    public boolean esExitoso() { return error.isBlank(); }
}
