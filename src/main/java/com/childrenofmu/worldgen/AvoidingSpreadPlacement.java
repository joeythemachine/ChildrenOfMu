package com.childrenofmu.worldgen;

import java.util.List;
import java.util.Optional;

import com.childrenofmu.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * Vanilla's random spread, but kept clear of several other structure sets at once (vanilla's exclusion zone takes
 * only one). Used for pillager outposts: away from villages as ever, and away from our towns too, so no illager
 * camp sets up under the walls of RCB City or beside a Frog town.
 */
public class AvoidingSpreadPlacement extends RandomSpreadStructurePlacement {
    /** One set to keep clear of, and by how many chunks. */
    public record Avoid(Holder<StructureSet> set, int chunks) {
        public static final Codec<Avoid> CODEC = RecordCodecBuilder.create(i -> i.group(
                RegistryFileCodec.create(Registries.STRUCTURE_SET, StructureSet.DIRECT_CODEC, false).fieldOf("set")
                        .forGetter(Avoid::set),
                Codec.intRange(1, 32).fieldOf("chunk_count").forGetter(Avoid::chunks)
        ).apply(i, Avoid::new));
    }

    public static final MapCodec<AvoidingSpreadPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO).forGetter(AvoidingSpreadPlacement::locateOffset),
            FrequencyReductionMethod.CODEC.optionalFieldOf("frequency_reduction_method", FrequencyReductionMethod.DEFAULT)
                    .forGetter(AvoidingSpreadPlacement::frequencyReductionMethod),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F).forGetter(AvoidingSpreadPlacement::frequency),
            net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(AvoidingSpreadPlacement::salt),
            ExclusionZone.CODEC.optionalFieldOf("exclusion_zone").forGetter(AvoidingSpreadPlacement::exclusionZone),
            Codec.intRange(0, 4096).fieldOf("spacing").forGetter(AvoidingSpreadPlacement::spacing),
            Codec.intRange(0, 4096).fieldOf("separation").forGetter(AvoidingSpreadPlacement::separation),
            RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(AvoidingSpreadPlacement::spreadType),
            Avoid.CODEC.listOf().optionalFieldOf("avoid", List.of()).forGetter(p -> p.avoid)
    ).apply(i, AvoidingSpreadPlacement::new));

    private final List<Avoid> avoid;

    public AvoidingSpreadPlacement(Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt,
                                   Optional<ExclusionZone> exclusionZone, int spacing, int separation,
                                   RandomSpreadType spreadType, List<Avoid> avoid) {
        super(locateOffset, method, frequency, salt, exclusionZone, spacing, separation, spreadType);
        this.avoid = avoid;
    }

    @Override
    public boolean applyInteractionsWithOtherStructures(ChunkGeneratorStructureState state, int x, int z) {
        if (!super.applyInteractionsWithOtherStructures(state, x, z)) {
            return false;
        }
        for (Avoid a : this.avoid) {
            if (state.hasStructureChunkInRange(a.set(), x, z, a.chunks())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModStructures.AVOIDING_SPREAD.get();
    }
}
