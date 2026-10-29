package com.childrenofmu;

import com.childrenofmu.client.ClientSetup;
import com.childrenofmu.registry.ModBlocks;
import com.childrenofmu.registry.ModCreativeTabs;
import com.childrenofmu.registry.ModEntities;
import com.childrenofmu.registry.ModItems;
import com.childrenofmu.registry.ModStructures;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(ChildrenOfMu.MODID)
public class ChildrenOfMu {
    public static final String MODID = "childrenofmu";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChildrenOfMu(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModStructures.STRUCTURE_TYPES.register(modEventBus);
        ModStructures.PIECE_TYPES.register(modEventBus);
        ModStructures.PLACEMENT_TYPES.register(modEventBus);

        modEventBus.addListener(ModEntities::registerAttributes);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.post.FolkPost::onRegisterCommands);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.post.FolkPost::onServerTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.FolkKitchen::onRegisterCommands);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.FolkCombat::onJoin);
        // RCB City: pit-fight betting and the Cat cops' law.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.FolkPit::onRegisterCommands);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.DogTrust::onRegisterCommands);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.DogTrust::onDeath);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.DogTrust::onPlayerTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.FolkLaw::onBreak);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.FolkLaw::onOpen);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.FolkLaw::onClose);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.FolkLaw::onHurt);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.FolkLaw::onDeath);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.FolkLaw::onServerStopping);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.childrenofmu.entity.folk.FolkCensus::onLevelTick);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modEventBus);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
