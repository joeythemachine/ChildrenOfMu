"""
Generates the Ithk profession costumes: geometry and textures.

    python tools/make_ithk_costumes.py

Writes:
    src/main/java/com/childrenofmu/client/model/IthkCostumeShapes.java
    src/main/resources/assets/childrenofmu/textures/entity/ithk/costume_<profession>.png
    src/main/resources/assets/childrenofmu/textures/entity/ithk/costume_transmuter_sheen.png

Coordinates are in model pixels relative to the eye's center: y points DOWN, the eye looks toward -z.
Requires Pillow (pip install pillow).
"""
import importlib.util
import math
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")
JAVA_OUT = os.path.join(ROOT, "src", "main", "java", "com", "childrenofmu", "client", "model", "IthkCostumeShapes.java")
TEX_OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "childrenofmu", "textures", "entity", "ithk")

_spec = importlib.util.spec_from_file_location("ithk_assets", os.path.join(HERE, "make_ithk_assets.py"))
assets = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(assets)
face_pixels, pack = assets.face_pixels, assets.pack

TEX_SIZE = 64   # declared texture size (UV space)
SCALE = 2       # image pixels per texture unit, for finer detail (tiny stars)

rng = random.Random(1936)


def clamp(c):
    return max(0, min(255, int(c)))


def shade(rgb, delta):
    return tuple(clamp(c + delta) for c in rgb)


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


FACE_LIGHT = {"up": -12, "down": 8, "west": -4, "east": -4, "north": 0, "south": -6}  # model y is down


# ------------------------------------------------------------------ helpers
def box(x, y, z, w, h, d, grow=(0.0, 0.0, 0.0), tag=None):
    assert float(w).is_integer() and float(h).is_integer() and float(d).is_integer(), "box sizes must be whole numbers"
    return {"x": x, "y": y, "z": z, "w": int(w), "h": int(h), "d": int(d), "grow": grow, "tag": tag}


def part(name, pivot, rot, boxes, share_uv_with=None, layer="base"):
    """layer: which render pass draws it ("base", "sheen" = rainbow shimmer, "lining" = shifting color)."""
    return {"name": name, "pivot": pivot, "rot": rot, "boxes": boxes, "share": share_uv_with, "layer": layer}


def lining_of(p, name):
    """A paper-thin copy of each box, laid over its inner (-z) face."""
    boxes = [box(b["x"], b["y"], b["z"] - 0.6, b["w"], b["h"], 1, grow=(-0.02, -0.02, -0.45)) for b in p["boxes"]]
    return part(name, p["pivot"], p["rot"], boxes, share_uv_with=p["name"], layer="lining")


def overlay_of(p, name, layer):
    boxes = [box(b["x"], b["y"], b["z"], b["w"], b["h"], b["d"], grow=(0.03, 0.03, 0.03), tag=b["tag"]) for b in p["boxes"]]
    return part(name, p["pivot"], p["rot"], boxes, share_uv_with=p["name"], layer=layer)


# ------------------------------------------------------------------ costumes
def transmuter():
    steps = [(10, 2), (8, 2), (6, 2), (4, 2), (2, 2)]
    top = part("transmuter_top", (0, -8.5, 0), (0, 0, 0),
               [box(-w / 2, -(i + 1) * 2, -w / 2, w, h, w) for i, (w, h) in enumerate(steps)])
    bottom = part("transmuter_bottom", (0, 8.5, 0), (0, 0, 0),
                  [box(-w / 2, i * 2, -w / 2, w, h, w) for i, (w, h) in enumerate(steps)])
    parts = [top, bottom, overlay_of(top, "transmuter_top_sheen", "sheen"),
             overlay_of(bottom, "transmuter_bottom_sheen", "sheen")]

    base = (138, 163, 192)  # faded blue

    def paint(p, b, face, tx, ty, pt, n):
        c = shade(base, FACE_LIGHT[face] + rng.randint(-4, 4))
        return (*c, 255)

    def sheen(p, b, face, tx, ty, pt, n):
        # Diagonal streaks; the game tints them through the rainbow over time.
        k = (tx + ty) % 11
        if k in (0, 1):
            return (255, 255, 255, 200)
        if k in (5,):
            return (255, 255, 255, 110)
        return (255, 255, 255, 34)

    return parts, {"costume_transmuter": paint, "costume_transmuter_sheen": sheen}


