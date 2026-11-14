# Génère les badges de rang Elo (256x256 RGBA, net même agrandi en fin de match) dans le style des logos d'équipe
# (data/menu/logos/T_Logo_*.png) : anneau métallique, disque coloré, nom en arc, emblème, reflet.
# Usage : python mods/art/make_rank_logos.py  ->  mods/data/Data/Menu/logos/T_Rank_*.png
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

S = 4                      # suréchantillonnage (dessin sur une base 128), réduit à OUT à la fin
OUT = 256
W = 128 * S
C = W / 2
R_OUT = 61 * S             # bord extérieur de l'anneau métal
R_METAL = 54 * S           # bord intérieur de l'anneau métal
R_DISC = 52 * S            # disque (après le filet coloré)
R_BAND = 37 * S            # bande du nom
FONT = "C:/Windows/Fonts/georgiab.ttf"
OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "data", "Data", "Menu", "logos")

GOLD = ((255, 244, 160), (236, 186, 40), (140, 88, 0))

# nom de fichier, texte, matière (clair, moyen, sombre), disque (centre, bord), emblème, déco
RANKS = [
    ("Bois", "BOIS", ((226, 192, 140), (164, 122, 76), (84, 58, 32)), ((150, 108, 64), (86, 58, 30)), "ball", ()),
    ("Bronze", "BRONZE", ((255, 204, 150), (208, 128, 60), (110, 58, 20)), ((178, 90, 34), (100, 44, 12)), "ball", ()),
    ("Argent", "ARGENT", ((255, 255, 255), (196, 202, 214), (104, 110, 126)), ((84, 100, 130), (36, 46, 70)), "ball", ()),
    ("Or", "OR", GOLD, ((214, 40, 36), (120, 10, 12)), "ball", ("laurels",)),
    ("Platine", "PLATINE", ((225, 255, 250), (120, 214, 204), (36, 120, 116)), ((24, 140, 140), (8, 70, 76)), "ball", ("laurels",)),
    ("Diamant", "DIAMANT", ((220, 238, 255), (112, 166, 255), (32, 64, 176)), ((24, 70, 200), (8, 26, 110)), "gem", ("laurels",)),
    ("Challenger", "CHALLENGER", ((255, 196, 250), (230, 84, 214), (104, 18, 116)), ((150, 26, 166), (64, 6, 82)), "ball", ("laurels", "crown")),
]


def grid():
    y, x = np.mgrid[0:W, 0:W].astype(np.float32)
    dx, dy = x + 0.5 - C, y + 0.5 - C
    return dx, dy, np.sqrt(dx * dx + dy * dy)


DX, DY, R = grid()


def mask(cond):
    return Image.fromarray((cond * 255).astype(np.uint8), "L")


def lerp(a, b, t):
    a, b = np.array(a, np.float32), np.array(b, np.float32)
    return a + (b - a) * t[..., None]


def bevel_ring(r_in, r_out, base, spec=1.0):
    """Anneau bombé éclairé en haut à gauche, comme le cerclage des logos."""
    t = np.clip((R - r_in) / (r_out - r_in), 0, 1) * 2 - 1      # -1 intérieur, 1 extérieur
    ux, uy = DX / np.maximum(R, 1), DY / np.maximum(R, 1)
    nz = np.sqrt(np.clip(1 - t * t, 0, 1))
    nx, ny = t * ux, t * uy
    lx, ly, lz = -0.45, -0.65, 0.62
    diff = np.clip(nx * lx + ny * ly + nz * lz, 0, 1)
    hz = np.array([lx, ly, lz + 1]) / np.linalg.norm([lx, ly, lz + 1])
    spc = np.clip(nx * hz[0] + ny * hz[1] + nz * hz[2], 0, 1) ** 24 * spec
    col = np.array(base, np.float32) * (0.35 + 0.8 * diff[..., None]) + 255 * spc[..., None]
    return np.clip(col, 0, 255)


def paste_array(img, arr, cond):
    layer = Image.fromarray(arr.astype(np.uint8), "RGB").convert("RGBA")
    img.paste(layer, (0, 0), mask(cond))


