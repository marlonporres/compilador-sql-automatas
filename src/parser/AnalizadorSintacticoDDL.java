package parser;

import java.util.ArrayList;
import java.util.List;

import lexer.TipoToken;
import lexer.Token;

/** Analizador sintáctico descendente para las sentencias DDL del proyecto. */
public class AnalizadorSintacticoDDL {

    private CursorTokens cursor;
    private List<ErrorSintactico> errores;

    public ResultadoSintacticoDDL analizar(List<Token> tokens) {
        cursor = new CursorTokens(tokens);
        errores = new ArrayList<>();
        List<SentenciaDDL> sentencias = new ArrayList<>();

        while (!cursor.estaAlFinal()) {
            if (cursor.coincidir(TipoToken.PUNTO_Y_COMA)) {
                error("SYN-001", "Se esperaba el inicio de una sentencia DDL", "PUNTO_Y_COMA");
                continue;
            }

            SentenciaDDL sentencia = analizarSentencia();
            if (sentencia == null) {
                cursor.recuperarHastaFinDeSentencia();
                continue;
            }

            if (!cursor.coincidir(TipoToken.PUNTO_Y_COMA)) {
                error("SYN-002", "Se esperaba ';' al final de la sentencia", "PUNTO_Y_COMA");
                cursor.recuperarHastaFinDeSentencia();
                continue;
            }
            sentencias.add(sentencia);
        }

        return new ResultadoSintacticoDDL(sentencias, errores);
    }

    protected SentenciaDDL analizarSentencia() {
        return switch (cursor.ver().getTipo()) {
            case CREAR -> analizarCrearTabla();
            case ALTERAR -> analizarSentenciaSimple(TipoSentenciaDDL.ALTERAR_TABLA);
            case ELIMINAR -> analizarSentenciaSimple(TipoSentenciaDDL.ELIMINAR_TABLA);
            case TRUNCAR -> analizarSentenciaSimple(TipoSentenciaDDL.TRUNCAR_TABLA);
            case RENOMBRAR -> analizarSentenciaSimple(TipoSentenciaDDL.RENOMBRAR_TABLA);
            case ERROR -> {
                error("SYN-003", "No se puede analizar un token léxicamente inválido", cursor.ver());
                cursor.avanzar();
                yield null;
            }
            default -> {
                error("SYN-004", "Se esperaba una sentencia DDL", cursor.ver());
                cursor.avanzar();
                yield null;
            }
        };
    }

    private SentenciaDDL analizarCrearTabla() {
        Token inicio = cursor.avanzar();
        if (!esperar(TipoToken.TABLA, "TABLA")) {
            return null;
        }
        Token tabla = esperarIdentificador("el nombre de la tabla");
        if (tabla == null || !esperar(TipoToken.PARENTESIS_IZQ, "'('")) {
            return null;
        }

        List<ColumnaDDL> columnas = new ArrayList<>();
        ColumnaDDL primera = analizarColumna();
        if (primera == null) {
            return null;
        }
        columnas.add(primera);

        while (cursor.coincidir(TipoToken.COMA)) {
            ColumnaDDL columna = analizarColumna();
            if (columna == null) {
                return null;
            }
            columnas.add(columna);
        }

        if (!esperar(TipoToken.PARENTESIS_DER, "')'")) {
            return null;
        }
        return new SentenciaDDL(TipoSentenciaDDL.CREAR_TABLA, tabla.getLexema(),
                columnas, inicio.getlinea(), inicio.getcolumna());
    }

    private ColumnaDDL analizarColumna() {
        Token nombre = esperarIdentificador("el nombre de la columna");
        if (nombre == null) {
            return null;
        }
        TipoToken tipo = cursor.ver().getTipo();
        if (!esTipoDato(tipo)) {
            error("SYN-005", "Se esperaba un tipo de dato después de la columna", cursor.ver());
            return null;
        }
        cursor.avanzar();
        return new ColumnaDDL(nombre.getLexema(), tipo, nombre.getlinea(), nombre.getcolumna());
    }

    private SentenciaDDL analizarSentenciaSimple(TipoSentenciaDDL tipo) {
        Token inicio = cursor.avanzar();
        if (!esperar(TipoToken.TABLA, "TABLA")) {
            return null;
        }
        Token tabla = esperarIdentificador("el nombre de la tabla");
        if (tabla == null) {
            return null;
        }
        return new SentenciaDDL(tipo, tabla.getLexema(), List.of(),
                inicio.getlinea(), inicio.getcolumna());
    }

    private boolean esperar(TipoToken esperado, String descripcion) {
        if (cursor.coincidir(esperado)) {
            return true;
        }
        error("SYN-006", "Se esperaba " + descripcion, cursor.ver());
        return false;
    }

    private Token esperarIdentificador(String descripcion) {
        if (cursor.comprobar(TipoToken.IDENTIFICADOR)) {
            return cursor.avanzar();
        }
        error("SYN-007", "Se esperaba " + descripcion, cursor.ver());
        return null;
    }

    private void error(String codigo, String mensaje, Token token) {
        errores.add(new ErrorSintactico(codigo, mensaje, token.getLexema(),
                token.getlinea(), token.getcolumna()));
    }

    private void error(String codigo, String mensaje, String lexema) {
        Token token = cursor.ver();
        errores.add(new ErrorSintactico(codigo, mensaje, lexema,
                token.getlinea(), token.getcolumna()));
    }

    private static boolean esTipoDato(TipoToken tipo) {
        return tipo == TipoToken.TIPO_ENTERO
                || tipo == TipoToken.TIPO_TEXTO
                || tipo == TipoToken.TIPO_FECHA
                || tipo == TipoToken.TIPO_DECIMAL
                || tipo == TipoToken.TIPO_BOOLEANO;
    }
}
