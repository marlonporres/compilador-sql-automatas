package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** Editor con numeración y resaltado visual; la validación pertenece al compilador. */
public final class EditorSQL extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Pattern COLORES = Pattern.compile(
            "/\\*[\\s\\S]*?\\*/|--[^\\r\\n]*|'(?:''|[^'])*'|"
            + "\\b(?i:CREAR|TABLA|ALTERAR|AGREGAR|MODIFICAR|QUITAR|ELIMINAR|TRUNCAR|RENOMBRAR|A|"
            + "SELECCIONAR|DESDE|INSERTAR|EN|VALORES|ACTUALIZAR|ASIGNAR|BORRAR|DONDE|Y|O|NO|"
            + "ENTERO|DECIMAL|TEXTO|FECHA|BOOLEANO|VERDADERO|FALSO|CREATE|TABLE|SELECT|FROM|"
            + "INSERT|INTO|VALUES|UPDATE|SET|DELETE|WHERE|ALTER|DROP|TRUNCATE|RENAME|TO)\\b|"
            + "\\b[0-9]+(?:\\.[0-9]+)?\\b");
    private final JTextPane editor = new JTextPane() {
        private static final long serialVersionUID = 1L;
        @Override public boolean getScrollableTracksViewportWidth() { return false; }
    };
    private final JTextArea numeros = new JTextArea("1");
    private final Timer resaltado;
    private transient Runnable alEditar = () -> { };

    public EditorSQL(String nombreAccesible) {
        super(new BorderLayout());
        setBackground(Estilo.PANEL);
        editor.setFont(Estilo.MONO);
        editor.setBackground(Estilo.PANEL);
        editor.setForeground(Estilo.TEXTO);
        editor.setCaretColor(Estilo.ACENTO);
        editor.setSelectionColor(new Color(44, 82, 91));
        editor.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        editor.getAccessibleContext().setAccessibleName(nombreAccesible);
        numeros.setEditable(false);
        numeros.setFocusable(false);
        numeros.setFont(Estilo.MONO);
        numeros.setBackground(Estilo.FONDO);
        numeros.setForeground(Estilo.MUTED);
        numeros.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        JScrollPane scroll = Estilo.scroll(editor);
        scroll.setRowHeaderView(numeros);
        add(scroll);
        resaltado = new Timer(180, e -> colorear());
        resaltado.setRepeats(false);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { cambiar(); }
            @Override public void removeUpdate(DocumentEvent e) { cambiar(); }
            @Override public void changedUpdate(DocumentEvent e) { /* Solo cambia el color. */ }
        });
    }

    private void cambiar() {
        int cantidad = editor.getDocument().getDefaultRootElement().getElementCount();
        StringBuilder texto = new StringBuilder();
        for (int i = 1; i <= cantidad; i++) texto.append(i).append('\n');
        numeros.setText(texto.toString());
        resaltado.restart();
        alEditar.run();
    }

    private void colorear() {
        String texto = editor.getText();
        StyledDocument documento = editor.getStyledDocument();
        SimpleAttributeSet normal = new SimpleAttributeSet();
        StyleConstants.setForeground(normal, Estilo.TEXTO);
        StyleConstants.setBold(normal, false);
        documento.setCharacterAttributes(0, texto.length(), normal, true);
        Matcher matcher = COLORES.matcher(texto);
        while (matcher.find()) {
            String token = matcher.group();
            SimpleAttributeSet estilo = new SimpleAttributeSet();
            Color color = token.startsWith("--") || token.startsWith("/*") ? Estilo.MUTED
                    : token.startsWith("'") ? new Color(225, 192, 129)
                    : Character.isDigit(token.charAt(0)) ? new Color(183, 172, 245) : Estilo.ACENTO;
            StyleConstants.setForeground(estilo, color);
            documento.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), estilo, false);
        }
    }

    public JTextPane campo() { return editor; }
    public String texto() { return editor.getText(); }
    public void texto(String texto) { editor.setText(texto); editor.setCaretPosition(0); }
    public void alEditar(Runnable accion) { alEditar = accion; }
    public void editable(boolean editable) { editor.setEditable(editable); }

    public void irA(int linea, int columna) {
        javax.swing.text.Element raiz = editor.getDocument().getDefaultRootElement();
        int indice = Math.max(0, Math.min(linea - 1, raiz.getElementCount() - 1));
        javax.swing.text.Element fila = raiz.getElement(indice);
        int posicion = Math.min(fila.getEndOffset() - 1,
                fila.getStartOffset() + Math.max(0, columna - 1));
        editor.setCaretPosition(Math.min(posicion, editor.getDocument().getLength()));
        editor.requestFocusInWindow();
    }
}
