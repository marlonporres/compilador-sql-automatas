$ErrorActionPreference = 'Stop'
$carpetaPaquete = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $carpetaPaquete
try {
    . (Join-Path $PSScriptRoot 'JavaProyecto.ps1')
    $jdkPaquete = Resolve-JdkProyecto -SoloEjecucion
    & $jdkPaquete.Java '-Dfile.encoding=UTF-8' '-jar' (Join-Path $carpetaPaquete 'SQLStudio.jar')
    if ($LASTEXITCODE -ne 0) { throw 'SQL Studio termino con un error; revisa los mensajes anteriores.' }
    exit 0
} catch {
    Write-Host ('ERROR: ' + $_.Exception.Message) -ForegroundColor Red
    exit 2
} finally { Pop-Location }
