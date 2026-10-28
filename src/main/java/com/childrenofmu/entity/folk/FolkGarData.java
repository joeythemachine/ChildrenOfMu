package com.childrenofmu.entity.folk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * What each RCB City remembers about its Great Gar, kept with the world: whether he's dead and since when, where his
 * throne is, which "epoch" of writs is good (a new Gar voids every writ the old one sealed), and who killed him (the
 * city stays hostile to them for a while).
 */
public final class FolkGarData extends SavedData {
    static final class City {
        long deadSince = -1;
        @Nullable BlockPos throne;
        int epoch;
        final Map<UUID, Long> enemies = new HashMap<>();
    }

    private final Map<Long, City> cities = new HashMap<>();

    static FolkGarData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(FolkGarData::new, FolkGarData::load), "childrenofmu_gar");
    }

    City city(long key) {
        return this.cities.computeIfAbsent(key, k -> new City());
    }

    @Nullable
    City peek(long key) {
        return this.cities.get(key);
    }

    static FolkGarData load(CompoundTag tag, HolderLookup.Provider registries) {
        FolkGarData d = new FolkGarData();
        ListTag list = tag.getList("Cities", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            City city = d.city(c.getLong("Key"));
            city.deadSince = c.getLong("DeadSince");
            city.epoch = c.getInt("Epoch");
            if (c.contains("Throne")) {
                city.throne = BlockPos.of(c.getLong("Throne"));
            }
            ListTag en = c.getList("Enemies", Tag.TAG_COMPOUND);
            for (int j = 0; j < en.size(); j++) {
                CompoundTag e = en.getCompound(j);
                city.enemies.put(e.getUUID("Id"), e.getLong("Until"));
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        this.cities.forEach((key, city) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("Key", key);
            c.putLong("DeadSince", city.deadSince);
            c.putInt("Epoch", city.epoch);
            if (city.throne != null) {
                c.putLong("Throne", city.throne.asLong());
            }
            ListTag en = new ListTag();
            city.enemies.forEach((id, until) -> {
                CompoundTag e = new CompoundTag();
                e.putUUID("Id", id);
                e.putLong("Until", until);
                en.add(e);
            });
            c.put("Enemies", en);
            list.add(c);
        });
        tag.put("Cities", list);
        return tag;
    }
}
