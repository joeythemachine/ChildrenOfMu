package com.childrenofmu.worldgen;

import java.util.ArrayList;
import java.util.List;

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
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A crooked city street between two doors of an RCB City, following the lie of the land: along x then z, or z then
 * x. Three blocks of smooth road (packed mud and mud brick flags with patches of tuff and stone, level from side to
 * side, a stone manhole cover over the sewer every so often), a flush stone curb either side with weeds growing up
 * through it, and street lamps on tall posts leaning their lanterns out over the road. It only paves natural ground
 * (grass, dirt, sand, stone), so it never cuts into a building.
 */
public class StreetPiece extends StructurePiece {
    static final int REACH = 14;
    /** A manhole every this many steps (SewerPiece puts its shaft under the same steps). */
    static final int MANHOLE_EVERY = 18;

    private final int x0, z0, x1, z1, y;
    private final boolean xFirst;
    /** The floor of the sewer under this street (no manholes where there's no sewer), or Integer.MIN_VALUE for none. */
    private final int sewerFloor;

    public StreetPiece(int x0, int z0, int x1, int z1, int y, boolean xFirst) {
        this(x0, z0, x1, z1, y, xFirst, Integer.MIN_VALUE);
    }

    public StreetPiece(int x0, int z0, int x1, int z1, int y, boolean xFirst, int sewerFloor) {
        super(ModStructures.STREET_PIECE.get(), 0, new BoundingBox(Math.min(x0, x1) - 3, y - REACH,
                Math.min(z0, z1) - 3, Math.max(x0, x1) + 3, y + REACH, Math.max(z0, z1) + 3));
        this.x0 = x0;
        this.z0 = z0;
        this.x1 = x1;
        this.z1 = z1;
        this.y = y;
        this.xFirst = xFirst;
        this.sewerFloor = sewerFloor;
    }

    public StreetPiece(CompoundTag tag) {
        super(ModStructures.STREET_PIECE.get(), tag);
        this.x0 = tag.getInt("X0");
        this.z0 = tag.getInt("Z0");
        this.x1 = tag.getInt("X1");
        this.z1 = tag.getInt("Z1");
        this.y = tag.getInt("Y");
        this.xFirst = tag.getBoolean("XFirst");
        this.sewerFloor = tag.contains("SewerFloor") ? tag.getInt("SewerFloor") : Integer.MIN_VALUE;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("X0", this.x0);
        tag.putInt("Z0", this.z0);
        tag.putInt("X1", this.x1);
        tag.putInt("Z1", this.z1);
        tag.putInt("Y", this.y);
        tag.putBoolean("XFirst", this.xFirst);
        tag.putInt("SewerFloor", this.sewerFloor);
    }

    /** One step of a route: where it is and which way the street runs there. */
    record Step(int x, int z, boolean alongX, int index) {}

    /** The steps of a route from (ax, az) to (bx, bz), first along x (or z), then the other way. */
    static List<Step> route(int ax, int az, int bx, int bz, boolean xFirst) {
        List<Step> steps = new ArrayList<>();
        int cornerX = xFirst ? bx : ax;
        int cornerZ = xFirst ? az : bz;
        leg(steps, ax, az, cornerX, cornerZ);
        leg(steps, cornerX, cornerZ, bx, bz);
        return steps;
    }

    private static void leg(List<Step> steps, int ax, int az, int bx, int bz) {
        int sx = Integer.signum(bx - ax), sz = Integer.signum(bz - az);
        boolean alongX = sx != 0;
        int x = ax, z = az;
        while (true) {
            steps.add(new Step(x, z, alongX, steps.size()));
            if (x == bx && z == bz) {
                return;
            }
            x += sx;
            z += sz;
        }
    }

