"""
Generates the Frog folk's swamp town buildings, all on stilts in the style of the "Frog Hut 1" sheet:

  frog_hut         a home (random frog residents, a froglet)
  frog_town_hall   the town hall and treasury: the Elder and the Treasurer, a bell on the porch
  frog_post        the courier post, with the town's post box and two Jumper couriers
  frog_guard_tower a lookout cabin high on tall stilts, with a Guard
  frog_potion_shop brewing stands and cauldrons, with a Potioneer
  frog_coppersmith a forge of copper, with a Coppersmith
  frog_chitinier   hives, honey and silk, with a Chitinier
  frog_pump_house  copper pipes and cauldrons for the town's plumbing, with a Plumber
  frog_fishing_dock a low roofed dock over the water, with a Fisher

    python tools/make_frog_town.py

Writes each to src/main/resources/data/childrenofmu/structure/<name>.nbt and prints each one's "door" distance (from
the center to its dock's outer edge, for the town's boardwalks; see worldgen/structure/frog_town.json).
"""
import math
import os

from frog_build import FENCE, FLOOR, LOG, SCREEN, FrogBuilder

HERE = os.path.dirname(os.path.abspath(__file__))
DIR = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure")

COPPER_BULB = ("minecraft:copper_bulb", {"lit": "true", "powered": "false"})


# ---- Decorations from other mods, each with a stand-in for when that mod isn't installed ----
def stove_and_pot(B, x, y, z, facing="north"):
    """Farmer's Delight stove with a cooking pot on it (a lit smoker and a cauldron without)."""
    B.mod(x, y, z, "farmersdelight:stove", {"facing": facing, "lit": "true"}, "minecraft:smoker",
          {"facing": facing, "lit": "true"})
    B.mod(x, y + 1, z, "farmersdelight:cooking_pot", {"facing": facing, "support": "none"}, "minecraft:cauldron")


def cutting_board(B, x, y, z, facing="north"):
    B.mod(x, y, z, "farmersdelight:cutting_board", {"facing": facing}, "minecraft:air")


def rug(B, x0, x1, z0, z1, y):
    """Farmer's Delight canvas rug (moss carpet without)."""
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, y, z) not in B.blocks or B.blocks[(x, y, z)][0] == "minecraft:air":
                B.mod(x, y, z, "farmersdelight:canvas_rug", {}, "minecraft:moss_carpet")


def jar(B, x, y, z):
    B.mod(x, y, z, "supplementaries:jar", {}, "minecraft:decorated_pot", {"facing": "north"})


def sack(B, x, y, z):
    B.mod(x, y, z, "supplementaries:sack", {"open": "false"}, "minecraft:barrel", {"facing": "up", "open": "false"})


def book_pile(B, x, y, z):
    B.mod(x, y, z, "supplementaries:book_pile", {}, "minecraft:air")


def candles(B, x, y, z, n=3):
    B.mod(x, y, z, "supplementaries:candle_holder", {"face": "floor", "facing": "north", "candles": str(n),
                                                     "lit": "true"},
          "minecraft:candle", {"candles": str(n), "lit": "true"})


def notice_board(B, x, y, z, facing):
    B.mod(x, y, z, "supplementaries:notice_board", {"facing": facing, "has_book": "false"}, "minecraft:air")


def stool(B, x, y, z, color="green"):
    B.mod(x, y, z, f"quark:{color}_stool", {"big": "false"}, "minecraft:spruce_slab", {"type": "bottom"})


def crate(B, x, y, z, kind="cabbage"):
    B.mod(x, y, z, f"farmersdelight:{kind}_crate", {}, "minecraft:barrel", {"facing": "up", "open": "false"})


def straw(B, x, y, z):
    B.mod(x, y, z, "farmersdelight:straw_bale", {"axis": "y"}, "minecraft:hay_block", {"axis": "y"})


def bed(B, x, y, z, color="red"):
    """A bed lying along +z: foot at z, head at z+1."""
    B.put(x, y, z, f"minecraft:{color}_bed", {"facing": "south", "part": "foot", "occupied": "false"})
    B.put(x, y, z + 1, f"minecraft:{color}_bed", {"facing": "south", "part": "head", "occupied": "false"})


def carpet(B, x0, x1, z0, z1, y, name="minecraft:moss_carpet"):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, y, z) not in B.blocks or B.blocks[(x, y, z)][0] == "minecraft:air":
                B.put(x, y, z, name)


MAT = "childrenofmu:reed_mat"


def mat(B, x, y, z):
    B.put(x, y, z, MAT)


# ---- Drying lines: fish and bugs strung up everywhere (frogs eat bugs) ----
STRUNG = ["childrenofmu:strung_fish", "childrenofmu:strung_bugs"]


def drying_line(B, x0, z0, x1, z1, y, posts=True):
    """
    A line between two posts standing on the floor at height y (the first air above it): posts 3 tall, a fence rail
    along the top, fish and bugs hanging under the rail. Runs along x or z.
    """
    cells = [(x, z0) for x in range(min(x0, x1), max(x0, x1) + 1)] if z0 == z1 else \
            [(x0, z) for z in range(min(z0, z1), max(z0, z1) + 1)]
    if posts:
        for x, z in (cells[0], cells[-1]):
            for dy in range(3):
                B.fence(x, y + dy, z, "minecraft:mangrove_fence")
    for x, z in cells[1:-1]:
        B.fence(x, y + 2, z, "minecraft:mangrove_fence")
        if B.rng.random() < 0.8:
            B.put(x, y + 1, z, B.rng.choice(STRUNG))


def frog_hut():
    B = FrogBuilder(21, seed=301)
    C = B.c
    h = B.hut(3, 3, 5, 4)
    f = h["floor"] + 1
    for z in (C + 1, C + 2):
        mat(B, C - 2, f, z)                 # frogs sleep curled up on reed mats, not in beds
    mat(B, C - 1, f, C + 2)
    B.barrel(C + 2, f, C + 2)
    cutting_board(B, C + 2, f + 1, C + 2, "west")
    stove_and_pot(B, C + 2, f, C + 1, "west")
    sack(B, C - 2, f, C - 2)
    jar(B, C - 2, f + 1, C - 2)
    stool(B, C + 1, f, C - 1)
    rug(B, C - 1, C + 1, C - 1, C + 1, f)
    B.lantern_below(C, h["top"], C, rope=1)
    for x in (C - 2, C + 2):
        B.potted(x, f, h["pz0"] + 1)
    B.chimney(C + 2, C + 2, h["top"] + 1, 5)
    B.entities.append((C + 0.5, f + 0.05, C - 1 + 0.5, {"id": "childrenofmu:frog_folk"}))
    B.entities.append((C - 1 + 0.5, f + 0.05, h["pz0"] + 1.5, {"id": "childrenofmu:frog_folk", "Variant": "froglet"}))
    B.overgrow(1, h["top"])
    B.finish()
    return B, h["door_dist"]


