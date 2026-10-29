package com.childrenofmu.worldgen;

import java.util.List;

import com.childrenofmu.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The lie of the land under an RCB City, worked before anything is built on it. Every building and stretch of wall has
 * a plot, levelled to its own height (filled up or cut down); around each plot the land is banked back to its natural
 * height over a few blocks, sharing out the ground between neighbouring plots so no plot buries or undercuts another.
 * Inside the walls, deep holes are filled and the odd hump taken down. Everything comes from the land as the world made
 * it, so the result is the same whichever chunk is built first.
 */
public class CityTerrainPiece extends StructurePiece {
    /** How far out a plot is banked back into the land. */
    static final int SKIRT = 5;

    private final int cy;
    private final int[] ring;
    /** minX, minZ, maxX, maxZ, ground (top block y) per plot. */
    private final int[] plots;

    /** The town wall's corners (x, z, x, z...). */
    public int[] ring() {
        return this.ring;
    }

    public CityTerrainPiece(int cy, int[] ring, List<int[]> plots) {
        super(ModStructures.CITY_TERRAIN_PIECE.get(), 0, box(cy, ring, plots));
        this.cy = cy;
        this.ring = ring;
        this.plots = new int[plots.size() * 5];
        for (int i = 0; i < plots.size(); i++) {
            System.arraycopy(plots.get(i), 0, this.plots, i * 5, 5);
        }
    }

