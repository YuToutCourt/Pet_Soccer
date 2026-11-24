"""Serveur web de stats Pet Soccer — classement, historique des matchs, profil joueur.

    python server\\web.py [--db fichier] [--port 8080] [-v]

Lit players.db en lecture seule (ne modifie rien). Lance séparément du serveur de jeu.
Installer Flask d'abord si besoin : pip install flask
"""
import argparse
import os
import sqlite3
import time
from datetime import datetime

from flask import Flask, abort, redirect, render_template_string, request, url_for

DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "players.db")

# --- données du jeu ---

TEAMS = {
    0:  ("Bearchester United",     "🐻", "#8B5E3C"),
    1:  ("Bullrusia Dortmund",     "🐂", "#FFD700"),
    2:  ("Deportivo la Chickeno",  "🐓", "#E8A020"),
    3:  ("Dynamo Tiranosaurus",    "🦕", "#4CAF50"),
    4:  ("Frogen Leverkusen",      "🐸", "#2ECC71"),
    5:  ("Giraffinaikos",          "🦒", "#F4A261"),
    6:  ("Wisla Gorillakov",       "🦍", "#6C757D"),
    7:  ("Lyon Lions",             "🦁", "#E74C3C"),
    8:  ("Ajax Panterdam",         "🐆", "#E67E22"),
    9:  ("Shalke Sharks",          "🦈", "#3498DB"),
    10: ("Fc Turtelona",           "🐢", "#1ABC9C"),
    11: ("As Robots",              "🤖", "#9B59B6"),
    12: ("Real Pandrid",           "🐼", "#ECF0F1"),
    13: ("Borussia Tigermund",     "🐯", "#F39C12"),
}

RANKS = [
    (1900, "Challenger", "#C471F5"),
    (1700, "Diamant",    "#5B9CF6"),
    (1500, "Platine",    "#72E5D8"),
    (1300, "Or",         "#FFD700"),
    (1100, "Argent",     "#C0C0C0"),
    (900,  "Bronze",     "#CD7F32"),
    (0,    "Bois",       "#A67C52"),
]


def get_rank(elo):
    for threshold, name, color in RANKS:
        if elo >= threshold:
            return name, color
    return RANKS[-1][1], RANKS[-1][2]


def team_name(idx):
    if idx is None:
        return "?", "❓", "#555"
    t = TEAMS.get(idx)
    return (t[0], t[1], t[2]) if t else ("Équipe %d" % idx, "❓", "#555")


def fmt_date(ts):
    return datetime.fromtimestamp(ts).strftime("%d/%m/%Y %H:%M")


def fmt_ago(ts):
    delta = time.time() - ts
    if delta < 60:
        return "il y a %ds" % int(delta)
    if delta < 3600:
        return "il y a %dmin" % int(delta / 60)
    if delta < 86400:
        return "il y a %dh" % int(delta / 3600)
    return "il y a %dj" % int(delta / 86400)


# --- requêtes DB ---

def db_connect():
    db = sqlite3.connect("file:%s?mode=ro" % DB_PATH, uri=True)
    db.row_factory = sqlite3.Row
    return db


def get_leaderboard(db, limit=100):
    return db.execute("""
        SELECT id, name, elo, games, wins, draws, losses, peak, last_seen
        FROM players WHERE games > 0
        ORDER BY elo DESC, games DESC, id ASC
        LIMIT ?""", (limit,)).fetchall()


def get_all_players(db):
    return db.execute("""
        SELECT id, name, elo, games, wins, draws, losses, peak, last_seen
        FROM players ORDER BY elo DESC""").fetchall()


def get_matches(db, limit=50, offset=0):
    return db.execute("""
        SELECT m.id, m.played, m.score_host, m.score_guest, m.forfeit,
               m.delta_host, m.delta_guest, m.team_host, m.team_guest,
               ph.id AS host_id, ph.name AS host_name,
               pg.id AS guest_id, pg.name AS guest_name
        FROM matches m
        JOIN players ph ON ph.id = m.host
        JOIN players pg ON pg.id = m.guest
        ORDER BY m.played DESC
        LIMIT ? OFFSET ?""", (limit, offset)).fetchall()


