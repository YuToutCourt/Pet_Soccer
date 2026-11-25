"""Format des paquets entre le jeu et le serveur (voir notes/online.md).

Tout paquet commence par "PSN1" (4 octets) et un octet de type. Entiers en gros-boutiste, comme le
ByteBuffer Java du jeu. Une chaîne = un octet de longueur puis un octet par caractère (cp1252 / Latin-1).

Sécurité (dialogue 6) : le serveur donne à chaque jeu un secret de session (PKT_INFO) que le jeu remet dans tous ses
paquets ; un paquet sans le bon secret, ou venu d'une autre adresse que celle du jeu, ne change rien. Le jeu complète
ses paquets jusqu'à LOBBY_MIN_SIZE octets : le serveur ne répond jamais plus gros que la demande à une adresse qu'il
n'a pas vérifiée (pas d'amplification par adresse usurpée).
À tenir à jour avec mods/java/ModLobby.java.
"""
import socket
import struct

MAGIC = 0x50534E31  # "PSN1"
LOBBY_PROTO = 7  # version du dialogue jeu <-> serveur (ModLobby._LOBBY_PROTO) ; 4 : ligue en ligne ; 5 : statistiques ;
#                   6 : secret de session, paquets complétés à LOBBY_MIN_SIZE ; 7 : spectateurs
LOBBY_PROTOS = (7,)  # versions acceptées : un jeu d'une autre version voit « pas la même version »
LOBBY_MIN_SIZE = 96  # taille minimale d'un paquet jeu -> serveur (le jeu complète par des zéros) ; plus court : ignoré

# Paquets du match (ModNet) : le serveur les recopie tels quels vers l'autre joueur du salon
PKT_INPUT = 1
PKT_SNAP = 2
PKT_BYE = 3
PKT_PUNCH = 4  # sonde de la liaison directe : va d'un jeu à l'autre, jamais par le serveur
PKT_SPEC_SNAP = 5  # copie de l'état du match pour les spectateurs : jeu de l'hôte -> serveur -> chaque spectateur.
#                    Même format que PKT_SNAP, avec les événements récents, confirmés ou pas (ModNet)

# Jeu -> serveur. Tous commencent par : identifiant du jeu (int), numéro du paquet (int), secret de session (int non
# signé, donné par PKT_INFO) ; sauf PKT_HELLO, où le secret vient après la version du dialogue (elle reste au même
# endroit pour toutes les versions : un vieux jeu est reconnu et refusé).
PKT_HELLO = 16  # + version du dialogue (short), secret de session (int, 0 si le jeu n'en a pas), version du mod (int),
#                 nom du joueur (chaîne), jeton du compte (chaîne), adresse locale du jeu (4 octets) et son port (short
#                 non signé), à zéro s'il ne les connaît pas. Un jeu inconnu reçoit un secret. Un jeu connu dont l'adresse
#                 a changé (box, 4G) doit donner son secret : il en reçoit un nouveau, à sa nouvelle adresse.
PKT_CREATE = 17  # crée un salon
PKT_JOIN = 18  # + code du salon (chaîne)
PKT_LEAVE = 19  # quitte le salon ou la file d'attente
PKT_STATE = 20  # + ce que le joueur montre à l'autre (équipe, prêt, tactique...), recopié tel quel ; le serveur en
#                 garde une copie lue (read_state) : équipes et tactiques du match enregistré
PKT_QUEUE = 21  # partie rapide : entre dans la file d'attente (répété tant que le joueur attend)
PKT_MATCH = 22  # + numéro du match (int), phase (octet), buts de l'hôte (octet), buts de l'invité (octet), drapeaux (octet)
PKT_TOP = 23  # demande le classement
PKT_LEAGUE = 24  # ligue en ligne : + commande (octet LG_*), version de l'état de la ligue que le jeu a déjà (int), suite
PKT_MATCH_STATS = 25  # le jeu de l'hôte, à la fin d'un match en ligne : ses statistiques (read_match_stats). Redit avec
#                       PKT_MATCH tant que le serveur n'a pas tranché ; le serveur ne garde que le premier.
PKT_LIVE = 26  # demande la liste des matchs qu'on peut regarder (classés et de ligue), réponse PKT_LIVE_LIST
PKT_SPECTATE = 27  # + code du salon (chaîne), numéro du match dont le stade est chargé chez le spectateur (int, 0 :
#                    aucun). Redit tant qu'on regarde ; PKT_LEAVE arrête. Refus dans PKT_INFO (JOIN_UNKNOWN, JOIN_FULL,
#                    JOIN_VERSION). Un spectateur qui arrive, ou dont le stade vient d'être chargé, fait tourner le
#                    compteur d'arrivées que l'hôte lit dans PKT_INFO : il lui envoie alors l'état durable du match.

