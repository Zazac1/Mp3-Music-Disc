param([switch]$PrepareAssetsOnly)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location -LiteralPath $projectRoot

$javaRoots = @('C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Java')
$javaHome = @(
foreach ($javaRoot in $javaRoots) {
    if (-not (Test-Path -LiteralPath $javaRoot)) { continue }
    Get-ChildItem -LiteralPath $javaRoot -Directory -Filter 'jdk-25*' -ErrorAction SilentlyContinue |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe') } |
        Sort-Object { [version](($_.Name -replace '^jdk-', '') -replace '-.*$', '') } -Descending |
        Select-Object -ExpandProperty FullName
}
) | Select-Object -First 1
if (-not $javaHome) {
    throw "JDK 25 introuvable dans : $($javaRoots -join ', ')"
}
Write-Host "JDK utilise : $javaHome"
$env:JAVA_HOME = $javaHome

# Keep this mod's Gradle files separate from other development launches. This
# avoids a locked shared wrapper cache preventing the launcher from starting.
$projectGradleCache = Join-Path $projectRoot '.gradle-user'
function Test-GradleCache([string]$cache) {
    return Test-Path -LiteralPath (Join-Path $cache 'wrapper\dists\gradle-9.5.1-bin\iq79hdu3mqx29lgffhp8bfmx\gradle-9.5.1\bin\gradle.bat')
}
$env:GRADLE_USER_HOME = $projectGradleCache
if (-not (Test-GradleCache $projectGradleCache)) {
    Write-Warning 'Cache Gradle 9.5.1 absent : une connexion Internet sera nécessaire au premier lancement.'
}

