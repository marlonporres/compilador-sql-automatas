import java.util.ArrayList;
import java.util.List;

import lexer.ScannerSQL;
import lexer.TipoToken;
import lexer.Token;
import parser.AnalizadorSintacticoDDL;
import parser.ColumnaDDL;
import parser.ResultadoSintacticoDDL;
import parser.SentenciaDDL;
import parser.TipoSentenciaDDL;
import semantic.AnalizadorSemanticoDDL;
import semantic.ErrorSemantico;
import symbol_table.CatalogoEsquema;
import symbol_table.CategoriaSimbolo;
import symbol_table.ColumnaDeclarada;
import symbol_table.TablaSimbolos;

/* Pruebas del catalogo y de la semantica DDL de la fase 2.
 * Casi todas arman las sentencias a mano para no depender del parser,
 * las ultimas pasan por el ScannerSQL y el parser DDL de verdad.
 *
 * powershell -NoProfile -ExecutionPolicy Bypass -File .\demo.ps1 -Modo pruebas
 * java -cp out PruebaSemanticaDDL
 */
public class PruebaSemanticaDDL {

    private static int total = 0;
    private static int fallidas = 0;

    public static void main(String[] args) {
        System.out.println("PRUEBAS DE LA SEMANTICA DDL");
        System.out.println();

        catalogoVacio();
        crearTabla();
        mismaColumnaEnDosTablas();
        tablaRepetida();
        columnaRepetida();
        eliminarTabla();
        truncarTabla();
        alterarYRenombrarSimple();
        renombrarCompleto();
        agregarYEliminarColumna();
        modificarColumna();
        ddlCompletoConScannerYParser();
        representacionesIncompletas();
        consultasParaDML();
        formatoDeErrores();
        conScannerYParser();
        clasificaLaTablaDeSimbolos();

        System.out.println();
        ejemploDeSalida();

        System.out.println();
        System.out.println("Pruebas: " + total + "   Fallidas: " + fallidas);
        if (fallidas > 0) {
            System.exit(1);
        }
    }

    private static void catalogoVacio() {
        CatalogoEsquema catalogo = new CatalogoEsquema();
        probar("catalogo nuevo vacio", catalogo.estaVacio());
        probar("sin tablas no hay columnas", catalogo.columnasDe("usuarios").isEmpty());
    }

    private static void crearTabla() {
        AnalizadorSemanticoDDL sem = nuevo();
        List<ErrorSemantico> errores = sem.analizarSentencia(crear("usuarios", 1, 1,
                col("id", TipoToken.TIPO_ENTERO, 1, 23),
                col("nombre", TipoToken.TIPO_TEXTO, 1, 34),
                col("activo", TipoToken.TIPO_BOOLEANO, 1, 47)));
        CatalogoEsquema catalogo = sem.getCatalogo();

        probar("CREAR bien no da errores", errores.isEmpty());
        probar("la tabla existe aunque se busque en mayusculas", catalogo.existeTabla("USUARIOS"));
        probar("columnas en el orden en que se declararon",
                nombres(catalogo.columnasDe("usuarios")).equals(List.of("id", "nombre", "activo")));
        probar("cada columna guarda su tipo", catalogo.tipoDeColumna("usuarios", "id") == TipoToken.TIPO_ENTERO
                && catalogo.tipoDeColumna("usuarios", "ACTIVO") == TipoToken.TIPO_BOOLEANO);
        ColumnaDeclarada id = catalogo.buscarColumna("usuarios", "id");
        probar("el ambito de la columna es su tabla", id.getAmbito().equals("usuarios")
                && id.getIdentidad().equals("usuarios.id"));
        probar("la tabla esta en el ambito global", catalogo.buscarTabla("usuarios").getAmbito().equals("GLOBAL"));
        probar("la columna guarda su posicion", id.getLinea() == 1 && id.getColumna() == 23);
    }

