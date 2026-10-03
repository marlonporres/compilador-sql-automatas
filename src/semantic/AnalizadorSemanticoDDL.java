package semantic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import lexer.TipoToken;
import lexer.Token;
import parser.ColumnaDDL;
import parser.SentenciaDDL;
import symbol_table.CatalogoEsquema;
import symbol_table.CategoriaSimbolo;
import symbol_table.Simbolo;
import symbol_table.TablaDeclarada;
import symbol_table.TablaSimbolos;

/* Semantica de las sentencias DDL.
 * Recibe las sentencias que el parser ya acepto y las revisa en orden contra
 * el catalogo. Primero junto todos los errores de la sentencia y solo si no
 * salio ninguno hago el cambio en el catalogo, asi una sentencia mala no deja
 * nada a medias. El catalogo es el mismo que despues usa la semantica DML.
 *
 * Codigos: SEM-101 tabla repetida, SEM-102 columna repetida, SEM-103 tabla
 * no existe, SEM-104 columna no existe, SEM-105 tabla sin columnas,
 * SEM-106 representacion DDL incompleta o invalida
 */
public class AnalizadorSemanticoDDL {

    private final CatalogoEsquema catalogo;
    private final TablaSimbolos tablaSimbolos;   //la de la fase 1, puede venir null
    private final List<ErrorSemantico> errores = new ArrayList<>();

    public AnalizadorSemanticoDDL(CatalogoEsquema catalogo, TablaSimbolos tablaSimbolos) {
        this.catalogo = catalogo;
        this.tablaSimbolos = tablaSimbolos;
    }

    public AnalizadorSemanticoDDL(CatalogoEsquema catalogo) {
        this(catalogo, null);
    }

    //Revisa todas las sentencias en orden y devuelve los errores que salieron
    public List<ErrorSemantico> analizar(List<SentenciaDDL> sentencias) {
        List<ErrorSemantico> encontrados = new ArrayList<>();
        for (SentenciaDDL s : sentencias) {
            encontrados.addAll(analizarSentencia(s));
        }
        return encontrados;
    }

    //Revisa una sola sentencia, si devuelve la lista vacia es que se aplico.
    //Main lo puede usar cuando vengan DDL y DML mezclados porque importa el orden
    public List<ErrorSemantico> analizarSentencia(SentenciaDDL sentencia) {
        List<ErrorSemantico> nuevos = switch (sentencia.getTipo()) {
            case CREAR_TABLA -> crearTabla(sentencia);
            case ELIMINAR_TABLA -> eliminarTabla(sentencia);
            //TRUNCAR borra filas y aqui no manejamos filas, solo reviso que exista
            case TRUNCAR_TABLA -> revisarQueExista(sentencia);
            case ALTERAR_TABLA -> alterarTabla(sentencia);
            case RENOMBRAR_TABLA -> renombrarTabla(sentencia);
        };
        errores.addAll(nuevos);
        return nuevos;
    }

    private List<ErrorSemantico> crearTabla(SentenciaDDL sentencia) {
        List<ErrorSemantico> nuevos = new ArrayList<>();
        String tabla = sentencia.getNombreTabla();

        if (sentencia.getColumnas().isEmpty()
                || sentencia.getColumnas().stream().anyMatch(col -> !columnaCompleta(col))) {
            return incompleta(sentencia, "CREAR requiere al menos una columna con nombre y tipo validos");
        }

        if (catalogo.existeTabla(tabla)) {
            nuevos.add(new ErrorSemantico("SEM-101", "Ya existe una tabla con ese nombre",
                    tabla, sentencia.getLinea(), sentencia.getColumna()));
        }

        //si una columna se repite reporto cada repeticion con su posicion
        Set<String> vistas = new HashSet<>();
        for (ColumnaDDL col : sentencia.getColumnas()) {
            if (!vistas.add(Simbolo.normalizar(col.getNombre()))) {
                nuevos.add(new ErrorSemantico("SEM-102", "La columna se declara mas de una vez",
                        tabla + "." + col.getNombre(), col.getLinea(), col.getColumna()));
            }
        }

        if (nuevos.isEmpty()) {
            catalogo.crearTabla(tabla, sentencia.getLinea(), sentencia.getColumna());
            marcar(tabla, CategoriaSimbolo.TABLA, null);
            for (ColumnaDDL col : sentencia.getColumnas()) {
                catalogo.agregarColumna(tabla, col.getNombre(), col.getTipoDato(), col.getLinea(), col.getColumna());
                marcar(col.getNombre(), CategoriaSimbolo.COLUMNA, col.getTipoDato());
            }
        }
        return nuevos;
    }

    private List<ErrorSemantico> eliminarTabla(SentenciaDDL sentencia) {
        List<ErrorSemantico> nuevos = revisarQueExista(sentencia);
        if (nuevos.isEmpty()) {
            catalogo.eliminarTabla(sentencia.getNombreTabla());
        }
        return nuevos;
    }

