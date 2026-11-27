"""Serveur du multijoueur en ligne de Pet Soccer : salons à code, partie rapide classée et relais UDP.

    python server\\server.py [--port 7777] [--db postgresql://...] [--lag 80] [--jitter 20] [--loss 5] [--code TEST] [-v]

Le serveur ne simule rien. Un joueur crée un salon et reçoit un code de 4 lettres, l'autre entre ce
code ; ensuite le serveur recopie les paquets de l'un vers l'autre, donc aucun des deux n'ouvre de
port chez lui. Quand c'est possible, les deux jeux se parlent ensuite en direct (le serveur donne à
chacun l'adresse de l'autre) et le relais ne sert plus que de secours. La partie rapide réunit deux
joueurs d'Elo proche dans un salon classé : le résultat change leur Elo en ligne, gardé avec les
comptes dans une base Postgres (--db, ou la variable DATABASE_URL ; schéma dans schema.sql). Chaque match en
ligne y est enregistré avec les tactiques et les statistiques envoyées par le jeu de l'hôte, pour le site
(web/). La base garde aussi les ligues en ligne (leagues.py) : plusieurs amis dans une ligue à 14, les
autres équipes à l'IA, une journée fermée au bout d'un délai.
Un seul port UDP ; seule dépendance : psycopg (pip install "psycopg[binary]"). Détails dans notes/online.md.

  --lag, --jitter, --loss : simulent une mauvaise connexion sur les paquets relayés (tests). Ils
                            coupent la liaison directe, comme --no-direct.
  --queue-range, --queue-grow : écart d'Elo accepté dans la file d'attente, et sa hausse par seconde.
  --code : tous les salons créés portent ce code (tests automatiques : un seul salon à la fois).
  --league-code : de même pour les ligues. --league-minute : secondes que dure une « minute » de délai (tests).
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
from leagues import Leagues
from store import DatabaseError, Store, clean_name, valid_token


class RateLimit:
    """Seau à jetons par adresse IP : `rate` paquets par seconde, `burst` d'un coup. Table de taille fixe (les
    adresses se partagent des cases par leur empreinte) : des millions d'adresses usurpées ne la font pas grossir.
    rate <= 0 : pas de limite."""
    SLOTS = 1 << 16

    def __init__(self, rate, burst):
        self.rate = rate
        self.burst = burst
        self.tokens = [float(burst)] * self.SLOTS
        self.stamp = [0.0] * self.SLOTS

    def allow(self, ip, now):
        if self.rate <= 0:
            return True
        i = hash(ip) & (self.SLOTS - 1)  # hash() des chaînes change à chaque lancement : collisions impossibles à viser
        tokens = min(self.burst, self.tokens[i] + (now - self.stamp[i]) * self.rate)
        self.stamp[i] = now
        if tokens < 1.0:
            self.tokens[i] = tokens
            return False
        self.tokens[i] = tokens - 1.0
        return True


class Server:
    def __init__(self, sock, lobby, lag=0.0, jitter=0.0, loss=0.0, verbose=False, ip_rate=60.0, ip_hello=2.0):
        self.sock = sock
        self.lobby = lobby
        self.packets = RateLimit(ip_rate, ip_rate * 2)  # paquets de salon par adresse IP
        self.hellos = RateLimit(ip_hello, ip_hello * 10)  # nouveaux jeux et lectures du compte dans la base, par IP
        self.refused = 0  # paquets écartés par les limites (journal)
        self.lag = lag  # secondes ajoutées à chaque paquet relayé
        self.jitter = jitter
        self.loss = loss  # part des paquets relayés perdus, 0..1
        self.verbose = verbose
        self.delayed = []  # (heure d'envoi, n° d'ordre, paquet, adresse)
        self.order = itertools.count()
        self.stats = dict(recv=0, relayed=0, lost=0, spectated=0)
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

    def reply(self, request, data, addr):
        """Réponse à une adresse que le serveur n'a pas vérifiée : jamais plus grosse que la demande, sinon une
        adresse usurpée ferait du serveur un amplificateur contre elle."""
        if len(data) <= len(request):
            self.send(data, addr)

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
                   queue_range=self.lobby.accepted_gap(session, self.lobby.now) if queued else 0, secret=session.secret)
        watched = session.watching
        if watched is not None:
            host, guest = watched.host, watched.guest
            return P.make_info(seq, P.STATUS_OK, watched.code, P.ROLE_SPECTATOR, watched.members(), host.name,
                               peer_elo=host.player.elo if host.player else 0, spectators=len(watched.spectators),
                               peer2_name=guest.name if guest else "", **own)
        if room is None:
            return P.make_info(seq, P.STATUS_OK, **own)
        peer = room.peer(session)
        role = P.ROLE_HOST if room.host is session else P.ROLE_GUEST
        if room.ranked:
            own["flags"] |= P.INFO_RANKED
        if room.league is not None:
            own["flags"] |= P.INFO_LEAGUE
        key, peer_addr, peer_local = self.lobby.link(session)
        return P.make_info(seq, P.STATUS_OK, room.code, role, room.members(), peer.name if peer else "",
                           peer_elo=peer.player.elo if peer and peer.player else 0,
                           key=key, peer_addr=peer_addr, peer_local=peer_local,
                           spectators=len(room.spectators), spectator_joins=room.spectator_joins, **own)

    def ranks(self, session):
        """Le haut du classement et le rang du joueur."""
        store = self.lobby.store
        if store is None:
            return P.make_ranks(0, 0, [])
        rows = [(i + 1, p.name, p.elo, p.wins, p.draws, p.losses) for i, p in enumerate(store.top(P.TOP_ROWS))]
        return P.make_ranks(store.rank(session.player) if session.player else 0, store.total(), rows)

    def known_login(self, session, name, token):
        """Vrai si le compte de ce jeu est déjà connu : pas besoin de la base. Elle n'est lue qu'une fois par jeu."""
        return (self.lobby.store is None or not valid_token(token)
                or session is not None and session.player is not None and session.token == token and session.player.name == name)

    def login(self, session, name, token, mod_version):
        """Le compte du joueur qui se présente (créé à son premier passage)."""
        store = self.lobby.store
        if store is None:
            return None
        if session is not None and session.player is not None and session.token == token and session.player.name == name:
            return session.player
        return store.login(token, name, mod_version)

    def hello(self, data, addr, now, client_id, seq):
        """PKT_HELLO : un jeu se présente, reste connu du serveur, ou revient d'une autre adresse avec son secret."""
        lobby = self.lobby
        proto, secret, mod_version, name, token, local_addr = P.read_hello(data)
        if proto not in P.LOBBY_PROTOS:
            self.reply(data, P.make_info(seq, P.STATUS_BAD_PROTO), addr)
            return
        if len(data) < P.LOBBY_MIN_SIZE:
            return  # le jeu complète son bonjour : ainsi la réponse n'est jamais plus grosse (pas d'amplification)
        session = lobby.sessions.get(client_id)
        if session is not None and secret != session.secret:
            # Sans le bon secret, on ne touche à rien. Depuis l'adresse du jeu, c'est sans doute qu'il n'a pas reçu
            # la réponse qui le portait : on la redit (à cette adresse seulement, qui est déjà vérifiée).
            if session.addr == addr:
                self.send(self.info(session, seq), addr)
            return
        if session is None:
            refused = lobby.refuse(addr, now)
            if refused is not None:
                if refused == P.STATUS_BUSY:
                    self.reply(data, P.make_info(seq, P.STATUS_BUSY), addr)
                return
        name = clean_name(name)
        if (session is None or not self.known_login(session, name, token)) and not self.hellos.allow(addr[0], now):
            # nouveaux jeux et écritures dans la base : à petite dose par adresse IP (le jeu redit son bonjour)
            self.refused += 1
            return
        player = self.login(session, name, token, mod_version)
        session = lobby.hello(client_id, addr, name, mod_version, now, player, token, local_addr)
        self.send(self.info(session, seq), addr)

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

        if ptype == P.PKT_SPEC_SNAP:
            # Copie de l'état du match pour les spectateurs : seul l'hôte du salon l'envoie, le serveur la distribue.
            # L'hôte n'en envoie qu'une, quel que soit le nombre de spectateurs (il les voit dans PKT_INFO).
            session = lobby.by_addr.get(addr)
            room = session.room if session is not None else None
            if room is None or room.host is not session:
                return
            session.last_seen = now
            for spectator in room.spectators:
                self.stats["spectated"] += 1
                self.relay(data, spectator.addr, now)
            return

        if ptype not in (P.PKT_HELLO, P.PKT_CREATE, P.PKT_JOIN, P.PKT_LEAVE, P.PKT_STATE, P.PKT_QUEUE, P.PKT_MATCH,
                         P.PKT_TOP, P.PKT_LEAGUE, P.PKT_MATCH_STATS, P.PKT_LIVE, P.PKT_SPECTATE, P.PKT_MATCH_LIVE):
            return
        if not self.packets.allow(addr[0], now):
            self.refused += 1
            return
        client_id, seq = P.read_lobby_head(data)

        if ptype == P.PKT_HELLO:
            self.hello(data, addr, now, client_id, seq)
            return

        if len(data) < P.LOBBY_MIN_SIZE:
            return  # pas un jeu du dialogue 6 (il complète ses paquets) : une réponse serait plus grosse que la demande
        session = lobby.sessions.get(client_id)
        if session is None or session.addr != addr or P.read_secret(data) != session.secret:
            # Serveur relancé, joueur oublié après un long silence, ou jeu qui a changé d'adresse : il doit se
            # présenter de nouveau (avec son secret). Un paquet usurpé reçoit la même réponse et ne change rien.
            self.reply(data, P.make_info(seq, P.STATUS_UNKNOWN), addr)
            return
        lobby.seen(session, now)

        if ptype == P.PKT_LEAGUE:
            self.league(session, seq, data, now)
            return
        if ptype == P.PKT_MATCH_STATS:
            # pas de réponse : le jeu redit ses statistiques avec PKT_MATCH, qui a la sienne
            lobby.match_stats(session, P.read_match_stats(data))
            return
        if ptype == P.PKT_MATCH_LIVE:
            lobby.match_live(session, P.read_match_live(data))  # pas de réponse non plus
            return

        join_error = P.JOIN_OK
        if ptype == P.PKT_CREATE:
            lobby.create(session)
        elif ptype == P.PKT_JOIN:
            join_error = lobby.join(session, P.read_join(data), now)
        elif ptype == P.PKT_LEAVE:
            lobby.leave(session)
        elif ptype == P.PKT_SPECTATE:
            join_error = lobby.spectate(session, *P.read_spectate(data))
        elif ptype == P.PKT_LIVE:
            self.send(P.make_live_list(lobby.live_matches()), addr)
        elif ptype == P.PKT_STATE:
            peer = lobby.peer(session)
            if peer is not None:
                self.relay(P.make_peer(data[P.LOBBY_HEAD_SIZE:]), peer.addr, now)
            room = session.room
            if room is not None and room.host is session:
                # les spectateurs chargent le match de l'hôte comme l'invité : stade, équipes, numéro du match
                for spectator in room.spectators:
                    self.relay(P.make_peer(data[P.LOBBY_HEAD_SIZE:]), spectator.addr, now)
            try:
                lobby.set_state(session, P.read_state(data[P.LOBBY_HEAD_SIZE:]))
            except (ValueError, struct.error):
                pass  # relayé quand même : c'est l'autre jeu qui le lit
        elif ptype == P.PKT_QUEUE:
            lobby.enqueue(session, now)
        elif ptype == P.PKT_MATCH:
            lobby.report(session, *P.read_match(data), now)
        elif ptype == P.PKT_TOP:
            self.send(self.ranks(session), addr)
        self.send(self.info(session, seq, join_error), addr)

    def presence(self, player_id):
        """Drapeaux LGM_* d'un membre d'une ligue : connecté, dans un salon."""
        session = self.lobby.by_player.get(player_id)
        if session is None or self.lobby.now - session.last_seen > 10.0:
            return 0
        return P.LGM_ONLINE | (P.LGM_BUSY if session.room is not None else 0)

    def league(self, session, seq, data, now):
        """PKT_LEAGUE : une commande de la ligue en ligne, et la ligue du joueur en réponse."""
        leagues = self.lobby.leagues
        cmd, known, args = P.read_league(data)
        if leagues is None:
            self.send(P.make_league_info(seq, P.LGE_LIMIT), session.addr)
            return
        wall = leagues.clock()
        error = leagues.command(session.player, session.name, cmd, args, wall)
        if cmd == P.LG_PLAY and error == P.LGE_NONE:
            error = self.lobby.league_play(session, now)
        self.send(P.make_league_info(seq, error, leagues.view(session.player, known, wall, self.presence)), session.addr)

    def write_status(self):
        if self.lobby.store is not None:
            self.lobby.store.write_status(*self.lobby.status())

    def write_live(self):
        """Matchs en cours pour la page « En direct » du site ; rien à écrire si la table est déjà vide."""
        if self.lobby.store is None:
            return
        rows = self.lobby.live_rows()
        if rows or self.live_written:
            self.lobby.store.write_live(rows)
        self.live_written = bool(rows)

    def run(self):
        next_expire = next_stats = next_status = next_live = time.monotonic()
        self.live_written = True  # vide au démarrage les matchs d'un serveur précédent
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
                except DatabaseError as e:
                    self.log("ERREUR base (paquet %d de %s:%d) : %s" % (data[4] if len(data) > 4 else -1, addr[0], addr[1], e))
                except Exception as e:
                    # un paquet imprévu ne doit pas arrêter le serveur (et déconnecter tout le monde)
                    self.log("ERREUR paquet %d de %s:%d : %r" % (data[4] if len(data) > 4 else -1, addr[0], addr[1], e))
            self.flush(now)

            try:
                if now >= next_expire:
                    next_expire = now + 1.0
                    self.lobby.expire(now)
                    if self.lobby.leagues is not None:
                        self.lobby.leagues.tick(self.lobby.leagues.clock(), self.lobby.league_busy())
                if now >= next_status:
                    next_status = now + 10.0
                    self.write_status()
                if now >= next_live:
                    next_live = now + 3.0
                    self.write_live()
            except DatabaseError as e:
                self.log("ERREUR base : %s" % e)
            except Exception as e:
                self.log("ERREUR : %r" % e)
            if now >= next_stats:
                next_stats = now + 10.0
                if self.refused:
                    self.log("limites : %d paquets écartés en 10 s" % self.refused)
                    self.refused = 0
                if self.verbose and self.stats["recv"]:
                    self.log("10 s : %d paquets reçus, %d relayés (%d perdus exprès), %d aux spectateurs, %d joueurs, %d salons, %d en attente" % (
                        self.stats["recv"], self.stats["relayed"], self.stats["lost"], self.stats["spectated"],
                        len(self.lobby.sessions), len(self.lobby.rooms), len(self.lobby.queue)))
                self.stats = dict(recv=0, relayed=0, lost=0, spectated=0)


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