def home_inside(B, C, f, top, r):
    """Furnish a round home: mats to sleep on, a stove and pot, a sack and a jar, a rug, a hanging lantern."""
    mat(B, C - r + 1, f, C + 1)
    mat(B, C - r + 1, f, C)
    mat(B, C - r + 2, f, C + 2)
    stove_and_pot(B, C + r - 1, f, C + 1, "west")
    B.barrel(C + r - 1, f, C)
    cutting_board(B, C + r - 1, f + 1, C, "west")
    sack(B, C - 1, f, C + r - 1)
    jar(B, C + 1, f, C + r - 1)
    rug(B, C - 1, C + 1, C - 1, C, f)
    B.lantern_below(C, top, C, rope=1)


def round_home():
    """A round home with a thatched cone roof."""
    B = FrogBuilder(21, seed=351)
    C = B.c
    h = B.round_hut(4, 5, 4)
    home_inside(B, C, h["floor"] + 1, h["top"], 4)
    B.entities.append((C + 0.5, h["floor"] + 1.05, C - 1 + 0.5, {"id": "childrenofmu:frog_folk"}))
    B.entities.append((C - 1 + 0.5, h["floor"] + 1.05, h["pz0"] + 1.5, {"id": "childrenofmu:frog_folk", "Variant": "froglet"}))
    B.overgrow(1, h["top"])
    B.finish()
    return B, h["door_dist"]


def tall_home():
    """A small round home under a tall, pointed witch's-hat roof."""
    B = FrogBuilder(21, seed=353)
    C = B.c
    h = B.round_hut(3, 6, 4, steep=2)
    f = h["floor"] + 1
    mat(B, C - 2, f, C + 1)
    mat(B, C - 2, f, C)
    stove_and_pot(B, C + 2, f, C, "west")
    jar(B, C, f, C + 2)
    rug(B, C - 1, C, C - 1, C, f)
    B.lantern_below(C, h["top"], C, rope=1)
    B.entities.append((C + 0.5, f + 0.05, C - 1 + 0.5, {"id": "childrenofmu:frog_folk"}))
    B.overgrow(1, h["top"])
    B.finish()
    return B, h["door_dist"]


def town_hall():
    B = FrogBuilder(25, seed=303)
    C = B.c
    h = B.hut(5, 4, 6, 6, porch=3)
    f = h["floor"] + 1
    # The treasury: a counter, ledgers, and the town's strongboxes.
    for x in range(C - 2, C + 3):
        B.put(x, f, C + 1, "minecraft:spruce_planks")
    B.put(C, f + 1, C + 1, "minecraft:lectern", {"facing": "north", "has_book": "false", "powered": "false"})
    book_pile(B, C - 2, f + 1, C + 1)
    candles(B, C + 2, f + 1, C + 1)
    B.mod(C - 1, f + 1, C + 1, "supplementaries:hourglass", {"facing": "up"}, "minecraft:air")
    notice_board(B, C + 3, f + 1, h["z0"] + 1, "south")
    notice_board(B, C - 3, f + 1, h["z0"] + 1, "south")
    for x in (C - 1, C + 1):
        B.idol(x, f, h["pz0"] + 1, "north")       # two stone frogs either side of the steps
    for x in (C - 4, C + 4):
        for z in (C + 3, C + 2):
            B.barrel(x, f, z)
        B.put(x, f + 1, C + 3, "minecraft:chiseled_bookshelf",
              {"facing": "north" if x < C else "north", "slot_0_occupied": "true", "slot_1_occupied": "false",
               "slot_2_occupied": "true", "slot_3_occupied": "true", "slot_4_occupied": "false",
               "slot_5_occupied": "true"})
    B.put(C - 4, f, C - 3, "minecraft:raw_gold_block")
    carpet(B, C - 3, C + 3, C - 3, C - 1, f, "minecraft:green_carpet")
    for x in (C - 3, C + 3):
        B.lantern_below(x, h["top"], C, rope=2)
    B.lantern_below(C, h["top"], C - 2, rope=1)
    # A bell on the porch, to call the town together.
    B.fence(C + 3, f, h["pz0"] + 1)
    B.put(C + 3, f + 1, h["pz0"] + 1, "minecraft:bell", {"attachment": "floor", "facing": "north", "powered": "false"})
    B.chimney(C - 4, C + 3, h["top"] + 1, 6)
    B.entities.append((C + 0.5, f + 0.05, C + 2.5, {"id": "childrenofmu:frog_folk", "Variant": "treasurer"}))
    B.entities.append((C - 2 + 0.5, f + 0.05, C - 2 + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "elder"}))
    B.overgrow(1, h["top"])
    B.finish()
    return B, h["door_dist"]


def post_office():
    B = FrogBuilder(21, seed=305)
    C = B.c
    h = B.hut(3, 3, 5, 4)
    f = h["floor"] + 1
    # The post box stands on the porch beside the door; parcels are stacked inside.
    B.put(C + 2, f, h["z0"] - 1, "childrenofmu:post_box")
    for x, z in ((C - 2, C + 2), (C - 2, C + 1), (C + 2, C + 2), (C - 1, C + 2)):
        sack(B, x, f, z)
    sack(B, C - 2, f + 1, C + 2)
    B.mod(C + 1, f, C + 2, "farmersdelight:rice_bag", {}, "minecraft:barrel", {"facing": "up", "open": "false"})
    notice_board(B, C - 2, f + 1, h["z0"] + 1, "south")
    B.put(C + 2, f, C, "minecraft:cartography_table")
    B.put(C + 2, f, C - 1, "minecraft:loom")
    B.lantern_below(C, h["top"], C)
    # A tall landing pole for the couriers, with a little platform and a windsock.
    px, pz = h["x1"] + 1, h["z1"] + 1
    peak = max(y for (x, y, z) in B.blocks if x == C and z == C)
    for y in range(0, peak + 4):
        B.log(px, y, pz, kind="mangrove_log")
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if (dx, dz) != (0, 0):
                B.put(px + dx, peak + 3, pz + dz, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
    B.fence(px, peak + 4, pz, "minecraft:mangrove_fence")
    B.put(px, peak + 5, pz, "minecraft:orange_wool")
    B.lily(px + 1, peak + 4, pz + 1)
    B.entities.append((C + 0.5, f + 0.05, C + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "jumper"}))
    B.entities.append((C - 1 + 0.5, f + 0.05, h["pz0"] + 1.5, {"id": "childrenofmu:frog_folk", "Variant": "jumper"}))
    B.overgrow(1, h["top"])
    B.finish()
    return B, h["door_dist"]


