<#
  Installe le mod de Pet Soccer sur une copie du jeu déjà installée (le jeu lui-même n'est pas fourni).

  Double-clique sur Installer.bat, ou :
      powershell -ExecutionPolicy Bypass -File install.ps1 [-Game "C:\Jeux\Pet Soccer"] [-Server adresse] [-Port 7777]
      powershell -ExecutionPolicy Bypass -File install.ps1 -Uninstall

  Ce que fait l'installation :
    - garde une copie des fichiers d'origine qu'elle remplace dans <jeu>\mod_backup (une seule fois) ;
    - ajoute les classes du mod à code.pak (en repartant de l'original) et refait l'index code_inf.pak ;
    - copie les fichiers de données du mod dans Data\ ;
    - écrit l'adresse du serveur dans Data\online.scr.
  Les sauvegardes (*.dat) ne sont jamais touchées. Relancer l'installation met le mod à jour.

  -Game      : dossier du jeu (celui de PetSoccer.exe). Sans lui, le dossier est cherché, puis demandé.
  -Server    : adresse du serveur de jeu en ligne (nom ou IP), donnée par celui qui l'héberge
  -Port      : port UDP du serveur (7777 par défaut)
  -Uninstall : remet les fichiers d'origine et retire ceux du mod
  -Quiet     : ne pose aucune question et n'attend pas à la fin (installation automatique)
#>
param([string]$Game = '', [string]$Server = '', [int]$Port = 0, [switch]$Uninstall, [switch]$Quiet)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$Here      = $PSScriptRoot
$Classes   = Join-Path $Here 'classes'
$Data      = Join-Path $Here 'data'
$Enc       = [Text.Encoding]::GetEncoding(1252)   # les .scr du jeu sont en cp1252 + CRLF
$LocalHost = '127.0.0.1'

function Stop-Install([string]$Message) {
    Write-Host ''
    Write-Host "ERREUR : $Message" -ForegroundColor Red
    if (-not $Quiet) { Read-Host 'Appuie sur Entrée pour fermer' | Out-Null }
    exit 1
}

function Test-GameDir([string]$Dir) {
    return [bool]($Dir -and (Test-Path (Join-Path $Dir 'PetSoccer.exe')) -and (Test-Path (Join-Path $Dir 'code.pak')) `
        -and (Test-Path (Join-Path $Dir 'Data')))
}

# Le dossier du jeu : celui donné, sinon celui que l'installeur d'origine a noté dans le registre, sinon les
# endroits habituels, sinon on le demande.
function Find-GameDir {
    if ($Game) {
        if (Test-GameDir $Game) { return (Resolve-Path $Game).Path }
        Stop-Install "Pet Soccer n'est pas dans « $Game » (PetSoccer.exe, code.pak et Data doivent s'y trouver)."
    }
    $found = @()
    foreach ($key in 'HKLM:\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\Pet Soccer',
                     'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\Pet Soccer') {
        $props = Get-ItemProperty $key -ErrorAction SilentlyContinue
        foreach ($value in @($props.InstallLocation, $props.DisplayIcon, $props.UninstallString)) {
            if (-not $value) { continue }
            $path = ([string]$value).Trim('"')
            if (Test-Path $path -PathType Leaf) { $path = Split-Path -Parent $path }
            $found += $path
        }
    }
    $found += 'C:\Jeux\Pet Soccer', 'C:\Games\Pet Soccer', "${env:ProgramFiles(x86)}\Pet Soccer", "$env:ProgramFiles\Pet Soccer",
              "${env:ProgramFiles(x86)}\Techland\Pet Soccer"
    foreach ($dir in $found) {
        if (-not (Test-GameDir $dir)) { continue }
        $dir = (Resolve-Path $dir).Path
        if ($Quiet) { return $dir }
        # trouvé tout seul : on demande avant de toucher à ce dossier (il peut y avoir plusieurs copies du jeu)
        Write-Host "Pet Soccer trouvé dans : $dir"
        $verb = if ($Uninstall) { 'Retirer le mod de' } else { 'Installer le mod dans' }
        if ((Read-Host "$verb ce dossier ? (o/n)") -match '^[oOyY]') { return $dir }
        break
    }
    if ($Quiet) { Stop-Install 'Dossier du jeu introuvable : donne-le avec -Game.' }
    Write-Host 'Choisis le dossier de Pet Soccer (celui de PetSoccer.exe) dans la fenêtre qui s''ouvre.'
    Add-Type -AssemblyName System.Windows.Forms
    $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
    $dialog.Description = 'Dossier où Pet Soccer est installé (celui de PetSoccer.exe)'
    $dialog.ShowNewFolderButton = $false
    if ($dialog.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { Stop-Install 'Aucun dossier choisi.' }
    if (-not (Test-GameDir $dialog.SelectedPath)) {
        Stop-Install "Pet Soccer n'est pas dans « $($dialog.SelectedPath) » (PetSoccer.exe, code.pak et Data doivent s'y trouver)."
    }
    return $dialog.SelectedPath
}

function Test-CanWrite([string]$Dir) {
    $probe = Join-Path $Dir ('mod_ecriture_' + [Guid]::NewGuid().ToString('N') + '.tmp')
    try {
        [IO.File]::WriteAllText($probe, 'x')
        Remove-Item $probe -Force
        return $true
    } catch {
        return $false
    }
}

function Get-RelativeFiles([string]$Root) {
    return @(Get-ChildItem $Root -Recurse -File | ForEach-Object { $_.FullName.Substring($Root.Length + 1) })
}

# Adresse et port du serveur écrits dans un online.scr, ou $null
function Get-ServerLine([string]$Path) {
    if (-not (Test-Path $Path)) { return $null }
    $match = [regex]::Match([IO.File]::ReadAllText($Path, $Enc), '(?m)^Server\("([^"]*)",\s*(\d+)\)')
    if ($match.Success) { return @{ Host = $match.Groups[1].Value; Port = [int]$match.Groups[2].Value } }
    return $null
}

# ---------------------------------------------------------------------------------------------------------------

# Une erreur imprévue ne doit pas fermer la fenêtre avant qu'on ait pu la lire
trap { Stop-Install $_.Exception.Message }

Write-Host '=== Mod de Pet Soccer ==='
$modInfo = @{}
foreach ($line in [IO.File]::ReadAllLines((Join-Path $Here 'mod.txt'))) {
    $parts = $line.Split('=', 2)
    if ($parts.Count -eq 2) { $modInfo[$parts[0].Trim()] = $parts[1].Trim() }
}

$GameDir = Find-GameDir
Write-Host "Jeu : $GameDir"
if (Get-Process 'PetSoccer.bum', 'PetSoccer' -ErrorAction SilentlyContinue) {
    Stop-Install 'Pet Soccer est ouvert : ferme le jeu, puis relance l''installation.'
}

if (-not (Test-CanWrite $GameDir)) {
    # Jeu installé dans Program Files : il faut les droits d'administrateur
    $admin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
        [Security.Principal.WindowsBuiltInRole]::Administrator)
    if ($admin -or $Quiet) { Stop-Install "Impossible d'écrire dans « $GameDir »." }
    Write-Host 'Ce dossier demande les droits d''administrateur : Windows va te demander de les accorder.'
    $relaunch = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`"", '-Game', "`"$GameDir`"")
    if ($Server) { $relaunch += @('-Server', "`"$Server`"") }
    if ($Port -gt 0) { $relaunch += @('-Port', $Port) }
    if ($Uninstall) { $relaunch += '-Uninstall' }
    Start-Process powershell -Verb RunAs -ArgumentList $relaunch
    exit 0
}

$Backup   = Join-Path $GameDir 'mod_backup'
$ListPath = Join-Path $Backup 'fichiers_du_mod.txt'

# --- désinstallation ---
if ($Uninstall) {
    if (-not (Test-Path (Join-Path $Backup 'code.pak'))) { Stop-Install 'Le mod n''est pas installé dans ce dossier (pas de mod_backup).' }
    $installed = @()
    if (Test-Path $ListPath) { $installed = @([IO.File]::ReadAllLines($ListPath) | Where-Object { $_ }) }
    $originals = @(Get-RelativeFiles $Backup | Where-Object { $_ -ne 'fichiers_du_mod.txt' })
    $removed = 0
    foreach ($rel in $installed) {
        # fichier ajouté par le mod : retiré. Ceux qui remplaçaient un original sont remis juste après.
        $target = Join-Path $GameDir $rel
        if (($originals -notcontains $rel) -and (Test-Path $target)) { Remove-Item $target -Force; $removed++ }
    }
    foreach ($rel in $originals) {
        Copy-Item (Join-Path $Backup $rel) (Join-Path $GameDir $rel) -Force
    }
    Remove-Item $Backup -Recurse -Force
    Write-Host "Mod retiré : $($originals.Count) fichiers d'origine remis, $removed fichiers du mod supprimés." -ForegroundColor Green
    Write-Host 'Tes sauvegardes (*.dat) sont restées en place.'
    if (-not $Quiet) { Read-Host 'Appuie sur Entrée pour fermer' | Out-Null }
    exit 0
}

# --- installation ---
$classFiles = @(Get-ChildItem $Classes -Filter *.class -File)
$dataFiles  = @(Get-RelativeFiles $Data)
if ($classFiles.Count -eq 0 -or $dataFiles.Count -eq 0) { Stop-Install 'Le paquet du mod est incomplet (dossiers classes et data).' }

# Copie des originaux, une seule fois : une mise à jour du mod repart toujours du jeu d'origine
$first = -not (Test-Path (Join-Path $Backup 'code.pak'))
if ($first) {
    New-Item -ItemType Directory -Force $Backup | Out-Null
    Copy-Item (Join-Path $GameDir 'code_inf.pak') $Backup -Force
    Copy-Item (Join-Path $GameDir 'code.pak') $Backup -Force
}
$oldServer = Get-ServerLine (Join-Path $GameDir 'Data\online.scr')   # mise à jour : l'adresse déjà réglée
$known = @()
if (Test-Path $ListPath) { $known = @([IO.File]::ReadAllLines($ListPath) | Where-Object { $_ }) }
foreach ($rel in $dataFiles) {
    $target = Join-Path $GameDir $rel
    $saved  = Join-Path $Backup $rel
    # un fichier du jeu que le mod remplace : gardé, sauf s'il vient déjà d'une version précédente du mod
    if ((Test-Path $target) -and -not (Test-Path $saved) -and ($known -notcontains $rel)) {
        New-Item -ItemType Directory -Force (Split-Path -Parent $saved) | Out-Null
        Copy-Item $target $saved -Force
    }
}

$sha = (Get-FileHash (Join-Path $Backup 'code.pak') -Algorithm SHA256).Hash.ToLower()
if ($modInfo['code_pak_sha256'] -and $sha -ne $modInfo['code_pak_sha256']) {
    Write-Host ''
    Write-Host 'ATTENTION : ton code.pak n''est pas celui de la version du jeu pour laquelle ce mod est fait' -ForegroundColor Yellow
    Write-Host '(autre édition du jeu, ou fichier déjà modifié). Le jeu risque de ne pas démarrer.' -ForegroundColor Yellow
    Write-Host 'La désinstallation (Desinstaller.bat) remet tes fichiers comme avant.'
    if (-not $Quiet -and (Read-Host 'Installer quand même ? (o/n)') -notmatch '^[oOyY]') {
        if ($first) { Remove-Item $Backup -Recurse -Force }
        Stop-Install 'Installation annulée, rien n''a été changé.'
    }
}

# code.pak : l'original + les classes du mod
$pak = Join-Path $GameDir 'code.pak'
Copy-Item (Join-Path $Backup 'code.pak') $pak -Force
$zip = [IO.Compression.ZipFile]::Open($pak, 'Update')
try {
    foreach ($cls in $classFiles) {
        $old = $zip.GetEntry($cls.Name)
        if ($old) { $old.Delete() }
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $cls.FullName, $cls.Name) | Out-Null
    }
} finally { $zip.Dispose() }

# code_inf.pak : la liste de toutes les classes de code.pak (une par ligne, CRLF, tri sans casse). Le moteur
# s'en sert pour créer un objet par son nom : une classe absente de la liste fait planter le jeu.
$zip = [IO.Compression.ZipFile]::OpenRead($pak)
try {
    [string[]]$names = @($zip.Entries | Where-Object { $_.FullName.EndsWith('.class') } | ForEach-Object { $_.FullName })
} finally { $zip.Dispose() }
[Array]::Sort($names, [StringComparer]::OrdinalIgnoreCase)
[IO.File]::WriteAllText((Join-Path $GameDir 'code_inf.pak'), (($names | ForEach-Object { "$_`r`n" }) -join ''), [Text.Encoding]::ASCII)

# Données
foreach ($rel in $dataFiles) {
    $target = Join-Path $GameDir $rel
    New-Item -ItemType Directory -Force (Split-Path -Parent $target) | Out-Null
    Copy-Item (Join-Path $Data $rel) $target -Force
}
New-Item -ItemType Directory -Force $Backup | Out-Null
[IO.File]::WriteAllLines($ListPath, [string[]]@(@($known + $dataFiles) | Sort-Object -Unique))

# Adresse du serveur : celle demandée ici, sinon celle du paquet, sinon celle déjà réglée dans le jeu, sinon on la demande
$onlinePath = Join-Path $GameDir 'Data\online.scr'
$packed = Get-ServerLine $onlinePath
$address = $Server
$udpPort = $Port
if (-not $address -and $packed -and $packed.Host -ne $LocalHost) { $address = $packed.Host; if ($udpPort -le 0) { $udpPort = $packed.Port } }
if (-not $address -and $oldServer -and $oldServer.Host -ne $LocalHost) { $address = $oldServer.Host; if ($udpPort -le 0) { $udpPort = $oldServer.Port } }
if (-not $address -and -not $Quiet) {
    Write-Host ''
    Write-Host 'Adresse du serveur de jeu en ligne : celui qui l''héberge te la donne (un nom ou une adresse IP).'
    $address = (Read-Host 'Adresse du serveur (Entrée pour la régler plus tard)').Trim()
}
if ($udpPort -le 0) { $udpPort = if ($packed) { $packed.Port } else { 7777 } }
if ($address) {
    if ($address -notmatch '^[A-Za-z0-9.\-]+$') { Stop-Install "« $address » n'est pas une adresse de serveur (lettres, chiffres, points et tirets)." }
    $text = [IO.File]::ReadAllText($onlinePath, $Enc)
    $text = [regex]::Replace($text, '(?m)^Server\("[^"]*",\s*\d+\)', "Server(`"$address`", $udpPort)")
    [IO.File]::WriteAllText($onlinePath, $text, $Enc)
}

Write-Host ''
Write-Host "Mod installé (version en ligne $($modInfo['version']), paquet du $($modInfo['date'])) : $($classFiles.Count) classes, $($dataFiles.Count) fichiers." -ForegroundColor Green
if ($address) {
    Write-Host "Serveur en ligne : $address, port UDP $udpPort"
} else {
    Write-Host 'Serveur en ligne : pas encore réglé. Relance Installer.bat quand tu as l''adresse,'
    Write-Host 'ou écris-la dans Data\online.scr, ligne Server("adresse", 7777).'
}
Write-Host 'Lance le jeu comme d''habitude : le bouton « en ligne » est au menu principal.'
Write-Host 'Fichiers d''origine gardés dans mod_backup : Desinstaller.bat les remet.'
if (-not $Quiet) { Read-Host 'Appuie sur Entrée pour fermer' | Out-Null }
exit 0
