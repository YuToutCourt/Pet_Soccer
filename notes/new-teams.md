# Nouvelles équipes : Panda et Tigre

Deux équipes ajoutées aux 12 d'origine, sur des modèles 3D existants retexturés (pas de nouveau `.3DA`).

| Index | Équipe | Modèle | Textures | Stade à domicile | Voix du commentateur |
|---|---|---|---|---|---|
| 12 | Real Pandrid (`Panda`) | `mis.3da` (ours) | `panda_*`, `pandy*`, `T_Logo_Pandrid` | 6, gorilles (jungle) | ours |
| 13 | Borussia Tigermund (`Tiger`) | `pantera.3da` (panthère) | `tygrys_*`, `tygrysy*`, `T_Logo_Tigermund` | 8, panthères | panthère |

- **Disponibles d'office** en amical, en extra et en multi.
- **Championnats** : le format reste à 12 places (4 poules de 3, puis quarts, demies, finale). `Championships.DrawRoster`
  tire les 12 participants parmi les 14 équipes : l'équipe du joueur et l'adversaire prévu en finale sont toujours là,
  les 10 autres places sont tirées au sort. Panda et Tigre peuvent donc être choisis par le joueur ou apparaître comme adversaires.

## Code (`mods\java\`, marqué `// [mod]`)

| Classe | Changement |
|---|---|
| `EAnimals` | `_ANIMAL_PANDA = 12`, `_ANIMAL_TIGER = 13`, `_ANIMALS_COUNT = 14` ; tableaux de maillages, textures, noms ; `m_nCommentAnimal` + `GetCommentAnimal(n)` pour les tableaux de sons (12 entrées) |
| `ETeams` | `TeamData` des deux équipes, stats par défaut, `GetHomeField` 12 → 6, 13 → 8 |
| `ETeamLogos` | `_LOGO_PANDA`, `_LOGO_TIGER` |
| `AIPanda`, `AITiger` | copies d'`AIBear` / `AIPanther` (même maillage, donc mêmes dimensions) |
| `SoccerModule.AddFootBaller` | classes `AIPanda` / `AITiger` (l'emplacement 12 pointait vers une classe `AI_` inexistante) |
| `MenuChooseTeam` | 14 entrées ; le `switch` de `OnOK` (stade = index d'équipe) remplacé par `GetHomeField` |
| `Championships` | `Reset` : 12 participants tirés parmi 14 (`DrawRoster`) ; `TeamRecord` passée en classe imbriquée `static` (artefact `this$0` de la décompilation), constructeurs inchangés en bytecode |
| `SoccerMenuShowObjectModule` | tableau de modèles du menu à 14 |
| `PlayerSave` | index ≥ 12 toujours disponible ; `settings.dat` garde ses 12 booléens (format inchangé) |
| `AIMatch` | `CommentTeam` / `CommentMissedShot` passent par `GetCommentAnimal` (accès directs à des tableaux de 12 dans `Commentator`) |
| `Watcher` | taille du public par animal (`switch` 0..11) : 12 = ours, 13 = panthère. Sans ça, `NullPointerException` dans `SetSkin` au chargement du stade, donc crash dès le lancement d'un match |

`AIPanda` et `AITiger` doivent aussi figurer dans `code_inf.pak` (index des classes créées par leur nom, voir
`architecture.md`) : `build.ps1` le régénère. Sans ça, plantage natif juste après `Addspecies ... AIFootballer.scr`.

Données : `texts.scr` (`&TeamPandas*&`, `&TeamTigers*&`), `team_stats.scr` (`Panda` 100/85/85, `Tiger` 90/90/100).

## Textures (générées, ne pas retoucher à la main)

```
python mods\art\make_team_textures.py   # maillots, gardien, public, arbitre, icônes de maillot
python mods\art\make_team_logos.py      # logos (réutilise make_rank_logos.py)
```

Par équipe : `_01.._05` (maillots), `_B01.._B05` (gardien), `_P` (public), `_S` (arbitre), en 512×512,
plus 5 icônes `Menu\TShirtsIcons` (256×128) et un logo 128×128. Les `.png` suffisent : le moteur n'exige pas de `.tex`.

### Dépliage UV de `mis.3da` (relevé en jeu)
Mesuré avec des textures « sondes » affichées sur le modèle du menu de choix d'équipe (grille 16×16 étiquetée,
puis grille fine sur la tête, puis pastilles de couleur aux emplacements candidats) :
- globes des yeux = coin haut-droit (x ≥ 400, y < 85) : les deux bosses sur le haut de la tête ;
- tour des yeux = zone ombrée vers (300-385, 20-115) ;
- oreilles = pièce ronde trouée posée sur le maillot (325-390, 245-305) ; leur base = petite bosse vers (279, 242) ;
- tête et visage = haut-centre ; dessus du museau vers (195-265, 20-90) ; museau à points vers (120-230, 110-170) ;
- maillot = centre (130-510, 200-400) ;
- bras et mains = colonne de gauche (x < 120) ; jambes et pieds = bande du bas (y ≥ 400) ;
- nez = boule noire du coin bas-droit.

Panda : fourrure (bleu clair) → blanc, ombré par la luminance ; noir sur les membres, les oreilles (forme prise sur
`mis_01`, où le maillot les détoure) et une tache à bord adouci autour des yeux.
Tigre : fourrure jaune → orange ; taches de la panthère rebouchées (composantes sombres entourées de fourrure) ;
rayures procédurales ; maillot 01 bleu → jaune (et son icône).

### Refaire une sonde pour un autre modèle
Remplacer temporairement `game_dev\Data\Textures\Tshirts\<equipe>_01.png` par une grille étiquetée, passer le jeu en
fenêtré (`//Fullscreen()` dans `game_dev\Data\VideoSettings.scr`) et capturer l'écran de choix d'équipe.
Le jeu dessine son propre curseur, piloté par des mouvements **relatifs** de la souris (`SetCursorPos` ne le déplace pas).

## Limites
- Commentaires audio : les deux équipes reprennent les voix de l'ours et de la panthère (fichiers absents de l'installation de toute façon).
- L'arbitre reste tiré parmi les 12 animaux d'origine (`EAnimals.SelectReferee`). Le public, lui, prend l'espèce des deux équipes en jeu (`Watcher.GetRandomSpecies`, textures `_P`).
- Écran de démo (`MenuMain.StartDemo`) et touches de debug `[` / `]` : toujours limités aux 12 équipes d'origine.
- Un modèle 3D vraiment nouveau demanderait le reverse du `.3DA` (squelette Biped `Bip01 *`, animations précalculées découpées par `<modèle>.ini`).
