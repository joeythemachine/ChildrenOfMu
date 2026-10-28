"""
Generates the Nexus: a tall floating diamond of deepslate brick and amethyst, inlaid with lines of emerald, gold,
lapis and glowstone, ringed at the waist, holding one cube-shaped room whose walls are lined with portals
(Nether, Aether, Twilight Forest, the End, the Undergarden, the Bumblezone), around a glowing crystal pedestal.

    python tools/make_nexus.py

Writes src/main/resources/data/childrenofmu/structure/nexus.nbt
The portals are decorative panels for now.
"""
import math
import os

from ithk_build import AIR, AMETHYST, GLOW, Builder

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure", "nexus.nbt")

SIZE = 33
B = Builder(SIZE, seed=31)
C = B.c
rng = B.rng

# ---------------------------------------------------------------- dimensions (y levels; 0 = ground)
GROUND_RADIUS = 8
SPIKE_TIP = 8
R = 9                         # half-width at the waist
H = 20                        # height from the waist to each point
MID = SPIKE_TIP + 4 + H
ROOM = 5                      # room walls 5 from the center: a 9x9x9 cube inside
ROOM_FLOOR, ROOM_CEIL = MID - 5, MID + 5

# ---------------------------------------------------------------- palette
# A deliberately small palette: deepslate, amethyst, obsidian, with emerald and lapis lines and froglight glow.
BRICK = ["minecraft:deepslate_bricks"] * 5 + ["minecraft:deepslate_tiles"]
OBSIDIAN = "minecraft:obsidian"
CRYING = "minecraft:crying_obsidian"
EMERALD = "childrenofmu:inlaid_emerald"
LAPIS = "childrenofmu:inlaid_lapis"
INLAY = [EMERALD, LAPIS]


def brick():
    return rng.choice(BRICK)


def r_at(y):
    return R * (1 - abs(y - MID) / H)


def inside(dx, y, dz):
    r = r_at(y)
    return abs(dx) <= r + 0.01 and abs(dz) <= r + 0.01


def in_room(dx, y, dz):
    return ROOM_FLOOR < y < ROOM_CEIL and abs(dx) < ROOM and abs(dz) < ROOM


def in_door(dx, y, dz):
    # The front portal is the way in: a tall opening through the north face.
    return dz <= -ROOM and abs(dx) <= 1 and ROOM_FLOOR < y <= ROOM_FLOOR + 5


