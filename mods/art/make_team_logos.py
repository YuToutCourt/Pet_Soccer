# Génère les logos des équipes ajoutées (128x128 RGBA, style data/menu/logos/T_Logo_*.png) :
#   T_Logo_Pandrid.png (Real Pandrid) et T_Logo_Tigermund.png (Borussia Tigermund).
# Réutilise l'anneau métal, le texte en arc et la couronne de make_rank_logos.py.
# Usage : python mods/art/make_team_logos.py  ->  mods/data/Data/Menu/logos/
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

from make_rank_logos import (C, DX, DY, GOLD, OUT_DIR, R, R_BAND, R_DISC, R_METAL, R_OUT, S, W, bevel_ring,
                             draw_crown, lerp, paste_array, text_on_arc)

INK = (20, 16, 12, 255)


def ellipse(d, cx, cy, rx, ry, **kw):
    d.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), **kw)


def rotated_ellipse(img, cx, cy, rx, ry, deg, fill):
    box = int(max(rx, ry) * 2 + 8)
    tile = Image.new("RGBA", (box, box), (0, 0, 0, 0))
    ellipse(ImageDraw.Draw(tile), box / 2, box / 2, rx, ry, fill=fill)
    tile = tile.rotate(deg, resample=Image.BICUBIC)
    img.alpha_composite(tile, (int(cx - box / 2), int(cy - box / 2)))


def badge(disc, band, rim, text, size):
    """Fond commun : ombre, anneau métal, filet, disque, bande du nom."""
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    sh = Image.new("L", (W, W), 0)
    o = 2.2 * S
    ImageDraw.Draw(sh).ellipse((C - R_OUT + o, C - R_OUT + o * 1.3, C + R_OUT + o, C + R_OUT + o * 1.3), fill=200)
    img.paste(Image.new("RGBA", (W, W), (0, 0, 0, 255)), (0, 0), sh.filter(ImageFilter.GaussianBlur(2.2 * S)))
    paste_array(img, bevel_ring(R_METAL, R_OUT, (225, 226, 232)), R <= R_OUT)
    paste_array(img, bevel_ring(R_DISC, R_METAL, rim, 0.6), R <= R_METAL)
    paste_array(img, lerp(disc[0], disc[1], np.clip(R / R_DISC, 0, 1) ** 1.6), R <= R_DISC)
    paste_array(img, lerp(band[0], band[1], np.clip((R - R_BAND) / (R_DISC - R_BAND), 0, 1)), (R <= R_DISC) & (R >= R_BAND))
    d = ImageDraw.Draw(img)
    for rr, col, wd in [(R_OUT, (40, 40, 44, 255), 4), (R_METAL, (30, 30, 34, 255), 4), (R_DISC, INK, 4), (R_BAND, INK, 5)]:
        ellipse(d, C, C, rr, rr, outline=col, width=wd)
    text_on_arc(img, text, (R_DISC + R_BAND) / 2, size, -90)
    return img


def finish(img, name):
    gl = np.clip(1 - ((DX / (R_DISC * 0.95)) ** 2 + ((DY + R_DISC * 0.45) / (R_DISC * 0.62)) ** 2), 0, 1)
    gl = (gl ** 0.6) * 60 * (DY < R_DISC * 0.1)
    glo = Image.new("RGBA", (W, W), (255, 255, 255, 0))
    glo.putalpha(Image.fromarray(gl.astype(np.uint8), "L"))
    img.alpha_composite(glo)
    out = img.resize((128, 128), Image.LANCZOS)
    out.save(os.path.join(OUT_DIR, "T_Logo_%s.png" % name))
    return out


def panda_face(img, cx, cy, k):
    d = ImageDraw.Draw(img)
    black, white = (24, 24, 28, 255), (250, 250, 246, 255)
    for sx in (-1, 1):                                       # oreilles
        ellipse(d, cx + sx * 66 * k, cy - 58 * k, 30 * k, 30 * k, fill=black, outline=INK, width=int(5 * k))
    ellipse(d, cx, cy, 86 * k, 76 * k, fill=white, outline=INK, width=int(6 * k))
    for sx in (-1, 1):                                       # taches des yeux, yeux
        rotated_ellipse(img, cx + sx * 34 * k, cy - 6 * k, 21 * k, 30 * k, sx * 35, black)
        d = ImageDraw.Draw(img)
        ellipse(d, cx + sx * 32 * k, cy - 10 * k, 9 * k, 9 * k, fill=white)
        ellipse(d, cx + sx * 30 * k, cy - 9 * k, 5 * k, 5 * k, fill=black)
    d.polygon([(cx - 15 * k, cy + 20 * k), (cx + 15 * k, cy + 20 * k), (cx, cy + 34 * k)], fill=black)
    d.line([(cx, cy + 34 * k), (cx, cy + 44 * k)], fill=black, width=int(4 * k))
    d.arc((cx - 22 * k, cy + 28 * k, cx, cy + 52 * k), 20, 160, fill=black, width=int(4 * k))
    d.arc((cx, cy + 28 * k, cx + 22 * k, cy + 52 * k), 20, 160, fill=black, width=int(4 * k))


