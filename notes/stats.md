# Statistiques de match et écran Profil (mod)

## En match (pause, mi-temps, fin de match)
Le cadre « résultats » de `MenuInGame` devient un tableau comparatif : hôte à gauche, invité à droite.
- période, score, nom court des équipes (`TeamData.m_sNameAbr`) ;
- buteurs, une ligne par joueur avec ses minutes (`Djalima 19' 40'`), « (csc) » pour un but contre son camp ;
  au plus 3 lignes, la 3e devient « ... » s'il y a plus de buteurs ;
- tirs, possession, passes réussies / tentées, ballons récupérés, arrêts du gardien, supers.

Les tirs, la possession et les supers sont les compteurs du jeu d'origine (`Team`).
Les autres sont comptés par `ModStats` :

| Statistique | Règle | Accroche |
|---|---|---|
| Passe tentée | `AIFootBaller.Pass` pendant le jeu (état 0) | `ModStats.OnPass` |
| Passe réussie | le ballon est ensuite pris par un coéquipier pendant le jeu | `AIMatch.ChangePossesor` → `OnPossession` |
| Ballon récupéré | l'équipe prend le ballon alors que l'adversaire l'avait (hors arrêt, hors remise en jeu) | `OnPossession` |
| Arrêt | après un tir adverse, le gardien touche le ballon (`Ball.OnHitFootballer` → `OnTouch`) ou le prend ; un but dans les 2 s annule l'arrêt | `OnShot` (là où le jeu compte les tirs), `OnTouch`, `OnPossession` |
| Buteur | dernier joueur à avoir frappé le ballon (`Ball.KickTo` / `KickToLob` → `OnKick`) ; s'il est de l'autre équipe, c'est un csc | `AIMatch.OnGoal` → `ModStats.OnGoal` |

- Les compteurs sont rangés par objet `Team` : les index d'équipe s'inversent à la mi-temps.
  Remise à zéro dans `AIMatch.ResetStatistics` (début ou reprise de match).
- Minute « football » : `temps réel × 45 / durée d'une mi-temps + 1`, plafonnée à 45 puis 90 (sauf but en or).
- Nom du buteur : liste `Team<Animal>Players` de `texts.scr`.
  Le n°1 de la liste est le gardien (Blufon = Buffon, Beartez = Barthez), alors que le gardien est le **dernier**
  joueur de l'équipe (`m_nNumberInTeam` 6). `ModStats.GetListIndex` : gardien → 1er nom, joueurs de champ → noms 2 à 7.
- Log : `[mod] stats : but de Holandia (VarDenVan, n1) a la 11e`.

## Historique
- Enregistré pour les mêmes matchs que l'Elo : humain contre IA, hors démo et multijoueur local.
  Fin normale : `AIMatch.AfterMatch` (après `ModElo.OnMatchEnd`). Abandon (quitter ou recommencer) :
  `MenuInGame` → `ModStats.OnAbandon`, avec le score du moment ; un abandon compte comme une défaite.
- `game_dev\stats.dat` : texte, une ligne par match, `profil;date;mode;équipe;adversaire;bp;bc;niveau;classé;ΔElo;abandon;tirs;possession;passes ok;passes;récupérations;arrêts;supers;buts du meilleur buteur;triche`
  (triche = une triche de match du menu Mods était active, voir `mods-menu.md`).
  Les champs ajoutés plus tard peuvent manquer dans les anciennes lignes : ils valent 0 à la lecture.
  Supprimer le fichier = historique vide. 2000 matchs gardés par profil.
- Mode : 0 amical, 1 coupe (championnat), 2 extra (terrains 15-20), 3 survie. Textes `StatsMode<n>`.
- 24e valeur `_SURVIVAL_WAVE` : numéro de la manche de survie (0 hors survie), voir `survival.md`.
- La date vient de la JVM 1.4 du jeu, qui ne connaît pas toujours le fuseau Windows (heure UTC possible) : seuls le jour et le mois sont affichés.

## Écran Profil
Bouton « profil » du menu principal (`ID_PROFILE` dans `Main*.scr`) → `MenuProfile` (`_UI_PROFILE = 19`).
- Bilan (calculé à partir de tout l'historique) : matchs, V/N/D, buts pour / contre, matchs sans encaisser, triplés,
  plus large victoire, meilleure série et série en cours de victoires, tirs par match, possession moyenne,
  % de passes réussies, récupérations et arrêts par match, record en survie.
- Derniers matchs : les 12 plus récents (date, mode, score en vert / gris / rouge, variation d'Elo).
- `Profile*.scr` générés par `mods/ui/make_profile_scr.py` (gabarit Options > Match, comme l'écran Tactique).
  `InGame*.scr` : cadre « résultats » réécrit par `mods/ui/make_stats_scr.py` (relançable).

## Pièges
- **Polices** : `SoccerFonts` ne charge que 3 polices par résolution (FLRoman21/44 + FLJazz32 en 800×600, etc.).
  Une police non chargée n'affiche rien (log « No font <MS Sans Serif> »).
  Le mod ajoute `FLRomanSmall` (16/16/21 px selon la résolution) pour les listes. En 640×480 elle faisait 14 px :
  le moteur y perdait une partie des lettres (vu sur l'écran « en ligne »), d'où 16 px, la taille de `FLRoman16`.
- **Textes multilignes** : avec `TextPosFlags` 256, l'alignement horizontal est ignoré (tout est à gauche, mal placé).
  On fait une static par ligne (18 gauche, 34 centre, 66 droite, toutes centrées verticalement).
- **Bouton « enfoncé »** : au retour d'un match, le bouton « profil » du menu principal pouvait rester en état
  `UIButton$LMBDown` et le premier relâchement de la souris rouvrait le Profil. `MenuMain` ignore un appui sur
  `ID_PROFILE` avant l'affichage du menu ou dans les 0,3 s qui suivent.