    private static void mismaColumnaEnDosTablas() {
        AnalizadorSemanticoDDL sem = nuevo();
        sem.analizarSentencia(crear("usuarios", 1, 1, col("id", TipoToken.TIPO_ENTERO, 1, 23)));
        List<ErrorSemantico> errores = sem.analizarSentencia(crear("productos", 2, 1,
                col("id", TipoToken.TIPO_TEXTO, 2, 24)));
        ColumnaDeclarada idUsuarios = sem.getCatalogo().buscarColumna("usuarios", "id");
        ColumnaDeclarada idProductos = sem.getCatalogo().buscarColumna("productos", "id");

        probar("id en dos tablas no es error", errores.isEmpty());
        probar("usuarios.id y productos.id son columnas distintas", idUsuarios != idProductos);
        probar("cada id tiene su propio tipo", idUsuarios.getTipoDato() == TipoToken.TIPO_ENTERO
                && idProductos.getTipoDato() == TipoToken.TIPO_TEXTO);
    }

    private static void tablaRepetida() {
        AnalizadorSemanticoDDL sem = conUsuarios();
        List<ErrorSemantico> errores = sem.analizarSentencia(crear("USUARIOS", 3, 1,
                col("correo", TipoToken.TIPO_TEXTO, 3, 23)));

        probar("tabla repetida da SEM-101", codigos(errores).equals(List.of("SEM-101"))
                && errores.get(0).getLinea() == 3);
        probar("la tabla que ya estaba no cambia", sem.getCatalogo().cantidadTablas() == 1
                && !sem.getCatalogo().existeColumna("usuarios", "correo"));
    }

    private static void columnaRepetida() {
        AnalizadorSemanticoDDL sem = nuevo();
        List<ErrorSemantico> errores = sem.analizarSentencia(crear("t", 1, 1,
                col("id", TipoToken.TIPO_ENTERO, 1, 16),
                col("nombre", TipoToken.TIPO_TEXTO, 1, 27),
                col("ID", TipoToken.TIPO_FECHA, 1, 40)));

        probar("columna repetida da SEM-102", codigos(errores).equals(List.of("SEM-102")));
        probar("el error sale donde se repite", errores.size() == 1
                && errores.get(0).getElemento().equals("t.ID") && errores.get(0).getColumna() == 40);
        probar("la tabla no se crea a medias", !sem.getCatalogo().existeTabla("t"));

        errores = sem.analizarSentencia(crear("t", 2, 1,
                col("a", TipoToken.TIPO_ENTERO, 2, 16),
                col("b", TipoToken.TIPO_TEXTO, 2, 27),
                col("a", TipoToken.TIPO_ENTERO, 2, 35),
                col("b", TipoToken.TIPO_TEXTO, 2, 46)));
        probar("si se repiten varias salen todas", codigos(errores).equals(List.of("SEM-102", "SEM-102")));
    }

    private static void eliminarTabla() {
        AnalizadorSemanticoDDL sem = conUsuarios();
        CatalogoEsquema catalogo = sem.getCatalogo();

        List<ErrorSemantico> errores = sem.analizarSentencia(simple(TipoSentenciaDDL.ELIMINAR_TABLA, "usuarios", 5));
        probar("ELIMINAR quita la tabla y sus columnas", errores.isEmpty()
                && !catalogo.existeTabla("usuarios") && !catalogo.existeColumna("usuarios", "id"));

        errores = sem.analizarSentencia(simple(TipoSentenciaDDL.ELIMINAR_TABLA, "usuarios", 6));
        probar("ELIMINAR una que no existe da SEM-103", codigos(errores).equals(List.of("SEM-103"))
                && errores.get(0).getLinea() == 6);

        errores = sem.analizarSentencia(crear("usuarios", 7, 1, col("correo", TipoToken.TIPO_TEXTO, 7, 23)));
        probar("despues de eliminarla se puede crear otra vez", errores.isEmpty()
                && nombres(catalogo.columnasDe("usuarios")).equals(List.of("correo")));
    }

