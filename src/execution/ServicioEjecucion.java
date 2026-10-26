package execution;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import compiler.CompiladorSQL;
import compiler.ResultadoCompilacion;
import parser.SentenciaSQL;

/** Ejecuta contra almacenamiento local o bases temporales para pruebas y comparación. */
public final class ServicioEjecucion {
    private record Estado(Map<String, List<String>> esquema,
            Map<String, Map<List<Object>, Long>> datos) { }
    private record Ejecucion(InformeEjecucion informe, Estado estado) { }
    private record Referencia(List<ResultadoEjecucion> resultados, Estado estado) { }

    private final Path archivo;

    /** Las pruebas y la CLI conservan sus bases temporales independientes. */
    public ServicioEjecucion() { archivo = null; }

    public ServicioEjecucion(Path archivo) {
        this.archivo = java.util.Objects.requireNonNull(archivo).toAbsolutePath().normalize();
    }

    public static ServicioEjecucion persistente() {
        return new ServicioEjecucion(BasePersistente.rutaPredeterminada());
    }

    public boolean esPersistente() { return archivo != null; }
    public String ubicacion() { return esPersistente() ? archivo + ".mv.db" : "Base temporal de demostración"; }

    public ResultadoCompilacion analizar(String fuente) {
        if (!esPersistente()) return new CompiladorSQL().analizar(fuente == null ? "" : fuente);
        try (Connection conexion = BasePersistente.abrir(archivo)) {
            return analizar(fuente, conexion, true);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer la base guardada: " + e.getMessage(), e);
        }
    }

    private static ResultadoCompilacion analizar(String fuente, Connection conexion, boolean persistente)
            throws SQLException {
        String texto = fuente == null ? "" : fuente;
        return persistente ? new CompiladorSQL().analizar(texto, BasePersistente.leerCatalogo(conexion))
                : new CompiladorSQL().analizar(texto);
    }

    public InformeEjecucion ejecutar(String fuente) { return ejecutarCompleto(fuente, esPersistente()).informe(); }

    private Ejecucion ejecutarCompleto(String fuente, boolean persistente) {
        long inicio = System.nanoTime();
        ResultadoCompilacion analisis = null;
        List<ResultadoEjecucion> resultados = new ArrayList<>();
        Estado estado = null;
        try (Connection conexion = persistente ? BasePersistente.abrir(archivo) : abrirBase()) {
            // Revalidar desde el esquema real evita ejecutar un análisis obsoleto.
            analisis = analizar(fuente, conexion, persistente);
            if (analisis.esValido() && !analisis.sentencias().isEmpty()) {
                int numero = 0;
                for (SentenciaSQL sentencia : analisis.sentencias()) {
                    numero++;
                    ResultadoEjecucion resultado;
                    try {
                        TraductorSQL.Plan plan = new TraductorSQL().traducir(sentencia);
                        resultado = ejecutarSentencia(conexion, numero,
                                plan.comando(), sentencia.getLinea(), sentencia.getColumna(),
                                plan.sql(), plan.parametros());
                    } catch (IllegalArgumentException e) {
                        resultado = fallo(numero, "TRADUCCIÓN", sentencia.getLinea(), sentencia.getColumna(), "", e);
                    }
                    resultados.add(resultado);
                    if (!resultado.esExitoso()) break;
                }
                if (!persistente && resultados.stream().allMatch(ResultadoEjecucion::esExitoso)) estado = estado(conexion);
            }
        } catch (SQLException | IllegalArgumentException e) {
            if (analisis == null) analisis = new CompiladorSQL().analizar(fuente == null ? "" : fuente);
            resultados.add(fallo(resultados.size() + 1, "EJECUCIÓN", 1, 1, "", e));
        }
        return new Ejecucion(new InformeEjecucion(analisis, resultados,
                (System.nanoTime() - inicio) / 1_000_000), estado);
    }

