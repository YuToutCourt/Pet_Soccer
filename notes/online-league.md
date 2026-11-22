# Ligue en ligne (chantier 8)

Plusieurs amis dans une même ligue à 14 (ligue en aller simple + playoffs à double élimination, les règles du
mode Ligue, `notes/league.md`) ; les équipes qui restent sont jouées par l'IA. Uniquement en ligne.

État : **phases 1 à 4 faites** : le serveur garde les ligues (testé sans le jeu) ; dans le jeu, on crée, on
rejoint, on choisit son équipe, on lance, on suit classement et tableau, on **joue ses matchs contre l'IA** et
**contre ses amis** (vu à deux jeux sur ce PC, sans souris, 2026-11-22). Reste la phase 5 : essai en vrai.

## Règles choisies (2026-11-22)

| Sujet | Choix |
|---|---|
| Où | En ligne seulement, pas de ligue à plusieurs sur un même PC |
| Qui garde la ligue | Le serveur (`server\leagues.py`, base SQLite) : elle dure des jours et ne dépend d'aucun jeu allumé |
| Équipes | Une par joueur, deux joueurs ne prennent pas la même ; les autres sont à l'IA |
| Match contre l'IA | Joué en solo chez le joueur, quand il veut ; le score part au serveur. **Elo local** (celui du jeu contre l'IA) |
| Match entre deux joueurs | Les deux cliquent « jouer » : le serveur ouvre un salon classé entre eux. **Elo en ligne** + résultat dans la ligue |
| Ami absent | Chaque journée a un délai choisi par le créateur. À la fin du délai, les matchs pas joués sont simulés. Le créateur peut aussi « passer » la journée tout de suite |
| Playoffs | But en or à égalité, en solo comme en ligne (à ajouter au match en ligne : `_MOD_VERSION` à monter) |
| Abandon | Avant le départ : on sort. Après : l'IA reprend l'équipe. Le créateur parti avant le départ ferme la ligue |

## Phases

| # | Phase | État |
|---|---|---|
| 1 | Serveur : ligue à plusieurs humains, base, délais, paquets, salon des matchs entre joueurs ; tests sans le jeu | **fait** |
| 2 | Jeu : écran « ligue en ligne » (créer, rejoindre par code, choisir son équipe, lancer), classement et tableau reçus du serveur | **fait** |
| 3 | Jeu : matchs contre l'IA (joués en solo, score envoyé et gardé jusqu'à la réponse, Elo local), délai affiché | **fait** |
| 4 | Jeu : matchs entre joueurs (salon ouvert par le serveur, départ automatique, Elo en ligne), but en or en ligne, `_MOD_VERSION` | **fait** |
| 5 | Tests en vrai avec des amis | à faire |

## Serveur (phase 1)

| Fichier | Rôle |
|---|---|
| `server\league.py` | Logique de la ligue, copie de `ModLeague.java` avec un **ensemble** d'équipes humaines au lieu d'un joueur. Pas de réseau. |
| `server\leagues.py` | Les ligues : commandes des joueurs, délais, ce que voit chacun, base SQLite (table `leagues`, une ligne JSON par ligue). |
| `server\rooms.py` | `Lobby.league_play` : salon classé entre deux joueurs d'une ligue ; `settle` passe le score à la ligue. |
| `server\protocol.py` | `PKT_LEAGUE` (24), `PKT_LEAGUE_INFO` (35), drapeau `INFO_LEAGUE` (4) du `PKT_INFO`. |
| `server\test_league.py` | La logique seule (voir « Tests »). |
| `server\java\LeagueCheck.java` | Pilote `ModLeague.java` pour comparer les deux. |

### Une journée

- Elle se ferme quand tous les matchs des humains sont joués : les matchs entre IA sont alors simulés (comme
  en solo, leurs résultats n'apparaissent qu'à la fermeture). Les playoffs avancent de même, jour par jour.
