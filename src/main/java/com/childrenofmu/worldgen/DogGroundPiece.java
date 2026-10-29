package com.childrenofmu.worldgen;

import com.childrenofmu.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Dog City's roads, laid last over whatever open grass is left: stone brick, edged in polished andesite, in a strict
 * grid. A square road rings the Keep; a road runs out along each axis to the door of that axis's tower; each quarter
 * is a block of four plots with roads round it and a road through the middle; and the avenue from the gate splits
 * round the tower by the gate and joins again before the Keep. Lamp posts stand along the way.
 */
public class DogGroundPiece extends StructurePiece {
    private final int cx, cz, cy, turn;

    public DogGroundPiece(int cx, int cz, int cy, int turn) {
        super(ModStructures.DOG_GROUND_PIECE.get(), 0, new BoundingBox(cx - 80, cy - 7, cz - 80, cx + 80, cy + 26, cz + 80));
        this.cx = cx;
        this.cz = cz;
        this.cy = cy;
        this.turn = turn;
    }

    public DogGroundPiece(CompoundTag tag) {
        super(ModStructures.DOG_GROUND_PIECE.get(), tag);
        this.cx = tag.getInt("CX");
        this.cz = tag.getInt("CZ");
        this.cy = tag.getInt("CY");
        this.turn = tag.getInt("Turn");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", this.cx);
        tag.putInt("CZ", this.cz);
        tag.putInt("CY", this.cy);
        tag.putInt("Turn", this.turn);
    }

    private static boolean in(int v, int lo, int hi) {
        return v >= lo && v <= hi;
    }

    /** Is the plan cell (lx, lz) road? (Plan coordinates: gate to the south, +z.) */
    static boolean road(int lx, int lz) {
        int a = Math.abs(lx), b = Math.abs(lz);
        int m = Math.max(a, b);
        if (in(m, 25, 27)) {
            return true;                                                     // round the Keep
        }
        if (a >= 26 && b >= 26 && a <= 56 && b <= 56) {                      // the quarters' grid
            if (in(a, 26, 28) || in(a, 42, 44) || in(b, 26, 28) || in(b, 42, 44)) {
                return true;
            }
            for (int c : DogCityStructure.PLOTS) {
                if (in(a, c - 1, c + 1) && b == 41) {
                    return true;                                             // a path up to a near-row door
                }
            }
            return false;
        }
        int t = DogCityStructure.TOWER_AT;
        if (lz > 0 && a <= 12) {                                             // the avenue from the gate
            if (a <= 1 && (in(lz, 28, 39) || in(lz, 55, DogCityStructure.GATE_AT - DogCityStructure.GATE_HALF))) {
                return true;
            }
            if (in(a, 9, 11) && in(lz, 37, 57)) {
                return true;                                                 // the two ways round the gate tower
            }
            return a <= 11 && (in(lz, 37, 39) || in(lz, 55, 57));
        }
        return (a <= 1 && in(b, 28, t - 8)) || (b <= 1 && in(a, 28, t - 8));  // out to each tower's door
    }

    // ---------------------------------------------------------------- the plan: where the lawns are

