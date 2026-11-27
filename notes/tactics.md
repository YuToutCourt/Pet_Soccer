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

## Tactiques variées de l'IA (2026-11-27)
- Demande : « que l'IA joue avec des tactiques différentes, ORIGINE ou les autres ». `AIVary(1)` dans `roles.scr`.
- Début de match : chaque équipe IA tire un schéma, gardé tout le match (`ModTactics.DrawScheme`, cache
  `m_tMatchSchemes` par `Team`). Avec la chance `AIFavorite` (0,4), son schéma préféré (`TeamScheme(animal, schéma)`,
  un par équipe : Tortue et Panda en BÉTON, Taureau / Lion / Requin / Tigre en ATTAQUE, etc.), sinon tirage pondéré
  `AISchemeWeight` (ORIGINE 3, les cinq autres 1).
- Mi-temps : `AIAdapt(chance, retard, avance, si menée, si elle mène)` = `(0.7, 1, 2, "ATTAQUE", "BETON")`. Score suivi
  par `ModTactics.OnGoal` (appelé dans `AIMatch.OnGoal`, équipe qui marque).
- Pas de tirage : équipe du joueur, équipe distante en ligne, matchs de méta (`ModMeta.IsActive`) : la méta garde
  `AIScheme` (ORIGINE) pour toutes les équipes, `TeamScheme` y est ignoré tant que `AIVary(1)`.
- Journal : `[mod] tactique IA : animal n joue X` et `... passe de X a Y a la mi-temps (a-b)`.
- Aux niveaux 1-4 (marquage 0), seul le placement change par rapport au jeu d'origine.

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

## Coup d'envoi de l'IA (`AIFootBaller.PassSpecialBall`, `ModKickoff`)
Bug signalé le 2026-11-19 : sur certains engagements (début de match, après un but), la passe de l'IA n'était pas reprise
et le ballon sortait en touche.

