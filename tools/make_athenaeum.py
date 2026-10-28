"""
Generates the Athenaeum: two amethyst pyramids joined base to base, cut through the middle by a glowing emerald
band, holding a single library room. A diamond-shaped window in the front opens onto the books.

    python tools/make_athenaeum.py

Writes src/main/resources/data/childrenofmu/structure/athenaeum.nbt

The pyramids' faces drop exactly one block per block of height, so the stair tapering turns them into smooth
45-degree slopes. (Turning the square 45 degrees would make every face a jagged diagonal staircase in blocks.)
"""
import math
import os

from ithk_build import AIR, AMETHYST, GLOW, Builder

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure", "athenaeum.nbt")

SIZE = 41
B = Builder(SIZE, seed=23)
C = B.c
rng = B.rng

# ---------------------------------------------------------------- dimensions (y levels; 0 = ground)
GROUND_RADIUS = 10
SPIKE_TIP = 8
R = 10                     # half-width of the pyramids' base: 21 blocks across (the band is 23)
STEEP = 1.45               # blocks of height per block the faces step in (1 = 45 degrees; higher = taller, slimmer)
PYRAMID = int(math.ceil((R + 1) * STEEP))   # each pyramid's height above/below the band
MID = SPIKE_TIP + 5 + PYRAMID + 1
ROOM = 5                   # the library: walls 5 from the center, a 9x9 room
ROOM_FLOOR, ROOM_CEIL = MID - 4, MID + 6
WINDOW_CENTER, WINDOW_R = MID + 3, 2

# ---------------------------------------------------------------- palette
EMERALD = "childrenofmu:inlaid_emerald"
BAND_GLOW = ("minecraft:verdant_froglight", {"axis": "y"})
FLOOR = ["minecraft:deepslate_tiles"] * 3 + ["minecraft:polished_deepslate"]


def r_at(y):
    """Half-width of the building at height y."""
    d = abs(y - MID)
    if d <= 1:
        return R + 1                     # the band
    return R - int((d - 2) / STEEP)      # the pyramids, stepping in one block every STEEP blocks


def inside(dx, y, dz):
    r = r_at(y)
    return r >= 0 and abs(dx) <= r and abs(dz) <= r


def in_room(dx, y, dz):
    return ROOM_FLOOR < y < ROOM_CEIL and abs(dx) < ROOM and abs(dz) < ROOM


def in_window(dx, y, dz):
    # A diamond-shaped opening from the library out through the front face.
    return dz <= -ROOM and abs(dx) + abs(y - WINDOW_CENTER) <= WINDOW_R


def window_frame(dx, y, dz):
    return dz <= -ROOM and abs(dx) + abs(y - WINDOW_CENTER) == WINDOW_R + 1


def in_v(dx, y, dz):
    """The green V dropping from the band down the front face."""
    depth = MID - 2 - y
    return dz < 0 and 0 <= depth <= 7 and abs(dx) == 5 - int(depth / STEEP)


def shell():
    for y in range(MID - PYRAMID - 1, MID + PYRAMID + 2):
        r = r_at(y)
        if r < 0:
            continue
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                x, z = C + dx, C + dz
                if in_room(dx, y, dz) or in_window(dx, y, dz):
                    B.air(x, y, z)
                    continue
                surface = any(not inside(dx + a, y + b, dz + c) for a, b, c in
                              ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)))
                if abs(y - MID) <= 1:
                    glow = surface and y == MID and (dx + dz) % 4 == 0
                    B.put(x, y, z, *(BAND_GLOW if glow else (EMERALD, {})), keep_shape=True)
                elif surface and (window_frame(dx, y, dz) or (in_v(dx, y, dz) and abs(dz) == r)):
                    B.put(x, y, z, EMERALD, keep_shape=True)
                else:
                    B.put(x, y, z, AMETHYST)


def surface_crystals():
    """Only a light frost of clusters under the lower pyramid, so the faces stay clean."""
    for (x, y, z), b in list(B.blocks.items()):
        if b[0] == AMETHYST and y < MID - 2 and (x, y - 1, z) not in B.blocks and rng.random() < 0.05:
            B.cluster(x, y - 1, z, "down", "amethyst_cluster")


