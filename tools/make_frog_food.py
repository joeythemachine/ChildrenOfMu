"""
Textures and models for the Chitinier's dishes (items) and the strung fish / strung bugs drying-line blocks.
    python tools/make_frog_food.py
"""
import json
import os
import random
import zlib

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
A = os.path.join(ROOT, "assets", "childrenofmu")
D = os.path.join(ROOT, "data", "childrenofmu")
NS = "childrenofmu"


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def canvas():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def px(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), c + (255,) if len(c) == 3 else c)


def plate(im):
    """A small white plate with a grey rim, seen from the side and a little above."""
    for x in range(2, 14):
        for y in (12, 13):
            px(im, x, y, (236, 234, 228))
    for x in range(3, 13):
        px(im, x, 14, (180, 178, 172))
    px(im, 1, 12, (200, 198, 192))
    px(im, 14, 12, (200, 198, 192))


def oval(im, cx, cy, rx, ry, c, shade=None):
    for y in range(16):
        for x in range(16):
            if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                cc = c
                if shade and y > cy:
                    cc = shade
                px(im, x, y, cc)


def honey_glazed_beetle():
    im = canvas(); plate(im)
    oval(im, 8, 9, 4.2, 3.0, (120, 60, 20), (80, 40, 14))
    for x in range(5, 12):
        px(im, x, 8 - (x % 2), (236, 176, 50))          # honey drizzle
    px(im, 8, 6, (40, 24, 12)); px(im, 8, 9, (40, 24, 12))
    for x, y in ((4, 11), (12, 11), (5, 12), (11, 12)):
        px(im, x, y, (40, 26, 14))                       # legs
    px(im, 6, 7, (255, 240, 180))                        # glint
    return im


