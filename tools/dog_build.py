"""
The toolkit for Dog City's buildings (see make_dog_city.py). The Dog folk live simple, formal, ordered lives, and their
city is a well-kept fort: a European stone fort with buildings in a Japanese manner inside it.

  - Ground floors of plain stone brick on a polished andesite plinth course, small square windows.
  - Upper storeys of white plaster between dark oak posts and stripped dark oak beams, paper-screen windows (white
    stained glass), a dark timber band at each floor.
  - Hip roofs of dark deepslate tile, stepped in one block a layer, with deep eaves trimmed underneath in dark oak.
  - Everything square, symmetrical and in good repair: no moss, no cracks, no vines.

Coordinates: the front (door) is toward -z. Template y = 0 is the ground floor's floor, laid in the ground.
"""
from nbt_structure import Byte
from rcb_build import RCBBuilder

STONE = "minecraft:stone_bricks"
PLINTH = "minecraft:polished_andesite"
PLASTER = "minecraft:white_terracotta"
POST = "dark_oak_log"
BEAM = "stripped_dark_oak_log"
FLOOR = "minecraft:spruce_planks"
SCREEN = "minecraft:white_stained_glass_pane"
TILE = ("minecraft:deepslate_tile_stairs", "minecraft:deepslate_tiles", "minecraft:deepslate_tile_slab")
DARK = ("minecraft:polished_blackstone_brick_stairs", "minecraft:polished_blackstone_bricks",
        "minecraft:polished_blackstone_brick_slab")
TRIM_STAIRS = "minecraft:dark_oak_stairs"

# Each quarter's colours: its roof, and two or three dyes that go with it (carpets, beds, banners, cloths).
THEMES = {
    "cloth": dict(roof=("minecraft:dark_prismarine_stairs", "minecraft:dark_prismarine", "minecraft:dark_prismarine_slab"),
                  colors=["blue", "light_blue", "white"]),             # indigo and undyed linen: the city's export
    "yards": dict(roof=("minecraft:brick_stairs", "minecraft:bricks", "minecraft:brick_slab"),
                  colors=["brown", "orange", "black"]),               # leather, forge-glow, soot
    "meat": dict(roof=("minecraft:red_nether_brick_stairs", "minecraft:red_nether_bricks",
                       "minecraft:red_nether_brick_slab"),
                 colors=["red", "gray", "white"]),                    # the butcher's red, his apron and his block
    "hall": dict(roof=("minecraft:waxed_oxidized_cut_copper_stairs", "minecraft:waxed_oxidized_cut_copper",
                       "minecraft:waxed_oxidized_cut_copper_slab"),
                 colors=["green", "yellow", "lime"]),                 # the mess, the music and the school: harvest colours
}
OUT = {(0, -1): "north", (0, 1): "south", (-1, 0): "west", (1, 0): "east"}
BED_COLORS = ["blue", "light_blue", "white", "gray", "cyan", "brown"]