    private static void truncarTabla() {
        AnalizadorSemanticoDDL sem = conUsuarios();
        List<ErrorSemantico> errores = sem.analizarSentencia(simple(TipoSentenciaDDL.TRUNCAR_TABLA, "usuarios", 5));
        probar("TRUNCAR deja la tabla igual", errores.isEmpty()
                && nombres(sem.getCatalogo().columnasDe("usuarios")).equals(List.of("id", "nombre")));

        errores = sem.analizarSentencia(simple(TipoSentenciaDDL.TRUNCAR_TABLA, "pedidos", 6));
        probar("TRUNCAR una que no existe da SEM-103", codigos(errores).equals(List.of("SEM-103")));
    }

    private static void alterarYRenombrarSimple() {
        AnalizadorSemanticoDDL sem = conUsuarios();
        List<ErrorSemantico> errores = sem.analizar(List.of(
                simple(TipoSentenciaDDL.ALTERAR_TABLA, "usuarios", 5),
                simple(TipoSentenciaDDL.RENOMBRAR_TABLA, "usuarios", 6)));
        probar("ALTERAR y RENOMBRAR incompletos dan SEM-106", codigos(errores).equals(List.of("SEM-106", "SEM-106"))
                && sem.getCatalogo().buscarTabla("usuarios").cantidadColumnas() == 2);

        errores = sem.analizar(List.of(
                simple(TipoSentenciaDDL.ALTERAR_TABLA, "pedidos", 7),
                simple(TipoSentenciaDDL.RENOMBRAR_TABLA, "pedidos", 8)));
        probar("AST incompletos se rechazan aun si la tabla no existe",
                codigos(errores).equals(List.of("SEM-106", "SEM-106")));
    }

    private static void renombrarCompleto() {
        AnalizadorSemanticoDDL sem = conUsuarios();
        CatalogoEsquema catalogo = sem.getCatalogo();
        sem.analizarSentencia(crear("productos", 2, 1, col("id", TipoToken.TIPO_ENTERO, 2, 24)));

        List<ErrorSemantico> errores = sem.renombrarTabla("usuarios", "clientes", 5, 1);
        probar("renombrar cambia el nombre", errores.isEmpty()
                && !catalogo.existeTabla("usuarios") && catalogo.existeTabla("clientes"));
        probar("las columnas siguen igual y cambian de ambito",
                catalogo.tipoDeColumna("clientes", "nombre") == TipoToken.TIPO_TEXTO
                        && catalogo.buscarColumna("clientes", "id").getIdentidad().equals("clientes.id"));

        errores = sem.renombrarTabla("clientes", "PRODUCTOS", 6, 1);
        probar("renombrar a un nombre que ya existe da SEM-101",
                codigos(errores).equals(List.of("SEM-101")) && catalogo.existeTabla("clientes"));

        errores = sem.renombrarTabla("usuarios", "personas", 7, 1);
        probar("renombrar una que no existe da SEM-103", codigos(errores).equals(List.of("SEM-103")));
    }

    private static void agregarYEliminarColumna() {
        AnalizadorSemanticoDDL sem = conUsuarios();
        CatalogoEsquema catalogo = sem.getCatalogo();

        List<ErrorSemantico> errores = sem.agregarColumna("usuarios", col("correo", TipoToken.TIPO_TEXTO, 5, 30));
        probar("agregar columna la pone al final", errores.isEmpty()
                && nombres(catalogo.columnasDe("usuarios")).equals(List.of("id", "nombre", "correo")));

        errores = sem.agregarColumna("usuarios", col("CORREO", TipoToken.TIPO_FECHA, 6, 30));
        probar("agregar una que ya esta da SEM-102", codigos(errores).equals(List.of("SEM-102")));

        errores = sem.agregarColumna("pedidos", col("total", TipoToken.TIPO_DECIMAL, 7, 30));
        probar("agregar en tabla que no existe da SEM-103", codigos(errores).equals(List.of("SEM-103")));

        errores = sem.eliminarColumna("usuarios", "correo", 8, 1);
        probar("eliminar columna la quita", errores.isEmpty() && !catalogo.existeColumna("usuarios", "correo"));

        errores = sem.eliminarColumna("usuarios", "telefono", 9, 1);
        probar("eliminar una que no esta da SEM-104", codigos(errores).equals(List.of("SEM-104")));

        sem.eliminarColumna("usuarios", "nombre", 10, 1);
        errores = sem.eliminarColumna("usuarios", "id", 11, 1);
        probar("no deja eliminar la unica columna (SEM-105)", codigos(errores).equals(List.of("SEM-105"))
                && catalogo.existeColumna("usuarios", "id"));
    }

