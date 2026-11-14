# Génère l'écran Profil (data/menu/scr/Profile{640,,1024}.scr) à partir d'Options > Match de chaque
# résolution (polices, cadres et boutons d'origine) : cadre « bilan » à gauche (libellés + valeurs),
# cadre « derniers matchs » à droite (ROWS lignes : date, mode, score, Elo), boutons « succès » et « retour » en bas.
# Usage : python mods/ui/make_profile_scr.py  ->  mods/data/Data/Menu/scr/Profile*.scr
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
SRC_DIR = os.path.join(ROOT, "game_pristine", "Data", "Menu", "scr")
OUT_DIR = os.path.join(ROOT, "mods", "data", "Data", "Menu", "scr")
ROWS = 12
SUMMARY_ROWS = 14
# petite police des listes, ajoutée par le mod dans SoccerFonts (14/16/21 px selon la résolution)
SMALL = "FLRomanSmall"
# TextPosFlags (une ligne, centré verticalement) : 16 gauche, 32 centre, 64 droite.
# Les textes multilignes (256) ignorent l'alignement horizontal : une static par ligne.
LEFT, CENTER, RIGHT = 18, 34, 66


def block(lines, start_marker, end_marker):
    """Lignes de start_marker à end_marker inclus (première occurrence)."""
    i = next(k for k, l in enumerate(lines) if start_marker in l)
    j = next(k for k in range(i, len(lines)) if end_marker in lines[k])
    return lines[i:j + 1]


def first(lines, prefix):
    return next(l.strip() for l in lines if l.strip().startswith(prefix))


def indent_block(lines, n):
    """Réindente un bloc d'éléments en suivant les accolades."""
    out, depth = [], n
    for l in lines:
        s = l.strip()
        if s.startswith("}"):
            depth -= 1
        out.append("   " * depth + s if s else "")
        if s == "{":
            depth += 1
    return out


def common(x, y, w, h, mouse=0):
    return [
        "LogicSize(%s,%s)" % (w, h),
        "LogicPos(%s,%s)" % (x, y),
        "Mouse(%d)" % mouse,
        "Keyboard(0)",
        "Joy(0)",
        "Visible(1)",
        "ParentPositioning(1)",
        "ParentCenteringX(0)",
        "ParentCenteringY(0)",
        "ParentSizingX(0)",
        "ParentSizingY(0)",
    ]


def static(id_, text, font, flags, x, y, w, h, extra=()):
    body = ['Text("%s")' % text, "TextColor(1.0,1.0,1.0)", 'Font("%s")' % font, "TextPosFlags(%d)" % flags]
    body += common(x, y, w, h) + list(extra)
    return ['Static("UIStatic","%s","Specific")' % id_, "{"] + body + ['}// [UIStatic] %s' % id_]


def frame(id_, text, jazz, deco, x, y, w, h, children):
    body = ['Text("%s")' % text, "TextColor(1.0,1.0,1.0)", 'Font("%s")' % jazz, "TextPosFlags(33)"]
    body += common(x, y, w, h, mouse=1) + deco
    out = ['Static("UIStatic","%s","Specific")' % id_, "{"] + body + [""]
    for c in children:
        out += c + [""]
    return out + ['}// [UIStatic] %s' % id_]


def build(res):
    name = "OptionsMatch%s.scr" % res
    src = open(os.path.join(SRC_DIR, name), "rb").read().decode("cp1252").replace("\r\n", "\n").split("\n")
    small = SMALL

    header = src[:next(k for k, l in enumerate(src) if l.startswith("Interface("))]
    iface = block(src, 'Interface("UI_OPTIONS_MATCH")', 'Menu("UIMenu","ID_MENU"')[:-1]
    iface = [l.replace("UI_OPTIONS_MATCH", "UI_PROFILE") for l in iface]
    menu = block(src, 'Menu("UIMenu","ID_MENU"', "}// [UIMenu] ID_MENU")
    # OK / Annuler deviennent « succès » (ID_ACHIEVEMENTS, écran MenuAchievements) / « retour » (ID_CANCEL)
    menu = [l.replace("ID_OK", "ID_ACHIEVEMENTS").replace("&&MenuOK&&", "&&MenuAchievements&&") for l in menu]
    menu = [l.replace("&&MenuCancel&&", "&&MenuBack&&") for l in menu]
    footer = src[next(k for k, l in enumerate(src) if l.strip().startswith("EditTexture(")):]
    frame_src = block(src, 'Static("UIStatic","ID_FRAME"', 'ControlObject("UIComboBox"')
    deco = [first(frame_src, "FrameCornerSize("), first(frame_src, "TextureRect(")]
    jazz = first(frame_src, "Font(")[6:-2]

    # cadre gauche : bilan (libellés à gauche, valeurs à droite)
    left = []
    for r in range(SUMMARY_ROWS):
        y = round(0.11 + r * 0.058, 4)
        left += [
            static("ID_S%d_LABEL" % r, "", small, LEFT, 0.07, y, 0.2, 0.03),
            static("ID_S%d_VALUE" % r, "", small, RIGHT, 0.55, y, 0.12, 0.03),
        ]

    # cadre droit : derniers matchs, du plus récent au plus ancien
    right = [static("ID_EMPTY", "", small, CENTER, 0.05, 0.4, 0.48, 0.05)]
    for r in range(ROWS):
        y = round(0.12 + r * 0.062, 4)
        right += [
            static("ID_H%d_DATE" % r, "", small, LEFT, 0.04, y, 0.06, 0.04),
            static("ID_H%d_MODE" % r, "", small, LEFT, 0.165, y, 0.075, 0.04),
            static("ID_H%d_MATCH" % r, "", small, CENTER, 0.29, y, 0.3, 0.04),
            static("ID_H%d_ELO" % r, "", small, CENTER, 0.86, y, 0.05, 0.04),
        ]

    out = header + iface
    out += indent_block(menu, 1) + [""]
    out += indent_block(static("ID_TITLE", "&&ProfileTitle&&", jazz, CENTER, 0.0375, 0.1, 0.925, 0.08), 1) + [""]
    out += indent_block(frame("ID_FRAME_SUMMARY", "&&ProfileFrameSummary&&", jazz, deco, 0.0375, 0.20833331, 0.38, 0.68833333, left), 1) + [""]
    out += indent_block(frame("ID_FRAME_HISTORY", "&&ProfileFrameHistory&&", jazz, deco, 0.43, 0.20833331, 0.5325, 0.68833333, right), 1)
    out += footer

    path = os.path.join(OUT_DIR, "Profile%s.scr" % res)
    open(path, "wb").write("\r\n".join(out).encode("cp1252"))
    print(path)


for res in ("640", "", "1024"):
    build(res)
