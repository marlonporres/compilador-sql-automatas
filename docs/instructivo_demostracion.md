# Instructivo para demostrar la fase 2

## 1 Antes de la clase

Abra esta carpeta completa del proyecto en su computadora. Necesita un
**JDK 21 o posterior**, con `java` y `javac` disponibles. No hace falta
conexión a Internet, bibliotecas adicionales ni un servidor de base de datos.

Haga doble clic en **DEMOSTRAR.cmd**, en la raíz del proyecto. El programa
compila primero y abre un menú. Si todo está bien verá las opciones 1 a 4.
Este archivo utiliza PowerShell con una política temporal para esa ejecución;
no modifica la política permanente del equipo.

Si prefiere una terminal PowerShell, ábrala en esta carpeta y ejecute:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\demo.ps1
```

**Ensaye hoy** las opciones 1, 2 y 3. Mantenga el proyecto completo disponible
en el mismo equipo que usará para presentar.

## 2 Demostración recomendada de cinco a siete minutos

### Paso A Explique el objetivo

Diga: «Nuestro compilador recibe un script SQL en español. El análisis léxico
separa tokens, el sintáctico verifica su estructura y el semántico valida
tablas, columnas y tipos. La fase 2 también muestra símbolos y errores».

Puede mostrar el diagrama al principio de [fase2.md](fase2.md).

### Paso B Muestre las pruebas

Seleccione **1 Ejecutar todas las pruebas**. Al final de cada suite debe
aparecer **Fallidas: 0**, y al terminar verá **Todas las pruebas pasaron**.
Las ocho suites cubren fase 1, símbolos, los módulos del equipo, la fase 2
completa y delimitadores ausentes. Todas deben mostrar cero fallos.

Explique: «Además del ejemplo, tenemos pruebas de entradas válidas, errores,
recuperación, posiciones y conservación del catálogo ante DDL inválido».

### Paso C Muestre el script válido

Abra [demo_fase2_valido.sql](../tests/demo_fase2_valido.sql) en el editor.
El archivo crea tablas, inserta, consulta, actualiza y borra; agrega, modifica
y quita una columna; renombra, trunca y elimina una tabla.

Seleccione **2 Mostrar script válido** en el menú.
Debe ver:

- **15 sentencias reconocidas y 15 válidas semánticamente**.
- **0 errores léxicos, 0 sintácticos y 0 semánticos**.
- Catálogo final con la tabla **clientes** y cinco columnas.
- **RESULTADO: ANALISIS COMPLETO SIN ERRORES**.

Explique dos cambios concretos: «usuarios se renombra a clientes y productos
se elimina. Por eso el catálogo final contiene clientes. TRUNCAR conserva
el esquema. En esta fase no almacenamos filas».

La condición del ejemplo muestra `NO`, `Y`, `O` y paréntesis. Explique
que el parser respeta la precedencia y la semántica comprueba sus operandos.

### Paso D Muestre errores y recuperación

Abra [demo_fase2_errores.sql](../tests/demo_fase2_errores.sql).
Seleccione **3 Mostrar errores y recuperación**.
El resultado esperado es:

- **1 error léxico** por el carácter `@`.
- **1 error sintáctico** porque `dato` no tiene tipo.
- **6 errores semánticos**: columna desconocida, cantidad de valores,
  asignación de texto a ENTERO, tabla duplicada, renombrado a un nombre
  ocupado y comparación incompatible.
- **Tabla recuperada** presente en el catálogo, aunque hubo errores antes.
- **RESULTADO: SE ENCONTRARON ERRORES**.

Explique: «Cada diagnóstico indica fase, código, mensaje, lexema, línea y
columna. Se descarta la sentencia inválida y se continúa en el siguiente
punto y coma. La tabla recuperada demuestra esa recuperación».

La salida 1 en este ejemplo es intencional. El menú confirma que encontró
los errores esperados; no significa que la demostración falló.

### Paso E Muestre el léxico si se lo piden

Seleccione **4 Mostrar script válido con tokens y tabla léxica**.
Esto agrega los tokens y los identificadores observados antes del resultado
de fase 2. La tabla léxica muestra apariciones; el catálogo semántico muestra
declaraciones válidas con categoría, tipo y ámbito.

Termine señalando que la interfaz gráfica y la ejecución contra SQL son
objetivos de la fase 3.

## 3 Probar un script propio

Cree un archivo UTF-8 con extensión `.sql`. Declare la tabla antes de usarla
porque cada ejecución comienza con un catálogo vacío.

```sql
CREAR TABLA alumnos (id ENTERO, nombre TEXTO, activo BOOLEANO);
INSERTAR EN alumnos VALORES (1, 'Ana', VERDADERO);
SELECCIONAR nombre DESDE alumnos DONDE id >= 1 Y activo = VERDADERO;
ALTERAR TABLA alumnos AGREGAR correo TEXTO;
RENOMBRAR TABLA alumnos A estudiantes;
SELECCIONAR * DESDE estudiantes;
```

Después de compilar, ejecute:

```powershell
java '-Dstdout.encoding=UTF-8' -cp out Main mi_script.sql --fase2 --resumen
```

Quite `--resumen` para mostrar también tokens y símbolos léxicos.
Los comandos terminan en `;`; las cadenas usan comillas simples y las fechas
son cadenas como `'2026-10-03'`. Los booleanos se escriben sin comillas.
`ALTERAR TABLA tabla;` y `RENOMBRAR TABLA tabla;` son incompletos en fase 2.

## 4 Comandos sin menú

El script automático cambia a la raíz del proyecto, usa el JDK detectado y
compila todas las fuentes, evitando las omisiones del comando manual.

```powershell
# Compilar y ejecutar todas las pruebas
powershell -NoProfile -ExecutionPolicy Bypass -File .\demo.ps1 -Modo pruebas

