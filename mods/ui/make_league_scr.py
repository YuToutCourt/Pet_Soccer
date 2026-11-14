# Génère les deux écrans du mode Ligue (ligue + playoffs à double élimination), à partir d'Options > Match
# de chaque résolution (polices, cadres et boutons d'origine) :
# - League{640,,1024}.scr (MenuLeague) : classement de la ligue à gauche (ROWS lignes),
#   prochain match et résultats de la dernière journée à droite ;
# - LeagueBracket{640,,1024}.scr (MenuLeagueBracket) : tableau des gagnants en haut, des perdants en bas,
#   grande finale (et revanche) à droite, deux lignes de message en bas.
# Le voile « info » (ID_INFO_SCREEN, texte clignotant) est repris des écrans de championnat d'origine.
# Usage : python mods/ui/make_league_scr.py  ->  mods/data/Data/Menu/scr/League*.scr
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
SRC_DIR = os.path.join(ROOT, "game_pristine", "Data", "Menu", "scr")
OUT_DIR = os.path.join(ROOT, "mods", "data", "Data", "Menu", "scr")
ROWS = 14  # MenuLeague._ROWS (équipes de la ligue)
RESULTS = 7  # MenuLeague._RESULTS (matchs d'une journée)
WB_ROUNDS, LB_ROUNDS = 3, 4  # tableau prévu pour 8 qualifiés au plus (MenuLeagueBracket)
# TextPosFlags (une ligne, centré verticalement) : 16 gauche, 32 centre, 64 droite.
LEFT, CENTER, RIGHT = 18, 34, 66
LOGO = "data/menu/logos/T_Logo_Bullrusia.png"  # remplacé par le code
SMALL = "FLRomanSmall"  # petite police ajoutée par le mod (SoccerFonts)
# cadres (position et taille en fraction de l'écran, comme les autres écrans du mod)
FRAME_Y, FRAME_H = 0.20833331, 0.68833333


def block(lines, start_marker, end_marker):
    i = next(k for k, l in enumerate(lines) if start_marker in l)
    j = next(k for k in range(i, len(lines)) if end_marker in lines[k])
    return lines[i:j + 1]


def first(lines, prefix):
    return next(l.strip() for l in lines if l.strip().startswith(prefix))


def indent_block(lines, n):
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
        "LogicSize(%s,%s)" % (round(w, 5), round(h, 5)),
        "LogicPos(%s,%s)" % (round(x, 5), round(y, 5)),
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


def static(id_, text, font, flags, x, y, w, h):
    body = ['Text("%s")' % text, "TextColor(1.0,1.0,1.0)", 'Font("%s")' % font, "TextPosFlags(%d)" % flags]
    body += common(x, y, w, h)
    return ['Static("UIStatic","%s","Specific")' % id_, "{"] + body + ['}// [UIStatic] %s' % id_]


def window(id_, x, y, w, h):
    body = common(x, y, w, h, mouse=1) + ['Texture("%s")' % LOGO]
    return ['Window("UIWindow","%s","Specific")' % id_, "{"] + body + ['}// [UIWindow] %s' % id_]


def frame(id_, text, jazz, deco, x, y, w, h, children):
    body = ['Text("%s")' % text, "TextColor(1.0,1.0,1.0)", 'Font("%s")' % jazz, "TextPosFlags(33)"]
    body += common(x, y, w, h, mouse=1) + deco
    out = ['Static("UIStatic","%s","Specific")' % id_, "{"] + body + [""]
    for c in children:
        out += c + [""]
    return out + ['}// [UIStatic] %s' % id_]


def read(name):
    return open(os.path.join(SRC_DIR, name), "rb").read().decode("cp1252").replace("\r\n", "\n").split("\n")


