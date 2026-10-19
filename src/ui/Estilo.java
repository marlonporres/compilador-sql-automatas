package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;

/** Paleta y componentes compartidos de la aplicación de escritorio. */
public final class Estilo {
    public static final Color FONDO = new Color(16, 23, 34);
    public static final Color PANEL = new Color(23, 33, 47);
    public static final Color TEXTO = new Color(229, 237, 246);
    public static final Color MUTED = new Color(165, 181, 199);
    public static final Color BORDE = new Color(46, 62, 81);
    public static final Color ACENTO = new Color(76, 215, 197);
    public static final Color ERROR = new Color(255, 145, 150);
    public static final Color EXITO = new Color(121, 225, 168);
    public static final Font UI = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font MONO = fuenteCodigo();

    private Estilo() { }

    private static Font fuenteCodigo() {
        Font fuente = new Font("Consolas", Font.PLAIN, 15);
        return fuente.getFamily().equalsIgnoreCase("Dialog")
                ? new Font(Font.MONOSPACED, Font.PLAIN, 15) : fuente;
    }

    public static void instalar() {
        System.setProperty("flatlaf.useNativeLibrary", "false");
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            throw new IllegalStateException("Falta el tema visual incluido. Inicia con ABRIR_GUI.cmd.", e);
        }
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TabbedPane.tabHeight", 36);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("Component.focusColor", ACENTO);
        UIManager.put("Table.showHorizontalLines", true);
        String[] fondos = {"Panel.background", "Viewport.background", "TabbedPane.background",
                "Table.background", "TextArea.background", "TextPane.background",
                "List.background", "ComboBox.background", "ScrollPane.background"};
        for (String clave : fondos) UIManager.put(clave, PANEL);
        String[] textos = {"Label.foreground", "Table.foreground", "TableHeader.foreground",
                "TextArea.foreground", "TextPane.foreground", "TabbedPane.foreground",
                "List.foreground", "ComboBox.foreground", "Button.foreground"};
        for (String clave : textos) UIManager.put(clave, TEXTO);
        for (String clave : new String[] {"Label.font", "Button.font", "Table.font",
                "TableHeader.font", "TabbedPane.font", "ComboBox.font", "List.font"}) {
            UIManager.put(clave, UI);
        }
        UIManager.put("TableHeader.background", BORDE);
        UIManager.put("TabbedPane.selected", BORDE);
        UIManager.put("TabbedPane.focus", ACENTO);
        UIManager.put("Table.selectionBackground", new Color(44, 82, 91));
        UIManager.put("Table.selectionForeground", TEXTO);
        UIManager.put("Button.background", BORDE);
        UIManager.put("Button.disabledText", MUTED);
        UIManager.put("SplitPane.background", FONDO);
        UIManager.put("SplitPane.dividerSize", 10);
        UIManager.put("ToolTip.background", PANEL);
        UIManager.put("ToolTip.foreground", TEXTO);
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("OptionPane.messageForeground", TEXTO);
    }

    public static JTable tabla() {
        JTable tabla = new JTable();
        tabla.setFont(UI);
        tabla.setRowHeight(30);
        tabla.setBackground(PANEL);
        tabla.setForeground(TEXTO);
        tabla.setGridColor(BORDE);
        tabla.setShowVerticalLines(false);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getAccessibleContext().setAccessibleName("Tabla de datos del compilador");
        return tabla;
    }

    public static JScrollPane scroll(Component contenido) {
        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(BorderFactory.createLineBorder(BORDE));
        scroll.getViewport().setBackground(PANEL);
        scroll.setWheelScrollingEnabled(true);
        return scroll;
    }

    public static JLabel etiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(UI);
        etiqueta.setForeground(TEXTO);
        return etiqueta;
    }

    public static JTextArea codigo() {
        JTextArea area = new JTextArea();
        area.setFont(MONO);
        area.setBackground(PANEL);
        area.setForeground(TEXTO);
        area.setCaretColor(ACENTO);
        area.setSelectionColor(new Color(43, 76, 94));
        area.setTabSize(4);
        area.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        return area;
    }

    public static JButton boton(String texto, boolean principal) {
        JButton boton = new JButton(texto);
        boton.setFont(UI);
        boton.setFocusPainted(true);
        boton.setBackground(principal ? ACENTO : BORDE);
        boton.setForeground(principal ? FONDO : TEXTO);
        boton.setMargin(new Insets(9, 14, 9, 14));
        boton.putClientProperty("JButton.buttonType", "roundRect");
        return boton;
    }
}
