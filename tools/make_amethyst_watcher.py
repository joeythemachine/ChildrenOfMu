"""
Generates the Amethyst Watcher: a single round room suspended in the air, with a great eye-shaped window,
curling stone tentacles, a crown of crystal spikes and an inverted crystal base.

    python tools/make_amethyst_watcher.py

Writes src/main/resources/data/childrenofmu/structure/amethyst_watcher.nbt
"""
import math
import os

from ithk_build import AIR, AMETHYST, Builder

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure", "amethyst_watcher.nbt")

SIZE = 31
B = Builder(SIZE, seed=11)
C = B.c
rng = B.rng

# ---------------------------------------------------------------- dimensions (y levels; 0 = ground)
GROUND_RADIUS = 8
SPIKE_TIP = 8
BOWL_BOTTOM, BOWL_TOP = 19, 22
FLOOR = 23
WALL_BOTTOM, WALL_TOP = 24, 42
R = 5                              # body radius: 11 blocks across
EYE_CENTER, EYE_HALF_HEIGHT, EYE_HALF_WIDTH = 33, 6.5, 3.2
CAP = 43

# ---------------------------------------------------------------- palette
STONE = ["minecraft:deepslate_bricks"] * 4 + ["minecraft:polished_deepslate", "minecraft:tuff_bricks",
                                              "minecraft:cobbled_deepslate"]
TRIM = "minecraft:polished_deepslate"
TENTACLE = ["minecraft:deepslate_tiles"] * 3 + ["minecraft:deepslate_bricks"]
GLASS = "minecraft:purple_stained_glass"
PUPIL = "minecraft:tinted_glass"
LIGHT = ("minecraft:pearlescent_froglight", {"axis": "y"})


def stone():
    return rng.choice(STONE)


def circ(dx, dz, r):
    return Builder.circ_in(dx, dz, r)


def ring(dx, dz, r):
    return circ(dx, dz, r) and not circ(dx, dz, r - 1)


# ---------------------------------------------------------------- base
def base():
    B.spike(C, BOWL_BOTTOM, C, BOWL_BOTTOM - SPIKE_TIP + 1, base_r=3, up=False)
    for dx, dz in ((2, 0), (-2, 0), (0, 2), (0, -2)):
        B.spike(C + dx, BOWL_BOTTOM - 1, C + dz, 6 + rng.randint(0, 2), base_r=1, up=False)
    for dx, dz in ((3, 3), (-3, 3), (3, -3), (-3, -3)):
        B.spike(C + dx, BOWL_BOTTOM, C + dz, 3 + rng.randint(0, 2), base_r=0, up=False)
    for i, y in enumerate(range(BOWL_BOTTOM, BOWL_TOP + 1)):
        r = 3 + i
        for dx, dz in B.each(r, Builder.circ_in):
            B.put(C + dx, y, C + dz, TRIM if (y == BOWL_TOP and ring(dx, dz, r)) else stone())


def floor():
    for dx, dz in B.each(R, Builder.circ_in):
        name = TRIM if ring(dx, dz, R) or ring(dx, dz, R - 1) else AMETHYST
        B.put(C + dx, FLOOR, C + dz, name, keep_shape=True)
    B.put(C, FLOOR, C, *LIGHT, keep_shape=True)


# ---------------------------------------------------------------- walls with the eye window
def eye_half_width(y):
    t = (y - EYE_CENTER) / EYE_HALF_HEIGHT
    if abs(t) >= 1:
        return -1
    return int(round(EYE_HALF_WIDTH * math.sqrt(1 - t * t)))


