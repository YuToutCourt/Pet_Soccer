"""Remplit une base Postgres de démonstration avec de faux joueurs et de faux matchs, pour voir le site (web/web.py).

    python server\\seed_demo.py                      # base pet_soccer_demo du Postgres local, vidée puis remplie
    python server\\seed_demo.py --matches 800 --players 40 --seed 7
    python web\\web.py --db postgresql://pet-soccer:toor@127.0.0.1:54320/pet_soccer_demo

Les matchs passent par store.Store.record_match, comme ceux du vrai serveur : Elo, tactiques, statistiques, fil du
match et heatmaps ont le format réel. Ils sont étalés sur les dernières semaines. Ne vise jamais la vraie base :
le nom de la base doit contenir « demo » ou « test », sauf avec --force (Postgres local seulement) :
    python server\\seed_demo.py --force --db postgresql://pet-soccer:toor@127.0.0.1:54320/pet-soccer
"""
import argparse
import math
import os
import random
import time

import psycopg

from store import Store, parse_tactic

DEFAULT_URL = "postgresql://pet-soccer:{password}@127.0.0.1:54320/pet_soccer_demo"
HEAT_D, HEAT_L = 12, 8

NAMES = ["Wapzi", "Yu", "KiwiFury", "Panda_Rouge", "LeTigre", "Zizou42", "Croquette", "MisterLion", "Gruik",
         "Bambou", "Shrek_FC", "Nounours", "Patatras", "TurboTortue", "Robotik", "Girafon", "CocoLapin", "Raptor",
         "Mamouth", "Frelon", "Pingouin", "Kangou", "LaChouette", "Moustache", "Kebab", "Biscotte", "Zebulon",
         "Grenouille", "Capybara", "Hibou", "Castor", "Pistache", "Nugget", "Bulldozer", "Marmotte", "Tornade",
         "Sardine", "Plume", "Cactus", "Popcorn"]

# data\roles.scr : schémas et rôles
SCHEMES = {
    "EQUILIBRE": ["DEFENSEUR", "DEFENSEUR", "MILIEU", "MILIEU", "ATTAQUANT", "ATTAQUANT"],
    "BETON": ["LIBERO", "STOPPEUR", "DEFENSEUR", "DEFENSEUR", "MILIEU", "PIVOT"],
    "CONTRE": ["LIBERO", "DEFENSEUR", "DEFENSEUR", "MILIEU", "AILIER", "AILIER"],
    "INDIVIDUEL": ["STOPPEUR", "STOPPEUR", "STOPPEUR", "MILIEU", "ATTAQUANT", "ATTAQUANT"],
    "ATTAQUE": ["DEFENSEUR", "MILIEU", "MILIEU", "AILIER", "AILIER", "PIVOT"],
}
ROLE_DEPTH = {"LIBERO": 0.14, "STOPPEUR": 0.22, "DEFENSEUR": 0.25, "LATERAL": 0.33, "MILIEU": 0.45, "AILIER": 0.6,
              "ATTAQUANT": 0.68, "PIVOT": 0.74}
ROLE_ATTACK = {"LIBERO": 0.1, "STOPPEUR": 0.2, "DEFENSEUR": 0.3, "LATERAL": 0.6, "MILIEU": 1.0, "AILIER": 1.6,
               "ATTAQUANT": 2.6, "PIVOT": 2.4}
DEFAULT_LAYOUT = [(0.2, 0.23), (0.18, 0.5), (0.2, 0.78), (0.44, 0.39), (0.41, 0.5), (0.44, 0.61)]  # ModTactics
STADIUMS = list(range(15))


def clamp(x, lo=0.0, hi=1.0):
    return max(lo, min(hi, x))


def poisson(rng, lam):
    lam = max(0.05, lam)
    l, k, p = math.exp(-lam), 0, 1.0
    while True:
        p *= rng.random()
        if p <= l:
            return k
        k += 1


def split(rng, total, weights):
    """Répartit un total entier selon des poids (la somme est exacte)."""
    out = [0] * len(weights)
    if total <= 0 or sum(weights) <= 0:
        return out
    for _ in range(total):
        r, acc = rng.random() * sum(weights), 0.0
        for i, w in enumerate(weights):
            acc += w
            if r <= acc:
                out[i] += 1
                break
        else:
            out[-1] += 1
    return out


