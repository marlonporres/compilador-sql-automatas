# Fase 3 — Persona 4: ejecución DML

## Responsabilidad
Ejecutar SELECCIONAR, INSERTAR, ACTUALIZAR y BORRAR sobre la conexión JDBC compartida, conservar las condiciones del parser y entregar resultados con columnas, filas, cantidad afectada, SQL generado y errores con posición.

## Integración sin modificar archivos del equipo
- `execution.EjecutorDML` es un módulo independiente que recibe `Connection`, número de sentencia y `SentenciaDML` ya validada.
- Reutiliza `TraductorSQL` y el `ResultadoEjecucion` que ya existen. No abre ni cierra la conexión común.
- `ui.PanelResultadosSQL` ya existe y consume `List<ResultadoEjecucion>`; no es necesario reemplazarlo.
- `ServicioEjecucion` ya contiene un camino funcional para DML. Para utilizar explícitamente este nuevo módulo en el servicio, Persona 3 o quien integre debe acordar ese cambio, sin sobrescribir su implementación actual.
- El ejecutor no valida la sintaxis ni la semántica: el llamador debe entregar únicamente sentencias aprobadas por `CompiladorSQL`.

## Flujo
Editor -> CompiladorSQL (válido) -> SentenciaDML -> EjecutorDML (conexión compartida) -> TraductorSQL -> PreparedStatement -> ResultadoEjecucion -> PanelResultadosSQL.

## Pruebas
Desde la raíz del proyecto, con Java 21:

```sh
javac -cp lib/h2-2.4.240.jar -d out $(find src tests -name '*.java')
java -cp out:lib/h2-2.4.240.jar PruebaEjecutorDMLPersona4
```

La prueba comprueba inserción, selección, columnas devueltas, actualización, borrado y tabla vacía. En Windows, sustituir `:` por `;` en el classpath y compilar mediante los scripts existentes del proyecto.

## Explicación para la exposición
Mi módulo ejecuta las cuatro instrucciones DML sobre la conexión del equipo. Usa consultas preparadas para separar valores del SQL, conserva las condiciones y devuelve filas o cantidades afectadas. Los errores de SQL se devuelven con la línea y columna de la sentencia. El panel de resultados del proyecto muestra esos datos.
