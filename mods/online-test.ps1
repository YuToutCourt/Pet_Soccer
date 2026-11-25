<#
  Lance un match en ligne de test sur ce PC : l'hôte dans game_dev, le client dans game_dev_b,
  tous les deux en fenêtré (voir notes\online.md).

  Avant : mods\build.ps1 -Second   (crée / met à jour game_dev_b)

  Trois façons de relier les deux jeux :
    (rien)   : en direct, sans serveur ni menus. Le match démarre tout de suite (équipes et stade fixes).
    -Menus   : par le serveur de salons lancé ici, avec les vrais menus. À toi de cliquer « en ligne » dans
               chaque fenêtre, de créer le salon dans l'une et de le rejoindre dans l'autre.
    -Server  : par le serveur de salons, sans toucher à la souris : game_dev crée le salon TEST, game_dev_b
               le rejoint, le match démarre, et chaque jeu revient au salon 5 s après la fin du match.
    -Ranked  : comme -Server, mais par la « partie rapide » : les deux jeux entrent dans la file d'attente,
               le serveur les réunit dans un salon classé (hôte tiré au sort) et le match compte pour l'Elo
               en ligne. -Bot et -HostBot valent alors pour les deux jeux.
    -League  : ligue en ligne (notes\online-league.md), sans souris : game_dev crée la ligue TEST, game_dev_b la
               rejoint, game_dev la lance quand les deux y sont, puis chacun ouvre le classement. Les ligues vont
               dans la base de test, avec les comptes.

  -Bot     : le client joue tout seul (utile pour regarder sans toucher au clavier)
  -HostBot : l'hôte fait une passe toutes les 2 s. À ajouter quand personne n'est au clavier de l'hôte : une
             touche ou un coup franc de son équipe attend sinon qu'on appuie sur la passe.
  -Port    : port UDP de l'hôte ou du serveur (7777 par défaut)
  -Half    : durée d'une mi-temps en secondes (0 = celle du profil ou du salon), pour un match de test court
  -Lag     : avec -Server ou -Menus, retard ajouté par le serveur à chaque paquet relayé, en ms
  -Loss    : avec -Server ou -Menus, pourcentage de paquets relayés perdus
  -Matches : avec -Server ou -Ranked, nombre de matchs enchaînés (1 par défaut)
  -TeamA, -TeamB : avec -Server ou -Ranked, équipe et tenue de game_dev et de game_dev_b, par exemple
             -TeamA '7,0' -TeamB '12,1'
  -QuitAt  : avec -Ranked, game_dev_b quitte le match tout seul après ce nombre de secondes (test du forfait)
  -NoDirect : avec -Server, -Menus ou -Ranked, pas de liaison directe : tout le match passe par le serveur.
             -Lag et -Loss la coupent aussi, puisqu'ils n'agissent que sur les paquets relayés.
  -CutDirect : la liaison directe casse 20 s après le début du match : les jeux doivent revenir au relais
  -NoCode  : avec -League, game_dev_b rejoint sans code (la dernière ligue ouverte)
  -Golden  : avec -League, but en or à égalité dans les matchs entre les deux jeux, même hors playoffs
  -NoSkip  : les jeux automatiques (-Bot, -HostBot) ne cliquent pas « continuer » à la mi-temps : la pause va
             au bout de son temps (HalfBreak de online.scr, 30 s)
  -Spectate : avec -Ranked ou -League, un troisième jeu (game_dev_c, mods\build.ps1 -Third) regarde le match en
             spectateur : il ouvre la liste des matchs en cours, prend le premier, puis revient à la liste.
  -SpectateAt : avec -Spectate, le spectateur n'est lancé que ce nombre de secondes après les deux joueurs
             (arrivée en plein match)

  Les comptes, l'Elo et les matchs de ces tests vont dans la base Postgres de test pet_soccer_test
  (server\testdb.py : le Postgres de docker-compose.yml, ou TEST_DATABASE_URL), jamais dans celle du vrai
  serveur. -ResetDb la vide avant de commencer. Dans le classement, game_dev_b s'appelle « invite » : les deux
  jeux ont sinon le même nom de profil.

  Le clavier va à la fenêtre active : clique sur celle du client pour jouer l'équipe de droite,
  sur celle de l'hôte pour jouer l'équipe de gauche.
  Ferme les deux jeux pour finir : le script remet alors game_dev comme avant et arrête le serveur.
#>
param([switch]$Bot, [switch]$HostBot, [int]$Port = 7777, [int]$Half = 0, [switch]$Server, [switch]$Menus,
      [switch]$Ranked, [int]$Lag = 0, [int]$Loss = 0, [int]$Matches = 1, [string]$TeamA = '', [string]$TeamB = '',
      [int]$QuitAt = 0, [switch]$NoDirect, [switch]$NoSkip, [switch]$CutDirect, [switch]$League, [switch]$Golden, [switch]$NoCode,
      [switch]$ResetDb, [switch]$Spectate, [int]$SpectateAt = 0)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$Dev  = Join-Path $Root 'game_dev'
