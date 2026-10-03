package semantic;

import java.util.ArrayList;
import java.util.List;

import errors.Diagnostico;
import lexer.TipoToken;
import lexer.Token;
import parser.AnalizadorSintactico;
import parser.ColumnaDDL;
import parser.SentenciaDML;
import parser.SentenciaSQL;
import symbol_table.CatalogoEsquema;
import symbol_table.TablaDeclarada;

/**
 * API de la contribución DML, adaptada al parser y la semántica comunes.
 * Conserva el catálogo del equipo y los códigos SEM-201 a SEM-205.
 * No ejecuta consultas ni modifica filas o declaraciones del catálogo.
 */
public final class SemanticaDML {
    private final CatalogoEsquema catalogo;
    private final TablaErrores tablaErrores;

    public SemanticaDML(CatalogoEsquema catalogo, TablaErrores tablaErrores) {
        if (catalogo == null) throw new IllegalArgumentException("El catálogo no puede ser null");
        this.catalogo = catalogo;
        this.tablaErrores = tablaErrores == null ? new TablaErrores() : tablaErrores;
    }

    public SemanticaDML(CatalogoEsquema catalogo) {
        this(catalogo, new TablaErrores());
    }

    /** Solo los AST sintácticamente válidos llegan al análisis semántico. */
    public List<ErrorCompilacion> analizar(List<Token> tokens) {
        List<SentenciaSQL> sentencias = new AnalizadorSintactico().analizar(tokens)
                .getSentencias().stream().filter(SentenciaDML.class::isInstance).toList();
        ResultadoSemantico resultado = new AnalizadorSemantico().analizar(sentencias, copiarCatalogo());
        List<ErrorCompilacion> encontrados = new ArrayList<>();
        for (Diagnostico error : resultado.errores()) {
            encontrados.add(new ErrorCompilacion("SEMANTICA", codigoCompatible(error),
                    error.getMensaje(), error.getLexema(), error.getLinea(), error.getColumna()));
        }
        tablaErrores.agregarTodos(encontrados);
        return encontrados;
    }

    /**
     * Conserva la firma anterior. El parser verifica los límites de cada
     * sentencia; no se descartan sentencias válidas por compartir una línea
     * con otra inválida. Los mensajes recibidos no sustituyen esa verificación.
     */
    public List<ErrorCompilacion> analizar(List<Token> tokens, List<String> erroresSintacticos) {
        return analizar(tokens);
    }

    public TablaErrores getTablaErrores() { return tablaErrores; }
    public CatalogoEsquema getCatalogo() { return catalogo; }

    private CatalogoSemantico copiarCatalogo() {
        CatalogoSemantico copia = new CatalogoSemantico();
        for (TablaDeclarada tabla : catalogo.obtenerTablas()) {
            List<ColumnaDDL> columnas = tabla.getColumnas().stream()
                    .map(columna -> new ColumnaDDL(columna.getNombre(), columna.getTipoDato(),
                            columna.getLinea(), columna.getColumna())).toList();
            copia.crear(new Token(TipoToken.IDENTIFICADOR, tabla.getNombre(),
                    tabla.getLinea(), tabla.getColumna()), columnas);
        }
        return copia;
    }

    private static String codigoCompatible(Diagnostico error) {
        return switch (error.getCodigo()) {
            case "SEM-003" -> "SEM-201";
            case "SEM-004" -> "SEM-202";
            case "SEM-005", "SEM-006" -> "SEM-203";
            case "SEM-008" -> error.getMensaje().contains("admiten solo") ? "SEM-205" : "SEM-204";
            default -> "SEM-204";
        };
    }
}