def archivist():
    cap = part("archivist_cap", (0, -6.5, 0), (-6, 0, 0), [
        box(-4.5, -2, -4.5, 9, 2, 9, tag="green"),                       # band
        box(-6.5, -3, -6.5, 13, 1, 13, tag="board"),                     # mortarboard
        box(-0.5, -4, -0.5, 1, 1, 1, tag="gold"),                        # button
        box(0.5, -3.5, -0.5, 6, 1, 1, grow=(0, -0.3, -0.3), tag="gold"),  # cord across the top
        box(6.2, -3, -0.5, 1, 4, 1, grow=(-0.3, 0, -0.3), tag="gold"),   # cord hanging over the edge
        box(5.7, 1, -1, 2, 3, 2, grow=(-0.2, 0, -0.2), tag="tassel"),     # tassel
    ])
    green = (32, 74, 46)
    gold = (222, 176, 58)

    def paint(p, b, face, tx, ty, pt, n):
        tag = b["tag"]
        if tag in ("green", "board"):
            c = shade(green, FACE_LIGHT[face] + rng.randint(-4, 4))
            if tag == "board" and face == "up":
                c = shade(c, 10)
        elif tag == "tassel":
            c = shade(gold, FACE_LIGHT[face] + (-18 if (tx // SCALE) % 2 else 0) + rng.randint(-6, 6))
        else:
            c = shade(gold, FACE_LIGHT[face] + rng.randint(-6, 6))
        return (*c, 255)

    return [cap], {"costume_archivist": paint}


def gatekeeper():
    a = 55  # how far the side panels wrap forward, in degrees
    tilt = -15
    parts = [
        part("gatekeeper_back", (0, 0, 7.2), (0, 0, 0), [
            box(-4, -6, 0, 8, 10, 1), box(-2, 4, 0, 4, 2, 1), box(-1, 6, 0, 2, 1, 1)]),
        part("gatekeeper_left", (-4, 0, 7.2), (0, -a, 0), [
            box(-6, -6, 0, 6, 10, 1), box(-6, 4, 0, 3, 2, 1)]),
        part("gatekeeper_right", (4, 0, 7.2), (0, a, 0), [
            box(0, -6, 0, 6, 10, 1), box(3, 4, 0, 3, 2, 1)]),
        part("gatekeeper_collar_back", (0, -6, 7.2), (tilt, 0, 0), [
            box(-4, -4, 0, 8, 4, 1)]),
        part("gatekeeper_collar_left", (-4, -6, 7.2), (tilt, -a, 0), [
            box(-6, -6, 0, 6, 6, 1), box(-6, -8, 0, 3, 2, 1), box(-6, -9, 0, 1, 1, 1)]),
        part("gatekeeper_collar_right", (4, -6, 7.2), (tilt, a, 0), [
            box(0, -6, 0, 6, 6, 1), box(3, -8, 0, 3, 2, 1), box(5, -9, 0, 1, 1, 1)]),
    ]
    parts += [lining_of(p, p["name"] + "_lining") for p in list(parts)]

    night = (20, 34, 96)  # deep blue

    def paint(p, b, face, tx, ty, pt, n):
        if face == "north":
            # Inner side: a swirl, in grey. The game colors it with a slowly shifting hue.
            ang = math.atan2(pt[1] + 2, pt[0] + 0.5)
            r = math.hypot(pt[0] + 0.5, pt[1] + 2)
            v = 0.5 + 0.5 * math.sin(r * 1.3 + ang * 3)
            g = clamp(150 + v * 105)
            return (g, g, g, 255)
        c = shade(night, FACE_LIGHT[face] // 2 + rng.randint(-3, 3))
        roll = rng.random()
        if roll < 0.009:
            c = (250, 250, 235)
        elif roll < 0.016:
            c = (200, 200, 180)
        return (*c, 255)

    return parts, {"costume_gatekeeper": paint}


def curator():
    def ring(name, pivot, rot, outer, wall=1, h=4):
        o, i = outer, outer - wall
        return part(name, pivot, rot, [
            box(-o, -h / 2, -o, 2 * o, h, wall),          # front
            box(-o, -h / 2, i, 2 * o, h, wall),           # back
            box(-o, -h / 2, -i, wall, h, 2 * i),          # left
            box(i, -h / 2, -i, wall, h, 2 * i),           # right
        ])

    parts = [
        # Two wraps, crossing at slightly different angles. The top edge stays below the iris.
        ring("curator_upper", (0, 5.0, 0), (0, 0, 5), 7.5),
        ring("curator_lower", (0, 8.0, 0), (0, 12, -6), 5.5, h=3),
        part("curator_end", (-4.5, 6, -7.9), (-12, 0, 8), [
            box(-2, 0, -0.5, 4, 4, 1, tag="end"),
            box(-2, 4, -0.5, 4, 1, 1, grow=(-0.1, 0, -0.1), tag="fringe"),
        ]),
    ]
    gold = (186, 140, 40)
    pink = (190, 146, 156)

    def paint(p, b, face, tx, ty, pt, n):
        if b["tag"] == "fringe":
            stripe = (tx // SCALE) % 2
            c = gold if stripe else shade(gold, -30)
        elif b["tag"] == "end":
            stripe = int(math.floor((pt[1]) / 2)) % 2
            c = gold if stripe == 0 else pink
        else:
            ang = math.atan2(pt[0], pt[2])
            stripe = int(math.floor(ang * 7.0 / 2.6)) % 2
            c = gold if stripe == 0 else pink
        c = shade(c, FACE_LIGHT[face] + rng.randint(-5, 5))
        return (*c, 255)

    return parts, {"costume_curator": paint}


COSTUMES = {
    "transmuter": transmuter,
    "archivist": archivist,
    "gatekeeper": gatekeeper,
    "curator": curator,
}


# ------------------------------------------------------------------ build
def main():
    os.makedirs(TEX_OUT, exist_ok=True)
    java_parts = []
    for prof, make in COSTUMES.items():
        parts, painters = make()
        # Pack every box that owns its UVs (parts that share UVs reuse their source's).
        owners = [p for p in parts if p["share"] is None]
        sizes, refs = [], []
        for p in owners:
            for bi, b in enumerate(p["boxes"]):
                sizes.append((2 * b["d"] + 2 * b["w"], b["d"] + b["h"]))
                refs.append((p["name"], bi))
        positions, tex_h = pack(sizes, TEX_SIZE)
        if tex_h > TEX_SIZE:
            raise SystemExit(f"{prof}: costume doesn't fit in a {TEX_SIZE}x{TEX_SIZE} texture")
        uv = dict(zip(refs, positions))
        for p in parts:
            src = p["share"] or p["name"]
            p["uvs"] = [uv[(src, bi)] for bi in range(len(p["boxes"]))]

        for tex_name, painter in painters.items():
            img = Image.new("RGBA", (TEX_SIZE * SCALE, TEX_SIZE * SCALE), (0, 0, 0, 0))
            for p in owners:
                for b, (u, v) in zip(p["boxes"], p["uvs"]):
                    for tx, ty, face, pt, n in face_pixels((b["x"], b["y"], b["z"]), (b["w"], b["h"], b["d"]), (u, v), SCALE):
                        img.putpixel((tx, ty), painter(p, b, face, tx, ty, pt, n))
            img.save(os.path.join(TEX_OUT, tex_name + ".png"))

        for p in parts:
            java_parts.append((prof, p))

    def f(x):
        s = f"{float(x):.3f}".rstrip("0")
        return (s + "0" if s.endswith(".") else s) + "F"

    lines = []
    for prof, p in java_parts:
        boxes = ", ".join(
            f"new Box({u}, {v}, {f(b['x'])}, {f(b['y'])}, {f(b['z'])}, {b['w']}, {b['h']}, {b['d']}, "
            f"{f(b['grow'][0])}, {f(b['grow'][1])}, {f(b['grow'][2])})"
            for b, (u, v) in zip(p["boxes"], p["uvs"]))
        px, py, pz = p["pivot"]
        rx, ry, rz = p["rot"]
        lines.append(
            f"            new Part(\"{p['name']}\", \"{prof}\", Layer.{p['layer'].upper()}, "
            f"{f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)},\n                    new Box[]{{{boxes}}}),")

    java = f"""package com.childrenofmu.client.model;

// GENERATED by tools/make_ithk_costumes.py. Don't edit by hand; change the script and re-run it.

/** Geometry for the Ithk profession costumes. Positions are relative to the eye's center (y down, front is -z). */
final class IthkCostumeShapes {{
    static final int TEX_SIZE = {TEX_SIZE};

    /** BASE: normal texture. SHEEN: rainbow shimmer overlay. LINING: inner cape, color shifting over time. */
    enum Layer {{ BASE, SHEEN, LINING }}

    record Box(int u, int v, float x, float y, float z, int w, int h, int d, float growX, float growY, float growZ) {{}}

    /** Rotations are in degrees. */
    record Part(String name, String profession, Layer layer, float pivotX, float pivotY, float pivotZ,
                float rotX, float rotY, float rotZ, Box[] boxes) {{}}

    static final Part[] PARTS = {{
{chr(10).join(lines)}
    }};

    private IthkCostumeShapes() {{}}
}}
"""
    with open(JAVA_OUT, "w", newline="\n") as fh:
        fh.write(java)
    print(f"{len(java_parts)} costume parts written")


if __name__ == "__main__":
    main()
