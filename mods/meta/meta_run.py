"""Matchs de méta : toutes les équipes l'une contre l'autre, IA contre IA, enregistrés dans la base du site.

    python mods\\meta\\meta_run.py --reset            # vide la base, puis joue tout (reprend là où il s'était arrêté sinon)
    python mods\\meta\\meta_run.py --workers 4 --per-pair 10 --levels 7,8 --speed 3

Chaque match est un match démo du jeu (ModMeta, notes/meta.md) : les deux équipes sont jouées par l'IA au niveau
demandé, avec des mi-temps de 4 min comme une partie classée (RankedHalf de online.scr) et sans le super The World.
Le jeu écrit meta_result.txt (statistiques au format PKT_MATCH_STATS) puis se ferme ; le résultat est enregistré
comme un match en ligne (kind = ranked, non compté pour l'Elo, salon META, sans compte joueur).

Avant : mods\\build.ps1 -Out build\\meta\\base (le jeu modé, compilé à part de game_dev).
Chaque jeu tourne dans sa copie build\\meta\\w<n> (fenêtré). Avancement dans build\\meta\\progress.json : relancer
le script reprend les matchs qui manquent. Ctrl+C : les jeux en cours sont fermés.

Tactiques (--tactics) : une équipe contre une autre, chaque schéma prédéfini de roles.scr contre chaque schéma
(MetaSchemes), --per-combo matchs par combinaison, côtés et niveaux alternés. Salon META-TACTIC, avancement et
diagnostic à part (tactics_progress.json, tactics_diag.jsonl). --reset vide toute la base comme pour la méta (le
site ne sépare pas les salons) : sauvegarder la série d'avant (pg_dump, build\\meta\\serie<n>.sql) avant.
    python mods\\meta\\meta_run.py --tactics 10-3,11-5 --per-combo 6
    python mods\\meta\\meta_run.py --tactics 10-3 --schemes-b ORIGINE      # la Tortue essaie tout contre le Dino d'origine
"""
import argparse
import itertools
import json
import os
import random
import shutil
import subprocess
import sys
import threading
import time

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.join(ROOT, "server"))

import protocol as P  # noqa: E402
from store import Store  # noqa: E402

META = os.path.join(ROOT, "build", "meta")
BASE = os.path.join(META, "base")
PROGRESS = os.path.join(META, "progress.json")
DEFAULT_DB = "postgresql://pet-soccer:{password}@127.0.0.1:54320/pet-soccer"
TEAMS = 14  # EAnimals 0..13
# fichiers propres à chaque copie (sauvegardes, réglages, journaux) : jamais recopiés depuis base
OWN = ["debugconf.scr", "VideoSettings.scr", "settings.dat", "stats.dat", "elo.dat", "achievements.dat", "tactics.dat",
       "mods.dat", "league.dat", "online.dat", "crash.log", "meta_result.txt", "meta_result.txt.tmp"]
CONF_DECL = ["!Debug(i)", "!ShowFPS(i)", "!AllTeams(i)", "!MenuOn(i)", "!MetaMatch(i,i,i,i,i)", "!MetaHalf(i)",
             "!MetaSpeed(f)", "!MetaSchemes(s,s)"]
SCHEMES = ["ORIGINE", "EQUILIBRE", "BETON", "CONTRE", "INDIVIDUEL", "ATTAQUE"]  # roles.scr

lock = threading.Lock()
stop = threading.Event()


def log(msg):
    with lock:
        print(time.strftime("%H:%M:%S"), msg, flush=True)


def mod_version():
    with open(os.path.join(ROOT, "mods", "java", "ModNet.java"), encoding="utf-8") as f:
        for line in f:
            if "_MOD_VERSION =" in line:
                return int(line.split("=")[1].split(";")[0])
    return None


