"""
Generates the Ithk Amethyst Tower structure (based on the reference sheet).

    python tools/make_amethyst_tower.py

Writes:
    src/main/resources/data/childrenofmu/structure/amethyst_tower.nbt

Layout, bottom to top (y = 0 is the ground):
    mud and red plants on the ground  ->  a gap of open air  ->  inverted crystal spike  ->  stone bowl
    ->  lower floor  ->  balcony  ->  middle floor with tall windows  ->  eaves with hanging crystals
    ->  open-sided top room where the Ithk live  ->  amethyst spire
Tweak the numbers in the SECTIONS block below and re-run.
"""
import math
import os
import random

from ithk_build import Builder
from nbt_structure import Byte

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure", "amethyst_tower.nbt")

SIZE = 37                 # footprint is SIZE x SIZE
C = SIZE // 2             # center column
B = Builder(SIZE, seed=7)
rng = B.rng

# ---------------------------------------------------------------- SECTIONS (y levels)
GROUND_RADIUS = 14
SPIKE_TIP = 7             # lowest point of the tower: the gap below it is open air
BOWL_BOTTOM, BOWL_TOP = 16, 23
LOWER_FLOOR, LOWER_TOP = 24, 31
BALCONY = 32
MIDDLE_TOP = 46
EAVES = 47
TOP_FLOOR, TOP_ROOM_TOP = 48, 54
ROOF_BOTTOM, ROOF_TOP = 55, 80
LOWER_R, MIDDLE_R, TOP_R, LEDGE_R = 8, 7, 7, 10

# ---------------------------------------------------------------- palette
STONE = ["minecraft:stone_bricks"] * 5 + ["minecraft:cracked_stone_bricks", "minecraft:tuff_bricks", "minecraft:polished_tuff"]
TRIM = "minecraft:polished_deepslate"
TRIM2 = "minecraft:deepslate_tiles"
AMETHYST = "minecraft:amethyst_block"
GLASS = "minecraft:purple_stained_glass"
FLOOR = "minecraft:polished_deepslate"
LIGHT = ("minecraft:pearlescent_froglight", {"axis": "y"})

blocks = B.blocks
entities = B.entities


def put(x, y, z, name, props=None, replace=True):
    if not (0 <= x < SIZE and 0 <= z < SIZE and y >= 0):
        return
    if not replace and (x, y, z) in blocks:
        return
    blocks[(x, y, z)] = (name, props or {})


def air(x, y, z):
    put(x, y, z, "minecraft:air")


def stone():
    return rng.choice(STONE)


def oct_in(dx, dz, r):
    """Octagonal footprint of 'radius' r."""
    dx, dz = abs(dx), abs(dz)
    return dx <= r and dz <= r and dx + dz <= r * 1.4


def ring(dx, dz, r, thick=1):
    return oct_in(dx, dz, r) and not oct_in(dx, dz, r - thick)


def is_corner(dx, dz, r):
    """The 8 'corner' columns of an octagon wall, where pillars go."""
    dx, dz = abs(dx), abs(dz)
    return ring(dx, dz, r) and (abs(dx - dz) <= 0 or dx == 0 or dz == 0) and max(dx, dz) >= r - 1 and (dx == dz or dx == 0 or dz == 0)


def cluster(x, y, z, facing, size="amethyst_cluster"):
    B.cluster(x, y, z, facing, size)


def each(r):
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if oct_in(dx, dz, r):
                yield dx, dz


# ---------------------------------------------------------------- ground
def ground():
    B.ground(GROUND_RADIUS)


