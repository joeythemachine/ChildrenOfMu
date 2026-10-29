"""
Draws a structure template (.nbt) as a little isometric picture, so a building can be looked over without loading the
game. Colors come from the block names (rough, but enough to see form, roofs, windows and furnishings).

    python3 render_template.py <template.nbt> <out.png> [scale] [view]
view: one of ne, nw, se, sw (the corner you look from; default se: from the front-right, front being -z).
Optional cut: env CUT_Y=n draws only layers below y = n (to see inside).
"""
import gzip
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
import anvil  # noqa: E402

COLORS = [
    ("glass_pane", (210, 230, 240)), ("white_stained_glass", (240, 240, 236)), ("glass", (200, 225, 240)),
    ("water", (60, 110, 200)), ("lava", (230, 110, 30)), ("leaves", (80, 140, 60)), ("azalea", (90, 150, 70)),
    ("grass_block", (100, 160, 70)), ("gravel", (140, 136, 130)), ("dirt", (120, 86, 60)),
    ("deepslate_tile", (60, 60, 66)), ("polished_blackstone", (40, 36, 42)), ("gilded", (90, 70, 30)),
    ("blackstone", (34, 30, 36)), ("polished_andesite", (150, 150, 146)), ("chiseled_stone", (130, 130, 130)),
    ("stone_brick", (122, 122, 122)), ("cobble", (110, 110, 110)), ("white_terracotta", (226, 210, 196)),
    ("stripped_dark_oak", (96, 72, 50)), ("dark_oak_log", (60, 44, 30)), ("dark_oak", (66, 44, 26)),
    ("spruce", (110, 80, 50)), ("oak", (160, 130, 80)), ("wool", (230, 230, 230)), ("carpet", (180, 60, 60)),
    ("bed", (160, 40, 40)), ("lantern", (250, 200, 90)), ("iron_bars", (90, 90, 96)), ("door", (80, 56, 34)),
    ("bookshelf", (140, 100, 60)), ("barrel", (120, 84, 50)), ("hay", (210, 180, 60)), ("cauldron", (60, 60, 60)),
    ("loom", (170, 140, 100)), ("furnace", (100, 100, 100)), ("smoker", (100, 90, 80)), ("anvil", (60, 60, 66)),
    ("black_concrete", (20, 20, 24)), ("bell", (230, 190, 60)), ("candle", (240, 230, 200)),
    ("lightning_rod", (200, 120, 80)), ("stone", (125, 125, 125)), ("ladder", (150, 110, 60)),
]


def color(name):
    n = name.split("[")[0].split(":")[-1]
    for k, c in COLORS:
        if k in n:
            return c
    return (200, 0, 200)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def main():
    path, out = sys.argv[1], sys.argv[2]
    scale = int(sys.argv[3]) if len(sys.argv) > 3 else 6
    view = sys.argv[4] if len(sys.argv) > 4 else "se"
    cut = int(os.environ.get("CUT_Y", "9999"))
    t = anvil.read_nbt(gzip.decompress(open(path, "rb").read()))
    pal = t["palette"] if "palette" in t else t["palettes"][0]
    names = [anvil.state_name(p) for p in pal]
    sx, sy, sz = t["size"]
    blocks = {}
    for b in t["blocks"]:
        n = names[b["state"]]
        if n.split("[")[0].endswith(":air") or b["pos"][1] >= cut:
            continue
        x, y, z = b["pos"]
        # Turn so we look from the chosen corner (screen right = +u, depth = +v).
        if view == "se":
            u, v = x, z
        elif view == "sw":
            u, v = sz - 1 - z, x
        elif view == "nw":
            u, v = sx - 1 - x, sz - 1 - z
        else:
            u, v = z, sx - 1 - x
        blocks[(u, y, v)] = n
    s = scale
    W = (sx + sz) * s * 2 + 40
    H = (sx + sz) * s + sy * s * 2 + 40
    img = Image.new("RGB", (W, H), (235, 240, 245))
    d = ImageDraw.Draw(img)
    ox, oy = (sz) * s * 2 + 20, sy * s * 2 + 20
    # Painter's order: far to near, bottom to top.
    for (u, y, v) in sorted(blocks, key=lambda p: (p[0] + p[2], p[1])):
        n = blocks[(u, y, v)]
        c = color(n)
        px = ox + (u - v) * 2 * s
        py = oy + (u + v) * s - y * 2 * s
        partial = any(k in n for k in ("slab", "stairs", "carpet", "pane", "bars", "fence", "lantern", "candle",
                                       "rod", "ladder", "door", "bed", "pressure"))
        h = s if partial else 2 * s
        top = [(px, py - h), (px + 2 * s, py + s - h), (px, py + 2 * s - h), (px - 2 * s, py + s - h)]
        left = [(px - 2 * s, py + s - h), (px, py + 2 * s - h), (px, py + 2 * s), (px - 2 * s, py + s)]
        right = [(px + 2 * s, py + s - h), (px, py + 2 * s - h), (px, py + 2 * s), (px + 2 * s, py + s)]
        d.polygon(left, fill=shade(c, 0.72))
        d.polygon(right, fill=shade(c, 0.86))
        d.polygon(top, fill=c)
    img.save(out)


if __name__ == "__main__":
    main()