# Serveur -> jeu
PKT_INFO = 32  # réponse à chaque paquet ci-dessus
PKT_PEER = 33  # le contenu du dernier PKT_STATE de l'autre joueur
PKT_RANKS = 34  # le classement, en réponse à PKT_TOP
PKT_LEAGUE_INFO = 35  # réponse à PKT_LEAGUE : la ligue du joueur (make_league_info)
PKT_LIVE_LIST = 36  # réponse à PKT_LIVE : les matchs en cours qu'on peut regarder (make_live_list)

# PKT_LEAGUE, commandes. Chacune est redite par le jeu jusqu'à ce que la réponse la confirme : elles ne font
# rien de plus quand on les répète.
LG_SYNC = 0  # rien à demander, je regarde ma ligue
LG_CREATE = 1  # + réglages (league_settings), équipe (octet), tenue (octet), notes des équipes (octet n, n shorts x100)
LG_JOIN = 2  # + code (chaîne), équipe, tenue
LG_TEAM = 3  # + équipe, tenue (avant le départ)
LG_START = 4  # le créateur lance la ligue
LG_LEAVE = 5  # quitte la ligue ; après le départ, l'IA reprend son équipe
LG_RESULT = 6  # + clé du match (int), buts de l'équipe qui reçoit, de l'invitée (octets) : match contre l'IA
LG_PLAY = 7  # + clé du match (int) : je joue ce match (contre l'IA) ou j'attends l'autre joueur (redit 3 fois par seconde)
LG_SKIP = 8  # le créateur ferme la journée tout de suite : ce qui reste est simulé

# PKT_LEAGUE_INFO, issue de la commande
LGE_NONE = 0
LGE_UNKNOWN = 1  # aucune ligue avec ce code
LGE_STARTED = 2  # la ligue a déjà commencé : on n'y entre plus
LGE_FULL = 3  # toutes les équipes sont prises
LGE_TEAM_TAKEN = 4  # équipe déjà prise : une autre a été donnée
LGE_NOT_OWNER = 5  # seul le créateur peut le faire
LGE_NO_ACCOUNT = 6  # pas de compte en ligne (jeton)
LGE_REFUSED = 7  # ce match n'est pas à jouer (déjà joué, simulé après le délai...)
LGE_LIMIT = 8  # le serveur n'accepte plus de ligue
LGE_VERSION = 9  # l'autre joueur n'a pas la même version du mod
LGE_REPLAY = 10  # nul en playoffs : le match est à rejouer
LGE_BAD = 11  # demande illisible (réglages, notes)

# PKT_LEAGUE_INFO, drapeaux de la ligue
LGF_OWNER = 1  # je l'ai créée
LGF_STARTED = 2
LGF_OVER = 4
# ... et d'un membre
LGM_ONLINE = 1  # connecté au serveur
LGM_ME = 2
LGM_OWNER = 4
LGM_LEFT = 8  # a abandonné : l'IA joue son équipe
LGM_WAITING = 16  # attend de jouer contre moi (notre match de la journée)
LGM_BUSY = 32  # dans un salon ou un match

INFO_LEAGUE = 4  # PKT_INFO : le salon est un match de ligue en ligne (créé par le serveur, voir rooms.Lobby.league_play)

# PKT_INFO, état
STATUS_OK = 0
STATUS_BAD_PROTO = 1  # jeu et serveur ne parlent pas la même version
STATUS_UNKNOWN = 2  # le serveur ne connaît pas ce jeu (ou pas ce secret, pas à cette adresse) : il doit renvoyer PKT_HELLO
STATUS_BUSY = 3  # trop de jeux connectés (en tout, ou depuis cette adresse IP) : réessayer plus tard

# PKT_INFO, rôle dans le salon
ROLE_NONE = 0
ROLE_HOST = 1
ROLE_GUEST = 2
ROLE_SPECTATOR = 3  # regarde le match du salon (PKT_SPECTATE) : peer_name est le nom de l'hôte, peer2_name de l'invité

