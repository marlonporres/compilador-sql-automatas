package errors;

import java.util.ArrayList;
import java.util.List;

import lexer.Token;

/** Colección ordenada de errores para mostrar el resultado de la fase 2. */
public final class TablaErrores {

    private final List<Diagnostico> errores = new ArrayList<>();

    public void agregar(Diagnostico diagnostico) {
        if (diagnostico != null) {
            errores.add(diagnostico);
        }
    }

    public void agregar(FaseError fase, String codigo, String mensaje,
            String lexema, int linea, int columna) {
        agregar(new Diagnostico(fase, codigo, mensaje, lexema, linea, columna));
    }

    public void agregarLexico(Token token) {
        if (token != null) {
            agregar(FaseError.LEXICA, "LEX-001", "Token no reconocido",
                    token.getLexema(), token.getlinea(), token.getcolumna());
        }
    }

    public void agregarTodos(List<Diagnostico> diagnosticos) {
        if (diagnosticos != null) {
            diagnosticos.forEach(this::agregar);
        }
    }

    public List<Diagnostico> obtenerTodos() {
        return List.copyOf(errores);
    }

    public boolean estaVacia() {
        return errores.isEmpty();
    }

    public int tamano() {
        return errores.size();
    }

    public long contar(FaseError fase) {
        return errores.stream().filter(error -> error.getFase() == fase).count();
    }

    public void imprimir() {
        if (errores.isEmpty()) {
            System.out.println("(tabla de errores vacía)");
            return;
        }
        errores.forEach(error -> System.out.println(error));
    }
}
