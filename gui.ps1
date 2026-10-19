param(
    [ValidateSet('gui', 'pruebas', 'demo', 'capturas')]
    [string]$Modo = 'gui'
)

$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try {
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
    $jdkProyecto = $env:JAVA_HOME
    $compiladorProyecto = $null
    if ($jdkProyecto -and (Test-Path -LiteralPath (Join-Path $jdkProyecto 'bin/javac.exe'))) {
        $compiladorProyecto = Join-Path $jdkProyecto 'bin/javac.exe'
    } else {
        $herramienta = Get-Command javac.exe -ErrorAction SilentlyContinue
        if (-not $herramienta) { throw 'Necesitas un JDK 21 o posterior con javac en PATH o JAVA_HOME.' }
        $compiladorProyecto = $herramienta.Source
        $archivoHerramienta = Get-Item -LiteralPath $compiladorProyecto
        if ($archivoHerramienta.Target) {
            $rutaDestino = @($archivoHerramienta.Target)[0]
            if (-not [System.IO.Path]::IsPathRooted($rutaDestino)) {
                $rutaDestino = Join-Path $archivoHerramienta.DirectoryName $rutaDestino
            }
            if (Test-Path -LiteralPath $rutaDestino) { $compiladorProyecto = $rutaDestino }
        }
    }
    $javaProyecto = Join-Path (Split-Path -Parent $compiladorProyecto) 'java.exe'
    if (-not (Test-Path -LiteralPath $javaProyecto)) { throw 'No se encontro java.exe junto a javac.exe.' }
    $driverProyecto = Join-Path $PSScriptRoot 'lib/h2-2.4.240.jar'
    if (-not (Test-Path -LiteralPath $driverProyecto)) {
        throw 'Falta lib/h2-2.4.240.jar. Recupera la carpeta completa del proyecto; consulta lib/README.md.'
    }
    $hashProyecto = (Get-FileHash -LiteralPath $driverProyecto -Algorithm SHA256).Hash
    if ($hashProyecto -ne '29B70E427CC1C40CDC376283ADBB0CC62853073797BB5FE5761F81FE73D57CE0') {
        throw 'El controlador H2 no coincide con la version verificada. Consulta lib/README.md.'
    }
    $temaProyecto = Join-Path $PSScriptRoot 'lib/flatlaf-3.6.2.jar'
    if (-not (Test-Path -LiteralPath $temaProyecto)) { throw 'Falta el tema lib/flatlaf-3.6.2.jar incluido en el repositorio.' }
    if ((Get-FileHash -LiteralPath $temaProyecto -Algorithm SHA256).Hash -ne
            '32C79DEE6D51341A3F8603598415841B4461FE535DE2C34BCC8EE40E91E72267') {
        throw 'El tema visual no coincide con la version verificada. Consulta lib/README.md.'
    }
    Write-Host 'Compilando para Java 21, sin advertencias...'
    $fuentesProyecto = @(Get-ChildItem -LiteralPath 'src', 'tests' -Filter '*.java' -Recurse |
        Sort-Object FullName | ForEach-Object { $_.FullName })
    New-Item -ItemType Directory -Path 'out' -Force | Out-Null
    & $compiladorProyecto '--release' '21' '-encoding' 'UTF-8' '-Xlint:all' '-Werror' '-d' 'out' @fuentesProyecto
    if ($LASTEXITCODE -ne 0) { throw 'La compilacion fallo. Revisa el primer error mostrado.' }
    $opcionesProyecto = @('-Dfile.encoding=UTF-8', '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8', '-cp', 'out;lib/*')
    switch ($Modo) {
        'gui' {
            Write-Host 'Abriendo SQL Studio. Mantiene esta consola disponible para diagnosticos.'
            & $javaProyecto @opcionesProyecto 'ui.AplicacionGUI'
            if ($LASTEXITCODE -ne 0) { throw 'La interfaz termino con un error.' }
        }
        'pruebas' {
            foreach ($suiteProyecto in @('symbol_table.PruebaTablaSimbolos', 'PruebaFase1',
                    'PruebaParserDDL', 'PruebaFase2', 'PruebaAnalizadorDML', 'PruebaSemanticaDDL',
                    'PruebaSemanticaDML', 'PruebaDelimitadores', 'PruebaEjecucionSQL',
                    'PruebaPanelesGUI', 'PruebaInterfaz')) {
                & $javaProyecto @opcionesProyecto $suiteProyecto
                if ($LASTEXITCODE -ne 0) { throw "Fallo la suite $suiteProyecto." }
            }
            Write-Host 'VERIFICACION COMPLETA: todas las pruebas pasaron.' -ForegroundColor Green
        }
        'demo' {
            & $javaProyecto @opcionesProyecto 'cli.Fase3CLI' 'tests/demo_fase3.sql' 'tests/demo_fase3_referencia.sql'
            if ($LASTEXITCODE -ne 0) { throw 'La comparacion de demostracion fallo.' }
        }
        'capturas' {
            & $javaProyecto @opcionesProyecto 'PruebaInterfaz' '--capturas'
            if ($LASTEXITCODE -ne 0) { throw 'La verificacion visual fallo.' }
        }
    }
    exit 0
} catch {
    Write-Host ('ERROR: ' + $_.Exception.Message) -ForegroundColor Red
    exit 2
} finally {
    Pop-Location
}