# ---------------------------------------------------------------- base: spike + bowl
def base():
    # Inverted crystal spike.
    for y in range(SPIKE_TIP, BOWL_BOTTOM):
        t = (y - SPIKE_TIP) / (BOWL_BOTTOM - SPIKE_TIP)
        r = int(round(t * t * 3.5))
        for dx, dz in each(r):
            put(C + dx, y, C + dz, AMETHYST)
    cluster(C, SPIKE_TIP - 1, C, "down")
    for y, r in ((SPIKE_TIP + 3, 1), (SPIKE_TIP + 5, 1), (SPIKE_TIP + 7, 2)):
        for (fx, fz, f) in ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north")):
            cluster(C + fx * (r + 1), y, C + fz * (r + 1), f)

    # Stone bowl, widening upward; hollow inside.
    for y in range(BOWL_BOTTOM, BOWL_TOP + 1):
        r = int(round(4 + (y - BOWL_BOTTOM) * 6 / (BOWL_TOP - BOWL_BOTTOM)))
        for dx, dz in each(r):
            shell = not oct_in(dx, dz, r - 2) or y == BOWL_BOTTOM
            if shell:
                name = TRIM if y in (BOWL_BOTTOM + 3, BOWL_TOP) and not oct_in(dx, dz, r - 1) else stone()
                put(C + dx, y, C + dz, name)
            else:
                air(C + dx, y, C + dz)
        # Amethyst veins running down the bowl at the 4 diagonals.
        for sx, sz in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
            d = int(r / 1.4 / 1.0 * 0.7)
            put(C + sx * d, y, C + sz * d, AMETHYST)


# ---------------------------------------------------------------- walls
def walls(y0, y1, r, window_rows, window_half_width, arch=False, open_windows=False):
    for y in range(y0, y1 + 1):
        for dx, dz in each(r):
            x, z = C + dx, C + dz
            if not ring(dx, dz, r):
                air(x, y, z)
                continue
            adx, adz = abs(dx), abs(dz)
            v = int(round(r * 0.4))  # where the flat faces meet the angled ones
            pillar = (adx == r and adz == v) or (adz == r and adx == v)
            frame = ((adz == r and adx == window_half_width + 1) or (adx == r and adz == window_half_width + 1))
            # Windows centered on the four flat sides.
            on_face = (adz == r and adx <= window_half_width) or (adx == r and adz <= window_half_width)
            in_rows = window_rows[0] <= y <= window_rows[1]
            if arch and y == window_rows[1] + 1:
                on_face = (adz == r and adx <= window_half_width - 1) or (adx == r and adz <= window_half_width - 1)
                in_rows = True
            if on_face and in_rows:
                if open_windows:
                    air(x, y, z)
                else:
                    put(x, y, z, GLASS)
            elif pillar:
                put(x, y, z, AMETHYST)  # amethyst pillars where the wall turns
            elif frame and window_rows[0] - 1 <= y <= window_rows[1] + 2:
                put(x, y, z, TRIM)      # dark frames beside the windows
            else:
                put(x, y, z, stone())


def floor(y, r, hole=1):
    for dx, dz in each(r):
        if max(abs(dx), abs(dz)) <= hole:
            air(C + dx, y, C + dz)  # a shaft so flying visitors (and the Ithk) can pass between floors
        else:
            put(C + dx, y, C + dz, FLOOR)
            B.no_taper.add((C + dx, y, C + dz))


def band(y, r, name):
    for dx, dz in each(r):
        if ring(dx, dz, r):
            put(C + dx, y, C + dz, name)


def ledge(y, r_in, r_out):
    for dx, dz in each(r_out):
        if not oct_in(dx, dz, r_in - 1):
            edge = ring(dx, dz, r_out)
            put(C + dx, y, C + dz, TRIM if edge else stone())


# ---------------------------------------------------------------- hanging and floating crystals
def pendant(x, y_top, z, chain_len):
    B.pendant(x, y_top, z, chain_len)


def floating_crystal(cx, cy, cz, r=2):
    B.floating_crystal(cx, cy, cz, r)


# ---------------------------------------------------------------- roof
def roof_radius(y):
    t = (y - ROOF_BOTTOM) / (ROOF_TOP - ROOF_BOTTOM)
    return int(round(8 * (1 - t) ** 0.8))


