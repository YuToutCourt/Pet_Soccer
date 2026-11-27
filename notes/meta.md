# Matchs de méta (IA contre IA → base du site)

Demande du 2026-11-26 : connaître la méta du jeu en faisant jouer toutes les équipes entre elles, au moins 10 fois
par paire, comme un match en ligne, au niveau d'un bon joueur (IA 7-8), sans The World, et voir le résultat sur le
site de stats.

## Lancer

```
mods\build.ps1 -Out build\meta\base                # le jeu modé, compilé à part (game_dev n'est pas touché)
python mods\meta\meta_run.py --reset               # vide la base pet-soccer locale, puis joue les 910 matchs
python mods\meta\meta_run.py                       # reprend les matchs qui manquent (build\meta\progress.json)
```

Options : `--workers 6` (jeux en même temps), `--per-pair 10`, `--levels 7,8` (alternés), `--half 240`
(secondes de jeu, = `RankedHalf(4)`), `--speed 3`, `--limit n` (essai), `--db <url>` (sinon `pet-soccer` du
Postgres local, port 54320). Ctrl+C ferme les jeux ; relancer sans `--reset` reprend.

## Ce que fait le jeu (`ModMeta`)

- `debugconf.scr` : `MetaMatch(A, tenue, B, tenue, niveau)`, `MetaHalf(s)`, `MetaSpeed(f)`. Au premier passage au
  menu, match démo (`CoreLogic.m_bDemo`) : les deux équipes à l'IA, A à gauche au coup d'envoi, stade de A.
- Niveau : `AIMatch` (démo fixée à 4 dans le jeu d'origine) et `CoreLogic.GetDiffLevel` rendent le niveau demandé.
  Même `Strategy` pour les deux équipes, donc même niveau. Tactique : celle de l'IA (`ModTactics`, schéma par équipe
  ou `ORIGINE`), comme contre l'ordinateur.
- The World exclu du tirage, comme en ligne (super retiré du mod depuis le 2026-11-27).
- Temps accéléré : `ModMeta.Speed` multiplie tous les `TimerSetSpeedUp` du module (Ball, Recorder, SoccerModule).
  Essai du 2026-11-26 (7 contre 3, mi-temps 120 s, 2 matchs par vitesse) : tirs, possession (46/54), passes,
  récupérations et distance du même ordre à ×1, ×3 et ×5 ; 384/330 s réelles à ×1, 112 s à ×3, 65 s à ×5.
  ×3 retenu par prudence.
- Fin du match : `meta_result.txt` (équipes, niveau, stade, score, tactiques, statistiques de
  `ModStats.PutMatchStats` en hexadécimal, secondes réelles), puis `GameObject.ExitGame`.

## En base

Score : `meta_run.py` le prend dans le fil du match (buts de chaque équipe dans les statistiques). Piège corrigé le
2026-11-26 : `Match.m_nScoreLeft / Right` restent ceux des côtés du coup d'envoi, ils ne suivent pas l'échange des
équipes à la mi-temps ; `ModMeta` les inversait, d'où des scores à l'envers sur le site (36 premiers matchs réparés
en base à partir de `match_events`).

Comme un match en ligne : `kind = ranked`, `rated = false` (pas de compte, pas d'Elo), `room_code = META`, joueurs
« IA niveau 7 / 8 », statistiques complètes (joueurs, tirs, heatmaps). Côtés alternés d'un match à l'autre.

## Sur le site

- `/meta` : tableau des équipes (dont buts subis et supers par match), tableau des supers (utilisations, victoires de
  l'équipe qui l'a lancé, un match compté une fois par équipe), carte des buts (tirs gagnants, `match_events`).
- `/meta/<animal>` : fiche d'une équipe (bilan, moyennes, où elle marque / encaisse, buts par quart d'heure, zones
  occupées, joueurs, adversaires, supers, schémas, derniers matchs).
- Terrain du site (`web/components/Pitch.tsx`) aux proportions du jeu, relevées sur la mini-carte du HUD
  (`Data/Menu/P_Menu.png`, 146 x 82 px ; terrain de 5802 x 3196 unités) : surface sur 16 % de la longueur et la
  moitié de la largeur, pas de petite surface.
- Constat des 250 premiers matchs : l'IA tire beaucoup de loin, souvent depuis le même point sur l'aile
  (profondeur 0,645, latéral 0,16 ou 0,83) ; plus de la moitié des buts viennent de plus de 30 % du terrain.

## Suite prévue : schéma contre schéma (2026-11-27)

