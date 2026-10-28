package com.childrenofmu.registry;

import com.childrenofmu.ChildrenOfMu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChildrenOfMu.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.childrenofmu"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.ITHK_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ITHK_SPAWN_EGG.get());
                        output.accept(ModItems.RAT_FOLK_SPAWN_EGG.get());
                        output.accept(ModItems.CAT_FOLK_SPAWN_EGG.get());
                        output.accept(ModItems.BAT_FOLK_SPAWN_EGG.get());
                        output.accept(ModItems.DOG_FOLK_SPAWN_EGG.get());
                        output.accept(ModItems.FROG_FOLK_SPAWN_EGG.get());
                        output.accept(ModItems.GOXA_SPAWN_EGG.get());
                        output.accept(ModItems.LILBUN_SPAWN_EGG.get());
                        output.accept(ModItems.LEVITATION_SHARD.get());
                        output.accept(ModItems.AMETHYST_STAIRS.get());
                        output.accept(ModItems.AMETHYST_SLAB.get());
                        output.accept(ModItems.INLAID_EMERALD.get());
                        output.accept(ModItems.INLAID_GOLD.get());
                        output.accept(ModItems.INLAID_LAPIS.get());
                        output.accept(ModItems.POST_BOX.get());
                        output.accept(ModItems.REED_THATCH.get());
                        output.accept(ModItems.REED_THATCH_STAIRS.get());
                        output.accept(ModItems.REED_THATCH_SLAB.get());
                        output.accept(ModItems.REED_SCREEN.get());
                        output.accept(ModItems.LILY_LANTERN.get());
                        output.accept(ModItems.FROG_IDOL.get());
                        output.accept(ModItems.FISHING_NET.get());
                        output.accept(ModItems.CATTAILS.get());
                        output.accept(ModItems.REED_MAT.get());
                        output.accept(ModItems.PINK_SLIME.get());
                        output.accept(ModItems.STRUNG_FISH.get());
                        output.accept(ModItems.STRUNG_BUGS.get());
                        output.accept(ModItems.HONEY_GLAZED_BEETLE.get());
                        output.accept(ModItems.CRICKET_CRUMBLE.get());
                        output.accept(ModItems.FIREFLY_FLAMBE.get());
                        output.accept(ModItems.DRAGONFLY_TARTARE.get());
                        output.accept(ModItems.MOTH_SOUFFLE.get());
                        output.accept(ModItems.BOG_SNAIL_ESCARGOT.get());
                        output.accept(ModItems.MOSQUITO_CONSOMME.get());
                        output.accept(ModItems.CALF.get());
                        output.accept(ModItems.CALF_BITE.get());
                        output.accept(ModItems.FERMENTED_TUNA.get());
                        output.accept(ModItems.MANHOLE.get());
                        output.accept(ModItems.BANKERS_LAMP.get());
                        output.accept(ModItems.WRIT_OF_TRADE.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
