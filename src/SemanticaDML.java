package semantic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lexer.AnalizadorDML;
import lexer.TipoToken;
import lexer.Token;

/**
 * Analizador semántico DML. No ejecuta consultas: valida referencias,
 * cantidades y compatibilidad de tipos contra un catálogo de esquemas.
 */
public final class SemanticaDML {
    public static final String FASE = "SEMANTICA";

    private final CatalogoSemantico catalogo;

    public SemanticaDML(CatalogoSemantico catalogo) {
        this.catalogo = catalogo;
    }

    public ReporteCompilacion analizar(List<Token> tokens) {
        TablaErrores errores = new TablaErrores();

        if (tokens == null) {
            errores.agregar(FASE, "SEM-000", "No se recibieron tokens para analizar",
                    "", 1, 1);
            return new ReporteCompilacion(errores.obtenerTodos());
        }

        // Los errores léxicos existentes también entran al reporte común.
        boolean tieneLexicos = false;
        for (Token token : tokens) {
            if (token.getTipo() == TipoToken.ERROR) {
                tieneLexicos = true;
                errores.agregar("LEXICO", "LEX-001", "Lexema inválido", token.getLexema(),
                        token.getlinea(), token.getcolumna());
            }
        }
        if (tieneLexicos) {
            return new ReporteCompilacion(errores.obtenerTodos());
        }

        // La semántica solo recibe programas sintácticamente válidos.
        List<String> sintacticos = new AnalizadorDML().analizar(tokens);
        if (!sintacticos.isEmpty()) {
            Pattern patron = Pattern.compile("linea (\\d+), columna (\\d+): (.*)");
            for (String mensaje : sintacticos) {
                Matcher match = patron.matcher(mensaje);
                if (match.find()) {
                    int linea = Integer.parseInt(match.group(1));
                    int columna = Integer.parseInt(match.group(2));
                    errores.agregar("SINTACTICA", "SYN-000", match.group(3), "",
                            linea, columna);
                } else {
                    errores.agregar("SINTACTICA", "SYN-000", mensaje, "", 0, 0);
                }
            }
            return new ReporteCompilacion(errores.obtenerTodos());
        }

        int pos = 0;
        while (pos < tokens.size()) {
            Token actual = tokens.get(pos);
            if (actual.getTipo() == TipoToken.FIN_DE_ARCHIVO) break;
            int fin = buscarFinSentencia(tokens, pos);
            List<Token> sentencia = tokens.subList(pos, fin);
            validarSentencia(sentencia, errores);
            pos = fin + (fin < tokens.size() && tokens.get(fin).getTipo() == TipoToken.PUNTO_Y_COMA ? 1 : 0);
        }
        return new ReporteCompilacion(errores.obtenerTodos());
    }

    private int buscarFinSentencia(List<Token> tokens, int inicio) {
        for (int i = inicio; i < tokens.size(); i++) {
            TipoToken tipo = tokens.get(i).getTipo();
            if (tipo == TipoToken.PUNTO_Y_COMA || tipo == TipoToken.FIN_DE_ARCHIVO) {
                return i;
            }
        }
        return tokens.size();
    }

    private void validarSentencia(List<Token> t, TablaErrores errores) {
        if (t.isEmpty()) return;
        switch (t.getFirst().getTipo()) {
            case INSERTAR -> validarInsertar(t, errores);
            case ACTUALIZAR -> validarActualizar(t, errores);
            case SELECCIONAR -> validarSeleccionar(t, errores);
            case BORRAR -> validarBorrar(t, errores);
            default -> { /* El parser DML ya filtró estas sentencias. */ }
        }
    }

    private void validarInsertar(List<Token> t, TablaErrores e) {
        int p = 1; // INSERTAR
        if (p >= t.size() || t.get(p).getTipo() != TipoToken.EN) return;
        Token tablaToken = siguiente(t, ++p);
        if (tablaToken == null) return;
        TablaCatalogo tabla = tabla(tablaToken, e);
        if (tabla == null) return;
        p++;

        List<Token> columnas = new ArrayList<>();
        if (p < t.size() && t.get(p).getTipo() == TipoToken.PARENTESIS_IZQ) {
            p++;
            while (p < t.size() && t.get(p).getTipo() != TipoToken.PARENTESIS_DER) {
                if (t.get(p).getTipo() == TipoToken.IDENTIFICADOR) columnas.add(t.get(p));
                p++;
            }
            validarColumnasInsert(columnas, tabla, e);
            p++;
        }

        while (p < t.size() && t.get(p).getTipo() != TipoToken.PARENTESIS_IZQ) p++;
        if (p >= t.size()) return;
        p++;
        List<Token> valores = new ArrayList<>();
        while (p < t.size() && t.get(p).getTipo() != TipoToken.PARENTESIS_DER) {
            if (esValor(t.get(p))) valores.add(t.get(p));
            p++;
        }

        List<ColumnaCatalogo> destino = new ArrayList<>();
        if (columnas.isEmpty()) {
            destino.addAll(tabla.columnas());
        } else {
            for (Token columna : columnas) {
                ColumnaCatalogo c = tabla.buscarColumna(columna.getLexema());
                if (c != null) destino.add(c);
            }
        }

        if (valores.size() != destino.size()) {
            Token ubicacion = valores.isEmpty() ? tablaToken : valores.get(Math.min(valores.size(), destino.size()));
            e.agregar(FASE, "SEM-004",
                    "La cantidad de valores no coincide con la cantidad de columnas",
                    ubicacion.getLexema(), ubicacion.getlinea(), ubicacion.getcolumna());
        }

        int n = Math.min(valores.size(), destino.size());
        for (int i = 0; i < n; i++) {
            validarValor(valores.get(i), destino.get(i), e);
        }
    }

