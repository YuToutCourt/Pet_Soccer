# Pet Soccer - diagnostic de lancement.
# A mettre dans le dossier du jeu (a cote de PetSoccer.exe), puis double-cliquer sur Diagnostic.bat.
# Ecrit un rapport "rapport-petsoccer.txt" sur le Bureau. Ne modifie rien dans le jeu.
param([string]$Game = $PSScriptRoot, [switch]$NoLaunch, [switch]$NoOpen)

$ErrorActionPreference = 'Continue'
$lines = New-Object System.Collections.Generic.List[string]
function Say([string]$s) { $lines.Add($s); Write-Host $s }

Say "=== Pet Soccer - diagnostic $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ==="
Say "Dossier du jeu : $Game"

# --- Systeme ---
$os = Get-CimInstance Win32_OperatingSystem
Say "Windows : $($os.Caption) $($os.Version) ($($os.OSArchitecture))"
$cpu = Get-CimInstance Win32_Processor | Select-Object -First 1
Say "Processeur : $($cpu.Name.Trim()) - $($cpu.NumberOfLogicalProcessors) coeurs logiques"
foreach ($g in Get-CimInstance Win32_VideoController) {
    Say "Carte graphique : $($g.Name) - pilote $($g.DriverVersion) - $($g.CurrentHorizontalResolution)x$($g.CurrentVerticalResolution)"
}

# --- Fichiers du jeu ---
Say ""
Say "--- Fichiers ---"
$need = 'PetSoccer.exe','PetSoccer.bum','ChromeEngine.dll','KernelGL.dll','MSVCP60.DLL','msvcrt.dll',
        'ogg.dll','vorbis.dll','vorbisfile.dll','code.pak','game.ini','jre\bin\classic\jvm.dll','jre\bin\java.dll','jre\lib\rt.jar'
foreach ($f in $need) {
    $p = Join-Path $Game $f
    if (Test-Path -LiteralPath $p) {
        $len = (Get-Item -LiteralPath $p).Length
        $motw = ''
        try { if (Get-Item -LiteralPath $p -Stream Zone.Identifier -ErrorAction Stop) { $motw = '  [bloque : telecharge depuis Internet]' } } catch {}
        Say ("OK       {0,-26} {1,10} octets{2}" -f $f, $len, $motw)
    } else {
        Say "MANQUANT $f"
    }
}
if (-not (Test-Path -LiteralPath (Join-Path $Game 'Data'))) { Say "MANQUANT dossier Data" }

# --- Protections Windows ---
Say ""
Say "--- Protections Windows ---"
try {
    $sac = (Get-ItemProperty 'HKLM:\SYSTEM\CurrentControlSet\Control\CI\Policy' -ErrorAction Stop).VerifiedAndReputablePolicyState
    $txt = switch ($sac) { 0 {'desactive'} 1 {'ACTIVE (peut bloquer le jeu sans message)'} 2 {'en evaluation (peut bloquer le jeu)'} default {"inconnu ($sac)"} }
    Say "Smart App Control : $txt"
} catch { Say "Smart App Control : absent" }
try {
    $hits = Get-MpThreatDetection -ErrorAction Stop | Where-Object { ($_.Resources -join ' ') -match 'Pet ?Soccer|\.bum|ChromeEngine|KernelGL' }
    if ($hits) { foreach ($h in $hits) { Say "Defender a bloque : $($h.Resources -join ', ') le $($h.InitialDetectionTime)" } }
    else { Say "Defender : aucune detection sur le jeu" }
} catch { Say "Defender : impossible de lire l'historique ($($_.Exception.Message))" }

# --- Lancement ---
$start = Get-Date
if (-not $NoLaunch) {
    Say ""
    Say "--- Lancement de PetSoccer.bum (comme le fait PetSoccer.exe) ---"
    $gameSlash = $Game.TrimEnd('\') + '\'
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = Join-Path $Game 'PetSoccer.bum'
    $psi.WorkingDirectory = $Game
    $psi.UseShellExecute = $false
    $psi.EnvironmentVariables['PATH'] = "$gameSlash;$($gameSlash)jre\bin\classic;$env:PATH"
    try {
        $proc = [System.Diagnostics.Process]::Start($psi)
        if ($proc.WaitForExit(20000)) {
            $code = $proc.ExitCode
            $hex = '0x{0:X8}' -f $code
            $why = switch ($hex) {
                '0xC0000135' {'une DLL est introuvable'}
                '0xC000007B' {'une DLL est abimee ou de la mauvaise architecture'}
                '0xC0000005' {'plantage (violation d acces)'}
                '0xC0000409' {'plantage (depassement de pile / tampon)'}
                '0xC0000142' {'echec d initialisation d une DLL'}
                default {''}
            }
            Say "Le jeu s'est ferme tout seul apres $([int]((Get-Date)-$start).TotalSeconds) s, code $code ($hex) $why"
        } else {
            Say "Le jeu tourne encore apres 20 s : il a demarre. (Ferme-le normalement.)"
        }
    } catch {
        Say "Impossible de lancer PetSoccer.bum : $($_.Exception.Message)"
    }
}

# --- Journal d'erreurs Windows ---
Say ""
Say "--- Journal Windows (plantages de PetSoccer, 7 derniers jours) ---"
try {
    $ev = Get-WinEvent -FilterHashtable @{ LogName = 'Application'; StartTime = (Get-Date).AddDays(-7) } -ErrorAction Stop |
          Where-Object { $_.Id -in 1000,1001,1005,1026 -and $_.Message -match 'PetSoccer' } | Select-Object -First 4
    if ($ev) { foreach ($e in $ev) { Say "[$($e.TimeCreated)] Id $($e.Id)"; Say $e.Message; Say "" } }
    else { Say "Aucun plantage de PetSoccer enregistre." }
} catch { Say "Aucun evenement lisible ($($_.Exception.Message))" }

# --- Journaux du jeu ---
Say ""
Say "--- Journaux du jeu ---"
$dirs = @($Game)
$vs = Join-Path $env:LOCALAPPDATA ('VirtualStore\' + ($Game -replace '^[A-Za-z]:\\', ''))
if (Test-Path -LiteralPath $vs) { $dirs += $vs }
$found = $false
foreach ($d in $dirs) {
    foreach ($n in 'crash.log','FXLog.txt','TexError.log','benchmark.log','loadtime.txt','gl_ext.txt') {
        $p = Join-Path $d $n
        if (Test-Path -LiteralPath $p) {
            $found = $true
            Say "== $p ($((Get-Item -LiteralPath $p).LastWriteTime))"
            if ($n -ne 'gl_ext.txt') { Get-Content -LiteralPath $p -Tail 40 | ForEach-Object { Say $_ } }
        }
    }
}
if (-not $found) { Say "Aucun journal : le jeu s'arrete avant d'ecrire quoi que ce soit." }

$out = Join-Path ([Environment]::GetFolderPath('Desktop')) 'rapport-petsoccer.txt'
$lines | Set-Content -LiteralPath $out -Encoding UTF8
Write-Host ""
Write-Host "Rapport ecrit dans : $out"
if (-not $NoOpen) { Start-Process notepad.exe $out }
