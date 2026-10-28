"""
Generates the Sleep Lid: where an Ithk goes to rest. The Ithk have no eyelids and can never stop watching, so to
rest they float inside one: a small, plain, almond-shaped shell of deepslate whose two lids have almost closed,
leaving a narrow slit across the front. Inside it is lined with obsidian and kept dark, so the Ithk can "blink".

    python tools/make_sleep_lid.py

Writes src/main/resources/data/childrenofmu/structure/sleep_lid.nbt
There is deliberately no light inside. The floor is carpeted so no monsters can spawn in the dark.
"""
import math
import os

from ithk_build import AIR, Builder

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure", "sleep_lid.nbt")

SIZE = 13
B = Builder(SIZE, seed=67)
C = B.c
rng = B.rng

# ---------------------------------------------------------------- dimensions (y levels; 0 = ground)
TIP = 3
A, BH, D = 5.3, 3.9, 4.6     # half-width, half-height and half-depth of the almond shell
CY = TIP + 2 + 4             # the shell's center
FLOOR = CY - 2               # the floor's top
SLIT = 1.1                   # half-height of the opening at its middle

OUTSIDE = ["minecraft:deepslate_bricks"] * 3 + ["minecraft:deepslate_tiles", "minecraft:polished_deepslate"]
INSIDE = "minecraft:obsidian"


def inside(dx, y, dz, shrink=0.0):
    return (dx / (A - shrink)) ** 2 + ((y - CY) / (BH - shrink)) ** 2 + (dz / (D - shrink)) ** 2 <= 1.0


def in_slit(dx, y, dz):
    return dz <= -1 and abs(y - CY) <= SLIT * (1 - (dx / A) ** 2)


def shell():
    for dx in range(-6, 7):
        for y in range(CY - 5, CY + 5):
            for dz in range(-6, 7):
                if not inside(dx, y, dz):
                    continue
                x, z = C + dx, C + dz
                if in_slit(dx, y, dz) and y > FLOOR:
                    B.air(x, y, z)
                elif not inside(dx, y, dz, shrink=1.0):
                    B.put(x, y, z, rng.choice(OUTSIDE))                 # the lid's outer skin
                elif not inside(dx, y, dz, shrink=2.0) or y <= FLOOR:
                    B.put(x, y, z, INSIDE, keep_shape=True)             # the dark lining, and the floor
                else:
                    B.air(x, y, z)
    # Soft black carpet over the floor: it stays dark, and nothing can spawn on it.
    for dx in range(-5, 6):
        for dz in range(-5, 6):
            p = (C + dx, FLOOR + 1, C + dz)
            if B.name_at(*p) == AIR and B.name_at(C + dx, FLOOR, C + dz) == INSIDE:
                B.put(*p, "minecraft:black_carpet")
    B.ithk(C, FLOOR + 1, C + 1, Profession="quiescent")


def underside():
    bottom = int(math.ceil(CY - BH))
    B.spike(C, bottom, C, bottom - TIP + 1, base_r=1, up=False, material="minecraft:deepslate_bricks", tip=False)


def build():
    shell()
    underside()
    B.taper()


blocks = B.blocks


def main():
    build()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    n, p, top = B.save(OUT)
    print(f"{n} blocks, {p} block types, size {SIZE}x{top}x{SIZE}")


if __name__ == "__main__":
    main()
