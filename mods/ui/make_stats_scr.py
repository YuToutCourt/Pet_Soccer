# Réorganise le cadre « résultats » de l'écran de pause / fin de match (data/menu/scr/InGame{640,,1024}.scr)
# en tableau comparatif : période, score, puis deux colonnes (équipe hôte à gauche, invitée à droite)
# avec le nom, les buteurs et les valeurs, et les libellés des statistiques au milieu.
# Le reste du fichier (menu, panneau Elo) n'est pas touché ; le script peut être relancé.
# Usage : python mods/ui/make_stats_scr.py
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
SCR_DIR = os.path.join(ROOT, "mods", "data", "Data", "Menu", "scr")

# polices par résolution : texte, gros score ; FLRomanSmall (buteurs) est ajoutée par le mod dans SoccerFonts
FONTS = {
    "640": ("FLRoman16", "FLRoman36"),
    "": ("FLRoman21", "FLRoman44"),
    "1024": ("FLRoman28", "FLRoman56"),
}
SMALL = "FLRomanSmall"
STAT_ROWS = 6  # ModStats.GetValues
SCORER_ROWS = 3  # ModStats._MAX_SCORER_LINES

# TextPosFlags (une ligne, centré verticalement). Les textes multilignes ignorent l'alignement : une static par ligne.
CENTER = 34

# positions en fraction du cadre, tailles en fraction d'écran (le cadre fait 0.42875 x 0.67666656)
COL1_X, COL2_X, COL_W = 0.03, 0.5, 0.2
VAL1_X, VAL2_X, VAL_W = 0.02, 0.805, 0.075
LABEL_X, LABEL_W = 0.2, 0.257


def static(id_, text, font, flags, x, y, w, h):
    return [
        'Static("UIStatic","%s","Specific")' % id_,
        "{",
        '   Text("%s")' % text,
        "   TextColor(1.0,1.0,1.0)",
        '   Font("%s")' % font,
        "   TextPosFlags(%d)" % flags,
        "   LogicSize(%s,%s)" % (w, h),
        "   LogicPos(%s,%s)" % (x, y),
        "   Mouse(0)",
        "   Keyboard(0)",
        "   Joy(0)",
        "   Visible(1)",
        "   ParentPositioning(1)",
        "   ParentCenteringX(0)",
        "   ParentCenteringY(0)",
        "   ParentSizingX(0)",
        "   ParentSizingY(0)",
        "}// [UIStatic] %s" % id_,
    ]


def build(res):
    roman, big = FONTS[res]
    path = os.path.join(SCR_DIR, "InGame%s.scr" % res)
    lines = open(path, "rb").read().decode("cp1252").split("\r\n")
    start = next(k for k, l in enumerate(lines) if 'Static("UIStatic","ID_FRAME"' in l)
    end = next(k for k in range(start, len(lines)) if lines[k].strip() == "}// [UIStatic] ID_FRAME")
    indent = lines[start][: len(lines[start]) - len(lines[start].lstrip())]
    # attributs du cadre lui-même (jusqu'à son premier enfant)
    first_child = next(k for k in range(start + 2, end) if lines[k].strip().startswith("Static("))
    head = [l for l in lines[start:first_child] if l.strip() and "[mod]" not in l]

    children = []
    children += static("ID_HALF", "", roman, CENTER, 0.05, 0.095, 0.386, 0.0517)
    children += static("ID_RESULT", "0 : 0", big, CENTER, 0.05, 0.165, 0.386, 0.0617)
    children += static("ID_TEAM_1", "", roman, CENTER, COL1_X, 0.27, COL_W, 0.04)
    children += static("ID_TEAM_2", "", roman, CENTER, COL2_X, 0.27, COL_W, 0.04)
    for r in range(SCORER_ROWS):
        y = round(0.335 + r * 0.044, 4)
        children += static("ID_SCORER%d_1" % r, "", SMALL, CENTER, COL1_X, y, COL_W, 0.03)
        children += static("ID_SCORER%d_2" % r, "", SMALL, CENTER, COL2_X, y, COL_W, 0.03)
    for r in range(STAT_ROWS):
        y = round(0.5 + r * 0.06, 4)
        children += static("ID_STAT%d_1" % r, "", roman, CENTER, VAL1_X, y, VAL_W, 0.04)
        children += static("ID_STAT%d_LABEL" % r, "", roman, CENTER, LABEL_X, y, LABEL_W, 0.04)
        children += static("ID_STAT%d_2" % r, "", roman, CENTER, VAL2_X, y, VAL_W, 0.04)

    block = head + ["", indent + "   // [mod] tableau comparatif (mods/ui/make_stats_scr.py)"]
    block += [indent + "   " + l for l in children]
    block += [lines[end]]
    lines[start:end + 1] = block
    open(path, "wb").write("\r\n".join(lines).encode("cp1252"))
    print(path)


for res in ("640", "", "1024"):
    build(res)
