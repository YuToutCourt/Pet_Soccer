# Stats d'équipes

## Dans le jeu d'origine : purement décoratives
`ETeams` contient trois tableaux de 12 valeurs (`m_nSkillsStrength`, `m_nSkillsAccuracy`, `m_nSkillsTurning`),
copiés dans `TeamData` puis **lus uniquement par `MenuChooseTeam`** pour dessiner les barres
(`5 - (100 - v) / 5` points). Aucune classe de gameplay ne les utilise.

Tous les animaux partagent les mêmes scripts d'animation et de vitesse (`Data\Animals_scr\AIFootballer.scr`
et `AIGoalkeeper.scr`, via `EAnimals.GetAnimalScrFile`). Les fichiers `lion.scr`, `bull.scr`… sont des restes inutilisés.
→ En match, les 12 équipes sont mécaniquement identiques.

Libellés du menu (`texts.scr`) : Force / Précision / **Réflexe** (`m_nSkillsTurning`).

## Mod : stats branchées sur le gameplay
Les valeurs sont lues dans `data\team_stats.scr` (`mods\data\Data\team_stats.scr`) au premier accès
(`ETeams.LoadStats`). Si le fichier est absent, les valeurs d'origine sont gardées.
Le menu affiche automatiquement les nouvelles valeurs.

| Stat | Effet | Point d'accroche |
|---|---|---|
| Force | puissance des tirs : au sol `fMaxKickSpeed` × mult. ; en l'air temps de vol ÷ mult. (trajectoire plus tendue, même point d'arrivée) | `Ball.KickTo` et `Ball.KickToLob` (branche `bShoot`) |
| Précision | écart gauche-droite des tirs (`fDevX` × mult.) ; la hauteur, qui décide tir en l'air / au sol, reste d'origine | `Strategy.GetShotDescription` |
| Réflexe | vitesse de déplacement, gardien compris (hors arbitre) | `AIFootBaller.SetLocalSpeed` → `SetLocalSpeedStat` |

Multiplicateurs (90 = neutre, plancher 0,1), coefficients réglables par `Effects(a, b, c)` en % par point :
- puissance = 1 + (Force − 90) × a/100 (défaut a = 1)
- dispersion = 1 + (90 − Précision) × b/100 (défaut b = 3)
- vitesse = 1 + (Réflexe − 90) × c/100 (défaut c = 0,5)

Limites connues :
- la vitesse ne change pas la cadence de l'animation (léger glissement des pieds aux valeurs extrêmes) ;
- les virages du joueur contrôlé sont instantanés (`FootBaller.ChangeDirectionLogic` → `SetDir`), d'où le choix
  de relier « Réflexe » à la vitesse plutôt qu'à la rotation.

## Correction annexe
`AIFootBaller.Kick` décompilée ne recompilait pas (`this.this$0 = this$0`, artefact Vineflower) :
transformée en classe imbriquée `static`, même constructeur `(AIFootBaller)`.

## Équilibrage (2026-11-25)
La hitbox n'est pas une stat mais compte beaucoup : chaque `AI<Animal>.UpdateExtents` fixe en dur la taille du modèle,
et le rayon (largeur / 2) s'ajoute à la portée de réception du ballon (`GetReceiveDistance` = `ReceiveRadius` + rayon ballon + rayon, avec `ReceiveRadius(30)` dans `ai_params.scr` : le 100 du code est remplacé),
de tacle (`Strategy.GetActiveDistance` = 110 + rayon), d'accroche (`fCanAttachBallDistance`) et au corps de collision.
La hauteur limite les ballons aériens captés (`vPos.fY + fHeight`).

| Animal | Largeur | Hauteur | Animal | Largeur | Hauteur |
|---|---|---|---|---|---|
| Gorille | 110 | 72,5 | Lion | 62 | 88 |
| Taureau | 101 | 85 | Poule | 60 | 76 |
| Dino | 100 | 105 | Panthère / Tigre | 60 | 74 |
| Girafe | 90 | 147 | Ours / Panda | 59,5 | 100 |
| Grenouille | 55 | 83,6 | Tortue | 51 | 77,4 |
| Requin | 48 | 84 | Robot | 45 | 57 |

Avant l'équilibrage, le Gorille était à 90/90/90 avec la plus grosse hitbox, et le Tigre avait la vitesse max sans contrepartie.

Règle : `(Force−90) + (Précision−90) + 2×(Réflexe−90) + bonus hitbox = 10` pour toutes les équipes.
- La vitesse compte double : elle sert à tous les joueurs pendant tout le match, alors que Force et Précision ne servent qu'aux tirs.
- Bonus hitbox = 0,8 × (rayon − 30), +12 pour la taille de la Girafe (147 de haut, contre 105 pour le Dino) : elle capte
  les ballons aériens plus haut que tout le monde (`AIFootBaller.java:1866`) et, en gardienne, ne saute qu'au-dessus
  de 167 (`Strategy.java:781`). Passé de +3 à +12 le 2026-11-25 : Girafe 95/100/80 → 95/91/80.
- Effet de la Précision passé de 3 à 2 % par point pour permettre des valeurs au-dessus de 100 (Tortue 114 → dispersion × 0,52 ; avec 3 %, elle tombait au plancher de 0,1).
- Les grosses hitbox paient en vitesse et en précision ; les petites ont droit à des valeurs au-delà de 100.

À régler en jeu : le poids de la hitbox (0,8) et celui de la vitesse (×2) sont des estimations, pas des mesures.
Les matchs simulés de la Ligue (`league.scr`, `RatingWeights`) moyennent les trois stats sans la hitbox :
les grosses équipes y sont un peu sous-notées.
