package parser;

import java.util.ArrayList;
import java.util.List;

import lexer.TipoToken;
import lexer.Token;

/** Analizador sintáctico descendente para DML y condiciones lógicas. */
public final class AnalizadorSintacticoDML {

    private CursorTokens cursor;
    private List<ErrorSintactico> errores;

    public ResultadoSintacticoDML analizar(List<Token> tokens) {
        cursor = new CursorTokens(tokens);
        errores = new ArrayList<>();
        List<SentenciaDML> sentencias = new ArrayList<>();

        while (!cursor.estaAlFinal()) {
            if (cursor.comprobar(TipoToken.PUNTO_Y_COMA)) {
                error("SYN-101", "Se esperaba el inicio de una sentencia DML", cursor.avanzar());
                continue;
            }
            int erroresAntes = errores.size();
            SentenciaDML sentencia = analizarSentencia();
            if (sentencia == null || errores.size() != erroresAntes) {
                cursor.recuperarHastaFinDeSentencia();
                continue;
            }
            if (!cursor.coincidir(TipoToken.PUNTO_Y_COMA)) {
                error("SYN-102", "Se esperaba ';' al final de la sentencia", cursor.ver());
                cursor.recuperarHastaFinDeSentencia();
                continue;
            }
            sentencias.add(sentencia);
        }
        return new ResultadoSintacticoDML(sentencias, errores);
    }

    private SentenciaDML analizarSentencia() {
        return switch (cursor.ver().getTipo()) {
            case SELECCIONAR -> analizarSeleccionar();
            case INSERTAR -> analizarInsertar();
            case ACTUALIZAR -> analizarActualizar();
            case BORRAR -> analizarBorrar();
            case ERROR -> {
                error("SYN-103", "No se puede analizar un token léxicamente inválido", cursor.ver());
                cursor.avanzar();
                yield null;
            }
            default -> {
                error("SYN-104", "Se esperaba una sentencia DML", cursor.ver());
                cursor.avanzar();
                yield null;
            }
        };
    }

    private SentenciaDML analizarSeleccionar() {
        Token inicio = cursor.avanzar();
        boolean todas = cursor.coincidir(TipoToken.ASTERISCO);
        List<Token> columnas = new ArrayList<>();
        if (!todas) {
            if (!agregarListaIdentificadores(columnas, "una columna para SELECCIONAR")) {
                return null;
            }
        }
        if (!esperar(TipoToken.DESDE, "DESDE")) {
            return null;
        }
        Token tabla = esperarIdentificador("el nombre de la tabla");
        if (tabla == null) return null;
        CondicionSQL condicion = analizarCondicionOpcional();
        return new SentenciaDML(TipoSentenciaDML.SELECCIONAR, tabla, columnas, todas,
                List.of(), List.of(), List.of(), condicion,
                inicio.getlinea(), inicio.getcolumna());
    }

    private SentenciaDML analizarInsertar() {
        Token inicio = cursor.avanzar();
        if (!esperar(TipoToken.EN, "EN")) {
            return null;
        }
        Token tabla = esperarIdentificador("el nombre de la tabla");
        if (tabla == null) {
            return null;
        }
        List<Token> columnas = new ArrayList<>();
        if (cursor.coincidir(TipoToken.PARENTESIS_IZQ)) {
            if (!agregarListaIdentificadores(columnas, "una columna para INSERTAR")) {
                return null;
            }
            if (!esperar(TipoToken.PARENTESIS_DER, "')'")) {
                return null;
            }
        }
        if (!esperar(TipoToken.VALORES, "VALORES")
                || !esperar(TipoToken.PARENTESIS_IZQ, "'('") ) {
            return null;
        }
        List<ValorSQL> valores = analizarValores();
        if (valores == null || !esperar(TipoToken.PARENTESIS_DER, "')'")) {
            return null;
        }
        return new SentenciaDML(TipoSentenciaDML.INSERTAR, tabla,
                List.of(), false, columnas, valores, List.of(), null,
                inicio.getlinea(), inicio.getcolumna());
    }

    private SentenciaDML analizarActualizar() {
        Token inicio = cursor.avanzar();
        Token tabla = esperarIdentificador("el nombre de la tabla");
        if (tabla == null || !esperar(TipoToken.ASIGNAR, "ASIGNAR")) {
            return null;
        }
        List<AsignacionSQL> asignaciones = new ArrayList<>();
        do {
            Token columna = esperarIdentificador("el nombre de la columna");
            if (columna == null || !esperar(TipoToken.OP_IGUAL, "'='")) {
                return null;
            }
            ValorSQL valor = analizarValor();
            if (valor == null) {
                return null;
            }
            asignaciones.add(new AsignacionSQL(columna, valor));
        } while (cursor.coincidir(TipoToken.COMA));
        if (cursor.comprobar(TipoToken.IDENTIFICADOR)) {
            error("SYN-110", "Falta ',' entre las asignaciones", cursor.ver());
            return null;
        }
        CondicionSQL condicion = analizarCondicionOpcional();
        return new SentenciaDML(TipoSentenciaDML.ACTUALIZAR, tabla,
                List.of(), false, List.of(), List.of(), asignaciones, condicion,
                inicio.getlinea(), inicio.getcolumna());
    }

    private SentenciaDML analizarBorrar() {
        Token inicio = cursor.avanzar();
        if (!esperar(TipoToken.DESDE, "DESDE")) {
            return null;
        }
        Token tabla = esperarIdentificador("el nombre de la tabla");
        if (tabla == null) {
            return null;
        }
        CondicionSQL condicion = analizarCondicionOpcional();
        return new SentenciaDML(TipoSentenciaDML.BORRAR, tabla,
                List.of(), false, List.of(), List.of(), List.of(), condicion,
                inicio.getlinea(), inicio.getcolumna());
    }

