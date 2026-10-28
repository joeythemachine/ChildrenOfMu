"""
The toolkit for RCB City's buildings (see make_rcb_city.py): crooked timber-framed town houses that can be built
alone or joined side by side into rows, with a range of finished roofs (trimmed steep gables facing the street or
running along it, dormers, mansards, flared spires), bay windows, shop fronts with awnings and hanging signs,
damage (patched walls, holes boarded up and propped with timber), enclosed chimney tops and plenty of lights.

Coordinates: the street (front) is toward -z. Template y = 0 is the ground floor's floor, laid in the ground.
"""
import math

from frog_build import FrogBuilder
from nbt_structure import Byte

FLOOR = "minecraft:spruce_planks"
PLASTERS = ["minecraft:white_terracotta", "minecraft:light_gray_terracotta", "minecraft:terracotta",
            "minecraft:mud_bricks", "minecraft:packed_mud", "minecraft:smooth_sandstone", "minecraft:calcite",
            "minecraft:yellow_terracotta", "minecraft:green_terracotta"]
BRICKS = ["minecraft:bricks", "minecraft:stone_bricks", "minecraft:mud_bricks", "minecraft:deepslate_bricks",
          "minecraft:tuff_bricks"]
STONES = ["minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks",
          "minecraft:cobblestone", "minecraft:mossy_cobblestone"]
# Roofs: (stairs, full block, slab). Mostly red tile, some verdigris copper, slate, dark shingle.
ROOFS = {
    "tile": ("minecraft:brick_stairs", "minecraft:bricks", "minecraft:brick_slab"),
    "copper": ("minecraft:waxed_weathered_cut_copper_stairs", "minecraft:waxed_weathered_cut_copper",
               "minecraft:waxed_weathered_cut_copper_slab"),
    "slate": ("minecraft:deepslate_tile_stairs", "minecraft:deepslate_tiles", "minecraft:deepslate_tile_slab"),
    "shingle": ("minecraft:dark_oak_stairs", "minecraft:dark_oak_planks", "minecraft:dark_oak_slab"),
    "mud": ("minecraft:mud_brick_stairs", "minecraft:mud_bricks", "minecraft:mud_brick_slab"),
}
TRIM = ("minecraft:dark_oak_stairs", "minecraft:dark_oak_planks", "minecraft:dark_oak_slab")

# Domum Ornamentum (the MineColonies decoration mod): smooth shingle roofs and real timber framing. Each of its blocks
# carries the materials it's made of; every building also gets a plain twin without them (see frog_build.save_pair).
DOMUM = True
DO_BE = "domum_ornamentum:materially_retexturable"
SHINGLE_MAT = {"tile": "minecraft:bricks", "copper": "minecraft:waxed_weathered_cut_copper",
               "slate": "minecraft:deepslate_tiles", "shingle": "minecraft:dark_oak_planks",
               "mud": "minecraft:mud_bricks"}
SUPPORT = "minecraft:dark_oak_planks"
FRAME_WOOD = "minecraft:dark_oak_planks"
DO_ROOFING = {"domum_ornamentum:shingle", "domum_ornamentum:shingle_steep", "domum_ornamentum:shingle_steep_lower",
              "domum_ornamentum:shingle_slab"}
OPP = {"north": "south", "south": "north", "west": "east", "east": "west"}
AIR_NAME = "minecraft:air"
# What the shops of RCB City sell, as their hanging signs put it (about ten letters a line).
SHOPS = [["FRESH", "MICE"], ["CHEESE", "& MICE"], ["PAWN", "Ask no", "questions"], ["KNIVES", "Sharpened"],
         ["CANDLES", "& WICKS"], ["HATS", "Ears cut", "to fit"], ["TAILOR", "Tails let", "out"],
         ["APOTHECARY", "Cures for", "fleas"], ["MAPS", "& CHARTS"], ["SECOND", "HAND", "Very"],
         ["FISHMONGER", "Cod Daily"], ["CALF", "Milk Bar"], ["BAKERY", "Rat pies"], ["CLOCKS", "Mended"],
         ["ROPE", "& TWINE"], ["LAMPS", "& OIL"], ["BOOKS", "Hardly", "chewed"], ["CATNIP", "Licensed"],
         ["GLOVES", "Five", "fingers"], ["BRASS", "& Copper"], ["TEA", "ROOM"], ["COBBLER", "Boots", "& Paws"]]
DIR4 = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
PLAIN_WALLS = set(PLASTERS) | {"minecraft:bricks", "minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
                               "minecraft:cracked_stone_bricks", "minecraft:cobblestone", "minecraft:mossy_cobblestone",
                               "minecraft:deepslate_bricks", "minecraft:tuff_bricks", "minecraft:dark_oak_planks",
                               "minecraft:spruce_planks"}
MOSSY = {"minecraft:stone_bricks": "minecraft:mossy_stone_bricks", "minecraft:cobblestone": "minecraft:mossy_cobblestone"}


