package symbol_table;

import java.util.ArrayList;
import java.util.List;

import lexer.TipoToken;
import lexer.Token;

/* Prueba de la tabla de simbolos sola, sin el scanner.
 * Los tokens los armo a mano como si vinieran del analizador.
 *
 * javac -encoding UTF-8 -d out src/lexer/TipoToken.java src/lexer/Token.java src/symbol_table/*.java
 * java -cp out symbol_table.PruebaTablaSimbolos
 */
public class PruebaTablaSimbolos {

    private static int total = 0;
    private static int fallidas = 0;

    public static void main(String[] args) {
        System.out.println("PRUEBAS DE LA TABLA DE SIMBOLOS");
        System.out.println();

        tablaVacia();
        insercion();
        duplicados();
        busqueda();
        apariciones();
        soloIdentificadores();
        listaDeTokens();
        actualizaciones();
        eliminarYLimpiar();
        casosNulos();

        System.out.println();
        ejemploDeSalida();

        System.out.println();
        System.out.println("Pruebas: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) {
            System.exit(1);
        }
    }

    private static void tablaVacia() {
        TablaSimbolos tabla = new TablaSimbolos();
        probar("tabla nueva vacia", tabla.estaVacia());
        probar("tamano 0", tabla.tamano() == 0);
    }

    private static void insercion() {
        TablaSimbolos tabla = new TablaSimbolos();
        probar("inserta identificador nuevo", tabla.insertar(id("USUARIOS", 1, 14)));
        probar("tamano 1", tabla.tamano() == 1);
        probar("ya no esta vacia", !tabla.estaVacia());
    }

    private static void duplicados() {
        TablaSimbolos tabla = new TablaSimbolos();
        tabla.insertar(id("USUARIOS", 1, 14));
        probar("no repite el mismo identificador", !tabla.insertar(id("USUARIOS", 5, 20)));
        probar("sigue habiendo uno solo", tabla.tamano() == 1);

        TablaSimbolos otra = new TablaSimbolos();
        otra.insertar(id("Usuarios", 1, 14));
        probar("usuarios y USUARIOS son el mismo", !otra.insertar(id("USUARIOS", 3, 8)));
        probar("no se duplico", otra.tamano() == 1);
    }

    private static void busqueda() {
        TablaSimbolos tabla = new TablaSimbolos();
        tabla.insertar(id("CORREO", 2, 5));

        probar("existe lo encuentra", tabla.existe("CORREO"));
        probar("existe sin importar mayusculas", tabla.existe("correo"));
        probar("existe false si no esta", !tabla.existe("TELEFONO"));
        probar("buscar lo devuelve", tabla.buscar("correo") != null);
        probar("guarda el lexema original", "CORREO".equals(tabla.buscar("correo").getNombre()));
        probar("buscar null si no esta", tabla.buscar("TELEFONO") == null);
    }

    private static void apariciones() {
        TablaSimbolos tabla = new TablaSimbolos();
        tabla.registrar(id("USUARIOS", 1, 14));
        tabla.registrar(id("usuarios", 4, 10));
        tabla.registrar(id("USUARIOS", 7, 3));

        probar("registrar no duplica", tabla.tamano() == 1);
        probar("cuenta 3 apariciones", tabla.buscar("USUARIOS").getApariciones() == 3);
        probar("guarda la primera linea", tabla.buscar("USUARIOS").getLinea() == 1);
        probar("guarda la primera columna", tabla.buscar("USUARIOS").getColumna() == 14);
    }

    private static void soloIdentificadores() {
        TablaSimbolos tabla = new TablaSimbolos();

        probar("no mete palabras reservadas",
                !tabla.insertar(new Token(TipoToken.SELECCIONAR, "SELECCIONAR", 1, 1)));
        probar("no mete enteros",
                !tabla.insertar(new Token(TipoToken.LITERAL_ENTERO, "42", 1, 20)));
        probar("no mete cadenas",
                !tabla.insertar(new Token(TipoToken.LITERAL_CADENA, "hola", 1, 30)));
        probar("no mete tipos de dato",
                !tabla.insertar(new Token(TipoToken.TIPO_ENTERO, "ENTERO", 1, 40)));
        probar("no mete errores",
                !tabla.insertar(new Token(TipoToken.ERROR, "#", 1, 50)));
        probar("la tabla quedo vacia", tabla.estaVacia());
    }

    private static void listaDeTokens() {
        //CREAR TABLA usuarios (id ENTERO, correo TEXTO);
        List<Token> tokens = new ArrayList<>();
        tokens.add(new Token(TipoToken.CREAR, "CREAR", 1, 1));
        tokens.add(new Token(TipoToken.TABLA, "TABLA", 1, 7));
        tokens.add(id("usuarios", 1, 13));
        tokens.add(new Token(TipoToken.PARENTESIS_IZQ, "(", 1, 22));
        tokens.add(id("id", 2, 5));
        tokens.add(new Token(TipoToken.TIPO_ENTERO, "ENTERO", 2, 8));
        tokens.add(new Token(TipoToken.COMA, ",", 2, 14));
        tokens.add(id("correo", 3, 5));
        tokens.add(new Token(TipoToken.TIPO_TEXTO, "TEXTO", 3, 12));
        tokens.add(new Token(TipoToken.PUNTO_Y_COMA, ";", 4, 2));
        tokens.add(new Token(TipoToken.FIN_DE_ARCHIVO, "EOF", 4, 3));

        TablaSimbolos tabla = new TablaSimbolos();
        probar("saca 3 simbolos de la lista", tabla.registrarTodos(tokens) == 3);
        probar("quedan 3 en la tabla", tabla.tamano() == 3);

        List<Simbolo> todos = tabla.obtenerTodos();
        probar("respeta el orden de aparicion",
                todos.get(0).getNombre().equals("usuarios")
                        && todos.get(1).getNombre().equals("id")
                        && todos.get(2).getNombre().equals("correo"));
        probar("los id van de 1 en 1",
                todos.get(0).getId() == 1 && todos.get(1).getId() == 2 && todos.get(2).getId() == 3);
    }

