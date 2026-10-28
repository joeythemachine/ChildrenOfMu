"""
Generates the Athanor, home of the Transmuters: a floating tower in an ancient-Egyptian manner. An upside-down
pyramid carries a platform with four arms; on it stands a square, columned hall with ankhs on its walls and a flared
cornice, reached by a stair to its glowing doorway; above it a pyramid floats, joined to the roof by a column of light.
(An athanor is the alchemist's furnace, the slow fire in which matter is transmuted.)

    python tools/make_athanor.py

Writes src/main/resources/data/childrenofmu/structure/athanor.nbt
"""
import math
import os

from ithk_build import AIR, AMETHYST, GLOW, Builder

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure", "athanor.nbt")

SIZE = 25
B = Builder(SIZE, seed=97)
C = B.c
rng = B.rng

# ---------------------------------------------------------------- dimensions (y levels; 0 = ground)
TIP = 3
BASE_R = 7
PLATFORM = TIP + 8                 # 11: the platform the hall stands on
ARM = 10                           # how far the four arms reach
HALL_R = 5                         # the hall: 11 x 11
FLOOR = PLATFORM + 5               # 16: the room's floor, up a stair
ROOM_TOP = FLOOR + 11              # 27: the room's ceiling
CORNICE = ROOM_TOP + 1             # 28
ROOF = CORNICE + 2                 # 30
PYRAMID = ROOF + 9                 # 39: base of the floating pyramid
PYR_R = 5

PURPUR = "minecraft:purpur_block"
PILLAR = ("minecraft:purpur_pillar", {"axis": "y"})
ENDSTONE = "minecraft:end_stone_bricks"
CALCITE = "minecraft:calcite"
GLASS = "minecraft:purple_stained_glass"

ANKH = ["..X..",
        ".X.X.",
        ".X.X.",
        "..X..",
        "XXXXX",
        "..X..",
        "..X..",
        "..X.."]


def wall_block():
    return AMETHYST if rng.random() < 0.18 else PURPUR


def base():
    """The upside-down pyramid under the platform, purpur with amethyst edges."""
    for y in range(TIP, PLATFORM):
        r = min(BASE_R, int((y - TIP) * 1.0))
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                edge = abs(dx) == abs(dz) == r
                B.put(C + dx, y, C + dz, AMETHYST if edge or rng.random() < 0.3 else PURPUR)
    B.spike(C, TIP - 1, C, 3, base_r=0, up=False)


def platform():
    for dx in range(-ARM, ARM + 1):
        for dz in range(-ARM - 1, ARM + 1):     # the front arm reaches one further, to carry the stair
            ring = max(abs(dx), abs(dz))
            arm = min(abs(dx), abs(dz)) <= 1 and (ring <= ARM or (dz < 0 and abs(dx) <= 1))
            if ring <= BASE_R + 1 or arm:
                border = ring == BASE_R + 1 or (arm and ring > BASE_R + 1 and (min(abs(dx), abs(dz)) == 1 or ring == ARM))
                B.put(C + dx, PLATFORM, C + dz, ENDSTONE if border else PURPUR, keep_shape=True)
    # Posts at the tips of the side and back arms, each hung with a lantern.
    for sx, sz in ((1, 0), (-1, 0), (0, 1)):
        x, z = C + sx * ARM, C + sz * ARM
        for y in range(PLATFORM + 1, PLATFORM + 4):
            B.put(x, y, z, *PILLAR, keep_shape=True)
        B.put(x, PLATFORM + 4, z, ENDSTONE, keep_shape=True)
        B.cluster(x, PLATFORM + 5, z, "up")
        ox, oz = (sz, sx)   # hang lanterns to either side of the post
        for k in (-1, 1):
            B.put(x + ox * k, PLATFORM + 4, z + oz * k, ENDSTONE, keep_shape=True)
            B.lantern(x + ox * k, PLATFORM + 3, z + oz * k, chain_len=1)


