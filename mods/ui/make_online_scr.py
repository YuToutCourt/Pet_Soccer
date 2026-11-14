# Génère l'écran « en ligne » (data/menu/scr/Online{640,,1024}.scr), lu par MenuOnline :
# - cadre « salon » à gauche : état du serveur, code du salon (champ de saisie), deux boutons dont MenuOnline change le
#   texte (« créer » / « rejoindre » hors salon, « équipe » / « tactique » dans un salon), puis stade et durée (« < valeur > ») ;
# - cadre « match » à droite : logo, équipe, nom et état de l'hôte et de l'invité, puis 3 lignes de message ;
#   hors salon le cadre s'appelle « classement » et montre 8 lignes de classement et celle du joueur ;
# - boutons ID_OK (« partie rapide » / « prêt » / « jouer ») et ID_CANCEL (« retour » / « annuler » / « quitter ») en bas.
# Polices, cadres, flèches et boutons du bas viennent d'Options > Match de chaque résolution ; le champ de saisie et les
# boutons du cadre viennent de l'écran de choix du profil (ChoosePlayer).
# Usage : python mods/ui/make_online_scr.py  ->  mods/data/Data/Menu/scr/Online*.scr
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
SRC_DIR = os.path.join(ROOT, "game_pristine", "Data", "Menu", "scr")
OUT_DIR = os.path.join(ROOT, "mods", "data", "Data", "Menu", "scr")
MSG_ROWS = 3  # MenuOnline._MSG_ROWS
TOP_ROWS = 8  # MenuOnline._TOP_ROWS : lignes du classement
# TextPosFlags (une ligne, centré verticalement) : 16 gauche, 32 centre, 64 droite.
LEFT, CENTER, RIGHT = 18, 34, 66
LOGO = "data/menu/logos/T_Logo_Bullrusia.png"  # remplacé par MenuOnline
SMALL = "FLRomanSmall"  # petite police ajoutée par le mod dans SoccerFonts (14/16/21 px selon la résolution)
ROW_H = 0.053333335
ACTION_WIDTH = 0.3  # boutons du cadre (0,249 dans ChoosePlayer) : en 640, « rejoindre » dépassait et perdait son fond


def read(name):
    return open(os.path.join(SRC_DIR, name), "rb").read().decode("cp1252").replace("\r\n", "\n").split("\n")


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


def clone(src, kind, old_id, new_id, text, pos, size=None, center_x=None):
    """Copie d'un bouton ou d'un champ de saisie : nouvel identifiant, texte, position (et taille, centrage)."""
    out = []
    for l in src:
        s = l.strip()
        if s.startswith('Button("%s","%s"' % (kind, old_id)):
            s = 'Button("%s","%s","%s","Specific")' % (kind, new_id, text)
        elif s.startswith("Text("):
            s = 'Text("%s")' % text
        elif s.startswith("LogicPos("):
            s = "LogicPos(%s,%s)" % pos
        elif s.startswith("LogicSize(") and size:
            s = "LogicSize(%s,%s)" % size
        elif s.startswith("ParentCenteringX(") and center_x is not None:
            s = "ParentCenteringX(%d)" % center_x
        elif s.startswith("}// [%s]" % kind):
            s = "}// [%s] %s" % (kind, new_id)
        out.append(s)
    return out


def row(trigger, value_deco, roman, prefix, label, y):
    """Une ligne « libellé  <  valeur  > » (positions en fraction du cadre, tailles en fraction de l'écran)."""
    return [
        static("ID_%s_LABEL" % prefix, label, roman, LEFT, 0.06, y, 0.1, ROW_H),
        clone(trigger, "UIButtonFlying", "ID_COMBO_TRIGGER", "ID_%s_PREV" % prefix, "<", (0.31, y - 0.005)),
        static("ID_%s_VALUE" % prefix, "", SMALL, CENTER, 0.43, y, 0.135, ROW_H, value_deco),
        clone(trigger, "UIButtonFlying", "ID_COMBO_TRIGGER", "ID_%s_NEXT" % prefix, ">", (0.8, y - 0.005)),
    ]


# Menu du bas : (largeur du menu, largeur de ses deux boutons) en fraction de l'écran, par résolution. Les boutons
# font 0,249 à l'origine ; en 640 la police est plus grosse par rapport à l'écran, il faut plus large.
MENU_SIZES = {"640": (0.92, 0.44), "": (0.84, 0.4), "1024": (0.84, 0.4)}


def spread_menu(menu, res):
    """Élargit et écarte les deux boutons du bas pour « partie rapide » : un texte plus large que son bouton
    perd son fond jaune (le moteur ne le dessine plus) et mordait sur « retour ».
    Le menu (centré) s'élargit, ID_OK reste à son bord gauche, ID_CANCEL va à son bord droit."""
    menu_w, button_w = MENU_SIZES[res]
    out, seen_size, button, widened = [], False, None, 0
    for l in menu:
        s = l.strip()
        if s.startswith("MenuButton("):
            button = s.split('"')[3]
        if s.startswith("LogicSize("):
            h = s[10:-1].split(",")[1]
            if not seen_size:
                seen_size = True
                l = l.replace(s, "LogicSize(%s,%s)" % (menu_w, h))
            else:
                widened += 1
                l = l.replace(s, "LogicSize(%s,%s)" % (button_w, h))
        if s.startswith("LogicPos(") and button == "ID_CANCEL":
            l = l.replace(s, "LogicPos(%s,0.0)" % round((menu_w - button_w) / menu_w, 5))
        out.append(l)
    assert seen_size and widened == 2
    return out