- **Délai** (`delay`, en minutes, choisi par le créateur ; 0 = pas de délai) : il part à l'ouverture de la
  journée. Au bout, les matchs des humains pas joués sont simulés comme des matchs entre IA.
- Un match contre l'IA lancé (`LG_PLAY`) échappe au délai pendant 20 min (`--league-grace`) : le score de
  quelqu'un qui joue au moment où le délai tombe compte encore. Un match entre joueurs en cours dans un salon y
  échappe tant que le salon existe.
- `LG_SKIP` (créateur) : la journée se ferme tout de suite, mêmes règles que le délai.
- Heure : `time.time()` (heure réelle), pas l'horloge du salon : une ligue survit à un redémarrage du serveur.

### Même ligue que le jeu

`league.py` refait les calculs en float 32 bits et tire son hasard comme `java.util.Random`. Avec la même graine,
une ligue à un humain donne **exactement** la ligue solo du jeu : vérifié sur 40 ligues, sauvegarde par sauvegarde.
`League.save(équipe)` rend la suite d'entiers de `ModLeague.Save`, avec cette équipe comme « joueur » : le jeu
lira l'état reçu avec `ModLeague.Load` et l'affichera avec `MenuLeague` et `MenuLeagueBracket`.

Réglages pris dans le `league.scr` du **créateur** au moment de la création (qualifiés, revanche, points, forme,
buts par minute, bonus à domicile, effet de la note) et notes des 14 équipes calculées par son jeu. La durée
des mi-temps choisie à la création sert au calcul des buts simulés.

### Paquets

`PKT_LEAGUE` (jeu → serveur) : en-tête des paquets de salon, puis commande (octet), version de l'état que le jeu
a déjà (int), puis la suite. Chaque commande est redite jusqu'à la réponse et ne fait rien de plus répétée.