def star_points(cx, cy, r_out, r_in, rot=-90):
    pts = []
    for i in range(10):
        r = r_out if i % 2 == 0 else r_in
        a = math.radians(rot + i * 36)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def shield_points(cx, cy, w, h, n=40):
    """Écusson classique : haut droit, flancs droits puis courbes jusqu'à la pointe."""
    top, side = cy - h / 2, cy + h * 0.02
    pts = [(cx - w / 2, top), (cx + w / 2, top), (cx + w / 2, side)]
    for i in range(1, n + 1):
        t = i / n
        pts.append((cx + w / 2 * math.cos(t * math.pi / 2), side + (cy + h / 2 - side) * math.sin(t * math.pi / 2) ** 0.8))
    for i in range(n - 1, -1, -1):
        t = i / n
        pts.append((cx - w / 2 * math.cos(t * math.pi / 2), side + (cy + h / 2 - side) * math.sin(t * math.pi / 2) ** 0.8))
    return pts


def scale_pts(pts, cx, cy, k):
    return [(cx + (x - cx) * k, cy + (y - cy) * k) for x, y in pts]


def gradient_fill(img, pts, mat, wood=False):
    """Remplit un polygone d'un dégradé métallique diagonal (clair en haut à gauche)."""
    m = Image.new("L", (W, W), 0)
    ImageDraw.Draw(m).polygon(pts, fill=255)
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    y, x = np.mgrid[0:W, 0:W].astype(np.float32)
    t = np.clip(((x - x0) / (x1 - x0) * 0.35 + (y - y0) / (y1 - y0) * 0.65), 0, 1)
    light, mid, dark = mat
    col = np.where((t < 0.45)[..., None], lerp(light, mid, t / 0.45), lerp(mid, dark, (t - 0.45) / 0.55))
    # reflet horizontal façon métal brossé
    col += (np.exp(-((t - 0.32) / 0.06) ** 2) * 70)[..., None]
    if wood:
        grain = np.sin((x * 0.13 + np.sin(y * 0.03) * 2.5 + np.sin(y * 0.009 + x * 0.02) * 5.0)) * 0.5 + 0.5
        col *= (0.62 + 0.38 * grain ** 2)[..., None]
    col = np.clip(col, 0, 255)
    layer = Image.fromarray(col.astype(np.uint8), "RGB").convert("RGBA")
    img.paste(layer, (0, 0), m)


def outline_poly(draw, pts, color, width):
    draw.line(pts + [pts[0]], fill=color, width=width, joint="curve")


def draw_ball(img, cx, cy, r):
    layer = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(250, 250, 250, 255))
    dark = (24, 24, 28, 255)
    pr = r * 0.34

    def pent(px, py, rr, rot):
        return [(px + rr * math.cos(math.radians(rot + k * 72)), py + rr * math.sin(math.radians(rot + k * 72))) for k in range(5)]

    centre = pent(cx, cy, pr, -90)
    for k in range(5):
        a = math.radians(-90 + k * 72)
        vx, vy = centre[k]
        ox, oy = cx + math.cos(a) * r * 1.02, cy + math.sin(a) * r * 1.02
        d.line([(vx, vy), (ox, oy)], fill=dark, width=max(2, int(r * 0.06)))
        d.polygon(pent(cx + math.cos(a) * r * 0.93, cy + math.sin(a) * r * 0.93, pr, -90 + k * 72 + 180), fill=dark)
    d.polygon(centre, fill=dark)
    # ombrage sphérique
    sh = np.clip((DX - (cx - C) + r * 0.35) * 0.6 + (DY - (cy - C) + r * 0.35) * 0.8, 0, None) / (r * 2.2)
    a = np.array(layer).astype(np.float32)
    a[..., :3] *= (1 - np.clip(sh, 0, 0.35))[..., None]
    layer = Image.fromarray(a.astype(np.uint8), "RGBA")
    clip = Image.new("L", (W, W), 0)
    ImageDraw.Draw(clip).ellipse((cx - r, cy - r, cx + r, cy + r), fill=255)
    img.paste(layer, (0, 0), clip)
    d = ImageDraw.Draw(img)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=(20, 20, 24, 255), width=int(r * 0.07))
    # petit reflet (calque fondu, ImageDraw remplacerait l'alpha)
    glint = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    ImageDraw.Draw(glint).ellipse((cx - r * 0.62, cy - r * 0.72, cx - r * 0.2, cy - r * 0.42), fill=(255, 255, 255, 140))
    img.alpha_composite(glint.filter(ImageFilter.GaussianBlur(r * 0.04)))


