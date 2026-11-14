# Génère les textures des équipes ajoutées (voir notes/new-teams.md) à partir des textures d'origine :
#   Panda (index 12) = ours (mis_*) : fourrure blanche, membres et tour des yeux noirs ;
#   Tigre (index 13) = panthère (pantera_*) : fourrure orange, taches effacées, rayures noires.
# Maillots de joueur (_01.._05), de gardien (_B01.._B05), public (_P), arbitre (_S), icônes de maillot.
# Usage : python mods/art/make_team_textures.py  ->  mods/data/Data/Textures/Tshirts, Menu/TShirtsIcons
import os

import numpy as np
from PIL import Image, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
SRC = os.path.join(ROOT, "game_pristine", "Data")
OUT = os.path.join(ROOT, "mods", "data", "Data")
SKINS = ["01", "02", "03", "04", "05", "B01", "B02", "B03", "B04", "B05", "P", "S"]
W = 512
Y, X = np.mgrid[0:W, 0:W]


def rgb_to_hsv(a):
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    mx, mn = a.max(-1), a.min(-1)
    d = mx - mn
    h = np.zeros_like(mx)
    nz = d > 1e-6
    rm = nz & (mx == r)
    gm = nz & (mx == g) & ~rm
    bm = nz & ~rm & ~gm
    h[rm] = ((g - b)[rm] / d[rm]) % 6
    h[gm] = (b - r)[gm] / d[gm] + 2
    h[bm] = (r - g)[bm] / d[bm] + 4
    h /= 6
    s = np.where(mx > 1e-6, d / np.maximum(mx, 1e-6), 0)
    return h, s, mx


def hsv_to_rgb(h, s, v):
    i = np.floor(h * 6).astype(int) % 6
    f = h * 6 - np.floor(h * 6)
    p, q, t = v * (1 - s), v * (1 - s * f), v * (1 - s * (1 - f))
    sel = [(v, t, p), (q, v, p), (p, v, t), (p, q, v), (t, p, v), (v, p, q)]
    out = np.zeros(h.shape + (3,))
    for k, (r, g, b) in enumerate(sel):
        m = i == k
        out[m] = np.stack([r[m], g[m], b[m]], -1)
    return out


def load(path):
    return np.asarray(Image.open(path).convert("RGB")).astype(np.float64) / 255