class DogBuilder(RCBBuilder):
    weathered = False      # well kept: no moss, no cracks, no vines
    capped = False

    def finish(self):
        self.unblock_windows()
        super().finish()

    def unblock_windows(self):
        """No ladder ever hangs over a window: glass behind a ladder becomes the wall it was cut in."""
        back = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}
        for (x, y, z), b in list(self.blocks.items()):
            if b[0] != "minecraft:ladder":
                continue
            dx, dz = back[b[1]["facing"]]
            w = self.blocks.get((x + dx, y, z + dz))
            if w and "glass" in w[0]:
                fill = STONE
                for yy in (y - 1, y + 1, y - 2, y + 2):
                    o = self.blocks.get((x + dx, yy, z + dz))
                    if o and "glass" not in o[0] and o[0] != "minecraft:air" and not o[1]:
                        fill = o[0]
                        break
                self.put(x + dx, y, z + dz, fill)

    def rug(self, x0, x1, z0, z1, y, edge, field):
        """A carpet rug: one colour round the edge, another in the middle; only on bare floor."""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if (x, y, z) in self.blocks and self.blocks[(x, y, z)][0] != "minecraft:air":
                    continue
                c = edge if x in (x0, x1) or z in (z0, z1) else field
                self.put(x, y, z, f"minecraft:{c}_carpet")

    def wall_banner(self, x, y, z, facing, color):
        self.put(x, y, z, f"minecraft:{color}_wall_banner", {"facing": facing})

    # ------------------------------------------------------------ walls
    def walls(self, x0, x1, z0, z1, y0, y1, mat=STONE):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    if x in (x0, x1) or z in (z0, z1):
                        self.put(x, y, z, mat)
                    else:
                        self.air(x, y, z)

    def floor(self, x0, x1, z0, z1, y, mat=FLOOR):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                self.put(x, y, z, mat)

    def stone_storey(self, x0, x1, z0, z1, y0, y1, windows=True):
        """A stone brick storey on a polished andesite course; small square windows, two per side."""
        self.walls(x0, x1, z0, z1, y0, y1)
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    self.put(x, y0, z, PLINTH)
        if windows:
            wy = y0 + 1 if y1 - y0 < 3 else y0 + 2
            for (xa, xb, z) in ((x0, x1, z0), (x0, x1, z1)):
                for x in (xa + 2, xb - 2):
                    if xb - xa >= 6:
                        self.put(x, wy, z, "minecraft:glass_pane", {})
            for (za, zb, x) in ((z0, z1, x0), (z0, z1, x1)):
                for z in (za + 3, zb - 3):
                    if zb - za >= 6:
                        self.put(x, wy, z, "minecraft:glass_pane", {})

    def timber_storey(self, x0, x1, z0, z1, y0, y1, spacing=3):
        """
        White plaster between dark oak posts (at the corners and every few blocks), a stripped dark oak beam along the
        top and bottom of each wall, paper-screen windows in the middle of each bay.
        """
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    if not (x in (x0, x1) or z in (z0, z1)):
                        self.air(x, y, z)
                        continue
                    along = x if z in (z0, z1) else z
                    start = x0 if z in (z0, z1) else z0
                    corner = x in (x0, x1) and z in (z0, z1)
                    post = corner or (along - start) % spacing == 0
                    if post:
                        self.log(x, y, z, kind=POST)
                    elif y in (y0, y1):
                        self.log(x, y, z, axis="x" if z in (z0, z1) else "z", kind=BEAM)
                    elif y == (y0 + y1) // 2 + (0 if (y1 - y0) % 2 == 0 else 1) or (y1 - y0 >= 4 and y == y0 + 2):
                        self.put(x, y, z, SCREEN, {})
                    else:
                        self.put(x, y, z, PLASTER)

    # ------------------------------------------------------------ roofs
    def hip_roof(self, x0, x1, z0, z1, y, mat=TILE, eaves=1, finial=True, soffit=True):
        """
        A hip roof over the walls x0..x1, z0..z1 (their top at y - 1): stair courses stepping in one block a layer from
        an eave `eaves` blocks out, the last ridge capped in slabs. Dark oak trim tucked under the overhang.
        Returns the y of the top.
        """
        stairs, full, slab = mat
        ax0, ax1, az0, az1 = x0 - eaves, x1 + eaves, z0 - eaves, z1 + eaves
        if soffit and eaves:
            for x in range(ax0, ax1 + 1):
                for z in range(az0, az1 + 1):
                    if x < x0 or x > x1 or z < z0 or z > z1:
                        edge = (z < z0 and "south") or (z > z1 and "north") or (x < x0 and "east") or "west"
                        if (x < x0 or x > x1) and (z < z0 or z > z1):
                            continue
                        self.stair(x, y - 1, z, TRIM_STAIRS, edge, half="top")
        i = 0
        while ax0 + i <= ax1 - i and az0 + i <= az1 - i:
            rx0, rx1, rz0, rz1 = ax0 + i, ax1 - i, az0 + i, az1 - i
            yy = y + i
            if rx1 - rx0 <= 0 or rz1 - rz0 <= 0:
                # The ridge (or the peak): slabs along it.
                for x in range(rx0, rx1 + 1):
                    for z in range(rz0, rz1 + 1):
                        self.put(x, yy, z, slab, {"type": "bottom", "waterlogged": "false"})
                if finial and rx1 == rx0 and rz1 == rz0:
                    self.put(rx0, yy, rz0, full)
                    self.put(rx0, yy + 1, rz0, "minecraft:lightning_rod", {"facing": "up", "powered": "false",
                                                                            "waterlogged": "false"})
                i += 1
                break
            for x in range(rx0, rx1 + 1):
                for z in range(rz0, rz1 + 1):
                    if not (x in (rx0, rx1) or z in (rz0, rz1)):
                        continue
                    if z == rz0:
                        f = "south"
                    elif z == rz1:
                        f = "north"
                    elif x == rx0:
                        f = "east"
                    else:
                        f = "west"
                    self.stair(x, yy, z, stairs, f)
            i += 1
        return y + i

    def ring_roof(self, ox0, ox1, oz0, oz1, ix0, ix1, iz0, iz1, y, mat=TILE):
        """
        A roof over a square ring of halls (outer walls ox.., inner walls ix..): it slopes up from both the outer and
        the inner eave to a ridge running round the middle of the ring.
        """
        stairs, full, slab = mat
        width = ix0 - ox0                                 # the ring's thickness (walls included)
        for i in range(0, width // 2 + 2):
            yy = y + i
            o = (ox0 - 1 + i, ox1 + 1 - i, oz0 - 1 + i, oz1 + 1 - i)     # outer course, stepping in
            n = (ix0 + 1 - i, ix1 - 1 + i, iz0 + 1 - i, iz1 - 1 + i)     # inner course, stepping out
            if o[0] >= n[0]:
                # They've met: cap the ridge.
                for (x0, x1, z0, z1) in (o,):
                    for x in range(x0, x1 + 1):
                        for z in range(z0, z1 + 1):
                            if x in (x0, x1) or z in (z0, z1):
                                self.put(x, yy, z, slab, {"type": "bottom", "waterlogged": "false"})
                break
            for (x0, x1, z0, z1), outward in ((o, True), (n, False)):
                for x in range(x0, x1 + 1):
                    for z in range(z0, z1 + 1):
                        if not (x in (x0, x1) or z in (z0, z1)):
                            continue
                        if z == z0:
                            f = "south" if outward else "north"
                        elif z == z1:
                            f = "north" if outward else "south"
                        elif x == x0:
                            f = "east" if outward else "west"
                        else:
                            f = "west" if outward else "east"
                        self.stair(x, yy, z, stairs, f)
        return y + width // 2 + 2

    # ------------------------------------------------------------ furnishing
    def bed(self, x, y, z, facing, color=None):
        """A bed with its foot at (x, y, z), its head one step toward `facing`."""
        color = color or self.rng.choice(BED_COLORS)
        dx, dz = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}[facing]
        self.put(x, y, z, f"minecraft:{color}_bed", {"facing": facing, "part": "foot", "occupied": "false"})
        self.put(x + dx, y, z + dz, f"minecraft:{color}_bed", {"facing": facing, "part": "head", "occupied": "false"})

    def dog(self, x, y, z, variant, stay=None, **extra):
        self.folk(x, y, z, "dog", variant, stay)
        self.entities[-1][3].update(extra)

    def animal(self, x, y, z, kind, **nbt):
        data = {"id": f"minecraft:{kind}", "PersistenceRequired": Byte(1)}
        data.update(nbt)
        self.entities.append((x + 0.5, y + 0.05, z + 0.5, data))

    def stone_lantern(self, x, y, z):
        """A garden lantern of stone: a post, a lantern, a cap."""
        self.put(x, y, z, "minecraft:stone_brick_wall", {"up": "true", "north": "none", "south": "none",
                                                         "east": "none", "west": "none", "waterlogged": "false"})
        self.put(x, y + 1, z, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
        self.put(x, y + 2, z, "minecraft:polished_andesite_slab", {"type": "bottom", "waterlogged": "false"})

    def stool(self, x, y, z):
        self.put(x, y, z, "minecraft:polished_andesite_slab", {"type": "bottom", "waterlogged": "false"})

    def table(self, x, y, z):
        self.fence(x, y, z, "minecraft:dark_oak_fence")
        self.put(x, y + 1, z, "minecraft:dark_oak_pressure_plate", {"powered": "false"})

    def low_table(self, x, y, z):
        self.put(x, y, z, "minecraft:dark_oak_slab", {"type": "bottom", "waterlogged": "false"})

    def bench(self, x, y, z, facing):
        self.stair(x, y, z, "minecraft:spruce_stairs", facing)