def draw_gem(img, cx, cy, w, mat):
    light, mid, dark = mat
    h_top, h_bot = w * 0.30, w * 0.62
    tl, tr = (cx - w * 0.28, cy - h_top), (cx + w * 0.28, cy - h_top)
    gl, gr = (cx - w / 2, cy), (cx + w / 2, cy)
    bot = (cx, cy + h_bot)
    d = ImageDraw.Draw(img)
    # couronne du diamant : facettes
    d.polygon([gl, tl, (cx - w * 0.14, cy)], fill=mid)
    d.polygon([tl, tr, (cx + w * 0.14, cy), (cx - w * 0.14, cy)], fill=light)
    d.polygon([tr, gr, (cx + w * 0.14, cy)], fill=mid)
    # pavillon
    d.polygon([gl, (cx - w * 0.14, cy), bot], fill=mid)
    d.polygon([(cx - w * 0.14, cy), (cx + w * 0.14, cy), bot], fill=light)
    d.polygon([(cx + w * 0.14, cy), gr, bot], fill=dark)
    lw = max(2, int(w * 0.025))
    fx = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(fx)
    for a, b in [(gl, gr), (tl, (cx - w * 0.14, cy)), (tr, (cx + w * 0.14, cy)), ((cx - w * 0.14, cy), bot), ((cx + w * 0.14, cy), bot)]:
        d.line([a, b], fill=(255, 255, 255, 170), width=lw)
    img.alpha_composite(fx)
    d = ImageDraw.Draw(img)
    outline_poly(d, [gl, tl, tr, gr, bot], (10, 20, 60, 255), int(w * 0.045))
    for sx, sy, sr in [(cx - w * 0.18, cy - h_top * 0.55, w * 0.07), (cx + w * 0.30, cy + h_bot * 0.12, w * 0.045)]:
        d.polygon(star_points(sx, sy, sr, sr * 0.25, -90), fill=(255, 255, 255, 255))


def draw_laurels(img, cx, cy, r, col):
    light, mid, dark = col
    d = ImageDraw.Draw(img)
    for side in (-1, 1):
        for i in range(6):
            a = math.radians(90 + side * (34 + i * 22))         # du bas vers le haut
            bx, by = cx + r * math.cos(a), cy + r * math.sin(a)
            # la feuille suit la tangente, pointe vers le haut
            tang = a - side * math.pi / 2
            ln, wd = r * 0.42, r * 0.19
            ux, uy = math.cos(tang), math.sin(tang)
            px, py = -uy, ux
            off = side * 0.45 if i % 2 == 0 else -side * 0.45
            leaf = []
            for k in range(13):
                t = k / 12
                leaf.append((bx + ux * ln * t + px * wd * math.sin(math.pi * t) * (1 + off) * 0.5,
                             by + uy * ln * t + py * wd * math.sin(math.pi * t) * (1 + off) * 0.5))
            for k in range(12, -1, -1):
                t = k / 12
                leaf.append((bx + ux * ln * t - px * wd * math.sin(math.pi * t) * (1 - off) * 0.5,
                             by + uy * ln * t - py * wd * math.sin(math.pi * t) * (1 - off) * 0.5))
            d.polygon(leaf, fill=mid if i % 2 else light)
            outline_poly(d, leaf, dark, max(2, int(r * 0.022)))


