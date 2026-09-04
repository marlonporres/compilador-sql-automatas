package lexer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ScannerSQL {
    /* Cada palabra reservada apuntando a su token.
     * Antes era un Set y el tipo salia de TipoToken.valueOf(), pero eso reventaba
     * cuando la palabra no estaba en el enum.
     *
     * OJO equipo: INSERTAR, NULO, PRIMARIA, LLAVE y FORANEA no existen en
     * TipoToken, por ahora quedan como identificadores. Si las queremos hay que
     * agregarlas primero al enum.
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
                columna += 2;
                //avanza mientras NO llegue el cierre
                while (i < linea.length() && !(linea.charAt(i)=='*' && i+1 < linea.length() && linea.charAt(i+1)=='/')){
                    if(linea.charAt(i) == '\n') {
                        fila++;
                        columna = 1;
                    } else {
                        columna++;
                    }
                    i++;
                }
                if (i < linea.length()) {
                    i += 2; //saltar '*' y '/'
                    columna += 2;
                }
                continue;
            }
            //salta comentario en linea
            if(ch == '-' && i+1 < linea.length() && linea.charAt(i+1) == '-'){
                while (i < linea.length() && linea.charAt(i) != '\n'){
                    i++;
                    columna++;
                }
                continue;
            }
            //Identificadores y palabras reservadas
            if (Character.isLetter(ch) || ch =='_'){
                int columnaInicio = columna;
                lexema.setLength(0); //si no se limpia se pegan los lexemas
                while (i < linea.length() && (Character.isLetterOrDigit(linea.charAt(i)) || linea.charAt(i) == '_')){
                    lexema.append(linea.charAt(i));
                    i++;
                    columna++;
                }
                String original = lexema.toString();
                String palabra = original.toUpperCase();
                TipoToken tipo = PALABRAS_RESERVADAS.getOrDefault(palabra, TipoToken.IDENTIFICADOR);
                //las reservadas en mayusculas, pero el identificador como lo escribieron
                token.add(new Token(tipo, tipo == TipoToken.IDENTIFICADOR ? original : palabra, fila, columnaInicio));
                continue;
            }

            //Numeros y decimales
            if (Character.isDigit(ch)){
                StringBuilder numero = new StringBuilder();
                int columnaInicio = columna;
                int puntos = 0;
                while (i < linea.length() && (Character.isDigit(linea.charAt(i)) || linea.charAt(i) == '.')){
                    if (linea.charAt(i) == '.')  puntos++;

                    numero.append(linea.charAt(i));
                    i++;
                    columna++;
                }
                //un numero solo puede llevar un punto y no puede terminar en punto
                TipoToken tipo;
                if (puntos > 1 || numero.charAt(numero.length()-1) == '.') {
                    tipo = TipoToken.ERROR;
                } else if (puntos == 1) {
                    tipo = TipoToken.LITERAL_DECIMAL;
                } else {
                    tipo = TipoToken.LITERAL_ENTERO;
                }
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
                case '!':
                    //faltaba !=, solo estaba <>
                    if(i+1<linea.length() && linea.charAt(i+1) == '='){
                        token.add(new Token(TipoToken.OP_DIFERENTE, "!=", fila, columnaInicio));
                        i += 2;
                        columna += 2;
                    } else {
                        token.add(new Token(TipoToken.ERROR, "!", fila, columnaInicio));
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
                case ',':
                    token.add(new Token(TipoToken.COMA, ",", fila, columnaInicio)); break;
                case ';':
                    token.add(new Token(TipoToken.PUNTO_Y_COMA, ";", fila, columnaInicio)); break;
                case '(':
                    token.add(new Token(TipoToken.PARENTESIS_IZQ, "(", fila, columnaInicio)); break;
                case ')':
                    token.add(new Token(TipoToken.PARENTESIS_DER, ")", fila, columnaInicio)); break;
                case '*':
                    token.add(new Token(TipoToken.ASTERISCO, "*", fila, columnaInicio)); break;
                default:
                    token.add(new Token(TipoToken.ERROR, String.valueOf(ch), fila, columnaInicio)); break;
            }
            i++; columna++;
        }//fin del while

        token.add(new Token(TipoToken.FIN_DE_ARCHIVO, "EOF", fila, columna));
    }

    //Los tokens de la ultima vez que se llamo Analizar. Los necesita el Main
    //para pasarselos a la tabla de simbolos.
    public List<Token> getTokens() {
        return new ArrayList<>(token);
    }


    /* Solo para probar el scanner por separado, el main de verdad va en Main.java
     *   java -cp out lexer.ScannerSQL tests/test_ddl.sql
     */
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
        scanner.Analizar(entrada);

        for (Token t : scanner.getTokens()) {
            System.out.println(t);
        }
    }
}