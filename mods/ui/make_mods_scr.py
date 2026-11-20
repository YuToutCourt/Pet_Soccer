# Génère l'écran Mods (data/menu/scr/Mods{640,,1024}.scr) à partir d'Options > Match de chaque résolution
# (polices, cadres, cases à cocher et boutons d'origine) : cadre « général » à gauche (cases : déblocages, affichage, debug),
# cadre « triche » à droite (lignes « < valeur > » + une case), boutons OK / Annuler en bas.
# Usage : python mods/ui/make_mods_scr.py  ->  mods/data/Data/Menu/scr/Mods*.scr
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
SRC_DIR = os.path.join(ROOT, "game_pristine", "Data", "Menu", "scr")
OUT_DIR = os.path.join(ROOT, "mods", "data", "Data", "Menu", "scr")
# TextPosFlags : 18 gauche, 34 centre (une ligne, centré verticalement), 273 multiligne en haut à gauche
LEFT, CENTER, MULTI = 18, 34, 273


def block(lines, start_marker, end_marker):
    """Lignes de start_marker à end_marker inclus (première occurrence)."""
    i = next(k for k, l in enumerate(lines) if start_marker in l)
    j = next(k for k in range(i, len(lines)) if end_marker in lines[k])
    return lines[i:j + 1]


def first(lines, prefix):
    return next(l.strip() for l in lines if l.strip().startswith(prefix))


def indent(lines, n):
    return ["   " * n + l.strip() if l.strip() else "" for l in lines]


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


def check(src_check, id_, text, x, y):
    """Case à cocher d'Options > Match (ID_REPLAYS_CHECK) renommée, avec son libellé à droite élargi."""
    out = []
    in_label = False
    for l in src_check:
        s = l.strip()
        s = s.replace("ID_REPLAYS_CHECK_TEXT", id_ + "_TEXT").replace("ID_REPLAYS_CHECK", id_)
        if s.startswith("Static("):
            in_label = True
        if in_label and s.startswith("Text("):
            s = 'Text("%s")' % text
        elif in_label and s.startswith("LogicSize("):
            s = "LogicSize(0.33,%s" % s.split(",", 1)[1]
        elif not in_label and s.startswith("LogicPos("):
            s = "LogicPos(%s,%s)" % (x, y)
        out.append(s)
    return out


def row(trigger, value_deco, roman, prefix, label, y, x_label=0.06, x_prev=0.36, x_value=0.47, w_value=0.15, x_next=0.84):
    """Une ligne « libellé  <  valeur  > » (positions en fraction du cadre, tailles en fraction d'écran)."""
    return [
        static("ID_%s_LABEL" % prefix, label, roman, LEFT, x_label, y, 0.13, 0.053333335),
        arrow(trigger, "ID_%s_PREV" % prefix, "<", x_prev, y - 0.005),
        static("ID_%s_VALUE" % prefix, "", roman, CENTER, x_value, y, w_value, 0.053333335, value_deco),
        arrow(trigger, "ID_%s_NEXT" % prefix, ">", x_next, y - 0.005),
    ]


def frame(id_, text, jazz, deco, x, y, children):
    body = ['Text("%s")' % text, "TextColor(1.0,1.0,1.0)", 'Font("%s")' % jazz, "TextPosFlags(33)"]
    body += common(x, y, 0.4225, 0.68833333) + deco
    out = ['Static("UIStatic","%s","Specific")' % id_, "{"] + indent(body, 1) + [""]
    for c in children:
        out += indent_block(c, 1) + [""]
    return out + ['}// [UIStatic] %s' % id_]


def build(res):
    name = "OptionsMatch%s.scr" % res
    src = open(os.path.join(SRC_DIR, name), "rb").read().decode("cp1252").replace("\r\n", "\n").split("\n")

    header = src[:next(k for k, l in enumerate(src) if l.startswith("Interface("))]
    iface = block(src, 'Interface("UI_OPTIONS_MATCH")', 'Menu("UIMenu","ID_MENU"')[:-1]
    iface = [l.replace("UI_OPTIONS_MATCH", "UI_MODS") for l in iface]
    menu = block(src, 'Menu("UIMenu","ID_MENU"', "}// [UIMenu] ID_MENU")
    footer = src[next(k for k, l in enumerate(src) if l.strip().startswith("EditTexture(")):]
    frame_src = block(src, 'Static("UIStatic","ID_FRAME"', 'ControlObject("UIComboBox"')
    deco = [first(frame_src, "FrameCornerSize("), first(frame_src, "TextureRect(")]
    jazz = first(frame_src, "Font(")[6:-2]
    trigger = block(src, 'Button("UIButtonFlying","ID_COMBO_TRIGGER"', "}// [UIButtonFlying] ID_COMBO_TRIGGER")
    combo_text = block(src, 'Static("UIStatic","ID_COMBO_TEXT"', "}// [UIStatic] ID_COMBO_TEXT")
    roman = first(combo_text, "Font(")[6:-2]
    value_deco = [first(combo_text, "FrameCornerSize("), first(combo_text, "TextureRect(")]
    src_check = block(src, 'Button("UIButton","ID_REPLAYS_CHECK"', "}// [UIButton] ID_REPLAYS_CHECK")

    # cadre gauche : déblocages, affichage, debug
    left = [
        check(src_check, "ID_ALLTEAMS_CHECK", "&&ModsAllTeams&&", 0.08, 0.16),
        check(src_check, "ID_ALLEXTRAS_CHECK", "&&ModsAllExtras&&", 0.08, 0.27),
        check(src_check, "ID_FPS_CHECK", "&&ModsFPS&&", 0.08, 0.38),
        check(src_check, "ID_DEBUG_CHECK", "&&ModsDebug&&", 0.08, 0.49),
        check(src_check, "ID_NUMBERS_CHECK", "&&ModsPlayerNumbers&&", 0.08, 0.60),
        static("ID_KEYS_INFO", "&&ModsKeysInfo&&", roman, MULTI, 0.08, 0.72, 0.36, 0.22),
    ]

    # cadre droit : triches de match
    right = row(trigger, value_deco, roman, "SPEED", "&&ModsSpeed&&", 0.14)
    right += row(trigger, value_deco, roman, "POWER", "&&ModsShotPower&&", 0.27)
    right += row(trigger, value_deco, roman, "SUPER", "&&ModsSuper&&", 0.40)
    right += [
        check(src_check, "ID_SUPERS_CHECK", "&&ModsInfiniteSupers&&", 0.08, 0.55),
        static("ID_CHEAT_INFO", "", roman, MULTI, 0.08, 0.68, 0.36, 0.16),
    ]

    out = header + iface
    out += indent_block(menu, 1) + [""]
    out += indent_block(static("ID_TITLE", "&&ModsTitle&&", jazz, CENTER, 0.0375, 0.1, 0.925, 0.08), 1) + [""]
    out += indent_block(frame("ID_FRAME_GENERAL", "&&ModsFrameGeneral&&", jazz, deco, 0.0375, 0.20833331, left), 1) + [""]
    out += indent_block(frame("ID_FRAME_CHEATS", "&&ModsFrameCheats&&", jazz, deco, 0.54, 0.20833331, right), 1)
    out += footer

    path = os.path.join(OUT_DIR, "Mods%s.scr" % res)
    open(path, "wb").write("\r\n".join(out).encode("cp1252"))
    print(path)


for res in ("640", "", "1024"):
    build(res)
