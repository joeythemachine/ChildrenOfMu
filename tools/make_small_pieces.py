"""
Generates the smaller Ithk village pieces:

  lens_arch      a deepslate archway with a crystal lens hanging in it, marking a village's edge (stands on the ground)
  scrying_pool   a still pool of dark water in a raised rim, lit by candles on crystal posts (set into the ground)
  mud_garden     a little floating island of mud and red plants, trailing roots and vines
  star_perch     a thin spire with a small platform on top, where an Ithk watches the sky
  memory_cairn   a floating stack of stone discs etched with lapis lines, glowing at their hearts
  golem_cradle   a floating platform where amber golems rest in glowing alcoves, around a pool of lava they step
                 into to melt down and come out renewed

    python tools/make_small_pieces.py

Writes each to src/main/resources/data/childrenofmu/structure/<name>.nbt
"""
import math
import os

from ithk_build import AIR, AMETHYST, GLOW, Builder

HERE = os.path.dirname(os.path.abspath(__file__))
DIR = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure")

BRICKS = ["minecraft:deepslate_bricks"] * 4 + ["minecraft:deepslate_tiles"]
POLISHED = "minecraft:polished_deepslate"
TILE = "minecraft:deepslate_tiles"
LAPIS = "childrenofmu:inlaid_lapis"
AMBER = ("minecraft:ochre_froglight", {"axis": "y"})
LAVA = ("minecraft:lava", {"level": "0"})


