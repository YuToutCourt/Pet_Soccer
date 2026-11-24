"""Site de stats Pet Soccer — classement, matchs, profil joueur, détail d'un match, méta des équipes et tactiques.

    python web\\web.py [--db postgresql://...] [--port 8080] [-v]
    gunicorn -b 0.0.0.0:8080 web:app        (docker-compose.yml : variable DATABASE_URL)

Lit la base Postgres du serveur de jeu (server/server.py, schéma dans server/schema.sql) en lecture seule : il ne
modifie rien. Dépendances : pip install -r web\\requirements.txt
"""
import argparse
import os
import time
from datetime import datetime

import psycopg
from psycopg.rows import dict_row
from flask import Flask, abort, g, redirect, render_template, request, url_for

DATABASE_URL = os.environ.get("DATABASE_URL")

# --- données du jeu ---

TEAMS = {
    0:  ("Bearchester United",     "🐻", "#8B5E3C", "T_Logo_Bearchester.png"),
    1:  ("Bullrusia Dortmund",     "🐂", "#FFD700", "T_Logo_Bullrusia.png"),
    2:  ("Deportivo la Chickeno",  "🐓", "#E8A020", "T_Logo_Chickena.png"),
    3:  ("Dynamo Tiranosaurus",    "🦕", "#4CAF50", "T_Logo_Dynamo.png"),
    4:  ("Frogen Leverkusen",      "🐸", "#2ECC71", "T_Logo_Frogen.png"),
    5:  ("Giraffinaikos",          "🦒", "#F4A261", "T_Logo_Giraffinaikos.png"),
    6:  ("Wisla Gorillakov",       "🦍", "#6C757D", "T_Logo_Gorilakov.png"),
    7:  ("Lyon Lions",             "🦁", "#E74C3C", "T_Logo_Lions.png"),
    8:  ("Ajax Panterdam",         "🐆", "#E67E22", "T_Logo_Panterdam.png"),
    9:  ("Shalke Sharks",          "🦈", "#3498DB", "T_Logo_Sharks.png"),
    10: ("Fc Turtelona",           "🐢", "#1ABC9C", "T_Logo_Turtelona.png"),
    11: ("As Robots",              "🤖", "#9B59B6", "T_Logo_Robots.png"),
    12: ("Real Pandrid",           "🐼", "#ECF0F1", "T_Logo_Pandrid.png"),
    13: ("Borussia Tigermund",     "🐯", "#F39C12", "T_Logo_Tigermund.png"),
}

# Joueurs de chaque équipe (texts.scr, Team<Animal>Players) : index 0 = gardien, 1..6 = joueurs de champ
FOOTBALLERS = {
    0: ["Beartez", "Silvet", "Cabell", "Owel", "Banc", "Honeys", "Furson"],
    1: ["Laman", "Worm", "Hornler", "Picken", "Biboc", "Dudu", "Buller"],
    2: ["Blufon", "Djalima", "Tistan", "Palbo", "Maycray", "Chickaldo", "Beako"],
    3: ["Shokolovsky", "Hoko", "Dmitrozaur", "Kardazaur", "Shevchoktyl", "Rexbrov", "Jurassov"],
    4: ["Bit", "Ziko", "Frogler", "Kissen", "Dissel", "Backlack", "Greenvald"],
    5: ["Nikolipokis", "Vitanodis", "Binasos", "Georgos", "Zebronis", "Giraffis", "Longneck"],
    6: ["Donkdek", "Gorilec", "Franco", "Kos", "Gulavik", "Rabbit", "Monkeysky"],
    7: ["Pupet", "Papaca", "Roaret", "Brucet", "Ardenson", "Clawinho", "Mane"],
    8: ["Glim", "Mixel", "VarDenVan", "Hopsam", "Pantasek", "VanPan", "Pinker"],
    9: ["Shreck", "Halok", "Valbok", "Nec", "Bierteeff", "Snack", "Sharptooth"],
    10: ["Banano", "Pulol", "Rolado", "Sorgi", "Turtelsson", "DeBro", "Shellbo"],
    11: ["Cajax", "Z-B", "Z-G", "R-M", "Batterystuta", "N-S", "R4"],
    12: ["Bambillas", "Hierpanda", "Bamberto Carlos", "Makebambou", "Zipandane", "Pandigo", "Raulpanda"],
    13: ["Tigermann", "Grrrns", "Rawrhl", "Stripicky", "Felinoso", "Kroller", "Rayuraldo"],
}

