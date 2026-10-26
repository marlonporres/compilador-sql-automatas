param(
    [ValidateSet('gui', 'compilar', 'empaquetar', 'pruebas', 'demo', 'capturas')]
    [string]$Modo = 'gui'
)

$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try {
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
    . (Join-Path $PSScriptRoot 'scripts/JavaProyecto.ps1')
    $jdkProyecto = Resolve-JdkProyecto
    Write-Host ('JDK {0}: {1}' -f $jdkProyecto.Version, $jdkProyecto.Directorio)
    $dependenciasProyecto = @{
        'h2-2.4.240.jar' = '29B70E427CC1C40CDC376283ADBB0CC62853073797BB5FE5761F81FE73D57CE0'
        'flatlaf-3.6.2.jar' = '32C79DEE6D51341A3F8603598415841B4461FE535DE2C34BCC8EE40E91E72267'
    }
    foreach ($nombreProyecto in $dependenciasProyecto.Keys) {
        $bibliotecaProyecto = Join-Path $PSScriptRoot ('lib/' + $nombreProyecto)
        if (-not (Test-Path -LiteralPath $bibliotecaProyecto)) {
            throw "Falta lib/$nombreProyecto. Recupera la carpeta completa del repositorio."
        }
        if ((Get-FileHash -LiteralPath $bibliotecaProyecto -Algorithm SHA256).Hash -ne
                $dependenciasProyecto[$nombreProyecto]) {
            throw "lib/$nombreProyecto no coincide con la version verificada. Consulta lib/README.md."
        }
    }
    Write-Host 'Compilando el proyecto completo para Java 21...'
    $fuentesProyecto = @(Get-ChildItem -LiteralPath 'src', 'tests' -Filter '*.java' -Recurse |
        Sort-Object FullName | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    $salidaProyecto = Join-Path $PSScriptRoot 'out'
    New-Item -ItemType Directory -Path $salidaProyecto -Force | Out-Null
    $listaProyecto = Join-Path $salidaProyecto 'fuentes.txt'
    [System.IO.File]::WriteAllLines($listaProyecto, $fuentesProyecto, [System.Text.UTF8Encoding]::new($false))
    & $jdkProyecto.Javac '--release' '21' '-g' '-encoding' 'UTF-8' '-Xlint:all' '-Werror' '-cp' 'lib/*' '-d' $salidaProyecto ('@' + $listaProyecto)
    if ($LASTEXITCODE -ne 0) { throw 'La compilacion fallo. No se iniciara una version anterior ni incompleta; revisa el primer error mostrado.' }
    $opcionesProyecto = @('-Dfile.encoding=UTF-8', '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8', '-cp', 'out;lib/*')
    switch ($Modo) {
        'compilar' { Write-Host 'COMPILACION CORRECTA: lista para F5 o ABRIR_GUI.cmd.' -ForegroundColor Green }
        'gui' {
            Write-Host 'Abriendo SQL Studio. Esta consola conserva los diagnosticos.'
            & $jdkProyecto.Java @opcionesProyecto 'ui.AplicacionGUI'
            if ($LASTEXITCODE -ne 0) { throw 'La interfaz termino con un error.' }
        }
        'empaquetar' {
            $paqueteProyecto = Join-Path $PSScriptRoot 'output/SQLStudio'
            foreach ($subcarpetaProyecto in @('', 'lib', 'tests', 'scripts')) {
                New-Item -ItemType Directory -Path (Join-Path $paqueteProyecto $subcarpetaProyecto) -Force | Out-Null
            }
            foreach ($nombreProyecto in $dependenciasProyecto.Keys) {
                Copy-Item -LiteralPath (Join-Path $PSScriptRoot ('lib/' + $nombreProyecto)) -Destination (Join-Path $paqueteProyecto 'lib') -Force
            }
            foreach ($ejemploProyecto in @('demo_fase3.sql', 'demo_fase3_referencia.sql', 'demo_fase2_errores.sql')) {
                Copy-Item -LiteralPath (Join-Path $PSScriptRoot ('tests/' + $ejemploProyecto)) -Destination (Join-Path $paqueteProyecto 'tests') -Force
            }
            Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'scripts/JavaProyecto.ps1') -Destination (Join-Path $paqueteProyecto 'scripts') -Force
            Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'scripts/AbrirPaquete.ps1') -Destination (Join-Path $paqueteProyecto 'scripts') -Force
            $manifiestoProyecto = Join-Path $salidaProyecto 'MANIFEST.MF'
            [System.IO.File]::WriteAllText($manifiestoProyecto, "Manifest-Version: 1.0`r`nMain-Class: ui.AplicacionGUI`r`nClass-Path: lib/h2-2.4.240.jar lib/flatlaf-3.6.2.jar`r`n`r`n", [System.Text.UTF8Encoding]::new($false))
            & $jdkProyecto.Jar ('-J-Djava.io.tmpdir=' + $paqueteProyecto) '--create' '--file' (Join-Path $paqueteProyecto 'SQLStudio.jar') '--manifest' $manifiestoProyecto '-C' $salidaProyecto '.' '-C' $PSScriptRoot 'tests/demo_fase3.sql' '-C' $PSScriptRoot 'tests/demo_fase3_referencia.sql' '-C' $PSScriptRoot 'tests/demo_fase2_errores.sql'
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar SQLStudio.jar.' }
            $abrirPaqueteProyecto = "@echo off`r`nsetlocal`r`nchcp 65001 >nul`r`npowershell.exe -NoProfile -ExecutionPolicy Bypass -File `"%~dp0scripts\AbrirPaquete.ps1`"`r`nif errorlevel 1 pause`r`n"
            [System.IO.File]::WriteAllText((Join-Path $paqueteProyecto 'ABRIR.cmd'), $abrirPaqueteProyecto, [System.Text.Encoding]::ASCII)
            Write-Host ("APLICACION PREPARADA: " + (Join-Path $paqueteProyecto 'ABRIR.cmd')) -ForegroundColor Green
            Write-Host 'Para moverla a otro equipo, copia toda la carpeta SQLStudio; requiere Java 21 o posterior.'
        }
        'pruebas' {
            foreach ($suiteProyecto in @('symbol_table.PruebaTablaSimbolos', 'PruebaFase1',
                    'PruebaParserDDL', 'PruebaFase2', 'PruebaAnalizadorDML', 'PruebaSemanticaDDL',
                    'PruebaSemanticaDML', 'PruebaDelimitadores', 'PruebaEjecucionSQL',
                    'PruebaPanelesGUI', 'PruebaIntegrante2', 'PruebaPersistencia', 'PruebaInterfaz')) {
                & $jdkProyecto.Java @opcionesProyecto $suiteProyecto
                if ($LASTEXITCODE -ne 0) { throw "Fallo la suite $suiteProyecto." }
            }
            Write-Host 'VERIFICACION COMPLETA: todas las pruebas pasaron.' -ForegroundColor Green
        }
        'demo' {
            & $jdkProyecto.Java @opcionesProyecto 'cli.Fase3CLI' 'tests/demo_fase3.sql' 'tests/demo_fase3_referencia.sql'
            if ($LASTEXITCODE -ne 0) { throw 'La comparacion de demostracion fallo.' }
        }
        'capturas' {
            & $jdkProyecto.Java @opcionesProyecto 'PruebaInterfaz' '--capturas'
            if ($LASTEXITCODE -ne 0) { throw 'La verificacion visual fallo.' }
        }
    }
    exit 0
} catch {
    Write-Host ('ERROR: ' + $_.Exception.Message) -ForegroundColor Red
    exit 2
} finally { Pop-Location }
