import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.List;

import lexer.ScannerSQL;
import lexer.Token;
import symbol_table.TablaSimbolos;
import compiler.CompiladorSQL;
import compiler.ResultadoCompilacion;
import errors.FaseError;

/** Sin argumentos abre la aplicación; con un archivo conserva el análisis por consola. */
public class Main {

    public static void main(String[] args) {

        if (args.length == 0 || (args.length == 1 && args[0].equals("--verificar-arranque"))) {
            ui.AplicacionGUI.main(args);
            return;
        }

        System.out.println("==============================================");
        System.out.println("       COMPILADOR SQL");
        System.out.println("       ANALISIS DEL LENGUAJE SQL EN ESPANOL");
        System.out.println("==============================================");
        System.out.println();

        if (args.length == 1 && (args[0].equals("--ayuda") || args[0].equals("--help"))) {
            imprimirUso();
            return;
        }

        // Verificar que se haya indicado un archivo
        boolean fase2 = (args.length == 2 || args.length == 3)
                && args[1].equals("--fase2")
                && (args.length != 3 || args[2].equals("--resumen"));
        if (args.length != 1 && !fase2) {
            imprimirUso();
            System.exit(2);
            return;
        }

        String rutaArchivo = args[0];

        try {
            // ==========================================
            // 1. Leer archivo SQL
            // ==========================================
            String entrada = new String(
                    Files.readAllBytes(Paths.get(rutaArchivo)),
                    StandardCharsets.UTF_8
            );

            System.out.println("Archivo analizado: " + rutaArchivo);
            System.out.println();
            if (fase2) {
                ResultadoCompilacion resultado = new CompiladorSQL().analizar(entrada);
                imprimirFase2(resultado, args.length == 3);
                if (!resultado.esValido()) System.exit(1);
                return;
            }

            // ==========================================
            // 2. Crear y ejecutar ScannerSQL
            // ==========================================
            ScannerSQL scanner = new ScannerSQL();
            scanner.analizar(entrada);

            // Obtener los tokens generados
            List<Token> tokens = scanner.getTokens();

            // ==========================================
            // 3. Mostrar tokens
            // ==========================================
            System.out.println("--------------- TOKENS ---------------");

            int errores = 0;

            for (Token token : tokens) {
                System.out.println(token);

                if (token.getTipo() == lexer.TipoToken.ERROR) {
                    errores++;
                }
            }

            System.out.println("--------------------------------------");
            System.out.println("Total de tokens: " + tokens.size());
            System.out.println("Errores lexicos: " + errores);
            System.out.println();

            // ==========================================
            // 4. Construir tabla de simbolos
            // ==========================================
            TablaSimbolos tabla = new TablaSimbolos();

            // Se registran solo identificadores; errores, reservadas y literales quedan fuera.
            tabla.registrarTodos(tokens);

            // ==========================================
            // 5. Mostrar tabla de simbolos
            // ==========================================
            System.out.println("---------- TABLA DE SIMBOLOS ----------");
            tabla.imprimir();

            // ==========================================
            // 6. Resultado final
            // ==========================================
            System.out.println();
            System.out.println("==============================================");

            if (errores == 0) {
                System.out.println("RESULTADO: ANALISIS LEXICO COMPLETADO");
                System.out.println("No se encontraron errores lexicos.");
            } else {
                System.out.println("RESULTADO: SE ENCONTRARON ERRORES LEXICOS");
                System.out.println("Cantidad de errores: " + errores);
            }

            System.out.println("==============================================");
            if (errores > 0) {
                System.exit(1);
            }

        } catch (IOException | InvalidPathException e) {

            System.out.println("ERROR AL LEER EL ARCHIVO:");
            System.out.println(e.getMessage());
            System.exit(2);

        } catch (Exception e) {

            System.out.println("ERROR DURANTE EL ANALISIS:");
            System.out.println(e.getMessage());
            System.exit(2);
        }
    }

    private static void imprimirUso() {
        System.out.println("Uso:");
        System.out.println("java -cp \"out;lib/*\" Main  (abre la interfaz grafica)");
        System.out.println("java -cp out Main <archivo.sql>");
        System.out.println("java -cp out Main <archivo.sql> --fase2 [--resumen]");
        System.out.println();
        System.out.println("Ejemplo:");
        System.out.println("java -cp out Main tests/test_ddl.sql");
        System.out.println("En VS Code abre SQLStudio.code-workspace y usa F5.");
    }

    private static void imprimirFase2(ResultadoCompilacion resultado, boolean resumen) {
        System.out.println("======= FASE 2: SINTAXIS Y SEMANTICA =======");
        if (!resumen) {
            System.out.println("--------------- TOKENS ---------------");
            resultado.tokens().forEach(System.out::println);
            System.out.println("----- IDENTIFICADORES OBSERVADOS POR EL LEXICO -----");
            resultado.simbolosLexicos().imprimir();
        }
        System.out.println("---------- SENTENCIAS RECONOCIDAS ----------");
        resultado.sentencias().forEach(s -> System.out.printf("L:%d C:%d | %s%n",
                s.getLinea(), s.getColumna(), s));
        System.out.println("---------- TABLA DE SIMBOLOS SEMANTICA ----------");
        resultado.catalogo().imprimir();
        System.out.println("---------- TABLA DE ERRORES ----------");
        resultado.errores().imprimir();
        System.out.printf("Errores lexicos: %d | sintacticos: %d | semanticos: %d%n",
                resultado.errores().contar(FaseError.LEXICA),
                resultado.errores().contar(FaseError.SINTACTICA),
                resultado.errores().contar(FaseError.SEMANTICA));
        System.out.printf("Sentencias reconocidas: %d | validas semanticamente: %d%n",
                resultado.sentencias().size(), resultado.sentenciasValidas());
        System.out.println(resultado.esValido()
                ? "RESULTADO: ANALISIS COMPLETO SIN ERRORES"
                : "RESULTADO: SE ENCONTRARON ERRORES");
        System.out.println("Fase 2 valida scripts y esquemas; no ejecuta consultas ni almacena filas.");
    }
}
