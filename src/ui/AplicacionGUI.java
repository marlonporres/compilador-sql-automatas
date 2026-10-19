package ui;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;

/** Entrada gráfica independiente de Main, que mantiene el modo CLI. */
public final class AplicacionGUI {
    private AplicacionGUI() { }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("La interfaz requiere un escritorio gráfico. Usa VERIFICAR_FASE3.cmd para las pruebas.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            Estilo.instalar();
            VentanaPrincipal ventana = new VentanaPrincipal();
            ventana.setVisible(true);
            ventana.cargarEjemplo(false);
        });
    }
}
