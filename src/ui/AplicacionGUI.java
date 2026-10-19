package ui;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Entrada gráfica independiente de Main, que mantiene el modo CLI. */
public final class AplicacionGUI {
    private AplicacionGUI() { }

    public static void main(String[] args) {
        boolean diagnostico = Arrays.asList(args).contains("--verificar-arranque");
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("La interfaz requiere un escritorio gráfico. Usa VERIFICAR_FASE3.cmd para las pruebas.");
            System.exit(1);
            return;
        }
        try {
            verificarDependencias();
        } catch (RuntimeException | LinkageError e) {
            informarFallo(e, diagnostico);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = null;
            try {
                Estilo.instalar();
                ventana = new VentanaPrincipal();
                ventana.setVisible(true);
                ventana.cargarEjemplo(false);
                if (diagnostico) DiagnosticoArranque.verificar(ventana);
            } catch (RuntimeException | LinkageError e) {
                if (ventana != null) ventana.cerrarVentana();
                informarFallo(e, diagnostico);
            }
        });
    }

    private static void verificarDependencias() {
        for (String dependencia : new String[] {"com.formdev.flatlaf.FlatDarkLaf", "org.h2.Driver"}) {
            try {
                Class.forName(dependencia);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("No se encontró " + dependencia
                        + ". Abre la carpeta completa compilador-sql-automatas en VS Code y usa SQL Studio"
                        + " - Interfaz grafica. También puedes abrir ABRIR_GUI.cmd."
                        + " Conserva todas las bibliotecas de lib.", e);
            }
        }
    }

    static void informarFallo(Throwable error, boolean diagnostico) {
        String mensaje = "SQL Studio no pudo iniciar. "
                + (error.getMessage() == null ? error.toString() : error.getMessage())
                + "\nRevisa la consola para ver el detalle. No ejecutes una clase aislada desde Archivos sueltos.";
        System.err.println(mensaje);
        error.printStackTrace(System.err);
        if (!diagnostico && !GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, mensaje, "SQL Studio · Revisar inicio", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(1);
    }
}
