package com.childrenofmu.worldgen;

import com.childrenofmu.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One straight run of an RCB City's town wall, from bend to bend (straight along a side, or on the diagonal across a
 * cut-off corner): two blocks thick and six high on a footing, old patched stone with merlons along the outer edge and
 * a walk along the top, vines creeping up the outside and a lantern on the inside now and then. Its height runs evenly
 * from one end to the other (the towers and buildings along it stand at the same heights), and it is laid after the
 * buildings: wherever a building or a tower already stands, the wall leaves that column alone, so the buildings
 * themselves become the wall there and every gap between them is closed.
 */
public class CityWallPiece extends StructurePiece {
    static final int HEIGHT = 7;

    private final int x0, z0, y0, x1, z1, y1;
    /** Where towers and gatehouses stand (minX, minZ, maxX, maxZ each): the wall leaves them be, and opens into them. */
    private final int[] keepOut;
    /** 0: RCB City's old weathered wall (moss, cracks, vines). 1: Dog City's: plain stone brick, kept in good repair. */
    private final int style;

    /** y0, y1: the surface height (first air above ground) at each end. */
    public CityWallPiece(int x0, int z0, int y0, int x1, int z1, int y1, int[] keepOut) {
        this(x0, z0, y0, x1, z1, y1, keepOut, 0);
    }

    public CityWallPiece(int x0, int z0, int y0, int x1, int z1, int y1, int[] keepOut, int style) {
        super(ModStructures.CITY_WALL_PIECE.get(), 0, new BoundingBox(Math.min(x0, x1) - 5, Math.min(y0, y1) - 8,
                Math.min(z0, z1) - 5, Math.max(x0, x1) + 5, Math.max(y0, y1) + HEIGHT + 4, Math.max(z0, z1) + 5));
        this.x0 = x0;
        this.z0 = z0;
        this.y0 = y0;
        this.x1 = x1;
        this.z1 = z1;
        this.y1 = y1;
        this.keepOut = keepOut;
        this.style = style;
    }

    public CityWallPiece(CompoundTag tag) {
        super(ModStructures.CITY_WALL_PIECE.get(), tag);
        this.x0 = tag.getInt("X0");
        this.z0 = tag.getInt("Z0");
        this.y0 = tag.getInt("Y0");
        this.x1 = tag.getInt("X1");
        this.z1 = tag.getInt("Z1");
        this.y1 = tag.getInt("Y1");
        this.keepOut = tag.getIntArray("Keep");
        this.style = tag.getInt("Style");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("X0", this.x0);
        tag.putInt("Z0", this.z0);
        tag.putInt("Y0", this.y0);
        tag.putInt("X1", this.x1);
        tag.putInt("Z1", this.z1);
        tag.putInt("Y1", this.y1);
        tag.putIntArray("Keep", this.keepOut);
        tag.putInt("Style", this.style);
    }

    /** This wall's stone: weathered and patched for RCB City, plain stone brick for Dog City. */
    private BlockState stoneFor(RandomSource r) {
        return this.style == 1 ? Blocks.STONE_BRICKS.defaultBlockState() : stone(r);
    }