def walls():
    for y in range(WALL_BOTTOM, WALL_TOP + 1):
        w = eye_half_width(y)
        for dx, dz in B.each(R, Builder.circ_in):
            x, z = C + dx, C + dz
            if not ring(dx, dz, R):
                B.air(x, y, z)
                continue
            front = dz <= -3
            back = dz >= 3
            if front and abs(dx) <= w:
                # The eye: purple glass with a dark slit of a pupil.
                pupil = dx == 0 and abs(y - EYE_CENTER) <= 4
                B.put(x, y, z, PUPIL if pupil else GLASS)
            elif front and (abs(dx) == w + 1 or (w == -1 and abs(dx) <= 1 and abs(y - EYE_CENTER) <= EYE_HALF_HEIGHT + 1)):
                B.put(x, y, z, AMETHYST, keep_shape=True)  # the eye's crystal rim
            elif back and dx == 0 and y <= WALL_BOTTOM + 2:
                B.air(x, y, z)                             # a narrow way in at the back
            elif back and dx == 0 and y <= WALL_TOP - 3:
                B.put(x, y, z, GLASS)                      # the back's tall slit window
            elif abs(dx) == abs(dz) or (abs(abs(dx) - abs(dz)) == 1 and abs(dx) + abs(dz) == 7):
                if y % 4:
                    B.put(x, y, z, AMETHYST)                 # crystal ribs on the diagonals...
                else:
                    B.put(x, y, z, *LIGHT, keep_shape=True)  # ...with glowing nodes
            else:
                B.put(x, y, z, stone())
    # Bands around the body.
    for y in (WALL_BOTTOM, WALL_TOP):
        for dx, dz in B.each(R, Builder.circ_in):
            if ring(dx, dz, R) and not (dz <= -3 and abs(dx) <= 1) and B.name_at(C + dx, y, C + dz) not in (AIR,):
                B.put(C + dx, y, C + dz, TRIM)


def cap_and_crown():
    for y, r in ((CAP, R + 1), (CAP + 1, R), (CAP + 2, R - 1), (CAP + 3, R - 2)):
        for dx, dz in B.each(r, Builder.circ_in):
            B.put(C + dx, y, C + dz, TRIM if ring(dx, dz, r) else stone())
    B.spike(C, CAP + 4, C, 16, base_r=2, tip=False)
    B.put(C, CAP + 20, C, "minecraft:end_rod", {"facing": "up"})
    B.put(C, CAP + 21, C, "minecraft:end_rod", {"facing": "up"})
    for dx, dz in ((3, 3), (-3, 3), (3, -3), (-3, -3)):
        B.spike(C + dx, CAP + 2, C + dz, 9 + rng.randint(-1, 2), base_r=1)
    for dx, dz in ((4, 0), (-4, 0), (0, 4), (0, -4)):
        h = 5 + rng.randint(0, 2)
        B.spike(C + dx, CAP + 1, C + dz, h, base_r=1, tip=False)
        B.end_rod(C + dx, CAP + 1 + h, C + dz)


# ---------------------------------------------------------------- tentacles
def tentacle(sx, sz, y0, up, length_turns=1.55, radius=4.5):
    """
    A curling stone tentacle leaving the body along direction (sx, sz). It heads outward, then curls
    upward (or downward) and back toward the body, thinning as it goes. Returns a point for a pendant.
    """
    n = math.hypot(sx, sz)
    ux, uz = sx / n, sz / n
    s_c, h_c = R + 0.5, (radius if up else -radius)
    pendant_at = None
    steps = 220
    for i in range(steps + 1):
        t = i / steps
        th = t * length_turns * math.pi
        rho = radius * (1 - 0.35 * t)
        s = s_c + rho * math.sin(th)
        h = (h_c - rho * math.cos(th)) if up else (h_c + rho * math.cos(th))
        thick = 1.25 * (1 - t) + 0.45
        px, py, pz = C + 0.5 + ux * s, y0 + 0.5 + h, C + 0.5 + uz * s
        rr = int(math.ceil(thick))
        for dx in range(-rr, rr + 1):
            for dy in range(-rr, rr + 1):
                for dz in range(-rr, rr + 1):
                    bx, by, bz = int(math.floor(px)) + dx, int(math.floor(py)) + dy, int(math.floor(pz)) + dz
                    if (bx + 0.5 - px) ** 2 + (by + 0.5 - py) ** 2 + (bz + 0.5 - pz) ** 2 <= thick * thick:
                        if B.name_at(bx, by, bz) in (None, AIR) and not circ(bx - C, bz - C, R - 1):
                            B.put(bx, by, bz, rng.choice(TENTACLE))
        if up and pendant_at is None and th >= math.pi / 2:
            pendant_at = (int(math.floor(px)), int(math.floor(py - thick)) - 1, int(math.floor(pz)))
        if not up and i == steps:
            pendant_at = (int(math.floor(px)), int(math.floor(py)) - 1, int(math.floor(pz)))
    return pendant_at


