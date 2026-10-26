package compiler;

import java.util.Comparator;
import java.util.List;
import errors.TablaErrores;
import lexer.ScannerSQL;
import lexer.TipoToken;
import lexer.Token;
import parser.AnalizadorSintactico;
import parser.ResultadoSintactico;
import semantic.AnalizadorSemantico;
import semantic.CatalogoSemantico;
import semantic.ResultadoSemantico;
import symbol_table.TablaSimbolos;

/** Conecta las tres fases con un catálogo vacío o una copia del esquema existente. */
public final class CompiladorSQL {
    public ResultadoCompilacion analizar(String entrada) {
        return analizar(entrada, new CatalogoSemantico());
    }

    public ResultadoCompilacion analizar(String entrada, CatalogoSemantico inicial) {
        ScannerSQL scanner = new ScannerSQL();
        scanner.analizar(entrada);
        List<Token> tokens = scanner.getTokens();
        TablaSimbolos simbolos = new TablaSimbolos();
        simbolos.registrarTodos(tokens);
        TablaErrores errores = new TablaErrores();
        tokens.stream().filter(t -> t.getTipo() == TipoToken.ERROR).forEach(errores::agregarLexico);
        ResultadoSintactico sintaxis = new AnalizadorSintactico().analizar(tokens);
        errores.agregarTodos(sintaxis.getErrores());
        ResultadoSemantico semantica = new AnalizadorSemantico().analizar(sintaxis.getSentencias(), inicial.copiar());
        errores.agregarTodos(semantica.errores());
        TablaErrores ordenadas = new TablaErrores();
        ordenadas.agregarTodos(errores.obtenerTodos().stream().sorted(Comparator
                .comparingInt(errors.Diagnostico::getLinea)
                .thenComparingInt(errors.Diagnostico::getColumna)).toList());
        return new ResultadoCompilacion(tokens, simbolos, sintaxis.getSentencias(),
                semantica.catalogo(), ordenadas, semantica.sentenciasValidas());
    }
}