    public ResultadoComparacion comparar(String fuente, String sqlReferencia) {
        Ejecucion propio = ejecutarCompleto(fuente, false);
        List<String> detalles = new ArrayList<>();
        if (!propio.informe().esExitoso()) {
            detalles.add("El código en español debe analizarse y ejecutarse sin errores antes de comparar.");
            return new ResultadoComparacion(false, detalles, propio.informe(), List.of());
        }
        Referencia referencia = ejecutarReferencia(sqlReferencia == null ? "" : sqlReferencia);
        if (referencia.resultados().isEmpty()
                || referencia.resultados().stream().anyMatch(r -> !r.esExitoso())) {
            detalles.add("El SQL de referencia está vacío o contiene errores; la comparación no es válida.");
            referencia.resultados().stream().filter(r -> !r.esExitoso()).forEach(r ->
                    detalles.add("SQL de referencia, línea " + r.linea() + ", columna " + r.columna()
                            + ": " + r.error()));
        } else {
            List<ResultadoEjecucion> consultas = propio.informe().resultados().stream()
                    .filter(ResultadoEjecucion::esConsulta).toList();
            List<ResultadoEjecucion> esperadas = referencia.resultados().stream()
                    .filter(ResultadoEjecucion::esConsulta).toList();
            if (consultas.size() != esperadas.size()) {
                detalles.add("Cantidad diferente de consultas: " + consultas.size() + " frente a " + esperadas.size() + ".");
            }
            for (int i = 0; i < Math.min(consultas.size(), esperadas.size()); i++) {
                ResultadoEjecucion a = consultas.get(i);
                ResultadoEjecucion b = esperadas.get(i);
                if (!nombres(a.columnas()).equals(nombres(b.columnas()))) {
                    detalles.add("Consulta " + (i + 1) + ": las columnas son diferentes.");
                }
                if (!bolsa(a.filas()).equals(bolsa(b.filas()))) {
                    detalles.add("Consulta " + (i + 1) + ": las filas o sus repeticiones son diferentes.");
                }
            }
            if (propio.estado() == null || referencia.estado() == null
                    || !propio.estado().esquema().equals(referencia.estado().esquema())) {
                detalles.add("El esquema final de tablas y columnas es diferente.");
            }
            if (propio.estado() == null || referencia.estado() == null
                    || !propio.estado().datos().equals(referencia.estado().datos())) {
                detalles.add("Los datos finales almacenados son diferentes.");
            }
        }
        boolean coincide = detalles.isEmpty();
        if (coincide) detalles.add("Coinciden las consultas, sus filas y el esquema y los datos finales."
                + " Se ignora el orden de las filas y se conservan sus repeticiones.");
        return new ResultadoComparacion(coincide, detalles, propio.informe(), referencia.resultados());
    }

