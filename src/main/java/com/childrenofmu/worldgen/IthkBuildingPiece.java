package com.childrenofmu.worldgen;

import com.childrenofmu.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.ChunkPos;

/**
 * Places an Ithk building template, and first reworks the existing ground beneath it: the top block of each
 * column turns to mud (with some packed mud) and grows red plants, fading out through rooted and coarse dirt
 * at a ragged edge. The land keeps its own shape.
 */
public class IthkBuildingPiece extends TemplateStructurePiece {
    /** Width of the fade from mud back into the natural ground. */
    private static final float FADE = 3.5F;

    /** How far a stilt may reach down to find the bottom. */
    private static final int MAX_STILT = 24;

    private final Rotation rotation;
    private final int groundRadius;
    /** Extend the logs in the template's bottom layer down to the ground: stilts (the Frog folk's buildings). */
    private final boolean supports;
    /** If 0 or more, level a plot for the building first; the template's ground layer is this many up, less one. */
    private final int levelSink;
    private int sink;
    /** How far out the levelled plot is banked back into the land. */
    private static final int SKIRT = 4;

    public IthkBuildingPiece(StructureTemplateManager manager, ResourceLocation template, BlockPos center,
                             Rotation rotation, int groundRadius) {
        this(manager, template, center, rotation, groundRadius, false);
    }

    public IthkBuildingPiece(StructureTemplateManager manager, ResourceLocation template, BlockPos center,
                             Rotation rotation, int groundRadius, boolean supports) {
        this(manager, template, center, rotation, groundRadius, supports, -1);
    }

    public IthkBuildingPiece(StructureTemplateManager manager, ResourceLocation template, BlockPos center,
                             Rotation rotation, int groundRadius, boolean supports, int levelSink) {
        this(manager, template, center, rotation, groundRadius, supports, levelSink, Math.max(0, levelSink));
    }

    /** sink: how many of the template's bottom layers sit below the surface (see groundY). */
    public IthkBuildingPiece(StructureTemplateManager manager, ResourceLocation template, BlockPos center,
                             Rotation rotation, int groundRadius, boolean supports, int levelSink, int sink) {
        this(manager, ModdedTemplates.pick(manager, template), center, rotation, groundRadius, supports, levelSink, true);
        this.sink = sink;
    }

    /** The template this piece builds (its resource location, as text). */
    public String templateId() {
        return this.templateName;
    }