def cricket_crumble():
    im = canvas(); plate(im)
    r = random.Random(3)
    for y in range(7, 12):
        for x in range(4 + (11 - y) // 2, 12 - (11 - y) // 2):
            px(im, x, y, r.choice([(170, 120, 60), (150, 100, 50), (190, 140, 76)]))
    for x, y in ((6, 6), (7, 5), (10, 6), (11, 5), (9, 4)):
        px(im, x, y, (90, 140, 50))                      # cricket legs poking out
    return im


def mosquito_consomme():
    im = canvas()
    for y in range(8, 14):                                # a wooden bowl
        for x in range(2 + (y - 8) // 2, 14 - (y - 8) // 2):
            px(im, x, y, (130, 86, 46) if y > 9 else (100, 66, 36))
    for x in range(3, 13):
        px(im, x, 8, (196, 130, 60))                      # amber broth
        px(im, x, 9, (176, 112, 50))
    for x, y in ((5, 8), (9, 8), (11, 9)):
        px(im, x, y, (50, 40, 36))                        # tiny mosquitoes
    px(im, 7, 7, (220, 220, 220)); px(im, 8, 6, (220, 220, 220))   # steam
    return im


def dragonfly_tartare():
    im = canvas(); plate(im)
    oval(im, 8, 10, 3.5, 2.2, (180, 40, 40), (140, 30, 30))
    for x, y in ((5, 6), (6, 7), (10, 6), (9, 7), (4, 5), (11, 5)):
        px(im, x, y, (150, 210, 230))                     # wings standing up
    px(im, 7, 8, (40, 120, 140)); px(im, 8, 8, (40, 120, 140)); px(im, 8, 7, (40, 120, 140))
    px(im, 10, 10, (240, 220, 80))                        # a quail egg yolk
    return im


def firefly_flambe():
    im = canvas(); plate(im)
    oval(im, 8, 10, 4, 2, (90, 60, 40))
    for x, y in ((6, 9), (9, 10), (11, 9), (7, 11)):
        px(im, x, y, (250, 240, 120))                     # glowing fireflies
    for x, y, c in ((7, 6, (250, 140, 40)), (8, 5, (250, 200, 60)), (9, 6, (250, 140, 40)), (8, 7, (240, 90, 30)),
                    (6, 7, (240, 90, 30)), (10, 7, (240, 90, 30)), (8, 4, (255, 240, 150))):
        px(im, x, y, c)                                   # the flame
    return im


def moth_souffle():
    im = canvas()
    for y in range(9, 15):                                 # white ramekin
        for x in range(4, 12):
            px(im, x, y, (240, 238, 232) if x not in (4, 11) else (200, 198, 192))
    for y in range(4, 9):
        for x in range(4 - (y < 6), 12 + (y < 6)):
            px(im, x, y, (226, 170, 80) if y < 7 else (240, 200, 110))   # puffed golden top
    px(im, 6, 3, (170, 150, 120)); px(im, 9, 3, (170, 150, 120)); px(im, 7, 4, (120, 100, 80))  # moth wing garnish
    return im


def bog_snail_escargot():
    im = canvas(); plate(im)
    oval(im, 8, 9, 3.5, 3.2, (140, 96, 56), (110, 74, 42))
    for x, y in ((8, 9), (9, 8), (8, 7), (7, 8), (7, 10), (9, 10), (10, 9)):
        px(im, x, y, (80, 52, 30))                         # the shell's spiral
    for x in range(3, 6):
        px(im, x, 11, (170, 150, 110))                     # the snail's foot
    px(im, 11, 11, (70, 150, 50)); px(im, 12, 11, (90, 170, 60))   # herb sprig
    return im


def strung(kind):
    """A drying line: a twine across the top and fish or bugs hanging off it (for a cross model)."""
    im = canvas()
    for x in range(16):
        px(im, x, 0, (150, 120, 80))
    r = random.Random(zlib.crc32(kind.encode()))
    for i, x in enumerate((2, 7, 12)):
        for y in range(1, 3):
            px(im, x, y, (150, 120, 80))
        if kind == "fish":
            body = r.choice([(150, 170, 186), (176, 140, 120), (130, 150, 120)])
            for y in range(3, 12):
                w = 1 if y in (3, 11) else 2
                for dx in range(-w + 1, w):
                    px(im, x + dx, y, body if dx <= 0 else tuple(max(0, v - 30) for v in body))
            px(im, x, 4, (30, 30, 30))                         # eye (hung tail-up, head down? head here)
            for dx in (-1, 0, 1):
                px(im, x + dx, 12, tuple(max(0, v - 40) for v in body))   # tail fin
            px(im, x - 1, 13, tuple(max(0, v - 40) for v in body)); px(im, x + 1, 13, tuple(max(0, v - 40) for v in body))
        else:
            bug = ["beetle", "cricket", "dragonfly"][i]
            if bug == "beetle":
                oval(im, x, 5, 1.4, 2.2, (60, 90, 50))
                px(im, x, 3, (20, 20, 20))
            elif bug == "cricket":
                for y in range(3, 9):
                    px(im, x, y, (110, 150, 60))
                px(im, x - 1, 8, (110, 150, 60)); px(im, x + 1, 8, (110, 150, 60))
                px(im, x - 1, 9, (90, 120, 50)); px(im, x + 1, 9, (90, 120, 50))
            else:
                for y in range(3, 11):
                    px(im, x, y, (40, 110, 140))
                for dx in (-1, 1):
                    px(im, x + dx, 4, (170, 220, 236)); px(im, x + dx, 5, (170, 220, 236)); px(im, x + 2 * dx, 5, (170, 220, 236))
            if r.random() < 0.6:
                for y in range(10, 14):
                    px(im, x + 0, y, (150, 120, 80)) if kind != "fish" and y == 10 else None
    return im


def reed_mat():
    """A woven reed sleeping mat: an over-under weave in greens and tans, a darker binding round the edge."""
    im = canvas()
    for y in range(16):
        for x in range(16):
            over = ((x // 2) + (y // 2)) % 2 == 0
            c = (150, 146, 82) if over else (118, 128, 62)
            if (x % 2 == 0) == over:
                c = tuple(v - 12 for v in c)
            if x in (0, 15) or y in (0, 15):
                c = (96, 80, 46)
            px(im, x, y, c)
    return im


def pink_slime():
    """A wobbling ball of pink slime, glossy, with something faintly glowing inside."""
    im = canvas()
    oval(im, 8, 9, 5.5, 5.0, (236, 110, 170), (200, 80, 140))
    for x, y in ((5, 6), (6, 5), (7, 5)):
        px(im, x, y, (255, 200, 230))                       # gloss
    px(im, 9, 10, (255, 240, 200)); px(im, 10, 9, (255, 220, 160)); px(im, 10, 10, (250, 170, 210))   # a glowing heart
    px(im, 3, 13, (200, 80, 140)); px(im, 12, 14, (200, 80, 140))                                     # drips
    return im


DISHES = {"honey_glazed_beetle": honey_glazed_beetle, "cricket_crumble": cricket_crumble,
          "mosquito_consomme": mosquito_consomme, "dragonfly_tartare": dragonfly_tartare,
          "firefly_flambe": firefly_flambe, "moth_souffle": moth_souffle, "bog_snail_escargot": bog_snail_escargot}


def main():
    for name, fn in DISHES.items():
        path = os.path.join(A, "textures", "item", name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        fn().save(path)
        write_json(os.path.join(A, "models", "item", name + ".json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
    for kind in ("fish", "bugs"):
        name = "strung_" + kind
        strung(kind).save(os.path.join(A, "textures", "block", name + ".png"))
        write_json(os.path.join(A, "models", "block", name + ".json"),
                   {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                    "textures": {"cross": f"{NS}:block/{name}"}})
        write_json(os.path.join(A, "blockstates", name + ".json"),
                   {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        write_json(os.path.join(A, "models", "item", name + ".json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/{name}"}})
        write_json(os.path.join(D, "loot_table", "blocks", name + ".json"), {"type": "minecraft:block", "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
             "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    reed_mat().save(os.path.join(A, "textures", "block", "reed_mat.png"))
    write_json(os.path.join(A, "models", "block", "reed_mat.json"),
               {"parent": "minecraft:block/carpet", "textures": {"wool": f"{NS}:block/reed_mat"}})
    write_json(os.path.join(A, "blockstates", "reed_mat.json"), {"variants": {"": {"model": f"{NS}:block/reed_mat"}}})
    write_json(os.path.join(A, "models", "item", "reed_mat.json"), {"parent": f"{NS}:block/reed_mat"})
    write_json(os.path.join(D, "loot_table", "blocks", "reed_mat.json"), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:reed_mat"}],
         "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    pink_slime().save(os.path.join(A, "textures", "item", "pink_slime.png"))
    write_json(os.path.join(A, "models", "item", "pink_slime.json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/pink_slime"}})
    print("dishes and drying lines written")


if __name__ == "__main__":
    main()