# Loom needs the same hashed assets as the Minecraft Launcher. Reuse whichever
# local cache already contains 26.3 instead of trying to contact Mojang.
function Sync-LauncherAssetsToLoom {
    $assetSources = @(
        (Join-Path $env:APPDATA '.minecraft\assets'),
        (Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\assets')
    )
    $sourceAssets = $null
    $index = $null
    foreach ($candidate in $assetSources) {
        $found = Get-ChildItem -LiteralPath (Join-Path $candidate 'indexes') -Filter '26.3-*.json' -File -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | Select-Object -First 1
        if ($null -ne $found) {
            $sourceAssets = $candidate
            $index = $found
            break
        }
    }
    if ($null -eq $index) {
        Write-Warning 'Assets 26.3 absents des caches locaux : Gradle les téléchargera au premier lancement.'
        return $false
    }

    $loomAssets = Join-Path $env:GRADLE_USER_HOME 'caches\fabric-loom\assets'
    $loomObjects = Join-Path $loomAssets 'objects'
    $loomIndexes = Join-Path $loomAssets 'indexes'
    New-Item -ItemType Directory -Force -Path $loomObjects, $loomIndexes | Out-Null
    $targetIndex = Join-Path $loomIndexes $index.Name
    if ([System.IO.Path]::GetFullPath($index.FullName) -ine [System.IO.Path]::GetFullPath($targetIndex)) {
        Copy-Item -LiteralPath $index.FullName -Destination $targetIndex -Force
    }

    $assetIndex = Get-Content -LiteralPath $index.FullName -Raw | ConvertFrom-Json
    $copied = 0
    foreach ($entry in $assetIndex.objects.PSObject.Properties) {
        $hash = $entry.Value.hash
        if ([string]::IsNullOrWhiteSpace($hash)) { continue }
        $folder = $hash.Substring(0, 2)
        $source = Join-Path $sourceAssets (Join-Path 'objects' (Join-Path $folder $hash))
        $destinationDirectory = Join-Path $loomObjects $folder
        $destination = Join-Path $destinationDirectory $hash
        if ((Test-Path -LiteralPath $destination) -or -not (Test-Path -LiteralPath $source)) { continue }
        New-Item -ItemType Directory -Force -Path $destinationDirectory | Out-Null
        Copy-Item -LiteralPath $source -Destination $destination
        $copied++
    }
    Write-Host "Assets 26.3 synchronisés depuis $sourceAssets : $copied fichier(s) ajouté(s)."
    return $true
}

$assetsCached = Sync-LauncherAssetsToLoom
if ($PrepareAssetsOnly) { return }
$offlineOption = if ($assetsCached) { '--offline' } else { '' }

$serverPidFile = Join-Path $projectRoot 'run-server\.mp3musicdiscs-server-launcher.json'
$worldLock = Join-Path $projectRoot 'run-server\world\session.lock'

function Test-ExclusiveFileAccess([string]$path) {
    if (-not (Test-Path -LiteralPath $path)) { return $true }
    try {
        $handle = [System.IO.File]::Open($path, [System.IO.FileMode]::Open,
            [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
        $handle.Dispose()
        return $true
    } catch [System.IO.IOException] {
        return $false
    }
}

function Wait-ForWorldUnlock([int]$timeoutSeconds = 12) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    do {
        if (Test-ExclusiveFileAccess $worldLock) { return $true }
        Start-Sleep -Milliseconds 250
    } while ((Get-Date) -lt $deadline)
    return $false
}

# Stop only the server window previously started by this script, verified by PID and start time.
if (Test-Path -LiteralPath $serverPidFile) {
    try {
        $previous = Get-Content -LiteralPath $serverPidFile -Raw | ConvertFrom-Json
        $oldProcess = Get-Process -Id $previous.processId -ErrorAction SilentlyContinue
        if ($oldProcess -and $oldProcess.StartTime.ToFileTimeUtc() -eq [long]$previous.startTime) {
            Write-Host "Arrêt de l'ancien serveur de test (PID $($previous.processId))..."
            & taskkill.exe /PID $previous.processId /T /F | Out-Null
        }
    } catch {
        Write-Warning "Impossible de lire l'ancien lanceur : $($_.Exception.Message)"
    }
    Remove-Item -LiteralPath $serverPidFile -Force -ErrorAction SilentlyContinue
}

if (-not (Wait-ForWorldUnlock)) {
    throw "Le monde run-server est encore utilise. Ferme l'ancien serveur, puis relance ce script."
}

$lanIp = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object { $_.IPAddress -ne '127.0.0.1' -and $_.IPAddress -notlike '169.254.*' } |
    Select-Object -First 1 -ExpandProperty IPAddress
if (-not $lanIp) { $lanIp = '127.0.0.1' }

$serverEula = Join-Path $projectRoot 'run-server\eula.txt'
if (-not (Test-Path -LiteralPath $serverEula) -or (Get-Content -LiteralPath $serverEula -Raw) -notmatch '(?m)^eula=true\s*$') {
    $accept = Read-Host 'Le serveur Minecraft exige EULA=true. Tape OUI pour accepter'
    if ($accept -ne 'OUI') { throw 'EULA non acceptée : lancement annulé.' }
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $serverEula) | Out-Null
    Set-Content -LiteralPath $serverEula -Value 'eula=true' -NoNewline
}

# Seulement pour le serveur Loom local : le client de développement n'est pas connecté à Microsoft.
$serverProperties = Join-Path $projectRoot 'run-server\server.properties'
$properties = if (Test-Path -LiteralPath $serverProperties) { Get-Content -LiteralPath $serverProperties -Raw } else { '' }
if ($properties -match '(?m)^online-mode=') {
    $properties = $properties -replace '(?m)^online-mode=.*$', 'online-mode=false'
} else {
    $properties += "`r`nonline-mode=false`r`n"
}
Set-Content -LiteralPath $serverProperties -Value $properties -NoNewline

Write-Host 'Compilation du mod...'
& '.\gradlew.bat' build $offlineOption --no-daemon
if ($LASTEXITCODE -ne 0) { throw 'Compilation échouée : lancement annulé.' }

# Un build unique évite que runClient et runServer compilent les mêmes classes en même temps.
$serverScript = "`$env:JAVA_HOME = '$javaHome'; `$env:GRADLE_USER_HOME = '$env:GRADLE_USER_HOME'; Set-Location -LiteralPath '$projectRoot'; & '.\gradlew.bat' runServer $offlineOption --no-daemon -x compileJava -x processResources -x classes"
$clientScript = "`$env:JAVA_HOME = '$javaHome'; `$env:GRADLE_USER_HOME = '$env:GRADLE_USER_HOME'; Set-Location -LiteralPath '$projectRoot'; & '.\gradlew.bat' runClient $offlineOption --no-daemon -x compileJava -x processResources -x classes"

Write-Host "Serveur : $lanIp`:25565"
Write-Host 'Démarrage du serveur, puis du client dans 6 secondes...'
$serverLauncher = Start-Process -FilePath 'powershell.exe' -WorkingDirectory $projectRoot `
    -ArgumentList '-NoExit', '-NoProfile', '-Command', $serverScript -PassThru
@{ processId = $serverLauncher.Id; startTime = $serverLauncher.StartTime.ToFileTimeUtc() } |
    ConvertTo-Json | Set-Content -LiteralPath $serverPidFile -NoNewline
Start-Sleep -Seconds 6
Start-Process -FilePath 'powershell.exe' -WorkingDirectory $projectRoot `
    -ArgumentList '-NoExit', '-NoProfile', '-Command', $clientScript

Write-Host "Dans le client : Multijoueur > Ajouter un serveur > $lanIp`:25565"