    /** Where the building's walls actually stand (minX, minZ, maxX, maxZ), in the world. */
    public int[] footprint() {
        TemplateInfo info = TemplateInfo.of(this.template);
        BlockPos a = StructureTemplate.calculateRelativePosition(this.placeSettings, new BlockPos(info.minX, 0, info.minZ))
                .offset(this.templatePosition);
        BlockPos b = StructureTemplate.calculateRelativePosition(this.placeSettings, new BlockPos(info.maxX, 0, info.maxZ))
                .offset(this.templatePosition);
        return new int[]{Math.min(a.getX(), b.getX()), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()),
                Math.max(a.getZ(), b.getZ())};
    }

    /** Where a skywalk may meet this building, in the world: the wall block to cut through, and which way is out. */
    public java.util.List<TemplateInfo.Anchor> worldAnchors() {
        java.util.List<TemplateInfo.Anchor> out = new java.util.ArrayList<>();
        for (TemplateInfo.Anchor a : TemplateInfo.of(this.template).anchors) {
            out.add(new TemplateInfo.Anchor(StructureTemplate.calculateRelativePosition(this.placeSettings, a.wall())
                    .offset(this.templatePosition), this.placeSettings.getRotation().rotate(a.out())));
        }
        return out;
    }

    /** The height of the ground the building stands on (its floor, in the template's layer sink - 1). */
    public int groundY() {
        return this.templatePosition.getY() + Math.max(0, this.sink - 1);
    }

    /** {@code template} here is the version actually built (see ModdedTemplates). */
    private IthkBuildingPiece(StructureTemplateManager manager, ResourceLocation template, BlockPos center,
                              Rotation rotation, int groundRadius, boolean supports, int levelSink, boolean resolved) {
        super(ModStructures.ITHK_BUILDING_PIECE.get(), 0, manager, template, template.toString(),
                settings(manager, template, rotation), corner(manager, template, center));
        this.rotation = rotation;
        this.groundRadius = groundRadius;
        this.supports = supports;
        this.levelSink = levelSink;
        if (levelSink >= 0) {
            // Take in the banked edges too, so every chunk they reach gets them.
            this.boundingBox = this.boundingBox.inflatedBy(SKIRT);
        }
    }

    public IthkBuildingPiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ModStructures.ITHK_BUILDING_PIECE.get(), tag, manager,
                location -> settings(manager, location, Rotation.valueOf(tag.getString("Rot"))));
        this.rotation = Rotation.valueOf(tag.getString("Rot"));
        this.groundRadius = tag.getInt("GroundRadius");
        this.supports = tag.getBoolean("Supports");
        this.levelSink = tag.contains("LevelSink") ? tag.getInt("LevelSink") : -1;
        this.sink = tag.getInt("Sink");
    }

    private static StructurePlaceSettings settings(StructureTemplateManager manager, ResourceLocation template,
                                                   Rotation rotation) {
        Vec3i size = manager.getOrCreate(template).getSize();
        return new StructurePlaceSettings()
                .setRotation(rotation)
                .setRotationPivot(new BlockPos(size.getX() / 2, 0, size.getZ() / 2))  // turn about the center
                .setKnownShape(true)          // keep the stair and slab shapes exactly as built
                .setFinalizeEntities(true)    // the Ithk inside get their names, eyes and trades
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .addProcessor(RotateFixProcessor.INSTANCE);   // modded posts that don't turn by themselves
    }

    private static BlockPos corner(StructureTemplateManager manager, ResourceLocation template, BlockPos center) {
        Vec3i size = manager.getOrCreate(template).getSize();
        return center.offset(-size.getX() / 2, 0, -size.getZ() / 2);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", this.rotation.name());
        tag.putInt("GroundRadius", this.groundRadius);
        tag.putBoolean("Supports", this.supports);
        tag.putInt("LevelSink", this.levelSink);
        tag.putInt("Sink", this.sink);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        // The ground goes first, while the heightmap still sees only the land and not the building above it.
        reworkGround(level, generator, box);
        if (this.levelSink >= 0) {
            levelPlot(level, box);
        }
        clearCanopy(level, box);
        super.postProcess(level, structureManager, generator, random, box, chunkPos, pivot);
        if (this.supports) {
            extendSupports(level, box);
        }
    }

    /**
     * Levels the building's plot before it goes down: across the template's whole footprint the land is filled up (or
     * cut down) to the template's ground layer, grass on top; around it the land is banked back to its own height over
     * a few blocks, so a building on a slope sits on a little terrace instead of floating or being buried.
     */
    private void levelPlot(WorldGenLevel level, BoundingBox box) {
        Vec3i size = this.template.getSize(this.rotation);
        int fx0 = this.templatePosition.getX(), fz0 = this.templatePosition.getZ();
        // Rotation about the center keeps the footprint centered; work it out from the center.
        int cx = fx0 + this.template.getSize().getX() / 2, cz = fz0 + this.template.getSize().getZ() / 2;
        int hx = size.getX() / 2, hz = size.getZ() / 2;
        int minX = cx - hx, maxX = minX + size.getX() - 1, minZ = cz - hz, maxZ = minZ + size.getZ() - 1;
        int groundY = this.templatePosition.getY() + Math.max(0, this.levelSink - 1);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = Math.max(box.minX(), minX - SKIRT); x <= Math.min(box.maxX(), maxX + SKIRT); x++) {
            for (int z = Math.max(box.minZ(), minZ - SKIRT); z <= Math.min(box.maxZ(), maxZ + SKIRT); z++) {
                int out = Math.max(Math.max(minX - x, x - maxX), Math.max(minZ - z, z - maxZ));
                out = Math.max(0, out);
                // A little wobble at the edge, the same whichever chunk does it.
                int wobble = out > 0 ? (int) Math.floorMod(Mth.getSeed(x, 5, z) >> 7, 2) : 0;
                int land = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                int fillTo = groundY - out - wobble;     // fill up to here (a bank falling away)
                int cutTo = groundY + out + wobble;          // cut down to here (a bank rising up)
                if (out >= SKIRT) {
                    continue;
                }
                // Fill: build the land up to the plot (or the bank), grass on top.
                if (land < fillTo) {
                    p.set(x, land, z);
                    if (level.getBlockState(p).is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                    }
                    for (int y = Math.max(land + 1, fillTo - 14); y <= fillTo; y++) {
                        p.set(x, y, z);
                        BlockState here = level.getBlockState(p);
                        if (here.isAir() || here.canBeReplaced() || !here.getFluidState().isEmpty()) {
                            level.setBlock(p, (y == fillTo ? Blocks.GRASS_BLOCK : y >= fillTo - 3 ? Blocks.DIRT
                                    : Blocks.STONE).defaultBlockState(), 2);
                        }
                    }
                }
                // Cut: clear the land above the plot (it's the hill the building is set into).
                for (int y = cutTo + 1; y <= land + 1 && y <= cutTo + 14; y++) {
                    p.set(x, y, z);
                    BlockState here = level.getBlockState(p);
                    if (StreetPiece.natural(here) || here.canBeReplaced() || here.is(BlockTags.LEAVES)
                            || here.is(BlockTags.LOGS)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                if (land > cutTo) {
                    p.set(x, cutTo, z);
                    BlockState top = level.getBlockState(p);
                    if (top.is(BlockTags.DIRT) || top.is(BlockTags.BASE_STONE_OVERWORLD)) {
                        level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** Leaves that grew there (not part of a building: those are placed persistent). */
    static boolean wildLeaves(BlockState s) {
        return s.is(BlockTags.LEAVES) && s.hasProperty(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)
                && !s.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT);
    }

    /**
     * Cuts back wild leaves crowding the building (trees grow before the swamp towns are built): anything inside the
     * building's footprint above its floor, and a two-block margin round it, from the floor up to the eaves, so no
     * door, window or porch is buried in mangrove.
     */
    private void clearCanopy(WorldGenLevel level, BoundingBox box) {
        BoundingBox bb = this.boundingBox;
        int floor = this.templatePosition.getY();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = Math.max(box.minX(), bb.minX() - 2); x <= Math.min(box.maxX(), bb.maxX() + 2); x++) {
            for (int z = Math.max(box.minZ(), bb.minZ() - 2); z <= Math.min(box.maxZ(), bb.maxZ() + 2); z++) {
                boolean inside = x >= bb.minX() && x <= bb.maxX() && z >= bb.minZ() && z <= bb.maxZ();
                int top = inside ? bb.maxY() + 1 : Math.min(bb.maxY(), floor + 7);
                for (int y = floor; y <= top; y++) {
                    p.set(x, y, z);
                    if (wildLeaves(level.getBlockState(p))) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** Runs each log in the template's bottom layer down through water and air until it reaches the ground. */
    private void extendSupports(WorldGenLevel level, BoundingBox box) {
        int baseY = this.templatePosition.getY();
        for (Block log : new Block[]{Blocks.SPRUCE_LOG, Blocks.OAK_LOG, Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG}) {
            for (StructureTemplate.StructureBlockInfo info : this.template.filterBlocks(this.templatePosition,
                    this.placeSettings, log)) {
                BlockPos p = info.pos();
                if (p.getY() != baseY || !box.isInside(p)) {
                    continue;
                }
                BlockPos.MutableBlockPos q = p.mutable();
                for (int n = 0; n < MAX_STILT; n++) {
                    q.move(0, -1, 0);
                    if (q.getY() <= level.getMinBuildHeight()) {
                        break;
                    }
                    BlockState here = level.getBlockState(q);
                    if (!(here.isAir() || here.canBeReplaced() || !here.getFluidState().isEmpty())) {
                        break;   // reached the swamp floor
                    }
                    level.setBlock(q, info.state(), 2);
                }
            }
        }
    }

    private void reworkGround(WorldGenLevel level, ChunkGenerator generator, BoundingBox box) {
        if (groundRadius <= 0) {
            return;
        }
        Vec3i size = this.template.getSize();
        int cx = this.templatePosition.getX() + size.getX() / 2;
        int cz = this.templatePosition.getZ() + size.getZ() / 2;
        int reach = groundRadius + (int) Math.ceil(FADE) + 1;
        long seed = level.getSeed();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = Math.max(box.minX(), cx - reach); x <= Math.min(box.maxX(), cx + reach); x++) {
            for (int z = Math.max(box.minZ(), cz - reach); z <= Math.min(box.maxZ(), cz + reach); z++) {
                // Seeded by position, so the result is the same whichever chunk is generated first.
                RandomSource r = RandomSource.create(Mth.getSeed(x, 31, z) ^ seed);
                float dist = Mth.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                float edge = groundRadius + r.nextFloat() * 2.5F - 1.5F;
                if (dist > edge + FADE) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                pos.set(x, top, z);
                BlockState ground = level.getBlockState(pos);
                if (!ground.is(BlockTags.DIRT) && ground.getFluidState().isEmpty()) {
                    // Something already built here (a neighboring building in a village) hides the ground:
                    // ask the terrain generator where the land itself is.
                    top = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level,
                            level.getLevel().getChunkSource().randomState()) - 1;
                    pos.set(x, top, z);
                    ground = level.getBlockState(pos);
                }
                if (!ground.is(BlockTags.DIRT)) {
                    continue;   // leave water, sand and stone as they are
                }
                boolean inner = dist <= edge;
                if (inner) {
                    level.setBlock(pos, (r.nextFloat() < 0.12F ? Blocks.PACKED_MUD : Blocks.MUD).defaultBlockState(), 2);
                } else {
                    // The fade: fewer and fewer changed blocks toward the outside.
                    float keep = 1.0F - (dist - edge) / FADE;
                    if (r.nextFloat() > keep) {
                        continue;
                    }
                    float pick = r.nextFloat();
                    level.setBlock(pos, (pick < 0.45F ? Blocks.MUD : pick < 0.75F ? Blocks.ROOTED_DIRT
                            : Blocks.COARSE_DIRT).defaultBlockState(), 2);
                }
                plant(level, pos.above(), r, inner ? 1.0F : 0.4F);
            }
        }
    }

    private static void plant(WorldGenLevel level, BlockPos pos, RandomSource r, float density) {
        if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).canBeReplaced()) {
            return;
        }
        float roll = r.nextFloat() / density;
        BlockState plant;
        if (roll < 0.20F) {
            plant = Blocks.CRIMSON_ROOTS.defaultBlockState();
        } else if (roll < 0.27F) {
            plant = Blocks.POPPY.defaultBlockState();
        } else if (roll < 0.31F) {
            plant = Blocks.RED_TULIP.defaultBlockState();
        } else if (roll < 0.32F) {
            if (level.getBlockState(pos.above()).isAir()) {
                level.setBlock(pos, Blocks.ROSE_BUSH.defaultBlockState()
                        .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), 2);
                level.setBlock(pos.above(), Blocks.ROSE_BUSH.defaultBlockState()
                        .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 2);
            }
            return;
        } else if (roll < 0.335F) {
            plant = Blocks.RED_MUSHROOM.defaultBlockState();
        } else {
            // Clear grass or flowers left from the old ground so the patch reads as one place.
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            return;
        }
        level.setBlock(pos, plant, 2);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random,
                                    BoundingBox box) {
        // The Ithk buildings use no data markers.
    }
}