# PKT_INFO, refus d'un PKT_JOIN
JOIN_OK = 0
JOIN_UNKNOWN = 1  # aucun salon avec ce code
JOIN_FULL = 2  # le salon a déjà deux joueurs
JOIN_VERSION = 3  # l'hôte du salon n'a pas la même version du mod
# PKT_SPECTATE : JOIN_UNKNOWN aussi pour un salon privé (on n'y regarde pas), JOIN_FULL quand les places de
# spectateurs sont prises.

# PKT_INFO, drapeaux
INFO_QUEUED = 1  # le joueur est dans la file d'attente
INFO_RANKED = 2  # son salon est une partie classée (créé par la file d'attente)

# PKT_MATCH, phase
MATCH_PLAYING = 1
MATCH_FINISHED = 2  # le match est allé au bout
MATCH_ABANDON = 3  # j'ai quitté le match en cours
MATCH_PEER_LOST = 4  # j'ai quitté parce que l'autre joueur ne répondait plus
# PKT_MATCH, drapeaux
MATCH_LINKED = 1  # mon terrain est chargé et j'envoie des paquets de match à l'autre joueur. En liaison directe le
#                   serveur ne voit plus passer ces paquets : c'est ce drapeau qui lui dit que le match a commencé.

# PKT_INFO, issue du dernier match classé du joueur
RESULT_NONE = 0
RESULT_RATED = 1  # compté dans l'Elo
RESULT_VOID = 2  # pas compté
# ... et comment il s'est fini
HOW_PLAYED = 1  # joué jusqu'au bout
HOW_PEER_FORFEIT = 2  # forfait de l'autre joueur
HOW_MY_FORFEIT = 3  # mon forfait

TOP_ROWS = 8  # lignes de classement dans un PKT_RANKS (MenuOnline._TOP_ROWS)
LIVE_ROWS = 8  # matchs dans un PKT_LIVE_LIST
LIVE_LEAGUE = 1  # PKT_LIVE_LIST, drapeau d'un match : match de ligue (sinon partie classée)

_HEAD = struct.Struct(">IB")
_HELLO_HEAD = struct.Struct(">IBii")  # PKT_HELLO : sans le secret
_LOBBY_HEAD = struct.Struct(">IBiiI")  # les autres : avec le secret
_INFO_TAIL = struct.Struct(">BBhhhhiBBh")
# ... puis la liaison directe et la file d'attente : clé du salon (int), adresse (4 octets) et port (short non signé)
# de l'autre joueur vus par le serveur, son adresse et son port locaux, écart d'Elo accepté dans la file (short),
# secret de session du jeu (int non signé) ; puis (dialogue 7) les spectateurs du salon (octet), leurs arrivées
# (octet, compteur qui tourne : l'hôte remet alors les spectateurs à niveau), et pour un spectateur le nom de
# l'invité (chaîne, vide sinon)
NO_ADDR = ("0.0.0.0", 0)
HEAD_SIZE = _HEAD.size
HELLO_HEAD_SIZE = _HELLO_HEAD.size
LOBBY_HEAD_SIZE = _LOBBY_HEAD.size


def put_string(s):
    data = s.encode("latin-1", "replace")[:255]
    return bytes([len(data)]) + data


def get_string(data, off):
    """Renvoie (chaîne, position suivante). Lève ValueError si le paquet est tronqué."""
    if off >= len(data):
        raise ValueError("chaîne tronquée")
    end = off + 1 + data[off]
    if end > len(data):
        raise ValueError("chaîne tronquée")
    return data[off + 1:end].decode("latin-1"), end


def read_head(data):
    """Renvoie le type du paquet, ou None si ce n'est pas un paquet du jeu."""
    if len(data) < HEAD_SIZE:
        return None
    magic, ptype = _HEAD.unpack_from(data, 0)
    return ptype if magic == MAGIC else None


def read_lobby_head(data):
    """Renvoie (identifiant du jeu, numéro du paquet) d'un paquet jeu -> serveur (PKT_HELLO compris)."""
    if len(data) < HELLO_HEAD_SIZE:
        raise ValueError("paquet tronqué")
    _, _, client_id, seq = _HELLO_HEAD.unpack_from(data, 0)
    return client_id, seq


def read_secret(data):
    """Le secret de session d'un paquet jeu -> serveur autre que PKT_HELLO."""
    if len(data) < LOBBY_HEAD_SIZE:
        raise ValueError("paquet tronqué")
    return _LOBBY_HEAD.unpack_from(data, 0)[4]