- Méta des équipes d'abord (toutes en ORIGINE), à **8 jeux** : banc du 2026-11-27 (Ryzen 9 9950X3D, 32 threads),
  1,8 cœur par jeu (pas de limite d'images), 1,72 à 6 jeux, 1,09 à 12 jeux (CPU total 54 %, matchs plus lents) :
  au-delà de 8, les jeux se gênent et l'IA joue à moins d'images par seconde.
- Puis les schémas en **matchs miroirs** (même équipe des deux côtés, schéma différent) pour retirer la force de
  l'équipe : 6 schémas = 15 paires ; 20 à 30 matchs par paire pour un écart net (beaucoup de nuls).
- À faire avant : un schéma par côté dans `ModMeta` (par exemple `MetaSchemes(s, s)`, lu par
  `ModTactics.GetTeamComposition` en match méta) ; vérifier qu'une équipe peut jouer contre elle-même
  (`ETeams.GetTeamData` rend le même objet pour les deux côtés : le copier pour avoir deux tenues). En base,
  `room_code = META-SCHEMA` pour séparer de la méta des équipes.

## Diagnostic de la possession (2026-11-27)

Bullrusia est à ~41 % de possession dans les trois séries (40,7 / 41,3 / 40,8) alors qu'il fait le plus de passes et
autant de récupérations que ses adversaires ; ses réglages Force et Précision ont changé entre les séries sans effet.
`ModMeta.DiagPossession` (appelé par `AITeam.Update`) compte, jeu en cours seulement : temps où l'équipe tient le
ballon, ballon libre après sa touche, dont gardien, temps pendant ses supers, phases de jeu (et celles finies par un
arrêt de jeu), temps de jeu, phase la plus longue, plus le compteur du jeu (`Team.m_fEntirePossesionTime`). Il
n'agit pas sur le match (pas de `_MOD_VERSION` en plus). Ligne 8 de `meta_result.txt`, rangée par `meta_run.py`
dans `build\meta\diag.jsonl` (une ligne JSON par match, avec le numéro du match en base).
En base : table `match_possession` (une ligne par équipe et par match, `server/schema.sql`), écrite par `meta_run.py`
à chaque match ; `python mods\meta\meta_run.py --import-diag` recharge `diag.jsonl` (sans doublon, à relancer quand
on veut, même série en cours). Sur le site : section « Possession détaillée » de `/meta/<animal>` (l'équipe contre
ses adversaires dans les mêmes matchs). Premiers matchs : le compteur du jeu vaut à peu près « tient + libre après sa
touche » moins le temps des supers ; le ballon libre fait environ 70 % du temps de balle d'une équipe.

## Série 3 (2026-11-27, 836 matchs)

- Écart entre équipes : écart-type des points par match 0,26 (0,29 en série 1, 0,34 en série 2) ; le bruit seul
  (~120 matchs par équipe) donnerait ~0,12. De 0,81 (Tortue) à 1,83 (Dino). Dino et Girafe sont dans le trio de tête
  des trois séries, Tortue et Requin en bas.
- Défense = taille du gardien : buts encaissés par tir subi de 3,4 % (Girafe) et 3,7 % (Dino) à 9,3 % (Tortue) et
  10,3 % (Robot), à peu près dans l'ordre de la taille du modèle, malgré `KeeperMinWidth(90)` / `KeeperMinReach(95)`.
- Niveau 8 : 1,34 but par équipe contre 1,03 au niveau 7. 32 % de nuls, 12,7 % de 0-0. Côté du coup d'envoi sans effet.
- Bullrusia (diagnostic, 91 matchs) : son compteur est juste. Ce n'est pas lui qui perd le ballon plus vite (temps
  tenu par phase 1,13 s, normal), c'est l'adversaire qui le garde plus longtemps : contre lui, une phase adverse dure
  4,8 s au lieu de 3,6 (ballon libre après la touche adverse 3,55 s au lieu de 2,64), d'où moins de phases pour
  les deux équipes (49 / 52 au lieu de 60). Le Taureau met plus de temps à récupérer un ballon libre. Ni la vitesse
  (Girafe 80, la plus lente, est la plus en possession), ni le graphe d'animation (`Bull.scr` = `Dino.scr`), ni la
  classe (`AIBull` = `AIGorilla` sauf la taille) n'expliquent seuls ; reste la combinaison largeur 101 / profondeur
  101 / dribble 0 (Dino : profondeur 240, dribble -10). Sans conséquence sur les résultats (1,41 pt, 5e, 0,89 but
  encaissé).
- Retouches après la série 3 (`team_stats.scr`, gardiens laissés tels quels) : Dino R 85→83 ; Girafe F 95→90
  (retour à la portée de tir d'origine, il faut construire le jeu) et P 91→88 ; Panthère F 84→88 ; Requin P et R
  90→92 ; Tortue R 83→88 ; Robot F 80→84, R 95→97 ; Panda P 85→89. Série 3 sauvegardée dans `build\meta\serie3*`.

## Tactiques : Tortue contre Dino (2026-11-27, 132 matchs sur 216, stats retouchées)

`meta_run.py --tactics 10-3 --per-combo 6` : chaque schéma prédéfini de la Tortue contre chaque schéma du Dino
(`MetaSchemes`, salon `META-TACTIC`). Série 3 retirée de la base avant (sauvegardée dans `serie3.sql`).
- Dino 99 V / 22 N / 11 D (2,42 pts), 3,73 buts par match ; en série 3 (ORIGINE contre ORIGINE, anciennes stats) :
  2 V / 5 N / 4 D pour la Tortue sur 11 matchs.
- Les schémas à rôles ouvrent le jeu : 5,96 buts par match quand aucune équipe n'est en ORIGINE, 3,20 avec une,
  2,50 avec deux ; conversion des tirs 12,1 % contre 7,9 % ; joueurs qui courent 20 % de plus.
- Ils profitent à l'équipe forte : Tortue 0,90 pt contre le Dino en ORIGINE, 0,05 à 0,63 contre ses autres schémas
  (INDIVIDUEL le plus dur, ATTAQUE 7,5 buts marqués par match). Le schéma de la Tortue change peu (0,30 à 0,55 pt,
  dans le bruit à ~20 matchs).