def draw_crown(img, cx, base_y, w):
    h = w * 0.55
    pts = [(cx - w / 2, base_y), (cx - w / 2 - w * 0.04, base_y - h * 0.85), (cx - w * 0.25, base_y - h * 0.45),
           (cx, base_y - h), (cx + w * 0.25, base_y - h * 0.45), (cx + w / 2 + w * 0.04, base_y - h * 0.85), (cx + w / 2, base_y)]
    gradient_fill(img, pts, GOLD)
    d = ImageDraw.Draw(img)
    outline_poly(d, pts, (90, 50, 0, 255), int(w * 0.05))
    d.rectangle((cx - w / 2, base_y - h * 0.08, cx + w / 2, base_y + h * 0.12), fill=GOLD[1], outline=(90, 50, 0, 255), width=int(w * 0.04))
    for px, py in [pts[1], pts[3], pts[5]]:
        rr = w * 0.07
        d.ellipse((px - rr, py - rr, px + rr, py + rr), fill=GOLD[0], outline=(90, 50, 0, 255), width=int(w * 0.03))
    for k, gc in [(-0.25, (60, 200, 255)), (0, (255, 40, 90)), (0.25, (60, 200, 255))]:
        gx, gy, rr = cx + k * w, base_y + h * 0.02, w * 0.06
        d.ellipse((gx - rr, gy - rr, gx + rr, gy + rr), fill=gc, outline=(60, 20, 0, 255), width=int(w * 0.02))


