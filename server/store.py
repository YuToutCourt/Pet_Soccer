"""Comptes des joueurs, Elo en ligne et matchs (score, tactiques, statistiques), dans une base Postgres.

Un compte = un jeton secret tiré par le jeu à son premier passage en ligne (online.dat). Pas de mot de
passe : qui présente le jeton est le joueur. Le serveur ne garde que l'empreinte du jeton.

La formule de l'Elo est celle du jeu contre l'IA (mods/java/ModElo.java, Data/elo.scr). Schéma : schema.sql.
"""
import hashlib
import json
import math
import os
import time

import psycopg

ELO_START = 1000
ELO_K = 32.0
PLACEMENT_GAMES = 10  # premiers matchs : l'Elo bouge plus vite
PLACEMENT_K = 48.0
NAME_MAX = 16
SITUATIONS = ("play", "kickoff", "throw", "goalkick")  # ModTactics._SIT_*

SCHEMA_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "schema.sql")
DatabaseError = psycopg.Error

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


class Database:
    """Une connexion Postgres (autocommit) qui se rouvre d'elle-même si la base a redémarré.

    Le serveur de jeu n'a qu'un fil : une seule connexion suffit. Une requête qui échoue lève DatabaseError ;
    server.py la journalise sans arrêter la boucle UDP."""

    def __init__(self, url, log=None):
        self.url = url
        self.log = log or (lambda text: None)
        self.conn = None
        self.connect()

    def connect(self):
        self.conn = psycopg.connect(self.url, autocommit=True)

    def run(self, fn):
        for attempt in (0, 1):
            try:
                if self.conn is None or self.conn.closed:
                    self.connect()
                return fn(self.conn)
            except psycopg.OperationalError as e:
                # connexion perdue (Postgres relancé...) : une nouvelle tentative avec une connexion neuve
                self.log("base : %s" % str(e).strip())
                try:
                    if self.conn is not None:
                        self.conn.close()
                except psycopg.Error:
                    pass
                self.conn = None
                if attempt:
                    raise

    def execute(self, sql, params=()):
        self.run(lambda c: c.execute(sql, params))

    def all(self, sql, params=()):
        return self.run(lambda c: c.execute(sql, params).fetchall())

    def one(self, sql, params=()):
        return self.run(lambda c: c.execute(sql, params).fetchone())

    def script(self, sql):
        """Plusieurs instructions sans paramètres (schéma)."""
        self.run(lambda c: c.execute(sql))

    def transaction(self, fn):
        """fn(connexion) dans une transaction : tout ou rien."""
        def body(c):
            with c.transaction():
                return fn(c)
        return self.run(body)

    def close(self):
        if self.conn is not None:
            self.conn.close()


def parse_tactic(line):
    """Composition envoyée par le jeu (ModTactics.ToLine) : « SCHEMA;R1,...,R6;jeu;coup d'envoi;touche;dégagement »,
    chaque placement « p/l,... » x6 en millièmes ou « - » (automatique). Renvoie None pour une ligne vide."""
    if not line:
        return None
    parts = line.split(";")
    scheme = parts[0].strip() or None
    roles = None
    if len(parts) > 1:
        roles = [r.strip() for r in parts[1].split(",")]
        if len(roles) != 6 or not all(roles):
            roles = None
    layouts = {}
    for i, name in enumerate(SITUATIONS):
        layouts[name] = None
        if roles is None or len(parts) <= 2 + i or parts[2 + i].strip() == "-":
            continue
        try:
            pairs = [p.split("/") for p in parts[2 + i].split(",")]
            if len(pairs) == 6:
                layouts[name] = [[int(d) / 1000.0, int(l) / 1000.0] for d, l in pairs]
        except ValueError:
            pass
    return dict(scheme=scheme, roles=roles, layouts=layouts)


def _result(goals_for, goals_against):
    return "W" if goals_for > goals_against else ("D" if goals_for == goals_against else "L")


