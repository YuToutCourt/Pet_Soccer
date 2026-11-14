# Génère la planche de l'écran Tactique (512x512 RGBA) : le terrain vu de dessus (notre but à gauche)
# et les pions (joueur, joueur sélectionné, gardien, ballon, joueur grisé).
# Les rectangles doivent rester alignés sur ceux de MenuTactics.java et make_tactics_scr.py.
# Usage : python mods/art/make_tactics_field.py  ->  mods/data/Data/Menu/tactics_field.png
import math
import os

from PIL import Image, ImageDraw, ImageFilter

S = 4                                  # suréchantillonnage
SIZE = 512
FIELD_W, FIELD_H = 512, 282            # rapport longueur / largeur du terrain du jeu (5802 / 3196)
TOKEN = 64                             # pions de 64x64, alignés à partir de TOKEN_Y
TOKEN_Y = 320
OUT = os.path.join(os.path.dirname(__file__), "..", "data", "Data", "Menu", "tactics_field.png")


def field():
    w, h = FIELD_W * S, FIELD_H * S
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    stripes = 12
    for i in range(stripes):
        c = (52, 128, 52, 255) if i % 2 == 0 else (60, 142, 60, 255)
        d.rectangle([i * w // stripes, 0, (i + 1) * w // stripes, h], fill=c)

    # notre moitié un peu plus claire
    half = Image.new("RGBA", (w // 2, h), (255, 255, 255, 18))
    img.alpha_composite(half, (0, 0))

    line = (240, 248, 240, 235)
    lw = 2 * S
    m = lw // 2 + S
    d.rectangle([m, m, w - m, h - m], outline=line, width=lw)
    d.line([w // 2, m, w // 2, h - m], fill=line, width=lw)
    r = int(0.075 * w)
    d.ellipse([w // 2 - r, h // 2 - r, w // 2 + r, h // 2 + r], outline=line, width=lw)
    d.ellipse([w // 2 - 2 * S, h // 2 - 2 * S, w // 2 + 2 * S, h // 2 + 2 * S], fill=line)

    for side in (0, 1):
        def x(f):
            return int(m + f * (w - 2 * m)) if side == 0 else int(w - m - f * (w - 2 * m))

        pb_d, pb_w = 0.13, 0.52        # surface de réparation
        ga_d, ga_w = 0.045, 0.26       # surface de but
        g_w = 0.12                     # but
        for depth, width in ((pb_d, pb_w), (ga_d, ga_w)):
            y0, y1 = int(h * (0.5 - width / 2)), int(h * (0.5 + width / 2))
            d.rectangle([min(x(0), x(depth)), y0, max(x(0), x(depth)), y1], outline=line, width=lw)
        y0, y1 = int(h * (0.5 - g_w / 2)), int(h * (0.5 + g_w / 2))
        gx0, gx1 = (0, m) if side == 0 else (w - m, w)
        d.rectangle([gx0, y0, gx1, y1], fill=(250, 250, 250, 255))
        pr = 2 * S
        px = x(0.09)
        d.ellipse([px - pr, h // 2 - pr, px + pr, h // 2 + pr], fill=line)

    # flèche discrète : sens d'attaque (vers la droite), en bas de notre moitié
    ay = int(h * 0.93)
    ax0, ax1 = int(w * 0.40), int(w * 0.60)
    arrow = (255, 255, 255, 110)
    d.line([ax0, ay, ax1, ay], fill=arrow, width=3 * S)
    d.polygon([(ax1 + 6 * S, ay), (ax1 - 4 * S, ay - 5 * S), (ax1 - 4 * S, ay + 5 * S)], fill=arrow)
    return img.resize((FIELD_W, FIELD_H), Image.LANCZOS)


def token(fill, rim, highlight=None, ball=False):
    w = TOKEN * S
    img = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    shadow = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse([6 * S, 9 * S, w - 4 * S, w - 1 * S], fill=(0, 0, 0, 120))
    img.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(3 * S)))
    d = ImageDraw.Draw(img)
    pad = 4 * S
    if highlight:
        d.ellipse([1 * S, 1 * S, w - 1 * S, w - 1 * S], fill=highlight)
    d.ellipse([pad, pad, w - pad, w - pad], fill=rim)
    rim_w = 5 * S
    d.ellipse([pad + rim_w, pad + rim_w, w - pad - rim_w, w - pad - rim_w], fill=fill)
    if ball:
        c = w // 2
        d.regular_polygon((c, c, 9 * S), 5, rotation=0, fill=(30, 30, 30, 255))
        for k in range(5):
            a = math.radians(90 + k * 72)
            ox, oy = c + int(math.cos(a) * 19 * S), c - int(math.sin(a) * 19 * S)
            d.regular_polygon((ox, oy, 6 * S), 5, rotation=k * 72 + 36, fill=(30, 30, 30, 255))
    # reflet
    gloss = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    ImageDraw.Draw(gloss).ellipse([pad + 10 * S, pad + 6 * S, w - pad - 10 * S, w // 2], fill=(255, 255, 255, 50))
    img.alpha_composite(gloss.filter(ImageFilter.GaussianBlur(2 * S)))
    return img.resize((TOKEN, TOKEN), Image.LANCZOS)


def main():
    sheet = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    sheet.alpha_composite(field(), (0, 0))
    tokens = [
        token((40, 90, 200, 255), (235, 240, 255, 255)),                                 # 0 joueur
        token((250, 196, 30, 255), (255, 255, 255, 255), highlight=(255, 230, 90, 200)),  # 1 sélectionné
        token((30, 150, 90, 255), (220, 255, 230, 255)),                                 # 2 gardien
        token((248, 248, 248, 255), (40, 40, 40, 255), ball=True),                       # 3 ballon
        token((110, 116, 130, 255), (190, 194, 204, 255)),                               # 4 grisé (jeu d'origine)
    ]
    for i, t in enumerate(tokens):
        sheet.alpha_composite(t, (i * TOKEN, TOKEN_Y))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sheet.save(OUT)
    print(os.path.abspath(OUT))


if __name__ == "__main__":
    main()
