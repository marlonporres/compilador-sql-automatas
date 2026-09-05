import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import lexer.ScannerSQL;
import lexer.Token;
import symbol_table.TablaSimbolos;

public class Main {

    public static void main(String[] args) {

        System.out.println("==============================================");
        System.out.println("       COMPILADOR SQL - FASE 1");
        System.out.println("       ANALIZADOR LEXICO");
        System.out.println("==============================================");
        System.out.println();

        // Verificar que se haya indicado un archivo
        if (args.length == 0) {
            System.out.println("Uso:");
            System.out.println("java -cp out Main <archivo.sql>");
            System.out.println();
            System.out.println("Ejemplo:");
            System.out.println("java -cp out Main tests/test_ddl.sql");
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

            // ==========================================
            // 2. Crear y ejecutar ScannerSQL
            // ==========================================
            ScannerSQL scanner = new ScannerSQL();
            scanner.Analizar(entrada);

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

        } catch (IOException e) {

            System.out.println("ERROR AL LEER EL ARCHIVO:");
            System.out.println(e.getMessage());

        } catch (Exception e) {

            System.out.println("ERROR DURANTE EL ANALISIS:");
            System.out.println(e.getMessage());
        }
    }
}