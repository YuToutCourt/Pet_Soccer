<#
  Lanceur du mod de Pet Soccer : met le mod à jour s'il le faut, puis lance le jeu.

  L'installeur le copie dans <jeu>\mod_maj, avec le mod.txt de la version installée, et crée Jouer.bat dans le
  dossier du jeu et le raccourci « Pet Soccer (mod) » sur le bureau.

  À chaque lancement : il demande à GitHub la dernière release du mod. Si son numéro (le tag, ex. 3.2) est plus
  grand que celui installé et qu'elle contient PetSoccerMod-update.zip, il le télécharge, vérifie son empreinte
  SHA-256, et lance son install.ps1 sans question (les sauvegardes et l'adresse du serveur sont gardées).
  Sans Internet, ou si quoi que ce soit échoue, le jeu se lance quand même avec la version déjà installée.

  -Game     : dossier du jeu (par défaut, le dossier parent de mod_maj)
  -Api      : adresse de la dernière release (pour les tests : un fichier JSON local en file:///)
  -NoLaunch : ne lance pas le jeu (tests)
#>
param([string]$Game = '', [string]$Api = 'https://api.github.com/repos/YuToutCourt/Pet_Soccer/releases/latest',
      [switch]$NoLaunch)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'   # la barre de progression de PowerShell 5 ralentit beaucoup les téléchargements
$AssetName = 'PetSoccerMod-update.zip'

if (-not $Game) { $Game = Split-Path -Parent $PSScriptRoot }

function Read-ModInfo([string]$Path) {
    $info = @{}
    if (Test-Path $Path) {
        foreach ($line in [IO.File]::ReadAllLines($Path)) {
            $parts = $line.Split('=', 2)
            if ($parts.Count -eq 2) { $info[$parts[0].Trim()] = $parts[1].Trim() }
        }
    }
    return $info
}

# Numéro de version (« 3.2 », « v3.2 ») en objet comparable, ou $null
function ConvertTo-Version([string]$Text) {
    $m = [regex]::Match([string]$Text, '\d+(\.\d+){0,3}')
    if (-not $m.Success) { return $null }
    $v = $m.Value
    if ($v -notmatch '\.') { $v += '.0' }
    return [version]$v
}

function Start-Game {
    if ($NoLaunch) { return }
    $exe = Join-Path $Game 'PetSoccer.exe'
    if (Test-Path $exe) {
        Start-Process -FilePath $exe -WorkingDirectory $Game
    } else {
        Write-Host "PetSoccer.exe introuvable dans « $Game »." -ForegroundColor Red
        Read-Host 'Appuie sur Entrée pour fermer' | Out-Null
    }
}

function Update-Mod {
    $installed = Read-ModInfo (Join-Path $PSScriptRoot 'mod.txt')
    $current = ConvertTo-Version $installed['release']
    Write-Host "Mod de Pet Soccer : version installée $(if ($current) { $current } else { 'inconnue' })"

    if (Get-Process 'PetSoccer.bum', 'PetSoccer' -ErrorAction SilentlyContinue) {
        Write-Host 'Le jeu est déjà ouvert : pas de mise à jour maintenant.'
        return
    }

    # GitHub n'accepte que TLS 1.2, que PowerShell 5 n'active pas toujours de lui-même
    [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
    Write-Host 'Recherche d''une mise à jour...'
    $release = Invoke-RestMethod -Uri $Api -UseBasicParsing -TimeoutSec 10 -Headers @{ 'User-Agent' = 'PetSoccerMod-updater' }
    $latest = ConvertTo-Version $release.tag_name
    if (-not $latest) { Write-Host "Release « $($release.tag_name) » sans numéro de version : ignorée."; return }
    if ($current -and $latest -le $current) { Write-Host 'Le mod est à jour.'; return }

    $asset = @($release.assets | Where-Object { $_.name -eq $AssetName }) | Select-Object -First 1
    if (-not $asset) { Write-Host "La version $latest n'a pas de $AssetName : rien à installer."; return }

    Write-Host "Nouvelle version $latest : téléchargement ($([Math]::Round($asset.size / 1MB, 1)) Mo)..." -ForegroundColor Yellow
    $work = Join-Path ([IO.Path]::GetTempPath()) ('PetSoccerMod-maj-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force $work | Out-Null
    try {
        $zip = Join-Path $work $AssetName
        Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $zip -UseBasicParsing -TimeoutSec 300 `
            -Headers @{ 'User-Agent' = 'PetSoccerMod-updater' }

        # empreinte donnée par GitHub (champ « digest ») : un téléchargement abîmé n'est pas installé
        if ($asset.digest -and ([string]$asset.digest).StartsWith('sha256:')) {
            $hash = (Get-FileHash $zip -Algorithm SHA256).Hash.ToLower()
            if ($hash -ne ([string]$asset.digest).Substring(7).ToLower()) { throw 'le fichier téléchargé est abîmé (empreinte SHA-256 différente)' }
        }

        Add-Type -AssemblyName System.IO.Compression.FileSystem
        [IO.Compression.ZipFile]::ExtractToDirectory($zip, $work)
        $install = @(Get-ChildItem $work -Recurse -Filter install.ps1 -File) | Select-Object -First 1
        if (-not $install) { throw "$AssetName ne contient pas install.ps1" }

        $arguments = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$($install.FullName)`"", '-Game', "`"$Game`"", '-Quiet')
        $probe = Join-Path $Game ('mod_ecriture_' + [Guid]::NewGuid().ToString('N') + '.tmp')
        $canWrite = $true
        try { [IO.File]::WriteAllText($probe, 'x'); Remove-Item $probe -Force } catch { $canWrite = $false }
        if ($canWrite) {
            & powershell @($arguments | ForEach-Object { $_.Trim('"') })
            $code = $LASTEXITCODE
        } else {
            # jeu dans Program Files : l'installation demande les droits d'administrateur
            Write-Host 'Windows va demander les droits d''administrateur pour écrire dans le dossier du jeu.'
            $code = (Start-Process powershell -Verb RunAs -Wait -PassThru -ArgumentList $arguments).ExitCode
        }
        if ($code -ne 0) { throw "l'installation de la version $latest a échoué (code $code)" }
        Write-Host "Mod mis à jour en version $latest." -ForegroundColor Green
    } finally {
        Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
    }
}

try {
    Update-Mod
} catch {
    Write-Host "Pas de mise à jour : $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host 'Le jeu se lance avec la version déjà installée.'
    if (-not $NoLaunch) { Start-Sleep -Seconds 3 }
}
Start-Game
exit 0