def hide_password(url):
    """L'adresse de la base sans son mot de passe, pour le journal."""
    head, sep, tail = url.rpartition("@")
    if not sep or ":" not in head.split("//", 1)[-1]:
        return url
    scheme, _, auth = head.partition("//")
    return "%s//%s:***@%s" % (scheme, auth.split(":", 1)[0], tail)


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")  # journal lisible même redirigé vers un fichier
    ap = argparse.ArgumentParser(description="Serveur du multijoueur en ligne de Pet Soccer (salons, partie rapide, relais UDP).")
    ap.add_argument("--db", default=os.environ.get("DATABASE_URL"),
                    help="base Postgres des comptes, de l'Elo et des matchs (postgresql://utilisateur:mot de passe@hôte:port/base)."
                         " Par défaut : la variable DATABASE_URL")
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
    ap.add_argument("--league-code", default=None, help="code imposé aux ligues créées (tests : une seule ligue)")
    ap.add_argument("--league-minute", type=float, default=60, help="secondes que dure une minute de délai d'une journée de ligue (tests)")
    ap.add_argument("--league-grace", type=float, default=20, help="minutes pendant lesquelles un match de ligue contre l'IA en cours échappe au délai")
    ap.add_argument("--no-direct", action="store_true", help="tout le match passe par le serveur : pas de liaison directe entre les jeux")
    ap.add_argument("--max-sessions", type=int, default=5000, help="jeux connus en même temps, en tout")
    ap.add_argument("--ip-sessions", type=int, default=16, help="jeux connus en même temps depuis une même adresse IP")
    ap.add_argument("--max-spectators", type=int, default=8,
                    help="spectateurs par match classé ou de ligue (chacun coûte au serveur environ 16 Ko/s)")
    ap.add_argument("--ip-rate", type=float, default=60,
                    help="paquets de salon par seconde et par adresse IP (le double d'un coup) ; 0 : sans limite")
    ap.add_argument("--ip-hello", type=float, default=2,
                    help="nouveaux jeux (et lectures de compte dans la base) par seconde et par adresse IP (dix fois plus"
                         " d'un coup) ; 0 : sans limite")
    ap.add_argument("-v", "--verbose", action="store_true", help="statistiques toutes les 10 s, paquets illisibles")
    args = ap.parse_args()
    if not args.db:
        ap.error("donner la base : --db postgresql://... ou la variable DATABASE_URL")

    code = args.code.strip().upper() if args.code else None
    store = Store(args.db, log=lambda text: print(time.strftime("%H:%M:%S ") + text, flush=True))
    lobby = Lobby(fixed_code=code, timeout=args.timeout, stale=args.stale, store=store,
                  lost_after=args.lost, claim_wait=args.claim, takeover_after=args.takeover,
                  queue_range=args.queue_range, queue_grow=args.queue_grow,
                  # un retard ou des pertes simulés ne touchent que les paquets relayés : pas de liaison directe alors
                  direct=not (args.no_direct or args.lag or args.jitter or args.loss),
                  max_sessions=args.max_sessions, ip_sessions=args.ip_sessions, max_spectators=args.max_spectators)
    lobby.leagues = Leagues(store.db, fixed_code=args.league_code.strip().upper() if args.league_code else None,
                            delay_scale=args.league_minute, grace=args.league_grace * 60)
    sock = open_socket(args.host, args.port)
    server = Server(sock, lobby, args.lag / 1000.0, args.jitter / 1000.0, args.loss / 100.0, args.verbose,
                    ip_rate=args.ip_rate, ip_hello=args.ip_hello)
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
    lobby.leagues.log = server.log
    server.log("base : %s (%d joueurs classés, %d ligues en cours)" % (hide_password(args.db), store.total(), len(lobby.leagues.by_code)))
    try:
        server.run()
    except KeyboardInterrupt:
        server.log("arrêt")
    finally:
        store.close()


if __name__ == "__main__":
    main()
