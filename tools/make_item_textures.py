"""
Draws the mod's item textures (16x16 pixel art).

    python tools/make_item_textures.py
"""
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "childrenofmu", "textures", "item")

# Levitation Shard: a pale, glassy crystal sliver, pointing up and to the right.
# Each character is one pixel. '.' = transparent.
SHARD = [
    "................",
    "..........o.....",
    ".........oW#....",
    "........oWh#....",
    ".......oWhl##...",
    "......oWhhlm#...",
    "......oWhllm#...",
    ".....oWhllmm#...",
    ".....oWhlmm#..o.",
    "....oWhllm#..oW#",
    "....oWhlmm#..oh#",
    "....ohllm#...om#",
    ".....ohlm#....#.",
    ".....ohm#.......",
    "......o#........",
    "................",
]
COLORS = {
    "o": (122, 92, 176, 255),   # outline, lit side
    "#": (74, 52, 120, 255),    # outline, shadow side
    "W": (255, 255, 255, 255),  # bright edge
    "h": (226, 214, 255, 255),  # highlight
    "l": (190, 164, 242, 255),  # light body
    "m": (150, 118, 214, 255),  # mid body
}


def draw(rows, path):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in COLORS:
                img.putpixel((x, y), COLORS[ch])
    img.save(path)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    draw(SHARD, os.path.join(OUT, "levitation_shard.png"))
    print("Wrote", os.path.abspath(OUT))