def hall():
    r = HALL_R
    # The solid plinth under the room.
    for y in range(PLATFORM + 1, FLOOR + 1):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                B.put(C + dx, y, C + dz, ENDSTONE if y == FLOOR and max(abs(dx), abs(dz)) == r else PURPUR,
                      keep_shape=(y == FLOOR))
    # Walls.
    for y in range(FLOOR + 1, ROOM_TOP + 1):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if max(abs(dx), abs(dz)) == r:
                    B.put(C + dx, y, C + dz, wall_block(), keep_shape=True)
                else:
                    B.air(C + dx, y, C + dz)
    # Columns standing proud of the walls: at the corners and either side of each face's middle.
    for side in ("north", "south", "east", "west"):
        for u in (-r, -2, 2, r):
            x, z = face_xy(side, u, r + 1)
            if side == "north" and abs(u) == 2:
                continue                                                            # the stair is here
            B.put(x, PLATFORM + 1, z, ENDSTONE, keep_shape=True)                    # base
            for y in range(PLATFORM + 2, ROOM_TOP):
                B.put(x, y, z, *PILLAR, keep_shape=True)
            B.put(x, ROOM_TOP, z, ENDSTONE, keep_shape=True)                        # capital
    # Ankhs on the side and back walls.
    for side in ("south", "east", "west"):
        draw_ankh(side, 0, FLOOR + 1)
    # The doorway on the front: 3 wide, 5 tall, framed in crystal, an ankh above it.
    for u in range(-2, 3):
        for y in range(FLOOR + 1, FLOOR + 7):
            x, z = face_xy("north", u, r)
            inside = abs(u) <= 1 and y <= FLOOR + 5
            B.put(x, y, z, AIR if inside else AMETHYST, keep_shape=True)
    x, z = face_xy("north", 0, r)
    B.put(x, FLOOR + 7, z, ENDSTONE, keep_shape=True)
    B.put(x, FLOOR + 8, z, CALCITE, keep_shape=True)


def face_xy(side, u, dist):
    """A position on one face: u runs along the face, dist is how far out from the center."""
    return {"north": (C + u, C - dist), "south": (C - u, C + dist),
            "east": (C + dist, C + u), "west": (C - dist, C - u)}[side]


def draw_ankh(side, u0, y0):
    for row, line in enumerate(ANKH):
        for col, ch in enumerate(line):
            if ch == "X":
                x, z = face_xy(side, u0 + col - 2, HALL_R)
                B.put(x, y0 + len(ANKH) - row, z, ENDSTONE, keep_shape=True)


def stair():
    """The front stair, climbing from the platform's front arm up to the doorway."""
    steps = FLOOR - PLATFORM
    for k in range(steps):
        y = PLATFORM + 1 + k
        z = C - HALL_R - steps + k
        for dx in (-1, 0, 1):
            B.put(C + dx, y, z, "minecraft:purpur_stairs", {"facing": "south", "half": "bottom", "shape": "straight",
                                                             "waterlogged": "false"})
            for yy in range(PLATFORM + 1, y):
                B.put(C + dx, yy, z, PURPUR, keep_shape=True)
        for dx in (-2, 2):
            B.put(C + dx, y, z, ENDSTONE, keep_shape=True)                          # low walls each side
            for yy in range(PLATFORM + 1, y):
                B.put(C + dx, yy, z, PURPUR, keep_shape=True)
    # The landing at the top, in front of the door.
    for dx in range(-2, 3):
        B.put(C + dx, FLOOR, C - HALL_R - 1, ENDSTONE if abs(dx) == 2 else PURPUR, keep_shape=True)


