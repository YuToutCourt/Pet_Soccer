# Génère la planche d'icônes HUD des nouveaux supers (128x256 RGBA), même grille que p_ikonki.png :
# gel, ballon fou, aimant, tir double (types 4..7 de ModSupers, voir notes/supers.md).
# Le ballon est repris de l'icône « feinte » d'origine pour garder le même rendu.
# Usage : python mods/art/make_super_icons.py  ->  mods/data/Data/Menu/P_Ikonki2.png (+ aperçu dans mods/art)
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "game_pristine", "Data", "Menu", "P_Ikonki.png")
OUT = os.path.join(HERE, "..", "data", "Data", "Menu", "P_Ikonki2.png")
PREVIEW = os.path.join(HERE, "super_icons_preview.png")

S = 4                                  # suréchantillonnage, réduit à la fin
CELL_W, CELL_H = 126, 50               # taille d'une case (HudSuper._MOD_ICON_RECTS)
CELLS_Y = [1, 53, 105, 157]            # haut de chaque case, comme p_ikonki.png
W, H = CELL_W * S, CELL_H * S
OUTLINE = (20, 20, 28, 255)

# ballon de l'icône « feinte » (case du haut de p_ikonki.png)
BALL_C = (35.5, 25.5)
BALL_R = 19.0


def load_ball():
    """Ballon d'origine détouré (cercle), agrandi à l'échelle de travail."""
    src = Image.open(SRC).convert("RGBA")
    cx, cy = BALL_C
    r = BALL_R + 1.0
    box = (int(cx - r), int(cy - r), int(math.ceil(cx + r)), int(math.ceil(cy + r)))
    ball = src.crop(box).resize(((box[2] - box[0]) * S, (box[3] - box[1]) * S), Image.LANCZOS)
    a = np.array(ball).astype(np.float32)
    h, w = a.shape[:2]
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    ccx, ccy = (cx - box[0]) * S, (cy - box[1]) * S
    d = np.sqrt((x + 0.5 - ccx) ** 2 + (y + 0.5 - ccy) ** 2)
    inner = (BALL_R - 0.6) * S
    a[..., 3] *= np.clip((inner - d) / S + 0.5, 0, 1)
    out = Image.fromarray(a.astype(np.uint8), "RGBA")
    # contour sombre propre (la flèche d'origine touchait le bord droit)
    dr = ImageDraw.Draw(out)
    dr.ellipse((ccx - inner, ccy - inner, ccx + inner, ccy + inner), outline=OUTLINE, width=int(1.4 * S))
    return out


BALL = None


def paste_ball(img, cx, cy, scale=1.0, alpha=1.0, tint=None):
    """Colle le ballon centré en (cx, cy) (coordonnées de travail)."""
    b = BALL
    if scale != 1.0:
        b = b.resize((int(b.width * scale), int(b.height * scale)), Image.LANCZOS)
    a = np.array(b).astype(np.float32)
    if tint is not None:
        lum = a[..., :3].mean(-1, keepdims=True) / 255.0
        a[..., :3] = lum * np.array(tint, np.float32)
    a[..., 3] *= alpha
    b = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
    img.alpha_composite(b, (int(cx - b.width / 2), int(cy - b.height / 2)))


def shadow(img, blur=3):
    """Ombre douce sous l'icône, comme les icônes d'origine."""
    a = np.array(img)[..., 3].astype(np.float32)
    sh = Image.fromarray((a * 0.45).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(blur * S))
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    layer.putalpha(sh)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.alpha_composite(layer, (int(1.5 * S), int(2.5 * S)))
    out.alpha_composite(img)
    return out


def gradient_fill(size, top, bottom):
    h = size[1]
    t = np.linspace(0, 1, h, dtype=np.float32)[:, None, None]
    g = np.array(top, np.float32) * (1 - t) + np.array(bottom, np.float32) * t
    g = np.broadcast_to(g, (h, size[0], len(top))).copy()
    return Image.fromarray(g.astype(np.uint8), "RGBA" if len(top) == 4 else "RGB")


def masked(fill, mask):
    out = fill.convert("RGBA")
    out.putalpha(mask)
    return out


# ------------------------------------------------------------------ gel : ballon pris dans un glaçon + flocon

def snowflake(dr, cx, cy, r, color, width):
    for k in range(6):
        ang = math.pi / 2 + k * math.pi / 3
        ex, ey = cx + r * math.cos(ang), cy - r * math.sin(ang)
        dr.line((cx, cy, ex, ey), fill=color, width=width)
        for f in (0.45, 0.72):
            bx, by = cx + r * f * math.cos(ang), cy - r * f * math.sin(ang)
            for s in (-1, 1):
                a2 = ang + s * math.pi / 4
                l = r * 0.28
                dr.line((bx, by, bx + l * math.cos(a2), by - l * math.sin(a2)), fill=color, width=width)