def tentacles():
    hangs = []
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        hangs.append(tentacle(sx, sz, WALL_TOP - 3, up=True))
        hangs.append(tentacle(sx, sz, WALL_BOTTOM + 3, up=False, length_turns=1.35, radius=4.0))
    for p in hangs:
        if p and (p[0], p[1], p[2]) not in B.blocks:
            B.pendant(p[0], p[1], p[2], rng.randint(1, 3))


# ---------------------------------------------------------------- interior and surroundings
def interior():
    for dx, dz in ((3, 1), (-3, 1), (2, 3), (-2, 3), (3, -2), (-3, -2)):
        B.cluster(C + dx, FLOOR + 1, C + dz, "up", rng.choice(["amethyst_cluster", "large_amethyst_bud"]))
    B.put(C, FLOOR + 1, C - 2, "minecraft:lectern", {"facing": "north", "has_book": "false", "powered": "false"})
    for dx, dz in ((1, -3), (-1, -3)):
        B.put(C + dx, FLOOR + 1, C + dz, "minecraft:purple_candle",
              {"candles": "3", "lit": "true", "waterlogged": "false"})
    for dx, dz in ((2, 0), (-2, 0), (0, 2), (0, -2)):
        B.put(C + dx, WALL_TOP, C + dz, *LIGHT, keep_shape=True)
    # Something watches within.
    B.ithk(C, FLOOR + 2, C + 1)


def surroundings():
    for i in range(4):
        a = i * math.pi / 2 + math.pi / 4
        B.pendant(C + int(round(math.cos(a) * 4.5)), BOWL_TOP - 1 - (i % 2), C + int(round(math.sin(a) * 4.5)), 2 + i % 2)
    for i in range(8):
        a = i * math.pi / 4 + 0.2
        r = 12 + (i % 2)
        y = 14 + (i * 7) % 26
        B.floating_crystal(C + int(round(math.cos(a) * r)), y, C + int(round(math.sin(a) * r)), r=1)


def lights():
    """Lanterns under the base and in the room, so the Watcher glows at night."""
    for dx, dz in ((6, 0), (-6, 0), (0, 6), (0, -6)):
        if B.name_at(C + dx, BOWL_TOP, C + dz) and B.name_at(C + dx, BOWL_TOP - 1, C + dz) is None:
            B.lantern(C + dx, BOWL_TOP - 1, C + dz, chain_len=1, soul=True)
    B.lantern(C, WALL_TOP, C, chain_len=2)
    for dx, dz in ((1, 1), (-1, -1), (1, -1), (-1, 1)):
        B.put(C + dx * 2, CAP + 2, C + dz * 2, *LIGHT, keep_shape=True)


def build():
    B.ground(GROUND_RADIUS)
    base()
    floor()
    walls()
    cap_and_crown()
    tentacles()
    interior()
    surroundings()
    lights()
    B.vines(SPIKE_TIP, WALL_BOTTOM + 2, fraction=12)
    B.taper()


blocks = B.blocks


def main():
    build()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    n, p, top = B.save(OUT)
    print(f"{n} blocks, {p} block types, size {SIZE}x{top}x{SIZE}")


if __name__ == "__main__":
    main()