def roof():
    height = ROOF_TOP - ROOF_BOTTOM
    for y in range(ROOF_BOTTOM, ROOF_TOP + 1):
        t = (y - ROOF_BOTTOM) / height
        r = roof_radius(y)
        skirt = (y - ROOF_BOTTOM) % 6 == 0 and r >= 3
        if skirt:
            r += 1  # a flared tier every 5 blocks, like stacked roofs
        for dx, dz in each(r):
            x, z = C + dx, C + dz
            if not oct_in(dx, dz, r - 1) or r <= 1:
                ridge = (abs(dx) == abs(dz) or dx == 0 or dz == 0) and r > 2
                if y == ROOF_BOTTOM or skirt:
                    put(x, y, z, TRIM)
                elif ridge:
                    put(x, y, z, TRIM2)
                else:
                    put(x, y, z, AMETHYST)
            else:
                air(x, y, z)
    # Crystal spikes along the roof.
    for y in range(ROOF_BOTTOM + 1, ROOF_TOP - 3, 6):
        r = roof_radius(y) + 1
        for fx, fz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            cluster(C + fx * (r + 1), y, C + fz * (r + 1), "up")
    put(C, ROOF_TOP + 1, C, "minecraft:end_rod", {"facing": "up"})
    put(C, ROOF_TOP + 2, C, "minecraft:end_rod", {"facing": "up"})

    # Four small spires standing on the eaves.
    for fx, fz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        sx, sz = C + fx * (LEDGE_R - 1), C + fz * (LEDGE_R - 1)
        for i, r in enumerate((1, 1, 1, 0, 0, 0)):
            for dx, dz in each(r):
                put(sx + dx, EAVES + 2 + i, sz + dz, AMETHYST if r == 0 or (dx == 0 and dz == 0) else TRIM2)
        cluster(sx, EAVES + 8, sz, "up")


# ---------------------------------------------------------------- interior
def interior():
    # Lower floor: an archive.
    r = LOWER_R
    for dx, dz in each(r - 1):
        x, z = C + dx, C + dz
        if ring(dx, dz, r - 1):
            for y in range(LOWER_FLOOR + 1, LOWER_FLOOR + 4):
                if not ((abs(dz) == r - 1 and abs(dx) <= 1) or (abs(dx) == r - 1 and abs(dz) <= 1)):
                    put(x, y, z, "minecraft:bookshelf")
        elif rng.random() < 0.25 and max(abs(dx), abs(dz)) > 2:
            put(x, LOWER_FLOOR + 1, z, "minecraft:purple_carpet")
    put(C + 3, LOWER_FLOOR + 1, C, "minecraft:lectern", {"facing": "west", "has_book": "false", "powered": "false"})
    put(C - 3, LOWER_FLOOR + 1, C, "minecraft:lectern", {"facing": "east", "has_book": "false", "powered": "false"})
    for dx, dz in ((3, 3), (-3, 3), (3, -3), (-3, -3)):
        put(C + dx, LOWER_TOP, C + dz, *LIGHT)

    # Middle floor: a crystal garden.
    for dx, dz in each(MIDDLE_R - 2):
        if rng.random() < 0.15 and max(abs(dx), abs(dz)) > 2:
            cluster(C + dx, BALCONY + 1, C + dz, "up", rng.choice(["amethyst_cluster", "large_amethyst_bud", "medium_amethyst_bud"]))
    put(C + 3, BALCONY + 1, C + 3, "minecraft:enchanting_table")
    for dx, dz in ((3, 0), (-3, 0), (0, 3), (0, -3)):
        put(C + dx, MIDDLE_TOP, C + dz, *LIGHT)

    # Top room, where the Ithk dwell: a focus crystal hovering high enough to float up beneath it.
    FOCUS = TOP_FLOOR + 5  # its center
    for y, r in ((FOCUS - 1, 0), (FOCUS, 1), (FOCUS + 1, 0)):
        for dx, dz in each(r):
            put(C + dx, y, C + dz, AMETHYST)
    put(C, FOCUS, C, *LIGHT)
    for dx, dz in ((4, 0), (-4, 0), (0, 4), (0, -4)):
        put(C + dx, TOP_ROOM_TOP, C + dz, *LIGHT)
    for dx, dz in ((3, 3), (-3, 3), (3, -3), (-3, -3)):
        cluster(C + dx, TOP_FLOOR + 1, C + dz, "up")

    # The Ithk.
    for dx, dz in ((3, 0), (-3, 0), (0, 3), (0, -3)):
        entities.append((C + dx + 0.5, TOP_FLOOR + 1.5, C + dz + 0.5,
                         {"id": "childrenofmu:ithk", "PersistenceRequired": Byte(1)}))


# ---------------------------------------------------------------- vines
def vines():
    B.vines(SPIKE_TIP, BALCONY)