def template(res, ui_name, ok_text, cancel_text):
    src = read("OptionsMatch%s.scr" % res)
    header = src[:next(k for k, l in enumerate(src) if l.startswith("Interface("))]
    iface = block(src, 'Interface("UI_OPTIONS_MATCH")', 'Menu("UIMenu","ID_MENU"')[:-1]
    iface = [l.replace("UI_OPTIONS_MATCH", ui_name) for l in iface]
    menu = block(src, 'Menu("UIMenu","ID_MENU"', "}// [UIMenu] ID_MENU")
    menu = [l.replace("&&MenuOK&&", ok_text).replace("&&MenuCancel&&", cancel_text) for l in menu]
    footer = src[next(k for k, l in enumerate(src) if l.strip().startswith("EditTexture(")):]
    frame_src = block(src, 'Static("UIStatic","ID_FRAME"', 'ControlObject("UIComboBox"')
    deco = [first(frame_src, "FrameCornerSize("), first(frame_src, "TextureRect(")]
    jazz = first(frame_src, "Font(")[6:-2]
    roman = first(src, 'Font("FLRoman')[6:-2]
    # voile d'information des écrans de championnat d'origine (texte clignotant au centre)
    info = block(read("Championships%s.scr" % res), 'Static("UIStatic","ID_INFO_SCREEN"', "}// [UIStatic] ID_INFO_SCREEN")
    return header, iface, menu, footer, deco, jazz, roman, info


def write(name, out):
    path = os.path.join(OUT_DIR, name)
    open(path, "wb").write("\r\n".join(out).encode("cp1252"))
    print(path)


def build_league(res):
    header, iface, menu, footer, deco, jazz, roman, info = template(res, "UI_LEAGUE", "&&LeaguePlay&&", "&&MenuBack&&")
    # classement : colonnes (id, texte d'en-tête, x en fraction du cadre, largeur en fraction de l'écran, alignement)
    cols = [
        ("POS", "#", 0.015, 0.03, RIGHT),
        ("TEAM", "&&LeagueColTeam&&", 0.085, 0.2, LEFT),
        ("P", "&&LeagueColPlayed&&", 0.44, 0.035, CENTER),
        ("W", "&&LeagueColWon&&", 0.51, 0.035, CENTER),
        ("D", "&&LeagueColDrawn&&", 0.58, 0.035, CENTER),
        ("L", "&&LeagueColLost&&", 0.65, 0.035, CENTER),
        ("GOALS", "&&LeagueColGoals&&", 0.72, 0.065, CENTER),
        ("DIFF", "&&LeagueColDiff&&", 0.835, 0.04, CENTER),
        ("PTS", "&&LeagueColPoints&&", 0.915, 0.04, CENTER),
    ]
    table = []
    for cid, text, x, w, flags in cols:
        table.append(static("ID_TH_%s" % cid, text, SMALL, flags, x, 0.075, w, 0.03))
    for r in range(ROWS):
        y = 0.125 + r * 0.0585
        for cid, text, x, w, flags in cols:
            table.append(static("ID_T%d_%s" % (r, cid), "", SMALL, flags, x, y, w, 0.03))

    # prochain match (logos, noms), puis résultats de la dernière journée et un message
    side = [
        window("ID_LOGO_1", 0.122, 0.085, 0.08, 0.10667),
        window("ID_LOGO_2", 0.622, 0.085, 0.08, 0.10667),
        static("ID_VS", "&&SurvivalVs&&", SMALL, CENTER, 0.4, 0.15, 0.0625, 0.03),
        static("ID_NAME_1", "", SMALL, CENTER, 0.0, 0.26, 0.15625, 0.03),
        static("ID_NAME_2", "", SMALL, CENTER, 0.5, 0.26, 0.15625, 0.03),
        static("ID_ROUND_TITLE", "", roman, CENTER, 0.03, 0.355, 0.29, 0.04),
    ]
    for r in range(RESULTS):
        side.append(static("ID_RES%d" % r, "", SMALL, CENTER, 0.03, 0.42 + r * 0.062, 0.29, 0.03))
    side.append(static("ID_MSG", "", SMALL, CENTER, 0.03, 0.885, 0.29, 0.03))

    out = header + iface
    out += indent_block(info, 1) + [""]
    out += indent_block(menu, 1) + [""]
    out += indent_block(static("ID_TITLE", "&&LeagueTitle&&", jazz, CENTER, 0.0375, 0.1, 0.925, 0.08), 1) + [""]
    out += indent_block(frame("ID_FRAME_TABLE", "&&LeagueFrameTable&&", jazz, deco, 0.0375, FRAME_Y, 0.6, FRAME_H, table), 1) + [""]
    out += indent_block(frame("ID_FRAME_MATCH", "&&LeagueFrameNext&&", jazz, deco, 0.65, FRAME_Y, 0.3125, FRAME_H, side), 1)
    out += footer
    write("League%s.scr" % res, out)


