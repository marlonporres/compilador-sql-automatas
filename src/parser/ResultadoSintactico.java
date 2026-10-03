package parser;

import java.util.List;

import errors.Diagnostico;

/** Resultado del parser completo, conservando el orden de las sentencias. */
public final class ResultadoSintactico {

    private final List<SentenciaSQL> sentencias;
    private final List<Diagnostico> errores;

    public ResultadoSintactico(List<SentenciaSQL> sentencias, List<Diagnostico> errores) {
        this.sentencias = List.copyOf(sentencias == null ? List.of() : sentencias);
        this.errores = List.copyOf(errores == null ? List.of() : errores);
    }

    public List<SentenciaSQL> getSentencias() {
        return sentencias;
    }

    public List<Diagnostico> getErrores() {
        return errores;
    }
}