def schedule(per_pair, levels, seed):
    """Toutes les paires d'équipes, per_pair matchs chacune : côtés alternés, niveaux alternés. Ordre mélangé (graine
    fixe : la même liste à chaque reprise)."""
    jobs = []
    for a, b in itertools.combinations(range(TEAMS), 2):
        for k in range(per_pair):
            left, right = (a, b) if k % 2 == 0 else (b, a)
            jobs.append(dict(id="%d-%d-%d" % (a, b, k), a=left, b=right, level=levels[(k // 2) % len(levels)]))
    random.Random(seed).shuffle(jobs)
    return jobs


def schedule_tactics(pairs, schemes_a, schemes_b, per_combo, levels, seed):
    """Pour chaque paire (a, b) : chaque schéma de a contre chaque schéma de b, per_combo matchs. Côtés alternés
    (l'équipe a garde son schéma quel que soit son côté) ; niveaux alternés, décalés d'une combinaison à l'autre pour
    en avoir autant à chaque niveau au total."""
    jobs = []
    combos = [(a, b, sa, sb) for a, b in pairs for sa in schemes_a for sb in schemes_b]
    for c, (a, b, sa, sb) in enumerate(combos):
        for k in range(per_combo):
            j = dict(id="%d-%d-%s-%s-%d" % (a, b, sa, sb, k), level=levels[(k // 2 + c) % len(levels)])
            if k % 2 == 0:
                j.update(a=a, b=b, sa=sa, sb=sb)
            else:
                j.update(a=b, b=a, sa=sb, sb=sa)
            jobs.append(j)
    random.Random(seed).shuffle(jobs)
    return jobs


def load_progress():
    try:
        with open(PROGRESS, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return {"done": {}}


def save_progress(progress):
    tmp = PROGRESS + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(progress, f)
    os.replace(tmp, PROGRESS)


def prepare_worker(n):
    """build\\meta\\w<n> : copie de base, en fenêtré, avec le profil de game_dev_c (pas de profil = écran de création)."""
    d = os.path.join(META, "w%d" % n)
    r = subprocess.run(["robocopy", BASE, d, "/MIR", "/XF"] + OWN + ["/NFL", "/NDL", "/NJH", "/NJS", "/NP"],
                       capture_output=True)
    if r.returncode >= 8:
        raise SystemExit("robocopy vers %s : code %d" % (d, r.returncode))
    profile = os.path.join(ROOT, "game_dev_c", "settings.dat")
    if not os.path.exists(os.path.join(d, "settings.dat")):
        shutil.copy(profile, d)
    with open(os.path.join(BASE, "Data", "VideoSettings.scr"), encoding="cp1252", newline="") as f:
        video = f.read()
    video = "\r\n".join("//" + l if l.startswith("Fullscreen()") else l for l in video.split("\r\n"))
    with open(os.path.join(d, "Data", "VideoSettings.scr"), "w", encoding="cp1252", newline="") as f:
        f.write(video)
    return d


def kill_game(d):
    """Ferme le jeu de ce dossier seulement (les autres jeux du PC ne sont pas touchés)."""
    ps = ("Get-CimInstance Win32_Process | Where-Object { $_.ExecutablePath -like '%s\\*' } | "
          "ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }" % d)
    subprocess.run(["powershell", "-NoProfile", "-Command", ps], capture_output=True)


def game_running(d):
    ps = ("@(Get-CimInstance Win32_Process | Where-Object { $_.ExecutablePath -like '%s\\*' }).Count" % d)
    r = subprocess.run(["powershell", "-NoProfile", "-Command", ps], capture_output=True, text=True)
    return r.stdout.strip() not in ("", "0")


def play(d, job, half, speed, timeout):
    """Joue un match dans le dossier d. Renvoie le contenu de meta_result.txt (liste de lignes) ou None."""
    conf = CONF_DECL + ["", "Debug(1)", "ShowFPS(1)", "AllTeams(1)", "MenuOn(1)",
                        "MetaMatch(%d, 0, %d, 0, %d)" % (job["a"], job["b"], job["level"]),
                        "MetaHalf(%d)" % half, "MetaSpeed(%.2f)" % speed]
    if "sa" in job:
        conf.append('MetaSchemes("%s", "%s")' % (job["sa"], job["sb"]))
    conf.append("")
    with open(os.path.join(d, "debugconf.scr"), "w", encoding="cp1252", newline="") as f:
        f.write("\r\n".join(conf))
    result = os.path.join(d, "meta_result.txt")
    for name in ("meta_result.txt", "crash.log"):
        try:
            os.remove(os.path.join(d, name))
        except OSError:
            pass
    subprocess.Popen([os.path.join(d, "PetSoccer.exe")], cwd=d)
    start = time.time()
    time.sleep(15)
    while time.time() - start < timeout and not stop.is_set():
        if os.path.exists(result):
            time.sleep(2)  # le jeu se ferme
            break
        if not game_running(d):
            time.sleep(2)
            break
        time.sleep(3)
    if not os.path.exists(result) or stop.is_set():
        kill_game(d)
        return None
    if game_running(d):
        time.sleep(5)
        kill_game(d)
    with open(result, encoding="latin-1") as f:
        lines = f.read().splitlines()
    return lines if lines and lines[0] == "ok" else None


def record(store, job, lines, half, version, room="META"):
    a, ca, b, cb, level, _half, stadium = (int(x) for x in lines[1].split())
    stats = P.read_match_stats(P.make_lobby(P.PKT_MATCH_STATS, 0, 0, bytes.fromhex(lines[5])))
    # score = buts du fil du match (csc compris, comptés pour l'équipe qui en profite) : la même source que le site.
    # La ligne 3 du résultat était inversée après la mi-temps avant le correctif de ModMeta du 2026-11-26.
    score = tuple(len(t["goals"]) for t in stats["teams"])
    real = int(lines[6]) if len(lines) > 6 and lines[6].strip() else None
    name = "IA niveau %d" % level
    sides = [dict(player=None, name=name, animal=a, clothes=ca, tactic=lines[3] or None),
             dict(player=None, name=name, animal=b, clothes=cb, tactic=lines[4] or None)]
    started = time.time() - real if real else None
    db_id, _ = store.record_match(sides, score, score, kind="ranked", rated=False, room_code=room, started=started,
                                  stadium=stadium, half_seconds=half, mod_version=version, stats=stats)
    return db_id, score


DIAG = os.path.join(META, "diag.jsonl")
DIAG_KEYS = ["hold", "loose", "gk_hold", "super", "spells", "spells_dead", "play", "longest", "game_counter"]


def save_diag(store, job, lines, db_id):
    """Ligne 8 de meta_result.txt (ModMeta.DiagPossession) : temps de balle détaillé des deux équipes (A puis B, donc
    side 0 puis 1 comme dans record), table match_possession du site, et une ligne JSON par match dans
    build\\meta\\diag.jsonl en copie (absente avec un jeu compilé avant le diagnostic)."""
    if len(lines) < 8 or "|" not in lines[7]:
        return
    try:
        teams = []
        for part in lines[7].split("|"):
            v = part.split()
            teams.append(dict(animal=int(v[0]), **{k: float(x) for k, x in zip(DIAG_KEYS, v[1:])}))
        row = dict(match=db_id, job=job["id"], level=job["level"], teams=teams)
    except (ValueError, IndexError) as e:
        log("diag illisible (%s) : %r" % (e, lines[7]))
        return
    with lock:
        with open(DIAG, "a", encoding="utf-8") as f:
            f.write(json.dumps(row) + "\n")
        try:
            store.record_possession(db_id, teams)
        except Exception as e:  # noqa: BLE001
            log("diag : ERREUR base pour le match %s : %s" % (db_id, e))


def import_diag(store):
    """--import-diag : recharge build\\meta\\diag.jsonl dans match_possession (les lignes déjà en base sont gardées)."""
    n = 0
    if not os.path.exists(DIAG):
        log("diag : %s n'existe pas encore" % DIAG)
        return
    with open(DIAG, encoding="utf-8") as f:
        for line in f:
            row = json.loads(line)
            store.record_possession(row["match"], row["teams"])
            n += 1
    log("diag : %d matchs relus depuis %s" % (n, DIAG))


def worker(n, queue, progress, store, args, version):
    d = prepare_worker(n)
    expected = (2 * args.half) / args.speed + 120
    while not stop.is_set():
        with lock:
            if not queue:
                return
            job = queue.pop(0)
        lines = None
        for attempt in range(3):
            lines = play(d, job, args.half, args.speed, timeout=expected * 2.5 + 60)
            if lines is not None or stop.is_set():
                break
            log("w%d : %s sans resultat (essai %d), on recommence" % (n, job["id"], attempt + 1))
        if stop.is_set():
            return
        if lines is None:
            log("w%d : %s ABANDONNE apres 3 essais" % (n, job["id"]))
            continue
        try:
            with lock:  # une seule connexion à la base pour tous les jeux
                db_id, score = record(store, job, lines, args.half, version, args.room)
        except Exception as e:  # noqa: BLE001
            log("w%d : %s ERREUR base : %s" % (n, job["id"], e))
            continue
        save_diag(store, job, lines, db_id)
        with lock:
            progress["done"][job["id"]] = db_id
            save_progress(progress)
            done = len(progress["done"])
        schemes = " %s/%s" % (job["sa"], job["sb"]) if "sa" in job else ""
        log("w%d : %2d-%-2d %d:%d%s (niveau %d, match %d)  [%d/%d]" % (n, job["a"], job["b"], score[0], score[1],
                                                                      schemes, job["level"], db_id, done, args.total))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--db", default=os.environ.get("META_DATABASE_URL"),
                    help="base Postgres du site (par défaut : pet-soccer du Postgres local, port 54320)")
    ap.add_argument("--reset", action="store_true", help="vide la base (tous les matchs et comptes) et l'avancement")
    ap.add_argument("--workers", type=int, default=4, help="jeux lancés en même temps")
    ap.add_argument("--per-pair", type=int, default=10, help="matchs par paire d'équipes (côtés alternés)")
    ap.add_argument("--levels", default="7,8", help="niveaux de l'IA, alternés d'un match à l'autre")
    ap.add_argument("--half", type=int, default=240, help="mi-temps en secondes de jeu (240 = partie classée)")
    ap.add_argument("--speed", type=float, default=3.0, help="accélération du temps du jeu")
    ap.add_argument("--limit", type=int, default=0, help="ne jouer que ce nombre de matchs (essai)")
    ap.add_argument("--seed", type=int, default=2026)
    ap.add_argument("--tactics", default="",
                    help="matchs de tactique : paires faible-forte, ex. 10-3,11-5 (numéros EAnimals)")
    ap.add_argument("--schemes-a", default=",".join(SCHEMES), help="schémas de la première équipe de chaque paire")
    ap.add_argument("--schemes-b", default=",".join(SCHEMES), help="schémas de la seconde équipe")
    ap.add_argument("--per-combo", type=int, default=6, help="matchs par combinaison de schémas (côtés alternés)")
    ap.add_argument("--import-diag", action="store_true",
                    help="recharge build\\meta\\diag.jsonl dans la table match_possession, puis s'arrête")
    args = ap.parse_args()

    global PROGRESS, DIAG
    args.room = "META"
    if args.tactics:
        args.room = "META-TACTIC"
        PROGRESS = os.path.join(META, "tactics_progress.json")
        DIAG = os.path.join(META, "tactics_diag.jsonl")
        pairs = [tuple(int(x) for x in p.split("-")) for p in args.tactics.split(",")]
        schemes_a = [x.strip().upper() for x in args.schemes_a.split(",")]
        schemes_b = [x.strip().upper() for x in args.schemes_b.split(",")]
        for x in schemes_a + schemes_b:
            if x not in SCHEMES:
                raise SystemExit("schema inconnu : %s (connus : %s)" % (x, ", ".join(SCHEMES)))
        for a, b in pairs:
            if a == b or not (0 <= a < TEAMS and 0 <= b < TEAMS):
                raise SystemExit("paire invalide : %d-%d (deux equipes differentes, 0..13)" % (a, b))

    url = args.db or DEFAULT_URL()
    if not os.path.exists(os.path.join(BASE, "code.pak")):
        raise SystemExit("build\\meta\\base manque : lancer d'abord mods\\build.ps1 -Out build\\meta\\base")
    store = Store(url)  # crée les tables si besoin
    if args.import_diag:
        import_diag(store)
        return
    if args.reset:  # méta ou tactiques : PROGRESS et DIAG sont ceux du mode choisi
        store.db.execute("TRUNCATE match_events, match_footballers, match_players, matches, player_names, players,"
                         " leagues, server_status RESTART IDENTITY CASCADE")
        for name in (PROGRESS, DIAG):
            try:
                os.remove(name)
            except OSError:
                pass
        log("base videe : %s" % url.split("@")[-1])

    levels = [int(x) for x in args.levels.split(",")]
    if args.tactics:
        jobs = schedule_tactics(pairs, schemes_a, schemes_b, args.per_combo, levels, args.seed)
    else:
        jobs = schedule(args.per_pair, levels, args.seed)
    progress = load_progress()
    queue = [j for j in jobs if j["id"] not in progress["done"]]
    if args.limit:
        queue = queue[:args.limit]
    args.total = len(progress["done"]) + len(queue)
    log("%d matchs au programme, %d deja joues, %d a jouer sur %d jeux (mi-temps %d s, vitesse x%g)"
        % (len(jobs), len(progress["done"]), len(queue), args.workers, args.half, args.speed))
    version = mod_version()
    threads = []
    for n in range(1, args.workers + 1):
        t = threading.Thread(target=worker, args=(n, queue, progress, store, args, version), daemon=True)
        t.start()
        threads.append(t)
        time.sleep(4)  # les jeux ne chargent pas tous en même temps
    try:
        while any(t.is_alive() for t in threads):
            time.sleep(1)
    except KeyboardInterrupt:
        log("arret demande : fermeture des jeux")
        stop.set()
        for n in range(1, args.workers + 1):
            kill_game(os.path.join(META, "w%d" % n))
    log("fini : %d matchs enregistres" % len(progress["done"]))


def DEFAULT_URL():
    return DEFAULT_DB.format(password=os.environ.get("POSTGRES_PASSWORD", "toor"))


if __name__ == "__main__":
    main()
