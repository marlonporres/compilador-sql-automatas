package semantic;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import errors.Diagnostico;
import errors.FaseError;
import lexer.TipoToken;
import lexer.Token;
import parser.AsignacionSQL;
import parser.ColumnaDDL;
import parser.CondicionSQL;
import parser.SentenciaDDL;
import parser.SentenciaDML;
import parser.SentenciaSQL;
import parser.TipoSentenciaDDL;
import parser.ValorSQL;

/** Valida nombres, ámbitos y tipos; aplica solo DDL válido al esquema. */
public final class AnalizadorSemantico {
    private CatalogoSemantico catalogo;
    private List<Diagnostico> errores;

    public ResultadoSemantico analizar(List<SentenciaSQL> sentencias) {
        return analizar(sentencias, new CatalogoSemantico());
    }

    /** Permite validar contra un esquema ya declarado usando las mismas reglas. */
    public ResultadoSemantico analizar(List<SentenciaSQL> sentencias, CatalogoSemantico inicial) {
        if (inicial == null) throw new IllegalArgumentException("El catálogo no puede ser null");
        catalogo = inicial;
        errores = new ArrayList<>();
        int validas = 0;
        for (SentenciaSQL sentencia : sentencias) {
            int antes = errores.size();
            if (sentencia instanceof SentenciaDDL ddl) validarDDL(ddl);
            else if (sentencia instanceof SentenciaDML dml) validarDML(dml);
            if (errores.size() == antes) validas++;
        }
        return new ResultadoSemantico(catalogo, errores, validas);
    }

    private void validarDDL(SentenciaDDL sentencia) {
        CatalogoSemantico.Tabla tabla = catalogo.buscar(sentencia.getNombreTabla());
        if (sentencia.getTipo() == TipoSentenciaDDL.CREAR_TABLA) {
            int antes = errores.size();
            if (tabla != null) error("SEM-001", "La tabla ya existe", sentencia.getTabla());
            Set<String> nombres = new HashSet<>();
            for (ColumnaDDL columna : sentencia.getColumnas()) {
                if (!nombres.add(CatalogoSemantico.clave(columna.getNombre()))) {
                    error("SEM-002", "Columna repetida en CREAR TABLA", token(columna));
                }
            }
            if (errores.size() == antes) catalogo.crear(sentencia.getTabla(), sentencia.getColumnas());
            return;
        }
        if (tabla == null) {
            error("SEM-003", "La tabla no existe", sentencia.getTabla());
            return;
        }
        switch (sentencia.getTipo()) {
            case ELIMINAR_TABLA -> catalogo.eliminar(sentencia.getNombreTabla());
            case TRUNCAR_TABLA -> { /* Fase 2 conserva el esquema, sin gestionar filas. */ }
            case RENOMBRAR_TABLA -> {
                Token nuevo = sentencia.getNuevoNombre();
                if (catalogo.buscar(nuevo.getLexema()) != null) {
                    error("SEM-001", "El nuevo nombre de tabla ya existe", nuevo);
                } else catalogo.renombrar(sentencia.getNombreTabla(), nuevo);
            }
            case ALTERAR_TABLA -> alterar(sentencia, tabla);
            default -> throw new IllegalStateException("DDL no soportado");
        }
    }

    private void alterar(SentenciaDDL sentencia, CatalogoSemantico.Tabla tabla) {
        if (sentencia.getOperacion().equals("QUITAR")) {
            Token columna = sentencia.getNuevoNombre();
            if (resolverColumna(tabla, columna) == null) return;
            if (tabla.obtenerColumnas().size() == 1) {
                error("SEM-009", "No se puede quitar la última columna de una tabla", columna);
            } else tabla.quitar(columna.getLexema());
            return;
        }
        ColumnaDDL columna = sentencia.getColumnas().get(0);
        CatalogoSemantico.Columna anterior = tabla.buscarColumna(columna.getNombre());
        if (sentencia.getOperacion().equals("AGREGAR")) {
            if (anterior != null) error("SEM-002", "La columna ya existe en esta tabla", token(columna));
            else tabla.agregar(columna);
        } else {
            if (anterior == null) error("SEM-004", "La columna a modificar no existe", token(columna));
            else tabla.modificar(columna);
        }
    }

    private void validarDML(SentenciaDML sentencia) {
        CatalogoSemantico.Tabla tabla = catalogo.buscar(sentencia.getNombreTabla());
        if (tabla == null) {
            error("SEM-003", "La tabla no existe", sentencia.getTabla());
            return;
        }
        switch (sentencia.getTipo()) {
            case SELECCIONAR -> {
                for (Token columna : sentencia.getColumnas()) resolverColumna(tabla, columna);
            }
            case INSERTAR -> validarInsertar(sentencia, tabla);
            case ACTUALIZAR -> {
                Set<String> nombres = new HashSet<>();
                for (AsignacionSQL asignacion : sentencia.getAsignaciones()) {
                    if (!nombres.add(CatalogoSemantico.clave(asignacion.getNombreColumna()))) {
                        error("SEM-005", "Columna asignada más de una vez", asignacion.getColumna());
                    }
                    CatalogoSemantico.Columna columna = resolverColumna(tabla, asignacion.getColumna());
                    if (columna != null) validarValor(columna.tipo(), token(asignacion.getValor()));
                }
            }
            case BORRAR -> { }
        }
        if (sentencia.getCondicion() != null) validarCondicion(tabla, sentencia.getCondicion());
    }

