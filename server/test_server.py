"""Test du serveur sans le jeu : lance server.py sur un port libre et joue le rôle de plusieurs jeux.

    python server\\test_server.py

Affiche « OK » pour chaque point vérifié et sort avec le code 1 au premier échec.
"""
import os
import random
import socket
import shutil
import struct
import subprocess
import sys
import tempfile
import time
import types

import protocol as P
import rooms

HERE = os.path.dirname(os.path.abspath(__file__))
MOD_VERSION = 4


class Game:
    """Un faux jeu : une socket UDP et un identifiant."""

    def __init__(self, port, client_id, name, mod_version=MOD_VERSION, token=None, local=P.NO_ADDR):
        self.server = ("127.0.0.1", port)
        self.local = local  # adresse que le jeu annonce pour son réseau local
        self.client_id = client_id
        self.name = name
        self.mod_version = mod_version
        self.token = token or "jeton%027d" % client_id  # le compte du joueur
        self.seq = 0
        self.new_socket()

    def new_socket(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind(("127.0.0.1", 0))
        self.sock.settimeout(1.0)

    def recv(self, wanted):
        """Prochain paquet du type demandé (les autres sont ignorés), ou None après 1 s."""
        end = time.time() + 1.0
        while time.time() < end:
            try:
                data, _ = self.sock.recvfrom(4096)
            except (socket.timeout, ConnectionResetError):
                return None
            if P.read_head(data) == wanted:
                return data
        return None

    @staticmethod
    def info(data):
        """PKT_INFO décodé. Un refus (autre version, jeu inconnu) n'a que son état."""
        status = data[P.HEAD_SIZE + 4]
        return P.read_info(data) if status == P.STATUS_OK else dict(seq=struct.unpack_from(">i", data, P.HEAD_SIZE)[0], status=status)

    def ask(self, ptype, body=b""):
        """Envoie un paquet de salon et renvoie la réponse du serveur (PKT_INFO décodé)."""
        self.seq += 1
        self.sock.sendto(P.make_lobby(ptype, self.client_id, self.seq, body), self.server)
        while True:
            data = self.recv(P.PKT_INFO)
            if data is None:
                return None
            info = self.info(data)
            if info["seq"] == self.seq:
                return info

    def hello(self, proto=P.LOBBY_PROTO):
        self.seq += 1
        self.sock.sendto(P.make_hello(self.client_id, self.seq, self.name, self.mod_version, self.token, proto, self.local),
                         self.server)
        data = self.recv(P.PKT_INFO)
        return self.info(data) if data else None

    def report(self, match_id, phase, score_host=0, score_guest=0, flags=0):
        """Annonce où en est le match (PKT_MATCH) et renvoie la réponse du serveur."""
        return self.ask(P.PKT_MATCH, P.make_match(match_id, phase, score_host, score_guest, flags))

    def top(self):
        self.seq += 1
        self.sock.sendto(P.make_lobby(P.PKT_TOP, self.client_id, self.seq), self.server)
        data = self.recv(P.PKT_RANKS)
        return P.read_ranks(data) if data else None

    def match(self, ptype, body):
        self.sock.sendto(struct.pack(">IB", P.MAGIC, ptype) + body, self.server)

    def league(self, cmd, known=0, **kw):
        """Commande de ligue (PKT_LEAGUE) ; renvoie la réponse décodée (PKT_LEAGUE_INFO)."""
        self.seq += 1
        self.sock.sendto(P.make_league(self.client_id, self.seq, cmd, known, **kw), self.server)
        while True:
            data = self.recv(P.PKT_LEAGUE_INFO)
            if data is None:
                return None
            info = P.read_league_info(data)
            if info["seq"] == self.seq:
                return info


checks = 0


def check(what, ok):
    global checks
    checks += 1
    print(("OK    " if ok else "ECHEC ") + what)
    if not ok:
        raise SystemExit(1)


def free_port():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.bind(("127.0.0.1", 0))
    port = s.getsockname()[1]
    s.close()
    return port


def start_server(port, *args):
    proc = subprocess.Popen([sys.executable, os.path.join(HERE, "server.py"), "--host", "127.0.0.1", "--port", str(port),
                             "--db", os.path.join(TMP, "test.db")]
                            + list(args), stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    probe = Game(port, 999, "sonde", token="x")  # jeton refusé : la sonde n'a pas de compte
    for _ in range(30):
        if probe.hello() is not None:
            return proc
        time.sleep(0.1)
    proc.kill()
    raise SystemExit("le serveur ne répond pas")


def test_rooms(port):
    a, b, c = Game(port, 1, "Alice"), Game(port, 2, "Bob", local=("192.168.1.20", 4321)), Game(port, 3, "Chloé")
    old = Game(port, 4, "Vieux", mod_version=MOD_VERSION - 1)

    info = a.ask(P.PKT_CREATE)
    check("un jeu inconnu doit d'abord se présenter", info and info["status"] == P.STATUS_UNKNOWN)
    info = a.hello(proto=P.LOBBY_PROTO + 1)
    check("version du dialogue différente : refusé", info and info["status"] == P.STATUS_BAD_PROTO)
    info = Game(port, 5, "Ancien").hello(proto=3)
    check("un jeu d'avant la ligue en ligne (dialogue 3) est accepté", info and info["status"] == P.STATUS_OK)
    info = a.hello()
    check("bonjour accepté, pas encore de salon", info and info["status"] == P.STATUS_OK and info["code"] == "")
    for g in (b, c, old):
        g.hello()

    info = a.ask(P.PKT_CREATE)
    code = info["code"]
    check("salon créé avec un code de 4 lettres (%s)" % code,
          len(code) == 4 and code.isalpha() and code.isupper() and info["role"] == P.ROLE_HOST and info["members"] == 1)
    check("seul dans le salon : pas d'adresse à joindre", info["key"] == 0 and info["peer_addr"] == P.NO_ADDR)
    check("créer deux fois rend le même salon", a.ask(P.PKT_CREATE)["code"] == code)

    wrong = "ZZZZ" if code != "ZZZZ" else "YYYY"
    check("code inconnu refusé", b.ask(P.PKT_JOIN, P.put_string(wrong))["join_error"] == P.JOIN_UNKNOWN)
    check("version du mod différente refusée", old.ask(P.PKT_JOIN, P.put_string(code))["join_error"] == P.JOIN_VERSION)
    info = b.ask(P.PKT_JOIN, P.put_string(code.lower()))
    check("l'invité entre avec le code en minuscules",
          info["join_error"] == P.JOIN_OK and info["code"] == code and info["role"] == P.ROLE_GUEST
          and info["members"] == 2 and info["peer_name"] == "Alice")
    info = a.ask(P.PKT_STATE, b"etat de A")
    check("l'hôte voit l'invité", info["members"] == 2 and info["peer_name"] == "Bob")
    info_b = b.ask(P.PKT_STATE, b"etat de B")
    a.recv(P.PKT_PEER)
    check("liaison directe : chacun reçoit l'adresse de l'autre et la même clé",
          info["key"] != 0 and info["key"] == info_b["key"] and info["peer_addr"] == b.sock.getsockname()
          and info_b["peer_addr"] == a.sock.getsockname())
    check("... et son adresse locale s'il l'a annoncée, puisqu'ils sortent par la même adresse",
          info["peer_local"] == b.local and info_b["peer_local"] == P.NO_ADDR)
    a.ask(P.PKT_STATE, b"etat de A")  # l'invité a jeté le premier en attendant sa réponse
    check("salon plein refusé", c.ask(P.PKT_JOIN, P.put_string(code))["join_error"] == P.JOIN_FULL)

    data = b.recv(P.PKT_PEER)
    check("l'état de l'hôte arrive chez l'invité", data is not None and data[P.HEAD_SIZE:] == b"etat de A")
    b.ask(P.PKT_STATE, b"etat de B")
    data = a.recv(P.PKT_PEER)
    check("l'état de l'invité arrive chez l'hôte", data is not None and data[P.HEAD_SIZE:] == b"etat de B")

    a.match(P.PKT_SNAP, b"x" * 500)
    data = b.recv(P.PKT_SNAP)
    check("un état du match est relayé tel quel", data == struct.pack(">IB", P.MAGIC, P.PKT_SNAP) + b"x" * 500)
    b.match(P.PKT_INPUT, b"touches")
    data = a.recv(P.PKT_INPUT)
    check("les touches sont relayées tel quel", data == struct.pack(">IB", P.MAGIC, P.PKT_INPUT) + b"touches")
    c.match(P.PKT_INPUT, b"intrus")
    check("un joueur hors du salon n'est pas relayé", a.recv(P.PKT_INPUT) is None)

    b.new_socket()  # la box de l'invité change son port de sortie
    b.ask(P.PKT_STATE, b"etat de B")
    a.recv(P.PKT_PEER)
    a.match(P.PKT_SNAP, b"apres")
    check("le relais suit la nouvelle adresse de l'invité", b.recv(P.PKT_SNAP) is not None)

    b.ask(P.PKT_LEAVE)
    check("l'invité sort, l'hôte reste seul", a.ask(P.PKT_STATE, b"")["members"] == 1)
    check("un autre invité peut entrer", c.ask(P.PKT_JOIN, P.put_string(code))["join_error"] == P.JOIN_OK)
    a.ask(P.PKT_LEAVE)
    info = c.ask(P.PKT_STATE, b"")
    check("l'hôte sort : le salon ferme pour l'invité", info["code"] == "" and info["role"] == P.ROLE_NONE)
    check("le code du salon fermé n'existe plus", b.ask(P.PKT_JOIN, P.put_string(code))["join_error"] == P.JOIN_UNKNOWN)


def test_timeout(port):
    a, b = Game(port, 11, "Alice"), Game(port, 12, "Bob")
    a.hello()
    b.hello()
    code = a.ask(P.PKT_CREATE)["code"]
    check("code imposé par --code", code == "TEST")
    b.ask(P.PKT_JOIN, P.put_string(code))
    for _ in range(8):  # l'hôte se tait, l'invité continue de parler
        time.sleep(0.5)
        info = b.ask(P.PKT_STATE, b"")
    check("hôte silencieux oublié : le salon ferme", info["code"] == "")
    check("l'hôte oublié doit se présenter de nouveau", a.ask(P.PKT_STATE, b"")["status"] == P.STATUS_UNKNOWN)


def test_stale(port):
    a, b, c = Game(port, 31, "Alice"), Game(port, 32, "Bob"), Game(port, 33, "Bob revenu")
    for g in (a, b, c):
        g.hello()
    code = a.ask(P.PKT_CREATE)["code"]
    b.ask(P.PKT_JOIN, P.put_string(code))
    check("invité présent : sa place est gardée", c.ask(P.PKT_JOIN, P.put_string(code))["join_error"] == P.JOIN_FULL)
    for _ in range(4):  # l'invité se tait (jeu planté), l'hôte continue de parler
        time.sleep(0.5)
        a.ask(P.PKT_STATE, b"")
    info = c.ask(P.PKT_JOIN, P.put_string(code))
    check("invité muet : un autre jeu prend sa place", info["join_error"] == P.JOIN_OK and info["role"] == P.ROLE_GUEST)
    check("l'ancien invité n'est plus dans le salon", b.ask(P.PKT_STATE, b"")["code"] == "")
    check("l'hôte voit le nouvel invité", a.ask(P.PKT_STATE, b"")["peer_name"] == "Bob revenu")


def test_lag(port):
    a, b = Game(port, 21, "Alice"), Game(port, 22, "Bob")
    a.hello()
    b.hello()
    code = a.ask(P.PKT_CREATE)["code"]
    info = b.ask(P.PKT_JOIN, P.put_string(code))
    check("retard simulé : pas de liaison directe, tout passe par le serveur",
          info["key"] == 0 and info["peer_addr"] == P.NO_ADDR)
    start = time.time()
    for _ in range(12):  # la moitié se perd (--loss 50) : on mesure le premier arrivé
        a.match(P.PKT_SNAP, b"lent")
    data = b.recv(P.PKT_SNAP)
    delay = (time.time() - start) * 1000
    check("--lag 150 : paquet relayé après %.0f ms" % delay, data is not None and 120 <= delay <= 400)
    b.sock.settimeout(0.6)
    while b.recv(P.PKT_SNAP) is not None:  # vide les paquets de la mesure précédente
        pass
    got = 0
    for i in range(200):
        a.match(P.PKT_SNAP, struct.pack(">i", i))
    b.sock.settimeout(0.6)
    while b.recv(P.PKT_SNAP) is not None:
        got += 1
    check("--loss 50 : %d paquets sur 200 arrivés" % got, 60 <= got <= 140)


# ------------------------------------------------------------------ partie rapide, Elo, classement (phase 4)

def pair(port, id_a, id_b, name_a="Alice", name_b="Bob"):
    """Deux joueurs passent par la file d'attente. Renvoie (hôte, invité), tirés au sort par le serveur."""
    a, b = Game(port, id_a, name_a), Game(port, id_b, name_b)
    a.hello()
    b.hello()
    a.ask(P.PKT_QUEUE)
    info_b = b.ask(P.PKT_QUEUE)
    return (b, a) if info_b["role"] == P.ROLE_HOST else (a, b)


def play(host, guest, match_id):
    """Les deux jeux annoncent le match, puis s'échangent des paquets : le match a vraiment commencé."""
    host.report(match_id, P.MATCH_PLAYING)
    guest.report(match_id, P.MATCH_PLAYING)
    guest.match(P.PKT_INPUT, b"touches")
    host.recv(P.PKT_INPUT)
    host.match(P.PKT_SNAP, b"etat")
    guest.recv(P.PKT_SNAP)


def test_ranked(port):
    a, b, c = Game(port, 41, "Alice"), Game(port, 42, "Bob"), Game(port, 43, "Chloé")
    info = a.hello()
    check("premier passage : compte créé à 1000 Elo, aucun match", info["elo"] == 1000 and info["games"] == 0)
    b.hello()
    c.hello()
    info = a.ask(P.PKT_QUEUE)
    check("seul dans la file : en attente, pas de salon", info["queued"] and info["code"] == "")
    a2 = Game(port, 44, "Alice", token=a.token)
    a2.hello()
    check("un joueur n'est pas opposé à lui-même", a2.ask(P.PKT_QUEUE)["code"] == "" and a.ask(P.PKT_QUEUE)["code"] == "")
    a2.ask(P.PKT_LEAVE)
    info_b = b.ask(P.PKT_QUEUE)
    info_a = a.ask(P.PKT_QUEUE)
    check("deux joueurs dans la file : un salon classé, un hôte et un invité",
          info_a["code"] == info_b["code"] != "" and info_a["ranked"] and info_b["ranked"]
          and {info_a["role"], info_b["role"]} == {P.ROLE_HOST, P.ROLE_GUEST} and not info_a["queued"])
    check("chacun voit le nom et l'Elo de l'autre", info_a["peer_name"] == "Bob" and info_a["peer_elo"] == 1000)
    check("on n'entre pas dans une partie classée avec son code",
          c.ask(P.PKT_JOIN, P.put_string(info_a["code"]))["join_error"] == P.JOIN_UNKNOWN)
    host, guest = (a, b) if info_a["role"] == P.ROLE_HOST else (b, a)

    # L'un des deux ne se met jamais prêt : l'autre quitte le salon, puis les deux repassent par la file
    guest.ask(P.PKT_LEAVE)
    info = guest.hello()
    check("quitter un salon classé avant le match : rien n'est compté pour celui qui part",
          info["code"] == "" and info["elo"] == 1000 and info["games"] == 0 and info["result"]["outcome"] == P.RESULT_NONE)
    info = host.ask(P.PKT_STATE, b"")
    check("... ni pour celui qui reste : le salon ferme, son Elo ne bouge pas",
          info["code"] == "" and info["elo"] == 1000 and info["games"] == 0 and info["result"]["outcome"] == P.RESULT_NONE)
    a.ask(P.PKT_QUEUE)
    info_b = b.ask(P.PKT_QUEUE)
    info_a = a.ask(P.PKT_QUEUE)
    check("ils se retrouvent par la file d'attente", info_a["code"] == info_b["code"] != "" and info_a["ranked"])
    host, guest = (a, b) if info_a["role"] == P.ROLE_HOST else (b, a)

    play(host, guest, 501)
    info = host.report(501, P.MATCH_FINISHED, 2, 1)
    check("un seul jeu annonce la fin : rien n'est compté", info["result"]["outcome"] == P.RESULT_NONE and info["elo"] == 1000)
    info = guest.report(501, P.MATCH_FINISHED, 2, 1)
    check("les deux jeux annoncent 2-1 : le perdant passe à 976",
          info["result"] == dict(match=501, outcome=P.RESULT_RATED, how=P.HOW_PLAYED, delta=-24) and info["elo"] == 976)
    info = host.report(501, P.MATCH_FINISHED, 2, 1)
    check("le vainqueur passe à 1024", info["result"]["delta"] == 24 and info["elo"] == 1024 and info["games"] == 1)
    info = guest.report(501, P.MATCH_FINISHED, 2, 1)
    check("le même match annoncé de nouveau n'est pas recompté", info["elo"] == 976 and info["games"] == 1)

    ranks = c.top()
    check("classement : le vainqueur devant, 2 joueurs classés", ranks["total"] == 2 and len(ranks["rows"]) == 2
          and ranks["rows"][0]["name"] == host.name and ranks["rows"][0]["elo"] == 1024 and ranks["rows"][0]["wins"] == 1
          and ranks["rows"][1]["losses"] == 1)
    check("un joueur sans match classé n'a pas de rang", ranks["my_rank"] == 0)
    check("le perdant est 2e", guest.top()["my_rank"] == 2)

    play(host, guest, 502)  # revanche dans le même salon
    host.report(502, P.MATCH_FINISHED, 3, 0)
    info = guest.report(502, P.MATCH_FINISHED, 0, 3)
    check("scores différents : match pas compté", info["result"]["outcome"] == P.RESULT_VOID and info["games"] == 1)

    play(host, guest, 503)
    guest.report(503, P.MATCH_PLAYING, 3, 0)
    info = guest.report(503, P.MATCH_ABANDON, 3, 0)
    check("abandon de l'invité mené 3-0 : forfait, compté comme une défaite de 3 buts",
          info["result"]["outcome"] == P.RESULT_RATED and info["result"]["how"] == P.HOW_MY_FORFEIT
          and info["result"]["delta"] < -30 and info["games"] == 2)
    info = host.report(503, P.MATCH_PEER_LOST, 3, 0)
    check("l'hôte gagne par forfait", info["result"]["how"] == P.HOW_PEER_FORFEIT and info["result"]["delta"] > 0)

    host.report(504, P.MATCH_PLAYING)
    guest.report(504, P.MATCH_PLAYING)
    info = host.report(504, P.MATCH_ABANDON)
    check("quitté avant le coup d'envoi : pas compté", info["result"]["outcome"] == P.RESULT_VOID and info["games"] == 2)

    # Liaison directe : le serveur ne voit passer aucun paquet de match, les jeux lui disent qu'ils jouent
    games = host.hello()["games"]
    host.report(506, P.MATCH_PLAYING)
    guest.report(506, P.MATCH_PLAYING, flags=P.MATCH_LINKED)
    info = host.report(506, P.MATCH_ABANDON)
    check("liaison directe, l'hôte n'a pas encore chargé son terrain : abandon pas compté",
          info["result"] == dict(match=506, outcome=P.RESULT_VOID, how=0, delta=0))
    host.report(507, P.MATCH_PLAYING, flags=P.MATCH_LINKED)
    guest.report(507, P.MATCH_PLAYING, flags=P.MATCH_LINKED)
    host.report(507, P.MATCH_PLAYING, flags=P.MATCH_LINKED)
    info = host.report(507, P.MATCH_ABANDON, 0, 0, P.MATCH_LINKED)
    check("liaison directe, les deux jeux disent qu'ils jouent : l'abandon est un forfait",
          info["result"]["outcome"] == P.RESULT_RATED and info["result"]["how"] == P.HOW_MY_FORFEIT
          and info["games"] == games + 1)
    guest.report(507, P.MATCH_PEER_LOST)

    play(host, guest, 505)
    host.ask(P.PKT_LEAVE)
    info = host.hello()
    check("quitter le salon en plein match : forfait",
          info["result"]["how"] == P.HOW_MY_FORFEIT and info["games"] == games + 2)
    info = guest.ask(P.PKT_STATE, b"")
    check("le salon classé ferme pour l'autre, qui gagne par forfait",
          info["code"] == "" and info["result"]["how"] == P.HOW_PEER_FORFEIT)
    info = a.hello()
    SAVED.update(elo=info["elo"], games=info["games"])  # relu après un redémarrage du serveur (test_persist)


SAVED = {}


def test_ranked_lost(port):
    """Serveur lancé avec --lost 1 --claim 2 --timeout 3."""
    host, guest = pair(port, 51, 52)
    play(host, guest, 601)
    guest.report(601, P.MATCH_PLAYING, 1, 0)
    for _ in range(3):  # l'invité ne joue plus (connexion coupée), l'hôte continue
        time.sleep(0.5)
        host.match(P.PKT_SNAP, b"etat")
    info = host.report(601, P.MATCH_PEER_LOST, 1, 0)
    check("l'autre ne répond plus : il est forfait", info["result"]["how"] == P.HOW_PEER_FORFEIT and info["result"]["delta"] > 0)
    guest.hello()

    play(host, guest, 602)
    info = host.report(602, P.MATCH_PEER_LOST)
    check("« j'ai perdu l'autre » alors qu'il joue encore : rien n'est décidé tout de suite",
          info["result"]["match"] == 601)
    for _ in range(6):
        time.sleep(0.5)
        guest.match(P.PKT_INPUT, b"touches")
        info = host.report(602, P.MATCH_PEER_LOST)
    check("... puis le match est annulé", info["result"] == dict(match=602, outcome=P.RESULT_VOID, how=0, delta=0))

    # Liaison directe : c'est l'annonce du jeu (MATCH_LINKED) qui dit au serveur qu'un joueur joue encore
    for g in (host, guest, host):
        g.report(604, P.MATCH_PLAYING, flags=P.MATCH_LINKED)
    for _ in range(3):  # le jeu de l'invité ne dit plus rien, l'hôte continue
        time.sleep(0.5)
        host.report(604, P.MATCH_PLAYING, 2, 0, P.MATCH_LINKED)
    info = host.report(604, P.MATCH_PEER_LOST, 2, 0, P.MATCH_LINKED)
    check("liaison directe, l'autre ne dit plus rien : il est forfait",
          info["result"]["match"] == 604 and info["result"]["how"] == P.HOW_PEER_FORFEIT)
    guest.hello()

    play(host, guest, 603)
    for _ in range(9):  # l'hôte se tait tout à fait (jeu fermé), l'invité reste
        time.sleep(0.5)
        info = guest.ask(P.PKT_STATE, b"")
    check("jeu fermé en plein match : forfait après le délai de silence",
          info["result"]["match"] == 603 and info["result"]["how"] == P.HOW_PEER_FORFEIT and info["code"] == "")


def test_takeover(port):
    """Serveur lancé avec --takeover 1."""
    host, guest = pair(port, 61, 62)
    code = host.ask(P.PKT_STATE, b"")["code"]
    play(host, guest, 701)
    for _ in range(3):  # le jeu de l'invité a planté
        time.sleep(0.5)
        host.ask(P.PKT_STATE, b"")
    back = Game(port, 63, guest.name, token=guest.token)  # relancé : nouvel identifiant, même compte
    info = back.hello()
    check("le jeu relancé retrouve son salon classé sans rien demander",
          info["code"] == code and info["role"] == P.ROLE_GUEST and info["ranked"])
    back.report(701, P.MATCH_PLAYING)
    host.match(P.PKT_SNAP, b"suite")
    check("le match continue vers le jeu relancé", back.recv(P.PKT_SNAP) is not None)
    host.report(701, P.MATCH_FINISHED, 1, 1)
    info = back.report(701, P.MATCH_FINISHED, 1, 1)
    check("match nul compté : Elo inchangé entre deux joueurs à 1000",
          info["result"]["outcome"] == P.RESULT_RATED and info["result"]["delta"] == 0 and info["games"] == 1)

    play(host, back, 702)
    for _ in range(3):  # cette fois c'est le jeu de l'hôte qui plante
        time.sleep(0.5)
        back.ask(P.PKT_STATE, b"")
    host2 = Game(port, 64, host.name, token=host.token)
    info = host2.hello()
    check("hôte relancé en plein match : il retrouve le salon, mais le match est perdu par forfait",
          info["code"] == code and info["role"] == P.ROLE_HOST and info["result"]["how"] == P.HOW_MY_FORFEIT)


def test_persist(port):
    a = Game(port, 71, "Alice", token="jeton%027d" % 41)  # le compte d'Alice du test « partie classée »
    info = a.hello()
    check("serveur relancé : l'Elo et les matchs du compte sont gardés (%d Elo, %d matchs)" % (info["elo"], info["games"]),
          info["games"] == SAVED["games"] == 4 and info["elo"] == SAVED["elo"])
    check("le classement aussi", a.top()["total"] >= 2)


# ------------------------------------------------------------------ ligue en ligne

LEAGUE_SETTINGS = dict(qualifiers=4, reset=False, points_win=3, points_draw=1, form=2.0, goals_per_minute=0.2,
                       home_bonus=0.1, strength_effect=0.08, delay=40, half=4)  # délai : 40 « minutes » = 2 s (--league-minute 0.05)
RATINGS14 = [91.5, 88.0, 93.25, 86.0, 90.0, 89.5, 92.0, 87.25, 90.75, 88.5, 94.0, 85.5, 91.0, 89.0]
LEAGUE_SAVED = {}


def read_save(v):
    """La sauvegarde d'une ligue (ModLeague.Save) en dictionnaire : de quoi trouver les matchs."""
    p = [0]

    def get(k=1):
        out = v[p[0]:p[0] + k]
        p[0] += k
        return out if k > 1 else out[0]
    d = dict(version=get(), points=get(2), reset=get(), qualifiers=get(), teams=get(), player=get(), phase=get(),
             round=get(), rounds=get(), day=get(), champion=get())
    get(3 * d["teams"])
    d["fixtures"] = [get(6) for _ in range(get())]
    d["seed"] = [get() for _ in range(get())]
    d["matches"] = [get(11) for _ in range(get())]
    if p[0] != len(v):
        raise ValueError("sauvegarde de ligue mal formée")
    return d


def key_sides(save, key):
    """(équipe qui reçoit, invitée) du match de cette clé."""
    if key < 10000:
        f = save["fixtures"][key]
        return f[1], f[2]
    m = save["matches"][key - 10000]
    return (m[6], m[7]) if m[10] & 1 else (m[7], m[6])


def league_sync(game, known=0):
    return game.league(P.LG_SYNC, known)["league"]


def test_league(port):
    a, b, c = Game(port, 81, "Alice"), Game(port, 82, "Bob"), Game(port, 83, "Chloé")
    nobody = Game(port, 84, "Sans compte", token="court")
    for g in (a, b, c, nobody):
        g.hello()
    check("pas de ligue au départ", league_sync(a) is None)
    check("sans code et sans ligue ouverte : refusé", c.league(P.LG_JOIN, code="", animal=7)["error"] == P.LGE_UNKNOWN)
    check("sans compte : pas de ligue",
          nobody.league(P.LG_CREATE, settings=LEAGUE_SETTINGS, animal=3, ratings=RATINGS14)["error"] == P.LGE_NO_ACCOUNT)
    check("notes illisibles refusées",
          a.league(P.LG_CREATE, settings=LEAGUE_SETTINGS, animal=3, ratings=RATINGS14[:5])["error"] == P.LGE_BAD)
    info = a.league(P.LG_CREATE, settings=LEAGUE_SETTINGS, animal=3, clothes=1, ratings=RATINGS14)
    lg = info["league"]
    check("ligue créée (code %s), Alice en est la créatrice, pas encore lancée" % lg["code"],
          info["error"] == P.LGE_NONE and lg["code"] == "LIGA" and lg["flags"] == P.LGF_OWNER and len(lg["members"]) == 1
          and lg["members"][0]["animal"] == 3 and lg["members"][0]["clothes"] == 1 and lg["state"] is None)
    check("créer deux fois rend la même ligue",
          a.league(P.LG_CREATE, settings=LEAGUE_SETTINGS, animal=3, ratings=RATINGS14)["league"]["code"] == "LIGA")
    check("code inconnu", c.league(P.LG_JOIN, code="ZZZZ", animal=7)["error"] == P.LGE_UNKNOWN)
    info = b.league(P.LG_JOIN, code="liga", animal=3)
    check("Bob entre avec le code en minuscules ; équipe 3 déjà prise : il reçoit la première libre (0)",
          info["error"] == P.LGE_TEAM_TAKEN and [m["animal"] for m in info["league"]["members"]] == [3, 0])
    check("Bob ne peut pas prendre l'équipe d'Alice", b.league(P.LG_TEAM, animal=3)["error"] == P.LGE_TEAM_TAKEN)
    info = b.league(P.LG_TEAM, animal=5, clothes=2)
    check("Bob prend l'équipe 5", info["error"] == P.LGE_NONE and info["league"]["members"][1]["animal"] == 5)
    info = c.league(P.LG_JOIN, code="", animal=7)
    check("Chloé entre sans code dans la dernière ligue ouverte",
          info["error"] == P.LGE_NONE and info["league"]["code"] == "LIGA")
    check("sans code une deuxième fois : rien ne change", c.league(P.LG_JOIN, code="", animal=7)["league"]["code"] == "LIGA")
    members = league_sync(a)["members"]
    check("Alice voit les trois joueurs connectés, elle-même marquée",
          len(members) == 3 and all(m["flags"] & P.LGM_ONLINE for m in members) and members[0]["flags"] & P.LGM_ME
          and members[0]["flags"] & P.LGM_OWNER and not members[1]["flags"] & P.LGM_ME)
    check("seule la créatrice lance la ligue", b.league(P.LG_START)["error"] == P.LGE_NOT_OWNER)
    c.league(P.LG_LEAVE)
    check("Chloé sort avant le départ", league_sync(c) is None and len(league_sync(a)["members"]) == 2)

    info = a.league(P.LG_START)
    lg = info["league"]
    save = read_save(lg["state"])
    check("ligue lancée : 14 équipes, 13 journées, l'état arrive avec Alice comme « joueur » (%d octets)" % lg["size"],
          lg["flags"] & P.LGF_STARTED and save["teams"] == 14 and save["rounds"] == 13 and save["player"] == 3 == lg["my_team"]
          and lg["size"] < 1400)
    check("délai de la journée annoncé", 0 <= lg["deadline"] <= 2)
    check("état déjà connu : pas renvoyé", league_sync(a, lg["version"])["state"] is None)
    check("Chloé ne peut plus entrer", c.league(P.LG_JOIN, code="LIGA", animal=7)["error"] == P.LGE_STARTED)
    check("sans code : la ligue lancée n'est plus ouverte", c.league(P.LG_JOIN, code="", animal=7)["error"] == P.LGE_UNKNOWN)
    lb = league_sync(b)
    check("Bob voit la ligue lancée, son équipe comme « joueur »", read_save(lb["state"])["player"] == 5 == lb["my_team"])

    players = {3: a, 5: b}
    met = False  # Alice et Bob se sont rencontrés
    rounds = 0
    deadline_tested = grace_tested = skip_tested = abandon_tested = False
    playoffs_played = []
    while True:
        la, lb = league_sync(a), league_sync(b)
        save = read_save(la["state"])
        if save["phase"] == 2:
            break
        rounds += 1
        check("la ligue avance (%d)" % rounds, rounds <= 40) if rounds > 40 else None
        keys = {3: la["my_key"], 5: lb["my_key"] if lb else -1}
        if keys[3] >= 0 and keys[3] == keys[5]:
            # Alice contre Bob : chacun demande à jouer, le serveur ouvre un salon classé, l'équipe qui reçoit en hôte
            key = keys[3]
            home, away = key_sides(save, key)
            check("un match entre joueurs n'est pas envoyé comme un match contre l'IA",
                  a.league(P.LG_RESULT, key=key, goals_host=1)["error"] == P.LGE_REFUSED)
            a.league(P.LG_PLAY, key=key)
            check("Bob voit qu'Alice l'attend", any(m["flags"] & P.LGM_WAITING for m in league_sync(b)["members"]))
            b.league(P.LG_PLAY, key=key)
            host, guest = players[home], players[away]
            ih, ig = host.ask(P.PKT_STATE, b""), guest.ask(P.PKT_STATE, b"")
            check("match de ligue entre joueurs : salon classé ouvert, l'équipe qui reçoit en hôte",
                  ih["code"] != "" and ih["code"] == ig["code"] and ih["ranked"] and ih["league"] and ig["league"]
                  and ih["role"] == P.ROLE_HOST and ig["role"] == P.ROLE_GUEST)
            gh, gg = (3, 1) if key < 10000 else (2, 1)
            play(host, guest, 7000 + rounds)
            host.report(7000 + rounds, P.MATCH_FINISHED, gh, gg)
            info = guest.report(7000 + rounds, P.MATCH_FINISHED, gh, gg)
            check("le match compte pour l'Elo en ligne", info["result"]["outcome"] == P.RESULT_RATED and info["elo"] != 1000)
            after = read_save(league_sync(a)["state"])
            rec = after["fixtures"][key] if key < 10000 else after["matches"][key - 10000]
            check("... et pour la ligue : %d-%d enregistré" % (gh, gg),
                  rec[3:5] == [gh, gg] and rec[5] == 1 if key < 10000 else bool(rec[10] & 2))
            host.ask(P.PKT_LEAVE)
            met = True
            continue
        if not deadline_tested and save["phase"] == 0:
            # personne ne joue : la journée se ferme au bout du délai, les matchs des joueurs sont simulés
            deadline_tested = True
            time.sleep(3.2)
            after = read_save(league_sync(a)["state"])
            check("délai dépassé : la journée %d est fermée, les matchs des joueurs simulés" % (save["round"] + 1),
                  after["round"] > save["round"] and all(after["fixtures"][k][5] for k in keys.values() if k >= 0))
            continue
        if not grace_tested and save["phase"] == 0 and keys[3] >= 0 and keys[5] >= 0:
            # Alice a lancé son match contre l'IA : le délai l'épargne tant qu'elle joue
            grace_tested = True
            a.league(P.LG_PLAY, key=keys[3])
            b.league(P.LG_RESULT, key=keys[5], goals_host=2, goals_guest=2)
            time.sleep(2.6)
            mid = read_save(league_sync(a)["state"])
            check("délai dépassé pendant le match d'Alice : la journée attend son score", mid["round"] == save["round"])
            info = a.league(P.LG_RESULT, key=keys[3], goals_host=1, goals_guest=0)
            again = a.league(P.LG_RESULT, key=keys[3], goals_host=1, goals_guest=0)
            check("score reçu, journée fermée ; le même score redit n'est pas une erreur",
                  info["error"] == P.LGE_NONE and read_save(info["league"]["state"])["round"] == save["round"] + 1
                  and again["error"] == P.LGE_NONE)
            continue
        if not skip_tested:
            skip_tested = True
            check("seule la créatrice passe une journée", b.league(P.LG_SKIP)["error"] == P.LGE_NOT_OWNER)
            info = a.league(P.LG_SKIP)
            after = read_save(info["league"]["state"])
            check("la créatrice passe la journée : ce qui reste est simulé",
                  (after["phase"], after["round"], after["day"]) != (save["phase"], save["round"], save["day"]))
            continue
        if met and not abandon_tested and save["phase"] == 0:
            abandon_tested = True
            b.league(P.LG_LEAVE)
            check("Bob abandonne : il n'a plus de ligue", league_sync(b) is None)
            members = league_sync(a)["members"]
            check("Alice le voit parti ; l'IA joue son équipe", members[1]["flags"] & P.LGM_LEFT and len(members) == 2)
            check("Bob ne peut pas en créer une autre ici : le code imposé est pris",
                  b.league(P.LG_CREATE, settings=LEAGUE_SETTINGS, animal=1, ratings=RATINGS14)["error"] == P.LGE_LIMIT)
            del players[5]
            continue
        for team, game in players.items():
            key = keys[team]
            if key < 0:
                continue
            game.league(P.LG_PLAY, key=key)
            if key >= 10000:
                check("nul en playoffs : à rejouer",
                      game.league(P.LG_RESULT, key=key, goals_host=1, goals_guest=1)["error"] == P.LGE_REPLAY)
                playoffs_played.append(key)
            win = (2, 0) if key_sides(save, key)[0] == team else (0, 2)  # le joueur gagne toujours : il va en playoffs
            info = game.league(P.LG_RESULT, key=key, goals_host=win[0], goals_guest=win[1])
            if info["error"] != P.LGE_NONE:
                check("score contre l'IA accepté (erreur %d)" % info["error"], False)
    final = read_save(league_sync(a)["state"])
    check("ligue finie : un champion, tous les matchs joués", final["champion"] >= 0 and all(f[5] for f in final["fixtures"]))
    check("Alice, qui gagne tout, joue les playoffs (%d matchs) et finit championne" % len(playoffs_played),
          len(playoffs_played) >= 2 and final["champion"] == 3)
    check("Alice et Bob se sont rencontrés ; délai, épargne, « passer » et abandon vus",
          met and deadline_tested and grace_tested and skip_tested and abandon_tested)
    check("la ligue finie reste visible", league_sync(a)["flags"] & P.LGF_OVER)
    LEAGUE_SAVED["version"] = league_sync(a)["version"]


def test_league_persist(port):
    a = Game(port, 91, "Alice", token="jeton%027d" % 81)
    a.hello()
    lg = league_sync(a)
    check("serveur relancé : la ligue est gardée, dans la même version",
          lg is not None and lg["version"] == LEAGUE_SAVED["version"] and lg["flags"] & P.LGF_OVER
          and read_save(lg["state"])["champion"] >= 0)
    a.league(P.LG_LEAVE)
    check("Alice quitte la ligue finie", league_sync(a) is None)


# ------------------------------------------------------------------ sans réseau : file d'attente par Elo, adresses

def fake_session(lobby, n, elo, addr=None, version=MOD_VERSION):
    player = types.SimpleNamespace(id=n, elo=elo, games=0, name="j%d" % n)
    return lobby.hello(n, addr or ("127.0.0.1", 1000 + n), "j%d" % n, version, lobby.now, player, "jeton",
                       ("192.168.1.%d" % n, 5000))


def quiet_lobby(**options):
    return rooms.Lobby(queue_stale=1e9, timeout=1e9, rng=random.Random(1), **options)


def test_queue_elo():
    """Lobby seul, avec une horloge à la main : écart accepté 100 Elo, + 10 par seconde d'attente."""
    lobby = quiet_lobby(queue_range=100.0, queue_grow=10.0)
    low, high, far = fake_session(lobby, 1, 1000), fake_session(lobby, 2, 1250), fake_session(lobby, 3, 2600)
    lobby.enqueue(low, 0.0)
    lobby.enqueue(high, 0.0)
    check("250 Elo d'écart : pas de partie tout de suite", low.room is None and high.room is None)
    check("l'écart accepté part de 100", lobby.accepted_gap(low, 0.0) == 100)
    lobby.enqueue(low, 14.0)
    check("... ni après 14 s (écart accepté 240)", low.room is None and lobby.accepted_gap(low, 14.0) == 240)
    lobby.enqueue(far, 14.5)
    lobby.enqueue(low, 15.0)
    check("après 15 s l'écart accepté atteint 250 : la partie est créée",
          low.room is not None and low.room is high.room and low.room.ranked and far.room is None)
    near = fake_session(lobby, 4, 2650)
    lobby.enqueue(near, 20.0)
    check("un joueur d'Elo proche est pris tout de suite", near.room is not None and near.room is far.room)

    lobby = quiet_lobby(queue_range=100.0, queue_grow=10.0)
    first, a, b = fake_session(lobby, 1, 1000), fake_session(lobby, 2, 1090), fake_session(lobby, 3, 1010)
    lobby.queue = [first, a, b]  # tous dans la file au même instant, avant que le serveur ne cherche
    lobby.pair(0.0)
    check("entre plusieurs adversaires possibles, le plus proche en Elo", first.room is b.room is not None and a.room is None)

    lobby = quiet_lobby(queue_range=100.0, queue_grow=10.0)
    weak, strong = fake_session(lobby, 1, 600), fake_session(lobby, 2, 3000)
    lobby.enqueue(weak, 0.0)
    lobby.enqueue(strong, 229.0)
    check("2400 Elo d'écart : toujours rien après 229 s", weak.room is None)
    lobby.enqueue(strong, 230.0)
    check("... mais l'écart accepté n'a pas de limite : partie créée à 230 s, même si l'autre vient d'arriver",
          weak.room is not None and weak.room is strong.room)

    lobby = quiet_lobby()
    old = fake_session(lobby, 1, 1000, version=MOD_VERSION - 1)
    new = fake_session(lobby, 2, 1000)
    lobby.enqueue(old, 0.0)
    lobby.enqueue(new, 0.0)
    check("deux versions du mod ne sont pas réunies, même à Elo égal", old.room is None and new.room is None)
    lobby.leave(old)
    lobby.enqueue(old, 50.0)
    check("sortir de la file puis y revenir remet l'attente à zéro", lobby.accepted_gap(old, 50.0) == 100)


def test_link_scope():
    """Liaison directe : l'adresse de l'autre n'est donnée que si elle mène quelque part."""
    def linked(addr_a, addr_b, **options):
        lobby = quiet_lobby(**options)
        a, b = fake_session(lobby, 1, 1000, addr_a), fake_session(lobby, 2, 1000, addr_b)
        lobby.create(a)
        lobby.join(b, a.room.code, 0.0)
        return lobby.link(a), lobby.link(b), a, b

    la, lb, a, b = linked(("82.10.20.30", 4000), ("91.5.6.7", 5000))
    check("deux joueurs par Internet : chacun reçoit l'adresse publique de l'autre, pas son adresse locale",
          la == (a.room.key, b.addr, P.NO_ADDR) and lb == (a.room.key, a.addr, P.NO_ADDR) and a.room.key != 0)
    la, lb, a, b = linked(("82.10.20.30", 4000), ("82.10.20.30", 5000))
    check("deux joueurs derrière la même box : l'adresse locale en plus", la == (a.room.key, b.addr, b.local_addr))
    la, lb, a, b = linked(("127.0.0.1", 4000), ("91.5.6.7", 5000))
    check("un joueur sur le PC du serveur, l'autre par Internet : relais seul",
          la == lb == (0, P.NO_ADDR, P.NO_ADDR))
    la, lb, a, b = linked(("192.168.1.10", 4000), ("91.5.6.7", 5000))
    check("un joueur sur le réseau du serveur, l'autre par Internet : relais seul", la == (0, P.NO_ADDR, P.NO_ADDR))
    la, lb, a, b = linked(("192.168.1.10", 4000), ("192.168.1.11", 5000))
    check("deux joueurs sur le réseau du serveur : liaison directe", la == (a.room.key, b.addr, P.NO_ADDR))
    la, lb, a, b = linked(("82.10.20.30", 4000), ("91.5.6.7", 5000), direct=False)
    check("--no-direct : aucune adresse donnée", la == lb == (0, P.NO_ADDR, P.NO_ADDR))


print("--- file d'attente par Elo")
test_queue_elo()
print("--- liaison directe : adresses")
test_link_scope()

LEAGUE_ARGS = ["--league-code", "LIGA", "--league-minute", "0.05", "--league-grace", "0.1"]
TMP = tempfile.mkdtemp(prefix="petsoccer-test-")  # la base de test : jamais celle des vrais joueurs
try:
    for name, test, args in (("salons", test_rooms, []), ("silence", test_timeout, ["--timeout", "2", "--code", "test"]),
                             ("invité muet", test_stale, ["--stale", "1.5"]),
                             ("mauvaise connexion", test_lag, ["--lag", "150", "--loss", "50"]),
                             ("partie classée", test_ranked, []),
                             ("classé : joueur disparu", test_ranked_lost, ["--lost", "1", "--claim", "2", "--timeout", "3"]),
                             ("classé : jeu relancé", test_takeover, ["--takeover", "1"]),
                             ("comptes gardés", test_persist, []),
                             ("ligue en ligne", test_league, LEAGUE_ARGS),
                             ("ligue gardée", test_league_persist, LEAGUE_ARGS)):
        print("--- " + name)
        port = free_port()
        server = start_server(port, *args)
        try:
            test(port)
        finally:
            server.kill()
            server.wait()
finally:
    shutil.rmtree(TMP, ignore_errors=True)
print("%d vérifications réussies" % checks)