ROLES = {
    "LIBERO": "Libéro", "STOPPEUR": "Stoppeur", "DEFENSEUR": "Défenseur", "LATERAL": "Latéral",
    "MILIEU": "Milieu", "AILIER": "Ailier", "ATTAQUANT": "Attaquant", "PIVOT": "Pivot",
}
SCHEMES = {
    "ORIGINE": "Jeu d'origine", "EQUILIBRE": "Équilibre", "BETON": "Béton", "CONTRE": "Contre",
    "INDIVIDUEL": "Individuel", "ATTAQUE": "Attaque",
}
KINDS = {"ranked": "Classé", "private": "Salon privé", "league": "Ligue"}
SHOT_RESULTS = {"goal": "But", "saved": "Arrêté", "blocked": "Contré", "missed": "À côté"}

RANKS = [
    (1900, "Challenger", "#C471F5", "T_Rank_Challenger.png"),
    (1700, "Diamant",    "#5B9CF6", "T_Rank_Diamant.png"),
    (1500, "Platine",    "#72E5D8", "T_Rank_Platine.png"),
    (1300, "Or",         "#FFD700", "T_Rank_Or.png"),
    (1100, "Argent",     "#C0C0C0", "T_Rank_Argent.png"),
    (900,  "Bronze",     "#CD7F32", "T_Rank_Bronze.png"),
    (0,    "Bois",       "#A67C52", "T_Rank_Bois.png"),
]

RANK_KEYS = ["challenger", "diamant", "platinet", "or", "argent", "bronze", "bois"]


def get_rank(elo):
    for threshold, name, color, badge in RANKS:
        if elo >= threshold:
            return name, color, badge
    return RANKS[-1][1], RANKS[-1][2], RANKS[-1][3]


def get_rank_info(elo):
    """Returns (name, color, badge_filename, rank_key) for template use."""
    for i, (threshold, name, color, badge) in enumerate(RANKS):
        if elo >= threshold:
            return name, color, badge, RANK_KEYS[i]
    return RANKS[-1][1], RANKS[-1][2], RANKS[-1][3], RANK_KEYS[-1]


def team_name(idx):
    if idx is None:
        return "?", "❓", "#555", None
    t = TEAMS.get(idx)
    return (t[0], t[1], t[2], t[3]) if t else ("Équipe %d" % idx, "❓", "#555", None)


def team_info(idx):
    """Alias for team_name, used in templates."""
    return team_name(idx)


def footballer_name(animal, number):
    names = FOOTBALLERS.get(animal)
    if names is None or number is None or not 0 <= number < len(names):
        return "?"
    return names[number]


def fmt_date(ts):
    return datetime.fromtimestamp(ts).strftime("%d/%m/%Y %H:%M") if ts else "?"


def fmt_ago(ts):
    if not ts:
        return "?"
    delta = time.time() - ts
    if delta < 60:
        return "il y a %ds" % int(delta)
    if delta < 3600:
        return "il y a %dmin" % int(delta / 60)
    if delta < 86400:
        return "il y a %dh" % int(delta / 3600)
    return "il y a %dj" % int(delta / 86400)


def pct(a, b):
    return round(100.0 * a / b) if b else 0


# --- base ---

def db():
    """Une connexion par requête, en lecture seule."""
    if "db" not in g:
        g.db = psycopg.connect(DATABASE_URL, row_factory=dict_row, autocommit=True,
                               options="-c default_transaction_read_only=on")
    return g.db


def q_all(sql, params=()):
    return db().execute(sql, params).fetchall()


def q_one(sql, params=()):
    return db().execute(sql, params).fetchone()


# Un match vu de ses deux côtés, colonnes attendues par les templates (hôte = côté 0)
MATCH_SELECT = """
    SELECT m.id, extract(epoch FROM m.played_at)::float8 AS played, m.kind, m.rated, m.outcome,
           m.outcome = 'forfeit' AS forfeit, m.forfeit_side, m.score_host, m.score_guest, m.has_stats,
           m.golden_goal, m.duration_s, m.stadium, m.room_code,
           h.player_id AS host_id, h.name AS host_name, h.animal AS team_host, h.delta AS delta_host, h.scheme AS scheme_host,
           gu.player_id AS guest_id, gu.name AS guest_name, gu.animal AS team_guest, gu.delta AS delta_guest, gu.scheme AS scheme_guest
    FROM matches m
    JOIN match_players h ON h.match_id = m.id AND h.side = 0
    JOIN match_players gu ON gu.match_id = m.id AND gu.side = 1
"""


