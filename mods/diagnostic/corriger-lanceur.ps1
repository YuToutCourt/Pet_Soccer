# Pet Soccer - corrige le lanceur PetSoccer.exe qui plante sans message quand la variable PATH de Windows est longue.
# Cause : PetSoccer.exe recopie PATH dans un tampon fixe de 1024 octets ("PATH=%s;%sjre\bin\classic").
# Correctif : "%s;" devient "%.s" (zero caractere), le jeu recoit seulement son propre dossier jre\bin\classic.
# Garde une copie dans PetSoccer.exe.orig. Relancer le script ne change rien de plus.
param([string]$Game = $PSScriptRoot)

$exe = Join-Path $Game 'PetSoccer.exe'
if (-not (Test-Path -LiteralPath $exe)) { Write-Host "PetSoccer.exe introuvable dans $Game"; exit 1 }

$enc = [System.Text.Encoding]::ASCII
$old = $enc.GetBytes('PATH=%s;%sjre\bin\classic')
$new = $enc.GetBytes('PATH=%.s%sjre\bin\classic')
$bytes = [System.IO.File]::ReadAllBytes($exe)

function Find-Bytes([byte[]]$hay, [byte[]]$needle) {
    $hits = @()
    for ($i = 0; $i -le $hay.Length - $needle.Length; $i++) {
        $ok = $true
        for ($j = 0; $j -lt $needle.Length; $j++) { if ($hay[$i + $j] -ne $needle[$j]) { $ok = $false; break } }
        if ($ok) { $hits += $i }
    }
    return ,$hits
}

if ((Find-Bytes $bytes $new).Count -eq 1) { Write-Host 'PetSoccer.exe est deja corrige.'; exit 0 }
$hits = Find-Bytes $bytes $old
if ($hits.Count -ne 1) { Write-Host 'Version de PetSoccer.exe inconnue : rien n''a ete modifie.'; exit 1 }

$backup = "$exe.orig"
if (-not (Test-Path -LiteralPath $backup)) { Copy-Item -LiteralPath $exe $backup }
[Array]::Copy($new, 0, $bytes, $hits[0], $new.Length)
try {
    [System.IO.File]::WriteAllBytes($exe, $bytes)
} catch {
    Write-Host "Impossible d'ecrire PetSoccer.exe ($($_.Exception.Message)). Ferme le jeu, ou lance en administrateur."
    exit 1
}
Write-Host "PetSoccer.exe corrige (copie d'origine : PetSoccer.exe.orig). Tu peux lancer le jeu normalement."
