package semantic;

import java.util.List;
import java.util.Locale;

/** Vista inmutable de una tabla y sus columnas para consumidores como Persona 4. */
public record TablaCatalogo(String nombre, List<ColumnaCatalogo> columnas) {
    public TablaCatalogo {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la tabla no puede estar vacío");
        }
        nombre = nombre.trim();
        columnas = List.copyOf(columnas == null ? List.of() : columnas);
    }

    public String clave() {
        return nombre.toUpperCase(Locale.ROOT);
    }

    public ColumnaCatalogo buscarColumna(String nombreColumna) {
        if (nombreColumna == null) return null;
        for (ColumnaCatalogo columna : columnas) {
            if (columna.clave().equals(nombreColumna.trim().toUpperCase(Locale.ROOT))) {
                return columna;
            }
        }
        return null;
    }
}
