"""
Generates the Hot Fount: a small pyramid hanging in the air, pouring a stream of lava down into a wider,
upside-down pyramid that holds a lava pool. The stream is open to view in the gap between them.

    python tools/make_hot_fount.py

Writes src/main/resources/data/childrenofmu/structure/hot_fount.nbt
"""
import math
import os

from ithk_build import AIR, AMETHYST, GLOW, Builder

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure", "hot_fount.nbt")

SIZE = 15
B = Builder(SIZE, seed=41)
C = B.c
rng = B.rng

# ---------------------------------------------------------------- dimensions (y levels; 0 = ground)
TIP = 3                     # point of the lower pyramid
LOWER_R, LOWER_STEEP = 5, 1.3
POOL_TOP = TIP + int(math.ceil(LOWER_R * LOWER_STEEP))   # the lava's surface
RIM = POOL_TOP + 1
GAP = 4                     # open air the stream falls through
UPPER_R, UPPER_STEEP = 3, 1.8   # the upper pyramid is smaller and steeper than the lower one
UPPER_BASE = RIM + GAP + 1

BRICK = ["minecraft:deepslate_bricks"] * 4 + ["minecraft:deepslate_tiles"]
LAVA = ("minecraft:lava", {"level": "0"})
FALLING = ("minecraft:lava", {"level": "8"})
MAGMA = "minecraft:magma_block"


def brick():
    return rng.choice(BRICK)


def face_block(dx, dz, r):
    """Crystal along the pyramid's four edges, brick on its faces."""
    return AMETHYST if abs(dx) == abs(dz) == r else brick()


def lower():
    for y in range(TIP, POOL_TOP + 1):
        r = min(LOWER_R, int((y - TIP) / LOWER_STEEP))
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                ring = max(abs(dx), abs(dz))
                if y == POOL_TOP and ring <= LOWER_R - 2:
                    B.put(C + dx, y, C + dz, *LAVA)
                elif y == POOL_TOP - 1 and ring <= LOWER_R - 3:
                    B.put(C + dx, y, C + dz, *LAVA)
                elif (y == POOL_TOP and ring == LOWER_R - 1) or (y == POOL_TOP - 1 and ring == LOWER_R - 2):
                    B.put(C + dx, y, C + dz, MAGMA, keep_shape=True)   # the pool glows hot at its edges
                else:
                    B.put(C + dx, y, C + dz, face_block(dx, dz, r))
    # A raised rim around the pool.
    for dx in range(-LOWER_R, LOWER_R + 1):
        for dz in range(-LOWER_R, LOWER_R + 1):
            ring = max(abs(dx), abs(dz))
            if ring >= LOWER_R - 1:
                corner = abs(dx) == abs(dz) == LOWER_R
                B.put(C + dx, RIM, C + dz, AMETHYST if corner else "minecraft:polished_deepslate")
    B.cluster(C, TIP - 1, C, "down")


def upper():
    y = UPPER_BASE
    while True:
        r = UPPER_R - int((y - UPPER_BASE) / UPPER_STEEP)
        if r < 0:
            break
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                B.put(C + dx, y, C + dz, face_block(dx, dz, r), keep_shape=(y == UPPER_BASE))
        y += 1
    B.end_rod(C, y, C)
    # The spring: a lava source set into the underside, pouring straight down into the pool.
    B.put(C, UPPER_BASE, C, *LAVA)
    for y in range(RIM, UPPER_BASE):
        B.put(C, y, C, *FALLING)
    # Glowing corners on the underside, and crystal points hanging from them.
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        B.put(C + sx * UPPER_R, UPPER_BASE, C + sz * UPPER_R, *GLOW, keep_shape=True)
        B.cluster(C + sx * UPPER_R, UPPER_BASE - 1, C + sz * UPPER_R, "down", "large_amethyst_bud")


def surroundings():
    # Glowing crystals hanging from the lower pyramid's corners.
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        B.pendant(C + sx * LOWER_R, POOL_TOP - 1, C + sz * LOWER_R, 1)


def build():
    lower()
    upper()
    surroundings()
    B.taper()


blocks = B.blocks


def main():
    build()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    n, p, top = B.save(OUT)
    print(f"{n} blocks, {p} block types, size {SIZE}x{top}x{SIZE}")


if __name__ == "__main__":
    main()