def build(res):
    src = read("OptionsMatch%s.scr" % res)
    player = read("ChoosePlayer%s.scr" % res)

    header = src[:next(k for k, l in enumerate(src) if l.startswith("Interface("))]
    iface = block(src, 'Interface("UI_OPTIONS_MATCH")', 'Menu("UIMenu","ID_MENU"')[:-1]
    iface = [l.replace("UI_OPTIONS_MATCH", "UI_ONLINE") for l in iface]
    menu = block(src, 'Menu("UIMenu","ID_MENU"', "}// [UIMenu] ID_MENU")
    menu = [l.replace("&&MenuOK&&", "&&OnlineReady&&").replace("&&MenuCancel&&", "&&MenuBack&&") for l in menu]
    menu = spread_menu(menu, res)
    footer = src[next(k for k, l in enumerate(src) if l.strip().startswith("EditTexture(")):]
    frame_src = block(src, 'Static("UIStatic","ID_FRAME"', 'ControlObject("UIComboBox"')
    deco = [first(frame_src, "FrameCornerSize("), first(frame_src, "TextureRect(")]
    jazz = first(frame_src, "Font(")[6:-2]
    trigger = block(src, 'Button("UIButtonFlying","ID_COMBO_TRIGGER"', "}// [UIButtonFlying] ID_COMBO_TRIGGER")
    combo_text = block(src, 'Static("UIStatic","ID_COMBO_TEXT"', "}// [UIStatic] ID_COMBO_TEXT")
    roman = first(combo_text, "Font(")[6:-2]
    value_deco = [first(combo_text, "FrameCornerSize("), first(combo_text, "TextureRect(")]
    button = block(player, 'Button("UIButtonFlying","ID_OK"', "}// [UIButtonFlying] ID_OK")
    edit = block(player, 'Button("UIEdit","ID_EDIT"', "}// [UIEdit] ID_EDIT")

    # cadre gauche : le salon. Positions = fraction du cadre (0,38 de large), tailles = fraction de l'écran.
    left = [
        static("ID_STATUS", "", SMALL, CENTER, 0.03, 0.13, 0.357, 0.045),
        static("ID_CODE_LABEL", "&&OnlineCode&&", roman, LEFT, 0.06, 0.24, 0.1, ROW_H),
        clone(edit, "UIEdit", "ID_EDIT", "ID_CODE", "", (0.43, 0.24), (0.135, 0.055), center_x=0),
        clone(button, "UIButtonFlying", "ID_OK", "ID_ACTION1", "&&OnlineCreate&&", (0.0, 0.36), (ACTION_WIDTH, 0.08)),
        clone(button, "UIButtonFlying", "ID_OK", "ID_ACTION2", "&&OnlineJoin&&", (0.0, 0.5), (ACTION_WIDTH, 0.08)),
    ]
    left += row(trigger, value_deco, roman, "STADIUM", "&&OnlineStadium&&", 0.69)
    left += row(trigger, value_deco, roman, "TIME", "&&OnlineTime&&", 0.82)

    # cadre droit : l'hôte sur la moitié gauche, l'invité sur la moitié droite, puis les messages.
    # Positions = fraction du cadre (0,5325 de large), tailles = fraction de l'écran.
    right = [
        window("ID_LOGO_1", 0.147, 0.13, 0.11, 0.14667),
        window("ID_LOGO_2", 0.646, 0.13, 0.11, 0.14667),
        static("ID_VS", "&&SurvivalVs&&", roman, CENTER, 0.4, 0.21, 0.1, 0.045),
    ]
    for n, x in ((1, 0.0), (2, 0.5)):
        right += [
            static("ID_NAME_%d" % n, "", SMALL, CENTER, x, 0.36, 0.2662, 0.045),
            static("ID_PLAYER_%d" % n, "", SMALL, CENTER, x, 0.435, 0.2662, 0.045),
            static("ID_READY_%d" % n, "", SMALL, CENTER, x, 0.51, 0.2662, 0.045),
        ]
    for r in range(MSG_ROWS):
        y = round(0.65 + r * 0.085, 4)
        right.append(static("ID_MSG%d" % r, "", SMALL, CENTER, 0.012, y, 0.52, 0.045))
    # classement de l'Elo en ligne : affiché hors salon, à la place des deux équipes (vides à ce moment-là).
    # Une ligne = rang et nom à gauche, Elo et bilan à droite ; puis la ligne du joueur.
    for r in range(TOP_ROWS):
        y = round(0.115 + r * 0.058, 4)
        right.append(static("ID_TOP%d_NAME" % r, "", SMALL, LEFT, 0.06, y, 0.25, 0.04))
        right.append(static("ID_TOP%d_ELO" % r, "", SMALL, RIGHT, 0.53, y, 0.22, 0.04))
    right.append(static("ID_TOP_ME", "", SMALL, CENTER, 0.012, 0.585, 0.52, 0.04))

    out = header + iface
    out += indent_block(menu, 1) + [""]
    out += indent_block(static("ID_TITLE", "&&OnlineTitle&&", jazz, CENTER, 0.0375, 0.1, 0.925, 0.08), 1) + [""]
    out += indent_block(frame("ID_FRAME_ROOM", "&&OnlineFrameRoom&&", jazz, deco, 0.0375, 0.20833331, 0.38, 0.68833333, left), 1) + [""]
    out += indent_block(frame("ID_FRAME_MATCH", "&&OnlineFrameMatch&&", jazz, deco, 0.43, 0.20833331, 0.5325, 0.68833333, right), 1)
    out += footer

    path = os.path.join(OUT_DIR, "Online%s.scr" % res)
    open(path, "wb").write("\r\n".join(out).encode("cp1252"))
    print(path)


for res in ("640", "", "1024"):
    build(res)
