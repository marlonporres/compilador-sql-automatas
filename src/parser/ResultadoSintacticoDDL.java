package parser;

import java.util.List;

/** Resultado completo del análisis DDL: sentencias válidas y diagnósticos. */
public final class ResultadoSintacticoDDL {

    private final List<SentenciaDDL> sentencias;
    private final List<ErrorSintactico> errores;

    public ResultadoSintacticoDDL(List<SentenciaDDL> sentencias, List<ErrorSintactico> errores) {
        this.sentencias = List.copyOf(sentencias == null ? List.of() : sentencias);
        this.errores = List.copyOf(errores == null ? List.of() : errores);
    }

    public List<SentenciaDDL> getSentencias() {
        return sentencias;
    }

    public List<ErrorSintactico> getErrores() {
        return errores;
    }

    public boolean esValido() {
        return errores.isEmpty();
    }

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }
}
