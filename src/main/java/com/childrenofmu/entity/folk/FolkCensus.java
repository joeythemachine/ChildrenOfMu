package com.childrenofmu.entity.folk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.childrenofmu.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Keeping the numbers of RCB City steady. Every Cat, Rat and Bat the city was built with has a place in it (their
 * post, their trade); when one of them dies, a baby of their kind turns up at that place within a quarter of a day
 * and, a day later, has grown into the job. And when there are more of a kind about town than the city has places for
 * (strays, the odd extra cop), the newest of them packs up and goes adventuring out in the world.
 */
public class FolkCensus extends SavedData {
    private static final String NAME = "childrenofmu_census";
    /** How long a place stays empty before a baby turns up in it. */
    static final long BABY_DELAY = 6000L;
    /** How long a baby takes to grow up. */
    static final long GROW = 24000L;
    /** How many over the city's places a kind may run before somebody leaves. */
    private static final int MARGIN = 2;

    static final class Slot {
        int id;
        String race;
        String variant;
        BlockPos home;
        int radius;
        @Nullable UUID occupant;
        long vacantSince = -1L;
    }

    static final class City {
        long key;
        BlockPos center;
        int radius;
        final List<Slot> slots = new ArrayList<>();
        long lastCheck;
    }

    private final Map<Long, City> cities = new HashMap<>();

