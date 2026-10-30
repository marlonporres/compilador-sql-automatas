package execution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import parser.SentenciaDML;

/** Ejecuta sentencias DML ya validadas usando la conexión compartida de la fase 3. */
public final class EjecutorDML {
    /** No abre ni cierra la conexión: su ciclo de vida pertenece al servicio común. */
    public ResultadoEjecucion ejecutar(Connection conexion, int numero, SentenciaDML sentencia) {
        Objects.requireNonNull(conexion, "conexion");
        Objects.requireNonNull(sentencia, "sentencia");
        String comando = sentencia.getTipo().name();
        String sql = "";
        try {
            TraductorSQL.Plan plan = new TraductorSQL().traducir(sentencia);
            sql = plan.sql();
            try (PreparedStatement preparada = conexion.prepareStatement(sql)) {
                for (int i = 0; i < plan.parametros().size(); i++) {
                    preparada.setObject(i + 1, plan.parametros().get(i));
                }
                if (preparada.execute()) {
                    try (ResultSet rs = preparada.getResultSet()) {
                        ResultSetMetaData meta = rs.getMetaData();
                        List<String> columnas = new ArrayList<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) columnas.add(meta.getColumnLabel(i));
                        List<List<Object>> filas = new ArrayList<>();
                        while (rs.next()) {
                            List<Object> fila = new ArrayList<>();
                            for (int i = 1; i <= meta.getColumnCount(); i++) {
                                Object valor = rs.getObject(i);
                                fila.add(valor instanceof java.sql.Date fecha ? fecha.toLocalDate() : valor);
                            }
                            filas.add(fila);
                        }
                        return new ResultadoEjecucion(numero, comando, sentencia.getLinea(),
                                sentencia.getColumna(), sql, columnas, filas, -1, "");
                    }
                }
                return new ResultadoEjecucion(numero, comando, sentencia.getLinea(),
                        sentencia.getColumna(), sql, List.of(), List.of(),
                        Math.max(0, preparada.getUpdateCount()), "");
            }
        } catch (SQLException | IllegalArgumentException ex) {
            return new ResultadoEjecucion(numero, comando, sentencia.getLinea(),
                    sentencia.getColumna(), sql, List.of(), List.of(), 0,
                    "EXEC-DML-001: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
    }
}
