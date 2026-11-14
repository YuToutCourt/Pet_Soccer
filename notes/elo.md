# Classement Elo (mod)

## Principe
- Un Elo par profil (nom `PlayerSave.m_Name`), départ 1000.
- Comptent : les matchs **humain contre IA** (match rapide, championnat, extras).
  Ne comptent pas : la démo, le multijoueur local, les matchs simulés du championnat.
- L'IA a un Elo fixe selon le niveau de difficulté effectif (`CoreLogic.GetDiffLevel`) : 600 (niveau 1) → 2000 (Légende).
  Battre une IA bien plus faible ne rapporte presque rien : pour monter, il faut jouer plus dur.

## Réglage « Classé » (Options > Match > Niveau de difficulté)
- La difficulté suit l'Elo : l'IA prend le niveau (1..10) dont l'Elo (`AIElo` dans `elo.scr`) est le plus proche
  de celui du profil, plus `RankedOffset` (0 par défaut ; 100 = IA un peu plus forte que le joueur).
  Ex. 1007 → niveau 4 (1050), 1480 → niveau 7 (1500). Les matchs sont donc toujours à peu près équilibrés (±75 Elo).
- Le niveau est recalculé à chaque match (`CoreLogic.GetDiffLevel`, cas `m_nDiffSetting == 6` → `ModElo.GetRankedLevel`),
  aussi en championnat. L'Elo ne change pas pendant un match : le niveau utilisé pour le calcul est le même qu'au coup d'envoi.
- Pas de changement de sauvegarde : `m_nDiffSetting` vaut 6 (entrée `MenuOptionsMatchDiff6` dans `texts.scr`).

## Calcul (`ModElo.Rate`)
- Attendu : `E = 1 / (1 + 10^((EloIA - EloJoueur) / 400))`.
- Résultat `S` : 1 victoire, 0.5 nul, 0 défaite.
- Écart de buts `G` : ×1 (0-1 but), ×1.5 (2 buts), ×(11 + écart)/8 au-delà (formule du World Football Elo).
- `Δ = arrondi(K × G × (S - E))`, K = 32, ou 48 pendant les 10 premiers matchs (placement).
- Abandon (quitter ou recommencer un match en cours) = défaite d'un but, si `AbandonIsLoss(1)`.

## Rangs
| Rang | Elo min |
|---|---|
| Bois | 0 |
| Bronze | 900 |
| Argent | 1100 |
| Or | 1300 |
| Platine | 1500 |
| Diamant | 1700 |
| Challenger | 1900 |

Seuils et couleurs dans `data\elo.scr` (`Rank(min, r, v, b)`), noms dans `texts.scr` (`EloRank0..6`, même ordre),
badges dans `elo.scr` (`RankBadge(rang, fichier)`).

## Badges
- `mods\data\Data\Menu\logos\T_Rank_<Rang>.png` : 256×256 RGBA (sortie en 256 pour rester nets agrandis en fin de match), même style que les logos d'équipe
  (anneau métal, disque coloré, nom en arc, écusson + ballon, étoiles = numéro du rang ; lauriers dès Or,
  diamant pour Diamant, couronne pour Challenger).
- Générés par `mods/art/make_rank_logos.py` (Python + Pillow + numpy), aperçu dans `mods/art/rank_logos_preview.png`.
  Pour retoucher : modifier la table `RANKS` du script, relancer, puis `mods/build.ps1`.
- On peut remplacer un PNG par un dessin fait main (même nom, 256×256, fond transparent).

## Fichiers
- `mods\data\Data\elo.scr` : réglages (départ, K, placement, abandon, Elo de l'IA, rangs).
- `game_dev\elo.dat` : sauvegarde texte, une ligne par profil `nom;elo;matchs;victoires;nuls;defaites;record`.
  Séparée de `settings.dat` pour ne jamais casser la sauvegarde du jeu. Supprimer le fichier = remise à zéro.

## Code
- `ModElo` : chargement, calcul, sauvegarde, textes.
- `AIMatch.AfterMatch` → `ModElo.OnMatchEnd` (fin normale et but en or).
  Piège : `m_nScoreLeft` = hôte, mais les index d'équipe suivent les côtés du terrain. Après la mi-temps (hors but en or),
  l'hôte est l'équipe 1 (même règle que `MenuInGame.ShowInfo`). Le joueur = l'équipe `m_bUserControled`
  (en championnat il peut être l'invité).
- Test rapide : code `LN<hôte><invité>` en match (ex. `LN20`) termine le match sur ce score.
- `MenuInGame.Quit` / `OnYesFromDlg` (recommencer) → `ModElo.OnAbandon` si le match n'est pas fini.
- Affichage :
  - menu principal : `ID_ELO` ajouté dans `Main*.scr` (sous « Joueur : … », polices FLRoman16/21/28), rempli par `MenuMain.ShowElo`,
    et le badge `ID_ELO_BADGE` (`UIWindow` avec `Texture`) en dessous, à droite ;
  - fin de match : panneau à droite sous les boutons, menu remonté à y = 0,14 et badge à 0,135 × 0,18 sous « ok », avec un écart (`ID_ELO_BADGE`, `ID_ELO_RANK`, `ID_ELO_VALUE`, `ID_ELO_DELTA` ajoutés dans `InGame*.scr`).
    `MenuInGame.StartElo` affiche l'ancien rang et l'ancien Elo, puis `EloTick` (toutes les 0,02 s, via `CallMethodEvery`)
    fait défiler le nombre jusqu'à la nouvelle valeur en 1,5 s ; le rang et sa couleur suivent, et « Promu : » / « Rétrogradé : »
    s'affiche à la fin si le rang a changé. L'écart est en vert (gain) ou rouge (perte).
  - badges : `ModElo.LoadBadges` charge les 7 textures dans la fenêtre (`AddTexture`, comme `MenuChooseTeam` pour les logos),
    `ModElo.SetBadge` change la texture ; pour masquer, alpha de texture à 0 (`SetVisible(false)` serait annulé par le `Show()` du menu).
- Log : `[mod] Elo <profil> : match 3-2 contre IA niveau 10 (2000) -> 1046 (+46)`.

## Plus tard
- Elo entre humains (multi local / en ligne).
