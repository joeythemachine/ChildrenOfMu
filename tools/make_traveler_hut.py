"""
The Old Traveller's hut: a little wayside cabin with a thatched roof, set down into the ground, with a sunken
campfire ringed with stones and log seats out front, and frontier living all round it: a woodpile and chopping
block, barrels, a drying line, and odds and ends that depend on who lives there (hay and a water trough for a dog's
pack animals, fish on the line for a cat, bugs for a frog or bat, a junk heap for a rat, crystals for a goxa).
Inside: a bedroll, a table with a candle, the map table, a lectern with the traveller's journal, a globe on a barrel,
sacks, a rug. One template per race of traveller: traveler_hut_<race>.nbt, plus the usual _vanilla twins.

Template y = 1 is the ground (sink 2): the floor is flush with the land and the campfire sits down in it.

    python tools/make_traveler_hut.py
"""
import os

from frog_build import FrogBuilder, SCREEN
from nbt_structure import Byte

HERE = os.path.dirname(os.path.abspath(__file__))
DIR = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure")
RACES = ["rat", "cat", "bat", "dog", "frog", "goxa"]
G = 1   # ground level in the template

JOURNAL = {
    "rat": ["Day something. Still walking. The boots are holding up better than the boots before them.",
            "Found a frog town in the swamps. They have a post office. A POST OFFICE. Rats could run one of those.",
            "Need maps? I've got maps. Bring bones, I'm always short of bones."],
    "cat": ["Left the city for a bit. Too many rats, not enough sun.",
            "The frogs dry their fish on lines. Barbaric. Delicious, but barbaric.",
            "Maps to the towns, for anyone with emeralds and a little cod."],
    "bat": ["The road is quiet at night. I hum Bazz to it and it doesn't complain.",
            "Slept upside down in a tree. Woke up the right way round. Odd.",
            "I trade maps for spider eyes. Don't ask."],
    "dog": ["Walked the old road east. Lost a horse, found a better one.",
            "A samurai is only as good as the map in his pack.",
            "Maps to the towns. Iron for the trouble."],
    "frog": ["Too far from the swamp. My skin is drying out. Worth it for the stories.",
            "The eyeball folk float. Nobody believes me.",
            "Maps to the towns. A few slimeballs, please, and emeralds."],
    "goxa": ["The land is younger than it looks.",
             "Amethyst sings if you listen. Most don't.",
             "I keep maps of the places the small folk live."],
}


def journal(race):
    """A book and quill with the traveller's notes, for the lectern (block entity NBT)."""
    pages = [{"raw": p} for p in JOURNAL[race]]
    return {"id": "minecraft:lectern", "Page": 0,
            "Book": {"id": "minecraft:writable_book", "count": 1,
                     "components": {"minecraft:writable_book_content": {"pages": pages}}}}


