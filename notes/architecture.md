# Architecture du code Java

`code.pak` = ZIP de 428 `.class`, package par défaut, bytecode Java 1.1 (version 45) avec
LineNumberTable + LocalVariableTable → décompilation Vineflower quasi fidèle (`decompiled/`, 399 fichiers, 0 échec).
Commentaires/chaînes internes en polonais.

`code_inf.pak` = **index texte** des classes de `code.pak` (un nom `X.class` par ligne, CRLF, tri alphabétique sans casse,
classes internes `A$B.class` comprises). Le moteur natif s'en sert pour `CreateObject("NomDeClasse")`.
Une classe ajoutée seulement dans `code.pak` se charge si du Java la référence directement (`ModElo`),
mais **plante le jeu** si elle est créée par son nom (`AIPanda` via `SoccerModule.AddFootBaller`).
`mods\build.ps1` régénère l'index à chaque build.

## Cycle de vie
- `SoccerGame extends Game extends GameObject` : classe principale (`game.ini` → `JavaGameClassName("SoccerGame")`).
  - `OnPreInitialize` : charge `data\texts.scr`, `LoadDebugConfig()` (← `debugconf.scr`), paramètres de ligne de commande.
  - `OnInitialize` : sons, réglages, crée `Cheat`, `MovieModule`, `ObjectManager`.
  - `LoadMenuLevel` → `SoccerMainMenuModule` (menus `Menu*`, UI index `m_nCurUI`).
  - `LoadLevel(n)` → `SoccerModule` (un match), `activeLevelModule`. `SoccerModule.m_cMatch` = `AIMatch`/`Match`.
- `CoreLogic` (`GameSerializeSettings.Logic`) : état global (joueur courant, équipes gauche/droite, niveau suivant).
- `PlayerSave` : sauvegarde du joueur (équipes / extras débloqués, triche).

## debugconf.scr (dossier du jeu, absent par défaut)
Mots-clés lus : `MenuOn(i)`, `FirstLevel(i)`, `RenderPublic(i)`, `RenderTreesAndGrass(i)`, `FBLighting(i)`,
`MenuEditor(i)`, `ShowFPS(i)`, `Debug(i)`, `Difficulty(i)`, `Multiplayer()`, `Demo()`, `RecordingEnabled(i)`.
`Debug(1)` active : mode capture (PAUSE, F12 = figer le temps, caméra libre), `[` / `]` = niveau précédent/suivant.

## Format .scr
Texte, Windows-1252, CRLF. En-tête de déclarations `!Mot(types)` (`s` chaîne, `i` entier, `f` flottant),
puis appels `Mot(valeurs)` et blocs `{ }`. **Éditer en conservant l'encodage et les CRLF** (`sed -b`).

## Gameplay — fichiers clés
| Sujet | Classes |
|---|---|
| Règles / état du match, scores, mi-temps, but en or | `Match`, `AIMatch`, `MatchData`, `Clock` |
| Joueur (physique, déplacements, tirs) | `FootBaller` (35 Ko), `AIFootBaller` (75 Ko) |
| Ballon | `Ball` (43 Ko) |
| IA | `Strategy` (88 Ko), `AITeam`, `AISystem`, `DecisionProcess`, `Goal*`/`Action*`/`Event*`, `data\ai_params.scr` |
| Supers (tir, saut, vitesse, mur) | `Team.GiveSuper`, `HudSuper`, `SuperWall`, `FXSuperSpeed` |
| Triches de dev | `Cheat` (méthodes), `SoccerGame.OnInputKey` / `CheckCheatString` |

## Équipes / animaux
- Une équipe = un animal, index 0..11 : Bear, Bull, Chicken, Dino, Frog, Giraffe, Gorilla, Lion, Panther, Shark, Turtle, Robo.
- `EAnimals` : constantes, mesh (`mis.3da`, `byk.3da`…), textures de maillots (`data/textures/tshirts/*`),
  scripts d'animation (`Data/Animals_scr/<animal>.scr` + `_GK`), arbitre, sons de commentaires.
  **Un 13ᵉ emplacement existe déjà** : `_ANIMAL_ = 12`, mesh `""` (prévu mais vide), `_ANIMALS_COUNT = 12`.
  → utilisé par le mod Panda / Tigre (index 12 et 13), voir `new-teams.md`.
- `ETeams` : `TeamData` par animal (noms via `texts.scr` `&TeamXxx&`, logo, joueurs) + compétences
  (`m_nSkillsStrength/Accuracy/Turning`, **affichage seul dans le jeu d'origine**, voir `team-stats.md`),
  terrain à domicile (`GetHomeField`).
- `ETeamLogos` : `data/menu/logos/T_Logo_*.png`.
- `AI<Animal>` / `AI<Animal>Referee` : sous-classes par animal.
- Nombre d'équipes codé en dur (`% 12`, `< 12`, tableaux de 12) dans : `Championships`, `ETeams`, `Match`,
  `MatchData`, `MenuChampionships*`, `MenuChooseTeam`, `MenuMain`, `MenuMultiplayer*`, `PlayerSave`,
  `SoccerGame`, `SoccerMainMenuModule`, `SoccerMenuShowObjectModule`. → à traiter pour ajouter une équipe.
- Niveaux : `SoccerModule.GetLevelFileName(n)`, dossiers `Data/Levels/<nom polonais>` (0..11 stades d'équipes,
  15..20 terrains bonus `E1..E6`).
