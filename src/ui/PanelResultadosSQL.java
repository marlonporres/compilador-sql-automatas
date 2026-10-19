package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import execution.ResultadoComparacion;
import execution.ResultadoEjecucion;

/** Resultados reales de JDBC, SQL generado y comparación en bases independientes. */
public final class PanelResultadosSQL extends JPanel {
    private static final long serialVersionUID = 1L;

    private final JComboBox<String> selector = new JComboBox<>();
    private final JTable tabla = Estilo.tabla();
    private final JTextArea sql = Estilo.codigo();
    private final JTextArea comparacion = Estilo.codigo();
    private final JLabel estado = Estilo.etiqueta("Ejecuta un script válido para ver sus resultados.");
    private final JLabel resumenComparacion = Estilo.etiqueta("COMPARACIÓN PENDIENTE");
    private transient List<ResultadoEjecucion> resultados = List.of();

    public PanelResultadosSQL() {
        super(new BorderLayout(0, 12));
        setBackground(Estilo.PANEL);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        JPanel encabezado = new JPanel(new BorderLayout(12, 8));
        encabezado.setOpaque(false);
        JLabel titulo = Estilo.etiqueta("RESULTADOS SQL");
        titulo.setForeground(Estilo.ACENTO);
        encabezado.add(titulo, BorderLayout.NORTH);
        selector.setFont(Estilo.UI);
        selector.setBackground(Estilo.FONDO);
        selector.setForeground(Estilo.TEXTO);
        selector.setToolTipText("Elige la sentencia cuyo resultado deseas inspeccionar");
        selector.getAccessibleContext().setAccessibleName("Sentencia ejecutada");
        selector.setEnabled(false);
        selector.addActionListener(evento -> mostrarSeleccion());
        encabezado.add(selector, BorderLayout.CENTER);
        estado.setForeground(Estilo.MUTED);
        estado.setToolTipText(null);
        encabezado.add(estado, BorderLayout.SOUTH);
        add(encabezado, BorderLayout.NORTH);

        tabla.setAutoCreateRowSorter(true);
        tabla.getAccessibleContext().setAccessibleName("Resultados de consulta");
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public Component getTableCellRendererComponent(JTable origen, Object valor,
                    boolean seleccionado, boolean foco, int fila, int columna) {
                super.getTableCellRendererComponent(origen, valor == null ? "NULL" : valor,
                        seleccionado, foco, fila, columna);
                if (!seleccionado) {
                    setForeground(valor == null ? Estilo.MUTED : Estilo.TEXTO);
                    setBackground(fila % 2 == 0 ? Estilo.PANEL : Estilo.FONDO);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setToolTipText(valor == null ? "Valor SQL nulo" : String.valueOf(valor));
                return this;
            }
        });
        sql.setEditable(false);
        sql.setLineWrap(false);
        sql.setRows(4);
        sql.setToolTipText("Los signos ? son parámetros. Los valores del código fuente se enlazan por separado al ejecutar; no faltan datos.");
        sql.getAccessibleContext().setAccessibleName("SQL generado");
        comparacion.setEditable(false);
        comparacion.setLineWrap(true);
        comparacion.setWrapStyleWord(true);
        comparacion.setRows(3);
        comparacion.getAccessibleContext().setAccessibleName("Detalle de comparación");

