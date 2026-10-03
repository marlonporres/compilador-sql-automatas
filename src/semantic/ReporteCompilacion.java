package semantic;

import java.util.List;

/** Presentacion comun y estable de los diagnosticos del compilador. */
public final class ReporteCompilacion {
    private ReporteCompilacion() {
    }

    public static String generar(TablaErrores tabla) {
        if (tabla == null || tabla.estaVacia()) {
            return "Sin errores.";
        }
        return generar(tabla.getErrores());
    }

    public static String generar(List<ErrorCompilacion> errores) {
        if (errores == null || errores.isEmpty()) {
            return "Sin errores.";
        }
        StringBuilder salida = new StringBuilder();
        for (ErrorCompilacion error : errores) {
            salida.append(error).append(System.lineSeparator());
        }
        return salida.toString().stripTrailing();
    }

    public static void imprimir(TablaErrores tabla) {
        System.out.println(generar(tabla));
    }
}
