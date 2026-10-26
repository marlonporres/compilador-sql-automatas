package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import compiler.ResultadoCompilacion;
import execution.InformeEjecucion;
import execution.ServicioEjecucion;
import execution.ResultadoComparacion;

/** Composición de la ventana. El controlador coordina el trabajo fuera del hilo gráfico. */
public final class VentanaPrincipal extends JFrame {
    private static final long serialVersionUID = 1L;
    private final EditorSQL fuente = new EditorSQL("Script SQL en español");
    private final EditorSQL referencia = new EditorSQL("SQL de referencia para comparación");
    private final PanelesAnalisis analisis = new PanelesAnalisis();
    private final PanelResultadosSQL resultados = new PanelResultadosSQL();
    private final JButton analizar = Estilo.boton("Analizar", true);
    private final JButton ejecutar = Estilo.boton("Ejecutar", false);
    private final JButton comparar = Estilo.boton("Comparar SQL", false);
    private final JButton limpiar = Estilo.boton("Limpiar", false);
    private final JButton ejemplo = Estilo.boton("Ejemplo completo", false);
    private final JButton errores = Estilo.boton("Ejemplo con errores", false);
    private final JButton abrir = Estilo.boton("Abrir .sql", false);
    private final JLabel estado = Estilo.etiqueta("Sin analizar");
    private final JLabel detalle = Estilo.etiqueta("Escribe o abre un script para comenzar.");
    private final JLabel posicion = Estilo.etiqueta("Línea 1 · Columna 1");
    private final JProgressBar progreso = new JProgressBar();
    private final JTabbedPane salida = new JTabbedPane();
    private final JTabbedPane editores = new JTabbedPane();
    private final transient ControladorGUI controlador;
    private final transient ServicioEjecucion servicio;

    public VentanaPrincipal() {
        this(ServicioEjecucion.persistente());
    }

