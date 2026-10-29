package com.childrenofmu.worldgen;

import java.util.List;

import com.childrenofmu.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Dog City's wall corners, laid after the wall runs. Where two runs meet (the star's tips and notches), and where the
 * diagonal runs come up against the gatehouse, the runs' boards, rails and parapets crossed each other and left holes;
 * here the whole band of wall and catwalk round each such point becomes one solid stone landing, its top level with
 * the walk, merlons along its outer edge, two clear blocks above it.
 */
public class DogCornerPiece extends StructurePiece {
    /** How far a landing reaches from its point (blocks), and how deep into the city the band goes. */
    static final int REACH = 4;
    static final double BAND = 3.6;

    private final int top;                 // the landing's top block (the walk stands on it)
    private final int[] ring;              // the star (its outer wall face)
    private final int[] points;            // x, z of each landing
    private final int[] keepOut;           // the gatehouse: left alone

    public DogCornerPiece(int top, int[] ring, int[] points, int[] keepOut) {
        super(ModStructures.DOG_CORNER_PIECE.get(), 0, box(top, points));
        this.top = top;
        this.ring = ring;
        this.points = points;
        this.keepOut = keepOut;
    }

    public DogCornerPiece(CompoundTag tag) {
        super(ModStructures.DOG_CORNER_PIECE.get(), tag);
        this.top = tag.getInt("Top");
        this.ring = tag.getIntArray("Ring");
        this.points = tag.getIntArray("Points");
        this.keepOut = tag.getIntArray("Keep");
    }

    private static BoundingBox box(int top, int[] points) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < points.length; i += 2) {
            minX = Math.min(minX, points[i]);
            maxX = Math.max(maxX, points[i]);
            minZ = Math.min(minZ, points[i + 1]);
            maxZ = Math.max(maxZ, points[i + 1]);
        }
        return new BoundingBox(minX - REACH - 1, top - CityWallPiece.HEIGHT - 4, minZ - REACH - 1, maxX + REACH + 1,
                top + 4, maxZ + REACH + 1);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("Top", this.top);
        tag.putIntArray("Ring", this.ring);
        tag.putIntArray("Points", this.points);
        tag.putIntArray("Keep", this.keepOut);
    }

    private boolean kept(int x, int z) {
        for (int i = 0; i + 4 < this.keepOut.length; i += 5) {
            if (x >= this.keepOut[i] && x <= this.keepOut[i + 2] && z >= this.keepOut[i + 1] && z <= this.keepOut[i + 3]) {
                return true;
            }
        }
        return false;
    }

    /** Within two blocks of the gatehouse (no merlon there: it would stand in front of a doorway). */
    private boolean nearKept(int x, int z) {
        for (int i = 0; i + 4 < this.keepOut.length; i += 5) {
            if (x >= this.keepOut[i] - 2 && x <= this.keepOut[i + 2] + 2 && z >= this.keepOut[i + 1] - 2
                    && z <= this.keepOut[i + 3] + 2) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        List<CityRing.Edge> edges = CityRing.edges(this.ring);
        // The wall runs' own cells, as CityWallPiece lays them: each step's first cell is the outer face, the rest wall.
        java.util.Map<Long, Boolean> faces = new java.util.HashMap<>();      // outer cell -> carries a merlon
        java.util.Set<Long> wall = new java.util.HashSet<>();
        for (int e = 0; e < this.ring.length; e += 2) {
            int f = (e + 2) % this.ring.length;
            int x0 = this.ring[e], z0 = this.ring[e + 1], dx = this.ring[f] - x0, dz = this.ring[f + 1] - z0;
            int n = Math.max(Math.abs(dx), Math.abs(dz)), ix = -Integer.signum(dz), iz = Integer.signum(dx);
            for (int t = 0; t <= n; t++) {
                int x = x0 + Math.round((float) dx * t / Math.max(1, n)), z = z0 + Math.round((float) dz * t / Math.max(1, n));
                faces.merge(BlockPos.asLong(x, 0, z), Math.floorMod(t, 2) == 0, Boolean::logicalOr);
                wall.add(BlockPos.asLong(x, 0, z));
                if (ix != 0 && iz != 0) {
                    wall.add(BlockPos.asLong(x + ix, 0, z));
                    wall.add(BlockPos.asLong(x, 0, z + iz));
                } else {
                    wall.add(BlockPos.asLong(x + ix, 0, z + iz));
                }
            }
        }
        int bottom = this.top - CityWallPiece.HEIGHT - 2;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        for (int i = 0; i < this.points.length; i += 2) {
            int px = this.points[i], pz = this.points[i + 1];
            for (int x = px - REACH; x <= px + REACH; x++) {
                for (int z = pz - REACH; z <= pz + REACH; z++) {
                    if (x < box.minX() || x > box.maxX() || z < box.minZ() || z > box.maxZ() || kept(x, z)) {
                        continue;
                    }
                    double d = DogMoatPiece.distance(this.ring, x + 0.5, z + 0.5);
                    boolean inside = CityRing.inside(edges, x + 0.5, z + 0.5);
                    long key = BlockPos.asLong(x, 0, z);
                    boolean outer = faces.containsKey(key) && !(inside && d > 0.9);   // the wall's outer face
                    if (!(inside && d <= BAND) && !wall.contains(key)) {
                        continue;
                    }
                    for (int y = bottom; y <= this.top + 2; y++) {
                        p.set(x, y, z);
                        if (!box.isInside(p)) {
                            continue;
                        }
                        BlockState s;
                        if (y <= this.top) {
                            if (y < this.top - 1 && !CityWallPiece.open(level.getBlockState(p))
                                    && !level.getBlockState(p).is(Blocks.STONE_BRICKS)) {
                                continue;                          // something solid below: leave it
                            }
                            s = stone;
                        } else if (y == this.top + 1 && outer && faces.get(key) && !nearKept(x, z)) {
                            s = stone;                             // a merlon
                        } else {
                            s = air;
                        }
                        level.setBlock(p, s, 2);
                    }
                }
            }
        }
    }
}