def pad(data):
    """Complète un paquet jeu -> serveur jusqu'à LOBBY_MIN_SIZE octets (comme ModLobby.Send)."""
    return data + bytes(max(0, LOBBY_MIN_SIZE - len(data)))


def put_addr(addr):
    """Adresse IPv4 et port sur 6 octets. Une adresse illisible part à zéro."""
    try:
        return socket.inet_aton(addr[0]) + struct.pack(">H", addr[1])
    except (OSError, struct.error, TypeError):
        return bytes(6)


def get_addr(data, off):
    """Renvoie ((adresse, port), position suivante)."""
    if off + 6 > len(data):
        raise ValueError("adresse tronquée")
    return (socket.inet_ntoa(data[off:off + 4]), struct.unpack_from(">H", data, off + 4)[0]), off + 6


def read_hello(data):
    """Renvoie (version du dialogue, secret, version du mod, nom, jeton, adresse locale). Un jeu d'une autre version
    n'est lu que jusqu'à sa version : la suite peut avoir un autre format."""
    (proto,) = struct.unpack_from(">h", data, HELLO_HEAD_SIZE)
    if proto not in LOBBY_PROTOS:
        return proto, 0, 0, "", "", NO_ADDR
    secret, mod_version = struct.unpack_from(">Ii", data, HELLO_HEAD_SIZE + 2)
    name, off = get_string(data, HELLO_HEAD_SIZE + 10)
    token, off = get_string(data, off)
    local, _ = get_addr(data, off)
    return proto, secret, mod_version, name, token, local


def read_join(data):
    code, _ = get_string(data, LOBBY_HEAD_SIZE)
    return code


def read_spectate(data):
    """Renvoie (code du salon, numéro du match chargé chez le spectateur)."""
    code, off = get_string(data, LOBBY_HEAD_SIZE)
    (ready,) = struct.unpack_from(">i", data, off)
    return code, ready


def make_spectate(code, ready=0):
    return put_string(code) + struct.pack(">i", ready)


def read_match(data):
    """Renvoie (numéro du match, phase, buts de l'hôte, buts de l'invité, drapeaux)."""
    return struct.unpack_from(">iBBBB", data, LOBBY_HEAD_SIZE)


_STATE_TAIL = struct.Struct(">Bhi4B")


def read_state(payload):
    """PKT_STATE sans l'en-tête de salon (ModLobby.SendState). Renvoie un dict ; ValueError s'il est tronqué."""
    if len(payload) < 5:
        raise ValueError("état tronqué")
    flags, animal, clothes = payload[0], payload[1], payload[2]
    (n,) = struct.unpack_from(">H", payload, 3)
    off = 5 + n
    if off + _STATE_TAIL.size > len(payload):
        raise ValueError("état tronqué")
    tactic = payload[5:off].decode("latin-1")
    level, half, match_id, t0, t1, t2, t3 = _STATE_TAIL.unpack_from(payload, off)
    return dict(ready=bool(flags & 1), host=bool(flags & 2), animal=animal, clothes=clothes, tactic=tactic,
                level=level, half=half, match_id=match_id, teams=(t0, t1, t2, t3))


def make_state(ready=False, host=False, animal=0, clothes=0, tactic="", level=0, half=0, match_id=0, teams=(0, 0, 0, 0)):
    """Pour les tests : le corps d'un PKT_STATE, comme ModLobby.SendState."""
    t = tactic.encode("latin-1")
    return (bytes([int(ready) | (2 if host else 0), animal, clothes]) + struct.pack(">H", len(t)) + t
            + _STATE_TAIL.pack(level, half, match_id, *teams))


# PKT_MATCH_STATS : n° du match (int), drapeaux (octet, 1 = but en or), taille de la grille des heatmaps (profondeur,
# latéral : octets), puis pour l'équipe de l'hôte et celle de l'invité :
#   tirs (short), possession % (octet), passes réussies, passes, récupérations, arrêts, supers (shorts),
#   types de supers (int, bit n = super n), plus gros retard (octet) ;
#   buts : n (octet), puis n x (minute, joueur (signé, -1 inconnu), drapeaux (1 = csc)) ; pour un csc, le joueur
#     est de l'autre équipe ;
#   supers : n, puis n x (minute, type) ;
#   tirs : n, puis n x (minute, joueur (signé), profondeur, latéral (0..255), issue SHOT_*) ;
#   joueurs : n, puis n x (place dans la liste Team<Animal>Players (0 = gardien), place de la tactique (signé, -1),
#     buts, tirs, passes, passes réussies, récupérations (octets), distance en m (short non signé),
#     profondeur et latéral moyens (0..255)) ;
#   heatmap de l'équipe, puis du ballon quand elle l'a : profondeur x latéral octets (0..255, ligne par profondeur).
# Profondeur 0 = son but, 1 = but adverse ; latéral 0 = haut de l'écran (repère de l'écran Tactique).
MS_GOLDEN = 1
SHOT_MISSED, SHOT_GOAL, SHOT_SAVED, SHOT_BLOCKED = range(4)
SHOT_RESULTS = ("missed", "goal", "saved", "blocked")
_MS_HEAD = struct.Struct(">iBBB")
_MS_TEAM = struct.Struct(">hBhhhhhiB")
_MS_PLAYER = struct.Struct(">BbBBBBBHBB")