def server_status():
    try:
        row = q_one("SELECT online, queued, live_matches, extract(epoch FROM updated)::float8 AS updated FROM server_status")
    except psycopg.Error:
        return None
    if row is None or time.time() - row["updated"] > 60:
        return None  # serveur de jeu arrêté
    return row


def elo_sparkline_points(history):
    """Renvoie (polyline_pts, polygon_pts) pour l'SVG de la courbe Elo."""
    if len(history) < 2:
        return "", ""
    mn, mx = min(history), max(history)
    span = mx - mn if mx != mn else 1
    n = len(history)
    pts = []
    for i, v in enumerate(history):
        x = i / (n - 1) * 398
        y = 76 - ((v - mn) / span * 72)
        pts.append("%.1f,%.1f" % (x, y))
    poly = " ".join(pts)
    polygon = poly + " %.1f,80 1,80" % 398
    return poly, polygon


# --- Flask app ---

app = Flask(__name__)


@app.teardown_appcontext
def close_db(exc):
    conn = g.pop("db", None)
    if conn is not None:
        conn.close()


@app.context_processor
def utility_processor():
    return dict(
        get_rank=get_rank, get_rank_info=get_rank_info, team_name=team_name, team_info=team_info,
        footballer_name=footballer_name, fmt_date=fmt_date, fmt_ago=fmt_ago, enumerate=enumerate, pct=pct,
        RANK_KEYS=RANK_KEYS, ROLES=ROLES, SCHEMES=SCHEMES, KINDS=KINDS, SHOT_RESULTS=SHOT_RESULTS,
        status=server_status(),
    )


@app.route("/")
def index():
    query = request.args.get("q", "").strip()
    sql = """
        SELECT p.id, p.name, p.elo, p.games, p.wins, p.draws, p.losses, p.peak,
               extract(epoch FROM p.last_seen)::float8 AS last_seen,
               (SELECT mp.animal FROM match_players mp WHERE mp.player_id = p.id AND mp.animal IS NOT NULL
                GROUP BY mp.animal ORDER BY count(*) DESC LIMIT 1) AS team
        FROM players p WHERE p.games > 0 {filter}
        ORDER BY p.elo DESC, p.games DESC, p.id LIMIT 200"""
    if query:
        rows = q_all(sql.format(filter="AND p.name ILIKE %s"), ("%" + query + "%",))
    else:
        rows = q_all(sql.format(filter=""))
    total_players = q_one("SELECT COUNT(*) AS n FROM players WHERE games > 0")["n"]

    players = []
    for i, p in enumerate(rows):
        rname, rcolor, rbadge, rkey = get_rank_info(p["elo"])
        tn, te, tc, tl = team_name(p["team"])
        players.append(dict(p, rank=i + 1, total=p["games"], rank_name=rname, rank_color=rcolor, rank_badge=rbadge,
                            rank_key=rkey, team=tn if p["team"] is not None else "", team_emoji=te, team_color=tc,
                            team_logo=tl))
    if query and len(players) == 1:
        return redirect(url_for("player", player_id=players[0]["id"]))
    return render_template("index.html", players=players, total_players=total_players, query=query)


