package execution;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

/* Conexion compartida de la fase 3 (Persona 3).
 * El motor es H2 embebido, va dentro del mismo programa y no hay que levantar
 * ningun servidor. Todos usamos el mismo jar: lib/h2-2.4.240.jar
 * Cada abrir() da una base en memoria nueva y vacia, asi cada script empieza
 * sin tablas como pide la prueba comun. El que ejecuta el script abre la
 * conexion una vez, se la pasa al EjecutorDDL y al EjecutorDML y la cierra al
 * final. Los ejecutores no la abren ni la cierran.
 */
public class ConexionSQL {

    public static final String VERSION_H2 = "2.4.240";
    public static final String CONTROLADOR = "org.h2.Driver";

    private static final String USUARIO = "sa";
    private static final String CLAVE = "";

    public static Connection abrir() throws SQLException {
        //cada base lleva su propio nombre para que no se mezclen las tablas
        //DB_CLOSE_DELAY=0 hace que la base se borre sola al cerrar la conexion
        String url = "jdbc:h2:mem:fase3_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=0";
        try {
            return DriverManager.getConnection(url, USUARIO, CLAVE);
        } catch (SQLException e) {
            //si falta el jar java solo dice "No suitable driver", pongo un mensaje mas claro
            if (!hayControlador()) {
                throw new SQLException("No se encontro el controlador de H2, falta lib/h2-"
                        + VERSION_H2 + ".jar en el classpath", e);
            }
            throw e;
        }
    }

    public static boolean hayControlador() {
        try {
            Class.forName(CONTROLADOR);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