    static BlockState stone(RandomSource r) {
        float f = r.nextFloat();
        return (f < 0.45F ? Blocks.STONE_BRICKS : f < 0.62F ? Blocks.MOSSY_STONE_BRICKS : f < 0.76F ? Blocks.CRACKED_STONE_BRICKS
                : f < 0.9F ? Blocks.COBBLESTONE : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
    }

    /** Ground the wall may be built into or over. */
    static boolean open(BlockState s) {
        return s.isAir() || s.canBeReplaced() || StreetPiece.natural(s) || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS)
                || !s.getFluidState().isEmpty() || s.is(BlockTags.TRAPDOORS) || s.is(Blocks.WALL_TORCH)
                // Anything growing: flowers, rose bushes, ferns, saplings, roots, mushrooms, vines, berry bushes.
                || s.getBlock() instanceof net.minecraft.world.level.block.BushBlock || s.is(BlockTags.FLOWERS)
                || s.is(BlockTags.REPLACEABLE) || s.getBlock() instanceof net.minecraft.world.level.block.MultifaceBlock
                || s.getBlock() instanceof net.minecraft.world.level.block.VineBlock || s.is(Blocks.BAMBOO)
                || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.CACTUS) || s.is(Blocks.MOSS_CARPET) || s.is(Blocks.SNOW);
                // (a neighbour's window shutters or a torch don't stop the wall or the catwalk)
    }

    /** Something built stands at the foot of this column (a building's floor or wall, a tower): leave it be. */
    private static boolean built(WorldGenLevel level, int x, int z, int ground) {
        // Above the ground only: paving, a yard's packed mud or a path underfoot is no building.
        for (int y = ground + 1; y <= ground + 2; y++) {
            BlockState s = level.getBlockState(new BlockPos(x, y, z));
            if (!open(s) && !isWall(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWall(BlockState s) {
        return s.is(Blocks.STONE_BRICKS) || s.is(Blocks.MOSSY_STONE_BRICKS) || s.is(Blocks.CRACKED_STONE_BRICKS)
                || s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE);
    }

    /** The keep-out box at (x, z), or -1. Each box: minX, minZ, maxX, maxZ, and the standing height of its walk-level floor. */
    private int keptBox(int x, int z) {
        for (int i = 0; i + 4 < this.keepOut.length; i += 5) {
            if (x >= this.keepOut[i] && x <= this.keepOut[i + 2] && z >= this.keepOut[i + 1] && z <= this.keepOut[i + 3]) {
                return i;
            }
        }
        return -1;
    }

    private boolean kept(int x, int z) {
        return keptBox(x, z) >= 0;
    }

    /**
     * Near a tower or gatehouse the wall comes to its walk-level floor, a block at a time (so the walk, and the catwalk
     * beside it, can be walked all the way up to the doorway): the ground y the wall should stand on here, given what
     * it would be otherwise.
     */
    private int nearGround(int x, int z, int ground) {
        int best = ground, bestD = Integer.MAX_VALUE;
        for (int i = 0; i + 4 < this.keepOut.length; i += 5) {
            int d = Math.max(Math.max(this.keepOut[i] - x, x - this.keepOut[i + 2]),
                    Math.max(this.keepOut[i + 1] - z, z - this.keepOut[i + 3]));
            int target = this.keepOut[i + 4] - HEIGHT;
            int reach = 3 + Math.abs(target - ground);
            if (d <= reach && d < bestD) {
                bestD = d;
                int slack = Math.max(0, d - 3);
                best = target + Mth.clamp(ground - target, -slack, slack);
            }
        }
        return best;
    }

    /**
     * Where the wall walk (or the catwalk beside it) runs up against a tower or gatehouse (box i): a neat doorway, one
     * block wide and two high at the tower's walk-level floor, through the face of the tower nearest the walk cell
     * (wx, wz), straight across from it and never at a corner. The walk and the catwalk side by side make a doorway
     * two wide. Each chunk cuts only its own blocks, after its own buildings are down.
     */
    private void doorway(WorldGenLevel level, BoundingBox box, int wx, int wz, int i) {
        int minX = this.keepOut[i], minZ = this.keepOut[i + 1], maxX = this.keepOut[i + 2], maxZ = this.keepOut[i + 3];
        int stand = this.keepOut[i + 4];
        int ox = wx < minX ? minX - wx : wx > maxX ? wx - maxX : 0;
        int oz = wz < minZ ? minZ - wz : wz > maxZ ? wz - maxZ : 0;
        if ((ox == 0 && oz == 0) || Math.max(ox, oz) > 2 || maxX - minX < 2 || maxZ - minZ < 2) {
            return;
        }
        int fx, fz, sx = 0, sz = 0, wx2, wz2;
        if (ox >= oz) {
            fx = wx < minX ? minX : maxX;
            fz = Mth.clamp(wz, minZ, maxZ);
            sx = wx < minX ? -1 : 1;
        } else {
            fz = wz < minZ ? minZ : maxZ;
            fx = Mth.clamp(wx, minX, maxX);
            sz = wz < minZ ? -1 : 1;
        }
        // Never through the wall a ladder hangs on (it would hang in the doorway): open up beside it instead.
        if (ladderBehind(level, fx, fz, sx, sz, stand)) {
            int best = Integer.MAX_VALUE, bx = fx, bz = fz;
            for (int d = -3; d <= 3; d++) {
                int cx = sx != 0 ? fx : fx + d, cz = sx != 0 ? fz + d : fz;
                if (d == 0 || cx <= minX && sx == 0 || cx >= maxX && sx == 0 || cz <= minZ && sz == 0 || cz >= maxZ && sz == 0
                        || ladderBehind(level, cx, cz, sx, sz, stand) || Math.abs(d) >= best) {
                    continue;
                }
                best = Math.abs(d);
                bx = cx;
                bz = cz;
            }
            fx = bx;
            fz = bz;
        }
        // Straight ahead of the walk; at a corner of the tower, two wide (the corner and the next block along the face),
        // so there's a way through into the room.
        wx2 = fx;
        wz2 = fz;
        if (ladderBehind(level, fx, fz, sx, sz, stand)) {
            return;    // nowhere clear of the ladder: leave the wall be
        }
        if (sx != 0 && (fz == minZ || fz == maxZ)) {
            wz2 = fz == minZ ? minZ + 1 : maxZ - 1;
        } else if (sz != 0 && (fx == minX || fx == maxX)) {
            wx2 = fx == minX ? minX + 1 : maxX - 1;
        }
        if ((wx2 != fx || wz2 != fz) && ladderBehind(level, wx2, wz2, sx, sz, stand)) {
            wx2 = fx;
            wz2 = fz;
        }
        if (wx2 != fx || wz2 != fz) {
            for (int y = stand; y <= stand + 1; y++) {
                BlockPos p = new BlockPos(wx2, y, wz2);
                BlockState here = level.getBlockState(p);
                if (box.isInside(p) && !here.isAir() && clearable(here)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
            }
            BlockPos f2 = new BlockPos(wx2, stand - 1, wz2);
            if (box.isInside(f2) && !level.getBlockState(f2).isSolid()) {
                level.setBlock(f2, Blocks.STONE_BRICKS.defaultBlockState(), 2);
            }
            BlockPos in2 = new BlockPos(wx2 - sx, stand - 1, wz2 - sz);
            if (box.isInside(in2) && level.getBlockState(in2).isAir()) {
                level.setBlock(in2, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
            }
            for (int y = stand; y <= stand + 1; y++) {
                BlockPos p = new BlockPos(wx2 - sx, y, wz2 - sz);
                BlockState here = level.getBlockState(p);
                if (box.isInside(p) && !here.isAir() && clearable(here) && isWall(here)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // From the face out to the walk: the opening, and boards underfoot across any gap.
        for (int k = 0; k <= Math.max(ox, oz); k++) {
            int x = fx + sx * k, z = fz + sz * k;
            for (int y = stand; y <= stand + 1; y++) {
                BlockPos p = new BlockPos(x, y, z);
                BlockState here = level.getBlockState(p);
                if (box.isInside(p) && !here.isAir() && clearable(here)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
            }
            BlockPos floor = new BlockPos(x, stand - 1, z);
            if (box.isInside(floor) && !level.getBlockState(floor).isSolid()) {
                level.setBlock(floor, Blocks.STONE_BRICKS.defaultBlockState(), 2);
            }
        }
        // And a floor just inside, if the tower has none there.
        BlockPos in = new BlockPos(fx - sx, stand - 1, fz - sz);
        if (box.isInside(in) && level.getBlockState(in).isAir()) {
            level.setBlock(in, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
        }
    }

    /**
     * The catwalk along the inside of the wall, level with the wall walk: dark oak slabs on brackets, with a fence rail
     * on its inner edge and posts down to the ground now and then, so the Cats can get right round the town. Every so
     * often a flight of stairs climbs to it from the street. (x, z): the wall's outer cell; (ix, iz): inward.
     */
    private boolean catwalk(WorldGenLevel level, BoundingBox box, int x, int z, int t, int ix, int iz, int ground,
                            Direction out) {
        int top = ground + HEIGHT - 1;                    // the slabs' height (standing on them = on the wall walk)
        BlockPos walk = new BlockPos(x + ix * 2, top, z + iz * 2);
        BlockPos rail = new BlockPos(x + ix * 3, top, z + iz * 3);
        if (kept(walk.getX(), walk.getZ()) || kept(rail.getX(), rail.getZ()) || !box.isInside(walk) || !box.isInside(rail)) {
            return false;
        }
        // (Whether there's room is decided before any doorway is cut: a doorway's sill must not crowd out the boards.)
        for (BlockPos c : new BlockPos[]{walk, rail}) {
            for (int yy = 0; yy <= 2; yy++) {
                BlockState st = level.getBlockState(c.above(yy));
                if (!open(st) && !ours(st)) {
                    return false;                         // a building stands here: the walk goes round on the wall top
                }
            }
        }
        // Running up against a tower or gatehouse: a doorway into it from the catwalk too.
        int ex = Integer.signum(this.x1 - this.x0), ez = Integer.signum(this.z1 - this.z0);
        for (int s = -1; s <= 1; s += 2) {
            int i = keptBox(walk.getX() + ex * s, walk.getZ() + ez * s);
            if (i >= 0) {
                doorway(level, box, walk.getX(), walk.getZ(), i);
            }
        }
        boolean flight = Math.floorMod(t, 24) == 12;
        // Boards laid alternately single and double, so the walk has a rhythm to it.
        BlockState slab = Blocks.DARK_OAK_SLAB.defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, Math.floorMod(t, 2) == 0
                        ? net.minecraft.world.level.block.state.properties.SlabType.DOUBLE
                        : net.minecraft.world.level.block.state.properties.SlabType.TOP);
        level.setBlock(walk, rampStair(t, top, Blocks.DARK_OAK_STAIRS.defaultBlockState(), slab), 2);
        level.setBlock(rail, slab, 2);
        level.setBlock(walk.above(), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(walk.above(2), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(rail.above(), flight ? Blocks.AIR.defaultBlockState() : Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);
        if (Math.floorMod(t, 9) == 4) {
            // A lantern hung under the boards (instead of on a bracket from the wall), lighting the street below.
            level.setBlock(walk.below(), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
        } else if (Math.floorMod(t, 2) == 0) {
            BlockPos br = walk.below();
            if (open(level.getBlockState(br))) {
                level.setBlock(br, Blocks.DARK_OAK_STAIRS.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.StairBlock.FACING, out)
                        .setValue(net.minecraft.world.level.block.StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP), 2);
            }
        }
        if (Math.floorMod(t, 4) == 0) {
            for (int yy = top - 1; yy > ground - 2; yy--) {
                BlockPos p = new BlockPos(rail.getX(), yy, rail.getZ());
                if (!open(level.getBlockState(p))) {
                    break;
                }
                level.setBlock(p, Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);    // a post down to the ground
            }
        }
        // A flight of stairs up from the street beside the rail every so often, climbing along the wall to an opening in
        // the rail. Each step is laid with the stretch of catwalk beside it (so a flight crossing a chunk edge is whole).
        int j = Math.floorMod(12 - t, 24);
        int n = Math.max(Math.abs(this.x1 - this.x0), Math.abs(this.z1 - this.z0));
        if (j < HEIGHT - 1 && t + j <= n) {
            int dx = Integer.signum(this.x1 - this.x0), dz = Integer.signum(this.z1 - this.z0);
            Direction up = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
            int topThere = Math.round(Mth.lerp((float) (t + j) / Math.max(1, n), this.y0, this.y1)) - 1 + HEIGHT - 1;
            BlockPos st = new BlockPos(x + ix * 4, topThere - j, z + iz * 4);
            if (box.isInside(st) && open(level.getBlockState(st)) && st.getY() > ground) {
                level.setBlock(st, Blocks.DARK_OAK_STAIRS.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.StairBlock.FACING, up), 2);
                for (int yy = 1; yy <= 3; yy++) {
                    BlockPos h = st.above(yy);
                    if (box.isInside(h) && open(level.getBlockState(h)) && !level.getBlockState(h).isAir()) {
                        level.setBlock(h, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                for (int yy = st.getY() - 1; yy > st.getY() - 12; yy--) {
                    BlockPos p = new BlockPos(st.getX(), yy, st.getZ());
                    if (!box.isInside(p) || !open(level.getBlockState(p))) {
                        break;
                    }
                    level.setBlock(p, Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
                }
            }
        }
        return true;
    }

    /**
     * The catwalk across a cut-off corner (the wall on the diagonal): a band of boards stepping along inside the wall,
     * so it still joins up with the catwalks along the sides, a rail on its inner edge.
     */
    private boolean diagonalCatwalk(WorldGenLevel level, BoundingBox box, int x, int z, int t, int ix, int iz, int ground) {
        int top = ground + HEIGHT - 1;
        BlockPos[] walk = {new BlockPos(x + ix, top, z + iz), new BlockPos(x + ix * 2, top, z + iz),
                new BlockPos(x + ix, top, z + iz * 2)};
        BlockPos rail = new BlockPos(x + ix * 2, top, z + iz * 2);
        for (BlockPos c : new BlockPos[]{walk[0], walk[1], walk[2], rail}) {
            if (kept(c.getX(), c.getZ())) {
                return false;
            }
        }
        boolean any = false;
        BlockState slab = Blocks.DARK_OAK_SLAB.defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, Math.floorMod(t, 2) == 0
                        ? net.minecraft.world.level.block.state.properties.SlabType.DOUBLE
                        : net.minecraft.world.level.block.state.properties.SlabType.TOP);
        for (BlockPos c : walk) {
            if (!box.isInside(c) || !open(level.getBlockState(c)) || !open(level.getBlockState(c.above()))
                    || !open(level.getBlockState(c.above(2)))) {
                continue;
            }
            level.setBlock(c, slab, 2);
            level.setBlock(c.above(), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(c.above(2), Blocks.AIR.defaultBlockState(), 2);
            any = true;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int b = keptBox(c.getX() + d.getStepX(), c.getZ() + d.getStepZ());
                if (b >= 0) {
                    doorway(level, box, c.getX(), c.getZ(), b);
                }
            }
        }
        if (any && box.isInside(rail) && open(level.getBlockState(rail)) && open(level.getBlockState(rail.above()))) {
            level.setBlock(rail, slab, 2);
            level.setBlock(rail.above(), Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);
        }
        if (any && Math.floorMod(t, 9) == 4 && box.isInside(walk[0].below()) && open(level.getBlockState(walk[0].below()))) {
            level.setBlock(walk[0].below(), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
        }
        return any;
    }

    /**
     * Where a building stands against the wall and the catwalk can't get past it: a covered timber gallery along the
     * wall top instead, beside the building's back wall. The catwalk's boards carry on over the wall walk (level with
     * it, so you just step across), fence posts stand on the merlons, and a roof of dark oak slabs runs over the top,
     * with a lantern hung in a crenel now and then. (x, z): the wall's outer cell; (ix, iz): inward.
     */
    /** The wall walk's top block height at step t along this wall (as postProcess works it out). */
    private int topAt(int t) {
        int dx = this.x1 - this.x0, dz = this.z1 - this.z0;
        int n = Math.max(Math.abs(dx), Math.abs(dz));
        t = Mth.clamp(t, 0, n);
        int x = this.x0 + Math.round((float) dx * t / Math.max(1, n));
        int z = this.z0 + Math.round((float) dz * t / Math.max(1, n));
        int ground = Math.round(Mth.lerp((float) t / Math.max(1, n), this.y0, this.y1)) - 1;
        return nearGround(x, z, ground) + HEIGHT - 1;
    }

    /**
     * Where the walk climbs a block from one cell to the next (coming up to a tower): a stair as this cell's top block
     * if it's the higher of the two, so nobody has to jump; otherwise the given block.
     */
    private BlockState rampStair(int t, int top, BlockState stair, BlockState otherwise) {
        int prev = topAt(t - 1), next = topAt(t + 1);
        int dx = Integer.signum(this.x1 - this.x0), dz = Integer.signum(this.z1 - this.z0);
        Direction along = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
        boolean fromBehind = prev == top - 1, fromAhead = next == top - 1;
        if (fromBehind == fromAhead) {
            return otherwise;
        }
        return stair.setValue(net.minecraft.world.level.block.StairBlock.FACING, fromBehind ? along : along.getOpposite());
    }

    /** A ladder on the inside of the tower wall at (x, z) (facing out of the wall (sx, sz)), at or about head height. */
    private static boolean ladderBehind(WorldGenLevel level, int x, int z, int sx, int sz, int stand) {
        for (int y = stand - 1; y <= stand + 2; y++) {
            if (level.getBlockState(new BlockPos(x - sx, y, z - sz)).is(Blocks.LADDER)
                    || level.getBlockState(new BlockPos(x, y, z)).is(Blocks.LADDER)) {
                return true;
            }
        }
        return false;
    }

    /** The wall's own timber (another stretch's boards or rail, where two stretches meet at a corner): can be relaid. */
    private static boolean ours(BlockState s) {
        return s.is(Blocks.DARK_OAK_FENCE) || s.is(Blocks.DARK_OAK_SLAB) || s.is(Blocks.DARK_OAK_STAIRS)
                || s.is(Blocks.LANTERN);
    }

    /** Within d blocks of a tower or gatehouse. */
    private boolean nearKept(int x, int z, int d) {
        for (int i = 0; i + 4 < this.keepOut.length; i += 5) {
            if (outside(x, z, this.keepOut[i], this.keepOut[i + 1], this.keepOut[i + 2], this.keepOut[i + 3]) <= d) {
                return true;
            }
        }
        return false;
    }

    private static boolean clearable(BlockState s) {
        return !s.hasBlockEntity() && !s.is(Blocks.LADDER) && !(s.getBlock() instanceof net.minecraft.world.level.block.DoorBlock)
                && !(s.getBlock() instanceof net.minecraft.world.level.block.BedBlock);
    }

    /**
     * Where a building stands against the wall and the catwalk can't get past it: a covered timber gallery along the
     * wall top instead, beside the building's back wall. The catwalk's boards carry on over the wall walk (level with
     * it, so you just step across), with stairs where the walk climbs a block, fence posts on the merlons, and a roof of
     * dark oak slabs high enough to jump up a step under. A lantern hangs in a crenel now and then. (x, z): the wall's
     * outer cell; (ix, iz): inward.
     */
    private void gallery(WorldGenLevel level, BoundingBox box, int x, int z, int t, int ix, int iz, int ground) {
        int top = ground + HEIGHT - 1;
        BlockPos in = new BlockPos(x + ix, top, z + iz);
        BlockPos outer = new BlockPos(x, top, z);
        BlockPos cat = new BlockPos(x + ix * 2, top, z + iz * 2);
        if (!box.isInside(in)) {
            return;
        }
        boolean blocked = false;
        for (int yy = 0; yy <= 2; yy++) {
            if (!open(level.getBlockState(cat.above(yy)))) {
                blocked = true;
            }
        }
        if (!blocked) {
            return;                                           // not a building in the way: nothing to go round
        }
        // A building's back wall standing right on the wall walk (going on up past head height): no way through.
        if (!open(level.getBlockState(in.above(3))) && !open(level.getBlockState(in.above(1)))) {
            return;
        }
        // The walk itself: boards on the wall top, and nothing (a building's footing, a stray stair) in the way.
        BlockState under = level.getBlockState(in);
        if (!isWall(under) && !open(under) && !under.isSolid()) {
            return;
        }
        for (int yy = 1; yy <= 2; yy++) {
            BlockState s = level.getBlockState(in.above(yy));
            if (!s.isAir() && clearable(s)) {
                level.setBlock(in.above(yy), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        int dx = Integer.signum(this.x1 - this.x0), dz = Integer.signum(this.z1 - this.z0);
        Direction along = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
        int prev = topAt(t - 1), next = topAt(t + 1);
        level.setBlock(in, rampStair(t, top, Blocks.DARK_OAK_STAIRS.defaultBlockState(), Blocks.DARK_OAK_SLAB.defaultBlockState()
                .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, Math.floorMod(t, 2) == 0
                        ? net.minecraft.world.level.block.state.properties.SlabType.DOUBLE
                        : net.minecraft.world.level.block.state.properties.SlabType.TOP)), 2);
        // The roof: a clear three blocks over the highest boards hereabouts.
        int roofY = Math.max(top, Math.max(prev, next)) + 4;
        boolean merlon = isWall(level.getBlockState(outer.above()));
        for (int yy = outer.getY() + 2; yy < roofY; yy++) {
            BlockPos post = new BlockPos(outer.getX(), yy, outer.getZ());
            if (!box.isInside(post) || !open(level.getBlockState(post))) {
                break;
            }
            if (yy == roofY - 1 && Math.floorMod(t, 9) == 4) {
                level.setBlock(post, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
            } else if (merlon) {
                level.setBlock(post, Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);
            }
        }
        for (BlockPos r : new BlockPos[]{new BlockPos(in.getX(), roofY, in.getZ()), new BlockPos(outer.getX(), roofY, outer.getZ())}) {
            if (box.isInside(r) && open(level.getBlockState(r))) {
                level.setBlock(r, Blocks.DARK_OAK_SLAB.defaultBlockState(), 2);
            }
        }
    }

    private static int outside(int x, int z, int minX, int minZ, int maxX, int maxZ) {
        return Math.max(0, Math.max(Math.max(minX - x, x - maxX), Math.max(minZ - z, z - maxZ)));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int dx = this.x1 - this.x0, dz = this.z1 - this.z0;
        int n = Math.max(Math.abs(dx), Math.abs(dz));
        int ix = -Integer.signum(dz), iz = Integer.signum(dx);          // inward
        Direction out = Math.abs(dz) >= Math.abs(dx) ? (ix > 0 ? Direction.WEST : Direction.EAST)
                : (iz > 0 ? Direction.NORTH : Direction.SOUTH);
        int[][] cells = ix != 0 && iz != 0 ? new int[][]{{0, 0}, {ix, 0}, {0, iz}} : new int[][]{{0, 0}, {ix, iz}};
        for (int t = 0; t <= n; t++) {
            int x = this.x0 + Math.round((float) dx * t / Math.max(1, n));
            int z = this.z0 + Math.round((float) dz * t / Math.max(1, n));
            int ground = Math.round(Mth.lerp((float) t / Math.max(1, n), this.y0, this.y1)) - 1;
            ground = nearGround(x, z, ground);
            boolean keptHere = kept(x, z);
            int walkX = x + cells[cells.length - 1][0], walkZ = z + cells[cells.length - 1][1];
            if (!keptHere && t + 1 <= n) {
                int nx = this.x0 + Math.round((float) dx * (t + 1) / Math.max(1, n));
                int nz = this.z0 + Math.round((float) dz * (t + 1) / Math.max(1, n));
                int ahead = keptBox(nx, nz);
                if (ahead >= 0) {
                    doorway(level, box, walkX, walkZ, ahead);          // into the tower ahead
                }
            }
            if (!keptHere && t > 0) {
                int px = this.x0 + Math.round((float) dx * (t - 1) / Math.max(1, n));
                int pz = this.z0 + Math.round((float) dz * (t - 1) / Math.max(1, n));
                int behind = keptBox(px, pz);
                if (behind >= 0) {
                    doorway(level, box, walkX, walkZ, behind);         // into the tower behind
                }
            }
            for (int k = 0; k < cells.length; k++) {
                int cx = x + cells[k][0], cz = z + cells[k][1];
                if (cx < box.minX() || cx > box.maxX() || cz < box.minZ() || cz > box.maxZ()) {
                    continue;
                }
                if (kept(cx, cz) || built(level, cx, cz, ground)) {
                    continue;
                }
                boolean outer = k == 0;
                for (int yy = ground - 3; yy <= ground + HEIGHT; yy++) {
                    BlockPos p = new BlockPos(cx, yy, cz);
                    if (!box.isInside(p) || !open(level.getBlockState(p))) {
                        continue;
                    }
                    RandomSource r = RandomSource.create(Mth.getSeed(cx, yy, cz));
                    if (yy == ground + HEIGHT - 1 && !outer && (ix == 0 || iz == 0)) {
                        level.setBlock(p, rampStair(t, yy, Blocks.STONE_BRICK_STAIRS.defaultBlockState(), stoneFor(r)), 2);
                    } else if (yy < ground + HEIGHT) {
                        level.setBlock(p, stoneFor(r), 2);
                    } else if (outer && Math.floorMod(t, 2) == 0 && !nearKept(cx, cz, 2)) {
                        level.setBlock(p, stoneFor(r), 2);                 // a merlon (none by a tower's doorway)
                    } else if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                // The wall walk (the inner half of the wall top): two clear blocks to walk in. Anything a neighbour left
                // poking into it goes (a stair, a beam end, a lamp), unless it's a building's own wall carrying on up.
                if (!outer) {
                    BlockPos s2 = new BlockPos(cx, ground + HEIGHT + 2, cz), s3 = s2.above();
                    boolean wallOfBuilding = box.isInside(s2) && !open(level.getBlockState(s2)) && !open(level.getBlockState(s3));
                    for (int yy = ground + HEIGHT; yy <= ground + HEIGHT + 1; yy++) {
                        BlockPos p = new BlockPos(cx, yy, cz);
                        BlockState here = level.getBlockState(p);
                        // A building's wall carrying on up is left; its roof eave, a stair or a beam end hanging into the
                        // walk goes regardless.
                        boolean eave = !here.isCollisionShapeFullBlock(level, p) || here.is(net.minecraft.tags.BlockTags.STAIRS)
                                || here.is(net.minecraft.tags.BlockTags.SLABS)
                                || net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(here.getBlock()).getPath().contains("shingle");
                        if (box.isInside(p) && !here.isAir() && clearable(here) && (eave || !wallOfBuilding)
                                && !here.getCollisionShape(level, p).isEmpty()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
                // Clear anything growing on the wall walk.
                for (int yy = ground + HEIGHT + 1; yy <= ground + HEIGHT + 2; yy++) {
                    BlockPos p = new BlockPos(cx, yy, cz);
                    if (box.isInside(p) && open(level.getBlockState(p)) && !level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                RandomSource r = RandomSource.create(Mth.getSeed(cx, 11, cz));
                if (outer && this.style == 0 && r.nextFloat() < 0.3F) {          // (no ivy on a well-kept wall)
                    int h = 2 + r.nextInt(4);
                    for (int yy = ground + HEIGHT - 1; yy > ground + HEIGHT - 1 - h; yy--) {
                        BlockPos v = new BlockPos(cx, yy, cz).relative(out);
                        if (box.isInside(v) && level.getBlockState(v).isAir()) {
                            level.setBlock(v, Blocks.VINE.defaultBlockState()
                                    .setValue(VineBlock.getPropertyForFace(out.getOpposite()), true), 2);
                        }
                    }
                }
                boolean walked = false;
                if (k == cells.length - 1 && !keptHere) {
                    walked = ix == 0 || iz == 0 ? catwalk(level, box, x, z, t, ix, iz, ground, out)
                            : diagonalCatwalk(level, box, x, z, t, ix, iz, ground);
                }
                if (k == cells.length - 1 && !keptHere && !walked && (ix == 0 || iz == 0)) {
                    gallery(level, box, x, z, t, ix, iz, ground);
                }
                if (k == cells.length - 1 && Math.floorMod(t, 9) == 4 && !walked) {
                    BlockPos lamp = new BlockPos(cx + ix, ground + 3, cz + iz);
                    BlockPos arm = lamp.above();
                    if (box.isInside(arm) && level.getBlockState(arm).isAir() && level.getBlockState(lamp).isAir()) {
                        level.setBlock(arm, Blocks.IRON_BARS.defaultBlockState()
                                .setValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(out), true), 2);
                        level.setBlock(lamp, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
                    }
                }
            }
        }
    }
}