    private void validarColumnasInsert(List<Token> columnas, TablaCatalogo tabla, TablaErrores e) {
        Set<String> vistas = new HashSet<>();
        for (Token token : columnas) {
            String clave = normalizar(token.getLexema());
            if (!vistas.add(clave)) {
                e.agregar(FASE, "SEM-003", "La columna aparece más de una vez en INSERTAR",
                        token.getLexema(), token.getlinea(), token.getcolumna());
            }
            if (tabla.buscarColumna(token.getLexema()) == null) {
                e.agregar(FASE, "SEM-002", "La columna no existe en la tabla",
                        token.getLexema(), token.getlinea(), token.getcolumna());
            }
        }
    }

    private void validarActualizar(List<Token> t, TablaErrores e) {
        int p = 1;
        Token tablaToken = siguiente(t, p);
        if (tablaToken == null) return;
        TablaCatalogo tabla = tabla(tablaToken, e);
        if (tabla == null) return;
        p = 2;

        while (p < t.size()) {
            Token actual = t.get(p);
            if (actual.getTipo() == TipoToken.DONDE) {
                validarCondicion(t, p + 1, t.size(), tabla, e);
                break;
            }
            if (actual.getTipo() == TipoToken.IDENTIFICADOR) {
                ColumnaCatalogo columna = tabla.buscarColumna(actual.getLexema());
                if (columna == null) {
                    e.agregar(FASE, "SEM-002", "La columna no existe en la tabla",
                            actual.getLexema(), actual.getlinea(), actual.getcolumna());
                }
                p += 2; // columna, =
                if (p < t.size() && esValor(t.get(p)) && columna != null) {
                    validarValor(t.get(p), columna, e);
                }
            } else {
                p++;
            }
        }
    }

    private void validarSeleccionar(List<Token> t, TablaErrores e) {
        int desde = indice(t, TipoToken.DESDE);
        if (desde < 0 || desde + 1 >= t.size()) return;
        Token tablaToken = t.get(desde + 1);
        TablaCatalogo tabla = tabla(tablaToken, e);
        if (tabla == null) return;

        if (desde > 1 && t.get(1).getTipo() != TipoToken.ASTERISCO) {
            Set<String> repetidas = new HashSet<>();
            for (int i = 1; i < desde; i++) {
                Token token = t.get(i);
                if (token.getTipo() != TipoToken.IDENTIFICADOR) continue;
                if (!repetidas.add(normalizar(token.getLexema()))) {
                    e.agregar(FASE, "SEM-003", "La columna aparece más de una vez en la lista de selección",
                            token.getLexema(), token.getlinea(), token.getcolumna());
                }
                if (tabla.buscarColumna(token.getLexema()) == null) {
                    e.agregar(FASE, "SEM-002", "La columna no existe en la tabla",
                            token.getLexema(), token.getlinea(), token.getcolumna());
                }
            }
        }

        int donde = indice(t, TipoToken.DONDE);
        if (donde >= 0) {
            validarCondicion(t, donde + 1, t.size(), tabla, e);
        }
    }

    private void validarBorrar(List<Token> t, TablaErrores e) {
        int desde = indice(t, TipoToken.DESDE);
        if (desde < 0 || desde + 1 >= t.size()) return;
        TablaCatalogo tabla = tabla(t.get(desde + 1), e);
        if (tabla == null) return;
        int donde = indice(t, TipoToken.DONDE);
        if (donde >= 0) validarCondicion(t, donde + 1, t.size(), tabla, e);
    }

    private void validarCondicion(List<Token> t, int inicio, int fin,
            TablaCatalogo tabla, TablaErrores e) {
        int p = inicio;
        while (p < fin) {
            Token token = t.get(p);
            if (token.getTipo() == TipoToken.OP_NO
                    || token.getTipo() == TipoToken.OP_Y
                    || token.getTipo() == TipoToken.OP_O
                    || token.getTipo() == TipoToken.PARENTESIS_IZQ
                    || token.getTipo() == TipoToken.PARENTESIS_DER) {
                p++;
                continue;
            }
            if (token.getTipo() == TipoToken.IDENTIFICADOR) {
                ColumnaCatalogo columna = tabla.buscarColumna(token.getLexema());
                if (columna == null) {
                    e.agregar(FASE, "SEM-002", "La columna no existe en la tabla",
                            token.getLexema(), token.getlinea(), token.getcolumna());
                }
                // estructura válida: identificador, operador, valor
                if (p + 2 < fin && esOperadorRelacional(t.get(p + 1))
                        && esValor(t.get(p + 2)) && columna != null) {
                    Token valor = t.get(p + 2);
                    if (valor.getTipo() == TipoToken.IDENTIFICADOR) {
                        ColumnaCatalogo derecha = tabla.buscarColumna(valor.getLexema());
                        if (derecha == null) {
                            e.agregar(FASE, "SEM-002", "La columna no existe en la tabla",
                                    valor.getLexema(), valor.getlinea(), valor.getcolumna());
                        } else {
                            validarCompatibilidadComparacion(columna, derecha, t.get(p + 1), valor, e);
                        }
                    } else {
                        validarValor(valor, columna, e);
                    }
                    p += 3;
                    continue;
                }
            }
            p++;
        }
    }