def guard_tower():
    """A lookout: a small round room high on braced stilts under a tall pointed roof, open on every side."""
    B = FrogBuilder(17, seed=307)
    C = B.c
    h = B.round_hut(2, 11, 3, porch=2, screens=False, steep=2)
    f = h["floor"] + 1
    for x, z in ((C + 2, C), (C - 2, C), (C, C + 2)):
        B.air(x, f + 1, z)                                    # open lookout windows
        B.air(x, f + 2, z)
    for y in (3, 7):                                          # cross-bracing between the stilts
        for x in range(C - 2, C + 3):
            B.log(x, y, C - 2, axis="x")
            B.log(x, y, C + 2, axis="x")
        for z in range(C - 1, C + 2):
            B.log(C - 2, y, z, axis="z")
            B.log(C + 2, y, z, axis="z")
    mat(B, C - 1, f, C + 1)
    B.lantern_below(C, h["top"], C)
    B.entities.append((C + 0.5, f + 0.05, C + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "guard"}))
    B.overgrow(1, h["top"], chance=0.2)
    B.finish()
    return B, h["door_dist"]


def potion_shop():
    """The Potioneer's shop: a round hut under a giant red mushroom cap, brewing stands and bubbling cauldrons."""
    B = FrogBuilder(23, seed=309)
    C = B.c
    h = B.round_hut(4, 5, 4, roof="mushroom")
    f = h["floor"] + 1
    for x in (C - 2, C + 2):
        B.put(x, f, C + 3, "minecraft:spruce_planks")
        B.put(x, f + 1, C + 3, "minecraft:brewing_stand", {"has_bottle_0": "true", "has_bottle_1": "false",
                                                           "has_bottle_2": "true"})
    B.put(C, f, C + 3, "minecraft:water_cauldron", {"level": "3"})
    B.put(C - 1, f, C + 3, "minecraft:barrel", {"facing": "up", "open": "false"})
    B.put(C + 1, f, C + 3, "minecraft:barrel", {"facing": "up", "open": "false"})
    B.potted(C - 1, f + 1, C + 3, "red_mushroom")
    B.potted(C + 1, f + 1, C + 3, "crimson_fungus")
    jar(B, C + 3, f, C - 1)
    jar(B, C + 3, f + 1, C - 1)
    candles(B, C - 3, f, C - 1, 2)
    book_pile(B, C - 3, f, C + 1)
    B.put(C + 3, f, C + 1, "minecraft:cauldron")
    carpet(B, C - 1, C + 1, C - 1, C, f, "minecraft:lime_carpet")
    mat(B, C - 2, f, C + 1)
    B.lantern_below(C, h["top"], C, rope=1)
    B.entities.append((C + 0.5, f + 0.05, C + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "potioneer"}))
    B.overgrow(1, h["top"])
    B.finish()
    return B, h["door_dist"]


def coppersmith():
    """The Coppersmith's forge: a round workshop under a copper cone roof with a lightning rod on the tip."""
    B = FrogBuilder(21, seed=311)
    C = B.c
    h = B.round_hut(4, 5, 4, roof="copper")
    f = h["floor"] + 1
    B.put(C - 3, f, C + 1, "minecraft:blast_furnace", {"facing": "east", "lit": "true"})
    B.put(C - 3, f, C, "minecraft:anvil", {"facing": "north"})
    for x in range(C - 1, C + 3):
        B.put(x, f, C + 3, "minecraft:cut_copper" if x % 2 else "minecraft:exposed_cut_copper")
    B.put(C + 3, f, C + 1, "minecraft:grindstone", {"face": "floor", "facing": "west"})
    B.put(C + 3, f, C - 1, "minecraft:copper_block")
    B.put(C + 3, f + 1, C - 1, *COPPER_BULB)
    B.put(C, h["top"], C, *COPPER_BULB)
    mat(B, C - 2, f, C + 2)
    B.entities.append((C + 0.5, f + 0.05, C + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "coppersmith"}))
    B.overgrow(1, h["top"])
    B.finish()
    return B, h["door_dist"]


def chitinier_shop():
    B = FrogBuilder(21, seed=313)
    C = B.c
    h = B.hut(3, 3, 5, 4)
    f = h["floor"] + 1
    for x in (h["x0"] + 1, h["x1"] - 1):
        B.put(x, f, h["pz0"] + 1, "minecraft:beehive", {"facing": "north", "honey_level": "5"})
    B.put(C - 2, f, C + 2, "minecraft:honey_block")
    B.put(C - 1, f, C + 2, "minecraft:honeycomb_block")
    B.put(C + 2, f, C + 2, "minecraft:honeycomb_block")
    B.put(C + 1, f, C + 2, "minecraft:barrel", {"facing": "up", "open": "false"})
    for x in (C - 2, C - 1, C + 2):
        jar(B, x, f + 1, C + 2)
    B.put(C + 2, h["top"], C + 2, "minecraft:cobweb")
    B.put(C - 2, h["top"], C - 2, "minecraft:cobweb")
    carpet(B, C - 1, C + 1, C - 1, C, f, "minecraft:orange_carpet")
    B.lantern_below(C, h["top"], C)
    B.chimney(C + 2, C - 2, h["top"] + 1, 4)
    B.entities.append((C + 0.5, f + 0.05, C + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "chitinier"}))
    B.overgrow(1, h["top"])
    B.finish()
    return B, h["door_dist"]