def build_bracket(res):
    header, iface, menu, footer, deco, jazz, roman, info = template(res, "UI_LEAGUE_BRACKET", "&&LeaguePlay&&", "&&MenuBack&&")
    col_x = [0.015 + c * 0.245 for c in range(4)]
    name_w, score_w, score_dx, line_dy, line_h = 0.175, 0.03, 0.19, 0.045, 0.03
    kids = []

    def slot(prefix, x, y):
        out = []
        for side, dy in (("A", 0.0), ("B", line_dy)):
            out.append(static("ID_%s_%sN" % (prefix, side), "", SMALL, LEFT, x, y + dy, name_w, line_h))
            out.append(static("ID_%s_%sS" % (prefix, side), "", SMALL, CENTER, x + score_dx, y + dy, score_w, line_h))
        return out

    def header_(id_, x, y):
        return static(id_, "", SMALL, LEFT, x, y, name_w + 0.03, line_h)

    # tableau des gagnants : 4 matchs au 1er tour, puis chaque match au milieu des deux dont il vient
    wb_y = [[0.12 + i * 0.1 for i in range(4)]]
    for r in range(1, WB_ROUNDS):
        prev = wb_y[-1]
        wb_y.append([(prev[2 * i] + prev[2 * i + 1]) / 2 for i in range(len(prev) // 2)])
    for r in range(WB_ROUNDS):
        kids.append(header_("ID_WH%d" % r, col_x[r], 0.07))
        for i, y in enumerate(wb_y[r]):
            kids += [l for l in slot("W%d_%d" % (r, i), col_x[r], y)]
    # tableau des perdants : 2, 2, 1, 1 matchs
    lb_counts = [2, 2, 1, 1]
    for r in range(LB_ROUNDS):
        kids.append(header_("ID_LH%d" % r, col_x[r], 0.53))
        for i in range(lb_counts[r]):
            y = 0.58 + i * 0.13 if lb_counts[r] == 2 else 0.645
            kids += slot("L%d_%d" % (r, i), col_x[r], y)
    # grande finale (et revanche) : dernière colonne, à hauteur de la finale des gagnants
    gy = wb_y[-1][0]
    kids.append(header_("ID_GH", col_x[3], gy - 0.05))
    kids += slot("G", col_x[3], gy)
    kids.append(header_("ID_GH2", col_x[3], gy + 0.115))
    kids += slot("G2", col_x[3], gy + 0.165)
    for r in range(2):
        kids.append(static("ID_MSG%d" % r, "", SMALL, CENTER, 0.015, 0.855 + r * 0.06, 0.9, line_h))

    out = header + iface
    out += indent_block(info, 1) + [""]
    out += indent_block(menu, 1) + [""]
    out += indent_block(static("ID_TITLE", "&&LeagueTitle&&", jazz, CENTER, 0.0375, 0.1, 0.925, 0.08), 1) + [""]
    out += indent_block(frame("ID_FRAME_BRACKET", "&&LeagueFramePlayoffs&&", jazz, deco, 0.0375, FRAME_Y, 0.925, FRAME_H, kids), 1)
    out += footer
    write("LeagueBracket%s.scr" % res, out)


for res in ("640", "", "1024"):
    build_league(res)
    build_bracket(res)