    public static FolkCensus get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(FolkCensus::new, FolkCensus::load, null), NAME);
    }

    @Nullable
    private static StructureStart start(ServerLevel level, BlockPos pos) {
        Structure s = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(FolkLaw.RCB_CITY);
        if (s == null || !level.isLoaded(pos)) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureAt(pos, s);
        return start.isValid() ? start : null;
    }

    /** Who counts: the city's own Cats, Rats and Bats (not the Gar, travellers, fighters, babies or adventurers). */
    static boolean counts(FolkEntity e) {
        return FolkLaw.isCityFolk(e) && !FolkGar.isGar(e) && !e.isTraveller() && !e.pitFighter && !e.isBaby()
                && !e.isAdventurer() && e.isAlive();
    }

    /** A citizen the city was built with (first seen with a home inside the walls): give them their place. */
    static void register(ServerLevel level, FolkEntity e) {
        BlockPos home = e.homePos();
        if (home == null || !counts(e) || !FolkLaw.insideWalls(level, home)) {
            return;
        }
        StructureStart start = start(level, home);
        if (start == null) {
            return;
        }
        FolkCensus census = get(level);
        long key = start.getChunkPos().toLong();
        City city = census.cities.computeIfAbsent(key, k -> {
            City c = new City();
            c.key = k;
            BoundingBox bb = start.getBoundingBox();
            c.center = bb.getCenter();
            c.radius = Math.max(bb.getXSpan(), bb.getZSpan()) / 2 + 8;
            return c;
        });
        Slot slot = new Slot();
        slot.id = city.slots.size();
        slot.race = e.getRace().name();
        slot.variant = e.getVariantName();
        slot.home = home;
        slot.radius = e.homeRadius();
        slot.occupant = e.getUUID();
        city.slots.add(slot);
        e.slotCity = key;
        e.slotId = slot.id;
        census.setDirty();
    }

    /** A citizen died: their place is empty, for now. */
    static void died(ServerLevel level, FolkEntity e) {
        if (e.slotId < 0) {
            return;
        }
        FolkCensus census = get(level);
        City city = census.cities.get(e.slotCity);
        if (city == null || e.slotId >= city.slots.size()) {
            return;
        }
        Slot slot = city.slots.get(e.slotId);
        if (slot.occupant != null && slot.occupant.equals(e.getUUID())) {
            slot.occupant = null;
            slot.vacantSince = level.getGameTime();
            census.setDirty();
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 200L != 37L) {
            return;
        }
        FolkCensus census = get(level);
        if (census.cities.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (City city : census.cities.values()) {
            if (!level.isPositionEntityTicking(city.center)) {
                continue;
            }
            for (Slot slot : city.slots) {
                if (slot.occupant == null && slot.vacantSince >= 0 && now - slot.vacantSince >= BABY_DELAY
                        && level.isPositionEntityTicking(slot.home)) {
                    born(level, city, slot);
                }
            }
            if (now - city.lastCheck >= 1200L) {
                city.lastCheck = now;
                crowding(level, city);
            }
        }
    }

    @Nullable
    private static EntityType<FolkEntity> type(String race) {
        return switch (race) {
            case "RAT" -> ModEntities.RAT_FOLK.get();
            case "CAT" -> ModEntities.CAT_FOLK.get();
            case "BAT" -> ModEntities.BAT_FOLK.get();
            default -> null;
        };
    }

    /** A baby turns up in an empty place, to grow into it. */
    private static void born(ServerLevel level, City city, Slot slot) {
        EntityType<FolkEntity> type = type(slot.race);
        FolkEntity baby = type == null ? null : type.create(level);
        if (baby == null) {
            return;
        }
        baby.presetVariant(slot.variant);
        baby.moveTo(slot.home.getX() + 0.5, slot.home.getY(), slot.home.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        baby.finalizeSpawn(level, level.getCurrentDifficultyAt(slot.home), MobSpawnType.BREEDING, null);
        baby.setHome(slot.home, slot.radius);
        baby.setBaby(level.getGameTime() + GROW);
        baby.slotCity = city.key;
        baby.slotId = slot.id;
        baby.setPersistenceRequired();
        if (level.addFreshEntity(baby)) {
            slot.occupant = baby.getUUID();
            slot.vacantSince = -1L;
            level.sendParticles(ParticleTypes.HEART, baby.getX(), baby.getY() + 0.6, baby.getZ(), 4, 0.3, 0.3, 0.3, 0.0);
            get(level).setDirty();
        }
    }

    /** Too many of a kind about town: the newest one without a place leaves to go adventuring. */
    private static void crowding(ServerLevel level, City city) {
        Map<String, Integer> places = new HashMap<>();
        for (Slot s : city.slots) {
            places.merge(s.race, 1, Integer::sum);
        }
        AABB area = new AABB(city.center).inflate(city.radius, 48, city.radius);
        Map<String, List<FolkEntity>> about = new HashMap<>();
        for (FolkEntity f : level.getEntitiesOfClass(FolkEntity.class, area, FolkCensus::counts)) {
            about.computeIfAbsent(f.getRace().name(), k -> new ArrayList<>()).add(f);
        }
        for (Map.Entry<String, List<FolkEntity>> e : about.entrySet()) {
            List<FolkEntity> folk = e.getValue();
            if (folk.size() <= places.getOrDefault(e.getKey(), 0) + MARGIN) {
                continue;
            }
            FolkEntity leaver = null;
            for (FolkEntity f : folk) {
                if (f.slotId < 0 && f.wanted == null && f.getTarget() == null
                        && (leaver == null || f.tickCount < leaver.tickCount)) {
                    leaver = f;
                }
            }
            if (leaver != null) {
                leaver.becomeAdventurer();
            }
        }
    }

    // ---- Saving ----------------------------------------------------------------------------------

    private static FolkCensus load(CompoundTag tag, HolderLookup.Provider registries) {
        FolkCensus c = new FolkCensus();
        ListTag list = tag.getList("Cities", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag ct = list.getCompound(i);
            City city = new City();
            city.key = ct.getLong("Key");
            city.center = BlockPos.of(ct.getLong("Center"));
            city.radius = ct.getInt("Radius");
            ListTag slots = ct.getList("Slots", Tag.TAG_COMPOUND);
            for (int j = 0; j < slots.size(); j++) {
                CompoundTag st = slots.getCompound(j);
                Slot s = new Slot();
                s.id = j;
                s.race = st.getString("Race");
                s.variant = st.getString("Variant");
                s.home = BlockPos.of(st.getLong("Home"));
                s.radius = st.getInt("Radius");
                s.occupant = st.hasUUID("Occupant") ? st.getUUID("Occupant") : null;
                s.vacantSince = st.getLong("VacantSince");
                city.slots.add(s);
            }
            c.cities.put(city.key, city);
        }
        return c;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (City city : this.cities.values()) {
            CompoundTag ct = new CompoundTag();
            ct.putLong("Key", city.key);
            ct.putLong("Center", city.center.asLong());
            ct.putInt("Radius", city.radius);
            ListTag slots = new ListTag();
            for (Slot s : city.slots) {
                CompoundTag st = new CompoundTag();
                st.putString("Race", s.race);
                st.putString("Variant", s.variant);
                st.putLong("Home", s.home.asLong());
                st.putInt("Radius", s.radius);
                if (s.occupant != null) {
                    st.putUUID("Occupant", s.occupant);
                }
                st.putLong("VacantSince", s.vacantSince);
                slots.add(st);
            }
            ct.put("Slots", slots);
            list.add(ct);
        }
        tag.put("Cities", list);
        return tag;
    }
}
