import compiler.CompiladorSQL;
import execution.EjecutorDML;
import execution.ResultadoEjecucion;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import parser.SentenciaDML;

/** Prueba independiente del módulo de Persona 4, sin modificar clases compartidas. */
public class PruebaEjecutorDML {
    private static int verificaciones;
    private static void comprobar(boolean condicion, String detalle) {
        verificaciones++;
        if (!condicion) throw new AssertionError(detalle);
    }
    private static SentenciaDML dml(String texto) {
        var analisis = new CompiladorSQL().analizar(texto);
        comprobar(analisis.esValido(), "No se pudo analizar: " + texto + " " + analisis.errores().obtenerTodos());
        return (SentenciaDML) analisis.sentencias().getLast();
    }
    public static void main(String[] args) throws Exception {
        try (Connection conexion = DriverManager.getConnection("jdbc:h2:mem:persona4_prueba;DB_CLOSE_DELAY=-1")) {
            try (Statement ddl = conexion.createStatement()) {
                ddl.execute("CREATE TABLE \"personas\" (\"id\" INTEGER, \"nombre\" VARCHAR(80))");
            }
            EjecutorDML ejecutor = new EjecutorDML();
            String crear = "CREAR TABLA personas (id ENTERO, nombre TEXTO); ";
            ResultadoEjecucion insertar = ejecutor.ejecutar(conexion, 1,
                    dml(crear + "INSERTAR EN personas (id, nombre) VALORES (1, 'Ana');"));
            comprobar(insertar.esExitoso() && insertar.filasAfectadas() == 1, "INSERTAR");
            ResultadoEjecucion seleccionar = ejecutor.ejecutar(conexion, 2,
                    dml(crear + "SELECCIONAR id, nombre DESDE personas DONDE id = 1;"));
            comprobar(seleccionar.esExitoso() && seleccionar.filas().size() == 1, "SELECCIONAR");
            comprobar(seleccionar.columnas().size() == 2, "Columnas del SELECT");
            ResultadoEjecucion actualizar = ejecutor.ejecutar(conexion, 3,
                    dml(crear + "ACTUALIZAR personas ASIGNAR nombre = 'Eva' DONDE id = 1;"));
            comprobar(actualizar.esExitoso() && actualizar.filasAfectadas() == 1, "ACTUALIZAR");
            ResultadoEjecucion borrar = ejecutor.ejecutar(conexion, 4,
                    dml(crear + "BORRAR DESDE personas DONDE id = 1;"));
            comprobar(borrar.esExitoso() && borrar.filasAfectadas() == 1, "BORRAR");
            comprobar(ejecutor.ejecutar(conexion, 5,
                    dml(crear + "SELECCIONAR * DESDE personas;")).filas().isEmpty(), "Tabla vacía");
        }
        System.out.println("Persona 4: " + verificaciones + " verificaciones correctas.");
    }
}
