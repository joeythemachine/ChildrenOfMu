"""
Shared building toolkit for the Ithk structures: placing blocks, common pieces (ground, crystals, pendants),
and the automatic stair/slab tapering pass.
"""
import math
import random

from nbt_structure import Byte, write_structure

AIR = "minecraft:air"
AMETHYST = "minecraft:amethyst_block"
GLOW = ("minecraft:pearlescent_froglight", {"axis": "y"})

# Full block -> (stairs, slab). Only these get tapered.
SHAPED = {
    "minecraft:stone_bricks": ("minecraft:stone_brick_stairs", "minecraft:stone_brick_slab"),
    "minecraft:cracked_stone_bricks": ("minecraft:stone_brick_stairs", "minecraft:stone_brick_slab"),
    "minecraft:tuff_bricks": ("minecraft:tuff_brick_stairs", "minecraft:tuff_brick_slab"),
    "minecraft:polished_tuff": ("minecraft:polished_tuff_stairs", "minecraft:polished_tuff_slab"),
    "minecraft:polished_deepslate": ("minecraft:polished_deepslate_stairs", "minecraft:polished_deepslate_slab"),
    "minecraft:deepslate_tiles": ("minecraft:deepslate_tile_stairs", "minecraft:deepslate_tile_slab"),
    "minecraft:deepslate_bricks": ("minecraft:deepslate_brick_stairs", "minecraft:deepslate_brick_slab"),
    "minecraft:cobbled_deepslate": ("minecraft:cobbled_deepslate_stairs", "minecraft:cobbled_deepslate_slab"),
    AMETHYST: ("childrenofmu:amethyst_stairs", "childrenofmu:amethyst_slab"),
    "minecraft:stone": ("minecraft:stone_stairs", "minecraft:stone_slab"),
    "minecraft:andesite": ("minecraft:andesite_stairs", "minecraft:andesite_slab"),
    "minecraft:polished_andesite": ("minecraft:polished_andesite_stairs", "minecraft:polished_andesite_slab"),
    "minecraft:quartz_block": ("minecraft:quartz_stairs", "minecraft:quartz_slab"),
    "minecraft:smooth_quartz": ("minecraft:smooth_quartz_stairs", "minecraft:smooth_quartz_slab"),
    "minecraft:mud_bricks": ("minecraft:mud_brick_stairs", "minecraft:mud_brick_slab"),
    "minecraft:purpur_block": ("minecraft:purpur_stairs", "minecraft:purpur_slab"),
    "minecraft:end_stone_bricks": ("minecraft:end_stone_brick_stairs", "minecraft:end_stone_brick_slab"),
    "minecraft:polished_blackstone_bricks": ("minecraft:polished_blackstone_brick_stairs",
                                             "minecraft:polished_blackstone_brick_slab"),
}

# Things that aren't solid, full blocks (light passes, and they don't count as "covering" a face).
NOT_SOLID_WORDS = ("air", "chain", "cluster", "bud", "vines", "rod", "roots", "poppy", "tulip", "rose_bush",
                   "mushroom", "carpet", "stairs", "slab", "lectern", "chest", "enchanting", "lantern", "candle",
                   "hanging_roots")

DIRS = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0)}
OPPOSITE = {"north": "south", "south": "north", "west": "east", "east": "west"}
CCW = {"north": "west", "west": "south", "south": "east", "east": "north"}


def is_solid(name):
    return not any(w in name for w in NOT_SOLID_WORDS)


