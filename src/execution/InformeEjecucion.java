package execution;

import java.util.List;
import compiler.ResultadoCompilacion;

/** El análisis completo siempre precede a cualquier modificación de datos. */
public record InformeEjecucion(ResultadoCompilacion analisis,
        List<ResultadoEjecucion> resultados, long duracionMs) {
    public InformeEjecucion { resultados = List.copyOf(resultados); }
    public boolean esExitoso() {
        return analisis.esValido() && !resultados.isEmpty()
                && resultados.stream().allMatch(ResultadoEjecucion::esExitoso);
    }
}
