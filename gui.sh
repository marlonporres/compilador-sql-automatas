#!/bin/bash
# Lanzador para macOS y Linux. Windows conserva gui.ps1.
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
modo="${1:-gui}"
case "$modo" in
    gui|compilar|pruebas|demo|capturas) ;;
    *) echo 'Uso: bash gui.sh [gui|compilar|pruebas|demo|capturas]' >&2; exit 2 ;;
esac
if [ "$#" -gt 1 ]; then
    echo 'Indica solo un modo de ejecución.' >&2
    exit 2
fi

# JAVA_HOME explícito tiene prioridad; macOS puede localizar su JDK instalado.
jdk_proyecto="${JAVA_HOME:-}"
if [ -z "$jdk_proyecto" ] && [ "$(uname -s)" = Darwin ]; then
    jdk_proyecto="$(/usr/libexec/java_home -v '21+' 2>/dev/null || true)"
fi
if [ -n "$jdk_proyecto" ]; then
    java_proyecto="$jdk_proyecto/bin/java"
    javac_proyecto="$jdk_proyecto/bin/javac"
else
    java_proyecto="$(command -v java || true)"
    javac_proyecto="$(command -v javac || true)"
fi
if [ ! -x "$java_proyecto" ] || [ ! -x "$javac_proyecto" ]; then
    echo 'Se necesita un JDK 21 o posterior. Revisa JAVA_HOME o el PATH.' >&2
    exit 2
fi
"$javac_proyecto" -version

for biblioteca in lib/h2-2.4.240.jar lib/flatlaf-3.6.2.jar; do
    if [ ! -f "$biblioteca" ]; then
        echo "Falta $biblioteca. Recupera la carpeta completa del repositorio." >&2
        exit 2
    fi
done

mkdir -p out
# El archivo de argumentos conserva los espacios de las rutas y evita límites
# de longitud. No se reutiliza fuentes.txt, generado por el lanzador de Windows.
find src tests -name '*.java' -type f | LC_ALL=C sort | while IFS= read -r fuente; do
    printf '"%s"\n' "$fuente"
done > out/fuentes-unix.txt
echo 'Compilando el proyecto completo para Java 21...'
"$javac_proyecto" --release 21 -g -encoding UTF-8 -Xlint:all -Werror \
    -cp 'lib/*' -d out @out/fuentes-unix.txt

opciones=(-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp 'out:lib/*')
case "$modo" in
    compilar) echo 'COMPILACIÓN CORRECTA: lista para F5 o bash gui.sh.' ;;
    gui) exec "$java_proyecto" "${opciones[@]}" ui.AplicacionGUI ;;
    demo) exec "$java_proyecto" "${opciones[@]}" cli.Fase3CLI \
        tests/demo_fase3.sql tests/demo_fase3_referencia.sql ;;
    capturas) exec "$java_proyecto" "${opciones[@]}" PruebaInterfaz --capturas ;;
    pruebas)
        for suite in symbol_table.PruebaTablaSimbolos PruebaFase1 PruebaParserDDL \
            PruebaFase2 PruebaAnalizadorDML PruebaSemanticaDDL PruebaSemanticaDML \
            PruebaDelimitadores PruebaEjecucionSQL PruebaPanelesGUI PruebaIntegrante2 PruebaPersistencia PruebaInterfaz; do
            "$java_proyecto" "${opciones[@]}" "$suite"
        done
        echo 'VERIFICACIÓN COMPLETA: todas las pruebas pasaron.'
        ;;
esac
