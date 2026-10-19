param()

$ErrorActionPreference = 'Stop'
$raizPrueba = Split-Path -Parent $PSScriptRoot
$editorPrueba = Join-Path $env:LOCALAPPDATA 'Programs/Microsoft VS Code/bin/code.cmd'
if (-not (Test-Path -LiteralPath $editorPrueba)) {
    $comandoPrueba = Get-Command code.cmd -ErrorAction SilentlyContinue
    if (-not $comandoPrueba) { throw 'No se encontro VS Code. Esta prueba es opcional; usa VERIFICAR_FASE3.cmd para la aplicacion.' }
    $editorPrueba = $comandoPrueba.Source
}
$perfilPrueba = Join-Path $raizPrueba ('tmp/vscode-verificacion-' + [guid]::NewGuid().ToString('N').Substring(0, 8))
$ajustesPrueba = Join-Path $perfilPrueba 'User'
New-Item -ItemType Directory -Path $ajustesPrueba -Force | Out-Null
@{
    'update.mode' = 'none'
    'extensions.autoUpdate' = $false
    'java.server.launchMode' = 'Standard'
    'workbench.startupEditor' = 'none'
    'telemetry.telemetryLevel' = 'off'
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $ajustesPrueba 'settings.json') -Encoding UTF8
$extensionesPrueba = Join-Path $env:USERPROFILE '.vscode/extensions'
$extensionPrueba = Join-Path $PSScriptRoot 'vscode'
$informePrueba = Join-Path $raizPrueba 'output/vscode/verificacion.json'
if (Test-Path -LiteralPath $informePrueba) { Remove-Item -LiteralPath $informePrueba }
Write-Host 'Abriendo una instancia de prueba de VS Code con perfil separado.'
Write-Host 'La ventana SQL Studio verificara Analizar, Ejecutar y Comparar y se cerrara sola.'
& $editorPrueba '--new-window' '--wait' '--disable-workspace-trust' '--skip-welcome' '--skip-release-notes' `
    "--user-data-dir=$perfilPrueba" "--extensions-dir=$extensionesPrueba" `
    "--extensionDevelopmentPath=$extensionPrueba" `
    "--extensionTestsPath=$(Join-Path $extensionPrueba 'verificar.js')" `
    (Join-Path $raizPrueba 'SQLStudio.code-workspace')
if ($LASTEXITCODE -ne 0) { throw "Fallo VS Code. Consulta $informePrueba" }
if (-not (Test-Path -LiteralPath $informePrueba)) { throw 'VS Code no genero la evidencia de inicio.' }
$resultadoPrueba = Get-Content -LiteralPath $informePrueba -Raw -Encoding UTF8 | ConvertFrom-Json
if (-not $resultadoPrueba.aprobado) { throw "Fallo la prueba de inicio: $($resultadoPrueba.error)" }
Write-Host 'VS CODE VERIFICADO: interfaz y consola iniciadas por el depurador, sin errores.'