def pump_house():
    B = FrogBuilder(21, seed=315)
    C = B.c
    h = B.hut(3, 2, 4, 4)
    f = h["floor"] + 1
    # Copper pipes running along the walls and down through the floor into the swamp.
    for x in range(C - 2, C + 3):
        B.put(x, f + 2, C + 1, "minecraft:waxed_copper_grate")
    for y in range(0, f + 2):
        B.put(C + 2, y, C + 3, "minecraft:waxed_exposed_copper_grate")
    B.put(C - 2, f, C + 1, "minecraft:water_cauldron", {"level": "3"})
    B.put(C - 1, f, C + 1, "minecraft:cauldron")
    B.put(C + 2, f, C + 1, *COPPER_BULB)
    B.put(C + 2, f, C - 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    B.lantern_below(C, h["top"], C)
    B.entities.append((C + 0.5, f + 0.05, C - 0.5, {"id": "childrenofmu:frog_folk", "Variant": "plumber"}))
    B.overgrow(1, h["top"])
    B.finish()
    return B, h["door_dist"]


def fishing_dock():
    B = FrogBuilder(17, seed=317)
    C = B.c
    # A wide dock at the water, a shelter on posts, and lines out over the water.
    x0, x1, z0, z1 = C - 4, C + 4, C - 5, C + 4
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            B.put(x, 0, z, FLOOR)
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1), (C, z0), (C, z1)):
        B.log(x, 0, z)
    sx0, sx1, sz0, sz1 = C - 2, C + 2, C - 1, C + 3
    for x, z in ((sx0, sz0), (sx1, sz0), (sx0, sz1), (sx1, sz1)):
        for y in range(1, 4):
            B.log(x, y, z)
    B.hip_roof(sx0 - 1, sx1 + 1, sz0 - 1, sz1 + 1, 4)
    for x in range(x0, x1 + 1):
        if x not in (C - 1, C, C + 1):
            B.fence(x, 1, z1)
    for z in range(z0, z1 + 1):
        B.fence(x0, 1, z)
        B.fence(x1, 1, z)
    for x in (x0, x1):
        B.lantern_on(x, 2, z0)
    B.barrel(sx0 + 1, 1, sz1)
    cutting_board(B, sx0 + 1, 2, sz1, "north")
    crate(B, sx1 - 1, 1, sz0 + 1, "cabbage")
    straw(B, sx0 + 1, 1, sz0 + 1)
    # Nets hung to dry along the shelter's sides, cattails at the dock's corners, a stone frog on the end post.
    for z in range(sz0 + 1, sz1):
        for y in (1, 2):
            B.net(sx0 - 1, y, z, "west")
            B.net(sx1 + 1, y, z, "east")
    for x, z in ((x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1)):
        B.cattails(x, 1, z)
    B.put(sx1 - 1, 1, sz1, "minecraft:smoker", {"facing": "north", "lit": "true"})
    B.put(C, 1, sz1, "minecraft:composter", {"level": "4"})
    B.lantern_below(C, 3, C + 1)
    B.entities.append((C + 0.5, 1.05, C + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "fisher"}))
    B.entities.append((C - 2 + 0.5, 1.05, z0 + 1.5, {"id": "childrenofmu:frog_folk", "Variant": "froglet"}))
    B.overgrow(0, 5, chance=0.12)
    B.finish()
    return B, C - z0


