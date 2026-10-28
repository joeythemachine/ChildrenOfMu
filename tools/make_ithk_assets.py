"""
Generates the Ithk's voxel-sphere geometry and both textures.

    python tools/make_ithk_assets.py

Writes:
    src/main/java/com/childrenofmu/client/model/IthkShape.java   (box list + UVs, read by IthkModel)
    src/main/resources/assets/childrenofmu/textures/entity/ithk/ithk.png
    src/main/resources/assets/childrenofmu/textures/entity/ithk/ithk_ethereal.png

Requires Pillow (pip install pillow).
Tweak DIAMETER / RADIUS below to change the sphere, then re-run and relaunch.
"""
import itertools
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
JAVA_OUT = os.path.join(ROOT, "src", "main", "java", "com", "childrenofmu", "client", "model", "IthkShape.java")
TEX_OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "childrenofmu", "textures", "entity", "ithk")

DIAMETER = 14      # in model pixels (16 = one block)
RADIUS = 7.0       # voxels whose centers are within this distance are filled
TEX_W = 128
WIRE_SCALE = 4     # the ethereal texture is this many times sharper, for thin wireframe lines

# Eye parts: name -> (x, y, z, w, h, d) in model pixels, relative to the sphere's center. Front is -z.
EYE_PARTS = {
    "iris":  (-3.0, -3.0, -7.5, 6, 6, 1),
    # The closed eye: exactly the same shape as the iris, shown in its place while blinking.
    "lid":   (-3.0, -3.0, -7.5, 6, 6, 1),
}


# ---------------------------------------------------------------- geometry

def make_voxels():
    half = DIAMETER / 2
    vox = set()
    for x, y, z in itertools.product(range(DIAMETER), repeat=3):
        cx, cy, cz = x + 0.5 - half, y + 0.5 - half, z + 0.5 - half
        if cx * cx + cy * cy + cz * cz <= RADIUS * RADIUS:
            vox.add((x, y, z))
    return vox


def decompose(vox):
    """Splits the voxels into non-overlapping boxes (so no faces flicker against each other)."""
    left = set(vox)
    boxes = []
    for y in range(DIAMETER):
        for z in range(DIAMETER):
            for x in range(DIAMETER):
                if (x, y, z) not in left:
                    continue
                w = 1
                while (x + w, y, z) in left:
                    w += 1
                d = 1
                while all((x + i, y, z + d) in left for i in range(w)):
                    d += 1
                h = 1
                while all((x + i, y + h, z + k) in left for i in range(w) for k in range(d)):
                    h += 1
                for i, j, k in itertools.product(range(w), range(h), range(d)):
                    left.discard((x + i, y + j, z + k))
                boxes.append((x, y, z, w, h, d))
    return boxes


def pack(sizes, width):
    """Simple shelf packing of box-UV layouts. sizes: list of (layout_w, layout_h). Returns positions, height."""
    order = sorted(range(len(sizes)), key=lambda i: -sizes[i][1])
    pos = [None] * len(sizes)
    x = y = shelf_h = 0
    for i in order:
        w, h = sizes[i]
        if x + w > width:
            x, y, shelf_h = 0, y + shelf_h, 0
        pos[i] = (x, y)
        x += w
        shelf_h = max(shelf_h, h)
    height = y + shelf_h
    tex_h = 32
    while tex_h < height:
        tex_h *= 2
    return pos, tex_h


# ------------------------------------------------- box-UV face <-> model space
# Mirrors Minecraft's ModelPart.Cube: which texture rectangle each face uses, and how its
# corners map onto the cube's corners.

def face_layout(u, v, w, h, d):
    f4, f5, f6, f7, f8, f9 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
    f10, f11, f12 = v, v + d, v + d + h
    return {
        # name: (u1, v1, u2, v2, corner-keys in vertex order, outward normal)
        "down":  (f5, f10, f6, f11, ("v4", "v3", "v7", "v0"), (0, -1, 0)),
        "up":    (f6, f11, f7, f10, ("v1", "v2", "v6", "v5"), (0, 1, 0)),
        "west":  (f4, f11, f5, f12, ("v7", "v3", "v6", "v2"), (-1, 0, 0)),
        "north": (f5, f11, f6, f12, ("v0", "v7", "v2", "v1"), (0, 0, -1)),
        "east":  (f6, f11, f8, f12, ("v4", "v0", "v1", "v5"), (1, 0, 0)),
        "south": (f8, f11, f9, f12, ("v3", "v4", "v5", "v6"), (0, 0, 1)),
    }


def corners(x, y, z, w, h, d):
    X, Y, Z = x + w, y + h, z + d
    return {
        "v7": (x, y, z), "v0": (X, y, z), "v1": (X, Y, z), "v2": (x, Y, z),
        "v3": (x, y, Z), "v4": (X, y, Z), "v5": (X, Y, Z), "v6": (x, Y, Z),
    }


