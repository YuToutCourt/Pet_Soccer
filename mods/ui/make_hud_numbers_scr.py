# [mod] HUD du match : étiquettes des numéros des joueurs 2 à 7 (ModSwitch.UpdateNumbers, voir notes/tactics.md).
# - ID_NUM_<côté>_<numéro> : au-dessus des têtes (option du menu Mods) ;
# - ID_MAPNUM_<côté>_<numéro> : à côté des points de la mini-carte.
# Le texte affiché est numéro - 1 (1 à 6) : le chiffre de la touche directe par défaut (touches 1 à 6),
# l'identifiant garde le numéro de l'écran Tactique (2 à 7).
# Déclarées visibles (sinon Show() ne les dessine pas), cachées par le code au premier affichage.
# Seules les polices chargées pour la résolution marchent (SoccerFonts.LoadFonts).
# Part des Hud*.scr du jeu d'origine et écrit les copies dans mods/data/Data/Menu/scr.
# Relancer après modification : python mods/ui/make_hud_numbers_scr.py
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
SRC_DIR = os.path.join(ROOT, "game_pristine", "Data", "Menu", "scr")
OUT_DIR = os.path.join(ROOT, "mods", "data", "Data", "Menu", "scr")
CRLF = "\r\n"

# résolution -> (police au-dessus des têtes, police de la mini-carte)
FONTS = {"640": ("FLRoman16", "MSSansSerif10"), "": ("FLRoman21", "FLRomanSmall"), "1024": ("FLRoman28", "FLRomanSmall")}


def label(id_, text, font, flags, w, h):
    return [
        '   Static("UIStatic","%s","Specific")' % id_,
        "   {",
        '      Text("%s")' % text,
        "      TextColor(1.0,1.0,1.0)",
        '      Font("%s")' % font,
        "      TextPosFlags(%d)" % flags,
        "      LogicSize(%s,%s)" % (w, h),
        "      LogicPos(0.0,0.0)",
        "      Mouse(0)",
        "      Keyboard(0)",
        "      Joy(0)",
        "      Visible(1)",
        "      ParentPositioning(1)",
        "      ParentCenteringX(0)",
        "      ParentCenteringY(0)",
        "      ParentSizingX(0)",
        "      ParentSizingY(0)",
        "   }// [UIStatic] %s" % id_,
        " ",
    ]


def build(res):
    name = "Hud%s.scr" % res
    text = open(os.path.join(SRC_DIR, name), "rb").read().decode("cp1252")
    head_font, map_font = FONTS[res]
    out = ["   // [mod] numeros des joueurs humains (ModSwitch.UpdateNumbers)"]
    for side in range(2):
        for n in range(2, 8):
            out += label("ID_NUM_%d_%d" % (side, n), str(n - 1), head_font, 34, "0.04", "0.05")
    for side in range(2):
        for n in range(2, 8):
            # aligné à gauche, centré verticalement : posé juste à droite du point
            out += label("ID_MAPNUM_%d_%d" % (side, n), str(n - 1), map_font, 18, "0.02", "0.03")
    anchor = '   Window("UIWindow","ID_ARROW_2","Specific")'
    assert text.count(anchor) == 1, name
    text = text.replace(anchor, CRLF.join(out) + CRLF + anchor)
    path = os.path.join(OUT_DIR, name)
    open(path, "wb").write(text.encode("cp1252"))
    print(path)


for res in ("640", "", "1024"):
    build(res)