def count_matches(db):
    return db.execute("SELECT COUNT(*) FROM matches").fetchone()[0]


def get_player(db, player_id):
    return db.execute("""
        SELECT id, name, elo, games, wins, draws, losses, peak, created, last_seen
        FROM players WHERE id = ?""", (player_id,)).fetchone()


def get_player_by_name(db, name):
    return db.execute("""
        SELECT id FROM players WHERE name = ? COLLATE NOCASE""", (name,)).fetchone()


def get_player_matches(db, player_id, limit=20):
    return db.execute("""
        SELECT m.id, m.played, m.score_host, m.score_guest, m.forfeit,
               m.delta_host, m.delta_guest, m.team_host, m.team_guest,
               ph.id AS host_id, ph.name AS host_name,
               pg.id AS guest_id, pg.name AS guest_name
        FROM matches m
        JOIN players ph ON ph.id = m.host
        JOIN players pg ON pg.id = m.guest
        WHERE m.host = ? OR m.guest = ?
        ORDER BY m.played DESC
        LIMIT ?""", (player_id, player_id, limit)).fetchall()


def get_player_rank(db, player_id, elo):
    row = db.execute("""
        SELECT COUNT(*) FROM players
        WHERE games > 0 AND elo > ?""", (elo,)).fetchone()
    return row[0] + 1


def player_elo_history(matches, player_id, current_elo):
    """Reconstruit l'historique Elo d'un joueur à partir de ses matchs (tri chrono)."""
    rows = sorted(matches, key=lambda r: r["played"])
    deltas = []
    for m in rows:
        d = m["delta_host"] if m["host_id"] == player_id else m["delta_guest"]
        deltas.append(d)
    # on remonte de l'Elo actuel en soustrayant les deltas (du plus récent au plus ancien)
    elo = current_elo
    history = [elo]
    for d in reversed(deltas):
        elo -= d
        history.append(elo)
    history.reverse()
    return history


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
    last_x = 398
    polygon = poly + " %.1f,80 1,80" % last_x
    return poly, polygon


def player_team_stats(matches, player_id):
    counts = {}
    for m in matches:
        if m["host_id"] == player_id:
            t = m["team_host"]
        else:
            t = m["team_guest"]
        if t is not None:
            counts[t] = counts.get(t, 0) + 1
    if not counts:
        return []
    total = sum(counts.values())
    return sorted([(t, n, round(100 * n / total)) for t, n in counts.items()], key=lambda x: -x[1])[:5]


# --- Flask app ---

app = Flask(__name__)


