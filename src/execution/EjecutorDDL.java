package execution;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import parser.SentenciaDDL;

/* Ejecuta los cinco comandos DDL en la base (Persona 3).
 * Recibe la conexion ya abierta (ConexionSQL) y una SentenciaDDL que ya paso
 * por el CompiladorSQL sin errores, aqui no se vuelve a revisar sintaxis ni
 * semantica.
 * El SQL lo arma TraductorSQL, el mismo que usa el DML, asi los nombres y los
 * cinco tipos se convierten igual en todo el script:
 *   CREAR TABLA     -> CREATE TABLE
 *   ALTERAR TABLA   -> ALTER TABLE ... ADD / ALTER / DROP COLUMN
 *   ELIMINAR TABLA  -> DROP TABLE
 *   TRUNCAR TABLA   -> TRUNCATE TABLE
 *   RENOMBRAR TABLA -> ALTER TABLE ... RENAME TO
 */
public class EjecutorDDL {

    public ResultadoEjecucion ejecutar(Connection conexion, int numero, SentenciaDDL sentencia) {
        //el comando sale igual que en el panel de resultados: CREAR TABLA, ALTERAR TABLA...
        String comando = sentencia.getTipo().name().replace('_', ' ');
        String sql = "";
        try {
            sql = new TraductorSQL().traducir(sentencia).sql();
            //los DDL no llevan valores, por eso basta un Statement normal
            try (Statement st = conexion.createStatement()) {
                st.execute(sql);
                //un DDL no devuelve filas, la cantidad casi siempre es 0
                return new ResultadoEjecucion(numero, comando, sentencia.getLinea(), sentencia.getColumna(),
                        sql, List.of(), List.of(), Math.max(0, st.getUpdateCount()), "");
            }
        } catch (SQLException | IllegalArgumentException e) {
            //no lanzo la excepcion, la devuelvo con la linea y columna de la sentencia
            //para que la ventana pueda decir donde se detuvo
            //el codigo EXEC-001 es el mismo que ya usaba ServicioEjecucion para los errores de H2
            String mensaje = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new ResultadoEjecucion(numero, comando, sentencia.getLinea(), sentencia.getColumna(),
                    sql, List.of(), List.of(), 0, "EXEC-001: " + mensaje);
        }
    }
}