def face_pixels(box_xyz, size, uv, scale=1):
    """Yields (tex_x, tex_y, face_name, model_point_on_face, outward_normal) for every texel of a box.
    With scale > 1, texels are that many times smaller (for a higher-resolution texture)."""
    x, y, z = box_xyz
    w, h, d = size
    c = corners(x, y, z, w, h, d)
    for name, (u1, v1, u2, v2, keys, n) in face_layout(uv[0], uv[1], w, h, d).items():
        p0, p1, p2 = c[keys[0]], c[keys[1]], c[keys[2]]   # p0=(u2,v1) p1=(u1,v1) p2=(u1,v2)
        for tx in range(min(u1, u2) * scale, max(u1, u2) * scale):
            for ty in range(min(v1, v2) * scale, max(v1, v2) * scale):
                s = ((tx + 0.5) / scale - u1) / (u2 - u1)
                t = ((ty + 0.5) / scale - v1) / (v2 - v1)
                point = tuple(p1[k] + s * (p0[k] - p1[k]) + t * (p2[k] - p1[k]) for k in range(3))
                yield tx, ty, name, point, n


# ---------------------------------------------------------------- painting

GLOBE_STEP = math.radians(30)   # spacing of the wireframe's latitude/longitude lines


def on_globe_line(pt):
    """True if a surface point lies on one of the wireframe's latitude or longitude lines."""
    x, y, z = pt
    r = math.sqrt(x * x + y * y + z * z) or 1.0
    lat = math.asin(max(-1.0, min(1.0, -y / r)))
    lon = math.atan2(x, z)
    ring = math.hypot(x, z)  # distance from the vertical axis, for converting angles to pixels
    d_lat = abs((lat + GLOBE_STEP / 2) % GLOBE_STEP - GLOBE_STEP / 2) * r
    d_lon = abs((lon + GLOBE_STEP / 2) % GLOBE_STEP - GLOBE_STEP / 2) * ring
    return d_lat < 0.55 or (d_lon < 0.55 and abs(lat) < math.radians(80))


def clamp(c):
    return max(0, min(255, int(c)))


def shade(rgb, delta):
    return tuple(clamp(c + delta) for c in rgb)


