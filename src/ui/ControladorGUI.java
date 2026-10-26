package ui;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.SwingWorker;
import compiler.ResultadoCompilacion;
import execution.ServicioEjecucion;

/** Estado de la sesión visual y coordinación de operaciones fuera del hilo gráfico. */
public final class ControladorGUI {
    private final VentanaPrincipal ventana;
    private final ServicioEjecucion servicio;
    private ResultadoCompilacion analisis;
    private String textoAnalizado;
    private boolean ocupado;
    private boolean cerrado;

    public ControladorGUI(VentanaPrincipal ventana, ServicioEjecucion servicio) {
        this.ventana = ventana;
        this.servicio = servicio;
    }

    public void contenidoModificado() {
        analisis = null;
        textoAnalizado = null;
        ventana.vaciarResultados();
        ventana.marcarEstado("Sin analizar", "El código cambió. Analiza el contenido actual.", Estilo.MUTED);
        actualizarBotones();
    }

    public void referenciaModificada() {
        ventana.panelResultados().limpiar();
        if (analisis != null && !ocupado) {
            ventana.marcarEstado("Referencia modificada", "Compara otra vez para verificar el SQL actual.", Estilo.MUTED);
        }
        actualizarBotones();
    }

    public void actualizarBotones() {
        boolean hayTexto = !ventana.editorFuente().texto().isBlank();
        boolean valido = analisis != null && analisis.esValido()
                && ventana.editorFuente().texto().equals(textoAnalizado);
        ventana.botonAnalizar().setEnabled(!ocupado && hayTexto);
        ventana.botonEjecutar().setEnabled(!ocupado && valido);
        ventana.botonComparar().setEnabled(!ocupado && (valido || servicio.esPersistente() && hayTexto)
                && !ventana.editorReferencia().texto().isBlank());
        ventana.botonLimpiar().setEnabled(!ocupado);
    }

    public void analizar() {
        if (ocupado || cerrado) return;
        String texto = ventana.editorFuente().texto();
        if (texto.isBlank()) { ventana.avisar("Ingresa un script antes de analizar."); return; }
        analisis = null;
        textoAnalizado = null;
        ventana.vaciarResultados();
        trabajar("Analizando", "Identificando tokens, estructura y tipos…",
                () -> servicio.analizar(texto), resultado -> {
                    aceptarAnalisis(texto, resultado);
                    ventana.marcarEstado(resultado.esValido() ? "Análisis válido" : "Revisar errores",
                            resultado.esValido() ? resultado.sentenciasValidas()
                                    + " sentencias válidas. Ya puedes ejecutar el script."
                                    : resultado.errores().tamano() + " errores. Selecciona uno para ubicarlo.",
                            resultado.esValido() ? Estilo.EXITO : Estilo.ERROR);
                });
    }

    private void aceptarAnalisis(String texto, ResultadoCompilacion resultado) {
        if (!texto.equals(ventana.editorFuente().texto())) return;
        textoAnalizado = texto;
        analisis = resultado;
        ventana.mostrarAnalisis(resultado);
    }

    public void ejecutar() {
        if (!puedeEjecutar()) return;
        String texto = ventana.editorFuente().texto();
        trabajar("Ejecutando", "Validando y ejecutando en " + servicio.ubicacion(),
                () -> servicio.ejecutar(texto), informe -> {
                    aceptarAnalisis(texto, informe.analisis());
                    ventana.mostrarEjecucion(informe);
                    ventana.marcarEstado(informe.esExitoso() ? "Ejecución completada" : "Error de ejecución",
                            informe.esExitoso() ? informe.resultados().size() + " sentencias ejecutadas en "
                                    + informe.duracionMs() + " ms."
                                    : "Se detuvo el script. Revisa el mensaje y las sentencias ejecutadas.",
                            informe.esExitoso() ? Estilo.EXITO : Estilo.ERROR);
                });
    }

    public void comparar() {
        if (ocupado || cerrado || ventana.editorFuente().texto().isBlank()) return;
        if (!servicio.esPersistente() && !puedeEjecutar()) return;
        String fuente = ventana.editorFuente().texto();
        String referencia = ventana.editorReferencia().texto();
        if (referencia.isBlank()) { ventana.avisar("Escribe el SQL equivalente en SQL de referencia."); return; }
        trabajar("Comparando", "Ejecutando ambos scripts en bases independientes…",
                () -> servicio.comparar(fuente, referencia), comparacion -> {
                    if (servicio.esPersistente()) {
                        // La comparación analiza contra bases vacías, no contra los datos guardados.
                        analisis = null;
                        textoAnalizado = null;
                        ventana.mostrarAnalisis(comparacion.compilador().analisis());
                    } else aceptarAnalisis(fuente, comparacion.compilador().analisis());
                    ventana.mostrarComparacion(comparacion);
                    ventana.marcarEstado(comparacion.coincide() ? "Comparación aprobada" : "Hay diferencias",
                            String.join(" · ", comparacion.detalles()),
                            comparacion.coincide() ? Estilo.EXITO : Estilo.ERROR);
                });
    }

    private boolean puedeEjecutar() {
        return !ocupado && !cerrado && analisis != null && analisis.esValido()
                && ventana.editorFuente().texto().equals(textoAnalizado);
    }

    private <T> void trabajar(String estado, String mensaje, Supplier<T> tarea, Consumer<T> terminar) {
        ocupado = true;
        ventana.ocupado(true);
        ventana.marcarEstado(estado, mensaje, Estilo.ACENTO);
        actualizarBotones();
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() { return tarea.get(); }
            @Override protected void done() {
                if (cerrado) return;
                try {
                    terminar.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    ventana.avisar("La operación no pudo completar su respuesta.");
                } catch (ExecutionException | RuntimeException e) {
                    Throwable causa = e.getCause() == null ? e : e.getCause();
                    ventana.avisar(causa.getMessage() == null ? causa.toString() : causa.getMessage());
                } finally {
                    ocupado = false;
                    ventana.ocupado(false);
                    actualizarBotones();
                }
            }
        }.execute();
    }

    public void limpiar() {
        if (ocupado) return;
        ventana.editorFuente().texto("");
        ventana.editorReferencia().texto("");
        contenidoModificado();
        ventana.marcarEstado("Sin analizar", "Editor y paneles vacíos. Puedes comenzar otro script.", Estilo.MUTED);
    }

    /** No interrumpe JDBC: cada operación cierra su conexión al terminar. */
    public void cerrar() { cerrado = true; }
    public boolean estaOcupado() { return ocupado; }
    public ResultadoCompilacion ultimoAnalisis() { return analisis; }
}
