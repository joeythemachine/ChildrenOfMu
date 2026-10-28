"""
Makes the textures, models, blockstates, loot tables, tags and names for the Frog folk's decor blocks:
reed thatch (block, stairs, slab), reed screen (a pane), lily lantern, frog idol, fishing net and cattails.
Run from anywhere: python tools/make_frog_blocks.py
"""
import json
import os
import random
import zlib

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
A = os.path.join(ROOT, "assets", "childrenofmu")
D = os.path.join(ROOT, "data")
NS = "childrenofmu"


def rng(name):
    return random.Random(zlib.crc32(name.encode()))


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def shade(c, k):
    return clamp((c[0] * k, c[1] * k, c[2] * k))


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def save(img, name):
    path = os.path.join(A, "textures", "block", name + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


# ---------------------------------------------------------------- textures
REEDS = [(118, 128, 62), (134, 140, 70), (100, 112, 52), (150, 146, 82), (124, 118, 60), (92, 104, 50)]


def reed_thatch_side():
    r = rng("reed_thatch_side")
    im = Image.new("RGBA", (16, 16))
    for x in range(16):
        base = r.choice(REEDS)
        for y in range(16):
            im.putpixel((x, y), clamp(shade(base, 0.85 + 0.25 * r.random())) + (255,))
    # Ragged tips hanging over each binding.
    for band in (4, 12):
        for x in range(16):
            im.putpixel((x, band), (70, 58, 34, 255))
            if r.random() < 0.6:
                im.putpixel((x, band + 1), (84, 70, 40, 255))
        for x in range(16):
            if r.random() < 0.35:
                im.putpixel((x, band - 1), shade(r.choice(REEDS), 1.2) + (255,))
    return im


def reed_thatch_top():
    r = rng("reed_thatch_top")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = r.choice(REEDS)
            im.putpixel((x, y), shade(c, 0.7 + 0.5 * r.random()) + (255,))
    for _ in range(26):                       # cut reed ends: light rings with dark hearts
        x, y = r.randrange(16), r.randrange(16)
        im.putpixel((x, y), (60, 64, 34, 255))
    return im


def reed_screen():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    light, dark = (176, 160, 98, 255), (124, 110, 62, 255)
    for i in range(16):                      # frame
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            im.putpixel(p, dark)
    for k in range(3, 15, 4):                # an over-under weave
        for i in range(1, 15):
            over = ((i // 2) + k // 4) % 2 == 0
            im.putpixel((i, k), light if over else dark)
            im.putpixel((k, i), dark if over else light)
    return im


def reed_screen_edge():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(7, 9):
            im.putpixel((x, y), (124, 110, 62, 255))
    return im


def lily_lantern():
    """
    UV layout: lily pad top 0..10 x 0..10; pad rim 0..10 x 10..11; bulb sides 10..16 x 0..6;
    bulb ends 10..16 x 6..12; stalk/string 14..15 x 12..16.
    """
    r = rng("lily_lantern")
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(10):
        for x in range(10):
            dx, dy = x - 4.5, y - 4.5
            if dx * dx + dy * dy > 26 or (dx > 0 and abs(dy) < 0.8):      # round pad with its notch
                continue
            im.putpixel((x, y), shade((70, 140, 50), 0.85 + 0.3 * r.random()) + (255,))
    for x in range(10):
        im.putpixel((x, 10), (50, 104, 38, 255))
    for y in range(6):
        for x in range(10, 16):
            edge = x in (10, 15) or y in (0, 5)
            im.putpixel((x, y), (236, 196, 96, 255) if edge else (255, 236, 160, 255))
    for y in range(6, 12):
        for x in range(10, 16):
            im.putpixel((x, y), (250, 220, 130, 255))
    for y in range(12, 16):
        im.putpixel((14, y), (60, 90, 40, 255))
    return im


def lily_lantern_item():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(3, 13):                   # pad
        for y in (5, 6):
            im.putpixel((x, y), (70, 140, 50, 255) if y == 5 else (50, 104, 38, 255))
    for y in range(1, 5):
        im.putpixel((8, y), (60, 90, 40, 255))
    for y in range(7, 14):                   # bulb
        for x in range(5, 11):
            edge = x in (5, 10) or y in (7, 13)
            im.putpixel((x, y), (236, 196, 96, 255) if edge else (255, 236, 160, 255))
    return im


def frog_idol():
    r = rng("frog_idol")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            stone = shade((112, 114, 104), 0.8 + 0.35 * r.random())
            moss = r.random() < (0.45 if y < 6 else 0.2)
            im.putpixel((x, y), (shade((88, 116, 54), 0.85 + 0.3 * r.random()) if moss else stone) + (255,))
    for x in range(12, 16):                  # eye texture region (12..16 x 0..4): gold with a dark slit
        for y in range(4):
            im.putpixel((x, y), (214, 170, 60, 255))
    im.putpixel((13, 1), (30, 26, 20, 255))
    im.putpixel((14, 1), (30, 26, 20, 255))
    return im


def fishing_net():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    twine = (196, 186, 150, 255)
    for i in range(16):
        for j in range(16):
            if (i + j) % 5 == 0 or (i - j) % 5 == 0:
                im.putpixel((i, j), twine)
    for x in range(16):
        im.putpixel((x, 0), (130, 100, 60, 255))
    for x, y in ((3, 11), (11, 6), (7, 14)):          # cork floats
        for dx in (0, 1):
            for dy in (0, 1):
                im.putpixel((x + dx, y + dy), (196, 120, 60, 255))
    return im


def cattails():
    r = rng("cattails")
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in (2, 5, 8, 11, 13):
        top = r.randint(1, 5)
        for y in range(top, 16):
            im.putpixel((x, y), shade((86, 128, 50), 0.85 + 0.3 * r.random()) + (255,))
        if r.random() < 0.7:                   # the brown cattail head
            for y in range(top + 1, top + 5):
                im.putpixel((x, y), (112, 74, 42, 255))
                if x + 1 < 16:
                    im.putpixel((x + 1, y), (96, 62, 34, 255))
    for _ in range(6):                          # a few leaning blades
        x = r.randrange(16)
        for y in range(r.randint(6, 10), 16):
            if 0 <= x < 16:
                im.putpixel((x, y), (104, 146, 60, 255))
            if r.random() < 0.3:
                x += r.choice((-1, 1))
    return im


# ---------------------------------------------------------------- models and states
def m(name, data):
    write_json(os.path.join(A, "models", "block", name + ".json"), data)


def item(name, data):
    write_json(os.path.join(A, "models", "item", name + ".json"), data)


def state(name, data):
    write_json(os.path.join(A, "blockstates", name + ".json"), data)


def stair_states(block):
    straight_y = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for facing, y0 in straight_y.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = f"{NS}:block/{block}" + ("" if shape == "straight" else ("_inner" if "inner" in shape else "_outer"))
                y = y0
                if half == "bottom" and shape.endswith("left"):
                    y -= 90
                if half == "top" and shape.endswith("right"):
                    y += 90
                v = {"model": model}
                if half == "top":
                    v["x"] = 180
                if y % 360:
                    v["y"] = y % 360
                if "x" in v or "y" in v:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    return {"variants": variants}


def cutout(d):
    d["render_type"] = "minecraft:cutout"
    return d


def main():
    for name, fn in (("reed_thatch_side", reed_thatch_side), ("reed_thatch_top", reed_thatch_top),
                     ("reed_screen", reed_screen), ("reed_screen_edge", reed_screen_edge),
                     ("lily_lantern", lily_lantern), ("frog_idol", frog_idol), ("fishing_net", fishing_net),
                     ("cattails", cattails)):
        save(fn(), name)
    path = os.path.join(A, "textures", "item", "lily_lantern.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    lily_lantern_item().save(path)

    tex = {"side": f"{NS}:block/reed_thatch_side", "top": f"{NS}:block/reed_thatch_top",
           "bottom": f"{NS}:block/reed_thatch_top"}
    m("reed_thatch", {"parent": "minecraft:block/cube_column",
                      "textures": {"end": f"{NS}:block/reed_thatch_top", "side": f"{NS}:block/reed_thatch_side"}})
    m("reed_thatch_stairs", {"parent": "minecraft:block/stairs", "textures": tex})
    m("reed_thatch_stairs_inner", {"parent": "minecraft:block/inner_stairs", "textures": tex})
    m("reed_thatch_stairs_outer", {"parent": "minecraft:block/outer_stairs", "textures": tex})
    m("reed_thatch_slab", {"parent": "minecraft:block/slab", "textures": tex})
    m("reed_thatch_slab_top", {"parent": "minecraft:block/slab_top", "textures": tex})
    state("reed_thatch", {"variants": {"": {"model": f"{NS}:block/reed_thatch"}}})
    state("reed_thatch_stairs", stair_states("reed_thatch_stairs"))
    state("reed_thatch_slab", {"variants": {
        "type=bottom": {"model": f"{NS}:block/reed_thatch_slab"},
        "type=top": {"model": f"{NS}:block/reed_thatch_slab_top"},
        "type=double": {"model": f"{NS}:block/reed_thatch"}}})
    for n in ("reed_thatch", "reed_thatch_stairs", "reed_thatch_slab"):
        item(n, {"parent": f"{NS}:block/{n}"})

    pane = {"pane": f"{NS}:block/reed_screen", "edge": f"{NS}:block/reed_screen_edge"}
    for suffix in ("post", "side", "side_alt", "noside", "noside_alt"):
        m(f"reed_screen_{suffix}", cutout({"parent": f"minecraft:block/template_glass_pane_{suffix}", "textures": pane}))
    b = f"{NS}:block/reed_screen_"
    state("reed_screen", {"multipart": [
        {"apply": {"model": b + "post"}},
        {"when": {"north": "true"}, "apply": {"model": b + "side"}},
        {"when": {"east": "true"}, "apply": {"model": b + "side", "y": 90}},
        {"when": {"south": "true"}, "apply": {"model": b + "side_alt"}},
        {"when": {"west": "true"}, "apply": {"model": b + "side_alt", "y": 90}},
        {"when": {"north": "false"}, "apply": {"model": b + "noside"}},
        {"when": {"east": "false"}, "apply": {"model": b + "noside_alt"}},
        {"when": {"south": "false"}, "apply": {"model": b + "noside_alt", "y": 90}},
        {"when": {"west": "false"}, "apply": {"model": b + "noside", "y": 270}}]})
    item("reed_screen", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/reed_screen"}})

    lt = f"{NS}:block/lily_lantern"

    def box(frm, to, faces):
        return {"from": frm, "to": to, "faces": faces}

    def all_faces(uv, top=None, bottom=None, glow=False):
        f = {d: {"uv": uv, "texture": "#t"} for d in ("north", "south", "east", "west")}
        f["up"] = {"uv": top or uv, "texture": "#t"}
        f["down"] = {"uv": bottom or uv, "texture": "#t"}
        return f

    bulb = all_faces([10, 0, 16, 6], [10, 6, 16, 12], [10, 6, 16, 12])
    pad = all_faces([0, 10, 10, 11], [0, 0, 10, 10], [0, 0, 10, 10])
    stalk = all_faces([14, 12, 15, 16])
    m("lily_lantern", cutout({"textures": {"t": lt, "particle": lt}, "elements": [
        box([5, 0, 5], [11, 6, 11], bulb), box([3, 6, 3], [13, 7, 13], pad), box([7.5, 7, 7.5], [8.5, 10, 8.5], stalk)]}))
    m("lily_lantern_hanging", cutout({"textures": {"t": lt, "particle": lt}, "elements": [
        box([7.5, 10, 7.5], [8.5, 16, 8.5], stalk), box([3, 9, 3], [13, 10, 13], pad), box([5, 3, 5], [11, 9, 11], bulb)]}))
    state("lily_lantern", {"variants": {"hanging=false": {"model": lt}, "hanging=true": {"model": lt + "_hanging"}}})
    item("lily_lantern", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/lily_lantern"}})

    it = f"{NS}:block/frog_idol"
    eye = {d: {"uv": [12, 0, 14, 2], "texture": "#eye"} for d in ("north", "south", "east", "west", "up", "down")}
    stone = {d: {"texture": "#t"} for d in ("north", "south", "east", "west", "up", "down")}
    m("frog_idol", {"parent": "minecraft:block/block", "textures": {"t": it, "eye": it, "particle": it}, "elements": [
        box([3, 0, 3], [13, 7, 13], stone), box([4, 7, 4], [12, 11, 12], stone),
        box([4, 11, 4], [6.5, 13, 6.5], eye), box([9.5, 11, 4], [12, 13, 6.5], eye),
        box([2.5, 0, 2.5], [5, 2, 5], stone), box([11, 0, 2.5], [13.5, 2, 5], stone)]})
    state("frog_idol", {"variants": {f"facing={f}": ({"model": it} | ({"y": y} if y else {}))
                                     for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    item("frog_idol", {"parent": it})

    nt = f"{NS}:block/fishing_net"
    m("fishing_net", cutout({"textures": {"t": nt, "particle": nt}, "elements": [
        {"from": [0, 0, 15.5], "to": [16, 16, 15.5], "shade": False,
         "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#t"}, "south": {"uv": [16, 0, 0, 16], "texture": "#t"}}}]}))
    state("fishing_net", {"variants": {f"facing={f}": ({"model": nt} | ({"y": y} if y else {}))
                                       for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    item("fishing_net", {"parent": "minecraft:item/generated", "textures": {"layer0": nt}})

    ct = f"{NS}:block/cattails"
    m("cattails", cutout({"parent": "minecraft:block/cross", "textures": {"cross": ct}}))
    state("cattails", {"variants": {"": {"model": ct}}})
    item("cattails", {"parent": "minecraft:item/generated", "textures": {"layer0": ct}})

    # Loot: everything drops itself (slabs two when doubled).
    for n in ("reed_thatch", "reed_thatch_stairs", "reed_screen", "lily_lantern", "frog_idol", "fishing_net", "cattails"):
        write_json(os.path.join(D, NS, "loot_table", "blocks", n + ".json"), {"type": "minecraft:block", "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{n}"}],
             "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_json(os.path.join(D, NS, "loot_table", "blocks", "reed_thatch_slab.json"), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:reed_thatch_slab", "functions": [
            {"function": "minecraft:set_count", "count": 2, "conditions": [
                {"condition": "minecraft:block_state_property", "block": f"{NS}:reed_thatch_slab",
                 "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})

    def tag(path, values):
        p = os.path.join(D, "minecraft", "tags", "block", path + ".json")
        d = json.load(open(p)) if os.path.exists(p) else {"values": []}
        for v in values:
            if v not in d["values"]:
                d["values"].append(v)
        write_json(p, d)

    tag("mineable/hoe", [f"{NS}:reed_thatch", f"{NS}:reed_thatch_stairs", f"{NS}:reed_thatch_slab"])
    tag("mineable/axe", [f"{NS}:reed_screen"])
    tag("mineable/pickaxe", [f"{NS}:frog_idol"])

    lang_path = os.path.join(A, "lang", "en_us.json")
    lang = json.load(open(lang_path))
    lang.update({f"block.{NS}.reed_thatch": "Reed Thatch", f"block.{NS}.reed_thatch_stairs": "Reed Thatch Stairs",
                 f"block.{NS}.reed_thatch_slab": "Reed Thatch Slab", f"block.{NS}.reed_screen": "Reed Screen",
                 f"block.{NS}.lily_lantern": "Lily Lantern", f"block.{NS}.frog_idol": "Frog Idol",
                 f"block.{NS}.fishing_net": "Fishing Net", f"block.{NS}.cattails": "Cattails"})
    write_json(lang_path, lang)
    print("frog blocks written")


if __name__ == "__main__":
    main()
