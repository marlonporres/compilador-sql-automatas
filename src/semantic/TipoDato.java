package semantic;

import lexer.TipoToken;

/** Tipos del catálogo de esquemas, independientes de las filas de datos. */
public enum TipoDato {
    ENTERO, TEXTO, FECHA, DECIMAL, BOOLEANO;

    public static TipoDato desdeToken(TipoToken tipo) {
        return switch (tipo) {
            case TIPO_ENTERO -> ENTERO;
            case TIPO_TEXTO -> TEXTO;
            case TIPO_FECHA -> FECHA;
            case TIPO_DECIMAL -> DECIMAL;
            case TIPO_BOOLEANO -> BOOLEANO;
            default -> throw new IllegalArgumentException("No es un tipo de dato: " + tipo);
        };
    }

    public boolean esNumerico() { return this == ENTERO || this == DECIMAL; }
}
