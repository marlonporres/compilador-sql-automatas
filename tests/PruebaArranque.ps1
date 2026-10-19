param([switch]$OmitirEmpaquetado)
$ErrorActionPreference = 'Stop'
$raizArranque = Split-Path -Parent $PSScriptRoot
$powershellArranque = Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
. (Join-Path $raizArranque 'scripts/JavaProyecto.ps1')
$script:comprobacionesArranque = 0
function Confirmar-Arranque([bool]$Condicion, [string]$Nombre) {
    if (-not $Condicion) { throw ('FALLO: ' + $Nombre) }
    $script:comprobacionesArranque++
    Write-Host ('OK: ' + $Nombre)
}

$jdkArranque = Resolve-JdkProyecto
Confirmar-Arranque ($jdkArranque.Version -ge 21) 'Detecta un JDK real y operativo'
$alternativaArranque = Resolve-JdkProyecto -Candidatos @('C:\jdk-que-no-existe', $jdkArranque.Directorio)
Confirmar-Arranque ($alternativaArranque.Java -eq $jdkArranque.Java) 'Omite instalaciones inexistentes sin bloquear'
$javaHomeAnteriorArranque = $env:JAVA_HOME
try {
    $env:JAVA_HOME = 'C:\jdk-que-no-existe'
    $respaldoArranque = Resolve-JdkProyecto
    Confirmar-Arranque ($respaldoArranque.Version -ge 21) 'JAVA_HOME incorrecto permite encontrar una alternativa'
} finally { $env:JAVA_HOME = $javaHomeAnteriorArranque }
$cronometroArranque = [System.Diagnostics.Stopwatch]::StartNew()
$timeoutArranque = Invoke-ProcesoAcotado -Archivo $powershellArranque `
    -Argumentos '-NoProfile -NonInteractive -Command "Start-Sleep -Seconds 30"' -TiempoLimiteMs 600
$cronometroArranque.Stop()
Confirmar-Arranque ($timeoutArranque.Agotado -and -not $timeoutArranque.Correcto -and
    $cronometroArranque.ElapsedMilliseconds -lt 5000) 'Una herramienta colgada termina por tiempo limite'
$inexistenteArranque = Invoke-ProcesoAcotado -Archivo 'C:\java-que-no-existe.exe'
Confirmar-Arranque (-not $inexistenteArranque.Correcto -and -not $inexistenteArranque.Agotado) 'Un ejecutable inexistente devuelve un fallo controlado'

# Ejecutar desde una carpeta ajena reproduce la terminal del usuario situada en ~.
Push-Location -LiteralPath $env:USERPROFILE
try {
    if (-not $OmitirEmpaquetado) {
        & $powershellArranque -NoProfile -ExecutionPolicy Bypass `
            -File (Join-Path $raizArranque 'gui.ps1') -Modo empaquetar
        Confirmar-Arranque ($LASTEXITCODE -eq 0) 'Compila y empaqueta desde una carpeta ajena, con espacios en la ruta'
    }
    $paqueteArranque = Join-Path $raizArranque 'output/SQLStudio'
    $jarArranque = Join-Path $paqueteArranque 'SQLStudio.jar'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zipArranque = [System.IO.Compression.ZipFile]::OpenRead($jarArranque)
    try {
        $manifestArranque = $zipArranque.GetEntry('META-INF/MANIFEST.MF')
        $lectorArranque = New-Object System.IO.StreamReader($manifestArranque.Open())
        try { $textoManifestArranque = $lectorArranque.ReadToEnd() } finally { $lectorArranque.Dispose() }
        Confirmar-Arranque ($textoManifestArranque.Contains('Main-Class: ui.AplicacionGUI')) 'JAR declara la entrada grafica correcta'
        Confirmar-Arranque ($textoManifestArranque.Contains('Class-Path: lib/h2-2.4.240.jar lib/flatlaf-3.6.2.jar')) 'Manifest incluye las dos dependencias relativas'
        Confirmar-Arranque ($null -ne $zipArranque.GetEntry('tests/demo_fase3.sql') -and
            $null -ne $zipArranque.GetEntry('tests/demo_fase3_referencia.sql') -and
            $null -ne $zipArranque.GetEntry('tests/demo_fase2_errores.sql')) 'El JAR conserva los tres ejemplos como recursos'
    } finally { $zipArranque.Dispose() }
    $inicioJarArranque = Invoke-ProcesoAcotado -Archivo $jdkArranque.Java `
        -Argumentos ('-Djava.awt.headless=true -Dfile.encoding=UTF-8 -jar "' + $jarArranque + '"')
    Confirmar-Arranque ($inicioJarArranque.Codigo -eq 1 -and $inicioJarArranque.Texto -match 'escritorio') 'java -jar encuentra AplicacionGUI y rechaza un entorno sin escritorio claramente'
    # No se agrega lib al classpath: la JVM debe resolverlo por el manifest.
    $panelesArranque = Invoke-ProcesoAcotado -Archivo $jdkArranque.Java -TiempoLimiteMs 15000 `
        -Argumentos ('-Djava.awt.headless=true -Dfile.encoding=UTF-8 -cp "' + $jarArranque + '" PruebaPanelesGUI')
    Confirmar-Arranque ($panelesArranque.Correcto -and $panelesArranque.Texto -match 'Fallidas: 0') 'JAR carga FlatLaf y supera los paneles Swing mediante el manifest'
} finally { Pop-Location }

Push-Location -LiteralPath (Join-Path $raizArranque 'output/SQLStudio')
try {
    $demostracionArranque = Invoke-ProcesoAcotado -Archivo $jdkArranque.Java -TiempoLimiteMs 15000 `
        -Argumentos '-Dfile.encoding=UTF-8 -cp SQLStudio.jar cli.Fase3CLI tests/demo_fase3.sql tests/demo_fase3_referencia.sql'
    Confirmar-Arranque ($demostracionArranque.Correcto -and $demostracionArranque.Texto.Contains('COMPARACION SQL: APROBADA')) 'El paquete carga H2 y compara la demostracion real con SQL independiente'
} finally { Pop-Location }
Write-Host ("PRUEBAS DE ARRANQUE: $script:comprobacionesArranque correctas, 0 fallidas.") -ForegroundColor Green
