"""Test de la logique de la ligue en ligne (league.py), sans réseau.

    python server\\test_league.py

1. Une ligue à un humain doit donner, entier pour entier, la ligue solo du jeu (mods/java/ModLeague.java compilé
   avec tools/jdk8, piloté par server/java/LeagueCheck.java). Sauté si le JDK n'est pas là.
2. Des milliers de ligues au hasard, de 0 à 14 humains, journées fermées par délai ou non : chaque équipe
   rencontre les autres une fois, les playoffs vont au bout, la sauvegarde se relit.
Affiche « OK » pour chaque point vérifié et sort avec le code 1 au premier échec.
"""
import json
import os
import random
import shutil
import subprocess
import sys
import tempfile

import league as L

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
JDK = os.path.join(ROOT, "tools", "jdk8", "bin")
RATINGS = [91.5, 88.0, 93.25, 86.0, 90.0, 89.5, 92.0, 87.25, 90.75, 88.5, 94.0, 85.5, 91.0, 89.0]


def fail(text):
    print("ÉCHEC : " + text)
    sys.exit(1)


def ok(text):
    print("OK  " + text)


def check(cond, text):
    if not cond:
        fail(text)


# ---------------------------------------------------------------- 1. même ligue que le jeu

def java_runs(cases):
    """Lance LeagueCheck pour chaque cas ; renvoie la liste des sauvegardes (listes d'entiers) par cas, ou None."""
    javac = os.path.join(JDK, "javac.exe" if os.name == "nt" else "javac")
    java = os.path.join(JDK, "java.exe" if os.name == "nt" else "java")
    if not os.path.exists(javac):
        return None
    out_dir = tempfile.mkdtemp(prefix="league_check_")
    try:
        subprocess.run([javac, "-nowarn", "-encoding", "UTF-8", "-d", out_dir,
                        os.path.join(ROOT, "mods", "java", "ModLeague.java"), os.path.join(HERE, "java", "LeagueCheck.java")],
                       check=True, capture_output=True)
        results = []
        for seed, score_seed, q, reset, form, player, ratings in cases:
            args = [java, "-cp", out_dir, "LeagueCheck", str(seed), str(score_seed), str(q), str(int(reset)), repr(form), str(player)]
            args += [repr(r) for r in ratings]
            text = subprocess.run(args, check=True, capture_output=True, text=True).stdout
            results.append([[int(x) for x in line.split(",")] for line in text.split()])
        return results
    finally:
        shutil.rmtree(out_dir, ignore_errors=True)


def python_run(seed, score_seed, q, reset, form, player, ratings):
    lg = L.League(L.JavaRandom(seed))
    lg.qualifiers = q
    lg.reset = reset
    lg.goals_per_match = L.f32(L.f32(L.f32(0.2) * 2.0) * 4)
    lg.start(list(range(len(ratings))), ratings, form, [player])
    scores = L.JavaRandom(score_seed)
    saves = [lg.save(player)]
    for _ in range(200):
        key = lg.next_key(player)
        if key is None:
            break
        gh = scores.next_int(5)
        gg = scores.next_int(5)
        if lg.phase == L.PHASE_PLAYOFFS and gh == gg:
            gh += 1
        check(lg.report(key, gh, gg) is True, "résultat refusé")
        saves.append(lg.save(player))
    return saves


def test_same_as_game():
    rnd = random.Random(7)
    cases = []
    for i in range(40):
        n = rnd.choice([14, 14, 13, 6, 3, 4, 8])
        ratings = [rnd.choice(RATINGS) + rnd.choice([0.0, 0.5, -1.25]) for _ in range(n)]
        cases.append((rnd.randrange(1 << 40), rnd.randrange(1 << 30), rnd.choice([2, 4, 8]), rnd.random() < 0.5,
                      rnd.choice([0.0, 2.0, 3.5]), rnd.randrange(n), ratings))
    java = java_runs(cases)
    if java is None:
        print("--  JDK absent (tools\\jdk8) : comparaison avec ModLeague.java sautée")
        return
    steps = 0
    for case, expected in zip(cases, java):
        got = python_run(*case)
        check(len(got) == len(expected), "cas %r : %d étapes au lieu de %d" % (case[:6], len(got), len(expected)))
        for k, (a, b) in enumerate(zip(got, expected)):
            if a != b:
                diff = next(i for i in range(min(len(a), len(b))) if a[i] != b[i]) if len(a) == len(b) else -1
                fail("cas %r, étape %d : sauvegarde différente du jeu (entier %d)" % (case[:6], k, diff))
        steps += len(got)
    ok("ligue à un humain identique à ModLeague.java : %d ligues, %d sauvegardes comparées" % (len(cases), steps))


# ---------------------------------------------------------------- 2. ligues au hasard, plusieurs humains

def random_league(rnd):
    n = rnd.choice([14, 14, 14, 13, 9, 6, 4, 3, 2])
    lg = L.League(L.JavaRandom(rnd.randrange(1 << 40)))
    lg.qualifiers = rnd.choice([2, 4, 8, 16])
    lg.reset = rnd.random() < 0.5
    humans = rnd.sample(range(n), rnd.randrange(0, n + 1))
    lg.start(list(range(n)), [rnd.choice(RATINGS) for _ in range(n)], 2.0, humans)
    return lg, humans