class FakePlayer:
    def __init__(self, rng, name, idx):
        self.name = name
        self.token = "demo%028d" % idx
        self.skill = rng.gauss(0.0, 1.0)
        self.activity = rng.uniform(0.3, 1.0)
        self.animals = rng.sample(range(14), rng.choice([1, 2, 2, 3]))
        self.schemes = rng.sample(["ORIGINE"] + list(SCHEMES), rng.choice([1, 1, 2]))
        self.custom = rng.random() < 0.4  # place ses joueurs lui-même sur la carte tactique
        self.account = None


def make_tactic(rng, fp):
    """Ligne ModTactics.ToLine d'un joueur, et placement de jeu effectif (6 places)."""
    scheme = rng.choice(fp.schemes)
    if scheme == "ORIGINE":
        return "ORIGINE", list(DEFAULT_LAYOUT), None
    roles = SCHEMES[scheme]
    order = sorted(range(6), key=lambda k: ROLE_DEPTH[roles[k]])
    layout = []
    for k in range(6):
        same = [j for j in order if ROLE_DEPTH[roles[j]] == ROLE_DEPTH[roles[k]]]
        n, i = len(same), same.index(k)
        lat = 0.5 if n == 1 else 0.2 + 0.6 * i / (n - 1)
        layout.append((clamp(ROLE_DEPTH[roles[k]] + rng.gauss(0, 0.03), 0.05, 0.9), clamp(lat + rng.gauss(0, 0.04), 0.08, 0.92)))
    situations = []
    for s in range(4):
        if fp.custom and s in (0, 1):
            src = layout if s == 0 else [(min(d, 0.45), l) for d, l in layout]
            situations.append(",".join("%d/%d" % (round(d * 1000), round(l * 1000)) for d, l in src))
        else:
            situations.append("-")
    line = "%s;%s;%s" % (scheme, ",".join(roles), ";".join(situations))
    return line, layout, roles


def blob_heat(points, spread=1.4):
    """Heatmap 12 x 8 à partir de positions (profondeur, latéral, poids), ramenée à 0..255."""
    grid = [0.0] * (HEAT_D * HEAT_L)
    for d, l, w in points:
        cd, cl = d * HEAT_D - 0.5, l * HEAT_L - 0.5
        for i in range(HEAT_D):
            for j in range(HEAT_L):
                grid[i * HEAT_L + j] += w * math.exp(-((i - cd) ** 2 + (j - cl) ** 2) / (2 * spread ** 2))
    top = max(grid) or 1.0
    return [int(round(v / top * 255)) for v in grid]


