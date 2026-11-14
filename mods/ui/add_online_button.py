# Ajoute le bouton « en ligne » (ID_ONLINE) au menu principal (data/menu/scr/Main{640,,1024}.scr), copie du bouton
# « multijoueur » placée juste après, et répartit les 9 boutons du menu : pas de 0,1 (fraction du menu),
# hauteur 0,066 (fraction de l'écran) ; le dernier finit au-dessus des icônes options / quitter.
# Relançable : ajoute le bouton s'il manque, replace toujours les boutons.
# Usage : python mods/ui/add_online_button.py
import os
import re

SCR_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "Data", "Menu", "scr")
ORDER = ["ID_MATCH", "ID_CHAMPIONSHIPS", "ID_LEAGUE", "ID_SURVIVAL", "ID_EXTRA", "ID_MULTIPLAYER", "ID_ONLINE", "ID_PROFILE", "ID_MODS"]
STEP = 0.1
HEIGHT = 0.066


def button_block(lines, id_):
    i = next(k for k, l in enumerate(lines) if 'MenuButton("UIMenuButtonFlying","%s"' % id_ in l)
    j = next(k for k in range(i, len(lines)) if "}// [UIMenuButtonFlying] %s" % id_ in lines[k])
    return i, j


for res in ("640", "", "1024"):
    path = os.path.join(SCR_DIR, "Main%s.scr" % res)
    lines = open(path, "rb").read().decode("cp1252").split("\r\n")
    if not any('"ID_ONLINE"' in l for l in lines):
        i, j = button_block(lines, "ID_MULTIPLAYER")
        copy = [l.replace("ID_MULTIPLAYER", "ID_ONLINE").replace("&&MenuMainMultiplayer&&", "&&MenuMainOnline&&") for l in lines[i:j + 1]]
        lines = lines[:j + 1] + [" "] + copy + lines[j + 1:]

    for n, id_ in enumerate(ORDER):
        i, j = button_block(lines, id_)
        # premier LogicSize / LogicPos du bloc = ceux du bouton
        k = next(k for k in range(i, j) if lines[k].strip().startswith("LogicPos("))
        lines[k] = re.sub(r"LogicPos\([^)]*\)", "LogicPos(0.0,%s)" % round(n * STEP, 4), lines[k])
        k = next(k for k in range(i, j) if lines[k].strip().startswith("LogicSize("))
        lines[k] = re.sub(r"LogicSize\(([^,]*),[^)]*\)", r"LogicSize(\g<1>,%s)" % HEIGHT, lines[k])

    open(path, "wb").write("\r\n".join(lines).encode("cp1252"))
    print(path)