$DevB = Join-Path $Root 'game_dev_b'
$enc  = [Text.Encoding]::GetEncoding(1252)   # les .scr sont en cp1252 + CRLF
$nl   = "`r`n"

if (-not (Test-Path $DevB)) { throw 'game_dev_b manque : lance d''abord mods\build.ps1 -Second' }
if (Get-Process 'PetSoccer.bum' -ErrorAction SilentlyContinue) { throw 'Pet Soccer est ouvert : ferme le jeu d''abord.' }
if (@($Server, $Menus, $Ranked, $League | Where-Object { $_ }).Count -gt 1) { throw '-Server, -Menus, -Ranked et -League ne vont pas ensemble.' }
$DevC = Join-Path $Root 'game_dev_c'
if ($Spectate -and -not ($Ranked -or $League)) { throw '-Spectate : seulement avec -Ranked ou -League (on ne regarde pas un salon privé).' }
if ($Spectate -and -not (Test-Path $DevC)) { throw 'game_dev_c manque : lance d''abord mods\build.ps1 -Third' }

function Write-DebugConf([string]$Dir, [string[]]$Lines) {
    # Les déclarations !Mot(types) doivent précéder les valeurs, sinon le parseur les ignore
    $decl = '!Debug(i)', '!ShowFPS(i)', '!AllTeams(i)', '!AllExtras(i)', '!MenuOn(i)', '!FirstLevel(i)',
            '!NetHost(i)', '!NetJoin(s,i)', '!NetBot(i)', '!NetHalf(i)',
            '!NetServer(s,i)', '!NetAuto(i)', '!NetCode(s)', '!NetTeam(i,i)', '!NetMatches(i)',
            '!NetName(s)', '!NetQuit(i)', '!NetDirect(i)', '!NetNoSkip(i)', '!NetGolden(i)'
    $body = @('Debug(1)', 'ShowFPS(1)', 'AllTeams(1)') + $Lines
    [IO.File]::WriteAllText((Join-Path $Dir 'debugconf.scr'), (($decl + '' + $body) -join $nl) + $nl, $enc)
}

function Set-Windowed([string]$Dir) {
    $path = Join-Path $Dir 'Data\VideoSettings.scr'
    $text = [IO.File]::ReadAllText($path, $enc) -replace '(?m)^Fullscreen\(\)', '//Fullscreen()'
    [IO.File]::WriteAllText($path, $text, $enc)
}

# game_dev retrouve son debugconf.scr et ses réglages vidéo à la fin (le jeu réécrit VideoSettings.scr en quittant)
$confPath  = Join-Path $Dev 'debugconf.scr'
$videoPath = Join-Path $Dev 'Data\VideoSettings.scr'
$confBak   = [IO.File]::ReadAllBytes($confPath)
$videoBak  = [IO.File]::ReadAllBytes($videoPath)
$serverProc = $null