class Store:
    def __init__(self, url, log=None):
        self.db = Database(url, log)
        with open(SCHEMA_FILE, encoding="utf-8") as f:
            self.db.script(f.read())
        self.players = {}  # numéro du compte -> Player : un seul objet par compte, partagé par ses sessions

    def close(self):
        self.db.close()

    def _player(self, row):
        player = self.players.get(row[0])
        if player is None:
            player = self.players[row[0]] = Player(row)
        return player

    def login(self, token, name, mod_version=0):
        """Le compte de ce jeton, créé s'il n'existe pas. None si le jeton n'a pas la bonne forme."""
        if not valid_token(token):
            return None
        digest = hashlib.sha256(token.encode("ascii")).hexdigest()
        name = clean_name(name)
        row = self.db.one(
            "INSERT INTO players (token, name, elo, peak, mod_version) VALUES (%s, %s, %s, %s, %s)"
            " ON CONFLICT (token) DO UPDATE SET name = EXCLUDED.name, mod_version = EXCLUDED.mod_version, last_seen = now()"
            " RETURNING " + _COLUMNS,
            (digest, name, ELO_START, ELO_START, mod_version))
        player = self._player(row)
        player.name = name
        self.db.execute(
            "INSERT INTO player_names (player_id, name) VALUES (%s, %s)"
            " ON CONFLICT (player_id, name) DO UPDATE SET last_seen = now()", (player.id, name))
        return player

    def record_match(self, sides, score, goals, kind="ranked", rated=True, forfeit_side=None, room_code=None,
                     league=None, started=None, stadium=None, half_seconds=None, mod_version=None, stats=None):
        """Enregistre un match. sides = deux dict (hôte, invité) : player (Player ou None), name, animal, clothes,
        tactic (chaîne de ModTactics.ToLine ou None). score = (buts de l'hôte, de l'invité). goals = le score retenu
        pour l'Elo et le résultat : le score, ou une défaite d'au moins un but d'écart pour un forfait.
        rated : l'Elo des deux comptes change (il faut deux comptes). stats : protocol.read_match_stats, ou None.
        Renvoie (numéro du match dans la base, (variation de l'hôte, de l'invité))."""
        host, guest = sides[0].get("player"), sides[1].get("player")
        rated = rated and host is not None and guest is not None
        deltas = (0, 0)
        if rated:
            deltas = (elo_delta(host.elo, host.games, guest.elo, goals[0], goals[1]),
                      elo_delta(guest.elo, guest.games, host.elo, goals[1], goals[0]))
        now = time.time()

        def body(c):
            match_id = c.execute(
                "INSERT INTO matches (played_at, started_at, duration_s, kind, rated, outcome, forfeit_side, room_code,"
                " league_id, league_key, stadium, half_seconds, score_host, score_guest, mod_version)"
                " VALUES (to_timestamp(%s), to_timestamp(%s), %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id",
                (now, started, int(now - started) if started else None, kind, rated,
                 "played" if forfeit_side is None else "forfeit", forfeit_side, room_code,
                 league[0] if league else None, league[1] if league else None, stadium, half_seconds,
                 score[0], score[1], mod_version)).fetchone()[0]
            for side, info in enumerate(sides):
                player = info.get("player")
                gf, ga = goals[side], goals[1 - side]
                before = after = None
                if rated:
                    before = player.elo
                    after = player.elo + deltas[side]
                    c.execute("UPDATE players SET elo = %s, games = games + 1, wins = wins + %s, draws = draws + %s,"
                              " losses = losses + %s, peak = GREATEST(peak, %s) WHERE id = %s",
                              (after, int(gf > ga), int(gf == ga), int(gf < ga), after, player.id))
                tactic = parse_tactic(info.get("tactic"))
                c.execute(
                    "INSERT INTO match_players (match_id, side, player_id, name, animal, clothes, goals, result,"
                    " elo_before, elo_after, delta, scheme, roles, layouts, tactic_raw)"
                    " VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
                    (match_id, side, player.id if player else None, info.get("name") or (player.name if player else "?"),
                     info.get("animal"), info.get("clothes"), score[side], _result(gf, ga), before, after, deltas[side],
                     tactic["scheme"] if tactic else None, tactic["roles"] if tactic else None,
                     json.dumps(tactic["layouts"]) if tactic else None, info.get("tactic") or None))
            if stats is not None:
                self._write_stats(c, match_id, stats)
            return match_id

        match_id = self.db.transaction(body)
        # la base a accepté : les objets en mémoire suivent
        if rated:
            for side, player in enumerate((host, guest)):
                gf, ga = goals[side], goals[1 - side]
                player.elo += deltas[side]
                player.games += 1
                player.wins += gf > ga
                player.draws += gf == ga
                player.losses += gf < ga
                player.peak = max(player.peak, player.elo)
        return match_id, deltas

    def record_possession(self, match_id, teams):
        """Temps de balle détaillé des matchs méta (teams[side] : clés de meta_run.DIAG_KEYS)."""
        def body(c):
            for side, t in enumerate(teams):
                c.execute(
                    "INSERT INTO match_possession (match_id, side, hold, loose, gk_hold, super_time, spells, spells_dead,"
                    " play, longest, game_counter) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)"
                    " ON CONFLICT (match_id, side) DO NOTHING",
                    (match_id, side, t["hold"], t["loose"], t["gk_hold"], t["super"], int(t["spells"]),
                     int(t["spells_dead"]), t["play"], t["longest"], t["game_counter"]))
        self.db.transaction(body)

    def attach_stats(self, match_id, stats):
        """Statistiques arrivées après l'enregistrement du match."""
        self.db.transaction(lambda c: self._write_stats(c, match_id, stats))

    def _write_stats(self, c, match_id, stats):
        if c.execute("SELECT has_stats FROM matches WHERE id = %s", (match_id,)).fetchone()[0]:
            return
        heat_d, heat_l = stats["heat_size"]
        c.execute("UPDATE matches SET has_stats = true, golden_goal = %s WHERE id = %s", (stats["golden_goal"], match_id))
        seq = 0
        for side, t in enumerate(stats["teams"]):
            roles = c.execute("SELECT roles FROM match_players WHERE match_id = %s AND side = %s", (match_id, side)).fetchone()
            roles = roles[0] if roles else None
            c.execute(
                "UPDATE match_players SET shots = %s, possession = %s, passes_ok = %s, passes = %s, recoveries = %s,"
                " saves = %s, supers = %s, super_types = %s, max_deficit = %s, heatmap = %s, ball_heatmap = %s,"
                " heat_depth = %s, heat_lateral = %s WHERE match_id = %s AND side = %s",
                (t["shots"], t["possession"], t["passes_ok"], t["passes"], t["recoveries"], t["saves"], t["supers"],
                 t["super_types"], t["max_deficit"], t["heatmap"], t["ball_heatmap"], heat_d, heat_l, match_id, side))
            own_goals = {}
            for g in stats["teams"][1 - side]["goals"]:
                if g["own_goal"] and g["number"] >= 0:
                    own_goals[g["number"]] = own_goals.get(g["number"], 0) + 1
            for p in t["players"]:
                slot = p["slot"]
                # jeu d'origine (mod v13) : défenseur ou attaquant du moteur, même si un schéma a été choisi
                role = p.get("origin_role") or (roles[slot] if roles and 0 <= slot < len(roles) else None)
                c.execute(
                    "INSERT INTO match_footballers (match_id, side, number, slot, role, goals, own_goals, shots, passes,"
                    " passes_ok, recoveries, distance, avg_depth, avg_lateral)"
                    " VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) ON CONFLICT DO NOTHING",
                    (match_id, side, p["number"], slot, role, p["goals"], own_goals.get(p["number"], 0), p["shots"],
                     p["passes"], p["passes_ok"], p["recoveries"], p["distance"], p["depth"], p["lateral"]))
            events = []
            for g in t["goals"]:
                # un csc est compté pour l'équipe qui en profite ; le joueur est de l'autre équipe
                events.append((g["minute"], side, "own_goal" if g["own_goal"] else "goal",
                               g["number"] if g["number"] >= 0 else None, None, None, None, None))
            for s in t["super_events"]:
                events.append((s["minute"], side, "super", None, s["type"], None, None, None))
            for s in t["shot_events"]:
                events.append((s["minute"], side, "shot", s["number"] if s["number"] >= 0 else None, None,
                               s["depth"], s["lateral"], s["result"]))
            for e in events:
                c.execute("INSERT INTO match_events (match_id, seq, minute, side, kind, number, super_type, pos_depth, pos_lateral, result)"
                          " VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)", (match_id, seq) + e)
                seq += 1

    def write_status(self, online, queued, rooms, live):
        self.db.execute(
            "INSERT INTO server_status (id, updated, online, queued, rooms, live_matches) VALUES (1, now(), %s, %s, %s, %s)"
            " ON CONFLICT (id) DO UPDATE SET updated = now(), online = EXCLUDED.online, queued = EXCLUDED.queued,"
            " rooms = EXCLUDED.rooms, live_matches = EXCLUDED.live_matches", (online, queued, rooms, live))

    _LIVE_COLUMNS = ("kind", "started_at", "host_id", "host_name", "host_animal", "host_elo", "guest_id", "guest_name",
                     "guest_animal", "guest_elo", "score_host", "score_guest", "minute", "half", "half_seconds", "stadium",
                     "spectators", "stats", "ended", "outcome", "match_id")

    def write_live(self, rows):
        """Remplace live_matches par ces lignes (rooms.Lobby.live_rows)."""
        def body(c):
            c.execute("DELETE FROM live_matches WHERE NOT (key = ANY(%s))", ([r["key"] for r in rows],))
            for r in rows:
                values = dict(r, started_at=r["started"], match_id=r["match_db"],
                              stats=json.dumps(r["stats"]) if r["stats"] is not None else None)
                cols = ", ".join(self._LIVE_COLUMNS)
                marks = ", ".join("to_timestamp(%s)" if k == "started_at" else ("%s::jsonb" if k == "stats" else "%s")
                                  for k in self._LIVE_COLUMNS)
                updates = ", ".join("%s = EXCLUDED.%s" % (k, k) for k in self._LIVE_COLUMNS)
                c.execute("INSERT INTO live_matches (key, updated, %s) VALUES (%%s, now(), %s)"
                          " ON CONFLICT (key) DO UPDATE SET updated = now(), %s" % (cols, marks, updates),
                          [r["key"]] + [values[k] for k in self._LIVE_COLUMNS])
        self.db.transaction(body)

    # --- classement : les joueurs qui ont au moins un match classé ---

    def top(self, count):
        rows = self.db.all(
            "SELECT %s FROM players WHERE games > 0 ORDER BY elo DESC, games DESC, id LIMIT %%s" % _COLUMNS, (count,))
        return [self._player(row) for row in rows]

    def total(self):
        return self.db.one("SELECT COUNT(*) FROM players WHERE games > 0")[0]

    def rank(self, player):
        """Rang du joueur à partir de 1, ou 0 s'il n'a pas encore de match classé."""
        if player.games == 0:
            return 0
        return 1 + self.db.one(
            "SELECT COUNT(*) FROM players WHERE games > 0 AND (elo > %s OR (elo = %s AND (games > %s OR (games = %s AND id < %s))))",
            (player.elo, player.elo, player.games, player.games, player.id))[0]
