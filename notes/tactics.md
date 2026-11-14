# Rôles, schémas et marquage (mod)

## Jeu d'origine
- 7 joueurs par équipe (stade standard) : 1 gardien, 3 défenseurs, 3 attaquants. Le type vient du point
  d'apparition dans le `.map` du stade (`SpawnPoint.sFootballerType` : `ATTACKER` / `DEFENDER` / `GOALKEEPER`),
  copié dans `FootBaller.m_nType` (0 attaquant, 1 défenseur, 2 gardien). `E6ThreeASide` : 1 gardien, 2 défenseurs, 1 attaquant.
- Sans le ballon, chaque joueur suit un bloc (`AITeam.UpdateValidPos`) : centre du bloc = profondeur du ballon,
  + son écart de départ, + bruit (`ValidPosDeviation`), borné par `Min/Max(Defender)ValidPosScope` autour de son
  point de départ, puis projeté sur son couloir (`aPatrolPath`, `Strategy.CalculateValidPosition`).
- Seuls le joueur actif (presse le porteur) et un « aide » vont au ballon. **Personne ne marque** : un adversaire
  démarqué reste libre. Les attaquants ne redescendent jamais sous ~38 % du terrain.
- `IsUserControled()` vaut pour **toute** l'équipe du joueur ; le joueur dirigé au clavier est `m_cActiveFootBaller`.

## Mod (`ModTactics`, `data\roles.scr`)
- Hook en fin d'`AITeam.UpdateValidPos` → `ModTactics.UpdateValidPos` (en jeu : état 0, pas en attente, ni gardien
  ni joueur dirigé ; arrêts de jeu : voir « Carte et placements »). Rôles attribués à chaque `AIMatch.Restart` (`ModTactics.OnRestart`).
- **Rôle** : placement sans ballon en fractions du terrain (0 = son but, 1 = but adverse ; largeur 0 = axe) :
  `Depth + Follow × (profondeur ballon − 0,5)` (+ `Push` si l'équipe a le ballon), borné par `MinDepth`/`MaxDepth` ;
  couloir `Width` (plusieurs joueurs du même rôle répartis de −Width à +Width, `Side(1)` = seul, garde son côté),
  `FollowX` = suit le ballon en largeur ; `Cover(1)` = reste derrière l'attaquant adverse le plus avancé ;
  `Engine` = type moteur donné au joueur (0/1). 8 rôles : Libéro, Stoppeur, Défenseur, Latéral, Milieu, Ailier, Attaquant, Pivot.
- **Attribution** : chaque joueur de champ a une place 0..5 (joueurs 2 à 7 de la carte) ; la composition donne un rôle
  et des positions par place (voir « Carte et placements »). Un schéma range ses rôles du plus défensif au plus offensif
  sur les places 0..5, donc le plus défensif va à la ligne la plus reculée.
- **Schéma** `ORIGINE` = placement d'origine intact (types du `.map`). Autres schémas : 6 rôles.
  `AIScheme` / `TeamScheme(animal, schéma)` pour l'IA (défaut ORIGINE), `HumanScheme` pour le joueur avant tout choix.
- **Marquage** (équipe sans le ballon) : toutes les 0,25 s, les menaces (adversaires sans ballon, gardien exclu,
  à moins de 70 % de profondeur) sont triées de la plus proche de notre but à la plus lointaine ; chacune prend le
  marqueur libre le plus proche dans son `MarkRange`. Position = point entre l'adversaire et notre but à
  `MarkDistance`, mélangé à la position de zone selon la force : `Marking` du rôle × difficulté (IA) ou
  × `HumanMarking` (coéquipiers du joueur). Schéma ORIGINE : seuls les défenseurs de l'IA marquent (force = difficulté).
  Exclus : porteur, joueur actif, aides. Le marqueur lâche s'il est entraîné à plus de 1,3 × `MarkRange` de sa zone.
- Difficulté (`difficulty.scr`) : `Marking_level` 0 aux niveaux 1-4, 0,3 (5) → 1,0 (10) ; `MarkDistance_level` 220 → 110.
  Les niveaux 1 à 4 restent identiques au jeu d'origine tant que l'IA joue en ORIGINE.
- `DebugLog(1)` dans `roles.scr` : rôles, positions, cibles et marquages dans `crash.log` toutes les 2 s.

## Écran Tactique (`MenuTactics`, `Menu\scr\Tactics*.scr`)
- Accès : bouton « Tactique » de l'avant-match (`MenuPreMatch`, écran 18 de `SoccerMainMenuModule`) et du menu
  pause (`MenuInGame` → `SoccerInGameMenuModule.ShowTactics`), **seulement à la mi-temps** : le bouton est grisé
  pendant le jeu et en fin de match (`MenuInGame.m_bMatchHalfTime`, recopié depuis `SoccerModule` à chaque ouverture). OK à la mi-temps : appliqué pour la 2e période.
- Menu de fin de match/pause remonté (`ID_MENU` y = 0,24, boutons à 0 / 0,19 / 0,38 / 0,57) pour que les 4 boutons ne cachent pas le badge Elo.
- Cadre gauche « Placement » : situation `< Jeu / Coup d'envoi / Touche / Dégagement >` (« (auto) » = pas de placement
  choisi), carte du terrain (bouton `ID_FIELD`, notre but à gauche) et pions `ID_TOK0..5` (joueurs 2 à 7), `ID_TOK_GK`
  (gardien, fixe), `ID_TOK_BALL`. Clic sur un pion = sélection (jaune) ; clic ailleurs sur la carte = le joueur
  sélectionné y va (`GetCursorPos` ramené au rectangle réel de la carte). Au coup d'envoi, profondeur bornée à 0,47.
- Cadre droit « Composition » : schéma `< >` (remet tous les rôles et placements par défaut), « Joueur n », rôle `< >`
  du joueur sélectionné (si sa position de jeu est choisie, il prend la profondeur du nouveau rôle, même couloir),
  description. Bouton « Auto » en bas : remet la situation affichée en automatique.
- Jeu d'origine : pions gris aux points d'apparition. Toute modification passe en « Personnalisé » avec 3 défenseurs +
  3 attaquants, position de jeu = points d'apparition, autres situations automatiques.
- Piège : dans un `.scr` d'interface, le **premier** élément défini est dessiné **au-dessus** des suivants (pions avant la
  carte, carte avant les cadres). Les `Static` avec texte coupent les lignes tout seuls à leur largeur.
