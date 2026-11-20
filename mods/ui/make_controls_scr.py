# [mod] Écran Options > Contrôles : ajoute sous « Spécial » une ligne « Joueurs 2-7 » avec 6 cases de touche
# (ID_EDIT_KEY_SEL2..7), pour les touches directes de ModSwitch (voir notes/tactics.md).
# Part des OptionsControls*.scr du jeu d'origine et écrit les copies dans mods/data/Data/Menu/scr.
# Relancer après modification : python mods/ui/make_controls_scr.py
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
SRC_DIR = os.path.join(ROOT, "game_pristine", "Data", "Menu", "scr")
OUT_DIR = os.path.join(ROOT, "mods", "data", "Data", "Menu", "scr")

NUMBERS = range(2, 8)
# en fractions de l'écran, depuis le bord gauche de la ligne : le libellé occupe le début,
# les 6 cases vont jusqu'au bord droit des cases existantes (libellé 0.2225 + case 0.1225)
BOX_START = 0.185
BOX_WIDTH = 0.025
BOX_STEP = 0.0278
# hauteur de la 1re ligne (« Haut ») : 0.053333335 d'origine, un peu réduite pour loger la 9e ligne dans le cadre
# (les lignes suivantes et les cases sont en tailles relatives, elles suivent)
ROW_HEIGHT = 0.048


def find_line(lines, start, needle):
    for k in range(start, len(lines)):
        if needle in lines[k]:
            return k
    raise ValueError(needle)


def value(lines, start, name):
    k = find_line(lines, start, name + "(")
    return [float(x) for x in re.search(r"\(([^)]*)\)", lines[k]).group(1).split(",")]


def edit_template(lines, start):
    """attributs de ID_EDIT_KEY_UP, sans ses enfants (DOWN, LEFT...)"""
    k0 = find_line(lines, start, 'Button("UIEditSelKey","ID_EDIT_KEY_UP"')
    k1 = find_line(lines, k0 + 1, 'Button("UIEditSelKey"')
    body = lines[k0 + 1:k1]
    while body and body[-1].strip() == "":
        body.pop()
    indent = len(lines[k0]) - len(lines[k0].lstrip())
    return [l[indent:] for l in body]


def label_template(lines, start):
    """attributs de ID_LABEL_KEY_SUPER (dernière ligne de libellés)"""
    k0 = find_line(lines, start, 'Static("UIStatic","ID_LABEL_KEY_SUPER"')
    k1 = find_line(lines, k0 + 1, "}// [UIStatic] ID_LABEL_KEY_SUPER")
    indent = len(lines[k0]) - len(lines[k0].lstrip())
    return [l[indent:] for l in lines[k0 + 1:k1] if l.strip() != ""], k1, indent


def build(res):
    name = "OptionsControls%s.scr" % res
    lines = open(os.path.join(SRC_DIR, name), "rb").read().decode("cp1252").replace("\r\n", "\n").split("\n")

    for keys in ("ID_KEYS1", "ID_KEYS2"):
        start = find_line(lines, 0, 'Window("UIWindow","%s"' % keys)
        keys_w = value(lines, start, "LogicSize")[0]
        label_start = find_line(lines, start, 'Static("UIStatic","ID_LABEL_KEY_UP"')
        label_w = value(lines, label_start, "LogicSize")[0] * keys_w
        k_size = find_line(lines, label_start, "LogicSize(")
        lines[k_size] = re.sub(r"LogicSize\(([^,]*),[^)]*\)", lambda m: "LogicSize(%s,%s)" % (m.group(1), ROW_HEIGHT), lines[k_size])
        edit = edit_template(lines, start)
        label, k_end, indent = label_template(lines, start)

        pad = " " * (indent + 3)
        out = [pad + 'Static("UIStatic","ID_LABEL_KEY_SEL","Specific")']
        for l in label:
            if l.strip().startswith("Text("):
                l = l.replace(re.search(r'Text\("[^"]*"\)', l).group(0), 'Text("&&MenuControlsKeySelect&&")')
            out.append(pad + l)
        out.append("")

        for i, n in enumerate(NUMBERS):
            x = (BOX_START + i * BOX_STEP) / label_w
            out.append(pad + '   Button("UIEditSelKey","ID_EDIT_KEY_SEL%d","","Specific")' % n)
            for l in edit:
                s = l.strip()
                if s.startswith("Text("):
                    l = l.replace(s, 'Text("")')
                elif s.startswith("LogicSize("):
                    l = l.replace(s, "LogicSize(%.6f,1.0)" % BOX_WIDTH)
                elif s.startswith("LogicPos("):
                    l = l.replace(s, "LogicPos(%.6f,0.099999905)" % x)
                out.append(pad + "   " + l)
            out.append(pad + "   }// [UIEditSelKey] ID_EDIT_KEY_SEL%d" % n)
            out.append("")

        out.append(pad + "}// [UIStatic] ID_LABEL_KEY_SEL")
        lines[k_end:k_end] = out

    path = os.path.join(OUT_DIR, name)
    open(path, "wb").write("\r\n".join(lines).encode("cp1252"))
    print(path)


for res in ("640", "", "1024"):
    build(res)