def great_mangrove():
    """
    The heart of a frog town: a giant mangrove on arching prop roots. The Town Hall is carved out inside its trunk
    (the Treasurer and the Elder live there), a wide deck rings it, and stepping pads spiral up its side to a lookout
    under the canopy. Fish and bugs hang drying on the deck; lanterns hang from the branches.
    """
    B = FrogBuilder(41, seed=331)
    C = B.c
    rng = B.rng
    MLOG = "minecraft:mangrove_log"
    LEAF = ("minecraft:mangrove_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})
    deck, hall_top, look, trunk_top = 6, 12, 16, 27

    def r_at(y):
        if y <= deck:
            return 4.8 - y * 0.05
        if y <= hall_top + 1:
            return 5.3
        return max(2.3, 5.1 - (y - hall_top - 1) * 0.22)

    def dist(x, z):
        return math.hypot(x - C, z - C)

    # The trunk, hollow at the hall.
    for y in range(0, trunk_top):
        r = r_at(y)
        for x in range(C - 6, C + 7):
            for z in range(C - 6, C + 7):
                d = dist(x, z)
                if d > r:
                    continue
                if deck < y <= hall_top and d <= r - 1.4:
                    B.air(x, y, z)
                elif y == deck and d <= r - 1.4:
                    B.put(x, y, z, FLOOR)
                else:
                    B.put(x, y, z, "minecraft:mangrove_wood" if rng.random() < 0.25 and d > r - 1.2 else MLOG,
                          {"axis": "y"})
    # The deck ringing the trunk, on posts, with a reed railing.
    R = 9.5
    for x in range(C - 10, C + 11):
        for z in range(C - 10, C + 11):
            d = dist(x, z)
            if r_at(deck) < d <= R:
                B.put(x, deck, z, FLOOR)
                if d > R - 1 and not (x == C and z < C):
                    B.put(x, deck + 1, z, SCREEN)
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        x, z = round(C + math.cos(a) * 9), round(C + math.sin(a) * 9)
        for y in range(0, deck):
            B.log(x, y, z, kind="mangrove_log")
        B.fence(x, deck + 1, z, "minecraft:mangrove_fence")
        B.lantern_on(x, deck + 2, z)
    # Prop roots arching out from the trunk into the water (none on the north, where the dock is).
    for k in range(11):
        a = k * 2 * math.pi / 11 + rng.random() * 0.3
        if abs(math.atan2(math.sin(a + math.pi / 2), math.cos(a + math.pi / 2))) < 0.5:
            continue
        r0, r1, ys = 4.0, 11.5 + rng.random() * 2.5, 4 + rng.randint(0, 1)
        last = None
        for i in range(60):
            t = i / 59
            rr = r0 + (r1 - r0) * t
            y = round(ys * (1 - t) ** 1.6 + 2.5 * math.sin(math.pi * t) * 0.6)
            x, z = round(C + math.cos(a) * rr), round(C + math.sin(a) * rr)
            if (x, y, z) == last:
                continue
            last = (x, y, z)
            if y <= 0:
                B.log(x, 0, z, kind="mangrove_log")
            elif B.is_free(x, y, z) or B.name_at(x, y, z) == "minecraft:air":
                B.put(x, y, z, "minecraft:mangrove_roots", {"waterlogged": "false"})
        B.log(round(C + math.cos(a) * r1), 0, round(C + math.sin(a) * r1), kind="mangrove_log")

    # The treasury vault, hollowed out of the trunk under the hall: an iron hatch behind the counter, a ladder down,
    # iron bars, a safe, and the town's (decorative: they only drop themselves) gold and emeralds.
    for y in range(2, deck):
        for x in range(C - 3, C + 4):
            for z in range(C - 3, C + 4):
                if dist(x, z) <= 3.0:
                    B.air(x, y, z)
    B.put(C - 1, deck, C + 2, "minecraft:iron_trapdoor", {"facing": "north", "half": "top", "open": "false",
                                                        "powered": "false", "waterlogged": "false"})
    B.ladder(C - 1, C + 2, 2, deck - 1, "north")
    for x, z in ((C - 2, C - 1), (C - 1, C - 2), (C, C - 2), (C + 1, C - 2), (C + 2, C - 1), (C + 2, C), (C - 2, C)):
        B.put(x, 2, z, "childrenofmu:inlaid_gold" if (x + z) % 2 else "childrenofmu:inlaid_emerald")
    B.put(C - 1, 3, C - 2, "childrenofmu:inlaid_gold")
    B.put(C + 2, 3, C - 1, "childrenofmu:inlaid_emerald")
    B.put(C, 2, C - 1, "childrenofmu:inlaid_gold")
    # The vault's front: the iron door framed in stone pillars (it sits flush in the stone, not out of line with the
    # bars), bars beyond, and a lever by the door on the outside.
    for x in range(C - 2, C + 3):
        for y in (2, 3):
            if x in (C - 2, C):
                B.put(x, y, C + 1, "minecraft:polished_deepslate")
            elif x != C - 1:
                B.put(x, y, C + 1, "minecraft:iron_bars", {})
    for x in range(C - 2, C + 3):
        B.put(x, 4, C + 1, "minecraft:polished_deepslate")
    B.put(C - 2, 3, C + 2, "minecraft:lever", {"face": "wall", "facing": "south", "powered": "false"})
    B.put(C - 1, 2, C + 1, "minecraft:iron_door", {"facing": "south", "half": "lower", "hinge": "left", "open": "false",
                                                  "powered": "false"})
    B.put(C - 1, 3, C + 1, "minecraft:iron_door", {"facing": "south", "half": "upper", "hinge": "left", "open": "false",
                                                  "powered": "false"})
    B.mod(C + 1, 2, C + 2, "supplementaries:safe", {"facing": "north", "open": "false"}, "minecraft:barrel",
          {"facing": "up", "open": "false"})
    B.lantern_below(C, 5, C - 1)

    # The hall's door (north) and windows.
    for y in (deck + 1, deck + 2):
        for z in range(C - 6, C - 3):
            if B.name_at(C, y, z) in (MLOG, "minecraft:mangrove_wood"):
                B.air(C, y, z)
    B.door(C, deck + 1, round(C - r_at(deck + 1) + 0.6), "south")
    for ang in (0, math.pi / 2, math.pi, math.pi * 0.25, math.pi * 0.75):
        wx, wz = round(C + math.cos(ang) * 4.7), round(C + math.sin(ang) * 4.7)
        for y in (deck + 3, deck + 4):
            B.put(wx, y, wz, SCREEN)
            ix, iz = round(C + math.cos(ang) * 3.8), round(C + math.sin(ang) * 3.8)
            if B.name_at(ix, y, iz) in (MLOG, "minecraft:mangrove_wood"):
                B.air(ix, y, iz)
    # Inside the hall: the treasury counter, ledgers, the town's gold.
    f = deck + 1
    for x in range(C - 2, C + 3):
        B.put(x, f, C + 1, "minecraft:spruce_planks")
    B.put(C, f + 1, C + 1, "minecraft:lectern", {"facing": "north", "has_book": "false", "powered": "false"})
    book_pile(B, C - 2, f + 1, C + 1)
    candles(B, C + 2, f + 1, C + 1)
    for x in (C - 2, C + 2):
        B.put(x, f, C + 3, "minecraft:chiseled_bookshelf",
              {"facing": "north", "slot_0_occupied": "true", "slot_1_occupied": "false", "slot_2_occupied": "true",
               "slot_3_occupied": "true", "slot_4_occupied": "false", "slot_5_occupied": "true"})
    B.put(C, f, C + 3, "minecraft:raw_gold_block")
    B.barrel(C - 3, f, C)
    rug(B, C - 2, C + 2, C - 2, C - 1, f)
    notice_board(B, C + 3, f + 1, C - 1, "west")
    for x, z in ((C - 2, C - 2), (C + 2, C - 2), (C, C)):
        B.lantern_below(x, hall_top, z, rope=1)
    for x in (C - 1, C + 1):
        B.idol(x, deck + 1, C - 7, "north")
    B.fence(C + 3, deck + 1, C - 7)
    B.put(C + 3, deck + 2, C - 7, "minecraft:bell", {"attachment": "floor", "facing": "north", "powered": "false"})
    # Drying lines on the deck, east and west.
    drying_line(B, C + 7, C - 3, C + 7, C + 3, deck + 1)
    drying_line(B, C - 7, C - 3, C - 7, C + 3, deck + 1)
    drying_line(B, C - 3, C + 7, C + 3, C + 7, deck + 1)

    # The lookout under the canopy, reached by pads spiralling up the trunk's side.
    for x in range(C - 8, C + 9):
        for z in range(C - 8, C + 9):
            d = dist(x, z)
            if r_at(look) < d <= 7.5:
                B.put(x, look, z, FLOOR)
                if d > 6.5:
                    B.put(x, look + 1, z, SCREEN)
    ring = {(x, z) for x in range(C - 9, C + 10) for z in range(C - 9, C + 10) if 7.6 < dist(x, z) <= 8.9}
    path = [(C + 8, C)]
    ang = lambda p: math.atan2(p[1] - C, p[0] - C) % (2 * math.pi)
    while len(path) < look - deck - 1:
        cx, cz = path[-1]
        options = [(cx + dx, cz + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))
                   if (cx + dx, cz + dz) in ring and (cx + dx, cz + dz) not in path
                   and (ang((cx + dx, cz + dz)) - ang((cx, cz))) % (2 * math.pi) < math.pi]
        if not options:
            break
        path.append(min(options, key=lambda p: (ang(p) - ang((cx, cz))) % (2 * math.pi)))
    for i, (x, z) in enumerate(path):
        h = deck + 1 + i
        B.put(x, h, z, "minecraft:mangrove_slab", {"type": "top", "waterlogged": "false"})
        for y in range(deck + 1, h):
            B.fence(x, y, z, "minecraft:mangrove_fence")
    # Open the lookout's railing where the last pad arrives.
    lx, lz = path[-1]
    for nx, nz in ((lx + 1, lz), (lx - 1, lz), (lx, lz + 1), (lx, lz - 1)):
        if dist(nx, nz) <= 7.5:
            B.air(nx, look + 1, nz)
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        x, z = round(C + math.cos(a) * 7), round(C + math.sin(a) * 7)
        B.fence(x, look + 1, z, "minecraft:mangrove_fence")
        B.lantern_on(x, look + 2, z)
    B.idol(C, look + 1, C - 6, "north")

    # Branches and the canopy.
    blobs = [(C, 27, C, 7)]
    for k in range(6):
        a = k * math.pi / 3 + rng.random() * 0.5
        y = 18 + rng.randint(0, 4)
        length = 8 + rng.randint(0, 4)
        x = z = None
        for i in range(2, length):
            x, z = round(C + math.cos(a) * i), round(C + math.sin(a) * i)
            yy = y + int(i * 0.45)
            axis = "x" if abs(math.cos(a)) > abs(math.sin(a)) else "z"
            B.put(x, yy, z, MLOG, {"axis": axis})
        blobs.append((x, y + int(length * 0.45) + 1, z, 4 + rng.randint(0, 1)))
    for bx, by, bz, br in blobs:
        for x in range(bx - br, bx + br + 1):
            for y in range(by - br // 2, by + br // 2 + 2):
                for z in range(bz - br, bz + br + 1):
                    dd = ((x - bx) / br) ** 2 + ((y - by) / (br * 0.6)) ** 2 + ((z - bz) / br) ** 2
                    if dd <= 1.0 and rng.random() > dd * 0.25 and B.is_free(x, y, z):
                        if y >= by + br // 2 and rng.random() < 0.2:
                            B.put(x, y, z, "minecraft:moss_block")
                        else:
                            B.put(x, y, z, *LEAF)
        # Lanterns hanging from under the leaves.
        for _ in range(3):
            x, z = bx + rng.randint(-br + 1, br - 1), bz + rng.randint(-br + 1, br - 1)
            low = min((y for (xx, y, zz), b in B.blocks.items() if xx == x and zz == z and b[0] == LEAF[0]), default=None)
            if low and low - 3 > look + 2:
                B.lantern_below(x, low - 1, z, rope=rng.randint(1, 3))

    # The dock to the north, and pilings up to the deck.
    pz0 = C - 9
    for x in range(C - 1, C + 2):
        for z in range(pz0 - 3, pz0):
            if (x, 0, z) not in B.blocks:
                B.put(x, 0, z, FLOOR)
    B.log(C - 1, 0, pz0 - 3, kind="mangrove_log")
    B.log(C + 1, 0, pz0 - 3, kind="mangrove_log")
    B.fence(C - 1, 1, pz0 - 3)
    B.lantern_on(C - 1, 2, pz0 - 3)
    B.cattails(C + 1, 1, pz0 - 3)
    if not B.pilings(C, pz0, deck, (C - 10, C + 10, pz0, C + 10), walkway=[(C, pz0 - 3), (C, pz0 - 2)],
                     dock=[(x, z) for x in range(C - 1, C + 2) for z in range(pz0 - 3, pz0)]):
        B.ladder(C, pz0 - 1, 1, deck, "north")

    B.entities.append((C + 0.5, f + 0.05, C + 2.5, {"id": "childrenofmu:frog_folk", "Variant": "treasurer"}))
    B.entities.append((C - 2 + 0.5, f + 0.05, C - 1 + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "elder"}))
    B.entities.append((C + 0.5, look + 1.05, C + 6.5, {"id": "childrenofmu:frog_folk", "Variant": "guard"}))
    for x, z in ((C - 5, C - 6), (C + 6, C + 5)):
        B.entities.append((x + 0.5, f + 0.05, z + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "froglet"}))
    B.overgrow(1, trunk_top, chance=0.08)
    B.finish()
    return B, C - (pz0 - 3)


