package semantic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import lexer.Token;
import parser.ColumnaDDL;

/**
 * Complemento de la tabla léxica: contiene solo declaraciones aceptadas.
 * Cada tabla tiene su propio ámbito de columnas y conserva su orden.
 */
public final class CatalogoSemantico {
    private final Map<String, Tabla> tablas = new LinkedHashMap<>();

    public static String clave(String nombre) { return nombre.toUpperCase(Locale.ROOT); }
    public Tabla buscar(String nombre) { return tablas.get(clave(nombre)); }
    public List<Tabla> obtenerTablas() { return List.copyOf(tablas.values()); }
    public int tamano() { return tablas.size(); }

    /** Carga un esquema externo; las posiciones son de origen, no del script actual. */
    public void incorporarTabla(String nombre, List<Columna> columnas) {
        Tabla tabla = new Tabla(nombre, 1, 1);
        for (Columna columna : columnas) tabla.columnas.put(clave(columna.nombre()), columna);
        tablas.put(clave(nombre), tabla);
    }

    /** El análisis simula DDL sobre su copia y nunca altera el esquema inicial. */
    public CatalogoSemantico copiar() {
        CatalogoSemantico copia = new CatalogoSemantico();
        for (Tabla tabla : obtenerTablas()) {
            Tabla nueva = new Tabla(tabla.nombre, tabla.linea, tabla.columna);
            nueva.columnas.putAll(tabla.columnas);
            copia.tablas.put(clave(tabla.nombre), nueva);
        }
        return copia;
    }

    void crear(Token nombre, List<ColumnaDDL> columnas) {
        Tabla tabla = new Tabla(nombre.getLexema(), nombre.getlinea(), nombre.getcolumna());
        columnas.forEach(tabla::agregar);
        tablas.put(clave(nombre.getLexema()), tabla);
    }

    void eliminar(String nombre) { tablas.remove(clave(nombre)); }

    void renombrar(String actual, Token nuevo) {
        Tabla tabla = buscar(actual);
        tabla.nombre = nuevo.getLexema();
        Map<String, Tabla> renombradas = new LinkedHashMap<>();
        for (Map.Entry<String, Tabla> entrada : tablas.entrySet()) {
            renombradas.put(entrada.getKey().equals(clave(actual))
                    ? clave(nuevo.getLexema()) : entrada.getKey(), entrada.getValue());
        }
        tablas.clear();
        tablas.putAll(renombradas);
    }

    public void imprimir() {
        if (tablas.isEmpty()) {
            System.out.println("(catálogo semántico vacío)");
            return;
        }
        System.out.printf("%-10s | %-18s | %-18s | %-9s | %s%n",
                "CATEGORIA", "NOMBRE", "AMBITO", "TIPO DATO", "POSICION");
        for (Tabla tabla : tablas.values()) {
            System.out.printf("%-10s | %-18s | %-18s | %-9s | L:%d C:%d%n",
                    "TABLA", tabla.nombre, "GLOBAL", "-", tabla.linea, tabla.columna);
            for (Columna columna : tabla.obtenerColumnas()) {
                System.out.printf("%-10s | %-18s | %-18s | %-9s | L:%d C:%d%n",
                        "COLUMNA", columna.nombre(), tabla.nombre, columna.tipo(),
                        columna.linea(), columna.columna());
            }
        }
    }

    public record Columna(String nombre, TipoDato tipo, int linea, int columna) { }

    public static final class Tabla {
        private String nombre;
        private final int linea;
        private final int columna;
        private final Map<String, Columna> columnas = new LinkedHashMap<>();

        private Tabla(String nombre, int linea, int columna) {
            this.nombre = nombre;
            this.linea = linea;
            this.columna = columna;
        }

        public String getNombre() { return nombre; }
        public Columna buscarColumna(String nombre) { return columnas.get(clave(nombre)); }
        public List<Columna> obtenerColumnas() { return List.copyOf(columnas.values()); }

        void agregar(ColumnaDDL columna) {
            columnas.put(clave(columna.getNombre()), new Columna(columna.getNombre(),
                    TipoDato.desdeToken(columna.getTipoDato()), columna.getLinea(), columna.getColumna()));
        }
        void quitar(String nombre) { columnas.remove(clave(nombre)); }
        void modificar(ColumnaDDL columna) {
            Columna anterior = buscarColumna(columna.getNombre());
            columnas.put(clave(columna.getNombre()), new Columna(anterior.nombre(),
                    TipoDato.desdeToken(columna.getTipoDato()), anterior.linea(), anterior.columna()));
        }
    }
}