    static boolean isManhole(Step s) {
        return s.index() % MANHOLE_EVERY == MANHOLE_EVERY / 2;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        List<Step> steps = route(this.x0, this.z0, this.x1, this.z1, this.xFirst);
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            boolean manhole = false;
            if (isManhole(s) && this.sewerFloor != Integer.MIN_VALUE && i > 0 && i + 1 < steps.size()) {
                // Only over a stretch of sewer that is really there (the same test SewerPiece makes).
                manhole = SewerPiece.covered(level, generator, steps.get(i - 1).x(), steps.get(i - 1).z(), this.sewerFloor)
                        && SewerPiece.covered(level, generator, s.x(), s.z(), this.sewerFloor)
                        && SewerPiece.covered(level, generator, steps.get(i + 1).x(), steps.get(i + 1).z(), this.sewerFloor);
            }
            lay(level, box, s, manhole);
        }
    }

    private void lay(WorldGenLevel level, BoundingBox box, Step s, boolean manhole) {
        // The street is laid level across its width, at the height of the ground in the middle (where the ground
        // either side is within a couple of blocks of it): no steps from one side of the road to the other.
        BlockPos middle = ground(level, s.x(), s.z(), this.y);
        for (int side = -2; side <= 2; side++) {
            int px = s.alongX() ? s.x() : s.x() + side;
            int pz = s.alongX() ? s.z() + side : s.z();
            BlockPos found = ground(level, px, pz, this.y);
            if (found == null) {
                continue;
            }
            int top = middle != null && Math.abs(found.getY() - middle.getY()) <= 2 ? middle.getY() : found.getY();
            BlockPos ground = new BlockPos(px, top, pz);
            if (!box.isInside(ground)) {
                continue;
            }
            for (int yy = found.getY() + 1; yy < top; yy++) {          // build up a dip
                BlockPos q = new BlockPos(px, yy, pz);
                if (level.getBlockState(q).canBeReplaced()) {
                    level.setBlock(q, Blocks.DIRT.defaultBlockState(), 2);
                }
            }
            for (int yy = top + 1; yy <= found.getY(); yy++) {         // cut down a hump
                BlockPos q = new BlockPos(px, yy, pz);
                if (natural(level.getBlockState(q))) {
                    level.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
                }
            }
            RandomSource r = RandomSource.create(Mth.getSeed(px, this.y, pz));
            clearAbove(level, ground);
            if (Math.abs(side) == 2) {
                curb(level, box, ground, r, s, side);
                continue;
            }
            BlockState road;
            if (side == -1 && manhole) {
                road = com.childrenofmu.registry.ModBlocks.MANHOLE.get().defaultBlockState()
                        .setValue(TrapDoorBlock.HALF, Half.TOP)
                        .setValue(TrapDoorBlock.FACING, s.alongX() ? Direction.EAST : Direction.SOUTH);
            } else {
                road = paving(px, pz, r);
            }
            level.setBlock(ground, road, 2);
        }
    }

    /**
     * The road surface, smooth to walk on like an old muddy lane: mostly packed mud and mud brick, with patches of
     * tuff and stone brick and the odd cobble, laid in two-by-two flags so it reads as a path, not as noise.
     */
    static BlockState paving(int x, int z, RandomSource r) {
        int cell = Math.floorMod((int) (Mth.getSeed(x >> 1, 7, z >> 1) >>> 12), 100);
        if (r.nextFloat() < 0.18F) {
            cell = Math.floorMod(cell + 37, 100);        // a stray flag
        }
        if (cell < 30) {
            return Blocks.PACKED_MUD.defaultBlockState();
        } else if (cell < 55) {
            return Blocks.MUD_BRICKS.defaultBlockState();
        } else if (cell < 67) {
            return Blocks.TUFF_BRICKS.defaultBlockState();
        } else if (cell < 79) {
            return (r.nextFloat() < 0.25F ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState();
        } else if (cell < 87) {
            return Blocks.COBBLESTONE.defaultBlockState();
        } else if (cell < 94) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        return Blocks.DIRT_PATH.defaultBlockState();
    }

    /** The curb: a flush edge of stone, broken here and there, with weeds coming up along it and a lamp now and then. */
    private void curb(WorldGenLevel level, BoundingBox box, BlockPos ground, RandomSource r, Step s, int side) {
        BlockPos above = ground.above();
        boolean lampHere = s.index() % 8 == 4 && side == ((s.index() / 8) % 2 == 0 ? 2 : -2);
        if (lampHere && box.isInside(ground.above(5)) && level.getBlockState(above).canBeReplaced()) {
            level.setBlock(ground, Blocks.STONE_BRICKS.defaultBlockState(), 2);
            lamp(level, box, above, s, side);
            return;
        }
        float f = r.nextFloat();
        boolean grassy = f < 0.22F;
        level.setBlock(ground, (grassy ? (r.nextBoolean() ? Blocks.GRASS_BLOCK : Blocks.COARSE_DIRT)
                : f < 0.62F ? Blocks.STONE_BRICKS : f < 0.80F ? Blocks.MOSSY_STONE_BRICKS : Blocks.COBBLESTONE)
                .defaultBlockState(), 2);
        if (level.getBlockState(above).isAir() && box.isInside(above)) {
            float w = r.nextFloat();
            if (grassy && w < 0.55F) {
                level.setBlock(above, (w < 0.4F ? Blocks.SHORT_GRASS : Blocks.FERN).defaultBlockState(), 2);
            } else if (w < 0.08F && box.isInside(above.above()) && level.getBlockState(above.above()).isAir()) {
                level.setBlock(above, Blocks.TALL_GRASS.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,
                                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), 2);
                level.setBlock(above.above(), Blocks.TALL_GRASS.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,
                                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 2);
            }
        }
    }

    /** A street lamp: a tall post with an arm leaning out over the road and a lantern hanging from it. */
    private void lamp(WorldGenLevel level, BoundingBox box, BlockPos base, Step s, int side) {
        BlockState fence = Blocks.DARK_OAK_FENCE.defaultBlockState();
        for (int i = 0; i < 4; i++) {
            level.setBlock(base.above(i), fence, 2);
        }
        int dx = s.alongX() ? 0 : -Integer.signum(side);
        int dz = s.alongX() ? -Integer.signum(side) : 0;
        BlockPos arm = base.above(3).offset(dx, 0, dz);
        if (box.isInside(arm) && level.getBlockState(arm).canBeReplaced()) {
            Direction toArm = Direction.fromDelta(dx, 0, dz);
            level.setBlock(arm, fence.setValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(toArm.getOpposite()), true), 2);
            level.setBlock(base.above(3), fence.setValue(net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(toArm), true), 2);
            BlockPos lantern = arm.below();
            if (level.getBlockState(lantern).canBeReplaced()) {
                level.setBlock(lantern, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
            }
        } else {
            level.setBlock(base.above(4), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), 2);
        }
    }

    private static void clearAbove(WorldGenLevel level, BlockPos ground) {
        for (int up = 1; up <= 2; up++) {
            BlockPos above = ground.above(up);
            BlockState a = level.getBlockState(above);
            if (!a.isAir() && a.canBeReplaced() && a.getFluidState().isEmpty()) {
                level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    /** Natural ground: what a street or the city's paving may pave over. */
    static boolean natural(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.CLAY) || s.is(Blocks.MUD)
                || s.is(Blocks.DIRT_PATH);
    }

    /** The natural ground at this column within REACH of y, if the top of it is natural ground. */
    static BlockPos ground(WorldGenLevel level, int x, int z, int y) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y + REACH - 1, z);
        for (int n = 0; n < REACH * 2 - 1; n++, p.move(0, -1, 0)) {
            BlockState s = level.getBlockState(p);
            if (s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty())) {
                continue;
            }
            return natural(s) ? p.immutable() : null;
        }
        return null;
    }
}
