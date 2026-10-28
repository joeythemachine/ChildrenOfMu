"""
Generates the Attuners: small floating pieces the Ithk set around their homes. In Ithk lore they hold the area
in tune, steadying the psychic resonance so thought carries cleanly from mind to mind.

    python tools/make_attuners.py

Writes three structures to src/main/resources/data/childrenofmu/structure/:
  attuner_shard.nbt  a glowing crystal hovering inside a slowly-tilted stone hoop
  attuner_fork.nbt   a crystal tuning fork, floating point-down
  attuner_ring.nbt   an upright stone ring with a crystal suspended at its center
"""
import math
import os

from ithk_build import AIR, AMETHYST, GLOW, Builder

HERE = os.path.dirname(os.path.abspath(__file__))
DIR = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure")

TILE = "minecraft:deepslate_tiles"
BRICK = "minecraft:polished_deepslate"


def hoop(B, cx, cy, cz, r, tilt, material, studs=AMETHYST, every=6):
    """A one-block-thick ring of radius r, tilted about the x axis by `tilt` radians."""
    n = int(2 * math.pi * r * 3)
    for i in range(n):
        t = 2 * math.pi * i / n
        x = r * math.cos(t)
        y0, z0 = 0.0, r * math.sin(t)
        y = y0 * math.cos(tilt) - z0 * math.sin(tilt)
        z = y0 * math.sin(tilt) + z0 * math.cos(tilt)
        p = (int(round(cx + x)), int(round(cy + y)), int(round(cz + z)))
        stud = i % max(1, n // every) == 0
        B.put(*p, studs if stud else material, keep_shape=True)


def shard():
    B = Builder(11, seed=71)
    C = B.c
    B.floating_crystal(C, 7, C, r=2)
    hoop(B, C, 7, C, 4, math.radians(25), TILE)
    return B


def fork():
    B = Builder(9, seed=73)
    C = B.c
    base = 3
    # The point and handle.
    B.cluster(C, base - 1, C, "down")
    for y in range(base, base + 4):
        B.put(C, y, C, AMETHYST, keep_shape=True)
    B.put(C, base + 1, C, *GLOW, keep_shape=True)
    # The bow joining the tines.
    y = base + 4
    for dx in range(-2, 3):
        B.put(C + dx, y, C, AMETHYST, keep_shape=True)
    B.put(C, y, C, BRICK, keep_shape=True)
    # Two tines, ringing.
    for dx in (-2, 2):
        for k in range(1, 7):
            B.put(C + dx, y + k, C, AMETHYST, keep_shape=True)
        B.cluster(C + dx, y + 7, C, "up")
    # Faint end rods humming between the tines.
    B.end_rod(C, y + 1, C)
    B.put(C, y + 6, C, "minecraft:end_rod", {"facing": "down"})
    return B


def ring():
    B = Builder(13, seed=79)
    C = B.c
    cy = 8
    # Upright ring facing north-south, set with crystal.
    for i in range(80):
        t = 2 * math.pi * i / 80
        x, y = int(round(C + 4.5 * math.cos(t))), int(round(cy + 4.5 * math.sin(t)))
        B.put(x, y, C, AMETHYST if i % 10 == 0 else TILE, keep_shape=True)
    # A crystal held at the center by four chains.
    B.put(C, cy, C, *GLOW, keep_shape=True)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        B.put(C + dx, cy + dy, C, AMETHYST, keep_shape=True)
    for k in range(2, 4):
        B.put(C, cy + k, C, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        B.put(C, cy - k, C, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        B.put(C + k, cy, C, "minecraft:chain", {"axis": "x", "waterlogged": "false"})
        B.put(C - k, cy, C, "minecraft:chain", {"axis": "x", "waterlogged": "false"})
    # A drip of crystal below the ring.
    B.cluster(C, cy - 6, C, "down", "large_amethyst_bud")
    B.put(C, cy - 5, C, AMETHYST, keep_shape=True)
    return B


PIECES = {"attuner_shard": shard, "attuner_fork": fork, "attuner_ring": ring}
blocks = {}
SIZE = 13


def build(name="attuner_ring"):
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