def read_match_stats(data):
    """PKT_MATCH_STATS complet (en-tête compris). Renvoie un dict ; ValueError s'il est tronqué ou mal formé."""
    off = LOBBY_HEAD_SIZE
    match_id, flags, heat_d, heat_l = _MS_HEAD.unpack_from(data, off)
    off += _MS_HEAD.size
    if not (0 < heat_d <= 32 and 0 < heat_l <= 32):
        raise ValueError("grille de heatmap invalide")
    cells = heat_d * heat_l
    teams = []
    for _ in range(2):
        shots, poss, passes_ok, passes, recoveries, saves, supers, super_types, deficit = _MS_TEAM.unpack_from(data, off)
        off += _MS_TEAM.size
        t = dict(shots=shots, possession=poss, passes_ok=passes_ok, passes=passes, recoveries=recoveries, saves=saves,
                 supers=supers, super_types=super_types, max_deficit=deficit)
        n = data[off]
        off += 1
        t["goals"] = []
        for _ in range(n):
            minute, number, gflags = struct.unpack_from(">BbB", data, off)
            off += 3
            t["goals"].append(dict(minute=minute, number=number, own_goal=bool(gflags & 1)))
        n = data[off]
        off += 1
        if off + 2 * n > len(data):
            raise ValueError("supers tronqués")
        t["super_events"] = [dict(minute=data[off + 2 * i], type=data[off + 2 * i + 1]) for i in range(n)]
        off += 2 * n
        n = data[off]
        off += 1
        t["shot_events"] = []
        for _ in range(n):
            minute, number, depth, lateral, result = struct.unpack_from(">BbBBB", data, off)
            off += 5
            t["shot_events"].append(dict(minute=minute, number=number, depth=depth / 255.0, lateral=lateral / 255.0,
                                   result=SHOT_RESULTS[result] if result < len(SHOT_RESULTS) else "missed"))
        n = data[off]
        off += 1
        t["players"] = []
        for _ in range(n):
            number, slot, goals, pshots, ppasses, pok, prec, distance, depth, lateral = _MS_PLAYER.unpack_from(data, off)
            off += _MS_PLAYER.size
            t["players"].append(dict(number=number, slot=slot, goals=goals, shots=pshots, passes=ppasses, passes_ok=pok,
                                     recoveries=prec, distance=distance, depth=depth / 255.0, lateral=lateral / 255.0))
        if off + 2 * cells > len(data):
            raise ValueError("heatmaps tronquées")
        t["heatmap"] = list(data[off:off + cells])
        t["ball_heatmap"] = list(data[off + cells:off + 2 * cells])
        off += 2 * cells
        teams.append(t)
    return dict(match_id=match_id, golden_goal=bool(flags & MS_GOLDEN), heat_size=(heat_d, heat_l), teams=teams)


def make_match_stats(match_id, teams, golden_goal=False, heat_size=(12, 8)):
    """Pour les tests : le corps d'un PKT_MATCH_STATS (même dict que read_match_stats, positions en 0..1)."""
    heat_d, heat_l = heat_size
    out = bytearray(_MS_HEAD.pack(match_id, MS_GOLDEN if golden_goal else 0, heat_d, heat_l))
    for t in teams:
        out += _MS_TEAM.pack(t["shots"], t["possession"], t["passes_ok"], t["passes"], t["recoveries"], t["saves"],
                             t["supers"], t["super_types"], t["max_deficit"])
        out.append(len(t["goals"]))
        for g in t["goals"]:
            out += struct.pack(">BbB", g["minute"], g["number"], 1 if g["own_goal"] else 0)
        out.append(len(t["super_events"]))
        for s in t["super_events"]:
            out += bytes([s["minute"], s["type"]])
        out.append(len(t["shot_events"]))
        for s in t["shot_events"]:
            out += struct.pack(">BbBBB", s["minute"], s["number"], round(s["depth"] * 255), round(s["lateral"] * 255),
                               SHOT_RESULTS.index(s["result"]))
        out.append(len(t["players"]))
        for p in t["players"]:
            out += _MS_PLAYER.pack(p["number"], p["slot"], p["goals"], p["shots"], p["passes"], p["passes_ok"],
                                   p["recoveries"], p["distance"], round(p["depth"] * 255), round(p["lateral"] * 255))
        out += bytes(t["heatmap"]) + bytes(t["ball_heatmap"])
    return bytes(out)


