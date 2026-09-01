// ...existing code...
package lexer;

import java.io.*;
import java.util.*;

public class ScannerSQL {
    private static final Set<String> PALABRAS_RESERVADAS = new HashSet<>(Arrays.asList(
        "SELECCIONAR", "INSERTAR", "ACTUALIZAR", "BORRAR", "CREAR", "ALTERAR", "ELIMINAR",
        "TRUNCAR", "RENOMBRAR", "TABLA", "EN", "VALORES", "DESDE", "DONDE",
        "ASIGNAR", "Y", "O", "NO", "NULO", "PRIMARIA", "LLAVE", "FORANEA"
    ));

    private static final Set<Character> SIMBOLOS = new HashSet<>(Arrays.asList(
        '(', ')', ',', ';', '=', '*', '<', '>', '+', '-', '/', '.', ':'
    ));

    private final List<Token> token = new ArrayList<>();
    private TipoToken tipo;

    public void Analizar(String linea) throws IOException {
        token.clear();
        StringBuilder lexema = new StringBuilder();
        int fila = 1;
        int columna = 1;
        boolean comentario = false;

        for (int i = 0; i < linea.length(); i++) {
            char ch = linea.charAt(i);

            if (comentario) {
                if (ch == '*' && i + 1 < linea.length() && linea.charAt(i + 1) == '/') {
                    comentario = false;
                    i++;
                }
                continue;
            }

            if (ch == '/' && i + 1 < linea.length() && linea.charAt(i + 1) == '*') {
                comentario = true;
                i++;
                continue;
            }

            if (Character.isWhitespace(ch)) {
                if (!lexema.isEmpty()) {
                    EstabToken(lexema.toString(), fila, columna - lexema.length());
                    lexema.setLength(0);
                }

                if (ch == '\n') {
                    fila++;
                    columna = 1;
                } else {
                    columna++;
                }
                continue;
            }

            if (SIMBOLOS.contains(ch)) {
                if (!lexema.isEmpty()) {
                    EstabToken(lexema.toString(), fila, columna - lexema.length());
                    lexema.setLength(0);
                }
                EstabToken(String.valueOf(ch), fila, columna);
                columna++;
                continue;
            }

            lexema.append(ch);
            columna++;
        }

        if (!lexema.isEmpty()) {
            EstabToken(lexema.toString(), fila, columna - lexema.length());
        }
    }

    public void EstabToken(String lexema, int linea, int columna) {
        if (lexema.isEmpty()) {
            return;
        }

        if (PALABRAS_RESERVADAS.contains(lexema.toUpperCase())) {
            tipo = TipoToken.valueOf(lexema.toUpperCase());
        } else if (lexema.matches("\\d+")) {
            tipo = TipoToken.LITERAL_ENTERO;
        } else if (lexema.matches("\\d+\\.\\d+")) {
            tipo = TipoToken.LITERAL_DECIMAL;
        } else if (lexema.matches("'[^']*'")) {
            tipo = TipoToken.LITERAL_CADENA;
        } else if (lexema.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            tipo = TipoToken.IDENTIFICADOR;
        } else {
            tipo = TipoToken.ERROR;
        }

        token.add(new Token(tipo, lexema, linea, columna));
    }

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        String linea = sc.nextLine();
        ScannerSQL scanner = new ScannerSQL();
        scanner.Analizar(linea);
    }
}