def icon_freeze():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    # glaçon : carré arrondi translucide
    x0, y0, x1, y1 = 34 * S, 3 * S, 80 * S, 46 * S
    paste_ball(img, (x0 + x1) / 2, (y0 + y1) / 2 + S, 0.92, 1.0, tint=(170, 215, 255))
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle((x0, y0, x1, y1), radius=7 * S, fill=125)
    ice = masked(gradient_fill((W, H), (230, 252, 255, 255), (110, 200, 255, 255)), mask)
    img.alpha_composite(ice)
    dr = ImageDraw.Draw(img)
    # reflets
    dr.line((x0 + 5 * S, y0 + 9 * S, x0 + 5 * S, y1 - 9 * S), fill=(255, 255, 255, 210), width=2 * S)
    dr.line((x0 + 9 * S, y0 + 5 * S, x1 - 14 * S, y0 + 5 * S), fill=(255, 255, 255, 210), width=2 * S)
    dr.polygon([(x1 - 13 * S, y1 - 4 * S), (x1 - 4 * S, y1 - 13 * S), (x1 - 4 * S, y1 - 9 * S), (x1 - 9 * S, y1 - 4 * S)], fill=(255, 255, 255, 170))
    dr.rounded_rectangle((x0, y0, x1, y1), radius=7 * S, outline=(18, 58, 120, 255), width=int(1.6 * S))
    # flocon à droite
    snowflake(dr, 103 * S, 25 * S, 17 * S, (18, 58, 120, 255), int(4.2 * S))
    snowflake(dr, 103 * S, 25 * S, 17 * S, (200, 240, 255, 255), int(2.0 * S))
    # éclats de givre à gauche
    for (px, py, pr) in [(18, 12, 4), (12, 30, 3), (24, 40, 3.5)]:
        snowflake(dr, px * S, py * S, pr * S, (150, 210, 255, 255), int(1.2 * S))
    return shadow(img)


# ------------------------------------------------------------------ ballon fou : traînée ondulée multicolore

def icon_crazy():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    colors = [(255, 70, 200), (255, 200, 40), (60, 220, 120), (70, 160, 255)]
    x_start, x_end = 6 * S, 88 * S
    amp, waves = 11 * S, 1.75
    pts = []
    for i in range(200):
        t = i / 199
        x = x_start + (x_end - x_start) * t
        y = 25 * S + amp * math.sin(t * waves * 2 * math.pi + math.pi) * (0.35 + 0.65 * t)
        pts.append((x, y))
    trail = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    td = ImageDraw.Draw(trail)
    td.line(pts, fill=OUTLINE, width=int(12 * S), joint="curve")
    for k, c in enumerate(colors):
        off = (k - 1.5) * 2.4 * S
        td.line([(x, y + off) for x, y in pts], fill=c + (255,), width=int(2.8 * S), joint="curve")
    # la traînée s'efface vers la gauche
    fade = np.clip(np.linspace(0.0, 1.6, W, dtype=np.float32), 0, 1)[None, :]
    ta = np.array(trail).astype(np.float32)
    ta[..., 3] *= fade
    img.alpha_composite(Image.fromarray(ta.astype(np.uint8), "RGBA"))
    dr = ImageDraw.Draw(img)
    # petites étoiles (tournis) autour du ballon
    for (sx, sy, sr) in [(84, 7, 4.5), (122, 15, 3.5), (118, 44, 4)]:
        star = []
        for k in range(10):
            a = -math.pi / 2 + k * math.pi / 5
            rr = sr * S if k % 2 == 0 else sr * S * 0.45
            star.append((sx * S + rr * math.cos(a), sy * S + rr * math.sin(a)))
        dr.polygon(star, fill=(255, 225, 60, 255), outline=OUTLINE)
    paste_ball(img, 101 * S, 26 * S, 1.0)
    return shadow(img)


# ------------------------------------------------------------------ aimant : fer à cheval qui attire le ballon

