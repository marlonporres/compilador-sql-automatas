package lexer;

import java.util.ArrayList;
import java.util.List;

/** Analizador sintactico para las sentencias DML del lenguaje SQL en espanol. */
public class AnalizadorDML {
    private List<Token> tokens;
    private final List<String> errores = new ArrayList<>();
    private int posicion;

    /** Valida una lista de tokens producida por ScannerSQL y devuelve sus errores. */
    public List<String> analizar(List<Token> entrada) {
        errores.clear();
        posicion = 0;

        if (entrada == null) {
            errores.add("No se recibieron tokens para analizar.");
            return new ArrayList<>(errores);
        }

        tokens = new ArrayList<>(entrada);
        if (tokens.isEmpty() || tokens.get(tokens.size() - 1).getTipo() != TipoToken.FIN_DE_ARCHIVO) {
            int linea = tokens.isEmpty() ? 1 : tokens.get(tokens.size() - 1).getlinea();
            int columna = tokens.isEmpty() ? 1 : tokens.get(tokens.size() - 1).getcolumna() + 1;
            tokens.add(new Token(TipoToken.FIN_DE_ARCHIVO, "EOF", linea, columna));
        }

        while (!es(TipoToken.FIN_DE_ARCHIVO)) {
            int inicio = posicion;
            try {
                if (coincide(TipoToken.PUNTO_Y_COMA)) {
                    throw error("no se permiten sentencias vacias");
                }
                sentencia();
                if (!coincide(TipoToken.PUNTO_Y_COMA) && !es(TipoToken.FIN_DE_ARCHIVO)) {
                    throw error("se esperaba ';' al final de la sentencia");
                }
            } catch (ErrorSintactico error) {
                errores.add("Error sintactico en linea " + error.token.getlinea()
                        + ", columna " + error.token.getcolumna() + ": " + error.getMessage());
                recuperarSentencia();
            }
            if (posicion == inicio && !es(TipoToken.FIN_DE_ARCHIVO)) {
                avanzar();
            }
        }
        return new ArrayList<>(errores);
    }

    private void sentencia() {
        switch (actual().getTipo()) {
            case SELECCIONAR -> seleccionar();
            case INSERTAR -> insertar();
            case ACTUALIZAR -> actualizar();
            case BORRAR -> borrar();
            default -> throw error("se esperaba SELECCIONAR, INSERTAR, ACTUALIZAR o BORRAR");
        }
    }

    private void seleccionar() {
        avanzar();
        if (!coincide(TipoToken.ASTERISCO)) {
            listaIdentificadores();
        }
        esperar(TipoToken.DESDE);
        esperar(TipoToken.IDENTIFICADOR);
        if (coincide(TipoToken.DONDE)) {
            condicion();
        }
    }

    private void insertar() {
        avanzar();
        esperar(TipoToken.EN);
        esperar(TipoToken.IDENTIFICADOR);
        if (coincide(TipoToken.PARENTESIS_IZQ)) {
            listaIdentificadores();
            esperar(TipoToken.PARENTESIS_DER);
        }
        esperar(TipoToken.VALORES);
        esperar(TipoToken.PARENTESIS_IZQ);
        listaValores();
        esperar(TipoToken.PARENTESIS_DER);
    }

    private void actualizar() {
        avanzar();
        esperar(TipoToken.IDENTIFICADOR);
        esperar(TipoToken.ASIGNAR);
        asignaciones();
        if (coincide(TipoToken.DONDE)) {
            condicion();
        }
    }

    private void borrar() {
        avanzar();
        esperar(TipoToken.DESDE);
        esperar(TipoToken.IDENTIFICADOR);
        if (coincide(TipoToken.DONDE)) {
            condicion();
        }
    }

    private void listaIdentificadores() {
        esperar(TipoToken.IDENTIFICADOR);
        while (coincide(TipoToken.COMA)) {
            esperar(TipoToken.IDENTIFICADOR);
        }
    }

    private void listaValores() {
        valor();
        while (coincide(TipoToken.COMA)) {
            valor();
        }
    }

    private void asignaciones() {
        asignacion();
        while (coincide(TipoToken.COMA)) {
            asignacion();
        }
    }

    private void asignacion() {
        esperar(TipoToken.IDENTIFICADOR);
        esperar(TipoToken.OP_IGUAL);
        valor();
    }

    private void condicion() {
        condicionO();
    }

    private void condicionO() {
        condicionY();
        while (coincide(TipoToken.OP_O)) {
            condicionY();
        }
    }

    private void condicionY() {
        condicionNo();
        while (coincide(TipoToken.OP_Y)) {
            condicionNo();
        }
    }

    private void condicionNo() {
        if (coincide(TipoToken.OP_NO)) {
            condicionNo();
        } else if (coincide(TipoToken.PARENTESIS_IZQ)) {
            condicion();
            esperar(TipoToken.PARENTESIS_DER);
        } else {
            esperar(TipoToken.IDENTIFICADOR);
            if (!coincide(TipoToken.OP_IGUAL) && !coincide(TipoToken.OP_MAYOR)
                    && !coincide(TipoToken.OP_MENOR) && !coincide(TipoToken.OP_MAYOR_IGUAL)
                    && !coincide(TipoToken.OP_MENOR_IGUAL) && !coincide(TipoToken.OP_DIFERENTE)) {
                throw error("se esperaba un operador relacional");
            }
            valor();
        }
    }

    private void valor() {
        if (coincide(TipoToken.IDENTIFICADOR) || coincide(TipoToken.LITERAL_ENTERO)
                || coincide(TipoToken.LITERAL_DECIMAL) || coincide(TipoToken.LITERAL_CADENA)) {
            return;
        }
        throw error("se esperaba un identificador o un literal");
    }

    private void esperar(TipoToken tipo) {
        if (!coincide(tipo)) {
            throw error("se esperaba " + tipo + " y se encontro " + actual().getLexema());
        }
    }

    private boolean coincide(TipoToken tipo) {
        if (!es(tipo)) {
            return false;
        }
        avanzar();
        return true;
    }

    private boolean es(TipoToken tipo) {
        return actual().getTipo() == tipo;
    }

    private Token actual() {
        return tokens.get(Math.min(posicion, tokens.size() - 1));
    }

    private Token avanzar() {
        Token token = actual();
        if (!es(TipoToken.FIN_DE_ARCHIVO)) {
            posicion++;
        }
        return token;
    }

    private ErrorSintactico error(String mensaje) {
        return new ErrorSintactico(actual(), mensaje);
    }

    private void recuperarSentencia() {
        while (!es(TipoToken.FIN_DE_ARCHIVO) && !es(TipoToken.PUNTO_Y_COMA)) {
            avanzar();
        }
        coincide(TipoToken.PUNTO_Y_COMA);
    }

    private static final class ErrorSintactico extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final transient Token token;

        private ErrorSintactico(Token token, String mensaje) {
            super(mensaje);
            this.token = token;
        }
    }
}