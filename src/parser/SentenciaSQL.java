package parser;

/** Contrato común para sentencias DDL y DML producidas por el parser. */
public interface SentenciaSQL {

    int getLinea();

    int getColumna();

    String getNombreTabla();
}
