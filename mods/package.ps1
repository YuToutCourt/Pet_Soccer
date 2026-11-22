<#
  Fabrique le paquet du mod à donner aux amis : build\PetSoccerMod.zip (voir notes\online.md, « Installeur »).

  Le paquet contient le mod, pas le jeu : les classes compilées, les fichiers de données de mods\data et
  l'installeur de mods\installer, qui les applique sur la copie du jeu de chacun. Rien de personnel n'y
  entre : ni sauvegardes (*.dat), ni compte en ligne (online.dat), ni debugconf.scr.

      powershell -ExecutionPolicy Bypass -File mods\package.ps1 -Server monadresse.duckdns.org

  -Server  : adresse du serveur écrite dans le online.scr du paquet (nom ou IP publique de celui qui héberge
             server\server.py). Sans elle, l'installeur la demande à chaque ami.
  -Port    : port UDP du serveur (7777 par défaut)
  -NoBuild : ne relance pas mods\build.ps1 avant (les classes de build\classes sont prises telles quelles)
  -Publish : publie le paquet sur GitHub (outil gh), dans la release dont le tag est la version du mod
             (ModVersion.RELEASE, ex. 3.2), sous le nom PetSoccerMod-update.zip. La release est créée si elle
             n'existe pas. Le lanceur des joueurs (update.ps1) la trouve et l'installe au prochain lancement.

  La version du mod (montrée au menu principal) est ModVersion.RELEASE dans mods\java\ModVersion.java :
  l'augmenter avant chaque nouveau paquet, sinon le lanceur des joueurs ne verra pas la mise à jour.
#>
param([string]$Server = '', [int]$Port = 7777, [switch]$NoBuild, [switch]$Publish)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$Root     = Split-Path -Parent $PSScriptRoot
$Pristine = Join-Path $Root 'game_pristine'
$Classes  = Join-Path $Root 'build\classes'
$DataSrc  = Join-Path $PSScriptRoot 'data'
$Tools    = Join-Path $PSScriptRoot 'installer'
$Out      = Join-Path $Root 'build\PetSoccerMod'
$Zip      = Join-Path $Root 'build\PetSoccerMod.zip'
$enc      = [Text.Encoding]::GetEncoding(1252)   # les .scr sont en cp1252 + CRLF
$bom      = New-Object Text.UTF8Encoding($true)

if ($Server -and $Server -notmatch '^[A-Za-z0-9.\-]+$') { throw "« $Server » n'est pas une adresse de serveur." }

if (-not $NoBuild) {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'build.ps1') | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Le build a échoué (jeu ouvert ?) : lance mods\build.ps1 pour voir pourquoi.' }
}
$classFiles = @(Get-ChildItem $Classes -Filter *.class -File -ErrorAction SilentlyContinue)
if ($classFiles.Count -eq 0) { throw 'build\classes est vide : lance d''abord mods\build.ps1.' }

if (Test-Path $Out) { Remove-Item $Out -Recurse -Force }
New-Item -ItemType Directory -Force (Join-Path $Out 'classes'), (Join-Path $Out 'data') | Out-Null

# Classes du mod
Copy-Item $classFiles.FullName (Join-Path $Out 'classes')

# Données : tout mods\data, sauf debugconf.scr (mode debug et réglages de test de ce PC)
robocopy $DataSrc (Join-Path $Out 'data') /E /XF debugconf.scr /NFL /NDL /NJH /NJS /NP | Out-Null
if ($LASTEXITCODE -ge 8) { throw "robocopy a échoué ($LASTEXITCODE)" }
$dataCount = @(Get-ChildItem (Join-Path $Out 'data') -Recurse -File).Count

