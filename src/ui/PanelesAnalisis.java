package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import compiler.ResultadoCompilacion;
import errors.Diagnostico;
import lexer.Token;
import semantic.CatalogoSemantico;
import symbol_table.Simbolo;

/** Vistas de análisis: conservan la diferencia entre observar y declarar un nombre. */
public final class PanelesAnalisis extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DefaultTableModel tokens = modelo(
            new String[] { "Tipo de token", "Lexema", "Línea", "Columna" }, 2, 3);
    private final DefaultTableModel simbolos = modelo(
            new String[] { "Elemento", "Nombre", "Ámbito", "Tipo de dato" });
    private final DefaultTableModel lexicos = modelo(
            new String[] { "ID", "Identificador", "Token", "Categoría", "Tipo", "Línea", "Columna", "Apariciones" },
            0, 5, 6, 7);
    private final DefaultTableModel errores = modelo(
            new String[] { "Fase", "Código", "Mensaje", "Lexema", "Línea", "Columna" }, 4, 5);
    private final JTable tablaTokens = tabla(tokens);
    private final JTable tablaSimbolos = tabla(simbolos);
    private final JTable tablaLexicos = tabla(lexicos);
    private final JTable tablaErrores = tabla(errores);
    private final JTabbedPane pestanas = new JTabbedPane();
    private final JLabel resumen = Estilo.etiqueta("Analiza tu código para inspeccionar cada etapa.");
    private final JLabel contador = Estilo.etiqueta("SIN ANÁLISIS");
    private transient BiConsumer<Integer, Integer> alSeleccionarError;
    private boolean actualizandoModelos;

    public PanelesAnalisis() {
        super(new BorderLayout(0, 12));
        setOpaque(true);
        setBackground(Estilo.PANEL);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        JPanel encabezado = new JPanel(new BorderLayout(12, 0));
        encabezado.setOpaque(false);
        JLabel titulo = Estilo.etiqueta("INSPECCIÓN DEL COMPILADOR");
        titulo.setForeground(Estilo.ACENTO);
        contador.setForeground(Estilo.MUTED);
        encabezado.add(titulo, BorderLayout.WEST);
        encabezado.add(contador, BorderLayout.EAST);
        add(encabezado, BorderLayout.NORTH);

        configurarPestanas(pestanas);
        pestanas.addTab("Tokens", contenedor(tablaTokens,
                "Tipo y posición original de cada lexema en el editor."));

        JTabbedPane vistasSimbolos = new JTabbedPane();
        configurarPestanas(vistasSimbolos);
        vistasSimbolos.addTab("Esquema declarado", contenedor(tablaSimbolos,
                "Objetos válidos: cada columna pertenece a una tabla."));
        vistasSimbolos.addTab("Identificadores léxicos", contenedor(tablaLexicos,
                "Observar un nombre no significa que esté declarado."));
        pestanas.addTab("Símbolos", vistasSimbolos);
        pestanas.addTab("Errores", contenedor(tablaErrores,
                "Selecciona un error para ir a su línea y columna en el editor."));
        add(pestanas, BorderLayout.CENTER);
        resumen.setForeground(Estilo.MUTED);
        add(resumen, BorderLayout.SOUTH);

        tablaTokens.getColumnModel().getColumn(0).setPreferredWidth(180);
        tablaTokens.getColumnModel().getColumn(1).setPreferredWidth(260);
        tablaErrores.getColumnModel().getColumn(2).setPreferredWidth(420);
        tablaErrores.getColumnModel().getColumn(3).setPreferredWidth(120);
        tablaErrores.getSelectionModel().addListSelectionListener(evento -> {
            if (actualizandoModelos || evento.getValueIsAdjusting() || alSeleccionarError == null) {
                return;
            }
            int vista = tablaErrores.getSelectedRow();
            if (vista < 0 || vista >= tablaErrores.getRowCount()) {
                return;
            }
            int fila = tablaErrores.convertRowIndexToModel(vista);
            if (fila >= 0 && fila < errores.getRowCount()) {
                alSeleccionarError.accept((Integer) errores.getValueAt(fila, 4),
                        (Integer) errores.getValueAt(fila, 5));
            }
        });
    }

    public void mostrar(ResultadoCompilacion resultado) {
        limpiar();
        if (resultado == null) {
            return;
        }
        for (Token token : resultado.tokens()) {
            tokens.addRow(new Object[] { token.getTipo(), token.getLexema(),
                    token.getlinea(), token.getcolumna() });
        }
        for (CatalogoSemantico.Tabla tabla : resultado.catalogo().obtenerTablas()) {
            simbolos.addRow(new Object[] { "TABLA", tabla.getNombre(), "GLOBAL", "—" });
            for (CatalogoSemantico.Columna columna : tabla.obtenerColumnas()) {
                simbolos.addRow(new Object[] { "COLUMNA", columna.nombre(),
                        tabla.getNombre(), columna.tipo() });
            }
        }
        for (Simbolo simbolo : resultado.simbolosLexicos().obtenerTodos()) {
            lexicos.addRow(new Object[] { simbolo.getId(), simbolo.getNombre(),
                    simbolo.getTipoToken(), simbolo.getCategoria(), simbolo.getTipoDato(),
                    simbolo.getLinea(), simbolo.getColumna(), simbolo.getApariciones() });
        }
        for (Diagnostico error : resultado.errores().obtenerTodos()) {
            errores.addRow(new Object[] { error.getFase(), error.getCodigo(), error.getMensaje(),
                    error.getLexema(), error.getLinea(), error.getColumna() });
        }
        actualizarTitulos();
        contador.setText(resultado.esValido() ? "ANÁLISIS CORRECTO" : "REQUIERE CORRECCIÓN");
        contador.setForeground(resultado.esValido() ? Estilo.EXITO : Estilo.ERROR);
        resumen.setText(resultado.sentenciasValidas() + " sentencias válidas · "
                + resultado.catalogo().tamano() + " tablas declaradas · "
                + errores.getRowCount() + " errores");
        if (!resultado.esValido()) {
            pestanas.setSelectedIndex(2);
        }
    }

    public void limpiar() {
        actualizandoModelos = true;
        try {
            reiniciarSeleccion(tablaTokens);
            reiniciarSeleccion(tablaSimbolos);
            reiniciarSeleccion(tablaLexicos);
            reiniciarSeleccion(tablaErrores);
            tokens.setRowCount(0);
            simbolos.setRowCount(0);
            lexicos.setRowCount(0);
            errores.setRowCount(0);
        } finally {
            actualizandoModelos = false;
        }
        actualizarTitulos();
        contador.setText("SIN ANÁLISIS");
        contador.setForeground(Estilo.MUTED);
        resumen.setText("Analiza tu código para inspeccionar cada etapa.");
        pestanas.setSelectedIndex(0);
    }

    public void mostrarErrorEjecucion(String mensaje, int linea, int columna) {
        errores.addRow(new Object[] { "EJECUCIÓN", "SQL-001", mensaje == null ? "Error de ejecución" : mensaje,
                "—", Math.max(1, linea), Math.max(1, columna) });
        actualizarTitulos();
        contador.setText("ERROR EN EJECUCIÓN");
        contador.setForeground(Estilo.ERROR);
        resumen.setText("La ejecución encontró un problema. Selecciona el error para localizarlo.");
        pestanas.setSelectedIndex(2);
    }

    public void alSeleccionarError(BiConsumer<Integer, Integer> listener) {
        alSeleccionarError = listener;
    }

    public JTable getTablaTokens() { return tablaTokens; }
    public JTable getTablaErrores() { return tablaErrores; }
    public JTable getTablaSimbolos() { return tablaSimbolos; }
    public int cantidadErrores() { return errores.getRowCount(); }

    private void actualizarTitulos() {
        pestanas.setTitleAt(0, "Tokens · " + tokens.getRowCount());
        pestanas.setTitleAt(1, "Símbolos · " + simbolos.getRowCount());
        pestanas.setTitleAt(2, "Errores · " + errores.getRowCount());
    }

    private static void configurarPestanas(JTabbedPane componente) {
        componente.setFont(Estilo.UI);
        componente.setBackground(Estilo.PANEL);
        componente.setForeground(Estilo.TEXTO);
        componente.setBorder(BorderFactory.createEmptyBorder());
    }

    private static JPanel contenedor(Component contenido, String ayuda) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        panel.add(Estilo.scroll(contenido), BorderLayout.CENTER);
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pie.setOpaque(false);
        JLabel texto = Estilo.etiqueta(ayuda);
        texto.setForeground(Estilo.MUTED);
        texto.setFont(Estilo.UI.deriveFont(11f));
        texto.setToolTipText(ayuda);
        pie.add(texto);
        panel.add(pie, BorderLayout.SOUTH);
        return panel;
    }

    private static JTable tabla(DefaultTableModel modelo) {
        JTable tabla = Estilo.tabla();
        tabla.setModel(modelo);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return tabla;
    }

    private static void reiniciarSeleccion(JTable tabla) {
        ListSelectionModel seleccion = tabla.getSelectionModel();
        seleccion.setValueIsAdjusting(true);
        try {
            tabla.clearSelection();
            // clearSelection conserva anchor/lead: JTable intentaría convertir esa
            // posición antigua al insertar la primera fila del siguiente análisis.
            seleccion.setAnchorSelectionIndex(-1);
            seleccion.setLeadSelectionIndex(-1);
        } finally {
            seleccion.setValueIsAdjusting(false);
        }
    }

    private static DefaultTableModel modelo(String[] columnas, int... numericas) {
        return new DefaultTableModel(columnas, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
            @Override public Class<?> getColumnClass(int columna) {
                for (int numerica : numericas) {
                    if (numerica == columna) { return Integer.class; }
                }
                return Object.class;
            }
        };
    }
}