def text_on_arc(img, text, radius, size, centre_deg, bottom=False):
    font = ImageFont.truetype(FONT, size)
    stroke = max(3, size // 9)
    widths = [font.getlength(ch) for ch in text]
    track = size * 0.06
    total = sum(widths) + track * (len(text) - 1)
    ang = total / radius                                  # angle total en radians
    pos = -ang / 2
    for ch, wch in zip(text, widths):
        mid = pos + wch / 2 / radius
        a = math.radians(centre_deg) + (-mid if bottom else mid)
        box = int(size * 2)
        tile = Image.new("RGBA", (box, box), (0, 0, 0, 0))
        ImageDraw.Draw(tile).text((box / 2, box / 2), ch, font=font, fill=(255, 255, 255, 255), anchor="mm",
                                  stroke_width=stroke, stroke_fill=(20, 16, 12, 255))
        rot = -math.degrees(a) - 90 if not bottom else -math.degrees(a) + 90
        tile = tile.rotate(rot, resample=Image.BICUBIC)
        x, y = C + radius * math.cos(a), C + radius * math.sin(a)
        img.alpha_composite(tile, (int(x - box / 2), int(y - box / 2)))
        pos += (wch + track) / radius


def stars_on_arc(img, n, radius, size, col):
    d = ImageDraw.Draw(img)
    step = 14
    for i in range(n):
        deg = 90 + (i - (n - 1) / 2) * step
        a = math.radians(deg)
        x, y = C + radius * math.cos(a), C + radius * math.sin(a)
        pts = star_points(x, y, size, size * 0.45, -90)
        d.polygon(pts, fill=col[0])
        outline_poly(d, pts, (20, 16, 12, 255), max(3, int(size * 0.18)))


def make(index, name, text, mat, disc, emblem, deco):
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))

    # ombre portée en bas à droite
    sh = Image.new("L", (W, W), 0)
    o = 2.2 * S
    ImageDraw.Draw(sh).ellipse((C - R_OUT + o, C - R_OUT + o * 1.3, C + R_OUT + o, C + R_OUT + o * 1.3), fill=200)
    sh = sh.filter(ImageFilter.GaussianBlur(2.2 * S))
    img.paste(Image.new("RGBA", (W, W), (0, 0, 0, 255)), (0, 0), sh)

    # anneau métal, filet coloré, disque, bande du nom
    paste_array(img, bevel_ring(R_METAL, R_OUT, (225, 226, 232)), R <= R_OUT)
    paste_array(img, bevel_ring(R_DISC, R_METAL, mat[1], 0.6), R <= R_METAL)
    t = np.clip(R / R_DISC, 0, 1) ** 1.6
    col = lerp(disc[0], disc[1], t)
    if index == 0:      # veinage du bois
        grain = np.sin(DX * 0.045 + np.sin(DY * 0.018) * 2.5 + np.sin(DY * 0.006 + DX * 0.008) * 5.0) * 0.5 + 0.5
        col *= (0.68 + 0.32 * grain ** 2)[..., None]
    paste_array(img, col, R <= R_DISC)
    band = lerp(disc[1], np.array(disc[1]) * 0.55, np.clip((R - R_BAND) / (R_DISC - R_BAND), 0, 1))
    paste_array(img, band, (R <= R_DISC) & (R >= R_BAND))
    d = ImageDraw.Draw(img)
    for rr, colr, wd in [(R_OUT, (40, 40, 44, 255), 4), (R_METAL, (30, 30, 34, 255), 4), (R_DISC, (20, 16, 12, 255), 4),
                         (R_BAND, mat[0] + (255,), 5)]:
        d.ellipse((C - rr, C - rr, C + rr, C + rr), outline=colr, width=wd)

    # emblème
    if "laurels" in deco:
        draw_laurels(img, C, C + 3 * S, 31 * S, GOLD if index != 4 else mat)
    crown = "crown" in deco
    sw, shh = (38 * S, 42 * S) if crown else (44 * S, 50 * S)
    scy = C + (6 * S if crown else 1 * S)
    shield = shield_points(C, scy, sw, shh)
    gradient_fill(img, shield, ((40, 30, 20), (20, 14, 10), (10, 8, 6)))
    inner = scale_pts(shield, C, scy, 0.86)
    gradient_fill(img, scale_pts(shield, C, scy, 0.95), mat, wood=(index == 0))
    gradient_fill(img, inner, (mat[1], mat[2], mat[2]), wood=(index == 0))
    d = ImageDraw.Draw(img)
    outline_poly(d, shield, (16, 12, 8, 255), 6)
    outline_poly(d, inner, mat[0] + (255,), 3)
    if emblem == "gem":
        draw_gem(img, C, scy - 7 * S, 30 * S, ((235, 248, 255), (140, 200, 255), (50, 100, 220)))
    else:
        draw_ball(img, C, scy - 2 * S, (12 if crown else 14.5) * S)
    if "crown" in deco:
        draw_crown(img, C, scy - shh / 2 + 1 * S, 24 * S)

    # nom en haut, étoiles de rang en bas
    size = int((12.5 if len(text) <= 7 else 10.5) * S)
    text_on_arc(img, text, (R_DISC + R_BAND) / 2, size, -90)
    stars_on_arc(img, index + 1, (R_DISC + R_BAND) / 2 + 0.3 * S, 4.8 * S, GOLD if index != 2 else mat)

    # reflet vitré sur la moitié haute
    gl = np.clip(1 - ((DX / (R_DISC * 0.95)) ** 2 + ((DY + R_DISC * 0.45) / (R_DISC * 0.62)) ** 2), 0, 1)
    gl = (gl ** 0.6) * 60 * (DY < R_DISC * 0.1)
    glo = Image.new("RGBA", (W, W), (255, 255, 255, 0))
    glo.putalpha(Image.fromarray(gl.astype(np.uint8), "L"))
    img.alpha_composite(glo)

    out = img.resize((OUT, OUT), Image.LANCZOS)
    path = os.path.join(OUT_DIR, "T_Rank_%s.png" % name)
    out.save(path)
    return out


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    tiles = [make(i, *r) for i, r in enumerate(RANKS)]
    # planche d'aperçu (pas utilisée par le jeu)
    sheet = Image.new("RGBA", (128 * len(tiles) + 8 * (len(tiles) + 1), 144), (70, 90, 60, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t.resize((128, 128), Image.LANCZOS), (8 + i * 136, 8))
    sheet.save(os.path.join(os.path.dirname(__file__), "rank_logos_preview.png"))


if __name__ == "__main__":
    main()
