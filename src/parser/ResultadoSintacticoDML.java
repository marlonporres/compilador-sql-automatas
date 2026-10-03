package parser;

import java.util.List;

/** Resultado del parser DML. */
public final class ResultadoSintacticoDML {

    private final List<SentenciaDML> sentencias;
    private final List<ErrorSintactico> errores;

    public ResultadoSintacticoDML(List<SentenciaDML> sentencias, List<ErrorSintactico> errores) {
        this.sentencias = List.copyOf(sentencias == null ? List.of() : sentencias);
        this.errores = List.copyOf(errores == null ? List.of() : errores);
    }

    public List<SentenciaDML> getSentencias() {
        return sentencias;
    }

    public List<ErrorSintactico> getErrores() {
        return errores;
    }
}