BASE_CSS = """
:root {
  --bg: #0f1119;
  --card: #1a1f2e;
  --card2: #242938;
  --border: #2d3348;
  --accent: #5b6bf8;
  --accent2: #7c8ff8;
  --text: #c8ccd8;
  --text2: #858ca8;
  --win: #3dba78;
  --loss: #e74c5a;
  --draw: #f5a623;
  --gold: #FFD700;
}
* { box-sizing: border-box; margin: 0; padding: 0; }
body { background: var(--bg); color: var(--text); font-family: 'Segoe UI', system-ui, sans-serif; font-size: 14px; }
a { color: var(--accent2); text-decoration: none; }
a:hover { color: white; text-decoration: underline; }

.nav { background: var(--card); border-bottom: 1px solid var(--border); padding: 0 24px; display: flex; align-items: center; gap: 24px; height: 52px; position: sticky; top: 0; z-index: 100; }
.nav-logo { font-size: 18px; font-weight: 700; color: white; letter-spacing: .5px; }
.nav-logo span { color: var(--accent); }
.nav a { color: var(--text2); font-weight: 500; padding: 6px 0; border-bottom: 2px solid transparent; }
.nav a:hover, .nav a.active { color: white; border-bottom-color: var(--accent); text-decoration: none; }

.container { max-width: 1100px; margin: 0 auto; padding: 28px 16px; }

h1 { font-size: 22px; font-weight: 700; color: white; margin-bottom: 20px; }
h2 { font-size: 16px; font-weight: 600; color: white; margin-bottom: 14px; }

.card { background: var(--card); border: 1px solid var(--border); border-radius: 10px; padding: 20px; margin-bottom: 20px; }

table { width: 100%; border-collapse: collapse; }
th { text-align: left; padding: 10px 12px; color: var(--text2); font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: .5px; border-bottom: 1px solid var(--border); }
td { padding: 10px 12px; border-bottom: 1px solid var(--border); vertical-align: middle; }
tr:last-child td { border-bottom: none; }
tr:hover td { background: rgba(255,255,255,.025); }

.rank-num { color: var(--text2); font-weight: 600; font-size: 13px; width: 36px; }
.rank-num.top1 { color: var(--gold); }
.rank-num.top2 { color: #C0C0C0; }
.rank-num.top3 { color: #CD7F32; }

.badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 700; letter-spacing: .3px; }
.elo-val { font-size: 15px; font-weight: 700; color: white; }
.wl { display: flex; gap: 6px; align-items: center; }
.wl-bar { height: 6px; border-radius: 3px; }
.w { color: var(--win); }
.l { color: var(--loss); }
.d { color: var(--draw); }
.score { font-size: 18px; font-weight: 800; color: white; }
.score-sep { color: var(--text2); margin: 0 2px; }
.delta-pos { color: var(--win); font-weight: 700; }
.delta-neg { color: var(--loss); font-weight: 700; }
.delta-zero { color: var(--text2); }
.forfeit-tag { background: #3a1a1a; color: var(--loss); border-radius: 4px; padding: 1px 5px; font-size: 10px; font-weight: 700; }
.team-tag { font-size: 12px; color: var(--text2); display: inline-flex; align-items: center; gap: 4px; }
.date-cell { color: var(--text2); font-size: 12px; white-space: nowrap; }
.player-link { font-weight: 600; color: white; }
.player-link:hover { color: var(--accent2); }

.profile-header { display: flex; gap: 20px; align-items: flex-start; }
.profile-avatar { width: 72px; height: 72px; border-radius: 50%; background: var(--card2); display: flex; align-items: center; justify-content: center; font-size: 36px; flex-shrink: 0; }
.profile-name { font-size: 24px; font-weight: 800; color: white; }
.profile-meta { color: var(--text2); font-size: 13px; margin-top: 4px; }
.profile-rank-badge { margin-top: 8px; }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(100px, 1fr)); gap: 12px; }
.stat-box { background: var(--card2); border-radius: 8px; padding: 12px 14px; }
.stat-label { font-size: 11px; color: var(--text2); text-transform: uppercase; letter-spacing: .4px; }
.stat-val { font-size: 20px; font-weight: 800; color: white; margin-top: 2px; }
.stat-val.win-color { color: var(--win); }
.stat-val.loss-color { color: var(--loss); }

.elo-chart-wrap { position: relative; height: 80px; margin: 10px 0; }
.elo-chart-wrap svg { width: 100%; height: 100%; overflow: visible; }

.match-row { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid var(--border); }
.match-row:last-child { border-bottom: none; }
.match-result-tag { width: 28px; height: 28px; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 800; flex-shrink: 0; }
.result-W { background: rgba(61,186,120,.2); color: var(--win); }
.result-L { background: rgba(231,76,90,.2); color: var(--loss); }
.result-D { background: rgba(245,166,35,.2); color: var(--draw); }
.match-score { font-size: 16px; font-weight: 800; color: white; min-width: 36px; text-align: center; }
.match-opp { font-size: 13px; font-weight: 600; }
.match-elo-delta { font-size: 13px; font-weight: 700; margin-left: auto; }
.match-date { font-size: 11px; color: var(--text2); }

.pagination { display: flex; gap: 8px; justify-content: center; margin-top: 20px; }
.pagination a, .pagination span { padding: 6px 14px; border-radius: 6px; background: var(--card2); border: 1px solid var(--border); font-size: 13px; }
.pagination a:hover { background: var(--border); color: white; text-decoration: none; }
.pagination .current { background: var(--accent); color: white; border-color: var(--accent); }

.search-bar { display: flex; gap: 10px; margin-bottom: 20px; }
.search-bar input { flex: 1; background: var(--card2); border: 1px solid var(--border); border-radius: 8px; padding: 8px 14px; color: var(--text); font-size: 14px; }
.search-bar input:focus { outline: none; border-color: var(--accent); }
.search-bar button { background: var(--accent); color: white; border: none; border-radius: 8px; padding: 8px 18px; font-weight: 600; cursor: pointer; }

.empty { color: var(--text2); text-align: center; padding: 40px; font-size: 15px; }
"""