    private static void actualizaciones() {
        TablaSimbolos tabla = new TablaSimbolos();
        tabla.insertar(id("usuarios", 1, 13));
        tabla.insertar(id("edad", 2, 5));

        probar("empieza en DESCONOCIDA",
                tabla.buscar("usuarios").getCategoria() == CategoriaSimbolo.DESCONOCIDA);
        probar("empieza en DESCONOCIDO",
                Simbolo.TIPO_DATO_DESCONOCIDO.equals(tabla.buscar("usuarios").getTipoDato()));
        probar("cambia la categoria",
                tabla.actualizarCategoria("usuarios", CategoriaSimbolo.TABLA));
        probar("quedo en TABLA",
                tabla.buscar("usuarios").getCategoria() == CategoriaSimbolo.TABLA);
        probar("cambia el tipo de dato",
                tabla.actualizarTipoDato("edad", TipoToken.TIPO_ENTERO));
        probar("TIPO_ENTERO se guarda como ENTERO",
                "ENTERO".equals(tabla.buscar("edad").getTipoDato()));
        probar("no actualiza lo que no existe",
                !tabla.actualizarTipoDato("telefono", TipoToken.TIPO_TEXTO));
    }

    private static void eliminarYLimpiar() {
        TablaSimbolos tabla = new TablaSimbolos();
        tabla.insertar(id("temporal", 1, 1));
        probar("elimina", tabla.eliminar("TEMPORAL"));
        probar("ya no existe", !tabla.existe("temporal"));
        probar("eliminar lo que no esta da false", !tabla.eliminar("temporal"));

        tabla.insertar(id("a", 1, 1));
        tabla.insertar(id("b", 1, 3));
        tabla.limpiar();
        probar("limpiar deja vacia", tabla.estaVacia());
        tabla.insertar(id("c", 1, 1));
        probar("los id vuelven a empezar en 1", tabla.buscar("c").getId() == 1);
    }

    private static void casosNulos() {
        TablaSimbolos tabla = new TablaSimbolos();

        probar("insertar simbolo null", !tabla.insertar((Simbolo) null));
        probar("insertar token null", !tabla.insertar((Token) null));
        probar("registrar null", tabla.registrar(null) == null);
        probar("buscar null", tabla.buscar(null) == null);
        probar("existe null", !tabla.existe(null));
        probar("eliminar null", !tabla.eliminar(null));
        probar("registrarTodos null", tabla.registrarTodos(null) == 0);
        probar("nada de eso cambio la tabla", tabla.estaVacia());

        boolean lanzo = false;
        try {
            new Simbolo(1, "", TipoToken.IDENTIFICADOR, 1, 1);
        } catch (IllegalArgumentException e) {
            lanzo = true;
        }
        probar("nombre vacio lanza excepcion", lanzo);
    }

    /* Simula lo que saldria al analizar:
     * CREAR TABLA usuarios (id ENTERO, correo TEXTO, edad ENTERO);
     * SELECCIONAR correo DESDE usuarios DONDE edad > 18;
     */
    private static void ejemploDeSalida() {
        TablaSimbolos tabla = new TablaSimbolos();
        tabla.registrar(id("usuarios", 1, 13));
        tabla.registrar(id("id", 1, 23));
        tabla.registrar(id("correo", 1, 34));
        tabla.registrar(id("edad", 1, 48));
        tabla.registrar(id("correo", 2, 13));
        tabla.registrar(id("usuarios", 2, 26));
        tabla.registrar(id("edad", 2, 41));

        tabla.actualizarCategoria("usuarios", CategoriaSimbolo.TABLA);
        tabla.actualizarCategoria("id", CategoriaSimbolo.COLUMNA);
        tabla.actualizarCategoria("correo", CategoriaSimbolo.COLUMNA);
        tabla.actualizarCategoria("edad", CategoriaSimbolo.COLUMNA);
        tabla.actualizarTipoDato("id", TipoToken.TIPO_ENTERO);
        tabla.actualizarTipoDato("correo", TipoToken.TIPO_TEXTO);
        tabla.actualizarTipoDato("edad", TipoToken.TIPO_ENTERO);

        tabla.imprimir();
    }

    private static Token id(String lexema, int linea, int columna) {
        return new Token(TipoToken.IDENTIFICADOR, lexema, linea, columna);
    }

    private static void probar(String descripcion, boolean condicion) {
        total++;
        if (condicion) {
            System.out.println("  ok    " + descripcion);
        } else {
            fallidas++;
            System.out.println("  FALLA " + descripcion);
        }
    }
}
