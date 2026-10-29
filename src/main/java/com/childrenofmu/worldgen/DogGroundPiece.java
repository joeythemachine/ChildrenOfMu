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
        super(ModStructures.DOG_GROUND_PIECE.get(), 0, new BoundingBox(cx - 80, cy - 3, cz - 80, cx + 80, cy + 4, cz + 80));
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

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
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
