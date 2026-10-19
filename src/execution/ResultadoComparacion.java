package execution;

import java.util.List;

/** Comparación de consultas y estado final usando dos bases independientes. */
public record ResultadoComparacion(boolean coincide, List<String> detalles,
        InformeEjecucion compilador, List<ResultadoEjecucion> referencia) {
    public ResultadoComparacion {
        detalles = List.copyOf(detalles);
        referencia = List.copyOf(referencia);
    }
}