def save(a, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray((np.clip(a, 0, 1) * 255 + 0.5).astype(np.uint8), "RGB").save(path)


def noise(scale, seed):
    """Bruit lisse dans [0, 1] : grille aléatoire agrandie en bicubique."""
    rng = np.random.default_rng(seed)
    n = max(2, W // scale + 2)
    g = Image.fromarray((rng.random((n, n)) * 255).astype(np.uint8), "L")
    a = np.asarray(g.resize((W + 2 * scale, W + 2 * scale), Image.BICUBIC)).astype(np.float64) / 255
    return a[scale:scale + W, scale:scale + W]


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def enclosed_blobs(dark, around, max_area=900, min_ratio=0.7):
    """Composantes connexes de `dark` d'aire limitée dont le pourtour est surtout dans `around`."""
    seen = np.zeros(dark.shape, bool)
    out = np.zeros(dark.shape, bool)
    for y0, x0 in zip(*np.nonzero(dark)):
        if seen[y0, x0]:
            continue
        comp, edge, stack = [], set(), [(y0, x0)]
        seen[y0, x0] = True
        while stack:
            y, x = stack.pop()
            comp.append((y, x))
            for ny, nx in ((y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)):
                if not (0 <= ny < W and 0 <= nx < W):
                    edge.add((-1, -1))
                elif dark[ny, nx]:
                    if not seen[ny, nx]:
                        seen[ny, nx] = True
                        stack.append((ny, nx))
                else:
                    edge.add((ny, nx))
        if len(comp) <= max_area and edge:
            ratio = sum(1 for p in edge if p[0] >= 0 and around[p]) / len(edge)
            if ratio >= min_ratio:
                ys, xs = zip(*comp)
                out[list(ys), list(xs)] = True
    return out


def blur_fill(a, known, radius):
    """Moyenne floue des seuls pixels connus (pour reboucher des taches)."""
    def blur(img):
        return np.asarray(img.filter(ImageFilter.GaussianBlur(radius))).astype(np.float64)
    w = blur(Image.fromarray((known * 255).astype(np.uint8), "L")) / 255
    out = np.zeros_like(a)
    for c in range(3):
        ch = Image.fromarray((a[..., c] * known * 255).astype(np.uint8), "L")
        out[..., c] = blur(ch) / 255 / np.maximum(w, 1e-3)
    return out, w


# --- Panda ---------------------------------------------------------------------------------
# Zones du dépliage UV de mis.3da (relevées en jeu avec une texture quadrillée) :
#   globes des yeux = coin haut-droit (bosses sur le dessus de la tête), tour des yeux = zone ombrée
#   vers (343, 67), oreilles = pièce ronde trouée posée sur le maillot (325-390, 245-305) et sa base
#   (250-305, 210-268), nez = boule du coin bas-droit, tête = haut-centre,
#   bras et mains = colonne de gauche, jambes et pieds = bande du bas.
EYES = (X >= 400) & (Y < 85)
NOSE = (X >= 420) & (Y >= 420)
LIMBS = (X < 120) | (Y >= 400)
EAR_BOXES = ((X >= 325) & (X < 390) & (Y >= 245) & (Y < 305)) | (((X - 279) ** 2 + (Y - 242) ** 2 < 24 ** 2) & (Y >= 222))
# taches autour des yeux : ellipse légèrement inclinée, bord adouci (le dépliage est symétrique : une tache pour deux yeux)
_ex, _ey = (X - 343.0), (Y - 67.0)
_c, _s = np.cos(np.radians(20)), np.sin(np.radians(20))
_d = np.sqrt(((_ex * _c + _ey * _s) / 54.0) ** 2 + ((-_ex * _s + _ey * _c) / 50.0) ** 2)
EYE_PATCH = np.clip((1.0 - _d) / 0.12, 0, 1)


def panda_fur(a):
    h, s, v = rgb_to_hsv(a)
    blue = (h > 0.42) & (h < 0.70) & (s > 0.06)
    whitish = (s < 0.15) & (v > 0.55)
    return blue | (whitish & ~EYES & ~NOSE)


def panda_ears():
    """Forme exacte des oreilles, prise sur mis_01 où le maillot rouge les entoure (même dépliage partout)."""
    return EAR_BOXES & panda_fur(load(os.path.join(SRC, "Textures", "Tshirts", "mis_01.png")))


def panda(a, ears):
    fur = panda_fur(a)
    # la luminance garde le modelé de la fourrure (la valeur HSV du bleu clair est presque constante)
    lum = a[..., 0] * 0.30 + a[..., 1] * 0.59 + a[..., 2] * 0.11
    shade = np.clip((lum - 0.10) / 0.80, 0, 1)
    white = np.array([0.98, 0.97, 0.93])[None, None] * (0.30 + 0.70 * shade)[..., None]
    black = np.array([0.10, 0.10, 0.12])[None, None] * (0.45 + 0.9 * shade)[..., None]
    dark = np.maximum((LIMBS | EYES | ears).astype(np.float64), EYE_PATCH)[..., None]
    out = np.where(fur[..., None], white * (1 - dark) + black * dark, a)
    return out


# --- Tigre ---------------------------------------------------------------------------------
STRIPE_N1 = noise(48, 1)
STRIPE_N2 = noise(96, 2)
STRIPE_N3 = noise(24, 3)


def tiger(a, kit_to_yellow=False):
    h, s, v = rgb_to_hsv(a)
    warm = (h >= 0.0) & (h <= 0.19)
    fur = warm & (s > 0.45) & (v > 0.45)
    spot = warm & (s > 0.3) & (v > 0.08) & (v <= 0.62) & ~fur
    cream = warm & (s <= 0.45) & (s > 0.08) & (v > 0.6)

    # taches de la panthère (et leur contour noir) rebouchées avec la fourrure voisine
    blobs = enclosed_blobs(~fur & ~cream, fur)
    known = (fur & ~blobs).astype(np.float64)
    fill, w = blur_fill(a, known, 7)
    for r in (14, 28):      # grandes taches : rayon plus large là où le premier passage manque de voisins
        f2, w2 = blur_fill(a, known, r)
        fill = np.where((w < 0.15)[..., None], f2, fill)
        w = np.maximum(w, w2)
    spot = (spot & (w > 0.05)) | blobs
    a = np.where(spot[..., None], fill, a)
    fur |= spot
    h, s, v = rgb_to_hsv(a)

    # fourrure : orange vif
    nh = np.clip(0.055 + (h - 0.11) * 0.35, 0.02, 0.09)
    ns = np.clip(s * 1.05 + 0.08, 0, 1)
    orange = hsv_to_rgb(nh, ns, np.clip(v * 0.97, 0, 1))
    # parties claires : presque blanches
    light = hsv_to_rgb(h, s * 0.25, np.clip(v * 1.03, 0, 1))
    out = np.where(fur[..., None], orange, np.where(cream[..., None], light, a))

    # rayures : bandes ondulées, épaisseur et coupures variables
    phase = Y / 15.0 + STRIPE_N1 * 5.0 + X * 0.018 + np.sin(X / 37.0) * 0.8
    band = np.sin(phase)
    thr = 0.50 + 0.45 * STRIPE_N2 + 0.15 * (STRIPE_N3 - 0.5)
    stripe = smoothstep(thr - 0.06, thr + 0.06, band) * fur
    ink = np.array([0.08, 0.04, 0.02])[None, None] * (0.6 + 0.6 * v)[..., None]
    out = out * (1 - stripe[..., None]) + ink * stripe[..., None]

    if kit_to_yellow:
        kit = (h > 0.55) & (h < 0.75) & (s > 0.3)
        vk = v / max(np.percentile(v[kit], 95), 1e-3) if kit.any() else v
        yellow = hsv_to_rgb(np.full_like(h, 0.14), np.full_like(h, 0.92), np.clip(0.25 + 0.78 * vk, 0, 1))
        out = np.where(kit[..., None], yellow, out)
    return out


def recolor_icon(src, dst, hue):
    im = Image.open(src).convert("RGBA")
    a = np.asarray(im).astype(np.float64) / 255
    h, s, v = rgb_to_hsv(a[..., :3])
    rgb = hsv_to_rgb(np.where(s > 0.3, hue, h), s, v)
    out = np.dstack([rgb, a[..., 3:]])
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    Image.fromarray((np.clip(out, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA").save(dst)


def main():
    tsh = os.path.join(SRC, "Textures", "Tshirts")
    out = os.path.join(OUT, "Textures", "Tshirts")
    ears = panda_ears()
    for k in SKINS:
        save(panda(load(os.path.join(tsh, "mis_%s.png" % k)), ears), os.path.join(out, "panda_%s.png" % k))
        save(tiger(load(os.path.join(tsh, "pantera_%s.png" % k)), kit_to_yellow=(k == "01")),
             os.path.join(out, "tygrys_%s.png" % k))
    icons = os.path.join(SRC, "Menu", "TShirtsIcons")
    iout = os.path.join(OUT, "Menu", "TShirtsIcons")
    os.makedirs(iout, exist_ok=True)
    for n in ["01", "02", "03", "04", "05"]:
        Image.open(os.path.join(icons, "misie%s.png" % n)).save(os.path.join(iout, "pandy%s.png" % n))
        if n == "01":     # maillot 01 du tigre passé du bleu au jaune
            recolor_icon(os.path.join(icons, "pantery01.png"), os.path.join(iout, "tygrysy01.png"), 0.135)
        else:
            Image.open(os.path.join(icons, "pantery%s.png" % n)).save(os.path.join(iout, "tygrysy%s.png" % n))


if __name__ == "__main__":
    main()
