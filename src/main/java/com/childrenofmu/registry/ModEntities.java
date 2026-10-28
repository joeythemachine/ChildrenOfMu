package com.childrenofmu.registry;

import com.childrenofmu.ChildrenOfMu;
import com.childrenofmu.entity.ithk.IthkEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.childrenofmu.entity.folk.FolkEntity;
import com.childrenofmu.entity.folk.FolkRace;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ChildrenOfMu.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<IthkEntity>> ITHK = ENTITIES.register("ithk",
            () -> EntityType.Builder.of(IthkEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.2F)  // covers the eye as drawn, floating above its feet
                    .clientTrackingRange(10)
                    .build("ithk"));

    // The walking folk: one entity class, one type per race.
    public static final DeferredHolder<EntityType<?>, EntityType<FolkEntity>> RAT_FOLK = folk("rat_folk", FolkRace.RAT);
    public static final DeferredHolder<EntityType<?>, EntityType<FolkEntity>> CAT_FOLK = folk("cat_folk", FolkRace.CAT);
    public static final DeferredHolder<EntityType<?>, EntityType<FolkEntity>> BAT_FOLK = folk("bat_folk", FolkRace.BAT);
    public static final DeferredHolder<EntityType<?>, EntityType<FolkEntity>> DOG_FOLK = folk("dog_folk", FolkRace.DOG);
    public static final DeferredHolder<EntityType<?>, EntityType<FolkEntity>> FROG_FOLK = folk("frog_folk", FolkRace.FROG);
    public static final DeferredHolder<EntityType<?>, EntityType<FolkEntity>> GOXA = folk("goxa", FolkRace.GOXA);
    public static final DeferredHolder<EntityType<?>, EntityType<FolkEntity>> LILBUN = folk("lilbun", FolkRace.LILBUN);

    private static DeferredHolder<EntityType<?>, EntityType<FolkEntity>> folk(String name, FolkRace race) {
        return ENTITIES.register(name, () -> EntityType.Builder.<FolkEntity>of(
                        (type, level) -> new FolkEntity(type, level, race), MobCategory.CREATURE)
                .sized(race.width(), race.height())
                .clientTrackingRange(10)
                .build(name));
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ITHK.get(), IthkEntity.createAttributes().build());
        event.put(RAT_FOLK.get(), FolkEntity.createAttributes(FolkRace.RAT).build());
        event.put(CAT_FOLK.get(), FolkEntity.createAttributes(FolkRace.CAT).build());
        event.put(BAT_FOLK.get(), FolkEntity.createAttributes(FolkRace.BAT).build());
        event.put(DOG_FOLK.get(), FolkEntity.createAttributes(FolkRace.DOG).build());
        event.put(FROG_FOLK.get(), FolkEntity.createAttributes(FolkRace.FROG).build());
        event.put(GOXA.get(), FolkEntity.createAttributes(FolkRace.GOXA).build());
        event.put(LILBUN.get(), FolkEntity.createAttributes(FolkRace.LILBUN).build());
    }

    private ModEntities() {}
}
