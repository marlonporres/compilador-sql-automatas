param(
    [string]$Destino,
    [switch]$Verificar
)
$ErrorActionPreference = 'Stop'
$raizEjecutable = Split-Path -Parent $PSScriptRoot
if (-not $Destino) { $Destino = Join-Path $raizEjecutable 'output/Escritorio' }
$Destino = [System.IO.Path]::GetFullPath($Destino)
$aplicacionEjecutable = Join-Path $Destino 'SQLStudio'
if (Test-Path -LiteralPath $aplicacionEjecutable) {
    throw "Ya existe $aplicacionEjecutable. Se conserva intacta. Usa -Destino con otra carpeta para generar una version nueva."
}
$entradaEjecutable = Join-Path $raizEjecutable 'output/SQLStudio'
if (-not (Test-Path -LiteralPath (Join-Path $entradaEjecutable 'SQLStudio.jar'))) {
    throw 'Primero ejecuta EMPAQUETAR_APP.cmd para preparar el JAR y sus bibliotecas.'
}
. (Join-Path $PSScriptRoot 'JavaProyecto.ps1')
$jdkEjecutable = Resolve-JdkProyecto
$herramientaEjecutable = Join-Path $jdkEjecutable.Directorio 'bin/jpackage.exe'
if (-not (Test-Path -LiteralPath $herramientaEjecutable)) {
    throw ('El JDK elegido no incluye jpackage.exe: ' + $jdkEjecutable.Directorio)
}
$temporalEjecutable = Join-Path $raizEjecutable ('output/jpackage-' + [System.Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temporalEjecutable -Force | Out-Null
New-Item -ItemType Directory -Path $Destino -Force | Out-Null
Write-Host 'Preparando SQLStudio.exe con Java incluido. No instala programas ni modifica el sistema.'
& $herramientaEjecutable ('-J-Djava.io.tmpdir=' + $temporalEjecutable) '--type' 'app-image' `
    '--dest' $Destino '--name' 'SQLStudio' '--input' $entradaEjecutable `
    '--main-jar' 'SQLStudio.jar' '--main-class' 'ui.AplicacionGUI' `
    '--app-version' '3.0' '--vendor' 'Equipo Compilador SQL' `
    '--description' 'Compilador SQL en espanol - Proyecto final' `
    '--java-options' '-Dfile.encoding=UTF-8' `
    '--java-options' '-Dflatlaf.useNativeLibrary=false' `
    '--temp' (Join-Path $temporalEjecutable 'trabajo')
if ($LASTEXITCODE -ne 0) { throw 'jpackage no pudo preparar la aplicacion. Se conservan los archivos y mensajes de diagnostico.' }
$exeEjecutable = Join-Path $aplicacionEjecutable 'SQLStudio.exe'
if (-not (Test-Path -LiteralPath $exeEjecutable)) { throw 'jpackage termino sin producir SQLStudio.exe.' }
Write-Host ('APLICACION AUTONOMA: ' + $exeEjecutable) -ForegroundColor Green
Write-Host 'Abre SQLStudio.exe con doble clic. Al trasladarlo, copia toda su carpeta SQLStudio, incluido runtime.'

if ($Verificar) {
    $inicioExeEjecutable = New-Object System.Diagnostics.ProcessStartInfo
    $inicioExeEjecutable.FileName = $exeEjecutable
    $inicioExeEjecutable.Arguments = '--verificar-arranque'
    $inicioExeEjecutable.WorkingDirectory = $env:USERPROFILE
    $inicioExeEjecutable.UseShellExecute = $false
    $inicioExeEjecutable.CreateNoWindow = $true
    $procesoExeEjecutable = New-Object System.Diagnostics.Process
    $procesoExeEjecutable.StartInfo = $inicioExeEjecutable
    try {
        [void]$procesoExeEjecutable.Start()
        if (-not $procesoExeEjecutable.WaitForExit(45000)) {
            try { $procesoExeEjecutable.Kill() } catch { }
            throw 'El ejecutable no termino la verificacion antes de 45 segundos.'
        }
        if ($procesoExeEjecutable.ExitCode -ne 0) { throw ('SQLStudio.exe no supero el flujo de arranque. Codigo: ' + $procesoExeEjecutable.ExitCode) }
        Write-Host 'EXE VERIFICADO: desde otra carpeta, con su propio Java, ventana, analisis, ejecucion y comparacion.' -ForegroundColor Green
    } finally { $procesoExeEjecutable.Dispose() }
}
$archivosEjecutable = @(Get-ChildItem -LiteralPath $aplicacionEjecutable -Recurse -File)
$bytesEjecutable = ($archivosEjecutable | Measure-Object -Property Length -Sum).Sum
Write-Host ('Carpeta completa: {0:N1} MiB; {1} archivos.' -f ($bytesEjecutable / 1MB), $archivosEjecutable.Count)
Write-Host ('SHA256 SQLStudio.exe: ' + (Get-FileHash -LiteralPath $exeEjecutable -Algorithm SHA256).Hash)
