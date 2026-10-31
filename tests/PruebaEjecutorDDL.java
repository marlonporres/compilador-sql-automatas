import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import compiler.CompiladorSQL;
import compiler.ResultadoCompilacion;
import execution.ConexionSQL;
import execution.EjecutorDDL;
import execution.EjecutorDML;
import execution.InformeEjecucion;
import execution.ResultadoEjecucion;
import execution.ServicioEjecucion;
import parser.SentenciaDDL;
import parser.SentenciaDML;
import parser.SentenciaSQL;

/* Pruebas de la conexion y de la ejecucion DDL de la fase 3 (Persona 3).
 * Los scripts pasan primero por el CompiladorSQL y despues se ejecutan en H2,
 * despues de cada sentencia reviso en los metadatos de la base que el cambio
 * de verdad se hizo. El DML de los casos lo corre el EjecutorDML de la
 * Persona 4 con la misma conexion.
 *
 * desde la raiz del proyecto (en Linux/Mac el ; del classpath va con :)
 * javac --release 21 -encoding UTF-8 -cp "lib/*" -d out @fuentes.txt    (fuentes.txt = todos los .java)
 * java -cp "out;lib/*" PruebaEjecutorDDL
 */
public class PruebaEjecutorDDL {

    private static int total = 0;
    private static int fallidas = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("PRUEBAS DE LA EJECUCION DDL");
        System.out.println();

        conexion();
        crearTabla();
        alterarTabla();
        renombrarTabla();
        truncarTabla();
        eliminarTabla();
        errorDeLaBase();
        demoFase3();

