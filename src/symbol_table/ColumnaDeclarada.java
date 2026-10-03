package symbol_table;

import lexer.TipoToken;

//Una columna que quedo bien declarada en un CREAR TABLA.
//El ambito es el nombre de su tabla, por eso usuarios.id y productos.id
//son dos columnas diferentes aunque se llamen igual
public class ColumnaDeclarada {

    private final String nombre;
    private final String clave;      //nombre en mayusculas
    private final TipoToken tipoDato;
    private final int linea;
    private final int columna;
    private String ambito;           //tabla a la que pertenece, cambia si se renombra la tabla

    public ColumnaDeclarada(String nombre, TipoToken tipoDato, String ambito, int linea, int columna) {
        this.nombre = nombre;
        this.clave = Simbolo.normalizar(nombre);
        this.tipoDato = tipoDato;
        this.ambito = ambito;
        this.linea = linea;
        this.columna = columna;
    }

    public String getNombre() {
        return nombre;
    }

    public String getClave() {
        return clave;
    }

    public TipoToken getTipoDato() {
        return tipoDato;
    }

    //ENTERO en vez de TIPO_ENTERO, igual que en la tabla de simbolos
    public String getTipoDatoTexto() {
        return Simbolo.nombreTipo(tipoDato);
    }

    public String getAmbito() {
        return ambito;
    }

    void setAmbito(String ambito) {
        this.ambito = ambito;
    }

    //para mostrarla como usuarios.id
    public String getIdentidad() {
        return ambito + "." + nombre;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    @Override
    public String toString() {
        return getIdentidad() + " " + getTipoDatoTexto();
    }
}
