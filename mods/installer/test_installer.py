"""Essai de l'installeur sur une fausse copie du jeu : installe, met à jour, désinstalle, et compare.

    powershell -ExecutionPolicy Bypass -File mods/package.ps1
    python mods/installer/test_installer.py
"""
import filecmp
import hashlib
import json
import os
import pathlib
import re
import shutil
import subprocess
import sys
import zipfile

root = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
pristine = os.path.join(root, "game_pristine")
dev = os.path.join(root, "game_dev")
data = os.path.join(root, "mods", "data")
pkg_zip = os.path.join(root, "build", "PetSoccerMod.zip")
work = os.path.join(root, "build", "installer-test")
game = os.path.join(work, "Pet Soccer")  # un espace dans le chemin, comme une vraie installation
checks = 0


def check(what, ok):
    global checks
    checks += 1
    print(("OK    " if ok else "ECHEC ") + what)
    if not ok:
        raise SystemExit(1)


def sha(path):
    return hashlib.sha256(open(path, "rb").read()).hexdigest()


def rel_files(top):
    out = []
    for d, _, files in os.walk(top):
        out += [os.path.relpath(os.path.join(d, f), top) for f in files]
    return sorted(out)


def run(*args):
    cmd = ["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", os.path.join(work, "PetSoccerMod", "install.ps1"),
           "-Game", game, "-Quiet"] + list(args)
    res = subprocess.run(cmd, capture_output=True)
    return res.returncode, res.stdout.decode("cp850", "replace") + res.stderr.decode("cp850", "replace")


shutil.rmtree(work, ignore_errors=True)
os.makedirs(game)
with zipfile.ZipFile(pkg_zip) as z:
    z.extractall(work)  # comme l'ami qui décompresse le zip
mod_files = [f for f in rel_files(data) if f != "debugconf.scr"]
replaced = [f for f in mod_files if os.path.exists(os.path.join(pristine, f))]

# la fausse copie : les fichiers que l'installeur regarde ou remplace, plus une sauvegarde du joueur
for f in ["PetSoccer.exe", "code.pak", "code_inf.pak"] + replaced:
    os.makedirs(os.path.dirname(os.path.join(game, f)), exist_ok=True)
    shutil.copy2(os.path.join(pristine, f), os.path.join(game, f))
for save in ("settings.dat", "online.dat"):
    open(os.path.join(game, save), "w").write("sauvegarde du joueur")
before = {f: sha(os.path.join(game, f)) for f in rel_files(game)}

names = sorted(n for n in zipfile.ZipFile(pkg_zip).namelist())
check("le paquet ne contient ni sauvegarde, ni compte, ni debugconf",
      not [n for n in names if n.lower().endswith((".dat", ".log", ".db")) or "debugconf" in n.lower()])

code, out = run("-Server", "exemple.duckdns.org", "-Port", "7780")
print(out.strip())
check("installation réussie", code == 0)
with zipfile.ZipFile(os.path.join(game, "code.pak")) as mine, zipfile.ZipFile(os.path.join(dev, "code.pak")) as ref:
    a = {i.filename: i.CRC for i in mine.infolist()}
    b = {i.filename: i.CRC for i in ref.infolist()}
check("code.pak : les mêmes %d classes que game_dev" % len(a), a == b)
check("code_inf.pak identique à celui de game_dev", filecmp.cmp(os.path.join(game, "code_inf.pak"), os.path.join(dev, "code_inf.pak"), False))
same = [f for f in mod_files if f != os.path.join("Data", "online.scr")
        and filecmp.cmp(os.path.join(game, f), os.path.join(data, f), False)]
check("les %d fichiers de données du mod sont en place" % (len(mod_files) - 1), len(same) == len(mod_files) - 1)
online = open(os.path.join(game, "Data", "online.scr"), "rb").read()
check("adresse du serveur écrite dans online.scr, fins de ligne CRLF gardées",
      b'Server("exemple.duckdns.org", 7780)\r\n' in online and b"127.0.0.1" not in online and b"HalfBreak(30)\r\n" in online)