class RCBBuilder(FrogBuilder):
    # ------------------------------------------------------------ Domum Ornamentum blocks
    def _do(self, x, y, z, name, props, textures, alt, alt_props=None, inherit=()):
        if not (0 <= x < self.size and 0 <= z < self.size and y >= 0):
            return
        self.blocks[(x, y, z)] = ("domum_ornamentum:" + name, props, {"id": DO_BE, "textureData": dict(textures)})
        self.alt[(x, y, z)] = (alt, alt_props or {}, tuple(inherit))

    def shingle(self, x, y, z, kind, facing, roof, half="bottom"):
        """
        A Domum shingle, laid like a stair (its high side toward `facing`). kind: "normal" (a 45-degree slope),
        "steep" and "steep_lower" (stacked, steep below and steep_lower above, they make a smooth 2:1 slope).
        Stand-in without the mod: the roof's full block (steep) or its stairs.
        """
        stairs, full, slab = ROOFS[roof]
        if not DOMUM:
            self.put(x, y, z, full) if kind == "steep" else self.stair(x, y, z, stairs, facing, half)
            return
        name = {"normal": "shingle", "steep": "shingle_steep", "steep_lower": "shingle_steep_lower"}[kind]
        tex = {"minecraft:block/clay": SHINGLE_MAT[roof], "minecraft:block/oak_planks": SUPPORT}
        if kind == "steep":
            alt, alt_props, inherit = full, {}, ()
        else:
            alt, alt_props, inherit = stairs, {"waterlogged": "false"}, ("facing", "half", "shape")
        self._do(x, y, z, name, {"facing": facing, "half": half, "shape": "straight"}, tex, alt, alt_props, inherit)
        self.stairs.add((x, y, z))

    def ridge_cap(self, x, y, z, along, roof):
        """A Domum ridge cap along x or z (stand-in: a slab of the roof)."""
        stairs, full, slab = ROOFS[roof]
        if not DOMUM:
            self.put(x, y, z, slab, {"type": "bottom", "waterlogged": "false"})
            return
        self._do(x, y, z, "shingle_slab", {"facing": "north" if along == "z" else "east", "shape": "two_way"},
                 {"minecraft:block/oak_planks": SHINGLE_MAT[roof], "minecraft:block/dark_oak_planks": SUPPORT},
                 slab, {"type": "bottom", "waterlogged": "false"})

    def frame(self, x, y, z, kind, facing, center, wood=FRAME_WOOD):
        """
        A Domum timber frame: plaster (center) between dark timbers in a pattern. kind: plain, framed, double_crossed,
        one_crossed_lr, one_crossed_rl, side_framed, side_framed_horizontal, horizontal_plain, up_gated, down_gated.
        Stand-in: the plain plaster.
        """
        if not DOMUM:
            self.put(x, y, z, center)
            return
        self._do(x, y, z, kind, {"facing": facing},
                 {"minecraft:block/oak_planks": wood, "minecraft:block/dark_oak_planks": center}, center)

    # ------------------------------------------------------------ weathering and texture
    def outside(self, x, y, z):
        """Open air outside the building: nothing placed there (rooms inside are filled with explicit air)."""
        return (x, y, z) not in self.blocks and 0 <= x < self.size and 0 <= z < self.size

    def weather(self, vines=0.009, shutters=0.35, moss=0.18):
        """
        Gives the outside of a building texture and age: moss on old stone, vines creeping down from high up,
        shutters beside the windows, wall posts breaking up long flat runs of brick or plaster, and a band of stone
        along the bottom of plain walls.
        """
        rng = self.rng
        faces = []
        for (x, y, z), b in list(self.blocks.items()):
            if y < 1:
                continue
            for d, (dx, dz) in DIR4.items():
                if self.outside(x + dx, y, z + dz):
                    faces.append(((x, y, z), b, d))
        for (x, y, z), b, d in faces:
            name = b[0]
            dx, dz = DIR4[d]
            if name in MOSSY and rng.random() < moss:
                self.put(x, y, z, MOSSY[name])
            elif name in PLAIN_WALLS and name not in MOSSY:
                along = x if d in ("north", "south") else z
                below = self.blocks.get((x, y - 1, z))
                # A post every few blocks up a long plain wall (only where the wall runs on either side).
                l1 = (x - dz, y, z + dx) if d in ("north", "south") else (x, y, z - 1)
                l2 = (x + dz, y, z - dx) if d in ("north", "south") else (x, y, z + 1)
                run = all(self.blocks.get(q, ("",))[0] == name for q in (l1, l2))
                if run and along % 4 == 0 and (name in PLASTERS or name == "minecraft:bricks"):
                    self.log(x, y, z, kind="dark_oak_log")
                elif y == 1 and (below is None or "log" not in below[0]) and name in PLASTERS:
                    self.stone(x, y, z)                                   # a plinth of stone under plaster
            stony = any(k in self.blocks.get((x + (dz if d in ("north", "south") else 0), y,
                                                   z + (dx if d in ("west", "east") else 0)), ("",))[0]
                        for k in ("stone", "cobble", "andesite", "deepslate", "tuff"))
            if name.endswith("glass_pane") and not stony and rng.random() < shutters:
                # Shutters folded back against the wall either side of the window.
                sx, sz = (dz, dx) if d in ("north", "south") else (dz, dx)
                for side in (-1, 1):
                    q = (x + dx + side * (1 if d in ("north", "south") else 0), y,
                         z + dz + side * (0 if d in ("north", "south") else 1))
                    wall = (q[0] - dx, y, q[2] - dz)
                    if self.outside(*q) and self.holds(*wall):
                        self.put(q[0], y, q[2], "minecraft:spruce_trapdoor", {"facing": d, "half": "bottom",
                                                                            "open": "true", "powered": "false",
                                                                            "waterlogged": "false"})
            if y >= 3 and rng.random() < vines and self.holds(x, y, z):
                self.hang_vines(x + dx, y, z + dz, OPP[d], rng.randint(2, 5))

    weathered = True

    def secure_lanterns(self):
        """
        No lantern floats: a hanging one with nothing to hang from gets a chain up to the beam above it (or stands on
        whatever is below it instead); a standing one with nothing under it gets a post.
        """
        for (x, y, z), b in list(self.blocks.items()):
            if b[0] != "minecraft:lantern":
                continue
            hanging = b[1].get("hanging") == "true"
            if hanging:
                above = self.blocks.get((x, y + 1, z), ("",))[0]
                if self.holds(x, y + 1, z) or any(k in above for k in ("chain", "fence", "bars", "slab", "stairs", "planks")):
                    continue
                for k in range(1, 7):
                    if self.holds(x, y + k, z) or "slab" in self.blocks.get((x, y + k, z), ("",))[0]:
                        for j in range(1, k):
                            self.put(x, y + j, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
                        break
                else:
                    if self.holds(x, y - 1, z):
                        self.put(x, y, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
                    else:
                        self.air(x, y, z)
            else:
                below = self.blocks.get((x, y - 1, z), ("",))[0]
                if below == AIR_NAME or (not below and y > 1):
                    self.air(x, y, z)

    def cap_walls(self):
        """
        Where a wall's top is left open to the sky (the floor above leans back off it, or steps in), a dark timber cap:
        a stair sloping up to the wall above if there is one, else a slab.
        """
        wallish = lambda n: (n.startswith("domum_ornamentum:") and "shingle" not in n) or n in PLASTERS \
            or n in ("minecraft:stripped_dark_oak_log", "minecraft:dark_oak_log", "minecraft:bricks")
        caps = {}
        for (x, y, z), b in self.blocks.items():
            if y < 2 or not wallish(b[0]) or (x, y + 1, z) in self.blocks:
                continue
            up = None
            for d, (dx, dz) in DIR4.items():
                nb = self.blocks.get((x + dx, y + 1, z + dz))
                if nb and wallish(nb[0]):
                    up = d
                    break
            # Only the tops of outside walls: open air beside it on some side at its own height.
            if not any(self.outside(x + dx, y, z + dz) for dx, dz in DIR4.values()):
                continue
            caps[(x, y + 1, z)] = up
        for (x, y, z), up in caps.items():
            if up:
                self.stair(x, y, z, "minecraft:dark_oak_stairs", up)
            else:
                self.put(x, y, z, "minecraft:dark_oak_slab", {"type": "bottom", "waterlogged": "false"})

    capped = True

    def finish(self):
        if self.capped:
            self.cap_walls()
        if self.weathered:
            self.weather()
        self.secure_lanterns()
        super().finish()

    # ------------------------------------------------------------ furniture and kit
    def bunk(self, x, y, z, facing, front, wood="spruce", colors=("red", "red")):
        """
        A bunk bed: two beds one above the other between timber posts, a ladder up the front of the foot post. (x, y, z)
        is the lower bed's foot; the head is one step toward `facing`; `front` is the open side of the room.
        """
        dx, dz = DIR4[facing]
        fx, fz = DIR4[front]
        head = (x + dx, z + dz)
        for (px, pz) in ((x - dx, z - dz), (head[0] + dx, head[1] + dz)):
            for yy in range(y, y + 4):
                self.log(px, yy, pz, kind=f"stripped_{wood}_log")
        for (bx, bz), part in (((x, z), "foot"), (head, "head")):
            self.put(bx, y, bz, f"minecraft:{colors[0]}_bed", {"facing": facing, "part": part, "occupied": "false"})
            self.put(bx, y + 1, bz, f"minecraft:{wood}_slab", {"type": "top", "waterlogged": "false"})
            self.put(bx, y + 2, bz, f"minecraft:{colors[1]}_bed", {"facing": facing, "part": part, "occupied": "false"})
            self.air(bx, y + 3, bz)          # open over the top bunk (room to climb in)
        lx, lz = x - dx + fx, z - dz + fz
        for yy in range(y, y + 3):
            self.put(lx, yy, lz, "minecraft:ladder", {"facing": front, "waterlogged": "false"})

    def armor_stand(self, x, y, z, yaw, kit="iron", hand="minecraft:iron_sword"):
        """An armor stand dressed in a set of armor, a weapon in hand (template entity)."""
        pieces = {"iron": "iron", "chain": "chainmail", "leather": "leather", "gold": "golden"}[kit]
        armor = [{"id": f"minecraft:{pieces}_{p}", "count": 1} for p in ("boots", "leggings", "chestplate", "helmet")]
        self.entities.append((x + 0.5, y, z + 0.5, {
            "id": "minecraft:armor_stand", "ArmorItems": armor,
            "HandItems": [{"id": hand, "count": 1} if hand else {}, {}],
            "ShowArms": Byte(1), "Rotation": [float(yaw), 0.0], "PersistenceRequired": Byte(1)}))

    def item_frame(self, x, y, z, facing, item, rotation=0):
        """An item frame in the air block (x, y, z), hung on the wall behind it, facing `facing`, holding `item`."""
        if facing in DIR4:
            dx, dz = DIR4[OPP[facing]]
            if not self.holds(x + dx, y, z + dz) or not self.is_free(x, y, z):
                return          # never on a window, a door or thin air
        f = {"down": 0, "up": 1, "north": 2, "south": 3, "west": 4, "east": 5}[facing]
        self.entities.append((x + 0.5, y + 0.5, z + 0.5, {
            "id": "minecraft:item_frame", "Facing": Byte(f), "TileX": x, "TileY": y, "TileZ": z,
            "Item": {"id": item, "count": 1}, "ItemRotation": Byte(rotation), "Fixed": Byte(1),
            "Invulnerable": Byte(1)}))

    def stocked_barrel(self, x, y, z, items, facing="up"):
        """A barrel with things in it: items = [(id, count)]."""
        self.blocks[(x, y, z)] = ("minecraft:barrel", {"facing": facing, "open": "false"},
                                  {"id": "minecraft:barrel", "Items": [
                                      {"Slot": Byte(i), "id": it, "count": n} for i, (it, n) in enumerate(items)]})

    def lamp(self, x, y, z, facing="north"):
        """A banker's lamp (brass and green glass) on a desk."""
        self.put(x, y, z, "childrenofmu:bankers_lamp", {"facing": facing})

    def chandelier(self, x, top, z, drop=2):
        """Chains down from the ceiling (top: the first air below it) to a cross of dark beams, a lantern under each arm."""
        for k in range(drop):
            self.put(x, top - k, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        y = top - drop
        self.fence(x, y, z, "minecraft:dark_oak_fence")
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            self.fence(x + dx, y, z + dz, "minecraft:dark_oak_fence")
            self.put(x + dx, y - 1, z + dz, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
        self.put(x, y - 1, z, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})

    # ------------------------------------------------------------ small things
    def stone(self, x, y, z):
        self.put(x, y, z, STONES[0] if self.rng.random() < 0.5 else self.rng.choice(STONES[1:]))

    def folk(self, x, y, z, race, variant, stay=None):
        nbt = {"id": f"childrenofmu:{race}_folk", "PersistenceRequired": Byte(1), "Variant": variant}
        if stay is not None:
            nbt["StayRadius"] = stay
        self.entities.append((x + 0.5, y + 0.05, z + 0.5, nbt))

    def hanging_lantern(self, x, y, z):
        if self.is_free(x, y, z):
            self.put(x, y, z, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})

    def lantern_post(self, x, y, z, h=2):
        for i in range(h):
            self.fence(x, y + i, z)
        self.put(x, y + h, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})

    def holds(self, x, y, z):
        """A full, solid block something can hang on (not glass, a door, a stair, a fence...)."""
        b = self.blocks.get((x, y, z))
        if not b:
            return False
        name = b[0]
        if name.startswith("domum_ornamentum:"):
            return not any(k in name for k in ("shingle", "panel", "pillar", "fancy_door", "trapdoor"))
        return not any(k in name for k in (AIR_NAME, "pane", "door", "trapdoor", "stairs", "slab", "fence", "_wall",
                                           "lantern", "carpet", "ladder", "bars", "sign", "torch", "glass",
                                           "leaves", "chain", "candle", "pot", "lectern", "bell", "vine", "campfire",
                                           "barrel", "sack", "composter", "cauldron", "grindstone", "anvil"))

    def wall_torch(self, x, y, z, facing):
        """A torch on the wall behind (x, y, z), only if that wall can hold it (one on glass or a door drops off)."""
        dx, dz = {"north": (0, 1), "south": (0, -1), "west": (1, 0), "east": (-1, 0)}[facing]
        if self.is_free(x, y, z) and self.holds(x + dx, y, z + dz):
            self.put(x, y, z, "minecraft:wall_torch", {"facing": facing})

    def bracket_lantern(self, x, y, z, facing):
        """A lantern hung from an iron bracket on a wall: (x, y, z) is the air just outside the wall."""
        dx, dz = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}[facing]
        if self.is_free(x, y, z) and self.is_free(x, y - 1, z):
            self.put(x, y, z, "minecraft:iron_bars", {})
            self.put(x, y - 1, z, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})

    def sign(self, x, y, z, lines, facing="east", wall=True, wood="spruce", color="black", glow=False):
        """
        A hanging sign with words on it (the same on both faces, up to four short lines: a hanging sign fits about ten
        letters a line). wall: hung by a bracket from the wall beside it (facing is the way its face points, so it sticks
        out from a wall on the axis across it); otherwise hung on chains from the block above.
        """
        lines = (list(lines) + ["", "", "", ""])[:4]
        text = {"messages": ['{"text":"%s"}' % l.replace('"', "'") for l in lines], "color": color,
                "has_glowing_text": Byte(1 if glow else 0)}
        name = f"minecraft:{wood}_wall_hanging_sign" if wall else f"minecraft:{wood}_hanging_sign"
        props = {"facing": facing, "waterlogged": "false"} if wall else \
            {"rotation": {"south": "0", "west": "4", "north": "8", "east": "12"}[facing], "attached": "false",
             "waterlogged": "false"}
        self.blocks[(x, y, z)] = (name, props, {"id": "minecraft:hanging_sign", "front_text": text,
                                                "back_text": dict(text), "is_waxed": Byte(1)})

    def street_sign(self, x, z_wall, y, lines, **kw):
        """A sign sticking straight out into the street from a front wall at z_wall (the street is toward -z)."""
        self.sign(x, y, z_wall - 1, lines, facing="east", **kw)

    def lectern(self, x, y, z, facing, pages):
        """A lectern holding a book and quill with these pages (so it can actually be read)."""
        self.blocks[(x, y, z)] = ("minecraft:lectern", {"facing": facing, "has_book": "true", "powered": "false"},
                                  {"id": "minecraft:lectern", "Page": 0,
                                   "Book": {"id": "minecraft:writable_book", "count": 1, "components": {
                                       "minecraft:writable_book_content": {"pages": [{"raw": t} for t in pages]}}}})

    def spire(self, x0, x1, z0, z1, y, roof="tile", rise=3, finial="rod"):
        return self.roof_spire(x0, x1, z0, z1, y, roof=roof, rise=rise, finial=finial)

    def chimney(self, x, z, y0, top):
        """A brick stack with its fire enclosed at the top by a crown of brick posts, and a pot on one side."""
        for y in range(y0, top):
            self.put(x, y, z, "minecraft:bricks")
        self.put(x, top, z, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false",
                                                   "waterlogged": "false"})
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if self.is_free(x + dx, top, z + dz):
                self.put(x + dx, top, z + dz, "minecraft:brick_wall", {"east": "none", "west": "none", "north": "none",
                                                                       "south": "none", "up": "true",
                                                                       "waterlogged": "false"})
            if self.is_free(x + dx, top - 1, z + dz):
                self.put(x + dx, top - 1, z + dz, "minecraft:bricks")
        self.put(x + 1, top + 1, z, "minecraft:decorated_pot", {"facing": "north", "cracked": "false",
                                                                "waterlogged": "false"}) \
            if self.rng.random() < 0.3 else None

    # ------------------------------------------------------------ roofs
    def _roof_cell(self, u, v, y, ridge, name, props=None):
        x, z = (u, v) if ridge == "z" else (v, u)
        self.put(x, y, z, name, props)
        if name.endswith("_stairs"):
            self.stairs.add((x, y, z))

    def _stair_dir(self, ridge, toward_plus_u):
        if ridge == "z":     # slopes run across x
            return "east" if toward_plus_u else "west"
        return "south" if toward_plus_u else "north"

    def roof_gable(self, x0, x1, z0, z1, y, ridge="z", roof="tile", wall="minecraft:white_terracotta", pitch=2,
                   trim=True, dormers=0):
        """
        A steep gable over the rectangle (walls at x0..x1, z0..z1, top of the walls at y - 1). ridge "z": the ridge
        runs front to back and the gable end faces the street; "x": the ridge runs along the street. The roof
        overhangs the walls by one all round; dark timber barge boards trim the gable ends; the gable walls are
        filled with plaster and get a little window. Returns the y just above the ridge.
        """
        stairs, full, slab = ROOFS[roof]
        u0, u1, v0, v1 = (x0, x1, z0, z1) if ridge == "z" else (z0, z1, x0, x1)
        i = 0
        ua, ub = u0 - 1, u1 + 1
        ye = y - 1
        while ua <= ub:
            for k in range(pitch):
                yy = ye + i * pitch + k
                for v in range(v0 - 1, v1 + 2):
                    rake = v in (v0 - 1, v1 + 1)
                    if ua == ub:
                        if DOMUM and pitch == 2:
                            if k == 0:
                                x, z = (ua, v) if ridge == "z" else (v, ua)
                                self.ridge_cap(x, yy, z, "z" if ridge == "z" else "x", roof)
                            continue
                        self._roof_cell(ua, v, yy, ridge, full if k < pitch - 1 else slab,
                                        None if k < pitch - 1 else {"type": "bottom", "waterlogged": "false"})
                        continue
                    for u, plus in ((ua, True), (ub, False)):
                        top = k == pitch - 1 or (i == 0 and k == 0)
                        if DOMUM and pitch == 2 and not (rake and trim):
                            x, z = (u, v) if ridge == "z" else (v, u)
                            self.shingle(x, yy, z, "steep" if k == 0 else "steep_lower",
                                         self._stair_dir(ridge, plus), roof)
                            continue
                        if rake and trim:
                            self._roof_cell(u, v, yy, ridge, TRIM[0] if top else TRIM[1],
                                            {"facing": self._stair_dir(ridge, plus), "half": "bottom",
                                             "shape": "straight", "waterlogged": "false"} if top else None)
                        elif top:
                            self._roof_cell(u, v, yy, ridge, stairs, {"facing": self._stair_dir(ridge, plus),
                                                                      "half": "bottom", "shape": "straight",
                                                                      "waterlogged": "false"})
                        else:
                            self._roof_cell(u, v, yy, ridge, full)
                    if v in (v0, v1) and yy >= y:
                        for u in range(ua + 1, ub):
                            if u0 <= u <= u1:
                                x, z = (u, v) if ridge == "z" else (v, u)
                                if DOMUM and wall in PLAIN_WALLS:          # the gable end walls, timber-framed
                                    out = ("north" if v == v0 else "south") if ridge == "z" else ("west" if v == v0 else "east")
                                    kind = "double_crossed" if (u - u0) % 2 == 0 else "side_framed"
                                    self.frame(x, yy, z, kind, out, wall)
                                else:
                                    self._roof_cell(u, v, yy, ridge, wall)
                    elif v0 < v < v1 and yy >= y:
                        for u in range(ua + 1, ub):
                            x, z = (u, v) if ridge == "z" else (v, u)
                            if self.is_free(x, yy, z) or (x, yy, z) not in self.blocks:
                                self.air(x, yy, z)                          # the attic
            ua += 1
            ub -= 1
            i += 1
        top_y = ye + i * pitch
        # A small window high in each gable end, and an upside-down trim under the eaves.
        mid = (u0 + u1) // 2
        for v in (v0, v1):
            if u1 - u0 >= 4:
                x, z = (mid, v) if ridge == "z" else (v, mid)
                self.put(x, y + 1, z, "minecraft:glass_pane", {})
        for v in range(v0, v1 + 1):
            for u, plus in ((u0 - 1, False), (u1 + 1, True)):
                x, z = (u, v) if ridge == "z" else (v, u)
                if self.is_free(x, ye - 1, z):
                    self._roof_cell(u, v, ye - 1, ridge, TRIM[0], {"facing": self._stair_dir(ridge, not plus),
                                                                  "half": "top", "shape": "straight",
                                                                  "waterlogged": "false"}) if v % 2 == 0 else None
        # Dormers on the street side of a roof running along the street.
        if ridge == "x" and dormers:
            span = x1 - x0
            for d in range(dormers):
                dx = x0 + (d + 1) * span // (dormers + 1)
                self.dormer(dx, z0 + 1 - 1, ye + pitch, roof)
        return top_y

    def dormer(self, x, z, y, roof="tile"):
        """A little gabled window box sticking out of the front slope of a roof at (x, z), sill at y."""
        stairs, full, slab = ROOFS[roof]
        for yy in (y, y + 1):
            self.put(x, yy, z, "minecraft:glass_pane", {})
            for dx in (-1, 1):
                self.put(x + dx, yy, z, "minecraft:spruce_planks")
            self.air(x, yy, z + 1)
        self.put(x, y - 1, z, "minecraft:dark_oak_slab", {"type": "top", "waterlogged": "false"})
        self.stair(x - 1, y + 2, z, stairs, "east")
        self.stair(x + 1, y + 2, z, stairs, "west")
        self.put(x, y + 2, z, slab, {"type": "top", "waterlogged": "false"})
        self.put(x, y + 3, z, slab, {"type": "bottom", "waterlogged": "false"})

    def roof_mansard(self, x0, x1, z0, z1, y, roof="slate", windows=True):
        """A steep mansard storey: tall slate sides just past the walls, pierced by windows, a flat cap."""
        stairs, full, slab = ROOFS[roof]
        for h in range(3):
            yy = y + h
            ins = 1 if h == 2 else 0
            lo_x, hi_x, lo_z, hi_z = x0 - 1 + ins, x1 + 1 - ins, z0 - 1 + ins, z1 + 1 - ins
            for x in range(lo_x, hi_x + 1):
                for z in range(lo_z, hi_z + 1):
                    edge_x, edge_z = x in (lo_x, hi_x), z in (lo_z, hi_z)
                    if not (edge_x or edge_z):
                        if h == 2:
                            self.put(x, yy, z, slab, {"type": "top", "waterlogged": "false"})   # the flat cap
                        else:
                            self.air(x, yy, z)
                    elif h == 2:
                        f = ("south" if z == lo_z else "north") if edge_z else ("east" if x == lo_x else "west")
                        self.shingle(x, yy, z, "normal", f, roof) if DOMUM else self.stair(x, yy, z, stairs, f)
                    elif DOMUM:
                        f = ("south" if z == lo_z else "north") if edge_z else ("east" if x == lo_x else "west")
                        self.shingle(x, yy, z, "steep" if h == 0 else "steep_lower", f, roof)
                    else:
                        self.put(x, yy, z, full)
        # Trim under the overhang, and windows in the steep sides.
        for x in range(x0 - 1, x1 + 2):
            for z in (z0 - 1, z1 + 1):
                if self.is_free(x, y - 1, z):
                    self.stair(x, y - 1, z, TRIM[0], "south" if z == z0 - 1 else "north", half="top")
        if windows:
            for x in range(x0 + 1, x1, 2):
                for z in (z0 - 1, z1 + 1):
                    self.put(x, y + 1, z, "minecraft:glass_pane", {})
        return y + 3

    def roof_spire(self, x0, x1, z0, z1, y, roof="tile", rise=3, finial="rod"):
        """
        A tall pointed roof: a bell-cast flare of stairs just past the walls, then a steep pyramid, then a needle
        finial. Returns the tip y.
        """
        stairs, full, slab = ROOFS[roof]
        # The flare.
        for x in range(x0 - 1, x1 + 2):
            for z in range(z0 - 1, z1 + 2):
                if x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1):
                    f = ("south" if z == z0 - 1 else "north") if z in (z0 - 1, z1 + 1) else ("east" if x == x0 - 1 else "west")
                    if DOMUM:
                        self.shingle(x, y - 1, z, "normal", f, roof)
                    else:
                        self.stair(x, y - 1, z, stairs, f)
        if DOMUM:
            rise = 2
        layer = 0
        while x1 >= x0 and z1 >= z0:
            for k in range(rise):
                yy = y + layer * rise + k
                for x in range(x0, x1 + 1):
                    for z in range(z0, z1 + 1):
                        edge_x, edge_z = x in (x0, x1), z in (z0, z1)
                        if DOMUM and (edge_x or edge_z) and (x1 > x0 or z1 > z0):
                            f = ("south" if z == z0 else "north") if edge_z else ("east" if x == x0 else "west")
                            self.shingle(x, yy, z, "steep" if k == 0 else "steep_lower", f, roof)
                        elif (edge_x or edge_z) and k == rise - 1 and (x1 > x0 or z1 > z0):
                            f = ("south" if z == z0 else "north") if edge_z else ("east" if x == x0 else "west")
                            self.stair(x, yy, z, stairs, f)
                        elif (edge_x or edge_z) or k == 0:
                            self.put(x, yy, z, full)
                        else:
                            self.air(x, yy, z)
            x0, x1, z0, z1 = x0 + 1, x1 - 1, z0 + 1, z1 - 1
            layer += 1
        tip = y + layer * rise
        cx, cz = (x0 + x1 + 1) // 2, (z0 + z1 + 1) // 2
        if finial == "rod":
            self.put(cx, tip, cz, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
        elif finial == "flag":
            for k in range(2):
                self.fence(cx, tip + k, cz, "minecraft:dark_oak_fence")
            self.put(cx, tip + 2, cz, "minecraft:red_wool")
        elif finial == "lantern":
            self.fence(cx, tip, cz)
            self.put(cx, tip + 1, cz, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
        return tip

    # ------------------------------------------------------------ the town house
    def townhouse(self, xa, xb, za, zb, floors, roof_kind="gable_z", roof="tile", plaster=None, ground="shop",
                  lean_z=0.0, jetty=True, bay=None, damage=None, chimney=True, folk=(), sides=(True, True),
                  shop_name=None):
        """
        One crooked town house on the lot xa..xb (walls inclusive), front wall at za, back at zb. Upper floors jut
        out over the street and lean; ground floor is a shop (big windows, an awning, a hanging sign), a stone
        workshop or brick. sides: whether the left/right walls are exposed (in a row they're shared). Returns the
        y of its roof top.
        """
        rng = self.rng
        plaster = plaster or rng.choice(PLASTERS)
        bounds = []
        for k in range(floors):
            g = (1 if k >= 1 else 0) + (1 if k >= 3 and jetty else 0) if jetty else 0
            sz = int(round(k * lean_z))
            bounds.append((xa, xb, za - g + sz, zb + sz))
        # The ladder post must stand inside every floor.
        oz0 = max(b[2] for b in bounds) + 2
        oz1 = min(b[3] for b in bounds) - 1
        post = (xb - 1 if xb - xa >= 4 else xa + 1, max(oz0, oz1))
        mid = (xa + xb) // 2
        for k, (x0, x1, z0, z1) in enumerate(bounds):
            y0 = k * 4
            ground_floor = k == 0
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    edge_x, edge_z = x in (x0, x1), z in (z0, z1)
                    # Floor (and, at the front of an overhang, a beam).
                    if ground_floor:
                        if edge_x and edge_z:
                            self.log(x, y0, z, kind="dark_oak_log")
                        elif edge_x or edge_z:
                            self.stone(x, y0, z)
                        else:
                            self.put(x, y0, z, FLOOR)
                    elif edge_x or edge_z:
                        self.log(x, y0, z, axis="x" if edge_z else "z", kind="stripped_dark_oak_log")
                    elif (x, z) != post:
                        self.put(x, y0, z, FLOOR)
                    for y in range(y0 + 1, y0 + 4):
                        if not (edge_x or edge_z):
                            self.air(x, y, z)
                        elif edge_x and edge_z:
                            self.log(x, y, z, kind="dark_oak_log")
                        elif ground_floor and ground in ("stone", "brick"):
                            if edge_z and z == z0 and abs(x - mid) == 1 and y < y0 + 3:
                                self.put(x, y, z, "minecraft:stone_bricks" if ground == "stone" else "minecraft:bricks")
                            else:
                                self.put(x, y, z, "minecraft:bricks") if ground == "brick" else self.stone(x, y, z)
                        elif ground_floor and ground == "shop":
                            front = edge_z and z == z0 and x not in (x0, x1)
                            if front and abs(x - mid) == 1:
                                self.log(x, y, z, kind="dark_oak_log")           # the door posts
                            elif front and y == y0 + 2 and x != mid:
                                self.put(x, y, z, "minecraft:glass_pane", {})     # the shop window, over a stallboard
                            elif front and y == y0 + 1 and x != mid:
                                self.put(x, y, z, "minecraft:spruce_planks")
                            else:
                                self.put(x, y, z, "minecraft:dark_oak_planks")
                        else:
                            along = x - x0 if edge_z else z - z0
                            span = (x1 - x0) if edge_z else (z1 - z0)
                            if span > 4 and along == span // 2 and y == y0 + 1:
                                self.log(x, y, z, kind="dark_oak_log")
                            elif DOMUM:
                                out = ("north" if z == z0 else "south") if edge_z else ("west" if x == x0 else "east")
                                row_in_floor = y - y0
                                if row_in_floor == 1:     # chevron braces under the windows
                                    kind = "one_crossed_lr" if along < span / 2 else "one_crossed_rl"
                                elif row_in_floor == 2:
                                    kind = "framed"
                                else:
                                    kind = "double_crossed" if along % 2 == 0 else "horizontal_plain"
                                self.frame(x, y, z, kind, out, plaster)
                            else:
                                self.put(x, y, z, plaster)
            # Brackets under the jetty (over a shop, a striped awning instead).
            if k >= 1:
                pz0 = bounds[k - 1][2]
                if z0 < pz0:
                    awning = k == 1 and ground == "shop"
                    color = rng.choice(["red", "green", "blue", "brown", "yellow", "black"])
                    for x in range(x0, x1 + 1):
                        for z in range(z0, pz0):
                            if awning and x0 < x < x1:
                                self.put(x, y0 - 1, z, f"minecraft:{color}_wool" if (x - x0) % 2 else "minecraft:white_wool")
                            elif x in (x0, x1) or (x - x0) % 2 == 0:
                                self.stair(x, y0 - 1, z, "minecraft:dark_oak_stairs", "south", half="top")
            # Windows.
            for x in range(x0 + 1, x1):
                if (x - x0) % 2 == 1 and not ground_floor:
                    self.put(x, y0 + 2, z0, "minecraft:glass_pane", {})
                    if rng.random() < 0.45:
                        self.put(x, y0 + 3, z0, "minecraft:glass_pane", {})
                    self.put(x, y0 + 2, z1, "minecraft:glass_pane", {})
            for side, x in ((0, x0), (1, x1)):
                if sides[side]:
                    for z in range(z0 + 1, z1):
                        if (z - z0) % 3 == 1:
                            self.put(x, y0 + 2, z, "minecraft:glass_pane", {})
            self.hanging_lantern(x0 + 1, y0 + 3, z1 - 1)
            # A lit window here and there: a torch inside on the sill wall.
            if not ground_floor and rng.random() < 0.6:
                self.wall_torch(x1 - 1, y0 + 2, z0 + 1, "south")
        # Where an upper floor steps back from the one below (a lean), the strip of the room below left open to the sky
        # gets a ceiling, with a timber ledge along it.
        for k in range(1, len(bounds)):
            lx0, lx1, lz0, lz1 = bounds[k - 1]
            ux0, ux1, uz0, uz1 = bounds[k]
            y = k * 4
            for x in range(lx0, lx1 + 1):
                for z in range(lz0, lz1 + 1):
                    if ux0 <= x <= ux1 and uz0 <= z <= uz1:
                        continue
                    edge = x in (lx0, lx1) or z in (lz0, lz1)
                    if edge:
                        self.log(x, y, z, axis="x" if z in (lz0, lz1) else "z", kind="stripped_dark_oak_log")
                    else:
                        self.put(x, y, z, "minecraft:dark_oak_planks")
                    if self.is_free(x, y + 1, z) or (x, y + 1, z) not in self.blocks:
                        self.put(x, y + 1, z, "minecraft:dark_oak_slab", {"type": "bottom", "waterlogged": "false"})
        # The ladder post through every floor.
        px, pz = post
        for y in range(0, floors * 4):
            self.log(px, y, pz, kind="dark_oak_log")
            if y >= 1:
                self.put(px, y, pz - 1, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
        for k in range(1, floors):
            self.put(px, k * 4, pz - 1, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
        # The door, with a lamp beside it; a shop gets an awning and a hanging sign.
        x0, x1, z0, z1 = bounds[0]
        self.door(mid, 1, z0, "south", kind="dark_oak_door")
        self.log(mid, 3, z0, axis="x", kind="stripped_dark_oak_log")        # the lintel
        side_x = mid + 1 if mid + 1 < x1 else mid - 1
        if ground != "shop":
            self.wall_torch(side_x, 2, z0 - 1, "north")
        if self.is_free(mid, 0, z0 - 1) or (mid, 0, z0 - 1) not in self.blocks:
            self.put(mid, 0, z0 - 1, "minecraft:stone_bricks")             # a doorstep
        if ground == "shop":
            used = self.__dict__.setdefault("_shops_used", set())
            shop = shop_name or rng.choice([sh for sh in SHOPS if sh[0] not in used] or SHOPS)
            used.add(shop[0])
            # The sign hangs right beside the door (where a house has its torch); the torch goes on the corner post.
            for sy in (2, 3):
                if self.is_free(side_x, sy, z0 - 1):
                    self.street_sign(side_x, z0, sy, shop)
                    break
            self.wall_torch(x1, 2, z0 - 1, "north")
        else:
            self.bracket_lantern(mid - 1 if mid - 1 > x0 else mid + 1, 3, z0 - 1, "north")
        # A bay window on the second floor.
        if bay is None:
            bay = floors >= 3 and x1 - x0 >= 4 and rng.random() < 0.55
        if bay and floors >= 2:
            self.bay_window(mid, 4, bounds[1][2], plaster)
        # Damage: a patched wall, or a hole boarded over and propped up.
        if damage is None:
            damage = rng.choice([None, None, "patch", "patch", "hole"])
        if damage and floors >= 2:
            self.damage(bounds, damage, plaster, sides)
        # Furnishing and folk.
        self._furnish(bounds, post)
        if ground == "shop":
            # The shopkeeper, just inside the door: sells what the sign says (see FolkTrades, "shop:" pools).
            for (sx, sz) in ((mid - 1, z0 + 2), (mid + 1, z0 + 2), (mid, z0 + 2), (mid - 1, z0 + 3), (mid + 1, z0 + 3)):
                if (sx, sz) in (post, (post[0], post[1] - 1)) or not (x0 < sx < x1 and z0 < sz < z1):
                    continue
                if self.is_free(sx, 1, sz) and self.is_free(sx, 2, sz) and not self.is_free(sx, 0, sz):
                    self.folk(sx, 1, sz, "rat", "scavenger", stay=2)
                    self.entities[-1][3]["Shop"] = shop[0]
                    break
        # The roof.
        tx0, tx1, tz0, tz1 = bounds[-1]
        ry = floors * 4
        for x in range(tx0, tx1 + 1):
            for z in range(tz0, tz1 + 1):
                self.put(x, ry, z, FLOOR if (x, z) != post else "minecraft:dark_oak_log")
        if roof_kind == "deck":
            top = ry + 5         # a flat deck: the caller builds on it (see roof_deck)
        elif roof_kind == "spire":
            top = self.roof_spire(tx0, tx1, tz0, tz1, ry + 1, roof=roof, rise=3,
                                  finial=rng.choice(["rod", "flag", "lantern"]))
        elif roof_kind == "mansard":
            top = self.roof_mansard(tx0, tx1, tz0, tz1, ry + 1, roof=roof)
        elif roof_kind == "gable_x":
            top = self.roof_gable(tx0, tx1, tz0, tz1, ry + 1, ridge="x", roof=roof, wall=plaster,
                                  dormers=1 if tx1 - tx0 >= 4 else 0)
        else:
            top = self.roof_gable(tx0, tx1, tz0, tz1, ry + 1, ridge="z", roof=roof, wall=plaster)
        if chimney:
            cx = tx1 - 1 if rng.random() < 0.5 else tx0 + 1
            self.chimney(cx, tz1, ry - 3, min(top + 1, ry + 7))
        for race, variant, floor in folk:
            fx0, fx1, fz0, fz1 = bounds[min(floor, floors - 1)]
            self.folk(fx0 + 1, floor * 4 + 1, fz0 + 2, race, variant)
        return top, bounds

    def bay_window(self, x, y, z, plaster):
        """A three-wide box of windows sticking out of the front wall at (x, z) above floor level y."""
        for dx in (-1, 0, 1):
            self.stair(x + dx, y, z - 1, "minecraft:dark_oak_stairs", "south", half="top")
            for yy in (y + 1, y + 2):
                self.put(x + dx, yy, z - 1, "minecraft:glass_pane", {})
                self.air(x + dx, yy, z)
            self.stair(x + dx, y + 3, z - 1, "minecraft:dark_oak_stairs", "south")
        for dx in (-2, 2):
            for yy in (y + 1, y + 2):
                self.put(x + dx, yy, z - 1, "minecraft:dark_oak_log", {"axis": "y"}) if self.is_free(x + dx, yy, z - 1) else None

    def damage(self, bounds, kind, plaster, sides):
        """Wear and tear: a patch of mismatched brick or planks, or an open hole boarded up and propped."""
        rng = self.rng
        k = rng.randint(1, len(bounds) - 1)
        x0, x1, z0, z1 = bounds[k]
        y0 = k * 4
        # Pick a wall: the front, or an exposed side.
        if kind == "patch":
            mat = rng.choice(["minecraft:bricks", "minecraft:cobblestone", "minecraft:spruce_planks",
                              "minecraft:mossy_cobblestone", "minecraft:oak_planks"])
            px = rng.randint(x0 + 1, max(x0 + 1, x1 - 2))
            for x in range(px, min(px + 2, x1)):
                for y in range(y0 + 1, y0 + 3):
                    b = self.blocks.get((x, y, z1))
                    if b and (b[0] == plaster or b[0].startswith("domum_ornamentum:")):
                        self.put(x, y, z1, mat)
                    b = self.blocks.get((x, y, z0))
                    if b and (b[0] == plaster or b[0].startswith("domum_ornamentum:")) and rng.random() < 0.6:
                        self.put(x, y, z0, mat)
        else:
            # A hole in the front wall, boards nailed across it, and a timber prop from the street.
            hx = x0 + 1 if x1 - x0 >= 3 else x0
            for y in (y0 + 1, y0 + 2):
                self.put(hx, y, z0, "minecraft:spruce_trapdoor", {"facing": "north", "half": "bottom",
                                                                  "open": "true", "powered": "false",
                                                                  "waterlogged": "false"})
            self.put(hx + 1, y0 + 2, z0, "minecraft:glass_pane", {}) if hx + 1 < x1 else None
            self.put(hx + 1, y0 + 1, z0, "minecraft:cobblestone") if hx + 1 < x1 else None
            for y in range(1, y0):
                if self.is_free(hx, y, z0 - 1):
                    self.log(hx, y, z0 - 1, kind="stripped_spruce_log")
            self.stair(hx, y0, z0 - 1, "minecraft:spruce_stairs", "south", half="top") \
                if self.is_free(hx, y0, z0 - 1) else None

    def _furnish(self, bounds, post):
        rng = self.rng
        for k, (x0, x1, z0, z1) in enumerate(bounds):
            y = k * 4 + 1
            mid = (x0 + x1) // 2
            for (x, z) in ((x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1), (x0 + 1, z0 + 1)):
                if (x, z) == post or (x, z + 1) == post or not self.is_free(x, y, z) or (x == mid and z == z0 + 1):
                    continue
                r = rng.random()
                if r < 0.2:
                    self.barrel(x, y, z)
                elif r < 0.3:
                    self.mod(x, y, z, "supplementaries:sack", {"open": "false"}, "minecraft:barrel",
                             {"facing": "up", "open": "false"})
                elif r < 0.42:
                    self.put(x, y, z, "minecraft:bookshelf")
                elif r < 0.52:
                    self.put(x, y, z, "minecraft:crafting_table")
                elif r < 0.62:
                    self.fence(x, y, z, "minecraft:spruce_fence")
                    self.put(x, y + 1, z, "minecraft:candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
                elif r < 0.72:
                    self.put(x, y, z, "minecraft:white_carpet" if k else "minecraft:red_carpet")
                elif r < 0.8:
                    self.potted(x, y, z)
                elif r < 0.9 and k == 0:
                    self.put(x, y, z, "minecraft:furnace", {"facing": "north", "lit": "false"})
            # A bed on every upper floor, along the back wall, clear of the ladder (never green: green beds are the
            # Gar's accountants').
            if k >= 1:
                self.bed_along_back(x0, x1, z1, y, post)

    BED_COLORS = ["red", "blue", "brown", "white", "light_blue", "yellow", "cyan", "purple", "orange", "gray"]

    def bed_along_back(self, x0, x1, z1, y, post):
        px, pz = post
        banned = {(px, pz), (px, pz - 1), (px, pz - 2)}
        z = z1 - 1
        spots = list(range(x0 + 1, x1 - 1))
        self.rng.shuffle(spots)
        for x in spots:
            if any((xx, z) in banned or not self.is_free(xx, y, z) or not self.is_free(xx, y + 1, z)
                   or self.is_free(xx, y - 1, z) for xx in (x, x + 1)):
                continue
            color = self.rng.choice(self.BED_COLORS)
            self.put(x, y, z, f"minecraft:{color}_bed", {"facing": "east", "part": "foot", "occupied": "false"})
            self.put(x + 1, y, z, f"minecraft:{color}_bed", {"facing": "east", "part": "head", "occupied": "false"})
            return True
        return False


def row(B, z_front, depth, lots, seed_folk):
    """
    Houses joined side by side into a terrace along the street: lots = [(width, floors, roof_kind, roof, ground)]
    from left to right. Neighbours share their side walls; their heights, materials and roofs all differ.
    Returns the bounds of the row.
    """
    total = sum(w for w, *_ in lots) - (len(lots) - 1)
    x = B.c - total // 2
    zb = z_front + depth - 1
    built = []
    for i, (w, floors, roof_kind, roof, ground) in enumerate(lots):
        xa, xb = x, x + w - 1
        folk = seed_folk[i % len(seed_folk)]
        _, bounds = B.townhouse(xa, xb, z_front, zb, floors, roof_kind=roof_kind, roof=roof, ground=ground,
                    lean_z=B.rng.choice([0.0, 0.25, -0.25, 0.34]), folk=folk,
                    sides=(i == 0, i == len(lots) - 1))
        built.append((xa, xb, bounds))
        x = xb
    # A neighbour's eaves mustn't poke into the rooms next door.
    roofing = {name for kind in ROOFS.values() for name in kind} | set(TRIM) | DO_ROOFING
    for xa, xb, bounds in built:
        for k, (x0, x1, z0, z1) in enumerate(bounds):
            for xx in range(xa + 1, xb):
                for zz in range(z0 + 1, z1):
                    for yy in range(k * 4 + 1, k * 4 + 4):
                        b = B.blocks.get((xx, yy, zz))
                        if b and b[0] in roofing:
                            B.air(xx, yy, zz)
    return B.c - total // 2, x


# ===================================================================== decks, roosts and skyways

def rickety_deck(B, x0, x1, z0, z1, y, ground=0, patrons=(), seed_gaps=0.12, openings=()):
    """
    A rickety wooden deck stuck onto a building at floor level y: odd planks and slabs with the occasional board
    missing, spindly fence-post legs down to the ground with a brace or two, a railing with gaps, little tables with
    a drink and a candle, stools, lanterns on posts. patrons: [(race, variant)] to sit about on it. The railing is left
    open in front of any doorway onto the deck (open air in the wall beside it) and at `openings` (edge cells where
    steps come up).
    """
    rng = B.rng
    openings = set(openings)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not (x in (x0, x1) or z in (z0, z1)):
                continue
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, nz = x + dx, z + dz
                if x0 <= nx <= x1 and z0 <= nz <= z1:
                    continue
                door = all(B.blocks.get((nx, yy, nz), ("",))[0] in ("minecraft:air",) or
                           B.blocks.get((nx, yy, nz), ("",))[0].endswith("_door") for yy in (y + 1, y + 2))
                if door and (nx, y + 1, nz) in B.blocks:
                    openings.add((x, z))
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge_x, edge_z = x in (x0, x1), z in (z0, z1)
            r = rng.random()
            if r < 0.35:
                B.put(x, y, z, "minecraft:spruce_slab", {"type": "top", "waterlogged": "false"})
            else:
                B.put(x, y, z, rng.choice(["minecraft:spruce_planks", "minecraft:oak_planks", "minecraft:dark_oak_planks",
                                           "minecraft:spruce_planks"]))
            for yy in range(y + 1, y + 3):
                if B.is_free(x, yy, z):
                    B.air(x, yy, z)
    # Legs: fence posts at the corners (and midway on a long side) down to the ground, braced under the boards.
    legs = {(x0, z0), (x1, z0), (x0, z1), (x1, z1)}
    if x1 - x0 >= 4:
        legs |= {((x0 + x1) // 2, z0), ((x0 + x1) // 2, z1)}
    if z1 - z0 >= 4:
        legs |= {(x0, (z0 + z1) // 2), (x1, (z0 + z1) // 2)}
    for (x, z) in legs:
        for yy in range(ground, y):
            if B.is_free(x, yy, z):
                B.fence(x, yy, z, "minecraft:spruce_fence")
        B.log(x, ground - 1, z, kind="spruce_log") if ground >= 1 and B.is_free(x, ground - 1, z) else None
    for x in range(x0, x1 + 1):
        for z in (z0, z1):
            if (x, z) not in legs and rng.random() < 0.6 and B.is_free(x, y - 1, z):
                B.stair(x, y - 1, z, "minecraft:spruce_stairs", "north" if z == z0 else "south", half="top")
    # The railing, all the way round (but not along a wall of the building: that side is closed already).
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not (x in (x0, x1) or z in (z0, z1)) or (x, z) in openings or not B.is_free(x, y + 1, z):
                continue
            open_side = False
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, nz = x + dx, z + dz
                if x0 <= nx <= x1 and z0 <= nz <= z1:
                    continue
                if not B.holds(nx, y + 1, nz) and not B.holds(nx, y + 2, nz):
                    open_side = True
            if open_side:
                B.fence(x, y + 1, z, "minecraft:spruce_fence")
    for (x, z) in ((x0, z0), (x1, z1)):
        if (x, y + 1, z) in B.blocks and B.blocks[(x, y + 1, z)][0].endswith("_fence"):
            B.put(x, y + 2, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    # The middle is left open (room to stand about); the patrons stand about in it.
    spots = [(x, z) for x in range(x0 + 1, x1) for z in range(z0 + 1, z1)]
    rng.shuffle(spots)
    for i, (race, variant) in enumerate(patrons):
        x, z = spots[(i * 3 + 1) % len(spots)]
        B.folk(x, y + 1, z, race, variant)


def steps(B, x, z, y_top, toward, ground=0, wood="spruce"):
    """
    An outside wooden stair up to a deck: it climbs `toward` ("east"...) from the street and its top step, at y_top,
    is at (x, z). The stair is propped on posts; everything above it is cleared for headroom.
    """
    dx, dz = DIR4[toward]
    n = y_top - ground
    for k in range(n):
        sx, sz = x - dx * k, z - dz * k
        sy = y_top - k
        B.stair(sx, sy, sz, f"minecraft:{wood}_stairs", toward)
        for yy in range(ground, sy):
            B.put(sx, yy, sz, f"minecraft:{wood}_planks" if yy == sy - 1 else f"minecraft:{wood}_fence")
        for yy in range(sy + 1, sy + 4):
            if (sx, yy, sz) not in B.blocks or B.blocks[(sx, yy, sz)][0].endswith("_fence"):
                B.air(sx, yy, sz)


def roof_deck(B, x0, x1, z0, z1, y, band=True, stage="back", hatch=None):
    """
    A flat deck on a roof for a Bazz band: railings, a little stage at the back under a striped canopy (with
    fence rods hanging from it, so the band can roost under their own canopy by day), lamps at the corners.
    """
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            B.put(x, y, z, "minecraft:spruce_planks" if (x + z) % 3 else "minecraft:dark_oak_planks")
            for yy in range(y + 1, y + 6):
                B.air(x, yy, z)
            if x in (x0, x1) or z in (z0, z1):
                B.fence(x, y + 1, z, "minecraft:dark_oak_fence")
    for (x, z) in ((x0, z0), (x1, z0)):
        B.lantern_post(x, y + 1, z, 2)
    # The stage: a step up at the back (or the front), posts and a canopy. The band can fly up to it; the audience
    # comes up through the hatch at the other end.
    sx0, sx1 = x0 + 1, x1 - 1
    sz0, sz1 = (z1 - 2, z1 - 1) if stage == "back" else (z0 + 1, z0 + 2)
    if hatch:
        hx, hz = hatch
        B.put(hx, y, hz, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    for x in range(sx0, sx1 + 1):
        for z in range(sz0, sz1 + 1):
            B.put(x, y + 1, z, "minecraft:dark_oak_slab", {"type": "bottom", "waterlogged": "false"})
    front_row = sz0 if stage == "back" else sz1        # the stage's edge facing the audience
    back_row = sz1 if stage == "back" else sz0
    for (x, z) in ((sx0, front_row), (sx1, front_row)):
        for yy in range(y + 1, y + 5):
            B.fence(x, yy, z, "minecraft:dark_oak_fence")
    color = B.rng.choice(["red", "purple", "blue", "black"])
    for x in range(sx0 - 1, sx1 + 2):
        for z in range(sz0 - 1, sz1 + 2):
            B.put(x, y + 5, z, f"minecraft:{color}_wool" if (x - sx0) % 2 == 0 else "minecraft:yellow_wool")
    for x in range(sx0 + 1, sx1):
        B.fence(x, y + 4, front_row, "minecraft:dark_oak_fence")        # roosting rods under the canopy
    B.put((sx0 + sx1) // 2, y + 4, back_row, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    B.put(sx1, y + 2, back_row, "minecraft:note_block", {"instrument": "bass", "note": "0", "powered": "false"})
    if band:
        mid = (sx0 + sx1) // 2
        B.folk(mid - 1, y + 1, back_row, "bat", "jazz")
        B.folk(mid + 1, y + 1, back_row, "bat", "folk")
        B.folk(mid, y + 1, front_row, "bat", "flapper")


def roost_rack(B, x0, x1, z_wall, y, face="north", length=2):
    """
    A bat roost rack on a wall: two brackets hold a beam out from the wall; a row of fence rods hangs under the
    beam for bats to hang from by day; a little lean-to awning of stairs runs along the top and shades them all.
    z_wall: the wall's z (for face north/south) or x (for west/east); x0..x1: the run along the wall.
    """
    sgn = -1 if face in ("north", "west") else 1
    out = OPP[face]      # stairs' high side points back at the wall

    def at(a, d):
        return (a, z_wall + sgn * d) if face in ("north", "south") else (z_wall + sgn * d, a)

    axis_out = "z" if face in ("north", "south") else "x"
    axis_along = "x" if face in ("north", "south") else "z"
    for a in (x0, x1):
        for d in range(1, length + 1):
            x, z = at(a, d)
            B.log(x, y, z, axis=axis_out, kind="stripped_dark_oak_log")
    for a in range(x0, x1 + 1):
        x, z = at(a, length)
        B.log(x, y, z, axis=axis_along, kind="stripped_dark_oak_log")
        if B.is_free(x, y - 1, z):
            B.fence(x, y - 1, z, "minecraft:spruce_fence")              # a rod to hang from
        for d in range(1, length + 1):
            x, z = at(a, d)
            B.stair(x, y + 1, z, "minecraft:dark_oak_stairs", out)       # the awning, high side to the wall