def play_out(lg, rnd, close_rate):
    """Joue les matchs des humains au hasard ; ferme parfois la journée (délai). Renvoie le nombre de matchs joués."""
    played = 0
    for _ in range(2000):
        if lg.phase == L.PHASE_OVER:
            return played
        keys = lg.pending_keys()
        check(keys, "ligue bloquée sans match d'humain en attente (phase %d)" % lg.phase)
        if rnd.random() < close_rate:
            keep = set(rnd.sample(keys, rnd.randrange(len(keys))))  # matchs « en cours » épargnés
            done = lg.close_step(keep)
            check(set(done) == set(keys) - keep, "fermeture de journée : mauvais matchs simulés")
            continue
        key = rnd.choice(keys)
        gh, gg = rnd.randrange(5), rnd.randrange(5)
        res = lg.report(key, gh, gg)
        if key >= L.KEY_PLAYOFFS and gh == gg:
            check(res is None, "nul en playoffs accepté")
            continue
        check(res is True, "résultat refusé")
        check(lg.report(key, 1, 0) is False, "un match joué accepte un second résultat")
        played += 1
        if rnd.random() < 0.03 and lg.humans:
            lg.drop_human(rnd.choice(sorted(lg.humans)))  # un joueur abandonne
    fail("ligue sans fin")


def check_finished(lg):
    n = lg.n_teams
    pairs = set()
    for f in range(lg.n_fixtures):
        check(lg.f_played[f], "match de ligue pas joué")
        pair = frozenset((lg.f_host[f], lg.f_guest[f]))
        check(pair not in pairs and len(pair) == 2, "rencontre en double")
        pairs.add(pair)
    check(len(pairs) == n * (n - 1) // 2, "rencontres manquantes")
    check(lg.round == lg.rounds, "journées pas toutes jouées")
    check(len(lg.seed) == L.clamp_qualifiers(lg.qualifiers, n), "nombre de qualifiés")
    check(lg.seed == lg.ranking()[:len(lg.seed)], "têtes de série = haut du classement")
    check(0 <= lg.champion < n and lg.champion in lg.seed, "champion")
    for m in range(lg.n_matches):
        if lg.b_active[m]:
            check(lg.b_played[m] and lg.b_ga[m] != lg.b_gb[m], "match de playoffs sans vainqueur")
    # chaque équipe qualifiée perd au plus deux fois, toutes sauf le champion perdent au moins une fois
    losses = {t: 0 for t in lg.seed}
    for m in range(lg.n_matches):
        if lg.b_active[m]:
            losses[lg.loser(m)] += 1
    for t, k in losses.items():
        check(k <= 2 or len(lg.seed) == 2, "équipe éliminée trois fois")
        check(t == lg.champion or k >= 1 or len(lg.seed) == 2, "équipe jamais battue mais pas championne")


def test_random_leagues():
    rnd = random.Random(2026)
    total = 0
    for i in range(3000):
        lg, humans = random_league(rnd)
        # la sauvegarde complète se relit à l'identique, en cours de route
        played = play_out(lg, rnd, rnd.choice([0.0, 0.05, 0.3]))
        total += played
        check_finished(lg)
        again = L.League.from_dict(json.loads(json.dumps(lg.to_dict())))
        check(again.save(0) == lg.save(0), "état relu différent")
    ok("3000 ligues de 2 à 14 équipes, 0 à 14 humains, délais au hasard : %d matchs d'humains joués" % total)


def test_resume_mid_way():
    rnd = random.Random(5)
    for _ in range(200):
        lg, _ = random_league(rnd)
        for _ in range(rnd.randrange(30)):
            keys = lg.pending_keys()
            if not keys:
                break
            lg.report(rnd.choice(keys), rnd.randrange(4), rnd.randrange(4))
        copy = L.League.from_dict(json.loads(json.dumps(lg.to_dict())))
        a, b = random.Random(9), random.Random(9)
        play_out(lg, a, 0.1)
        play_out(copy, b, 0.1)
        check(lg.save(0) == copy.save(0), "ligue relue en cours de route : suite différente")
    ok("ligue relue en cours de route : même suite, hasard compris (200 ligues)")


def test_waits_for_humans():
    lg = L.League(L.JavaRandom(1))
    lg.start(list(range(14)), RATINGS, 2.0, [0, 5])
    keys = lg.pending_keys()
    check(lg.round == 0 and 1 <= len(keys) <= 2, "la première journée attend les deux humains")
    other = [f for f in range(lg.n_fixtures) if lg.f_round[f] == 0 and f not in keys]
    check(not any(lg.f_played[f] for f in other), "les matchs entre IA se jouent à la fermeture de la journée")
    first = keys[0]
    lg.report(first, 2, 1)
    if len(keys) == 2:
        check(lg.round == 0, "la journée attend encore l'autre humain")
        lg.report(keys[1], 0, 0)
    check(lg.round == 1 and all(lg.f_played[f] for f in other), "journée fermée quand les humains ont joué")
    check(lg.sides(first)[0] in (lg.f_host[first],), "sides")
    ok("une journée attend tous les humains, puis simule les matchs entre IA")


def main():
    test_waits_for_humans()
    test_same_as_game()
    test_random_leagues()
    test_resume_mid_way()
    print("Tout est bon.")


if __name__ == "__main__":
    main()
