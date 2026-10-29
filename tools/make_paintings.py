#!/usr/bin/env python3
"""Turns the owner's painting images (CoM Reference Images/Paintings, named with the city's initials first) into
Minecraft paintings: a texture (assets/childrenofmu/textures/painting/<id>.png) and a painting variant
(data/childrenofmu/painting_variant/<id>.json), sized in blocks.

None of ours go in the minecraft:placeable tag: they only appear where a template hangs them (B.painting in the
builders), never from a player's random painting. Add an entry here for each new image, then rebuild the templates
that hang it.

Textures are 32 pixels per block (vanilla is 16; the atlas takes any size). The image is resized to the painting's
exact shape, so make the source in (close to) that aspect ratio.
"""
import json
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, "CoM Reference Images", "Paintings")
TEX = os.path.join(ROOT, "src/main/resources/assets/childrenofmu/textures/painting")
DATA = os.path.join(ROOT, "src/main/resources/data/childrenofmu/painting_variant")
PX = 32

# id: (source file, width in blocks, height in blocks)
PAINTINGS = {
    # The Keep's prayer hall, over the altar, candles before it. Only there: never in a dwelling.
    "dc_the_whipped": ("DC The Whipped Painting.png", 5, 4),
}


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(DATA, exist_ok=True)
    for pid, (src, w, h) in PAINTINGS.items():
        path = os.path.join(SRC, src)
        if not os.path.exists(path):
            print(f"missing: {path}")
            continue
        im = Image.open(path).convert("RGBA")
        # Box filter: the sources are already pixel art on a coarse grid, so averaging keeps the cells clean.
        im.resize((w * PX, h * PX), Image.BOX).save(os.path.join(TEX, pid + ".png"))
        with open(os.path.join(DATA, pid + ".json"), "w") as f:
            json.dump({"asset_id": f"childrenofmu:{pid}", "width": w, "height": h}, f, indent=2)
            f.write("\n")


if __name__ == "__main__":
    main()
