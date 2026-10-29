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
 * Dog City's moat: a stone-lined channel of water following the star of the wall a little way out from it (a berm of
 * grass between), four wide and three deep, with a stone bridge across it in front of the gate.
 */
public class DogMoatPiece extends StructurePiece {
    /** The channel's stone banks, measured out from the wall's outer face (the water lies between them). */
    static final int INNER = 3, OUTER = 8, DEPTH = 4;

    private final int cx, cz, cy, turn;
    private final int[] ring;

    public DogMoatPiece(int cx, int cz, int cy, int turn, int[] ring) {
        super(ModStructures.DOG_MOAT_PIECE.get(), 0, box(cy, ring));
        this.cx = cx;
        this.cz = cz;
        this.cy = cy;
        this.turn = turn;
        this.ring = ring;
    }

    public DogMoatPiece(CompoundTag tag) {
        super(ModStructures.DOG_MOAT_PIECE.get(), tag);
        this.cx = tag.getInt("CX");
        this.cz = tag.getInt("CZ");
        this.cy = tag.getInt("CY");
        this.turn = tag.getInt("Turn");
        this.ring = tag.getIntArray("Ring");
    }

    private static BoundingBox box(int cy, int[] ring) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < ring.length; i += 2) {
            minX = Math.min(minX, ring[i]);
            maxX = Math.max(maxX, ring[i]);
            minZ = Math.min(minZ, ring[i + 1]);
            maxZ = Math.max(maxZ, ring[i + 1]);
        }
        return new BoundingBox(minX - OUTER - 2, cy - DEPTH - 3, minZ - OUTER - 2, maxX + OUTER + 2, cy + 4, maxZ + OUTER + 2);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", this.cx);
        tag.putInt("CZ", this.cz);
        tag.putInt("CY", this.cy);
        tag.putInt("Turn", this.turn);
        tag.putIntArray("Ring", this.ring);
    }

    /** Distance from (x, z) to the nearest edge of the star (its outer wall face). */
    static double distance(int[] ring, double x, double z) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < ring.length; i += 2) {
            int j = (i + 2) % ring.length;
            double ax = ring[i], az = ring[i + 1], bx = ring[j], bz = ring[j + 1];
            double dx = bx - ax, dz = bz - az;
            double t = ((x - ax) * dx + (z - az) * dz) / (dx * dx + dz * dz);
            t = Math.max(0, Math.min(1, t));
            double px = ax + t * dx - x, pz = az + t * dz - z;
            best = Math.min(best, Math.sqrt(px * px + pz * pz));
        }
        return best;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        List<CityRing.Edge> edges = CityRing.edges(this.ring);
        int top = this.cy - 1;                         // the ground's top block
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState(), water = Blocks.WATER.defaultBlockState(),
                air = Blocks.AIR.defaultBlockState();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (CityRing.inside(edges, x + 0.5, z + 0.5)) {
                    continue;
                }
                double d = distance(this.ring, x + 0.5, z + 0.5);
                if (d < INNER - 0.5 || d > OUTER + 0.5) {
                    continue;
                }
                // The bridge: five wide, straight out from the gate (to the south, before the city was turned).
                int[] local = DogCityStructure.turn(x - this.cx, z - this.cz, -this.turn);
                boolean bridge = Math.abs(local[0]) <= 2 && local[1] > 0;
                boolean bank = d < INNER + 0.5 || d > OUTER - 0.5;
                for (int y = top - DEPTH; y <= top + 3; y++) {
                    p.set(x, y, z);
                    if (!box.isInside(p)) {
                        continue;
                    }
                    BlockState s;
                    if (bridge) {
                        if (y < top) {
                            s = Math.abs(local[0]) == 2 || bank || y == top - DEPTH ? stone : y >= top - 1 ? air : water;   // over the water
                        } else if (y == top) {
                            s = stone;                                                 // the deck
                        } else if (y == top + 1 && Math.abs(local[0]) == 2) {
                            s = Blocks.STONE_BRICK_WALL.defaultBlockState();          // its parapets
                        } else {
                            s = air;
                        }
                    } else if (bank) {
                        s = y <= top ? stone : air;                                    // the stone banks, flush with the grass
                    } else if (y == top - DEPTH) {
                        s = stone;                                                     // the floor
                    } else if (y < top) {
                        s = water;
                    } else {
                        s = air;
                    }
                    level.setBlock(p, s, 2);
                }
            }
        }
    }
}