        JPanel resultadosPanel = new JPanel(new BorderLayout(0, 8));
        resultadosPanel.setOpaque(false);
        resultadosPanel.add(Estilo.scroll(tabla), BorderLayout.CENTER);
        JPanel codigoPanel = seccion("SQL PREPARADO · VALORES SEPARADOS", sql);
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultadosPanel, codigoPanel);
        division.setOpaque(false);
        division.setBorder(BorderFactory.createEmptyBorder());
        division.setResizeWeight(0.72);
        division.setDividerSize(8);
        add(division, BorderLayout.CENTER);

        JPanel comparacionPanel = new JPanel(new BorderLayout(0, 6));
        comparacionPanel.setOpaque(false);
        comparacionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Estilo.BORDE),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)));
        resumenComparacion.setForeground(Estilo.MUTED);
        resumenComparacion.getAccessibleContext().setAccessibleName("Estado de comparación");
        comparacionPanel.add(resumenComparacion, BorderLayout.NORTH);
        comparacionPanel.add(Estilo.scroll(comparacion), BorderLayout.CENTER);
        add(comparacionPanel, BorderLayout.SOUTH);
        limpiar();
    }

    public void mostrar(List<ResultadoEjecucion> nuevosResultados) {
        limpiar();
        resultados = nuevosResultados == null ? List.of() : List.copyOf(nuevosResultados);
        for (ResultadoEjecucion resultado : resultados) {
            selector.addItem("Sentencia " + resultado.numero() + " · " + resultado.comando()
                    + " · línea " + resultado.linea()
                    + (resultado.esExitoso() ? "" : " · ERROR"));
        }
        selector.setEnabled(!resultados.isEmpty());
        if (!resultados.isEmpty()) {
            selector.setSelectedIndex(resultados.size() - 1);
            mostrarSeleccion();
        }
    }

    public void limpiar() {
        resultados = List.of();
        selector.removeAllItems();
        selector.setEnabled(false);
        tabla.setModel(modelo(List.of("Resultado"), List.of()));
        sql.setText("-- El SQL generado aparecerá después de ejecutar un script válido.");
        sql.setCaretPosition(0);
        estado.setText("Ejecuta un script válido para ver sus resultados.");
        estado.setForeground(Estilo.MUTED);
        estado.setToolTipText(null);
        resumenComparacion.setText("COMPARACIÓN PENDIENTE");
        resumenComparacion.setForeground(Estilo.MUTED);
        comparacion.setText("La comparación verificará las consultas y el estado final de las tablas frente al motor SQL de referencia.");
        comparacion.setCaretPosition(0);
    }

    public void mostrarComparacion(ResultadoComparacion resultado) {
        if (resultado == null) {
            limpiar();
            resumenComparacion.setText("COMPARACIÓN NO DISPONIBLE");
            resumenComparacion.setForeground(Estilo.MUTED);
            comparacion.setText("No se recibió un resultado de comparación.");
            return;
        }
        mostrar(resultado.compilador() == null ? List.of() : resultado.compilador().resultados());
        resumenComparacion.setText(resultado.coincide()
                ? "COINCIDE CON EL MOTOR SQL DE REFERENCIA" : "SE ENCONTRARON DIFERENCIAS");
        resumenComparacion.setForeground(resultado.coincide() ? Estilo.EXITO : Estilo.ERROR);
        comparacion.setText(resultado.detalles().isEmpty()
                ? (resultado.coincide() ? "Los resultados coinciden." : "Los resultados no coinciden.")
                : String.join(System.lineSeparator(), resultado.detalles()));
        comparacion.setCaretPosition(0);
    }

    private void mostrarSeleccion() {
        int indice = selector.getSelectedIndex();
        if (indice < 0 || indice >= resultados.size()) {
            return;
        }
        ResultadoEjecucion resultado = resultados.get(indice);
        sql.setText(resultado.sql() == null ? "" : resultado.sql());
        sql.setCaretPosition(0);
        tabla.setModel(modelo(resultado.columnas(), resultado.filas()));
        if (!resultado.esExitoso()) {
            estado.setText("Error: " + resultado.error());
            estado.setForeground(Estilo.ERROR);
            estado.setToolTipText(resultado.error());
        } else {
            estado.setText(resultado.esConsulta()
                    ? resultado.filas().size() + " filas devueltas · " + resultado.columnas().size() + " columnas"
                    : "Operación completada · " + Math.max(0, resultado.filasAfectadas()) + " filas afectadas");
            estado.setForeground(Estilo.EXITO);
            estado.setToolTipText(estado.getText());
        }
    }

    private static JPanel seccion(String titulo, JTextArea contenido) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel etiqueta = Estilo.etiqueta(titulo);
        etiqueta.setForeground(Estilo.MUTED);
        etiqueta.setFont(Estilo.UI.deriveFont(11f));
        panel.add(etiqueta, BorderLayout.NORTH);
        panel.add(Estilo.scroll(contenido), BorderLayout.CENTER);
        return panel;
    }

    private static DefaultTableModel modelo(List<String> columnas, List<List<Object>> filas) {
        DefaultTableModel modelo = new DefaultTableModel(columnas.toArray(), 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
        for (List<Object> fila : filas) {
            modelo.addRow(fila.toArray());
        }
        return modelo;
    }
}
