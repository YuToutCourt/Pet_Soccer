# Survie (mod)

Des matchs enchaînés contre l'IA tant qu'on ne perd pas. Chaque manche réussie fait monter l'IA d'un niveau
(1 → 10), puis, au-delà de Légende, l'IA gagne en vitesse et en puissance de tir à chaque manche.

## Déroulement
1. Menu principal → bouton **survie** (`ID_SURVIVAL`, 3e bouton) → choix de l'équipe et de sa tenue (écrans d'origine).
2. Écran **Survie** (`MenuSurvival`, `_UI_SURVIVAL = 22`) :
   - cadre « série » : manche, manches réussies, record, niveau de l'IA, bonus de vitesse et de frappe de l'IA ;
   - cadre « prochain match » : les deux logos et noms, puis le résultat de la manche précédente
     (« Victoire 3-0 contre As Robots », « Au but en or ! ») et l'annonce d'une montée de niveau ;
   - « jouer » → avant-match (Tactique possible) → match ; « retour » revient au menu
     (« abandonner », avec confirmation, dès qu'une manche est réussie).
3. Fin de match : le panneau Elo de droite affiche « Manche 3 réussie ! », « Manche 4 » et le niveau de la prochaine IA,
   ou « Série terminée », le nombre de manches et le record. « Recommencer le match » est grisé en survie.
4. Retour automatique à l'écran Survie (`SoccerMainMenuModule.ShowLoadingScreen`). Après une défaite, il devient
   « fin de la série » : « rejouer » relance une série avec la même équipe.

- Adversaires : les 13 autres équipes, mélangées, chacune une fois avant de recroiser la première ; on joue au stade de l'adversaire.
- Égalité à la fin du temps : but en or (`Match.Update`, comme les phases finales de coupe). Les supers restent permis.
- Défaite, abandon d'un match (quitter ou recommencer) : la série s'arrête.
- La série ne vit qu'en mémoire : quitter le jeu entre deux manches l'arrête (le record est déjà enregistré).

## Difficulté
| Manche | IA |
|---|---|
| 1..10 | niveau = manche (`StartLevel` + (manche − 1) / `WavesPerLevel`, plafonné à `MaxLevel`) |
| 11 et + | Légende, + `SpeedPerWave` de vitesse et + `ShotPowerPerWave` de puissance par manche, plafonnés |

- `CoreLogic.GetDiffLevel` renvoie le niveau de la manche (quel que soit le réglage Options > Match).
- Les multiplicateurs `AISpeedMul` / `AIShotPowerMul` (`ModDifficulty`) sont multipliés par ceux de la survie,
  **seulement pendant un match de survie** (`ModSurvival.IsActive`). Hors survie, rien ne change.

## survival.scr
| Clé | Défaut | Rôle |
|---|---|---|
| `StartLevel` | 1 | niveau de la 1re manche |
| `MaxLevel` | 10 | niveau max |
| `WavesPerLevel` | 1 | manches à réussir pour monter d'un niveau |
| `SpeedPerWave` / `ShotPowerPerWave` | 0.03 / 0.04 | bonus par manche au-delà du niveau max |
| `MaxSpeedMul` / `MaxShotPowerMul` | 1.5 / 1.6 | plafonds |
| `GoldenGoal` | 1 | égalité → but en or (0 : l'égalité fait passer la manche) |
| `Rated` | 0 | 1 : les manches comptent pour l'Elo tant que l'IA n'a pas de bonus |

## Historique, record, succès
- Les matchs de survie sont enregistrés comme les autres (`stats.dat`), mode 3 « Survie », avec le numéro de manche
  (24e valeur `_SURVIVAL_WAVE`, voir `stats.md`). Non classés par défaut : la colonne Elo du Profil affiche « m.3 ».
- **Record** = plus haute manche réussie de l'historique, hors matchs avec triche du menu Mods (`ModSurvival.GetRecord`).
  Affiché sur l'écran Survie et dans le bilan du Profil (« Record en survie »).
- Succès : `survivor` (5 manches), `unbreakable` (10), `immortal` (15), condition `SurvivalWaves`.

## Fichiers
`ModSurvival`, `MenuSurvival`, `CoreLogic` (`m_bSurvival`, `GetDiffLevel`), `MenuMain`, `SoccerMainMenuModule`,
`MenuChooseTeamColors`, `MenuPreMatch`, `Match` (but en or), `AIMatch.AfterMatch`, `MenuInGame`, `ModDifficulty`,
`ModElo`, `ModStats`, `ModAchievements`.
Données : `survival.scr`, `achievements.scr`, `texts.scr` (`Survival*`, `MenuMainSurvival`, `StatsMode3`, `StatsWave`, `Ach_survivor`…).
Écrans : `Survival*.scr` (`mods/ui/make_survival_scr.py`), bouton du menu dans `Main*.scr` (`mods/ui/add_survival_button.py`,
qui resserre les 7 boutons au pas de 0,125), ligne du bilan dans `Profile*.scr` (`mods/ui/make_profile_scr.py`, 14 lignes).
Log : `[mod] survie : manche 1 reussie 3-0, manche 2 niveau 2`.

## Pièges
- `Match.java` est passé dans `mods/java` pour la première fois : le code décompilé redéclarait `nType` / `nTypeLoss`
  dans `MakeWeatherEvents` (cas 19), renommés en `nType19` / `nTypeLoss19` (variables locales du cas, sans effet ailleurs).
- À surveiller : lors du test, un 2e match lancé juste après un match terminé par le code `LN30` est resté bloqué au coup
  d'envoi (00:00, personne ne bouge). Le joueur manipulait le jeu en même temps : cause non établie
  (code `LN` qui coupe le match en plein jeu, ou autre). À revérifier en jouant normalement.
