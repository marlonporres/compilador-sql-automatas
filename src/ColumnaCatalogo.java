package semantic;

import java.util.Locale;
import java.util.Objects;
import lexer.TipoToken;

/** Vista inmutable de una columna publicada por el catálogo semántico. */
public record ColumnaCatalogo(String nombre, TipoToken tipoDato) {
    public ColumnaCatalogo {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la columna no puede estar vacío");
        }
        if (tipoDato == null) {
            throw new IllegalArgumentException("El tipo de dato no puede ser nulo");
        }
        nombre = nombre.trim();
    }

    public String clave() {
        return nombre.toUpperCase(Locale.ROOT);
    }
}
