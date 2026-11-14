# Ajoute le bouton « Tactique » à l'écran d'avant-match (PreMatch*.scr, copiés depuis game_pristine)
# et au menu pause (InGame*.scr, déjà modifiés dans mods). Sans effet si le bouton existe déjà.
# Usage : python mods/ui/add_tactics_buttons.py
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
PRISTINE = os.path.join(ROOT, "game_pristine", "Data", "Menu", "scr")
MODS = os.path.join(ROOT, "mods", "data", "Data", "Menu", "scr")


def read(path):
    return open(path, "rb").read().decode("cp1252").replace("\r\n", "\n").split("\n")


def write(path, lines):
    open(path, "wb").write("\r\n".join(lines).encode("cp1252"))
    print(path)


def find_block(lines, start, end):
    i = next(k for k, l in enumerate(lines) if start in l)
    j = next(k for k in range(i, len(lines)) if end in lines[k])
    return i, j


def clone_button(lines, src_id, new_id, text_key, pos):
    i, j = find_block(lines, '"%s"' % src_id, "}// [UIMenuButtonFlying] %s" % src_id)
    out = []
    for l in lines[i:j + 1]:
        l = l.replace(src_id, new_id)
        s = l.strip()
        pad = l[:len(l) - len(l.lstrip())]
        if s.startswith("MenuButton("):
            l = pad + 'MenuButton("UIMenuButtonFlying","%s","&&%s&&","Standard","Specific")' % (new_id, text_key)
        elif s.startswith("Text("):
            l = pad + 'Text("&&%s&&")' % text_key
        elif s.startswith("LogicPos("):
            l = pad + "LogicPos(%s,%s)" % pos
        out.append(l)
    return out


def set_pos(lines, button_id, pos):
    i, j = find_block(lines, '"%s"' % button_id, "}// [UIMenuButtonFlying] %s" % button_id)
    for k in range(i, j + 1):
        s = lines[k].strip()
        if s.startswith("LogicPos("):
            pad = lines[k][:len(lines[k]) - len(lines[k].lstrip())]
            lines[k] = pad + "LogicPos(%s,%s)" % pos
            return
    raise ValueError(button_id)


def set_menu_rect(lines, pos, size):
    i, j = find_block(lines, 'Menu("UIMenu","ID_MENU"', "MenuButton(")
    for k in range(i, j):
        s = lines[k].strip()
        pad = lines[k][:len(lines[k]) - len(lines[k].lstrip())]
        if s.startswith("LogicPos("):
            lines[k] = pad + "LogicPos(%s,%s)" % pos
        elif s.startswith("LogicSize("):
            lines[k] = pad + "LogicSize(%s,%s)" % size


for res in ("640", "", "1024"):
    # avant-match : Retour | Tactique | Jouer, menu élargi
    lines = read(os.path.join(PRISTINE, "PreMatch%s.scr" % res))
    set_menu_rect(lines, (0.1925, 0.90166664), (0.8, 0.083333336))
    set_pos(lines, "ID_BACK", (0.0, 0.0))
    set_pos(lines, "ID_PLAY", (0.69, 0.0))
    i, j = find_block(lines, '"ID_BACK"', "}// [UIMenuButtonFlying] ID_BACK")
    btn = clone_button(lines, "ID_BACK", "ID_TACTICS", "MenuPreMatchTactics", (0.345, 0.0))
    lines[j + 1:j + 1] = [" "] + ["      // [mod] ecran Tactique (MenuTactics)"] + btn
    write(os.path.join(MODS, "PreMatch%s.scr" % res), lines)

    # pause : Continuer | Tactique | Rejouer | Quitter
    path = os.path.join(MODS, "InGame%s.scr" % res)
    lines = read(path)
    if any('"ID_TACTICS"' in l for l in lines):
        print("deja present :", path)
        continue
    # menu remonté pour libérer le badge Elo (ID_ELO_BADGE, y = 0.59, ecart sous « ok ») sous les 4 boutons
    set_menu_rect(lines, (0.62250006, 0.14), (0.25624996, 0.42333332))
    set_pos(lines, "ID_RE_MATCH", (0.5, 0.38))
    set_pos(lines, "ID_QUIT", (0.5, 0.57))
    i, j = find_block(lines, '"ID_CONTINUE"', "}// [UIMenuButtonFlying] ID_CONTINUE")
    btn = clone_button(lines, "ID_CONTINUE", "ID_TACTICS", "InGameTactics", (0.5, 0.19))
    lines[j + 1:j + 1] = [" "] + ["      // [mod] ecran Tactique (MenuTactics)"] + btn
    write(path, lines)
