package parser;

import java.util.Objects;

import lexer.TipoToken;

/** Declaración de una columna dentro de una sentencia CREAR TABLA. */
public final class ColumnaDDL {

    private final String nombre;
    private final TipoToken tipoDato;
    private final int linea;
    private final int columna;

    public ColumnaDDL(String nombre, TipoToken tipoDato, int linea, int columna) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la columna no puede estar vacío");
        }
        if (!esTipoDato(tipoDato)) {
            throw new IllegalArgumentException("El tipo de dato de la columna no es válido");
        }
        this.nombre = nombre;
        this.tipoDato = tipoDato;
        this.linea = linea;
        this.columna = columna;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoToken getTipoDato() {
        return tipoDato;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    private static boolean esTipoDato(TipoToken tipo) {
        return tipo == TipoToken.TIPO_ENTERO
                || tipo == TipoToken.TIPO_TEXTO
                || tipo == TipoToken.TIPO_FECHA
                || tipo == TipoToken.TIPO_DECIMAL
                || tipo == TipoToken.TIPO_BOOLEANO;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof ColumnaDDL columna)) {
            return false;
        }
        return nombre.equals(columna.nombre)
                && tipoDato == columna.tipoDato
                && linea == columna.linea
                && this.columna == columna.columna;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, tipoDato, linea, columna);
    }

    @Override
    public String toString() {
        return nombre + " " + tipoDato;
    }
}