    private static void consultasParaDML() {
        CatalogoEsquema catalogo = conUsuarios().getCatalogo();
        probar("buscar con null no truena", catalogo.buscarTabla(null) == null
                && catalogo.buscarColumna(null, "id") == null
                && catalogo.tipoDeColumna("usuarios", null) == null);
        probar("columna de una tabla que no existe", !catalogo.existeColumna("pedidos", "id")
                && catalogo.columnasDe("pedidos").isEmpty());

        catalogo.columnasDe("usuarios").clear();
        probar("borrar la lista que devuelve no cambia el catalogo", catalogo.columnasDe("usuarios").size() == 2);
    }

    private static void modificarColumna() {
        AnalizadorSemanticoDDL sem = conUsuarios();
        CatalogoEsquema catalogo = sem.getCatalogo();
        List<ErrorSemantico> errores = sem.modificarColumna("usuarios", col("ID", TipoToken.TIPO_DECIMAL, 5, 31));
        ColumnaDeclarada id = catalogo.buscarColumna("usuarios", "id");
        probar("MODIFICAR cambia el tipo", errores.isEmpty() && id.getTipoDato() == TipoToken.TIPO_DECIMAL);
        probar("MODIFICAR conserva nombre, orden y posicion original", id.getNombre().equals("id")
                && id.getLinea() == 1 && id.getColumna() == 23
                && nombres(catalogo.columnasDe("usuarios")).equals(List.of("id", "nombre")));
        errores = sem.modificarColumna("usuarios", col("fantasma", TipoToken.TIPO_TEXTO, 6, 31));
        probar("MODIFICAR columna ausente no cambia el catalogo", codigos(errores).equals(List.of("SEM-104"))
                && !catalogo.existeColumna("usuarios", "fantasma")
                && catalogo.tipoDeColumna("usuarios", "id") == TipoToken.TIPO_DECIMAL);
        errores = sem.modificarColumna("ausente", col("id", TipoToken.TIPO_TEXTO, 7, 31));
        probar("MODIFICAR tabla ausente da SEM-103", codigos(errores).equals(List.of("SEM-103")));
        errores = sem.modificarColumna("usuarios", null);
        probar("MODIFICAR columna incompleta no aplica cambios", codigos(errores).equals(List.of("SEM-106"))
                && catalogo.tipoDeColumna("usuarios", "id") == TipoToken.TIPO_DECIMAL);
        probar("catalogo devuelve false al modificar objetos inexistentes",
                !catalogo.modificarColumna("ausente", "id", TipoToken.TIPO_TEXTO)
                        && !catalogo.modificarColumna("usuarios", "ausente", TipoToken.TIPO_TEXTO));
    }

