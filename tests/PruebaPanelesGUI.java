import java.awt.Component;
import java.awt.Container;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import compiler.CompiladorSQL;
import compiler.ResultadoCompilacion;
import execution.InformeEjecucion;
import execution.ResultadoComparacion;
import execution.ResultadoEjecucion;
import ui.Estilo;
import ui.PanelesAnalisis;
import ui.PanelResultadosSQL;

/** Regresiones de Swing real en el EDT, sin abrir ventanas ni necesitar pantalla. */
public final class PruebaPanelesGUI {
    private static int total;
    private static int fallidas;
    private PruebaPanelesGUI() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(() -> {
            Estilo.instalar();
            analisisYErrores();
            resultadosYNull();
            comparacionDirecta();
            seleccionTrasReanalizar();
        });
        System.out.println("Pruebas paneles GUI: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) System.exit(1);
    }

    private static void probar(String nombre, boolean condicion) {
        total++;
        if (!condicion) { fallidas++; System.out.println("FALLA: " + nombre); }
    }

    private static void analisisYErrores() {
        PanelesAnalisis panel = new PanelesAnalisis();
        panel.mostrar(analizar("CREAR TABLA personas (id ENTERO, nombre TEXTO);"));
        probar("muestra los tokens del scanner", panel.getTablaTokens().getRowCount() > 5);
        probar("catálogo contiene tabla y columnas", panel.getTablaSimbolos().getRowCount() == 3);
        probar("script válido no produce errores", panel.cantidadErrores() == 0);
        probar("columna conserva su ámbito de tabla",
                "personas".equals(panel.getTablaSimbolos().getValueAt(1, 2)));

        List<Integer> lineas = new ArrayList<>();
        panel.alSeleccionarError((linea, columna) -> lineas.add(linea));
        panel.mostrarErrorEjecucion("último", 20, 4);
        panel.mostrarErrorEjecucion("primero", 3, 2);
        JTable errores = panel.getTablaErrores();
        errores.getRowSorter().toggleSortOrder(4);
        errores.setRowSelectionInterval(0, 0);
        probar("error ordenado localiza su línea original", !lineas.isEmpty() && lineas.getLast() == 3);
        int eventos = lineas.size();
        panel.limpiar();
        probar("limpiar no navega a posiciones antiguas", lineas.size() == eventos);
        probar("limpiar borra los errores de ejecución", panel.cantidadErrores() == 0);
        panel.mostrar(analizar("CREAR TABLA incompleta (id ENTERO"));
        probar("muestra los errores de sintaxis", panel.cantidadErrores() > 0);
        panel.mostrar(analizar("CREAR TABLA limpia (id ENTERO);"));
        probar("nuevo análisis reemplaza el catálogo", panel.getTablaSimbolos().getRowCount() == 2);
        probar("nuevo análisis no conserva errores anteriores", panel.cantidadErrores() == 0);
        panel.mostrar(null);
        probar("análisis ausente deja una vista vacía", panel.getTablaTokens().getRowCount() == 0);
    }

    private static void resultadosYNull() {
        PanelResultadosSQL panel = new PanelResultadosSQL();
        ResultadoEjecucion crear = new ResultadoEjecucion(1, "CREAR", 1, 1,
                "CREATE TABLE x(id INTEGER)", List.of(), List.of(), 0, "");
        ResultadoEjecucion consulta = consultaNula();
        panel.mostrar(List.of(crear, consulta));
        JTable grilla = componente(panel, JTable.class, "Resultados de consulta");
        JComboBox<?> selector = componente(panel, JComboBox.class, "Sentencia ejecutada");
        probar("última consulta muestra filas y columnas", grilla.getRowCount() == 1 && grilla.getColumnCount() == 2);
        probar("celda conserva el valor SQL nulo", grilla.getValueAt(0, 1) == null);
        JLabel celda = (JLabel) grilla.getCellRenderer(0, 1)
                .getTableCellRendererComponent(grilla, null, false, false, 0, 1);
        probar("NULL aparece explícito en pantalla", "NULL".equals(celda.getText()));
        selector.setSelectedIndex(0);
        probar("DDL no conserva filas de otra consulta", grilla.getRowCount() == 0);
        selector.setSelectedIndex(1);
        probar("selector vuelve al resultado de la consulta", grilla.getRowCount() == 1);
        panel.limpiar();
        probar("limpiar elimina resultados y selector", grilla.getRowCount() == 0 && selector.getItemCount() == 0);
        panel.mostrar(null);
        panel.mostrarComparacion(null);
        probar("ausencia de resultados desactiva el selector", !selector.isEnabled());
    }

