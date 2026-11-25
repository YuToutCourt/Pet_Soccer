"""Logo du site (web/public/img/logo-petsoccer.png) : le logo.png du jeu (512x256) sans son fond bleu.

    python mods\\art\\make_site_logo.py

Le fond uni est retiré façon « couleur vers alpha » (GIMP) : chaque pixel prend l'alpha minimal qui permet de le
retrouver en le posant sur le bleu, ce qui garde les bords lissés propres sur n'importe quel fond. Le mot « Online »
est ajouté par le site lui-même (composant LogoOnline), en texte, pour rester net à toutes les tailles.
"""
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(ROOT, "game_pristine", "Data", "Menu", "logo.png")
DST = os.path.join(ROOT, "web", "public", "img", "logo-petsoccer.png")


def color_to_alpha(px, bg):
    alpha = 0.0
    for c, b in zip(px, bg):
        if c > b:
            a = (c - b) / (255 - b) if b < 255 else 0
        elif c < b:
            a = (b - c) / b if b > 0 else 0
        else:
            a = 0
        alpha = max(alpha, a)
    if alpha <= 0.004:
        return 0, 0, 0, 0
    rgb = tuple(max(0, min(255, round((c - b) / alpha + b))) for c, b in zip(px, bg))
    return rgb + (round(alpha * 255),)


def main():
    im = Image.open(SRC).convert("RGBA")
    bg = im.getpixel((2, 2))[:3]
    out = Image.new("RGBA", im.size)
    src, dst = im.load(), out.load()
    for y in range(im.height):
        for x in range(im.width):
            dst[x, y] = color_to_alpha(src[x, y][:3], bg)
    out = out.crop(out.getbbox())
    out.save(DST, optimize=True)
    print("%s (%dx%d)" % (DST, out.width, out.height))


if __name__ == "__main__":
    main()