def icon_magnet():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    cx, cy = 30 * S, 25 * S
    r_out, r_in = 21 * S, 10 * S
    arm = 20 * S
    mask = Image.new("L", (W, H), 0)
    md = ImageDraw.Draw(mask)
    # U ouvert vers la droite : demi-anneau à gauche + deux bras
    md.pieslice((cx - r_out, cy - r_out, cx + r_out, cy + r_out), 90, 270, fill=255)
    md.rectangle((cx, cy - r_out, cx + arm, cy + r_out), fill=255)
    md.pieslice((cx - r_in, cy - r_in, cx + r_in, cy + r_in), 90, 270, fill=0)
    md.rectangle((cx, cy - r_in, cx + arm + S, cy + r_in), fill=0)
    body = masked(gradient_fill((W, H), (255, 110, 100, 255), (170, 10, 20, 255)), mask)
    tips = Image.new("L", (W, H), 0)
    ImageDraw.Draw(tips).rectangle((cx + arm - 8 * S, 0, cx + arm, H), fill=255)
    tips = Image.fromarray(np.minimum(np.array(tips), np.array(mask)), "L")
    metal = masked(gradient_fill((W, H), (250, 250, 255, 255), (130, 135, 150, 255)), tips)
    img.alpha_composite(body)
    img.alpha_composite(metal)
    # contour
    edge = mask.filter(ImageFilter.MaxFilter(int(2 * S) * 2 + 1))
    ea = np.clip(np.array(edge).astype(np.int32) - np.array(mask).astype(np.int32), 0, 255).astype(np.uint8)
    ol = Image.new("RGBA", (W, H), OUTLINE)
    ol.putalpha(Image.fromarray(ea, "L"))
    img.alpha_composite(ol)
    dr = ImageDraw.Draw(img)
    dr.line((cx + arm - 8 * S, cy - r_out, cx + arm - 8 * S, cy - r_in), fill=OUTLINE, width=S)
    dr.line((cx + arm - 8 * S, cy + r_in, cx + arm - 8 * S, cy + r_out), fill=OUTLINE, width=S)
    # reflet sur le haut de l'anneau
    dr.arc((cx - r_out + 3 * S, cy - r_out + 3 * S, cx + r_out - 3 * S, cy + r_out - 3 * S), 190, 250, fill=(255, 210, 205, 230), width=2 * S)
    # ondes d'attraction qui partent de l'ouverture de l'aimant vers le ballon
    mx = cx + arm - 6 * S
    for rr in (9, 16, 23):
        box = (mx - rr * S, cy - rr * S, mx + rr * S, cy + rr * S)
        dr.arc(box, -38, 38, fill=OUTLINE, width=int(4.2 * S))
        dr.arc(box, -36, 36, fill=(110, 200, 255, 255), width=int(2.2 * S))
    paste_ball(img, 102 * S, 25 * S, 1.0)
    return shadow(img)


# ------------------------------------------------------------------ tir double : vrai ballon + leurre fantôme

def icon_double():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    # traits de vitesse en V (deux trajectoires), plus pâles pour le leurre
    streaks = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(streaks)
    for (y0, y1, x1, color) in [(26, 15, 62, (150, 200, 255)), (28, 29, 82, (255, 255, 255))]:
        for dy in (-6, 0, 6):
            seg = (6 * S, (y0 + dy * 0.4) * S, x1 * S, (y1 + dy) * S)
            sd.line(seg, fill=OUTLINE, width=int(4.4 * S))
            sd.line(seg, fill=color + (255,), width=int(2.2 * S))
    fade = np.clip(np.linspace(0.0, 1.8, W, dtype=np.float32), 0, 1)[None, :]
    sa = np.array(streaks).astype(np.float32)
    sa[..., 3] *= fade
    img.alpha_composite(Image.fromarray(sa.astype(np.uint8), "RGBA"))
    # leurre : ballon translucide bleuté, en haut
    paste_ball(img, 76 * S, 15 * S, 0.78, 0.55, tint=(150, 200, 255))
    # vrai ballon devant
    paste_ball(img, 98 * S, 29 * S, 1.0)
    return shadow(img)


def main():
    global BALL
    BALL = load_ball()
    sheet = Image.new("RGBA", (128, 256), (0, 0, 0, 0))
    icons = [icon_freeze(), icon_crazy(), icon_magnet(), icon_double()]
    for icon, top in zip(icons, CELLS_Y):
        small = icon.resize((CELL_W, CELL_H), Image.LANCZOS)
        sheet.alpha_composite(small, (1, top))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sheet.save(OUT)
    # aperçu : planche x3 sur fond sombre et clair
    big = sheet.resize((128 * 3, 256 * 3), Image.LANCZOS)
    prev = Image.new("RGBA", (128 * 6 + 30, 256 * 3 + 20), (255, 255, 255, 255))
    dark = Image.new("RGBA", big.size, (40, 60, 40, 255))
    dark.alpha_composite(big)
    prev.alpha_composite(dark, (10, 10))
    light = Image.new("RGBA", big.size, (200, 220, 200, 255))
    light.alpha_composite(big)
    prev.alpha_composite(light, (20 + 128 * 3, 10))
    prev.convert("RGB").save(PREVIEW)
    print("ecrit", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