| Commande | Suite | Rôle |
|---|---|---|
| 0 `LG_SYNC` | — | Rien à demander : je regarde ma ligue (une fois par seconde devant ses écrans) |
| 1 `LG_CREATE` | réglages, équipe, tenue, 14 notes (x100) | Crée une ligue (code de 4 lettres) |
| 2 `LG_JOIN` | code, équipe, tenue | Entre dans une ligue pas encore lancée (code vide : la dernière ouverte, sinon `LGE_UNKNOWN`). Équipe prise : la première libre est donnée (`LGE_TEAM_TAKEN`) |
| 3 `LG_TEAM` | équipe, tenue | Change d'équipe avant le départ |
| 4 `LG_START` | — | Le créateur lance la ligue |
| 5 `LG_LEAVE` | — | Sort ; après le départ, l'IA reprend l'équipe |
| 6 `LG_RESULT` | clé, buts de l'équipe qui **reçoit**, de l'invitée | Score d'un match contre l'IA |
| 7 `LG_PLAY` | clé | « Je joue ce match » (contre l'IA), ou « j'attends l'autre » (redit 3 fois par seconde) |
| 8 `LG_SKIP` | — | Le créateur passe la journée |

`PKT_LEAGUE_INFO` (serveur → jeu), réponse à chacune : numéro du paquet, issue (`LGE_*`), puis si le joueur a une
ligue : code, drapeaux (créateur, lancée, finie), version de l'état, réglages, secondes avant la fin du délai
(-1 : aucun), mon équipe et mon prochain match (clé, -1 : aucun), les membres (nom, équipe, tenue, drapeaux :
connecté, moi, créateur, parti, m'attend, occupé), puis l'état (`ModLeague.Save`) en entiers « zigzag » de 1 à 3
octets, **seulement si le jeu ne l'a pas dans cette version**. Environ 700 octets avec l'état.

- **Clé d'un match** : numéro du match de ligue (0 à 90), ou 10000 + numéro du match du tableau.
- Le score va toujours dans l'ordre « équipe qui reçoit, invitée », pas « moi, l'autre ».
- Un match entre joueurs ne s'envoie pas par `LG_RESULT` (refusé) : il passe par le salon.
- Nul en playoffs (match interrompu) : `LGE_REPLAY`, le match reste à jouer.
- Version du dialogue : 4 (`LOBBY_PROTO`). Le serveur accepte encore la 3 (les jeux d'avant la ligue) : rien
  d'autre n'a changé, ils jouent en salon et en partie rapide comme avant.

### Match entre deux joueurs

1. Les deux sont devant l'écran de la ligue et cliquent « jouer » : chacun redit `LG_PLAY` avec la clé.
2. Quand les deux l'ont dit depuis moins de 3 s, le serveur ouvre un salon classé (`Room.league`), l'équipe
   qui reçoit en hôte : c'est elle qui simule, au stade de son équipe. Le `PKT_INFO` porte `INFO_RANKED` et
   `INFO_LEAGUE`. Les deux jeux doivent avoir la même version du mod (`LGE_VERSION` sinon).
3. La suite est celle d'une partie classée (`notes/online.md`) : annonces `PKT_MATCH`, forfaits, annulations.
   Le score retenu pour l'Elo (un forfait = défaite d'au moins un but) entre dans la ligue. Match annulé : il
   reste à jouer.

### Lancer

Rien de neuf : `python server\server.py`. Les ligues sont dans la même base que les comptes (`--db`).
Options de test : `--league-code` (code imposé), `--league-minute` (secondes par minute de délai),
`--league-grace` (minutes).

## Jeu (phase 2)

### Pour le joueur

1. Menu principal, « en ligne », bouton **ligue** (troisième bouton du cadre « salon », caché dans un salon).
2. Sans ligue : régler le **délai** d'une journée (6 h, 12 h, 24 h par défaut, 48 h, 72 h, aucun) et la
   **durée** des mi-temps, choisir son équipe (« équipe »), puis **créer** ; ou **rejoindre**, le champ du code
   vide : le serveur donne la dernière ligue ouverte (pas lancée, avec de la place, créée par un autre). Avec un
   code tapé, c'est cette ligue (Entrée dans le champ marche aussi). Le champ ne prenait pas le clavier chez
   l'utilisateur ni chez ses amis (le 2026-11-22, cause pas trouvée), d'où l'entrée sans code. Équipe déjà prise : le serveur en donne une autre, à
   changer avec « équipe » avant le départ.
3. Ligue pas lancée : le code à donner aux amis, les joueurs et leurs équipes (créateur marqué « * »).
   Le créateur clique **lancer** : les équipes restantes vont à l'IA.
4. Ligue lancée : journée en cours, temps avant la fin du délai, mon prochain match (contre l'IA, ou contre un
   ami, nommé). **tableau** ouvre le classement de la ligue (puis le tableau des playoffs), qui se met à jour
   pendant qu'on le regarde. **passer** (créateur) ferme la journée. **quitter** : l'IA reprend l'équipe.
   **jouer** : voir phases 3 et 4.
5. Couleurs : mon équipe en or, celles des autres joueurs en bleu (classement, résultats, tableau, liste des
   joueurs) ; un joueur déconnecté est en gris clair, un joueur parti en gris.

### Code

| Classe | Rôle |
|---|---|
| `ModOnlineLeague` | Dialogue avec le serveur (`PKT_LEAGUE` / `PKT_LEAGUE_INFO`), ma ligue, `ModLeague` relu de l'état reçu. `LG_SYNC` une fois par seconde tant qu'un écran de la ligue est ouvert (`m_bScreen`) ; une commande est redite toutes les 300 ms jusqu'à sa réponse. |
| `MenuOnlineLeague` | Écran (`_UI_ONLINE_LEAGUE = 26`), généré par `mods/ui/make_online_league_scr.py` (`OnlineLeague*.scr`). Relu 5 fois par seconde. |
| `MenuLeague`, `MenuLeagueBracket` | Mode « en ligne » quand `ModOnlineLeague.m_bView` : ligue lue par `GetViewLeague`, relus quand la version de l'état change, « jouer » caché, « retour » vers l'écran 26. Le mode Ligue solo ne change pas. |
| `ModLeague.m_tHuman` | Équipes des joueurs (pas sauvegardé), pour les couleurs (`IsOtherHuman`). |
| Branchements | `ModLobby` (version 4 du dialogue, paquet 35, `Begin` et `Send` ouverts, `OnClose`), `MenuOnline` (bouton `ID_ACTION3`, `NetText` statique), `MenuChooseTeam(Colors)` (retour à la ligue quand `m_bPickTeam`), `SoccerMainMenuModule` (écran 26). |

- Écran « en ligne » : trois boutons au lieu de deux (hauteur 0,075), lignes stade et durée descendues.
- 640 x 480 : « tableau » plutôt que « classement » (un texte plus large que son bouton lui fait perdre son
  fond), noms d'équipe courts quand une ligne ne tient pas (`MenuOnlineLeague.Fits`).
- Le message d'erreur d'une commande s'efface au bout de 10 s.

## Matchs contre l'IA (phase 3)

### Pour le joueur

- Écran de la ligue lancée : **jouer** quand mon match de la journée est contre l'IA (grisé contre un ami, en
  attendant la phase 4, et pendant l'envoi du score). Puis l'avant-match habituel, et le match : mon équipe à
  gauche, au stade de l'équipe qui reçoit, mi-temps de la durée choisie à la création de la ligue, difficulté
  d'Options > Match, but en or à égalité en playoffs.
- Au retour, l'écran dit « Victoire 2-1 contre Gorillakov » et où en est le score : « Envoi du score au
  serveur... », « Score enregistré dans la ligue », ou « Délai dépassé : ce match avait été simulé ».
- Elo **local** (celui du jeu contre l'IA, `ModElo`) et historique du profil (mode « Ligue ») comme un match
  normal. « Recommencer » est grisé. Quitter un match en cours ne compte pas : il reste à jouer.

### Code

- `ModOnlineLeague.PrepareMatch` : équipes, stade, `CoreLogic.m_bLeague` (« recommencer » grisé, retour de
  l'avant-match) et **`m_bMatch`**, qui dit à `ModLeagueMode.IsActive` que ce n'est pas le match de la ligue solo
  du profil (`league.dat` n'est jamais touché). Envoie `LG_PLAY` : le délai épargne le match pendant 20 min.
- Branchements : `Match()` (durée des mi-temps : `GetHalfSeconds`, ou `NetHalf` des tests), `Match.Update` (but
  en or : `IsGoldenGoal`), `AIMatch.AfterMatch` → `OnMatchEnd`, `ModStats` (mode Ligue),
  `SoccerMainMenuModule.ShowLoadingScreen` (retour à l'écran 26), `MenuPreMatch.OnBack`.
- **Score gardé** : `OnMatchEnd` l'écrit dans `online_league.dat` (dossier du jeu, une ligne
  `profil;code de la ligue;clé;buts de l'équipe qui reçoit;buts de l'invitée`), puis l'envoie (`LG_RESULT`, redit
  jusqu'à la réponse). La réponse, quelle qu'elle soit, l'efface. Jeu fermé avant : `LoadPending` le relit à
  l'ouverture de l'écran de la ligue et il repart. Un score d'une autre ligue, ou d'une ligue quittée, est oublié.
- Tant que son score n'est pas confirmé, le match n'est pas rejouable (`PlayableKey`).

### Test sans souris

```powershell
powershell -ExecutionPolicy Bypass -File mods\build.ps1 -Second
powershell -ExecutionPolicy Bypass -File mods\online-test.ps1 -League -Half 20
```

Avec la ligue lancée, chaque jeu joue tout seul jusqu'à 2 matchs contre l'IA (`ModOnlineLeague.AutoMatchFrame` :
une passe toutes les 2 s, « continuer » à la mi-temps, fin quittée après 5 s), en attendant entre deux que
l'autre jeu ait joué le sien ; il s'arrête avant un match contre l'autre jeu (phase 4). Journal :
`match 4 : equipe 2 contre 5 (IA)`, `fin du match 4 : 0-0, score 0-0 a envoyer`, `score du match 4 : reponse 0`.
Vu aussi (2026-11-22) : serveur arrêté pendant le match, score gardé dans `online_league.dat` puis renvoyé à sa
relance ; score écrit à la main dans ce fichier, renvoyé à l'ouverture de l'écran de la ligue.

Serveur lancé avec `--league-code TEST`. `game_dev` (`NetAuto(5)`) crée la ligue et la lance quand
`NetMatches(2)` joueurs y sont ; `game_dev_b` (`NetAuto(6)`, `NetCode("TEST")`) la rejoint. Chaque jeu montre
4 s l'écran « en ligne », puis l'écran de la ligue, puis le classement 8 s après le départ. Journal :
`[mod] ligue en ligne : ligue lancee : equipe …`. Pour avancer la ligue pendant le test, un faux jeu peut envoyer
`LG_SKIP` avec le jeton du profil de `game_dev` (dans `game_dev\online.dat`) : le classement passe tout seul au
tableau des playoffs, puis à la fin. La ligue reste dans `build\online-test.db` : le supprimer avant un nouveau
test, sinon le code TEST est pris. `-NoCode` : `game_dev_b` rejoint sans code (`NetCode("")`).

## Matchs entre amis (phase 4)

### Pour le joueur

- Mon match de la journée est contre un ami : l'écran dit « Contre un ami : cliquez tous les deux sur jouer »,
  « Lucas t'attend : clique sur jouer » quand il a cliqué, « Lucas n'est pas connecté » s'il n'est pas là.
- **jouer** : « En attente de Lucas... », le bouton devient **annuler**. Quand l'ami clique aussi, le serveur
  ouvre un salon classé : « Salon ouvert : le match va commencer ». Le match part tout seul, sans avant-match :
  chacun avec son équipe et sa tenue de la ligue, l'équipe qui reçoit à gauche (son jeu simule le match), à son
  stade, mi-temps de la durée de la ligue, but en or à égalité en playoffs.
- C'est une **partie classée** : Elo en ligne, forfait si on quitte en cours (défaite d'au moins un but, qui
  compte aussi dans la ligue), pause de mi-temps et « Tactique » comme d'habitude.
- Au retour, l'écran de la ligue : « Victoire 2-1 : +24 Elo » (vert, rouge, ou gris), puis la ligue à jour.
  Match annulé (« Match non compté ») : il reste à jouer, « jouer » repart.
- L'attente s'arrête toute seule : quitter l'écran de la ligue (« tableau » aussi), journée fermée entre-temps
  (« Ce match n'est plus à jouer »), salon ouvert mais match pas parti au bout de 20 s (« Le match n'a pas pu
  commencer »), versions du mod différentes, serveur sans salon libre.

### Code

- `ModOnlineLeague.StartWaiting` : `m_bWaitFriend`, `ModLobby.WantLeague()` (demande `_WANT_LEAGUE` : sans
  elle le jeu quitterait le salon dès qu'il l'apprend), puis `Frame` redit `LG_PLAY` toutes les 300 ms au lieu de
  `LG_SYNC`. Une erreur en réponse (version, salon, match plus à jouer) arrête l'attente.
- `FriendFrame` (appelé par `MenuOnlineLeague.Tick`) : dans le salon de la ligue (`ModLobby.m_bLeagueRoom`,
  drapeau `INFO_LEAGUE`), `ModLobby.SetLeagueTeam` (équipe et tenue de la ligue, composition, prêt), puis le
  départ comme en partie classée (`HostStart` chez l'hôte, `GuestCheckStart` chez l'invité). L'ami sort du salon
  avant le départ : on le quitte aussi et on attend de nouveau (sinon le serveur le croirait encore ouvert).
  Après le match : on quitte le salon quand le serveur a donné l'issue (`ModLobby.m_nRepMatch` à 0).
- `ModLobby.BeginMatch` → `OnFriendMatchBegin` : `m_bFriendMatch` (retour à l'écran 26 :
  `SoccerMainMenuModule.ShowLoadingScreen`), `m_bMatchGolden`. `ModLobby.GetHalfSeconds` : durée de la ligue.
- **But en or en ligne** : `ModOnlineLeague.IsGoldenGoal` vaut aussi pour `m_bFriendMatch`. Seul l'hôte le
  décide (`Match.Update`) ; le client reçoit `m_bGoldGoal` dans l'état du match (`_FA_GOLD_GOAL`, déjà là).
- `ModNet._MOD_VERSION` passe à **6** : le serveur ne met dans un même salon que deux jeux de même version.
- `MenuOnline.Tick` quitte un salon de ligue (il ne se joue que depuis l'écran de la ligue, avec les équipes de la
  ligue).
- `CanPlayFriend` : pas de nouveau « jouer » sur le match qui vient d'être compté, le temps que l'état de la ligue
  arrive (sauf match pas compté).

### Test sans souris

```powershell
powershell -ExecutionPolicy Bypass -File modsuild.ps1 -Second
powershell -ExecutionPolicy Bypass -File mods\online-test.ps1 -League -Bot -Half 20 -Port 7778 [-Golden]
```

- Chaque jeu joue tout seul 3 matchs (`MenuOnlineLeague._AUTO_MATCHES`) : contre l'IA, ou contre l'autre jeu
  (« jouer » des deux côtés, `NetBot` dans le match en ligne). Le match entre les deux tombe à une journée tirée au
  hasard : `build\online-test.db` la donne (`League.from_dict`, `f_round` du match entre les deux équipes
  humaines). `LG_SKIP` ne passe pas une journée dont un match contre l'IA est en cours (normal : il est épargné).
- `-Golden` (`NetGolden(1)`) : but en or à égalité dans tout match entre les deux jeux, pour le voir hors playoffs.
- `-Port 7778` quand un vrai serveur tourne sur le 7777 : le script refuse maintenant de démarrer si le port est
  pris (sinon les jeux de test allaient sur le vrai serveur et écrivaient dans sa base).
- Journaux : `match 19 contre invite : attente`, `dans le salon XHLF (hote), partie classee, ligue`,
  `ligue : equipe 0 tenue 0, pret`, `match … (hote) classe (ligue)`, `match classe … : compte`,
  `fin du match contre un ami : on quitte le salon` ; serveur : `salon XHLF pour journée 3 : 0 contre 2`,
  `journée 3 : 0 contre 2 0-0 (entre joueurs)`.

## Tests

```powershell
python server\test_league.py   # logique : même ligue que ModLeague.java, 3000 ligues au hasard, reprise
python server\test_server.py   # serveur par le réseau : 124 points, dont un scénario de ligue complet
```

- `test_league.py` compile `ModLeague.java` + `LeagueCheck.java` avec `tools\jdk8` (sauté sans le JDK) et compare.
  Puis 3000 ligues de 2 à 14 équipes, de 0 à 14 humains, journées fermées au hasard, abandons : toutes vont au
  bout, chaque équipe rencontre les autres une fois, le tableau donne un champion, l'état se relit à l'identique.
- `test_server.py`, « ligue en ligne » : création, entrée par le code, équipe prise, départ, délai dépassé,
  match épargné par le délai, « passer », match entre joueurs par un salon classé (Elo en ligne + ligue),
  abandon, nul en playoffs, fin ; puis « ligue gardée » : serveur relancé, la ligue est là.
