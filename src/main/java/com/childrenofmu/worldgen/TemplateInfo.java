package com.childrenofmu.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * What a building template looks like, worked out once from its blocks and remembered: where its walls actually are
 * (the template is usually bigger than the building), and where a skywalk could meet it: places on an upper floor's
 * outside wall with a room behind, a floor underfoot and nothing overhead outside.
 */
public final class TemplateInfo {
    /** A spot on an outside wall, in template coordinates: the wall block a doorway would be cut through (two high from y). */
    public record Anchor(BlockPos wall, Direction out) {}

    public final int minX, minZ, maxX, maxZ;
    public final List<Anchor> anchors;

    private TemplateInfo(int minX, int minZ, int maxX, int maxZ, List<Anchor> anchors) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.anchors = anchors;
    }

    private static final Map<StructureTemplate, TemplateInfo> CACHE = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static TemplateInfo of(StructureTemplate template) {
        TemplateInfo info = CACHE.get(template);
        if (info == null) {
            info = compute(template);
            CACHE.put(template, info);
        }
        return info;
    }

    private static TemplateInfo compute(StructureTemplate template) {
        CompoundTag tag = template.save(new CompoundTag());
        ListTag sizeTag = tag.getList("size", Tag.TAG_INT);
        int sx = sizeTag.getInt(0), sy = sizeTag.getInt(1), sz = sizeTag.getInt(2);
        ListTag palette = tag.contains("palette") ? tag.getList("palette", Tag.TAG_COMPOUND)
                : tag.getList("palettes", Tag.TAG_LIST).getList(0);
        String[] names = new String[palette.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = palette.getCompound(i).getString("Name");
        }
        String[][][] grid = new String[sx][sy][sz];
        int minX = sx, minZ = sz, maxX = -1, maxZ = -1;
        ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag b = blocks.getCompound(i);
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            int x = pos.getInt(0), y = pos.getInt(1), z = pos.getInt(2);
            int st = b.getInt("state");
            if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz || st < 0 || st >= names.length) {
                continue;
            }
            String n = names[st];
            grid[x][y][z] = n;
            if (!isAir(n) && !n.equals("minecraft:structure_void")) {
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        if (maxX < 0) {
            return new TemplateInfo(0, 0, sx - 1, sz - 1, List.of());
        }
        return new TemplateInfo(minX, minZ, maxX, maxZ, anchors(grid, sx, sy, sz));
    }

    private static boolean isAir(String n) {
        return n != null && n.endsWith(":air");
    }

    private static String at(String[][][] g, int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= g.length || y >= g[0].length || z >= g[0][0].length) {
            return null;
        }
        return g[x][y][z];
    }

    /** Built: something there (not air, not nothing). */
    private static boolean solid(String n) {
        return n != null && !isAir(n);
    }

    /** Open: nothing there, or air. */
    private static boolean open(String n) {
        return n == null || isAir(n);
    }

    /** A real floor (boards, a beam, stone), not a bed or a table standing on one. */
    private static boolean isFloor(String n) {
        return solid(n) && (n.contains("planks") || n.contains("_log") || n.contains("bricks") || n.contains("stone")
                || n.contains("andesite") || n.contains("tiles"));
    }

    /** A plain piece of wall a doorway may be cut through. */
    private static boolean wallish(String n) {
        if (!solid(n)) {
            return false;
        }
        for (String k : new String[]{"door", "ladder", "sign", "stairs", "slab", "fence", "lantern", "torch", "bars",
                "chain", "carpet", "bed", "barrel", "campfire", "banner", "vine", "shingle", "lamp", "pot", "candle",
                "bell", "sack", "bookshelf", "lectern", "chest", "furnace", "table", "anvil", "button", "lever"}) {
            if (n.contains(k)) {
                return false;
            }
        }
        return true;
    }

    private static boolean glassy(String n) {
        return n != null && (n.contains("glass") || n.contains("pane"));
    }

    /** Solid, glassless wall from the floor beam up past head height, beside a doorway. */
    private static boolean plainWall(String[][][] g, int x, int y, int z) {
        for (int yy = y - 1; yy <= y + 2; yy++) {
            String n = at(g, x, yy, z);
            if (!solid(n) || glassy(n) || (yy >= y && yy <= y + 1 && !wallish(n))) {
                return false;
            }
        }
        return true;
    }

    private static List<Anchor> anchors(String[][][] g, int sx, int sy, int sz) {
        List<Anchor> out = new ArrayList<>();
        Map<Long, List<BlockPos>> runs = new java.util.HashMap<>();
        for (int y = 5; y + 3 < sy; y++) {
            for (int x = 1; x < sx - 1; x++) {
                for (int z = 1; z < sz - 1; z++) {
                    if (!wallish(at(g, x, y, z)) || !wallish(at(g, x, y + 1, z)) || !solid(at(g, x, y + 2, z))
                            || !solid(at(g, x, y - 1, z))) {
                        continue;
                    }
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        int dx = d.getStepX(), dz = d.getStepZ();
                        // Room behind: two of air over a floor.
                        String floor = at(g, x - dx, y - 1, z - dz);
                        if (!isAir(at(g, x - dx, y, z - dz)) || !isAir(at(g, x - dx, y + 1, z - dz)) || !isFloor(floor)) {
                            continue;
                        }
                        // Wall carrying on, flat and plain, to either side (not a corner, not a window: the walk's rails
                        // and roof butt up against it), over a proper floor beam at the threshold (not a bay window's
                        // sill sticking out of the front).
                        int lx = -dz, lz = dx;
                        if (!isFloor(at(g, x, y - 1, z)) || !plainWall(g, x + lx, y, z + lz) || !plainWall(g, x - lx, y, z - lz)) {
                            continue;
                        }
                        // Nothing outside, three wide, from the floor to well over head height, all the way out.
                        boolean clear = true;
                        for (int k = 1; clear && x + dx * k >= 0 && x + dx * k < sx && z + dz * k >= 0 && z + dz * k < sz; k++) {
                            for (int l = -1; l <= 1 && clear; l++) {
                                for (int yy = y - 1; yy <= y + 3; yy++) {
                                    if (!open(at(g, x + dx * k + lx * l, yy, z + dz * k + lz * l))) {
                                        clear = false;
                                        break;
                                    }
                                }
                            }
                        }
                        if (clear) {
                            int plane = d.getAxis() == Direction.Axis.X ? x : z;
                            long key = ((long) y << 20) | ((long) plane << 4) | d.get2DDataValue();
                            runs.computeIfAbsent(key, k -> new ArrayList<>()).add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
        }
        // Every spot along each stretch of wall (the skywalks pick the ones that line up across a street), but not the
        // very ends of a stretch.
        for (Map.Entry<Long, List<BlockPos>> e : runs.entrySet()) {
            List<BlockPos> list = e.getValue();
            Direction d = Direction.from2DDataValue((int) (e.getKey() & 0xF));
            list.sort((a, b) -> d.getAxis() == Direction.Axis.X ? Integer.compare(a.getZ(), b.getZ())
                    : Integer.compare(a.getX(), b.getX()));
            for (int i = list.size() > 2 ? 1 : 0; i < (list.size() > 2 ? list.size() - 1 : list.size()); i++) {
                out.add(new Anchor(list.get(i), d));
            }
        }
        return out;
    }
}
