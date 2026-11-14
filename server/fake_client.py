"""Faux client du multijoueur en ligne : teste l'hôte sans lancer un second jeu.

    python server\\fake_client.py [ip] [port] [durée en secondes]

Se connecte à un jeu lancé avec NetHost(port) dans debugconf.scr, envoie des touches selon un
petit scénario (directions qui tournent, passe, tir) et affiche chaque seconde ce que l'hôte diffuse.
Le format des paquets est celui de mods\\java\\ModNet.java.
"""
import socket
import struct
import sys
import time

MAGIC = 0x50534E31
PKT_INPUT, PKT_SNAP = 1, 2
EV_ANIM = 1
EV_NAMES = {1: "anim", 2: "effet", 3: "son", 4: "hud", 5: "commentaire", 6: "cote", 7: "public", 8: "meteo", 9: "stats"}
ACTIONS = ["passe", "tir", "feinte", "super", "haut", "droite", "bas", "gauche"]
PASS, SHOT, FEINT, SUPER, UP, RIGHT, DOWN, LEFT = range(8)

host = sys.argv[1] if len(sys.argv) > 1 else "127.0.0.1"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 7777
duration = float(sys.argv[3]) if len(sys.argv) > 3 else 60.0

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind(("", 0))  # Windows refuse de lire sur une socket ni liée ni déjà utilisée
sock.setblocking(False)
session = int(time.time()) & 0x7FFFFFFF
inputs = []  # tous les changements de touches, le numéro est l'index + 1
event_applied = -1
last_seq = 0
snaps = 0
anims = {}
ev_counts = {}  # nombre d'événements reçus par type
last = None


def key(action, state):
    inputs.append(action | (0x80 if state else 0))
    print(f"   touche {ACTIONS[action]} {'enfoncee' if state else 'relachee'} (n° {len(inputs)})")


def send_input():
    tail = inputs[-15:]
    pkt = struct.pack(">IBiiiB", MAGIC, PKT_INPUT, session, max(event_applied, 0), len(inputs), len(tail))
    sock.sendto(pkt + bytes(tail), (host, port))


def read_snap(data):
    global event_applied, last_seq, snaps, last
    magic, ptype = struct.unpack_from(">IB", data, 0)
    if magic != MAGIC or ptype != PKT_SNAP:
        return
    off = 5
    sess, seq, ack_input, t, state, half, score_l, score_r, clock = struct.unpack_from(">iiifbbBBf", data, off)
    off += struct.calcsize(">iiifbbBBf")
    if sess != session:
        return
    ball = struct.unpack_from(">6f", data, off)
    off += 24
    owner, cam, focus0, mark0, focus1, mark1 = struct.unpack_from(">6b", data, off)
    off += 6 + 25  # + caméra de l'hôte (position, direction, drapeau ralenti)
    n0, n1 = struct.unpack_from(">BB", data, off)
    off += 2
    count = n0 + n1 + 1
    objs = struct.unpack_from(f">{count * 6}f", data, off)
    off += count * 24
    # suite de l'état : drapeaux, menu, fondu, ralenti, joueur 1, mi-temps, sons de super, 5 positions
    # ... et la pause de mi-temps : secondes avant la reprise (255 : hors de la pause)
    flags_a, flags_b, menu, fade, speed, player1, half_time, speed_obj, feint_obj, half_left = struct.unpack_from(">6BhbbB", data, off)
    off += struct.calcsize(">6BhbbB") + 60
    first, nev = struct.unpack_from(">iB", data, off)
    off += 5
    if event_applied < 0 or first > event_applied + 1:
        event_applied = first - 1
    for i in range(nev):
        (length,) = struct.unpack_from(">H", data, off)
        off += 2
        ev = data[off:off + length]
        off += length
        if first + i == event_applied + 1:
            event_applied = first + i
            ev_counts[ev[0]] = ev_counts.get(ev[0], 0) + 1
            if ev[0] == EV_ANIM:
                index, rep, nlen = ev[1], struct.unpack("b", ev[2:3])[0], ev[3]
                anims[index] = (ev[4:4 + nlen].decode("latin-1"), rep)
    if seq > last_seq:
        last_seq = seq
        snaps += 1
        last = dict(seq=seq, ack=ack_input, t=t, state=state, half=half, score=(score_l, score_r), clock=clock,
                    ball=ball, owner=owner, cam=cam, focus=(focus0, focus1), n0=n0, n1=n1, objs=objs, size=len(data),
                    flags=(flags_a, flags_b), menu=menu, fade=fade, speed=speed, half_time=half_time)


def pos(index):
    return last["objs"][index * 6], last["objs"][index * 6 + 2]


print(f"faux client -> {host}:{port}, session {session}, {duration:.0f} s")
start = time.time()
next_send = next_print = start
step = 0
prev_pos = None
while time.time() - start < duration:
    now = time.time()
    try:
        while True:
            data, _ = sock.recvfrom(4096)
            read_snap(data)
    except (BlockingIOError, ConnectionResetError):
        pass
    if now >= next_send:
        next_send = now + 0.033
        send_input()
    if now >= next_print:
        next_print = now + 1.0
        second = int(now - start)
        if last is None:
            print(f"{second:3d}s  rien recu de l'hote")
        else:
            n0, n1 = last["n0"], last["n1"]
            cur = [pos(n0 + i) for i in range(n1)]
            moved = ""
            if prev_pos and len(prev_pos) == len(cur):
                d = [((cur[i][0] - prev_pos[i][0]) ** 2 + (cur[i][1] - prev_pos[i][1]) ** 2) ** 0.5 for i in range(n1)]
                moved = " dep. equipe 2 : " + " ".join(f"{x:5.0f}" for x in d)
            prev_pos = cur
            b = last["ball"]
            print(f"{second:3d}s  etats {snaps:4d} ({last['size']} o) etat {last['state']} mi-temps {last['half']} "
                  f"score {last['score'][0]}-{last['score'][1]} horloge {last['clock']:6.1f} "
                  f"ballon ({b[0]:6.0f},{b[1]:5.0f},{b[2]:6.0f}) porteur {last['owner']:3d} camera {last['cam']:3d} "
                  f"actifs {last['focus'][0]:2d}/{last['focus'][1]:2d} "
                  f"drapeaux {last['flags'][0]:02x}/{last['flags'][1]:02x} menu {last['menu']} fondu {last['fade']:3d} "
                  f"vitesse {last['speed']:3d} "
                  f"touches jouees {last['ack']}/{len(inputs)} evts {event_applied}{moved}")
        # scénario : une direction tenue 2 s, une passe toutes les 3 s, un tir toutes les 7 s
        if second % 2 == 0:
            direction = [RIGHT, DOWN, LEFT, UP][(second // 2) % 4]
            if step > 0:
                key(prev_dir, False)
            key(direction, True)
            prev_dir = direction
            step += 1
        if second % 3 == 1:
            key(PASS, True)
            key(PASS, False)
        if second % 7 == 5:
            key(SHOT, True)
            key(SHOT, False)

print("evenements recus par type : " + ", ".join(f"{EV_NAMES.get(k, k)} {v}" for k, v in sorted(ev_counts.items())))
print("dernieres animations par objet (0-6 equipe 1, 16-22 equipe 2, 32 arbitre) :")
for index in sorted(anims):
    print(f"   {index:2d} : {anims[index][0]} x{anims[index][1]}")
