package execution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import semantic.CatalogoSemantico;
import semantic.TipoDato;

/** Almacenamiento local compartido por las sesiones de SQL Studio. */
public final class BasePersistente {
    private BasePersistente() { }

    /** Ruta independiente de la carpeta de lanzamiento, en Windows, macOS y Linux. */
    public static Path rutaPredeterminada() {
        return Path.of(System.getProperty("user.home"), "SQLStudio", "datos", "compilador")
                .toAbsolutePath().normalize();
    }

    public static Connection abrir(Path archivo) throws SQLException {
        Path ruta = archivo.toAbsolutePath().normalize();
        if (ruta.toString().contains(";")) {
            throw new SQLException("La ruta de la base no puede contener punto y coma: " + ruta);
        }
        try {
            Files.createDirectories(ruta.getParent());
        } catch (IOException e) {
            throw new SQLException("No se pudo crear la carpeta de datos: " + ruta.getParent(), e);
        }
        return DriverManager.getConnection("jdbc:h2:file:" + ruta.toString().replace('\\', '/'), "sa", "");
    }

    /** Solo lee metadatos: analizar no ejecuta DDL ni modifica filas. */
    public static CatalogoSemantico leerCatalogo(Connection conexion) throws SQLException {
        CatalogoSemantico catalogo = new CatalogoSemantico();
        DatabaseMetaData meta = conexion.getMetaData();
        try (ResultSet tablas = meta.getTables(null, "PUBLIC", "%", new String[] {"TABLE"})) {
            while (tablas.next()) {
                String nombre = tablas.getString("TABLE_NAME");
                String escape = meta.getSearchStringEscape();
                String patron = nombre.replace(escape, escape + escape)
                        .replace("_", escape + "_").replace("%", escape + "%");
                List<CatalogoSemantico.Columna> columnas = new ArrayList<>();
                try (ResultSet campos = meta.getColumns(null, "PUBLIC", patron, "%")) {
                    while (campos.next()) {
                        columnas.add(new CatalogoSemantico.Columna(campos.getString("COLUMN_NAME"),
                                tipo(campos), 1, 1));
                    }
                }
                catalogo.incorporarTabla(nombre, columnas);
            }
        }
        return catalogo;
    }

    private static TipoDato tipo(ResultSet columna) throws SQLException {
        if ("DECFLOAT".equalsIgnoreCase(columna.getString("TYPE_NAME"))) return TipoDato.DECIMAL;
        return switch (columna.getInt("DATA_TYPE")) {
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT -> TipoDato.ENTERO;
            case Types.NUMERIC, Types.DECIMAL -> columna.getInt("DECIMAL_DIGITS") == 0
                    ? TipoDato.ENTERO : TipoDato.DECIMAL;
            case Types.FLOAT, Types.REAL, Types.DOUBLE -> TipoDato.DECIMAL;
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.CLOB -> TipoDato.TEXTO;
            case Types.DATE -> TipoDato.FECHA;
            case Types.BOOLEAN, Types.BIT -> TipoDato.BOOLEANO;
            default -> throw new SQLException("Tipo no soportado en " + columna.getString("TABLE_NAME")
                    + "." + columna.getString("COLUMN_NAME") + ": " + columna.getString("TYPE_NAME"));
        };
    }
}
