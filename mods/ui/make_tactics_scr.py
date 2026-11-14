# Génère l'écran Tactique (data/menu/scr/Tactics{640,,1024}.scr) à partir des blocs d'Options > Match
# de chaque résolution (polices, cadres et boutons d'origine) :
#   - cadre « Placement » à gauche : situation « < Jeu > », carte du terrain (bouton ID_FIELD) et ses pions ;
#   - cadre « Composition » à droite : schéma « < > », joueur sélectionné, son rôle « < > », description ;
#   - boutons Auto / OK / Annuler en bas.
# Les pions (ID_TOK*) sont placés par MenuTactics.java ; la texture vient de mods/art/make_tactics_field.py.
# Usage : python mods/ui/make_tactics_scr.py  ->  mods/data/Data/Menu/scr/Tactics*.scr
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
SRC_DIR = os.path.join(ROOT, "game_pristine", "Data", "Menu", "scr")
OUT_DIR = os.path.join(ROOT, "mods", "data", "Data", "Menu", "scr")
SLOTS = 6

TEX = "data/menu/tactics_field.png"
FIELD_RECT = (0.0, 0.0, 512.0, 282.0)          # doit rester aligné sur MenuTactics.java
TOKEN_RECT = (0.0, 320.0, 64.0, 384.0)         # pion « joueur »
GK_RECT = (128.0, 320.0, 192.0, 384.0)
BALL_RECT = (192.0, 320.0, 256.0, 384.0)

# disposition (fractions de l'écran)
MAP_FRAME = (0.0375, 0.20833331, 0.555, 0.68833333)
SIDE_FRAME = (0.6075, 0.20833331, 0.355, 0.68833333)
FIELD_X, FIELD_Y, FIELD_W = 0.0625, 0.345, 0.505
FIELD_H = FIELD_W * (4.0 / 3.0) / (512.0 / 282.0)   # écran 4:3, terrain 512x282
TOKEN_W, TOKEN_H = 0.034, 0.034 * 4.0 / 3.0
BALL_W, BALL_H = 0.024, 0.024 * 4.0 / 3.0


def block(lines, start_marker, end_marker):
    """Lignes de start_marker à end_marker inclus (première occurrence)."""
    i = next(k for k, l in enumerate(lines) if start_marker in l)
    j = next(k for k in range(i, len(lines)) if end_marker in lines[k])
    return lines[i:j + 1]


def first(lines, prefix):
    return next(l.strip() for l in lines if l.strip().startswith(prefix))


def indent(lines, n):
    return ["   " * n + l.strip() if l.strip() else "" for l in lines]


def common(x, y, w, h, mouse=1):
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


def tex_rect(rect):
    return 'TextureRect("%s",512.0,512.0,%s,%s,%s,%s)' % ((TEX,) + rect)


def static(id_, text, font, flags, x, y, w, h, extra=()):
    body = ['Text("%s")' % text, "TextColor(1.0,1.0,1.0)", 'Font("%s")' % font, "TextPosFlags(%d)" % flags]
    body += common(x, y, w, h, mouse=0) + list(extra)
    return ['Static("UIStatic","%s","Specific")' % id_, "{"] + indent(body, 1) + ['}// [UIStatic] %s' % id_]


def arrow(trigger, id_, text, x, y):
    """Bouton « = » des listes déroulantes, renommé et repositionné."""
    out = []
    for l in trigger:
        s = l.strip()
        if s.startswith('Button("UIButtonFlying","ID_COMBO_TRIGGER"'):
            s = 'Button("UIButtonFlying","%s","%s","Specific")' % (id_, text)
        elif s.startswith("Text("):
            s = 'Text("%s")' % text
        elif s.startswith("LogicPos("):
            s = "LogicPos(%s,%s)" % (x, y)
        elif s.startswith("}// [UIButtonFlying]"):
            s = "}// [UIButtonFlying] %s" % id_
        out.append(s)
    return [out[0]] + indent(out[1:-1], 0) + [out[-1]]