    private static void ddlCompletoConScannerYParser() {
        String script = "CREAR TABLA usuarios (id ENTERO, nombre TEXTO);\n"
                + "CREAR TABLA productos (id ENTERO);\n"
                + "ALTERAR TABLA usuarios AGREGAR activo BOOLEANO;\n"
                + "ALTERAR TABLA usuarios MODIFICAR ID DECIMAL;\n"
                + "ALTERAR TABLA usuarios QUITAR nombre;\n"
                + "RENOMBRAR TABLA usuarios A clientes;";
        List<Token> tokens = tokens(script);
        TablaSimbolos simbolos = new TablaSimbolos();
        simbolos.registrarTodos(tokens);
        AnalizadorSemanticoDDL sem = new AnalizadorSemanticoDDL(new CatalogoEsquema(), simbolos);
        ResultadoSintacticoDDL sintaxis = new AnalizadorSintacticoDDL().analizar(tokens);
        List<ErrorSemantico> errores = sem.analizar(sintaxis.getSentencias());
        CatalogoEsquema catalogo = sem.getCatalogo();
        probar("parser y semantica aceptan ALTERAR y RENOMBRAR completos", sintaxis.esValido() && errores.isEmpty());
        probar("AGREGAR y QUITAR modifican el esquema", nombres(catalogo.columnasDe("clientes"))
                .equals(List.of("id", "activo")) && !catalogo.existeTabla("usuarios"));
        probar("tipo modificado y declaracion original quedan en catalogo",
                catalogo.tipoDeColumna("clientes", "id") == TipoToken.TIPO_DECIMAL
                        && catalogo.buscarColumna("clientes", "id").getLinea() == 1
                        && catalogo.buscarColumna("clientes", "id").getColumna() == 23);
        probar("RENOMBRAR cambia ambitos y conserva orden de tablas",
                catalogo.buscarColumna("clientes", "activo").getIdentidad().equals("clientes.activo")
                        && catalogo.obtenerTablas().get(0).getNombre().equals("clientes")
                        && catalogo.obtenerTablas().get(1).getNombre().equals("productos"));
        probar("declaraciones nuevas actualizan simbolos lexicos",
                simbolos.buscar("clientes").getCategoria() == CategoriaSimbolo.TABLA
                        && simbolos.buscar("activo").getCategoria() == CategoriaSimbolo.COLUMNA
                        && simbolos.buscar("activo").getTipoDato().equals("BOOLEANO"));

        String invalidas = "ALTERAR TABLA clientes AGREGAR id FECHA;\n"
                + "ALTERAR TABLA clientes MODIFICAR fantasma FECHA;\n"
                + "RENOMBRAR TABLA clientes A productos;\n"
                + "ALTERAR TABLA clientes QUITAR activo;\n"
                + "ALTERAR TABLA clientes QUITAR id;\n"
                + "ALTERAR TABLA ausente MODIFICAR id TEXTO;";
        ResultadoSintacticoDDL segunda = new AnalizadorSintacticoDDL().analizar(tokens(invalidas));
        errores = sem.analizar(segunda.getSentencias());
        probar("DDL completos invalidos generan sus errores en orden", segunda.esValido()
                && codigos(errores).equals(List.of("SEM-102", "SEM-104", "SEM-101", "SEM-105", "SEM-103")));
        probar("errores no dejan cambios parciales", catalogo.cantidadTablas() == 2
                && nombres(catalogo.columnasDe("clientes")).equals(List.of("id"))
                && catalogo.tipoDeColumna("clientes", "id") == TipoToken.TIPO_DECIMAL
                && !catalogo.existeColumna("clientes", "fantasma"));
        probar("cada error se registra una sola vez", sem.getErrores().size() == errores.size());
        probar("errores usan posicion del elemento original", errores.get(0).getLinea() == 1
                && errores.get(0).getColumna() == 32 && errores.get(2).getLinea() == 3
                && errores.get(2).getColumna() == 28);
    }

