"""Serveur du multijoueur en ligne de Pet Soccer : salons à code, partie rapide classée et relais UDP.

    python server\\server.py [--port 7777] [--db fichier] [--lag 80] [--jitter 20] [--loss 5] [--code TEST] [-v]

Le serveur ne simule rien. Un joueur crée un salon et reçoit un code de 4 lettres, l'autre entre ce
code ; ensuite le serveur recopie les paquets de l'un vers l'autre, donc aucun des deux n'ouvre de
port chez lui. Quand c'est possible, les deux jeux se parlent ensuite en direct (le serveur donne à
chacun l'adresse de l'autre) et le relais ne sert plus que de secours. La partie rapide réunit deux
joueurs d'Elo proche dans un salon classé : le résultat change leur Elo en ligne, gardé avec les
comptes dans un fichier SQLite (--db).
Un seul port UDP, bibliothèque standard seule. Détails dans notes/online.md.

  --lag, --jitter, --loss : simulent une mauvaise connexion sur les paquets relayés (tests). Ils
                            coupent la liaison directe, comme --no-direct.
  --queue-range, --queue-grow : écart d'Elo accepté dans la file d'attente, et sa hausse par seconde.
  --code : tous les salons créés portent ce code (tests automatiques : un seul salon à la fois).
"""
import argparse
import heapq
import itertools
import os
import random
import socket
import struct
import sys
import time

import protocol as P
from rooms import Lobby
from store import Store, clean_name