def main():
    rng = random.Random(1936)
    vox = make_voxels()
    shell = decompose(vox)
    half = DIAMETER // 2

    eye_names = list(EYE_PARTS)
    sizes = [(2 * d + 2 * w, d + h) for (_, _, _, w, h, d) in shell]
    sizes += [(2 * p[5] + 2 * p[3], p[5] + p[4]) for p in EYE_PARTS.values()]
    positions, tex_h = pack(sizes, TEX_W)

    normal = Image.new("RGBA", (TEX_W, tex_h), (0, 0, 0, 0))
    wire = Image.new("RGBA", (TEX_W * WIRE_SCALE, tex_h * WIRE_SCALE), (0, 0, 0, 0))

    sclera = (234, 228, 218)
    face_light = {"up": -14, "down": 8, "west": -4, "east": -4, "north": 0, "south": -8}  # model y points down
    line = (90, 255, 130, 255)
    bright = (200, 255, 210, 255)

    # Veins: a few short random walks over the voxel surface, away from the eye's front.
    veins = {}
    surface = [v for v in vox if any((v[0] + a, v[1] + b, v[2] + c) not in vox
                                     for a, b, c in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)))]
    for _ in range(14):
        p = rng.choice([v for v in surface if v[2] >= 4])
        color = rng.choice([(176, 122, 192), (196, 92, 88)])
        for _ in range(rng.randint(3, 6)):
            veins[p] = color
            step = rng.choice(((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)))
            q = (p[0] + step[0], p[1] + step[1], p[2] + step[2])
            if q in surface:
                p = q

    for (bx, by, bz, w, h, d), uv in zip(shell, positions):
        for tx, ty, fname, pt, n in face_pixels((bx, by, bz), (w, h, d), uv):
            v = tuple(int((pt[k] - 0.5 * n[k]) // 1) for k in range(3))   # the voxel this texel belongs to
            out = (v[0] + n[0], v[1] + n[1], v[2] + n[2])
            exposed = out not in vox
            if not exposed:
                continue  # hidden internal face: leave transparent
            # Edge of a flat patch: a neighbor along the face is missing, or something sits in front of it.
            edge = False
            for e in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                if e == n or e == (-n[0], -n[1], -n[2]):
                    continue
                nb = (v[0] + e[0], v[1] + e[1], v[2] + e[2])
                front = (nb[0] + n[0], nb[1] + n[1], nb[2] + n[2])
                if nb not in vox or front in vox:
                    edge = True
            base = shade(sclera, face_light[fname] + rng.randint(-5, 5))
            if v in veins and fname != "north":
                base = shade(veins[v], rng.randint(-10, 10))
            if edge:
                base = shade(base, -10)
            normal.putpixel((tx, ty), (*base, 255))

    # Ethereal wireframe, drawn at WIRE_SCALE times the resolution so the lines are thin:
    # a line runs along every crease of the voxel surface.
    for (bx, by, bz, w, h, d), uv in zip(shell, positions):
        for tx, ty, fname, pt, n in face_pixels((bx, by, bz), (w, h, d), uv, WIRE_SCALE):
            v = tuple(int((pt[k] - 0.5 * n[k]) // 1) for k in range(3))
            if (v[0] + n[0], v[1] + n[1], v[2] + n[2]) in vox:
                continue
            for e in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                if e == n or e == (-n[0], -n[1], -n[2]):
                    continue
                axis = 0 if e[0] else 1 if e[1] else 2
                offset = (pt[axis] - (v[axis] + 0.5)) * (e[axis])
                if offset < 0.5 - 1.0 / WIRE_SCALE:
                    continue  # not the strip of this texel nearest that side
                nb = (v[0] + e[0], v[1] + e[1], v[2] + e[2])
                front = (nb[0] + n[0], nb[1] + n[1], nb[2] + n[2])
                if nb not in vox or front in vox:
                    wire.putpixel((tx, ty), line)
                    break

    # Eye parts.
    eye_uv = dict(zip(eye_names, positions[len(shell):]))
    for name, (x, y, z, w, h, d) in EYE_PARTS.items():
        u, vv = eye_uv[name]
        for fname, (u1, v1, u2, v2, _, _) in face_layout(u, vv, w, h, d).items():
            x0, x1, y0, y1 = min(u1, u2), max(u1, u2), min(v1, v2), max(v1, v2)
            for tx in range(x0, x1):
                for ty in range(y0, y1):
                    fx, fy = tx - x0, ty - y0
                    fw, fh = x1 - x0, y1 - y0
                    if name == "iris":
                        if fname == "north":
                            cx, cy = (fw - 1) / 2, (fh - 1) / 2
                            r = max(abs(fx - cx), abs(fy - cy))
                            if r < 1.0:
                                c = (16, 14, 20)          # pupil (stays near-black when tinted)
                            elif (fx, fy) == (1, 1):
                                c = (255, 255, 255)
                            elif r >= 2.4:
                                c = (120, 120, 120)
                            elif r >= 1.4:
                                c = (clamp(200 + rng.randint(-15, 15)),) * 3
                            else:
                                c = (235, 235, 235)
                        else:
                            c = (150, 150, 150)
                    else:  # lid
                        lid = (107, 74, 122)
                        if fname == "north" and fy == fh // 2:
                            c = (58, 38, 68)
                        else:
                            c = shade(lid, (12 if fy < fh // 2 else -6) + rng.randint(-5, 5))
                    normal.putpixel((tx, ty), (*c, 255))
                    # Wire: outline each face, one thin texel wide.
                    for sx in range(WIRE_SCALE):
                        for sy in range(WIRE_SCALE):
                            gx, gy = fx * WIRE_SCALE + sx, fy * WIRE_SCALE + sy
                            # Pupil outline on the iris's front: the 2x2 center square.
                            px0, px1 = (fw // 2 - 1) * WIRE_SCALE, (fw // 2 + 1) * WIRE_SCALE - 1
                            py0, py1 = (fh // 2 - 1) * WIRE_SCALE, (fh // 2 + 1) * WIRE_SCALE - 1
                            on_pupil = (name == "iris" and fname == "north" and px0 <= gx <= px1 and py0 <= gy <= py1
                                        and (gx in (px0, px1) or gy in (py0, py1)))
                            if gx in (0, fw * WIRE_SCALE - 1) or gy in (0, fh * WIRE_SCALE - 1) or on_pupil:
                                wire.putpixel((tx * WIRE_SCALE + sx, ty * WIRE_SCALE + sy), bright)

    os.makedirs(TEX_OUT, exist_ok=True)
    normal.save(os.path.join(TEX_OUT, "ithk.png"))
    wire.save(os.path.join(TEX_OUT, "ithk_ethereal.png"))

    # Java: shell boxes are centered on the sphere's middle.
    lines = []
    for (bx, by, bz, w, h, d), (u, vv) in zip(shell, positions):
        lines.append(f"            {{{u}, {vv}, {bx - half}, {by - half}, {bz - half}, {w}, {h}, {d}}},")
    eye = {n: eye_uv[n] for n in eye_names}
    java = f"""package com.childrenofmu.client.model;

// GENERATED by tools/make_ithk_assets.py. Don't edit by hand; change the script and re-run it.

/** Voxel-sphere geometry for the Ithk: {len(shell)} non-overlapping boxes, diameter {DIAMETER}px. */
final class IthkShape {{
    static final int TEX_WIDTH = {TEX_W};
    static final int TEX_HEIGHT = {tex_h};

    /** Each row: texU, texV, x, y, z, width, height, depth. */
    static final int[][] SHELL = {{
{chr(10).join(lines)}
    }};

    static final int IRIS_U = {eye['iris'][0]}, IRIS_V = {eye['iris'][1]};
    static final int LID_U = {eye['lid'][0]}, LID_V = {eye['lid'][1]};

    private IthkShape() {{}}
}}
"""
    with open(JAVA_OUT, "w", newline="\n") as f:
        f.write(java)
    print(f"{len(shell)} boxes, texture {TEX_W}x{tex_h}")


if __name__ == "__main__":
    main()