- Équipe : en match celle qui est dirigée ; avant le match l'hôte, ou l'invitée en championnat si le joueur l'est.
- Sauvegarde `game_dev\tactics.dat` : `profil;animal;schéma;rôle1,...,rôle6;jeu;engagement;touche;dégagement`, rôles dans
  l'ordre des places, chaque placement = 6 couples `profondeur/latéral` en millièmes ou `-` (automatique). Les anciennes
  lignes (`profil;animal;schéma;rôles`) sont lues avec les rôles rangés par profondeur, comme avant.
- Test sans souris : `!TacticsTest(i)` + `TacticsTest(1)` dans `game_dev\debugconf.scr` ouvre l'écran au menu principal
  (après la connexion ; sans équipe choisie, la composition est enregistrée sous l'animal -1).
- Fichiers générés : `mods/ui/make_tactics_scr.py` (Tactics*.scr à partir d'OptionsMatch*.scr) et
  `mods/ui/add_tactics_buttons.py` (bouton dans PreMatch*.scr et InGame*.scr). Listes déroulantes évitées :
  elles s'ouvrent vers le bas et sortiraient de l'écran.
- `SoccerMainMenuModule` : tableau d'écrans passé à 19 ; les boucles codées `< 18` (masquage, changement de
  résolution) utilisent `m_tUIs.length`, sinon l'écran Tactique reste affiché sous le suivant.
- Piège : `GameObject.CrashLog` interprète `%` comme un format, ne pas en mettre dans les messages.

## Carte et placements (`ModTactics`, `MenuTactics`)
- Repère : profondeur 0 = son but, 1 = but adverse ; latéral 0 = **haut de l'écran**, 1 = bas. Caméra par défaut côté X
  faible, tournée vers X croissant : haut = X élevé (`_TOP_IS_HIGH_X`, vérifié sur la mini-carte). Comme dans le jeu
  d'origine (points d'apparition échangés à X identique), un joueur garde son côté de l'écran à la mi-temps.
- Places : points d'apparition triés par ligne (écart de profondeur < 0,03 = même ligne), défense puis attaque, puis de
  haut en bas. Stade standard : 2 = n5 (20/23), 3 = n4 (18/50), 4 = n3 (20/78), 5 = n1 (44/39), 6 = n2 (41/50),
  7 = n0 (44/61) (`_DEFAULT_LAYOUT`). La ligne `[mod] tactique` du journal donne `j<numéro>=n<joueur>:rôle@base`.
- **Jeu** : base (profondeur, latéral) par joueur ; en match `RolePlace` : base + `Follow` × (ballon − 0,5) (+ `Push`),
  bornée à [base − (Depth − MinDepth), base + (MaxDepth − Depth)], latéral base + `FollowX` × (ballon − 0,5).
  Automatique : profondeur `Depth` du rôle, couloirs répartis comme avant (`Width`, `Side`) ⇒ identique à l'ancien comportement.
