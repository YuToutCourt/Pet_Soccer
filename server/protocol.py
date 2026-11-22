"""Format des paquets entre le jeu et le serveur (voir notes/online.md).

Tout paquet commence par "PSN1" (4 octets) et un octet de type. Entiers en gros-boutiste, comme le
ByteBuffer Java du jeu. Une chaîne = un octet de longueur puis un octet par caractère (cp1252 / Latin-1).
À tenir à jour avec mods/java/ModLobby.java.
"""
import socket
import struct

MAGIC = 0x50534E31  # "PSN1"
LOBBY_PROTO = 4  # version du dialogue jeu <-> serveur (ModLobby._LOBBY_PROTO) ; 4 : ligue en ligne
LOBBY_PROTOS = (3, 4)  # versions acceptées : la 3 n'a pas la ligue, le reste du dialogue est le même

# Paquets du match (ModNet) : le serveur les recopie tels quels vers l'autre joueur du salon
PKT_INPUT = 1
PKT_SNAP = 2
PKT_BYE = 3
PKT_PUNCH = 4  # sonde de la liaison directe : va d'un jeu à l'autre, jamais par le serveur

# Jeu -> serveur. Tous commencent par : identifiant du jeu (int), numéro du paquet (int).
PKT_HELLO = 16  # + version du dialogue (short), version du mod (int), nom du joueur (chaîne), jeton du compte (chaîne),
#                 adresse locale du jeu (4 octets) et son port (short non signé), à zéro s'il ne les connaît pas
PKT_CREATE = 17  # crée un salon
PKT_JOIN = 18  # + code du salon (chaîne)
PKT_LEAVE = 19  # quitte le salon ou la file d'attente
PKT_STATE = 20  # + ce que le joueur montre à l'autre (équipe, prêt, tactique...), recopié sans être lu
PKT_QUEUE = 21  # partie rapide : entre dans la file d'attente (répété tant que le joueur attend)
PKT_MATCH = 22  # + numéro du match (int), phase (octet), buts de l'hôte (octet), buts de l'invité (octet), drapeaux (octet)
PKT_TOP = 23  # demande le classement
PKT_LEAGUE = 24  # ligue en ligne : + commande (octet LG_*), version de l'état de la ligue que le jeu a déjà (int), suite

# Serveur -> jeu
PKT_INFO = 32  # réponse à chaque paquet ci-dessus
PKT_PEER = 33  # le contenu du dernier PKT_STATE de l'autre joueur
PKT_RANKS = 34  # le classement, en réponse à PKT_TOP
PKT_LEAGUE_INFO = 35  # réponse à PKT_LEAGUE : la ligue du joueur (make_league_info)

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
STATUS_UNKNOWN = 2  # le serveur ne connaît pas ce jeu : il doit renvoyer PKT_HELLO

# PKT_INFO, rôle dans le salon
ROLE_NONE = 0
ROLE_HOST = 1
ROLE_GUEST = 2

# PKT_INFO, refus d'un PKT_JOIN
JOIN_OK = 0
JOIN_UNKNOWN = 1  # aucun salon avec ce code
JOIN_FULL = 2  # le salon a déjà deux joueurs
JOIN_VERSION = 3  # l'hôte du salon n'a pas la même version du mod

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

_HEAD = struct.Struct(">IB")
_LOBBY_HEAD = struct.Struct(">IBii")
_INFO_TAIL = struct.Struct(">BBhhhhiBBh")
# ... puis la liaison directe et la file d'attente : clé du salon (int), adresse (4 octets) et port (short non signé)
# de l'autre joueur vus par le serveur, son adresse et son port locaux, écart d'Elo accepté dans la file (short)
NO_ADDR = ("0.0.0.0", 0)
HEAD_SIZE = _HEAD.size
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
    """Renvoie (identifiant du jeu, numéro du paquet) d'un paquet jeu -> serveur."""
    if len(data) < LOBBY_HEAD_SIZE:
        raise ValueError("paquet tronqué")
    _, _, client_id, seq = _LOBBY_HEAD.unpack_from(data, 0)
    return client_id, seq


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
    """Renvoie (version du dialogue, version du mod, nom, jeton, adresse locale). Un jeu d'une autre version
    n'est lu que jusqu'à sa version : la suite peut avoir un autre format."""
    proto, mod_version = struct.unpack_from(">hi", data, LOBBY_HEAD_SIZE)
    if proto not in LOBBY_PROTOS:
        return proto, mod_version, "", "", NO_ADDR
    name, off = get_string(data, LOBBY_HEAD_SIZE + 6)
    token, off = get_string(data, off)
    local, _ = get_addr(data, off)
    return proto, mod_version, name, token, local


def read_join(data):
    code, _ = get_string(data, LOBBY_HEAD_SIZE)
    return code


def read_match(data):
    """Renvoie (numéro du match, phase, buts de l'hôte, buts de l'invité, drapeaux)."""
    return struct.unpack_from(">iBBBB", data, LOBBY_HEAD_SIZE)


def make_info(seq, status, code="", role=ROLE_NONE, members=0, peer_name="", join_error=JOIN_OK, flags=0,
              elo=0, games=0, peer_elo=0, online=0, result=(0, RESULT_NONE, 0, 0),
              key=0, peer_addr=NO_ADDR, peer_local=NO_ADDR, queue_range=0):
    """result = (numéro du match, issue RESULT_*, façon HOW_*, variation d'Elo).
    key, peer_addr, peer_local : liaison directe (rooms.Lobby.link). queue_range : écart d'Elo accepté."""
    return (_HEAD.pack(MAGIC, PKT_INFO) + struct.pack(">iB", seq, status) + put_string(code)
            + bytes([role, members]) + put_string(peer_name)
            + _INFO_TAIL.pack(join_error, flags, elo, games, peer_elo, online, result[0], result[1], result[2], result[3])
            + struct.pack(">i", key) + put_addr(peer_addr) + put_addr(peer_local)
            + struct.pack(">h", max(0, min(32767, int(queue_range)))))


def make_peer(payload):
    return _HEAD.pack(MAGIC, PKT_PEER) + payload


def make_ranks(my_rank, total, rows):
    """rows = [(rang, nom, elo, victoires, nuls, défaites)]. my_rank = 0 : pas encore classé."""
    out = _HEAD.pack(MAGIC, PKT_RANKS) + struct.pack(">hhB", my_rank, total, len(rows))
    for rank, name, elo, wins, draws, losses in rows:
        out += struct.pack(">h", rank) + put_string(name) + struct.pack(">hhhh", elo, wins, draws, losses)
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

def make_lobby(ptype, client_id, seq, body=b""):
    return _LOBBY_HEAD.pack(MAGIC, ptype, client_id, seq) + body


def make_hello(client_id, seq, name, mod_version, token="", proto=LOBBY_PROTO, local=NO_ADDR):
    return make_lobby(PKT_HELLO, client_id, seq,
                      struct.pack(">hi", proto, mod_version) + put_string(name) + put_string(token) + put_addr(local))


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
    (queue_range,) = struct.unpack_from(">h", data, off)
    return dict(key=key, peer_addr=peer_addr, peer_local=peer_local, queue_range=queue_range, seq=seq, status=status, code=code, role=role, members=members, peer_name=peer_name,
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
                goals_host=0, goals_guest=0):
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
    return make_lobby(PKT_LEAGUE, client_id, seq, body)


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