def torpor_house():
    """
    Where the Frog folk spend the night: a long, low, damp house with a mud floor and rows of reed mats. At dusk
    everyone but the Guards drifts in, finds a mat, tucks in their head and limbs and goes torpid until morning.
    Barely lit, so it stays sleepy; vines and moss inside.
    """
    B = FrogBuilder(25, seed=343)
    C = B.c
    h = B.hut(6, 4, 4, 4, porch=3, screens=True)
    f = h["floor"] + 1
    rng = B.rng
    for x in range(h["x0"] + 1, h["x1"]):
        for z in range(h["z0"] + 1, h["z1"]):
            B.put(x, h["floor"], z, "minecraft:mud" if rng.random() < 0.6 else "minecraft:packed_mud")
    # Rows of mats, with an aisle down the middle from the door.
    for x in range(h["x0"] + 1, h["x1"]):
        for z in range(h["z0"] + 1, h["z1"]):
            if x != C and (x - h["x0"]) % 2 == 1:
                mat(B, x, f, z)
            elif x != C and rng.random() < 0.3:
                B.put(x, f, z, "minecraft:moss_carpet")
    for x in (h["x0"] + 1, h["x1"] - 1):
        B.put(x, f, h["z1"] - 1, "minecraft:water_cauldron", {"level": "2"})
    B.lantern_below(C, h["top"], C, rope=1)
    for y in range(f + 1, h["top"] + 1):
        for x in range(h["x0"] + 1, h["x1"]):
            if rng.random() < 0.12:
                B.vine(x, y, h["z1"] - 1, "south")
    B.overgrow(1, h["top"], chance=0.15)
    B.finish()
    return B, h["door_dist"]


