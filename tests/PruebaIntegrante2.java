import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import compiler.CompiladorSQL;
import compiler.ResultadoCompilacion;
import execution.ResultadoComparacion;
import execution.ServicioEjecucion;
import ui.Estilo;
import ui.PanelesAnalisis;

/** Contrato del integrante 2 con datos reales del compilador y del ejecutor. */
public final class PruebaIntegrante2 {
    private static int total;
    private static int fallidas;
    private PruebaIntegrante2() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        String valido = leer("demo_fase2_valido.sql");
        String errores = leer("demo_fase2_errores.sql");
        SwingUtilities.invokeAndWait(() -> {
            Estilo.instalar();
            contratoDeDatos(valido, errores);
            reemplazoYAmbitos();
            posicionesYOrden();
            delimitadores();
        });
        pruebasCruzadas(valido, leer("demo_fase2_referencia.sql"), errores);
        System.out.println("Pruebas Integrante 2: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) System.exit(1);
    }

    private static void contratoDeDatos(String valido, String errores) {
        PanelesAnalisis panel = new PanelesAnalisis();
        ResultadoCompilacion resultado = new CompiladorSQL().analizar(valido);
        panel.mostrar(resultado);
        comprobar("ejemplo válido: 15 sentencias sin errores",
                resultado.esValido() && resultado.sentenciasValidas() == 15);
        comprobar("tokens conservan tipo, lexema y posición exactos", tokensIguales(panel, resultado));
        comprobar("identificadores conservan todos los datos léxicos", lexicosIguales(panel, resultado));
        comprobar("catálogo final: clientes y sus cinco columnas", panel.getTablaSimbolos().getRowCount() == 6
                && "clientes".equals(panel.getTablaSimbolos().getValueAt(0, 1)));
        for (JTable tabla : tablas(panel)) {
            comprobar("tabla de solo lectura: " + tabla.getAccessibleContext().getAccessibleName(),
                    !tabla.getModel().isCellEditable(0, 0));
        }
        resultado = new CompiladorSQL().analizar(errores);
        panel.mostrar(resultado);
        comprobar("ejemplo negativo: ocho diagnósticos", panel.cantidadErrores() == 8);
        comprobar("diagnósticos conservan fase, código, mensaje, lexema y posición", erroresIguales(panel, resultado));
        panel.limpiar();
        comprobar("limpiar vacía las cuatro tablas", tablas(panel).stream().allMatch(t -> t.getRowCount() == 0));
        panel.mostrar(null);
        comprobar("resultado ausente deja las cuatro tablas vacías", tablas(panel).stream().allMatch(t -> t.getRowCount() == 0));
    }

    private static void reemplazoYAmbitos() {
        PanelesAnalisis panel = new PanelesAnalisis();
        CompiladorSQL compilador = new CompiladorSQL();
        panel.mostrar(compilador.analizar("CREAR TABLA a (id ENTERO); CREAR TABLA b (id TEXTO);"));
        comprobar("mismo nombre de columna conserva tabla y tipo distintos",
                panel.getTablaSimbolos().getValueAt(1, 2).equals("a")
                && panel.getTablaSimbolos().getValueAt(3, 2).equals("b")
                && !panel.getTablaSimbolos().getValueAt(1, 3).equals(panel.getTablaSimbolos().getValueAt(3, 3)));
        panel.mostrar(compilador.analizar("SELECCIONAR * DESDE a;"));
        comprobar("cada análisis empieza sin declaraciones previas",
                panel.cantidadErrores() > 0 && panel.getTablaSimbolos().getRowCount() == 0);
        comprobar("identificador observado no se confunde con tabla declarada", panel.getTablaLexicos().getRowCount() == 1);
        panel.mostrar(compilador.analizar("CREAR TABLA nueva (id ENTERO);"));
        comprobar("reanálisis sustituye errores y catálogo anterior",
                panel.cantidadErrores() == 0 && panel.getTablaSimbolos().getRowCount() == 2
                && "nueva".equals(panel.getTablaSimbolos().getValueAt(0, 1)));
        comprobar("reanálisis sustituye también identificadores", panel.getTablaLexicos().getRowCount() == 2);
        panel.mostrar(compilador.analizar(""));
        comprobar("entrada vacía no retiene símbolos ni errores", panel.getTablaSimbolos().getRowCount() == 0
                && panel.getTablaLexicos().getRowCount() == 0 && panel.cantidadErrores() == 0);
    }

    private static void posicionesYOrden() {
        PanelesAnalisis panel = new PanelesAnalisis();
        panel.mostrarErrorEjecucion("línea diez", 10, 8);
        panel.mostrarErrorEjecucion("línea dos", 2, 3);
        JTable tabla = panel.getTablaErrores();
        tabla.getRowSorter().toggleSortOrder(4);
        comprobar("líneas se ordenan numéricamente", tabla.getValueAt(0, 4).equals(2));
        int[] posicion = {0, 0};
        panel.alSeleccionarError((linea, columna) -> { posicion[0] = linea; posicion[1] = columna; });
        tabla.setRowSelectionInterval(0, 0);
        comprobar("selección ordenada conserva línea y columna originales", posicion[0] == 2 && posicion[1] == 3);
        panel.limpiar();
        comprobar("limpiar no dispara navegación anterior", posicion[0] == 2 && posicion[1] == 3);
        panel.mostrarErrorEjecucion(null, 0, -1);
        comprobar("fallo sin mensaje ni posición muestra valores utilizables",
                tabla.getValueAt(0, 2).equals("Error de ejecución")
                && tabla.getValueAt(0, 4).equals(1) && tabla.getValueAt(0, 5).equals(1));
    }

