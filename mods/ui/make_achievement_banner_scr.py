# Ajoute le bandeau « succès débloqué » en haut de l'écran de pause / fin de match (data/menu/scr/InGame{640,,1024}.scr) :
# un cadre (même habillage que le cadre « résultats ») dont le titre est réglé par MenuInGame, le nom du succès
# et sa description. Le reste du fichier n'est pas touché ; le script peut être relancé (il remplace son bloc).
# Usage : python mods/ui/make_achievement_banner_scr.py
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
SCR_DIR = os.path.join(ROOT, "mods", "data", "Data", "Menu", "scr")
ROMAN = {"640": "FLRoman16", "": "FLRoman21", "1024": "FLRoman28"}
SMALL = "FLRomanSmall"  # ajoutée par le mod dans SoccerFonts
CENTER = 34
BEGIN = "// [mod] bandeau des succes (mods/ui/make_achievement_banner_scr.py)"
END = "// [mod] fin du bandeau des succes"


def common(x, y, w, h):
    return [
        "LogicSize(%s,%s)" % (w, h),
        "LogicPos(%s,%s)" % (x, y),
        "Mouse(0)",
        "Keyboard(0)",
        "Joy(0)",
        "Visible(1)",
        "ParentPositioning(1)",
        "ParentCenteringX(0)",
        "ParentCenteringY(0)",
        "ParentSizingX(0)",
        "ParentSizingY(0)",
    ]


def static(id_, font, flags, x, y, w, h, extra=(), children=()):
    out = ['Static("UIStatic","%s","Specific")' % id_, "{"]
    body = ['Text("")', "TextColor(1.0,1.0,1.0)", 'Font("%s")' % font, "TextPosFlags(%d)" % flags]
    out += ["   " + l for l in body + common(x, y, w, h) + list(extra)]
    for c in children:
        out += [""] + ["   " + l for l in c]
    return out + ["}// [UIStatic] %s" % id_]


def build(res):
    path = os.path.join(SCR_DIR, "InGame%s.scr" % res)
    lines = open(path, "rb").read().decode("cp1252").split("\r\n")
    if any(BEGIN in l for l in lines):
        i = next(k for k, l in enumerate(lines) if BEGIN in l)
        j = next(k for k, l in enumerate(lines) if END in l)
        del lines[i:j + 1]

    start = next(k for k, l in enumerate(lines) if 'Static("UIStatic","ID_FRAME"' in l)
    frame = lines[start:start + 40]
    first = lambda prefix: next(l.strip() for l in frame if l.strip().startswith(prefix))
    jazz = first("Font(")[6:-2]
    deco = [first("FrameCornerSize("), first("TextureRect(")]

    # positions des enfants en fraction du bandeau, tailles en fraction d'écran
    name = static("ID_ACH_NAME", ROMAN[res], CENTER, 0.05, 0.38, 0.5, 0.045)
    desc = static("ID_ACH_DESC", SMALL, CENTER, 0.05, 0.68, 0.5, 0.035)
    banner = static("ID_ACH_FRAME", jazz, 33, 0.22, 0.005, 0.56, 0.15, deco, [name, desc])

    at = next(k for k, l in enumerate(lines) if l.strip().startswith("EditTexture("))
    lines[at:at] = ["   " + BEGIN] + ["   " + l for l in banner] + ["   " + END]
    open(path, "wb").write("\r\n".join(lines).encode("cp1252"))
    print(path)


for res in ("640", "", "1024"):
    build(res)