class Builder:
    def __init__(self, size, seed):
        self.size = size
        self.c = size // 2
        self.rng = random.Random(seed)
        self.blocks = {}
        self.entities = []
        self.no_taper = set()   # blocks that must keep their full shape (floors, things holding decorations)

    # ------------------------------------------------------------ placing
    def put(self, x, y, z, name, props=None, replace=True, keep_shape=False):
        if not (0 <= x < self.size and 0 <= z < self.size and y >= 0):
            return
        if not replace and (x, y, z) in self.blocks:
            return
        self.blocks[(x, y, z)] = (name, props or {})
        if keep_shape:
            self.no_taper.add((x, y, z))

    def air(self, x, y, z):
        self.put(x, y, z, AIR)

    def name_at(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0] if b else None

    def chest(self, x, y, z, facing, loot):
        self.blocks[(x, y, z)] = ("minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"},
                                  {"id": "minecraft:chest", "LootTable": loot})

    def cluster(self, x, y, z, facing, size="amethyst_cluster"):
        if (x, y, z) in self.blocks and self.blocks[(x, y, z)][0] != AIR:
            return
        self.put(x, y, z, f"minecraft:{size}", {"facing": facing, "waterlogged": "false"})
        # The block it grows from must stay a full block.
        dx, dy, dz = {"up": (0, -1, 0), "down": (0, 1, 0), "north": (0, 0, 1), "south": (0, 0, -1),
                      "east": (-1, 0, 0), "west": (1, 0, 0)}[facing]
        self.no_taper.add((x + dx, y + dy, z + dz))

    def ithk(self, x, y, z, **nbt):
        data = {"id": "childrenofmu:ithk", "PersistenceRequired": Byte(1)}
        data.update(nbt)
        self.entities.append((x + 0.5, y + 0.5, z + 0.5, data))

    # ------------------------------------------------------------ shapes
    @staticmethod
    def oct_in(dx, dz, r):
        dx, dz = abs(dx), abs(dz)
        return dx <= r and dz <= r and dx + dz <= r * 1.4

    @staticmethod
    def circ_in(dx, dz, r):
        return dx * dx + dz * dz <= (r + 0.35) ** 2

    def each(self, r, shape=None):
        shape = shape or self.oct_in
        for dx in range(-r - 1, r + 2):
            for dz in range(-r - 1, r + 2):
                if shape(dx, dz, r):
                    yield dx, dz

    # ------------------------------------------------------------ common pieces
    def ground(self, radius, y=0):
        """
        The mud and red plants are no longer baked into the template: the game reworks the real ground under each
        building when it generates (IthkBuildingPiece), so the land keeps its shape. This only draws the same
        random numbers as before, so every building's other random details stay exactly as they were.
        """
        rng = self.rng
        for dx in range(-radius, radius + 1):
            for dz in range(-radius, radius + 1):
                if math.hypot(dx, dz) + rng.uniform(-1.5, 1.0) > radius:
                    continue
                rng.random()
                rng.random()

    def pendant(self, x, y_top, z, chain_len, glow=True):
        """A chain with a small crystal hanging from it; its heart glows."""
        for i in range(chain_len):
            self.put(x, y_top - i, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        self.no_taper.add((x, y_top + 1, z))
        y = y_top - chain_len
        for i in range(3):
            if glow and i == 1:
                self.put(x, y - i, z, *GLOW, keep_shape=True)
            else:
                self.put(x, y - i, z, AMETHYST, keep_shape=True)
        for fx, fz, f in ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north")):
            self.cluster(x + fx, y - 1, z + fz, f, "large_amethyst_bud")
        self.cluster(x, y - 3, z, "down")

    def floating_crystal(self, cx, cy, cz, r=2):
        """A detached octahedral crystal hovering in the air."""
        for dx in range(-r, r + 1):
            for dy in range(-r * 2, r * 2 + 1):
                for dz in range(-r, r + 1):
                    if abs(dx) + abs(dz) + abs(dy) / 2.0 <= r:
                        self.put(cx + dx, cy + dy, cz + dz, AMETHYST)
        self.cluster(cx, cy + r * 2 + 1, cz, "up")
        self.cluster(cx, cy - r * 2 - 1, cz, "down")
        # A glowing heart, showing through at the widest point.
        for dx, dz in ((r, 0), (-r, 0), (0, r), (0, -r)):
            self.put(cx + dx, cy, cz + dz, *GLOW, keep_shape=True)

    def lantern(self, x, y_top, z, chain_len=1, soul=False):
        """A lantern hanging on a chain below the block at y_top + 1."""
        for i in range(chain_len):
            self.put(x, y_top - i, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        self.put(x, y_top - chain_len, z, "minecraft:soul_lantern" if soul else "minecraft:lantern",
                 {"hanging": "true", "waterlogged": "false"})
        self.no_taper.add((x, y_top + 1, z))

    def end_rod(self, x, y, z, facing="up"):
        if (x, y, z) in self.blocks and self.blocks[(x, y, z)][0] != AIR:
            return
        self.put(x, y, z, "minecraft:end_rod", {"facing": facing})
        dx, dy, dz = {"up": (0, -1, 0), "down": (0, 1, 0)}[facing]
        self.no_taper.add((x + dx, y + dy, z + dz))

    def candle(self, x, y, z, count=3, color="purple"):
        self.put(x, y, z, f"minecraft:{color}_candle", {"candles": str(count), "lit": "true", "waterlogged": "false"})
        self.no_taper.add((x, y - 1, z))

    def spike(self, x, y, z, height, base_r=1, up=True, material=AMETHYST, tip=True):
        """A tapering crystal spike, pointing up (or down)."""
        s = 1 if up else -1
        for k in range(height):
            t = k / max(1, height - 1)
            r = int(round(base_r * (1 - t) ** 0.8))
            for dx, dz in self.each(r, self.circ_in if r > 1 else self.oct_in):
                self.put(x + dx, y + s * k, z + dz, material)
        if tip:
            self.cluster(x, y + s * height, z, "up" if up else "down")

    def vines(self, y_min, y_max, fraction=9):
        """Weeping vines hanging from overhangs between y_min and y_max."""
        rng = self.rng
        cands = [(x, y, z) for (x, y, z), b in self.blocks.items()
                 if y_min <= y <= y_max and is_solid(b[0]) and "glass" not in b[0] and (x, y - 1, z) not in self.blocks]
        rng.shuffle(cands)
        for x, y, z in cands[: len(cands) // fraction]:
            length = rng.randint(1, 5)
            if y - length <= 3 or any((x, y - i, z) in self.blocks for i in range(1, length + 1)):
                continue
            self.no_taper.add((x, y, z))
            for i in range(1, length):
                self.put(x, y - i, z, "minecraft:weeping_vines_plant")
            self.put(x, y - length, z, "minecraft:weeping_vines", {"age": "25"})

    # ------------------------------------------------------------ tapering
    def exterior_air(self):
        """Every position outside the structure that's reachable through non-solid space."""
        ys = [p[1] for p in self.blocks]
        y0, y1 = -1, max(ys) + 2
        lo, hi = -1, self.size
        solid = {p for p, b in self.blocks.items() if is_solid(b[0])}
        seen = set()
        stack = [(lo, y1, lo)]
        while stack:
            p = stack.pop()
            if p in seen:
                continue
            x, y, z = p
            if not (lo <= x <= hi and y0 <= y <= y1 and lo <= z <= hi) or p in solid:
                continue
            seen.add(p)
            stack.extend(((x + 1, y, z), (x - 1, y, z), (x, y + 1, z), (x, y - 1, z), (x, y, z + 1), (x, y, z - 1)))
        return seen

    def taper(self, y_min=1):
        """
        Softens exposed edges: a block with open air above (or below) and open air beside it becomes a stair
        stepping down toward the open side (upside-down underneath overhangs), or a slab where both sides are open.
        """
        outside = self.exterior_air()
        changes = {}
        for p, b in self.blocks.items():
            name = b[0]
            if name not in SHAPED or p in self.no_taper or p[1] < y_min or len(b) > 2:
                continue
            x, y, z = p
            up, down = (x, y + 1, z) in outside, (x, y - 1, z) in outside
            if not (up or down) or (up and down and False):
                continue
            open_sides = [d for d, (dx, _, dz) in DIRS.items() if (x + dx, y, z + dz) in outside]
            if not open_sides:
                continue
            half = "bottom" if up else "top"
            stairs, slab = SHAPED[name]
            if len(open_sides) == 1 or (len(open_sides) == 2 and OPPOSITE[open_sides[0]] != open_sides[1]):
                changes[p] = (stairs, {"facing": OPPOSITE[open_sides[0]], "half": half,
                                       "shape": "straight", "waterlogged": "false"})
            else:
                changes[p] = (slab, {"type": half, "waterlogged": "false"})
        self.blocks.update(changes)
        self._fix_stair_shapes(changes.keys())
        return len(changes)

    def _fix_stair_shapes(self, positions):
        """Minecraft normally works out corner shapes when stairs are placed; structures need them precomputed."""
        def stair(p):
            b = self.blocks.get(p)
            stairlike = b and (b[0].endswith("_stairs") or b[0] in ("domum_ornamentum:shingle", "domum_ornamentum:shingle_steep",
                                                                     "domum_ornamentum:shingle_steep_lower"))
            return b if stairlike else None

        def can_take(state, p, face):
            other = stair(tuple(a + d for a, d in zip(p, DIRS[face])))
            return other is None or other[1]["facing"] != state[1]["facing"] or other[1]["half"] != state[1]["half"]

        updates = {}
        for p in positions:
            st = stair(p)
            if not st:
                continue
            facing, half = st[1]["facing"], st[1]["half"]
            shape = "straight"
            front = stair(tuple(a + d for a, d in zip(p, DIRS[facing])))
            back = stair(tuple(a + d for a, d in zip(p, DIRS[OPPOSITE[facing]])))
            if front and front[1]["half"] == half:
                f2 = front[1]["facing"]
                if f2 not in (facing, OPPOSITE[facing]) and can_take(st, p, OPPOSITE[f2]):
                    shape = "outer_left" if f2 == CCW[facing] else "outer_right"
            if shape == "straight" and back and back[1]["half"] == half:
                b2 = back[1]["facing"]
                if b2 not in (facing, OPPOSITE[facing]) and can_take(st, p, b2):
                    shape = "inner_left" if b2 == CCW[facing] else "inner_right"
            if shape != "straight":
                props = dict(st[1])
                props["shape"] = shape
                updates[p] = (st[0], props) + tuple(st[2:])     # keep any block entity data
        self.blocks.update(updates)

    # ------------------------------------------------------------ output
    def save(self, path):
        top = max(y for _, y, _ in self.blocks) + 1
        n, p = write_structure(path, (self.size, top, self.size), self.blocks, self.entities)
        return n, p, top
