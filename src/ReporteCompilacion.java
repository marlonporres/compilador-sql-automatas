package semantic;

import java.util.List;

/** Resultado común de una ejecución de las fases con sus diagnósticos. */
public final class ReporteCompilacion {
    private final List<ErrorCompilacion> errores;

    public ReporteCompilacion(List<ErrorCompilacion> errores) {
        this.errores = List.copyOf(errores == null ? List.of() : errores);
    }

    public List<ErrorCompilacion> getErrores() { return errores; }
    public boolean esValido() { return errores.isEmpty(); }
    public int cantidad() { return errores.size(); }

    public String toString() {
        if (errores.isEmpty()) {
            return "No se encontraron errores.";
        }
        return errores.stream().map(ErrorCompilacion::toString)
                .reduce((a,b) -> a + System.lineSeparator() + b).orElse("");
    }
}