def team_stats(rng, roles, layout, goals, shots, possession, saves, opp_shots_saved, supers, half):
    """Statistiques d'une équipe, cohérentes entre l'équipe et ses joueurs."""
    field_roles = roles or ["MILIEU"] * 6
    # place k de la tactique -> numéro du joueur (1..6) : ordre du jeu, mélangé
    numbers = list(range(1, 7))
    rng.shuffle(numbers)
    attack = [ROLE_ATTACK[r] * (0.6 + rng.random()) for r in field_roles]
    passes = max(goals * 3, int(rng.gauss(10 + possession / 4, 4)))
    passes_ok = int(passes * clamp(rng.gauss(0.55 + possession / 400, 0.08), 0.25, 0.95))
    recoveries = max(0, int(rng.gauss(7, 2.5)))
    p_goals = split(rng, goals, attack)
    p_shots = [g + x for g, x in zip(p_goals, split(rng, shots - goals, attack))]
    p_passes = split(rng, passes, [1.0 + 0.3 * rng.random() for _ in range(6)])
    p_ok = [min(pp, x) for pp, x in zip(p_passes, split(rng, passes_ok, p_passes))]
    passes_ok = sum(p_ok)
    p_rec = split(rng, recoveries, [2.0 - ROLE_ATTACK[r] / 2 for r in field_roles])
    scale = half / 120.0
    players = [dict(number=0, slot=-1, goals=0, shots=0, passes=rng.randint(0, 3), passes_ok=0, recoveries=rng.randint(0, 2),
                    distance=int(rng.uniform(25, 70) * scale), depth=clamp(rng.gauss(0.03, 0.01)), lateral=clamp(rng.gauss(0.5, 0.03)))]
    players[0]["passes_ok"] = players[0]["passes"]
    for k in range(6):
        d, l = layout[k]
        players.append(dict(number=numbers[k], slot=k if roles else -1, goals=p_goals[k], shots=p_shots[k], passes=p_passes[k],
                            passes_ok=p_ok[k], recoveries=p_rec[k], distance=int(rng.uniform(230, 420) * scale),
                            depth=clamp(d + rng.gauss(0.04, 0.05), 0.05, 0.95), lateral=clamp(l + rng.gauss(0, 0.06), 0.05, 0.95)))
    passes = sum(p["passes"] for p in players)
    passes_ok = sum(p["passes_ok"] for p in players)
    recoveries = sum(p["recoveries"] for p in players)
    by_number = {p["number"]: p for p in players}

    # tirs : les buts, puis les arrêts adverses, des contrés, le reste à côté
    shot_events = []
    results = ["goal"] * goals + ["saved"] * opp_shots_saved
    results += ["blocked" if rng.random() < 0.3 else "missed" for _ in range(shots - len(results))]
    shooters = [p["number"] for p in players for _ in range(p["shots"])]
    rng.shuffle(shooters)
    goal_minutes = sorted(rng.randint(1, 90) for _ in range(goals))
    goal_events = []
    gi = 0
    for i, res in enumerate(results[:30]):
        shooter = shooters[i] if i < len(shooters) else rng.randint(1, 6)
        if res == "goal":
            minute = goal_minutes[gi]
            gi += 1
            # le buteur est le tireur (aucun csc ici : ajoutés à part)
            goal_events.append(dict(minute=minute, number=shooter, own_goal=False))
        else:
            minute = rng.randint(1, 90)
        deep = by_number[shooter]["depth"]
        shot_events.append(dict(minute=minute, number=shooter, depth=clamp(rng.gauss(0.72 + deep / 6, 0.09), 0.45, 0.98),
                                lateral=clamp(rng.gauss(0.5, 0.17), 0.05, 0.95), result=res))
    # buts du joueur = buts comptés dans ses tirs (cohérence parfaite)
    for p in players:
        p["goals"] = sum(1 for g in goal_events if g["number"] == p["number"])
    super_events = [dict(minute=rng.randint(5, 90), type=rng.randint(0, 5)) for _ in range(supers)]
    super_types = 0
    for s in super_events:
        super_types |= 1 << s["type"]
    heat = blob_heat([(p["depth"], p["lateral"], 1.0) for p in players] +
                     [(clamp(p["depth"] + 0.12), p["lateral"], 0.5) for p in players[1:]])
    att = sum(p["depth"] for p in players[1:]) / 6
    ball = blob_heat([(clamp(att + 0.15), 0.5, 1.0), (clamp(att + 0.3), rng.uniform(0.2, 0.8), 0.7),
                      (clamp(att - 0.1), rng.uniform(0.2, 0.8), 0.6)], spread=1.8)
    return dict(shots=shots, possession=possession, passes_ok=passes_ok, passes=passes, recoveries=recoveries, saves=saves,
                supers=supers, super_types=super_types, max_deficit=0, goals=sorted(goal_events, key=lambda g: g["minute"]),
                super_events=sorted(super_events, key=lambda s: s["minute"]), shot_events=shot_events, players=players,
                heatmap=heat, ball_heatmap=ball)