    private List<ErrorSemantico> revisarQueExista(SentenciaDDL sentencia) {
        List<ErrorSemantico> nuevos = new ArrayList<>();
        if (!catalogo.existeTabla(sentencia.getNombreTabla())) {
            nuevos.add(new ErrorSemantico("SEM-103", "No existe una tabla con ese nombre",
                    sentencia.getNombreTabla(), sentencia.getLinea(), sentencia.getColumna()));
        }
        return nuevos;
    }

    private List<ErrorSemantico> alterarTabla(SentenciaDDL sentencia) {
        String operacion = sentencia.getOperacion();
        if (operacion == null) return incompleta(sentencia, "Falta la operacion de ALTERAR");
        operacion = operacion.toUpperCase(Locale.ROOT);
        if (operacion.equals("QUITAR")) {
            Token columna = sentencia.getNuevoNombre();
            if (!nombreCompleto(columna) || !sentencia.getColumnas().isEmpty()) {
                return incompleta(sentencia, "QUITAR requiere el nombre de una columna");
            }
            return quitarColumna(sentencia.getNombreTabla(), columna.getLexema(),
                    columna.getlinea(), columna.getcolumna());
        }
        if ((!operacion.equals("AGREGAR") && !operacion.equals("MODIFICAR"))
                || sentencia.getColumnas().size() != 1
                || !columnaCompleta(sentencia.getColumnas().get(0))
                || sentencia.getNuevoNombre() != null) {
            return incompleta(sentencia, "ALTERAR requiere AGREGAR o MODIFICAR una columna con tipo, o QUITAR un nombre");
        }
        ColumnaDDL columna = sentencia.getColumnas().get(0);
        return operacion.equals("AGREGAR")
                ? agregarColumnaInterna(sentencia.getNombreTabla(), columna)
                : modificarColumnaInterna(sentencia.getNombreTabla(), columna);
    }

    private List<ErrorSemantico> renombrarTabla(SentenciaDDL sentencia) {
        Token nuevo = sentencia.getNuevoNombre();
        if (!nombreCompleto(nuevo) || !sentencia.getColumnas().isEmpty()
                || (sentencia.getOperacion() != null && !sentencia.getOperacion().isEmpty())) {
            return incompleta(sentencia, "RENOMBRAR requiere A y el nuevo nombre de tabla");
        }
        return renombrarTablaInterna(sentencia.getNombreTabla(), nuevo.getLexema(),
                nuevo.getlinea(), nuevo.getcolumna());
    }

    private static boolean nombreCompleto(Token token) {
        return token != null && token.getTipo() == TipoToken.IDENTIFICADOR
                && token.getLexema() != null && !token.getLexema().isBlank();
    }

    private static boolean columnaCompleta(ColumnaDDL columna) {
        if (columna == null || columna.getNombre() == null || columna.getNombre().isBlank()) return false;
        TipoToken tipo = columna.getTipoDato();
        return tipo == TipoToken.TIPO_ENTERO || tipo == TipoToken.TIPO_TEXTO
                || tipo == TipoToken.TIPO_FECHA || tipo == TipoToken.TIPO_DECIMAL
                || tipo == TipoToken.TIPO_BOOLEANO;
    }

    private List<ErrorSemantico> incompleta(SentenciaDDL sentencia, String mensaje) {
        return List.of(new ErrorSemantico("SEM-106", mensaje, sentencia.getNombreTabla(),
                sentencia.getLinea(), sentencia.getColumna()));
    }

    //La API publica se conserva para usar y probar operaciones individuales.

    public List<ErrorSemantico> renombrarTabla(String actual, String nuevo, int linea, int columna) {
        List<ErrorSemantico> nuevos = renombrarTablaInterna(actual, nuevo, linea, columna);
        errores.addAll(nuevos);
        return nuevos;
    }

    private List<ErrorSemantico> renombrarTablaInterna(String actual, String nuevo, int linea, int columna) {
        List<ErrorSemantico> nuevos = new ArrayList<>();
        if (nuevo == null || nuevo.isBlank()) {
            nuevos.add(new ErrorSemantico("SEM-106", "Falta el nuevo nombre de tabla", actual, linea, columna));
            return nuevos;
        }
        if (!catalogo.existeTabla(actual)) {
            nuevos.add(new ErrorSemantico("SEM-103", "No existe una tabla con ese nombre", actual, linea, columna));
        }
        if (catalogo.existeTabla(nuevo)) {
            nuevos.add(new ErrorSemantico("SEM-101", "Ya existe una tabla con el nombre nuevo", nuevo, linea, columna));
        }
        if (nuevos.isEmpty()) {
            catalogo.renombrarTabla(actual, nuevo);
            marcar(nuevo, CategoriaSimbolo.TABLA, null);
        }
        return nuevos;
    }

