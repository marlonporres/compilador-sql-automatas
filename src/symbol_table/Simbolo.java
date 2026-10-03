package symbol_table;

import java.util.Locale;

import lexer.TipoToken;
import lexer.Token;

//Cada fila de la tabla de simbolos: un identificador y sus datos.
public class Simbolo {

    public static final String TIPO_DATO_DESCONOCIDO = "DESCONOCIDO";
    //fase 2: cuando la misma columna esta en varias tablas con distinto tipo
    public static final String TIPO_DATO_VARIOS = "VARIOS";

    private final int id;
    private final String nombre;        //como se escribio la primera vez
    private final String clave;         //el nombre en mayusculas, sirve de llave
    private final TipoToken tipoToken;
    private final int linea;
    private final int columna;

    private CategoriaSimbolo categoria;
    private String tipoDato;
    private int apariciones;

    public Simbolo(int id, String nombre, TipoToken tipoToken, int linea, int columna) {
        if (nombre == null || nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre del simbolo no puede ser vacio");
        }
        this.id = id;
        this.nombre = nombre;
        this.clave = normalizar(nombre);
        this.tipoToken = tipoToken;
        this.linea = linea;
        this.columna = columna;
        this.categoria = CategoriaSimbolo.DESCONOCIDA;
        this.tipoDato = TIPO_DATO_DESCONOCIDO;
        this.apariciones = 1;
    }

    public Simbolo(int id, Token token) {
        this(id, token.getLexema(), token.getTipo(), token.getlinea(), token.getcolumna());
    }

    // La identidad del símbolo no debe depender de la configuración regional del equipo.
    public static String normalizar(String nombre) {
        return nombre == null ? null : nombre.trim().toUpperCase(Locale.ROOT);
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getClave() {
        return clave;
    }

    public TipoToken getTipoToken() {
        return tipoToken;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    public CategoriaSimbolo getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaSimbolo categoria) {
        this.categoria = (categoria == null) ? CategoriaSimbolo.DESCONOCIDA : categoria;
    }

    public String getTipoDato() {
        return tipoDato;
    }

    public void setTipoDato(String tipoDato) {
        if (tipoDato == null || tipoDato.isEmpty()) {
            this.tipoDato = TIPO_DATO_DESCONOCIDO;
        } else {
            this.tipoDato = tipoDato.trim().toUpperCase(Locale.ROOT);
        }
    }

    //Recibe TIPO_ENTERO y guarda solo ENTERO, para que se vea mejor en la tabla.
    public void setTipoDato(TipoToken tipoDeDato) {
        this.tipoDato = nombreTipo(tipoDeDato);
    }

    //lo saque aparte porque el catalogo tambien lo usa
    public static String nombreTipo(TipoToken tipoDeDato) {
        if (tipoDeDato == null) {
            return TIPO_DATO_DESCONOCIDO;
        }
        String nombreTipo = tipoDeDato.name();
        return nombreTipo.startsWith("TIPO_") ? nombreTipo.substring(5) : nombreTipo;
    }

    public int getApariciones() {
        return apariciones;
    }

    public void incrementarApariciones() {
        this.apariciones++;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Simbolo)) {
            return false;
        }
        return this.clave.equals(((Simbolo) otro).clave);
    }

    @Override
    public int hashCode() {
        return clave.hashCode();
    }

    @Override
    public String toString() {
        return String.format("<#%d | %s | %s | %s | %s | L:%d C:%d | apariciones:%d>",
                id, nombre, tipoToken, categoria, tipoDato, linea, columna, apariciones);
    }
}