def room():
    r = HALL_R - 1
    # The floor: a transmutation circle of calcite and crystal around the athanor itself.
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            d = math.hypot(dx, dz)
            name = CALCITE if 2.6 < d < 3.5 else (AMETHYST if abs(dx) == abs(dz) and d < 2 else PURPUR)
            B.put(C + dx, FLOOR, C + dz, name, keep_shape=True)
    # The athanor: a furnace of end stone around a cauldron of lava, on a crystal dais.
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx or dz:
                B.put(C + dx, FLOOR + 1, C + dz, "minecraft:end_stone_brick_slab", {"type": "bottom", "waterlogged": "false"})
    B.put(C, FLOOR + 1, C, "minecraft:lava_cauldron")
    B.put(C, FLOOR, C, GLOW[0], GLOW[1], keep_shape=True)
    # Alembics: brewing stands on pedestals in the corners, with candles.
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        x, z = C + sx * (r - 1), C + sz * (r - 1)
        B.put(x, FLOOR + 1, z, ENDSTONE, keep_shape=True)
        B.put(x, FLOOR + 2, z, "minecraft:brewing_stand", {"has_bottle_0": "false", "has_bottle_1": "false",
                                                           "has_bottle_2": "false"})
        B.candle(C + sx * r, FLOOR + 1, C + sz * (r - 2), count=3)
    # A glowing panel on the back wall, opposite the door.
    for u in range(-1, 2):
        for y in range(FLOOR + 2, FLOOR + 6):
            x, z = face_xy("south", u, HALL_R)
            B.put(x, y, z, GLASS)
            bx, bz = face_xy("south", u, HALL_R + 1)
            B.put(bx, y, bz, *GLOW, keep_shape=True)
    # Ceiling with lights.
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            B.put(C + dx, ROOM_TOP, C + dz, GLOW[0] if (dx, dz) in ((2, 2), (-2, 2), (2, -2), (-2, -2)) else PURPUR,
                  GLOW[1] if (dx, dz) in ((2, 2), (-2, 2), (2, -2), (-2, -2)) else None, keep_shape=True)
    B.lantern(C, ROOM_TOP - 1, C, chain_len=3)
    # The Transmuters.
    B.ithk(C - 2, FLOOR + 3, C + 1, Profession="transmuter")
    B.ithk(C + 2, FLOOR + 3, C + 1, Profession="transmuter")


def cornice_and_roof():
    # The flared Egyptian cornice: a band of end stone and calcite (the "hieroglyph" frieze) under an overhang.
    for y, r in ((CORNICE, HALL_R + 1), (CORNICE + 1, HALL_R + 2)):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if max(abs(dx), abs(dz)) == r or y == CORNICE + 1:
                    if y == CORNICE:
                        k = (dx + dz) % 4
                        name = ENDSTONE if k in (0, 1) else (CALCITE if k == 2 else PURPUR)
                    else:
                        name = PURPUR
                    B.put(C + dx, y, C + dz, name, keep_shape=(y == CORNICE))
    for dx in range(-HALL_R, HALL_R + 1):
        for dz in range(-HALL_R, HALL_R + 1):
            B.put(C + dx, CORNICE, C + dz, PURPUR, keep_shape=True)
    # Crystals hanging from the cornice's corners.
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        B.pendant(C + sx * (HALL_R + 2), CORNICE, C + sz * (HALL_R + 2), 1)
    # A low stepped roof, and a crystal on top.
    for i, r in enumerate((HALL_R, HALL_R - 2, 1)):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                B.put(C + dx, ROOF + i, C + dz, PURPUR)
    B.put(C, ROOF + 3, C, AMETHYST, keep_shape=True)


def light_column():
    """The column of light between the roof and the floating pyramid."""
    for y in range(ROOF + 4, PYRAMID):
        B.put(C, y, C, GLASS if (y - ROOF) % 2 else GLOW[0], None if (y - ROOF) % 2 else GLOW[1], keep_shape=True)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        B.end_rod(C + dx, ROOF + 4, C + dz)
        B.put(C + dx, PYRAMID - 1, C + dz, "minecraft:end_rod", {"facing": "down"})


def floating_pyramid():
    y = PYRAMID
    while True:
        r = PYR_R - int((y - PYRAMID) / 1.4)
        if r < 0:
            break
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                edge = abs(dx) == abs(dz) == r
                B.put(C + dx, y, C + dz, AMETHYST if edge else PURPUR, keep_shape=(y == PYRAMID))
        y += 1
    B.end_rod(C, y, C)
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        B.pendant(C + sx * PYR_R, PYRAMID - 1, C + sz * PYR_R, 2)


def surroundings():
    # Pendants under the platform's edges.
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        B.pendant(C + sx * (BASE_R + 1), PLATFORM - 1, C + sz * (BASE_R + 1), rng.randint(2, 4))
    for sx, sz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        B.pendant(C + sx * (ARM - 1), PLATFORM - 1, C + sz * (ARM - 1), rng.randint(1, 2))


def build():
    B.ground(10)
    base()
    platform()
    hall()
    stair()
    room()
    cornice_and_roof()
    light_column()
    floating_pyramid()
    surroundings()
    B.vines(TIP + 1, PLATFORM - 1, fraction=14)
    B.taper()


blocks = B.blocks


def main():
    build()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    n, p, top = B.save(OUT)
    print(f"{n} blocks, {p} block types, size {SIZE}x{top}x{SIZE}")


if __name__ == "__main__":
    main()
