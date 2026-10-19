# compilador-sql-automatas
Proyecto Final - Autómatas y Lenguajes Formales (Java)

## Proyecto final: interfaz y ejecución SQL

En Windows, abre **ABRIR_GUI.cmd**. Compila el proyecto completo y abre **SQL Studio**, con editor,
tokens, símbolos, errores, ejecución real y comparación contra SQL de referencia.
Los dos controladores incluidos en `lib/` permiten demostrarlo sin Internet
ni instalar un servidor. Requiere **JDK 21 o posterior**.

1. Pulsa **Ejemplo completo** y **Analizar**.
2. Pulsa **Ejecutar**; en **SQL y resultados**, selecciona una consulta anterior
   al TRUNCAR para ver registros.
3. Pulsa **Comparar SQL**: el ejemplo de 19 sentencias debe aprobarse.
4. Prueba **Ejemplo con errores**: la interfaz señala los errores y bloquea Ejecutar.

**VERIFICAR_FASE3.cmd** ejecuta las once suites: **536 comprobaciones, cero fallos**.
Cada ejecución usa una base temporal nueva; los scripts incluyen sus declaraciones.

- [Guía del integrante 1 y plan de once días](docs/guia_integrante1.md).
- [Fase 3: arquitectura, contratos y alcance](docs/fase3.md).
- [Auditoría y evidencia de verificación](docs/auditoria_final.md).
- [Dependencias incluidas y huellas](lib/README.md).

Desde PowerShell también puedes usar `powershell -NoProfile -ExecutionPolicy Bypass -File .\gui.ps1 -Modo pruebas`.
El modo `demo` verifica la comparación sin abrir la ventana; `capturas` prueba
sus controles y guarda imágenes en `output/fase3/capturas/`.

## Abrir desde Visual Studio Code

Necesitas **JDK 21 o posterior**, **Extension Pack for Java** de Microsoft y
la carpeta completa del repositorio, incluyendo `lib/`. No necesitas Maven,
Gradle ni un servidor de base de datos. VS Code utiliza los JAR incluidos.

1. Haz doble clic en **ABRIR_EN_VSCODE.cmd**. Abre el área de trabajo
   `SQLStudio.code-workspace`, con la carpeta y las librerías correctas.
2. Abre **Extensiones** con **Ctrl+Shift+X** y verifica que esté instalado
   **Extension Pack for Java**. Espera a que Java termine de cargar el proyecto.
3. Abre **Ejecutar y depurar** con **Ctrl+Shift+D**. Selecciona
   **SQL Studio - Interfaz grafica** y pulsa **F5**.
4. En la ventana, pulsa **Ejemplo completo → Analizar → Ejecutar**.
   Para terminar, cierra la ventana o detén la depuración.

Antes de F5 se compilan todas las clases con el JDK detectado y se incluyen
las bibliotecas de `lib/`; el inicio no depende de clases incompletas del editor.
La entrada gráfica es `src/ui/AplicacionGUI.java`. **Main sin argumentos
también abre la interfaz**. Con un archivo, `Main` conserva la versión de consola.
Para esa versión, selecciona **Fase 2 - Consola con ejemplo valido** en el
mismo menú; ya incluye el archivo y los argumentos necesarios.

Si Java no reconoce las librerías después de actualizar, abre **Ctrl+Shift+P**,
ejecuta **Java: Clean Java Language Server Workspace** y acepta reiniciar.
Para revisar el JDK, usa **Java: Configure Java Runtime** y elige 21 o posterior.
También puedes abrir `ABRIR_GUI.cmd` desde el Explorador de Windows.

**Si aparece `Estilo cannot be resolved` o `VentanaPrincipal cannot be resolved`:**
VS Code está ejecutando un archivo aislado, habitualmente desde `~` y con clases
en `AppData/Local/Temp/vscodesws...`. Cierra esa ejecución y usa
**ABRIR_EN_VSCODE.cmd → SQL Studio - Interfaz grafica → F5**.
Abrir únicamente un archivo `.java` no carga el proyecto ni su configuración.

## Aplicación preparada para presentar

La versión autónoma de Windows está en **output/Escritorio/SQLStudio/SQLStudio.exe**.
Incluye su propio Java: abre el ejecutable con doble clic. Para trasladarla,
copia **toda la carpeta SQLStudio**, incluidos `app/` y `runtime/`.
No necesita VS Code, NetBeans, Internet ni instalar Java en el equipo de exposición.

