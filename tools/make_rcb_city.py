"""
RCB City: the Rat, Cat and Bat city. A crowded, crooked medieval labyrinth in the spirit of Ankh-Morpork and
Antipolis: tall, narrow, leaning houses with jettied upper floors, red-tile spires and the odd verdigris copper roof,
alleys only a couple of blocks wide, lamp-lit cobbles. Bats roost on posts sticking out of the towers by day (some
under little awnings) and play Bazz in the clubs all night; Cats rule from a keep with a dead tree on its top; Rats
live underneath and do the dirty work.

    python tools/make_rcb_city.py

Template y = SINK - 1 is the ground; the front (door) of every building faces -z, DOOR blocks from the center.
Writes data/childrenofmu/structure/rcb_*.nbt (plus _vanilla twins where mod blocks are used).
"""
import math
import json
import os

from frog_build import FrogBuilder
from nbt_structure import Byte

from rcb_build import DOMUM, FLOOR, OPP, PLASTERS, STONES, RCBBuilder, rickety_deck, roof_deck, roost_rack, row, steps

HERE = os.path.dirname(os.path.abspath(__file__))
DIR = os.path.join(HERE, "..", "src", "main", "resources", "data", "childrenofmu", "structure")
OPPOSITE_OF = OPP


# ===================================================================== the buildings

def house(variant):
    """Five kinds of stand-alone town house: narrow, tall, leaning, each with its own kind of roof."""
    specs = {
        "a": dict(hw=2, hd=3, floors=4, lean=0.34, roof="tile", kind="gable_z", ground="shop",
                  folk=[("rat", "runner", 0), ("cat", "rogue", 1), ("rat", "postrat", 2)], seed=911),
        "b": dict(hw=3, hd=3, floors=3, lean=-0.25, roof="copper", kind="spire", ground="brick",
                  folk=[("cat", "fellmonger", 0), ("rat", "scavenger", 1)], seed=912),
        "c": dict(hw=3, hd=3, floors=4, lean=0.25, roof="slate", kind="gable_x", ground="stone",
                  folk=[("rat", "accountant", 0), ("rat", "sweeper", 1), ("cat", "adventurer", 3)], seed=913),
        "d": dict(hw=3, hd=3, floors=3, lean=0.34, roof="slate", kind="mansard", ground="shop",
                  folk=[("rat", "engineer", 0), ("cat", "hunter", 1), ("rat", "runner", 2)], seed=914),
        "e": dict(hw=2, hd=2, floors=5, lean=-0.34, roof="tile", kind="spire", ground="stone",
                  folk=[("rat", "postrat", 0), ("bat", "folk", 4)], seed=915),
    }
    s = specs[variant]
    B = RCBBuilder(15, seed=s["seed"])
    C = B.c
    B.townhouse(C - s["hw"], C + s["hw"], C - s["hd"], C + s["hd"], s["floors"], roof_kind=s["kind"], roof=s["roof"],
                ground=s["ground"], lean_z=s["lean"], folk=s["folk"])
    B.finish()
    return B, s["hd"] + 1


ROW_FOLK = [[("rat", "runner", 0)], [("cat", "rogue", 1)], [("rat", "sweeper", 0)], [("rat", "scavenger", 1)],
            [("cat", "fellmonger", 0)], [("rat", "postrat", 2)], [("bat", "flapper", 1)], [("rat", "accountant", 0)]]


def terrace(name):
    """Rows of joined houses, like rowhomes: wildly different heights, materials and roofs, shoulder to shoulder."""
    specs = {
        "a": (921, [(5, 4, "gable_z", "tile", "shop"), (7, 3, "gable_x", "slate", "stone"),
                    (5, 5, "spire", "tile", "shop"), (6, 2, "gable_z", "mud", "brick")]),
        "b": (922, [(6, 3, "mansard", "slate", "shop"), (5, 5, "gable_z", "tile", "stone"),
                    (7, 4, "gable_x", "copper", "shop")]),
        "c": (923, [(5, 3, "gable_z", "shingle", "stone"), (5, 4, "gable_z", "tile", "shop"),
                    (6, 2, "gable_x", "tile", "brick"), (5, 5, "spire", "copper", "shop"),
                    (5, 3, "gable_z", "slate", "stone")]),
    }
    seed, lots = specs[name]
    B = RCBBuilder(31, seed=seed)
    depth = 7
    z_front = B.c - depth // 2
    folk = [ROW_FOLK[(i + seed) % len(ROW_FOLK)] for i in range(len(lots))]
    row(B, z_front, depth, lots, folk)
    B.finish()
    return B, B.c - z_front + 1


def roost_posts(B, x0, x1, z0, z1, ys):
    """Bat roost posts (fences) sticking out of every face of a tower at these heights, some under awnings."""
    rng = B.rng
    faces = [("north", [(x, z0 - 1) for x in range(x0 + 1, x1)], (0, 1)),
             ("south", [(x, z1 + 1) for x in range(x0 + 1, x1)], (0, -1)),
             ("west", [(x0 - 1, z) for z in range(z0 + 1, z1)], (1, 0)),
             ("east", [(x1 + 1, z) for z in range(z0 + 1, z1)], (-1, 0))]
    for y in ys:
        for face, spots, (ix, iz) in faces:
            chosen = [p for i, p in enumerate(spots) if (i + y) % 2 == 0 or rng.random() < 0.3]
            chosen.sort()
            run = []
            for p in chosen:
                if not B.is_free(p[0], y, p[1]) or not B.is_free(p[0], y - 1, p[1]):
                    continue
                B.fence(p[0], y, p[1], "minecraft:spruce_fence")
                run.append(p)
            # Awnings: some posts get their own, some neighbours share one.
            i = 0
            while i < len(run):
                p = run[i]
                r = rng.random()
                share = i + 1 < len(run) and abs(run[i + 1][0] - p[0]) + abs(run[i + 1][1] - p[1]) == 1
                if r < 0.35:
                    i += 1
                    continue
                group = [p, run[i + 1]] if share and r > 0.6 else [p]
                for q in group:
                    # A lean-to of stairs, high side against the wall, over the post.
                    B.stair(q[0], y + 1, q[1], "minecraft:dark_oak_stairs", OPPOSITE_OF[face])
                i += len(group)


OPPOSITE_OF = {"north": "south", "south": "north", "west": "east", "east": "west"}


