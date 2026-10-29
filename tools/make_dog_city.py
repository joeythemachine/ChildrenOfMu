"""
Builds Dog City's structure templates (see worldgen/DogCityStructure.java for the plan they're laid out to):

  dc_keep            the monks' stone Keep on its raised plinth, one wide stair up, halls round an inner courtyard
  dc_dark_tower      the forbidden tower in the courtyard: a spiral stair to a bare chamber and the bandaged dog
  dc_tower_bow / dc_tower_sword / dc_tower_pike / dc_kennel     the four military towers
  dc_gatehouse       the gate in the southern point of the star
  16 workshops, four to a quarter, each with beds upstairs for the dogs who work there

Every dog has a bed, except the Biters.
Run: python3 make_dog_city.py
"""
import os

from dog_build import (THEMES, DogBuilder, STONE, PLINTH, PLASTER, FLOOR, SCREEN, TILE, DARK, POST, BEAM)

DIR = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "childrenofmu", "structure")

# A workshop: walls 1..11 (eleven square), eaves to 0..12.
S, X0, X1, Z0, Z1, C = 13, 1, 11, 1, 11, 6


QUARTER_THEME = {1: "cloth", 2: "yards", 3: "meat", 4: "hall"}


def workshop(seed, sign, workers, beds, furnish, floor_mat=FLOOR):
    """
    Two storeys: a stone workroom below, white plaster rooms above with a bed for each worker; a hip roof of dark
    tile. The door in the middle of the front; a ladder up in the back corner. furnish(B) fits out the workroom.
    workers: [(variant, x, z)] put in the workroom.
    """
    B = DogBuilder(S, seed)
    theme = THEMES[QUARTER_THEME[seed // 100]]
    B.theme = theme
    cols = theme["colors"]
    B.floor(X0, X1, Z0, Z1, 0, PLINTH)
    B.floor(X0 + 1, X1 - 1, Z0 + 1, Z1 - 1, 0, floor_mat)
    B.stone_storey(X0, X1, Z0, Z1, 1, 4)
    B.floor(X0 + 1, X1 - 1, Z0 + 1, Z1 - 1, 5, FLOOR)
    B.timber_storey(X0, X1, Z0, Z1, 5, 8)
    B.floor(X0 + 1, X1 - 1, Z0 + 1, Z1 - 1, 5, FLOOR)
    B.hip_roof(X0, X1, Z0, Z1, 9, mat=theme["roof"])
    # The door, its threshold, a lamp each side and the trade's sign.
    B.door(C, 1, Z0, "south", kind="dark_oak_door")
    B.put(C, 3, Z0, "minecraft:chiseled_stone_bricks")
    B.put(C, 0, Z0 - 1, PLINTH)                       # a threshold, flush with the road
    for x in (C - 2, C + 2):
        B.bracket_lantern(x, 3, Z0 - 1, "north")
    B.street_sign(C + 1, Z0, 2, sign, wood="dark_oak")
    for x in (X0 + 1, X1 - 1):
        B.wall_banner(x, 4, Z0 - 1, "north", cols[0])         # the quarter's colours either side of the front
    # The ladder up, in the back corner, through a hatch.
    B.ladder(X1 - 1, Z1 - 1, 1, 5, "north")
    # Upstairs: the beds along the back and the sides, a barrel at each bed's foot, a lantern.
    spots = [(3, Z1 - 2, "south"), (5, Z1 - 2, "south"), (7, Z1 - 2, "south"),
             (X0 + 1, 3, "west"), (X1 - 1, 3, "east"), (X0 + 1, 6, "west")]
    for (x, z, f) in spots[:beds]:
        color = cols[len([1 for b in B.blocks.values() if b[0].endswith("_bed")]) // 2 % 2]
        if f == "south":
            B.bed(x, 6, z - 1, "south", color)
        else:
            B.bed(x + (1 if f == "west" else -1), 6, z, f, color)
    B.put(C, 8, C, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    B.put(C + 2, 6, Z0 + 2, "minecraft:barrel", {"facing": "up", "open": "false"})
    B.potted(C - 2, 6, Z0 + 1, "bamboo")
    B.rug(X0 + 2, X1 - 2, Z0 + 2, Z1 - 3, 6, cols[0], cols[-1])    # the bedroom's rug, in the quarter's colours
    # Below: the lamps, the fittings, the workers.
    for (x, z) in ((4, 4), (8, 4), (4, 8), (8, 8)):
        B.put(x, 4, z, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    furnish(B)
    for (v, x, z) in workers:
        if v == "pup":
            B.dog(x, 1, z, B.rng.choice(["weaver", "smith", "cook", "tailor"]), Age=-24000)   # a pupil
        else:
            B.dog(x, 1, z, v)
    B.finish()
    return B


# ---------------------------------------------------------------- the clothing quarter
def f_weavery(B):
    for (x, z) in ((3, 4), (3, 7), (9, 4), (9, 7)):
        B.put(x, 1, z, "minecraft:loom", {"facing": "east" if x < C else "west"})
    for (x, z, w) in ((2, 9, "white"), (3, 9, "light_gray"), (4, 9, "white"), (8, 9, "white"), (2, 2, "white")):
        B.put(x, 1, z, f"minecraft:{w}_wool")
        B.put(x, 2, z, f"minecraft:{w}_wool")
    B.stocked_barrel(10, 1, 2, [("minecraft:string", 32), ("minecraft:white_wool", 16)])
    B.put(6, 1, 9, "minecraft:crafting_table")


def f_dyeworks(B):
    for (x, z) in ((3, 3), (3, 6), (9, 3), (9, 6)):
        B.put(x, 1, z, "minecraft:water_cauldron", {"level": "3"})
    for (x, z, c) in ((2, 9, "red"), (3, 9, "blue"), (4, 9, "yellow"), (8, 9, "green"), (2, 7, "purple")):
        B.put(x, 1, z, f"minecraft:{c}_wool")
    for (x, z, plant) in ((10, 8, "red_tulip"), (10, 7, "cornflower"), (10, 6, "dandelion")):
        B.potted(x, 1, z, plant)
    B.stocked_barrel(10, 1, 2, [("minecraft:red_dye", 16), ("minecraft:blue_dye", 16), ("minecraft:yellow_dye", 16)])
    B.put(6, 1, 9, "minecraft:composter", {"level": "0"})


def f_tailor(B):
    B.put(3, 1, 3, "minecraft:loom", {"facing": "east"})
    B.put(3, 1, 4, "minecraft:crafting_table")
    for (x, z) in ((9, 3), (9, 5)):
        B.table(x, 1, z)
    B.armor_stand(3, 1, 8, 180.0, kit="leather", hand=None)
    B.armor_stand(9, 1, 8, 180.0, kit="leather", hand=None)
    for x in range(4, 9):
        B.put(x, 1, 9, f"minecraft:{B.theme['colors'][1]}_carpet")
    B.stocked_barrel(10, 1, 2, [("minecraft:leather", 16), ("minecraft:white_wool", 16)])


def f_woolbarn(B):
    # A sheep fold inside: a fenced pen, hay, the shepherd's shears on the wall.
    for x in range(2, 11):
        for z in range(5, 11):
            B.put(x, 0, z, "minecraft:coarse_dirt")
            if x in (2, 10) or z == 5:
                if not (z == 5 and x == 6):
                    B.fence(x, 1, z, "minecraft:spruce_fence")
    B.put(6, 1, 5, "minecraft:spruce_fence_gate", {"facing": "south", "in_wall": "false", "open": "false",
                                                    "powered": "false"})
    for (x, z) in ((3, 9), (4, 9), (9, 9)):
        B.put(x, 1, z, "minecraft:hay_block", {"axis": "y"})
    for (x, z, c) in ((4, 7, "white"), (7, 7, "white"), (8, 8, "light_gray")):
        B.animal(x, 1, z, "sheep", Color=Byte_(0 if c == "white" else 8))
    B.put(9, 1, 2, "minecraft:hay_block", {"axis": "x"})
    B.stocked_barrel(3, 1, 2, [("minecraft:white_wool", 24), ("minecraft:shears", 1)])


# ---------------------------------------------------------------- smiths, leather, livery, lumber
def f_smithy(B):
    B.put(3, 1, 3, "minecraft:blast_furnace", {"facing": "east", "lit": "false"})
    B.put(3, 1, 4, "minecraft:blast_furnace", {"facing": "east", "lit": "false"})
    B.put(3, 1, 6, "minecraft:lava_cauldron")
    B.put(6, 1, 7, "minecraft:anvil", {"facing": "north"})
    B.put(9, 1, 3, "minecraft:smithing_table")
    B.put(9, 1, 5, "minecraft:grindstone", {"face": "floor", "facing": "west"})
    B.put(9, 1, 8, "minecraft:iron_block")
    for x in (2, 3, 4):
        B.put(x, 3, 9, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    B.stocked_barrel(10, 1, 2, [("minecraft:iron_ingot", 16), ("minecraft:coal", 32)])


def f_tannery(B):
    for (x, z) in ((3, 3), (3, 5), (3, 7)):
        B.put(x, 1, z, "minecraft:water_cauldron", {"level": "3"})
    B.put(9, 1, 3, "minecraft:smoker", {"facing": "west", "lit": "false"})
    for (x, z) in ((9, 6), (9, 7)):
        B.put(x, 1, z, "minecraft:brown_wool")
    B.put(6, 1, 9, "minecraft:composter", {"level": "0"})
    B.stocked_barrel(10, 1, 2, [("minecraft:leather", 16), ("minecraft:rabbit_hide", 8)])


def f_livery(B):
    # Two stalls with horses, hay in the mangers, saddles on the wall.
    for z in range(5, 11):
        B.fence(5, 1, z, "minecraft:spruce_fence")
        B.fence(7, 1, z, "minecraft:spruce_fence")
    for x in (2, 3, 4, 8, 9, 10):
        B.fence(x, 1, 5, "minecraft:spruce_fence")
    for (x, z) in ((2, 10), (10, 10)):
        B.put(x, 1, z, "minecraft:hay_block", {"axis": "y"})
    B.put(6, 0, 7, "minecraft:coarse_dirt")
    for x in range(2, 5):
        for z in range(6, 11):
            B.put(x, 0, z, "minecraft:coarse_dirt")
    for x in range(8, 11):
        for z in range(6, 11):
            B.put(x, 0, z, "minecraft:coarse_dirt")
    B.animal(3, 1, 8, "horse", Tame=Byte_(1), Variant=0)
    B.animal(9, 1, 8, "horse", Tame=Byte_(1), Variant=2)
    B.stocked_barrel(2, 1, 2, [("minecraft:saddle", 2), ("minecraft:wheat", 32)])


def f_sawmill(B):
    B.put(6, 1, 6, "minecraft:stonecutter", {"facing": "north"})
    for (x, z) in ((2, 8), (3, 8), (2, 9), (3, 9), (9, 8), (9, 9), (10, 8), (10, 9)):
        B.log(x, 1, z, axis="x", kind="spruce_log")
    B.log(2, 2, 9, axis="x", kind="spruce_log")
    B.log(10, 2, 9, axis="x", kind="spruce_log")
    for (x, z) in ((3, 3), (9, 3)):
        B.put(x, 1, z, "minecraft:crafting_table")
    B.stocked_barrel(10, 1, 2, [("minecraft:spruce_planks", 32), ("minecraft:iron_axe", 1)])


# ---------------------------------------------------------------- meat
def f_slaughterhouse(B):
    for x in range(2, 11):
        for z in range(6, 11):
            B.put(x, 0, z, "minecraft:coarse_dirt")
            if x in (2, 10) or z == 6:
                if not (z == 6 and x == 6):
                    B.fence(x, 1, z, "minecraft:spruce_fence")
    B.put(6, 1, 6, "minecraft:spruce_fence_gate", {"facing": "south", "in_wall": "false", "open": "false",
                                                    "powered": "false"})
    B.animal(4, 1, 8, "pig")
    B.animal(8, 1, 8, "cow")
    B.put(3, 1, 3, "minecraft:water_cauldron", {"level": "3"})
    for x in (8, 9):
        B.put(x, 3, 3, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    B.stocked_barrel(10, 1, 2, [("minecraft:salt", 1)] if False else [("minecraft:bone", 8)])


def f_butchery(B):
    for x in range(3, 10):
        B.put(x, 1, 5, "minecraft:spruce_planks")                     # the counter
    B.mod(4, 2, 5, "farmersdelight:cutting_board", {"facing": "south"}, "minecraft:spruce_pressure_plate",
          {"powered": "false"})
    B.mod(8, 2, 5, "farmersdelight:cutting_board", {"facing": "south"}, "minecraft:spruce_pressure_plate",
          {"powered": "false"})
    for x in (3, 5, 7, 9):
        B.put(x, 3, 8, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    B.put(2, 1, 9, "minecraft:smoker", {"facing": "east", "lit": "false"})
    B.stocked_barrel(10, 1, 9, [("minecraft:beef", 16), ("minecraft:porkchop", 16), ("minecraft:mutton", 8)])


def f_smokehouse(B):
    for (x, z) in ((3, 3), (3, 5), (3, 7), (9, 3), (9, 5), (9, 7)):
        B.put(x, 1, z, "minecraft:smoker", {"facing": "east" if x < C else "west", "lit": "true"})
    B.put(6, 1, 9, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false",
                                          "waterlogged": "false"})
    for x in (5, 7):
        B.put(x, 3, 9, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    B.stocked_barrel(10, 1, 9, [("minecraft:cooked_beef", 8), ("minecraft:cooked_porkchop", 8)])


def f_kitchen(B):
    for x in (3, 4, 5):
        B.mod(x, 1, 9, "farmersdelight:stove", {"facing": "north", "lit": "true"}, "minecraft:furnace",
              {"facing": "north", "lit": "true"})
    B.mod(4, 2, 9, "farmersdelight:cooking_pot", {"facing": "north", "support": "tray"}, "minecraft:cauldron", {})
    for x in (7, 8, 9):
        B.put(x, 1, 9, "minecraft:spruce_planks")
    B.mod(8, 2, 9, "farmersdelight:cutting_board", {"facing": "north"}, "minecraft:spruce_pressure_plate",
          {"powered": "false"})
    B.put(3, 1, 3, "minecraft:smoker", {"facing": "east", "lit": "false"})
    for z in (5, 6):
        B.table(9, 1, z)
    B.stocked_barrel(10, 1, 2, [("minecraft:cooked_beef", 16), ("minecraft:cooked_porkchop", 16)])


# ---------------------------------------------------------------- meals, music, school
def f_mess_hall(B):
    for x in (4, 8):
        for z in range(4, 10):
            B.low_table(x, 1, z) if False else B.put(x, 1, z, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
            B.bench(x - 1, 1, z, "east")
            B.bench(x + 1, 1, z, "west")
    B.put(2, 1, 9, "minecraft:smoker", {"facing": "east", "lit": "false"})
    B.stocked_barrel(10, 1, 2, [("minecraft:cooked_mutton", 32), ("minecraft:cooked_beef", 16)])


def f_music_hall(B):
    for x in range(3, 10):
        for z in (8, 9, 10):
            B.put(x, 1, z, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})   # the stage
    B.put(4, 1, 10, "minecraft:note_block", {"instrument": "harp", "note": "0", "powered": "false"})
    B.put(6, 1, 10, "minecraft:jukebox", {"has_record": "false"})
    B.put(8, 1, 10, "minecraft:note_block", {"instrument": "flute", "note": "0", "powered": "false"})
    B.put(6, 4, 9, "minecraft:bell", {"attachment": "ceiling", "facing": "north", "powered": "false"})
    for z in (4, 6):
        for x in range(3, 10):
            if x != C:
                B.bench(x, 1, z, "north")


def f_school(B):
    for x in range(4, 9):
        B.put(x, 2, 10, "minecraft:black_concrete")                      # the blackboard
        B.put(x, 3, 10, "minecraft:black_concrete")
    B.lectern(6, 1, 9, "north", ["Rules of the House: rise at the bell. Sweep your step. Speak plainly.",
                                 "The Four Duties: the bow, the blade, the pike, the kennel.",
                                 "Never set foot in the dark tower. Never ask why."])
    for z in (4, 6):
        for x in (3, 5, 7, 9):
            B.low_table(x, 1, z)
    for (x, z) in ((2, 2), (2, 3), (10, 3)):
        B.put(x, 1, z, "minecraft:bookshelf")


def f_teahouse(B):
    for (x, z) in ((4, 4), (8, 4), (4, 8), (8, 8)):
        B.low_table(x, 1, z)
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            B.put(x + dx, 1, z + dz, f"minecraft:{B.theme['colors'][1]}_carpet")
    # The kettle over the hearth: the fire sunk in the floor, the pot standing at floor level.
    B.put(C, 0, 6, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false",
                                          "waterlogged": "false"})
    B.put(C, 1, 6, "minecraft:cauldron")
    for (x, z) in ((2, 2), (10, 2), (2, 10)):
        B.potted(x, 1, z, "azalea_bush")


def Byte_(v):
    from nbt_structure import Byte
    return Byte(v)


WORKSHOPS = {
    # name: (seed, sign, workers, beds, furnish, floor)
    "dc_weavery": (101, ["WEAVERY"], [("weaver", 5, 6), ("weaver", 7, 6)], 2, f_weavery, FLOOR),
    "dc_dyeworks": (102, ["DYEWORKS"], [("dyer", 6, 4), ("dyer", 6, 7)], 2, f_dyeworks, "minecraft:polished_andesite"),
    "dc_tailor": (103, ["TAILOR"], [("tailor", 6, 6), ("tailor", 5, 3)], 2, f_tailor, FLOOR),
    "dc_woolbarn": (104, ["WOOL", "BARN"], [("shepherd", 6, 3)], 1, f_woolbarn, FLOOR),
    "dc_smithy": (201, ["SMITHY"], [("smith", 6, 5), ("smith", 5, 3)], 2, f_smithy, "minecraft:polished_andesite"),
    "dc_tannery": (202, ["TANNERY"], [("tanner", 6, 5), ("tanner", 6, 7)], 2, f_tannery, "minecraft:polished_andesite"),
    "dc_livery": (203, ["LIVERY"], [("stablemaster", 6, 3), ("stablemaster", 6, 6)], 2, f_livery, FLOOR),
    "dc_sawmill": (204, ["SAWMILL"], [("woodcutter", 5, 4), ("woodcutter", 7, 7)], 2, f_sawmill, FLOOR),
    "dc_slaughterhouse": (301, ["SHAMBLES"], [("butcher", 6, 3)], 1, f_slaughterhouse, "minecraft:polished_andesite"),
    "dc_butchery": (302, ["BUTCHER"], [("butcher", 6, 3), ("butcher", 6, 7)], 2, f_butchery, "minecraft:polished_andesite"),
    "dc_smokehouse": (303, ["SMOKEHOUSE"], [("cook", 6, 4), ("cook", 6, 7)], 2, f_smokehouse, "minecraft:polished_andesite"),
    "dc_kitchen": (304, ["KITCHEN"], [("cook", 5, 6), ("cook", 7, 6), ("cook", 6, 3)], 3, f_kitchen, FLOOR),
    "dc_mess_hall": (401, ["MESS HALL"], [("server", 6, 3), ("server", 6, 9)], 2, f_mess_hall, FLOOR),
    "dc_music_hall": (402, ["MUSIC HALL"], [("musician", 4, 9), ("musician", 8, 9)], 2, f_music_hall, FLOOR),
    "dc_school": (403, ["SCHOOL"], [("teacher", 6, 8), ("pup", 5, 5), ("pup", 7, 5)], 3, f_school, FLOOR),
    "dc_teahouse": (404, ["TEA HOUSE"], [("server", 6, 3), ("server", 3, 6)], 2, f_teahouse, FLOOR),
}


# ---------------------------------------------------------------- the military towers
def tower(seed, kind):
    """
    Thirteen square: two storeys of stone, a third of white plaster and dark timber, a skirt roof round the plaster
    storey and a hip roof over it (a yagura on a fort wall). A ladder up the back wall through every floor.
    """
    TS, a0, a1 = 15, 1, 13
    c = 7
    B = DogBuilder(TS, seed)
    B.floor(a0, a1, a0, a1, 0, PLINTH)
    B.floor(a0 + 1, a1 - 1, a0 + 1, a1 - 1, 0, FLOOR)
    B.stone_storey(a0, a1, a0, a1, 1, 9)
    for y in (5, 10):
        B.floor(a0 + 1, a1 - 1, a0 + 1, a1 - 1, y, FLOOR)
    B.timber_storey(a0, a1, a0, a1, 10, 14)
    B.floor(a0 + 1, a1 - 1, a0 + 1, a1 - 1, 10, FLOOR)
    # Arrow loops in the stone, two a side on each floor.
    for y in (2, 3, 7, 8):
        for v in (4, 10):
            for (x, z) in ((v, a0), (v, a1), (a0, v), (a1, v)):
                if not (z == a0 and v == c):
                    B.air(x, y, z) if y in (3, 8) else None
    # The skirt roof round the top storey, and the roof.
    for x in range(a0 - 1, a1 + 2):
        for z in range(a0 - 1, a1 + 2):
            if x in (a0 - 1, a1 + 1) or z in (a0 - 1, a1 + 1):
                f = ("south" if z == a0 - 1 else "north") if z in (a0 - 1, a1 + 1) else ("east" if x == a0 - 1 else "west")
                B.stair(x, 10, z, TILE[0], f)
    B.hip_roof(a0, a1, a0, a1, 15)
    for y in (1, 2, 3):
        B.air(c, y, a0)                                   # an archway three high: the tallest soldiers walk in
    B.put(c, 4, a0, "minecraft:chiseled_stone_bricks")
    B.put(c, 0, a0 - 1, PLINTH)                       # a threshold, flush with the road
    B.bracket_lantern(c - 2, 3, a0 - 1, "north")
    B.bracket_lantern(c + 2, 3, a0 - 1, "north")
    B.ladder(a1 - 2, a1 - 1, 1, 14, "north")
    for y in (4, 9, 14):
        B.put(c, y, c, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    banner = {"bow": "green", "sword": "red", "pike": "blue", "kennel": "black"}[kind]
    for x in (c - 2, c + 2):
        B.put(x, 8, a0 - 1, f"minecraft:{banner}_wall_banner", {"facing": "north"})
    if kind == "kennel":
        kennel(B, a0, a1, c)
        B.rug(4, 10, 3, 10, 6, "gray", "black")          # the keepers' room
        B.finish()
        return B
    # Second and third floors: the soldiers' beds (one each), barrels, a stand of arms.
    soldier = {"bow": "archer", "sword": "samurai", "pike": "pikeman"}[kind]
    for (x, z) in ((3, 3), (3, 6), (3, 9)):
        B.bed(x, 6, z, "east")
    B.bed(10, 11, 3, "west")
    B.put(c, 6, a1 - 2, "minecraft:barrel", {"facing": "up", "open": "false"})
    if kind == "bow":
        B.put(10, 1, 3, "minecraft:fletching_table")
        for (x, z) in ((3, 4), (3, 8)):
            B.stocked_barrel(x, 1, z, [("minecraft:arrow", 64), ("minecraft:bow", 1)])
        for z in (4, 6, 8):
            B.item_frame(a0 + 1, 2, z, "east", "minecraft:bow")
        for (x, z) in ((10, 6), (10, 8)):
            B.put(x, 1, z, "minecraft:target", {"power": "0"})
    elif kind == "sword":
        for (x, z) in ((3, 4), (3, 7), (3, 10)):
            B.armor_stand(x, 1, z, 270.0, kit="iron", hand="minecraft:iron_sword")
        B.put(10, 1, 3, "minecraft:grindstone", {"face": "floor", "facing": "west"})
        B.armor_stand(10, 1, 7, 90.0, kit="leather", hand=None)            # the practice dummy
    else:
        for z in (3, 5, 7, 9):
            B.item_frame(a0 + 1, 2, z, "east", "minecraft:trident")
            B.item_frame(a1 - 1, 2, z, "west", "minecraft:trident")
        B.stocked_barrel(10, 1, 4, [("minecraft:stick", 32), ("minecraft:iron_ingot", 8)])
    for (x, z) in ((5, 5), (9, 5), (5, 9), (9, 9)):
        B.dog(x, 1, z, soldier)
    for y in (6, 11):
        B.rug(4, 10, 3, 10, y, "gray", banner)          # the barrack rooms: gray, in the tower's colour
    B.finish()
    return B


def kennel(B, a0, a1, c):
    """The Biters' kennel: four barred cells round a middle aisle, straw and bones, no beds. Their keepers sleep above."""
    for (cx0, cz0) in ((2, 2), (9, 2), (2, 8), (9, 8)):
        if cz0 == 2 and cx0 == 2:
            pass
        for x in range(cx0, cx0 + 3):
            for z in range(cz0, cz0 + 3):
                B.put(x, 0, z, "minecraft:coarse_dirt")
                B.put(x, 1, z, "minecraft:air")
        # Bars on the cell's open sides (toward the aisle).
        for x in range(cx0 - 1, cx0 + 4):
            for z in range(cz0 - 1, cz0 + 4):
                if a0 < x < a1 and a0 < z < a1 and (x in (cx0 - 1, cx0 + 3) or z in (cz0 - 1, cz0 + 3)):
                    for y in (1, 2, 3, 4):
                        B.put(x, y, z, "minecraft:iron_bars", {})
        B.put(cx0 + 1, 1, cz0 + 1, "minecraft:hay_block", {"axis": "y"}) if False else None
        B.put(cx0, 1, cz0 + 2 if cz0 == 2 else cz0, "minecraft:bone_block", {"axis": "y"})
        B.dog(cx0 + 1, 1, cz0 + 1, "biter", stay=1)
    # The aisle: a water trough, chains, a lantern.
    B.put(c, 1, c + 3, "minecraft:water_cauldron", {"level": "3"})
    for x in (c - 1, c + 1):
        B.put(x, 4, c, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    # The keepers: two, with beds upstairs.
    B.bed(3, 6, 3, "east")
    B.bed(3, 6, 6, "east")
    B.dog(5, 6, 8, "husky_monk")
    B.dog(8, 6, 8, "samurai")
    for (x, z) in ((9, 4), (10, 4)):
        B.put(x, 6, z, "minecraft:barrel", {"facing": "up", "open": "false"})


# ---------------------------------------------------------------- the gatehouse
def gatehouse():
    """
    Eleven square: a stone gate passage through the middle (front -z faces into the city), guard rooms either side, an
    upper floor level with the wall walk (the wall cuts its own doorways in), a plaster storey and a tile roof.
    """
    B = DogBuilder(13, 501)
    B.floor(1, 11, 1, 11, 0, PLINTH)
    B.stone_storey(1, 11, 1, 11, 1, 6, windows=False)
    B.floor(2, 10, 2, 10, 6, FLOOR)
    B.timber_storey(1, 11, 1, 11, 7, 10)
    B.hip_roof(1, 11, 1, 11, 11)
    # The passage, three wide and four high, end to end, its floor of stone.
    for z in range(1, 12):
        for x in (5, 6, 7):
            B.put(x, 0, z, STONE)
            for y in range(1, 5):
                B.air(x, y, z)
        for x in (4, 8):
            for y in range(1, 6):
                B.put(x, y, z, STONE)
    for x in (5, 6, 7):
        B.put(x, 5, 1, "minecraft:chiseled_stone_bricks")
        B.put(x, 5, 11, "minecraft:chiseled_stone_bricks")
        B.put(x, 4, 11, "minecraft:iron_bars", {})                         # the portcullis, raised
    # The guard room: a door off the passage, a ladder up.
    for y in (1, 2, 3):
        B.air(4, y, 6)                                    # archways three high off the passage
    B.ladder(2, 2, 1, 6, "south")
    B.put(2, 1, 9, "minecraft:barrel", {"facing": "up", "open": "false"})
    B.put(3, 4, 6, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    for y in (1, 2, 3):
        B.air(8, y, 6)
    B.armor_stand(10, 1, 6, 90.0, kit="iron", hand="minecraft:iron_sword")
    B.put(9, 4, 6, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    for z in (1, 11):
        for x in (3, 9):
            B.bracket_lantern(x, 3, z - 1 if z == 1 else z + 1, "north" if z == 1 else "south")
    # Upstairs: the guards' beds, and doors out onto the wall walk on both sides.
    B.bed(5, 7, 9, "east")
    B.bed(5, 7, 3, "east")
    B.put(6, 10, 6, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    B.dog(6, 1, 3, "samurai", stay=3)
    B.dog(6, 1, 9, "samurai", stay=3)
    B.rug(3, 9, 3, 9, 7, "gray", "red")                  # the guards' room
    B.finish()
    return B


# ---------------------------------------------------------------- the Keep
K = 49           # the template (the plinth is 1..47, half 23 about the middle)
KC = 24
P = 4            # the plinth's top (the courtyard's and the halls' floor)
HO0, HO1 = 5, 43          # the halls' outer walls
HI0, HI1 = 12, 36         # their inner walls (the courtyard is between)
TOWER = (19, 29)          # the dark tower's template (11 square) sits here: leave it be


def keep():
    B = DogBuilder(K, 601)
    # The plinth: raised land faced in stone, its top a terrace round the halls.
    for x in range(1, 48):
        for z in range(1, 48):
            B.put(x, 0, z, PLINTH)
            for y in range(1, P):
                B.put(x, y, z, STONE)
            edge = x in (1, 47) or z in (1, 47)
            B.put(x, P, z, PLINTH if edge else "minecraft:stone_bricks")
            if edge:
                B.put(x, P + 1, z, "minecraft:stone_brick_wall", {"up": "true", "north": "none", "south": "none",
                                                                  "east": "none", "west": "none", "waterlogged": "false"})
    # The one wide stair, nine across, up the front (-z) to the gate passage; balustrades either side.
    for i, z in enumerate((1, 2, 3, 4)):
        for x in range(20, 29):
            for y in range(i + 1, P + 2):
                B.air(x, y, z)
            B.stair(x, i + 1, z, "minecraft:stone_brick_stairs", "south")
            for y in range(1, i + 1):
                B.put(x, y, z, STONE)
        for x in (19, 29):
            for y in range(1, i + 3):
                B.put(x, y, z, STONE)
            B.put(x, i + 2, z, "minecraft:stone_brick_wall", {"up": "true", "north": "none", "south": "none",
                                                               "east": "none", "west": "none", "waterlogged": "false"})
    # The halls: stone, six high, round the courtyard.
    top = P + 6
    for y in range(P + 1, top + 1):
        for x in range(HO0, HO1 + 1):
            for z in range(HO0, HO1 + 1):
                outer = x in (HO0, HO1) or z in (HO0, HO1)
                inner = (x in (HI0, HI1) and HI0 <= z <= HI1) or (z in (HI0, HI1) and HI0 <= x <= HI1)
                inside_ring = (HO0 < x < HO1 and HO0 < z < HO1) and not (HI0 <= x <= HI1 and HI0 <= z <= HI1)
                if outer or inner:
                    B.put(x, y, z, PLINTH if y == P + 1 else STONE)
                elif inside_ring:
                    B.air(x, y, z)
    for x in range(HO0, HO1 + 1):
        for z in range(HO0, HO1 + 1):
            if not (HI0 < x < HI1 and HI0 < z < HI1):
                B.put(x, top + 1, z, "minecraft:spruce_planks")          # the ceiling
                if HO0 < x < HO1 and HO0 < z < HO1 and not (HI0 <= x <= HI1 and HI0 <= z <= HI1):
                    B.put(x, P, z, "minecraft:dark_oak_planks")       # the halls' wooden floors
    # Tall windows in the outer walls, paper screens to the courtyard.
    for v in range(HO0 + 3, HO1 - 2, 4):
        for (x, z) in ((v, HO0), (v, HO1), (HO0, v), (HO1, v)):
            if not (z == HO0 and 20 <= x <= 28):
                for y in (P + 2, P + 3):
                    B.put(x, y, z, "minecraft:glass_pane", {})
    for v in range(HI0 + 3, HI1 - 2, 4):
        for (x, z) in ((v, HI0), (v, HI1), (HI0, v), (HI1, v)):
            for y in (P + 2, P + 3):
                B.put(x, y, z, SCREEN, {})
    # The gate passage through the front hall, five wide, into the courtyard; arches into it from the other halls.
    for z in range(HO0, HI0 + 1):
        for x in range(22, 27):
            for y in range(P + 1, P + 5):
                B.air(x, y, z)
            B.put(x, P, z, PLINTH)
    for x in range(22, 27):
        B.put(x, P + 5, HO0, "minecraft:chiseled_stone_bricks")
    # The ring roof over the halls, and a raised roof over the gate.
    B.ring_roof(HO0, HO1, HO0, HO1, HI0, HI1, HI0, HI1, top + 2)
    for (x, z) in ((20, HO0 - 1), (28, HO0 - 1)):
        B.stone_lantern(x, P + 1, z)
    for (x, z) in ((3, 3), (45, 3), (3, 45), (45, 45)):
        B.stone_lantern(x, P + 1, z)

    # ---- The halls inside, walled into rooms (a doorway three high in every dividing wall, on the hall's middle line,
    # so one can walk right round the ring). Front: the gate passage, the guards' barracks, an audience room. West
    # (study): the library, the scriptorium. East: the council chamber, the monks' dormitory. Back: the prayer hall.
    # Corners: an armoury, a store, a meditation room, the abbot's cell.
    hall_y = P + 1
    lo, hi = HO0 + 1, HI0 - 1                         # a hall's inside, across (6..11); its middle line is 8..9
    def wall_x(x, z0, z1):                            # a dividing wall across a front/back hall at x
        for z in range(z0, z1 + 1):
            for y in range(hall_y, top + 1):
                B.put(x, y, z, PLINTH if y == hall_y else STONE)
        for z in (z0 + 2, z0 + 3):
            for y in range(hall_y, hall_y + 3):
                B.air(x, y, z)
    def wall_z(z, x0, x1):                            # across a side hall at z
        for x in range(x0, x1 + 1):
            for y in range(hall_y, top + 1):
                B.put(x, y, z, PLINTH if y == hall_y else STONE)
        for x in (x0 + 2, x0 + 3):
            for y in range(hall_y, hall_y + 3):
                B.air(x, y, z)
    for x in (HI0, HI1, 21, 27):
        wall_x(x, lo, hi)                             # front hall: corners off, the passage walled
    for x in (HI0, HI1):
        wall_x(x, HI1 + 1, HO1 - 1)                   # back hall: corners off
    for (x0, x1) in ((lo, hi), (HI1 + 1, HO1 - 1)):
        for z in (HI0, KC, HI1):
            wall_z(z, x0, x1)                         # side halls: corners off, and halved
    # The courtyard opens into the library, the council chamber and the prayer hall.
    for (x, z) in ((KC, HI1), (HI0, 18), (HI1, 18)):
        for d in (-1, 0, 1):
            xx, zz = (x + d, z) if z == HI1 else (x, z + d)
            for y in range(hall_y, hall_y + 3):
                B.air(xx, y, zz)
    rooms = []
    def lamp(x, z):
        B.put(x, top, z, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})

    # The prayer hall (the back): mats in rows before the altar, candles, the bell.
    for x in range(HI0 + 2, HI1 - 1):
        for z in (HI1 + 2, HI1 + 4):
            if (x - HI0) % 3 != 0:
                B.put(x, hall_y, z, "minecraft:light_gray_carpet")
    for x in range(KC - 3, KC + 4):
        B.put(x, hall_y, HO1 - 1, "minecraft:polished_andesite_slab", {"type": "bottom", "waterlogged": "false"})
    B.put(KC, hall_y, HO1 - 1, "minecraft:polished_andesite")
    B.put(KC, hall_y + 1, HO1 - 1, "minecraft:candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    for x in (KC - 2, KC + 2):
        B.put(x, hall_y, HO1 - 1, "minecraft:polished_andesite")
        B.put(x, hall_y + 1, HO1 - 1, "minecraft:candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
    B.put(KC, top, HO1 - 2, "minecraft:bell", {"attachment": "ceiling", "facing": "north", "powered": "false"})
    for x in (17, 31):
        lamp(x, HI1 + 3)

    # The library (west, front half): shelves on the outer wall, the five fables on lecterns, reading cushions.
    for z in range(HI0 + 1, KC):
        if z % 4 != 0:
            B.put(lo, hall_y, z, "minecraft:bookshelf")
            B.put(lo, hall_y + 1, z, "minecraft:bookshelf")
    for z, pages in zip((14, 16, 18, 20, 22), FABLES):
        B.lectern(hi - 1, hall_y, z, "west", pages)
        B.put(hi - 2, hall_y, z, "minecraft:light_gray_carpet")
    lamp(8, 18)
    # The scriptorium (west, back half): writing desks with cushions, the old books of rules, shelves.
    for z in range(KC + 1, HI1):
        if z % 4 != 0:
            B.put(lo, hall_y, z, "minecraft:bookshelf")
    for z in (27, 31):
        B.low_table(hi - 1, hall_y, z)
        B.put(hi - 2, hall_y, z, "minecraft:red_carpet")
        B.put(hi - 1, hall_y + 1, z, "minecraft:candle", {"candles": "1", "lit": "true", "waterlogged": "false"})
    B.lectern(hi - 1, hall_y, 34, "west", ["On Duty. The bow, the blade, the pike, the kennel: four duties, one dog.",
                                          "On Silence. The tower is not spoken of. It is kept.",
                                          "On the Bandages. He kneels so that we may stand."])
    lamp(8, 30)

    # The council chamber (east, front half): a long low table, cushions down both sides.
    for z in range(15, 22):
        B.low_table(HO1 - 3, hall_y, z)
        B.put(HO1 - 4, hall_y, z, "minecraft:red_carpet")
        B.put(HO1 - 2, hall_y, z, "minecraft:red_carpet")
    B.put(HO1 - 3, hall_y, 14, "minecraft:red_carpet")
    lamp(HO1 - 4, 18)
    # The monks' dormitory (east, back half): five beds against the outer wall, a rug, a lantern.
    for z in (26, 28, 30, 32, 34):
        B.bed(HO1 - 2, hall_y, z, "east", "light_gray")
    B.rug(HI1 + 1, HO1 - 4, 26, 34, hall_y, "gray", "light_gray")
    lamp(HO1 - 5, 30)

    # The abbot's cell (back east corner): a bed, a rug, a lectern; the meditation room (back west): one mat, candles.
    B.bed(HO1 - 2, hall_y, HO1 - 2, "east", "white")
    B.rug(HI1 + 1, HO1 - 3, HI1 + 1, HO1 - 1, hall_y, "light_gray", "white")
    lamp(HI1 + 3, HI1 + 3)
    B.put(lo + 2, hall_y, HO1 - 3, "minecraft:brown_carpet")
    for (x, z) in ((lo, HO1 - 1), (hi, HO1 - 1), (lo, HI1 + 1)):
        B.put(x, hall_y, z, "minecraft:candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    B.potted(hi, hall_y, HI1 + 1, "bamboo")

    # The guards' barracks (front, west of the passage): four beds along the outer wall.
    for x in (14, 16, 18, 20):
        B.bed(x, hall_y, lo + 1, "north", "gray")
    B.rug(13, 20, lo + 3, hi, hall_y, "gray", "red")
    lamp(17, 8)
    # The audience room (front, east of the passage): low tables and cushions where guests are received.
    for x in (30, 33):
        B.low_table(x, hall_y, lo + 1)
        B.low_table(x, hall_y, hi - 1)
        for z in (lo + 2, hi - 2):
            B.put(x, hall_y, z, "minecraft:red_carpet")
    B.potted(35, hall_y, lo, "azalea_bush")
    lamp(31, 8)
    # The armoury (front west corner) and the store (front east corner).
    for x in (lo + 1, lo + 3):
        B.armor_stand(x, hall_y, lo, 0.0, kit="iron", hand="minecraft:iron_sword")
    B.stocked_barrel(hi, hall_y, lo, [("minecraft:arrow", 64), ("minecraft:shield", 2)])
    for (x, z) in ((HO1 - 1, lo), (HO1 - 1, lo + 1), (HO1 - 2, lo)):
        B.stocked_barrel(x, hall_y, z, [("minecraft:cooked_beef", 32), ("minecraft:bone", 16)])
    lamp(8, 8)
    lamp(HO1 - 4, 8)

    # ---- The courtyard: grass, gravel walks, flowering shrubs, stone lanterns and stools, a pond in each corner.
    for x in range(HI0 + 1, HI1):
        for z in range(HI0 + 1, HI1):
            if TOWER[0] <= x <= TOWER[1] and TOWER[0] <= z <= TOWER[1]:
                continue
            walk = x in (KC - 1, KC, KC + 1) or z in (KC - 1, KC, KC + 1) or x in (HI0 + 2, HI1 - 2) or z in (HI0 + 2, HI1 - 2)
            B.put(x, P, z, "minecraft:gravel" if walk else "minecraft:grass_block", {"snowy": "false"} if not walk else None)
    for (qx, qz) in ((16, 16), (32, 16), (16, 32), (32, 32)):
        # A little pond with lily pads, shrubs round it, two stools and a stone lantern.
        for x in (qx - 1, qx):
            for z in (qz - 1, qz):
                B.put(x, P, z, "minecraft:water", {"level": "0"})
                B.put(x, P - 1, z, STONE)
        B.put(qx - 1, P + 1, qz - 1, "minecraft:lily_pad")
        for (x, z) in ((qx + 1, qz - 1), (qx - 2, qz)):
            B.put(x, P + 1, z, "minecraft:flowering_azalea_leaves", {"persistent": "true", "distance": "1",
                                                                     "waterlogged": "false"})
        B.put(qx + 1, P + 1, qz + 1, "minecraft:azalea_leaves", {"persistent": "true", "distance": "1",
                                                                 "waterlogged": "false"})
        B.stool(qx - 2, P + 1, qz - 2)
        B.stool(qx + 1, P + 1, qz - 2)
        B.stone_lantern(qx - 2, P + 1, qz + 1)
    for (x, z) in ((HI0 + 1, HI0 + 1), (HI1 - 1, HI0 + 1), (HI0 + 1, HI1 - 1), (HI1 - 1, HI1 - 1)):
        B.potted(x, P + 1, z, "bamboo")

    # ---- The monks, and the keep's guards (who'll keep everyone out of the tower).
    for (v, x, z) in (("monk", 20, HI1 + 3), ("husky_monk", 28, HI1 + 3), ("monk", 8, 19),
                      ("husky_monk", 8, 29), ("monk", HO1 - 5, 17), ("husky_monk", HO1 - 5, 29)):
        B.dog(x, hall_y, z, v)
    for (x, z) in ((KC - 2, HO0 - 1), (KC + 2, HO0 - 1)):
        B.dog(x, P + 1, z, "samurai", stay=3)                          # at the top of the stair
    # The stair up to the dark tower's door: three wide, four steps, parapets either side (its top step is the
    # tower template's front row, which the tower leaves empty below its portal).
    for i in range(4):
        z = TOWER[0] - 3 + i
        for x in range(KC - 1, KC + 2):
            for y in range(P + 1, P + 1 + i):
                B.put(x, y, z, "minecraft:polished_blackstone_bricks")
            B.stair(x, P + 1 + i, z, DARK[0], "south")
            for y in range(P + 2 + i, P + 8):
                B.air(x, y, z)
        for x in (KC - 2, KC + 2):
            if i < 3:
                for y in range(P + 1, P + 2 + i):
                    B.put(x, y, z, "minecraft:polished_blackstone_bricks")
                B.put(x, P + 2 + i, z, "minecraft:polished_blackstone_brick_wall", {"up": "true", "north": "none",
                                                                                    "south": "none", "east": "none",
                                                                                    "west": "none", "waterlogged": "false"})
    for (x, z) in ((KC - 3, TOWER[0] - 3), (KC + 3, TOWER[0] - 3)):
        B.dog(x, P + 1, z, "high_guard", stay=1)                       # the High Guard, at the foot of the stair
    B.finish()
    return B


# ---------------------------------------------------------------- the farmsteads outside the walls
def farmstead():
    """
    Seventeen square, out among the wheat terraces (front -z toward the city): a little stone tool hut in one corner
    with the farmhand's bed, and the rest a fenced paddock for the cattle, horses and hens the wheat is grown for.
    """
    B = DogBuilder(17, 901)
    for x in range(1, 16):
        for z in range(1, 16):
            B.put(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    # The hut.
    B.floor(1, 7, 1, 7, 0, PLINTH)
    B.floor(2, 6, 2, 6, 0, FLOOR)
    B.stone_storey(1, 7, 1, 7, 1, 3, windows=True)
    B.hip_roof(1, 7, 1, 7, 4)
    B.door(4, 1, 1, "south", kind="dark_oak_door")
    B.put(4, 0, 0, PLINTH)
    B.bracket_lantern(2, 3, 0, "north")
    B.bed(2, 1, 5, "south", "brown")
    B.stocked_barrel(6, 1, 2, [("minecraft:iron_hoe", 2), ("minecraft:shears", 1), ("minecraft:bucket", 2),
                                ("minecraft:wheat_seeds", 32), ("minecraft:lead", 2)])
    B.put(6, 1, 4, "minecraft:grindstone", {"face": "floor", "facing": "north"})
    B.put(6, 1, 6, "minecraft:composter", {"level": "3"})
    B.put(4, 1, 6, "minecraft:hay_block", {"axis": "y"})
    B.put(4, 3, 4, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    B.dog(3, 1, 3, "stablemaster", stay=6)
    # The paddock: fenced round, a gate toward the city, hay, a trough.
    for i in range(1, 16):
        if i >= 8:
            B.fence(i, 1, 1, "minecraft:spruce_fence")
            B.fence(1, 1, i, "minecraft:spruce_fence")
        B.fence(15, 1, i, "minecraft:spruce_fence")
        B.fence(i, 1, 15, "minecraft:spruce_fence")
    B.put(12, 1, 1, "minecraft:spruce_fence_gate", {"facing": "south", "in_wall": "false", "open": "false",
                                                    "powered": "false"})
    for (x, z) in ((13, 13), (13, 12), (12, 13)):
        B.put(x, 1, z, "minecraft:hay_block", {"axis": "y"})
    B.put(13, 2, 13, "minecraft:hay_block", {"axis": "y"})
    for x in (9, 10):
        B.put(x, 1, 14, "minecraft:water_cauldron", {"level": "3"})
    for (x, z) in ((10, 4), (12, 7), (4, 11)):
        B.animal(x, 1, z, "cow")
    for (x, z, v) in ((11, 10, 3), (6, 13, 257)):
        B.animal(x, 1, z, "horse", Variant=v)
    for (x, z) in ((13, 4), (9, 9), (3, 10), (7, 10)):
        B.animal(x, 1, z, "chicken")
    B.finish()
    return B


# ---------------------------------------------------------------- the fables of the Keep
FABLES = [
    ["The First Fable:\nThe Three Bowls\n\nOld Brother Ashe set three bowls before his pupils. One was gold, and full of "
     "clear water. One was oak, and heaped with meat. One was clay, and held nothing at all.",
     "\"Take one,\" he said, \"and it is yours for life.\"\n\nThe proud pup took the gold, and was thirsty ever after, "
     "for he would not drink and spoil its shine.",
     "The hungry pup took the oak, and ate, and was hungry again by evening, and the bowl was empty.\n\nThe last pup "
     "lifted the empty clay.",
     "\"Why that one?\" the others laughed.\n\n\"Because it is the only one with room in it,\" she said, \"for "
     "whatever the day brings.\"\n\nShe became abbot."],
    ["The Second Fable:\nThe Two Banners\n\nTwo bannermen stood on the wall when the great storm came off the sea.",
     "The first lashed his banner tight to the pole, every corner bound, so that the wind could not take it. The second "
     "untied his cords and let the cloth run out loose on the wind.",
     "By morning the first banner hung in rags, torn on its own knots.\n\nThe second still flew, whole, cracking in the "
     "last of the gale.",
     "\"What is held too hard is torn,\" said the captain. \"Let it fly, and it will be there when the wind is gone.\""],
    ["The Third Fable:\nThe Butcher's Thumb\n\nThere was a butcher in the meat quarter who rested his thumb on the "
     "scale as he weighed. Each cut weighed a little more than it was, and he grew fat and rich.",
     "One winter his own pup fell ill, and he went to buy her medicine by weight. The apothecary rested his thumb on "
     "the scale, as the butcher had taught the whole quarter to do.",
     "The dose was short. The pup grew no better.\n\nThe butcher went home and wept, and from that day he weighed "
     "with his thumb laid on his own heart,",
     "where it weighs what a dog owes to every other dog.\n\nHis scales are kept in the Keep to this day, and they "
     "are true."],
    ["The Fourth Fable:\nThe Silent Bell\n\nIn the back hall hangs a bell that has not rung in a hundred years.",
     "The young monks wished to melt it down. \"It does nothing,\" they said. \"It is only bronze taking up room.\""
     "\n\nThe abbot said, \"It rings when the city is in peril.\"",
     "\"Then it is worth nothing, for it never rings.\"\n\n\"Then count,\" said the abbot, \"every year it has "
     "not rung. That is what it is worth.\"",
     "They counted a hundred years, and did not melt it."],
    ["The Fifth Fable:\nThe One Who Is Wrapped\n\nWhen the city was young, a sickness came that no medicine could "
     "touch. It went from dog to dog, and the strong fell first.",
     "One dog went up into the high room and called the sickness to himself. It came, all of it, out of the city and "
     "into him, and he wrapped himself in linen so that none of it could get out again.",
     "He kneels there still. He does not eat. He does not speak.\n\nWhat is bound must stay bound. Bring him no "
     "harm, and never loose a single turn of the linen.",
     "For what he holds, he holds for all of us."],
]


# ---------------------------------------------------------------- the dark tower
def dark_tower():
    """
    Nine square (walls 1..9, eleven with its eaves), dark stone, high above everything. It stands on a solid base, its
    one door four blocks up in a massive portal, reached by a stair from the courtyard (built in the Keep's template).
    Inside, a stair spirals up round a solid core to a bare wooden chamber at the top, where a dog kneels on a mat,
    wrapped in bandages. The spiral turns on flat landings, so every rise is a half-block stair step.
    """
    B = DogBuilder(11, 701)
    T = 44                     # the chamber's floor
    W0, W1 = 1, 9
    D = 5                      # the door's sill: the base below it is solid
    BRICK, POL = "minecraft:polished_blackstone_bricks", "minecraft:polished_blackstone"
    B.floor(W0, W1, W0, W1, 0, BRICK)
    for y in range(1, T + 6):
        for x in range(W0, W1 + 1):
            for z in range(W0, W1 + 1):
                wall = x in (W0, W1) or z in (W0, W1)
                core = 3 <= x <= 7 and 3 <= z <= 7
                if wall:
                    B.put(x, y, z, POL if y <= 2 or y % 9 == 0 else BRICK)
                elif (core and y < T) or y < D:
                    B.put(x, y, z, "minecraft:blackstone" if (x + z + y) % 7 else "minecraft:gilded_blackstone")
                else:
                    B.air(x, y, z)
    # A battered foot round the base (but not before the door, where the stair arrives).
    for x in range(0, 11):
        for z in range(0, 11):
            if (x in (0, 10) or z in (0, 10)) and not (z == 0 and 2 <= x <= 8):
                f = "south" if z == 0 else "north" if z == 10 else "east" if x == 0 else "west"
                B.stair(x, 1, z, DARK[0], f)
                B.put(x, 0, z, BRICK)
    # The portal: two great pillars standing out from the face, a deep lintel, a hood of upturned stairs, soul lamps.
    for x in (3, 7):
        for y in range(1, 12):
            B.put(x, y, 0, "minecraft:chiseled_polished_blackstone" if y in (D + 3, 11) else BRICK)
    for x in range(3, 8):
        for y in (D + 4, D + 5):
            B.put(x, y, 0, BRICK)
        B.stair(x, D + 6, 0, DARK[0], "south", half="top")
    B.put(5, D + 5, 0, "minecraft:chiseled_polished_blackstone")
    for x in (4, 6):
        B.put(x, D + 3, 0, "minecraft:soul_lantern", {"hanging": "true", "waterlogged": "false"})
    # The door, dark oak, iron-barred above.
    B.door(5, D, W0, "south", kind="dark_oak_door")
    B.put(5, D + 2, W0, "minecraft:iron_bars", {})
    B.put(5, D + 3, W0, "minecraft:chiseled_polished_blackstone")
    # The spiral. Runs of stairs along each side, a flat landing at each corner.
    ring = [(5, 2), (6, 2), (7, 2), (8, 2)] + [(8, z) for z in range(3, 9)] + [(x, 8) for x in range(7, 1, -1)] \
        + [(2, z) for z in range(7, 1, -1)] + [(3, 2), (4, 2)]
    corners = {(8, 2), (8, 8), (2, 8), (2, 2)}
    dirs = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}
    level = D                  # the height a climber stands at
    i = 0
    steps = []
    while level < T + 1:
        x, z = ring[i % len(ring)]
        nx, nz = ring[(i + 1) % len(ring)]
        if (x, z) in corners:
            B.put(x, level - 1, z, DARK[1])                                       # a landing, level with the last step
        else:
            B.stair(x, level, z, DARK[0], dirs[(nx - x, nz - z)])
            if level - 1 >= D:
                B.put(x, level - 1, z, DARK[1])
            level += 1
        steps.append((x, z, level))
        if i % 6 == 3 and (x, z) not in corners:
            # A soul torch on the core beside the step, at head height.
            cx, cz = (x + (1 if x == 2 else -1 if x == 8 else 0), z + (1 if z == 2 else -1 if z == 8 else 0))
            face = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(x - cx, z - cz)] \
                if (x - cx, z - cz) in ((1, 0), (-1, 0), (0, 1), (0, -1)) else None
            if face and 3 <= cx <= 7 and 3 <= cz <= 7:
                B.put(x, level + 1, z, "minecraft:soul_wall_torch", {"facing": face})
        i += 1
    # Slits of light up the walls.
    for y in range(D + 8, T, 9):
        for (x, z) in ((5, W0), (W1, 5), (5, W1), (W0, 5)):
            B.put(x, y, z, "minecraft:iron_bars", {})
    # The chamber: a bare wooden floor, open over the last of the stair; narrow windows, a mat, the dog.
    well = {(x, z) for (x, z, lv) in steps if lv >= T - 2}
    for x in range(W0 + 1, W1):
        for z in range(W0 + 1, W1):
            if (x, z) not in well:
                B.put(x, T, z, "minecraft:spruce_planks")
    for (x, z) in ((5, W0), (W1, 5), (5, W1), (W0, 5)):
        B.put(x, T + 2, z, "minecraft:iron_bars", {})
        B.put(x, T + 3, z, "minecraft:iron_bars", {})
    B.put(5, T + 1, 5, "minecraft:brown_carpet")
    B.dog(5, T + 1, 5, "bandaged", stay=0)
    B.hip_roof(W0, W1, W0, W1, T + 6, mat=DARK)
    B.finish()
    return B


def main():
    os.makedirs(DIR, exist_ok=True)
    out = {}
    for name, (seed, sign, workers, beds, furnish, floor) in WORKSHOPS.items():
        assert beds >= len(workers), name
        out[name] = workshop(seed, sign, workers, beds, furnish, floor)
    for kind in ("bow", "sword", "pike"):
        out[f"dc_tower_{kind}"] = tower(800 + len(kind), kind)
    out["dc_kennel"] = tower(810, "kennel")
    out["dc_gatehouse"] = gatehouse()
    out["dc_keep"] = keep()
    out["dc_dark_tower"] = dark_tower()
    out["dc_farmstead"] = farmstead()
    for name, B in out.items():
        n, p, top, mods = B.save_pair(os.path.join(DIR, name))
        dogs = sum(1 for e in B.entities if e[3].get("id") == "childrenofmu:dog_folk")
        beds = sum(1 for b in B.blocks.values() if b[0].endswith("_bed") and b[1].get("part") == "head")
        print(f"{name}: size {B.size}, height {top}, {n} blocks, {mods} mod blocks, {dogs} dogs, {beds} beds")


if __name__ == "__main__":
    main()