def schoolhouse():
    """
    The schoolhouse: one room, rows of desks facing a blackboard, the Schoolmarm's lectern and globe, shelves of
    books, and a school bell on a post by the door. The froglets come here in the daytime.
    """
    B = FrogBuilder(25, seed=345)
    C = B.c
    h = B.hut(5, 4, 5, 4, porch=3, screens=False)
    f = h["floor"] + 1
    z1 = h["z1"]
    # The blackboard on the back wall, the Schoolmarm's lectern before it.
    for x in (C - 1, C, C + 1):
        B.mod(x, f + 1, z1 - 1, "supplementaries:blackboard", {"facing": "north"}, "minecraft:air")
    B.put(C + 2, f, z1 - 1, "minecraft:lectern", {"facing": "north", "has_book": "true", "powered": "false"})
    B.mod(C - 3, f, z1 - 1, "supplementaries:globe", {"facing": "north"}, "minecraft:potted_fern")
    book_pile(B, C - 2, f, z1 - 1)
    # Desks (a top slab) with a little stair seat behind each, facing the board.
    for row in (C - 2, C):
        for x in (C - 3, C - 1, C + 1, C + 3):
            B.put(x, f, row + 1, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
            B.put(x, f, row, "minecraft:spruce_stairs", {"facing": "north", "half": "bottom", "shape": "straight",
                                                        "waterlogged": "false"})
    for x in (h["x0"] + 1, h["x1"] - 1):
        B.mod(x, f, h["z0"] + 1, "quark:spruce_bookshelf", {}, "minecraft:bookshelf")
        B.mod(x, f + 1, h["z0"] + 1, "quark:spruce_bookshelf", {}, "minecraft:bookshelf")
    B.lantern_below(C, h["top"], C, rope=1)
    B.lantern_below(C, h["top"], z1 - 2, rope=1)
    # A belfry on the roof's peak: four posts, a little pointed cap, and the school bell hanging inside.
    peak = max(y for (x, y, z) in B.blocks if x == C and z == C)
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        for y in range(peak + 1, peak + 4):
            B.fence(C + dx, y, C + dz, "minecraft:dark_oak_fence")
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            B.put(C + dx, peak + 4, C + dz, "minecraft:dark_oak_planks")
    for dx, dz, fc in ((0, -2, "south"), (0, 2, "north"), (-2, 0, "east"), (2, 0, "west")):
        for k in (-1, 0, 1):
            x, z = (C + k, C + dz) if dx == 0 else (C + dx, C + k)
            B.stair(x, peak + 4, z, "minecraft:dark_oak_stairs", fc)
    B.put(C, peak + 5, C, "minecraft:dark_oak_planks")
    B.put(C, peak + 6, C, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    B.put(C, peak + 3, C, "minecraft:bell", {"attachment": "ceiling", "facing": "north", "powered": "false"})
    B.entities.append((C + 2 + 0.5, f + 0.05, z1 - 2 + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "schoolmarm"}))
    for x in (C - 3, C + 1, C + 3):
        B.entities.append((x + 0.5, f + 0.55, C + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "froglet"}))
    B.overgrow(1, h["top"], chance=0.1)
    B.finish()
    return B, h["door_dist"]


def restaurant():
    """
    The Chitinier's restaurant: the swankiest building in town, all copper. A copper door, copper bulbs, copper
    trim and shutters, a copper chimney; a kitchen of stoves, pots and cutting boards behind a copper pass, where the
    Cooks rush about making each dish fresh to order; a dining room of tables and stools out front.
    """
    B = FrogBuilder(25, seed=339)
    C = B.c
    h = B.hut(5, 4, 5, 5, porch=3, screens=False)
    f = h["floor"] + 1
    x0, x1, z0, z1 = h["x0"], h["x1"], h["z0"], h["z1"]
    # Copper: the door, trim along the wall tops, shutters by the windows, bulbs on the corners.
    for half, dy in (("lower", 0), ("upper", 1)):
        B.put(C, f + dy, z0, "minecraft:waxed_copper_door", {"facing": "south", "half": half, "hinge": "left",
                                                            "open": "false", "powered": "false"})
    for x in range(x0 + 1, x1):
        for z in (z0, z1):
            B.put(x, h["top"], z, "minecraft:waxed_cut_copper")
    for z in range(z0 + 1, z1):
        for x in (x0, x1):
            B.put(x, h["top"], z, "minecraft:waxed_cut_copper")
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        B.put(x, h["top"], z, "minecraft:waxed_chiseled_copper")
    for z in (C - 2, C + 2):
        B.put(x0 - 1, f + 1, z, "minecraft:waxed_copper_trapdoor", {"facing": "west", "half": "top", "open": "true",
                                                                 "powered": "false", "waterlogged": "false"})
        B.put(x1 + 1, f + 1, z, "minecraft:waxed_copper_trapdoor", {"facing": "east", "half": "top", "open": "true",
                                                                 "powered": "false", "waterlogged": "false"})
    # The kitchen, along the back.
    stove_and_pot(B, C - 4, f, C + 3, "north")
    stove_and_pot(B, C - 2, f, C + 3, "north")
    B.mod(C, f, C + 3, "farmersdelight:stove", {"facing": "north", "lit": "true"}, "minecraft:smoker",
          {"facing": "north", "lit": "true"})
    B.mod(C, f + 1, C + 3, "farmersdelight:skillet", {"facing": "north", "support": "true"}, "minecraft:air")
    for x in (C - 3, C - 1, C + 1):
        B.put(x, f, C + 3, "minecraft:waxed_cut_copper")
        cutting_board(B, x, f + 1, C + 3, "north")
    B.put(C + 2, f, C + 3, "minecraft:smoker", {"facing": "north", "lit": "true"})
    B.put(C + 3, f, C + 3, "minecraft:water_cauldron", {"level": "3"})
    B.put(C + 4, f, C + 3, "minecraft:barrel", {"facing": "up", "open": "false"})
    jar(B, C + 4, f + 1, C + 3)
    sack(B, C + 4, f, C + 2)
    for x in range(C - 4, C + 5, 2):
        B.put(x, h["top"], C + 2, "minecraft:waxed_copper_bulb", {"lit": "true", "powered": "false"})
    # The pass: a copper counter across the room, open at one end, candles on it.
    for x in range(C - 4, C + 4):
        B.put(x, f, C, "minecraft:waxed_cut_copper" if x % 2 else "minecraft:waxed_copper_grate")
    candles(B, C - 3, f + 1, C)
    candles(B, C + 1, f + 1, C, 2)
    # The dining room: tables (a post with a gold plate on top) and stools.
    for tx in (C - 3, C + 2):
        B.fence(tx, f, C - 2, "minecraft:dark_oak_fence")
        B.put(tx, f + 1, C - 2, "minecraft:light_weighted_pressure_plate", {"power": "0"})
        stool(B, tx - 1, f, C - 2, "red")
        stool(B, tx + 1, f, C - 2, "red")
    B.put(C - 4, f, C - 3, "minecraft:potted_azure_bluet")
    B.put(C + 4, f, C - 3, "minecraft:potted_blue_orchid")
    notice_board(B, C + 2, f + 1, z0 + 1, "south")
    for x in (C - 3, C + 2):
        B.lantern_below(x, h["top"], C - 2, rope=1)
    # A fancy porch: outdoor tables, lily lanterns, and the menu by the door.
    B.fence(x0 + 2, f, h["pz0"] + 1, "minecraft:dark_oak_fence")
    B.put(x0 + 2, f + 1, h["pz0"] + 1, "minecraft:light_weighted_pressure_plate", {"power": "0"})
    stool(B, x0 + 1, f, h["pz0"] + 1, "red")
    B.fence(x1 - 2, f, h["pz0"] + 1, "minecraft:dark_oak_fence")
    B.put(x1 - 2, f + 1, h["pz0"] + 1, "minecraft:light_weighted_pressure_plate", {"power": "0"})
    stool(B, x1 - 1, f, h["pz0"] + 1, "red")
    # A copper chimney with a lightning rod on top.
    top_y = max(y for (x, y, z) in B.blocks if x == C - 4 and z == C + 3)
    for y in range(h["top"] + 1, top_y + 3):
        B.put(C - 4, y, C + 3, "minecraft:waxed_cut_copper" if y < top_y + 2 else "minecraft:waxed_exposed_cut_copper")
    B.put(C - 4, top_y + 3, C + 3, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    B.put(C + 4, top_y, C + 3, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false",
                                                       "waterlogged": "false"})
    B.entities.append((C + 0.5, f + 0.05, C - 1 + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "chitinier"}))
    for x in (C - 3, C + 1, C + 3):
        B.entities.append((x + 0.5, f + 0.05, C + 1.5, {"id": "childrenofmu:frog_folk", "Variant": "cook"}))
    B.overgrow(1, h["top"], chance=0.1)
    B.finish()
    return B, h["door_dist"]


