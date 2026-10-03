package symbol_table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lexer.TipoToken;

/* Catalogo del esquema (fase 2).
 * La TablaSimbolos guarda todos los nombres que vio el scanner aunque la
 * sentencia este mala. Aqui solo entran las tablas y columnas que la semantica
 * DDL acepto. Empieza vacio y va cambiando en el orden de las sentencias.
 * Las validaciones con mensaje de error las hace AnalizadorSemanticoDDL antes
 * de llamar a estos metodos, aqui solo se agrega o se quita.
 */
public class CatalogoEsquema {

    //igual que en TablaSimbolos: la llave es el nombre en mayusculas
    private final Map<String, TablaDeclarada> tablas = new LinkedHashMap<>();

    //---- metodos para cambiar el esquema (los usa la semantica DDL) ----

    public boolean crearTabla(String nombre, int linea, int columna) {
        if (nombre == null || existeTabla(nombre)) {
            return false;
        }
        TablaDeclarada nueva = new TablaDeclarada(nombre, linea, columna);
        tablas.put(nueva.getClave(), nueva);
        return true;
    }

    public boolean agregarColumna(String nombreTabla, String nombreColumna, TipoToken tipo, int linea, int columna) {
        TablaDeclarada tabla = buscarTabla(nombreTabla);
        if (tabla == null || nombreColumna == null || tabla.tieneColumna(nombreColumna)) {
            return false;
        }
        tabla.agregarColumna(nombreColumna, tipo, linea, columna);
        return true;
    }

    public boolean eliminarTabla(String nombre) {
        if (nombre == null) {
            return false;
        }
        return tablas.remove(Simbolo.normalizar(nombre)) != null;
    }

    public boolean renombrarTabla(String actual, String nuevo) {
        TablaDeclarada tabla = buscarTabla(actual);
        if (tabla == null || nuevo == null || existeTabla(nuevo)) {
            return false;
        }
        String claveAnterior = tabla.getClave();
        tabla.renombrar(nuevo);
        Map<String, TablaDeclarada> renombradas = new LinkedHashMap<>();
        for (Map.Entry<String, TablaDeclarada> entrada : tablas.entrySet()) {
            renombradas.put(entrada.getKey().equals(claveAnterior)
                    ? tabla.getClave() : entrada.getKey(), entrada.getValue());
        }
        tablas.clear();
        tablas.putAll(renombradas);
        return true;
    }

    public boolean modificarColumna(String nombreTabla, String nombreColumna, TipoToken tipo) {
        TablaDeclarada tabla = buscarTabla(nombreTabla);
        if (tabla == null || tipo == null) {
            return false;
        }
        return tabla.modificarColumna(nombreColumna, tipo);
    }

    public boolean eliminarColumna(String nombreTabla, String nombreColumna) {
        TablaDeclarada tabla = buscarTabla(nombreTabla);
        if (tabla == null || !tabla.tieneColumna(nombreColumna)) {
            return false;
        }
        tabla.eliminarColumna(nombreColumna);
        return true;
    }

    public void limpiar() {
        tablas.clear();
    }

    //---- consultas (las usa la semantica DML de la persona 4) ----

    public TablaDeclarada buscarTabla(String nombre) {
        if (nombre == null) {
            return null;
        }
        return tablas.get(Simbolo.normalizar(nombre));
    }

    public boolean existeTabla(String nombre) {
        return buscarTabla(nombre) != null;
    }

    //busca la columna DENTRO de esa tabla
    public ColumnaDeclarada buscarColumna(String nombreTabla, String nombreColumna) {
        TablaDeclarada tabla = buscarTabla(nombreTabla);
        if (tabla == null) {
            return null;
        }
        return tabla.buscarColumna(nombreColumna);
    }

    public boolean existeColumna(String nombreTabla, String nombreColumna) {
        return buscarColumna(nombreTabla, nombreColumna) != null;
    }

    //null si no existe la tabla o la columna
    public TipoToken tipoDeColumna(String nombreTabla, String nombreColumna) {
        ColumnaDeclarada col = buscarColumna(nombreTabla, nombreColumna);
        if (col == null) {
            return null;
        }
        return col.getTipoDato();
    }

    //columnas en el orden en que se declararon, vacia si la tabla no existe
    public List<ColumnaDeclarada> columnasDe(String nombreTabla) {
        TablaDeclarada tabla = buscarTabla(nombreTabla);
        if (tabla == null) {
            return new ArrayList<>();
        }
        return tabla.getColumnas();
    }

    public List<TablaDeclarada> obtenerTablas() {
        return new ArrayList<>(tablas.values());
    }

    public int cantidadTablas() {
        return tablas.size();
    }

    public boolean estaVacio() {
        return tablas.isEmpty();
    }

    //---- salida, con el mismo estilo de tabla que TablaSimbolos ----

    private static final String SEPARADOR =
            "+--------------------------------+-----------+-----------+----------------------+-------+-------+";

    @Override
    public String toString() {
        String salto = System.lineSeparator();
        StringBuilder sb = new StringBuilder();

        sb.append(SEPARADOR).append(salto);
        sb.append(String.format("| %-30s | %-9s | %-9s | %-20s | %-5s | %-5s |",
                "IDENTIDAD", "CATEGORIA", "TIPO DATO", "AMBITO", "LINEA", "COL"));
        sb.append(salto).append(SEPARADOR).append(salto);

        if (tablas.isEmpty()) {
            sb.append("  (catalogo vacio, no hay tablas declaradas)").append(salto);
            sb.append(SEPARADOR);
            return sb.toString();
        }

        int totalColumnas = 0;
        for (TablaDeclarada t : tablas.values()) {
            sb.append(fila(t.getNombre(), "TABLA", "-", t.getAmbito(), t.getLinea(), t.getColumna()));
            sb.append(salto);
            for (ColumnaDeclarada c : t.getColumnas()) {
                sb.append(fila(c.getIdentidad(), "COLUMNA", c.getTipoDatoTexto(), c.getAmbito(),
                        c.getLinea(), c.getColumna()));
                sb.append(salto);
                totalColumnas++;
            }
        }
        sb.append(SEPARADOR).append(salto);
        sb.append("Tablas: ").append(tablas.size()).append("   Columnas: ").append(totalColumnas);
        return sb.toString();
    }

    private String fila(String identidad, String categoria, String tipo, String ambito, int linea, int columna) {
        return String.format("| %-30s | %-9s | %-9s | %-20s | %-5d | %-5d |",
                recortar(identidad, 30), categoria, tipo, recortar(ambito, 20), linea, columna);
    }

    private String recortar(String texto, int ancho) {
        return texto.length() <= ancho ? texto : texto.substring(0, ancho - 3) + "...";
    }

    public void imprimir() {
        System.out.println(this);
    }
}