def hut(race):
    B = FrogBuilder(17, seed=401 + RACES.index(race))
    C = B.c
    rng = B.rng
    x0, x1, z0, z1 = C - 4, C + 4, C - 3, C + 3
    # A stone footing below the ground, and a plank floor flush with it.
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            B.put(x, 0, z, "minecraft:cobblestone")
            B.put(x, G, z, ("minecraft:mossy_cobblestone" if rng.random() < 0.4 else "minecraft:cobblestone")
                  if edge else "minecraft:spruce_planks")
    # Walls: spruce planks between stripped logs, a door to the north, little windows.
    for y in range(G + 1, G + 4):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge_x, edge_z = x in (x0, x1), z in (z0, z1)
                if edge_x and edge_z:
                    B.log(x, y, z, kind="stripped_spruce_log")
                elif edge_x or edge_z:
                    B.put(x, y, z, "minecraft:spruce_planks")
                else:
                    B.air(x, y, z)
    B.door(C, G + 1, z0, "south")
    for x in (x0, x1):
        B.put(x, G + 2, C, SCREEN)
    B.put(C + 2, G + 2, z1, SCREEN)
    B.put(C - 2, G + 2, z0, SCREEN)
    B.hip_roof(x0 - 1, x1 + 1, z0 - 1, z1 + 1, G + 4)

    # ---- Inside
    fy = G + 1
    B.put(x0 + 1, fy, C + 1, "minecraft:brown_carpet")              # the bedroll...
    B.put(x0 + 1, fy, C + 2, "minecraft:white_carpet")              # ...and its pillow
    B.put(x0 + 2, fy, C + 1, "minecraft:brown_carpet")
    B.put(C, fy, C, "minecraft:red_carpet")                          # a worn rug
    B.put(C - 1, fy, C, "minecraft:red_carpet")
    B.put(C, fy, z1 - 1, "minecraft:lectern", {"facing": "north", "has_book": "true", "powered": "false"})
    B.blocks[(C, fy, z1 - 1)] = B.blocks[(C, fy, z1 - 1)] + (journal(race),)
    B.put(C + 1, fy, z1 - 1, "minecraft:cartography_table")
    B.barrel(x1 - 1, fy, z1 - 1)
    B.mod(x1 - 1, fy + 1, z1 - 1, "supplementaries:globe", {"facing": "west"}, "minecraft:potted_fern")
    B.fence(x1 - 1, fy, z0 + 1, "minecraft:spruce_fence")            # a little table...
    B.put(x1 - 1, fy + 1, z0 + 1, "minecraft:candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
    B.stair(x1 - 2, fy, z0 + 1, "minecraft:spruce_stairs", "west")    # ...and a stool
    B.mod(x0 + 1, fy, z0 + 1, "supplementaries:sack", {"open": "false"}, "minecraft:barrel",
          {"facing": "up", "open": "false"})
    B.mod(x1 - 1, fy, C, "supplementaries:sack", {"open": "false"}, "minecraft:barrel", {"facing": "up", "open": "false"})
    B.potted(x0 + 1, fy, z1 - 1, "dead_bush")
    B.put(C, G + 3, C, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})

    # ---- Outside: a sunken campfire ringed with stones, well away from the door, with log seats.
    fx, fz = C - 2, z0 - 5
    for x in range(fx - 1, fx + 2):
        for z in range(fz - 1, fz + 2):
            B.put(x, G, z, "minecraft:cobblestone" if (x + z) % 2 else "minecraft:mossy_cobblestone")
            B.air(x, G + 1, z)
    B.put(fx, G, fz, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false",
                                            "waterlogged": "false"})
    B.log(fx - 2, G + 1, fz, axis="z", kind="stripped_spruce_log")
    B.log(fx + 2, G + 1, fz, axis="z", kind="stripped_spruce_log")
    B.log(fx, G + 1, fz - 2, axis="x", kind="stripped_spruce_log")
    # A trodden path from the door.
    for z in range(z0 - 3, z0):
        B.put(C, G, z, "minecraft:dirt_path")
    B.put(C - 1, G, z0 - 3, "minecraft:dirt_path")
    # The woodpile against the east wall, and the chopping block.
    for z in range(C - 2, C + 2):
        for y in (G + 1, G + 2):
            if not (y == G + 2 and z == C + 1):
                B.log(x1 + 1, y, z, axis="x", kind="oak_log" if (y + z) % 3 else "birch_log")
    B.put(x1 + 1, G + 3, C - 1, "minecraft:oak_slab", {"type": "bottom", "waterlogged": "false"})
    B.log(x1 + 2, G + 1, z0 - 1, kind="stripped_oak_log")
    # Barrels by the door, a lantern on a post.
    B.barrel(C - 2, G + 1, z0 - 1)
    B.barrel(C - 3, G + 1, z0 - 1, "north")
    B.fence(C + 2, G + 1, z0 - 1, "minecraft:spruce_fence")
    B.put(C + 2, G + 2, z0 - 1, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})

    # A drying line between two posts.
    lx0, lx1, lz = C + 2, C + 5, z0 - 4

    def drying_line(what):
        for x in (lx0, lx1):
            for y in range(G + 1, G + 4):
                B.fence(x, y, lz, "minecraft:spruce_fence")
        for x in range(lx0 + 1, lx1):
            B.fence(x, G + 3, lz, "minecraft:spruce_fence")
            B.put(x, G + 2, lz, f"childrenofmu:strung_{what[(x - lx0) % len(what)]}")

    if race == "dog":
        # Pack animals: hay, a water trough, a hitching rail.
        for (x, z) in ((C + 3, z0 - 3), (C + 4, z0 - 3), (C + 3, z0 - 4)):
            B.put(x, G + 1, z, "minecraft:hay_block", {"axis": "y"})
        B.put(C + 4, G + 2, z0 - 3, "minecraft:hay_block", {"axis": "x"})
        B.put(C + 5, G + 1, z0 - 1, "minecraft:water_cauldron", {"level": "3"})
        for x in range(C + 3, C + 6):
            B.fence(x, G + 1, z0 - 6, "minecraft:spruce_fence")
    elif race == "cat":
        drying_line(["fish"])
        B.barrel(C + 5, G + 1, z0 - 2, "up")
        B.put(C + 4, G + 1, z0 - 2, "minecraft:cauldron")
    elif race == "frog":
        drying_line(["bugs", "fish"])
        B.lily(C + 5, G + 1, z0 - 1)
        B.put(C + 4, G + 1, z0 - 1, "minecraft:water_cauldron", {"level": "3"})
    elif race == "bat":
        drying_line(["bugs"])
        for z in (C - 1, C + 1):
            B.fence(x0 - 1, G + 3, z, "minecraft:spruce_fence")        # somewhere to hang
    elif race == "rat":
        # A heap of useful junk.
        B.put(C + 3, G + 1, z0 - 3, "minecraft:composter", {"level": "5"})
        B.put(C + 4, G + 1, z0 - 3, "minecraft:coarse_dirt")
        B.put(C + 4, G + 2, z0 - 3, "minecraft:bone_block", {"axis": "x"})
        B.put(C + 4, G + 1, z0 - 4, "minecraft:gravel")
        B.put(C + 5, G + 1, z0 - 3, "minecraft:cobweb")
        B.barrel(C + 3, G + 1, z0 - 4, "east")
    elif race == "goxa":
        for (x, z, size) in ((C + 3, z0 - 3, "amethyst_cluster"), (C + 4, z0 - 4, "large_amethyst_bud"),
                             (C + 5, z0 - 2, "medium_amethyst_bud")):
            B.put(x, G, z, "minecraft:amethyst_block")
            B.put(x, G + 1, z, f"minecraft:{size}", {"facing": "up", "waterlogged": "false"})
        B.put(C + 4, G + 1, z0 - 1, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})

    B.entities.append((C - 1 + 0.5, G + 1.05, C - 1 + 0.5, {"id": f"childrenofmu:{race}" if race == "goxa" else f"childrenofmu:{race}_folk",
                                                            "PersistenceRequired": Byte(1), "Traveler": Byte(1)}))
    B.finish()
    return B


def main():
    os.makedirs(DIR, exist_ok=True)
    for race in RACES:
        B = hut(race)
        n, p, top, mods = B.save_pair(os.path.join(DIR, f"traveler_hut_{race}"))
        print(f"traveler_hut_{race}: {n} blocks, {mods} mod blocks")


if __name__ == "__main__":
    main()
