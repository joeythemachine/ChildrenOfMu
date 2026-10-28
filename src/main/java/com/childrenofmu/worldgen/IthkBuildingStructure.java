package com.childrenofmu.worldgen;

import java.util.Optional;

import com.childrenofmu.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A single Ithk building. Unlike a jigsaw structure it leaves the land alone: the building floats at a height
 * chosen from the terrain around it, is turned to a random side, and the ground below is reworked in place
 * (see {@link IthkBuildingPiece}) instead of being flattened into a platform.
 */
public class IthkBuildingStructure extends Structure {
    public static final MapCodec<IthkBuildingStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
            Codec.intRange(0, 32).fieldOf("ground_radius").forGetter(s -> s.groundRadius),
            Codec.intRange(0, 16).optionalFieldOf("sink", 0).forGetter(s -> s.sink),
            Codec.BOOL.optionalFieldOf("follow_slopes", true).forGetter(s -> s.followSlopes),
            Codec.BOOL.optionalFieldOf("extend_supports", false).forGetter(s -> s.supports),
            Codec.BOOL.optionalFieldOf("level_ground", false).forGetter(s -> s.levelGround)
    ).apply(instance, IthkBuildingStructure::new));

    /** How far from the center the terrain is sampled when choosing the height. */
    private static final int SAMPLE = 6;
    /** The building never rises more than this far above the center's ground to clear a nearby hill. */
    private static final int MAX_LIFT = 6;

    private final ResourceLocation template;
    private final int groundRadius;
    /** How many of the template's bottom layers sit below ground (a foundation, for things that stand on it). */
    private final int sink;
    /** Floating buildings rise to clear nearby slopes; grounded ones stay at the center's ground level. */
    private final boolean followSlopes;
    /** Run the bottom layer's logs down to the ground: stilts. */
    private final boolean supports;
    /** Level a plot for it first: fill under it and round it, cut back the hill above it, and bank the edges. */
    private final boolean levelGround;

    public IthkBuildingStructure(StructureSettings settings, ResourceLocation template, int groundRadius, int sink,
                                 boolean followSlopes, boolean supports, boolean levelGround) {
        super(settings);
        this.template = template;
        this.groundRadius = groundRadius;
        this.sink = sink;
        this.followSlopes = followSlopes;
        this.supports = supports;
        this.levelGround = levelGround;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();
        int y = levelGround ? averageHeight(context, cx, cz) - sink : heightAt(context, cx, cz, followSlopes, sink);

        Rotation rotation = Rotation.getRandom(context.random());
        BlockPos centerPos = new BlockPos(cx, y, cz);
        return Optional.of(new GenerationStub(centerPos, builder -> builder.addPiece(
                new IthkBuildingPiece(context.structureTemplateManager(), template, centerPos, rotation, groundRadius,
                        supports, levelGround ? sink : -1))));
    }

    /** The middle of the land around x, z (center and four points around it), for a plot levelled to suit. */
    static int averageHeight(GenerationContext context, int x, int z) {
        int sum = surface(context, x, z) * 2;
        for (int[] d : new int[][]{{SAMPLE, 0}, {-SAMPLE, 0}, {0, SAMPLE}, {0, -SAMPLE}}) {
            sum += surface(context, x + d[0], z + d[1]);
        }
        return Math.round(sum / 6.0F);
    }

    /**
     * The height to place a building's origin (template y = 0) centered on x, z. Floating buildings sample around
     * the center and rise (a little) to clear nearby slopes; a sunk building sets its bottom layers into the ground.
     */
    public static int heightAt(GenerationContext context, int x, int z, boolean followSlopes, int sink) {
        int center = surface(context, x, z);
        int y = center;
        if (followSlopes) {
            for (int[] d : new int[][]{{SAMPLE, 0}, {-SAMPLE, 0}, {0, SAMPLE}, {0, -SAMPLE}}) {
                y = Math.max(y, surface(context, x + d[0], z + d[1]));
            }
        }
        return Math.min(y, center + MAX_LIFT) - sink;
    }

    static int surface(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
    }

    /** True if the ground at x, z is under water. */
    static boolean isWet(GenerationContext context, int x, int z) {
        return surface(context, x, z) != context.chunkGenerator().getFirstFreeHeight(x, z,
                Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ITHK_BUILDING.get();
    }
}
