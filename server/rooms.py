"""Joueurs connectés, salons à code, file d'attente et matchs classés. Aucun réseau ici : server.py s'en occupe.

Partie classée : la file d'attente met deux joueurs dans un salon « classé ». Pendant le match, chaque jeu
annonce où il en est (PKT_MATCH). Le match compte quand les deux jeux annoncent la même fin avec le même
score. Un joueur qui quitte, ferme son jeu ou perd sa connexion pendant le match est forfait.

File d'attente : deux joueurs sont réunis quand leur écart d'Elo tient dans l'écart accepté, qui grandit sans
limite avec le temps d'attente (queue_range + queue_grow par seconde).

Liaison directe : le serveur donne à chaque joueur d'un salon l'adresse de l'autre, pour que les paquets du
match aillent d'un jeu à l'autre sans passer par lui. Il reste le relais de secours.
"""
import ipaddress
import random

import protocol as P

# Lettres d'un code de salon : sans I ni O, trop proches de 1 et 0 quand on dicte le code
CODE_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ"
CODE_LENGTH = 4

# Portée d'une adresse vue par le serveur
SCOPE_LOOPBACK = 0  # le jeu tourne sur la machine du serveur
SCOPE_PRIVATE = 1  # le jeu est sur le réseau local du serveur
SCOPE_PUBLIC = 2  # le jeu arrive par Internet


def scope(ip):
    try:
        addr = ipaddress.ip_address(ip)
    except ValueError:
        return SCOPE_PUBLIC
    if addr.is_loopback:
        return SCOPE_LOOPBACK
    return SCOPE_PRIVATE if addr.is_private or addr.is_link_local else SCOPE_PUBLIC


class Session:
    """Un jeu connecté au serveur."""

    def __init__(self, client_id, addr, name, mod_version, now):
        self.client_id = client_id
        self.addr = addr
        self.name = name
        self.mod_version = mod_version
        self.last_seen = now
        self.room = None
        self.token = ""
        self.player = None  # compte du joueur (store.Player), None sans jeton valable
        self.local_addr = P.NO_ADDR  # adresse du jeu sur son réseau local, annoncée par lui (liaison directe)
        self.last_queue = 0.0  # dernière demande de partie rapide
        self.queue_since = 0.0  # entrée dans la file d'attente
        self.last_match = now  # dernier paquet de match de ce jeu : relayé, ou annoncé par lui (MATCH_LINKED)
        self.report = None  # dernier PKT_MATCH : (numéro du match, phase, buts de l'hôte, buts de l'invité)
        self.result = (0, P.RESULT_NONE, 0, 0)  # issue de son dernier match classé (voir protocol.make_info)

    def __repr__(self):
        return "%s (%s:%d)" % (self.name, self.addr[0], self.addr[1])


class Room:
    """Un salon : l'hôte qui l'a créé, et au plus un invité."""

    def __init__(self, code, host, ranked=False, key=0):
        self.code = code
        self.host = host
        self.guest = None
        self.key = key  # secret du salon : les sondes de la liaison directe le portent
        self.ranked = ranked  # créé par la file d'attente : ses matchs comptent pour l'Elo
        self.relayed = 0  # paquets de match recopiés
        # partie classée
        self.live = 0  # numéro du match en cours, annoncé par les deux jeux
        self.engaged = set()  # rôles qui ont envoyé un paquet de match depuis : le match a vraiment commencé
        self.claim_at = None  # heure où un joueur a dit avoir perdu l'autre, pas encore confirmé
        self.decided = set()  # numéros des matchs déjà comptés ou annulés

    def members(self):
        return 2 if self.guest else 1

    def peer(self, session):
        return self.guest if session is self.host else self.host


