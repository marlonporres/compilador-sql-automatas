package symbol_table;

import java.util.ArrayList;
import java.util.List;

import lexer.TipoToken;

//Tabla que se creo bien con CREAR TABLA.
//Las columnas van en una lista para que queden en el orden en que se
//declararon, ese orden lo ocupa INSERTAR para comparar los valores
public class TablaDeclarada {

    private String nombre;
    private String clave;
    private final int linea;
    private final int columna;
    private final List<ColumnaDeclarada> columnas = new ArrayList<>();

    public TablaDeclarada(String nombre, int linea, int columna) {
        this.nombre = nombre;
        this.clave = Simbolo.normalizar(nombre);
        this.linea = linea;
        this.columna = columna;
    }

    public String getNombre() {
        return nombre;
    }

    public String getClave() {
        return clave;
    }

    //las tablas estan en el ambito global, las columnas en el de su tabla
    public String getAmbito() {
        return "GLOBAL";
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    //devuelvo una copia para que de afuera no le metan o quiten columnas
    public List<ColumnaDeclarada> getColumnas() {
        return new ArrayList<>(columnas);
    }

    public int cantidadColumnas() {
        return columnas.size();
    }

    public ColumnaDeclarada buscarColumna(String nombreColumna) {
        String buscada = Simbolo.normalizar(nombreColumna);
        for (ColumnaDeclarada c : columnas) {
            if (c.getClave().equals(buscada)) {
                return c;
            }
        }
        return null;
    }

    public boolean tieneColumna(String nombreColumna) {
        return buscarColumna(nombreColumna) != null;
    }

    void agregarColumna(String nombreColumna, TipoToken tipoDato, int lineaCol, int columnaCol) {
        columnas.add(new ColumnaDeclarada(nombreColumna, tipoDato, nombre, lineaCol, columnaCol));
    }

    void eliminarColumna(String nombreColumna) {
        columnas.remove(buscarColumna(nombreColumna));
    }

    //Sustituye el tipo conservando nombre, posicion de declaracion y orden.
    boolean modificarColumna(String nombreColumna, TipoToken tipoDato) {
        ColumnaDeclarada anterior = buscarColumna(nombreColumna);
        if (anterior == null) {
            return false;
        }
        int posicion = columnas.indexOf(anterior);
        columnas.set(posicion, new ColumnaDeclarada(anterior.getNombre(), tipoDato,
                nombre, anterior.getLinea(), anterior.getColumna()));
        return true;
    }

    //al renombrar tambien hay que cambiarle el ambito a las columnas
    void renombrar(String nuevoNombre) {
        nombre = nuevoNombre;
        clave = Simbolo.normalizar(nuevoNombre);
        for (ColumnaDeclarada c : columnas) {
            c.setAmbito(nuevoNombre);
        }
    }

    @Override
    public String toString() {
        return nombre + " " + columnas;
    }
}