def build():
    ground()
    base()
    floor(LOWER_FLOOR, LOWER_R, hole=1)
    walls(LOWER_FLOOR + 1, LOWER_TOP, LOWER_R, (LOWER_FLOOR + 3, LOWER_FLOOR + 5), 1)
    band(LOWER_TOP, LOWER_R, TRIM)
    ledge(BALCONY, MIDDLE_R, LEDGE_R)
    floor(BALCONY, MIDDLE_R - 1, hole=1)
    walls(BALCONY + 1, MIDDLE_TOP, MIDDLE_R, (BALCONY + 3, BALCONY + 10), 1, arch=True)
    band(BALCONY + 7, MIDDLE_R, TRIM)
    ledge(EAVES, MIDDLE_R, LEDGE_R)
    floor(TOP_FLOOR, TOP_R, hole=0)
    ledge(TOP_FLOOR, TOP_R, LEDGE_R - 1)
    walls(TOP_FLOOR + 1, TOP_ROOM_TOP, TOP_R, (TOP_FLOOR + 1, TOP_ROOM_TOP - 2), 2, arch=True, open_windows=True)
    roof()

    # Crystals hanging from the balcony and eaves at the 8 points of the octagon.
    for r, y in ((LEDGE_R, BALCONY - 1), (LEDGE_R, EAVES - 1)):
        for ang in range(8):
            a = ang * math.pi / 4
            x = C + int(round(math.cos(a) * (r - 0.5)))
            z = C + int(round(math.sin(a) * (r - 0.5)))
            pendant(x, y, z, rng.randint(2, 4))
    # Crystals hanging from the bowl's rim.
    for ang in range(4):
        a = ang * math.pi / 2 + math.pi / 4
        pendant(C + int(round(math.cos(a) * 7)), BOWL_TOP - 3, C + int(round(math.sin(a) * 7)), 3)
    # Floating crystals.
    for ang in range(4):
        a = ang * math.pi / 2 + math.pi / 4
        floating_crystal(C + int(round(math.cos(a) * 15)), 20 + ang % 2 * 3, C + int(round(math.sin(a) * 15)))
        floating_crystal(C + int(round(math.cos(a + math.pi / 4) * 15)), 38 + ang % 2 * 2,
                         C + int(round(math.sin(a + math.pi / 4) * 15)), r=1)

    interior()
    lights()
    vines()
    B.taper()


def lights():
    """Lanterns, candles and end rods so the tower glows at night."""
    # Lanterns hanging under the balcony and eaves, between the crystal pendants.
    for y in (BALCONY - 1, EAVES - 1):
        for ang in range(8):
            a = ang * math.pi / 4 + math.pi / 8
            x = C + int(round(math.cos(a) * (LEDGE_R - 0.8)))
            z = C + int(round(math.sin(a) * (LEDGE_R - 0.8)))
            if B.name_at(x, y + 1, z) and B.name_at(x, y, z) is None:
                B.lantern(x, y, z, chain_len=1, soul=(y == EAVES - 1))
    # Candles along the inner edge of the balcony and eaves.
    for y, r in ((BALCONY, MIDDLE_R + 1), (EAVES, MIDDLE_R + 1)):
        for ang in range(8):
            a = ang * math.pi / 4 + math.pi / 8
            x, z = C + int(round(math.cos(a) * r)), C + int(round(math.sin(a) * r))
            if B.name_at(x, y, z) and B.name_at(x, y + 1, z) is None:
                B.candle(x, y + 1, z, count=rng.randint(2, 4))
    # End rods standing on the corners of each tier of the spire.
    for y in range(ROOF_BOTTOM, ROOF_TOP, 6):
        for ang in range(8):
            a = ang * math.pi / 4
            for d in range(12, 0, -1):
                x, z = C + int(round(math.cos(a) * d)), C + int(round(math.sin(a) * d))
                if B.name_at(x, y, z) not in (None, "minecraft:air"):
                    B.end_rod(x, y + 1, z)
                    break
    # Glowing panes in the underside of the bowl.
    for dx, dz in ((2, 0), (-2, 0), (0, 2), (0, -2)):
        B.put(C + dx, BOWL_BOTTOM, C + dz, *LIGHT, keep_shape=True)
    # Spires on the eaves get an end rod on top of their crystal.
    for fx, fz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        B.end_rod(C + fx * (LEDGE_R - 1), EAVES + 9, C + fz * (LEDGE_R - 1))


def main():
    build()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    n, p, top = B.save(OUT)
    print(f"{n} blocks, {p} block types, {len(entities)} Ithk, size {SIZE}x{top}x{SIZE}")
    return blocks


if __name__ == "__main__":
    main()
