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
    //private TipoToken tipo;

    public void Analizar(String linea) throws IOException {
        token.clear();
        StringBuilder lexema = new StringBuilder();
        int fila = 1;
        int columna = 1;
        int i = 0;

        while (i<linea.length()){
            char ch = linea.charAt(i);
            //se salta espacios en blanco y tabulaciones
            if(Character.isWhitespace(ch)){
                if (ch == '\n') {
                    fila++;
                    columna = 1;
                } else {
                    columna++;
                }
                i++;
                continue;
            }

            //se salta comentarios en bloque
            if(ch == '/' && i+1 < linea.length() && linea.charAt(i+1) == '*'){
                i += 2; // Saltar '/' y '*'
                while (i < linea.length()&& (linea.charAt(i)=='*' && i+1 < linea.length() && linea.charAt(i+1)=='/')){
                    if(linea.charAt(i) == '\n') {
                        fila++;
                        columna = 1;
                    } else {
                        columna++;
                    }
                    i++;
                }
                i +=2; //saltar '*' y '/'
                continue;
            }
            //salta comentario en linea
            if(ch == '-' && i+1 < linea.length() && linea.charAt(i) == '-'){
                while (i < linea.length() && linea.charAt(i) != '\n'){
                    i++;
                }
                continue;
            }
            //Identificadores y palabras reservadas
            if (Character.isLetter(ch) || ch =='_'){
                int columnaInicio = columna;
                while (i < linea.length() && (Character.isLetterOrDigit(linea.charAt(i)) || linea.charAt(i) == '_')){
                    lexema.append(linea.charAt(i));
                    i++;
                    columna++;
                }
                String palabra = lexema.toString().toUpperCase();
                TipoToken tipo = PALABRAS_RESERVADAS.contains(palabra) ? TipoToken.valueOf(palabra) : TipoToken.IDENTIFICADOR;
                token.add(new Token(tipo, palabra, fila, columnaInicio));
                continue;
            }

            //Numeros y decimales
            if (Character.isDigit(ch)){
                StringBuilder numero = new StringBuilder();
                int columnaInicio = columna;
                boolean Decimal = false;
                while (i < linea.length() && (Character.isDigit(linea.charAt(i)) || linea.charAt(i) == '.')){
                    if (linea.charAt(i) == '.')  Decimal = true;
                    
                    numero.append(linea.charAt(i));
                    i++;
                    columna++;
                }
                TipoToken tipo = Decimal ? TipoToken.LITERAL_DECIMAL : TipoToken.LITERAL_ENTERO;
                token.add(new Token(tipo, numero.toString(), fila, columnaInicio));
                continue;
            }

            //Cadenas de texto
            if (ch == '\''){
                StringBuilder cadena = new StringBuilder();
                int columnaInicio = columna;
                i++; // Saltar el primer apóstrofe
                columna++;
                while (i < linea.length() && linea.charAt(i) != '\''){
                    cadena.append(linea.charAt(i));
                    i++;
                    columna++;
                }
                if (i < linea.length() && linea.charAt(i) == '\''){
                    i++; // Saltar el segundo apóstrofe
                    columna++;
                    token.add(new Token(TipoToken.LITERAL_CADENA, cadena.toString(), fila, columnaInicio));
                } else {
                    token.add(new Token(TipoToken.ERROR, cadena.toString(), fila, columnaInicio));
                }
                continue;
            }

            //Operadores y simbolos
            int columnaInicio = columna;
            switch(ch){
                case '=':
                    if(i+1<linea.length() && linea.charAt(i+1) == '='){
                        token.add(new Token(TipoToken.OP_IGUAL, "==", fila, columnaInicio));
                        i += 2;
                        columna += 2;
                    } else {
                        token.add(new Token(TipoToken.OP_ASIGNACION, "=", fila, columnaInicio));
                        i++;
                        columna++;
                    }
                    continue;
                case '>':
                    if(i+1<linea.length() && linea.charAt(i+1) == '='){
                        token.add(new Token(TipoToken.OP_MAYOR_IGUAL, ">=", fila, columnaInicio));
                        i += 2;
                        columna += 2;
                    } else {
                        token.add(new Token(TipoToken.OP_MAYOR, ">", fila, columnaInicio));
                        i++;
                        columna++;
                    }
                    continue;
                case '<':
                    if(i+1<linea.length() && linea.charAt(i+1) == '='){
                        token.add(new Token(TipoToken.OP_MENOR_IGUAL, "<=", fila, columnaInicio));
                        i += 2;
                        columna += 2;
                    } else if(i+1<linea.length() && linea.charAt(i+1) == '>'){
                        token.add(new Token(TipoToken.OP_DIFERENTE, "<>", fila, columnaInicio));
                        i += 2;
                        columna += 2;
                    } else {
                        token.add(new Token(TipoToken.OP_MENOR, "<", fila, columnaInicio));
                        i++;
                        columna++;
                    }
                    continue;
                case SIMBOLOS.COMA:
                    token.add(new Token(TipoToken.COMA, ",", fila, columnaInicio)); break;
                case SIMBOLOS.PUNTO_Y_COMA:
                    token.add(new Token(TipoToken.PUNTO_Y_COMA, ";", fila, columnaInicio)); break;
                case SIMBOLOS.PARENTESIS_IZQ:
                    token.add(new Token(TipoToken.PARENTESIS_IZQ, "(", fila, columnaInicio)); break;
                case SIMBOLOS.PARENTESIS_DER:
                    token.add(new Token(TipoToken.PARENTESIS_DER, ")", fila, columnaInicio)); break;
                case SIMBOLOS.ASTERISCO:
                    token.add(new Token(TipoToken.ASTERISCO, "*", fila, columnaInicio)); break;
                default:
                    token.add(new Token(TipoToken.ERROR, String.valueOf(ch), fila, columnaInicio)); break;
            }
            i++; columna++;
        }//fin del while

        token.add(new Token(TipoToken.FIN_DE_ARCHIVO, "EOF", fila, columna));
    }


    public static void main(String[] args) throws IOException {
        /*Esta clase solo lo hice para hacer puebas. Este main va en la clase principal
         *me quede bloqueqdo, para avanzar hace falta la tabla de simbolos
         */
        try (Scanner sc = new Scanner(System.in)) {
            String linea = sc.useDelimiter("\\A").nextLine();
            ScannerSQL scanner = new ScannerSQL();
            
        }catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        scanner.Analizar(linea);
    }
}