NAV_TEMPLATE = """<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{% block title %}Pet Soccer Stats{% endblock %}</title>
<style>{{ css }}</style>
</head>
<body>
<nav class="nav">
  <span class="nav-logo">Pet <span>Soccer</span> Stats</span>
  <a href="{{ url_for('index') }}" class="{{ 'active' if page=='home' }}">Classement</a>
  <a href="{{ url_for('matches') }}" class="{{ 'active' if page=='matches' }}">Matchs</a>
</nav>
<div class="container">
{% block content %}{% endblock %}
</div>
</body>
</html>"""

INDEX_TEMPLATE = NAV_TEMPLATE.replace("{% block content %}{% endblock %}", """
{% block content %}
<h1>Classement</h1>
<div class="card" style="padding:0">
<table>
<thead><tr>
  <th>#</th><th>Joueur</th><th>Rang</th><th>Elo</th>
  <th>Parties</th><th>V / N / D</th><th>% Win</th><th>Pic</th><th>Vu</th>
</tr></thead>
<tbody>
{% for p in players %}
{% set rname, rcolor = get_rank(p['elo']) %}
{% set wr = (p['wins'] * 100 // p['games']) if p['games'] else 0 %}
<tr>
  <td class="rank-num {{ 'top1' if loop.index==1 else ('top2' if loop.index==2 else ('top3' if loop.index==3 else '')) }}">{{ loop.index }}</td>
  <td><a class="player-link" href="{{ url_for('player', player_id=p['id']) }}">{{ p['name'] }}</a></td>
  <td><span class="badge" style="background:{{ rcolor }}22;color:{{ rcolor }}">{{ rname }}</span></td>
  <td><span class="elo-val">{{ p['elo'] }}</span></td>
  <td style="color:var(--text2)">{{ p['games'] }}</td>
  <td>
    <span class="w">{{ p['wins'] }}V</span>
    <span style="color:var(--text2)"> / </span>
    <span class="d">{{ p['draws'] }}N</span>
    <span style="color:var(--text2)"> / </span>
    <span class="l">{{ p['losses'] }}D</span>
  </td>
  <td>
    <span style="color:{{ 'var(--win)' if wr>=55 else ('var(--loss)' if wr<45 else 'var(--draw)') }}; font-weight:600">{{ wr }}%</span>
  </td>
  <td style="color:var(--text2)">{{ p['peak'] }}</td>
  <td class="date-cell">{{ fmt_ago(p['last_seen']) }}</td>
</tr>
{% else %}
<tr><td colspan="9" class="empty">Aucun joueur classé pour le moment.</td></tr>
{% endfor %}
</tbody>
</table>
</div>
{% endblock %}""")

