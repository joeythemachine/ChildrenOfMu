package com.childrenofmu.worldgen;

import com.childrenofmu.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A plank boardwalk, three wide, laid over the water between two points of a Frog town: first along x, then along z.
 * Posts drop to the swamp floor every few steps, lantern posts stand along one side, and lily pads float alongside.
 * It only fills open water, air and plants, so it never cuts into a building.
 */
public class BoardwalkPiece extends StructurePiece {
    private static final int MAX_POST = 16;

    private final int x0, z0, x1, z1, y;

    public BoardwalkPiece(int x0, int z0, int x1, int z1, int y) {
        super(ModStructures.BOARDWALK_PIECE.get(), 0, new BoundingBox(Math.min(x0, x1) - 3, y - MAX_POST,
                Math.min(z0, z1) - 3, Math.max(x0, x1) + 3, y + 3, Math.max(z0, z1) + 3));
        this.x0 = x0;
        this.z0 = z0;
        this.x1 = x1;
        this.z1 = z1;
        this.y = y;
    }

    public BoardwalkPiece(CompoundTag tag) {
        super(ModStructures.BOARDWALK_PIECE.get(), tag);
        this.x0 = tag.getInt("X0");
        this.z0 = tag.getInt("Z0");
        this.x1 = tag.getInt("X1");
        this.z1 = tag.getInt("Z1");
        this.y = tag.getInt("Y");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("X0", this.x0);
        tag.putInt("Z0", this.z0);
        tag.putInt("X1", this.x1);
        tag.putInt("Z1", this.z1);
        tag.putInt("Y", this.y);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int step = 0;
        // Along x...
        int sx = Integer.signum(this.x1 - this.x0);
        for (int x = this.x0; ; x += sx) {
            lay(level, box, x, this.z0, true, step++);
            if (x == this.x1) {
                break;
            }
        }
        // ...then along z.
        int sz = Integer.signum(this.z1 - this.z0);
        for (int z = this.z0 + sz; sz != 0; z += sz) {
            lay(level, box, this.x1, z, false, step++);
            if (z == this.z1) {
                break;
            }
        }
    }

    /** One step of the walk: a three-wide row of planks across the direction of travel. */
    private void lay(WorldGenLevel level, BoundingBox box, int x, int z, boolean alongX, int step) {
        for (int side = -3; side <= 3; side++) {
            int px = alongX ? x : x + side;
            int pz = alongX ? z + side : z;
            BlockPos deck = new BlockPos(px, this.y, pz);
            if (!box.isInside(deck)) {
                continue;
            }
            RandomSource r = RandomSource.create(Mth.getSeed(px, this.y, pz));
            if (Math.abs(side) >= 2) {
                // Lily pads drifting beside the walk.
                if (r.nextFloat() < 0.14F && level.getBlockState(deck).isAir()
                        && level.getFluidState(deck.below()).isSource()) {
                    level.setBlock(deck, Blocks.LILY_PAD.defaultBlockState(), 2);
                }
                continue;
            }
            if (!open(level.getBlockState(deck))) {
                continue;
            }
            level.setBlock(deck, (side == 0 ? Blocks.OAK_PLANKS : Blocks.SPRUCE_PLANKS).defaultBlockState(), 2);
            BlockPos above = deck.above();
            // Headroom: wild leaves hanging over the walk are cut back (three blocks up).
            for (int up = 1; up <= 3; up++) {
                BlockPos h = deck.above(up);
                if (box.isInside(h) && IthkBuildingPiece.wildLeaves(level.getBlockState(h))) {
                    level.setBlock(h, Blocks.AIR.defaultBlockState(), 2);
                }
            }
            if (!level.getBlockState(above).isAir() && open(level.getBlockState(above))) {
                level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
            }
            if (Math.abs(side) == 1 && step % 4 == 0) {
                post(level, deck);
            }
            if (side == 1 && step % 8 == 4 && level.getBlockState(above).isAir()) {
                level.setBlock(above, Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);
                level.setBlock(above.above(), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), 2);
            }
        }
    }

    /** A log post from under the deck down to the swamp floor. */
    private void post(WorldGenLevel level, BlockPos deck) {
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockPos.MutableBlockPos q = deck.mutable();
        for (int n = 0; n < MAX_POST; n++) {
            q.move(0, -1, 0);
            if (!open(level.getBlockState(q))) {
                break;
            }
            level.setBlock(q, log, 2);
        }
    }

    private static boolean open(BlockState state) {
        return state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty();
    }
}
