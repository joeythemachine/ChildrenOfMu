package com.childrenofmu.client;

import com.childrenofmu.client.model.FolkModel;
import com.childrenofmu.client.model.IthkCostumeModel;
import com.childrenofmu.client.model.IthkModel;
import com.childrenofmu.client.renderer.FolkRenderer;
import com.childrenofmu.client.renderer.IthkRenderer;
import com.childrenofmu.entity.folk.FolkRace;
import com.childrenofmu.registry.ModEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only registration. Only called when running on a client. */
public final class ClientSetup {

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientSetup::registerLayers);
        modEventBus.addListener(ClientSetup::registerRenderers);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(IthkModel.LAYER, IthkModel::createBodyLayer);
        event.registerLayerDefinition(IthkCostumeModel.LAYER, IthkCostumeModel::createLayer);
        for (FolkRace race : FolkRace.values()) {
            event.registerLayerDefinition(FolkModel.layer(race), () -> FolkModel.createLayer(race));
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ITHK.get(), IthkRenderer::new);
        event.registerEntityRenderer(ModEntities.RAT_FOLK.get(), ctx -> new FolkRenderer(ctx, FolkRace.RAT));
        event.registerEntityRenderer(ModEntities.CAT_FOLK.get(), ctx -> new FolkRenderer(ctx, FolkRace.CAT));
        event.registerEntityRenderer(ModEntities.BAT_FOLK.get(), ctx -> new FolkRenderer(ctx, FolkRace.BAT));
        event.registerEntityRenderer(ModEntities.DOG_FOLK.get(), ctx -> new FolkRenderer(ctx, FolkRace.DOG));
        event.registerEntityRenderer(ModEntities.FROG_FOLK.get(), ctx -> new FolkRenderer(ctx, FolkRace.FROG));
        event.registerEntityRenderer(ModEntities.GOXA.get(), ctx -> new FolkRenderer(ctx, FolkRace.GOXA));
        event.registerEntityRenderer(ModEntities.LILBUN.get(), ctx -> new FolkRenderer(ctx, FolkRace.LILBUN));
    }

    private ClientSetup() {}
}