check("debugconf.scr n'est pas installé", not os.path.exists(os.path.join(game, "debugconf.scr")))
backup = os.path.join(game, "mod_backup")
saved = [f for f in ["code.pak", "code_inf.pak"] + replaced if sha(os.path.join(backup, f)) == before[f]]
check("les %d fichiers d'origine remplacés sont gardés dans mod_backup" % len(saved), len(saved) == len(replaced) + 2)
check("les sauvegardes du joueur ne sont pas touchées",
      all(sha(os.path.join(game, s)) == before[s] for s in ("settings.dat", "online.dat")))
release = re.search(r'RELEASE\s*=\s*"([0-9.]+)"', open(os.path.join(root, "mods", "java", "ModVersion.java"), encoding="utf-8").read()).group(1)
maj = os.path.join(game, "mod_maj")
check("lanceur en place : Jouer.bat, mod_maj\\update.ps1 et mod.txt de la version %s" % release,
      os.path.exists(os.path.join(game, "Jouer.bat")) and os.path.exists(os.path.join(maj, "update.ps1"))
      and ("release=%s" % release) in open(os.path.join(maj, "mod.txt")).read())

code, out = run()  # mise à jour : même paquet, sans redonner l'adresse
check("mise à jour réussie", code == 0)
online = open(os.path.join(game, "Data", "online.scr"), "rb").read()
check("mise à jour : l'adresse du serveur déjà réglée est gardée", b'Server("exemple.duckdns.org", 7780)' in online)
check("mise à jour : mod_backup garde les originaux, pas les fichiers du mod",
      all(sha(os.path.join(backup, f)) == before[f] for f in ["code.pak", "code_inf.pak"] + replaced)
      and sorted(rel_files(backup)) == sorted(["code.pak", "code_inf.pak", "fichiers_du_mod.txt"] + replaced))

# --- lanceur : mise à jour depuis une fausse release GitHub (fichiers locaux en file:///) ---
asset = os.path.join(work, "PetSoccerMod-update.zip")
shutil.copy2(pkg_zip, asset)
feed = os.path.join(work, "latest.json")


def fake_release(tag, digest):
    json.dump({"tag_name": tag, "assets": [{"name": "PetSoccerMod-update.zip", "size": os.path.getsize(asset), "digest": digest,
                                            "browser_download_url": pathlib.Path(asset).as_uri()}]}, open(feed, "w"))


def launch():
    cmd = ["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", os.path.join(maj, "update.ps1"),
           "-Api", pathlib.Path(feed).as_uri(), "-NoLaunch"]
    res = subprocess.run(cmd, capture_output=True)
    return res.returncode, res.stdout.decode("cp850", "replace") + res.stderr.decode("cp850", "replace")


def installed_release():
    return re.search(r"release=(\S+)", open(os.path.join(maj, "mod.txt")).read()).group(1)


fake_release(release, "sha256:" + sha(asset))
code, out = launch()
check("lanceur : même version que la release, rien à faire", code == 0 and installed_release() == release and "jour." in out)

open(os.path.join(maj, "mod.txt"), "w").write("release=0.1\n")  # une ancienne version installée
fake_release(release, "sha256:" + "0" * 64)
code, out = launch()
check("lanceur : téléchargement abîmé (empreinte fausse) refusé, le jeu garde sa version",
      code == 0 and installed_release() == "0.1" and "SHA-256" in out)

fake_release("v" + release, "sha256:" + sha(asset))
code, out = launch()
print(out.strip())
check("lanceur : nouvelle version téléchargée et installée", code == 0 and installed_release() == release)
online = open(os.path.join(game, "Data", "online.scr"), "rb").read()
check("lanceur : l'adresse du serveur et les sauvegardes sont gardées", b'Server("exemple.duckdns.org", 7780)' in online
      and all(sha(os.path.join(game, s)) == before[s] for s in ("settings.dat", "online.dat")))

code, out = run("-Uninstall")
print(out.strip())
check("désinstallation réussie", code == 0)
after = {f: sha(os.path.join(game, f)) for f in rel_files(game)}
check("le dossier du jeu est revenu exactement comme avant (%d fichiers)" % len(after), after == before)
code, out = run("-Uninstall")
check("désinstaller deux fois : refusé proprement", code == 1 and "pas install" in out)

shutil.rmtree(work, ignore_errors=True)
print("%d vérifications réussies" % checks)
