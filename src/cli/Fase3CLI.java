package cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import execution.InformeEjecucion;
import execution.ResultadoComparacion;
import execution.ResultadoEjecucion;
import execution.ServicioEjecucion;

/** Verificación reproducible de la misma ejecución usada por la GUI. */
public final class Fase3CLI {
    private Fase3CLI() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Uso: cli.Fase3CLI archivo-espanol.sql [referencia.sql]");
            System.exit(2);
            return;
        }
        try {
            String fuente = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
            ServicioEjecucion servicio = new ServicioEjecucion();
            if (args.length == 2) {
                String referencia = Files.readString(Path.of(args[1]), StandardCharsets.UTF_8);
                ResultadoComparacion comparacion = servicio.comparar(fuente, referencia);
                imprimir(comparacion.compilador());
                comparacion.detalles().forEach(System.out::println);
                System.out.println(comparacion.coincide() ? "COMPARACION SQL: APROBADA" : "COMPARACION SQL: DIFERENTE");
                if (!comparacion.coincide()) System.exit(1);
            } else {
                InformeEjecucion informe = servicio.ejecutar(fuente);
                imprimir(informe);
                if (!informe.esExitoso()) System.exit(1);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("No se pudo completar la demostracion: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void imprimir(InformeEjecucion informe) {
        informe.analisis().errores().obtenerTodos().forEach(System.out::println);
        for (ResultadoEjecucion resultado : informe.resultados()) {
            System.out.printf("%02d | %s | L:%d C:%d | %s%n", resultado.numero(), resultado.comando(),
                    resultado.linea(), resultado.columna(), resultado.sql());
            if (resultado.esConsulta()) {
                System.out.println(resultado.columnas());
                resultado.filas().forEach(System.out::println);
            } else System.out.println("Filas afectadas: " + resultado.filasAfectadas());
            if (!resultado.esExitoso()) System.out.println("ERROR SQL: " + resultado.error());
        }
        System.out.printf("Sentencias ejecutadas: %d | Duracion: %d ms%n",
                informe.resultados().size(), informe.duracionMs());
    }
}
