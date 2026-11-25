<#
  Construit le jeu modé dans game_dev à partir de game_pristine + mods.

  mods\java\*.java  -> compilés (JDK 8, cible 1.3) et injectés dans game_dev\code.pak
  mods\data\...     -> copiés tels quels par-dessus game_dev (même arborescence que le dossier game)

  -Clean  : remet game_dev à l'identique de game_pristine avant d'appliquer les mods
  -Run    : lance le jeu après le build
  -Second : recopie game_dev dans game_dev_b, un second dossier de jeu pour tester le multijoueur en ligne
            à deux sur le même PC (ses sauvegardes, son debugconf.scr et ses réglages vidéo lui restent propres)
  -Third  : de même dans game_dev_c, un troisième jeu qui regarde le match en spectateur (online-test.ps1 -Spectate)
#>
param([switch]$Clean, [switch]$Run, [switch]$Second, [switch]$Third)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$Root     = Split-Path -Parent $PSScriptRoot
$Pristine = Join-Path $Root 'game_pristine'
$Dev      = Join-Path $Root 'game_dev'
$Javac    = Join-Path $Root 'tools\jdk8\bin\javac.exe'
$Classes  = Join-Path $Root 'build\classes'
$JavaSrc  = Join-Path $PSScriptRoot 'java'
$DataSrc  = Join-Path $PSScriptRoot 'data'

# Le jeu verrouille code.pak : un build lancé jeu ouvert laisserait game_dev à moitié construit
if (Get-Process 'PetSoccer.bum' -ErrorAction SilentlyContinue) {
    throw 'Pet Soccer est ouvert : ferme le jeu avant de lancer le build.'
}

if ($Clean) {
    Write-Host '[clean] game_dev <- game_pristine'
    robocopy $Pristine $Dev /MIR /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "robocopy a échoué ($LASTEXITCODE)" }
}

# --- Java : compile et injecte dans une copie fraîche de code.pak ---
$pak = Join-Path $Dev 'code.pak'
Copy-Item (Join-Path $Pristine 'code.pak') $pak -Force

$sources = @(Get-ChildItem $JavaSrc -Recurse -Filter *.java -ErrorAction SilentlyContinue)
if ($sources.Count -gt 0) {
    if (Test-Path $Classes) { Remove-Item $Classes -Recurse -Force }
    New-Item -ItemType Directory -Force $Classes | Out-Null

    Write-Host "[javac] $($sources.Count) fichier(s)"
    & $Javac -nowarn -source 1.3 -target 1.3 -encoding UTF-8 `
        -bootclasspath (Join-Path $Pristine 'jre\lib\rt.jar') `
        -classpath (Join-Path $Pristine 'code.pak') `
        -d $Classes $sources.FullName
    if ($LASTEXITCODE -ne 0) { throw 'Échec de compilation' }

    $zip = [IO.Compression.ZipFile]::Open($pak, 'Update')
    try {
        foreach ($cls in Get-ChildItem $Classes -Recurse -Filter *.class) {
            $name = $cls.FullName.Substring($Classes.Length + 1).Replace('\', '/')
            $old = $zip.GetEntry($name)
            if ($old) { $old.Delete(); $tag = 'remplace' } else { $tag = 'ajoute' }
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $cls.FullName, $name) | Out-Null
            Write-Host "  [$tag] $name"
        }
    } finally { $zip.Dispose() }
}

# --- Index des classes : code_inf.pak liste toutes les classes de code.pak (une par ligne, CRLF,
#     tri alphabétique sans casse). Le moteur s'en sert pour créer un objet par son nom
#     (CreateObject("AIPanda")) : une classe ajoutée absente de l'index fait planter le jeu. ---
$zip = [IO.Compression.ZipFile]::OpenRead($pak)
try {
    [string[]]$names = @($zip.Entries | Where-Object { $_.FullName.EndsWith('.class') } | ForEach-Object { $_.FullName })
} finally { $zip.Dispose() }
[Array]::Sort($names, [StringComparer]::OrdinalIgnoreCase)
[IO.File]::WriteAllText((Join-Path $Dev 'code_inf.pak'), (($names | ForEach-Object { "$_`r`n" }) -join ''), [Text.Encoding]::ASCII)
Write-Host "[index] code_inf.pak : $($names.Count) classes"

# --- Données : superpose mods\data sur game_dev ---
if (Test-Path $DataSrc) {
    Write-Host '[data] mods\data -> game_dev'
    robocopy $DataSrc $Dev /E /XX /NDL /NJH /NJS /NP
    if ($LASTEXITCODE -ge 8) { throw "robocopy a échoué ($LASTEXITCODE)" }
}

# --- Autres dossiers de jeu (multijoueur en ligne sur un seul PC) ---
function Copy-GameDir([string]$Name) {
    $DevB = Join-Path $Root $Name
    $first = -not (Test-Path $DevB)
    Write-Host "[copie] game_dev -> $Name"
    # Les fichiers exclus ne sont ni copiés ni supprimés : le dossier copié garde les siens.
    # Seuls les .dat et .log de la racine sont des sauvegardes et des journaux : ceux de Data\ (polices
    # de Data\Panel) font partie du jeu, d'où une liste de noms et pas *.dat.
    $own = @('debugconf.scr', 'VideoSettings.scr')
    foreach ($d in @($Dev, $DevB)) {
        if (Test-Path $d) {
            $own += @(Get-ChildItem $d -File | Where-Object { $_.Extension -in '.dat', '.log' } | ForEach-Object { $_.Name })
        }
    }
    $own = @($own | Sort-Object -Unique)
    robocopy $Dev $DevB /MIR /XF $own /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "robocopy a échoué ($LASTEXITCODE)" }
    if ($first) {
        # Premier passage : même profil et mêmes réglages que game_dev, mais en fenêtré
        Copy-Item (Join-Path $Dev '*.dat') $DevB
        Copy-Item (Join-Path $Dev 'debugconf.scr') $DevB
        $enc = [Text.Encoding]::GetEncoding(1252)
        $video = [IO.File]::ReadAllText((Join-Path $Dev 'Data\VideoSettings.scr'), $enc) -replace '(?m)^Fullscreen\(\)', '//Fullscreen()'
        [IO.File]::WriteAllText((Join-Path $DevB 'Data\VideoSettings.scr'), $video, $enc)
    }
}

if ($Second) { Copy-GameDir 'game_dev_b' }
if ($Third) { Copy-GameDir 'game_dev_c' }  # le spectateur des tests (online-test.ps1 -Spectate)

Write-Host '[ok] build terminé'

if ($Run) {
    Start-Process (Join-Path $Dev 'PetSoccer.exe') -WorkingDirectory $Dev
}
exit 0
