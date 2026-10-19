# Utilidades compartidas por el lanzador y sus pruebas. Compatible con Windows PowerShell 5.1.
function Invoke-ProcesoAcotado {
    param(
        [Parameter(Mandatory = $true)][string]$Archivo,
        [string]$Argumentos = '-version',
        [ValidateRange(100, 60000)][int]$TiempoLimiteMs = 5000
    )
    $inicioJava = New-Object System.Diagnostics.ProcessStartInfo
    $inicioJava.FileName = $Archivo
    $inicioJava.Arguments = $Argumentos
    $inicioJava.WorkingDirectory = (Get-Location).ProviderPath
    $inicioJava.UseShellExecute = $false
    $inicioJava.CreateNoWindow = $true
    $inicioJava.RedirectStandardOutput = $true
    $inicioJava.RedirectStandardError = $true
    $procesoJava = New-Object System.Diagnostics.Process
    $procesoJava.StartInfo = $inicioJava
    $relojJava = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        [void]$procesoJava.Start()
        $salidaJava = $procesoJava.StandardOutput.ReadToEndAsync()
        $errorJava = $procesoJava.StandardError.ReadToEndAsync()
        if (-not $procesoJava.WaitForExit($TiempoLimiteMs)) {
            try { $procesoJava.Kill() } catch { }
            return [pscustomobject]@{ Correcto = $false; Agotado = $true; Texto = ''; Codigo = -1 }
        }
        $restanteJava = [Math]::Max(1, $TiempoLimiteMs - [int]$relojJava.ElapsedMilliseconds)
        if (-not $salidaJava.Wait($restanteJava)) {
            return [pscustomobject]@{ Correcto = $false; Agotado = $true; Texto = ''; Codigo = -1 }
        }
        $restanteJava = [Math]::Max(1, $TiempoLimiteMs - [int]$relojJava.ElapsedMilliseconds)
        if (-not $errorJava.Wait($restanteJava)) {
            return [pscustomobject]@{ Correcto = $false; Agotado = $true; Texto = ''; Codigo = -1 }
        }
        $textoJava = $salidaJava.GetAwaiter().GetResult() + $errorJava.GetAwaiter().GetResult()
        return [pscustomobject]@{
            Correcto = ($procesoJava.ExitCode -eq 0); Agotado = $false
            Texto = $textoJava; Codigo = $procesoJava.ExitCode
        }
    } catch {
        return [pscustomobject]@{ Correcto = $false; Agotado = $false; Texto = $_.Exception.Message; Codigo = -1 }
    } finally { $procesoJava.Dispose() }
}

function Resolve-JdkProyecto {
    param([string[]]$Candidatos, [switch]$SoloEjecucion)
    if (-not $PSBoundParameters.ContainsKey('Candidatos')) {
        $rutasJava = New-Object 'System.Collections.Generic.List[string]'
        if ($env:JAVA_HOME) { $rutasJava.Add($env:JAVA_HOME.Trim('"')) }
        foreach ($baseJava in @(
                (Join-Path $env:ProgramFiles 'Java'),
                (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
                (Join-Path $env:ProgramFiles 'Microsoft'),
                (Join-Path $env:USERPROFILE '.jdks'))) {
            if (Test-Path -LiteralPath $baseJava) {
                Get-ChildItem -LiteralPath $baseJava -Directory -ErrorAction SilentlyContinue |
                    Sort-Object Name -Descending | ForEach-Object { $rutasJava.Add($_.FullName) }
            }
        }
        $extensionesJava = Join-Path $env:USERPROFILE '.vscode/extensions'
        if (Test-Path -LiteralPath $extensionesJava) {
            foreach ($extensionJava in @(Get-ChildItem -LiteralPath $extensionesJava -Directory -Filter 'redhat.java-*' |
                    Sort-Object Name -Descending)) {
                $baseJre = Join-Path $extensionJava.FullName 'jre'
                if (Test-Path -LiteralPath $baseJre) {
                    Get-ChildItem -LiteralPath $baseJre -Directory |
                        ForEach-Object { $rutasJava.Add($_.FullName) }
                }
            }
        }
        $nombreComandoJava = if ($SoloEjecucion) { 'java.exe' } else { 'javac.exe' }
        foreach ($comandoJava in @(Get-Command $nombreComandoJava -All -ErrorAction SilentlyContinue)) {
            $archivoJava = Get-Item -LiteralPath $comandoJava.Source
            $rutaJava = $archivoJava.FullName
            if ($archivoJava.Target) {
                $destinoJava = @($archivoJava.Target)[0]
                if (-not [System.IO.Path]::IsPathRooted($destinoJava)) {
                    $destinoJava = Join-Path $archivoJava.DirectoryName $destinoJava
                }
                if (Test-Path -LiteralPath $destinoJava) { $rutaJava = $destinoJava }
            }
            $rutasJava.Add((Split-Path -Parent (Split-Path -Parent $rutaJava)))
        }
        $Candidatos = $rutasJava.ToArray()
    }
    $vistosJava = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($directorioJava in $Candidatos) {
        if (-not $directorioJava -or -not $vistosJava.Add($directorioJava)) { continue }
        $javaElegido = Join-Path $directorioJava 'bin/java.exe'
        $javacElegido = Join-Path $directorioJava 'bin/javac.exe'
        $jarElegido = Join-Path $directorioJava 'bin/jar.exe'
        if (-not (Test-Path -LiteralPath $javaElegido)) { continue }
        if (-not $SoloEjecucion -and (-not (Test-Path -LiteralPath $javacElegido) -or
                -not (Test-Path -LiteralPath $jarElegido))) { continue }
        $versionJava = Invoke-ProcesoAcotado -Archivo $javaElegido
        if (-not $versionJava.Correcto -or $versionJava.Texto -notmatch 'version\s+"(?<numero>\d+)') { continue }
        $numeroJava = [int]$Matches.numero
        if ($numeroJava -lt 21) { continue }
        if (-not $SoloEjecucion) {
            $versionJavac = Invoke-ProcesoAcotado -Archivo $javacElegido
            if (-not $versionJavac.Correcto -or $versionJavac.Texto -notmatch 'javac\s+(?<numero>\d+)' -or
                    [int]$Matches.numero -lt 21) { continue }
        }
        return [pscustomobject]@{
            Directorio = $directorioJava; Java = $javaElegido; Javac = $javacElegido
            Jar = $jarElegido; Version = $numeroJava
        }
    }
    if ($SoloEjecucion) { throw 'No se encontro Java 21 o posterior. Instala Java 21 o configura JAVA_HOME con una instalacion operativa.' }
    throw 'No se encontro un JDK 21 o posterior operativo. Instala un JDK completo o abre VS Code con Extension Pack for Java; no basta abrir un archivo Java suelto.'
}