@app.route("/matches")
def matches():
    page = max(1, request.args.get("p", 1, type=int))
    kind = request.args.get("kind")
    if kind not in KINDS:
        kind = None
    per_page = 30
    where = "WHERE m.kind = %s" if kind else ""
    params = (kind,) if kind else ()
    total = q_one("SELECT COUNT(*) AS n FROM matches m " + where, params)["n"]
    pages = max(1, (total + per_page - 1) // per_page)
    page = min(page, pages)
    rows = q_all(MATCH_SELECT + where + " ORDER BY m.played_at DESC LIMIT %s OFFSET %s",
                 params + (per_page, (page - 1) * per_page))
    return render_template("matches.html", matches=rows, total=total, cur_page=page, pages=pages, kind=kind)


@app.route("/match/<int:match_id>")
def match(match_id):
    m = q_one(MATCH_SELECT + " WHERE m.id = %s", (match_id,))
    if m is None:
        abort(404)
    sides = q_all("SELECT * FROM match_players WHERE match_id = %s ORDER BY side", (match_id,))
    footballers = q_all("SELECT * FROM match_footballers WHERE match_id = %s ORDER BY side, number", (match_id,))
    events = q_all("SELECT * FROM match_events WHERE match_id = %s ORDER BY minute, seq", (match_id,))
    teams = []
    for s in sides:
        team = dict(s, footballers=[f for f in footballers if f["side"] == s["side"]],
                    shots_list=[e for e in events if e["side"] == s["side"] and e["kind"] == "shot"])
        # placement prévu (carte tactique) par place, pour comparer à la position moyenne réelle
        layout = (s["layouts"] or {}).get("play") if s["layouts"] else None
        for f in team["footballers"]:
            f["planned"] = layout[f["slot"]] if layout and f["slot"] is not None and 0 <= f["slot"] < len(layout) else None
            f["display_name"] = footballer_name(s["animal"], f["number"])
        heat = s["heatmap"]
        if heat and s["heat_depth"]:
            team["heat_grid"] = [heat[d * s["heat_lateral"]:(d + 1) * s["heat_lateral"]] for d in range(s["heat_depth"])]
        ball = s["ball_heatmap"]
        if ball and s["heat_depth"]:
            team["ball_grid"] = [ball[d * s["heat_lateral"]:(d + 1) * s["heat_lateral"]] for d in range(s["heat_depth"])]
        teams.append(team)
    timeline = []
    for e in events:
        if e["kind"] == "shot":
            continue
        side_animal = sides[e["side"]]["animal"] if e["side"] < len(sides) else None
        if e["kind"] == "own_goal":
            # le joueur qui marque contre son camp est de l'autre équipe
            other = sides[1 - e["side"]]["animal"] if len(sides) == 2 else None
            who = footballer_name(other, e["number"])
        else:
            who = footballer_name(side_animal, e["number"]) if e["number"] is not None else None
        timeline.append(dict(e, who=who))
    return render_template("match.html", m=m, teams=teams, timeline=timeline)


@app.route("/player/<int:player_id>")
def player(player_id):
    p = q_one("""SELECT id, name, elo, games, wins, draws, losses, peak,
                        extract(epoch FROM created)::float8 AS created, extract(epoch FROM last_seen)::float8 AS last_seen
                 FROM players WHERE id = %s""", (player_id,))
    if p is None:
        abort(404)
    matches = q_all(MATCH_SELECT + " WHERE h.player_id = %s OR gu.player_id = %s ORDER BY m.played_at DESC LIMIT 20",
                    (player_id, player_id))
    rank_pos = 1 + q_one("SELECT COUNT(*) AS n FROM players WHERE games > 0 AND elo > %s", (p["elo"],))["n"]
    total_players = q_one("SELECT COUNT(*) AS n FROM players WHERE games > 0")["n"]

    rated = q_all("""SELECT mp.elo_before, mp.elo_after FROM match_players mp JOIN matches m ON m.id = mp.match_id
                     WHERE mp.player_id = %s AND m.rated ORDER BY m.played_at DESC LIMIT 50""", (player_id,))
    rated.reverse()
    history = ([rated[0]["elo_before"]] + [r["elo_after"] for r in rated]) if rated else []
    spark_line, spark_poly = elo_sparkline_points(history)

    # équipes jouées, avec leur bilan
    team_rows = q_all("""SELECT animal, COUNT(*) AS n, SUM((result = 'W')::int) AS w FROM match_players
                         WHERE player_id = %s AND animal IS NOT NULL GROUP BY animal ORDER BY n DESC LIMIT 5""", (player_id,))
    total_team = sum(r["n"] for r in team_rows) or 1
    team_stats = [(r["animal"], r["n"], round(100 * r["n"] / total_team), pct(r["w"], r["n"])) for r in team_rows]
    schemes = q_all("""SELECT scheme, COUNT(*) AS n, SUM((result = 'W')::int) AS w FROM match_players
                       WHERE player_id = %s AND scheme IS NOT NULL GROUP BY scheme ORDER BY n DESC LIMIT 6""", (player_id,))
    averages = q_one("""SELECT COUNT(*) AS n, AVG(shots) AS shots, AVG(possession) AS possession,
                               SUM(passes_ok) AS passes_ok, SUM(passes) AS passes, AVG(recoveries) AS recoveries,
                               AVG(saves) AS saves, AVG(goals) AS goals, MAX(goals) AS max_goals
                        FROM match_players WHERE player_id = %s AND shots IS NOT NULL""", (player_id,))
    form = [r["result"] for r in q_all("""SELECT mp.result FROM match_players mp JOIN matches m ON m.id = mp.match_id
                                          WHERE mp.player_id = %s ORDER BY m.played_at DESC LIMIT 10""", (player_id,))]
    animal = TEAMS[team_rows[0]["animal"]][1] if team_rows and team_rows[0]["animal"] in TEAMS else "⚽"
    return render_template("player.html", p=p, matches=matches, rank_pos=rank_pos, total_players=total_players,
                           elo_history=history, spark_line=spark_line, spark_poly=spark_poly, team_stats=team_stats,
                           schemes=schemes, averages=averages, form=form, animal=animal)


@app.route("/player/<name>")
def player_by_name(name):
    row = q_one("SELECT id FROM players WHERE name = %s ORDER BY games DESC LIMIT 1", (name,)) or \
        q_one("SELECT id FROM players WHERE lower(name) = lower(%s) ORDER BY games DESC LIMIT 1", (name,))
    if row is None:
        abort(404)
    return redirect(url_for("player", player_id=row["id"]))


@app.route("/meta")
def meta():
    """Taux de choix et de victoire des équipes, des schémas et des rôles ; équipe contre équipe."""
    kind = request.args.get("kind")
    if kind not in KINDS:
        kind = None
    where = "WHERE m.kind = %s" if kind else ""
    params = (kind,) if kind else ()
    total = q_one("SELECT COUNT(*) AS n FROM matches m " + where, params)["n"]
    base = "FROM match_players mp JOIN matches m ON m.id = mp.match_id " + where
    teams = q_all("""SELECT mp.animal, COUNT(*) AS n, SUM((mp.result = 'W')::int) AS w, SUM((mp.result = 'D')::int) AS d,
                            AVG(mp.goals) AS goals, AVG(mp.possession) AS possession """ + base +
                  (" AND" if where else " WHERE") + " mp.animal IS NOT NULL GROUP BY mp.animal ORDER BY n DESC", params)
    schemes = q_all("""SELECT mp.scheme, COUNT(*) AS n, SUM((mp.result = 'W')::int) AS w """ + base +
                    (" AND" if where else " WHERE") + " mp.scheme IS NOT NULL GROUP BY mp.scheme ORDER BY n DESC", params)
    roles = q_all("""SELECT r.role, COUNT(*) AS n, SUM((mp.result = 'W')::int) AS w
                     FROM match_players mp JOIN matches m ON m.id = mp.match_id, unnest(mp.roles) AS r(role) """ + where +
                  " GROUP BY r.role ORDER BY n DESC", params)
    matchups = q_all("""SELECT a.animal AS a, b.animal AS b, COUNT(*) AS n, SUM((a.result = 'W')::int) AS w
                        FROM match_players a JOIN match_players b ON b.match_id = a.match_id AND b.side <> a.side
                        JOIN matches m ON m.id = a.match_id """ + where +
                     (" AND" if where else " WHERE") + " a.animal IS NOT NULL AND b.animal IS NOT NULL GROUP BY a.animal, b.animal",
                     params)
    scorers = q_all("""SELECT mp.animal, f.number, SUM(f.goals) AS goals, SUM(f.shots) AS shots, COUNT(*) AS n
                       FROM match_footballers f JOIN match_players mp ON mp.match_id = f.match_id AND mp.side = f.side
                       JOIN matches m ON m.id = f.match_id """ + where +
                    " GROUP BY mp.animal, f.number HAVING SUM(f.goals) > 0 ORDER BY goals DESC LIMIT 10", params)
    grid = {(r["a"], r["b"]): r for r in matchups}
    animals = sorted({r["animal"] for r in teams})
    return render_template("meta.html", total=total, teams=teams, schemes=schemes, roles=roles, grid=grid,
                           animals=animals, scorers=scorers, kind=kind)


@app.errorhandler(404)
def not_found(e):
    return render_template("base.html",
                           content="<div class='empty' style='margin-top:60px'>Introuvable.</div>"), 404


def main():
    global DATABASE_URL
    ap = argparse.ArgumentParser(description="Serveur web de stats Pet Soccer.")
    ap.add_argument("--db", default=DATABASE_URL, help="base Postgres (par défaut : la variable DATABASE_URL)")
    ap.add_argument("--port", type=int, default=8080)
    ap.add_argument("--host", default="0.0.0.0")
    ap.add_argument("-v", "--verbose", action="store_true")
    args = ap.parse_args()
    if not args.db:
        ap.error("donner la base : --db postgresql://... ou la variable DATABASE_URL")
    DATABASE_URL = args.db
    print("Pet Soccer Stats sur http://localhost:%d" % args.port)
    app.run(host=args.host, port=args.port, debug=args.verbose)


if __name__ == "__main__":
    main()
