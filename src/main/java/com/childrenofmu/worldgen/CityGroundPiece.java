package com.childrenofmu.worldgen;

import com.childrenofmu.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The ground of an RCB City: every bit of natural ground left between its buildings and streets (yards, alleys,
 * corners) is paved over in the same patchwork as the streets, so there's almost no grass inside the city, with a
 * ragged edge where the city gives out. Alleys get their clutter (barrels, crates, a compost heap, a cobweb) and the
 * odd torch on a wall to keep the place feeling lived in. Laid last, so it only fills what's left.
 */
public class CityGroundPiece extends StructurePiece {
    private final int cx, cz, y, radius;
    /** Inside a town wall: the wall's corners (x, z, x, z...); paving stops a little inside it. Empty: round. */
    private final int[] ring;

    /** An open city: round, with a ragged edge. */
    public CityGroundPiece(int cx, int cz, int y, int radius) {
        this(cx, cz, y, radius, new int[0]);
    }

    /** A walled city: everything inside the wall. */
    public CityGroundPiece(int cx, int cz, int y, int[] ring) {
        this(cx, cz, y, reach(cx, cz, ring), ring);
    }

    private CityGroundPiece(int cx, int cz, int y, int radius, int[] ring) {
        super(ModStructures.CITY_GROUND_PIECE.get(), 0, new BoundingBox(cx - radius - 3, y - StreetPiece.REACH - 8,
                cz - radius - 3, cx + radius + 3, y + StreetPiece.REACH + 8, cz + radius + 3));
        this.cx = cx;
        this.cz = cz;
        this.y = y;
        this.radius = radius;
        this.ring = ring;
    }

    private static int reach(int cx, int cz, int[] ring) {
        int r = 0;
        for (int i = 0; i < ring.length; i += 2) {
            r = Math.max(r, Math.max(Math.abs(ring[i] - cx), Math.abs(ring[i + 1] - cz)));
        }
        return r;
    }

    public CityGroundPiece(CompoundTag tag) {
        super(ModStructures.CITY_GROUND_PIECE.get(), tag);
        this.cx = tag.getInt("CX");
        this.cz = tag.getInt("CZ");
        this.y = tag.getInt("Y");
        this.radius = tag.getInt("R");
        this.ring = tag.getIntArray("Ring");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", this.cx);
        tag.putInt("CZ", this.cz);
        tag.putInt("Y", this.y);
        tag.putInt("R", this.radius);
        tag.putIntArray("Ring", this.ring);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        java.util.List<CityRing.Edge> edges = this.ring.length >= 6 ? CityRing.edges(this.ring) : null;
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                RandomSource r = RandomSource.create(Mth.getSeed(x, 3, z));
                if (edges != null) {
                    if (!CityRing.inside(edges, x + 0.5, z + 0.5)) {
                        continue;
                    }
                } else {
                    double d = Math.sqrt((double) (x - this.cx) * (x - this.cx) + (double) (z - this.cz) * (z - this.cz));
                    if (d > this.radius - 4 + r.nextFloat() * 8) {
                        continue;
                    }
                }
                BlockPos ground = StreetPiece.ground(level, x, z, this.y + 8);
                if (ground == null || !box.isInside(ground)) {
                    continue;
                }
                if (level.getBlockState(ground).is(net.minecraft.tags.BlockTags.SAND)) {
                    continue;      // sand stays sand: the fighting pit's floor (and a beach is a beach)
                }
                for (int up = 1; up <= 2; up++) {
                    BlockPos above = ground.above(up);
                    BlockState a = level.getBlockState(above);
                    if (!a.isAir() && a.canBeReplaced() && a.getFluidState().isEmpty()) {
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                yard(level, box, ground, r);
                clutter(level, box, ground, r);
            }
        }
    }