MATCHES_TEMPLATE = NAV_TEMPLATE.replace("{% block content %}{% endblock %}", """
{% block content %}
<h1>Historique des matchs <small style="font-size:14px;color:var(--text2)">({{ total }} au total)</small></h1>
<div class="card" style="padding:0">
<table>
<thead><tr>
  <th>Date</th>
  <th>Hôte</th><th>Équipe hôte</th>
  <th style="text-align:center">Score</th>
  <th>Équipe invité</th><th>Invité</th>
  <th>Elo Δ</th><th></th>
</tr></thead>
<tbody>
{% for m in matches %}
{% set hn, he, hc = team_name(m['team_host']) %}
{% set gn, ge, gc = team_name(m['team_guest']) %}
<tr>
  <td class="date-cell">{{ fmt_date(m['played']) }}</td>
  <td><a class="player-link" href="{{ url_for('player', player_id=m['host_id']) }}">{{ m['host_name'] }}</a></td>
  <td><span class="team-tag" title="{{ hn }}"><span style="color:{{ hc }}">{{ he }}</span> {{ hn }}</span></td>
  <td style="text-align:center">
    <span class="score">{{ m['score_host'] }}<span class="score-sep">–</span>{{ m['score_guest'] }}</span>
  </td>
  <td><span class="team-tag" title="{{ gn }}"><span style="color:{{ gc }}">{{ ge }}</span> {{ gn }}</span></td>
  <td><a class="player-link" href="{{ url_for('player', player_id=m['guest_id']) }}">{{ m['guest_name'] }}</a></td>
  <td>
    <span class="{{ 'delta-pos' if m['delta_host']>0 else ('delta-neg' if m['delta_host']<0 else 'delta-zero') }}">
      {{ '+' if m['delta_host']>0 else '' }}{{ m['delta_host'] }}
    </span>
    <span style="color:var(--text2)"> / </span>
    <span class="{{ 'delta-pos' if m['delta_guest']>0 else ('delta-neg' if m['delta_guest']<0 else 'delta-zero') }}">
      {{ '+' if m['delta_guest']>0 else '' }}{{ m['delta_guest'] }}
    </span>
  </td>
  <td>{% if m['forfeit'] %}<span class="forfeit-tag">FF</span>{% endif %}</td>
</tr>
{% else %}
<tr><td colspan="8" class="empty">Aucun match enregistré.</td></tr>
{% endfor %}
</tbody>
</table>
</div>

<div class="pagination">
{% if cur_page > 1 %}<a href="{{ url_for('matches', p=cur_page-1) }}">‹ Préc.</a>{% endif %}
{% for i in range(1, pages+1) %}
  {% if i == cur_page %}<span class="current">{{ i }}</span>
  {% elif i <= 2 or i >= pages-1 or (i >= cur_page-2 and i <= cur_page+2) %}
    <a href="{{ url_for('matches', p=i) }}">{{ i }}</a>
  {% elif i == 3 or i == pages-2 %}<span>…</span>{% endif %}
{% endfor %}
{% if cur_page < pages %}<a href="{{ url_for('matches', p=cur_page+1) }}">Suiv. ›</a>{% endif %}
</div>
{% endblock %}""")

