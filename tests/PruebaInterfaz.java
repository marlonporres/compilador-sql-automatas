import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import ui.Estilo;
import ui.VentanaPrincipal;

/** Verifica los botones y trabajadores reales de Swing; requiere escritorio o Xvfb. */
public final class PruebaInterfaz {
    private static int total;
    private static int fallidas;
    private static VentanaPrincipal ventana;
    private static boolean capturas;

    private PruebaInterfaz() { }

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("PruebaInterfaz requiere escritorio; en Linux usa xvfb-run.");
        }
        capturas = args.length == 1 && args[0].equals("--capturas");
        try {
            enUI(() -> {
                Estilo.instalar();
                ventana = new VentanaPrincipal(new execution.ServicioEjecucion());
                ventana.setSize(1360, 900);
                ventana.addNotify();
                ventana.validate();
                return null;
            });
            comprobar("editor vacio bloquea analizar", !enUI(() -> ventana.botonAnalizar().isEnabled()));
            comprobar("editor vacio bloquea ejecutar", !enUI(() -> ventana.botonEjecutar().isEnabled()));
            comprobar("editor vacio bloquea comparar", !enUI(() -> ventana.botonComparar().isEnabled()));

            enUI(() -> { ventana.cargarEjemplo(false); return null; });
            comprobar("carga ejemplo fuente", enUI(() -> ventana.editorFuente().texto().contains("CREAR TABLA")));
            comprobar("carga referencia independiente", enUI(() -> ventana.editorReferencia().texto().contains("CREATE TABLE")));
            comprobar("cargar no habilita ejecutar sin analizar", !enUI(() -> ventana.botonEjecutar().isEnabled()));
            enUI(() -> {
                ventana.botonAnalizar().doClick();
                comprobar("ocupado bloquea repetir analizar", !ventana.botonAnalizar().isEnabled());
                comprobar("ocupado bloquea editar", !ventana.editorFuente().campo().isEditable());
                return null;
            });
            esperar();
            comprobar("analisis valido habilita ejecutar", enUI(() -> ventana.botonEjecutar().isEnabled()));
            comprobar("analisis valido muestra tokens", enUI(() -> ventana.panelAnalisis().getTablaTokens().getRowCount() > 30));
            comprobar("analisis valido sin errores", enUI(() -> ventana.panelAnalisis().cantidadErrores() == 0));
            comprobar("referencia permite comparar", enUI(() -> ventana.botonComparar().isEnabled()));
            Thread.sleep(300);
            comprobar("colorear editor no invalida analisis", enUI(() -> ventana.botonEjecutar().isEnabled()));
            capturar("01-analisis");

            enUI(() -> { ventana.botonComparar().doClick(); return null; });
            esperar();
            comprobar("comparacion directa aprobada", enUI(() -> ventana.textoEstado().equals("Comparación aprobada")));
            enUI(() -> {
                JComboBox<?> selector = componentes(ventana.panelResultados(), JComboBox.class).get(0);
                comprobar("comparar muestra todas las sentencias", selector.getItemCount() == 19);
                selector.setSelectedIndex(6);
                return null;
            });
            comprobar("consulta real muestra dos filas", enUI(() ->
                    componentes(ventana.panelResultados(), JTable.class).get(0).getRowCount() == 2));
            capturar("02-resultados");

            enUI(() -> { ventana.editorReferencia().texto(ventana.editorReferencia().texto() + "\n-- referencia editada"); return null; });
            comprobar("editar referencia invalida aprobacion anterior", enUI(() -> ventana.textoEstado().equals("Referencia modificada")));
            comprobar("editar referencia conserva analisis fuente", enUI(() -> ventana.botonEjecutar().isEnabled()));
            comprobar("editar referencia limpia resultado anterior", enUI(() ->
                    componentes(ventana.panelResultados(), JTable.class).get(0).getRowCount() == 0));

            enUI(() -> { ventana.botonEjecutar().doClick(); return null; });
            esperar();
            comprobar("ejecutar funciona despues de comparar", enUI(() -> ventana.textoEstado().equals("Ejecución completada")));
            comprobar("operacion restablece edicion", enUI(() -> ventana.editorFuente().campo().isEditable()));
            comprobar("script nuevo reinicia base sin duplicados", enUI(() -> ventana.panelAnalisis().cantidadErrores() == 0));

            enUI(() -> { ventana.editorFuente().texto(ventana.editorFuente().texto() + "\n-- editado"); return null; });
            comprobar("editar invalida ejecutar", !enUI(() -> ventana.botonEjecutar().isEnabled()));
            comprobar("editar invalida comparar", !enUI(() -> ventana.botonComparar().isEnabled()));
            comprobar("editar limpia diagnosticos anteriores", enUI(() -> ventana.panelAnalisis().getTablaTokens().getRowCount() == 0));

            enUI(() -> { ventana.cargarEjemplo(true); ventana.botonAnalizar().doClick(); return null; });
            esperar();
            comprobar("errores se muestran en tabla", enUI(() -> ventana.panelAnalisis().cantidadErrores() == 8));
            comprobar("errores bloquean toda ejecucion", !enUI(() -> ventana.botonEjecutar().isEnabled()));
            comprobar("errores bloquean comparacion", !enUI(() -> ventana.botonComparar().isEnabled()));
            enUI(() -> { ventana.panelAnalisis().getTablaErrores().setRowSelectionInterval(0, 0); return null; });
            comprobar("seleccionar error mueve el cursor", enUI(() -> ventana.editorFuente().campo().getCaretPosition() > 0));
            capturar("03-errores");

            String valido = "CREAR TABLA t (id ENTERO); INSERTAR EN t VALORES (1); SELECCIONAR * DESDE t;";
            enUI(() -> {
                ventana.editorFuente().texto(valido);
                ventana.editorReferencia().texto("CREATE TABLE T (ID INT); INSERT INTO T VALUES (2); SELECT * FROM T;");
                ventana.botonAnalizar().doClick();
                return null;
            });
            esperar();
            enUI(() -> { ventana.botonComparar().doClick(); return null; });
            esperar();
            comprobar("comparacion detecta datos diferentes", enUI(() -> ventana.textoEstado().equals("Hay diferencias")));
            comprobar("comparacion distinta devuelve controles", enUI(() -> ventana.botonEjecutar().isEnabled()));

            String errorMotor = "CREAR TABLA t (id ENTERO);\nINSERTAR EN t VALORES (" + "9".repeat(1100) + ");";
            enUI(() -> { ventana.editorFuente().texto(errorMotor); ventana.botonAnalizar().doClick(); return null; });
            esperar();
            comprobar("literal grande llega a limite del motor", enUI(() -> ventana.botonEjecutar().isEnabled()));
            enUI(() -> { ventana.botonEjecutar().doClick(); return null; });
            esperar();
            comprobar("error del motor no cierra ventana", enUI(() -> ventana.textoEstado().equals("Error de ejecución")));
            comprobar("error del motor visible", enUI(() -> ventana.panelAnalisis().cantidadErrores() >= 1));
            comprobar("error del motor restablece controles", enUI(() -> ventana.botonAnalizar().isEnabled()));
            enUI(() -> { ventana.botonLimpiar().doClick(); return null; });
            comprobar("limpiar borra editor", enUI(() -> ventana.editorFuente().texto().isEmpty()));
            comprobar("limpiar borra referencia", enUI(() -> ventana.editorReferencia().texto().isEmpty()));
            comprobar("limpiar borra tablas", enUI(() -> ventana.panelAnalisis().cantidadErrores() == 0));
            comprobar("limpiar deshabilita ejecutar", !enUI(() -> ventana.botonEjecutar().isEnabled()));
            AtomicReference<Throwable> errorPintura = new AtomicReference<>();
            Thread.UncaughtExceptionHandler manejadorAnterior = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> errorPintura.set(error));
            try {
                enUI(() -> { ventana.editorFuente().texto("'" + "x".repeat(50000) + "'"); return null; });
                Thread.sleep(500);
                comprobar("resaltar literal largo no desborda la pila", errorPintura.get() == null);
                enUI(() -> { ventana.editorFuente().texto("'" + "''".repeat(25000) + "'"); return null; });
                Thread.sleep(500);
                comprobar("resaltar escapes largos mantiene interfaz disponible", errorPintura.get() == null
                        && enUI(() -> ventana.editorFuente().campo().isEditable()));
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(manejadorAnterior);
                enUI(() -> { ventana.botonLimpiar().doClick(); return null; });
            }
            enUI(() -> {
                ventana.setSize(1000, 690);
                ventana.validate();
                comprobar("botones caben en tamano minimo", ventana.botonLimpiar().getY()
                        == ventana.botonAnalizar().getY());
                return null;
            });
            capturar("04-ventana-compacta");
        } finally {
            if (ventana != null) enUI(() -> { ventana.cerrarVentana(); return null; });
        }
        comprobar("arranque completo desde otra carpeta", verificarArranqueDesdeOtraCarpeta());
        persistenciaEnVentana();
        System.out.println("Pruebas Interfaz: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) System.exit(1);
    }

    private static void persistenciaEnVentana() throws Exception {
        Path carpeta = Files.createTempDirectory("sqlstudio-gui-");
        Path archivo = carpeta.resolve("datos/compilador");
        try {
            enUI(() -> {
                ventana = new VentanaPrincipal(new execution.ServicioEjecucion(archivo));
                ventana.editorFuente().texto("CREAR TABLA alumnos (id ENTERO, nombre TEXTO); "
                        + "INSERTAR EN alumnos VALORES (1, 'Ana');");
                ventana.botonAnalizar().doClick();
                return null;
            });
            esperar();
            comprobar("persistencia: analizar no crea tablas", new execution.ServicioEjecucion(archivo)
                    .analizar("").catalogo().tamano() == 0);
            enUI(() -> { ventana.botonEjecutar().doClick(); return null; });
            esperar();
            comprobar("persistencia: crear e insertar desde GUI", enUI(() -> ventana.textoEstado().equals("Ejecución completada")));
            enUI(() -> {
                ventana.botonLimpiar().doClick();
                ventana.editorFuente().texto("SELECCIONAR nombre DESDE alumnos;");
                ventana.botonAnalizar().doClick();
                return null;
            });
            esperar();
            comprobar("persistencia: limpiar y analizar conservan tabla", enUI(() -> ventana.botonEjecutar().isEnabled()));
            enUI(() -> { ventana.botonEjecutar().doClick(); return null; });
            esperar();
            comprobar("persistencia: GUI recupera fila guardada", enUI(() ->
                    "Ana".equals(componentes(ventana.panelResultados(), JTable.class).getFirst().getValueAt(0, 0))));
            enUI(() -> { ventana.cerrarVentana();
                ventana = new VentanaPrincipal(new execution.ServicioEjecucion(archivo));
                ventana.editorFuente().texto("SELECCIONAR * DESDE alumnos;");
                ventana.botonAnalizar().doClick();
                return null;
            });
            esperar();
            comprobar("persistencia: otra ventana reconoce tablas", enUI(() -> ventana.botonEjecutar().isEnabled()));
            enUI(() -> {
                ventana.editorFuente().texto("CREAR TABLA alumnos (id ENTERO);");
                ventana.editorReferencia().texto("CREATE TABLE alumnos (id INTEGER);");
                ventana.botonAnalizar().doClick();
                return null;
            });
            esperar();
            comprobar("persistencia: CREAR repetido bloquea ejecutar", !enUI(() -> ventana.botonEjecutar().isEnabled()));
            comprobar("persistencia: comparar permite ejemplo sobre base vacía", enUI(() -> ventana.botonComparar().isEnabled()));
            enUI(() -> { ventana.botonComparar().doClick(); return null; });
            esperar();
            comprobar("persistencia: comparación independiente aprobada", enUI(() -> ventana.textoEstado().equals("Comparación aprobada")));
            comprobar("persistencia: comparación requiere analizar antes de ejecutar", !enUI(() -> ventana.botonEjecutar().isEnabled()));
            var informe = new execution.ServicioEjecucion(archivo).ejecutar("SELECCIONAR nombre DESDE alumnos;");
            comprobar("persistencia: comparación no modifica fila real", informe.esExitoso()
                    && informe.resultados().getFirst().filas().equals(List.of(List.of("Ana"))));
        } finally {
            if (ventana != null) enUI(() -> { ventana.cerrarVentana(); return null; });
            try (var archivos = Files.walk(carpeta)) {
                for (Path ruta : archivos.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(ruta);
            }
        }
    }

    private static boolean verificarArranqueDesdeOtraCarpeta() throws Exception {
        String[] entradas = System.getProperty("java.class.path").split(
                java.util.regex.Pattern.quote(File.pathSeparator));
        List<String> rutas = new ArrayList<>();
        for (String entrada : entradas) rutas.add(Path.of(entrada).toAbsolutePath().normalize().toString());
        String ejecutable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Process proceso = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", ejecutable).toString(),
                "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                "-cp", String.join(File.pathSeparator, rutas),
                "ui.AplicacionGUI", "--verificar-arranque")
                .directory(Path.of(System.getProperty("java.io.tmpdir")).toFile())
                .redirectErrorStream(true).start();
        boolean termino = proceso.waitFor(40, TimeUnit.SECONDS);
        if (!termino) { proceso.destroyForcibly(); proceso.waitFor(); }
        String salidaProceso = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean correcto = termino && proceso.exitValue() == 0
                && salidaProceso.contains("ARRANQUE VERIFICADO");
        if (!correcto) System.err.println(salidaProceso);
        return correcto;
    }

    private static void esperar() throws Exception {
        long limite = System.nanoTime() + 20_000_000_000L;
        while (enUI(() -> ventana.controlador().estaOcupado())) {
            if (System.nanoTime() > limite) throw new IllegalStateException("El trabajador grafico no termino.");
            Thread.sleep(20);
        }
    }

    private static <T> T enUI(Callable<T> tarea) throws Exception {
        AtomicReference<T> valor = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try { valor.set(tarea.call()); } catch (Exception e) { error.set(e); }
        });
        if (error.get() != null) throw error.get();
        return valor.get();
    }

    private static <T> List<T> componentes(Container origen, Class<T> tipo) {
        List<T> lista = new ArrayList<>();
        for (Component hijo : origen.getComponents()) {
            if (tipo.isInstance(hijo)) lista.add(tipo.cast(hijo));
            if (hijo instanceof Container contenedor) lista.addAll(componentes(contenedor, tipo));
        }
        return lista;
    }

    private static void capturar(String nombre) throws Exception {
        if (!capturas) return;
        Path carpeta = Path.of("output", "fase3", "capturas");
        Files.createDirectories(carpeta);
        enUI(() -> {
            ventana.validate();
            Container contenido = ventana.getContentPane();
            BufferedImage imagen = new BufferedImage(contenido.getWidth(), contenido.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D grafico = imagen.createGraphics();
            contenido.printAll(grafico);
            grafico.dispose();
            ImageIO.write(imagen, "png", carpeta.resolve(nombre + ".png").toFile());
            return null;
        });
    }

    private static void comprobar(String nombre, boolean condicion) {
        total++;
        if (!condicion) { fallidas++; System.err.println("FALLA: " + nombre); }
    }
}
