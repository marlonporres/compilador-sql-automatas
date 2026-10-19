package ui;

import javax.swing.Timer;

/** Comprobación opcional del arranque y el flujo completo del paquete distribuido. */
final class DiagnosticoArranque {
    private DiagnosticoArranque() { }

    static void verificar(VentanaPrincipal ventana) {
        if (ventana.editorFuente().texto().isBlank() || ventana.editorReferencia().texto().isBlank()) {
            throw new IllegalStateException("El paquete abrió la ventana pero no pudo cargar los ejemplos.");
        }
        long limite = System.nanoTime() + 30_000_000_000L;
        int[] etapa = {0};
        Timer seguimiento = new Timer(40, null);
        seguimiento.addActionListener(evento -> {
            try {
                if (System.nanoTime() > limite) {
                    throw new IllegalStateException("El flujo gráfico no terminó antes de 30 segundos.");
                }
                if (ventana.controlador().estaOcupado()) return;
                switch (etapa[0]) {
                    case 0 -> {
                        etapa[0] = 1;
                        ventana.botonAnalizar().doClick();
                    }
                    case 1 -> {
                        exigir(ventana.botonEjecutar().isEnabled(), "El ejemplo no superó el análisis.");
                        System.out.println("ARRANQUE: análisis válido y ejecución habilitada.");
                        etapa[0] = 2;
                        ventana.botonEjecutar().doClick();
                    }
                    case 2 -> {
                        exigir(ventana.textoEstado().equals("Ejecución completada"),
                                "Falló la ejecución del ejemplo: " + ventana.textoEstado());
                        System.out.println("ARRANQUE: ejecución real SQL completada.");
                        etapa[0] = 3;
                        ventana.botonComparar().doClick();
                    }
                    case 3 -> {
                        exigir(ventana.textoEstado().equals("Comparación aprobada"),
                                "Falló la comparación: " + ventana.textoEstado());
                        seguimiento.stop();
                        ventana.cerrarVentana();
                        System.out.println("ARRANQUE VERIFICADO: ventana, ejemplos, análisis, ejecución y comparación SQL.");
                        System.exit(0);
                    }
                    default -> throw new IllegalStateException("Etapa de diagnóstico inesperada.");
                }
            } catch (RuntimeException | LinkageError e) {
                seguimiento.stop();
                ventana.cerrarVentana();
                AplicacionGUI.informarFallo(e, true);
            }
        });
        seguimiento.start();
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalStateException(mensaje);
    }
}