def make_info(seq, status, code="", role=ROLE_NONE, members=0, peer_name="", join_error=JOIN_OK, flags=0,
              elo=0, games=0, peer_elo=0, online=0, result=(0, RESULT_NONE, 0, 0),
              key=0, peer_addr=NO_ADDR, peer_local=NO_ADDR, queue_range=0, secret=0, spectators=0, spectator_joins=0,
              peer2_name=""):
    """result = (numéro du match, issue RESULT_*, façon HOW_*, variation d'Elo).
    key, peer_addr, peer_local : liaison directe (rooms.Lobby.link). queue_range : écart d'Elo accepté.
    secret : secret de session du jeu (0 dans un refus). spectators, spectator_joins : spectateurs du salon et
    compteur de leurs arrivées. peer2_name : pour un spectateur, le nom de l'invité (peer_name est celui de l'hôte)."""
    return (_HEAD.pack(MAGIC, PKT_INFO) + struct.pack(">iB", seq, status) + put_string(code)
            + bytes([role, members]) + put_string(peer_name)
            + _INFO_TAIL.pack(join_error, flags, elo, games, peer_elo, online, result[0], result[1], result[2], result[3])
            + struct.pack(">i", key) + put_addr(peer_addr) + put_addr(peer_local)
            + struct.pack(">hI", max(0, min(32767, int(queue_range))), secret)
            + bytes([min(255, spectators), spectator_joins & 0xFF]) + put_string(peer2_name))


def make_peer(payload):
    return _HEAD.pack(MAGIC, PKT_PEER) + payload


def make_ranks(my_rank, total, rows):
    """rows = [(rang, nom, elo, victoires, nuls, défaites)]. my_rank = 0 : pas encore classé."""
    out = _HEAD.pack(MAGIC, PKT_RANKS) + struct.pack(">hhB", my_rank, total, len(rows))
    for rank, name, elo, wins, draws, losses in rows:
        out += struct.pack(">h", rank) + put_string(name) + struct.pack(">hhhh", elo, wins, draws, losses)
    return out


def make_live_list(rows):
    """rows = [dict(code, host, guest, host_elo, guest_elo, host_animal, guest_animal, score_host, score_guest,
    spectators, league)], LIVE_ROWS au plus."""
    rows = rows[:LIVE_ROWS]
    out = _HEAD.pack(MAGIC, PKT_LIVE_LIST) + bytes([len(rows)])
    for r in rows:
        out += (put_string(r["code"]) + put_string(r["host"]) + put_string(r["guest"])
                + struct.pack(">hhBBBBBB", r["host_elo"], r["guest_elo"], r["host_animal"] & 0xFF,
                              r["guest_animal"] & 0xFF, min(255, r["score_host"]), min(255, r["score_guest"]),
                              min(255, r["spectators"]), LIVE_LEAGUE if r["league"] else 0))
    return out


# --- ligue en ligne ---

_LEAGUE_SETTINGS = struct.Struct(">BBBBhhhhiB")
# qualifiés, revanche en grande finale, points victoire, points nul, forme x100, buts par minute x1000, bonus à domicile
# x1000, effet de la note x1000, délai d'une journée en minutes, minutes d'une mi-temps


def put_zigzag(out, n):
    """Entier en varint zigzag : 1 octet de -64 à 63, 2 jusqu'à ±8191..."""
    n = (n << 1) ^ (n >> 31)
    n &= 0xFFFFFFFF
    while n >= 0x80:
        out.append((n & 0x7F) | 0x80)
        n >>= 7
    out.append(n)


def get_zigzag(data, off):
    n = shift = 0
    while True:
        if off >= len(data) or shift > 28:
            raise ValueError("entier tronqué")
        b = data[off]
        off += 1
        n |= (b & 0x7F) << shift
        shift += 7
        if not b & 0x80:
            break
    return (n >> 1) ^ -(n & 1), off


