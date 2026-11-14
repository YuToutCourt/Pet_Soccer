# Développement du mod

Guide technique : dossiers de travail, build, liste détaillée des mods et des fichiers touchés, diagnostic.
Pour la présentation du mod, voir le [README](../README.md).

Moteur natif ChromeEngine + logique de jeu en Java (JRE 1.4 embarqué).

## Organisation

| Dossier | Contenu |
|---|---|
| `game_pristine\` | copie intacte du jeu installé (référence, ne jamais modifier) |
| `game_dev\` | copie de test, **c'est elle qui reçoit les mods** |
| `mods\java\` | classes Java modifiées (sources) |
| `mods\data\` | fichiers de données copiés tels quels dans `game_dev\` (même arborescence) |
| `mods\build.ps1` | script de build |
| `decompiled\` | code Java décompilé de `code.pak` (lecture seule, sert de base aux mods) |
| `notes\` | documentation du reverse |
| `tools\` | JDK 8, JDK 21, Ghidra, Vineflower |

L'installation d'origine du jeu n'est jamais touchée : `game_pristine\` en est une copie.
`game_pristine`, `game_dev`, `tools`, `build`, `decompiled` et l'ISO ne sont pas versionnés.

## Lancer le jeu

**Version modée** (compile, injecte, puis lance) :

```powershell
powershell -ExecutionPolicy Bypass -File mods\build.ps1 -Run
```

**Sans recompiler** : double-cliquer sur `game_dev\PetSoccer.exe`.

**Version d'origine** : `game_pristine\PetSoccer.exe` (ou le raccourci de l'installation).

> `PetSoccer.exe` lance `PetSoccer.bum` (le vrai exécutable). `start.exe` n'est que l'autorun du CD.
> Le jeu doit être lancé **depuis son dossier** (le build le fait déjà).

## Build

```powershell
powershell -ExecutionPolicy Bypass -File mods\build.ps1           # build
powershell -ExecutionPolicy Bypass -File mods\build.ps1 -Run      # build + lancement
powershell -ExecutionPolicy Bypass -File mods\build.ps1 -Clean    # recopie game_dev depuis game_pristine, puis build
powershell -ExecutionPolicy Bypass -File mods\build.ps1 -Second   # build + second dossier game_dev_b (test du jeu en ligne à deux sur un PC)
powershell -ExecutionPolicy Bypass -File mods\package.ps1 -Server ton.adresse   # build + build\PetSoccerMod.zip, le mod à donner aux amis (notes\online.md)
```

Ce que fait le script :
1. repart du `code.pak` d'origine ;
2. compile `mods\java\*.java` avec `tools\jdk8` (`-source 1.3 -target 1.3`, contre le `rt.jar` du jeu) ;
3. remplace / ajoute les `.class` dans `game_dev\code.pak` ;
4. régénère `game_dev\code_inf.pak`, l'index des classes : sans lui, le moteur ne sait pas créer une classe ajoutée par son nom, et le jeu plante ;
5. copie `mods\data\` dans `game_dev\`.

**Modifier une classe** : copier `decompiled\<Classe>.java` dans `mods\java\`, l'éditer, relancer le build.
Marquer les changements avec un commentaire `// [mod]`.

**Fichiers `.scr`** : encodage Windows-1252 et fins de ligne CRLF à conserver.

