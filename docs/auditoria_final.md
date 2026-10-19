# Auditoría del proyecto final — 19 de octubre de 2026

## Resultado

La revisión inicial de `main` encontró la fase 2 funcional por consola y
**390 comprobaciones aprobadas**, pero sin interfaz gráfica, motor SQL ni
comparación de ejecuciones. No había ramas de fase 3 ni PR abiertos en GitHub
al consultar el repositorio el 19/10/2026.

Se completó la fase 3 sobre esa base. La verificación final local reúne
**524 comprobaciones aprobadas, cero fallos**, en once suites. La compilación
usa `--release 21 -encoding UTF-8 -Xlint:all -Werror`; en este equipo se ejecutó
con JDK 25.0.3. El flujo de GitHub verifica además el proyecto con JDK 21.

## Requisitos y evidencia

Fuente: enunciado «Proyecto Autómatas y Lenguajes Formales», páginas 3 y 4.
Los requisitos provienen del enunciado; Swing, FlatLaf, H2 y el modo de
demostración con bases temporales son decisiones del proyecto.

| Requisito de fase 3 | Estado y evidencia |
|---|---|
| Interfaz para ingresar cadenas | Editor Swing con numeración y resaltado, apertura UTF-8 y botones. `PruebaInterfaz` prueba las acciones reales. |
| Mostrar lexemas, tokens y elementos del análisis | Tablas de tokens, catálogo semántico e identificadores léxicos. `PruebaPanelesGUI` verifica sus datos. |
| Mostrar errores | Tabla con posición, navegación al editor y errores del motor. Pruebas de selección tras ordenar y limpiar. |
| Validar scripts | Léxico, sintaxis y semántica antes de ejecutar; editar invalida el resultado. Un script inválido no abre una base ni ejecuta sentencias. |
| Ejecución de scripts frente a SQL | Nueve comandos traducidos desde el AST, JDBC con parámetros y H2 real. Comparación con SQL independiente en otra base. |
| Documentación y diagramas por fase | Documentación previa, `fase3.md`, guía del integrante 1, diagramas y ejemplos reproducibles. |

El ejemplo de fase 3 ejecuta **19 sentencias** y coincide con el SQL de
referencia en consultas, filas, esquema y datos finales. La comparación ignora
el orden de las filas y conserva sus repeticiones; también detecta diferencias
de esquema y datos aunque no exista un SELECT final.

## Problemas corregidos

- Faltaba la fase 3 completa: se añadieron GUI, ejecución y comparación real.
- Una cadena SQL con apóstrofe escapado se dividía en dos literales. Ahora
  `'O''Brien'` produce un solo token `O'Brien`, con posiciones correctas.
- Las líneas largas podían envolver el editor y desalinear su numeración.
  Ahora tienen desplazamiento horizontal y posiciones verificadas.
- Cambiar código o referencia podía conservar un estado visual anterior.
  Se invalidan análisis y comparaciones según el contenido modificado.
- Una selección antigua de JTable generaba un aviso al insertar un error.
  La limpieza reinicia selección e índices y evita navegación durante el cambio.
- El procesamiento de referencias largas copiaba cadenas repetidamente y un
  patrón de expresiones regulares desbordaba la pila. Se reemplazó por recorridos
  lineales, con pruebas de cadenas largas, BOM y saltos de línea.
- Errores del traductor y SQL de referencia necesitaban mensajes y posiciones
  verificables. Se conservan por sentencia y se recuperan los controles de la GUI.

## Conteo reproducible

| Suite | Comprobaciones | Fallos |
|---|---:|---:|
| Tabla de símbolos | 50 | 0 |
| Fase 1 | 82 | 0 |
| Parser DDL | 10 | 0 |
| Fase 2 | 80 | 0 |
| Analizador DML | 15 | 0 |
| Semántica DDL | 70 | 0 |
| Semántica DML | 32 | 0 |
| Delimitadores | 54 | 0 |
| Ejecución SQL | 63 | 0 |
| Paneles GUI | 28 | 0 |
| Ventana y controles | 40 | 0 |
| **Total** | **524** | **0** |

Windows: `VERIFICAR_FASE3.cmd`. Equivalente: `powershell -NoProfile
-ExecutionPolicy Bypass -File .\gui.ps1 -Modo pruebas` en una sola línea.
La comprobación gráfica requiere escritorio; GitHub usa Xvfb en Linux.

Las capturas de la aplicación real se generan con `gui.ps1 -Modo capturas`:
análisis, consultas/comparación, errores y tamaño mínimo de ventana.
Los archivos de la presentación anterior permanecen intactos.

## Alcance de la entrega

Cada script usa una base nueva en memoria y debe declarar sus tablas. No hay
persistencia entre clics. El SQL de referencia admite las operaciones básicas
del proyecto; no es una consola administrativa general. Los valores numéricos
de ejecución tienen un límite explícito de 1000 dígitos significativos; un
exceso produce un diagnóstico, sin redondeo silencioso. Los `?` del SQL mostrado
son parámetros: los valores se enlazan por separado.

La rúbrica del PDF asigna **50 % funcionamiento, 20 % documentación y 30 %
explicación**. La evidencia cubre funcionamiento y documentación; los cuatro
integrantes deben practicar y explicar su aporte. La fecha publicada es
**24/10/2026**. El plan de once días solicitado llega al **29/10/2026**, por lo
que sus días posteriores al 24 requieren extensión confirmada.
