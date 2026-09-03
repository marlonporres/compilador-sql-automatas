package lexer;

import java.util.*;

public class ScannerSQL {
    private static final Set<String> PALABRAS_RESERVADAS = new HashSet<>(Arrays.asList(
        "SELECCIONAR", "INSERTAR", "ACTUALIZAR", "BORRAR", "CREAR", "ALTERAR", "ELIMINAR",
        "TRUNCAR", "RENOMBRAR", "TABLA", "EN", "VALORES", "DESDE", "DONDE",
        "ASIGNAR", "Y", "O", "NO", "NULO", "PRIMARIA", "LLAVE", "FORANEA"
    ));

    private final List<Token> tokens = new ArrayList<>();

    public void analizar(String entrada) {
        tokens.clear();
        int fila = 1;
        int columna = 1;
        int i = 0;

        while (i < entrada.length()) {
            char ch = entrada.charAt(i);

            // Saltar espacios y saltos de línea
            if (Character.isWhitespace(ch)) {
                if (ch == '\n') {
                    fila++;
                    columna = 1;
                } else {
                    columna++;
                }
                i++;
                continue;
            }

            // Comentarios de bloque /* ... */
            if (ch == '/' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '*') {
                i += 2;
                while (i < entrada.length() && !(entrada.charAt(i) == '*' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '/')) {
                    if (entrada.charAt(i) == '\n') {
                        fila++;
                        columna = 1;
                    } else {
                        columna++;
                    }
                    i++;
                }
                i += 2; // cerrar comentario
                continue;
            }

            // Identificadores o palabras reservadas
            if (Character.isLetter(ch) || ch == '_') {
                StringBuilder lexema = new StringBuilder();
                int colInicio = columna;
                while (i < entrada.length() && (Character.isLetterOrDigit(entrada.charAt(i)) || entrada.charAt(i) == '_')) {
                    lexema.append(entrada.charAt(i));
                    i++;
                    columna++;
                }
                String palabra = lexema.toString().toUpperCase();
                TipoToken tipo = PALABRAS_RESERVADAS.contains(palabra) ? TipoToken.valueOf(palabra) : TipoToken.IDENTIFICADOR;
                tokens.add(new Token(tipo, palabra, fila, colInicio));
                continue;
            }

            // Números enteros y decimales
            if (Character.isDigit(ch)) {
                StringBuilder numero = new StringBuilder();
                int colInicio = columna;
                boolean esDecimal = false;
                while (i < entrada.length() && (Character.isDigit(entrada.charAt(i)) || entrada.charAt(i) == '.')) {
                    if (entrada.charAt(i) == '.') esDecimal = true;
                    numero.append(entrada.charAt(i));
                    i++;
                    columna++;
                }
                TipoToken tipo = esDecimal ? TipoToken.LITERAL_DECIMAL : TipoToken.LITERAL_ENTERO;
                tokens.add(new Token(tipo, numero.toString(), fila, colInicio));
                continue;
            }

            // Literales de cadena
            if (ch == '\'') {
                StringBuilder cadena = new StringBuilder();
                int colInicio = columna;
                cadena.append(ch);
                i++;
                columna++;
                while (i < entrada.length() && entrada.charAt(i) != '\'') {
                    cadena.append(entrada.charAt(i));
                    i++;
                    columna++;
                }
                if (i < entrada.length()) {
                    cadena.append('\'');
                    i++;
                    columna++;
                    tokens.add(new Token(TipoToken.LITERAL_CADENA, cadena.toString(), fila, colInicio));
                } else {
                    tokens.add(new Token(TipoToken.ERROR, cadena.toString(), fila, colInicio));
                }
                continue;
            }

            // Operadores y delimitadores
            int colInicio = columna;
            switch (ch) {
                case '=':
                    if (i + 1 < entrada.length() && entrada.charAt(i + 1) == '=') {
                        tokens.add(new Token(TipoToken.OP_IGUAL, "==", fila, colInicio));
                        i += 2; columna += 2;
                    } else {
                        tokens.add(new Token(TipoToken.OP_ASIGNACION, "=", fila, colInicio));
                        i++; columna++;
                    }
                    continue;
                case '<':
                    if (i + 1 < entrada.length() && entrada.charAt(i + 1) == '=') {
                        tokens.add(new Token(TipoToken.OP_MENOR_IGUAL, "<=", fila, colInicio));
                        i += 2; columna += 2;
                    } else if (i + 1 < entrada.length() && entrada.charAt(i + 1) == '>') {
                        tokens.add(new Token(TipoToken.OP_DIFERENTE, "<>", fila, colInicio));
                        i += 2; columna += 2;
                    } else {
                        tokens.add(new Token(TipoToken.OP_MENOR, "<", fila, colInicio));
                        i++; columna++;
                    }
                    continue;
                case '>':
                    if (i + 1 < entrada.length() && entrada.charAt(i + 1) == '=') {
                        tokens.add(new Token(TipoToken.OP_MAYOR_IGUAL, ">=", fila, colInicio));
                        i += 2; columna += 2;
                    } else {
                        tokens.add(new Token(TipoToken.OP_MAYOR, ">", fila, colInicio));
                        i++; columna++;
                    }
                    continue;
                case ',':
                    tokens.add(new Token(TipoToken.COMA, ",", fila, colInicio)); break;
                case ';':
                    tokens.add(new Token(TipoToken.PUNTO_Y_COMA, ";", fila, colInicio)); break;
                case '(':
                    tokens.add(new Token(TipoToken.PARENTESIS_IZQ, "(", fila, colInicio)); break;
                case ')':
                    tokens.add(new Token(TipoToken.PARENTESIS_DER, ")", fila, colInicio)); break;
                case '*':
                    tokens.add(new Token(TipoToken.ASTERISCO, "*", fila, colInicio)); break;
                default:
                    tokens.add(new Token(TipoToken.ERROR, String.valueOf(ch), fila, colInicio)); break;
            }
            i++; columna++;
        }

        // Token de fin de archivo
        tokens.add(new Token(TipoToken.FIN_DE_ARCHIVO, "EOF", fila, columna));
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void mostrarTokens() {
        for (Token t : tokens) {
            System.out.println(t);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Ingrese su script SQL:");
        String entrada = sc.useDelimiter("\\A").next();
        ScannerSQL scanner = new ScannerSQL();
        scanner.analizar(entrada);
        scanner.mostrarTokens();
    }
}