**Après chaque modification dans `mods\`** (Java *ou* données comme `team_stats.scr`), relancer le build :
le jeu lit les fichiers de `game_dev\`, pas ceux de `mods\`.

## Mods actuels

| Mod | Fichiers | Détails |
|---|---|---|
| Mode debug + compteur FPS + toutes les équipes débloquées (`AllTeams(1)`) | `mods\data\debugconf.scr` | `notes\architecture.md`, `notes\cheats.md` |
| Bouton « match (mod) » | `mods\data\Data\texts.scr` | test de mod de données |
| Codes de triche restaurés | `mods\java\SoccerGame.java` | `notes\cheats.md` |
| Stats d'équipes effectives et éditables | `mods\data\Data\team_stats.scr` + `ETeams`, `Ball`, `Strategy`, `AIFootBaller` | `notes\team-stats.md` |
| Difficulté Expert / Maître / Légende (niveaux 8-10), IA plus réactive et plus précise, marquage progressif dès le niveau 5 | `mods\data\Data\difficulty.scr` + `ModDifficulty`, `CoreLogic`, `MenuOptionsMatch`, `Cheat` | `notes\difficulty.md` |
| Rôles (libéro, stoppeur, latéral, milieu, ailier, pivot…), schémas de jeu, marquage, écran « Tactique » (avant-match et pause) avec carte du terrain : rôle et position de chaque joueur, placements en jeu, au coup d'envoi, sur nos touches et nos dégagements | `mods\data\Data\roles.scr`, `texts.scr`, `Menu\scr\Tactics*.scr`, `Menu\tactics_field.png` (`mods/art/make_tactics_field.py`), `PreMatch*.scr`, `InGame*.scr` (`mods/ui/*.py`) + `ModTactics`, `MenuTactics`, `AITeam`, `AIMatch`, `MenuMain`, `MenuPreMatch`, `MenuInGame`, `SoccerMainMenuModule`, `SoccerInGameMenuModule` ; sauvegarde `game_dev\tactics.dat` | `notes\tactics.md` |
| Hors-jeu « doux » (personne ne campe derrière l'avant-dernier adversaire, l'IA ne sert plus un joueur hors-jeu) et passes longues moins précises | fin de `mods\data\Data\roles.scr` + `ModTactics`, `Strategy`, `AIFootBaller` | `notes\tactics.md` |
| Déplacements sans ballon : démarquage (ligne de passe libre), appels en profondeur, une-deux, passe dans la course (automatique sur S quand le coéquipier visé fait un appel) ; l'IA passe enfin au coéquipier le mieux placé | `mods\data\Data\roles.scr` (`Free`, `Runs`, `GiveGo`, `Drop` + réglages de fin) + `ModMovement`, `ModTactics`, `Strategy`, `AITeam`, `AIFootBaller` | `notes\tactics.md` |
| Classement Elo et rangs (Bois → Challenger) avec badges, affiché au menu et en fin de match | `mods\data\Data\elo.scr`, `Menu\scr\Main*.scr`, `Menu\logos\T_Rank_*.png` (`mods/art/make_rank_logos.py`) + `ModElo`, `AIMatch`, `MenuInGame`, `MenuMain` ; sauvegarde `game_dev\elo.dat` | `notes\elo.md` |
| Deux nouvelles équipes : Real Pandrid (panda) et Borussia Tigermund (tigre), modèles existants retexturés | `Textures\Tshirts\panda_*` / `tygrys_*`, `Menu\TShirtsIcons`, `Menu\logos\T_Logo_Pandrid` / `T_Logo_Tigermund` (`mods/art/make_team_textures.py`, `make_team_logos.py`), `texts.scr`, `team_stats.scr` + `EAnimals`, `ETeams`, `ETeamLogos`, `AIPanda`, `AITiger`, `SoccerModule`, `MenuChooseTeam`, `SoccerMenuShowObjectModule`, `PlayerSave`, `AIMatch`, `Watcher`, `Championships` | `notes\new-teams.md` |
| Statistiques de match (buteurs, passes, ballons récupérés, arrêts) en pause et en fin de match ; écran « Profil » (bilan + derniers matchs) au menu principal | `Menu\scr\InGame*.scr` (`mods/ui/make_stats_scr.py`), `Profile*.scr` (`mods/ui/make_profile_scr.py`), `Main*.scr`, `texts.scr` + `ModStats`, `MenuProfile`, `MenuInGame`, `MenuMain`, `SoccerMainMenuModule`, `SoccerFonts`, `AIMatch`, `AIFootBaller`, `Ball`, `ModElo` ; sauvegarde `game_dev\stats.dat` | `notes\stats.md` |
| Menu « Mods » : déblocages, FPS, debug, triches de match (vitesse, frappe, super imposé, supers infinis) qui rendent le match non classé | `Menu\scr\Mods*.scr` (`mods/ui/make_mods_scr.py`), `Main*.scr`, `texts.scr` + `ModOptions`, `MenuMods`, `MenuMain`, `SoccerMainMenuModule`, `SoccerGame`, `Cheat`, `AITeam`, `AIFootBaller`, `ModDifficulty`, `ModElo`, `ModStats`, `MenuInGame` ; sauvegarde `game_dev\mods.dat` | `notes\mods-menu.md` |
| Succès (39) : bandeau « Succès débloqué ! » en fin de match, liste page par page (Profil > « succès »), progression calculée sur l'historique | `mods\data\Data\achievements.scr`, `texts.scr`, `Menu\scr\Achievements*.scr` (`mods/ui/make_achievements_scr.py`), `Profile*.scr`, `InGame*.scr` (`mods/ui/make_achievement_banner_scr.py`) + `ModAchievements`, `MenuAchievements`, `ModStats`, `MenuInGame`, `MenuProfile`, `MenuMain`, `SoccerMainMenuModule`, `AIFootBaller`, `ModElo` ; sauvegarde `game_dev\achievements.dat` | `notes\achievements.md` |
| Mode Survie : manches enchaînées tant qu'on ne perd pas, IA de niveau 1 à 10 puis plus rapide et plus puissante, but en or en cas d'égalité, record (Profil) et 3 succès | `mods\data\Data\survival.scr`, `texts.scr`, `achievements.scr`, `Menu\scr\Survival*.scr` (`mods/ui/make_survival_scr.py`), `Main*.scr` (`mods/ui/add_survival_button.py`), `Profile*.scr` + `ModSurvival`, `MenuSurvival`, `CoreLogic`, `MenuMain`, `SoccerMainMenuModule`, `MenuChooseTeamColors`, `MenuPreMatch`, `Match`, `AIMatch`, `MenuInGame`, `ModDifficulty`, `ModElo`, `ModStats`, `ModAchievements` | `notes\survival.md` |
| Mode Ligue (bouton « ligue ») : ligue à 14 en aller simple puis playoffs à double élimination (8 qualifiés), matchs IA simulés d'après les stats d'équipes, but en or en playoffs ; le championnat d'origine ne change pas | `mods\data\Data\league.scr`, `texts.scr`, `Menu\scr\League*.scr` (`mods/ui/make_league_scr.py`), `Main*.scr` (`mods/ui/add_league_button.py`) + `ModLeague`, `ModLeagueMode`, `MenuLeague`, `MenuLeagueBracket`, `CoreLogic`, `MenuMain`, `SoccerMainMenuModule`, `MenuChooseTeamColors`, `MenuPreMatch`, `Match`, `AIMatch`, `MenuInGame`, `ModStats` ; sauvegarde `game_dev\league.dat` | `notes\league.md` |
| Multijoueur en ligne, **à valider en jeu** : bouton « en ligne », serveur de salons à code (`python server\server.py`) qui met les deux jeux en liaison directe ou relaie leurs paquets, choix de l'équipe, de la tactique, du stade et de la durée dans le salon ; « partie rapide » classée (file d'attente par Elo, Elo en ligne gardé par le serveur, classement, forfait sur abandon ou coupure, compte du joueur dans `online.dat`) ; match complet entre deux jeux (l'hôte simule, le client envoie ses touches et rejoue l'état, les effets, les sons, le HUD et les menus reçus) ; pause de 30 s à la mi-temps pour revoir sa tactique ; triches coupées en ligne. Test : `mods\online-test.ps1 -Menus`. Paquet pour les amis : `mods\package.ps1` (installeur dans `mods\installer`) | `mods\data\Data\online.scr`, `texts.scr`, `Menu\scr\Online*.scr` (`mods/ui/make_online_scr.py`), `Main*.scr` (`mods/ui/add_online_button.py`) + `ModNet`, `ModLobby`, `MenuOnline`, `NetSnap`, `TeamController`, `Test1`, `FootBaller`, `Ball`, `Individual`, `Match`, `AIMatch`, `Recorder`, `ScriptedEvent`, `PetSoccerHud`, `Commentator`, `MenuInGame`, `MenuMain`, `MenuChooseTeam`, `MenuChooseTeamColors`, `MenuTactics`, `ModTactics`, `ModOptions`, `CoreLogic`, `SoccerMainMenuModule`, `SoccerCamera`, `SoccerModule`, `SoccerGame`, `SoccerFonts` ; serveur `server\server.py`, `rooms.py`, `protocol.py`, `store.py`, tests `server\test_server.py` ; `build.ps1 -Second` | `notes\online.md` |

## Contrôles (joueur 1, par défaut)

Flèches = déplacement, `S` = passe, `D` = tir, `A` = tête, `F` = super.

- **Feinte** : `A` *avec* le ballon, quand le joueur actif adverse est devant toi (à moins de 90° de ta direction) et à
  moins de 300 unités. Hors de cette zone, la touche ne fait rien. Réussite : 99 % au niveau 1, 40 % au niveau 7, 25 % au niveau 10.
  Contre le gardien, elle réussit toujours.
- **Tête** : `A` (ou `D`) *sans* le ballon, quand il est en l'air et à moins de 300 unités de ton joueur. Appuie *avant*
  que le ballon arrive, puis la tête part toute seule à hauteur de tête. À moins de 1200 du but adverse, c'est un tir ;
  plus loin, c'est une passe de la tête. Sans appui, le joueur contrôle de la poitrine. Pas deux têtes à la suite.

Codes de triche : `L` + code + **Shift gauche** (ex. `L111` au menu principal = toutes les équipes). Liste complète dans `notes\cheats.md`.

## Diagnostic

Après un lancement, regarder dans `game_dev\` :
- `crash.log` : journal Java (les lignes `[mod] ...` viennent des mods) ;
- `FXLog.txt`, `TexError.log` : effets et textures.

Avertissements normaux (déjà présents dans le jeu d'origine) : `data/music/Menu.wav` manquant,
`cannot open file data/sounds/commentary/*.wav` (commentaires audio non installés).

**Lancer directement un match** (sans passer par les menus) : ajouter dans `game_dev\debugconf.scr`

```
!MenuOn(i)
!FirstLevel(i)
MenuOn(0)
FirstLevel(7)
```

(le build remet la version de `mods\data\` au prochain lancement).

## Outils

- `tools\jdk8` : recompilation (seul JDK capable de produire du bytecode lisible par Java 1.4).
- `tools\jdk21` + `tools\ghidra_12.1.4_PUBLIC` : reverse natif avec **rea** (`npx -y rea-agents@6.8.0 ...`),
  via les variables utilisateur `JAVA_HOME` et `GHIDRA_INSTALL_DIR`.
- Vineflower : décompilation de `code.pak` vers `decompiled\`.
