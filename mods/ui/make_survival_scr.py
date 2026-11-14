# Génère l'écran Survie (data/menu/scr/Survival{640,,1024}.scr) à partir d'Options > Match de chaque
# résolution (polices, cadres et boutons d'origine) :
# - cadre « série » à gauche : manche, manches réussies, record, niveau et bonus de l'IA (libellé + valeur) ;
# - cadre « prochain match » à droite : logos et noms des deux équipes, puis 4 lignes de message
#   (résultat du dernier match, fin de série, record) ;
# - boutons « jouer » (ID_OK) et « retour » (ID_CANCEL) en bas ; MenuSurvival change leur texte selon l'état.
# Usage : python mods/ui/make_survival_scr.py  ->  mods/data/Data/Menu/scr/Survival*.scr
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
SRC_DIR = os.path.join(ROOT, "game_pristine", "Data", "Menu", "scr")
OUT_DIR = os.path.join(ROOT, "mods", "data", "Data", "Menu", "scr")
RUN_ROWS = 6  # MenuSurvival._RUN_ROWS
MSG_ROWS = 4  # MenuSurvival._MSG_ROWS
# TextPosFlags (une ligne, centré verticalement) : 16 gauche, 32 centre, 64 droite.
LEFT, CENTER, RIGHT = 18, 34, 66
LOGO = "data/menu/logos/T_Logo_Bullrusia.png"  # remplacé par MenuSurvival
# petite police des listes, ajoutée par le mod dans SoccerFonts (14/16/21 px selon la résolution) : messages longs
SMALL = "FLRomanSmall"


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


def build(res):
    name = "OptionsMatch%s.scr" % res
    src = open(os.path.join(SRC_DIR, name), "rb").read().decode("cp1252").replace("\r\n", "\n").split("\n")

    header = src[:next(k for k, l in enumerate(src) if l.startswith("Interface("))]
    iface = block(src, 'Interface("UI_OPTIONS_MATCH")', 'Menu("UIMenu","ID_MENU"')[:-1]
    iface = [l.replace("UI_OPTIONS_MATCH", "UI_SURVIVAL") for l in iface]
    menu = block(src, 'Menu("UIMenu","ID_MENU"', "}// [UIMenu] ID_MENU")
    menu = [l.replace("&&MenuOK&&", "&&SurvivalPlay&&").replace("&&MenuCancel&&", "&&MenuBack&&") for l in menu]
    footer = src[next(k for k, l in enumerate(src) if l.strip().startswith("EditTexture(")):]
    frame_src = block(src, 'Static("UIStatic","ID_FRAME"', 'ControlObject("UIComboBox"')
    deco = [first(frame_src, "FrameCornerSize("), first(frame_src, "TextureRect(")]
    jazz = first(frame_src, "Font(")[6:-2]
    roman = first(src, 'Font("FLRoman')[6:-2]

    # cadre gauche : la série (libellés à gauche, valeurs à droite)
    left = []
    for r in range(RUN_ROWS):
        y = round(0.15 + r * 0.12, 4)
        left += [
            static("ID_R%d_LABEL" % r, "", roman, LEFT, 0.06, y, 0.22, 0.045),
            static("ID_R%d_VALUE" % r, "", roman, RIGHT, 0.42, y, 0.17, 0.045),
        ]

    # cadre droit : le prochain match (logos centrés sur chaque moitié du cadre, noms dessous), puis les messages.
    # Positions = fraction du cadre (0,5325 de large), tailles = fraction de l'écran.
    right = [
        window("ID_LOGO_1", 0.147, 0.17, 0.11, 0.14667),
        window("ID_LOGO_2", 0.646, 0.17, 0.11, 0.14667),
        static("ID_VS", "&&SurvivalVs&&", roman, CENTER, 0.4, 0.27, 0.1, 0.045),
        static("ID_NAME_1", "", SMALL, CENTER, 0.0, 0.42, 0.2662, 0.045),
        static("ID_NAME_2", "", SMALL, CENTER, 0.5, 0.42, 0.2662, 0.045),
    ]
    for r in range(MSG_ROWS):
        y = round(0.58 + r * 0.085, 4)
        right.append(static("ID_MSG%d" % r, "", SMALL, CENTER, 0.012, y, 0.52, 0.045))

    out = header + iface
    out += indent_block(menu, 1) + [""]
    out += indent_block(static("ID_TITLE", "&&SurvivalTitle&&", jazz, CENTER, 0.0375, 0.1, 0.925, 0.08), 1) + [""]
    out += indent_block(frame("ID_FRAME_RUN", "&&SurvivalFrameRun&&", jazz, deco, 0.0375, 0.20833331, 0.38, 0.68833333, left), 1) + [""]
    out += indent_block(frame("ID_FRAME_MATCH", "&&SurvivalFrameMatch&&", jazz, deco, 0.43, 0.20833331, 0.5325, 0.68833333, right), 1)
    out += footer

    path = os.path.join(OUT_DIR, "Survival%s.scr" % res)
    open(path, "wb").write("\r\n".join(out).encode("cp1252"))
    print(path)


for res in ("640", "", "1024"):
    build(res)