PLAYER_TEMPLATE = NAV_TEMPLATE.replace("{% block content %}{% endblock %}", """
{% block content %}
{% set rname, rcolor = get_rank(p['elo']) %}
{% set wr = (p['wins'] * 100 // p['games']) if p['games'] else 0 %}

<div class="card">
  <div class="profile-header">
    <div class="profile-avatar">{{ animal }}</div>
    <div>
      <div class="profile-name">{{ p['name'] }}</div>
      <div class="profile-meta">
        Inscrit le {{ fmt_date(p['created']) }} · Vu {{ fmt_ago(p['last_seen']) }}
        · Rang <strong style="color:white">#{{ rank_pos }}</strong> sur {{ total_players }}
      </div>
      <div class="profile-rank-badge">
        <span class="badge" style="background:{{ rcolor }}22;color:{{ rcolor }};font-size:13px;padding:4px 12px">
          {{ rname }} · {{ p['elo'] }} Elo
        </span>
        {% if p['peak'] > p['elo'] %}
        <span style="color:var(--text2);font-size:12px;margin-left:8px">Pic : {{ p['peak'] }}</span>
        {% endif %}
      </div>
    </div>
  </div>
</div>

<div class="card">
  <h2>Statistiques</h2>
  <div class="stats-grid">
    <div class="stat-box"><div class="stat-label">Parties</div><div class="stat-val">{{ p['games'] }}</div></div>
    <div class="stat-box"><div class="stat-label">Victoires</div><div class="stat-val win-color">{{ p['wins'] }}</div></div>
    <div class="stat-box"><div class="stat-label">Nuls</div><div class="stat-val" style="color:var(--draw)">{{ p['draws'] }}</div></div>
    <div class="stat-box"><div class="stat-label">Défaites</div><div class="stat-val loss-color">{{ p['losses'] }}</div></div>
    <div class="stat-box"><div class="stat-label">% Victoires</div>
      <div class="stat-val" style="color:{{ 'var(--win)' if wr>=55 else ('var(--loss)' if wr<45 else 'var(--draw)') }}">{{ wr }}%</div>
    </div>
    <div class="stat-box"><div class="stat-label">Elo actuel</div><div class="stat-val" style="color:{{ rcolor }}">{{ p['elo'] }}</div></div>
    <div class="stat-box"><div class="stat-label">Pic Elo</div><div class="stat-val" style="color:var(--gold)">{{ p['peak'] }}</div></div>
  </div>

  {% if spark_line %}
  <div style="margin-top:16px">
    <div style="font-size:12px;color:var(--text2);margin-bottom:6px">Courbe Elo</div>
    <div class="elo-chart-wrap">
      <svg viewBox="0 0 400 80" preserveAspectRatio="none">
        <defs>
          <linearGradient id="eloGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="{{ rcolor }}" stop-opacity="0.3"/>
            <stop offset="100%" stop-color="{{ rcolor }}" stop-opacity="0"/>
          </linearGradient>
        </defs>
        <polygon points="{{ spark_poly }}" fill="url(#eloGrad)"/>
        <polyline points="{{ spark_line }}" fill="none" stroke="{{ rcolor }}" stroke-width="2"/>
      </svg>
    </div>
  </div>
  {% endif %}
</div>

{% if team_stats %}
<div class="card">
  <h2>Équipes favorites</h2>
  <div style="display:flex;flex-wrap:wrap;gap:10px">
  {% for tidx, count, pct in team_stats %}
    {% set tn, te, tc = team_name(tidx) %}
    <div style="background:var(--card2);border-radius:8px;padding:10px 14px;display:flex;align-items:center;gap:8px">
      <span style="font-size:22px">{{ te }}</span>
      <div>
        <div style="font-weight:600;color:white;font-size:13px">{{ tn }}</div>
        <div style="color:var(--text2);font-size:12px">{{ count }} match{{ 's' if count>1 }} · {{ pct }}%</div>
      </div>
    </div>
  {% endfor %}
  </div>
</div>
{% endif %}

<div class="card">
  <h2>Derniers matchs</h2>
  {% if not matches %}
  <div class="empty">Aucun match.</div>
  {% endif %}
  {% for m in matches %}
    {% set is_host = m['host_id'] == p['id'] %}
    {% set my_score = m['score_host'] if is_host else m['score_guest'] %}
    {% set op_score = m['score_guest'] if is_host else m['score_host'] %}
    {% set my_delta = m['delta_host'] if is_host else m['delta_guest'] %}
    {% set opp_id = m['guest_id'] if is_host else m['host_id'] %}
    {% set opp_name = m['guest_name'] if is_host else m['host_name'] %}
    {% set my_team = m['team_host'] if is_host else m['team_guest'] %}
    {% set op_team = m['team_guest'] if is_host else m['team_host'] %}
    {% set result = 'W' if my_score > op_score else ('L' if my_score < op_score else 'D') %}
    {% set tn_my, te_my, tc_my = team_name(my_team) %}
    {% set tn_op, te_op, tc_op = team_name(op_team) %}
    <div class="match-row">
      <div class="match-result-tag result-{{ result }}">{{ result }}</div>
      <div>
        <div style="display:flex;align-items:center;gap:6px">
          <span class="team-tag" title="{{ tn_my }}" style="color:{{ tc_my }}">{{ te_my }}</span>
          <span class="match-score">{{ my_score }}–{{ op_score }}</span>
          <span class="team-tag" title="{{ tn_op }}" style="color:{{ tc_op }}">{{ te_op }}</span>
        </div>
        <div style="display:flex;align-items:center;gap:8px;margin-top:3px">
          <span class="match-date">{{ fmt_ago(m['played']) }}</span>
          <span style="color:var(--text2);font-size:12px">vs
            <a href="{{ url_for('player', player_id=opp_id) }}" style="color:var(--text2)">{{ opp_name }}</a>
          </span>
          {% if m['forfeit'] %}<span class="forfeit-tag">FF</span>{% endif %}
        </div>
      </div>
      <div class="match-elo-delta">
        <span class="{{ 'delta-pos' if my_delta>0 else ('delta-neg' if my_delta<0 else 'delta-zero') }}">
          {{ '+' if my_delta>0 else '' }}{{ my_delta }}
        </span>
      </div>
    </div>
  {% endfor %}
</div>
{% endblock %}""")