def read_league(data):
    """Renvoie (commande, version connue, arguments) d'un PKT_LEAGUE."""
    cmd = data[LOBBY_HEAD_SIZE]
    (known,) = struct.unpack_from(">i", data, LOBBY_HEAD_SIZE + 1)
    off = LOBBY_HEAD_SIZE + 5
    args = {}
    if cmd == LG_CREATE:
        q, reset, pw, pd, form, gpm, home, strength, delay, half = _LEAGUE_SETTINGS.unpack_from(data, off)
        off += _LEAGUE_SETTINGS.size
        args["settings"] = dict(qualifiers=q, reset=bool(reset), points_win=pw, points_draw=pd, form=form / 100.0,
                                goals_per_minute=gpm / 1000.0, home_bonus=home / 1000.0, strength_effect=strength / 1000.0,
                                delay=delay, half=half)
        args["animal"], args["clothes"], n = data[off], data[off + 1], data[off + 2]
        off += 3
        args["ratings"] = [r / 100.0 for r in struct.unpack_from(">%dh" % n, data, off)]
    elif cmd == LG_JOIN:
        args["code"], off = get_string(data, off)
        args["animal"], args["clothes"] = data[off], data[off + 1]
    elif cmd == LG_TEAM:
        args["animal"], args["clothes"] = data[off], data[off + 1]
    elif cmd == LG_RESULT:
        args["key"], args["goals_host"], args["goals_guest"] = struct.unpack_from(">iBB", data, off)
    elif cmd == LG_PLAY:
        (args["key"],) = struct.unpack_from(">i", data, off)
    return cmd, known, args


def make_league_info(seq, error, view=None):
    """view : ce que voit le joueur de sa ligue (leagues.Leagues.view), None s'il n'en a pas.
    L'état de la ligue (ModLeague.Save) ne part que si le jeu ne l'a pas déjà dans cette version."""
    out = bytearray(_HEAD.pack(MAGIC, PKT_LEAGUE_INFO) + struct.pack(">iBB", seq, error, 1 if view else 0))
    if not view:
        return bytes(out)
    s = view["settings"]
    out += put_string(view["code"]) + struct.pack(">Bi", view["flags"], view["version"])
    out += _LEAGUE_SETTINGS.pack(s["qualifiers"], int(s["reset"]), s["points_win"], s["points_draw"], int(round(s["form"] * 100)),
                                 int(round(s["goals_per_minute"] * 1000)), int(round(s["home_bonus"] * 1000)),
                                 int(round(s["strength_effect"] * 1000)), s["delay"], s["half"])
    out += struct.pack(">ibiB", view["deadline"], view["my_team"], view["my_key"], len(view["members"]))
    for m in view["members"]:
        out += put_string(m["name"]) + bytes([m["animal"], m["clothes"], m["flags"]])
    state = view.get("state")
    out += struct.pack(">h", len(state) if state else 0)
    for n in state or ():
        put_zigzag(out, n)
    return bytes(out)


# --- côté jeu : sert aux tests (server/test_server.py) ---

def make_lobby(ptype, client_id, seq, body=b"", secret=0):
    return pad(_LOBBY_HEAD.pack(MAGIC, ptype, client_id, seq, secret) + body)


def make_hello(client_id, seq, name, mod_version, token="", proto=LOBBY_PROTO, local=NO_ADDR, secret=0):
    return pad(_HELLO_HEAD.pack(MAGIC, PKT_HELLO, client_id, seq) + struct.pack(">hIi", proto, secret, mod_version)
               + put_string(name) + put_string(token) + put_addr(local))


def make_match(match_id, phase, score_host, score_guest, flags=0):
    return struct.pack(">iBBBB", match_id, phase, score_host, score_guest, flags)