    /** Permite ensayar la GUI sobre una base temporal sin tocar los datos del usuario. */
    public VentanaPrincipal(ServicioEjecucion servicio) {
        super("SQL Studio · Compilador SQL en español");
        this.servicio = java.util.Objects.requireNonNull(servicio);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 690));
        setSize(1360, 900);
        setLocationRelativeTo(null);
        JPanel contenido = new JPanel(new BorderLayout(0, 20));
        contenido.setBackground(Estilo.FONDO);
        contenido.setBorder(BorderFactory.createEmptyBorder(22, 26, 18, 26));
        setContentPane(contenido);
        contenido.add(cabecera(), BorderLayout.NORTH);
        contenido.add(areaTrabajo(), BorderLayout.CENTER);
        contenido.add(pie(), BorderLayout.SOUTH);

        controlador = new ControladorGUI(this, servicio);
        fuente.alEditar(controlador::contenidoModificado);
        referencia.alEditar(controlador::referenciaModificada);
        analizar.addActionListener(e -> controlador.analizar());
        ejecutar.addActionListener(e -> controlador.ejecutar());
        comparar.addActionListener(e -> controlador.comparar());
        limpiar.addActionListener(e -> controlador.limpiar());
        ejemplo.addActionListener(e -> cargarEjemplo(false));
        errores.addActionListener(e -> cargarEjemplo(true));
        abrir.addActionListener(e -> abrirArchivo());
        analisis.alSeleccionarError((linea, columna) -> {
            editores.setSelectedIndex(0);
            fuente.irA(linea, columna);
        });
        fuente.campo().addCaretListener(e -> actualizarPosicion());
        atajo("control ENTER", "analizar", analizar);
        atajo("control shift ENTER", "ejecutar", ejecutar);
        atajo("control O", "abrir", abrir);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { controlador.cerrar(); }
        });
        controlador.actualizarBotones();
    }

    private JPanel cabecera() {
        JPanel panel = new JPanel(new BorderLayout(22, 0));
        panel.setOpaque(false);
        JPanel titulo = new JPanel();
        titulo.setOpaque(false);
        titulo.setLayout(new BoxLayout(titulo, BoxLayout.Y_AXIS));
        JLabel marca = Estilo.etiqueta("SQL STUDIO");
        marca.setFont(new Font("Segoe UI", Font.BOLD, 28));
        JLabel subtitulo = Estilo.etiqueta("Compilador en español  /  Proyecto final");
        subtitulo.setForeground(Estilo.MUTED);
        titulo.add(marca);
        titulo.add(Box.createVerticalStrut(5));
        titulo.add(subtitulo);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(ejemplo);
        acciones.add(errores);
        acciones.add(abrir);
        panel.add(titulo, BorderLayout.WEST);
        panel.add(acciones, BorderLayout.EAST);
        return panel;
    }

    private JComponent areaTrabajo() {
        editores.addTab("Código en español", fuente);
        editores.addTab("SQL de referencia", referencia);
        editores.setToolTipTextAt(1, "SQL equivalente escrito por separado para contrastar los resultados.");
        JPanel izquierda = new JPanel(new BorderLayout(0, 12));
        izquierda.setOpaque(false);
        JLabel instrucciones = Estilo.etiqueta("01  FUENTE  ·  Crea tablas o consulta las que ya guardaste");
        instrucciones.setForeground(Estilo.MUTED);
        izquierda.add(instrucciones, BorderLayout.NORTH);
        izquierda.add(editores, BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(analizar);
        botones.add(ejecutar);
        botones.add(comparar);
        botones.add(limpiar);
        izquierda.add(botones, BorderLayout.SOUTH);

        salida.addTab("Análisis", analisis);
        salida.addTab("SQL y resultados", resultados);
        JPanel derecha = new JPanel(new BorderLayout(0, 12));
        derecha.setOpaque(false);
        JLabel titulo = Estilo.etiqueta("02  INSPECCIÓN  ·  Datos y diagnósticos del script actual");
        titulo.setForeground(Estilo.MUTED);
        derecha.add(titulo, BorderLayout.NORTH);
        derecha.add(salida, BorderLayout.CENTER);
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha);
        division.setBorder(BorderFactory.createEmptyBorder());
        division.setResizeWeight(0.52);
        division.setDividerLocation(0.52);
        izquierda.setMinimumSize(new Dimension(450, 300));
        derecha.setMinimumSize(new Dimension(400, 300));
        return division;
    }

    private JPanel pie() {
        JPanel panel = new JPanel(new BorderLayout(12, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Estilo.BORDE));
        JPanel linea = new JPanel(new BorderLayout(12, 0));
        linea.setOpaque(false);
        estado.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        posicion.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        posicion.setForeground(Estilo.MUTED);
        linea.add(estado, BorderLayout.WEST);
        linea.add(posicion, BorderLayout.EAST);
        progreso.setPreferredSize(new Dimension(80, 3));
        progreso.setForeground(Estilo.ACENTO);
        progreso.setBackground(Estilo.BORDE);
        progreso.setBorderPainted(false);
        progreso.setVisible(false);
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        detalle.setForeground(Estilo.MUTED);
        centro.add(detalle, BorderLayout.CENTER);
        centro.add(progreso, BorderLayout.SOUTH);
        panel.add(linea, BorderLayout.NORTH);
        panel.add(centro, BorderLayout.CENTER);
        JLabel nota = Estilo.etiqueta(servicio.esPersistente()
                ? "Datos guardados en SQLStudio/datos · Comparar usa bases temporales"
                : "Base temporal de demostración · Ctrl+Enter para analizar");
        nota.setToolTipText(servicio.ubicacion());
        nota.setForeground(Estilo.MUTED);
        panel.add(nota, BorderLayout.SOUTH);
        return panel;
    }

    private void atajo(String tecla, String nombre, JButton boton) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(tecla), nombre);
        getRootPane().getActionMap().put(nombre, new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent e) {
                if (boton.isEnabled()) boton.doClick();
            }
        });
    }

    private void actualizarPosicion() {
        int caret = fuente.campo().getCaretPosition();
        javax.swing.text.Element raiz = fuente.campo().getDocument().getDefaultRootElement();
        int fila = raiz.getElementIndex(caret);
        posicion.setText("Línea " + (fila + 1) + " · Columna "
                + (caret - raiz.getElement(fila).getStartOffset() + 1));
    }

    public void cargarEjemplo(boolean conErrores) {
        try {
            String archivo = conErrores ? "demo_fase2_errores.sql" : "demo_fase3.sql";
            String contenidoFuente = RecursosProyecto.leerEjemplo(archivo);
            String contenidoReferencia = conErrores ? "" : RecursosProyecto.leerEjemplo(
                    "demo_fase3_referencia.sql");
            fuente.texto(contenidoFuente);
            referencia.texto(contenidoReferencia);
            editores.setSelectedIndex(0);
        } catch (IOException e) {
            avisar("No se pudo abrir el ejemplo. " + e.getMessage());
        }
    }

    private void abrirArchivo() {
        JFileChooser selector = new JFileChooser(RecursosProyecto.carpetaEjemplos().toFile());
        selector.setFileFilter(new FileNameExtensionFilter("Scripts SQL UTF-8", "sql"));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Path archivo = selector.getSelectedFile().toPath();
            if (Files.size(archivo) > 1_000_000) throw new IOException("El límite del editor es 1 MB.");
            fuente.texto(Files.readString(archivo, StandardCharsets.UTF_8));
            referencia.texto("");
            editores.setSelectedIndex(0);
        } catch (IOException e) { avisar("No se pudo leer el archivo: " + e.getMessage()); }
    }

    void mostrarAnalisis(ResultadoCompilacion resultado) {
        analisis.mostrar(resultado);
        salida.setSelectedIndex(0);
    }

    void mostrarEjecucion(InformeEjecucion informe) {
        resultados.mostrar(informe.resultados());
        salida.setSelectedIndex(1);
        informe.resultados().stream().filter(r -> !r.esExitoso()).forEach(r ->
                analisis.mostrarErrorEjecucion(r.error(), r.linea(), r.columna()));
    }

    void mostrarComparacion(ResultadoComparacion comparacion) {
        resultados.mostrarComparacion(comparacion);
        salida.setSelectedIndex(1);
    }

    void vaciarResultados() { analisis.limpiar(); resultados.limpiar(); }
    void marcarEstado(String texto, String descripcion, Color color) {
        estado.setText(texto);
        estado.setForeground(color);
        detalle.setText(descripcion);
        detalle.setToolTipText(descripcion);
    }

    void ocupado(boolean valor) {
        progreso.setIndeterminate(valor);
        progreso.setVisible(valor);
        fuente.editable(!valor);
        referencia.editable(!valor);
        ejemplo.setEnabled(!valor);
        errores.setEnabled(!valor);
        abrir.setEnabled(!valor);
    }

    void avisar(String mensaje) {
        marcarEstado("No se pudo completar", mensaje, Estilo.ERROR);
    }

    public EditorSQL editorFuente() { return fuente; }
    public EditorSQL editorReferencia() { return referencia; }
    public JButton botonAnalizar() { return analizar; }
    public JButton botonEjecutar() { return ejecutar; }
    public JButton botonComparar() { return comparar; }
    public JButton botonLimpiar() { return limpiar; }
    public PanelesAnalisis panelAnalisis() { return analisis; }
    public PanelResultadosSQL panelResultados() { return resultados; }
    public String textoEstado() { return estado.getText(); }
    public ControladorGUI controlador() { return controlador; }
    public void cerrarVentana() { controlador.cerrar(); dispose(); }
}