ANIMALS_FOR_PROFILE = ["🐻", "🐂", "🐓", "🦕", "🐸", "🦒", "🦍", "🦁", "🐆", "🦈", "🐢", "🤖", "🐼", "🐯"]


def pick_avatar(player_id):
    return ANIMALS_FOR_PROFILE[player_id % len(ANIMALS_FOR_PROFILE)]


def render(template, **ctx):
    ctx.setdefault("css", BASE_CSS)
    ctx.setdefault("get_rank", get_rank)
    ctx.setdefault("team_name", team_name)
    ctx.setdefault("fmt_date", fmt_date)
    ctx.setdefault("fmt_ago", fmt_ago)
    ctx["enumerate"] = enumerate
    return render_template_string(template, **ctx)


@app.route("/")
def index():
    db = db_connect()
    players = get_leaderboard(db, 200)
    db.close()
    return render(INDEX_TEMPLATE, page="home", players=players)


@app.route("/matches")
def matches():
    page = max(1, request.args.get("p", 1, type=int))
    per_page = 30
    db = db_connect()
    total = count_matches(db)
    pages = max(1, (total + per_page - 1) // per_page)
    page = min(page, pages)
    rows = get_matches(db, per_page, (page - 1) * per_page)
    db.close()
    return render(MATCHES_TEMPLATE, page="matches",
                  matches=rows, total=total, cur_page=page, pages=pages)


@app.route("/player/<int:player_id>")
def player(player_id):
    db = db_connect()
    p = get_player(db, player_id)
    if p is None:
        db.close()
        abort(404)
    matches = get_player_matches(db, player_id, 20)
    rank_pos = get_player_rank(db, player_id, p["elo"])
    total_players = db.execute("SELECT COUNT(*) FROM players WHERE games > 0").fetchone()[0]
    db.close()

    history = player_elo_history(matches, player_id, p["elo"])
    spark_line, spark_poly = elo_sparkline_points(history)
    team_stats = player_team_stats(matches, player_id)
    avatar = pick_avatar(player_id)

    return render(PLAYER_TEMPLATE, page="player",
                  p=p, matches=matches, rank_pos=rank_pos, total_players=total_players,
                  elo_history=history, spark_line=spark_line, spark_poly=spark_poly,
                  team_stats=team_stats, animal=avatar)


@app.route("/player/<name>")
def player_by_name(name):
    db = db_connect()
    row = get_player_by_name(db, name)
    db.close()
    if row is None:
        abort(404)
    return redirect(url_for("player", player_id=row["id"]))


@app.errorhandler(404)
def not_found(e):
    return render(NAV_TEMPLATE.replace("{% block content %}{% endblock %}",
        "{% block content %}<div class='empty' style='margin-top:60px'>Joueur introuvable.</div>{% endblock %}"),
        page=""), 404


def main():
    global DB_PATH
    ap = argparse.ArgumentParser(description="Serveur web de stats Pet Soccer.")
    ap.add_argument("--db", default=DB_PATH, help="chemin vers players.db")
    ap.add_argument("--port", type=int, default=8080)
    ap.add_argument("--host", default="0.0.0.0")
    ap.add_argument("-v", "--verbose", action="store_true")
    args = ap.parse_args()
    DB_PATH = args.db
    print("Pet Soccer Stats sur http://localhost:%d" % args.port)
    app.run(host=args.host, port=args.port, debug=args.verbose)


if __name__ == "__main__":
    main()