    private void validarInsertar(SentenciaDML sentencia, CatalogoSemantico.Tabla tabla) {
        List<CatalogoSemantico.Columna> destino = new ArrayList<>();
        if (sentencia.getColumnasInsertar().isEmpty()) {
            destino.addAll(tabla.obtenerColumnas());
        } else {
            Set<String> nombres = new HashSet<>();
            for (Token nombre : sentencia.getColumnasInsertar()) {
                if (!nombres.add(CatalogoSemantico.clave(nombre.getLexema()))) {
                    error("SEM-005", "Columna repetida en INSERTAR", nombre);
                }
                destino.add(resolverColumna(tabla, nombre));
            }
        }
        if (destino.size() != sentencia.getValores().size()) {
            error("SEM-006", "Cantidad de valores incorrecta: se esperaban "
                    + destino.size() + " y se recibieron " + sentencia.getValores().size(),
                    sentencia.getTabla());
            return;
        }
        for (int i = 0; i < destino.size(); i++) {
            if (destino.get(i) != null) validarValor(destino.get(i).tipo(), token(sentencia.getValores().get(i)));
        }
    }

    private CatalogoSemantico.Columna resolverColumna(CatalogoSemantico.Tabla tabla, Token token) {
        CatalogoSemantico.Columna columna = tabla.buscarColumna(token.getLexema());
        if (columna == null) error("SEM-004", "La columna no existe en " + tabla.getNombre(), token);
        return columna;
    }

    private void validarValor(TipoDato tipo, Token valor) {
        if (!compatible(tipo, valor)) {
            error("SEM-007", "Valor incompatible con " + tipo, valor);
        }
    }

    private static boolean compatible(TipoDato tipo, Token valor) {
        return switch (tipo) {
            case ENTERO -> valor.getTipo() == TipoToken.LITERAL_ENTERO;
            case DECIMAL -> valor.getTipo() == TipoToken.LITERAL_ENTERO
                    || valor.getTipo() == TipoToken.LITERAL_DECIMAL;
            case TEXTO -> valor.getTipo() == TipoToken.LITERAL_CADENA;
            case FECHA -> esFecha(valor);
            case BOOLEANO -> CondicionSQL.esBooleano(valor);
        };
    }

    private static boolean esFecha(Token valor) {
        if (valor.getTipo() != TipoToken.LITERAL_CADENA
                || !valor.getLexema().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return false;
        try {
            LocalDate.parse(valor.getLexema());
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void validarCondicion(CatalogoSemantico.Tabla tabla, CondicionSQL condicion) {
        if (!condicion.esComparacion()) {
            validarCondicion(tabla, condicion.getPrimera());
            if (condicion.getSegunda() != null) validarCondicion(tabla, condicion.getSegunda());
            return;
        }
        Token izquierda = condicion.getIzquierdo();
        Token derecha = condicion.getDerecho();
        TipoDato tipoIzq = resolverTipo(tabla, izquierda);
        TipoDato tipoDer = resolverTipo(tabla, derecha);
        if (tipoIzq == null || tipoDer == null) return;
        boolean compatibles = tipoIzq == tipoDer || (tipoIzq.esNumerico() && tipoDer.esNumerico());
        // Las cadenas adquieren tipo FECHA solo al compararse con una columna FECHA.
        if (tipoIzq == TipoDato.FECHA && derecha.getTipo() == TipoToken.LITERAL_CADENA) {
            compatibles = esFecha(derecha);
        } else if (tipoDer == TipoDato.FECHA && izquierda.getTipo() == TipoToken.LITERAL_CADENA) {
            compatibles = esFecha(izquierda);
        }
        if (!compatibles) {
            error("SEM-008", "Comparación incompatible: " + tipoIzq + " con " + tipoDer, derecha);
        } else if ((tipoIzq == TipoDato.BOOLEANO || tipoDer == TipoDato.BOOLEANO
                || (tipoIzq == TipoDato.TEXTO && tipoDer == TipoDato.TEXTO))
                && !List.of("=", "!=", "<>").contains(condicion.getOperador())) {
            error("SEM-008", "TEXTO y BOOLEANO admiten solo igualdad o desigualdad", derecha);
        }
    }

    private TipoDato resolverTipo(CatalogoSemantico.Tabla tabla, Token token) {
        if (CondicionSQL.esBooleano(token)) return TipoDato.BOOLEANO;
        return switch (token.getTipo()) {
            case IDENTIFICADOR -> {
                CatalogoSemantico.Columna columna = resolverColumna(tabla, token);
                yield columna == null ? null : columna.tipo();
            }
            case LITERAL_ENTERO -> TipoDato.ENTERO;
            case LITERAL_DECIMAL -> TipoDato.DECIMAL;
            case LITERAL_CADENA -> TipoDato.TEXTO;
            default -> null;
        };
    }

    private void error(String codigo, String mensaje, Token token) {
        errores.add(new Diagnostico(FaseError.SEMANTICA, codigo, mensaje,
                token.getLexema(), token.getlinea(), token.getcolumna()));
    }
    private static Token token(ColumnaDDL columna) {
        return new Token(TipoToken.IDENTIFICADOR, columna.getNombre(),
                columna.getLinea(), columna.getColumna());
    }
    private static Token token(ValorSQL valor) {
        return new Token(valor.getTipo(), valor.getLexema(), valor.getLinea(), valor.getColumna());
    }
}
