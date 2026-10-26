# Tablas y filas permanentes en SQL Studio

La interfaz normal guarda tablas y filas entre ejecuciones y reinicios de la
aplicación. La base es local a cada usuario y computadora.

## Ubicación

Se usa `Path.of(System.getProperty("user.home"), "SQLStudio", "datos", "compilador")`.
H2 añade `.mv.db` al nombre. Las carpetas se crean automáticamente.

- Windows: `C:\Users\TuUsuario\SQLStudio\datos\compilador.mv.db`.
- macOS: `/Users/tuusuario/SQLStudio/datos/compilador.mv.db`.
- Linux: `/home/tuusuario/SQLStudio/datos/compilador.mv.db`.

La ubicación no depende de la carpeta desde la que se inicia Java. El pie de
la ventana indica que los datos se guardan; al colocar el cursor encima muestra
la ruta completa. La base no se comparte automáticamente entre computadoras.

## Ejemplo en dos ejecuciones

Primero, pegar este script, pulsar Analizar y luego Ejecutar:

```sql
CREAR TABLA alumnos (id ENTERO, nombre TEXTO);
INSERTAR EN alumnos VALORES (1, 'Ana');
```

Después, pulsar Limpiar y escribir solamente:

```sql
SELECCIONAR * DESDE alumnos;
```

Pulsar Analizar y Ejecutar. Aparecerá la fila de Ana. La misma consulta funciona
al cerrar y volver a abrir SQL Studio. No se debe volver a ejecutar la creación
si la tabla ya existe; el análisis informará que está duplicada. Ejecutar de
nuevo un INSERTAR agrega otra fila, no reinicia la base.

## Qué hace cada acción

- Analizar lee el esquema actual por JDBC y valida sobre una copia del catálogo.
  No crea, altera ni elimina tablas y tampoco inserta, actualiza o borra filas.
  El catálogo mostrado es la previsión del script; se aplica al ejecutar.
- Ejecutar vuelve a validar con el esquema real y guarda los cambios. Si hay
  errores de análisis no ejecuta ninguna sentencia. Ante un fallo SQL se detiene;
  las sentencias anteriores pueden haber quedado guardadas y se muestran en resultados.
- Limpiar borra editor y paneles; conserva la base.
- Comparar SQL usa dos bases temporales vacías, aun si las tablas existen en la
  base guardada. Por ello, los scripts de comparación deben incluir sus declaraciones.
  Está disponible con fuente y referencia presentes y valida internamente el script.
  Después de comparar se debe pulsar Analizar para habilitar Ejecutar contra la base real.
- TRUNCAR y BORRAR eliminan filas únicamente cuando se ejecutan; ELIMINAR borra
  una tabla cuando se ejecuta. Son cambios permanentes.

Los ejemplos completos pueden dejar tablas guardadas si se ejecutan. Para
repetir demostraciones desde cero, usar Comparar SQL con su referencia independiente.

## Implementación

- `execution/BasePersistente.java`: ruta portable, apertura y recuperación del esquema
  desde los metadatos de H2, incluyendo los cinco tipos admitidos.
- `compiler/CompiladorSQL.java`: nueva sobrecarga que acepta un catálogo inicial y lo copia.
- `semantic/CatalogoSemantico.java`: carga y copia del esquema.
- `execution/ServicioEjecucion.java`: servicio permanente para la GUI; el constructor sin
  argumentos mantiene el modo temporal de CLI y pruebas. Comparar siempre usa bases vacías.
- `ui/ControladorGUI.java`: usa el mismo servicio para analizar y ejecutar, y evita
  reutilizar un análisis de comparación como permiso para ejecutar sobre la base permanente.
- `ui/VentanaPrincipal.java`: persistencia activa por defecto, ubicación visible por tooltip
  y constructor que permite inyectar servicios aislados para pruebas.
- Los diagnósticos de arranque y pruebas GUI usan bases temporales para no tocar datos personales.

## Verificación

```bash
bash gui.sh compilar
java -cp 'out:lib/*' PruebaPersistencia
```

En Windows se usa `gui.ps1 -Modo compilar` y `java -cp 'out;lib/*' PruebaPersistencia`.
La suite también está incorporada en ambos lanzadores y en GitHub Actions.

Validación local: compilación para Java 21 sin advertencias y 33 pruebas de persistencia
aprobadas. Incluyen reinicio en procesos separados, ejecución desde otra carpeta,
rutas con espacios y acentos, recuperación de los cinco tipos, DDL/DML entre sesiones,
análisis sin cambios reales, fallos de ruta y aislamiento de Comparar. También aprobaron
las 628 comprobaciones existentes sin ventana: 661 en total.

Las rutas usan las API portables de Java. Las pruebas se ejecutaron en macOS; Windows
no se ejecutó localmente. Se añadieron diez comprobaciones GUI a `PruebaInterfaz` para
limpieza, análisis, consulta, reapertura y comparación en modo permanente. El ensayo de
la ventana nativa queda pendiente en una sesión de escritorio, por la limitación ya
observada en este entorno. Las pruebas de persistencia no crean la base real del usuario;
usan archivos temporales que se eliminan al terminar.