def fake_match(rng, home, away, guest_name, kind, half):
    """Score, tactiques et statistiques d'un match entre deux faux joueurs (away None : invité sans compte)."""
    skill_a = home.skill
    skill_b = away.skill if away else rng.gauss(-0.3, 0.8)
    diff = skill_a - skill_b
    goals_a = poisson(rng, 1.5 * math.exp(0.35 * diff))
    goals_b = poisson(rng, 1.5 * math.exp(-0.35 * diff))
    poss_a = int(clamp(rng.gauss(50 + 6 * diff, 6), 25, 75))
    shots_a = goals_a + poisson(rng, 4 + diff)
    shots_b = goals_b + poisson(rng, 4 - diff)
    # arrêts : une partie des tirs non marqués de l'adversaire
    saves_a = min(shots_b - goals_b, poisson(rng, (shots_b - goals_b) * 0.45))
    saves_b = min(shots_a - goals_a, poisson(rng, (shots_a - goals_a) * 0.45))
    fps = (home, away)
    tactics, layouts, roles = [], [], []
    for fp in fps:
        if fp is None:
            t, lay, r = "ORIGINE", list(DEFAULT_LAYOUT), None
        else:
            t, lay, r = make_tactic(rng, fp)
        tactics.append(t)
        layouts.append(lay)
        roles.append(r)
    t_a = team_stats(rng, roles[0], layouts[0], goals_a, shots_a, poss_a, saves_a, saves_b, poisson(rng, 1.2), half)
    t_b = team_stats(rng, roles[1], layouts[1], goals_b, shots_b, 100 - poss_a, saves_b, saves_a, poisson(rng, 1.2), half)
    # un csc de temps en temps : un but de A devient un csc d'un joueur de B
    if t_a["goals"] and rng.random() < 0.06:
        g = t_a["goals"][-1]
        g["own_goal"] = True
        g["number"] = rng.randint(1, 6)
        for s in t_a["shot_events"]:
            if s["result"] == "goal" and s["minute"] == g["minute"]:
                s["result"] = "missed"
                break
        for p in t_a["players"]:
            p["goals"] = sum(1 for x in t_a["goals"] if x["number"] == p["number"] and not x["own_goal"])
    # plus gros retard au score, minute par minute
    timeline = sorted([(g["minute"], 0) for g in t_a["goals"]] + [(g["minute"], 1) for g in t_b["goals"]])
    a = b = 0
    for _, side in timeline:
        a, b = (a + 1, b) if side == 0 else (a, b + 1)
        t_a["max_deficit"] = max(t_a["max_deficit"], b - a)
        t_b["max_deficit"] = max(t_b["max_deficit"], a - b)
    stats = dict(match_id=rng.randint(1, 2 ** 31 - 1), golden_goal=kind == "league" and goals_a == goals_b and rng.random() < 0.3,
                 heat_size=(HEAT_D, HEAT_L), teams=[t_a, t_b])
    animals = (rng.choice(home.animals), rng.choice(away.animals) if away else rng.randrange(14))
    clothes = (0, 1 if animals[0] == animals[1] else 0)
    sides = [dict(player=home.account, name=home.name, animal=animals[0], clothes=clothes[0], tactic=tactics[0]),
             dict(player=away.account if away else None, name=away.name if away else guest_name, animal=animals[1],
                  clothes=clothes[1], tactic=tactics[1])]
    return sides, (goals_a, goals_b), stats


def demo_url(url=None, force=False):
    url = url or DEFAULT_URL.format(password=os.environ.get("POSTGRES_PASSWORD", "toor"))
    info = psycopg.conninfo.conninfo_to_dict(url)
    name = info.get("dbname") or ""
    if force and info.get("host") not in ("127.0.0.1", "localhost", "::1"):
        raise SystemExit("refusé : --force ne sert que pour le Postgres local (%s)" % info.get("host"))
    if not force and "demo" not in name and "test" not in name:
        raise SystemExit("refusé : le nom de la base doit contenir « demo » ou « test » (%s)" % name)
    admin = dict(info, dbname="postgres")
    with psycopg.connect(psycopg.conninfo.make_conninfo(**admin), autocommit=True) as conn:
        if conn.execute("SELECT 1 FROM pg_database WHERE datname = %s", (name,)).fetchone() is None:
            conn.execute('CREATE DATABASE "%s"' % name.replace('"', ""))
    with psycopg.connect(url, autocommit=True) as conn:
        conn.execute("DROP SCHEMA public CASCADE")
        conn.execute("CREATE SCHEMA public")
    return url


