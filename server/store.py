"""Comptes des joueurs, Elo en ligne et historique des matchs classés, dans un fichier SQLite.

Un compte = un jeton secret tiré par le jeu à son premier passage en ligne (online.dat). Pas de mot de
passe : qui présente le jeton est le joueur. Le serveur ne garde que l'empreinte du jeton.

La formule de l'Elo est celle du jeu contre l'IA (mods/java/ModElo.java, Data/elo.scr).
"""
import hashlib
import math
import sqlite3
import time

ELO_START = 1000
ELO_K = 32.0
PLACEMENT_GAMES = 10  # premiers matchs : l'Elo bouge plus vite
PLACEMENT_K = 48.0
NAME_MAX = 16

_SCHEMA = """
CREATE TABLE IF NOT EXISTS players (
    id INTEGER PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    elo INTEGER NOT NULL,
    games INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    draws INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    peak INTEGER NOT NULL,
    created REAL NOT NULL,
    last_seen REAL NOT NULL
);
CREATE TABLE IF NOT EXISTS matches (
    id INTEGER PRIMARY KEY,
    played REAL NOT NULL,
    host INTEGER NOT NULL,
    guest INTEGER NOT NULL,
    score_host INTEGER NOT NULL,
    score_guest INTEGER NOT NULL,
    forfeit INTEGER NOT NULL,
    delta_host INTEGER NOT NULL,
    delta_guest INTEGER NOT NULL,
    team_host INTEGER,
    team_guest INTEGER
);
"""
_COLUMNS = "id, name, elo, games, wins, draws, losses, peak"


def elo_delta(elo, games, opponent_elo, goals_for, goals_against):
    """Variation d'Elo d'un joueur après un match."""
    expected = 1.0 / (1.0 + math.pow(10.0, (opponent_elo - elo) / 400.0))
    score = 1.0 if goals_for > goals_against else (0.5 if goals_for == goals_against else 0.0)
    # écart de buts : x1 jusqu'à 1 but, x1,5 à 2 buts, puis (11 + écart) / 8
    margin = abs(goals_for - goals_against)
    margin_mul = 1.0 if margin <= 1 else (1.5 if margin == 2 else (11.0 + margin) / 8.0)
    k = PLACEMENT_K if games < PLACEMENT_GAMES else ELO_K
    return int(math.floor(k * margin_mul * (score - expected) + 0.5))


def clean_name(name):
    name = "".join(c for c in name if ord(c) >= 32).strip()[:NAME_MAX]
    return name or "?"


def valid_token(token):
    return 16 <= len(token) <= 64 and token.isalnum() and token.isascii()


class Player:
    def __init__(self, row):
        self.id, self.name, self.elo, self.games, self.wins, self.draws, self.losses, self.peak = row

    def __repr__(self):
        return "%s#%d (%d)" % (self.name, self.id, self.elo)


class Store:
    def __init__(self, path):
        self.db = sqlite3.connect(path)
        self.db.executescript(_SCHEMA)
        # Migration : colonnes ajoutées après la création initiale de la table
        for col in ("team_host", "team_guest"):
            try:
                self.db.execute("ALTER TABLE matches ADD COLUMN %s INTEGER" % col)
            except Exception:
                pass
        self.db.commit()
        self.players = {}  # numéro du compte -> Player : un seul objet par compte, partagé par ses sessions

    def close(self):
        self.db.close()

    def _player(self, row):
        player = self.players.get(row[0])
        if player is None:
            player = self.players[row[0]] = Player(row)
        return player

    def login(self, token, name):
        """Le compte de ce jeton, créé s'il n'existe pas. None si le jeton n'a pas la bonne forme."""
        if not valid_token(token):
            return None
        digest = hashlib.sha256(token.encode("ascii")).hexdigest()
        name = clean_name(name)
        now = time.time()
        row = self.db.execute("SELECT %s FROM players WHERE token = ?" % _COLUMNS, (digest,)).fetchone()
        if row is None:
            cur = self.db.execute(
                "INSERT INTO players (token, name, elo, peak, created, last_seen) VALUES (?, ?, ?, ?, ?, ?)",
                (digest, name, ELO_START, ELO_START, now, now))
            row = (cur.lastrowid, name, ELO_START, 0, 0, 0, 0, ELO_START)
        player = self._player(row)
        player.name = name
        self.db.execute("UPDATE players SET name = ?, last_seen = ? WHERE id = ?", (name, now, player.id))
        self.db.commit()
        return player

    def record(self, host, guest, score_host, score_guest, goals, forfeit, team_host=None, team_guest=None):
        """Compte un match classé. goals = (buts de l'hôte, buts de l'invité) retenus pour l'Elo : le score,
        ou une défaite d'au moins un but d'écart pour un forfait. Renvoie (variation de l'hôte, de l'invité)."""
        delta_host = elo_delta(host.elo, host.games, guest.elo, goals[0], goals[1])
        delta_guest = elo_delta(guest.elo, guest.games, host.elo, goals[1], goals[0])
        for player, delta, gf, ga in ((host, delta_host, goals[0], goals[1]), (guest, delta_guest, goals[1], goals[0])):
            player.elo += delta
            player.games += 1
            player.wins += gf > ga
            player.draws += gf == ga
            player.losses += gf < ga
            player.peak = max(player.peak, player.elo)
            self.db.execute("UPDATE players SET elo = ?, games = ?, wins = ?, draws = ?, losses = ?, peak = ? WHERE id = ?",
                            (player.elo, player.games, player.wins, player.draws, player.losses, player.peak, player.id))
        self.db.execute(
            "INSERT INTO matches (played, host, guest, score_host, score_guest, forfeit, delta_host, delta_guest, team_host, team_guest)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (time.time(), host.id, guest.id, score_host, score_guest, int(forfeit), delta_host, delta_guest, team_host, team_guest))
        self.db.commit()
        return delta_host, delta_guest

    # --- classement : les joueurs qui ont au moins un match classé ---

    def top(self, count):
        rows = self.db.execute(
            "SELECT %s FROM players WHERE games > 0 ORDER BY elo DESC, games DESC, id LIMIT ?" % _COLUMNS, (count,))
        return [self._player(row) for row in rows]

    def total(self):
        return self.db.execute("SELECT COUNT(*) FROM players WHERE games > 0").fetchone()[0]

    def rank(self, player):
        """Rang du joueur à partir de 1, ou 0 s'il n'a pas encore de match classé."""
        if player.games == 0:
            return 0
        return 1 + self.db.execute(
            "SELECT COUNT(*) FROM players WHERE games > 0 AND (elo > ? OR (elo = ? AND (games > ? OR (games = ? AND id < ?))))",
            (player.elo, player.elo, player.games, player.games, player.id)).fetchone()[0]
