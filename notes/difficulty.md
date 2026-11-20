# Difficulté (jeu d'origine + mod niveaux 8-10)

## Jeu d'origine
- Niveau effectif 1..7 = `CoreLogic.GetDiffLevel()`, croisement de :
  - `PlayerSave.m_nDiffSetting` (Options > Match : 0 Facile, 1 Normal, 2 Difficile) ;
  - `PlayerSave.m_nDiffLevel` (1..7, +1 par coupe gagnée, `PlayerSave.NextDiffLevel`).
  - Facile → 1..4, Normal → 2..5, Difficile → 4..7. Le niveau 7 = Difficile après 6 coupes.
- `AIMatch.Restart` → `Strategy.FixDifficultyLevel(n)` copie les 11 valeurs `*_level_n` d'`ai_params.scr` dans les champs actifs (démo : niveau 4 forcé).
- Particularité : l'IA tire toujours avec une dispersion de 1.0 (`Strategy.GetShotDescription`), `MaxShotDeviation` ne concerne que l'humain.

| Paramètre | Effet |
|---|---|
| `AttackInteria` | délai avant que l'IA presse le porteur |
| `FreeBallInteria` | réaction sur ballon libre |
| `BallTakenInteria` | réaction après une perte de balle |
| `FeintChance` | réussite des feintes **du joueur** |
| `FeintChance_AI` | réussite des feintes **de l'IA** contre le joueur |
| `SuperUpgradeTime` | recharge de la jauge SUPER (6 s partout ; remplacé ensuite par `FillTime` de `supers.scr`, 10 s, voir tactics.md) |
| `PassChoiceCoef` | qualité du choix des passes |
| `ValidPosDeviation` | imprécision du placement IA |
| `ShotOpportunity` | seuil de décision de tir IA (plus bas = tire plus) |
| `MaxShotDeviation` | dispersion des tirs **du joueur** |
| `GKNoticeTime` | retard de réaction du gardien **du joueur** |

## Mod
- Options > Match > Niveau : 3 entrées ajoutées, niveaux **fixes** (indépendants des coupes) :
  Expert = 8, Maître = 9, Légende = 10 (`CoreLogic.GetDiffLevel`, `MenuOptionsMatch.FillDiffCombo`, textes `MenuOptionsMatchDiff3..5`).
  Classé = niveau choisi selon l'Elo du profil (voir `notes/elo.md`).
  Pas de changement du format de sauvegarde (`m_nDiffSetting` vaut 3..5).
- `data\difficulty.scr` (classe `ModDifficulty`) : `Parametre_level(niveau, valeur)`.
  - Niveaux 8..10 : base = niveau 7, puis les lignes du fichier.
  - Une ligne pour un niveau 1..7 remplace la valeur d'`ai_params.scr`.
- 3 paramètres ajoutés, réservés aux équipes IA (`!IsUserControled()`), 1.0 par défaut :
  - `AIShotDeviation` : écart gauche-droite des tirs IA (`Strategy.GetShotDescription`), la hauteur reste d'origine ;
  - `AISpeedMul` : vitesse (`AIFootBaller.SetLocalSpeedStat`, cumulé avec la stat Réflexe) ;
  - `AIShotPowerMul` : puissance des tirs (`Ball.KickTo` / `KickToLob`, cumulé avec la stat Force).
  - `AISpeedMul` et `AIShotPowerMul` sont désactivés (1.0 partout) : la difficulté doit venir du comportement de l'IA, pas de stats gonflées. Le code reste en place.
- `Marking_level` / `MarkDistance_level` : marquage des joueurs libres par l'IA (0 aux niveaux 1-4, puis 0,3 → 1,0 ;
  distance 220 → 110), voir `notes/tactics.md`.
- Log : `[mod] difficulte niveau N` à chaque début de match.
- Codes : `LB8`, `LB9`, `LB0` (= 10). En match : niveau appliqué immédiatement. Au menu : règle Expert / Maître / Légende.
