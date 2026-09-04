package symbol_table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lexer.TipoToken;
import lexer.Token;

/* Tabla de simbolos del compilador.
 * Uso LinkedHashMap: el hash me da la busqueda rapida para detectar los
 * duplicados y el linked me mantiene el orden en que fueron apareciendo.
 * La llave es el nombre en mayusculas.
 */
public class TablaSimbolos {

    private final Map<String, Simbolo> simbolos = new LinkedHashMap<>();
    private int contadorId = 0;

    //Inserta un simbolo ya armado. Devuelve false si ya estaba.
    public boolean insertar(Simbolo simbolo) {
        if (simbolo == null || simbolos.containsKey(simbolo.getClave())) {
            return false;
        }
        simbolos.put(simbolo.getClave(), simbolo);
        contadorId++;
        return true;
    }

    //Inserta directo desde el token del scanner.
    public boolean insertar(Token token) {
        if (!esInsertable(token)) {
            return false;
        }
        String clave = Simbolo.normalizar(token.getLexema());
        if (simbolos.containsKey(clave)) {
            return false;
        }
        simbolos.put(clave, new Simbolo(contadorId + 1, token));
        contadorId++;
        return true;
    }

    /* Este es el que usa el analizador: si el identificador es nuevo lo mete,
     * y si ya estaba solo le suma una aparicion en vez de repetir la fila.
     */
    public Simbolo registrar(Token token) {
        if (!esInsertable(token)) {
            return null;
        }
        String clave = Simbolo.normalizar(token.getLexema());
        Simbolo existente = simbolos.get(clave);
        if (existente != null) {
            existente.incrementarApariciones();
            return existente;
        }
        Simbolo nuevo = new Simbolo(contadorId + 1, token);
        simbolos.put(clave, nuevo);
        contadorId++;
        return nuevo;
    }

    //Recorre toda la lista de tokens y devuelve cuantos simbolos nuevos salieron.
    public int registrarTodos(List<Token> tokens) {
        if (tokens == null) {
            return 0;
        }
        int nuevos = 0;
        for (Token token : tokens) {
            int antes = simbolos.size();
            registrar(token);
            if (simbolos.size() > antes) {
                nuevos++;
            }
        }
        return nuevos;
    }

    /* Solo entran los identificadores. Las palabras reservadas y los tipos ya
     * estan en el catalogo del scanner y los literales no son nombres.
     */
    private boolean esInsertable(Token token) {
        return token != null
                && token.getTipo() == TipoToken.IDENTIFICADOR
                && token.getLexema() != null
                && !token.getLexema().trim().isEmpty();
    }

    public Simbolo buscar(String nombre) {
        if (nombre == null) {
            return null;
        }
        return simbolos.get(Simbolo.normalizar(nombre));
    }

    public boolean existe(String nombre) {
        return buscar(nombre) != null;
    }

    public int tamano() {
        return simbolos.size();
    }

    public boolean estaVacia() {
        return simbolos.isEmpty();
    }

    public List<Simbolo> obtenerTodos() {
        return new ArrayList<>(simbolos.values());
    }

    public boolean eliminar(String nombre) {
        if (nombre == null) {
            return false;
        }
        return simbolos.remove(Simbolo.normalizar(nombre)) != null;
    }

    public boolean actualizarTipoDato(String nombre, String tipoDato) {
        Simbolo simbolo = buscar(nombre);
        if (simbolo == null) {
            return false;
        }
        simbolo.setTipoDato(tipoDato);
        return true;
    }

    public boolean actualizarTipoDato(String nombre, TipoToken tipoDeDato) {
        Simbolo simbolo = buscar(nombre);
        if (simbolo == null) {
            return false;
        }
        simbolo.setTipoDato(tipoDeDato);
        return true;
    }

    public boolean actualizarCategoria(String nombre, CategoriaSimbolo categoria) {
        Simbolo simbolo = buscar(nombre);
        if (simbolo == null) {
            return false;
        }
        simbolo.setCategoria(categoria);
        return true;
    }

    public void limpiar() {
        simbolos.clear();
        contadorId = 0;
    }

    private static final String SEPARADOR =
            "+-----+----------------------+---------------+--------------+--------------+-------+-------+-------------+";

    @Override
    public String toString() {
        String salto = System.lineSeparator();
        StringBuilder sb = new StringBuilder();

        sb.append(SEPARADOR).append(salto);
        sb.append(String.format("| %-3s | %-20s | %-13s | %-12s | %-12s | %-5s | %-5s | %-11s |",
                "ID", "NOMBRE", "TOKEN", "CATEGORIA", "TIPO DATO", "LINEA", "COL", "APARICION"));
        sb.append(salto).append(SEPARADOR).append(salto);

        if (simbolos.isEmpty()) {
            sb.append("  (tabla de simbolos vacia)").append(salto);
            sb.append(SEPARADOR);
            return sb.toString();
        }

        for (Simbolo s : simbolos.values()) {
            sb.append(String.format("| %-3d | %-20s | %-13s | %-12s | %-12s | %-5d | %-5d | %-11d |",
                    s.getId(), recortar(s.getNombre(), 20), s.getTipoToken(), s.getCategoria(),
                    recortar(s.getTipoDato(), 12), s.getLinea(), s.getColumna(), s.getApariciones()));
            sb.append(salto);
        }
        sb.append(SEPARADOR).append(salto);
        sb.append("Total de simbolos: ").append(simbolos.size());
        return sb.toString();
    }

    //Para que un nombre largo no dañe la alineacion de la tabla.
    private String recortar(String texto, int ancho) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= ancho ? texto : texto.substring(0, ancho - 3) + "...";
    }

    public void imprimir() {
        System.out.println(this);
    }
}
