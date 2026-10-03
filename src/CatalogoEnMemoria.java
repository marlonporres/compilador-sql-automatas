package semantic;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Implementación pequeña para pruebas y demostraciones del contrato del catálogo. */
public final class CatalogoEnMemoria implements CatalogoSemantico {
    private final Map<String, TablaCatalogo> tablas = new LinkedHashMap<>();

    public void agregarTabla(TablaCatalogo tabla) {
        tablas.put(tabla.clave(), tabla);
    }

    @Override
    public TablaCatalogo buscarTabla(String nombre) {
        if (nombre == null) return null;
        return tablas.get(nombre.trim().toUpperCase(Locale.ROOT));
    }
}
