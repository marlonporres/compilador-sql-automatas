# SQL Studio: abrir y comprobar

> **Actualización: almacenamiento permanente.** La interfaz guarda ahora tablas y filas en la carpeta del usuario `SQLStudio/datos`. Analizar y Limpiar conservan los datos. Comparar SQL y las pruebas siguen usando bases temporales. Consulta [persistencia y ejemplo entre sesiones](docs/persistencia.md); las indicaciones anteriores sobre una base nueva en cada ejecución normal ya no aplican.

## En macOS o Linux

Abre una terminal en la carpeta del proyecto y ejecuta **`bash gui.sh`**.
Requiere un JDK 21 o posterior; no necesitas PowerShell.
En VS Code, abre **SQLStudio.code-workspace**, instala **Extension Pack for Java**
y usa **SQL Studio - Interfaz grafica → F5**. La compilación detecta el sistema.
Para comprobar sin abrir la ventana: **`bash gui.sh demo`**.

## Para usar la aplicación

**Aplicación lista, sin compilar ni instalar Java:** abre
**output/Escritorio/SQLStudio/SQLStudio.exe**. Incluye su propio entorno Java.

**Si modificaste el código**, usa el lanzador del proyecto para compilar tus cambios:

1. Haz doble clic en **ABRIR_GUI.cmd**, en la carpeta del proyecto.
2. Espera a que termine de compilar. No hace falta abrir un archivo Java.
3. En la ventana pulsa **Analizar → Ejecutar → Comparar SQL**.
4. Debe indicar **Comparación aprobada**.
5. En **SQL y resultados**, selecciona una consulta anterior a `TRUNCAR`
   para ver registros. La consulta final está vacía porque el ejemplo vacía la tabla.

Requiere Java 21 o posterior. Se detecta una instalación válida automáticamente;
las librerías de base de datos y del tema visual ya están incluidas.

## Para trabajar en VS Code

1. Abre **ABRIR_EN_VSCODE.cmd**.
2. Presiona **Ctrl+Shift+D**.
3. Selecciona **SQL Studio - Interfaz grafica** y presiona **F5**.

Esto abre el proyecto completo y compila sus clases antes de iniciar.
**No ejecutes un `.java` desde Archivos sueltos:** VS Code no encontrará
las clases del resto del proyecto. `Main` sin argumentos también abre la ventana;
con argumentos conserva las pruebas por consola de las fases anteriores.

## Para llevarla a la exposición

La carpeta **output/Escritorio/SQLStudio** contiene el ejecutable autónomo de Windows.
Cópiala completa, incluidos `app/` y `runtime/`, y abre **SQLStudio.exe**.
No requiere Java instalado ni un editor. Para regenerarla consulta el README.

Como alternativa, la carpeta **output/SQLStudio** contiene la versión JAR compilada.
Cópiala completa y abre su **ABRIR.cmd**; requiere Java 21 o posterior.
Ninguna de las dos versiones necesita Internet, VS Code, NetBeans ni un servidor de base de datos.
Para volver a prepararla, usa `gui.ps1 -Modo empaquetar`.

## Para comprobar el funcionamiento

Abre **VERIFICAR_FASE3.cmd**. Todas las suites deben terminar con
**Fallidas: 0** y debe aparecer **VERIFICACION COMPLETA**.

Después de verla funcionar, sigue [la guía del integrante 1](docs/guia_integrante1.md).
