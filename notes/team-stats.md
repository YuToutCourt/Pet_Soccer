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
