package semantic;

import java.util.List;
import errors.Diagnostico;

/** Resultado de validar las sentencias en su orden de aparición. */
public record ResultadoSemantico(CatalogoSemantico catalogo, List<Diagnostico> errores,
        int sentenciasValidas) {
    public ResultadoSemantico {
        errores = List.copyOf(errores);
    }
}
