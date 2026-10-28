package com.childrenofmu.worldgen;

import com.childrenofmu.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A covered timber skywalk across a street, from an upper floor of one building to an upper floor of the one opposite:
 * a boarded walk between log beams, fence rails either side, posts holding up a little roof of dark oak, a lantern
 * hanging underneath over the street, and a doorway cut through each building's wall. If the two floors aren't quite
 * level it takes a step or two in the middle.
 */
public class SkywalkPiece extends StructurePiece {
    /** The wall blocks at each end, cut through two high from their y (the standing height of each floor). */
    private final BlockPos a, b;

    public SkywalkPiece(BlockPos a, BlockPos b) {
        super(ModStructures.SKYWALK_PIECE.get(), 0, box(a, b));
        this.a = a;
        this.b = b;
    }

    public SkywalkPiece(CompoundTag tag) {
        super(ModStructures.SKYWALK_PIECE.get(), tag);
        this.a = new BlockPos(tag.getInt("AX"), tag.getInt("AY"), tag.getInt("AZ"));
        this.b = new BlockPos(tag.getInt("BX"), tag.getInt("BY"), tag.getInt("BZ"));
    }

    private static BoundingBox box(BlockPos a, BlockPos b) {
        boolean alongX = a.getZ() == b.getZ();
        return new BoundingBox(Math.min(a.getX(), b.getX()) - (alongX ? 0 : 1), Math.min(a.getY(), b.getY()) - 2,
                Math.min(a.getZ(), b.getZ()) - (alongX ? 1 : 0), Math.max(a.getX(), b.getX()) + (alongX ? 0 : 1),
                Math.max(a.getY(), b.getY()) + 3, Math.max(a.getZ(), b.getZ()) + (alongX ? 1 : 0));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("AX", this.a.getX());
        tag.putInt("AY", this.a.getY());
        tag.putInt("AZ", this.a.getZ());
        tag.putInt("BX", this.b.getX());
        tag.putInt("BY", this.b.getY());
        tag.putInt("BZ", this.b.getZ());
    }

    /** The floor heights (the block underfoot) of each cell between the walls, stepping in the middle if need be. */
    static int[] floors(int n, int fa, int fb) {
        int[] f = new int[n];
        int k = Math.abs(fb - fa), s = Integer.signum(fb - fa);
        int start = (n - k) / 2;
        for (int t = 0; t < n; t++) {
            int stepped = Math.max(0, Math.min(k, t - start + 1));
            f[t] = fa + s * stepped;
        }
        return f;
    }

    /** Where the lantern hangs under the walk: the middle, or the nearest level cell to it. */
    static int lampAt(int[] f) {
        int n = f.length;
        for (int d = 0; d < n; d++) {
            for (int t : new int[]{n / 2 - d, n / 2 + d}) {
                if (t >= 1 && t + 1 < n && f[t] == f[t - 1] && f[t] == f[t + 1]) {
                    return t;
                }
            }
        }
        return -1;
    }

    private void set(WorldGenLevel level, BoundingBox box, BlockPos p, BlockState s) {
        if (box.isInside(p)) {
            level.setBlock(p, s, 2);
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        boolean alongX = this.a.getZ() == this.b.getZ();
        Direction dir = alongX ? (this.b.getX() > this.a.getX() ? Direction.EAST : Direction.WEST)
                : (this.b.getZ() > this.a.getZ() ? Direction.SOUTH : Direction.NORTH);
        Direction left = dir.getCounterClockWise(), right = dir.getClockWise();
        int n = Math.abs(alongX ? this.b.getX() - this.a.getX() : this.b.getZ() - this.a.getZ()) - 1;
        int[] f = floors(n, this.a.getY() - 1, this.b.getY() - 1);
        // The deck is all top slabs: the same footing as a full block, but half a block more headroom over the street.
        BlockState plank = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState beam = Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState rail = Blocks.DARK_OAK_FENCE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState slab = Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        int lamp = lampAt(f);
        for (int t = 0; t < n; t++) {
            BlockPos c = this.a.relative(dir, t + 1);
            int y = f[t];
            boolean post = t == 0 || t == n - 1 || t % 3 == 0;
            // The boards, or a step where the walk climbs (or drops) a block.
            BlockPos floor = new BlockPos(c.getX(), y, c.getZ());
            if (t > 0 && f[t] == f[t - 1] + 1) {
                set(level, box, floor, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, dir));
            } else if (t + 1 < n && f[t + 1] == f[t] - 1) {
                set(level, box, floor, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, dir.getOpposite()));
            } else {
                set(level, box, floor, plank);
            }
            for (int yy = y + 1; yy <= y + 2; yy++) {
                set(level, box, new BlockPos(c.getX(), yy, c.getZ()), air);
            }
            set(level, box, new BlockPos(c.getX(), y + 3, c.getZ()), slab);
            for (Direction side : new Direction[]{left, right}) {
                BlockPos s = new BlockPos(c.getX(), y, c.getZ()).relative(side);
                set(level, box, s, beam);
                set(level, box, s.above(), rail);
                // Lanterns hang from the eaves at the middle, over the rails (out of the way, and not under the deck).
                set(level, box, s.above(2), t == lamp
                        ? Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true) : post ? rail : air);
                set(level, box, s.above(3), Blocks.DARK_OAK_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING, side.getOpposite()).setValue(StairBlock.HALF, Half.BOTTOM));
            }
        }
        // The doorways at either end.
        for (BlockPos w : new BlockPos[]{this.a, this.b}) {
            for (int yy = 0; yy <= 1; yy++) {
                BlockPos p = w.above(yy);
                // Through whatever the wall is made of, timber frames (which carry block entities) included.
                if (box.isInside(p)) {
                    level.setBlock(p, air, 2);
                }
            }
        }
    }
}