def shell():
    for y in range(MID - H, MID + H + 1):
        r = int(math.floor(r_at(y)))
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                x, z = C + dx, C + dz
                if in_room(dx, y, dz) or in_door(dx, y, dz):
                    B.air(x, y, z)
                    continue
                surface = any(not inside(dx + a, y + b, dz + c) for a, b, c in
                              ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)))
                if not surface:
                    B.put(x, y, z, brick())
                    continue
                on_center_line = (dx == 0 and abs(dz) == r) or (dz == 0 and abs(dx) == r)
                if on_center_line and abs(y - MID) > ROOM + 1:
                    # Lines of inlay running from the waist to the points.
                    B.put(x, y, z, INLAY[(y // 2) % 2], keep_shape=True)
                elif abs(y - MID) >= ROOM + 3 and rng.random() < 0.55:
                    B.put(x, y, z, AMETHYST)            # crystal grows over the upper and lower points
                elif rng.random() < 0.05:
                    B.put(x, y, z, OBSIDIAN)
                else:
                    B.put(x, y, z, brick())


def front_frame():
    """The front opening, framed in obsidian, flanked by glowing purple panes set flush into the face."""
    for y in range(ROOM_FLOOR, ROOM_FLOOR + 7):
        for dx in (-2, 2):
            for dz in range(-int(r_at(y)), -ROOM + 1):
                B.put(C + dx, y, C + dz, OBSIDIAN, keep_shape=True)
    for dx in range(-2, 3):
        for dz in range(-int(r_at(ROOM_FLOOR + 6)), -ROOM + 1):
            B.put(C + dx, ROOM_FLOOR + 6, C + dz, OBSIDIAN, keep_shape=True)
    # Each pane sits exactly in the face at its own height, with the light one block behind it.
    for y in range(MID - 2, MID + 3):
        face = -int(math.floor(r_at(y)))
        for dx in (-4, 4):
            if abs(dx) <= abs(face):
                B.put(C + dx, y, C + face, "minecraft:purple_stained_glass", keep_shape=True)
                B.put(C + dx, y, C + face + 1, *GLOW, keep_shape=True)


def waist_rings():
    """Loops of stone standing out from the left and right sides, set with amethyst."""
    for side in (1, -1):
        cx = C + side * (R + 3)
        for i in range(60):
            t = i / 60 * 2 * math.pi
            x = int(round(cx + side * 3 * math.cos(t)))
            y = int(round(MID + 3.5 * math.sin(t)))
            if abs(x - C) <= R:
                continue
            for dz in (-1, 0, 1):
                special = dz == 0 and i % 10 == 0
                B.put(x, y, C + dz, AMETHYST if special else brick(), keep_shape=special)
        # A crystal spike pointing out from each ring.
        for k in range(1, 4):
            B.put(C + side * (R + 6 + k), MID, C, AMETHYST)
        B.cluster(C + side * (R + 10), MID, C, "east" if side > 0 else "west")
    # Shorter spikes from the front and back corners.
    for side in (1, -1):
        for k in range(1, 4):
            B.put(C, MID, C + side * (R + k), AMETHYST)
        B.cluster(C, MID, C + side * (R + 4), "south" if side > 0 else "north")


def points():
    top = MID + H
    for k, name in enumerate([OBSIDIAN, OBSIDIAN, CRYING, OBSIDIAN]):
        B.put(C, top + 1 + k, C, name, keep_shape=True)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        B.put(C + dx, top + 3, C + dz, OBSIDIAN, keep_shape=True)   # a dark cross at the top
    B.end_rod(C, top + 5, C)
    bottom = MID - H
    B.spike(C, bottom - 1, C, bottom - SPIKE_TIP - 1, base_r=1, up=False, tip=False)
    B.put(C, SPIKE_TIP, C, CRYING, keep_shape=True)
    B.cluster(C, SPIKE_TIP - 1, C, "down")


def room():
    r = ROOM
    # Floor: dark tiles with glowstone and lapis inlay.
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            ring = max(abs(dx), abs(dz))
            if ring == 3 and (dx + dz) % 4 == 0:
                name = GLOW[0]
            elif ring == 4 and abs(dx) == abs(dz):
                name = LAPIS
            else:
                name = rng.choice(["minecraft:deepslate_tiles", "minecraft:polished_deepslate", "minecraft:deepslate_bricks"])
            B.put(C + dx, ROOM_FLOOR, C + dz, name, keep_shape=True)
            B.put(C + dx, ROOM_CEIL, C + dz, AMETHYST, keep_shape=True)
    # Walls.
    for y in range(ROOM_FLOOR + 1, ROOM_CEIL):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if max(abs(dx), abs(dz)) == r and not in_door(dx, y, dz):
                    B.put(C + dx, y, C + dz, brick(), keep_shape=True)

    # Portal windows: two on each side wall and the back wall, each a glowing slit of coloured glass in obsidian.
    portals = [
        ("x", 1, "purple", OBSIDIAN),       # the Nether
        ("x", -1, "light_blue", OBSIDIAN),  # the Aether
        ("z", 1, "lime", OBSIDIAN),         # the Twilight Forest
        ("z", 1, "cyan", OBSIDIAN),         # the End
        ("x", 1, "orange", OBSIDIAN),       # the Undergarden
        ("x", -1, "yellow", OBSIDIAN),      # the Bumblezone
    ]
    slots = {("x", 1): [-2, 2], ("x", -1): [-2, 2], ("z", 1): [-2, 2]}
    used = {k: 0 for k in slots}
    for axis, side, colour, frame in portals:
        key = (axis, side)
        offset = slots[key][used[key]]
        used[key] += 1
        portal_panel(axis, side, offset, colour, frame)

    # The pedestal: obsidian steps, crying obsidian, a crystal, and a beam of light to the ceiling.
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            B.put(C + dx, ROOM_FLOOR + 1, C + dz, OBSIDIAN if (dx or dz) else CRYING, keep_shape=True)
    B.put(C, ROOM_FLOOR + 2, C, AMETHYST, keep_shape=True)
    B.cluster(C, ROOM_FLOOR + 3, C, "up")
    for y in range(ROOM_FLOOR + 4, ROOM_CEIL):
        B.put(C, y, C, "minecraft:end_rod", {"facing": "up"})
    B.put(C, ROOM_CEIL, C, *GLOW, keep_shape=True)
    # Lamps in the ceiling corners.
    for dx, dz in ((4, 4), (-4, 4), (4, -4), (-4, -4)):
        B.put(C + dx, ROOM_CEIL, C + dz, *GLOW, keep_shape=True)
    # Keepers of the gates.
    B.ithk(C - 2, ROOM_FLOOR + 3, C + 1, Profession="gatekeeper")
    B.ithk(C + 2, ROOM_FLOOR + 3, C + 1, Profession="gatekeeper")


def portal_panel(axis, side, offset, colour, frame):
    """A slim glowing panel of coloured glass (1 wide, 3 tall) in an obsidian frame, at eye level."""
    w = ROOM * side
    for u in range(-1, 2):
        for v in range(0, 5):
            y = ROOM_FLOOR + 2 + v
            a = offset + u
            edge = abs(u) == 1 or v in (0, 4)
            if axis == "x":
                x, z, bx, bz = C + w, C + a, C + w + side, C + a
            else:
                x, z, bx, bz = C + a, C + w, C + a, C + w + side
            if edge:
                B.put(x, y, z, frame, keep_shape=True)
            else:
                B.put(x, y, z, f"minecraft:{colour}_stained_glass")
                B.put(bx, y, bz, *GLOW, keep_shape=True)   # light behind the glass makes it glow


def surroundings():
    for dx, dz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        x, z = C + dx * 4, C + dz * 4
        y = MID - 1
        while (x, y, z) in B.blocks and y > 0:
            y -= 1
        B.pendant(x, y, z, rng.randint(1, 2))
    for side in (1, -1):
        B.lantern(C + side * (R + 3), MID - 4, C, chain_len=1, soul=True)


def build():
    B.ground(GROUND_RADIUS)
    shell()
    room()
    front_frame()
    waist_rings()
    points()
    surroundings()
    B.vines(SPIKE_TIP, MID - 4, fraction=16)
    B.taper()


blocks = B.blocks


def main():
    build()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    n, p, top = B.save(OUT)
    print(f"{n} blocks, {p} block types, size {SIZE}x{top}x{SIZE}")


if __name__ == "__main__":
    main()