class Server:
    def __init__(self, sock, lobby, lag=0.0, jitter=0.0, loss=0.0, verbose=False):
        self.sock = sock
        self.lobby = lobby
        self.lag = lag  # secondes ajoutées à chaque paquet relayé
        self.jitter = jitter
        self.loss = loss  # part des paquets relayés perdus, 0..1
        self.verbose = verbose
        self.delayed = []  # (heure d'envoi, n° d'ordre, paquet, adresse)
        self.order = itertools.count()
        self.stats = dict(recv=0, relayed=0, lost=0)
        lobby.log = self.log

    def log(self, text):
        print(time.strftime("%H:%M:%S ") + text, flush=True)

    def send(self, data, addr):
        try:
            self.sock.sendto(data, addr)
        except OSError as e:
            if self.verbose:
                self.log("envoi impossible vers %s:%d : %s" % (addr[0], addr[1], e))

    def relay(self, data, addr, now):
        """Envoie un paquet d'un joueur à l'autre, avec le retard et les pertes demandés."""
        self.stats["relayed"] += 1
        if self.loss > 0 and random.random() < self.loss:
            self.stats["lost"] += 1
            return
        delay = self.lag + (random.uniform(0, self.jitter) if self.jitter > 0 else 0)
        if delay <= 0:
            self.send(data, addr)
        else:
            heapq.heappush(self.delayed, (now + delay, next(self.order), data, addr))

    def flush(self, now):
        while self.delayed and self.delayed[0][0] <= now:
            _, _, data, addr = heapq.heappop(self.delayed)
            self.send(data, addr)

    def info(self, session, seq, join_error=P.JOIN_OK):
        room = session.room
        player = session.player
        queued = session in self.lobby.queue
        own = dict(join_error=join_error, flags=P.INFO_QUEUED if queued else 0,
                   elo=player.elo if player else 0, games=player.games if player else 0,
                   online=len(self.lobby.sessions), result=session.result,
                   queue_range=self.lobby.accepted_gap(session, self.lobby.now) if queued else 0)
        if room is None:
            return P.make_info(seq, P.STATUS_OK, **own)
        peer = room.peer(session)
        role = P.ROLE_HOST if room.host is session else P.ROLE_GUEST
        if room.ranked:
            own["flags"] |= P.INFO_RANKED
        key, peer_addr, peer_local = self.lobby.link(session)
        return P.make_info(seq, P.STATUS_OK, room.code, role, room.members(), peer.name if peer else "",
                           peer_elo=peer.player.elo if peer and peer.player else 0,
                           key=key, peer_addr=peer_addr, peer_local=peer_local, **own)

    def ranks(self, session):
        """Le haut du classement et le rang du joueur."""
        store = self.lobby.store
        if store is None:
            return P.make_ranks(0, 0, [])
        rows = [(i + 1, p.name, p.elo, p.wins, p.draws, p.losses) for i, p in enumerate(store.top(P.TOP_ROWS))]
        return P.make_ranks(store.rank(session.player) if session.player else 0, store.total(), rows)

    def login(self, client_id, name, token):
        """Le compte du joueur qui se présente. Lu dans la base une fois par jeu, pas à chaque bonjour."""
        store = self.lobby.store
        if store is None:
            return None
        known = self.lobby.sessions.get(client_id)
        if known is not None and known.player is not None and known.token == token and known.player.name == name:
            return known.player
        return store.login(token, name)

    def handle(self, data, addr, now):
        ptype = P.read_head(data)
        if ptype is None:
            return
        self.stats["recv"] += 1
        lobby = self.lobby

        if ptype in (P.PKT_INPUT, P.PKT_SNAP, P.PKT_BYE):
            # Paquet de match : recopié tel quel à l'autre joueur du salon. En liaison directe les deux jeux se
            # les envoient sans passer par ici, et y reviennent si ce chemin casse.
            session = lobby.by_addr.get(addr)
            if session is None or session.room is None:
                return
            session.last_seen = now
            lobby.match_packet(session, now)
            peer = lobby.peer(session)
            if peer is not None:
                session.room.relayed += 1
                self.relay(data, peer.addr, now)
            return

        if ptype not in (P.PKT_HELLO, P.PKT_CREATE, P.PKT_JOIN, P.PKT_LEAVE, P.PKT_STATE, P.PKT_QUEUE, P.PKT_MATCH,
                         P.PKT_TOP):
            return
        client_id, seq = P.read_lobby_head(data)

        if ptype == P.PKT_HELLO:
            proto, mod_version, name, token, local_addr = P.read_hello(data)
            if proto != P.LOBBY_PROTO:
                self.send(P.make_info(seq, P.STATUS_BAD_PROTO), addr)
                return
            name = clean_name(name)
            session = lobby.hello(client_id, addr, name, mod_version, now, self.login(client_id, name, token), token,
                                  local_addr)
            self.send(self.info(session, seq), addr)
            return

        session = lobby.sessions.get(client_id)
        if session is None:
            # Serveur relancé, ou joueur oublié après un long silence : il doit se présenter de nouveau
            self.send(P.make_info(seq, P.STATUS_UNKNOWN), addr)
            return
        lobby.touch(session, addr, now)

        join_error = P.JOIN_OK
        if ptype == P.PKT_CREATE:
            lobby.create(session)
        elif ptype == P.PKT_JOIN:
            join_error = lobby.join(session, P.read_join(data), now)
        elif ptype == P.PKT_LEAVE:
            lobby.leave(session)
        elif ptype == P.PKT_STATE:
            peer = lobby.peer(session)
            if peer is not None:
                self.relay(P.make_peer(data[P.LOBBY_HEAD_SIZE:]), peer.addr, now)
        elif ptype == P.PKT_QUEUE:
            lobby.enqueue(session, now)
        elif ptype == P.PKT_MATCH:
            lobby.report(session, *P.read_match(data), now)
        elif ptype == P.PKT_TOP:
            self.send(self.ranks(session), addr)
        self.send(self.info(session, seq, join_error), addr)

    def run(self):
        next_expire = next_stats = time.monotonic()
        while True:
            now = time.monotonic()
            wait = 0.5
            if self.delayed:
                wait = max(0.0, min(wait, self.delayed[0][0] - now))
            self.sock.settimeout(wait if wait > 0 else 0.0001)
            try:
                data, addr = self.sock.recvfrom(4096)
            except socket.timeout:
                data = None
            except ConnectionResetError:
                data = None  # Windows : un joueur a fermé son jeu, son port ne répond plus
            except OSError as e:
                self.log("erreur de lecture : %s" % e)
                data = None

            now = time.monotonic()
            if data is not None:
                try:
                    self.handle(data, addr, now)
                except (ValueError, IndexError, struct.error) as e:
                    if self.verbose:
                        self.log("paquet illisible de %s:%d : %s" % (addr[0], addr[1], e))
            self.flush(now)

            if now >= next_expire:
                next_expire = now + 1.0
                self.lobby.expire(now)
            if self.verbose and now >= next_stats:
                next_stats = now + 10.0
                if self.stats["recv"]:
                    self.log("10 s : %d paquets reçus, %d relayés (%d perdus exprès), %d joueurs, %d salons, %d en attente" % (
                        self.stats["recv"], self.stats["relayed"], self.stats["lost"],
                        len(self.lobby.sessions), len(self.lobby.rooms), len(self.lobby.queue)))
                self.stats = dict(recv=0, relayed=0, lost=0)