    /** Inside the star of the wall, at least `margin` blocks in (plan coordinates). */
    static boolean insideWalls(double lx, double lz, double margin) {
        int[][] st = DogCityStructure.STAR;
        boolean in = false;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < st.length; i++) {
            int[] a = st[i], b = st[(i + 1) % st.length];
            if ((a[1] > lz) != (b[1] > lz) && lx < a[0] + (lz - a[1]) * (double) (b[0] - a[0]) / (b[1] - a[1])) {
                in = !in;
            }
            double dx = b[0] - a[0], dz = b[1] - a[1];
            double t = Math.max(0, Math.min(1, ((lx - a[0]) * dx + (lz - a[1]) * dz) / (dx * dx + dz * dz)));
            best = Math.min(best, Math.hypot(a[0] + t * dx - lx, a[1] + t * dz - lz));
        }
        return in && best >= margin;
    }

    /** Within `pad` blocks of a building of the plan (Keep, towers, gatehouse, workshops). */
    static boolean nearBuilding(int lx, int lz, int pad) {
        int a = Math.abs(lx), b = Math.abs(lz);
        if (a <= 25 + pad && b <= 25 + pad) {
            return true;                                                     // the Keep and its stair
        }
        int t = DogCityStructure.TOWER_AT;
        if ((a <= 8 + pad && Math.abs(b - t) <= 8 + pad) || (b <= 8 + pad && Math.abs(a - t) <= 8 + pad)) {
            return true;                                                     // the towers
        }
        if (a <= 7 + pad && Math.abs(lz - DogCityStructure.GATE_AT) <= 7 + pad) {
            return true;                                                     // the gatehouse
        }
        for (int u : DogCityStructure.PLOTS) {
            for (int v : DogCityStructure.PLOTS) {
                if (Math.abs(a - u) <= 7 + pad && Math.abs(b - v) <= 7 + pad) {
                    return true;                                             // a workshop
                }
            }
        }
        return false;
    }

    /** Ground the levelling left: earth, stone, ore, sand, gravel, snow (and holes in it: air, water). */
    private static boolean natural(BlockState s) {
        return s.isAir() || !s.getFluidState().isEmpty() || StreetPiece.natural(s) || s.is(BlockTags.DIRT)
                || s.is(net.neoforged.neoforge.common.Tags.Blocks.ORES) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.ICE)
                || s.is(Blocks.PACKED_ICE) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.MOSSY_COBBLESTONE);
    }

    /** Loose stuff above the lawn that shouldn't be there: leaves, wood, bushes, rocks, snow. */
    private static boolean junk(BlockState s) {
        return !s.isAir() && (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced() || natural(s)
                || s.getBlock() instanceof net.minecraft.world.level.block.BushBlock
                || s.getBlock() instanceof net.minecraft.world.level.block.MultifaceBlock
                || s.getBlock() instanceof net.minecraft.world.level.block.VineBlock || s.is(Blocks.BAMBOO)
                || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.MUSHROOM_STEM) || s.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || s.is(Blocks.RED_MUSHROOM_BLOCK) || s.is(Blocks.SNOW));
    }

    // ---------------------------------------------------------------- the trees on the lawns

    static final int TREE_GRID = 7;

    /** A tree for this grid cell (plan coordinates): {lx, lz, kind 0 spruce/1 oak/2 birch, trunk height}, or null. */
    private int[] tree(int gx, int gz, boolean taiga) {
        long h = net.minecraft.util.Mth.getSeed(gx * 31 + this.cx, 7, gz * 17 + this.cz);
        java.util.Random r = new java.util.Random(h);
        if (r.nextFloat() < 0.35F) {
            return null;
        }
        int lx = gx * TREE_GRID + r.nextInt(TREE_GRID - 2) + 1, lz = gz * TREE_GRID + r.nextInt(TREE_GRID - 2) + 1;
        if (!insideWalls(lx, lz, 9) || nearBuilding(lx, lz, 3)) {
            return null;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (road(lx + dx, lz + dz)) {
                    return null;                                             // not in the road, nor right beside it
                }
            }
        }
        int kind = taiga ? 0 : r.nextFloat() < 0.6F ? 1 : 2;
        int height = switch (kind) {
            case 0 -> 10 + r.nextInt(8);                                     // spruce: 10 to 17
            case 1 -> 8 + r.nextInt(5);                                      // oak: 8 to 12
            default -> 9 + r.nextInt(5);                                     // birch: 9 to 13
        };
        return new int[]{lx, lz, kind, height};
    }

    /** Draw the part of a tree that falls in this chunk. No leaves in its lowest six blocks (clear underneath). */
    private void drawTree(WorldGenLevel level, BoundingBox box, int[] t, int ground) {
        int[] w = DogCityStructure.turn(t[0], t[1], this.turn);
        int bx = this.cx + w[0], bz = this.cz + w[1];
        int kind = t[2], h = t[3];
        BlockState log = (kind == 0 ? Blocks.SPRUCE_LOG : kind == 1 ? Blocks.OAK_LOG : Blocks.BIRCH_LOG).defaultBlockState();
        BlockState leaf = (kind == 0 ? Blocks.SPRUCE_LEAVES : kind == 1 ? Blocks.OAK_LEAVES : Blocks.BIRCH_LEAVES)
                .defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        java.util.Random r = new java.util.Random(net.minecraft.util.Mth.getSeed(bx, h, bz));
        for (int dy = 7; dy <= h + 2; dy++) {
            double rad = switch (kind) {
                case 0 -> dy > h ? 0.6 : Math.max(0.6, (h + 1 - dy) * 0.36 + ((dy % 2 == 0) ? 0.6 : 0.0));   // tiers
                case 1 -> Math.max(0.0, 3.0 - Math.abs(dy - (h - 1)) * 0.75);                                 // a crown
                default -> dy >= h - 3 ? (dy > h ? 1.0 : 2.0) : 0.0;                                          // narrow
            };
            int ri = (int) Math.ceil(rad);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > rad + 0.3 || (d > rad - 0.4 && r.nextFloat() < 0.25F)) {
                        continue;
                    }
                    p.set(bx + dx, ground + dy, bz + dz);
                    if (box.isInside(p) && level.getBlockState(p).isAir()) {
                        level.setBlock(p, leaf, 2);
                    }
                }
            }
        }
        for (int dy = 1; dy <= h; dy++) {
            p.set(bx, ground + dy, bz);
            if (box.isInside(p)) {
                level.setBlock(p, log, 2);
            }
        }
        p.set(bx, ground, bz);
        if (box.isInside(p)) {
            level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int top = this.cy - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // First the lawns inside the walls: holes filled, the surface made good earth and grass, anything loose above
        // it (leaves left hanging, bushes, rocks, stone) taken away. Buildings, the wall, the Keep are left alone.
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int[] l = DogCityStructure.turn(x - this.cx, z - this.cz, -this.turn);
                // (Only the Keep's own footprint is skipped: the road round it, just outside, gets its holes filled too.)
                if (!insideWalls(l[0] + 0.5, l[1] + 0.5, 0.5) || (Math.abs(l[0]) <= 24 && Math.abs(l[1]) <= 24)) {
                    continue;
                }
                for (int y = top - 8; y <= top + 2; y++) {
                    p.set(x, y, z);
                    if (box.isInside(p) && level.getFluidState(p).is(net.minecraft.tags.FluidTags.LAVA)) {
                        level.setBlock(p, (y <= top ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);   // no lava
                    }
                }
                p.set(x, top, z);
                if (!box.isInside(p) || !natural(level.getBlockState(p))) {
                    continue;                                                // something built stands here
                }
                for (int y = top - 1; y >= top - 5; y--) {
                    p.set(x, y, z);
                    BlockState below = level.getBlockState(p);
                    if (box.isInside(p) && natural(below) && !below.is(BlockTags.DIRT)) {
                        level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);    // earth under the lawn, pits filled
                    }
                }
                level.setBlock(p.set(x, top, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = top + 1; y <= top + 24; y++) {
                    p.set(x, y, z);
                    if (box.isInside(p) && junk(level.getBlockState(p))) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        // Then the trees, on the plan's lawns (every chunk agrees where, so a tree's crown can cross chunk lines).
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome = level.getBiome(new BlockPos(this.cx, top, this.cz));
        String bn = biome.unwrapKey().map(k -> k.location().getPath()).orElse("");
        boolean taiga = bn.contains("taiga") || bn.contains("snowy") || bn.contains("grove");
        int[] c0 = DogCityStructure.turn(box.minX() - 4 - this.cx, box.minZ() - 4 - this.cz, -this.turn);
        int[] c1 = DogCityStructure.turn(box.maxX() + 4 - this.cx, box.maxZ() + 4 - this.cz, -this.turn);
        int gx0 = Math.floorDiv(Math.min(c0[0], c1[0]), TREE_GRID), gx1 = Math.floorDiv(Math.max(c0[0], c1[0]), TREE_GRID);
        int gz0 = Math.floorDiv(Math.min(c0[1], c1[1]), TREE_GRID), gz1 = Math.floorDiv(Math.max(c0[1], c1[1]), TREE_GRID);
        for (int gx = gx0; gx <= gx1; gx++) {
            for (int gz = gz0; gz <= gz1; gz++) {
                int[] t = tree(gx, gz, taiga);
                if (t != null) {
                    drawTree(level, box, t, top);
                }
            }
        }
        postProcessRoads(level, box);
    }

    private void postProcessRoads(WorldGenLevel level, BoundingBox box) {
        int top = this.cy - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int[] l = DogCityStructure.turn(x - this.cx, z - this.cz, -this.turn);
                p.set(x, top, z);
                if (!box.isInside(p)) {
                    continue;
                }
                BlockState ground = level.getBlockState(p);
                boolean open = ground.is(BlockTags.DIRT) || ground.is(Blocks.GRASS_BLOCK) || ground.is(BlockTags.SAND)
                        || ground.is(Blocks.GRAVEL) || ground.is(Blocks.STONE) || ground.is(Blocks.SNOW_BLOCK);
                if (!open) {
                    continue;                                                // a building, the wall, the moat
                }
                if (road(l[0], l[1])) {
                    boolean edge = !road(l[0] + 1, l[1]) || !road(l[0] - 1, l[1]) || !road(l[0], l[1] + 1)
                            || !road(l[0], l[1] - 1);
                    level.setBlock(p, (edge ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS).defaultBlockState(), 2);
                    for (int up = 1; up <= 2; up++) {
                        BlockPos q = p.above(up);
                        if (box.isInside(q) && level.getBlockState(q).canBeReplaced()) {
                            level.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                } else if (ground.is(BlockTags.DIRT) && !ground.is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);   // tidy lawns, not bare earth
                }
                // Lamp posts beside the roads, evenly spaced.
                boolean beside = road(l[0] + 1, l[1]) || road(l[0] - 1, l[1]);       // a road running along z
                boolean ahead = road(l[0], l[1] + 1) || road(l[0], l[1] - 1);        // a road running along x
                if (!road(l[0], l[1]) && ((beside && !ahead && Math.floorMod(l[1], 8) == 0)
                        || (ahead && !beside && Math.floorMod(l[0], 8) == 0))
                        && level.getBlockState(p.above()).canBeReplaced() && box.isInside(p.above(3))) {
                    level.setBlock(p.above(), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
                    level.setBlock(p.above(2), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
                    level.setBlock(p.above(3), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), 2);
                }
            }
        }
    }
}