`powershell -NoProfile -ExecutionPolicy Bypass -File .\gui.ps1 -Modo empaquetar`
genera **output/SQLStudio/**. Abre **ABRIR.cmd** dentro de esa carpeta: ejecuta
el JAR ya compilado con sus dependencias y ejemplos. Puedes copiar la carpeta
completa a otro equipo con Java 21 o posterior; no requiere VS Code ni compilar.
El JAR conserva sus ejemplos y funciona aunque la terminal esté en otra carpeta.

Para generar de nuevo el ejecutable de Windows, prepara primero el JAR y usa
`powershell -NoProfile -ExecutionPolicy Bypass -File scripts/CrearEjecutable.ps1 -Verificar`.
Requiere un JDK con `jpackage`. Si ya existe la carpeta de salida, se conserva;
usa `-Destino output/Escritorio-nueva` para crear otra versión.

`tests/PruebaArranque.ps1` verifica detección del JDK, límites de espera y paquete.
`tests/PruebaVSCode.ps1` comprueba el depurador real en una instancia de prueba
de VS Code con perfil separado. Esta última es opcional y requiere las extensiones
de Java instaladas. No altera la configuración personal del editor.

## Demostración de fase 2

En Windows, haga doble clic en **DEMOSTRAR.cmd**. Compila el proyecto y abre
un menú para ejecutar las pruebas, mostrar un script válido, mostrar errores
y recuperación, o ver los tokens. Requiere un **JDK 21 o posterior**.

Desde PowerShell en la raíz, también puede ejecutar:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\demo.ps1 -Modo todo
```

El recorrido completo es archivo UTF-8 → scanner → parser DDL/DML →
semántica → catálogo de tablas/columnas y tabla de errores. La gramática
cubre los nueve comandos en español, cinco tipos y condiciones con Y/O/NO.
Las consultas se validan; su ejecución e interfaz corresponden a fase 3.

Use [el instructivo de demostración](docs/instructivo_demostracion.md) y
[la documentación técnica con diagramas](docs/fase2.md).

## Fase 1

Analizador léxico manual de SQL en español y tabla de símbolos global.
El flujo es archivo UTF-8 → `ScannerSQL.analizar` → `List<Token>` →
`TablaSimbolos.registrarTodos`. `Main` imprime tokens, errores y símbolos.
Esta fase no valida la sintaxis ni ejecuta SQL; las categorías y tipos de los
identificadores permanecen en `DESCONOCIDA` y `DESCONOCIDO`.

## Compilación y ejecución

Requiere **JDK 21 o posterior** y compila para Java 21. Desde la raíz:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\gui.ps1 -Modo compilar
java -cp out PruebaParserDDL
java -cp out Main tests/demo_fase2_valido.sql --fase2 --resumen
java -cp out Main tests/demo_fase2_errores.sql --fase2 --resumen
```

Los archivos históricos `tests/test_ddl.sql` y `tests/test_dml.sql` contienen
una sección válida y otra con **4 errores léxicos
intencionales**. Por ello, ejecutarlos completos debe reportar 4 errores y salir
con código `1`. La cadena sin cerrar consume hasta el final del archivo.

Códigos de salida de `Main`: `0` sin errores o con `--ayuda`, `1` con errores del lenguaje,
`2` por argumentos incorrectos, problemas de lectura o fallo de ejecución por consola.
Sin argumentos abre la GUI y necesita `out;lib/*` en el classpath en Windows.

`Main archivo.sql` conserva el modo léxico de fase 1. Agregue `--fase2`
para el análisis completo; `--resumen` omite la impresión de tokens y símbolos
léxicos. El ejemplo válido tiene 15 sentencias válidas y el catálogo final
clientes; el negativo tiene 1 error léxico, 1 sintáctico y 6 semánticos.

## Pruebas

Las ocho suites de fases 1 y 2 reúnen **396 comprobaciones**. En Windows, la opción 1 de
DEMOSTRAR.cmd las ejecuta todas. GitHub también las verifica con Java 21 en
cada publicación a `main` y en los pull requests dirigidos a esa rama.

Después de compilar:

```sh
java -cp out symbol_table.PruebaTablaSimbolos
java -cp out PruebaFase1
java -cp out PruebaParserDDL
java -cp out PruebaFase2
java -cp out PruebaAnalizadorDML
java -cp out PruebaSemanticaDDL
java -cp out PruebaSemanticaDML
java -cp out PruebaDelimitadores
```

- Tabla de símbolos: 50 comprobaciones aisladas.
- Fase 1: 85 comprobaciones de reservadas, identificadores, literales, operadores,
  errores, comentarios, posiciones, reutilización del scanner e integración.
  Ejecuta `Main` en procesos separados con DDL/DML válidos, archivos con errores,
  archivos vacíos, argumentos incorrectos y archivos ilegibles. Usa archivos temporales que elimina
  al terminar; no requiere dependencias ni generadores.

- Parser DDL: 10 comprobaciones aisladas.
- Fase 2: 80 comprobaciones de gramática, precedencia, ámbitos, tipos,
  atomicidad, recuperación, demos y códigos de salida reales.
- Las suites DML y semánticas del equipo comprueban sus APIs compatibles
  con el recorrido completo; se conservan los aportes de las cuatro personas.
- Delimitadores: 54 comprobaciones de `;`, comas y paréntesis ausentes o
  incorrectos, recuperación y delimitadores dentro de cadenas y comentarios.

Los mensajes identifican la falta de coma en listas de columnas, valores y
asignaciones. Las sentencias inválidas no pasan al análisis semántico.
La recuperación continúa después del siguiente `;` o llega al fin del archivo.

Todas deben terminar con código `0` y `Fallidas: 0`; un fallo produce
código `1`. Ejecute desde la raíz para encontrar `tests/`.

## Documentación

- [Especificación léxica](docs/especificacion_lexica.md)
- [Tabla de símbolos](docs/tabla_simbolos.md)
- [Analizador sintáctico DDL](docs/parser_ddl.md)
- [Fase 2 y diagramas](docs/fase2.md)
- [Símbolos y semántica DDL del equipo](docs/semantica_ddl.md)
- [Semántica DML y errores del equipo](docs/semantica_dml_errores.md)
- [Instructivo de demostración](docs/instructivo_demostracion.md)
- [Fase 3 e interfaz gráfica](docs/fase3.md)
- [Guía del integrante 1](docs/guia_integrante1.md)