    private List<ValorSQL> analizarValores() {
        List<ValorSQL> valores = new ArrayList<>();
        ValorSQL primero = analizarValor();
        if (primero == null) {
            return null;
        }
        valores.add(primero);
        while (cursor.coincidir(TipoToken.COMA)) {
            ValorSQL valor = analizarValor();
            if (valor == null) {
                return null;
            }
            valores.add(valor);
        }
        Token siguiente = cursor.ver();
        if (siguiente.getTipo() == TipoToken.LITERAL_ENTERO
                || siguiente.getTipo() == TipoToken.LITERAL_DECIMAL
                || siguiente.getTipo() == TipoToken.LITERAL_CADENA
                || CondicionSQL.esBooleano(siguiente)) {
            error("SYN-110", "Falta ',' entre los valores", siguiente);
            return null;
        }
        return valores;
    }

    private ValorSQL analizarValor() {
        Token token = cursor.ver();
        if (token.getTipo() != TipoToken.LITERAL_ENTERO
                && token.getTipo() != TipoToken.LITERAL_DECIMAL
                && token.getTipo() != TipoToken.LITERAL_CADENA
                && !CondicionSQL.esBooleano(token)) {
            error("SYN-105", "Se esperaba un literal", token);
            return null;
        }
        cursor.avanzar();
        return new ValorSQL(token.getTipo(), token.getLexema(),
                token.getlinea(), token.getcolumna());
    }

    private CondicionSQL analizarCondicionOpcional() {
        if (!cursor.coincidir(TipoToken.DONDE)) {
            return null;
        }
        return analizarOr();
    }

    private CondicionSQL analizarOr() {
        CondicionSQL izquierda = analizarAnd();
        if (izquierda == null) {
            return null;
        }
        while (cursor.coincidir(TipoToken.OP_O)) {
            CondicionSQL derecha = analizarAnd();
            if (derecha == null) {
                return null;
            }
            izquierda = CondicionSQL.unir("O", izquierda, derecha);
        }
        return izquierda;
    }

    private CondicionSQL analizarAnd() {
        CondicionSQL izquierda = analizarNo();
        if (izquierda == null) {
            return null;
        }
        while (cursor.coincidir(TipoToken.OP_Y)) {
            CondicionSQL derecha = analizarNo();
            if (derecha == null) {
                return null;
            }
            izquierda = CondicionSQL.unir("Y", izquierda, derecha);
        }
        return izquierda;
    }

    private CondicionSQL analizarNo() {
        if (cursor.coincidir(TipoToken.OP_NO)) {
            CondicionSQL condicion = analizarNo();
            return condicion == null ? null : CondicionSQL.negar(condicion);
        }
        return analizarPrimaria();
    }

    private CondicionSQL analizarPrimaria() {
        if (cursor.coincidir(TipoToken.PARENTESIS_IZQ)) {
            CondicionSQL interna = analizarOr();
            if (interna == null || !esperar(TipoToken.PARENTESIS_DER, "')'")) {
                return null;
            }
            return interna;
        }

        Token izquierda = analizarOperando();
        if (izquierda == null) return null;
        if (!esOperadorRelacional(cursor.ver().getTipo())) {
            error("SYN-106", "Se esperaba un operador relacional", cursor.ver());
            return null;
        }
        Token operador = cursor.avanzar();
        Token derecha = analizarOperando();
        if (derecha == null) {
            return null;
        }
        return CondicionSQL.comparar(izquierda, operador, derecha);
    }

    private Token analizarOperando() {
        Token token = cursor.ver();
        if (token.getTipo() == TipoToken.IDENTIFICADOR) {
            cursor.avanzar();
            return token;
        }
        if (token.getTipo() == TipoToken.LITERAL_ENTERO
                || token.getTipo() == TipoToken.LITERAL_DECIMAL
                || token.getTipo() == TipoToken.LITERAL_CADENA) {
            cursor.avanzar();
            return token;
        }
        error("SYN-107", "Se esperaba una columna o literal", token);
        return null;
    }

    private boolean agregarListaIdentificadores(List<Token> destino, String descripcion) {
        Token primero = esperarIdentificador(descripcion);
        if (primero == null) {
            return false;
        }
        destino.add(primero);
        while (cursor.coincidir(TipoToken.COMA)) {
            Token siguiente = esperarIdentificador(descripcion);
            if (siguiente == null) {
                return false;
            }
            destino.add(siguiente);
        }
        if (cursor.comprobar(TipoToken.IDENTIFICADOR)) {
            error("SYN-110", "Falta ',' entre los nombres de columnas", cursor.ver());
            return false;
        }
        return true;
    }

    private Token esperarIdentificador(String descripcion) {
        if (cursor.comprobar(TipoToken.IDENTIFICADOR)) {
            return cursor.avanzar();
        }
        error("SYN-108", "Se esperaba " + descripcion, cursor.ver());
        return null;
    }

    private boolean esperar(TipoToken tipo, String descripcion) {
        if (cursor.coincidir(tipo)) {
            return true;
        }
        error("SYN-109", "Se esperaba " + descripcion, cursor.ver());
        return false;
    }

    private void error(String codigo, String mensaje, Token token) {
        errores.add(new ErrorSintactico(codigo, mensaje, token.getLexema(),
                token.getlinea(), token.getcolumna()));
    }

    private static boolean esOperadorRelacional(TipoToken tipo) {
        return tipo == TipoToken.OP_IGUAL || tipo == TipoToken.OP_MAYOR
                || tipo == TipoToken.OP_MENOR || tipo == TipoToken.OP_MAYOR_IGUAL
                || tipo == TipoToken.OP_MENOR_IGUAL || tipo == TipoToken.OP_DIFERENTE;
    }

}
