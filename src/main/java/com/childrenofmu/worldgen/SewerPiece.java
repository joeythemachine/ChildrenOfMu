package com.childrenofmu.worldgen;

import com.childrenofmu.entity.folk.FolkEntity;
import com.childrenofmu.registry.ModEntities;
import com.childrenofmu.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The sewer under an RCB City street: a vaulted brick tunnel {@link #DEPTH} blocks down, following the street
 * above step for step, with a channel of water down the middle and a walkway either side, lanterns here and there,
 * a ladder up to every manhole in the street, and the odd Rat at work. Where two sewers meet they open into each
 * other. It only cuts through natural ground, never through a building's cellar.
 */
public class SewerPiece extends StructurePiece {
    static final int DEPTH = 8;

    private final int x0, z0, x1, z1, y;
    private final boolean xFirst;

    /** cityY: the height of the city's heart. Every sewer runs at the same depth below it, so they all meet. */
    public SewerPiece(int x0, int z0, int x1, int z1, int streetY, boolean xFirst) {
        super(ModStructures.SEWER_PIECE.get(), 0, new BoundingBox(Math.min(x0, x1) - 3, streetY - DEPTH - 2,
                Math.min(z0, z1) - 3, Math.max(x0, x1) + 3, streetY + 2 * StreetPiece.REACH, Math.max(z0, z1) + 3));
        this.x0 = x0;
        this.z0 = z0;
        this.x1 = x1;
        this.z1 = z1;
        this.y = streetY;
        this.xFirst = xFirst;
    }

    public SewerPiece(CompoundTag tag) {
        super(ModStructures.SEWER_PIECE.get(), tag);
        this.x0 = tag.getInt("X0");
        this.z0 = tag.getInt("Z0");
        this.x1 = tag.getInt("X1");
        this.z1 = tag.getInt("Z1");
        this.y = tag.getInt("Y");
        this.xFirst = tag.getBoolean("XFirst");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("X0", this.x0);
        tag.putInt("Z0", this.z0);
        tag.putInt("X1", this.x1);
        tag.putInt("Z1", this.z1);
        tag.putInt("Y", this.y);
        tag.putBoolean("XFirst", this.xFirst);
    }

    /** Ground a sewer may be dug through. */
    private static boolean diggable(BlockState s) {
        return s.isAir() || !s.getFluidState().isEmpty() || StreetPiece.natural(s) || s.is(BlockTags.COAL_ORES)
                || s.is(BlockTags.IRON_ORES) || s.is(BlockTags.COPPER_ORES) || s.is(Blocks.TUFF) || s.is(Blocks.CALCITE);
    }

    /** The sewer's own masonry (so a crossing sewer can open into this one). */
    private static boolean masonry(BlockState s) {
        return s.is(Blocks.BRICKS) || s.is(Blocks.MOSSY_STONE_BRICKS) || s.is(Blocks.STONE_BRICKS);
    }

    /**
     * Is there enough land over the tunnel here to bury it? (The land as the world made it, before the city: where
     * the ground falls away, the sewer stops at an iron grate instead of breaking out into the street.)
     */
    static boolean covered(WorldGenLevel level, ChunkGenerator generator, int x, int z, int floor) {
        int land = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level,
                level.getLevel().getChunkSource().randomState());
        return land - 1 >= floor + 7;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int floor = this.y - DEPTH;
        java.util.List<StreetPiece.Step> steps = StreetPiece.route(this.x0, this.z0, this.x1, this.z1, this.xFirst);
        boolean[] cover = new boolean[steps.size()];
        boolean[] near = new boolean[steps.size()];
        for (int i = 0; i < steps.size(); i++) {
            StreetPiece.Step s = steps.get(i);
            near[i] = s.x() >= box.minX() - 4 && s.x() <= box.maxX() + 4 && s.z() >= box.minZ() - 4 && s.z() <= box.maxZ() + 4;
        }
        for (int i = 0; i < steps.size(); i++) {
            boolean look = near[i] || (i > 0 && near[i - 1]) || (i + 1 < steps.size() && near[i + 1]);
            cover[i] = look && covered(level, generator, steps.get(i).x(), steps.get(i).z(), floor);
        }
        for (int i = 0; i < steps.size(); i++) {
            StreetPiece.Step s = steps.get(i);
            if (!near[i] || !cover[i]) {
                continue;
            }
            for (int side = -2; side <= 2; side++) {
                int px = s.alongX() ? s.x() : s.x() + side;
                int pz = s.alongX() ? s.z() + side : s.z();
                for (int dy = -1; dy <= 4; dy++) {
                    BlockPos p = new BlockPos(px, floor + dy, pz);
                    if (!box.isInside(p)) {
                        continue;
                    }
                    BlockState here = level.getBlockState(p);
                    boolean wall = Math.abs(side) == 2 || dy == -1 || dy == 4;
                    BlockState brick = brick(px, floor + dy, pz);
                    if (wall) {
                        if (diggable(here)) {
                            level.setBlock(p, brick, 2);      // the tunnel is always walled, even through a cave
                        }
                    } else if (diggable(here) || masonry(here)) {
                        if (dy == 0) {
                            level.setBlock(p, side == 0 ? Blocks.WATER.defaultBlockState() : brick, 2);
                        } else {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
            // Where the tunnel ends (the land falls away), an iron grate closes it off.
            boolean end = (i > 0 && !cover[i - 1]) || (i + 1 < steps.size() && !cover[i + 1]);
            if (end) {
                for (int side = -1; side <= 1; side++) {
                    for (int dy = 1; dy <= 3; dy++) {
                        BlockPos p = s.alongX() ? new BlockPos(s.x(), floor + dy, s.z() + side)
                                : new BlockPos(s.x() + side, floor + dy, s.z());
                        if (box.isInside(p) && level.getBlockState(p).isAir()) {
                            level.setBlock(p, grate(s.alongX()), 2);
                        }
                    }
                }
            }
            BlockPos center = new BlockPos(s.x(), floor, s.z());
            if (s.index() % 10 == 5 && box.isInside(center.above(3)) && level.getBlockState(center.above(4)).isSolid()) {
                level.setBlock(center.above(3), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
            }
            if (StreetPiece.isManhole(s) && !end) {
                shaft(level, box, s, floor);
            }
            if (s.index() % 23 == 11) {
                rat(level, box, s, floor);
            }
            if (s.index() % 14 == 4 && !end && !StreetPiece.isManhole(s)) {
                nook(level, box, s, floor);
            }
        }
    }

    private static BlockState brick(int x, int y, int z) {
        RandomSource r = RandomSource.create(Mth.getSeed(x, y, z));
        return r.nextFloat() < 0.75F ? Blocks.BRICKS.defaultBlockState() : Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    }

    /** Iron bars across the tunnel (joined to each other and the walls). */
    private static BlockState grate(boolean alongX) {
        BlockState bars = Blocks.IRON_BARS.defaultBlockState();
        return alongX ? bars.setValue(net.minecraft.world.level.block.CrossCollisionBlock.NORTH, true)
                .setValue(net.minecraft.world.level.block.CrossCollisionBlock.SOUTH, true)
                : bars.setValue(net.minecraft.world.level.block.CrossCollisionBlock.EAST, true)
                .setValue(net.minecraft.world.level.block.CrossCollisionBlock.WEST, true);
    }

    /**
     * The way up to a manhole: a ladder from the walkway (you just walk into it) up a brick-lined shaft to the stone
     * cover in the street above, over the walkway on one side of the channel.
     */
    private void shaft(WorldGenLevel level, BoundingBox box, StreetPiece.Step s, int floor) {
        int px = s.alongX() ? s.x() : s.x() - 1;
        int pz = s.alongX() ? s.z() - 1 : s.z();
        Direction facing = s.alongX() ? Direction.SOUTH : Direction.EAST;     // away from the tunnel wall
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
        int top = this.y + 2 * StreetPiece.REACH;
        // Find the cover in the street over this spot.
        int coverY = -1;
        for (int yy = floor + 5; yy < top; yy++) {
            BlockState here = level.getBlockState(new BlockPos(px, yy, pz));
            if (here.is(com.childrenofmu.registry.ModBlocks.MANHOLE.get())) {
                coverY = yy;
                break;
            }
            if (here.isAir()) {
                break;    // open air before any cover: no street here
            }
        }
        if (coverY < 0) {
            return;
        }
        Direction back = facing.getOpposite();
        for (int yy = floor + 1; yy < coverY; yy++) {
            BlockPos p = new BlockPos(px, yy, pz);
            if (!box.isInside(p)) {
                continue;
            }
            BlockPos behind = p.relative(back);
            if (box.isInside(behind) && !level.getBlockState(behind).isSolid()) {
                level.setBlock(behind, brick(behind.getX(), yy, behind.getZ()), 2);
            }
            if (yy >= floor + 4) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos q = p.relative(d);
                    if (box.isInside(q) && (diggable(level.getBlockState(q)) || level.getBlockState(q).canBeReplaced())) {
                        level.setBlock(q, brick(q.getX(), yy, q.getZ()), 2);
                    }
                }
            }
            level.setBlock(p, ladder, 2);
        }
    }

    /**
     * A rats' nook: a little hollow dug into the tunnel wall beside the walkway, two wide, one deep and two high, with
     * hay and old rags to sleep on (by day, rats curl up in piles in these; see FolkStreets.NestGoal), and a couple of
     * rats to start with.
     */
    private void nook(WorldGenLevel level, BoundingBox box, StreetPiece.Step s, int floor) {
        int side = Math.floorMod(s.index() / 14, 2) == 0 ? 3 : -3;   // which wall
        boolean any = false;
        for (int along = 0; along <= 1; along++) {
            int px = s.alongX() ? s.x() + along : s.x() + side;
            int pz = s.alongX() ? s.z() + side : s.z() + along;
            int wx = s.alongX() ? px : px - Integer.signum(side);      // the tunnel wall between walkway and nook
            int wz = s.alongX() ? pz - Integer.signum(side) : pz;
            BlockPos nest = new BlockPos(px, floor, pz);
            if (!box.isInside(nest) || !diggable(level.getBlockState(nest.above()))
                    && !masonry(level.getBlockState(nest.above()))) {
                continue;
            }
            // Masonry round the hollow.
            for (int dy = -1; dy <= 3; dy++) {
                for (int[] d : new int[][]{{0, 0}, {Integer.signum(side), 0}, {0, 1}, {0, -1}}) {
                    int ox = s.alongX() ? d[1] : d[0], oz = s.alongX() ? d[0] : d[1];
                    BlockPos q = new BlockPos(px + ox, floor + dy, pz + oz);
                    if (box.isInside(q) && (d[0] != 0 || d[1] != 0 || dy == -1 || dy == 3)
                            && diggable(level.getBlockState(q)) && !level.getBlockState(q).isAir()) {
                        level.setBlock(q, brick(q.getX(), q.getY(), q.getZ()), 2);
                    }
                }
            }
            level.setBlock(nest, Blocks.HAY_BLOCK.defaultBlockState(), 2);
            for (int dy = 1; dy <= 2; dy++) {
                BlockPos air = nest.above(dy);
                level.setBlock(air, Blocks.AIR.defaultBlockState(), 2);
                BlockPos door = new BlockPos(wx, floor + dy, wz);
                if (box.isInside(door)) {
                    level.setBlock(door, Blocks.AIR.defaultBlockState(), 2);
                }
            }
            BlockPos rug = nest.above();
            RandomSource r = RandomSource.create(Mth.getSeed(px, floor, pz));
            if (r.nextBoolean()) {
                level.setBlock(rug, (r.nextBoolean() ? Blocks.WHITE_CARPET : Blocks.BROWN_CARPET).defaultBlockState(), 2);
            }
            any = true;
        }
        if (any && Math.floorMod(s.index() / 14, 3) != 2) {
            rat(level, box, s, floor);
        }
    }

    /** Now and then a Rat down here, keeping the sewer running. */
    private void rat(WorldGenLevel level, BoundingBox box, StreetPiece.Step s, int floor) {
        BlockPos at = s.alongX() ? new BlockPos(s.x(), floor + 1, s.z() - 1) : new BlockPos(s.x() - 1, floor + 1, s.z());
        if (!box.isInside(at) || !level.getBlockState(at).isAir() || !level.getBlockState(at.below()).isSolid()
                || level.getBlockState(at.above(3)).isAir()) {
            return;
        }
        FolkEntity rat = ModEntities.RAT_FOLK.get().create(level.getLevel());
        if (rat == null) {
            return;
        }
        RandomSource r = RandomSource.create(Mth.getSeed(at));
        rat.presetVariant(r.nextInt(3) == 0 ? "scavenger" : r.nextBoolean() ? "gong" : "engineer");
        rat.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360.0F, 0.0F);
        rat.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        rat.setPersistenceRequired();
        level.addFreshEntityWithPassengers(rat);
    }
}
