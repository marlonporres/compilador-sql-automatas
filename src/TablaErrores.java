package semantic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lexer.TipoToken;
import lexer.Token;

/** Colección ordenada de diagnósticos producidos durante la compilación. */
public final class TablaErrores {
    private final List<ErrorCompilacion> errores = new ArrayList<>();

    public void agregar(ErrorCompilacion error) {
        if (error != null) {
            errores.add(error);
        }
    }

    public void agregar(String fase, String codigo, String mensaje,
            String elemento, int linea, int columna) {
        agregar(new ErrorCompilacion(fase, codigo, mensaje, elemento, linea, columna));
    }

    public void agregarTodos(Collection<ErrorCompilacion> nuevos) {
        if (nuevos != null) {
            nuevos.forEach(this::agregar);
        }
    }

    /** Incorpora los tokens ERROR del scanner al formato común de diagnósticos. */
    public void agregarErroresLexicos(Collection<Token> tokens) {
        if (tokens == null) return;
        for (Token token : tokens) {
            if (token != null && token.getTipo() == TipoToken.ERROR) {
                agregar("LEXICO", "LEX-001", "Lexema inválido", token.getLexema(),
                        token.getlinea(), token.getcolumna());
            }
        }
    }

    public List<ErrorCompilacion> obtenerTodos() {
        return List.copyOf(errores);
    }

    public int cantidad() { return errores.size(); }
    public boolean estaVacia() { return errores.isEmpty(); }
    public void limpiar() { errores.clear(); }

    public String formatear() {
        if (errores.isEmpty()) {
            return "No se encontraron errores.";
        }
        StringBuilder sb = new StringBuilder();
        for (ErrorCompilacion error : errores) {
            sb.append(error).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
