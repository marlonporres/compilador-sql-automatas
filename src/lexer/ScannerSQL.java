package lexer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/** Scanner manual de Fase 1: produce tokens y posiciones, sin interpretar sentencias. */
public class ScannerSQL {
    /* Catalogo de palabras reservadas -> token concreto. Usar Map evita
     * dependencias peligrosas con TipoToken.valueOf() cuando la palabra
     * no existe en el enum.
     */
    private static final Map<String, TipoToken> PALABRAS_RESERVADAS = crearCatalogo();

    private static Map<String, TipoToken> crearCatalogo() {
        Map<String, TipoToken> catalogo = new HashMap<>();

        // DDL
        catalogo.put("CREAR", TipoToken.CREAR);
        catalogo.put("TABLA", TipoToken.TABLA);
        catalogo.put("ALTERAR", TipoToken.ALTERAR);
        catalogo.put("ELIMINAR", TipoToken.ELIMINAR);
        catalogo.put("TRUNCAR", TipoToken.TRUNCAR);
        catalogo.put("RENOMBRAR", TipoToken.RENOMBRAR);

        // DML
        catalogo.put("INSERTAR", TipoToken.INSERTAR);
        catalogo.put("SELECCIONAR", TipoToken.SELECCIONAR);
        catalogo.put("ACTUALIZAR", TipoToken.ACTUALIZAR);
        catalogo.put("DESDE", TipoToken.DESDE);
        catalogo.put("DONDE", TipoToken.DONDE);
        catalogo.put("EN", TipoToken.EN);
        catalogo.put("VALORES", TipoToken.VALORES);
        catalogo.put("ASIGNAR", TipoToken.ASIGNAR);
        catalogo.put("BORRAR", TipoToken.BORRAR);

        // Tipos de dato
        catalogo.put("ENTERO", TipoToken.TIPO_ENTERO);
        catalogo.put("TEXTO", TipoToken.TIPO_TEXTO);
        catalogo.put("FECHA", TipoToken.TIPO_FECHA);
        catalogo.put("DECIMAL", TipoToken.TIPO_DECIMAL);
        catalogo.put("BOOLEANO", TipoToken.TIPO_BOOLEANO);

        // Operadores logicos
        catalogo.put("Y", TipoToken.OP_Y);
        catalogo.put("O", TipoToken.OP_O);
        catalogo.put("NO", TipoToken.OP_NO);

        return Collections.unmodifiableMap(catalogo);
    }

    private final List<Token> tokens = new ArrayList<>();

