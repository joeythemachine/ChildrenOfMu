"""
Shared pieces for the Frog folk's swamp buildings, in the style of the "Frog Hut 1" reference sheet: wooden rooms on
stilts over the water, oak walls framed in spruce logs, mossy hip roofs with leaves, porches with dark oak railings,
ladders down to little docks, lanterns and vines everywhere.

Template y = 0 is the water's surface (the first air above the water). The game extends every log and fence in that
bottom layer down through the water to the swamp floor, so buildings stand on real stilts wherever they generate.
The front of every building is toward -z; its dock's outer edge is DOOR blocks from the center (the town's
boardwalks run there).
"""
import math
import random

from ithk_build import AIR, Builder

LOG = ("minecraft:spruce_log", {"axis": "y"})
WALL = "minecraft:oak_planks"
FLOOR = "minecraft:spruce_planks"
FENCE = "minecraft:dark_oak_fence"
PANE = "minecraft:glass_pane"
# The Frog folk's own blocks (always there: they're part of this mod).
REED = "childrenofmu:reed_thatch"
REED_STAIRS = "childrenofmu:reed_thatch_stairs"
REED_SLAB = "childrenofmu:reed_thatch_slab"
SCREEN = "childrenofmu:reed_screen"
LILY = "childrenofmu:lily_lantern"
IDOL = "childrenofmu:frog_idol"
NET = "childrenofmu:fishing_net"
CATTAILS = "childrenofmu:cattails"
LEAVES = [("minecraft:oak_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"}),
          ("minecraft:azalea_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"}),
          ("minecraft:flowering_azalea_leaves", {"persistent": "true", "distance": "1", "waterlogged": "false"})]

DIRS = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
OPP = {"north": "south", "south": "north", "west": "east", "east": "west"}