def bat_tower(seed):
    """A tall roost tower: stone below, timber above, a spire on top, and roost posts all the way up."""
    B = RCBBuilder(17, seed=seed)
    C = B.c
    rng = B.rng
    # Stone base, 7 x 7, two storeys.
    for y in range(0, 9):
        for x in range(C - 3, C + 4):
            for z in range(C - 3, C + 4):
                edge = x in (C - 3, C + 3) or z in (C - 3, C + 3)
                if y == 0 or edge:
                    B.stone(x, y, z)
                else:
                    B.air(x, y, z)
    for (x, z) in ((C - 3, C - 3), (C + 3, C - 3), (C - 3, C + 3), (C + 3, C + 3)):
        B.log(x, 0, z, kind="dark_oak_log")        # footings
    # The narrower timber shaft above, leaning a little as it climbs.
    lean = rng.choice([(1, 0), (0, 1), (-1, 0)])
    top = 26
    shaft = []
    for y in range(9, top):
        s = (y - 9) // 9
        x0, x1 = C - 2 + lean[0] * s, C + 2 + lean[0] * s
        z0, z1 = C - 2 + lean[1] * s, C + 2 + lean[1] * s
        shaft.append((y, x0, x1, z0, z1))
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge_x, edge_z = x in (x0, x1), z in (z0, z1)
                if y == 9 or (y - 9) % 9 == 0:
                    if edge_x or edge_z:
                        B.log(x, y, z, axis="x" if edge_z else "z", kind="stripped_dark_oak_log")
                    else:
                        B.put(x, y, z, FLOOR)
                elif edge_x and edge_z:
                    B.log(x, y, z, kind="dark_oak_log")
                elif edge_x or edge_z:
                    plaster = "minecraft:white_terracotta" if (y // 3) % 2 else "minecraft:light_gray_terracotta"
                    out = ("north" if z == z0 else "south") if edge_z else ("west" if x == x0 else "east")
                    B.frame(x, y, z, ["one_crossed_lr", "framed", "double_crossed"][(y - 9) % 3], out, plaster)
                else:
                    B.air(x, y, z)
        # Little round windows.
        if y % 4 == 2:
            B.pane((x0 + x1) // 2, y, z0)
            B.pane((x0 + x1) // 2, y, z1)
    # Ladders up the inside.
    for y in range(1, top):
        row = [r for r in shaft if r[0] == y]
        x0, x1, z0, z1 = (C - 3, C + 3, C - 3, C + 3) if not row else row[0][1:]
        B.put(x1 - 1, y, z1 - 1, "minecraft:ladder", {"facing": "west", "waterlogged": "false"}) \
            if not B.blocks.get((x1 - 1, y, z1 - 1), ("",))[0].endswith("log") else None
    # Roost racks round the shaft, staggered so every rod has open air beneath it, and posts on the base.
    for i, (yy, faces) in enumerate(((13, ("north", "south")), (16, ("east", "west")), (21, ("north", "south")),
                                    (24, ("east", "west")))):
        _, x0, x1, z0, z1 = [r for r in shaft if r[0] == yy][0]
        for f in faces:
            if f in ("north", "south"):
                roost_rack(B, x0 + 1, x1 - 1, z0 if f == "north" else z1, yy, f)
            else:
                roost_rack(B, z0 + 1, z1 - 1, x0 if f == "west" else x1, yy, f)
    roost_posts(B, C - 3, C + 3, C - 3, C + 3, [6])
    # The spire.
    _, x0, x1, z0, z1 = shaft[-1]
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            B.put(x, top, z, "minecraft:dark_oak_planks")
    B.roof_spire(x0, x1, z0, z1, top + 1, roof="tile", rise=3, finial="rod")
    B.put((x0 + x1) // 2, top - 1, (z0 + z1) // 2, "minecraft:bell", {"attachment": "ceiling", "facing": "north",
                                                                      "powered": "false"})
    B.door(C, 1, C - 3, "south", kind="dark_oak_door")
    B.street_sign(C + 1, C - 3, 3, ["BAT ROOST", "No cats"])
    for (x, z, f) in ((C - 2, C - 4, "north"), (C + 2, C - 4, "north"), (C - 4, C, "west"), (C + 4, C, "east")):
        B.bracket_lantern(x, 4, z, f)
    B.hanging_lantern(C, 7, C)
    B.hanging_lantern(C - 1, 3, C + 1)
    for v in ("folk", "flapper", "jazz", "folk"):
        B.folk(C + rng.randint(-1, 1), 1, C + rng.randint(-1, 1), "bat", v)
    B.finish()
    return B, 4


def bazz_club():
    """A Bazz club: a low brick hall, a stage at the back, a bar down one side, warm lamps, a Calf on every table."""
    B = RCBBuilder(23, seed=921)
    C = B.c
    x0, x1, z0, z1 = C - 6, C + 6, C - 5, C + 5
    H = 6
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            B.put(x, 0, z, "minecraft:dark_oak_planks" if (x + z) % 2 else "minecraft:spruce_planks")
            for y in range(1, H + 1):
                edge_x, edge_z = x in (x0, x1), z in (z0, z1)
                if edge_x and edge_z:
                    B.log(x, y, z, kind="dark_oak_log")
                elif edge_x or edge_z:
                    B.put(x, y, z, "minecraft:bricks")
                else:
                    B.air(x, y, z)
    for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        B.log(x, 0, z, kind="dark_oak_log")
    # Tall windows along the front and back, glowing amber, each in a timber frame (none crowding the doors).
    for x in range(x0 + 2, x1 - 1, 3):
        for z in (z0, z1):
            if z == z0 and C - 1 <= x <= C + 2:
                continue
            for y in (2, 3):
                B.put(x, y, z, "minecraft:orange_stained_glass_pane")
            B.put(x, 4, z, "minecraft:stone_bricks")
            B.put(x, 1, z, "minecraft:stone_bricks")
            for dx in (-1, 1):
                for y in (1, 2, 3, 4):
                    B.log(x + dx, y, z, kind="dark_oak_log")
    # A course of stone along the bottom and a dark beam along the top of the brick, and windows in the ends.
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x in (x0, x1) or z in (z0, z1)) and not (x in (x0, x1) and z in (z0, z1)):
                if B.blocks.get((x, 1, z), ("",))[0] == "minecraft:bricks":
                    B.stone(x, 1, z)
                if B.blocks.get((x, H, z), ("",))[0] in ("minecraft:bricks", "minecraft:dark_oak_planks"):
                    B.log(x, H, z, axis="x" if z in (z0, z1) else "z", kind="stripped_dark_oak_log")
    for x in (x0, x1):
        for z in (C - 2, C + 2):
            for y in (2, 3):
                B.put(x, y, z, "minecraft:orange_stained_glass_pane")
    # The stage, at the back.
    for x in range(x0 + 1, x1):
        for z in range(z1 - 3, z1):
            B.put(x, 1, z, "minecraft:dark_oak_slab", {"type": "top", "waterlogged": "false"}) if z == z1 - 3 else \
                B.put(x, 1, z, "minecraft:dark_oak_planks")
    for x in (x0 + 1, x1 - 1):
        for y in range(2, H):
            B.put(x, y, z1 - 1, "minecraft:red_wool")              # curtains
    B.put(C, 2, z1 - 1, "minecraft:jukebox")
    B.put(C + 2, 2, z1 - 1, "minecraft:note_block", {"instrument": "bass", "note": "0", "powered": "false"})
    B.put(C - 2, 2, z1 - 1, "minecraft:note_block", {"instrument": "flute", "note": "0", "powered": "false"})
    for x in range(x0 + 2, x1 - 1, 2):
        B.put(x, H - 1, z1 - 2, "minecraft:ochre_froglight", {"axis": "y"}) if (x - x0) % 4 == 2 else None
    # The bar, down the west side, with barrels of Calf behind it.
    for z in range(z0 + 2, z1 - 4):
        B.stair(x0 + 3, 1, z, "minecraft:dark_oak_stairs", "east", half="top")
        B.barrel(x0 + 1, 1, z, "east")
        B.put(x0 + 1, 2, z, "minecraft:brewing_stand", {"has_bottle_0": "true", "has_bottle_1": "false",
                                                         "has_bottle_2": "true"}) if z % 3 == 0 else None
    # Tables and stools.
    for (tx, tz) in ((C + 1, z0 + 2), (C + 4, z0 + 2), (C + 1, C), (C + 4, C)):
        B.fence(tx, 1, tz, "minecraft:dark_oak_fence")
        B.put(tx, 2, tz, "minecraft:candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
        for sx in (tx - 1, tx + 1):
            B.stair(sx, 1, tz, "minecraft:spruce_stairs", "west" if sx < tx else "east")   # stools
    for x in range(x0 + 2, x1 - 1, 3):
        for z in (z0 + 2, C + 2):
            B.hanging_lantern(x, H, z)
    # Roof: a low red-tile gable, and a sign of the club over the door.
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            B.put(x, H, z, "minecraft:dark_oak_planks")
    B.roof_gable(x0, x1, z0, z1, H + 1, ridge="x", roof="tile", wall="minecraft:bricks", dormers=2)
    for x in range(x0 + 1, x1, 3):
        B.wall_torch(x, 3, z0 - 1, "north")
    # Double doors: hinged on the outside edges, so they swing open as a pair.
    B.door(C, 1, z0, "south", kind="dark_oak_door")
    B.door(C + 1, 1, z0, "south", kind="dark_oak_door")
    for y in (1, 2):
        B.blocks[(C, y, z0)][1]["hinge"] = "right"
        B.blocks[(C + 1, y, z0)][1]["hinge"] = "left"
    for y in (1, 2, 3):
        B.log(C - 1, y, z0, kind="dark_oak_log")
        B.log(C + 2, y, z0, kind="dark_oak_log")
    for x in (C, C + 1):
        B.log(x, 3, z0, axis="x", kind="stripped_dark_oak_log")
    for x in (C - 1, C + 2):
        B.lantern_post(x, 1, z0 - 1, 2)
    for x in range(C - 2, C + 4):
        B.put(x, 4, z0 - 1, "minecraft:red_wool" if x % 2 else "minecraft:black_wool")   # a striped awning
    B.street_sign(C - 3, z0, 4, ["THE", "UPSIDE", "DOWN", "Bazz nightly"], wood="dark_oak", color="yellow", glow=True)
    # A rickety terrace on stilts off the east side, a step up from the street, with a door out to it.
    for y in (2, 3):
        B.air(x1, y, C)
    B.put(x1, 1, C, "minecraft:dark_oak_planks")
    rickety_deck(B, x1 + 1, x1 + 4, z0 + 1, z1 - 1, 2, ground=0,
                 patrons=[("cat", "rogue"), ("rat", "runner"), ("cat", "adventurer")])
    # The band and the barman.
    B.folk(x0 + 2, 1, C - 1, "bat", "barman")
    B.folk(C - 2, 2, z1 - 2, "bat", "jazz")
    B.folk(C + 2, 2, z1 - 2, "bat", "folk")
    B.folk(C, 2, z1 - 2, "bat", "flapper")
    B.folk(C + 3, 1, C - 1, "cat", "rogue")
    B.folk(C + 1, 1, z0 + 3, "cat", "hunter")
    B.finish()
    return B, 6


def bazz_deck():
    """
    A Bazz house: a tall crooked house with the bar downstairs, a flat roof deck where the band plays under a
    striped canopy at the front, and a rickety deck stuck out from the second floor for listening, drinking Calf and
    watching the street, fenced all round and reached from inside; the ladder runs on up through a hatch
    in the roof deck, at the back, well away from the stage (the bats just fly up to it).
    """
    B = RCBBuilder(23, seed=925)
    C = B.c
    xa, xb, za, zb = C - 6, C, C - 3, C + 3
    top, bounds = B.townhouse(xa, xb, za, zb, 3, roof_kind="deck", roof="tile", ground="shop", lean_z=0.0,
                              bay=False, damage=False, chimney=False,
                              folk=[("bat", "barman", 0), ("cat", "hunter", 1)],
                              shop_name=["CALF BAR", "Bazz up", "on the roof"])
    tx0, tx1, tz0, tz1 = bounds[-1]
    # The townhouse's ladder post (see RCBBuilder.townhouse): carry its ladder up through a hatch in the deck.
    oz0 = max(b[2] for b in bounds) + 2
    oz1 = min(b[3] for b in bounds) - 1
    px, pz = (xb - 1 if xb - xa >= 4 else xa + 1), max(oz0, oz1)
    roof_deck(B, tx0, tx1, tz0, tz1, 12, stage="front", hatch=(px, pz - 1))
    B.log(px, 13, pz, kind="dark_oak_log")            # a stub of the post to hold the top of the ladder
    B.put(px, 13, pz - 1, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    B.put(px, 14, pz, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # The listening deck off the second floor, with a doorway out to it from the room inside.
    for y in (5, 6):
        B.air(xb, y, C)
    rickety_deck(B, xb + 1, xb + 5, za, zb, 4, ground=0,
                 patrons=[("cat", "rogue"), ("rat", "scavenger"), ("bat", "folk"), ("cat", "fellmonger")])
    for x in range(xb + 1, xb + 6):
        B.wall_torch(x, 2, za - 1, "north") if (x - xb) % 2 == 0 else None
    B.finish()
    return B, 4


def skyway(variant):
    """
    Two crooked houses either side of an alley, joined high up by a covered timber skyway, like the old bridges
    between houses in Ankh-Morpork (or the Rhine towns). Variant b's skyway is bigger and lived in: a little room
    with a window box, a lamp hanging under it over the alley.
    """
    seed = {"a": 931, "b": 932}[variant]
    B = RCBBuilder(27, seed=seed)
    C = B.c
    gap = 3 if variant == "a" else 4
    depth = 7
    za, zb = C - 3, C + 3
    lx1 = C - (gap + 1) // 2 - 1
    rx0 = lx1 + gap + 1
    lf, rf = (3, 4) if variant == "a" else (4, 3)
    B.townhouse(lx1 - 5, lx1, za, zb, lf, roof_kind="gable_z", roof="tile", ground="shop", lean_z=0.25,
                bay=False, sides=(True, False), folk=[("rat", "runner", 0), ("cat", "rogue", 1)])
    B.townhouse(rx0, rx0 + 6, za, zb, rf, roof_kind="gable_x" if variant == "a" else "spire",
                roof="slate" if variant == "a" else "copper", ground="stone", lean_z=-0.25, bay=False,
                sides=(False, True), folk=[("rat", "postrat", 0), ("bat", "flapper", 2)])
    # The skyway: a timber-framed room across the alley at the second floor (and the third for variant b).
    fl = 1
    y0 = fl * 4
    sz0, sz1 = za + 1, zb - 1 if variant == "a" else zb
    for x in range(lx1, rx0 + 1):
        for z in range(sz0, sz1 + 1):
            inside = lx1 < x < rx0 and sz0 < z < sz1
            B.put(x, y0, z, FLOOR) if inside else B.log(x, y0, z, axis="x", kind="stripped_dark_oak_log")
            for y in range(y0 + 1, y0 + 4):
                if lx1 < x < rx0:
                    if z in (sz0, sz1):
                        if y == y0 + 2 and (x - lx1) % 2 == 1:
                            B.put(x, y, z, "minecraft:glass_pane", {})
                        else:
                            B.frame(x, y, z, "double_crossed" if y == y0 + 1 else "framed",
                                    "north" if z == sz0 else "south", "minecraft:white_terracotta")
                    else:
                        B.air(x, y, z)
                elif sz0 < z < sz1 and y < y0 + 3:
                    B.air(x, y, z)       # doorways into both houses
    # Brackets under it, a lamp over the alley, a little roof along it.
    for x in range(lx1 + 1, rx0):
        for z in (sz0, sz1):
            B.stair(x, y0 - 1, z, "minecraft:dark_oak_stairs", "south" if z == sz0 else "north", half="top")
    B.hanging_lantern((lx1 + rx0) // 2, y0 - 1, (sz0 + sz1) // 2)
    B.roof_gable(lx1 + 1, rx0 - 1, sz0, sz1, y0 + 5, ridge="x", roof="tile", wall="minecraft:white_terracotta",
                 trim=False)
    for x in range(lx1 + 1, rx0):
        for z in range(sz0 + 1, sz1):
            B.put(x, y0 + 4, z, FLOOR)
    if variant == "b":
        for x in range(lx1 + 1, rx0):
            if B.is_free(x, y0 + 1, sz0 - 1):
                B.put(x, y0, sz0 - 1, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
                B.put(x, y0 + 1, sz0 - 1, B.rng.choice(["minecraft:potted_red_tulip", "minecraft:potted_fern",
                                                        "minecraft:potted_poppy"]))
        B.put((lx1 + rx0) // 2, y0 + 1, (sz0 + sz1) // 2, "minecraft:red_carpet")
        B.folk((lx1 + rx0) // 2, y0 + 1, (sz0 + sz1) // 2, "rat", "accountant")
    # The alley underneath, cobbled.
    for x in range(lx1 + 1, rx0):
        for z in range(za - 2, zb + 3):
            B.put(x, 0, z, "minecraft:cobblestone" if (x + z) % 3 else "minecraft:mossy_cobblestone")
            for y in range(1, 4):
                if B.is_free(x, y, z):
                    B.air(x, y, z)
    B.finish()
    return B, 4


def bridge_houses():
    """
    The gatehouse: a lived-in bridge, like the painting. A stone arch on two towers over the passage out of town, and
    on top of it a row of tiny narrow houses, each with its own gable, window and chimney, opening onto a covered
    catwalk along the arch. Each tower has a door at the foot, a ladder up to a floor at catwalk level, a doorway out
    onto the catwalk, and (cut by the town wall where it meets the tower) a doorway onto the wall walk.
    """
    B = RCBBuilder(31, seed=941)
    C = B.c
    za, zb = C - 3, C + 3
    deck = 6
    for tx in (C - 9, C + 9):
        inner = 1 if tx < C else -1                     # toward the bridge
        for x in range(tx - 2, tx + 3):
            for z in range(za - 1, zb + 2):
                edge = x in (tx - 2, tx + 2) or z in (za - 1, zb + 1)
                for y in range(0, deck + 6):
                    if edge or y == 0:
                        B.stone(x, y, z)
                    else:
                        B.air(x, y, z)
                if not edge:
                    B.put(x, deck, z, "minecraft:spruce_planks")       # the upper floor, at catwalk level
        for (x, z) in ((tx - 2, za - 1), (tx + 2, za - 1), (tx - 2, zb + 1), (tx + 2, zb + 1)):
            B.log(x, 0, z, kind="dark_oak_log")
        B.roof_spire(tx - 2, tx + 2, za - 1, zb + 1, deck + 7, roof="slate", rise=3, finial="flag")
        for x in range(tx - 2, tx + 3):
            for z in range(za - 1, zb + 2):
                B.put(x, deck + 6, z, "minecraft:spruce_planks")
        B.door(tx, 1, za - 1, "south", kind="spruce_door")
        B.wall_torch(tx + 1, 3, za - 2, "north")
        for y in (3, 9):
            B.put(tx, y, za - 1, "minecraft:glass_pane", {})
        lx = tx - inner                                 # the ladder, in the outer back corner, up to the floor
        for y in range(1, deck + 1):
            B.put(lx, y, zb, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
        # Out onto the catwalk: a doorway in the tower's inner wall, at the catwalk's end.
        B.door(tx + 2 * inner, deck + 1, za, "west" if inner > 0 else "east", kind="spruce_door")
        # A guard's corner upstairs: a bunk, a barrel of arrows, a lamp.
        B.hanging_lantern(tx, deck + 5, C)
        B.stocked_barrel(tx + inner, deck + 1, zb, [("minecraft:arrow", 24), ("minecraft:bow", 1)])
        B.put(tx - inner, deck + 1, za, "minecraft:white_carpet")
        B.hanging_lantern(tx, 4, C)
        B.folk(tx, deck + 1, C, "cat", "archer" if tx < C else "guard")
    # The arch: spans between the towers, open underneath.
    for x in range(C - 6, C + 7):
        span = abs(x - C)
        under = 4 if span <= 3 else 3 if span <= 5 else 1
        for z in range(za - 1, zb + 1):
            for y in range(under + 1, deck + 1):
                B.stone(x, y, z) if y < deck else B.put(x, y, z, "minecraft:stone_bricks")
            if z in (za - 1, zb):
                B.stair(x, under, z, "minecraft:stone_brick_stairs", "east" if x < C else "west", half="top") \
                    if span > 0 else B.put(x, under, z, "minecraft:stone_brick_slab", {"type": "top", "waterlogged": "false"})
        for z in range(za - 3, zb + 4):
            B.put(x, 0, z, "minecraft:cobblestone" if (x * 7 + z) % 4 else "minecraft:stone_bricks")
            for y in range(1, under + 1):
                if B.is_free(x, y, z):
                    B.air(x, y, z)
    B.hanging_lantern(C, 4, C)
    # The catwalk along the front of the houses: on the arch, with a rail and lanterns.
    for x in range(C - 6, C + 7):
        B.put(x, deck, za, "minecraft:spruce_planks" if x % 3 else "minecraft:dark_oak_planks")
        B.fence(x, deck + 1, za - 1, "minecraft:spruce_fence")
    for x in (C - 4, C + 4):
        B.put(x, deck + 2, za - 1, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # The little houses on the bridge, set back one behind the catwalk.
    lots = [(C - 6, C - 3), (C - 3, C + 1), (C + 1, C + 4), (C + 4, C + 6)]
    roofs = ["tile", "slate", "tile", "mud"]
    for i, (xa, xb) in enumerate(lots):
        xb = max(xb, xa + 2)
        sub = RCBBuilder(B.size, seed=950 + i)
        sub.capped = False
        sub._shops_used = B.__dict__.setdefault("_shops_used", set())   # no two shops on the bridge alike
        sub.townhouse(xa, xb, za + 1, zb, 2, roof_kind="gable_z", roof=roofs[i], ground="shop" if i % 2 else "stone",
                      lean_z=0.0, jetty=True, bay=False, damage=None, sides=(i == 0, i == len(lots) - 1),
                      folk=[(["rat", "cat", "rat", "bat"][i], ["sweeper", "fellmonger", "postrat", "folk"][i], 0)])
        for (x, y, z), b in sub.blocks.items():
            if y == 0 and z == za:
                continue                                 # keep the catwalk the shop's doorstep would cover
            B.alt.pop((x, y + deck, z), None)
            B.blocks[(x, y + deck, z)] = b
            if (x, y, z) in sub.alt:
                B.alt[(x, y + deck, z)] = sub.alt[(x, y, z)]
            if (x, y, z) in sub.stairs:
                B.stairs.add((x, y + deck, z))
        for (ex, ey, ez, nbt) in sub.entities:
            B.entities.append((ex, ey + deck, ez, nbt))
    # Headroom along the catwalk: no lamps or brackets at head height.
    for x in range(C - 6, C + 7):
        for y in (deck + 1, deck + 2):
            b = B.blocks.get((x, y, za))
            if b and any(k in b[0] for k in ("lantern", "iron_bars")):
                B.air(x, y, za)
    B.finish()
    return B, 6


def guard_tower(seed):
    """
    A guard tower on the town wall: seven square, stone. The door at the foot faces into town. Inside, a ladder runs up
    one corner the whole way. On the ground floor, the guards' quarters: a bunk, a table and stool, arrows and bows in
    the barrels, a bow on the wall. The floor above is level with the wall walk (the wall cuts its own doorways into
    the tower wherever it meets it). On top, a fighting platform behind merlons. Open arrow loops all the way up.
    Ground level is y = 0 (sink 1).
    """
    B = RCBBuilder(11, seed=seed)
    C = B.c
    rng = B.rng
    x0, x1, z0, z1 = C - 3, C + 3, C - 3, C + 3
    TOP = 11
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            corner = x in (x0, x1) and z in (z0, z1)
            B.stone(x, 0, z) if edge else B.put(x, 0, z, "minecraft:spruce_planks")
            for y in range(1, TOP + 1):
                if edge:
                    if corner:
                        B.put(x, y, z, "minecraft:stone_bricks" if y % 3 else "minecraft:polished_andesite")
                    else:
                        B.stone(x, y, z)
                elif y in (6, TOP):
                    B.put(x, y, z, "minecraft:spruce_planks" if y == 6 else "minecraft:stone_bricks")
                else:
                    B.air(x, y, z)
            # Merlons round the top, every other block (the corners always).
            if edge and (corner or (x + z) % 2 == 0):
                B.stone(x, TOP + 1, z)
    # A cornice under the merlons.
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            if x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1):
                f = ("south" if z == z0 - 1 else "north") if z in (z0 - 1, z1 + 1) else ("east" if x == x0 - 1 else "west")
                B.stair(x, TOP, z, "minecraft:stone_brick_stairs", OPP[f], half="top")
    # The ladder, on the back wall just east of the middle, from the ground to the top (through hatches in both floors). It hangs on the
    # back wall, which faces out of town: the town wall never meets the tower there, so its doorways never cut it.
    # Off the corner too (a corner cell backs onto a side wall as well, where the town wall may come in).
    lx, lz = C + 1, z1 - 1
    for y in range(1, TOP + 2):
        B.put(lx, y, lz, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    B.stone(lx, TOP + 1, z1)                             # the merlon the top of the ladder hangs on
    # An arrow loop at walk level, facing out of town (the wall may meet the tower on the other faces), and the
    # fighting platform on top. The ground floor is the guards' quarters: solid walls.
    for y in (8, 9):
        B.air(C, y, z1)
    # The door, facing into town, a lamp beside it, a step.
    B.door(C, 1, z0, "south", kind="spruce_door")
    B.put(C, 3, z0, "minecraft:chiseled_stone_bricks")
    B.bracket_lantern(C - 2, 5, z0 - 1, "north")       # high and to one side, clear of the door
    B.stair(C, 0, z0 - 1, "minecraft:stone_brick_stairs", "south")
    # ---- The guards' quarters.
    B.bunk(x0 + 1, 1, z1 - 2, "south", "east")             # along the west wall, foot to the south... head north
    B.stair(C + 1, 1, z0 + 1, "minecraft:spruce_stairs", "east", half="top")      # a table...
    B.stair(C + 2, 1, z0 + 1, "minecraft:spruce_stairs", "west")                  # ...and a stool
    B.put(C + 1, 2, z0 + 1, "minecraft:candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
    B.stocked_barrel(x1 - 1, 1, C, [("minecraft:arrow", 32), ("minecraft:bow", 1), ("minecraft:arrow", 16)])
    B.stocked_barrel(x1 - 1, 2, C, [("minecraft:arrow", 32)], facing="west")
    B.item_frame(x1 - 1, 3, C - 1, "west", "minecraft:bow")
    B.item_frame(C - 1, 3, z0 + 1, "south", "minecraft:iron_sword")
    B.hanging_lantern(C, 5, C)
    # ---- Walk level: the watch post. Kept clear along the walls for the wall's doorways.
    B.stocked_barrel(C, 7, C, [("minecraft:arrow", 32), ("minecraft:bow", 1)])
    B.hanging_lantern(C - 1, TOP - 1, C)
    # ---- The top: a flag, and archers.
    B.fence(x0 + 1, TOP + 1, z0 + 1, "minecraft:dark_oak_fence")
    B.fence(x0 + 1, TOP + 2, z0 + 1, "minecraft:dark_oak_fence")
    B.put(x0 + 1, TOP + 3, z0 + 1, rng.choice(["minecraft:red_wool", "minecraft:yellow_wool"]))
    B.put(x0 + 2, TOP + 3, z0 + 1, "minecraft:red_wool" if rng.random() < 0.5 else "minecraft:black_wool")
    B.lantern_post(x1 - 1, TOP + 1, z0 + 1, 1)
    B.folk(C, 1, C, "cat", "guard")
    B.folk(C - 1, 7, C + 1, "cat", "archer", stay=3)
    B.folk(C, TOP + 1, C, "cat", "archer", stay=2)
    B.weathered = False
    B.finish()
    return B, 4


def fight_pit():
    """
    The fighting pit: a square of sand sunk four deep, walled in stone and ringed with iron bars, stepped stands
    all round, and the Bat bookie's booth on the corner. Ground level is y = 4 (sink 4).
    """
    B = RCBBuilder(21, seed=931)
    C = B.c
    G = 4        # ground level (first air above the ground) in the template
    R = 4        # the pit's half-width
    S = R + 4    # outer half-width of the stands
    for x in range(C - S, C + S + 1):
        for z in range(C - S, C + S + 1):
            d = max(abs(x - C), abs(z - C))
            for y in range(0, G + 4):
                B.air(x, y, z) if y >= 1 else None
            if d <= R:
                B.stone(x, 0, z)
                B.put(x, 1, z, "minecraft:sand")
            elif d == R + 1:
                for y in range(0, G):
                    B.stone(x, y, z)
                B.put(x, G, z, "minecraft:iron_bars")
            elif d <= S:
                # The stands step up and away from the pit.
                step = d - (R + 2)            # 0, 1, 2
                for y in range(0, G - 1 + step):
                    B.stone(x, y, z) if y < G - 1 else B.put(x, y, z, "minecraft:spruce_planks")
                if d < S:
                    # Seats face the pit: their backs (the stairs' high side) away from it.
                    face = ("east" if x > C else "west") if abs(x - C) >= abs(z - C) else ("south" if z > C else "north")
                    B.stair(x, G - 1 + step, z, "minecraft:spruce_stairs", face)
                else:
                    B.put(x, G - 1 + step, z, "minecraft:stone_bricks")
                    B.fence(x, G + step, z, "minecraft:dark_oak_fence")
    # Blood and sawdust on the sand.
    for _ in range(6):
        x, z = C + B.rng.randint(-R, R), C + B.rng.randint(-R, R)
        B.put(x, 2, z, "minecraft:red_carpet") if B.rng.random() < 0.5 else None
    # The way in, at the front.
    for z in range(C - S, C - R - 1):
        for x in (C - 1, C, C + 1):
            for y in range(G, G + 4):
                B.air(x, y, z)
            B.put(x, G - 1, z, "minecraft:stone_bricks")
    # The fighters' gate: a tunnel under the east stands, a gate into the pit, a ramp up to the street.
    for x in range(C + R + 1, C + S + 3):
        for z in (C - 1, C, C + 1):
            B.stone(x, 1, z) if z == C or x <= C + S else B.put(x, 1, z, "minecraft:stone_bricks")
        for y in (2, 3):
            B.air(x, y, C)
            for z in (C - 1, C + 1):
                B.put(x, y, z, "minecraft:stone_bricks")
        B.put(x, 4, C, "minecraft:stone_bricks") if x <= C + S else None
    B.put(C + R + 1, 2, C, "minecraft:dark_oak_fence_gate", {"facing": "east", "in_wall": "false", "open": "false",
                                                               "powered": "false"})
    B.put(C + R + 1, 3, C, "minecraft:iron_bars", {})
    B.stair(C + S + 1, 2, C, "minecraft:stone_brick_stairs", "east")
    B.stair(C + S + 2, 3, C, "minecraft:stone_brick_stairs", "east")
    B.put(C + S + 2, 2, C, "minecraft:stone_bricks")
    B.hanging_lantern(C + R + 3, 3, C)
    # The bookie's booth on the front-left corner.
    bx, bz = C - S + 1, C - S + 1
    for x in range(bx - 1, bx + 3):
        for z in range(bz - 1, bz + 3):
            B.put(x, G + 3, z, "minecraft:red_wool" if (x + z) % 2 else "minecraft:white_wool")
    for (x, z) in ((bx - 1, bz - 1), (bx + 2, bz - 1), (bx - 1, bz + 2), (bx + 2, bz + 2)):
        for y in range(G, G + 3):
            B.fence(x, y, z, "minecraft:dark_oak_fence")
    B.lectern(bx + 1, G, bz + 1, "south", ["THE BOOK. Bets pay double. No credit. No hedging.",
                                             "Mangler Tibbles: 14 wins, 2 losses, 1 ear.",
                                             "Rats: many. Odds: depends how many."])
    B.hanging_lantern(bx, G + 2, bz)
    B.sign(bx + 1, G + 2, bz - 1, ["THE PIT", "Bets taken", "Pays double"], facing="north", wall=False,
           wood="dark_oak", color="red")
    B.folk(bx, G, bz + 1, "bat", "bookie")
    # Lamp posts at the corners of the stands, and spectators.
    for (x, z) in ((C + S, C - S), (C - S, C + S), (C + S, C + S)):
        B.lantern_post(x, G + 2, z, 2)
    B.folk(C + S - 1, G + 2, C, "cat", "rogue")
    B.folk(C, G + 2, C + S - 1, "rat", "scavenger")
    B.folk(C - S + 1, G + 2, C + 2, "cat", "cop")
    B.folk(C + 2, G + 2, C + S - 1, "bat", "flapper")
    B.finish()
    return B, S + 1


def justice_hall():
    """
    The Justice Hall, at the heart of the city: a tall stone hall of three storeys with round corner turrets under
    red spires, a crooked bell tower rising out of its roof with a dead tree on top (the Cats like a good sharpening
    post). Inside, open staircases climb from floor to floor along the walls.
      - Ground floor: the Watch. Desks under lamps, the charge book, a waiting bench, wanted posters, a rack of arms,
        a fireplace, and the cell, built in under the stairs, with somebody in it.
      - Middle floor: the guards' barracks and armory. Bunk beds, armor on stands, weapons on the walls, barrels of
        arrows, a mess table, a smithy corner.
      - Top floor: the throne room of the Great Gar, master of the city: vast, fat, and going nowhere, on a great
        yellow cushion on a gilded dais. Rich carpets, stained glass, chandeliers, banners, and the Gar's clerks (rats
        in green eyeshades) at their desks under green banker's lamps. Nobody trades or bets in the city without the
        Gar's Writ of Trade.
    """
    B = RCBBuilder(31, seed=941)
    B.weathered = False
    C = B.c
    rng = B.rng
    hw = 8
    FH = 6                       # storey height
    floors = 3
    TOPH = 10                    # the throne room is lofty
    H = FH * (floors - 1) + TOPH  # top of the walls
    LEVELS = (0, FH, 2 * FH)     # floor heights
    x0, x1, z0, z1 = C - hw, C + hw, C - hw, C + hw
    GOLD = "childrenofmu:inlaid_gold"
    WALL_UP = {"east": "none", "west": "none", "north": "none", "south": "none", "up": "true", "waterlogged": "false"}
    # ---- The shell.
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            corner = x in (x0, x1) and z in (z0, z1)
            B.stone(x, 0, z) if edge else B.put(x, 0, z, "minecraft:polished_andesite")
            for y in range(1, H + 1):
                if edge:
                    if corner:
                        B.put(x, y, z, "minecraft:stone_bricks" if (y // 2) % 2 else "minecraft:polished_andesite")
                    elif y in LEVELS[1:]:
                        B.put(x, y, z, "minecraft:polished_andesite")           # a string course at every floor
                    else:
                        B.stone(x, y, z)
                else:
                    B.air(x, y, z)
            if edge and (x + z) % 2 == 0:
                B.put(x, H + 1, z, "minecraft:stone_brick_wall", dict(WALL_UP))
    for k in range(1, floors + 1):
        for x in range(x0 + 1, x1):
            for z in range(z0 + 1, z1):
                B.put(x, k * FH if k < floors else H, z, "minecraft:spruce_planks" if k < floors else "minecraft:dark_oak_planks")
    # The ground floor's floor: polished andesite with a border of stone bricks.
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            if x in (x0 + 1, x1 - 1) or z in (z0 + 1, z1 - 1):
                B.put(x, 0, z, "minecraft:stone_bricks")
    # ---- Windows (with stone sills and hoods) between buttresses. Iron bars below, glass above, stained at the top.
    for k in range(floors):
        yb = k * FH
        for t in range(-hw + 2, hw - 1, 3):
            win = t % 6 != 2
            for (x, z, out) in ((C + t, z0, (0, -1)), (C + t, z1, (0, 1)), (x0, C + t, (-1, 0)), (x1, C + t, (1, 0))):
                f = {(0, -1): "north", (0, 1): "south", (-1, 0): "west", (1, 0): "east"}[out]
                if win:
                    if k == 0 and z == z0 and abs(t) <= 2:
                        continue                                 # the great door goes here
                    glass = "minecraft:iron_bars" if k == 0 else "minecraft:glass_pane" if k == 1 else \
                        ("minecraft:purple_stained_glass_pane" if t % 4 else "minecraft:yellow_stained_glass_pane")
                    for y in (yb + 2, yb + 3, yb + 4, yb + 5, yb + 6) if k == 2 else (yb + 2, yb + 3):
                        B.put(x, y, z, glass, {})
                    B.stair(x + out[0], yb + (7 if k == 2 else 4), z + out[1], "minecraft:stone_brick_stairs", OPP[f], half="top")
                    B.put(x + out[0], yb + 1, z + out[1], "minecraft:stone_brick_slab", {"type": "top", "waterlogged": "false"})
                else:
                    for y in range(yb + 1, (H if k == 2 else yb + FH)):
                        if y < H:
                            B.put(x + out[0], y, z + out[1], "minecraft:stone_brick_wall", dict(WALL_UP))
    # ---- Corner turrets: round, rising above the hall, with tall spires, and a little room at every floor (the
    # hall's corner opens into it). The north-east turret has a ladder up to the roof.
    for i, (tx, tz) in enumerate(((x0, z0), (x1, z0), (x0, z1), (x1, z1))):
        th = H + 5 + rng.randint(0, 3)
        for y in range(0, th + 1):
            for x in range(tx - 2, tx + 3):
                for z in range(tz - 2, tz + 3):
                    d = math.hypot(x - tx, z - tz)
                    if d > 2.4 or (x0 < x < x1 and z0 < z < z1 and y <= H):
                        continue
                    if d > 1.4:
                        B.stone(x, y, z)
                    elif y in LEVELS:
                        B.put(x, y, z, "minecraft:spruce_planks" if y else "minecraft:stone_bricks")   # a floor
                    elif y > 0:
                        B.air(x, y, z)
            if y % 4 == 2:
                for (wx, wz) in ((tx - 2, tz), (tx + 2, tz), (tx, tz - 2), (tx, tz + 2)):
                    if not (x0 <= wx <= x1 and z0 <= wz <= z1):
                        B.pane(wx, y, wz)
        B.spire(tx - 2, tx + 2, tz - 2, tz + 2, th + 1, roof="tile", rise=3, finial="flag")
        if i == 1:
            lx, lz = tx + 1, tz                          # against the turret's outer wall
            for y in range(1, H + 2):
                B.put(lx, y, lz, "minecraft:ladder", {"facing": "west", "waterlogged": "false"})
        for k in range(floors):
            B.potted(tx + (1 if tx == x0 else -1) * 0, k * FH + 1, tz + (-1 if tz == z0 else 1)) if i != 1 else None
    # ---- The tower out of the roof: two crooked storeys. The lower, of stone, is the Post Office (its door opens onto
    # the roof); the upper, of brick, overhanging it, is where the Gar's accountants sleep, reached by an outside stone
    # stair up the tower's side. A tiled spire caps it.
    y = H + 1
    cx, cz, r = C, C + 1, 3
    po_sh = 8
    for yy in range(y, y + po_sh):
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                if x in (cx - r, cx + r) or z in (cz - r, cz + r):
                    B.put(x, yy, z, rng.choice(STONES[:3]))
                else:
                    B.air(x, yy, z)
        if yy % 3 == 1:
            B.pane(cx, yy, cz - r)
            B.pane(cx - r, yy, cz)
            B.pane(cx, yy, cz + r)
    # Its ceiling (the dormitory's floor), with a cornice of stairs round the top.
    yc = y + po_sh
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            if x in (cx - r - 1, cx + r + 1) or z in (cz - r - 1, cz + r + 1):
                face = ("east" if x < cx else "west") if abs(x - cx) > abs(z - cz) else ("south" if z < cz else "north")
                B.stair(x, yc, z, "minecraft:stone_brick_stairs", face, half="top")
            else:
                B.put(x, yc, z, "minecraft:spruce_planks")
    # The Post Office: a door from the roof on the east side, a counter, sorting pigeonholes, sacks of letters.
    B.door(cx + r, y, cz, "west", kind="spruce_door")
    for zz in range(cz - 2, cz + 3):
        if zz != cz:
            B.stair(cx + 1, y, zz, "minecraft:spruce_stairs", "east", half="top")     # the counter
    B.put(cx + 1, y + 1, cz - 2, "minecraft:white_carpet")                              # letters on it
    B.lamp(cx + 1, y + 1, cz + 2, "west")
    for zz in range(cz - 2, cz + 3):
        B.put(cx - 2, y, zz, "minecraft:chiseled_bookshelf", {"facing": "east", "slot_0_occupied": "false",
              "slot_1_occupied": "false", "slot_2_occupied": "false", "slot_3_occupied": "false",
              "slot_4_occupied": "false", "slot_5_occupied": "false"})
        B.put(cx - 2, y + 1, zz, "minecraft:chiseled_bookshelf", {"facing": "east", "slot_0_occupied": "true",
              "slot_1_occupied": "false", "slot_2_occupied": "true", "slot_3_occupied": "true",
              "slot_4_occupied": "false", "slot_5_occupied": "true"})
    B.stocked_barrel(cx - 1, y, cz + 2, [("minecraft:paper", 24), ("minecraft:paper", 16)])
    B.stocked_barrel(cx - 1, y, cz - 2, [("minecraft:paper", 20), ("minecraft:writable_book", 1)])
    B.put(cx, y, cz - 2, "minecraft:cartography_table")
    B.lectern(cx, y, cz + 2, "north", ["RCB POST OFFICE.", "Letters: one fish. Parcels: two fish.",
                                       "Nothing to the Frogs. They have their own post.",
                                       "Upstairs is private. Accountants only."])
    B.put(cx, y + po_sh - 1, cz, "minecraft:bell", {"attachment": "ceiling", "facing": "north", "powered": "false"})
    B.hanging_lantern(cx - 1, y + po_sh - 1, cz - 1)
    B.hanging_lantern(cx + 1, y + po_sh - 1, cz + 1)
    B.folk(cx, y, cz, "rat", "postrat", stay=3)
    # The accountants' dormitory above, of brick, stepped east over the cornice.
    dx0 = cx + 1
    dy = yc + 1
    d_sh = 7
    for yy in range(dy, dy + d_sh):
        for x in range(dx0 - r, dx0 + r + 1):
            for z in range(cz - r, cz + r + 1):
                if x in (dx0 - r, dx0 + r) or z in (cz - r, cz + r):
                    B.put(x, yy, z, "minecraft:bricks")
                else:
                    B.air(x, yy, z)
        if yy % 3 == 1:
            B.pane(dx0, yy, cz - r)
            B.pane(dx0 + r, yy, cz)
            B.pane(dx0, yy, cz + r)
    for x in range(dx0 - r, dx0 + r + 1):
        for z in range(cz - r, cz + r + 1):
            B.put(x, dy + d_sh, z, "minecraft:spruce_planks")
    B.spire(dx0 - r, dx0 + r, cz - r, cz + r, dy + d_sh + 1, roof="tile", rise=3, finial="flag")
    # Green bunks (the accountants' own: nobody else sleeps in green), a lamp, a rug.
    B.bunk(dx0 - 1, dy, cz - 2, "east", "south", colors=("green", "green"))
    B.bunk(dx0 - 1, dy, cz + 2, "east", "north", colors=("green", "green"))
    for xx in range(dx0 - 1, dx0 + 3):
        B.put(xx, dy, cz, "minecraft:green_carpet")
    B.lamp(dx0 + 2, dy, cz, "west")
    B.hanging_lantern(dx0 + 1, dy + d_sh - 1, cz)
    # Its door, on the west, and the outside stone stair up to it: from the roof at the north, climbing south along
    # the Post Office's west wall to a landing at the door.
    door_z = cz
    B.door(dx0 - r, dy, door_z, "east", kind="spruce_door")
    sx = cx - r - 2                               # clear of the cornice
    top_z = door_z - 1
    rise = yc - (H + 1)                           # stairs from the roof up to the landing (at the dormitory's floor)
    for k in range(rise):
        zz = top_z - (rise - 1 - k)
        sy = H + 1 + k
        for yy in range(H + 1, sy):
            B.put(sx, yy, zz, "minecraft:stone_bricks")
        B.stair(sx, sy, zz, "minecraft:stone_brick_stairs", "south")
        if k % 2 == 0:
            B.put(sx - 1, sy + 1, zz, "minecraft:stone_brick_wall", dict(WALL_UP))       # a balustrade
        for yy in range(sy + 1, sy + 4):
            if not B.is_free(sx, yy, zz):
                B.air(sx, yy, zz)
    for zz in (door_z, door_z + 1):
        for yy in range(H + 1, yc + 1):
            B.put(sx, yy, zz, "minecraft:stone_bricks")                                 # the landing
        B.put(sx - 1, yc + 1, zz, "minecraft:stone_brick_wall", dict(WALL_UP))
    B.put(sx + 1, yc, door_z, "minecraft:stone_bricks")                                 # across to the cornice
    B.put(sx, yc + 2, door_z + 1, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # Lamps round the roof, so nothing spawns up there in the dark.
    for (lx, lz) in ((x0 + 3, z0 + 3), (x1 - 3, z0 + 3), (x0 + 3, z1 - 3), (x1 - 3, z1 - 3), (C + 4, z0 + 3),
                     (C + 6, C + 2), (C - 6, C + 5)):
        if B.is_free(lx, H + 1, lz):
            B.lantern_post(lx, H + 1, lz, 1)
    # ---- The Gar's turret: round, corbelled out of the east wall where the stairs come up to his floor, with a spiral
    # stair inside that climbs out onto the roof (and so to the Post Office and the dormitory stair).
    tx, tz = x1 + 2, C - 2
    tbase = 2 * FH
    ttop = H + 5
    for yy in range(tbase - 3, ttop + 1):
        for x in range(tx - 2, tx + 3):
            for z in range(tz - 2, tz + 3):
                d = math.hypot(x - tx, z - tz)
                if d > 2.4 or x < x1:
                    continue
                if yy < tbase:
                    if d <= [0.1, 1.0, 1.5][yy - (tbase - 3)]:
                        B.put(x, yy, z, "minecraft:stone_bricks")                      # the corbelling
                    continue
                if d > 1.5:
                    B.stone(x, yy, z)
                elif yy == tbase:
                    B.put(x, yy, z, "minecraft:stone_bricks")
                else:
                    B.air(x, yy, z)
        if yy % 4 == 2 and yy > tbase:
            for (wx, wz) in ((tx + 2, tz), (tx, tz - 2), (tx, tz + 2)):
                B.pane(wx, yy, wz)
    B.spire(tx - 2, tx + 2, tz - 2, tz + 2, ttop + 1, roof="tile", rise=3, finial="flag")
    for yy in range(tbase + 1, ttop + 1):
        B.log(tx, yy, tz, kind="stripped_dark_oak_log")                                # the newel
    ring = [(-1, 1), (-1, 0), (-1, -1), (0, -1), (1, -1), (1, 0), (1, 1), (0, 1)]       # clockwise from SW
    t0 = tbase + 1
    steps_n = H - t0
    cell = None
    for i in range(1, steps_n + 1):
        px, pz = ring[(i - 1) % 8]
        cx2, cz2 = ring[i % 8]
        face = {(0, -1): "north", (0, 1): "south", (1, 0): "east", (-1, 0): "west"}[(cx2 - px, cz2 - pz)]
        sy = t0 + i - 1
        B.stair(tx + cx2, sy, tz + cz2, "minecraft:spruce_stairs", face)
        if sy - 1 > tbase:
            B.put(tx + cx2, sy - 1, tz + cz2, "minecraft:spruce_planks")                # the underside
        cell = ring[(i + 1) % 8]
    lx, lz = tx + cell[0], tz + cell[1]
    B.put(lx, H, lz, "minecraft:spruce_planks")                                         # the landing at the top
    for yy in (H + 1, H + 2):
        B.air(x1, yy, lz)                                                               # out onto the roof
        B.air(x1 - 1, yy, lz)
    B.put(x1, H, lz, "minecraft:stone_bricks")
    for yy in (t0, t0 + 1):
        B.air(x1, yy, tz + 1)                                                           # in from the Gar's floor
    B.hanging_lantern(tx + 1, ttop, tz + 1)
    B.hanging_lantern(tx - 1, ttop, tz - 1)

    # ---- The great door: a three-wide arched opening, steps up to it, braziers and banners either side.
    for x in (C - 1, C, C + 1):
        for yy in (1, 2):
            B.air(x, yy, z0)
    B.air(C, 3, z0)
    B.stair(C - 1, 3, z0, "minecraft:stone_brick_stairs", "east", half="top")
    B.stair(C + 1, 3, z0, "minecraft:stone_brick_stairs", "west", half="top")
    B.put(C, 4, z0, "minecraft:chiseled_stone_bricks")
    for x in (C - 2, C + 2):
        for yy in (1, 2, 3, 4):
            B.put(x, yy, z0, "minecraft:polished_andesite")
    for x in range(C - 2, C + 3):
        B.stair(x, 0, z0 - 1, "minecraft:stone_brick_stairs", "south")
    B.street_sign(C + 4, z0, 3, ["HALL OF", "JUSTICE", "Writs sold", "top floor"], wood="dark_oak")
    for x in (C - 3, C + 3):
        B.stone(x, 1, z0 - 1)
        B.put(x, 2, z0 - 1, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
        B.put(x, 4, z0 - 1, "minecraft:red_wall_banner", {"facing": "north"})

    # ---- Staircases: open flights along the walls, two wide, each step with a timber stringer under it, the floor
    # above cut away over them and a rail round the opening.
    def flight(xs, z_start, base, toward, rail_side):
        dz = 1 if toward == "south" else -1
        for k in range(FH):
            sy = base + 1 + k
            zz = z_start + dz * k
            for sx in xs:
                B.stair(sx, sy, zz, "minecraft:spruce_stairs", toward)
                if k >= 1:
                    B.stair(sx, sy - 1, zz, "minecraft:spruce_stairs", OPP[toward], half="top")   # the underside
                for yy in range(sy + 1, sy + 4):
                    if yy == base + FH or B.is_free(sx, yy, zz) or (sx, yy, zz) not in B.blocks:
                        B.air(sx, yy, zz)
        for sx in xs:
            B.put(sx, base + FH, z_start + dz * FH, "minecraft:spruce_planks")       # the landing

    flight([x0 + 1, x0 + 2], z0 + 3, 0, "south", x0 + 3)          # ground floor -> barracks, up the west wall
    flight([x1 - 1, x1 - 2], z1 - 3, FH, "north", x1 - 3)         # barracks -> throne room, up the east wall

    # ======================= Ground floor: the Watch.
    g = 1
    # The cell, in the back west corner, well clear of the stairs: solid stone walls, a barred window into the hall, an
    # iron door in the stone (opened by the lever outside it), a cot, a bucket, and somebody inside.
    cx0, cx1, cz0, cz1 = x0 + 1, x0 + 4, z1 - 4, z1 - 1     # inside
    for yy in range(1, 4):
        for xx in range(cx0, cx1 + 2):
            B.put(xx, yy, cz0 - 1, "minecraft:stone_bricks")          # the front wall
        for zz in range(cz0 - 1, cz1 + 1):
            B.put(cx1 + 1, yy, zz, "minecraft:stone_bricks")          # the side wall
        for xx in range(cx0, cx1 + 1):
            for zz in range(cz0, cz1 + 1):
                if yy < 3:
                    B.air(xx, yy, zz)
    for xx in range(cx0, cx1 + 1):
        for zz in range(cz0, cz1 + 1):
            B.put(xx, 3, zz, "minecraft:stone_bricks")                # a low stone ceiling
    for xx in (cx0 + 1, cx0 + 2):
        B.put(xx, 2, cz0 - 1, "minecraft:iron_bars", {})              # the barred window
    dz = cz0 + 2
    for yy in (1, 2):
        B.put(cx1 + 1, yy, dz, "minecraft:iron_door", {"facing": "east", "half": "lower" if yy == 1 else "upper",
                                                      "hinge": "right", "open": "false", "powered": "false"})
    B.put(cx1 + 2, 2, dz - 1, "minecraft:lever", {"face": "wall", "facing": "east", "powered": "false"})
    B.put(cx0, 1, cz0, "minecraft:white_bed", {"facing": "south", "part": "foot", "occupied": "false"})
    B.put(cx0, 1, cz0 + 1, "minecraft:white_bed", {"facing": "south", "part": "head", "occupied": "false"})
    B.put(cx0, 1, cz1, "minecraft:cauldron")
    B.put(cx0 + 2, 1, cz1, "minecraft:brown_carpet")
    B.folk(cx0 + 2, 1, cz0 + 2, "rat", "scavenger", stay=1)
    # The aisle, and a rug at the front desk.
    for zz in range(z0 + 1, z1 - 4):
        B.put(C, g, zz, "minecraft:red_carpet")
    # The front desk (sergeant's), across the hall on the right: three desks in a row under lamps.
    for i, zz in enumerate((z0 + 3, z0 + 6, z0 + 9)):
        dx = x1 - 4
        B.stair(dx, g, zz, "minecraft:dark_oak_stairs", "west", half="top")
        B.stair(dx, g, zz + 1, "minecraft:dark_oak_stairs", "west", half="top")
        B.lamp(dx, g + 1, zz, "west")
        B.put(dx, g + 1, zz + 1, "minecraft:white_carpet")                    # papers
        B.stair(dx + 1, g, zz, "minecraft:spruce_stairs", "east")             # the cop's chair, back to the wall
        B.stocked_barrel(dx + 2, g, zz + 1, [("minecraft:paper", 12), ("minecraft:book", 2)])
        if i < 2:
            B.folk(dx + 1, g, zz + 1, "cat", "cop")
    B.lectern(C + 3, g, z0 + 2, "south", ["CHARGE BOOK.", "Loitering (rat): fined one fish.",
                                           "Loitering (rat): fined one fish. Same rat.",
                                           "Unlicensed trading: see the Great Gar. Nobody trades without a Writ."])
    # The waiting bench along the front wall, left of the door.
    for xx in range(x0 + 4, C - 2):
        B.stair(xx, g, z0 + 1, "minecraft:spruce_stairs", "north")
    # A rack of arms on the front wall.
    for xx, item in ((C + 2, "minecraft:iron_sword"), (C + 3, "minecraft:crossbow"), (C + 4, "minecraft:iron_sword"),
                     (C + 5, "minecraft:shield")):
        B.item_frame(xx, 3, z0 + 1, "south", item)
    # The fireplace in the back wall, with a brick hood, and armchairs (stairs) before it.
    fx = C
    for xx in (fx - 1, fx + 1):
        for yy in (1, 2, 3):
            B.put(xx, yy, z1 - 1, "minecraft:bricks")
    B.put(fx, 1, z1 - 1, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    B.put(fx, 1, z1 - 2, "minecraft:iron_bars", {})                    # a fire guard
    for yy in (3, 4, 5):
        B.put(fx, yy, z1 - 1, "minecraft:bricks")
    for xx in (fx - 2, fx - 1, fx, fx + 1, fx + 2):
        B.put(xx, 3, z1 - 2, "minecraft:brick_slab", {"type": "top", "waterlogged": "false"}) if abs(xx - fx) <= 1 else None
    B.stair(fx - 2, g, z1 - 4, "minecraft:spruce_stairs", "north")
    B.stair(fx + 2, g, z1 - 4, "minecraft:spruce_stairs", "north")
    for xx in range(fx - 1, fx + 2):
        for zz in (z1 - 4, z1 - 3):
            B.put(xx, g, zz, "minecraft:red_carpet")
    # Shelves and lockers on the back wall.
    for xx in list(range(x0 + 6, fx - 2)) + list(range(fx + 3, x1 - 3)):
        B.put(xx, g, z1 - 1, "minecraft:bookshelf" if xx % 2 else "minecraft:barrel", None if xx % 2 else
              {"facing": "north", "open": "false"})
    # Stone pillars holding up the floor, with lanterns.
    for (px, pz) in ((C - 3, C - 2), (C + 3, C - 2), (C - 3, C + 3), (C + 3, C + 3)):
        for yy in range(1, FH):
            B.put(px, yy, pz, "minecraft:polished_andesite" if yy % 2 else "minecraft:stone_bricks")
        B.put(px, 3, pz - 1, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"}) if False else None
    for (lx, lz) in ((C, C - 4), (C, C + 1), (C - 5, C - 1), (C + 5, C + 4)):
        B.hanging_lantern(lx, FH - 1, lz)
    B.folk(C + 1, g, C, "cat", "cop")
    B.folk(C - 2, g, z0 + 2, "rat", "accountant", stay=2)

    # ======================= Middle floor: barracks and armory.
    m = FH + 1
    # Bunks along the front wall, ladders on the room side.
    for bx in (x0 + 4, x0 + 8, x0 + 12):
        B.bunk(bx, m, z0 + 1, "east", "south", colors=("red", "red") if bx % 8 else ("blue", "blue"))
    # Armor on stands along the back wall, weapons hung over them, barrels of arrows between.
    for i, ax in enumerate((C - 6, C - 3, C, C + 3)):
        B.armor_stand(ax, m, z1 - 1, 180.0, kit=["iron", "chain", "iron", "leather"][i],
                      hand=["minecraft:iron_sword", "minecraft:bow", "minecraft:iron_axe", "minecraft:crossbow"][i])
        B.item_frame(ax, m + 2, z1 - 1, "north", ["minecraft:bow", "minecraft:iron_sword", "minecraft:shield",
                                                 "minecraft:crossbow"][i])
        if i < 3:
            B.stocked_barrel(ax + 1, m, z1 - 1, [("minecraft:arrow", 32), ("minecraft:arrow", 32)])
            B.item_frame(ax + 1, m + 2, z1 - 1, "north", "minecraft:arrow")
    # The mess table: a row of tables with benches either side, and a cask.
    for xx in range(C - 4, C + 1):
        B.stair(xx, m, C, "minecraft:spruce_stairs", "north", half="top")
        B.stair(xx, m, C - 1, "minecraft:spruce_stairs", "north")
        B.stair(xx, m, C + 1, "minecraft:spruce_stairs", "south")
    B.put(C - 4, m + 1, C, "minecraft:candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    B.put(C - 1, m + 1, C, "minecraft:candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
    B.barrel(C + 2, m, C, "east")
    # The smithy corner, by the west wall past the stairwell.
    B.put(x0 + 1, m, z1 - 5, "minecraft:blast_furnace", {"facing": "east", "lit": "true"})
    B.put(x0 + 1, m, z1 - 4, "minecraft:anvil", {"facing": "north"})
    B.put(x0 + 1, m, z1 - 3, "minecraft:grindstone", {"face": "floor", "facing": "east"})
    B.put(x0 + 1, m, z1 - 2, "minecraft:smithing_table")
    for (lx, lz) in ((C - 4, C), (C + 3, C - 4), (C - 4, z1 - 3), (C + 2, C + 4)):
        B.hanging_lantern(lx, 2 * FH - 1, lz)
    B.folk(C - 2, m, C - 1, "cat", "guard")
    B.folk(C + 1, m, C + 3, "cat", "guard")
    B.folk(x0 + 2, m, z1 - 3, "cat", "archer")

    # ======================= Top floor: the throne room of the Great Gar.
    t = 2 * FH + 1
    # Carpet: purple and magenta checks, a gold-edged aisle from the stairs to the dais.
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            if B.is_free(x, t, z) and x0 + 3 < x < x1 - 3 and z0 + 1 < z < z1 - 5:
                B.put(x, t, z, "minecraft:purple_carpet" if (x + z) % 2 else "minecraft:magenta_carpet")
    for z in range(z0 + 2, z1 - 5):
        B.put(C, t, z, "minecraft:yellow_carpet")
    # The dais at the back: two steps of polished blackstone, gilded at the edges, gold pillars at its corners.
    for x in range(C - 4, C + 5):
        for z in range(z1 - 5, z1):
            B.put(x, t, z, "minecraft:gilded_blackstone" if x in (C - 4, C + 4) else "minecraft:polished_blackstone_bricks")
            B.air(x, t + 1, z)
    for x in range(C - 4, C + 5):
        B.stair(x, t, z1 - 6, "minecraft:polished_blackstone_brick_stairs", "south")
    for x in range(C - 3, C + 4):
        for z in range(z1 - 4, z1):
            B.put(x, t + 1, z, "minecraft:polished_blackstone_bricks" if z >= z1 - 4 else "minecraft:air")
        B.stair(x, t + 1, z1 - 5, "minecraft:polished_blackstone_brick_stairs", "south")
    # The great yellow cushion, and the Gar upon it.
    for x in range(C - 1, C + 2):
        for z in (z1 - 3, z1 - 2):
            B.put(x, t + 2, z, "minecraft:yellow_wool")
    for x in (C - 1, C + 1):
        B.put(x, t + 3, z1 - 2, "minecraft:yellow_carpet")
    B.folk(C, t + 3, z1 - 3, "cat", "gar")
    # Gold pillars with lanterns, purple wool hangings and banners behind, braziers.
    for x in (C - 4, C + 4):
        for yy in range(t + 1, H):
            B.put(x, yy, z1 - 1, GOLD if yy % 2 else "minecraft:gilded_blackstone")
        B.put(x, t + 1, z1 - 5, GOLD)
        B.put(x, t + 2, z1 - 5, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    for x in range(C - 3, C + 4):
        for yy in range(t + 2, H):
            B.put(x, yy, z1 - 1, "minecraft:purple_wool" if (x + yy) % 3 else "minecraft:yellow_wool")
    for x in (C - 2, C, C + 2):
        B.put(x, H - 1, z1 - 2, "minecraft:purple_wall_banner", {"facing": "north"})
    for x in (C - 3, C + 3):
        B.put(x, t + 2, z1 - 2, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
        B.put(x, t + 2, z1 - 3, "minecraft:candle", {"candles": "4", "lit": "true", "waterlogged": "false"})
    # The clerks: rats in green eyeshades at desks under banker's lamps, along the west side, ledgers behind them.
    for zz in (z0 + 3, z0 + 6, z0 + 9):
        dx = x0 + 3
        B.stair(dx, t, zz, "minecraft:dark_oak_stairs", "west", half="top")
        B.stair(dx, t, zz + 1, "minecraft:dark_oak_stairs", "west", half="top")
        B.lamp(dx, t + 1, zz, "east")
        B.put(dx, t + 1, zz + 1, "minecraft:white_carpet")
        B.stair(dx - 1, t, zz, "minecraft:spruce_stairs", "west")         # the clerk's stool
        B.put(x0 + 1, t, zz, "minecraft:bookshelf")
        B.put(x0 + 1, t, zz + 1, "minecraft:bookshelf")
        B.put(x0 + 1, t + 1, zz, "minecraft:bookshelf")
        B.folk(dx - 1, t, zz, "rat", "accountant", stay=1)
    # Along the east side: the Gar's treasures, in pots and on gold plinths, between the windows.
    for zz in (z0 + 10, z0 + 12):
        B.put(x1 - 4, t, zz, GOLD)
        B.put(x1 - 4, t + 1, zz, "minecraft:decorated_pot", {"facing": "west", "cracked": "false", "waterlogged": "false"})
    B.put(x1 - 4, t, z0 + 2, "minecraft:potted_azure_bluet")
    # Chandeliers.
    for (lx, lz) in ((C, C - 4), (C, C + 1), (C - 4, C - 2), (C + 4, C - 2)):
        B.chandelier(lx, H - 1, lz, drop=3)
    B.folk(C - 3, t + 1, z1 - 6, "cat", "guard")
    B.folk(C + 3, t + 1, z1 - 6, "cat", "guard")
    B.folk(C + 2, t, C - 2, "rat", "runner", stay=4)                   # the Gar's errand rat
    B.finish()
    return B, hw + 2


def wall_row(name):
    """
    A block of joined houses that is also a stretch of the town wall: fronts to the street inside, and at the back,
    facing out of town, the ground floor and the one above are solid stone with only slits and a high window or two,
    buttressed, so the row keeps out what the wall keeps out.
    """
    specs = {
        "a": (961, [(6, 4, "gable_z", "tile", "shop"), (5, 3, "gable_z", "slate", "stone"), (7, 5, "gable_x", "tile", "shop"),
                    (5, 4, "spire", "copper", "stone"), (6, 3, "gable_z", "mud", "shop")]),
        "b": (962, [(7, 3, "gable_x", "slate", "stone"), (5, 5, "spire", "tile", "shop"), (6, 4, "mansard", "slate", "shop"),
                    (5, 3, "gable_z", "tile", "brick"), (6, 4, "gable_z", "shingle", "shop")]),
        "c": (963, [(5, 4, "gable_z", "tile", "shop"), (6, 3, "gable_z", "tile", "stone"), (5, 5, "gable_z", "copper", "shop"),
                    (7, 4, "gable_x", "slate", "stone"), (5, 3, "spire", "tile", "shop"), (5, 4, "gable_z", "mud", "brick")]),
        "d": (964, [(6, 5, "spire", "slate", "stone"), (6, 3, "gable_x", "tile", "shop"), (5, 4, "gable_z", "tile", "shop"),
                    (6, 3, "mansard", "copper", "stone")]),
    }
    seed, lots = specs[name]
    B = RCBBuilder(31, seed=seed)
    depth = 7
    z_front = B.c - depth // 2
    zb = z_front + depth - 1
    folk = [ROW_FOLK[(i + seed) % len(ROW_FOLK)] for i in range(len(lots))]
    xa, xb = row(B, z_front, depth, lots, folk)
    # The back: solid stone for two storeys, arrow slits, buttresses every few blocks.
    for x in range(xa, xb + 1):
        for y in range(1, 8):
            b = B.blocks.get((x, y, zb))
            if b and "log" not in b[0]:
                B.stone(x, y, zb)
        if (x - xa) % 5 == 2 and x not in (xa, xb):
            B.put(x, 3, zb, "minecraft:iron_bars", {})            # a slit
        if (x - xa) % 5 == 0:
            for y in range(0, 6):
                B.stone(x, y, zb + 1)
            B.stair(x, 6, zb + 1, "minecraft:stone_brick_stairs", "north")
    for x in range(xa, xb + 1):
        B.stone(x, 0, zb + 1) if (x, 0, zb + 1) not in B.blocks else None
    B.finish()
    return B, B.c - z_front + 1, xb - xa + 1


def rat_works():
    """
    The Rat works: a yard behind a ramshackle fence where the Rats mend, sort and sell what the city throws away. A
    workshop shed with a forge, heaps of junk and scrap, a crane made of fence posts with a barrel on a chain, and the
    Rats who run it.
    """
    B = RCBBuilder(19, seed=971)
    C = B.c
    rng = B.rng
    x0, x1, z0, z1 = C - 7, C + 7, C - 6, C + 6
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            B.put(x, 0, z, rng.choice(["minecraft:coarse_dirt", "minecraft:gravel", "minecraft:packed_mud",
                                       "minecraft:coarse_dirt", "minecraft:mud_bricks"]))
            if x in (x0, x1) or z in (z0, z1):
                if not (z == z0 and abs(x - C) <= 1):
                    B.fence(x, 1, z, rng.choice(["minecraft:spruce_fence", "minecraft:oak_fence", "minecraft:dark_oak_fence"]))
                    if rng.random() < 0.5:
                        B.fence(x, 2, z, "minecraft:spruce_fence")
    for x in (C - 2, C + 2):
        for y in range(1, 4):
            B.fence(x, y, z0, "minecraft:dark_oak_fence")
    for x in range(C - 2, C + 3):
        B.log(x, 4, z0, axis="x", kind="dark_oak_log")
    B.sign(C, 3, z0, ["RAT WORKS", "Junk bought", "& sold"], facing="north", wall=False)
    B.put(C - 1, 1, z0, "minecraft:spruce_fence_gate", {"facing": "south", "in_wall": "false", "open": "true", "powered": "false"})
    B.put(C + 1, 1, z0, "minecraft:spruce_fence_gate", {"facing": "south", "in_wall": "false", "open": "true", "powered": "false"})
    # The shed, at the back.
    sx0, sx1, sz0, sz1 = x0 + 1, x0 + 7, z1 - 5, z1 - 1
    for x in range(sx0, sx1 + 1):
        for z in range(sz0, sz1 + 1):
            for y in range(1, 4):
                edge_x, edge_z = x in (sx0, sx1), z in (sz0, sz1)
                if edge_x and edge_z:
                    B.log(x, y, z, kind="spruce_log")
                elif edge_x or (edge_z and z == sz1):
                    B.put(x, y, z, rng.choice(["minecraft:spruce_planks", "minecraft:oak_planks", "minecraft:spruce_planks",
                                               "minecraft:cobblestone"]))
                elif edge_z:
                    B.air(x, y, z) if y < 3 else B.log(x, y, z, axis="x", kind="spruce_log")    # open front
                else:
                    B.air(x, y, z)
    for x in range(sx0 - 1, sx1 + 2):
        for z in range(sz0 - 1, sz1 + 2):
            B.put(x, 4, z, "minecraft:spruce_slab", {"type": "bottom", "waterlogged": "false"})
    B.put(sx0 + 1, 1, sz1 - 1, "minecraft:blast_furnace", {"facing": "south", "lit": "true"})
    B.put(sx0 + 2, 1, sz1 - 1, "minecraft:anvil", {"facing": "east"})
    B.put(sx0 + 3, 1, sz1 - 1, "minecraft:smithing_table")
    B.put(sx0 + 4, 1, sz1 - 1, "minecraft:crafting_table")
    B.put(sx0 + 5, 1, sz1 - 1, "minecraft:grindstone", {"face": "floor", "facing": "north"})
    B.hanging_lantern(sx0 + 3, 3, sz0 + 2)
    # Junk heaps.
    for (hx, hz) in ((x1 - 2, z1 - 2), (x1 - 3, C - 1), (C + 1, z1 - 2)):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if rng.random() < 0.7:
                    B.put(hx + dx, 1, hz + dz, rng.choice(["minecraft:gravel", "minecraft:coarse_dirt", "minecraft:barrel",
                                                           "minecraft:composter", "minecraft:cobweb", "minecraft:chain",
                                                           "minecraft:bone_block", "minecraft:hay_block"]),
                          {"axis": "y"} if False else None)
        B.put(hx, 2, hz, rng.choice(["minecraft:barrel", "minecraft:cauldron", "minecraft:gravel"]))
    for (p, b) in list(B.blocks.items()):
        if b[0] == "minecraft:barrel":
            B.blocks[p] = ("minecraft:barrel", {"facing": rng.choice(["up", "north", "east"]), "open": "false"})
        elif b[0] == "minecraft:composter":
            B.blocks[p] = ("minecraft:composter", {"level": "4"})
        elif b[0] == "minecraft:chain":
            B.blocks[p] = ("minecraft:chain", {"axis": "x", "waterlogged": "false"})
        elif b[0] in ("minecraft:bone_block", "minecraft:hay_block"):
            B.blocks[p] = (b[0], {"axis": "y"})
    # The crane: a fence-post mast and jib, a barrel hanging on a chain.
    mx, mz = x0 + 2, z0 + 2
    for y in range(1, 7):
        B.fence(mx, y, mz, "minecraft:dark_oak_fence")
    for x in range(mx, mx + 5):
        B.fence(x, 7, mz, "minecraft:dark_oak_fence")
    for y in (6, 5, 4):
        B.put(mx + 4, y, mz, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    B.put(mx + 4, 3, mz, "minecraft:barrel", {"facing": "up", "open": "false"})
    B.lantern_post(C + 3, 1, z0 + 1, 2)
    B.folk(sx0 + 3, 1, sz0 + 1, "rat", "engineer")
    B.folk(C, 1, C, "rat", "scavenger")
    B.folk(x1 - 3, 1, C + 1, "rat", "gong")
    B.folk(C - 2, 1, z0 + 2, "rat", "runner")
    B.finish()
    return B, 7


def watch_house():
    """The Watch House: where the Cat cops loaf between beats. Iron bars on the windows, a desk, a notice board."""
    B = RCBBuilder(13, seed=951)
    C = B.c
    tip, bounds = B.townhouse(C - 3, C + 3, C - 3, C + 3, 2, roof_kind="mansard", roof="slate", ground="stone",
                              jetty=False, chimney=True, damage=False, bay=False,
                              folk=[("cat", "cop", 0), ("cat", "cop", 1), ("cat", "guard", 0)])
    x0, x1, z0, z1 = bounds[0]
    for (p, b) in list(B.blocks.items()):
        if b[0] == "minecraft:glass_pane":
            B.blocks[p] = ("minecraft:iron_bars", {})
    C = B.c
    B.lectern(x1 - 1, 1, C, "west", ["WATCH HOUSE. Fines payable in emeralds or fish.",
                                       "Today's arrests: none. We don't do arrests.",
                                       "Note to self: buy more fish."])
    B.lantern_post(x0 - 1, 1, z0 - 1, 2)
    B.lantern_post(x1 + 1, 1, z0 - 1, 2)
    B.street_sign(x1 - 1, z0, 3, ["THE WATCH", "Fines paid", "here"], wood="dark_oak")
    B.finish()
    return B, 4


def fish_stall(seed):
    """A market stall under a striped awning: fish on the lines, barrels, a Cat selling (probably stolen) goods."""
    B = RCBBuilder(9, seed=seed)
    C = B.c
    x0, x1, z0, z1 = C - 3, C + 3, C - 2, C + 2
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            B.put(x, 0, z, "minecraft:spruce_planks")
    for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        B.log(x, 0, z, kind="dark_oak_log")
        for y in range(1, 4):
            B.fence(x, y, z, "minecraft:dark_oak_fence")
    color = B.rng.choice(["red", "blue", "green", "yellow"])
    for x in range(x0, x1 + 1):
        for z in range(z0 - 1, z1 + 1):
            B.put(x, 4, z, f"minecraft:{color}_wool" if (x - x0) % 2 == 0 else "minecraft:white_wool")
    for x in range(x0 + 1, x1):
        B.stair(x, 1, z0, "minecraft:spruce_stairs", "south", half="top")   # the counter
        B.put(x, 3, z0 + 1, "childrenofmu:strung_fish")
    B.barrel(x0 + 1, 1, z1 - 1)
    B.barrel(x1 - 1, 1, z1 - 1)
    B.put(x1 - 1, 2, z1 - 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    B.sign(C, 3, z0 - 1, ["FRESH", "FISH", "Not stolen"], facing="north", wall=False)
    B.folk(C, 1, C, "cat", B.rng.choice(["rogue", "fellmonger"]))
    B.finish()
    return B, 3


def sewer():
    """
    The way down to the Rats' city beneath the city: a little drain house over a shaft, and below, a vaulted brick
    tunnel with a channel of water, nests of junk and the Rats who live there. Ground level is y = 9 (sink 9).
    """
    B = RCBBuilder(15, seed=961)
    C = B.c
    G = 9
    # The tunnel: 13 long (x), 5 wide, vaulted.
    x0, x1, z0, z1 = C - 6, C + 6, C - 2, C + 2
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            for y in range(0, 6):
                edge = x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1) or y in (0, 5)
                vault = y == 4 and z in (z0, z1)
                if edge or vault:
                    B.put(x, y, z, "minecraft:bricks" if B.rng.random() < 0.7 else "minecraft:mossy_stone_bricks")
                else:
                    B.air(x, y, z)
            if z == C:
                B.put(x, 0, z, "minecraft:water", {"level": "0"}) if x0 <= x <= x1 else None
            elif z0 <= z <= z1 and x0 <= x <= x1:
                B.put(x, 0, z, "minecraft:stone_bricks" if B.rng.random() < 0.7 else "minecraft:cobblestone")
    for x in range(x0 + 1, x1, 3):
        B.hanging_lantern(x, 4, C)
    # Junk nests and barrels.
    for (x, z) in ((x0, z0), (x0 + 1, z0), (x1, z1), (x1 - 1, z1), (x0, z1)):
        B.put(x, 1, z, B.rng.choice(["minecraft:hay_block", "minecraft:white_carpet", "minecraft:brown_carpet"])) \
            if B.rng.random() < 0.7 else B.barrel(x, 1, z)
    B.put(x1, 1, z0, "minecraft:composter", {"level": "4"})
    B.put(x1 - 2, 1, z0, "minecraft:cauldron")
    # The shaft up to the drain house.
    sx, sz = C, z0 - 1
    for y in range(1, G + 1):
        B.put(sx, y, sz - 1, "minecraft:bricks")
        B.put(sx - 1, y, sz, "minecraft:bricks")
        B.put(sx + 1, y, sz, "minecraft:bricks")
        B.air(sx, y, sz)
        B.put(sx, y, sz, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    for y in range(6, G):
        B.put(sx, y, sz + 1, "minecraft:bricks")
    B.put(sx, 5, sz, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    # The drain house on top: 5 x 5, a pyramid roof.
    hx0, hx1, hz0, hz1 = sx - 2, sx + 2, sz - 2, sz + 2
    for x in range(hx0, hx1 + 1):
        for z in range(hz0, hz1 + 1):
            if (x, z) != (sx, sz):
                B.stone(x, G - 1, z)
            for y in range(G, G + 3):
                edge = x in (hx0, hx1) or z in (hz0, hz1)
                if edge:
                    B.put(x, y, z, "minecraft:bricks") if not (x in (hx0, hx1) and z in (hz0, hz1)) else \
                        B.log(x, y, z, kind="dark_oak_log")
                elif (x, z) != (sx, sz) or y > G + 1:
                    B.air(x, y, z)
    B.put(sx, G - 1, sz, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    B.put(sx, G, sz, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    B.roof_spire(hx0, hx1, hz0, hz1, G + 3, roof="slate", rise=2, finial="lantern")
    B.door(sx, G, hz0, "south", kind="spruce_door")
    B.street_sign(sx + 1, hz0, G + 1, ["SEWER", "Rats only", "Mind the", "step"])
    B.hanging_lantern(sx + 1, G + 2, sz + 1)
    # Rats.
    B.folk(x0 + 2, 1, z0, "rat", "gong")
    B.folk(x0 + 4, 1, z1, "rat", "gong")
    B.folk(x1 - 1, 1, z1 - 1, "rat", "beautiful")
    B.folk(x1 - 3, 1, z0, "rat", "scavenger")
    B.folk(x0 + 6, 1, z1, "rat", "engineer")
    B.finish()
    return B, C - hz0 + 1


# Hand edits made in a real world, carried back into the templates (see tools/overrides/*.json: template position,
# block, properties). Signs keep their words; other mods' blocks get plain stand-ins for the vanilla twin.
SIGN_TEXT = {"rcb_justice_hall": {(11, 34, 16): ["Lucky", "Quarters"]}}
MOD_ALTS = {"quark:stripped_spruce_post": ("minecraft:stripped_spruce_log", ("axis",)),
            "quark:spruce_chest": ("minecraft:chest", ("facing", "type")),
            "supplementaries:candle_holder_green": ("minecraft:green_candle", ())}


def apply_overrides(B, name):
    path = os.path.join(os.path.dirname(__file__), "overrides", name + ".json")
    if not os.path.exists(path):
        return 0
    from nbt_structure import Byte
    edits = json.load(open(path))
    for (x, y, z), block, props in edits:
        props = dict(props)
        if block.endswith("hanging_sign"):
            lines = (SIGN_TEXT.get(name, {}).get((x, y, z), []) + ["", "", "", ""])[:4]
            text = {"messages": ['{"text":"%s"}' % l for l in lines], "color": "black", "has_glowing_text": Byte(0)}
            B.blocks[(x, y, z)] = (block, props, {"id": "minecraft:hanging_sign", "front_text": text,
                                                  "back_text": dict(text), "is_waxed": Byte(1)})
            B.alt.pop((x, y, z), None)
        elif block in MOD_ALTS:
            alt, keep = MOD_ALTS[block]
            alt_props = {k: props[k] for k in keep if k in props}
            if alt == "minecraft:green_candle":
                alt_props = {"candles": "1", "lit": "true", "waterlogged": "false"}
            B.mod(x, y, z, block, props, alt, alt_props)
        elif block in ("air", "minecraft:air", "minecraft:cave_air"):
            B.air(x, y, z)
        else:
            B.put(x, y, z, block, props)
    return len(edits)


def main():
    os.makedirs(DIR, exist_ok=True)
    out = {}
    for v in "abcde":
        out[f"rcb_house_{v}"] = house(v)
    for v in "abc":
        out[f"rcb_row_{v}"] = terrace(v)
    out["rcb_skyway_a"] = skyway("a")
    out["rcb_skyway_b"] = skyway("b")
    out["rcb_bridge_houses"] = bridge_houses()
    out["rcb_bazz_deck"] = bazz_deck()
    out["rcb_bat_tower"] = bat_tower(971)
    out["rcb_bat_tower_b"] = bat_tower(972)
    out["rcb_bazz_club"] = bazz_club()
    out["rcb_fight_pit"] = fight_pit()
    out["rcb_justice_hall"] = justice_hall()
    out["rcb_guard_tower"] = guard_tower(991)
    out["rcb_guard_tower_b"] = guard_tower(992)
    out["rcb_rat_works"] = rat_works()
    lengths = {}
    for v in "abcd":
        B, door, length = wall_row(v)
        out[f"rcb_wall_{v}"] = (B, door)
        lengths[f"rcb_wall_{v}"] = length
    out["rcb_watch_house"] = watch_house()
    out["rcb_fish_stall"] = fish_stall(981)
    out["rcb_sewer"] = sewer()
    for name, (B, door) in out.items():
        apply_overrides(B, name)
        n, p, top, mods = B.save_pair(os.path.join(DIR, name))
        print(f"{name}: size {B.size}, height {top}, {n} blocks, {mods} mod blocks, {len(B.entities)} folk, door {door}"
              + (f", length {lengths[name]}" if name in lengths else ""))


if __name__ == "__main__":
    main()