def main():
    ap = argparse.ArgumentParser(description="Fausses données pour le site de stats Pet Soccer.")
    ap.add_argument("--db", default=None, help="base à remplir (nom contenant demo ou test) ; par défaut pet_soccer_demo en local")
    ap.add_argument("--matches", type=int, default=500)
    ap.add_argument("--players", type=int, default=28)
    ap.add_argument("--days", type=int, default=45, help="les matchs sont étalés sur ce nombre de jours")
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--force", action="store_true", help="accepte une base locale d'un autre nom (elle est vidée)")
    args = ap.parse_args()
    rng = random.Random(args.seed)
    url = demo_url(args.db, args.force)
    store = Store(url)

    fakes = [FakePlayer(rng, NAMES[i % len(NAMES)] + ("" if i < len(NAMES) else str(i)), i) for i in range(args.players)]
    for fp in fakes:
        fp.account = store.login(fp.token, fp.name, 8)

    now = time.time()
    times = sorted(now - rng.uniform(0, args.days * 86400) for _ in range(args.matches))
    league_ids = [1, 2, 3]
    when = []  # (n° du match dans la base, début, fin, joueurs)
    for t in times:
        r = rng.random()
        kind = "ranked" if r < 0.68 else ("private" if r < 0.88 else "league")
        active = [f for f in fakes if rng.random() < f.activity] or fakes
        home = rng.choice(active)
        if kind == "private" and rng.random() < 0.3:
            away = None  # invité sans compte
        else:
            # partie classée : un adversaire d'Elo proche, comme la file d'attente
            others = sorted((f for f in fakes if f is not home), key=lambda f: abs(f.account.elo - home.account.elo) + rng.uniform(0, 150))
            away = others[0]
        if rng.random() < 0.5 and away is not None:
            home, away = away, home  # l'hôte est tiré au sort
        half = rng.choice([120, 180, 240, 240, 300]) if kind != "ranked" else 240
        sides, score, stats = fake_match(rng, home, away, rng.choice(["Invité", "Copain", "Cousin"]), kind, half)
        goals, forfeit_side = score, None
        if rng.random() < 0.04:
            forfeit_side = rng.randint(0, 1)
            margin = max(1, (score[1] - score[0]) if forfeit_side == 0 else (score[0] - score[1]))
            goals = (0, margin) if forfeit_side == 0 else (margin, 0)
        has_stats = forfeit_side is None or forfeit_side == 1  # l'hôte qui quitte envoie encore ses stats ; parfois non
        duration = half * 2 + rng.randint(20, 90)
        match_id, _ = store.record_match(
            sides, score, goals, kind=kind, rated=kind != "private", forfeit_side=forfeit_side,
            room_code="".join(rng.choice("ABCDEFGHJKLMNPQRSTUVWXYZ") for _ in range(4)),
            league=(rng.choice(league_ids), rng.randint(0, 90)) if kind == "league" else None,
            started=t - duration, stadium=rng.choice(STADIUMS), half_seconds=half, mod_version=8,
            stats=stats if has_stats and rng.random() < 0.97 else None)
        when.append((match_id, t - duration, t, [s["player"].id for s in sides if s["player"] is not None]))

    # heures réelles des matchs et des comptes
    with psycopg.connect(url, autocommit=True) as conn:
        with conn.transaction():
            for match_id, start, end, _ in when:
                conn.execute("UPDATE matches SET started_at = to_timestamp(%s), played_at = to_timestamp(%s) WHERE id = %s",
                             (start, end, match_id))
            first, last = {}, {}
            for _, start, end, ids in when:
                for pid in ids:
                    first.setdefault(pid, start)
                    last[pid] = end
            for pid in first:
                conn.execute("UPDATE players SET created = to_timestamp(%s), last_seen = to_timestamp(%s) WHERE id = %s",
                             (first[pid] - rng.uniform(600, 86400), last[pid], pid))
                conn.execute("UPDATE player_names SET first_seen = to_timestamp(%s), last_seen = to_timestamp(%s) WHERE player_id = %s",
                             (first[pid], last[pid], pid))
    store.write_status(online=rng.randint(4, 12), queued=rng.randint(0, 2), rooms=rng.randint(1, 4), live=rng.randint(1, 3))
    n_stats = store.db.one("SELECT COUNT(*) FROM matches WHERE has_stats")[0]
    print("%s : %d joueurs, %d matchs (%d avec statistiques)" % (url.split("@")[-1], len(fakes), len(times), n_stats))
    store.close()


if __name__ == "__main__":
    main()
