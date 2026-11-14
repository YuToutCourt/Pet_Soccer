# Mode Ligue (mod)

Un mode à part, bouton **ligue** au menu principal (sous « championnat ») : une ligue à 14 en aller simple,
puis des playoffs à double élimination pour les premiers. Le **championnat d'origine ne change pas**
(7 coupes, poules de 3, déblocages) : la ligue n'en dépend pas et ne débloque rien.

## Déroulement
1. Menu principal → **ligue**. Si une ligue est en cours : « Continuer la ligue en cours ? » (non = elle est abandonnée).
2. Choix de l'équipe et de sa tenue (écrans d'origine) → écran **classement** (`MenuLeague`, `_UI_LEAGUE = 23`).
3. **Ligue** : les 14 équipes, chacune rencontre les 13 autres une fois (13 journées, calendrier tiré au sort,
   domicile / extérieur alternés). Victoire 3 pts, nul 1. Départage : points, différence de buts, buts marqués,
   victoires, tirage au sort.
   - « jouer » → avant-match → match ; au retour, les 6 autres matchs de la journée sont simulés.
   - Le classement colore en vert le numéro des places qualificatives ; ton équipe est en jaune.
4. **Playoffs** (8 premiers par défaut), écran **tableau** (`MenuLeagueBracket`, `_UI_LEAGUE_BRACKET = 24`) :
   - tableau des gagnants : 1-8, 4-5, 2-7, 3-6, puis demies et finale des gagnants ;
   - une défaite y envoie dans le tableau des perdants, une défaite dans celui-ci élimine ;
   - grande finale : vainqueur des gagnants contre vainqueur des perdants (revanche possible, `GrandFinalReset`).
   - Le mieux classé en ligue reçoit ; en grande finale, le vainqueur du tableau des gagnants.
   - Égalité à la fin du temps : but en or (`Match.Update`). Un nul (match interrompu) est rejoué.
   - Les matchs sans le joueur sont simulés jusqu'à son prochain match. Éliminé ou non qualifié :
     tout le reste est simulé, le tableau montre le vainqueur.
5. Fin : « menu » retire la ligue de `league.dat`. « retour » en cours de route garde la ligue (reprise par le bouton).

- Le joueur joue toujours à gauche (comme en survie) ; le match a lieu au stade de l'équipe qui reçoit.
- Difficulté : réglage Options > Match. Elo : comme un match normal. Historique : mode 4 « Ligue » (`StatsMode4`).
- « Recommencer le match » est grisé (le résultat est enregistré dès la fin du match). Quitter un match en cours
  ne compte pas : le match reste à jouer.

## Matchs simulés (IA contre IA)
- Note d'équipe = moyenne pondérée Force / Précision / Réflexe (`team_stats.scr`), ou `Rating(...)`, plus une forme
  tirée au hasard pour la ligue (± `Form`).
- Buts : loi de Poisson, moyenne = `GoalsPerMinute` × durée du match × e^(écart de note × `StrengthEffect`),
  + `HomeBonus` pour l'équipe qui reçoit. Calé sur l'historique du profil (≈ 3,5 buts par match en 2 × 4 min) :
  ≈ 3,4 buts et 21 % de nuls par match simulé.
- Nul en élimination : but en or tiré au sort selon les forces.

## league.scr
| Clé | Défaut | Rôle |
|---|---|---|
| `Qualifiers` | 8 | qualifiés : 2, 4 ou 8 (puissance de 2 inférieure) ; l'écran du tableau est prévu pour 8 au plus |
| `GrandFinalReset` | 0 | 1 : revanche si le vainqueur des perdants gagne la grande finale |
| `Points` | 3, 1 | points victoire, nul |
| `GoalsPerMinute` | 0.2 | buts par équipe et par minute de jeu (équipes égales) |
| `HomeBonus` | 0.1 | +10 % de buts pour l'équipe qui reçoit |
| `StrengthEffect` | 0.08 | +8 % de buts par point de note d'écart |
| `Form` | 2.0 | forme de la ligue, ± points de note au hasard par équipe |
| `RatingWeights` | 1, 1, 1 | poids de Force, Précision, Réflexe dans la note |
| `Rating("Animal", f)` | — | note imposée |

Qualifiés, revanche et points sont figés au départ d'une ligue ; les réglages de simulation s'appliquent tout de suite.

## Sauvegarde
`game_dev\league.dat` : une ligne par profil, `profil;tenue;v0;v1;...` (entiers, `ModLeague.Save`, version 1 en tête :
réglages figés, équipes, notes, calendrier et résultats, têtes de série, tableau). Supprimer le fichier = plus de ligue en cours.
`settings.dat` n'est pas touché.

## Code
| Classe | Rôle |
|---|---|
| `ModLeague` | logique pure, sans le moteur : calendrier (méthode du cercle), classement, tableau à double élimination (générique 2 / 4 / 8), simulation, sauvegarde en entiers. Testée hors du jeu (6 000 ligues : 14 / 13 / 6 / 3 équipes, 2 / 4 / 8 qualifiés, avec ou sans revanche) |
| `ModLeagueMode` | réglages, `league.dat`, `Start` / `Stop`, `PrepareMatch` (équipes, stade), `OnMatchEnd` (`AIMatch.AfterMatch`), `IsGoldenGoal` |
| `MenuLeague`, `MenuLeagueBracket` | écrans, générés par `mods/ui/make_league_scr.py` (`League*.scr`, `LeagueBracket*.scr`) |
| Branchements | `CoreLogic.m_bLeague`, `MenuMain` (bouton, reprise), `MenuChooseTeamColors`, `MenuPreMatch` (retour), `SoccerMainMenuModule` (écrans 23-24, retour de match), `Match` (but en or), `AIMatch`, `MenuInGame` (recommencer grisé), `ModStats` (mode 4) |

Bouton du menu : `mods/ui/add_league_button.py` (8 boutons au pas de 0,115, hauteur 0,075). Textes `League*`, `MenuMainLeague`, `StatsMode4`.

## Test sans souris
Dans `game_dev\debugconf.scr` : déclarer `!LeagueTest(i)` puis `LeagueTest(n)`. Au menu principal, une ligue démarre
avec Real Pandrid, n matchs du joueur sont joués au hasard, puis l'écran de la ligue s'ouvre
(13 = fin de la ligue, 14 et plus = playoffs). Remplace la ligue en cours. Le build remet le `debugconf.scr` du mod.
Log : `[mod] ligue : 2-1 -> -1, phase 0, journee 4, jour 0`.

## Historique
Une première version remplaçait le format des coupes (poules → ligue + playoffs). À la demande de l'utilisateur
(2026-11-10), le championnat d'origine a été remis tel quel et la ligue est devenue un mode séparé.
