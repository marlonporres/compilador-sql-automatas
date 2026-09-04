package symbol_table;

//Categoria del identificador. En la fase 1 casi todo queda en DESCONOCIDA
//porque el scanner todavia no sabe si es tabla o columna, eso es de la fase 2.
public enum CategoriaSimbolo {
    TABLA,
    COLUMNA,
    DESCONOCIDA
}