def tips():
    top = MID + PYRAMID + 1
    B.spike(C, top + 1, C, 7, base_r=1, tip=False)
    B.end_rod(C, top + 8, C)
    bottom = MID - PYRAMID - 1
    B.spike(C, bottom - 1, C, bottom - SPIKE_TIP, base_r=1, up=False)
    # Crystal shards pointing out from the band's four corners.
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        for k in range(1, 3):
            B.put(C + sx * (R + 1 + k), MID, C + sz * (R + 1 + k), AMETHYST, keep_shape=True)
        B.end_rod(C + sx * (R + 3), MID + 1, C + sz * (R + 3))


def library():
    """
    The library, entered through the window in the north wall:
      - a carpeted aisle runs from the window to an enchanting table at the back, which the shelves power;
      - two reading lecterns face each other across the aisle, each lit by a hanging lantern;
      - a crystal pillar stands in each corner, with glowing bands;
      - shelves line every wall, floor to ceiling.
    """
    r = ROOM
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            edge = max(abs(dx), abs(dz))
            name = AMETHYST if edge == r - 1 and (dx + dz) % 2 == 0 else rng.choice(FLOOR)
            B.put(C + dx, ROOM_FLOOR, C + dz, name, keep_shape=True)
            B.put(C + dx, ROOM_CEIL, C + dz, AMETHYST, keep_shape=True)
    B.put(C, ROOM_CEIL, C, *GLOW, keep_shape=True)
    # Shelf walls.
    for y in range(ROOM_FLOOR + 1, ROOM_CEIL):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if max(abs(dx), abs(dz)) == r and not in_window(dx, y, dz):
                    corner = abs(dx) == r and abs(dz) == r
                    B.put(C + dx, y, C + dz, AMETHYST if corner else "minecraft:bookshelf", keep_shape=True)
    # Four corner pillars.
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        for y in range(ROOM_FLOOR + 1, ROOM_CEIL):
            band = (y - ROOM_FLOOR) % 3 == 0
            B.put(C + sx * (r - 1), y, C + sz * (r - 1), *(GLOW if band else (AMETHYST, {})), keep_shape=True)
    # The aisle, from the window (north) to the enchanting table (south).
    for dz in range(-r + 1, 3):
        B.put(C, ROOM_FLOOR + 1, C + dz, "minecraft:purple_carpet")
    B.put(C, ROOM_FLOOR + 1, C + 3, "minecraft:enchanting_table")
    B.candle(C - 1, ROOM_FLOOR + 1, C + 3, count=3)
    B.candle(C + 1, ROOM_FLOOR + 1, C + 3, count=3)
    # Reading lecterns facing each other across the aisle.
    B.put(C - 2, ROOM_FLOOR + 1, C - 1, "minecraft:lectern", {"facing": "east", "has_book": "false", "powered": "false"})
    B.put(C + 2, ROOM_FLOOR + 1, C - 1, "minecraft:lectern", {"facing": "west", "has_book": "false", "powered": "false"})
    B.lantern(C - 2, ROOM_CEIL - 1, C - 1, chain_len=2)
    B.lantern(C + 2, ROOM_CEIL - 1, C - 1, chain_len=2)
    B.lantern(C, ROOM_CEIL - 1, C + 3, chain_len=3)
    # The keepers of the library.
    B.ithk(C - 2, ROOM_FLOOR + 2, C + 2, Profession="archivist")
    B.ithk(C + 2, ROOM_FLOOR + 2, C + 2, Profession="archivist")


def surroundings():
    # Glowing crystals hanging beneath the band, at its corners and the middle of each side.
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1), (1, 0), (-1, 0), (0, 1), (0, -1)):
        d = R + 1 if sx and sz else R + 1
        x, z = C + sx * d, C + sz * d
        if (x, MID - 2, z) not in B.blocks:
            B.pendant(x, MID - 2, z, rng.randint(2, 3))
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        r = 15 + (i % 2)
        B.floating_crystal(C + int(round(math.cos(a) * r)), MID - 4 - (i * 5) % 12,
                           C + int(round(math.sin(a) * r)), r=1)


def build():
    B.ground(GROUND_RADIUS)
    shell()
    library()
    tips()
    surface_crystals()
    surroundings()
    B.vines(SPIKE_TIP, MID - 3, fraction=30)
    B.taper()


blocks = B.blocks


def main():
    build()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    n, p, top = B.save(OUT)
    print(f"{n} blocks, {p} block types, size {SIZE}x{top}x{SIZE}")


if __name__ == "__main__":
    main()