- **Coup d'envoi** (état 7, les deux équipes, que l'on engage ou non) : `AIMatch.RestartGameRaw` → `PlaceKickoff` (les
  joueurs apparaissent à leur place, écran noir) ; `AITeam.UpdateStabilizationPos` (cas 7) → `KickoffPos`. Le plus
  proche du rond central engage, le suivant reçoit (code d'origine). Automatique : latéral de jeu, profondeur
  min(base, 0,3 + 0,2 × base, 0,47).
- **Touche** (état 5, la nôtre) : dessinée ballon en haut à mi-terrain ; en match profondeur = ballon + (p − 0,5),
  latéral inversé si la touche est en bas. **Dégagement** (état 6, notre gardien) : positions fixes.
  Touches/dégagements adverses, corners, ou placement automatique : `RolePlace` autour du ballon (`GetSpecialBallPos` /
  `GetGoalKeeperOutPos`), avec `Push` si c'est à nous. Jeu d'origine : placement d'origine inchangé.
- `DebugLog(1)` écrit aussi `[mod] arret <état> <équipe> j<n> cible profondeur/latéral` (toutes les 3 s par joueur).
- Test du 2026-11-11 (match direct, composition de test) : coup d'envoi, touche (en bas, donc inversée) et dégagement
  conformes au placement ; le 7 placé en haut reste en haut en jeu (X ≈ 92 %).

## Hors-jeu doux et passes longues (`ModTactics`, réglages en fin de `roles.scr`)
- Problème d'origine : quand son équipe a le ballon, un attaquant (type 0) reçoit +500 en profondeur et peut aller jusqu'à
  `MaxValidPosScope` (2700) de son départ, donc derrière la défense. L'IA lui envoie alors un lob (portée = demi-terrain).
- **Ligne de hors-jeu** (`OffsideLine`, repère de l'attaquant) = avant-dernier adversaire (gardien compris), jamais en deçà
  du ballon ni de la ligne médiane. Pas de coup de sifflet.
- Placement : en fin de `ModTactics.UpdateValidPos`, la cible d'un joueur sans ballon est ramenée à `ligne − OffsideMargin`
  (deux équipes, tous schémas, ORIGINE compris ; sauf le joueur dirigé au clavier).
- `Strategy.ChoseBestNewPosition` (appel de balle « GoToBest ») refuse une position hors-jeu.
- Passes : `Strategy.FindBestFootBaller` (choix de l'IA, têtes) et `FindPassPlayer` côté IA ignorent un coéquipier à plus
  de `ligne + OffsideTolerance`. Le joueur humain garde le droit de passer où il veut. La passe en profondeur reste
  possible : le receveur est en jeu au départ et court vers le point visé (`ChosePassPosition`).
- **Passes longues** (`ModTactics.LongPassError`, appelé au début d'`AIFootBaller.Pass`) : en jeu courant (état 0), hors
  gardien, au-delà de `LongPassStart` (1200) le point visé dérive dans une direction aléatoire de
  `LongPassError × excès` (0,25), tiré entre 50 et 100 %, au plus `LongPassMaxError` (450). Vaut pour les deux équipes.
- Les paramètres d'origine `PassPrecision`, `PerfectPrecisionDist`, `MaxPrecisionDist` d'`ai_params.scr` sont lus mais
  jamais utilisés par le jeu.
- `DebugLog(1)` écrit aussi la ligne de hors-jeu de chaque équipe et chaque passe longue (`passe longue <distance> ecart <x>`).
  Test du 2026-11-10 (match direct IA contre IA, 100 s) : aucune cible au-delà de la ligne ; un attaquant qui revient
  d'une action peut rester quelques secondes derrière, mais ne reçoit plus de passe.

## Déplacements sans ballon : démarquage, appels, une-deux, passe dans la course (`ModMovement`)
Demande du 2026-11-11 (« passes en profondeur, se démarquer »). Choix de l'utilisateur : passe en profondeur **automatique
sur S** (pas de nouvelle touche), pour **les deux équipes**, réglée **par le rôle**.

- **Jeu d'origine** : sans ballon, un joueur va à sa place (formation ou rôle). Le seul démarquage
  (`Strategy.ChoseBestNewPosition`, événement 12) teste 4 points à `NewPosDist` (401) autour de lui, seulement dans la
  zone 2, sans regarder la ligne de passe. Il est désactivé pour les joueurs sans ballon quand `Movement(1)`.
- **Bug d'origine corrigé** (`PassToBest(1)`) : `Strategy.FindBestFootBaller` (choix du receveur de l'IA, `GoalPass`,
  `PassAndShotChance`, remises en jeu) gardait le coéquipier à la note de situation la **plus basse**. Il garde
  désormais la plus haute.
- **Démarquage** (`ModMovement.Adjust`, appelé dans `ModTactics.UpdateValidPos`) : quand l'équipe a le ballon, chaque
  joueur de champ dirigé par l'IA note toutes les `FreeRefresh` s 17 places autour de sa place de rôle (rayon
  `Free` × longueur). Critères : espace libre (adversaire le plus proche), ligne de passe dégagée depuis le porteur,
  gain vers l'avant, distance au porteur (ni collé ni trop loin), espacement avec les coéquipiers, et rester proche de
  sa zone. Il garde sa place tant qu'une autre n'est pas meilleure de `FreeKeep`. Le hors-jeu doux s'applique toujours.
  `Drop` : un joueur loin devant le porteur décroche vers lui (pivot).
- **Appels en profondeur** : toutes les `RunCheck` s, un joueur avec `Runs` > 0 peut partir dans le dos de la défense
  (chance `Runs` × `RunChance`), si : le porteur est derrière lui, il est à moins de `RunFrom` (0,25) de la ligne de
  hors-jeu, la ligne est avant 0,86, et `MaxRunners` n'est pas atteint. Cible : ligne + `RunDepth`, couloir droit, vers
  l'axe ou vers l'extérieur (le plus libre). Il court le temps du trajet + 0,4 s (entre 1,2 et `RunDuration` s), puis se
  repose `RunCooldown` s. Dans une équipe IA, le porteur décide tout de suite s'il le sert. Pendant sa course, le
  hors-jeu doux ne le retient pas, et pendant `RunPassWindow` s il compte comme en jeu pour les passes (`IsOffside`).
- **Passe dans la course** (`ModMovement.ThroughPoint`, au début de `Strategy.ChosePassPosition`) : une passe vers un
  joueur en pleine course part devant lui, là où il arrivera (vitesse `RunSpeed` ou `FBSpeed`, temps de vol du ballon +
  `KickDelay`, avance au plus `ThroughLead`, jamais au-delà de 0,9). Si un adversaire (gardien compris) y arriverait
  plus de `ThroughMargin` s avant le coureur, passe normale ; un adversaire qui doit courir vers son propre but a
  `ThroughTurn` s de retard (il se retourne). Vaut pour **S** (le coéquipier visé avec les flèches) et pour
  l'IA. Le porteur IA sert un coureur tout frais avec la chance `AIThroughChance` toutes les 0,3 s
  (`ModMovement.AIThroughPass`, avant `AITeam.PassAndShotChance`).
- **Une-deux** (`ModMovement.OnPass`, dans `AIFootBaller.Pass`) : après une passe de moins de `GiveGoMaxDist`, le passeur
  repart vers l'avant (+0,12, au plus ligne + 0,06) avec la chance `GiveGo` de son rôle, pendant `GiveGoDuration` s. Une
  passe en retour lui arrive dans la course.
- **Réglages par rôle** (`Free`, `Runs`, `GiveGo`, `Drop`) : défenseurs presque immobiles, latéral et milieu en une-deux,
  ailier et attaquant en appels, pivot qui décroche. Le schéma ORIGINE (IA par défaut) utilise `OriginAttacker` et
  `OriginDefender` selon le type d'origine du joueur.
- `DebugLog(1)` écrit chaque appel, chaque coureur non servi par l'IA (`pas servi : adversaire plus pres` ou `tirage`),
  les compteurs `appels refuses/lances` (0 repos/porteur gardien/max, 1 ligne trop haute, 2 trop loin de la ligne,
  3 porteur devant, 4 trop loin du porteur, 5 tirage, 6 pas de couloir libre, 7 lancés), chaque une-deux, chaque passe dans la course
  de l'IA, chaque coureur servi, et ajoute `appel`, `une-deux` ou `demarque` aux lignes de position.
- Tests du 2026-11-11 (match direct Polska, sans toucher la manette, contre Holandia, niveau 9, schémas ORIGINE,
  3 min) : 13 appels, 10 une-deux, 4 passes de l'IA dans la course (dont des retours de une-deux), 6 coureurs servis,
  aucune exception. Avant les réglages `RunFrom` / `ThroughTurn` : 1 à 4 appels et aucune passe dans la course.
  Pas encore validé en jeu par l'utilisateur, ni la passe dans la course sur **S**.