    private void validarCompatibilidadComparacion(ColumnaCatalogo izquierda,
            ColumnaCatalogo derecha, Token operador, Token elemento, TablaErrores e) {
        if (!tiposCompatibles(izquierda.tipoDato(), derecha.tipoDato())) {
            e.agregar(FASE, "SEM-005", "Los operandos de la condición tienen tipos incompatibles",
                    elemento.getLexema(), elemento.getlinea(), elemento.getcolumna());
        }
        if (esOrden(operatorType(operador))
                && !esOrdenable(izquierda.tipoDato())) {
            e.agregar(FASE, "SEM-006", "El operador relacional requiere un tipo ordenable",
                    operador.getLexema(), operador.getlinea(), operador.getcolumna());
        }
    }

    private void validarValor(Token valor, ColumnaCatalogo columna, TablaErrores e) {
        TipoToken esperado = columna.tipoDato();
        if (!valorCompatible(valor.getTipo(), esperado)) {
            e.agregar(FASE, "SEM-005", "El valor no es compatible con el tipo de la columna "
                    + columna.nombre(),
                    valor.getLexema(), valor.getlinea(), valor.getcolumna());
        }
    }

    private boolean valorCompatible(TipoToken valor, TipoToken esperado) {
        return switch (valor) {
            case LITERAL_ENTERO -> esperado == TipoToken.TIPO_ENTERO
                    || esperado == TipoToken.TIPO_DECIMAL;
            case LITERAL_DECIMAL -> esperado == TipoToken.TIPO_DECIMAL;
            case LITERAL_CADENA -> esperado == TipoToken.TIPO_TEXTO
                    || esperado == TipoToken.TIPO_FECHA;
            case IDENTIFICADOR -> true; // se valida contra la columna al resolver referencias
            default -> false;
        };
    }

    private boolean tiposCompatibles(TipoToken a, TipoToken b) {
        return a == b || (esNumerico(a) && esNumerico(b));
    }

    private boolean esNumerico(TipoToken t) {
        return t == TipoToken.TIPO_ENTERO || t == TipoToken.TIPO_DECIMAL;
    }

    private boolean esOrdenable(TipoToken t) {
        return t == TipoToken.TIPO_ENTERO || t == TipoToken.TIPO_DECIMAL
                || t == TipoToken.TIPO_TEXTO || t == TipoToken.TIPO_FECHA;
    }

    private boolean esOrden(TipoToken t) {
        return t == TipoToken.OP_MAYOR || t == TipoToken.OP_MENOR
                || t == TipoToken.OP_MAYOR_IGUAL || t == TipoToken.OP_MENOR_IGUAL;
    }

    private TipoToken operatorType(Token t) { return t.getTipo(); }

    private boolean esOperadorRelacional(Token t) {
        return t.getTipo() == TipoToken.OP_IGUAL || t.getTipo() == TipoToken.OP_MAYOR
                || t.getTipo() == TipoToken.OP_MENOR || t.getTipo() == TipoToken.OP_MAYOR_IGUAL
                || t.getTipo() == TipoToken.OP_MENOR_IGUAL || t.getTipo() == TipoToken.OP_DIFERENTE;
    }

    private boolean esValor(Token t) {
        return t.getTipo() == TipoToken.IDENTIFICADOR
                || t.getTipo() == TipoToken.LITERAL_ENTERO
                || t.getTipo() == TipoToken.LITERAL_DECIMAL
                || t.getTipo() == TipoToken.LITERAL_CADENA;
    }

    private TablaCatalogo tabla(Token token, TablaErrores e) {
        if (catalogo == null) {
            e.agregar(FASE, "SEM-000", "No se configuró un catálogo semántico",
                    "", token.getlinea(), token.getcolumna());
            return null;
        }
        TablaCatalogo tabla = catalogo.buscarTabla(token.getLexema());
        if (tabla == null) {
            e.agregar(FASE, "SEM-001", "La tabla no existe en el catálogo",
                    token.getLexema(), token.getlinea(), token.getcolumna());
        }
        return tabla;
    }

    private Token siguiente(List<Token> t, int p) {
        return p >= 0 && p < t.size() ? t.get(p) : null;
    }

    private int indice(List<Token> t, TipoToken tipo) {
        for (int i = 0; i < t.size(); i++) if (t.get(i).getTipo() == tipo) return i;
        return -1;
    }

    private String normalizar(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}