    private static BoundingBox box(int cy, int[] ring, List<int[]> plots) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < ring.length; i += 2) {
            minX = Math.min(minX, ring[i]);
            maxX = Math.max(maxX, ring[i]);
            minZ = Math.min(minZ, ring[i + 1]);
            maxZ = Math.max(maxZ, ring[i + 1]);
        }
        for (int[] p : plots) {
            minX = Math.min(minX, p[0]);
            minZ = Math.min(minZ, p[1]);
            maxX = Math.max(maxX, p[2]);
            maxZ = Math.max(maxZ, p[3]);
        }
        return new BoundingBox(minX - SKIRT - 2, cy - 40, minZ - SKIRT - 2, maxX + SKIRT + 2, cy + 40, maxZ + SKIRT + 2);
    }

    public CityTerrainPiece(CompoundTag tag) {
        super(ModStructures.CITY_TERRAIN_PIECE.get(), tag);
        this.cy = tag.getInt("CY");
        this.ring = tag.getIntArray("Ring");
        this.plots = tag.getIntArray("Plots");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CY", this.cy);
        tag.putIntArray("Ring", this.ring);
        tag.putIntArray("Plots", this.plots);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        List<CityRing.Edge> edges = CityRing.edges(this.ring);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int low = this.cy - 5, high = this.cy + 5;       // inside the walls: no deep holes, no big humps
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int noise = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level,
                        level.getLevel().getChunkSource().randomState()) - 1;
                // Forest rocks (old growth taiga's mossy boulders) are placed before the city and would read as ground:
                // the city built round them and paved up to them. Take them away, and fill what was sunk in the earth.
                for (int y = Math.min(noise + 8, box.maxY()); y >= Math.max(noise - 4, box.minY()); y--) {
                    p.set(x, y, z);
                    if (level.getBlockState(p).is(Blocks.MOSSY_COBBLESTONE)) {
                        level.setBlock(p, (y > noise ? Blocks.AIR : y == noise ? Blocks.GRASS_BLOCK : Blocks.DIRT)
                                .defaultBlockState(), 2);
                    }
                }
                // The real ground: below any water, and down into any ravine or cave the noise height knows nothing of.
                int natural = noise;
                boolean wet = false;
                for (int y = noise + 2; y >= box.minY(); y--) {
                    p.set(x, y, z);
                    BlockState here = level.getBlockState(p);
                    if (!here.getFluidState().isEmpty()) {
                        wet = true;
                    }
                    if (here.blocksMotion() && here.getFluidState().isEmpty() && !here.is(BlockTags.LEAVES)
                            && !here.is(BlockTags.LOGS)) {
                        natural = y;
                        break;
                    }
                    natural = y - 1;
                }
                // The nearest plot. A building's plot is its whole template box, and neighbours' boxes overlap a good
                // deal, so where several plots cover a spot, the one whose middle is nearest decides (not the highest,
                // which buried lower shop fronts under their neighbours' ground).
                int bestD = Integer.MAX_VALUE, py = 0;
                long bestC = Long.MAX_VALUE;
                for (int i = 0; i < this.plots.length; i += 5) {
                    int d = Math.max(Math.max(this.plots[i] - x, x - this.plots[i + 2]),
                            Math.max(this.plots[i + 1] - z, z - this.plots[i + 3]));
                    d = Math.max(0, d);
                    long mx = (this.plots[i] + this.plots[i + 2]) / 2, mz = (this.plots[i + 1] + this.plots[i + 3]) / 2;
                    long c = (x - mx) * (x - mx) + (z - mz) * (z - mz);
                    if (d < bestD || (d == bestD && c < bestC)) {
                        bestD = d;
                        bestC = c;
                        py = this.plots[i + 4];
                    }
                }
                boolean inRing = CityRing.inside(edges, x + 0.5, z + 0.5);
                // Inside the walls: no deep holes, no big humps, and never below the sea (so nothing floods).
                int target = inRing ? Mth.clamp(natural, Math.max(low, generator.getSeaLevel() - 1), Math.max(high,
                        generator.getSeaLevel() - 1)) : natural;
                if (!inRing && wet && bestD <= SKIRT) {
                    target = Math.max(natural, generator.getSeaLevel() - 1);       // plots by the water stand dry
                }
                if (bestD == 0) {
                    target = py;
                } else if (bestD <= SKIRT) {
                    int wobble = (int) Math.floorMod(Mth.getSeed(x, 5, z) >> 7, 2);
                    int give = bestD + wobble;
                    target = Mth.clamp(target, py - give, py + give);
                }
                if (inRing || bestD <= SKIRT) {
                    // No lava in an orderly town: any lying near the surface (a lake the levelling opened) is sealed
                    // with stone below the ground, and cleared above it.
                    for (int y = Math.min(target, natural) - 6; y <= Math.max(target, natural) + 2; y++) {
                        p.set(x, y, z);
                        if (box.isInside(p) && level.getFluidState(p).is(net.minecraft.tags.FluidTags.LAVA)) {
                            level.setBlock(p, (y <= target ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
                        }
                    }
                }
                if (target == natural && !(wet && (inRing || bestD <= SKIRT))) {
                    continue;       // untouched (and the sea round about stays the sea)
                }
                if (target > natural) {
                    p.set(x, natural, z);
                    if (level.getBlockState(p).is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                    }
                    for (int y = Math.max(natural + 1, target - (inRing ? 64 : 24)); y <= target; y++) {
                        p.set(x, y, z);
                        BlockState here = level.getBlockState(p);
                        if (here.isAir() || here.canBeReplaced() || !here.getFluidState().isEmpty()) {
                            level.setBlock(p, (y == target ? Blocks.GRASS_BLOCK : y >= target - 3 ? Blocks.DIRT
                                    : Blocks.STONE).defaultBlockState(), 2);
                        }
                    }
                } else {
                    for (int y = target + 1; y <= Math.max(natural, noise) + 2 && y <= target + 26; y++) {
                        p.set(x, y, z);
                        BlockState here = level.getBlockState(p);
                        if (StreetPiece.natural(here) || here.canBeReplaced() || here.is(BlockTags.LEAVES)
                                || !here.getFluidState().isEmpty()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                    p.set(x, target, z);
                    if (level.getBlockState(p).is(BlockTags.DIRT)) {
                        level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
}