def tiger_face(img, cx, cy, k):
    d = ImageDraw.Draw(img)
    orange, white, black = (246, 128, 20, 255), (252, 248, 236, 255), (22, 14, 8, 255)
    for sx in (-1, 1):                                       # oreilles
        ellipse(d, cx + sx * 64 * k, cy - 60 * k, 26 * k, 26 * k, fill=orange, outline=INK, width=int(5 * k))
        ellipse(d, cx + sx * 64 * k, cy - 58 * k, 13 * k, 13 * k, fill=white)
    ellipse(d, cx, cy, 86 * k, 78 * k, fill=orange, outline=INK, width=int(6 * k))
    for sx in (-1, 1):                                       # joues
        ellipse(d, cx + sx * 32 * k, cy + 34 * k, 38 * k, 28 * k, fill=white)
    # rayures du front et des joues
    for sx in (-1, 0, 1):
        x0 = cx + sx * 22 * k
        d.polygon([(x0 - 8 * k, cy - 74 * k), (x0 + 8 * k, cy - 74 * k), (x0, cy - (40 if sx == 0 else 48) * k)], fill=black)
    for sx in (-1, 1):
        for i, yy in enumerate((-10, 8)):
            xe = cx + sx * 86 * k
            d.polygon([(xe, cy + (yy - 8) * k), (xe, cy + (yy + 8) * k), (xe - sx * (34 - i * 6) * k, cy + yy * k)], fill=black)
    for sx in (-1, 1):                                       # yeux
        ellipse(d, cx + sx * 30 * k, cy - 12 * k, 14 * k, 11 * k, fill=(196, 230, 60, 255), outline=black, width=int(3 * k))
        ellipse(d, cx + sx * 30 * k, cy - 12 * k, 3 * k, 9 * k, fill=black)
    d.polygon([(cx - 14 * k, cy + 14 * k), (cx + 14 * k, cy + 14 * k), (cx, cy + 28 * k)], fill=(232, 110, 120, 255), outline=black)
    d.line([(cx, cy + 28 * k), (cx, cy + 40 * k)], fill=black, width=int(4 * k))
    d.arc((cx - 24 * k, cy + 22 * k, cx, cy + 50 * k), 20, 160, fill=black, width=int(4 * k))
    d.arc((cx, cy + 22 * k, cx + 24 * k, cy + 50 * k), 20, 160, fill=black, width=int(4 * k))


def pandrid():
    # blanc et or, bande violette (clin d'œil au club madrilène), couronne sur la tête
    img = badge(((255, 255, 255), (214, 214, 226)), ((92, 52, 160), (48, 22, 96)), GOLD[1], "REAL PANDRID", int(10 * S))
    # écharpe diagonale derrière la tête
    band = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    ImageDraw.Draw(band).polygon([(C - 150, C + 60), (C + 120, C - 150), (C + 150, C - 110), (C - 120, C + 100)], fill=(92, 52, 160, 255))
    clip = Image.new("L", (W, W), 0)
    ImageDraw.Draw(clip).ellipse((C - R_BAND + 3, C - R_BAND + 3, C + R_BAND - 3, C + R_BAND - 3), fill=255)
    img.paste(band, (0, 0), Image.fromarray(np.minimum(np.asarray(clip), np.asarray(band)[..., 3]), "L"))
    panda_face(img, C, C + 18, 1.05)
    draw_crown(img, C, C - 52, 64)
    return finish(img, "Pandrid")


def tigermund():
    # jaune et noir, à la façon du club de la Ruhr
    img = badge(((255, 232, 40), (236, 186, 0)), ((34, 30, 26), (8, 6, 4)), (236, 196, 0), "TIGERMUND", int(11 * S))
    d = ImageDraw.Draw(img)
    ellipse(d, C, C, R_BAND - 12, R_BAND - 12, outline=INK, width=6)
    tiger_face(img, C, C + 14, 1.05)
    return finish(img, "Tigermund")


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    tiles = [pandrid(), tigermund()]
    sheet = Image.new("RGBA", (128 * len(tiles) + 8 * (len(tiles) + 1), 144), (70, 90, 60, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, (8 + i * 136, 8))
    sheet.save(os.path.join(os.path.dirname(__file__), "team_logos_preview.png"))


if __name__ == "__main__":
    main()
