package semantic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lexer.AnalizadorDML;
import lexer.TipoToken;
import lexer.Token;
import symbol_table.CatalogoEsquema;
import symbol_table.ColumnaDeclarada;

/**
 * Validador semantico de DML. No ejecuta consultas ni modifica filas.
 * Usa el mismo CatalogoEsquema que construye la semantica DDL.
 */
public final class SemanticaDML {
    private static final String FASE = "SEMANTICA";
    private static final Pattern LINEA_ERROR_SINTACTICO =
            Pattern.compile("linea\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    private final CatalogoEsquema catalogo;
    private final TablaErrores tablaErrores;

    public SemanticaDML(CatalogoEsquema catalogo, TablaErrores tablaErrores) {
        if (catalogo == null) {
            throw new IllegalArgumentException("El catalogo no puede ser null");
        }
        this.catalogo = catalogo;
        this.tablaErrores = tablaErrores == null ? new TablaErrores() : tablaErrores;
    }

    public SemanticaDML(CatalogoEsquema catalogo) {
        this(catalogo, new TablaErrores());
    }

    /** Analiza DML despues de comprobar la sintaxis con el analizador existente. */
    public List<ErrorCompilacion> analizar(List<Token> tokens) {
        return analizar(tokens, new AnalizadorDML().analizar(tokens));
    }

    /**
     * Variante para Main: recibe los errores sintacticos ya calculados y evita
     * analizar semanticamente las sentencias que contienen un error de sintaxis.
     */
    public List<ErrorCompilacion> analizar(List<Token> tokens, List<String> erroresSintacticos) {
        List<ErrorCompilacion> encontrados = new ArrayList<>();
        if (tokens == null || tokens.isEmpty()) {
            return encontrados;
        }

        Set<Integer> lineasInvalidas = extraerLineasInvalidas(erroresSintacticos);
        List<Token> sentencia = new ArrayList<>();
        for (Token token : tokens) {
            if (token.getTipo() == TipoToken.FIN_DE_ARCHIVO) {
                procesar(sentencia, lineasInvalidas, encontrados);
                sentencia.clear();
                break;
            }
            if (token.getTipo() == TipoToken.PUNTO_Y_COMA) {
                procesar(sentencia, lineasInvalidas, encontrados);
                sentencia.clear();
            } else {
                sentencia.add(token);
            }
        }
        if (!sentencia.isEmpty()) {
            procesar(sentencia, lineasInvalidas, encontrados);
        }
        tablaErrores.agregarTodos(encontrados);
        return encontrados;
    }

    public TablaErrores getTablaErrores() {
        return tablaErrores;
    }

    public CatalogoEsquema getCatalogo() {
        return catalogo;
    }

    private void procesar(List<Token> tokens, Set<Integer> lineasInvalidas,
            List<ErrorCompilacion> encontrados) {
        if (tokens.isEmpty() || contieneErrorLexico(tokens) || contieneLineaInvalida(tokens, lineasInvalidas)) {
            return;
        }
        switch (tokens.get(0).getTipo()) {
            case INSERTAR -> insertar(tokens, encontrados);
            case SELECCIONAR -> seleccionar(tokens, encontrados);
            case ACTUALIZAR -> actualizar(tokens, encontrados);
            case BORRAR -> borrar(tokens, encontrados);
            default -> {
                // DDL y otros tokens no pertenecen a este modulo.
            }
        }
    }

    private void insertar(List<Token> tokens, List<ErrorCompilacion> errores) {
        int i = 1;
        if (i >= tokens.size() || tokens.get(i).getTipo() != TipoToken.EN) {
            return;
        }
        i++;
        Token tablaToken = token(tokens, i++);
        if (tablaToken == null) {
            return;
        }
        String tabla = tablaToken.getLexema();
        if (!catalogo.existeTabla(tabla)) {
            error(errores, "SEM-201", "No existe una tabla con ese nombre", tablaToken);
            return;
        }

        List<String> columnas = new ArrayList<>();
        if (i < tokens.size() && tokens.get(i).getTipo() == TipoToken.PARENTESIS_IZQ) {
            i++;
            while (i < tokens.size() && tokens.get(i).getTipo() != TipoToken.PARENTESIS_DER) {
                Token col = tokens.get(i++);
                if (col.getTipo() == TipoToken.IDENTIFICADOR) {
                    columnas.add(col.getLexema());
                }
            }
            if (i < tokens.size()) {
                i++;
            }
        }
        if (i >= tokens.size() || tokens.get(i).getTipo() != TipoToken.VALORES) {
            return;
        }
        i++;
        if (i >= tokens.size() || tokens.get(i).getTipo() != TipoToken.PARENTESIS_IZQ) {
            return;
        }
        i++;
        List<Token> valores = new ArrayList<>();
        while (i < tokens.size() && tokens.get(i).getTipo() != TipoToken.PARENTESIS_DER) {
            Token valor = tokens.get(i++);
            if (valor.getTipo() != TipoToken.COMA) {
                valores.add(valor);
            }
        }

        List<ColumnaDeclarada> columnasTabla = catalogo.columnasDe(tabla);
        List<ColumnaDeclarada> destino = new ArrayList<>();
        if (columnas.isEmpty()) {
            destino.addAll(columnasTabla);
        } else {
            Set<String> repetidas = new HashSet<>();
            for (String nombre : columnas) {
                if (!repetidas.add(nombre.toUpperCase())) {
                    Token t = encontrar(tokens, nombre);
                    error(errores, "SEM-203", "La columna aparece mas de una vez en INSERTAR",
                            t == null ? tablaToken : t);
                    continue;
                }
                ColumnaDeclarada columna = catalogo.buscarColumna(tabla, nombre);
                if (columna == null) {
                    Token t = encontrar(tokens, nombre);
                    error(errores, "SEM-202", "No existe la columna en la tabla", t == null ? tablaToken : t);
                } else {
                    destino.add(columna);
                }
            }
        }

        if (valores.size() != destino.size()) {
            Token referencia = valores.isEmpty() ? tablaToken : valores.get(valores.size() - 1);
            error(errores, "SEM-203", "La cantidad de valores no coincide con las columnas destino", referencia);
            return;
        }
        for (int j = 0; j < Math.min(valores.size(), destino.size()); j++) {
            validarValor(valores.get(j), destino.get(j), tabla, errores);
        }
    }

    private void seleccionar(List<Token> tokens, List<ErrorCompilacion> errores) {
        int i = 1;
        if (i < tokens.size() && tokens.get(i).getTipo() == TipoToken.ASTERISCO) {
            i++;
        } else {
            while (i < tokens.size() && tokens.get(i).getTipo() != TipoToken.DESDE) {
                Token col = tokens.get(i++);
                if (col.getTipo() == TipoToken.IDENTIFICADOR) {
                    validarColumna(tokens, col, errores);
                }
            }
        }
        if (i < tokens.size() && tokens.get(i).getTipo() == TipoToken.DESDE) {
            i++;
        }
        Token tablaToken = token(tokens, i++);
        if (tablaToken == null) {
            return;
        }
        if (!catalogo.existeTabla(tablaToken.getLexema())) {
            error(errores, "SEM-201", "No existe una tabla con ese nombre", tablaToken);
            return;
        }
        // La lista de columnas debe validarse contra la tabla FROM.
        if (tokens.size() > 1 && tokens.get(1).getTipo() != TipoToken.ASTERISCO) {
            i = 1;
            while (i < tokens.size() && tokens.get(i).getTipo() != TipoToken.DESDE) {
                Token col = tokens.get(i++);
                if (col.getTipo() == TipoToken.IDENTIFICADOR && !catalogo.existeColumna(tablaToken.getLexema(), col.getLexema())) {
                    error(errores, "SEM-202", "No existe la columna en la tabla", col);
                }
            }
        }
        if (i < tokens.size() && tokens.get(i).getTipo() == TipoToken.DESDE) {
            i++;
            i++;
        }
        condicionDesde(tokens, i, tablaToken.getLexema(), errores);
    }

    private void actualizar(List<Token> tokens, List<ErrorCompilacion> errores) {
        Token tablaToken = token(tokens, 1);
        if (tablaToken == null) {
            return;
        }
        if (!catalogo.existeTabla(tablaToken.getLexema())) {
            error(errores, "SEM-201", "No existe una tabla con ese nombre", tablaToken);
            return;
        }
        int i = 3; // ACTUALIZAR tabla ASIGNAR
        while (i < tokens.size() && tokens.get(i).getTipo() != TipoToken.DONDE) {
            Token columna = token(tokens, i++);
            if (columna == null) {
                break;
            }
            if (columna.getTipo() != TipoToken.IDENTIFICADOR) {
                continue;
            }
            if (i < tokens.size() && tokens.get(i).getTipo() == TipoToken.OP_IGUAL) {
                i++;
            }
            Token valor = token(tokens, i++);
            ColumnaDeclarada declarada = catalogo.buscarColumna(tablaToken.getLexema(), columna.getLexema());
            if (declarada == null) {
                error(errores, "SEM-202", "No existe la columna en la tabla", columna);
            } else if (valor != null) {
                validarValor(valor, declarada, tablaToken.getLexema(), errores);
            }
            if (i < tokens.size() && tokens.get(i).getTipo() == TipoToken.COMA) {
                i++;
            }
        }
        condicionDesde(tokens, i, tablaToken.getLexema(), errores);
    }

    private void borrar(List<Token> tokens, List<ErrorCompilacion> errores) {
        Token tablaToken = token(tokens, 2);
        if (tablaToken == null) {
            return;
        }
        if (!catalogo.existeTabla(tablaToken.getLexema())) {
            error(errores, "SEM-201", "No existe una tabla con ese nombre", tablaToken);
            return;
        }
        condicionDesde(tokens, 3, tablaToken.getLexema(), errores);
    }

    private void condicionDesde(List<Token> tokens, int inicio, String tabla, List<ErrorCompilacion> errores) {
        int donde = inicio;
        while (donde < tokens.size() && tokens.get(donde).getTipo() != TipoToken.DONDE) {
            donde++;
        }
        if (donde >= tokens.size()) {
            return;
        }
        for (int i = donde + 1; i < tokens.size();) {
            Token izquierdo = tokens.get(i++);
            if (izquierdo.getTipo() == TipoToken.PARENTESIS_IZQ
                    || izquierdo.getTipo() == TipoToken.PARENTESIS_DER
                    || izquierdo.getTipo() == TipoToken.OP_NO
                    || izquierdo.getTipo() == TipoToken.OP_Y
                    || izquierdo.getTipo() == TipoToken.OP_O) {
                continue;
            }
            if (izquierdo.getTipo() != TipoToken.IDENTIFICADOR) {
                continue;
            }
            ColumnaDeclarada columna = catalogo.buscarColumna(tabla, izquierdo.getLexema());
            if (columna == null) {
                error(errores, "SEM-202", "No existe la columna en la tabla", izquierdo);
                saltarComparacion(tokens, i);
                continue;
            }
            if (i >= tokens.size()) {
                break;
            }
            Token operador = tokens.get(i++);
            if (!esOperadorRelacional(operador.getTipo())) {
                continue;
            }
            Token derecho = token(tokens, i++);
            if (derecho != null) {
                validarCondicion(columna, operador, derecho, tabla, errores);
            }
        }
    }

    private void validarCondicion(ColumnaDeclarada columna, Token operador, Token derecho,
            String tabla, List<ErrorCompilacion> errores) {
        TipoToken tipoDerecho = tipoDelValor(derecho, tabla);
        if (tipoDerecho == null) {
            error(errores, "SEM-202", "No existe la columna referenciada en la condicion", derecho);
            return;
        }
        if (!tiposCompatibles(columna.getTipoDato(), tipoDerecho)) {
            error(errores, "SEM-204", "Los tipos de la condicion no son compatibles",
                    derecho);
            return;
        }
        if ((operador.getTipo() != TipoToken.OP_IGUAL && operador.getTipo() != TipoToken.OP_DIFERENTE)
                && !esNumerico(columna.getTipoDato())) {
            error(errores, "SEM-205", "El operador relacional requiere una columna numerica",
                    operador);
        }
    }

    private void validarValor(Token valor, ColumnaDeclarada columna, String tabla,
            List<ErrorCompilacion> errores) {
        TipoToken tipoValor = tipoDelValor(valor, tabla);
        if (tipoValor == null) {
            error(errores, "SEM-202", "No existe la columna referenciada", valor);
        } else if (!tiposCompatibles(columna.getTipoDato(), tipoValor)) {
            error(errores, "SEM-204", "El tipo del valor no es compatible con la columna "
                    + columna.getTipoDatoTexto(), valor);
        }
    }

    private TipoToken tipoDelValor(Token token, String tabla) {
        return switch (token.getTipo()) {
            case LITERAL_ENTERO -> TipoToken.TIPO_ENTERO;
            case LITERAL_DECIMAL -> TipoToken.TIPO_DECIMAL;
            case LITERAL_CADENA -> TipoToken.TIPO_TEXTO;
            case IDENTIFICADOR -> catalogo.tipoDeColumna(tabla, token.getLexema());
            default -> null;
        };
    }

    private boolean tiposCompatibles(TipoToken esperado, TipoToken recibido) {
        if (esperado == recibido) {
            return true;
        }
        return esperado == TipoToken.TIPO_DECIMAL && recibido == TipoToken.TIPO_ENTERO;
    }

    private boolean esNumerico(TipoToken tipo) {
        return tipo == TipoToken.TIPO_ENTERO || tipo == TipoToken.TIPO_DECIMAL;
    }

    private void validarColumna(List<Token> tokens, Token col, List<ErrorCompilacion> errores) {
        // Se valida nuevamente cuando ya se conoce la tabla FROM.
    }

    private boolean contieneErrorLexico(List<Token> tokens) {
        for (Token token : tokens) {
            if (token.getTipo() == TipoToken.ERROR) {
                return true;
            }
        }
        return false;
    }

    private boolean contieneLineaInvalida(List<Token> tokens, Set<Integer> lineasInvalidas) {
        for (Token token : tokens) {
            if (lineasInvalidas.contains(token.getlinea())) {
                return true;
            }
        }
        return false;
    }

    private Set<Integer> extraerLineasInvalidas(List<String> erroresSintacticos) {
        Set<Integer> lineas = new HashSet<>();
        if (erroresSintacticos == null) {
            return lineas;
        }
        for (String error : erroresSintacticos) {
            Matcher matcher = LINEA_ERROR_SINTACTICO.matcher(error == null ? "" : error);
            if (matcher.find()) {
                lineas.add(Integer.parseInt(matcher.group(1)));
            }
        }
        return lineas;
    }

    private void saltarComparacion(List<Token> tokens, int indice) {
        // La siguiente iteracion encontrara el proximo identificador/operador.
    }

    private boolean esOperadorRelacional(TipoToken tipo) {
        return tipo == TipoToken.OP_IGUAL || tipo == TipoToken.OP_MAYOR
                || tipo == TipoToken.OP_MENOR || tipo == TipoToken.OP_MAYOR_IGUAL
                || tipo == TipoToken.OP_MENOR_IGUAL || tipo == TipoToken.OP_DIFERENTE;
    }

    private Token token(List<Token> tokens, int indice) {
        return indice >= 0 && indice < tokens.size() ? tokens.get(indice) : null;
    }

    private Token encontrar(List<Token> tokens, String lexema) {
        for (Token token : tokens) {
            if (token.getTipo() == TipoToken.IDENTIFICADOR && token.getLexema().equalsIgnoreCase(lexema)) {
                return token;
            }
        }
        return null;
    }

    private void error(List<ErrorCompilacion> errores, String codigo, String mensaje, Token token) {
        errores.add(new ErrorCompilacion(FASE, codigo, mensaje,
                token.getLexema(), token.getlinea(), token.getcolumna()));
    }
}