    private static Connection abrirBase() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:fase3_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=0");
    }

    private static ResultadoEjecucion ejecutarSentencia(Connection conexion, int numero,
            String comando, int linea, int columna, String sql, List<Object> parametros) {
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.size(); i++) consulta.setObject(i + 1, parametros.get(i));
            if (consulta.execute()) {
                try (ResultSet filas = consulta.getResultSet()) {
                    ResultSetMetaData meta = filas.getMetaData();
                    List<String> columnas = new ArrayList<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) columnas.add(meta.getColumnLabel(i));
                    List<List<Object>> datos = new ArrayList<>();
                    while (filas.next()) {
                        List<Object> fila = new ArrayList<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            Object valor = filas.getObject(i);
                            fila.add(valor instanceof java.sql.Date fecha ? fecha.toLocalDate() : valor);
                        }
                        datos.add(fila);
                    }
                    return new ResultadoEjecucion(numero, comando, linea, columna, sql,
                            columnas, datos, -1, "");
                }
            }
            return new ResultadoEjecucion(numero, comando, linea, columna, sql, List.of(),
                    List.of(), Math.max(0, consulta.getUpdateCount()), "");
        } catch (SQLException e) { return fallo(numero, comando, linea, columna, sql, e); }
    }

    private static ResultadoEjecucion fallo(int numero, String comando, int linea,
            int columna, String sql, Exception error) {
        String mensaje = error.getMessage();
        if (mensaje == null || mensaje.isBlank()) mensaje = error.getClass().getSimpleName();
        return new ResultadoEjecucion(numero, comando, linea, columna, sql, List.of(), List.of(),
                0, "EXEC-001: " + mensaje);
    }

    private static Estado estado(Connection conexion) throws SQLException {
        Map<String, List<String>> esquema = new LinkedHashMap<>();
        Map<String, Map<List<Object>, Long>> datos = new LinkedHashMap<>();
        DatabaseMetaData meta = conexion.getMetaData();
        try (ResultSet tablas = meta.getTables(null, "PUBLIC", "%", new String[] { "TABLE" })) {
            while (tablas.next()) {
                String tabla = tablas.getString("TABLE_NAME");
                String clave = tabla.toLowerCase(Locale.ROOT);
                List<String> columnas = new ArrayList<>();
                // JDBC interpreta '_' y '%' como comodines en tableNamePattern.
                // Una tabla t_a no debe incorporar también las columnas de tba.
                String escape = meta.getSearchStringEscape();
                String tablaExacta = tabla.replace(escape, escape + escape)
                        .replace("_", escape + "_").replace("%", escape + "%");
                try (ResultSet campos = meta.getColumns(null, "PUBLIC", tablaExacta, "%")) {
                    while (campos.next()) columnas.add(campos.getString("COLUMN_NAME").toLowerCase(Locale.ROOT)
                            + ":" + (campos.getString("TYPE_NAME").equalsIgnoreCase("DECFLOAT") ? "DECIMAL"
                                    : familia(campos.getInt("DATA_TYPE"), campos.getInt("DECIMAL_DIGITS"))));
                }
                esquema.put(clave, List.copyOf(columnas));
                String identificador = "\"" + tabla.replace("\"", "\"\"") + "\"";
                ResultadoEjecucion contenido = ejecutarSentencia(conexion, 0, "ESTADO", 0, 0,
                        "SELECT * FROM " + identificador, List.of());
                if (!contenido.esExitoso()) throw new SQLException(contenido.error());
                datos.put(clave, bolsa(contenido.filas()));
            }
        }
        return new Estado(Map.copyOf(esquema), Map.copyOf(datos));
    }

    private static String familia(int tipo, int escala) {
        return switch (tipo) {
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT -> "ENTERO";
            case Types.NUMERIC, Types.DECIMAL -> escala == 0 ? "ENTERO" : "DECIMAL";
            case Types.FLOAT, Types.REAL, Types.DOUBLE -> "DECIMAL";
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.CLOB -> "TEXTO";
            case Types.BOOLEAN, Types.BIT -> "BOOLEANO";
            case Types.DATE -> "FECHA";
            default -> Integer.toString(tipo);
        };
    }

    private static List<String> nombres(List<String> columnas) {
        return columnas.stream().map(c -> c.toLowerCase(Locale.ROOT)).toList();
    }

    private static Map<List<Object>, Long> bolsa(List<List<Object>> filas) {
        Map<List<Object>, Long> resultado = new HashMap<>();
        for (List<Object> fila : filas) {
            List<Object> normalizada = new ArrayList<>();
            for (Object valor : fila) {
                if (valor instanceof Number numero) {
                    normalizada.add(new BigDecimal(numero.toString()).stripTrailingZeros());
                } else if (valor instanceof java.sql.Date fecha) normalizada.add(fecha.toLocalDate());
                else normalizada.add(valor);
            }
            resultado.merge(normalizada, 1L, Long::sum);
        }
        return resultado;
    }

    private static Referencia ejecutarReferencia(String fuente) {
        List<ResultadoEjecucion> resultados = new ArrayList<>();
        Estado estado = null;
        try {
            List<ScriptReferencia.Fragmento> sentencias = ScriptReferencia.separar(fuente);
            for (ScriptReferencia.Fragmento sentencia : sentencias) ScriptReferencia.validar(sentencia.sql());
            if (sentencias.isEmpty()) return new Referencia(List.of(), null);
            try (Connection conexion = abrirBase()) {
                int numero = 0;
                for (ScriptReferencia.Fragmento sentencia : sentencias) {
                    ResultadoEjecucion resultado = ejecutarSentencia(conexion, ++numero, "SQL DE REFERENCIA",
                            sentencia.linea(), sentencia.columna(), sentencia.sql(), List.of());
                    resultados.add(resultado);
                    if (!resultado.esExitoso()) break;
                }
                if (resultados.stream().allMatch(ResultadoEjecucion::esExitoso)) estado = estado(conexion);
            }
        } catch (SQLException | IllegalArgumentException e) {
            resultados.add(fallo(resultados.size() + 1, "SQL DE REFERENCIA", 1, 1, "", e));
        }
        return new Referencia(List.copyOf(resultados), estado);
    }
}
