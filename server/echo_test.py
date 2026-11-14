"""Écho UDP pour le test de faisabilité (phase 0 du multijoueur en ligne).

    python server\\echo_test.py [port]

Renvoie chaque paquet tel quel à son expéditeur. Côté jeu : NetTest("127.0.0.1", port)
dans debugconf.scr, puis lire les lignes « [mod] net » de crash.log.
"""
import socket
import sys

port = int(sys.argv[1]) if len(sys.argv) > 1 else 7777
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind(("0.0.0.0", port))
print(f"écho UDP sur le port {port}", flush=True)
while True:
    data, addr = sock.recvfrom(2048)
    print(f"{len(data)} octets de {addr[0]}:{addr[1]}", flush=True)
    sock.sendto(data, addr)