def read_info(data):
    seq, status = struct.unpack_from(">iB", data, HEAD_SIZE)
    code, off = get_string(data, HEAD_SIZE + 5)
    role, members = data[off], data[off + 1]
    peer_name, off = get_string(data, off + 2)
    join_error, flags, elo, games, peer_elo, online, match_id, outcome, how, delta = _INFO_TAIL.unpack_from(data, off)
    off += _INFO_TAIL.size
    (key,) = struct.unpack_from(">i", data, off)
    peer_addr, off = get_addr(data, off + 4)
    peer_local, off = get_addr(data, off)
    queue_range, secret = struct.unpack_from(">hI", data, off)
    spectators, spectator_joins = data[off + 6], data[off + 7]
    peer2_name, off = get_string(data, off + 8)
    return dict(spectators=spectators, spectator_joins=spectator_joins, peer2_name=peer2_name, secret=secret, key=key, peer_addr=peer_addr, peer_local=peer_local, queue_range=queue_range, seq=seq, status=status, code=code, role=role, members=members, peer_name=peer_name,
                join_error=join_error, queued=bool(flags & INFO_QUEUED), ranked=bool(flags & INFO_RANKED),
                league=bool(flags & INFO_LEAGUE),
                elo=elo, games=games, peer_elo=peer_elo, online=online,
                result=dict(match=match_id, outcome=outcome, how=how, delta=delta))


def read_ranks(data):
    my_rank, total, count = struct.unpack_from(">hhB", data, HEAD_SIZE)
    off = HEAD_SIZE + 5
    rows = []
    for _ in range(count):
        (rank,) = struct.unpack_from(">h", data, off)
        name, off = get_string(data, off + 2)
        elo, wins, draws, losses = struct.unpack_from(">hhhh", data, off)
        off += 8
        rows.append(dict(rank=rank, name=name, elo=elo, wins=wins, draws=draws, losses=losses))
    return dict(my_rank=my_rank, total=total, rows=rows)


def make_league(client_id, seq, cmd, known=0, settings=None, animal=0, clothes=0, ratings=(), code="", key=0,
                goals_host=0, goals_guest=0, secret=0):
    body = bytes([cmd]) + struct.pack(">i", known)
    if cmd == LG_CREATE:
        s = settings
        body += _LEAGUE_SETTINGS.pack(s["qualifiers"], int(s["reset"]), s["points_win"], s["points_draw"], int(round(s["form"] * 100)),
                                      int(round(s["goals_per_minute"] * 1000)), int(round(s["home_bonus"] * 1000)),
                                      int(round(s["strength_effect"] * 1000)), s["delay"], s["half"])
        body += bytes([animal, clothes, len(ratings)]) + struct.pack(">%dh" % len(ratings), *[int(round(r * 100)) for r in ratings])
    elif cmd == LG_JOIN:
        body += put_string(code) + bytes([animal, clothes])
    elif cmd == LG_TEAM:
        body += bytes([animal, clothes])
    elif cmd == LG_RESULT:
        body += struct.pack(">iBB", key, goals_host, goals_guest)
    elif cmd == LG_PLAY:
        body += struct.pack(">i", key)
    return make_lobby(PKT_LEAGUE, client_id, seq, body, secret)


def read_live_list(data):
    count = data[HEAD_SIZE]
    off = HEAD_SIZE + 1
    rows = []
    for _ in range(count):
        code, off = get_string(data, off)
        host, off = get_string(data, off)
        guest, off = get_string(data, off)
        he, ge, ha, ga, sh, sg, spec, flags = struct.unpack_from(">hhBBBBBB", data, off)
        off += 10
        rows.append(dict(code=code, host=host, guest=guest, host_elo=he, guest_elo=ge, host_animal=ha, guest_animal=ga,
                         score_host=sh, score_guest=sg, spectators=spec, league=bool(flags & LIVE_LEAGUE)))
    return rows


def read_league_info(data):
    seq, error, here = struct.unpack_from(">iBB", data, HEAD_SIZE)
    out = dict(seq=seq, error=error, league=None)
    if not here:
        return out
    code, off = get_string(data, HEAD_SIZE + 6)
    flags, version = struct.unpack_from(">Bi", data, off)
    off += 5
    q, reset, pw, pd, form, gpm, home, strength, delay, half = _LEAGUE_SETTINGS.unpack_from(data, off)
    off += _LEAGUE_SETTINGS.size
    deadline, my_team, my_key, count = struct.unpack_from(">ibiB", data, off)
    off += 10
    members = []
    for _ in range(count):
        name, off = get_string(data, off)
        members.append(dict(name=name, animal=data[off], clothes=data[off + 1], flags=data[off + 2]))
        off += 3
    (n,) = struct.unpack_from(">h", data, off)
    off += 2
    state = []
    for _ in range(n):
        v, off = get_zigzag(data, off)
        state.append(v)
    out["league"] = dict(code=code, flags=flags, version=version, qualifiers=q, delay=delay, half=half, deadline=deadline,
                         my_team=my_team, my_key=my_key, members=members, state=state or None, size=len(data))
    return out
