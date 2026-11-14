# Ajoute le bouton « survie » (ID_SURVIVAL) au menu principal (data/menu/scr/Main{640,,1024}.scr),
# copie du bouton « championnat » placée juste après, et resserre les 7 boutons du menu
# (pas de 0,125 au lieu de 0,15 : le dernier reste à 0,75, au-dessus des icônes options / quitter).
# Relançable : ne fait rien si le bouton existe déjà.
# Usage : python mods/ui/add_survival_button.py
import os
import re

SCR_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "Data", "Menu", "scr")
ORDER = ["ID_MATCH", "ID_CHAMPIONSHIPS", "ID_SURVIVAL", "ID_EXTRA", "ID_MULTIPLAYER", "ID_PROFILE", "ID_MODS"]
STEP = 0.125


def button_block(lines, id_):
    i = next(k for k, l in enumerate(lines) if 'MenuButton("UIMenuButtonFlying","%s"' % id_ in l)
    j = next(k for k in range(i, len(lines)) if "}// [UIMenuButtonFlying] %s" % id_ in lines[k])
    return i, j


for res in ("640", "", "1024"):
    path = os.path.join(SCR_DIR, "Main%s.scr" % res)
    lines = open(path, "rb").read().decode("cp1252").split("\r\n")
    if any('"ID_SURVIVAL"' in l for l in lines):
        print("deja present :", path)
        continue

    i, j = button_block(lines, "ID_CHAMPIONSHIPS")
    copy = [l.replace("ID_CHAMPIONSHIPS", "ID_SURVIVAL").replace("&&MenuMainChamp&&", "&&MenuMainSurvival&&") for l in lines[i:j + 1]]
    lines = lines[:j + 1] + [" "] + copy + lines[j + 1:]

    for n, id_ in enumerate(ORDER):
        i, j = button_block(lines, id_)
        k = next(k for k in range(i, j) if lines[k].strip().startswith("LogicPos("))
        lines[k] = re.sub(r"LogicPos\([^)]*\)", "LogicPos(0.0,%s)" % round(n * STEP, 4), lines[k])

    open(path, "wb").write("\r\n".join(lines).encode("cp1252"))
    print(path)