def lens_arch():
    """Ground y is 1: the bottom layer is foundation."""
    B = Builder(11, seed=101)
    C, rng = B.c, B.rng
    G = 1
    for side in (-1, 1):
        x = C + side * 3
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                B.put(x + dx, 0, C + dz, POLISHED, keep_shape=True)
                B.put(x + dx, G, C + dz, AMETHYST if dx and dz else POLISHED)   # a broad foot
        for y in range(G + 1, G + 7):
            B.put(x, y, C, rng.choice(BRICKS))
        B.put(x, G + 1, C, *GLOW, keep_shape=True)
    # The arch.
    top = G + 6
    for i in range(41):
        t = math.pi * i / 40
        x, y = int(round(C + 3 * math.cos(t))), int(round(top + 3 * math.sin(t)))
        B.put(x, y, C, AMETHYST if i == 20 else rng.choice(BRICKS), keep_shape=(i == 20))
    B.end_rod(C, top + 4, C)
    # The lens: a small crystal ring with a glowing heart, hung from the keystone.
    B.put(C, top + 2, C, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    cy = top
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        B.put(C + dx, cy + dy, C, AMETHYST, keep_shape=True)
    B.put(C, cy, C, *GLOW, keep_shape=True)
    B.vines(top, top + 3, fraction=8)
    B.taper(y_min=G)
    return B


def scrying_pool():
    """Ground y is 2: the pool's floor and water are set into the ground; the rim stands one block above it."""
    B = Builder(9, seed=103)
    C, rng = B.c, B.rng
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            ring = max(abs(dx), abs(dz))
            B.put(C + dx, 0, C + dz, "minecraft:obsidian", keep_shape=True)     # a black bottom: the water reads dark
            if ring <= 2:
                B.put(C + dx, 1, C + dz, "minecraft:water", {"level": "0"})
            else:
                B.put(C + dx, 1, C + dz, POLISHED, keep_shape=True)
                B.put(C + dx, 2, C + dz, "minecraft:polished_deepslate_slab", {"type": "bottom", "waterlogged": "false"})
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        x, z = C + sx * 3, C + sz * 3
        for y in range(2, 4):
            B.put(x, y, z, AMETHYST, keep_shape=True)
        B.candle(x, 4, z, count=4)
    B.ithk(C, 3, C, Profession="quiescent")    # gazing into the water
    return B


def mud_garden():
    B = Builder(11, seed=107)
    C, rng = B.c, B.rng
    TOP, DEPTH = 8, 5
    for k in range(DEPTH):
        y = TOP - k
        r = 4.3 - k * 0.85
        for dx in range(-5, 6):
            for dz in range(-5, 6):
                if dx * dx + dz * dz > (r + rng.uniform(-0.3, 0.4)) ** 2:
                    continue
                if k == 0:
                    name = "minecraft:packed_mud" if rng.random() < 0.15 else "minecraft:mud"
                else:
                    name = rng.choice(["minecraft:mud", "minecraft:rooted_dirt", "minecraft:mud_bricks",
                                       "minecraft:packed_mud"])
                B.put(C + dx, y, C + dz, name, keep_shape=(k == 0))
    # Red plants on top.
    for (x, y, z), b in list(B.blocks.items()):
        if y == TOP and (x, y + 1, z) not in B.blocks:
            roll = rng.random()
            if roll < 0.35:
                B.put(x, y + 1, z, "minecraft:crimson_roots")
            elif roll < 0.5:
                B.put(x, y + 1, z, "minecraft:poppy")
            elif roll < 0.58:
                B.put(x, y + 1, z, "minecraft:red_tulip")
            elif roll < 0.62 and (x, y + 2, z) not in B.blocks:
                B.put(x, y + 1, z, "minecraft:rose_bush", {"half": "lower"})
                B.put(x, y + 2, z, "minecraft:rose_bush", {"half": "upper"})
    # Roots and vines trailing underneath, and a glowing crystal at the tip.
    for (x, y, z), b in list(B.blocks.items()):
        if y < TOP and (x, y - 1, z) not in B.blocks and b[0] == "minecraft:rooted_dirt":
            B.put(x, y - 1, z, "minecraft:hanging_roots", {"waterlogged": "false"})
    B.vines(TOP - DEPTH, TOP, fraction=4)
    tip = TOP - DEPTH
    B.pendant(C, tip, C, 1)
    return B


def star_perch():
    """Ground y is 1."""
    B = Builder(9, seed=109)
    C, rng = B.c, B.rng
    G, PLAT = 1, 14
    for y in range(0, PLAT):
        r = 1 if y <= G + 1 else 0
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                B.put(C + dx, y, C + dz, rng.choice(BRICKS), keep_shape=(y < G))
    # Glowing bands up the spire.
    for y in (G + 4, G + 8):
        B.put(C, y, C, *GLOW, keep_shape=True)
    # The platform.
    for dx, dz in B.each(2):
        B.put(C + dx, PLAT, C + dz, POLISHED if (dx, dz) != (0, 0) else GLOW[0],
              GLOW[1] if (dx, dz) == (0, 0) else None, keep_shape=True)
    for dx, dz in ((2, 0), (-2, 0), (0, 2), (0, -2)):
        B.end_rod(C + dx, PLAT + 1, C + dz)
    # A crystal "telescope", angled at the sky.
    for k in range(3):
        B.put(C + 1 + k, PLAT + 1 + k, C + 1, AMETHYST, keep_shape=True)
    B.end_rod(C + 3, PLAT + 4, C + 1)
    B.ithk(C - 1, PLAT + 1, C - 1, Profession="quiescent")
    B.taper(y_min=G)
    return B


def memory_cairn():
    B = Builder(9, seed=113)
    C, rng = B.c, B.rng
    for i, r in enumerate((3.4, 2.8, 2.2, 1.6, 1.0)):
        y = 3 + i * 2
        for dx in range(-4, 5):
            for dz in range(-4, 5):
                if dx * dx + dz * dz > (r + 0.3) ** 2:
                    continue
                if dx == 0 and dz == 0:
                    B.put(C, y, C, *GLOW, keep_shape=True)               # a glowing heart in every disc
                elif dx == 0 or dz == 0:
                    B.put(C + dx, y, C + dz, LAPIS, keep_shape=True)     # etched lines
                else:
                    B.put(C + dx, y, C + dz, POLISHED if i % 2 == 0 else TILE)
    B.cluster(C, 12, C, "up")
    B.cluster(C, 2, C, "down")
    B.taper()
    return B


def golem_cradle():
    B = Builder(17, seed=127)
    C, rng = B.c, B.rng
    R, TIP = 7, 2
    F = TIP + R                     # the platform's floor
    # An upside-down pyramid holding the platform.
    for y in range(TIP, F + 1):
        r = min(R, y - TIP)
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                edge = abs(dx) == abs(dz) == r
                B.put(C + dx, y, C + dz, AMETHYST if edge else rng.choice(BRICKS), keep_shape=(y == F))
    B.cluster(C, TIP - 1, C, "down")
    # The pool of renewal, flush with the floor.
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            ring = max(abs(dx), abs(dz))
            if ring <= 1:
                B.put(C + dx, F, C + dz, *LAVA)
                B.put(C + dx, F - 1, C + dz, *LAVA)
            else:
                B.put(C + dx, F, C + dz, POLISHED, keep_shape=True)
                B.put(C + dx, F - 1, C + dz, "minecraft:magma_block", keep_shape=True)
    # Three alcoves, each big enough for a golem (3 wide, 4 tall, 2 deep), facing the pool; the fourth side is open.
    def alcove(rot):
        def P(dx, y, dz):
            for _ in range(rot):
                dx, dz = -dz, dx
            return C + dx, y, C + dz
        for dx in range(-2, 3):
            for dz in range(4, 8):
                for y in range(F + 1, F + 6):
                    inner = abs(dx) <= 1 and dz in (5, 6) and y <= F + 4
                    if inner:
                        continue
                    if dz == 4:
                        continue          # the open front
                    if dz == 7 and abs(dx) <= 1 and y <= F + 4:
                        B.put(*P(dx, y, dz), *AMBER, keep_shape=True)    # the warm amber glow they rest against
                    else:
                        B.put(*P(dx, y, dz), TILE if y == F + 5 else rng.choice(BRICKS))
        B.put(*P(0, F + 6, 6), AMETHYST)
        B.cluster(*P(0, F + 7, 6), "up")
    for rot in (0, 1, 3):
        alcove(rot)
    # Corner posts topped with amber light.
    for sx, sz in ((1, -1), (-1, -1)):
        x, z = C + sx * 6, C + sz * 6
        for y in range(F + 1, F + 3):
            B.put(x, y, z, POLISHED, keep_shape=True)
        B.put(x, F + 3, z, *AMBER, keep_shape=True)
    # Glowing crystals hanging from the pyramid's corners.
    for sx, sz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        B.pendant(C + sx * R, F - 1, C + sz * R, 1)
    B.vines(TIP + 1, F - 2, fraction=10)
    B.taper()
    return B


PIECES = {"lens_arch": lens_arch, "scrying_pool": scrying_pool, "mud_garden": mud_garden,
          "star_perch": star_perch, "memory_cairn": memory_cairn, "golem_cradle": golem_cradle}
blocks, SIZE = {}, 17


def build(name="golem_cradle"):
    global blocks, SIZE
    B = PIECES[name]()
    blocks, SIZE = B.blocks, B.size
    return B


def main():
    os.makedirs(DIR, exist_ok=True)
    for name, make in PIECES.items():
        B = make()
        n, p, top = B.save(os.path.join(DIR, name + ".nbt"))
        print(f"{name}: {n} blocks, {p} block types, size {B.size}x{top}x{B.size}")


if __name__ == "__main__":
    main()