def open_socket(host, port):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    if sys.platform == "win32":
        # Sans cela, un envoi vers un port fermé fait échouer les lectures suivantes (WSAECONNRESET)
        try:
            sock.ioctl(getattr(socket, "SIO_UDP_CONNRESET", 0x9800000C), False)
        except (OSError, ValueError):
            pass
    sock.bind((host, port))
    return sock


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")  # journal lisible même redirigé vers un fichier
    ap = argparse.ArgumentParser(description="Serveur du multijoueur en ligne de Pet Soccer (salons, partie rapide, relais UDP).")
    ap.add_argument("--db", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "players.db"),
                    help="fichier SQLite des comptes et de l'Elo en ligne (players.db à côté de server.py par défaut)")
    ap.add_argument("--host", default="0.0.0.0", help="adresse d'écoute (0.0.0.0 : toutes)")
    ap.add_argument("--port", type=int, default=7777, help="port UDP (7777 par défaut)")
    ap.add_argument("--lag", type=float, default=0, help="retard ajouté à chaque paquet relayé, en ms")
    ap.add_argument("--jitter", type=float, default=0, help="retard aléatoire en plus, de 0 à N ms")
    ap.add_argument("--loss", type=float, default=0, help="pourcentage de paquets relayés perdus")
    ap.add_argument("--code", default=None, help="code imposé aux salons créés (tests : un seul salon)")
    ap.add_argument("--timeout", type=float, default=30, help="secondes de silence avant d'oublier un joueur")
    ap.add_argument("--stale", type=float, default=10, help="secondes de silence avant qu'un invité puisse être remplacé")
    ap.add_argument("--lost", type=float, default=4, help="secondes sans paquet de match avant qu'un joueur puisse être déclaré forfait")
    ap.add_argument("--claim", type=float, default=6, help="secondes laissées pour confirmer qu'un joueur a disparu, avant d'annuler le match")
    ap.add_argument("--takeover", type=float, default=3, help="secondes de silence avant qu'un jeu relancé reprenne la place de l'ancien")
    ap.add_argument("--queue-range", type=float, default=100, help="partie rapide : écart d'Elo accepté en entrant dans la file d'attente")
    ap.add_argument("--queue-grow", type=float, default=10, help="partie rapide : points d'Elo ajoutés à cet écart par seconde d'attente")
    ap.add_argument("--no-direct", action="store_true", help="tout le match passe par le serveur : pas de liaison directe entre les jeux")
    ap.add_argument("-v", "--verbose", action="store_true", help="statistiques toutes les 10 s, paquets illisibles")
    args = ap.parse_args()

    code = args.code.strip().upper() if args.code else None
    store = Store(args.db)
    lobby = Lobby(fixed_code=code, timeout=args.timeout, stale=args.stale, store=store,
                  lost_after=args.lost, claim_wait=args.claim, takeover_after=args.takeover,
                  queue_range=args.queue_range, queue_grow=args.queue_grow,
                  # un retard ou des pertes simulés ne touchent que les paquets relayés : pas de liaison directe alors
                  direct=not (args.no_direct or args.lag or args.jitter or args.loss))
    sock = open_socket(args.host, args.port)
    server = Server(sock, lobby, args.lag / 1000.0, args.jitter / 1000.0, args.loss / 100.0, args.verbose)
    extras = []
    if not lobby.direct:
        extras.append("sans liaison directe")
    if args.lag or args.jitter:
        extras.append("retard %g ms + 0 à %g ms" % (args.lag, args.jitter))
    if args.loss:
        extras.append("pertes %g %%" % args.loss)
    if code:
        extras.append("code imposé %s" % code)
    server.log("serveur Pet Soccer sur le port UDP %d%s" % (args.port, " (" + ", ".join(extras) + ")" if extras else ""))
    server.log("comptes et Elo : %s (%d joueurs classés)" % (args.db, store.total()))
    try:
        server.run()
    except KeyboardInterrupt:
        server.log("arrêt")
    finally:
        store.close()


if __name__ == "__main__":
    main()