        System.out.println();
        System.out.println("Pruebas: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) {
            System.exit(1);
        }
    }

    private static void conexion() throws SQLException {
        probar("esta el controlador de H2", ConexionSQL.hayControlador());
        try (Connection c = ConexionSQL.abrir(); Connection otra = ConexionSQL.abrir()) {
            probar("abre la conexion", c.isValid(2));
            probar("el controlador es la version de lib/",
                    c.getMetaData().getDriverVersion().startsWith(ConexionSQL.VERSION_H2));
            probar("la base empieza sin tablas", tablas(c).isEmpty());
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE \"solo\" (\"id\" INTEGER)");
            }
            probar("cada abrir() es una base aparte", existeTabla(c, "solo") && !existeTabla(otra, "solo"));
        }
    }

    private static void crearTabla() throws SQLException {
        List<SentenciaSQL> s = analizar(
                "CREAR TABLA usuarios (id ENTERO, nombre TEXTO, salario DECIMAL, nacimiento FECHA, activo BOOLEANO);\n"
                + "  CREAR TABLA pedidos (id ENTERO);");
        try (Connection c = ConexionSQL.abrir()) {
            ResultadoEjecucion r = ejecutar(c, 1, s.get(0));
            probar("CREAR sale bien", r.esExitoso());
            probar("el comando es CREAR TABLA", r.comando().equals("CREAR TABLA"));
            probar("guarda el SQL generado", r.sql().startsWith("CREATE TABLE \"usuarios\""));
            probar("un DDL no trae filas", !r.esConsulta() && r.filasAfectadas() == 0);
            probar("la tabla existe en H2", existeTabla(c, "usuarios"));
            probar("columnas en el mismo orden",
                    columnas(c, "usuarios").equals(List.of("id", "nombre", "salario", "nacimiento", "activo")));
            probar("los cinco tipos quedan como se acordo", tipo(c, "usuarios", "id").equals("NUMERIC")
                    && tipo(c, "usuarios", "salario").equals("DECFLOAT")
                    && tipo(c, "usuarios", "nombre").equals("CHARACTER VARYING")
                    && tipo(c, "usuarios", "nacimiento").equals("DATE")
                    && tipo(c, "usuarios", "activo").equals("BOOLEAN"));

            ResultadoEjecucion segunda = ejecutar(c, 2, s.get(1));
            probar("guarda numero, linea y columna de la sentencia", segunda.numero() == 2
                    && segunda.linea() == 2 && segunda.columna() == 3);
        }
    }

    private static void alterarTabla() throws SQLException {
        List<SentenciaSQL> s = analizar("CREAR TABLA usuarios (id ENTERO, nombre TEXTO);"
                + " ALTERAR TABLA usuarios AGREGAR correo TEXTO;"
                + " ALTERAR TABLA usuarios MODIFICAR id DECIMAL;"
                + " ALTERAR TABLA usuarios QUITAR correo;");
        try (Connection c = ConexionSQL.abrir()) {
            ejecutar(c, 1, s.get(0));

            ResultadoEjecucion agregar = ejecutar(c, 2, s.get(1));
            probar("ALTERAR AGREGAR sale bien", agregar.esExitoso() && agregar.comando().equals("ALTERAR TABLA"));
            probar("la columna nueva queda al final", columnas(c, "usuarios").equals(List.of("id", "nombre", "correo")));

            ResultadoEjecucion modificar = ejecutar(c, 3, s.get(2));
            probar("ALTERAR MODIFICAR sale bien", modificar.esExitoso());
            probar("id cambio de ENTERO a DECIMAL", tipo(c, "usuarios", "id").equals("DECFLOAT"));

            ResultadoEjecucion quitar = ejecutar(c, 4, s.get(3));
            probar("ALTERAR QUITAR sale bien", quitar.esExitoso());
            probar("la columna ya no esta", columnas(c, "usuarios").equals(List.of("id", "nombre")));
        }
    }

    private static void renombrarTabla() throws SQLException {
        List<SentenciaSQL> s = analizar("CREAR TABLA usuarios (id ENTERO, nombre TEXTO);"
                + " INSERTAR EN usuarios VALORES (1, 'Ana');"
                + " INSERTAR EN usuarios VALORES (2, 'Luis');"
                + " RENOMBRAR TABLA usuarios A clientes;"
                + " SELECCIONAR * DESDE clientes;");
        try (Connection c = ConexionSQL.abrir()) {
            ejecutar(c, 1, s.get(0));
            ejecutar(c, 2, s.get(1));
            ejecutar(c, 3, s.get(2));

            ResultadoEjecucion renombrar = ejecutar(c, 4, s.get(3));
            probar("RENOMBRAR sale bien", renombrar.esExitoso() && renombrar.comando().equals("RENOMBRAR TABLA"));
            probar("el nombre viejo ya no existe", !existeTabla(c, "usuarios"));
            probar("el nombre nuevo si existe", existeTabla(c, "clientes"));
            probar("las filas se quedan al renombrar", filas(c, "clientes") == 2);

            ResultadoEjecucion select = ejecutar(c, 5, s.get(4));
            probar("el DML usa la misma conexion despues del DDL", select.esExitoso() && select.filas().size() == 2);
        }
    }

    private static void truncarTabla() throws SQLException {
        List<SentenciaSQL> s = analizar("CREAR TABLA usuarios (id ENTERO, nombre TEXTO);"
                + " INSERTAR EN usuarios VALORES (1, 'Ana');"
                + " INSERTAR EN usuarios VALORES (2, 'Luis');"
                + " TRUNCAR TABLA usuarios;");
        try (Connection c = ConexionSQL.abrir()) {
            ejecutar(c, 1, s.get(0));
            ejecutar(c, 2, s.get(1));
            ejecutar(c, 3, s.get(2));
            probar("antes de TRUNCAR hay 2 filas", filas(c, "usuarios") == 2);

            ResultadoEjecucion truncar = ejecutar(c, 4, s.get(3));
            probar("TRUNCAR sale bien", truncar.esExitoso() && truncar.comando().equals("TRUNCAR TABLA"));
            probar("TRUNCAR deja la tabla vacia", filas(c, "usuarios") == 0);
            probar("pero la tabla y sus columnas siguen", columnas(c, "usuarios").equals(List.of("id", "nombre")));
        }
    }

    private static void eliminarTabla() throws SQLException {
        List<SentenciaSQL> s = analizar("CREAR TABLA usuarios (id ENTERO);"
                + " CREAR TABLA pedidos (id ENTERO);"
                + " ELIMINAR TABLA usuarios;");
        try (Connection c = ConexionSQL.abrir()) {
            ejecutar(c, 1, s.get(0));
            ejecutar(c, 2, s.get(1));

            ResultadoEjecucion eliminar = ejecutar(c, 3, s.get(2));
            probar("ELIMINAR sale bien", eliminar.esExitoso() && eliminar.comando().equals("ELIMINAR TABLA"));
            probar("la tabla ya no existe", !existeTabla(c, "usuarios"));
            probar("las otras tablas no se tocan", existeTabla(c, "pedidos"));
        }
    }

    private static void errorDeLaBase() throws SQLException {
        //el compilador empieza con catalogo vacio y acepta este CREAR,
        //pero en la base la tabla ya existe y H2 lo rechaza
        List<SentenciaSQL> s = analizar("\n\n   CREAR TABLA usuarios (id ENTERO);");
        Connection c = ConexionSQL.abrir();
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE \"usuarios\" (\"id\" INTEGER)");
        }
        ResultadoEjecucion repetida = ejecutar(c, 1, s.get(0));
        probar("si H2 falla no sale exitoso", !repetida.esExitoso());
        probar("el error lleva su codigo", repetida.error().startsWith("EXEC-DDL-001"));
        probar("el error dice la linea y columna", repetida.linea() == 3 && repetida.columna() == 4);
        probar("se guarda el SQL que fallo", repetida.sql().startsWith("CREATE TABLE"));

        //con la conexion cerrada tampoco se cae, devuelve el error
        c.close();
        ResultadoEjecucion cerrada = ejecutar(c, 1, s.get(0));
        probar("con la conexion cerrada devuelve error y no se cae", !cerrada.esExitoso());
    }

    private static void demoFase3() throws Exception {
        String texto = Files.readString(Path.of("tests", "demo_fase3.sql"));
        List<SentenciaSQL> s = analizar(texto);
        List<ResultadoEjecucion> resultados = new ArrayList<>();
        Set<String> comandosDDL = new LinkedHashSet<>();
        try (Connection c = ConexionSQL.abrir()) {
            for (int i = 0; i < s.size(); i++) {
                ResultadoEjecucion r = ejecutar(c, i + 1, s.get(i));
                resultados.add(r);
                if (s.get(i) instanceof SentenciaDDL) {
                    comandosDDL.add(r.comando());
                }
                if (!r.esExitoso()) {
                    System.out.println("  se detuvo en la sentencia " + r.numero() + ": " + r.error());
                    break;
                }
            }
            probar("demo_fase3.sql corre completo", resultados.size() == s.size()
                    && resultados.stream().allMatch(ResultadoEjecucion::esExitoso));
            probar("la demo usa los cinco DDL", comandosDDL.size() == 5);
            probar("al final solo queda la tabla clientes", tablas(c).equals(List.of("clientes")));
        }

        //el ServicioEjecucion que ya esta en main debe armar el mismo SQL que mi ejecutor
        InformeEjecucion informe = new ServicioEjecucion().ejecutar(texto);
        boolean igual = informe.resultados().size() == resultados.size();
        for (int i = 0; igual && i < resultados.size(); i++) {
            if (s.get(i) instanceof SentenciaDDL) {
                igual = informe.resultados().get(i).sql().equals(resultados.get(i).sql());
            }
        }
        probar("mismo SQL DDL que el ServicioEjecucion actual", igual);
    }

    //---- ayudas ----

    //analiza el script entero una vez y devuelve las sentencias en orden
    private static List<SentenciaSQL> analizar(String script) {
        ResultadoCompilacion r = new CompiladorSQL().analizar(script);
        if (!r.esValido()) {
            System.out.println("  OJO el script tiene errores: " + r.errores().obtenerTodos());
        }
        return r.sentencias();
    }

    //cada sentencia va al ejecutor que le toca, DDL al mio y DML al de la Persona 4
    private static ResultadoEjecucion ejecutar(Connection c, int numero, SentenciaSQL sentencia) {
        if (sentencia instanceof SentenciaDDL ddl) {
            return new EjecutorDDL().ejecutar(c, numero, ddl);
        }
        return new EjecutorDML().ejecutar(c, numero, (SentenciaDML) sentencia);
    }

    private static List<String> tablas(Connection c) throws SQLException {
        List<String> nombres = new ArrayList<>();
        try (ResultSet rs = c.getMetaData().getTables(null, "PUBLIC", "%", new String[] {"TABLE"})) {
            while (rs.next()) {
                nombres.add(rs.getString("TABLE_NAME"));
            }
        }
        return nombres;
    }

    private static boolean existeTabla(Connection c, String tabla) throws SQLException {
        return tablas(c).contains(tabla);
    }

    private static List<String> columnas(Connection c, String tabla) throws SQLException {
        List<String> nombres = new ArrayList<>();
        DatabaseMetaData meta = c.getMetaData();
        try (ResultSet rs = meta.getColumns(null, "PUBLIC", tabla, "%")) {
            while (rs.next()) {
                nombres.add(rs.getString("COLUMN_NAME"));
            }
        }
        return nombres;
    }

    private static String tipo(Connection c, String tabla, String columna) throws SQLException {
        try (ResultSet rs = c.getMetaData().getColumns(null, "PUBLIC", tabla, columna)) {
            return rs.next() ? rs.getString("TYPE_NAME") : "";
        }
    }

    private static int filas(Connection c, String tabla) throws SQLException {
        try (Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM \"" + tabla + "\"")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static void probar(String descripcion, boolean condicion) {
        total++;
        if (condicion) {
            System.out.println("  ok    " + descripcion);
        } else {
            fallidas++;
            System.out.println("  FALLA " + descripcion);
        }
    }
}
