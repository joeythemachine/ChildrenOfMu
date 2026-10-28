"""
Generates the Eye Statue: a great floating eyeball carved from grey stone and quartz, with an amethyst iris,
gazing a little skyward, hung with red vines.

    python tools/make_eye_statue.py

Writes src/main/resources/data/childrenofmu/structure/eye_statue.nbt
"""
import math
import os

from ithk_build import AIR, AMETHYST, GLOW, Builder

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure", "eye_statue.nbt")

SIZE = 15
B = Builder(SIZE, seed=53)
C = B.c
rng = B.rng

# ---------------------------------------------------------------- dimensions (y levels; 0 = ground)
R = 4.6
CY = 10                                   # the eye's center: its lowest point floats about 5 blocks up
GAZE = (0.0, 0.35, -1.0)                  # looking forward and a little up

# The white of the eye: quartz and pale stone, weathered with grey brick.
SCLERA = ["minecraft:quartz_block"] * 3 + ["minecraft:smooth_quartz", "minecraft:quartz_bricks",
                                           "minecraft:stone_bricks", "minecraft:polished_andesite"]
WEATHERED = ["minecraft:stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:stone",
             "minecraft:andesite", "minecraft:smooth_stone"]
RING = "minecraft:polished_andesite"      # the dark edge around the iris
PUPIL = "minecraft:polished_deepslate"


def norm(v):
    m = math.sqrt(sum(a * a for a in v))
    return tuple(a / m for a in v)


def eye():
    gaze = norm(GAZE)
    rr = int(math.ceil(R))
    for dx in range(-rr, rr + 1):
        for dy in range(-rr, rr + 1):
            for dz in range(-rr, rr + 1):
                d = math.sqrt(dx * dx + dy * dy + dz * dz)
                if d > R + 0.01:
                    continue
                x, y, z = C + dx, CY + dy, C + dz
                if d < R - 1.6:
                    B.put(x, y, z, "minecraft:stone")          # the solid core
                    continue
                cosang = (dx * gaze[0] + dy * gaze[1] + dz * gaze[2]) / max(d, 0.001)
                if cosang > 0.95:
                    B.put(x, y, z, PUPIL, keep_shape=True)
                elif cosang > 0.72:
                    B.put(x, y, z, AMETHYST, keep_shape=True)  # the iris: the only crystal on it
                elif cosang > 0.64:
                    B.put(x, y, z, RING, keep_shape=True)
                elif dy < -1 and rng.random() < 0.6:
                    B.put(x, y, z, rng.choice(WEATHERED))     # weathered grey underneath
                else:
                    B.put(x, y, z, rng.choice(SCLERA if rng.random() < 0.8 else WEATHERED))


def build():
    eye()
    B.vines(CY - 5, CY + 3, fraction=1)
    B.taper()


blocks = B.blocks


def main():
    build()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    n, p, top = B.save(OUT)
    print(f"{n} blocks, {p} block types, size {SIZE}x{top}x{SIZE}")


if __name__ == "__main__":
    main()