def fish_market():
    """
    The fish market: an open stall on a dock, under a thatched roof, with the Fishmonger behind a counter of ice and
    barrels, and lines of fish and bugs drying all round it.
    """
    B = FrogBuilder(19, seed=341)
    C = B.c
    x0, x1, z0, z1 = C - 5, C + 5, C - 4, C + 4
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            B.put(x, 0, z, FLOOR)
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1), (C, z1)):
        B.log(x, 0, z, kind="mangrove_log")
    sx0, sx1, sz0, sz1 = C - 3, C + 3, C - 2, C + 3
    for x, z in ((sx0, sz0), (sx1, sz0), (sx0, sz1), (sx1, sz1)):
        for y in range(1, 4):
            B.log(x, y, z, kind="mangrove_log")
    B.hip_roof(sx0 - 1, sx1 + 1, sz0 - 1, sz1 + 1, 4)
    # The counter: packed ice for the fish, barrels and crates either side.
    for x in range(sx0 + 1, sx1):
        B.put(x, 1, sz0, "minecraft:packed_ice" if abs(x - C) <= 1 else "minecraft:barrel",
              None if abs(x - C) <= 1 else {"facing": "up", "open": "false"})
    for x in (C - 1, C + 1):
        B.put(x, 2, sz0, "childrenofmu:strung_fish")
    B.put(sx0 + 1, 1, sz1 - 1, "minecraft:water_cauldron", {"level": "3"})
    crate(B, sx1 - 1, 1, sz1 - 1, "cabbage")
    sack(B, sx1 - 1, 1, sz1 - 2)
    cutting_board(B, sx1 - 1, 2, sz1 - 1, "north")
    B.lantern_below(C, 3, C, rope=0)
    # Drying lines on every side of the dock.
    drying_line(B, x0, z0 + 1, x0, z1 - 1, 1)
    drying_line(B, x1, z0 + 1, x1, z1 - 1, 1)
    drying_line(B, x0 + 1, z1, x1 - 1, z1, 1)
    for x in (x0 + 1, x1 - 1):
        B.fence(x, 1, z0)
        B.lantern_on(x, 2, z0)
    for x, z in ((x0 + 2, z0 + 1), (x1 - 2, z0 + 1)):
        B.cattails(x, 1, z)
    B.entities.append((C + 0.5, 1.05, C + 0.5, {"id": "childrenofmu:frog_folk", "Variant": "fishmonger"}))
    B.entities.append((C - 2 + 0.5, 1.05, z0 + 1.5, {"id": "childrenofmu:frog_folk", "Variant": "froglet"}))
    B.overgrow(0, 5, chance=0.12)
    B.finish()
    return B, C - z0


PIECES = {"frog_hut_round": round_home, "frog_hut_tall": tall_home, "frog_great_mangrove": great_mangrove, "frog_fish_market": fish_market, "frog_torpor_house": torpor_house,
          "frog_schoolhouse": schoolhouse, "frog_hut": frog_hut, "frog_town_hall": town_hall, "frog_post": post_office, "frog_guard_tower": guard_tower,
          "frog_potion_shop": potion_shop, "frog_coppersmith": coppersmith, "frog_chitinier": restaurant,
          "frog_pump_house": pump_house, "frog_fishing_dock": fishing_dock}
blocks, SIZE = {}, 21


def build(name="frog_hut"):
    global blocks, SIZE
    B, _ = PIECES[name]()
    blocks, SIZE = B.blocks, B.size
    return B


def main():
    os.makedirs(DIR, exist_ok=True)
    for name, make in PIECES.items():
        B, door = make()
        n, p, top, mods = B.save_pair(os.path.join(DIR, name))
        print(f"{name}: {n} blocks, {p} types, size {B.size}x{top}x{B.size}, door {door}, {mods} mod blocks")


if __name__ == "__main__":
    main()
