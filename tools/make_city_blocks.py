"""
RCB City's own block and item: the stone Manhole Cover (a trapdoor that opens by hand) and the Great Gar's Writ of
Trade. Textures, models, blockstate, loot table and tag entries.

    python tools/make_city_blocks.py
"""
import json
import math
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
A = os.path.join(RES, "assets", "childrenofmu")
D = os.path.join(RES, "data")


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as f:
        json.dump(data, f, indent=2)


def manhole_texture():
    """A plain grey stone cover: smooth, with a darker edge and a faint seam."""
    rng = random.Random(77)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            n = rng.randint(-5, 5)
            c = (128 + n, 128 + n, 126 + n)
            if x in (0, 15) or y in (0, 15):
                c = (92 + n, 92 + n, 90 + n)
            elif x in (1, 14) or y in (1, 14):
                c = (142 + n, 142 + n, 140 + n)
            img.putpixel((x, y), c + (255,))
    return img


def lamp_textures():
    """Brass (stem and base) and green glass (the shade), for the banker's lamp."""
    rng = random.Random(5)
    brass = Image.new("RGBA", (16, 16))
    shade = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            n = rng.randint(-10, 10)
            brass.putpixel((x, y), (196 + n, 154 + n, 58 + n // 2, 255))
            g = (34 + n // 2, 120 + n, 52 + n // 2)
            if y in (0, 1):
                g = (190, 160, 70)                      # brass rim along the top
            elif y >= 14:
                g = (150, 230, 140)                     # the lit underside glowing
            shade.putpixel((x, y), g + (255,))
    return brass, shade


def writ_texture():
    """A rolled parchment with a ribbon and a big red wax seal with a paw on it."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(9)
    for y in range(2, 14):
        for x in range(3, 13):
            n = rng.randint(-6, 6)
            c = (232 + n, 214 + n, 168 + n)
            if x in (3, 12):
                c = (200 + n, 178 + n, 128 + n)
            if y in (4, 6, 8) and 5 <= x <= 10 and rng.random() < 0.8:
                c = (120, 100, 80)                       # lines of writing
            img.putpixel((x, y), c + (255,))
    for x in range(2, 14):                               # the rolled ends
        for y in (1, 14):
            img.putpixel((x, y), (206, 184, 132, 255))
    for y in range(9, 16):                               # the ribbon
        img.putpixel((7, y), (130, 40, 120, 255))
        img.putpixel((8, y), (110, 30, 100, 255))
    for y in range(9, 14):
        for x in range(6, 11):
            if math.hypot(x - 8, y - 11) <= 2.3:
                img.putpixel((x, y), (178, 28, 30, 255))
    img.putpixel((8, 11), (120, 14, 16, 255))
    img.putpixel((7, 10), (120, 14, 16, 255))
    img.putpixel((9, 10), (120, 14, 16, 255))
    return img


def main():
    os.makedirs(os.path.join(A, "textures", "block"), exist_ok=True)
    os.makedirs(os.path.join(A, "textures", "item"), exist_ok=True)
    manhole_texture().save(os.path.join(A, "textures", "block", "manhole.png"))
    brass, shade = lamp_textures()
    brass.save(os.path.join(A, "textures", "block", "bankers_lamp_brass.png"))
    shade.save(os.path.join(A, "textures", "block", "bankers_lamp_shade.png"))
    def cube(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), light=False):
        e = {"from": frm, "to": to, "faces": {f: {"texture": tex} for f in faces}}
        if light:
            e["light_emission"] = 12
        return e
    write(os.path.join(A, "models", "block", "bankers_lamp.json"), {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"particle": "childrenofmu:block/bankers_lamp_brass", "brass": "childrenofmu:block/bankers_lamp_brass",
                     "shade": "childrenofmu:block/bankers_lamp_shade"},
        "elements": [cube([5, 0, 6], [11, 1, 10], "#brass"),                 # the base
                     cube([7.5, 1, 7.5], [8.5, 6, 8.5], "#brass"),           # the stem
                     cube([7.5, 6, 6.5], [8.5, 7, 8.5], "#brass"),           # the arm out to the shade
                     cube([3, 7, 5], [13, 9, 8], "#shade", light=True),      # the green shade, long and low
                     cube([3.5, 6.5, 5.5], [12.5, 7, 7.5], "#shade", ("down",), light=True)]})
    write(os.path.join(A, "models", "item", "bankers_lamp.json"), {"parent": "childrenofmu:block/bankers_lamp"})
    write(os.path.join(A, "blockstates", "bankers_lamp.json"), {"variants": {
        f"facing={f}": ({"model": "childrenofmu:block/bankers_lamp", "y": y} if y else {"model": "childrenofmu:block/bankers_lamp"})
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(os.path.join(D, "childrenofmu", "loot_table", "blocks", "bankers_lamp.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "childrenofmu:bankers_lamp"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    writ_texture().save(os.path.join(A, "textures", "item", "writ_of_trade.png"))

    tex = {"texture": "childrenofmu:block/manhole"}
    for part in ("bottom", "top", "open"):
        write(os.path.join(A, "models", "block", f"manhole_{part}.json"),
              {"parent": f"minecraft:block/template_trapdoor_{part}", "textures": tex})
    write(os.path.join(A, "models", "item", "manhole.json"), {"parent": "childrenofmu:block/manhole_bottom"})
    write(os.path.join(A, "models", "item", "writ_of_trade.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": "childrenofmu:item/writ_of_trade"}})
    variants = {}
    ys = {"north": 0, "east": 90, "south": 180, "west": 270}
    open_top = {"north": 180, "east": 270, "south": 0, "west": 90}
    for facing, y in ys.items():
        for half in ("bottom", "top"):
            variants[f"facing={facing},half={half},open=false"] = {"model": f"childrenofmu:block/manhole_{half}",
                                                                  **({"y": y} if y else {})}
            if half == "bottom":
                v = {"model": "childrenofmu:block/manhole_open"}
                if y:
                    v["y"] = y
            else:
                v = {"model": "childrenofmu:block/manhole_open", "x": 180}
                if open_top[facing]:
                    v["y"] = open_top[facing]
            variants[f"facing={facing},half={half},open=true"] = v
    write(os.path.join(A, "blockstates", "manhole.json"), {"variants": variants})
    write(os.path.join(D, "childrenofmu", "loot_table", "blocks", "manhole.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "childrenofmu:manhole"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    for tag in ("mineable/pickaxe",):
        p = os.path.join(D, "minecraft", "tags", "block", tag + ".json")
        d = json.load(open(p))
        for b in ("childrenofmu:manhole", "childrenofmu:bankers_lamp"):
            if b not in d["values"]:
                d["values"].append(b)
        write(p, d)
    print("manhole + writ assets written")


if __name__ == "__main__":
    main()