def field_button(roman):
    """Carte du terrain : bouton simple (sans ressort) texturé, le clic est lu dans MenuTactics."""
    body = [
        "Checked(0)", "Disabled(0)", "CanCallPress(0)", "CanCheck(0)", "MouseLeaveDestroyFocus(1)",
        'CheckSound("data/sounds/click")', tex_rect(FIELD_RECT).replace("TextureRect", "TextureCheckedRect"),
        "CallPressPeriod(0.1)", "DoubleClick(0.3)", "Command(1)", "Notify(1)", "CanFocus(0)",
        "SelKeyUp(0)", "SelKeyDown(0)", 'Text("")', "TextColor(1.0,1.0,1.0)", 'Font("%s")' % roman,
        "TextPosFlags(34)",
    ] + common(FIELD_X, FIELD_Y, FIELD_W, round(FIELD_H, 6)) + [tex_rect(FIELD_RECT)]
    return ['Button("UIButton","ID_FIELD","","Specific")', "{"] + indent(body, 1) + ["}// [UIButton] ID_FIELD"]


def frame(id_, text, jazz, deco, rect, children):
    x, y, w, h = rect
    body = ['Text("%s")' % text, "TextColor(1.0,1.0,1.0)", 'Font("%s")' % jazz, "TextPosFlags(33)"]
    body += common(x, y, w, h) + deco
    out = ['Static("UIStatic","%s","Specific")' % id_, "{"] + indent(body, 1) + [""]
    for c in children:
        out += indent_block(c, 1) + [""]
    return out + ['}// [UIStatic] %s' % id_]


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


def row(trigger, value_deco, roman, prefix, y, x_prev, x_value, w_value, x_next):
    """Une ligne « <  valeur  > » (positions en fraction du cadre, tailles en fraction d'écran)."""
    return [
        arrow(trigger, "ID_%s_PREV" % prefix, "<", x_prev, y - 0.005),
        static("ID_%s_VALUE" % prefix, "", roman, 34, x_value, y, w_value, 0.053333335, value_deco),
        arrow(trigger, "ID_%s_NEXT" % prefix, ">", x_next, y - 0.005),
    ]


def menu_with_auto(menu):
    """Menu OK / Annuler élargi, avec un bouton Auto à gauche (copie du bouton OK)."""
    ok = block(menu, 'MenuButton("UIMenuButtonFlying","ID_OK"', "}// [UIMenuButtonFlying] ID_OK")
    auto = [l.replace("ID_OK", "ID_AUTO").replace("&&MenuOK&&", "&&TacticsAuto&&") for l in ok]
    out = []
    for l in menu:
        s = l.strip()
        if s.startswith("LogicSize(0.55375"):
            l = l.replace(s, "LogicSize(0.8,0.083333336)")
        elif s.startswith("LogicPos(0.44249997"):
            l = l.replace(s, "LogicPos(0.19,0.90166664)")
        elif s.startswith('MenuButton("UIMenuButtonFlying","ID_OK"'):
            out += auto
        out.append(l)
    # positions des boutons : Auto 0, OK 1/3, Annuler 2/3 (fractions du menu)
    res, current = [], None
    for l in out:
        s = l.strip()
        if s.startswith("MenuButton("):
            current = s.split('"')[3]
        if current and s.startswith("LogicPos("):
            pos = {"ID_AUTO": "0.0", "ID_OK": "0.3333", "ID_CANCEL": "0.6667"}[current]
            l = l.replace(s, "LogicPos(%s,0.0)" % pos)
            current = None
        res.append(l)
    return res