    private static void delimitadores() {
        for (String fuente : List.of("CREAR TABLA t (id ENTERO)",
                "CREAR TABLA t (id ENTERO nombre TEXTO);", "CREAR TABLA t (id ENTERO;")) {
            ResultadoCompilacion resultado = new CompiladorSQL().analizar(fuente);
            PanelesAnalisis panel = new PanelesAnalisis();
            panel.mostrar(resultado);
            comprobar("delimitador ausente se muestra con posición: " + fuente,
                    !resultado.esValido() && panel.cantidadErrores() > 0 && erroresIguales(panel, resultado));
            comprobar("delimitador ausente bloquea todas las sentencias",
                    new ServicioEjecucion().ejecutar(fuente).resultados().isEmpty());
        }
    }

    private static void pruebasCruzadas(String valido, String referencia, String errores) {
        ServicioEjecucion servicio = new ServicioEjecucion();
        ResultadoComparacion comparacion = servicio.comparar(valido, referencia);
        comprobar("ejemplo de fase 2 coincide con SQL independiente: " + comparacion.detalles(), comparacion.coincide());
        comprobar("las 15 sentencias se ejecutaron", comparacion.compilador().resultados().size() == 15);
        comprobar("segunda demostración usa bases nuevas", servicio.comparar(valido, referencia).coincide());
        comprobar("script inválido completo no ejecuta DDL ni DML", servicio.ejecutar(errores).resultados().isEmpty());
        comprobar("entrada vacía no ejecuta SQL", servicio.ejecutar("").resultados().isEmpty());
        String fuente = "CREAR TABLA t (id ENTERO); INSERTAR EN t VALORES (1); "
                + "INSERTAR EN t VALORES (2); SELECCIONAR * DESDE t;";
        comprobar("comparación ignora orden de filas", servicio.comparar(fuente,
                "CREATE TABLE t (id INTEGER); INSERT INTO t VALUES (2); INSERT INTO t VALUES (1); SELECT * FROM t;").coincide());
        comprobar("comparación conserva repeticiones", !servicio.comparar(fuente,
                "CREATE TABLE t (id INTEGER); INSERT INTO t VALUES (1); INSERT INTO t VALUES (1); INSERT INTO t VALUES (2); SELECT * FROM t;").coincide());
    }

    private static boolean tokensIguales(PanelesAnalisis panel, ResultadoCompilacion resultado) {
        JTable tabla = panel.getTablaTokens();
        if (tabla.getRowCount() != resultado.tokens().size()) return false;
        for (int i = 0; i < tabla.getRowCount(); i++) {
            var token = resultado.tokens().get(i);
            if (!filaIgual(tabla, i, token.getTipo(), token.getLexema(), token.getlinea(), token.getcolumna())) return false;
        }
        return true;
    }

    private static boolean lexicosIguales(PanelesAnalisis panel, ResultadoCompilacion resultado) {
        var simbolos = resultado.simbolosLexicos().obtenerTodos();
        JTable tabla = panel.getTablaLexicos();
        if (tabla.getRowCount() != simbolos.size()) return false;
        int i = 0;
        for (var simbolo : simbolos) {
            if (!filaIgual(tabla, i++, simbolo.getId(), simbolo.getNombre(), simbolo.getTipoToken(),
                    simbolo.getCategoria(), simbolo.getTipoDato(), simbolo.getLinea(),
                    simbolo.getColumna(), simbolo.getApariciones())) return false;
        }
        return true;
    }

    private static boolean erroresIguales(PanelesAnalisis panel, ResultadoCompilacion resultado) {
        var errores = resultado.errores().obtenerTodos();
        JTable tabla = panel.getTablaErrores();
        if (tabla.getRowCount() != errores.size()) return false;
        int i = 0;
        for (var error : errores) {
            if (!filaIgual(tabla, i++, error.getFase(), error.getCodigo(), error.getMensaje(),
                    error.getLexema(), error.getLinea(), error.getColumna())
                    || error.getLinea() < 1 || error.getColumna() < 1) return false;
        }
        return true;
    }

    private static boolean filaIgual(JTable tabla, int fila, Object... valores) {
        for (int i = 0; i < valores.length; i++) {
            if (!Objects.equals(tabla.getValueAt(fila, i), valores[i])) return false;
        }
        return true;
    }

    private static List<JTable> tablas(PanelesAnalisis panel) {
        return List.of(panel.getTablaTokens(), panel.getTablaSimbolos(), panel.getTablaLexicos(), panel.getTablaErrores());
    }

    private static String leer(String nombre) throws Exception {
        return Files.readString(Path.of("tests", nombre), StandardCharsets.UTF_8);
    }

    private static void comprobar(String nombre, boolean condicion) {
        total++;
        if (!condicion) { fallidas++; System.err.println("FALLA: " + nombre); }
    }
}