    private static void representacionesIncompletas() {
        AnalizadorSemanticoDDL sem = conUsuarios();
        Token tabla = new Token(TipoToken.IDENTIFICADOR, "usuarios", 9, 1);
        Token nuevo = new Token(TipoToken.IDENTIFICADOR, "clientes", 9, 30);
        List<SentenciaDDL> invalidas = List.of(
                new SentenciaDDL(TipoSentenciaDDL.ALTERAR_TABLA, tabla, List.of(), 9, 1, "AGREGAR", null),
                new SentenciaDDL(TipoSentenciaDDL.ALTERAR_TABLA, tabla, List.of(), 9, 1, "QUITAR", null),
                new SentenciaDDL(TipoSentenciaDDL.ALTERAR_TABLA, tabla,
                        List.of(col("edad", TipoToken.TIPO_ENTERO, 9, 30)), 9, 1, "CAMBIAR", null),
                new SentenciaDDL(TipoSentenciaDDL.RENOMBRAR_TABLA, tabla, List.of(), 9, 1, "", null),
                new SentenciaDDL(TipoSentenciaDDL.RENOMBRAR_TABLA, tabla,
                        List.of(col("edad", TipoToken.TIPO_ENTERO, 9, 30)), 9, 1, "", nuevo),
                crear("vacía", 9, 1));
        for (SentenciaDDL invalida : invalidas) {
            probar("rechaza AST DDL incompleto o contradictorio", codigos(sem.analizarSentencia(invalida))
                    .equals(List.of("SEM-106")));
        }
        probar("AST invalidos conservan catalogo", sem.getCatalogo().cantidadTablas() == 1
                && nombres(sem.getCatalogo().columnasDe("usuarios")).equals(List.of("id", "nombre")));
    }

    private static void formatoDeErrores() {
        AnalizadorSemanticoDDL sem = conUsuarios();
        sem.analizarSentencia(crear("usuarios", 4, 1, col("x", TipoToken.TIPO_ENTERO, 4, 23)));
        sem.analizarSentencia(simple(TipoSentenciaDDL.ELIMINAR_TABLA, "pedidos", 5));
        List<ErrorSemantico> errores = sem.getErrores();

        boolean completos = errores.size() == 2;
        for (ErrorSemantico e : errores) {
            if (!e.getFase().equals("SEMANTICA") || e.getElemento().isEmpty() || e.getLinea() < 1) {
                completos = false;
            }
        }
        probar("los errores traen fase, codigo, elemento y posicion", completos);
        probar("se imprimen igual que los sintacticos", errores.size() == 2 && errores.get(1).toString()
                .equals("[SEMANTICA][SEM-103] No existe una tabla con ese nombre (elemento: \"pedidos\", L:5 C:1)"));
    }

    private static void conScannerYParser() {
        String script = "CREAR TABLA usuarios (id ENTERO, nombre TEXTO);\n"
                + "CREAR TABLA productos (id ENTERO, precio DECIMAL);\n"
                + "CREAR TABLA usuarios (correo TEXTO);\n"
                + "CREAR TABLA pedidos (id ENTERO, id FECHA);\n"
                + "TRUNCAR TABLA usuarios;\n"
                + "ELIMINAR TABLA productos;\n"
                + "ALTERAR TABLA productos AGREGAR activo BOOLEANO;\n"
                + "CREAR TABLA roto (id);";
        ResultadoSintacticoDDL sintaxis = new AnalizadorSintacticoDDL().analizar(tokens(script));
        AnalizadorSemanticoDDL sem = nuevo();
        List<ErrorSemantico> errores = sem.analizar(sintaxis.getSentencias());
        CatalogoEsquema catalogo = sem.getCatalogo();

        probar("la sentencia mal escrita no llega a la semantica", sintaxis.getErrores().size() == 1
                && !catalogo.existeTabla("roto"));
        probar("salen los errores en orden", codigos(errores).equals(List.of("SEM-101", "SEM-102", "SEM-103")));
        probar("con la linea y columna del archivo", errores.size() == 3
                && errores.get(0).getLinea() == 3 && errores.get(1).getLinea() == 4
                && errores.get(1).getColumna() == 33 && errores.get(2).getLinea() == 7);
        probar("al final solo queda usuarios", catalogo.cantidadTablas() == 1
                && nombres(catalogo.columnasDe("usuarios")).equals(List.of("id", "nombre")));
    }