# Compilar y demostrar ambos archivos
powershell -NoProfile -ExecutionPolicy Bypass -File .\demo.ps1 -Modo todo

# Un solo ejemplo
powershell -NoProfile -ExecutionPolicy Bypass -File .\demo.ps1 -Modo valido
powershell -NoProfile -ExecutionPolicy Bypass -File .\demo.ps1 -Modo errores
```

Los códigos de Main son **0** sin errores, **1** con errores del lenguaje y
**2** por argumentos incorrectos o problemas de lectura/ejecución.
El script de demostración trata el código 1 del ejemplo negativo como esperado.

## 5 Si aparece un problema

| Mensaje o situación | Qué revisar |
|---|---|
| No se reconoce java o javac | Instale un JDK 21 o posterior y agregue bin a PATH; abra de nuevo la terminal |
| La compilación falla con release 21 | El compilador detectado es anterior a Java 21; revise JAVA_HOME |
| Una clase no se encuentra | Abra DEMOSTRAR.cmd para recompilar; no reutilice clases antiguas |
| Error al leer archivo | Ejecute desde la raíz y compruebe el nombre o use una ruta entre comillas |
| Acentos ilegibles | Use el menú, que configura UTF-8; conserve los archivos SQL en UTF-8 |
| Un script DML indica tabla inexistente | Incluya CREAR TABLA en ese mismo archivo antes del DML |
| El ejemplo negativo termina con errores | Es el resultado previsto; muestre su tabla de errores y la recuperación |

Los archivos históricos `test_ddl.sql` y `test_dml.sql` son pruebas léxicas
de fase 1 y contienen errores intencionales. Para esta presentación use los
dos archivos `demo_fase2_...` que incluyen la sintaxis y declaraciones completas.

## 6 Preguntas habituales de la explicación

**¿Qué distingue sintaxis de semántica?** Una sentencia puede tener la forma
correcta y aun así usar una tabla desconocida o asignar texto a una columna
ENTERO. La estructura se valida primero; los nombres y tipos después.

**¿Qué evita que una columna id de dos tablas sea la misma?** Cada tabla tiene
su ámbito de columnas en el catálogo semántico.

**¿Por qué un error no detiene todo?** El parser usa el punto y coma como
delimitador de recuperación. Solo las sentencias con sintaxis válida se
envían a semántica; un DDL inválido conserva el esquema anterior.

**¿Se ejecutó INSERTAR o SELECCIONAR?** En esta fase se valida el lenguaje.
La ejecución de consultas y la interfaz corresponden a fase 3.
