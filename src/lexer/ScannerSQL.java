package lexer;

import java.io.*;
import java.util.*;

public class ScannerSQL{
    private static final Set<String> PALABRAS_RESERVADAS = new HashSet<>(Arrays.asList(
    "SELECCIONAR", "INSERTAR", "ACTUALIZAR", "BORRAR", "CREAR", "ALTERAR", "ELIMINAR",
    "TRUNCAR", "RENOMBRAR", "TABLA", "EN", "VALORES", "DESDE", "DONDE",
    "ASIGNAR", "Y", "O", "NO", "NULO", "PRIMARIA", "LLAVE", "FORANEA"
));


    // Operadores y símbolos comunes
    private static final Set<Character> SIMBOLOS = new HashSet<>(Arrays.asList(
        '(', ')', ',', ';', '=', '*', '<', '>', '+', '-', '/', '.'
    ));
    private List<lexer.Token> token=new ArrayList<>();
    lexer.TipoToken tipo;


    public void Analizar(String linea) throws IOException{
        StringBuilder lexema = new StringBuilder();
        int conlinea=0;
        char chr;
        boolean comentario=false;
        for(int i=0; i<linea.length(); i++){
            chr = linea.charAt(i);
            if(!comentario && linea.charAt(i)=='/' && linea.charAt(i+1)=='*'){
                comentario=true;
                continue;
            }
            if(comentario && linea.charAt(i)=='*' && linea.charAt(i+1)=='/'){
                comentario=false;
                continue;
            }
            if(!Character.isWhitespace(chr)){
                lexema.append(chr);
                continue;
            }
            if(chr=='\n'){
                conlinea++;
            }
            EstabToken(lexema.toString(), conlinea, linea.length()-(i-1));
        }
    }

    public void EstabToken(String lexema, int linea, int columna){
        if(lexema.isEmpty()){
            return;
        }
        if(PALABRAS_RESERVADAS.contains(lexema.toUpperCase())){
            tipo = TipoToken.valueOf(lexema.toUpperCase());
        } else if(lexema.matches("\\d+")){
            tipo = TipoToken.LITERAL_ENTERO;
        } else if(lexema.matches("\\d+\\.\\d+")){
            tipo = TipoToken.LITERAL_DECIMAL;
        } else if(lexema.matches("'[^']*'")){
            tipo = TipoToken.LITERAL_CADENA;
        } else if(lexema.matches("[a-zA-Z_][a-zA-Z0-9_]*")){
            tipo = TipoToken.IDENTIFICADOR;
        } else {
            tipo = TipoToken.ERROR;
        }
        token.add(new Token(tipo, lexema, linea, columna));
    }

    
    public static void main(String[] args) throws IOException {
        Scanner sc  = new Scanner(System.in);
        String linea= sc.useDelimiter("\\n").next();
        ScannerSQL scanner = new ScannerSQL();
        scanner.Analizar(linea);

    }
}