    private static void clasificaLaTablaDeSimbolos() {
        String script = "CREAR TABLA usuarios (id ENTERO, nombre TEXTO);\n"
                + "CREAR TABLA productos (id TEXTO, precio DECIMAL);\n"
                + "CREAR TABLA usuarios (fantasma FECHA);\n"
                + "CREAR TABLA mala (x @);";
        List<Token> tokens = tokens(script);
        TablaSimbolos tabla = new TablaSimbolos();
        tabla.registrarTodos(tokens);
        AnalizadorSemanticoDDL sem = new AnalizadorSemanticoDDL(new CatalogoEsquema(), tabla);
        sem.analizar(new AnalizadorSintacticoDDL().analizar(tokens).getSentencias());

        probar("usuarios queda como TABLA", tabla.buscar("usuarios").getCategoria() == CategoriaSimbolo.TABLA);
        probar("precio queda como COLUMNA DECIMAL", tabla.buscar("precio").getCategoria() == CategoriaSimbolo.COLUMNA
                && tabla.buscar("precio").getTipoDato().equals("DECIMAL"));
        probar("id con dos tipos queda VARIOS", tabla.buscar("id").getTipoDato().equals("VARIOS"));
        probar("lo de sentencias rechazadas sigue DESCONOCIDA",
                tabla.buscar("fantasma").getCategoria() == CategoriaSimbolo.DESCONOCIDA
                        && tabla.buscar("mala").getCategoria() == CategoriaSimbolo.DESCONOCIDA);
    }

    //Imprime el catalogo para ver como queda
    private static void ejemploDeSalida() {
        AnalizadorSemanticoDDL sem = conUsuarios();
        sem.analizarSentencia(crear("productos", 2, 1,
                col("id", TipoToken.TIPO_TEXTO, 2, 24),
                col("precio", TipoToken.TIPO_DECIMAL, 2, 33)));
        sem.getCatalogo().imprimir();
    }

    //---- ayudas ----

    private static AnalizadorSemanticoDDL nuevo() {
        return new AnalizadorSemanticoDDL(new CatalogoEsquema());
    }

    //catalogo con usuarios(id ENTERO, nombre TEXTO) ya creada
    private static AnalizadorSemanticoDDL conUsuarios() {
        AnalizadorSemanticoDDL sem = nuevo();
        sem.analizarSentencia(crear("usuarios", 1, 1,
                col("id", TipoToken.TIPO_ENTERO, 1, 23),
                col("nombre", TipoToken.TIPO_TEXTO, 1, 34)));
        return sem;
    }

    private static SentenciaDDL crear(String tabla, int linea, int columna, ColumnaDDL... columnas) {
        return new SentenciaDDL(TipoSentenciaDDL.CREAR_TABLA, tabla, List.of(columnas), linea, columna);
    }

    private static SentenciaDDL simple(TipoSentenciaDDL tipo, String tabla, int linea) {
        return new SentenciaDDL(tipo, tabla, List.of(), linea, 1);
    }

    private static ColumnaDDL col(String nombre, TipoToken tipo, int linea, int columna) {
        return new ColumnaDDL(nombre, tipo, linea, columna);
    }

    private static List<String> nombres(List<ColumnaDeclarada> columnas) {
        List<String> nombres = new ArrayList<>();
        for (ColumnaDeclarada c : columnas) {
            nombres.add(c.getNombre());
        }
        return nombres;
    }

    private static List<String> codigos(List<ErrorSemantico> errores) {
        List<String> codigos = new ArrayList<>();
        for (ErrorSemantico e : errores) {
            codigos.add(e.getCodigo());
        }
        return codigos;
    }

    private static List<Token> tokens(String entrada) {
        ScannerSQL scanner = new ScannerSQL();
        scanner.analizar(entrada);
        return scanner.getTokens();
    }

    private static void probar(String descripcion, boolean condicion) {
        total++;
        if (condicion) {
            System.out.println("  ok    " + descripcion);
        } else {
            fallidas++;
            System.out.println("  FALLA " + descripcion);
        }
    }
}
