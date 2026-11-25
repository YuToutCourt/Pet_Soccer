# Multijoueur en ligne (chantier 7)

État : **phase 5 faite** (liaison directe, installeur pour les amis, file d'attente par Elo, pause de mi-temps),
à valider en jeu, sans VPS : le serveur reste sur ton PC. Phases 3 et 4 (salons, partie rapide, Elo en ligne) faites,
à valider en jeu. Phase 2 (match complet) faite, sons, ralentis et supers encore à valider en jeu. Phase 1 validée.
Plan complet : six phases, voir « Suite » en bas.

## Principe

La simulation du match vit dans `ChromeEngine.dll` et n'est pas déterministe : elle ne peut ni tourner sur un
serveur, ni être rejouée à l'identique sur deux machines. Donc :

| Morceau | Rôle |
|---|---|
| **Hôte** | Simule le match comme un multi local. L'équipe de droite est pilotée par les touches du client. Diffuse l'état. |
| **Client** | « Marionnette » : aucune logique, aucune physique. Envoie ses touches, applique l'état reçu. |
| **Serveur Python** (`server\server.py`) | Salons à code, file d'attente, comptes et Elo en ligne. Met les deux jeux en liaison directe quand c'est possible, sinon relaie leurs paquets. |

L'API réseau native de `GameObject` (`CreateServer`, `ConnectToServer`…) n'est pas utilisée.

## Serveur et salons (phase 3)

Les deux jeux se présentent au serveur, qui recopie les paquets de l'un vers l'autre : personne n'ouvre de port
chez soi, sauf celui qui héberge le serveur. Pendant un match, les jeux essaient en plus de se parler sans passer
par lui (voir « Liaison directe »).

### Lancer le serveur

```powershell
docker compose up -d postgres                # la base (docker-compose.yml, mot de passe dans .env)
python server\server.py --db postgresql://pet-soccer:MOT_DE_PASSE@127.0.0.1:54320/pet-soccer
python server\server.py --lag 80 --loss 5    # simule une mauvaise connexion (tests)
python server\server.py --no-direct          # tout le match passe par le serveur
python server\test_server.py                 # vérifie le serveur sans le jeu (151 points, base pet_soccer_test)
```

- Python 3 et `psycopg` (`pip install -r server\requirements.txt`). Un seul port UDP, une boucle, aucun thread.
- `--db postgresql://...` (ou la variable `DATABASE_URL`) : base Postgres des comptes, de l'Elo en ligne, des
  ligues et de **tous les matchs en ligne** avec leurs tactiques et statistiques (`server\schema.sql`, tables créées
  au démarrage). Une base injoignable n'arrête pas le serveur : l'erreur est journalisée, la connexion se rouvre.
- Sur un VPS : `docker compose up -d --build` lance la base, le serveur de jeu (UDP 7777) et le site (`web\`, Next.js,
  port 8080 sur 127.0.0.1, à brancher derrière le reverse proxy). Voir « Base et site de stats ».
- `--lost` (4 s), `--claim` (6 s), `--takeover` (3 s) : délais des forfaits et de la reprise, voir « Partie rapide ».
- `--lag` et `--jitter` (ms) retardent chaque paquet relayé, `--loss` (%) en perd. Le retard s'ajoute dans chaque
  sens : `--lag 80` ajoute 160 ms à l'aller-retour. Ils coupent la liaison directe, qui leur échapperait.
- `--queue-range` (100) et `--queue-grow` (10) : écart d'Elo accepté dans la file d'attente, et sa hausse par
  seconde d'attente. `--no-direct` : pas de liaison directe.
- `--code TEST` impose le code des salons (un seul salon à la fois) : sert aux tests automatiques.
- `--timeout` (30 s) : silence après lequel un jeu est oublié. `--stale` (10 s) : silence après lequel l'invité d'un
  salon peut être remplacé (jeu planté puis relancé, voir « Coupures »).
- Adresse du serveur côté jeu : `Data\online.scr`, ligne `Server("127.0.0.1", 7777)`. Même fichier :
  `TeammateLevel(5)`, niveau de l'IA des coéquipiers en ligne, `RankedHalf(4)`, minutes d'une mi-temps en
  partie classée, `HalfBreak(30)`, secondes de pause à la mi-temps, et `Direct(1)`, liaison directe permise.

### Héberger pour tes amis, sans VPS

Le serveur tourne sur ton PC, allumé pendant les parties.

1. Sur ta box : rediriger le port **UDP 7777** vers ce PC (« NAT/PAT » ou « redirection de ports »). Donne au PC
   une adresse locale fixe dans la box, sinon la redirection se perd quand elle change.
2. Pare-feu Windows : au premier lancement de `server.py`, autoriser Python sur les réseaux privés et publics.
3. Ton adresse publique : celle qu'affiche un site « quelle est mon IP ». Elle change de temps en temps chez la
   plupart des opérateurs : un nom gratuit (DuckDNS, No-IP) évite de la redonner à chaque fois.
4. `powershell -ExecutionPolicy Bypass -File mods\package.ps1 -Server ton.adresse` fabrique le zip pour tes amis,
   avec cette adresse dedans (voir « Installeur »).
5. Ton propre jeu garde `Server("127.0.0.1", 7777)` : beaucoup de box ne laissent pas un PC se joindre lui-même
   par l'adresse publique.

Si ton opérateur ne permet pas de redirection (adresse partagée, « CGNAT »), rien de cela ne marche : il faut alors
un serveur ailleurs, ou qu'un ami héberge.

### Déroulement pour le joueur

1. Menu principal, « en ligne ». L'écran dit si le serveur répond.
2. L'un clique « créer » : le salon reçoit un code de 4 lettres. L'autre tape ce code et clique « rejoindre ».
3. Chacun choisit son équipe et sa tenue (« équipe », écrans du match simple) et sa composition (« tactique »).
   L'hôte choisit le stade et la durée.
4. L'invité clique « prêt », l'hôte clique « jouer ». Après le match, les deux reviennent dans le salon.

L'hôte du salon est aussi l'hôte du match : son jeu simule, son équipe commence à gauche.

Partie rapide : au lieu des étapes 2 à 4, chacun clique « partie rapide » et attend. Voir plus bas.

### Dialogue avec le serveur (`ModLobby`, `server\protocol.py`)

Rien n'est fiable ni ordonné. Chaque paquet du jeu redit tout ce que le joueur veut, et le serveur répond à chacun
par l'état du salon : une perte se rattrape au paquet suivant.

| Paquet | Sens | Contenu |
|---|---|---|
| 16 `HELLO` | jeu → serveur | version du dialogue, secret de session (0 au premier bonjour), version du mod, nom du profil, jeton du compte, adresse locale du jeu. Sert aussi à rester connu du serveur, et à revenir d'une autre adresse. |
| 17 `CREATE`, 18 `JOIN` + code, 19 `LEAVE` | jeu → serveur | répétés jusqu'à ce que la réponse le confirme. `LEAVE` sort aussi de la file d'attente. |
| 20 `STATE` | jeu → serveur | ce que le joueur montre à l'autre, 4 fois par seconde (1 fois pendant un match) |
| 21 `QUEUE` | jeu → serveur | partie rapide : « j'attends un adversaire », répété 3 fois par seconde tant que le joueur attend |
| 22 `MATCH` | jeu → serveur | tout match en ligne : numéro du match, phase (en cours, fini, abandon, adversaire perdu), score, « j'envoie des paquets de match ». Une fois par seconde. |
| 25 `MATCH_STATS` | jeu → serveur | jeu de l'hôte, match fini : statistiques des deux équipes (format dans `protocol.read_match_stats`), redites avec `MATCH`. Pas de réponse. |
| 23 `TOP` | jeu → serveur | demande le classement (toutes les 5 s devant l'écran « en ligne », hors salon) |
| 32 `INFO` | serveur → jeu | réponse à chacun : code du salon, rôle, nombre de joueurs, nom de l'autre, refus d'entrée ; puis file d'attente, salon classé, Elo du joueur et de l'autre, joueurs connectés, issue du dernier match classé ; clé du salon et adresses de l'autre joueur (liaison directe) ; écart d'Elo accepté dans la file ; secret de session |
| 33 `PEER` | serveur → jeu | le `STATE` de l'autre joueur, recopié tel quel |
| 34 `RANKS` | serveur → jeu | les 8 premiers du classement (nom, Elo, victoires, nuls, défaites), le rang du joueur, le nombre de classés |
| 1, 2, 3 | les deux sens | paquets du match (`ModNet`), recopiés tels quels |
| 4 `PUNCH` | jeu → jeu | sonde de la liaison directe. Ne passe jamais par le serveur. |

- Tous les paquets jeu → serveur portent l'identifiant du jeu (tiré à l'ouverture de l'écran) et un numéro. `INFO`
  rend ce numéro : après un changement d'avis (créer, rejoindre, quitter), les réponses plus anciennes sont ignorées.
- **Secret de session** (dialogue 6) : au premier `HELLO`, le serveur tire un secret et le donne dans `INFO`. Le jeu
  le met dans tous ses paquets (en-tête de 17 octets ; dans `HELLO`, juste après la version du dialogue, qui reste
  à l'octet 13 pour reconnaître un vieux jeu). Un paquet sans le bon secret, ou venu d'une autre adresse que celle
  du jeu, ne change rien et reçoit « jeu inconnu » : connaître l'identifiant d'un autre jeu ne permet plus de le
  sortir de son salon ni de détourner ses paquets de match. Si l'adresse du jeu change (box, 4G), il reçoit
  « jeu inconnu », redit `HELLO` avec son secret, garde sa place et reçoit un nouveau secret à sa nouvelle adresse.
  Un `HELLO` sans le bon secret pour un jeu connu n'a de réponse que depuis l'adresse de ce jeu (réponse perdue).
  Le secret passe en clair : il protège des inconnus, pas de quelqu'un qui écoute le même réseau.
- **Pas d'amplification** : le jeu complète ses paquets par des zéros jusqu'à 96 octets (`protocol.LOBBY_MIN_SIZE`,
  `ModLobby._MIN_SIZE`) ; le serveur ignore les plus courts. À une adresse qu'il n'a pas vérifiée (sans le secret),
  il ne répond jamais plus gros que la demande (`Server.reply`). Classement et ligue ne partent qu'à l'adresse du jeu.
- **Limites** (`server.py`) : `--ip-rate` paquets de salon par seconde et par adresse IP (60, le double d'un coup),
  `--ip-hello` nouveaux jeux et lectures de compte dans la base par seconde et par IP (2, vingt d'un coup),
  `--ip-sessions` jeux connus depuis une même IP (16), `--max-sessions` en tout (5000). Au-delà des deux derniers,
  `INFO` dit « serveur plein » (état 3) et le jeu réessaie toutes les 3 s. Les paquets écartés sont comptés dans le
  journal toutes les 10 s. Les tests (`test_server.py`) lèvent ces limites, sauf ceux qui les essaient.
- `STATE` : prêt, équipe, tenue, composition (une ligne au format de `tactics.dat`) ; et pour l'hôte : stade, durée,
  numéro du match en cours (0 = il est dans le salon), équipes et tenues retenues pour ce match.
- **Départ** : l'hôte tire un numéro de match, l'envoie, puis charge le stade. L'invité qui voit un numéro de match
  qu'il n'a pas encore joué charge le même match. Les réglages viennent tous de l'hôte.
- **Version** : `ModNet._MOD_VERSION` (9 depuis le secret de session ; 8 depuis les statistiques en ligne ; 7 depuis la caméra propre au client ; 6 depuis le match entre amis de la ligue en ligne). Le serveur refuse l'entrée dans un salon dont l'hôte a une autre version,
  et la file d'attente ne réunit que des joueurs de même version. À augmenter dès que les paquets ou les règles du
  match changent.
- **Version du dialogue** : `ModLobby._LOBBY_PROTO` et `protocol.LOBBY_PROTO` (3 depuis la phase 5, 4 depuis la
  ligue en ligne, `notes/online-league.md`, 5 depuis `MATCH_STATS`, 6 depuis le secret de session). Le serveur
  accepte les versions de `protocol.LOBBY_PROTOS` (6 seulement : un jeu sans secret de session laisserait la
  faille ouverte) ; un jeu d'une autre version voit « Le serveur n'a pas la même version ».
- Même équipe et même tenue des deux côtés : l'invité prend la tenue suivante.
- Le serveur ne lit pas les paquets du match. Il relaie `STATE` tel quel et en garde une copie lue
  (`protocol.read_state`) : équipes, tenues, stade et tactique du match qu'il enregistre. `ModLobby.SendState` et
  `read_state` doivent donc rester d'accord.
- **Mi-temps** : l'invité qui revoit sa composition la renvoie dans son `STATE` ; l'hôte, qui voit qu'elle a
  changé, l'applique à l'équipe de l'invité (`ModLobby.ReadPeer` → `ModNet.HostRemoteTactics`).

## Partie rapide, Elo en ligne, classement (phase 4)

### Pour le joueur

1. Écran « en ligne », bouton « partie rapide » : le jeu entre dans la file d'attente (« annuler » pour en sortir).
2. Dès qu'un autre joueur d'Elo assez proche attend aussi, le serveur les met dans un salon **classé**. L'hôte
   est tiré au sort.
3. Chacun choisit son équipe et sa tactique puis clique « prêt ». Quand les deux sont prêts, le match part tout seul.
   Stade : celui de l'équipe de l'hôte. Durée : `RankedHalf` de `online.scr` (2 x 4 min).
4. À la fin, l'écran de fin de match montre le nouvel Elo en ligne et sa variation. Retour dans le salon : revanche
   (comptée aussi) ou « quitter ».

Hors salon, le cadre de droite montre le classement (8 premiers) et la ligne du joueur : rang, Elo, place.
Les rangs (Bois, Bronze…) et leurs couleurs sont ceux de l'Elo contre l'IA (`elo.scr`), mais les deux Elo sont
séparés. Un salon privé à code ne compte jamais pour l'Elo.

### File d'attente par Elo (`rooms.Lobby.pair`)

- Chaque joueur accepte un écart d'Elo de 100 en entrant dans la file, plus 10 par seconde d'attente, **sans
  limite** (`--queue-range`, `--queue-grow`). Deux joueurs sont réunis quand leur écart tient dans ce que l'un des
  deux accepte : celui qui attend depuis longtemps finit par jouer contre n'importe qui, même si l'autre vient
  d'arriver. Exemples : 250 d'écart, 15 s d'attente ; 800 d'écart, 70 s.
- Le serveur part du joueur qui attend depuis le plus longtemps et lui donne l'adversaire le plus proche en Elo.
- Sortir de la file (« annuler ») puis y revenir remet l'attente à zéro.
- L'écran montre la fourchette en cours : « Adversaire cherché : Elo 900 à 1100 », puis « tous les Elo ».

### Compte (`online.dat`, `server\store.py`)

- Au premier passage sur l'écran « en ligne », le jeu tire un jeton de 32 caractères et l'écrit dans `online.dat`
  (dossier du jeu), une ligne `profil;jeton` par profil. Pas de mot de passe : le serveur reconnaît le joueur à ce
  jeton, et n'en garde que l'empreinte.
- Perdre `online.dat`, c'est perdre son compte (un nouveau est créé à 1000). Le copier, c'est prêter son compte :
  ne pas le donner avec le jeu à un ami.
- Le nom affiché est celui du profil, pris au dernier passage. Deux joueurs peuvent porter le même nom.

### Elo

Formule de `ModElo` (contre l'IA), recopiée dans `store.elo_delta` : 1000 au départ, K = 48 pendant les 10 premiers
matchs puis 32, écart de buts x1 / x1,5 / (11 + écart) / 8. Chacun gagne ou perd selon l'Elo de l'autre.
Seuls les joueurs avec au moins un match classé sont dans le classement.

### Quand un match compte (`rooms.py`, `ModLobby.ReportFrame`)

Le serveur ne voit pas le match : chaque jeu lui annonce où il en est (`MATCH`). Il se sert aussi de ce qu'il voit
lui-même : qui se tait, qui quitte le salon. Un joueur « joue » quand le serveur relaie un de ses paquets de match,
ou, en liaison directe, quand son jeu dit qu'il en envoie (drapeau de `MATCH`).

| Cas | Issue |
|---|---|
| Les deux jeux annoncent la fin avec le même score | Compté |
| Les deux annoncent la fin avec des scores différents | Pas compté |
| Un joueur quitte le match par le menu (« abandon ») | Forfait pour lui |
| Le serveur voit un joueur sortir du salon pendant le match | Forfait pour lui |
| Le jeu d'un joueur se tait 30 s (fermé, planté, connexion coupée) | Forfait pour lui |
| Un joueur quitte en disant « l'autre ne répond plus » | Forfait pour l'autre si le serveur n'a plus reçu de paquet de match de lui depuis 4 s (`--lost`) ; sinon pas compté après 6 s (`--claim`) |
| Le jeu de l'hôte est relancé pendant le match | Forfait pour l'hôte : le match qu'il simulait est perdu |
| Un joueur part avant que les deux jeux aient échangé des paquets de match | Pas compté |
| Un joueur quitte le salon classé avant le match (l'autre ne se met pas prêt, par exemple) | Rien : aucun match n'existe, aucun Elo ne bouge. Le salon ferme, chacun repasse par « partie rapide ». |
| Les deux jeux se taisent en même temps | Pas compté |

- **Salon privé** : même suivi (`MATCH` envoyé pour tout match en ligne depuis la version 8), le match est enregistré
  dans la base (`kind = 'private'`) sans toucher à l'Elo. Le serveur répond « pas compté » (`RESULT_VOID`) : c'est
  ce qui arrête les redites du jeu, qui n'affiche rien puisque le match n'était pas classé.
- Forfait = défaite d'au moins un but d'écart. Si le joueur forfait était mené de plus (0-3), c'est cet écart qui
  compte : quitter un match mal engagé ne coûte pas moins cher que le finir.
- Un match n'est compté qu'une fois (numéro du match), même annoncé dix fois.
- « Recommencer » est grisé en partie classée.

### Jeu relancé : retour dans le salon

Le compte sert aussi à cela. Un jeu qui se présente avec le jeton d'un jeu muet depuis 3 s (`--takeover`) reprend sa
place : il se retrouve dans son salon sans rien taper, et l'invité retombe dans le match en cours. Vaut pour les
salons privés et classés. Un invité relancé à temps (avant les 30 s de silence) évite donc le forfait.

### Coupures

| Cas | Effet |
|---|---|
| Un joueur quitte le salon | L'autre le voit partir. Si c'est l'hôte, le salon ferme (« Le salon a été fermé »). |
| Jeu de l'invité fermé pendant un match | L'hôte voit « Adversaire déconnecté » après 5 s. L'invité relance son jeu et clique « en ligne » : il retrouve son salon et retombe dans le match en cours (voir « Jeu relancé »). |
| Jeu de l'hôte fermé | L'invité voit « Connexion perdue » en match ; le salon ferme après 30 s. |
| Un joueur sort d'un salon classé | Le salon ferme pour les deux : chacun repasse par « partie rapide ». |
| L'invité quitte le match par le menu | Il revient dans le salon, qui affiche « L'hôte est encore en match ». |
| Serveur arrêté | « Le serveur ne répond pas » après 6 s. À son retour, les jeux se représentent ; les salons sont perdus. |

## Liaison directe (phase 5)

Par le relais, chaque paquet fait jeu → serveur → jeu. En liaison directe il va d'un jeu à l'autre, et le serveur
ne garde que le salon et l'Elo. Il reste le secours : rien n'est à régler chez les joueurs.

### Comment elle se fait (`ModNet.PunchFrame`, `rooms.Lobby.link`)

1. Le serveur voit de quelle adresse et de quel port chaque jeu lui parle. Il donne à chacun l'adresse de l'autre,
   et une clé tirée au hasard pour le salon.
2. Pendant le match, chaque jeu envoie des **sondes** (`PUNCH`, 4 par seconde) à cette adresse, par le canal qui
   parle déjà au serveur. Sa box note « ce jeu parle à cette adresse » et laisse alors passer ce qui en revient :
   les sondes de l'autre finissent par arriver.
3. Une sonde dit aussi « je reçois les tiennes ». Quand c'est vrai des deux côtés, le chemin marche dans les deux
   sens : le client envoie ses touches en direct, et l'hôte répond toujours par où les touches arrivent.
4. Les sondes continuent une fois par seconde, pour garder le passage ouvert.

- **Retour au relais** : si le client ne reçoit plus d'état depuis 1,5 s en direct, ou plus de sonde depuis 3 s,
  il renvoie ses touches par le serveur (10 s avant un nouvel essai en direct). Le jeu se fige au plus 1,5 s.
- **Même box** : à deux joueurs qui sortent par la même adresse, le serveur donne aussi l'adresse locale de l'autre
  (annoncée dans `HELLO`), car beaucoup de box ne savent pas se joindre elles-mêmes.
- **Pas d'adresse donnée** quand les deux joueurs n'arrivent pas par le même chemin : l'un sur le PC du serveur ou
  son réseau local, l'autre par Internet. L'adresse vue par le serveur ne mènerait nulle part, et le relais ne
  coûte alors rien, puisque le serveur est déjà chez l'un des deux. **C'est ton cas quand tu joues contre un ami
  depuis le PC qui héberge** : tu verras « relais ». La liaison directe sert aux matchs entre deux de tes amis.
- **Elle échoue** avec certaines box et la plupart des connexions 4G/5G, qui changent de port selon le
  destinataire : le match reste alors sur le relais, sans rien dire au joueur.
- **Sécurité** : une sonde n'est écoutée que si elle porte la clé du salon et le numéro du match ; un paquet de
  match venu d'ailleurs que du serveur doit venir de l'adresse des sondes ou porter la session en cours.
- **Partie classée** : en direct, le serveur ne voit plus passer les paquets du match. Chaque jeu lui dit donc
  « j'envoie des paquets de match » dans son `MATCH` : c'est ce qui prouve que le match a commencé et qu'un joueur
  est encore là.
- Journal : `[mod] net liaison directe avec …`, `relais par le serveur`, et `(direct)` ou `(relais)` dans la ligne
  `net stat` du mode debug. Rien à l'écran.
- Pour la couper : `Direct(0)` dans `online.scr` (un seul des deux joueurs suffit), ou `--no-direct` au serveur.

## Pause de mi-temps (phase 5)

À la mi-temps d'un match en ligne, classé ou non, le menu reste ouvert 30 s (`HalfBreak` de `online.scr`, celui
de l'hôte) : le temps de revoir sa tactique.

- Le panneau en bas à droite montre « Reprise dans 23 s » et, selon le cas, « Ton adversaire est prêt » ou « En
  attente de l'adversaire ».
- « Continuer » (ou échap) dit qu'on est prêt ; le bouton se grise. Quand **les deux** l'ont fait, le match
  reprend tout de suite. Sinon il reprend à la fin du temps, sans rien demander.
- « Tactique » marche aussi chez l'invité, à la mi-temps seulement : sa composition part chez l'hôte, qui
  l'applique. Un écran Tactique encore ouvert à la reprise garde ce qui y est réglé, comme un clic sur « ok ».
- Si l'adversaire est déconnecté, « continuer » de l'hôte suffit.
- `HalfBreak(0)` : pas de limite, le premier « continuer » ferme le menu, comme avant.
- Le temps du match est arrêté pendant ce menu : le compte à rebours suit l'horloge du PC de l'hôte
  (`ModNet.HostHalfBreak`), envoyé au client dans l'état (secondes restantes, qui est prêt).
- En cours de match, échap ne met plus en pause : voir « Échap en cours de match ».

### Échap en cours de match

- Échap ouvre le menu (continuer, quitter, et recommencer chez l'hôte) **chez ce joueur seulement** : le match
  continue pour les deux (`ModNet.OnEscape`, `OpenLocalMenu`). Ni horloge arrêtée ni caméra figée, rien
  d'envoyé à l'autre jeu (`HostMenuState` vaut 0 pendant ce menu).
- Pendant ce menu, les touches appuyées ne jouent pas (`TeamController.OnInputKey` → `ModNet.IsKeyBlocked`) ;
  les relâchées passent, pour qu'une direction tenue au moment d'échap ne reste pas enfoncée.
- Échap ou « continuer » le referment. Un vrai menu (mi-temps, fin, attente de l'adversaire, menu de l'hôte
  chez le client) le remplace (`ModNet.OnShowMenu`, `ClientMenu`).
- Pendant la pause de mi-temps, échap vaut toujours « je suis prêt », des deux côtés.

## Spectateurs (mod v10, dialogue 7)

On peut regarder les **parties classées** et les **matchs de ligue** entre joueurs, en direct. Les salons privés
entre amis ne se regardent pas (ni dans la liste, ni avec leur code).

### Pour le joueur

Écran « en ligne », hors salon : « regarder » (à la place de la ligne « durée ») montre à droite les matchs en
cours, relus toutes les 3 s : joueurs, score, Elo moyen ou « ligue ». La ligne « match < 1 / 3 > » choisit, le
bouton du bas « regarder » charge le match. Échap ouvre un menu local (« continuer », « quitter ») ; les menus de
l'hôte (mi-temps, fin) s'affichent sans bouton actif. À la fin (8 s après l'écran de fin, ou 3 s sans nouvelles
de l'hôte), retour seul à la liste. Rien ne compte chez le spectateur (Elo, historique, succès).
L'hôte voit « Partie classée, 2 spectateurs » dans son salon.

### Comment ça marche

```
hôte ──(direct ou relais)──> invité                     (inchangé)
hôte ──_PKT_SPEC_SNAP (5), 30/s──> serveur ──> chaque spectateur
```

- L'hôte n'envoie qu'**une** copie en plus, et seulement quand `PKT_INFO` lui annonce des spectateurs
  (`ModLobby.m_nSpectators`) : son débit montant ne dépend pas de leur nombre. Le serveur paie ~16 Ko/s par
  spectateur, d'où `--max-spectators` (8 par défaut).
- Copie = même état que pour l'invité, puis l'**état durable** et les **événements des 500 dernières ms**
  (`HostPutSpecEvents`). Un spectateur ne confirme rien à l'hôte : chaque événement passe donc dans ~15 paquets, et
  le spectateur ignore ceux qu'il a déjà vus (même numérotation que l'invité). Les plus récents passent d'abord
  s'il n'y a pas la place.
- **État durable** (`HostResync` : côtés, animation en cours de chaque joueur, lettres des supers) : le serveur
  fait tourner un compteur d'arrivées (`PKT_INFO`) quand un spectateur arrive **ou dit que son stade est chargé**
  (`PKT_SPECTATE` porte le numéro du match chargé). L'hôte voit le compteur tourner, capture `HostResync`
  (`m_vCapture`) et le met 1,5 s dans un paquet sur trois de la copie, avec un numéro : le spectateur ne
  l'applique qu'une fois. L'invité ne le reçoit pas (ses animations ne repartent pas de zéro).
- Le spectateur reçoit aussi le `STATE` de l'hôte (`PKT_PEER`) : il charge le match comme l'invité
  (`ModLobby.SpectatorCheckStart` → `ModNet.StartSpectating`). C'est un client (`m_nRole` client,
  `m_bSpectator`) qui n'envoie ni touches ni demandes, n'a pas de liaison directe, et accepte l'état quel que
  soit le numéro de session.

| Paquet | Sens | Contenu |
|---|---|---|
| 26 `LIVE` | jeu → serveur | demande la liste des matchs en cours |
| 36 `LIVE_LIST` | serveur → jeu | 8 matchs au plus : code, noms, Elo, équipes, score, spectateurs, ligue |
| 27 `SPECTATE` | jeu → serveur | code du salon, match dont le stade est chargé (0 : aucun). Redit 1 fois par seconde ; `LEAVE` arrête |
| 5 `SPEC_SNAP` | hôte → serveur → spectateurs | copie de l'état (voir plus haut) ; le serveur n'accepte que celle de l'hôte du salon |
| `INFO` (fin) | serveur → jeu | spectateurs du salon, compteur d'arrivées, nom de l'invité pour un spectateur (rôle 3) |

- Test sans souris : `NetAuto(7)` regarde le premier match de la liste (`NetMatches` fois).
  `mods\build.ps1 -Third` crée `game_dev_c` ; `mods\online-test.ps1 -Ranked -Bot -Half 30 -Spectate
  [-SpectateAt 40]`. Supprimer `game_dev_c\online.dat` s'il vient d'une copie de `game_dev` (même compte).

## Installeur pour les amis (phase 5)

On donne le mod, pas le jeu : chaque ami a déjà Pet Soccer installé.

```powershell
powershell -ExecutionPolicy Bypass -File mods\package.ps1 -Server ton.adresse   # fabrique build\PetSoccerMod.zip
python mods\installer\test_installer.py                                        # essai sur une fausse copie du jeu
```

- `package.ps1` relance le build, puis met dans `build\PetSoccerMod.zip` (environ 5 Mo) : les classes compilées,
  `mods\data` sans `debugconf.scr`, et `mods\installer` (`Installer.bat`, `Desinstaller.bat`, `install.ps1`,
  `LISEZMOI.txt`). `-Server` écrit l'adresse du serveur dans le `online.scr` du paquet ; sans lui, l'installeur la
  demande à chaque ami.
- Rien de personnel dans le paquet : ni sauvegardes, ni `online.dat`, ni journaux. Le script refuse de finir s'il
  en trouve.
- Chez l'ami, `Installer.bat` : trouve le dossier du jeu (registre, dossiers habituels, sinon il le demande),
  demande avant d'y toucher, garde les originaux dans `<jeu>\mod_backup`, ajoute les classes à `code.pak` en
  repartant de l'original, refait `code_inf.pak`, copie les données, écrit l'adresse du serveur. Jeu dans
  « Program Files » : il redemande les droits d'administrateur.
- Mise à jour : relancer `Installer.bat` du nouveau zip. Les sauvegardes et l'adresse du serveur sont gardées.
  Depuis la version 3.2, le lanceur le fait tout seul (voir « Mises à jour automatiques »).
- `Desinstaller.bat` remet le dossier du jeu exactement comme avant (vérifié par le test), sauvegardes à part.
- Il prévient si le `code.pak` de l'ami n'est pas celui de ta version du jeu (autre édition) : le mod est compilé
  contre celle-ci.
- **Sur ton PC, n'essaie pas `Installer.bat` sur ton installation d'origine** : il la trouve par le registre.
  Réponds « n » et choisis une copie, ou passe `-Game`.
- Tous les joueurs doivent avoir la même version du mod (`_MOD_VERSION`) : redonne le zip après chaque changement
  des règles ou des paquets.

### Mises à jour automatiques (version 3.2)

Deux numéros différents :
- `ModVersion.RELEASE` (ex. `3.2`) : la version du mod, affichée au menu principal sous le logo (`ID_VERSION` dans
  `Main*.scr`), écrite `release=` dans le `mod.txt` du paquet. C'est aussi le tag de la release GitHub.
  **À augmenter à chaque nouveau paquet**, sinon les lanceurs ne voient rien.
- `ModNet._MOD_VERSION` (ex. `7`) : la version du jeu en ligne. Elle ne change que si les paquets réseau ou les
  règles du match changent.

Publier une version : augmenter `RELEASE`, puis

```powershell
powershell -ExecutionPolicy Bypass -File mods\package.ps1 -Server ton.adresse -Publish
```

`-Publish` (outil `gh`) envoie le paquet sous le nom `PetSoccerMod-update.zip` dans la release GitHub dont le tag
est `RELEASE` (créée si elle n'existe pas, sinon le fichier est remplacé). Le gros zip avec l'ISO peut rester à côté
dans la même release.

Chez les joueurs, l'installation met en place `<jeu>\mod_maj\update.ps1` (avec le `mod.txt` de la version
installée), `<jeu>\Jouer.bat` et le raccourci « Pet Soccer (mod) » sur le bureau (installation normale seulement,
pas en `-Quiet`). À chaque lancement, `update.ps1` lit
`https://api.github.com/repos/YuToutCourt/Pet_Soccer/releases/latest` (les brouillons et les préversions sont
ignorés) ; si le tag est plus grand que la version installée et que la release contient `PetSoccerMod-update.zip`,
il le télécharge, vérifie son SHA-256 (champ `digest` de GitHub), lance `install.ps1 -Game … -Quiet` (droits
d'administrateur demandés si le jeu est dans Program Files), puis lance le jeu. En cas d'échec (pas d'Internet,
fichier abîmé), le jeu se lance avec la version déjà installée. `Desinstaller.bat` retire aussi le lanceur et le
raccourci. Testé par `test_installer.py` avec une fausse release en `file:///`.

Les amis qui ont une version d'avant 3.2 doivent installer la 3.2 une dernière fois à la main.

## Base et site de stats

Tout match en ligne (partie classée, ligue entre amis, salon privé) est enregistré par `Lobby.settle` →
`store.Store.record_match` dans Postgres (`server\schema.sql`) :

| Table | Contenu |
|---|---|
| `players`, `player_names` | comptes (empreinte du jeton), Elo, bilan classé ; pseudos portés |
| `matches` | type (`ranked`, `private`, `league`), classé ou non, issue (`played`, `forfeit` + côté), score, stade, durée de mi-temps, but en or, durée réelle |
| `match_players` | un côté (0 = hôte) : compte, nom, équipe, tenue, résultat W/D/L, Elo avant/après, **tactique** (schéma, 6 rôles, placements des 4 situations, ligne brute), stats d'équipe, heatmaps |
| `match_footballers` | les 7 joueurs : place dans `Team<Animal>Players` (0 = gardien), place et rôle dans la tactique, buts, csc, tirs, passes, récupérations, distance, position moyenne |
| `match_events` | fil du match : buts, csc, supers (minute, type), tirs (minute, tireur, position, issue) |
| `leagues`, `server_status` | ligues en ligne ; état du serveur (joueurs connectés, en file, matchs en cours), réécrit toutes les 10 s |

- Les équipes, le stade et les tactiques viennent du dernier `STATE` de chaque jeu portant le numéro du match (une
  tactique revue à la mi-temps compte donc).
- Les statistiques viennent du **jeu de l'hôte**, qui simule (`ModStats.PutMatchStats`, `ModLobby.TakeStats`) :
  `MATCH_STATS` est pris au coup de sifflet final ou quand on quitte, puis redit avec `MATCH`. Celles de l'invité
  sont ignorées. Arrivées avant la fin du match, elles attendent (`Lobby.pending_stats`) ; juste après, elles
  sont ajoutées au match déjà enregistré (`Lobby.recorded`). Un hôte dont le jeu plante : match sans statistiques
  (`has_stats = false`).
- Positions : repère de l'écran Tactique (profondeur 0 = son but, latéral 0 = haut de l'écran), voir `notes/stats.md`.
- Site : `web\` (Next.js 16, connexion en lecture seule, image Docker à part) : accueil (présentation du mod, téléchargement de
  la dernière Release GitHub), classement, matchs, profil (courbe d'Elo, forme,
  équipes et schémas favoris, moyennes), page d'un match (frise, stats comparées, carte tactique prévu / réel,
  heatmaps, tirs, joueurs), méta (équipes, schémas, rôles, buteurs, équipe contre équipe).

## Ce qui est coupé en ligne

- Triches du menu Mods (vitesse, puissance de tir, supers infinis, super imposé) : sans effet pendant un match en
  ligne, direct ou en salon (`ModOptions`).
- Codes clavier (`L…`) pendant un match en ligne, sauf `LF` (FPS). Touche F10 du profil tricheur.
- Touches de debug du match dans un salon.
- Niveau de l'IA des coéquipiers : fixe (`TeammateLevel`), il ne dépend plus du profil de l'hôte.

## Réseau

- UDP par `java.nio.channels.DatagramChannel` non bloquant (le JRE 1.4.0 du jeu a `nio.dll`), lu à chaque frame
  depuis `SoccerGame.OnFrame` → `ModNet.OnFrame`. Aucun thread.
- **Piège JRE 1.4.0** : `receive()` renvoie une adresse même quand rien n'est arrivé. Seul `position() == 0`
  dit que le tampon est vide.
- **Piège Windows** : lire une socket jamais liée lève « Invalid argument ». Le client se lie au port 0.
- **Piège Windows** : quand un port à qui on a écrit ne répond pas (autre jeu fermé, sonde de la liaison directe
  partie vers une adresse sans personne), la lecture suivante lève « Connection reset by peer ». `Receive`
  l'ignore (une ligne au journal) et lit les paquets suivants ; c'est le délai de 5 s sans paquets qui décide de
  la coupure.

### Paquets (`ModNet`, en-tête `"PSN1"` + type)

| Sens | Contenu |
|---|---|
| Client → hôte, 30/s et à chaque touche | session, dernier événement reçu, les 15 derniers changements de touches numérotés |
| Hôte → client, 30/s | état du match (environ 530 octets) + événements pas encore confirmés |
| Les deux sens | « au revoir » quand un joueur quitte le match |

- **État** : horloge de l'hôte, état du match, mi-temps, score, chrono, ballon (position, vitesse, porteur), cible
  de caméra, joueur actif et marqueur de chaque équipe, caméra de l'hôte, 14 joueurs + arbitre (position, direction).
- **Suite de l'état** (`HostPutExtras`) : HUD visible, ralenti en cours, menu ouvert (pause, mi-temps, fin), fondu
  au noir, vitesse du temps, durée d'une mi-temps, réglage « supers » de l'hôte, viseur des coups de pied arrêtés,
  repère de destination du ballon, murs, leurre du tir double, équipes gelées, sons de supers en cours, pause de
  mi-temps (secondes restantes, qui a cliqué « continuer »).
- **Événements** (fiables, ordonnés). L'hôte renvoie tout ce qui n'est pas confirmé dans chaque paquet d'état ; le
  client confirme dans ses paquets de touches.

| Événement | Crochet chez l'hôte |
|---|---|
| Animation | `FootBaller.PlayAnim` |
| Effet | `FootBaller.ExecuteFX`, `Ball.ExecuteFX`, leurre |
| Son | `Ball.PlaySound`, `FootBaller.PlaySound`, `Individual.StartSound` (cris), bip de fin de mi-temps, gel |
| HUD | méthodes de `PetSoccerHud` (lettres et icône des supers, bandeaux « But », « Out », « Corner », barre de score) |
| Commentateur | `Commentator.Say` et `StopTalking` |
| Changement de côté | `AIMatch.ChangeTeamSides` |
| Public | `Match.PlayOnGoalReaction` |
| Météo | `ScriptedEvent.Activate`, `Match.ClearWeatherEvents` |
| Statistiques du menu | `MenuInGame.ShowInfo` |

- **Touches** : 8 actions (passe, tir, feinte/tête, super, 4 directions) et 3 demandes (espace, échap,
  « continuer »). Répétées dans chaque paquet, donc une perte ne fait rien rater.
- **Session** : nombre tiré par le client. Quand il change, l'hôte repart de zéro et renvoie ce qui dure
  (côtés, animation en cours de chaque joueur, lettres des supers) : client arrivé ou revenu en cours de match.
- **Index d'un joueur** : équipe d'origine × 16 + numéro. Il ne change pas à la mi-temps, quand les équipes
  changent de côté.

## Côté hôte

- `Test1.OnCreate` : en ligne, `CoreLogic.m_bMultiplayer = true`.
- `Test1.InitTeams` : la manette de l'équipe de droite passe en `SetRemote()` (`TeamController`) : sourde au
  clavier local, elle reçoit les touches par `Inject()`, qui appelle `InputAction.Translate` comme le clavier.
  Passe, tir, feinte, tête, super et visée passent donc par le code d'origine.
- Client perdu (5 s sans paquet, ou « au revoir ») : toutes ses touches sont relâchées.
- Sans adversaire, l'hôte ouvre le menu du match avec « En attente de l'adversaire » (ou « Adversaire
  déconnecté »). Le menu se ferme seul quand le client arrive. L'hôte peut aussi choisir « continuer ».
- Espace et « continuer » du client sont joués par le module de l'hôte (`ModNet.HostRequest`), échap seulement
  pendant la pause de mi-temps (« je suis prêt »).

## Côté client

- `SoccerModule.OnFrame` n'appelle plus `m_cMatch.Update()` : `ModNet.ClientMatchFrame` applique l'état.
- Objets figés comme dans `Recorder.Replay()` : `ClearFlag(1)`, `ClearFlag(32)`, `SetCollFlags(8192)`, ballon
  sans physique.
- Interpolation entre deux états, avec 100 ms de retard (`_INTERP_DELAY`).
- `FootBaller.PlayAnim` ne joue que les animations venues de l'hôte.
- Caméra : en jeu, chaque joueur a la sienne (mode F1-F4, zoom, souris) : elle suit la cible envoyée par l'hôte
  (ballon, joueur…). L'hôte marque son état « caméra libre » (bit 2 de l'octet du ralenti) quand la sienne suit
  sa cible sans ralenti, fin de mi-temps, pause ni super tir. Sinon le client copie la caméra de l'hôte
  (`SoccerCamera.OnPostUpdate` → `ModNet.ClientCamera`), pour garder les mêmes plans.
  Le début de match lance aussi chez le client `SetEndHalfCamera` (plan d'intro), que seul
  `AIMatch.ActionAfterStabilization` efface, et le client ne la joue pas : `ModNet` appelle donc
  `ClearEndHalfCamera` quand la caméra repasse en locale (sinon plan qui flotte tout seul, retard de 4 s).
- Touches : la manette de l'équipe de gauche (réglages du joueur 1) capte les touches et les envoie au lieu de
  les jouer (`ModNet.OnLocalKey`). Clavier seulement pour l'instant. Les touches directes vers un joueur (2 à 7,
  `ModSwitch`) partent avant ce crochet, en actions `_ACT_SEL + k` (11..16) jouées par `ModNet.HostApplyKey`.
- Le client ne joue que les effets, sons, appels du HUD et commentaires venus de l'hôte : les crochets renvoient
  faux hors de `m_bApplying`.
- Menu du match : il suit celui de l'hôte (mi-temps, fin). Échap en cours de match ouvre un menu chez le client
  seulement (voir « Échap en cours de match »). Dans un menu de l'hôte, « continuer » et échap sont des demandes
  à l'hôte. « Recommencer » est réservé à l'hôte. « Tactique » marche à la mi-temps dans un match de salon (voir
  « Pause de mi-temps »). Les statistiques affichées sont celles de l'hôte.
- Ralentis : ce sont ceux de l'hôte, reçus comme le reste (positions, animations, caméra, vitesse du temps).
- Hôte perdu (5 s sans paquet, ou « au revoir ») : le menu s'ouvre avec « Connexion perdue », seul « quitter »
  répond.
- Quitter le match prévient l'autre joueur, puis le jeu redevient hors ligne jusqu'à sa fermeture.

## Ce qui ne compte pas en ligne

Un match en ligne est un multi local pour le reste du jeu : pas d'Elo contre l'IA, pas d'historique, pas de
succès, ni chez l'hôte ni chez le client. Une partie classée ne change que l'Elo en ligne, gardé par le serveur.

## Lancer un match de test

Le plus simple, à deux fenêtres sur ce PC :

```powershell
powershell -ExecutionPolicy Bypass -File mods\build.ps1 -Second      # une fois, puis après chaque changement
powershell -ExecutionPolicy Bypass -File mods\online-test.ps1 -Menus # serveur + deux jeux, vrais menus (phase 3)
powershell -ExecutionPolicy Bypass -File mods\online-test.ps1 -Server -Bot -Half 60  # salon et match sans souris
powershell -ExecutionPolicy Bypass -File mods\online-test.ps1 -Ranked -Bot -Half 60  # partie rapide classée sans souris
powershell -ExecutionPolicy Bypass -File mods\online-test.ps1        # en direct, sans serveur ni menus (phase 1)
```

| Option | Effet |
|---|---|
| `-Menus` | Lance le serveur et les deux jeux avec leurs menus. À toi de cliquer « en ligne », « créer », « rejoindre ». |
| `-Server` | Lance le serveur ; `game_dev` crée le salon `TEST`, `game_dev_b` le rejoint, le match démarre, et chaque jeu revient au salon 5 s après la fin. |
| `-Ranked` | Lance le serveur ; les deux jeux entrent dans la file d'attente, se mettent prêts, jouent un match classé et reviennent au salon. L'hôte est tiré au sort : `-Bot`, `-HostBot` et `-Half` valent pour les deux jeux. |
| `-QuitAt 15` | Avec `-Ranked` : `game_dev_b` quitte le match au bout de 15 s (test du forfait). |
| (rien) | Les deux jeux se parlent en direct, le match démarre tout de suite (équipes et stade fixes). |
| `-Bot` | Le client joue tout seul. |
| `-HostBot` | L'hôte fait une passe toutes les 2 s. À ajouter quand personne n'est au clavier de l'hôte : sinon une touche ou un coup franc de son équipe attend qu'on appuie, horloge arrêtée. |
| `-Half 60` | Mi-temps de 60 s. |
| `-Lag 80`, `-Loss 5` | Avec `-Server` ou `-Menus` : mauvaise connexion simulée par le serveur. |
| `-Matches 2` | Avec `-Server` ou `-Ranked` : nombre de matchs enchaînés. |
| `-NoDirect` | Pas de liaison directe : tout le match passe par le serveur. `-Lag` et `-Loss` la coupent aussi. |
| `-CutDirect` | La liaison directe casse 20 s après le début du match : les jeux doivent revenir au relais. |
| `-NoSkip` | Les jeux automatiques ne cliquent pas « continuer » à la mi-temps : la pause va au bout de ses 30 s. |
| `-TeamA '7,0'`, `-TeamB '12,1'` | Avec `-Server` ou `-Ranked` : équipe et tenue de `game_dev` et de `game_dev_b`. |

Le clavier va à la fenêtre active : celle du client joue l'équipe de droite, celle de l'hôte l'équipe de gauche.
À la fermeture des deux jeux, le script remet `debugconf.scr` et les réglages vidéo de `game_dev`, et arrête le
serveur (journal : `build\online-server.log`).

Les comptes, l'Elo et les matchs de ces tests vont dans la base Postgres `pet_soccer_test` (`server\testdb.py`),
jamais dans celle du vrai serveur. `-ResetDb` la vide avant le test ; `server\test_server.py` la vide aussi. `game_dev_b` s'y appelle « invite » (`NetName`) : les deux dossiers
ont sinon le même nom de profil.

À la main, dans `debugconf.scr`, déclarations **avant** les valeurs :

```
!MenuOn(i)
!FirstLevel(i)
!NetHost(i)
!NetJoin(s,i)
!NetBot(i)
!NetHalf(i)
!NetServer(s,i)
!NetAuto(i)
!NetCode(s)
!NetTeam(i,i)
!NetMatches(i)
!NetName(s)
!NetQuit(i)
!NetDirect(i)
!NetNoSkip(i)
```

| Jeu | Lignes |
|---|---|
| Hôte, en direct | `MenuOn(0)` `FirstLevel(7)` `NetHost(7777)` |
| Client, en direct | `MenuOn(0)` `FirstLevel(7)` `NetJoin("127.0.0.1", 7777)` |
| Hôte, par le salon | `MenuOn(1)` `NetServer("127.0.0.1", 7777)` `NetAuto(1)` |
| Client, par le salon | `MenuOn(1)` `NetServer("127.0.0.1", 7777)` `NetAuto(2)` `NetCode("TEST")` |

- `NetServer` : adresse du serveur, à la place de celle de `online.scr`.
- `NetAuto(1)` : sans souris, ouvre « en ligne », crée un salon et lance le match dès que l'invité est prêt
  (`NetMatches(n)` fois). `NetAuto(2)` : rejoint le salon `NetCode` et se met prêt. Dans les deux cas le jeu saute
  le choix du profil (dernier profil utilisé) et quitte l'écran de fin au bout de 5 s. `NetAuto(3)` : saute
  seulement le choix du profil. `NetAuto(4)` : partie rapide, se met prêt tout seul, `NetMatches(n)` fois
  (`NetMatches(0)` : ouvre seulement l'écran « en ligne », pour le voir sans souris).
- `NetName("nom")` : nom montré aux autres joueurs à la place de celui du profil.
- `NetQuit(n)` : ouvre le menu du match n secondes après le coup d'envoi, puis quitte (forfait en partie classée).
- `NetDirect(0)` : pas de liaison directe, quoi que dise `online.scr`. `NetDirect(2)` : elle casse après 20 s.
- `NetNoSkip(1)` : les jeux automatiques laissent la pause de mi-temps aller au bout.
- `NetTeam(équipe, tenue)` : équipe prise dans le salon.
- `NetBot(1)` côté client : il joue tout seul (directions qui tournent, passes, tirs, supers, touches directes vers un joueur, espace,
  « continuer » à la mi-temps). À la mi-temps il envoie aussi un autre schéma à l'hôte, comme un invité qui
  revoit sa tactique.
- `NetBot(1)` côté hôte : une passe toutes les 2 s, pour qu'un arrêt de jeu de son équipe ne bloque pas un test
  sans personne au clavier, et « continuer » à la mi-temps.
- Avec `NetBot(1)`, chaque jeu ouvre une fois son menu par échap puis le referme (hôte vers 16 s, client vers
  25 s). L'hôte écrit `test : menu local de l'hote, horloge a -> b` : b plus grand que a, le match a continué.
- `NetHalf(n)` côté hôte : mi-temps de n secondes, pour un match de test court.
- En mode debug, la ligne `[mod] net stat` de l'hôte donne aussi l'état du match, l'horloge et le dernier arrêt
  de jeu : c'est par là qu'on voit un match qui attend une touche.
- Deux jeux sur un PC : `mods\build.ps1 -Second` crée et met à jour `game_dev_b` (fenêtré, ses propres
  sauvegardes, son `debugconf.scr`). L'hôte dans `game_dev`, le client dans `game_dev_b`.
- En direct, les deux jeux doivent charger le même stade et les mêmes équipes (ici `FirstLevel(7)`).
- Journal : lignes `[mod] net` (match) et `[mod] salon` (serveur, salon) de `crash.log` ; une ligne de
  statistiques toutes les 5 s en mode debug.

Outils de test dans `server\` :
- `test_server.py` : vérifie le serveur sans le jeu (salons, relais, silences, mauvaise connexion, file d'attente
  par Elo, Elo, forfaits, salon classé quitté avant le match, adresses de la liaison directe, jeu relancé, comptes
  gardés après un redémarrage). Sa base est un fichier temporaire.
- `echo_test.py` : écho UDP, avec `NetTest("127.0.0.1", 7777)` (phase 0).
- `fake_client.py` : faux client qui pilote un hôte en direct et affiche ce qu'il diffuse. À tenir à jour avec `ModNet`.

## Limites actuelles

- L'invité joue toujours l'équipe qui commence à droite.
- En match, « recommencer » reste réservé à l'hôte. « Tactique » n'est ouvert qu'à la mi-temps, pour les deux.
- Stades proposés : ceux des 12 équipes d'origine (pas les terrains bonus).
- Manette non gérée côté client.
- L'hôte simule : il peut tricher en modifiant son jeu. Le serveur ne contrôle que la version annoncée, et qu'un
  score soit annoncé pareil par les deux jeux : un invité honnête refuse donc un faux score, mais pas un match
  truqué en cours de route. Pas de vrai anti-triche.
- Partie classée : l'hôte joue sans retard, l'invité avec le sien. Seul le tirage au sort de l'hôte rééquilibre ;
  le « délai d'entrée de l'hôte » du plan n'est pas fait.
- Dans un salon classé, rien ne presse un joueur qui ne se met pas prêt : l'autre quitte, sans rien perdre.
- Après la pause de mi-temps, le coup d'envoi attend encore la passe de l'équipe qui engage, comme hors ligne :
  un joueur absent bloque la reprise jusqu'à ce que l'autre quitte.
- Liaison directe : pas entre un joueur du PC (ou du réseau) du serveur et un joueur d'Internet, où le relais
  vaut autant ; elle échoue avec les box et les réseaux mobiles qui changent de port selon le destinataire.
  Testée sur ce PC seulement (deux jeux et le serveur sur la même machine), jamais par Internet.
- Installeur : testé sur une fausse copie du jeu, sans question posée. Le choix du dossier à la souris, la demande
  des droits d'administrateur et la saisie de l'adresse n'ont pas été essayés, ni une autre édition du jeu.
- Le jeton du compte voyage en clair et `online.dat` n'est pas protégé.
- En 640×480, la petite police passe de 14 à 16 px sur tous les écrans du mod : en 14 px le moteur perdait une
  partie des lettres. Vu et corrigé sur l'écran « en ligne » ; les autres écrans (Profil, Succès, Ligue, fin de
  match) n'ont pas été revus dans cette résolution.
- Un bouton dont le texte est plus large que lui perd son fond jaune : d'où les boutons élargis de cet écran
  (`make_online_scr.py`).
- Cris des joueurs : joués une fois chez le client, même ceux qui tournent en boucle chez l'hôte.
- Éclairs et tonnerre d'un orage : tirés au hasard de chaque côté, pas au même instant.

## Suite

| # | Phase | État |
|---|---|---|
| 0 | Essais de faisabilité (UDP dans la JVM du jeu, deux jeux sur un PC) | fait |
| 1 | Match en direct, sans serveur | fait, validé |
| 2 | Match complet : effets, sons, HUD, viseur, mi-temps, but, ralenti, fin, pause, coupure | fait, à valider |
| 3 | Serveur Python + salons à code, écran « en ligne », choix des équipes, tactiques | fait, à valider |
| 4 | Partie rapide, Elo en ligne, classement, compte du joueur | fait, à valider |
| 5 | Liaison directe, installeur pour les amis, file d'attente par Elo, pause de mi-temps (VPS écarté : serveur sur ton PC) | fait, à valider |
