package semantic;

import java.util.ArrayList;
import java.util.List;

import lexer.Token;

/** Coleccion comun de diagnosticos de las fases del compilador. */
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

    /** Adapta un token ERROR del scanner al formato comun. */
    public void agregarLexico(Token token) {
        if (token != null) {
            agregar("LEXICO", "LEX-001", "Lexema no reconocido", token.getLexema(),
                    token.getlinea(), token.getcolumna());
        }
    }

    public void agregarTodos(List<ErrorCompilacion> nuevos) {
        if (nuevos != null) {
            for (ErrorCompilacion error : nuevos) {
                agregar(error);
            }
        }
    }

    public List<ErrorCompilacion> getErrores() {
        return new ArrayList<>(errores);
    }

    public int cantidad() {
        return errores.size();
    }

    public boolean estaVacia() {
        return errores.isEmpty();
    }

    public void limpiar() {
        errores.clear();
    }
}
