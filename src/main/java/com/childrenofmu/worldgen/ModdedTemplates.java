package com.childrenofmu.worldgen;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.fml.ModList;

/**
 * Some buildings come in two versions: one decorated with blocks from other mods (Quark, Farmer's Delight,
 * Supplementaries, Domum Ornamentum's shingles and timber frames), and a "_vanilla" twin where each of those blocks is swapped for the nearest
 * vanilla block. The decorated one is used when all those mods are installed, the twin otherwise, so a building
 * never comes out with holes where a missing mod's blocks should be. tools/frog_build.py writes both versions.
 */
public final class ModdedTemplates {
    /** Mods the decorated versions draw on. */
    public static final List<String> DECOR_MODS = List.of("quark", "farmersdelight", "supplementaries", "domum_ornamentum");

    private ModdedTemplates() {}

    public static boolean decorModsLoaded() {
        ModList mods = ModList.get();
        return mods == null || DECOR_MODS.stream().allMatch(mods::isLoaded);
    }

    /** The template to actually build for this one: its vanilla twin if it has one and a decor mod is missing. */
    public static ResourceLocation pick(StructureTemplateManager manager, ResourceLocation template) {
        if (decorModsLoaded()) {
            return template;
        }
        ResourceLocation twin = ResourceLocation.fromNamespaceAndPath(template.getNamespace(), template.getPath() + "_vanilla");
        return manager.get(twin).isPresent() ? twin : template;
    }
}