class Lobby:
    def __init__(self, fixed_code=None, timeout=30.0, stale=10.0, max_rooms=500, rng=None, store=None,
                 lost_after=4.0, claim_wait=6.0, takeover_after=3.0, queue_stale=3.0,
                 queue_range=100.0, queue_grow=10.0, direct=True):
        self.sessions = {}  # identifiant du jeu -> Session
        self.by_addr = {}  # adresse -> Session
        self.by_player = {}  # numéro du compte -> sa dernière Session
        self.rooms = {}  # code -> Room
        self.queue = []  # joueurs qui attendent une partie rapide, le plus ancien d'abord
        self.fixed_code = fixed_code
        self.timeout = timeout  # silence après lequel un joueur est oublié
        self.stale = stale  # silence après lequel l'invité d'un salon peut être remplacé
        self.max_rooms = max_rooms
        self.rng = rng or random.Random()
        self.store = store  # comptes et Elo ; None : pas de partie classée
        self.lost_after = lost_after  # silence d'un jeu en match après lequel l'autre peut le déclarer perdu
        self.claim_wait = claim_wait  # temps laissé pour confirmer « j'ai perdu l'autre » avant d'annuler le match
        self.takeover_after = takeover_after  # silence après lequel un jeu relancé reprend la place de l'ancien
        self.queue_stale = queue_stale  # silence après lequel un joueur sort de la file d'attente
        self.queue_range = queue_range  # écart d'Elo accepté en entrant dans la file d'attente
        self.queue_grow = queue_grow  # points d'Elo ajoutés à cet écart par seconde d'attente, sans limite
        self.direct = direct  # donner aux joueurs d'un salon l'adresse de l'autre (liaison directe)
        self.now = 0.0
        self.log = lambda text: None

    # --- joueurs ---

    def hello(self, client_id, addr, name, mod_version, now, player=None, token="", local_addr=P.NO_ADDR):
        self.now = now
        session = self.sessions.get(client_id)
        if session is None:
            session = Session(client_id, addr, name, mod_version, now)
            self.sessions[client_id] = session
            self.log("bonjour %r, mod v%d%s" % (session, mod_version, ", compte %r" % player if player else ""))
            old = self.by_player.get(player.id) if player is not None else None
            if old is not None and old.client_id in self.sessions and now - old.last_seen >= self.takeover_after:
                self.takeover(old, session)
        session.name = name
        session.mod_version = mod_version
        session.token = token
        session.player = player
        session.local_addr = local_addr
        if player is not None:
            self.by_player[player.id] = session
        self.touch(session, addr, now)
        return session

    def touch(self, session, addr, now):
        """Le joueur vient de parler. Son adresse peut changer en route (box, 4G) : on suit la dernière."""
        self.now = now
        session.last_seen = now
        if session.addr != addr:
            if self.by_addr.get(session.addr) is session:
                del self.by_addr[session.addr]
            session.addr = addr
        self.by_addr[addr] = session

    def forget(self, session):
        self.sessions.pop(session.client_id, None)
        if self.by_addr.get(session.addr) is session:
            del self.by_addr[session.addr]
        if session.player is not None and self.by_player.get(session.player.id) is session:
            del self.by_player[session.player.id]

    def drop(self, session, why, silent=False):
        self.leave(session, why, silent)
        self.forget(session)
        self.log("parti : %r (%s)" % (session, why))

    def takeover(self, old, new):
        """Le jeu d'un joueur a été relancé (nouvel identifiant, même compte) : il retrouve son salon."""
        room = old.room
        if room is not None:
            if room.ranked and room.live and room.host is old:
                # l'hôte simulait le match : il est perdu avec son jeu
                self.exit_live(old, "jeu de l'hôte relancé")
            if room.host is old:
                room.host = new
            else:
                room.guest = new
            new.room = room
            old.room = None
            self.log("salon %s : %r reprend sa place" % (room.code, new))
        if old in self.queue:
            self.queue.remove(old)
        new.result = old.result
        new.last_match = old.last_match
        self.forget(old)

    def expire(self, now):
        self.now = now
        for session in [s for s in self.sessions.values() if now - s.last_seen > self.timeout]:
            self.drop(session, "silence de %d s" % self.timeout, silent=True)
        self.queue = [s for s in self.queue if now - s.last_queue <= self.queue_stale]

    # --- salons ---

    def new_code(self):
        if self.fixed_code:
            return None if self.fixed_code in self.rooms else self.fixed_code
        for _ in range(200):
            code = "".join(self.rng.choice(CODE_LETTERS) for _ in range(CODE_LENGTH))
            if code not in self.rooms:
                return code
        return None

    def create(self, session):
        """Crée un salon dont le joueur est l'hôte. Renvoie le salon, ou None s'il n'y a plus de place."""
        room = session.room
        if room is not None and room.host is session and not room.ranked:
            return room  # demande répétée
        self.leave(session, "crée un autre salon")
        if len(self.rooms) >= self.max_rooms:
            return None
        code = self.new_code()
        if code is None:
            return None
        room = Room(code, session, key=self.new_key())
        self.rooms[code] = room
        session.room = room
        self.log("salon %s créé par %r" % (code, session))
        return room

    def new_key(self):
        return self.rng.randrange(1, 0x7FFFFFFF)

    def link(self, session):
        """Liaison directe : ce que le serveur dit à un joueur pour qu'il joigne l'autre sans passer par lui.
        Renvoie (clé du salon, adresse de l'autre vue par le serveur, son adresse locale).

        L'adresse de l'autre ne sert que si les deux joueurs arrivent par le même chemin : tous deux par
        Internet, par le réseau local du serveur, ou sur sa machine. Sinon (un joueur sur le PC du serveur,
        l'autre par Internet) elle ne mènerait nulle part, et le relais ne coûte alors rien : le serveur est
        déjà chez l'un des deux. L'adresse locale n'est donnée qu'à deux joueurs qui sortent par la même
        adresse, donc sans doute par la même box : beaucoup de box ne savent pas se joindre elles-mêmes.
        """
        room = session.room
        peer = room.peer(session) if room is not None else None
        if not self.direct or peer is None or scope(session.addr[0]) != scope(peer.addr[0]):
            return 0, P.NO_ADDR, P.NO_ADDR
        same_exit = session.addr[0] == peer.addr[0]
        return room.key, peer.addr, peer.local_addr if same_exit else P.NO_ADDR

    def join(self, session, code, now):
        """Fait entrer le joueur dans le salon. Renvoie un code P.JOIN_*.

        Un invité muet depuis `stale` secondes cède sa place : son jeu a sans doute planté, et un jeu sans
        compte ne peut pas reprendre sa place tout seul (voir takeover).
        """
        code = code.strip().upper()
        if session in self.queue:
            self.queue.remove(session)  # il a changé d'avis : même si ce code est refusé, il n'attend plus
        room = self.rooms.get(code)
        if room is not None and session.room is room:
            return P.JOIN_OK  # demande répétée
        if room is None or room.ranked:
            return P.JOIN_UNKNOWN  # on n'entre pas dans une partie classée avec son code
        if room.guest is not None:
            if now - room.guest.last_seen < self.stale:
                return P.JOIN_FULL
            self.leave(room.guest, "muet depuis %d s, remplacé" % (now - room.guest.last_seen))
        if room.host.mod_version != session.mod_version:
            return P.JOIN_VERSION
        self.leave(session, "rejoint un autre salon")
        room.guest = session
        session.room = room
        self.log("salon %s : %r rejoint %r" % (code, session, room.host))
        return P.JOIN_OK

    def leave(self, session, why="quitte", silent=False):
        """Sort le joueur de la file d'attente et de son salon. Le salon ferme si c'est l'hôte qui sort, ou
        si c'est une partie classée : l'autre joueur en sort aussi."""
        if session in self.queue:
            self.queue.remove(session)
            self.log("file d'attente : %r sort (%s)" % (session, why))
        room = session.room
        if room is None:
            return
        self.exit_live(session, why, silent)
        session.room = None
        if room.host is session or room.ranked:
            for other in (room.host, room.guest):
                if other is not None:
                    other.room = None
            del self.rooms[room.code]
            self.log("salon %s fermé (%s), %d paquets de match relayés" % (room.code, why, room.relayed))
        else:
            room.guest = None
            self.log("salon %s : %r sort (%s)" % (room.code, session, why))

    def peer(self, session):
        """L'autre joueur du salon, ou None."""
        return session.room.peer(session) if session.room else None

    # --- file d'attente ---

    def enqueue(self, session, now):
        """Partie rapide : le joueur attend un adversaire. Demande répétée par le jeu tant qu'il attend."""
        self.now = now
        if session.player is None:
            return
        room = session.room
        if room is not None:
            if room.ranked:
                return  # adversaire déjà trouvé
            self.leave(session, "cherche une partie rapide")
        session.last_queue = now
        if session not in self.queue:
            session.queue_since = now
            self.queue.append(session)
            self.log("file d'attente : %r entre (%d en attente)" % (session, len(self.queue)))
        self.pair(now)

    def accepted_gap(self, session, now):
        """Écart d'Elo que ce joueur accepte : il grandit sans limite tant qu'il attend."""
        return self.queue_range + self.queue_grow * max(0.0, now - session.queue_since)

    def pair(self, now):
        """Réunit des joueurs de la file d'attente, de même version du mod et d'Elo assez proches.

        On part du joueur qui attend depuis le plus longtemps et on lui donne l'adversaire le plus proche de
        son Elo, pourvu que l'écart tienne dans ce que l'un des deux accepte : celui qui attend depuis
        longtemps finit donc par jouer contre n'importe qui, même si l'autre vient d'arriver.
        """
        waiting = [s for s in self.queue if now - s.last_queue <= self.queue_stale and s.room is None]
        waiting.sort(key=lambda s: s.queue_since)
        paired = set()
        for i, first in enumerate(waiting):
            if first in paired:
                continue
            best, best_gap = None, 0
            for second in waiting[i + 1:]:
                if second in paired or first.mod_version != second.mod_version or first.player is second.player:
                    continue
                gap = abs(first.player.elo - second.player.elo)
                if gap > max(self.accepted_gap(first, now), self.accepted_gap(second, now)):
                    continue
                if best is None or gap < best_gap:
                    best, best_gap = second, gap
            if best is None:
                continue
            code = self.new_code()
            if code is None or len(self.rooms) >= self.max_rooms:
                return
            # L'hôte simule le match et joue sans retard : tiré au sort
            host, guest = (first, best) if self.rng.random() < 0.5 else (best, first)
            room = Room(code, host, ranked=True, key=self.new_key())
            room.guest = guest
            self.rooms[code] = room
            for s in (host, guest):
                s.room = room
                s.report = None
                self.queue.remove(s)
                paired.add(s)
            self.log("partie classée %s : %r (hôte) contre %r, écart d'Elo %d après %d s d'attente" % (
                code, host.player, guest.player, best_gap, now - first.queue_since))

    # --- matchs classés ---

    def match_packet(self, session, now):
        """Ce joueur envoie des paquets de match : le serveur en relaie un, ou le jeu le dit (liaison directe)."""
        session.last_match = now
        room = session.room
        if room.ranked and room.live:
            room.engaged.add(session is room.host)

    def report(self, session, match_id, phase, score_host, score_guest, flags, now):
        """PKT_MATCH : le jeu dit où en est son match."""
        self.now = now
        session.report = (match_id, phase, score_host, score_guest)
        room = session.room
        if room is None or not room.ranked or match_id == 0 or match_id in room.decided:
            return
        peer = room.peer(session)
        peer_report = peer.report if peer is not None else None
        if peer_report is None or peer_report[0] != match_id:
            return  # l'autre jeu n'a pas encore annoncé ce match
        if room.live != match_id:
            if room.live:
                self.void(room, "un autre match commence")
            room.live = match_id
            room.engaged = set()
            room.claim_at = None
            self.log("partie classée %s : match %d en cours" % (room.code, match_id))
        if flags & P.MATCH_LINKED:
            self.match_packet(session, now)

        engaged = len(room.engaged) == 2
        if phase == P.MATCH_ABANDON:
            if engaged:
                self.forfeit(room, session, "abandon")
            else:
                self.void(room, "quitté avant le coup d'envoi")
        elif phase == P.MATCH_PEER_LOST:
            if not engaged:
                self.void(room, "quitté avant le coup d'envoi")
            elif now - peer.last_match >= self.lost_after:
                self.forfeit(room, peer, "ne répond plus")
            elif room.claim_at is None:
                room.claim_at = now  # le jeu redit sa phase chaque seconde : on tranchera à ce moment-là
            elif now - room.claim_at > self.claim_wait:
                self.void(room, "%r dit avoir perdu %r, qui joue encore" % (session, peer))
        elif phase == P.MATCH_FINISHED and peer_report[1] == P.MATCH_FINISHED:
            if peer_report[2:] == (score_host, score_guest):
                self.settle(room, score_host, score_guest)
            else:
                self.void(room, "scores différents : %d-%d et %d-%d" % (score_host, score_guest, peer_report[2], peer_report[3]))

    def exit_live(self, session, why, silent=False):
        """Le joueur sort de son salon (départ, silence, jeu relancé) : s'il y joue un match classé, on tranche."""
        room = session.room
        if room is None or not room.ranked or not room.live:
            return
        peer = room.peer(session)
        mine = session.report if session.report is not None and session.report[0] == room.live else None
        theirs = peer.report if peer is not None and peer.report is not None and peer.report[0] == room.live else None
        if len(room.engaged) < 2:
            self.void(room, "%s avant le coup d'envoi" % why)
        elif theirs is not None and theirs[1] == P.MATCH_FINISHED and mine is not None and mine[2:] == theirs[2:]:
            # l'autre a vu la fin du match, avec le score que ce joueur voyait aussi : le match est joué
            self.settle(room, theirs[2], theirs[3])
        elif mine is not None and mine[1] == P.MATCH_PEER_LOST:
            if self.now - peer.last_match >= self.lost_after:
                self.forfeit(room, peer, "ne répond plus")
            else:
                self.void(room, "%r dit avoir perdu %r, qui joue encore" % (session, peer))
        elif silent and self.now - peer.last_seen > self.lost_after:
            self.void(room, "les deux joueurs ont disparu")
        else:
            self.forfeit(room, session, why)

    def settle(self, room, score_host, score_guest, loser=None, why=""):
        """Compte le match en cours du salon. loser : le joueur forfait."""
        match_id = room.live
        self.close_match(room)
        host, guest = room.host, room.guest
        if self.store is None or host is None or guest is None or host.player is None or guest.player is None:
            return
        goals = (score_host, score_guest)
        if loser is not None:
            # Forfait : défaite d'au moins un but d'écart, ou de l'écart que le joueur subissait en partant
            # (sinon quitter un match mal engagé coûterait moins cher que le finir)
            margin = max(1, score_guest - score_host if loser is host else score_host - score_guest)
            goals = (0, margin) if loser is host else (margin, 0)
        delta_host, delta_guest = self.store.record(host.player, guest.player, score_host, score_guest, goals, loser is not None)
        for session, delta in ((host, delta_host), (guest, delta_guest)):
            how = P.HOW_PLAYED if loser is None else (P.HOW_MY_FORFEIT if session is loser else P.HOW_PEER_FORFEIT)
            session.result = (match_id, P.RESULT_RATED, how, delta)
        self.log("partie classée %s : match %d, %r %d-%d %r%s, Elo %+d et %+d" % (
            room.code, match_id, host.player, score_host, score_guest, guest.player,
            " (forfait de %r : %s)" % (loser.player, why) if loser is not None else "", delta_host, delta_guest))

    def forfeit(self, room, loser, why):
        # Le score retenu est celui que le joueur forfait a lui-même annoncé en dernier
        report = loser.report if loser.report is not None and loser.report[0] == room.live else None
        self.settle(room, report[2] if report else 0, report[3] if report else 0, loser, why)

    def void(self, room, why):
        match_id = room.live
        self.close_match(room)
        for session in (room.host, room.guest):
            if session is not None:
                session.result = (match_id, P.RESULT_VOID, 0, 0)
        self.log("partie classée %s : match %d pas compté (%s)" % (room.code, match_id, why))

    def close_match(self, room):
        room.decided.add(room.live)
        room.live = 0
        room.engaged = set()
        room.claim_at = None