try {
    if ($Server -or $Menus -or $Ranked -or $League) {
        # Les deux jeux passent par le serveur de salons, lancé ici. Port déjà pris (un vrai serveur lancé à la
        # main) : on s'arrête, sinon les jeux de test iraient sur lui et écriraient dans sa base.
        try { $probe = New-Object Net.Sockets.UdpClient($Port); $probe.Close() }
        catch { throw "Le port UDP $Port est deja pris (un serveur tourne deja ?) : arrete-le, ou choisis -Port." }
        $log = Join-Path $Root 'build\online-server.log'
        New-Item -ItemType Directory -Force (Split-Path $log) | Out-Null
        # base de test Postgres (server\testdb.py) : créée si besoin, vidée avec -ResetDb
        $dbArgs = @("$(Join-Path $Root 'server\testdb.py')")
        if (-not $ResetDb) { $dbArgs += '--keep' }
        $testDb = (& python @dbArgs | Select-Object -Last 1)
        if ($LASTEXITCODE -ne 0 -or -not $testDb) { throw 'Base de test injoignable : lancer docker compose up -d postgres' }
        $srvArgs = @("`"$(Join-Path $Root 'server\server.py')`"", '--port', $Port, '-v', '--db', "`"$testDb`"")
        if ($Server) { $srvArgs += @('--code', 'TEST') }
        if ($League) { $srvArgs += @('--league-code', 'TEST') }
        if ($Lag -gt 0) { $srvArgs += @('--lag', $Lag) }
        if ($Loss -gt 0) { $srvArgs += @('--loss', $Loss) }
        if ($NoDirect) { $srvArgs += '--no-direct' }
        $serverProc = Start-Process python -ArgumentList $srvArgs -PassThru -WindowStyle Hidden `
            -RedirectStandardOutput $log -RedirectStandardError (Join-Path $Root 'build\online-server.err.log')
        Write-Host "[serveur] port UDP $Port, journal : build\online-server.log"

        $common = @('MenuOn(1)', "NetServer(`"127.0.0.1`", $Port)")
        $hostLines = $common
        $client = $common + 'NetName("invite")'
        if ($Server) {
            $hostLines += @('NetAuto(1)', "NetMatches($Matches)")
            $client += @('NetAuto(2)', 'NetCode("TEST")')
        }
        if ($League) {
            # NetMatches : joueurs attendus avant que le créateur lance la ligue
            $hostLines += @('NetAuto(5)', 'NetMatches(2)')
            $client += @('NetAuto(6)', $(if ($NoCode) { 'NetCode("")' } else { 'NetCode("TEST")' }))
        }
        if ($Ranked) {
            $hostLines += @('NetAuto(4)', "NetMatches($Matches)")
            $client += @('NetAuto(4)', "NetMatches($Matches)")
            if ($QuitAt -gt 0) { $client += "NetQuit($QuitAt)" }
        }
        if ($TeamA -and -not $Menus) { $hostLines += "NetTeam($TeamA)" }
        if ($TeamB -and -not $Menus) { $client += "NetTeam($TeamB)" }
    } else {
        $hostLines = @('MenuOn(0)', 'FirstLevel(7)', "NetHost($Port)")
        $client = @('MenuOn(0)', 'FirstLevel(7)', "NetJoin(`"127.0.0.1`", $Port)")
    }
    if ($Ranked -or $League) {
        # l'hôte est tiré au sort par le serveur : mi-temps de test et jeu automatique valent pour les deux jeux
        if ($Half -gt 0) { $hostLines += "NetHalf($Half)"; $client += "NetHalf($Half)" }
        if ($Bot -or $HostBot) { $hostLines += 'NetBot(1)'; $client += 'NetBot(1)' }
    } else {
        if ($Half -gt 0) { $hostLines += "NetHalf($Half)" }
        if ($Bot) { $client += 'NetBot(1)' }
        if ($HostBot) { $hostLines += 'NetBot(1)' }
    }
    if ($NoSkip) { $hostLines += 'NetNoSkip(1)'; $client += 'NetNoSkip(1)' }
    if ($Golden) { $hostLines += 'NetGolden(1)'; $client += 'NetGolden(1)' }
    if ($CutDirect) { $hostLines += 'NetDirect(2)'; $client += 'NetDirect(2)' }
    Write-DebugConf $Dev $hostLines
    Write-DebugConf $DevB $client
    Set-Windowed $Dev
    Set-Windowed $DevB

    Write-Host '[hôte]   game_dev   (équipe de gauche)'
    Start-Process (Join-Path $Dev 'PetSoccer.exe') -WorkingDirectory $Dev
    Start-Sleep -Seconds 3
    Write-Host '[client] game_dev_b (équipe de droite)'
    Start-Process (Join-Path $DevB 'PetSoccer.exe') -WorkingDirectory $DevB
    if ($Spectate) {
        # le spectateur ouvre la liste des matchs en cours et regarde le premier (NetAuto 7)
        Write-DebugConf $DevC @('MenuOn(1)', "NetServer(`"127.0.0.1`", $Port)", 'NetName("spectateur")', 'NetAuto(7)', 'NetMatches(1)')
        Set-Windowed $DevC
        if ($SpectateAt -gt 0) { Start-Sleep -Seconds $SpectateAt }
        Write-Host '[spectateur] game_dev_c'
        Start-Process (Join-Path $DevC 'PetSoccer.exe') -WorkingDirectory $DevC
    }

    if ($Menus) {
        Write-Host 'Dans chaque fenêtre : « en ligne », puis « créer » dans l''une et le code + « rejoindre » dans l''autre.'
        Write-Host 'Ou « partie rapide » dans les deux : partie classée, comptée dans la base de test.'
    }
    Write-Host 'Ferme les deux jeux pour finir.'
    Start-Sleep -Seconds 10
    while (Get-Process 'PetSoccer.bum' -ErrorAction SilentlyContinue) { Start-Sleep -Seconds 1 }
} finally {
    Start-Sleep -Seconds 1
    if ($serverProc -and -not $serverProc.HasExited) { Stop-Process -Id $serverProc.Id -Force }
    [IO.File]::WriteAllBytes($confPath, $confBak)
    [IO.File]::WriteAllBytes($videoPath, $videoBak)
    Write-Host '[ok] game_dev remis comme avant'
}