- **Cause (jeu d'origine)** : pour une équipe IA, la passe d'engagement visait `Strategy.ChosePassPosition`, un point à
  environ 180 unités du receveur, en diagonale vers la touche. `RealKick` devait remplacer la cible par la position du
  receveur en état 7, mais `GoalKick.ActionAfterFalling` passe en état 0 (`StartGameDirectly`) juste avant `RealKick` :
  cette correction ne s'appliquait jamais. Si le receveur ne devenait pas actif à temps, personne n'allait au ballon.
  L'équipe humaine, elle, vise déjà les pieds du receveur.
- **Correction** : en état 7, le passeur IA vise les pieds de `m_cSpecialDestFB` et le désigne comme destinataire
  (`m_cKick.m_cPassDestFB` -> `SetPassActive` : actif, `destpasse`, va au ballon).
- **Traces** : avec `DebugLog(1)`, `ModKickoff` écrit chaque engagement (`engagement #n passeur -> receveur vise x (ecart receveur)`),
  puis toutes les 0,25 s pendant 5 s : ballon, cible, porteur, actif de chaque équipe (but, special, destpasse), et
  `TOUCHE` si le ballon sort. `KickoffTest(8)` relance un coup d'envoi toutes les 8 s (côtés alternés), pour les tests.
- Test du 2026-11-19 (match direct Polska / Holandia, `game_dev_b`) : avant, 6 engagements IA visaient à 172-190 unités
  du receveur ; après, 5 sur 5 dans les pieds, le receveur actif dès le départ, ballon contrôlé en 0,25 à 0,5 s.
- Au passage : `ModMovement.Adjust` levait une `NullPointerException` quand le porteur IA servait tout de suite un
  coureur (`OnPass` remet `m_vRun` à null pendant `TryStartRun`). Vue deux fois dans `crash.log`, corrigée.

## Changement de joueur et jauge SUPER (`ModSwitch`, `data\switch.scr`, `data\supers.scr`)
Demandé le 2026-11-19 (todo.md) : « le SUPER est trop fort, je fais plein de passes et je le stocke » et « le changement
de joueur pourrait être mieux, voire 6 touches pour prendre le joueur qu'on veut ».

**Jauge SUPER** : une lettre s'allume toutes les `Strategy.m_fSuperUpgradeTime` secondes de possession continue
(`Team.Update` d'origine), les passes ne coupent pas la possession, et un but en ajoute une. Choix de l'utilisateur :
seulement un remplissage plus lent. `FillTime(10)` dans `supers.scr` (jeu d'origine 6 : 50 s de possession pour un super
au lieu de 30), appliqué aux deux équipes après `ModDifficulty.Apply` (`Strategy`). 0 = valeur du jeu.

**Jauge gardée à la mi-temps, autres sources de lettres** (todo.md, 2026-11-25). Le jeu vidait la jauge au changement
de côté (`AIMatch.ChangeTeamSides` → `ResetSupers`) ; maintenant chaque équipe garde ses lettres, son type de super et
ses compteurs, et `ModSupers.OnSidesSwapped` rallume le HUD du bon côté (l'icône est d'abord cachée : `LightLetters` ne la
cache jamais sous 5 lettres). Remise à zéro seulement au début du match. Nouvelles sources (`ModSupers.AddAction`,
compteurs `AITeam.m_nModSuperActions`) : une lettre tous les `ShotsPerLetter(8)` tirs cadrés (choix de l'utilisateur : comptés à
l'issue, arrêt du gardien dans `ModStats.AddSave` ou but dans `ModStats.OnGoal` ; un tir arrêté puis repoussé dans le but
ne compte qu'une fois ; pas le super tir, `ModStats.Shot.m_bSuper` ; le poteau seul ne compte pas), `RecoveriesPerLetter(15)` ballons repris (`ModStats.OnPossession`), `SavesPerLetter(8)` arrêts du gardien
(`ModStats.AddSave`) ; 0 = source coupée. Rien pendant un super, au but en or, en ralenti, ni jauge pleine (pas de réserve) ;
en ligne, seul l'hôte compte. Journal en debug : `[mod] lettre super : <équipe> tir|recuperation|arret n/X`.

**Tirage du super** (bug signalé le 2026-11-20 : « j'ai toujours le même super des 2 côtés ») : le jeu tire avec
`GameObject.RandomFloat`, un `Random(0)` à graine fixe, donc les mêmes tirages d'un lancement à l'autre, et les deux
équipes tombaient souvent sur le même type. `ModSupers.GetRandomSuper(équipe)` tire maintenant avec son propre
générateur (graine selon l'heure) et exclut le super que l'autre équipe a en ce moment. Journal en debug :
`[mod] super tire pour …`, `[mod] HUD super : …`, `[mod] UseSuper …`.

**Touches 1 à 7 en mode debug** : le jeu d'origine y met des raccourcis de debug (`AIFootBaller.OnInputKey` : animations
du joueur contrôlé ; `AIReferee.OnInputKey` : arbitre téléporté). Ils sont ignorés pour les touches directes
(`ModSwitch.IsSelectKey`), sinon appuyer sur 1 à 6 en debug lançait aussi une animation.

**Changement de joueur** (équipes humaines seulement, l'IA garde son comportement) :
- Jeu d'origine : la touche passe sans ballon appelle `AITeam.ChangeActive` → le plus proche de la destination du ballon
  (`FindNearest`), ou le 2e s'il est déjà actif ; en défense `FindAttacker`. `AITeam.UpdateGame` change aussi tout seul,
  bloqué par `BlockControl` tant qu'une flèche est tenue et 0,4 s après (`BlockControlTime`).
- Mod (`ModSwitch.Choose`, appelé par `ChangeActive` en jeu) :
  1. le coéquipier le plus proche du ballon (position réelle du ballon, pas sa destination, et plus d'« attaquant »
     en défense) : demandé par l'utilisateur le 2026-11-20 ;
  2. anti-retour : pas le joueur qu'on vient de quitter pendant `SwitchNoBack` (0,6 s), donc appuyer deux fois passe au
     suivant le plus proche ;
  3. option, désactivée par défaut (`SwitchCone(0)`) : avec une flèche tenue, le plus proche dans un cône de
     ±`SwitchCone` degrés autour de la direction (`TeamController.GetFieldDirFromKeys`). Désactivée parce qu'on tient
     presque toujours une flèche en jouant, et le choix ne tombait alors pas sur le plus proche.
- Verrou : après un changement manuel ou une touche directe, pas de changement automatique pendant `SwitchLock` (1,2 s),
  en prolongeant le `BlockControl` d'origine (`ModSwitch.CanUnblock` dans `AITeam.UpdateParams`). Un coéquipier qui
  reçoit le ballon prend toujours la main (cas 1 de `UpdateGame`, inchangé).
- `AITeam.SwitchTo` : changement commun (actif + focus, but de l'ancien effacé, verrou).

**Touches directes** vers les joueurs 2 à 7 (numéros de l'écran Tactique : place k de `ModTactics.ApplyTeam` = joueur
k + 2, `ModTactics.GetNumbered`, repli sur l'ordre des points d'apparition en composition ORIGINE ou chez le client) :
- Par défaut touches 1 à 6 de la rangée du haut pour le joueur 1, aucune pour le joueur 2.
- Réglées dans Options > Commandes, ligne « Joueurs 1-6 » (6 cases `ID_EDIT_KEY_SEL2..7`, `MenuOptionsControls`,
  `OptionsControls*.scr` générés par `mods/ui/make_controls_scr.py`, lignes un peu resserrées pour tenir dans le cadre).
  Doublons vidés avec les autres touches. Manette : boutons acceptés, retirés en repassant au clavier.
- Enregistrées dans `mods.dat` (`SelKey<jeu>_<numéro>`), pas dans la sauvegarde du profil (`KbSettings` inchangé).
- `TeamController.OnInputKey` / `OnInputJoyButton` → `ModSwitch.OnKey` (jeu de touches = `m_nKeySet`, fixé dans `Init`).
  Ignorée hors du jeu, si l'équipe a le ballon, si le joueur est occupé ou déjà contrôlé.
- En ligne : le client envoie `_ACT_SEL + k` (11..16), l'hôte appelle `ModSwitch.Select` sur l'équipe de l'invité.

**Numéros des joueurs** (`ModSwitch.UpdateNumbers`, appelé par `PetSoccerHud.OnFrame`) :
- Sur la mini-carte, à droite du point de chaque joueur humain (`ID_MAPNUM_*`), activés par défaut (`MapNumbers` dans
  `switch.scr`). Choix de l'utilisateur : la caméra ne montre pas tous les joueurs, la mini-carte si.
- Le texte affiché est le nom de la touche directe choisie pour ce joueur dans Options > Commandes
  (`ModSwitch.GetLabelText`, `KbSettings.GetKeyString` raccourci : `Numpad7` → `N7`, `LControl` → `LCtrl`, manette
  `Feu3`), jeu de touches de l'équipe (en ligne celui du joueur 1). Sans touche (joueur 2 par défaut), le numéro
  Tactique - 1 (1 à 6), demandé par l'utilisateur (« j'appuie sur 2, ça va sur le 3 ») ; touche affichée demandée le
  2026-11-25 (todo.md). Texte de départ posé par `make_hud_numbers_scr.py` (étiquettes élargies : 0,10 au-dessus des
  têtes, 0,06 sur la mini-carte), les identifiants gardent le numéro Tactique.
- Au-dessus des têtes (`ID_NUM_*`) : option « Numéros sur les joueurs » du menu Mods, désactivée par défaut
  (l'utilisateur ne les veut pas d'office). Hauteur `NumberHeight` (120, un joueur mesure environ 90), plus
  `NumberFocusExtra` pour le joueur contrôlé (au-dessus du losange d'origine).
- Blanc pour le joueur contrôlé, vert ou rouge comme la flèche de l'équipe sinon. En ligne, chacun ne voit que les
  numéros de son équipe.
- `Hud*.scr` générés par `mods/ui/make_hud_numbers_scr.py`. Pièges vus en test : une étiquette `Visible(0)` ne s'affiche
  jamais (déclarer visible, cacher dans le code) ; seules les polices chargées pour la résolution marchent
  (`SoccerFonts.LoadFonts`) ; `Camera.PointToScreenClampToFrustum` donne des positions décalées, utiliser
  `GameObject.PointToScreen` avec la caméra active (comme `MeshObject.ShowObjectName`).

**Tests** (2026-11-19/20, `online-test.ps1 -Bot -HostBot`) : le client automatique envoie aussi des touches directes.
Journal (`DebugLog(1)`) : `[mod] switch <équipe> : direction|proche|suivant|touche n -> n<joueur>` ; les quatre cas
vus, touches de l'invité appliquées par l'hôte, numéros identiques des deux côtés. `ControlsTest(1)` dans
`debugconf.scr` ouvre l'écran Commandes au menu (après la connexion). Pas encore validé au clavier par l'utilisateur
(saisie des touches dans l'écran Commandes, flèche + passe, touches 1 à 6, rythme du SUPER).