    private static void comparacionDirecta() {
        PanelResultadosSQL panel = new PanelResultadosSQL();
        ResultadoEjecucion consulta = consultaNula();
        InformeEjecucion ejecucion = new InformeEjecucion(analizar("CREAR TABLA x (id ENTERO);"),
                List.of(consulta), 1);
        panel.mostrarComparacion(new ResultadoComparacion(true,
                List.of("Las consultas y el estado final coinciden."), ejecucion, List.of(consulta)));
        JTable grilla = componente(panel, JTable.class, "Resultados de consulta");
        JTextArea sql = componente(panel, JTextArea.class, "SQL generado");
        JLabel estado = componente(panel, JLabel.class, "Estado de comparación");
        JTextArea detalle = componente(panel, JTextArea.class, "Detalle de comparación");
        probar("comparar directamente muestra las filas del compilador", grilla.getRowCount() == 1);
        probar("comparar directamente muestra SQL generado", consulta.sql().equals(sql.getText()));
        probar("comparación correcta muestra estado de éxito",
                estado.getText().startsWith("COINCIDE") && Estilo.EXITO.equals(estado.getForeground()));
        probar("SQL generado permanece de solo lectura", !sql.isEditable());

        panel.mostrarComparacion(new ResultadoComparacion(false,
                List.of("Las consultas tienen resultados diferentes."), ejecucion, List.of()));
        probar("diferencia reemplaza el estado de éxito",
                "SE ENCONTRARON DIFERENCIAS".equals(estado.getText()) && Estilo.ERROR.equals(estado.getForeground()));
        probar("diferencia reemplaza el detalle anterior",
                detalle.getText().equals("Las consultas tienen resultados diferentes."));
        panel.mostrarComparacion(null);
        probar("comparación ausente elimina filas anteriores", grilla.getRowCount() == 0);
        probar("comparación ausente elimina etiqueta exitosa",
                "COMPARACIÓN NO DISPONIBLE".equals(estado.getText()) && Estilo.MUTED.equals(estado.getForeground()));
    }

    private static ResultadoCompilacion analizar(String codigo) {
        return new CompiladorSQL().analizar(codigo);
    }

    private static void seleccionTrasReanalizar() {
        PrintStream anterior = System.err;
        ByteArrayOutputStream avisos = new ByteArrayOutputStream();
        try (PrintStream captura = new PrintStream(avisos, true, StandardCharsets.UTF_8)) {
            System.setErr(captura);
            PanelesAnalisis panel = new PanelesAnalisis();
            panel.alSeleccionarError((linea, columna) -> { });
            panel.mostrar(analizar("SELECCIONAR * DESDE ausente;"));
            panel.getTablaErrores().setRowSelectionInterval(0, 0);
            panel.limpiar();
            panel.mostrar(analizar("CREAR TABLA nueva (id ENTERO);"));
            panel.mostrarErrorEjecucion("Límite del motor", 2, 1);
            probar("error de ejecución después de reanalizar queda visible", panel.cantidadErrores() == 1);
        } finally {
            System.setErr(anterior);
        }
        probar("reemplazar errores seleccionados no desincroniza el ordenador de filas",
                !avisos.toString(StandardCharsets.UTF_8).contains("row index is bigger"));
    }

    private static ResultadoEjecucion consultaNula() {
        return new ResultadoEjecucion(2, "SELECCIONAR", 2, 1, "SELECT * FROM x",
                List.of("id", "dato"), List.of(Arrays.asList(1, null)), -1, "");
    }

    private static <T extends Component> T componente(Container contenedor, Class<T> tipo, String nombre) {
        for (Component componente : contenedor.getComponents()) {
            if (tipo.isInstance(componente) && componente.getAccessibleContext() != null
                    && nombre.equals(componente.getAccessibleContext().getAccessibleName())) {
                return tipo.cast(componente);
            }
            if (componente instanceof Container hijo) {
                T encontrado = componente(hijo, tipo, nombre);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }
}