    /**
     * The trodden ground of yards and back alleys: worn grass, coarse dirt, packed mud and the odd flagstone, with weeds
     * (grass, tall grass, ferns, a dead bush) growing wherever feet don't go.
     */
    private static void yard(WorldGenLevel level, BoundingBox box, BlockPos ground, RandomSource r) {
        int patch = Math.floorMod((int) (Mth.getSeed(ground.getX() >> 2, 9, ground.getZ() >> 2) >>> 10), 10);
        float f = r.nextFloat();
        BlockState top;
        boolean grassy = false;
        if (patch < 4) {
            grassy = f < 0.75F;
            top = (grassy ? Blocks.GRASS_BLOCK : Blocks.COARSE_DIRT).defaultBlockState();
        } else if (patch < 6) {
            top = (f < 0.5F ? Blocks.COARSE_DIRT : f < 0.75F ? Blocks.ROOTED_DIRT : Blocks.GRASS_BLOCK).defaultBlockState();
            grassy = f >= 0.75F;
        } else if (patch < 8) {
            top = (f < 0.6F ? Blocks.PACKED_MUD : f < 0.8F ? Blocks.COARSE_DIRT : Blocks.MUD_BRICKS).defaultBlockState();
        } else {
            top = StreetPiece.paving(ground.getX(), ground.getZ(), r);
        }
        level.setBlock(ground, top, 2);
        BlockPos above = ground.above();
        if (!level.getBlockState(above).isAir() || !box.isInside(above.above())) {
            return;
        }
        float w = r.nextFloat();
        if (grassy) {
            if (w < 0.30F) {
                level.setBlock(above, Blocks.SHORT_GRASS.defaultBlockState(), 2);
            } else if (w < 0.42F && level.getBlockState(above.above()).isAir()) {
                net.minecraft.world.level.block.DoublePlantBlock.placeAt(level,
                        (w < 0.38F ? Blocks.TALL_GRASS : Blocks.LARGE_FERN).defaultBlockState(), above, 2);
            } else if (w < 0.47F) {
                level.setBlock(above, Blocks.FERN.defaultBlockState(), 2);
            } else if (w < 0.49F) {
                level.setBlock(above, (r.nextBoolean() ? Blocks.DANDELION : Blocks.POPPY).defaultBlockState(), 2);
            }
        } else if (top.is(Blocks.COARSE_DIRT) && w < 0.04F) {
            level.setBlock(above, Blocks.DEAD_BUSH.defaultBlockState(), 2);
        }
    }

    /** Against a wall: now and then a barrel, a crate, a compost heap, a cobweb, or a torch. */
    private static void clutter(WorldGenLevel level, BoundingBox box, BlockPos ground, RandomSource r) {
        BlockPos above = ground.above();
        if (!level.getBlockState(above).isAir()) {
            return;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos wall = above.relative(d);
            if (!box.isInside(wall)) {
                continue;
            }
            BlockState w = level.getBlockState(wall);
            if (!w.isSolidRender(level, wall) || StreetPiece.natural(w)) {
                continue;
            }
            float f = r.nextFloat();
            if (f < 0.14F) {
                // Vines creeping up the wall.
                int h = 1 + r.nextInt(4);
                for (int k = 0; k < h; k++) {
                    BlockPos v = above.above(k);
                    if (!box.isInside(v) || !level.getBlockState(v).isAir()
                            || !level.getBlockState(v.relative(d)).isSolidRender(level, v.relative(d))) {
                        break;
                    }
                    level.setBlock(v, Blocks.VINE.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(d), true), 2);
                }
                return;
            }
            if (f < 0.19F) {
                // Only on a wall that's really there at torch height (not a fence or a ledge stepping back).
                BlockPos at = above.above(), on = at.relative(d);
                if (box.isInside(on) && level.getBlockState(at).isAir()
                        && level.getBlockState(on).isFaceSturdy(level, on, d.getOpposite())) {
                    level.setBlock(at, Blocks.WALL_TORCH.defaultBlockState()
                            .setValue(WallTorchBlock.FACING, d.getOpposite()), 2);
                }
            } else if (f < 0.22F) {
                level.setBlock(above, Blocks.BARREL.defaultBlockState(), 2);
            } else if (f < 0.235F) {
                level.setBlock(above, Blocks.COMPOSTER.defaultBlockState(), 2);
            } else if (f < 0.245F) {
                level.setBlock(above, Blocks.COBWEB.defaultBlockState(), 2);
            } else if (f < 0.25F) {
                level.setBlock(above, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), 2);
            }
            return;
        }
    }
}
