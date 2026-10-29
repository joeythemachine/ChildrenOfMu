package com.childrenofmu.worldgen;

import java.util.List;

import com.childrenofmu.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The fields round Dog City, outside the moat: wheat (for the cattle, the horses and the hens; the dogs themselves eat
 * only meat), on terraces that follow the star of the wall outward, a step down every few blocks, each riser faced in
 * stone brick. Paths run through on a grid, with steps where they cross a riser; a water hole in every field; the road
 * from the bridge runs straight out level; a stone retaining wall rims the cleared ground and the farmsteads. Anything
 * left lying above the fields (forest rocks, odd stone) is cleared away.
 */
public class DogFarmPiece extends StructurePiece {
    static final int TERRACE = 7, STEPS = 3, FIELD = 9;
    /** The farmsteads (plan coordinates), each 17 square: kept clear of fields, rimmed in stone. */
    static final int STEAD_AT = 82, STEAD_HALF = 9;

    private final int cx, cz, cy, turn;
    private final int[] ring;

    public DogFarmPiece(int cx, int cz, int cy, int turn, int[] ring) {
        super(ModStructures.DOG_FARM_PIECE.get(), 0, new BoundingBox(cx - DogCityStructure.REACH, cy - STEPS - 6,
                cz - DogCityStructure.REACH, cx + DogCityStructure.REACH, cy + 22, cz + DogCityStructure.REACH));
        this.cx = cx;
        this.cz = cz;
        this.cy = cy;
        this.turn = turn;
        this.ring = ring;
    }

    public DogFarmPiece(CompoundTag tag) {
        super(ModStructures.DOG_FARM_PIECE.get(), tag);
        this.cx = tag.getInt("CX");
        this.cz = tag.getInt("CZ");
        this.cy = tag.getInt("CY");
        this.turn = tag.getInt("Turn");
        this.ring = tag.getIntArray("Ring");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", this.cx);
        tag.putInt("CZ", this.cz);
        tag.putInt("CY", this.cy);
        tag.putInt("Turn", this.turn);
        tag.putIntArray("Ring", this.ring);
    }

    private static final int OUT = 0, FARM = 1, ROAD = 2, RIM = 3;

    /** What a world column is: 0 not ours, else farm / road / rim. */
    private int kind(List<CityRing.Edge> edges, int x, int z) {
        int[] l = DogCityStructure.turn(x - this.cx, z - this.cz, -this.turn);
        int a = Math.abs(l[0]), b = Math.abs(l[1]);
        int edge = DogCityStructure.REACH - 1;
        if (a > edge || b > edge || CityRing.inside(edges, x + 0.5, z + 0.5)) {
            return OUT;
        }
        double d = DogMoatPiece.distance(this.ring, x + 0.5, z + 0.5);
        if (d < DogMoatPiece.OUTER + 1.5) {
            return OUT;                                                    // the moat and its berm
        }
        int s0 = STEAD_AT - STEAD_HALF, s1 = STEAD_AT + STEAD_HALF;
        if (a >= s0 && a <= s1 && b >= s0 && b <= s1) {
            return a == s0 || a == s1 || b == s0 || b == s1 ? RIM : OUT;   // a farmstead (its own template)
        }
        if (a >= edge - 1 || b >= edge - 1) {
            return RIM;                                                    // the edge of the cleared ground
        }
        if (l[1] > 0 && a <= 2) {
            return ROAD;                                                   // the road out from the bridge
        }
        return FARM;
    }

