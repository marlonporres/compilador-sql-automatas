param(
    [ValidateSet('menu', 'pruebas', 'valido', 'errores', 'todo')]
    [string]$Modo = 'menu'
)

$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try {
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
    $jdkHome = $env:JAVA_HOME
    if ($jdkHome -and (Test-Path -LiteralPath (Join-Path $jdkHome 'bin/javac.exe'))) {
        $compilador = Join-Path $jdkHome 'bin/javac.exe'
    } else {
        $comandoJava = Get-Command javac.exe -ErrorAction SilentlyContinue
        if (-not $comandoJava) {
            throw 'Necesitas instalar un JDK 21 o posterior y agregar su carpeta bin a PATH.'
        }
        $compilador = $comandoJava.Source
        # Oracle javapath puede ser un enlace al JDK instalado.
        $enlace = Get-Item -LiteralPath $compilador
        if ($enlace.Target) {
            $destino = @($enlace.Target)[0]
            if (-not [System.IO.Path]::IsPathRooted($destino)) {
                $destino = Join-Path $enlace.DirectoryName $destino
            }
            if (Test-Path -LiteralPath $destino) { $compilador = $destino }
        }
    }
    $ejecutor = Join-Path (Split-Path -Parent $compilador) 'java.exe'
    if (-not (Test-Path -LiteralPath $ejecutor)) { throw 'No se encuentra java.exe junto a javac.exe.' }
    Write-Host 'Compilando el proyecto con comprobaciones estrictas...'
    & $compilador -version
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el JDK.' }
    $fuentes = @(Get-ChildItem -LiteralPath 'src', 'tests' -Filter '*.java' -Recurse |
        Sort-Object FullName | ForEach-Object { $_.FullName })
    New-Item -ItemType Directory -Path 'out' -Force | Out-Null
    & $compilador '--release' '21' '-encoding' 'UTF-8' '-Xlint:all' '-Werror' '-d' 'out' @fuentes
    if ($LASTEXITCODE -ne 0) { throw 'La compilacion fallo. Corrige el error mostrado antes de demostrar.' }
    $opcionesJava = @('-Dfile.encoding=UTF-8', '-Dstdout.encoding=UTF-8',
        '-Dstderr.encoding=UTF-8', '-cp', 'out')

    function Probar-Proyecto {
        foreach ($clase in @('symbol_table.PruebaTablaSimbolos', 'PruebaFase1',
                'PruebaParserDDL', 'PruebaFase2', 'PruebaAnalizadorDML',
                'PruebaSemanticaDDL', 'PruebaSemanticaDML', 'PruebaDelimitadores')) {
            & $ejecutor @opcionesJava $clase
            if ($LASTEXITCODE -ne 0) { throw "La prueba $clase fallo." }
        }
        Write-Host 'Todas las pruebas pasaron.'
    }

    function Mostrar-Demo([string]$archivo, [int]$codigoEsperado, [bool]$resumen = $true) {
        $argumentos = @('Main', $archivo, '--fase2')
        if ($resumen) { $argumentos += '--resumen' }
        & $ejecutor @opcionesJava @argumentos
        if ($LASTEXITCODE -ne $codigoEsperado) {
            throw "Salida inesperada al analizar $archivo. Esperada: $codigoEsperado."
        }
        if ($codigoEsperado -eq 1) {
            Write-Host 'Los errores de este archivo son intencionales. La recuperacion es parte de la demostracion.'
        }
    }

    switch ($Modo) {
        'pruebas' { Probar-Proyecto }
        'valido' { Mostrar-Demo 'tests/demo_fase2_valido.sql' 0 }
        'errores' { Mostrar-Demo 'tests/demo_fase2_errores.sql' 1 }
        'todo' {
            Probar-Proyecto
            Mostrar-Demo 'tests/demo_fase2_valido.sql' 0
            Mostrar-Demo 'tests/demo_fase2_errores.sql' 1
        }
        'menu' {
            do {
                Write-Host ''
                Write-Host 'COMPILADOR SQL - DEMOSTRACION FASE 2'
                Write-Host '1. Ejecutar todas las pruebas'
                Write-Host '2. Mostrar script valido (resumen)'
                Write-Host '3. Mostrar errores y recuperacion'
                Write-Host '4. Mostrar script valido con tokens y tabla lexica'
                Write-Host '0. Salir'
                $eleccion = Read-Host 'Elige una opcion'
                switch ($eleccion) {
                    '1' { Probar-Proyecto }
                    '2' { Mostrar-Demo 'tests/demo_fase2_valido.sql' 0 }
                    '3' { Mostrar-Demo 'tests/demo_fase2_errores.sql' 1 }
                    '4' { Mostrar-Demo 'tests/demo_fase2_valido.sql' 0 $false }
                    '0' { }
                    default { Write-Host 'Elige un numero entre 0 y 4.' }
                }
            } while ($eleccion -ne '0')
        }
    }
    exit 0
} catch {
    Write-Host ("ERROR: " + $_.Exception.Message) -ForegroundColor Red
    exit 2
} finally {
    Pop-Location
}