    public void analizar(String entrada) {
        tokens.clear();
        int fila = 1;
        int columna = 1;
        // La marca UTF-8 que algunos editores conservan no es parte del SQL
        // ni ocupa una columna visible. En otras posiciones sigue siendo error.
        int i = entrada.startsWith("\uFEFF") ? 1 : 0;

        while (i < entrada.length()) {
            char ch = entrada.charAt(i);

            // Saltar espacios y saltos de línea
            if (Character.isWhitespace(ch)) {
                int salto = longitudSalto(entrada, i);
                if (salto > 0) {
                    fila++;
                    columna = 1;
                } else {
                    columna++;
                }
                i += salto > 0 ? salto : 1;
                continue;
            }

            // Comentarios de bloque /* ... */
            if (ch == '/' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '*') {
                int inicio = i;
                int filaInicio = fila;
                int colInicio = columna;
                i += 2;
                columna += 2;
                while (i < entrada.length() && !(entrada.charAt(i) == '*' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '/')) {
                    int salto = longitudSalto(entrada, i);
                    if (salto > 0) {
                        fila++;
                        columna = 1;
                    } else {
                        columna++;
                    }
                    i += salto > 0 ? salto : 1;
                }
                if (i < entrada.length()) {
                    i += 2; // cerrar comentario
                    columna += 2;
                } else {
                    tokens.add(new Token(TipoToken.ERROR, entrada.substring(inicio), filaInicio, colInicio));
                }
                continue;
            }

            // Comentario en linea -- ...
            if (ch == '-' && i + 1 < entrada.length() && entrada.charAt(i + 1) == '-') {
                while (i < entrada.length() && longitudSalto(entrada, i) == 0) {
                    i++;
                    columna++;
                }
                continue;
            }

            // Identificadores o palabras reservadas
            if (Character.isLetter(ch) || ch == '_') {
                StringBuilder lexema = new StringBuilder();
                int colInicio = columna;
                while (i < entrada.length() && (Character.isLetter(entrada.charAt(i))
                        || esDigito(entrada.charAt(i)) || entrada.charAt(i) == '_')) {
                    lexema.append(entrada.charAt(i));
                    i++;
                    columna++;
                }
                String original = lexema.toString();
                String palabra = original.toUpperCase(Locale.ROOT);
                TipoToken tipo = PALABRAS_RESERVADAS.getOrDefault(palabra, TipoToken.IDENTIFICADOR);
                // Reservadas en mayúsculas, identificadores tal como fueron escritos
                tokens.add(new Token(tipo, tipo == TipoToken.IDENTIFICADOR ? original : palabra, fila, colInicio));
                continue;
            }

            // Números enteros y decimales (validación de puntos)
            if (esDigito(ch)) {
                StringBuilder numero = new StringBuilder();
                int colInicio = columna;
                int puntos = 0;
                while (i < entrada.length() && (esDigito(entrada.charAt(i)) || entrada.charAt(i) == '.')) {
                    if (entrada.charAt(i) == '.') puntos++;
                    numero.append(entrada.charAt(i));
                    i++;
                    columna++;
                }
                TipoToken tipo;
                if (puntos > 1 || numero.charAt(numero.length() - 1) == '.') {
                    tipo = TipoToken.ERROR;
                } else if (puntos == 1) {
                    tipo = TipoToken.LITERAL_DECIMAL;
                } else {
                    tipo = TipoToken.LITERAL_ENTERO;
                }
                tokens.add(new Token(tipo, numero.toString(), fila, colInicio));
                continue;
            }

            // Literales de cadena (sin incluir apóstrofes en el lexema)
            if (ch == '\'') {
                StringBuilder cadena = new StringBuilder();
                int filaInicio = fila;
                int colInicio = columna;
                i++; // Saltar el primer apóstrofe
                columna++;
                while (i < entrada.length()) {
                    if (entrada.charAt(i) == '\'') {
                        if (i + 1 < entrada.length() && entrada.charAt(i + 1) == '\'') {
                            cadena.append('\'');
                            i += 2;
                            columna += 2;
                            continue;
                        }
                        break;
                    }
                    int salto = longitudSalto(entrada, i);
                    if (salto > 0) {
                        cadena.append(entrada, i, i + salto);
                        i += salto;
                        fila++;
                        columna = 1;
                    } else {
                        cadena.append(entrada.charAt(i));
                        i++;
                        columna++;
                    }
                }
                if (i < entrada.length() && entrada.charAt(i) == '\'') {
                    i++; // Saltar el cierre
                    columna++;
                    tokens.add(new Token(TipoToken.LITERAL_CADENA, cadena.toString(), filaInicio, colInicio));
                } else {
                    tokens.add(new Token(TipoToken.ERROR, cadena.toString(), filaInicio, colInicio));
                }
                continue;
            }

            // Operadores y delimitadores
            int colInicio = columna;
            switch (ch) {
                case '=':
                    if (i + 1 < entrada.length() && entrada.charAt(i + 1) == '=') {
                        // El catálogo define únicamente '='; no aceptar '==' como igualdad.
                        tokens.add(new Token(TipoToken.ERROR, "==", fila, colInicio));
                        i += 2; columna += 2;
                    } else {
                        tokens.add(new Token(TipoToken.OP_IGUAL, "=", fila, colInicio));
                        i++; columna++;
                    }
                    continue;
                case '!':
                    if (i + 1 < entrada.length() && entrada.charAt(i + 1) == '=') {
                        tokens.add(new Token(TipoToken.OP_DIFERENTE, "!=", fila, colInicio));
                        i += 2; columna += 2;
                    } else {
                        tokens.add(new Token(TipoToken.ERROR, "!", fila, colInicio));
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

    // CRLF cuenta como un solo salto; se conservan los caracteres originales del lexema.
    private static int longitudSalto(String entrada, int i) {
        char ch = entrada.charAt(i);
        if (ch == '\r') {
            return i + 1 < entrada.length() && entrada.charAt(i + 1) == '\n' ? 2 : 1;
        }
        return ch == '\n' ? 1 : 0;
    }

    private static boolean esDigito(char ch) {
        return ch >= '0' && ch <= '9';
    }

    // Main recibe una copia: recorrer o modificar esa lista no altera el scanner.
    public List<Token> getTokens() {
        return new ArrayList<>(tokens);
    }

    public void mostrarTokens() {
        for (Token t : tokens) {
            System.out.println(t);
        }
    }

    public static void main(String[] args) throws IOException {
        String entrada;

        if (args.length > 0) {
            entrada = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        } else {
            try (Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8)) {
                entrada = sc.useDelimiter("\\A").hasNext() ? sc.next() : "";
            }
        }

        ScannerSQL scanner = new ScannerSQL();
        scanner.analizar(entrada);
        scanner.mostrarTokens();
    }
}