    /** The top block's height in a column of ours: the fields step down away from the wall; road and rim stay level. */
    private int level(List<CityRing.Edge> edges, int x, int z, int kind) {
        int ground = this.cy - 1;
        if (kind != FARM) {
            return ground;
        }
        double d = DogMoatPiece.distance(this.ring, x + 0.5, z + 0.5) - (DogMoatPiece.OUTER + 1.5);
        return ground - Math.min(STEPS, (int) (d / TERRACE));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        List<CityRing.Edge> edges = CityRing.edges(this.ring);
        int ground = this.cy - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState brick = Blocks.STONE_BRICKS.defaultBlockState(), air = Blocks.AIR.defaultBlockState(),
                dirt = Blocks.DIRT.defaultBlockState();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int kind = kind(edges, x, z);
                if (kind == OUT) {
                    continue;
                }
                int top = level(edges, x, z, kind);
                int[] l = DogCityStructure.turn(x - this.cx, z - this.cz, -this.turn);
                // The lowest neighbour of ours: the riser below this column's edge is faced in brick down to it.
                int low = top;
                Direction up = null;          // for a path: a neighbouring path one higher (steps up to it)
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    int nk = kind(edges, x + dir.getStepX(), z + dir.getStepZ());
                    if (nk == OUT) {
                        continue;
                    }
                    int nt = level(edges, x + dir.getStepX(), z + dir.getStepZ(), nk);
                    low = Math.min(low, nt);
                    if (nt == top + 1 && kind == FARM && path(x + dir.getStepX(), z + dir.getStepZ(), nk)) {
                        up = dir;
                    }
                }
                boolean kerb = low < top;
                // Clear everything above (crops come after), and fill the ground below.
                for (int y = ground + 20; y > top; y--) {
                    p.set(x, y, z);
                    if (box.isInside(p) && !level.getBlockState(p).isAir()) {
                        level.setBlock(p, air, 2);
                    }
                }
                for (int y = top - 1; y >= Math.min(low, top) - 3; y--) {
                    p.set(x, y, z);
                    if (box.isInside(p)) {
                        level.setBlock(p, y > low && kerb ? brick : dirt, 2);
                    }
                }
                p.set(x, top, z);
                if (!box.isInside(p)) {
                    continue;
                }
                if (kind == RIM) {
                    for (int y = top; y >= top - STEPS - 1; y--) {
                        level.setBlock(p.set(x, y, z), y == top ? Blocks.POLISHED_ANDESITE.defaultBlockState() : brick, 2);
                    }
                    continue;
                }
                if (kind == ROAD) {
                    level.setBlock(p, (Math.abs(l[0]) == 2 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS).defaultBlockState(), 2);
                    continue;
                }
                if (kerb) {
                    level.setBlock(p, Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);    // the terrace's edge
                    continue;
                }
                if (path(x, z, kind)) {
                    level.setBlock(p, Blocks.DIRT_PATH.defaultBlockState(), 2);
                    if (up != null) {
                        BlockPos s = p.above();
                        if (box.isInside(s)) {
                            level.setBlock(s, Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                                    .setValue(StairBlock.FACING, up), 2);                   // a step up to the next terrace
                        }
                    }
                    continue;
                }
                int fa = Math.floorMod(l[0], FIELD), fb = Math.floorMod(l[1], FIELD);
                if (fa == FIELD / 2 && fb == FIELD / 2 && flatAround(edges, x, z, top)) {
                    level.setBlock(p, Blocks.WATER.defaultBlockState(), 2);                 // the field's water hole
                    continue;
                }
                level.setBlock(p, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
                BlockPos c = p.above();
                if (box.isInside(c)) {
                    level.setBlock(c, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3 + random.nextInt(5)), 2);
                }
            }
        }
    }

    /** The paths through the fields, on a grid (plan coordinates). */
    private boolean path(int x, int z, int kind) {
        if (kind != FARM) {
            return false;
        }
        int[] l = DogCityStructure.turn(x - this.cx, z - this.cz, -this.turn);
        return Math.floorMod(l[0], FIELD) == 0 || Math.floorMod(l[1], FIELD) == 0;
    }

    /** All eight neighbours are fields at this height (so the water hole stays put). */
    private boolean flatAround(List<CityRing.Edge> edges, int x, int z, int top) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int k = kind(edges, x + dx, z + dz);
                if (k != FARM || level(edges, x + dx, z + dz, k) != top) {
                    return false;
                }
            }
        }
        return true;
    }
}