# online.scr du paquet : l'adresse du serveur pour les amis. Sans -Server elle reste 127.0.0.1, et
# l'installeur la demande.
$onlinePath = Join-Path $Out 'data\Data\online.scr'
$text = [IO.File]::ReadAllText($onlinePath, $enc)
$address = if ($Server) { $Server } else { '127.0.0.1' }
$text = [regex]::Replace($text, '(?m)^Server\("[^"]*",\s*\d+\)', "Server(`"$address`", $Port)")
[IO.File]::WriteAllText($onlinePath, $text, $enc)

# Installeur. PowerShell 5 et le Bloc-notes ne lisent les accents d'un fichier UTF-8 qu'avec sa marque
# d'en-tête (BOM) ; cmd.exe veut des fins de ligne CRLF dans un .bat.
foreach ($name in 'install.ps1', 'update.ps1', 'LISEZMOI.txt') {
    $content = [IO.File]::ReadAllText((Join-Path $Tools $name), [Text.Encoding]::UTF8) -replace "`r?`n", "`r`n"
    [IO.File]::WriteAllText((Join-Path $Out $name), $content, $bom)
}
foreach ($name in 'Installer.bat', 'Desinstaller.bat', 'Jouer.bat') {
    $content = [IO.File]::ReadAllText((Join-Path $Tools $name), [Text.Encoding]::ASCII) -replace "`r?`n", "`r`n"
    [IO.File]::WriteAllText((Join-Path $Out $name), $content, [Text.Encoding]::ASCII)
}

# mod.txt : ce que l'installeur doit savoir du paquet
$version = [regex]::Match([IO.File]::ReadAllText((Join-Path $PSScriptRoot 'java\ModNet.java')), '_MOD_VERSION\s*=\s*(\d+)').Groups[1].Value
$sha = (Get-FileHash (Join-Path $Pristine 'code.pak') -Algorithm SHA256).Hash.ToLower()
$release = [regex]::Match([IO.File]::ReadAllText((Join-Path $PSScriptRoot 'java\ModVersion.java')), 'RELEASE\s*=\s*"([0-9.]+)"').Groups[1].Value
if (-not $release) { throw 'Version du mod introuvable (RELEASE dans mods\java\ModVersion.java).' }
$info = @("release=$release", "version=$version", "date=$(Get-Date -Format 'yyyy-MM-dd')", "code_pak_sha256=$sha")
[IO.File]::WriteAllLines((Join-Path $Out 'mod.txt'), [string[]]$info, [Text.Encoding]::ASCII)

# Rien de personnel dans le paquet : il part chez d'autres joueurs
$leaks = @(Get-ChildItem $Out -Recurse -File | Where-Object { $_.Extension -in '.dat', '.log', '.db' -or $_.Name -eq 'debugconf.scr' })
if ($leaks.Count -gt 0) { throw "Fichiers qui ne doivent pas partir : $($leaks.FullName -join ', ')" }

if (Test-Path $Zip) { Remove-Item $Zip -Force }
[IO.Compression.ZipFile]::CreateFromDirectory($Out, $Zip, [IO.Compression.CompressionLevel]::Optimal, $true)

$size = [Math]::Round((Get-Item $Zip).Length / 1MB, 1)
Write-Host "[ok] build\PetSoccerMod.zip : $size Mo, $($classFiles.Count) classes, $dataCount fichiers de données, version $release (en ligne $version)"
if ($Server) {
    Write-Host "     serveur écrit dans le paquet : $Server, port UDP $Port"
} else {
    Write-Host '     pas d''adresse de serveur dans le paquet (-Server) : l''installeur la demandera à chaque ami'
}

if ($Publish) {
    # le nom que cherche le lanceur des joueurs (update.ps1)
    $asset = Join-Path $Root 'build\PetSoccerMod-update.zip'
    Copy-Item $Zip $asset -Force
    # par cmd : sous PowerShell 5, rediriger la sortie d'erreur d'un programme avec « Stop » arrête le script
    & cmd /c "gh release view $release >nul 2>&1"
    if ($LASTEXITCODE -eq 0) {
        & gh release upload $release $asset --clobber
    } else {
        & gh release create $release $asset --title "pet_soccer_mod_V$release" --notes "Mod de Pet Soccer, version $release. Avec le raccourci « Pet Soccer (mod) », le jeu se met à jour tout seul."
    }
    if ($LASTEXITCODE -ne 0) { throw 'La publication sur GitHub a échoué (gh auth status ?).' }
    Write-Host "[ok] publié sur GitHub : release $release, PetSoccerMod-update.zip"
}
exit 0
