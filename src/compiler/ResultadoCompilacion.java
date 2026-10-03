package compiler;

import java.util.List;
import errors.TablaErrores;
import lexer.Token;
import parser.SentenciaSQL;
import semantic.CatalogoSemantico;
import symbol_table.TablaSimbolos;

/** Resultado verificable del recorrido archivo -> léxico -> sintaxis -> semántica. */
public record ResultadoCompilacion(List<Token> tokens, TablaSimbolos simbolosLexicos,
        List<SentenciaSQL> sentencias, CatalogoSemantico catalogo, TablaErrores errores,
        int sentenciasValidas) {
    public ResultadoCompilacion {
        tokens = List.copyOf(tokens);
        sentencias = List.copyOf(sentencias);
    }
    public boolean esValido() { return errores.estaVacia(); }
}