class FrogBuilder(Builder):
    def __init__(self, size, seed):
        super().__init__(size, seed)
        self.stairs = set()
        # Blocks from other mods, with the vanilla (or our own) block to use instead when those mods aren't
        # installed: pos -> (name, props, property names to copy over from the mod block).
        self.alt = {}

    def put(self, x, y, z, name, props=None, replace=True, keep_shape=False):
        if replace or (x, y, z) not in self.blocks:
            self.alt.pop((x, y, z), None)
        super().put(x, y, z, name, props, replace, keep_shape)

    def mod(self, x, y, z, name, props, alt, alt_props=None, inherit=()):
        """A block from another mod (Quark, Farmer's Delight, Supplementaries...), with its stand-in."""
        if not (0 <= x < self.size and 0 <= z < self.size and y >= 0):
            return
        self.put(x, y, z, name, props)
        self.alt[(x, y, z)] = (alt, alt_props or {}, tuple(inherit))

    def save_pair(self, path):
        """
        Writes <path>.nbt with the mod blocks and, if there are any, <path>_vanilla.nbt with their stand-ins.
        The game picks between them (worldgen/ModdedTemplates.java).
        """
        n, p, top = self.save(path + ".nbt")
        if self.alt:
            rich = dict(self.blocks)
            for pos, (name, props, inherit) in self.alt.items():
                if pos not in self.blocks or self.blocks[pos][0].startswith("minecraft:"):
                    continue    # gone, or since replaced by a plain block
                props = dict(props)
                for k in inherit:
                    if k in self.blocks[pos][1]:
                        props[k] = self.blocks[pos][1][k]
                self.blocks[pos] = (name, props)
            self.save(path + "_vanilla.nbt")
            self.blocks = rich
        return n, p, top, len(self.alt)
    # ------------------------------------------------------------ basics
    def log(self, x, y, z, axis="y", kind="spruce_log"):
        self.put(x, y, z, f"minecraft:{kind}", {"axis": axis})

    def stair(self, x, y, z, name, facing, half="bottom", alt=None):
        props = {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"}
        if alt:
            self.mod(x, y, z, name, props, alt, {"waterlogged": "false"}, ("facing", "half", "shape"))
        else:
            self.put(x, y, z, name, props)
        self.stairs.add((x, y, z))

    def thatch_stair(self, x, y, z, facing, half="bottom"):
        """Mostly our reed thatch, with patches of Quark's straw thatch."""
        if self.rng.random() < 0.3:
            self.stair(x, y, z, "quark:thatch_stairs", facing, half, alt=REED_STAIRS)
        else:
            self.stair(x, y, z, REED_STAIRS, facing, half)

    def lily(self, x, y, z, hanging=False):
        self.put(x, y, z, LILY, {"hanging": "true" if hanging else "false", "waterlogged": "false"})

    def net(self, x, y, z, facing):
        """A fishing net in the air block at (x, y, z), hung on the wall behind it (facing away from the wall)."""
        if self.is_free(x, y, z):
            self.put(x, y, z, NET, {"facing": facing})

    def idol(self, x, y, z, facing):
        self.put(x, y, z, IDOL, {"facing": facing})

    def cattails(self, x, y, z):
        if self.is_free(x, y, z):
            self.put(x, y, z, CATTAILS)

    def fence(self, x, y, z, name=FENCE):
        self.put(x, y, z, name, {})

    def pane(self, x, y, z):
        self.put(x, y, z, PANE, {})

    def leaves(self, x, y, z):
        self.put(x, y, z, *self.rng.choice(LEAVES))

    def vine(self, x, y, z, side):
        """A vine at (x, y, z) clinging to the block on its `side`."""
        if (x, y, z) in self.blocks:
            return
        props = {d: "false" for d in ("north", "south", "east", "west", "up")}
        props[side] = "true"
        self.put(x, y, z, "minecraft:vine", props)

    def hang_vines(self, x, y, z, side, length):
        for i in range(length):
            if (x, y - i, z) in self.blocks or y - i < 1:
                break
            self.vine(x, y - i, z, side)

    def is_free(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b is None or b[0] == AIR

    def lantern_below(self, x, y, z, rope=0):
        """A lily lantern hanging from the block above (x, y, z), on `rope` blocks of Quark rope (chain without)."""
        for i in range(rope):
            if self.is_free(x, y - i, z):
                self.mod(x, y - i, z, "quark:rope", {}, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        if self.is_free(x, y - rope, z):
            self.lily(x, y - rope, z, hanging=True)

    def lantern_on(self, x, y, z):
        self.lily(x, y, z)

    def door(self, x, y, z, facing, kind="spruce_door"):
        for half, dy in (("lower", 0), ("upper", 1)):
            self.put(x, y + dy, z, f"minecraft:{kind}", {"facing": facing, "half": half, "hinge": "left",
                                                          "open": "false", "powered": "false"})

    def ladder(self, x, z, y0, y1, facing):
        for y in range(y0, y1 + 1):
            self.put(x, y, z, "minecraft:ladder", {"facing": facing, "waterlogged": "false"})

    def barrel(self, x, y, z, facing="up"):
        self.put(x, y, z, "minecraft:barrel", {"facing": facing, "open": "false"})

    def potted(self, x, y, z, plant=None):
        plant = plant or self.rng.choice(["fern", "azure_bluet", "red_mushroom", "blue_orchid", "oak_sapling",
                                          "brown_mushroom", "lily_of_the_valley"])
        self.put(x, y, z, f"minecraft:potted_{plant}")

    def frog(self, x, y, z, variant):
        from nbt_structure import Byte
        self.entities.append((x + 0.5, y + 0.05, z + 0.5,
                              {"id": "childrenofmu:frog_folk", "PersistenceRequired": Byte(1), "Variant": variant}))

    # ------------------------------------------------------------ the hut
    def hut(self, hw, hd, floor_y, wall_h, porch=3, roof_extra=0, windows=True, door=True, screens=None):
        """
        A room (2*hw+1 by 2*hd+1, walls included) on stilts, with a porch in front, a ladder down to a dock, and a
        mossy hip roof. Returns a dict of handy coordinates.
        """
        C = self.c
        x0, x1, z0, z1 = C - hw, C + hw, C - hd, C + hd
        pz0 = z0 - porch                                  # front edge of the porch
        top = floor_y + wall_h
        # Stilts under the room's corners and the middle of each side, and the porch's front corners.
        posts = {(x0, z0), (x1, z0), (x0, z1), (x1, z1), (C, z1), (x0, C), (x1, C), (x0, pz0), (x1, pz0)}
        for x, z in posts:
            for y in range(0, floor_y):
                self.log(x, y, z)
        # Floor and porch.
        for x in range(x0, x1 + 1):
            for z in range(pz0, z1 + 1):
                self.put(x, floor_y, z, FLOOR)
        for x in range(x0 - 1, x1 + 2):                   # spruce beams under the floor's edges
            for z in (z1 + 0, pz0):
                self.log(x, floor_y - 1, z, axis="x") if x0 <= x <= x1 else None
        # Walls.
        for y in range(floor_y + 1, top + 1):
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    edge_x, edge_z = x in (x0, x1), z in (z0, z1)
                    if edge_x and edge_z:
                        self.log(x, y, z)
                    elif edge_x or edge_z:
                        self.put(x, y, z, WALL)
                    else:
                        self.air(x, y, z)
        if screens is None:
            screens = self.rng.random() < 0.5            # woven reed screens instead of glass in some huts
        if windows:
            wy = floor_y + 2
            for z in range(C - 1, C + 2) if hd >= 3 else [C]:
                for x in (x0, x1):
                    for y in (wy, wy + 1):
                        self.put(x, y, z, SCREEN) if screens else self.pane(x, y, z)
            for x in range(C - 1, C + 2) if hw >= 3 else [C]:
                for y in (wy, wy + 1):
                    self.put(x, y, z1, SCREEN) if screens else self.pane(x, y, z1)
        if door:
            self.door(C, floor_y + 1, z0, "south")
            self.lantern_on_wall = (C, floor_y + 3, z0 - 1)
            self.mod(C, floor_y + 1, z0 - 1, "supplementaries:doormat", {"facing": "north"}, "minecraft:moss_carpet")
        # Porch railing of woven reed screens between fence posts, open at the front middle for the pilings.
        for x in range(x0, x1 + 1):
            if x != C:
                self.fence(x, floor_y + 1, pz0) if x in (x0, x1) else self.put(x, floor_y + 1, pz0, SCREEN)
        for z in range(pz0 + 1, z0):
            self.put(x0, floor_y + 1, z, SCREEN)
            self.put(x1, floor_y + 1, z, SCREEN)
        for x in (x0, x1):
            self.lantern_on(x, floor_y + 2, pz0)
        # A little dock at the water, and hop-up pilings from it to the porch (frogs jump; nobody needs a ladder).
        for y in range(0, floor_y):
            self.log(C, y, pz0)
        for x in range(C - 1, C + 2):
            for z in range(pz0 - 3, pz0):
                if (x, 0, z) not in self.blocks:
                    self.put(x, 0, z, FLOOR)
        self.log(C - 1, 0, pz0 - 3)
        self.log(C + 1, 0, pz0 - 3)
        self.fence(C - 1, 1, pz0 - 3)
        self.lantern_on(C - 1, 2, pz0 - 3)
        self.cattails(C + 1, 1, pz0 - 3)
        # A net drying on one side wall, and a frog idol keeping watch from the porch.
        self.net(x1 + 1, floor_y + 2, C + self.rng.choice((-1, 1)), "east")
        self.idol(x0 + 1 if self.rng.random() < 0.5 else x1 - 1, floor_y + 1, pz0 + 1, "north")
        if not self.pilings(C, pz0, floor_y, (x0, x1, pz0, z1), walkway=[(C, pz0 - 3), (C, pz0 - 2)],
                            dock=[(x, z) for x in range(C - 1, C + 2) for z in range(pz0 - 3, pz0)]):
            self.ladder(C, pz0 - 1, 1, floor_y, "north")
        # Fish and bugs hung to dry under the porch eaves.
        for x in range(x0 + 1, x1):
            if x != C and self.rng.random() < 0.7:
                self.put(x, top, pz0, self.rng.choice(["childrenofmu:strung_fish", "childrenofmu:strung_bugs"]))
        # Roof.
        self.hip_roof(x0 - 1, x1 + 1, pz0 - 0, z1 + 1, top + 1, extra=roof_extra)
        return {"x0": x0, "x1": x1, "z0": z0, "z1": z1, "pz0": pz0, "floor": floor_y, "top": top,
                "door_dist": C - (pz0 - 3)}

    def pilings(self, tx, pz0, floor_y, keep_out, walkway, dock):
        """
        Stepped pilings, each one block higher than the last, from the dock up to the gap in the porch railing at
        (tx, pz0): log posts standing in the water, capped with moss, winding round however the space allows. Frogs
        hop up them; so can a player. Returns False if no route fits (the caller falls back to a ladder).
        """
        x0, x1, kz0, kz1 = keep_out
        steps = floor_y - 1                       # posts topped at heights 1 .. floor_y - 1
        reserved = set(walkway)

        def free(x, z, h):
            if not (1 <= x < self.size - 1 and 1 <= z < self.size - 1) or (x, z) in reserved:
                return False
            if x0 <= x <= x1 and kz0 <= z <= kz1:
                return False
            return all((x, y, z) not in self.blocks for y in range(1, h + 4))

        def search(path, h):
            x, z = path[-1]
            if h == 1:
                ok = any(abs(x - wx) + abs(z - wz) == 1 and (wx, wz) not in path for wx, wz in dock)
                return path if ok else None
            options = [(x + dx, z + dz) for dx, dz in DIRS.values()]
            self.rng.shuffle(options)
            for nx, nz in options:
                if (nx, nz) not in path and free(nx, nz, h - 1):
                    found = search(path + [(nx, nz)], h - 1)
                    if found:
                        return found
            return None

        start = (tx, pz0 - 1)
        route = search([start], steps) if steps >= 1 and free(tx, pz0 - 1, steps) else None
        if not route:
            return False
        for i, (x, z) in enumerate(route):
            h = steps - i
            kind = "mangrove_log" if self.rng.random() < 0.3 else "spruce_log"
            for y in range(0, h + 1):
                self.log(x, y, z, kind=kind)
            self.put(x, h + 1, z, "minecraft:moss_carpet")
        return True

    # ------------------------------------------------------------ round buildings
    def facing_out(self, x, z):
        """For a roof stair at (x, z) on a ring round the centre: face (its high side) toward the middle."""
        dx, dz = x - self.c, z - self.c
        if abs(dx) >= abs(dz):
            return "west" if dx > 0 else "east"
        return "north" if dz > 0 else "south"

    def cone_roof(self, r, y, material="thatch", finial=True, steep=1):
        """
        A round, pointed roof: rings of stairs, each ring one smaller, rising `steep` blocks per ring, filled
        underneath so it reads as a ceiling inside. material: "thatch" (reed and Quark thatch), "copper" (cut copper
        stairs), "shingle" (dark oak). Returns the y of the tip.
        """
        C = self.c
        ring = 0
        rr = r
        while rr >= 0.6:
            for x in range(C - int(rr) - 1, C + int(rr) + 2):
                for z in range(C - int(rr) - 1, C + int(rr) + 2):
                    d = math.hypot(x - C, z - C)
                    if d > rr + 0.5:
                        continue
                    for k in range(steep):
                        yy = y + ring * steep + k
                        if d > rr - 0.5 and k == steep - 1:
                            f = self.facing_out(x, z)
                            if material == "copper":
                                self.stair(x, yy, z, "minecraft:waxed_cut_copper_stairs", f)
                            elif material == "shingle":
                                self.stair(x, yy, z, "minecraft:dark_oak_stairs", f)
                            else:
                                self.thatch_stair(x, yy, z, f)
                                if self.rng.random() < 0.12 and ring > 0:
                                    self.leaves(x, yy, z)
                        elif d <= rr - 0.5 or k < steep - 1:
                            self.put(x, yy, z, {"copper": "minecraft:waxed_cut_copper", "shingle": "minecraft:dark_oak_planks"}
                                     .get(material, REED))
            rr -= 1.0
            ring += 1
        tip = y + ring * steep
        if finial:
            self.put(C, tip, C, REED if material == "thatch" else "minecraft:waxed_cut_copper")
            self.put(C, tip + 1, C, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"}) \
                if material == "copper" else self.fence(C, tip + 1, C, "minecraft:mangrove_fence")
            if material != "copper":
                self.lily(C, tip + 2, C)
        return tip

    def round_hut(self, r, floor_y, wall_h, porch=3, roof="thatch", screens=None, steep=1):
        """
        A round hut on stilts: a ring wall of planks ribbed with logs, round windows on three sides, a door to the
        north onto a square porch, pilings up from a little dock, and a cone roof. Returns handy coordinates like hut().
        """
        C = self.c
        top = floor_y + wall_h
        if screens is None:
            screens = self.rng.random() < 0.5

        def dist(x, z):
            return math.hypot(x - C, z - C)

        cells = [(x, z) for x in range(C - r - 1, C + r + 2) for z in range(C - r - 1, C + r + 2) if dist(x, z) <= r + 0.5]
        for x, z in cells:
            self.put(x, floor_y, z, FLOOR)
            wall = dist(x, z) > r - 0.5
            for y in range(floor_y + 1, top + 1):
                if wall:
                    ang = math.atan2(z - C, x - C)
                    rib = abs(((ang / (math.pi / 4)) + 0.5) % 1 - 0.5) < 0.12
                    self.log(x, y, z) if rib else self.put(x, y, z, WALL)
                else:
                    self.air(x, y, z)
        # Stilts under the ring and the middle.
        for k in range(8):
            a = k * math.pi / 4
            x, z = round(C + math.cos(a) * r), round(C + math.sin(a) * r)
            for y in range(0, floor_y):
                self.log(x, y, z)
        for y in range(0, floor_y):
            self.log(C, y, C)
        # Windows east, west and south.
        for x, z in ((C + r, C), (C - r, C), (C, C + r)):
            for y in (floor_y + 2, floor_y + 3):
                self.put(x, y, z, SCREEN) if screens else self.pane(x, y, z)
        # The door, and a square porch in front of it.
        z0 = C - r
        self.door(C, floor_y + 1, z0, "south")
        self.mod(C, floor_y + 1, z0 - 1, "supplementaries:doormat", {"facing": "north"}, "minecraft:moss_carpet")
        pz0 = z0 - porch
        px0, px1 = C - 2, C + 2
        for x in range(px0, px1 + 1):
            for z in range(pz0, z0):
                self.put(x, floor_y, z, FLOOR)
        for x, z in ((px0, pz0), (px1, pz0)):
            for y in range(0, floor_y):
                self.log(x, y, z)
        for x in range(px0, px1 + 1):
            if x != C:
                self.fence(x, floor_y + 1, pz0) if x in (px0, px1) else self.put(x, floor_y + 1, pz0, SCREEN)
        for z in range(pz0 + 1, z0):
            for x in (px0, px1):
                if dist(x, z) > r + 0.5:
                    self.put(x, floor_y + 1, z, SCREEN)
        for x in (px0, px1):
            self.lantern_on(x, floor_y + 2, pz0)
        # Dock and pilings.
        for y in range(0, floor_y):
            self.log(C, y, pz0)
        for x in range(C - 1, C + 2):
            for z in range(pz0 - 3, pz0):
                if (x, 0, z) not in self.blocks:
                    self.put(x, 0, z, FLOOR)
        self.log(C - 1, 0, pz0 - 3)
        self.log(C + 1, 0, pz0 - 3)
        self.fence(C - 1, 1, pz0 - 3)
        self.lantern_on(C - 1, 2, pz0 - 3)
        self.cattails(C + 1, 1, pz0 - 3)
        if not self.pilings(C, pz0, floor_y, (C - r - 1, C + r + 1, pz0, C + r + 1),
                            walkway=[(C, pz0 - 3), (C, pz0 - 2)],
                            dock=[(x, z) for x in range(C - 1, C + 2) for z in range(pz0 - 3, pz0)]):
            self.ladder(C, pz0 - 1, 1, floor_y, "north")
        # Roof (overhanging the wall by one), with fish and bugs drying under its edge.
        tip = None
        if roof == "mushroom":
            tip = self.mushroom_cap(r + 2, top + 1)
        else:
            tip = self.cone_roof(r + 1, top + 1, roof, steep=steep)
        for k in range(10):
            a = self.rng.random() * 2 * math.pi
            x, z = round(C + math.cos(a) * (r + 1)), round(C + math.sin(a) * (r + 1))
            if self.is_free(x, top, z) and z > pz0:
                self.put(x, top, z, self.rng.choice(["childrenofmu:strung_fish", "childrenofmu:strung_bugs"]))
        self.net(C + r + 1, floor_y + 2, C + 1, "east")
        return {"x0": C - r, "x1": C + r, "z0": z0, "z1": C + r, "pz0": pz0, "floor": floor_y, "top": top,
                "tip": tip, "door_dist": C - (pz0 - 3)}

    def mushroom_cap(self, r, y):
        """A giant red mushroom cap for a roof: a dome of red mushroom block, white spotted, gills underneath."""
        C = self.c
        h = max(3, r // 2 + 1)
        for x in range(C - r, C + r + 1):
            for z in range(C - r, C + r + 1):
                d = math.hypot(x - C, z - C)
                if d > r + 0.4:
                    continue
                cap_y = y + int(round(h * math.sqrt(max(0.0, 1 - (d / (r + 0.4)) ** 2))))
                self.put(x, cap_y, z, "minecraft:red_mushroom_block",
                         {"up": "true", "down": "false", "north": "true", "south": "true", "east": "true", "west": "true"})
                for yy in range(y, cap_y):
                    if d > r - 0.6 or yy == y:
                        self.put(x, yy, z, "minecraft:red_mushroom_block",
                                 {"up": "true", "down": "true", "north": "true", "south": "true", "east": "true", "west": "true"})
        return y + h

    def hip_roof(self, x0, x1, z0, z1, y, extra=0):
        """A mossy hip roof stepping in one block per layer, stairs on its edges, leaves and moss on top."""
        rng = self.rng
        layer = 0
        while x1 - x0 >= 0 and z1 - z0 >= 0:
            yy = y + layer
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    on_x, on_z = x in (x0, x1), z in (z0, z1)
                    if (on_x or on_z) and (x1 - x0 >= 2 and z1 - z0 >= 2):
                        if on_x and on_z:
                            if rng.random() < 0.4:
                                self.leaves(x, yy, z)
                            else:
                                self.thatch_stair(x, yy, z, "south" if z == z0 else "north")
                            continue
                        facing = ("east" if x == x0 else "west") if on_x else ("south" if z == z0 else "north")
                        if rng.random() < 0.15:
                            self.stair(x, yy, z, "minecraft:mossy_cobblestone_stairs", facing)
                        else:
                            self.thatch_stair(x, yy, z, facing)
                        if rng.random() < 0.18 and layer > 0:
                            self.leaves(x, yy, z)
                    elif x1 - x0 < 2 or z1 - z0 < 2:
                        roll = rng.random()
                        if roll < 0.35:
                            self.put(x, yy, z, "minecraft:moss_block")
                        elif roll < 0.7:
                            self.leaves(x, yy, z)
                        else:
                            self.put(x, yy, z, REED)
                    else:
                        self.put(x, yy, z, REED)      # thatch, seen as the ceiling from inside
            x0, x1, z0, z1 = x0 + 1, x1 - 1, z0 + 1, z1 - 1
            layer += 1
        for _ in range(extra):
            pass
        # Vines hanging from the eaves.
        return y + layer

    def chimney(self, x, z, y0, height):
        for y in range(y0, y0 + height):
            self.put(x, y, z, "minecraft:mossy_cobblestone")
        self.put(x, y0 + height, z, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false",
                                                             "waterlogged": "false"})
        # A lantern on a little spruce bracket off the chimney's side.
        self.fence(x + 1, y0 + height - 1, z)
        self.fence(x + 2, y0 + height - 1, z)
        self.lantern_below(x + 2, y0 + height - 2, z)

    def overgrow(self, y_min, y_max, chance=0.18):
        """Vines on the outsides of walls and stilts."""
        rng = self.rng
        solid = [(p, b) for p, b in self.blocks.items() if y_min <= p[1] <= y_max and
                 any(k in b[0] for k in ("log", "planks", "cobblestone"))]
        for (x, y, z), b in solid:
            if rng.random() > chance:
                continue
            side = rng.choice(list(DIRS))
            dx, dz = DIRS[side]
            if (x + dx, y, z + dz) not in self.blocks:
                self.hang_vines(x + dx, y, z + dz, OPP[side], rng.randint(1, 4))

    # ------------------------------------------------------------ finishing
    def finish(self):
        """Works out how fences and panes connect, and stair corners, as the game would when placing them."""
        def solid(p):
            b = self.blocks.get(p)
            return b is not None and b[0] != AIR and not any(k in b[0] for k in
                    ("lantern", "vine", "ladder", "door", "leaves", "potted", "carpet", "torch", "flower", "candle",
                     "net", "cattail", "idol", "rope", "doormat", "pot", "board", "jar", "rug", "mat", "shelf",
                     "sack", "stool", "skillet", "bell", "chain", "screen", "fence", "glass_pane", "iron_bars"))
        updates = {}
        for p, b in self.blocks.items():
            if b[0].endswith("_fence") or b[0].endswith("glass_pane") or b[0] in ("minecraft:iron_bars", SCREEN):
                props = dict(b[1])
                for d, (dx, dz) in DIRS.items():
                    q = (p[0] + dx, p[1], p[2] + dz)
                    nb = self.blocks.get(q)
                    props[d] = "true" if nb and (nb[0] == b[0] or solid(q)) else "false"
                props["waterlogged"] = "false"
                updates[p] = (b[0], props)
        self.blocks.update(updates)
        self._fix_stair_shapes(list(self.stairs))