    public List<ErrorSemantico> agregarColumna(String nombreTabla, ColumnaDDL col) {
        List<ErrorSemantico> nuevos = agregarColumnaInterna(nombreTabla, col);
        errores.addAll(nuevos);
        return nuevos;
    }

    private List<ErrorSemantico> agregarColumnaInterna(String nombreTabla, ColumnaDDL col) {
        List<ErrorSemantico> nuevos = new ArrayList<>();
        if (!columnaCompleta(col)) {
            nuevos.add(new ErrorSemantico("SEM-106", "La columna requiere nombre y tipo validos",
                    nombreTabla, col == null ? 1 : col.getLinea(), col == null ? 1 : col.getColumna()));
            return nuevos;
        }
        TablaDeclarada tabla = catalogo.buscarTabla(nombreTabla);
        if (tabla == null) {
            nuevos.add(new ErrorSemantico("SEM-103", "No existe una tabla con ese nombre",
                    nombreTabla, col.getLinea(), col.getColumna()));
        } else if (tabla.tieneColumna(col.getNombre())) {
            nuevos.add(new ErrorSemantico("SEM-102", "La tabla ya tiene una columna con ese nombre",
                    tabla.getNombre() + "." + col.getNombre(), col.getLinea(), col.getColumna()));
        } else {
            catalogo.agregarColumna(nombreTabla, col.getNombre(), col.getTipoDato(), col.getLinea(), col.getColumna());
            marcar(col.getNombre(), CategoriaSimbolo.COLUMNA, col.getTipoDato());
        }
        return nuevos;
    }

    public List<ErrorSemantico> modificarColumna(String nombreTabla, ColumnaDDL col) {
        List<ErrorSemantico> nuevos = modificarColumnaInterna(nombreTabla, col);
        errores.addAll(nuevos);
        return nuevos;
    }

    private List<ErrorSemantico> modificarColumnaInterna(String nombreTabla, ColumnaDDL col) {
        List<ErrorSemantico> nuevos = new ArrayList<>();
        if (!columnaCompleta(col)) {
            nuevos.add(new ErrorSemantico("SEM-106", "La columna requiere nombre y tipo validos",
                    nombreTabla, col == null ? 1 : col.getLinea(), col == null ? 1 : col.getColumna()));
            return nuevos;
        }
        TablaDeclarada tabla = catalogo.buscarTabla(nombreTabla);
        if (tabla == null) {
            nuevos.add(new ErrorSemantico("SEM-103", "No existe una tabla con ese nombre",
                    nombreTabla, col.getLinea(), col.getColumna()));
        } else if (!tabla.tieneColumna(col.getNombre())) {
            nuevos.add(new ErrorSemantico("SEM-104", "La tabla no tiene una columna con ese nombre",
                    tabla.getNombre() + "." + col.getNombre(), col.getLinea(), col.getColumna()));
        } else {
            catalogo.modificarColumna(nombreTabla, col.getNombre(), col.getTipoDato());
            marcar(col.getNombre(), CategoriaSimbolo.COLUMNA, col.getTipoDato());
        }
        return nuevos;
    }

    public List<ErrorSemantico> eliminarColumna(String nombreTabla, String nombreColumna, int linea, int columna) {
        List<ErrorSemantico> nuevos = quitarColumna(nombreTabla, nombreColumna, linea, columna);
        errores.addAll(nuevos);
        return nuevos;
    }

    private List<ErrorSemantico> quitarColumna(String nombreTabla, String nombreColumna, int linea, int columna) {
        List<ErrorSemantico> nuevos = new ArrayList<>();
        TablaDeclarada tabla = catalogo.buscarTabla(nombreTabla);
        if (tabla == null) {
            nuevos.add(new ErrorSemantico("SEM-103", "No existe una tabla con ese nombre",
                    nombreTabla, linea, columna));
        } else if (!tabla.tieneColumna(nombreColumna)) {
            nuevos.add(new ErrorSemantico("SEM-104", "La tabla no tiene una columna con ese nombre",
                    tabla.getNombre() + "." + nombreColumna, linea, columna));
        } else if (tabla.cantidadColumnas() == 1) {
            //no dejo que una tabla se quede sin columnas
            nuevos.add(new ErrorSemantico("SEM-105", "No se puede eliminar la unica columna de la tabla",
                    tabla.getNombre() + "." + nombreColumna, linea, columna));
        } else {
            catalogo.eliminarColumna(nombreTabla, nombreColumna);
        }
        return nuevos;
    }

    //le avisa a la tabla de simbolos de la fase 1 que el nombre quedo declarado
    private void marcar(String nombre, CategoriaSimbolo categoria, TipoToken tipo) {
        if (tablaSimbolos != null) {
            tablaSimbolos.marcarDeclarado(nombre, categoria, tipo);
        }
    }

    public List<ErrorSemantico> getErrores() {
        return new ArrayList<>(errores);
    }

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }

    public CatalogoEsquema getCatalogo() {
        return catalogo;
    }
}
