import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import compiler.CompiladorSQL;
import execution.BasePersistente;
import execution.InformeEjecucion;
import execution.ServicioEjecucion;

/** Bases de archivo reales, aisladas de la carpeta de datos del usuario. */
public final class PruebaPersistencia {
    private static int total;
    private static int fallidas;
    private PruebaPersistencia() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 2) {
            ServicioEjecucion servicio = new ServicioEjecucion(Path.of(args[1]));
            if (args[0].equals("crear")) {
                if (!servicio.ejecutar("CREAR TABLA alumnos (id ENTERO, nombre TEXTO); "
                        + "INSERTAR EN alumnos VALORES (1, 'Ana');").esExitoso()) System.exit(1);
            } else {
                InformeEjecucion informe = servicio.ejecutar("SELECCIONAR nombre DESDE alumnos;");
                if (!informe.esExitoso() || !informe.resultados().getFirst().filas()
                        .equals(List.of(List.of("Ana")))) System.exit(1);
            }
            System.out.println("PERSISTENCIA VERIFICADA");
            return;
        }
        Path carpeta = Files.createTempDirectory("sqlstudio persistencia ñ ");
        try {
            rutaPortable(carpeta);
            operaciones(carpeta.resolve("datos/compilador"));
            procesosSeparados(carpeta);
        } finally {
            try (var archivos = Files.walk(carpeta)) {
                for (Path archivo : archivos.sorted(Comparator.reverseOrder()).toList()) Files.delete(archivo);
            }
        }
        System.out.println("Pruebas Persistencia: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) System.exit(1);
    }

    private static void rutaPortable(Path carpeta) {
        String anterior = System.getProperty("user.home");
        try {
            System.setProperty("user.home", carpeta.toString());
            Path esperada = carpeta.resolve("SQLStudio/datos/compilador").toAbsolutePath().normalize();
            comprobar("ruta usa user.home y no la carpeta del proyecto", BasePersistente.rutaPredeterminada().equals(esperada));
            comprobar("factoría de GUI usa almacenamiento permanente", ServicioEjecucion.persistente().esPersistente());
            comprobar("ruta mostrada incluye archivo real", ServicioEjecucion.persistente().ubicacion().equals(esperada + ".mv.db"));
        } finally {
            System.setProperty("user.home", anterior);
        }
    }

    private static void operaciones(Path archivo) throws Exception {
        ServicioEjecucion servicio = new ServicioEjecucion(archivo);
        String crear = "CREAR TABLA t_a (id ENTERO, monto DECIMAL, nombre TEXTO, nacimiento FECHA, activo BOOLEANO); "
                + "CREAR TABLA tba (extra TEXTO); "
                + "INSERTAR EN t_a VALORES (1, 12.5, 'Ana', '2026-10-25', VERDADERO);";
        comprobar("crear e insertar guarda los cinco tipos", servicio.ejecutar(crear).esExitoso());
        comprobar("se crea archivo en subcarpetas con espacios y acentos", Files.isRegularFile(Path.of(archivo + ".mv.db")));
        servicio = new ServicioEjecucion(archivo);
        comprobar("otra instancia consulta datos guardados", filas(servicio, "SELECCIONAR nombre DESDE t_a;")
                .equals(List.of(List.of("Ana"))));
        var analisis = servicio.analizar("SELECCIONAR * DESDE t_a;");
        comprobar("analizar reconoce tablas guardadas sin CREAR", analisis.esValido());
        comprobar("guion bajo no mezcla columnas de otra tabla", analisis.catalogo().buscar("t_a").obtenerColumnas().size() == 5);
        comprobar("los cinco tipos recuperados permiten INSERTAR", servicio.analizar(
                "INSERTAR EN t_a VALORES (2, 3.5, 'Luis', '2024-02-29', FALSO);").esValido());
        comprobar("tipos guardados detectan asignación inválida", !servicio.analizar(
                "ACTUALIZAR t_a ASIGNAR id = 'texto';").esValido());
        comprobar("analizar simula ALTERAR", servicio.analizar("ALTERAR TABLA t_a AGREGAR correo TEXTO;").esValido());
        comprobar("analizar no agrega columnas realmente", !servicio.analizar("SELECCIONAR correo DESDE t_a;").esValido());
        comprobar("analizar simula ELIMINAR", servicio.analizar("ELIMINAR TABLA t_a;").esValido());
        comprobar("analizar no elimina tabla ni filas", filas(servicio, "SELECCIONAR nombre DESDE t_a;")
                .equals(List.of(List.of("Ana"))));
        comprobar("script inválido no aplica cambios previos", servicio.ejecutar(
                "INSERTAR EN t_a VALORES (2, 3, 'Luis', '2024-02-29', FALSO); SELECCIONAR * DESDE ausente;")
                .resultados().isEmpty());
        comprobar("fila inválida no se guardó", filas(servicio, "SELECCIONAR nombre DESDE t_a;").size() == 1);
        comprobar("crear tabla existente se rechaza", !servicio.ejecutar("CREAR TABLA t_a (id ENTERO);").esExitoso());
        comprobar("ejecutar vuelve a validar esquema real", servicio.analizar("CREAR TABLA nueva (id ENTERO);").esValido()
                && servicio.ejecutar("CREAR TABLA nueva (id ENTERO);").esExitoso()
                && !servicio.ejecutar("CREAR TABLA nueva (id ENTERO);").esExitoso());
        comprobar("comparar usa bases vacías aunque exista la misma tabla guardada", servicio.comparar(
                "CREAR TABLA t_a (otro TEXTO); INSERTAR EN t_a VALORES ('temporal'); SELECCIONAR * DESDE t_a;",
                "CREATE TABLE t_a (otro VARCHAR); INSERT INTO t_a VALUES ('temporal'); SELECT * FROM t_a;").coincide());
        comprobar("comparar no altera datos permanentes", filas(servicio, "SELECCIONAR nombre DESDE t_a;")
                .equals(List.of(List.of("Ana"))));
        comprobar("comparar no importa tablas permanentes", !servicio.comparar("SELECCIONAR * DESDE t_a;",
                "CREATE TABLE t_a (id INTEGER); SELECT * FROM t_a;").coincide());
        comprobar("ACTUALIZAR persiste", servicio.ejecutar("ACTUALIZAR t_a ASIGNAR nombre = 'María' DONDE id = 1;").esExitoso()
                && filas(new ServicioEjecucion(archivo), "SELECCIONAR nombre DESDE t_a;").equals(List.of(List.of("María"))));
        comprobar("ALTERAR y RENOMBRAR persisten", servicio.ejecutar("ALTERAR TABLA t_a AGREGAR correo TEXTO; "
                + "RENOMBRAR TABLA t_a A personas;").esExitoso()
                && new ServicioEjecucion(archivo).analizar("SELECCIONAR correo DESDE personas;").esValido());
        comprobar("BORRAR persiste", servicio.ejecutar("BORRAR DESDE personas DONDE id = 1;").esExitoso()
                && filas(new ServicioEjecucion(archivo), "SELECCIONAR * DESDE personas;").isEmpty());
        comprobar("TRUNCAR conserva esquema", servicio.ejecutar("TRUNCAR TABLA personas;").esExitoso()
                && servicio.analizar("SELECCIONAR * DESDE personas;").esValido());
        comprobar("ELIMINAR solo ocurre al ejecutarlo", servicio.ejecutar("ELIMINAR TABLA personas;").esExitoso()
                && !servicio.analizar("SELECCIONAR * DESDE personas;").esValido());
        var inicial = servicio.analizar("").catalogo();
        new CompiladorSQL().analizar("ELIMINAR TABLA tba;", inicial);
        comprobar("compilar no muta catálogo inicial compartido", inicial.buscar("tba") != null);
        String fallo = "CREAR TABLA parcial (id ENTERO); INSERTAR EN parcial VALORES (" + "9".repeat(1100) + ");";
        comprobar("fallo SQL se informa", !servicio.ejecutar(fallo).esExitoso());
        comprobar("tras fallo catálogo se recupera de la base real", servicio.analizar("SELECCIONAR * DESDE parcial;").esValido());
        Path bloqueada = archivo.getParent().resolve("archivo-no-carpeta");
        Files.writeString(bloqueada, "x");
        ServicioEjecucion inaccesible = new ServicioEjecucion(bloqueada.resolve("db"));
        comprobar("fallo de ruta devuelve error de ejecución", !inaccesible.ejecutar("CREAR TABLA t (id ENTERO);").esExitoso());
        boolean rechazo = false;
        try { inaccesible.analizar("SELECCIONAR * DESDE t;"); }
        catch (IllegalStateException e) { rechazo = e.getCause() instanceof java.sql.SQLException; }
        comprobar("fallo de lectura no se presenta como catálogo vacío", rechazo);
    }

    private static List<List<Object>> filas(ServicioEjecucion servicio, String consulta) {
        InformeEjecucion informe = servicio.ejecutar(consulta);
        if (!informe.esExitoso()) throw new IllegalStateException("Consulta fallida: " + informe);
        return informe.resultados().getFirst().filas();
    }

    private static void procesosSeparados(Path carpeta) throws Exception {
        Path archivo = carpeta.resolve("reinicio/base");
        comprobar("proceso independiente crea la base", proceso("crear", archivo, carpeta));
        comprobar("otro proceso desde otra carpeta recupera la fila", proceso("consultar", archivo, carpeta.getParent()));
    }

    private static boolean proceso(String modo, Path archivo, Path cwd) throws Exception {
        List<String> rutas = new ArrayList<>();
        for (String entrada : System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            rutas.add(Path.of(entrada).toAbsolutePath().normalize().toString());
        }
        String ejecutable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Process proceso = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", ejecutable).toString(),
                "-cp", String.join(File.pathSeparator, rutas), "PruebaPersistencia", modo, archivo.toString())
                .directory(cwd.toFile()).redirectErrorStream(true).start();
        boolean termino = proceso.waitFor(30, TimeUnit.SECONDS);
        if (!termino) { proceso.destroyForcibly(); proceso.waitFor(); }
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean correcto = termino && proceso.exitValue() == 0 && salida.contains("PERSISTENCIA VERIFICADA");
        if (!correcto) System.err.println(salida);
        return correcto;
    }

    private static void comprobar(String nombre, boolean condicion) {
        total++;
        if (!condicion) { fallidas++; System.err.println("FALLA: " + nombre); }
    }
}