def build(res):
    name = "OptionsMatch%s.scr" % res
    src = open(os.path.join(SRC_DIR, name), "rb").read().decode("cp1252").replace("\r\n", "\n").split("\n")

    header = src[:next(k for k, l in enumerate(src) if l.startswith("Interface("))]
    iface = block(src, 'Interface("UI_OPTIONS_MATCH")', 'Menu("UIMenu","ID_MENU"')[:-1]
    iface = [l.replace("UI_OPTIONS_MATCH", "UI_TACTICS") for l in iface]
    menu = block(src, 'Menu("UIMenu","ID_MENU"', "}// [UIMenu] ID_MENU")
    footer = src[next(k for k, l in enumerate(src) if l.strip().startswith("EditTexture(")):]
    frame_src = block(src, 'Static("UIStatic","ID_FRAME"', 'ControlObject("UIComboBox"')
    deco = [first(frame_src, "FrameCornerSize("), first(frame_src, "TextureRect(")]
    jazz = first(frame_src, "Font(")[6:-2]
    trigger = block(src, 'Button("UIButtonFlying","ID_COMBO_TRIGGER"', "}// [UIButtonFlying] ID_COMBO_TRIGGER")
    combo_text = block(src, 'Static("UIStatic","ID_COMBO_TEXT"', "}// [UIStatic] ID_COMBO_TEXT")
    roman = first(combo_text, "Font(")[6:-2]
    value_deco = [first(combo_text, "FrameCornerSize("), first(combo_text, "TextureRect(")]

    # cadre gauche : situation au-dessus de la carte, aide en dessous
    mx, my, mw, mh = MAP_FRAME
    left = row(trigger, value_deco, roman, "SIT", 0.1, 0.14, 0.23, 0.30, 0.78)
    hint_y = (FIELD_Y + FIELD_H + 0.012 - my) / mh
    left += [static("ID_MAP_HINT", "", roman, 273, (FIELD_X - mx) / mw, round(hint_y, 4), FIELD_W, 0.15)]

    # cadre droit : schéma, joueur sélectionné, rôle, description
    right = row(trigger, value_deco, roman, "SCHEME", 0.1, 0.04, 0.17, 0.235, 0.84)
    right += [static("ID_PLAYER", "", roman, 34, 0.04, 0.22, 0.33, 0.053333335)]
    right += row(trigger, value_deco, roman, "ROLE", 0.31, 0.04, 0.17, 0.235, 0.84)
    right += [static("ID_INFO", "", roman, 273, 0.05, 0.43, 0.33, 0.38)]

    out = header + iface
    out += indent_block(menu_with_auto(menu), 1) + [""]
    out += indent_block(static("ID_TITLE", "&&TacticsTitle&&", jazz, 34, 0.0375, 0.1, 0.925, 0.08), 1) + [""]
    # le premier élément défini est dessiné au-dessus des suivants : pions, puis carte, puis cadres
    out += indent_block(static("ID_TOK_BALL", "", roman, 34, 0.0, 0.0, BALL_W, round(BALL_H, 6), [tex_rect(BALL_RECT)]), 1) + [""]
    for k in range(SLOTS):
        out += indent_block(static("ID_TOK%d" % k, str(k + 2), roman, 34, 0.0, 0.0, TOKEN_W, round(TOKEN_H, 6), [tex_rect(TOKEN_RECT)]), 1) + [""]
    out += indent_block(static("ID_TOK_GK", "1", roman, 34, 0.0, 0.0, TOKEN_W, round(TOKEN_H, 6), [tex_rect(GK_RECT)]), 1) + [""]
    out += indent_block(field_button(roman), 1) + [""]
    out += indent_block(frame("ID_FRAME_MAP", "&&TacticsFrameMap&&", jazz, deco, MAP_FRAME, left), 1) + [""]
    out += indent_block(frame("ID_FRAME_ROLES", "&&TacticsFrameRoles&&", jazz, deco, SIDE_FRAME, right), 1)
    out += footer

    path = os.path.join(OUT_DIR, "Tactics%s.scr" % res)
    open(path, "wb").write("\r\n".join(out).encode("cp1252"))
    print(path)


for res in ("640", "", "1024"):
    build(res)
