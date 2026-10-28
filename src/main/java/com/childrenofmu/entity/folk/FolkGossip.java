package com.childrenofmu.entity.folk;

import java.util.List;

import com.childrenofmu.ChildrenOfMu;
import com.childrenofmu.post.PostOffice;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * What the Frog Guards gossip about, and what the Elders remember. A Guard's gossip is a mix of town chatter, news
 * passed along from other frog towns, and real rumors: which way the nearest Ithk village lies, and how far.
 */
public final class FolkGossip {
    private static final int TOWN_LINES = 18;
    private static final int AFAR_LINES = 8;
    private static final int ELDER_LINES = 10;
    private static final TagKey<Structure> ITHK_VILLAGES =
            TagKey.create(Registries.STRUCTURE, ChildrenOfMu.id("ithk_villages"));
    private static final String[] DIRECTIONS = {"south", "south_west", "west", "north_west", "north", "north_east",
            "east", "south_east"};

    public static void speak(FolkEntity frog, Player player) {
        if (!(frog.level() instanceof ServerLevel level)) {
            return;
        }
        RandomSource r = frog.getRandom();
        Component line;
        if ("elder".equals(frog.getVariantName())) {
            line = Component.translatable("gossip.childrenofmu.elder." + r.nextInt(ELDER_LINES));
        } else {
            int roll = r.nextInt(10);
            Component rumor = roll == 0 ? ithkRumor(level, frog.blockPosition()) : null;
            List<String> towns = PostOffice.get(level.getServer()).towns();
            if (rumor != null) {
                line = rumor;
            } else if (roll < 4 && !towns.isEmpty()) {
                String town = towns.get(r.nextInt(towns.size()));
                line = Component.translatable("gossip.childrenofmu.afar." + r.nextInt(AFAR_LINES), town);
            } else {
                line = Component.translatable("gossip.childrenofmu.town." + r.nextInt(TOWN_LINES));
            }
        }
        player.sendSystemMessage(Component.translatable("gossip.childrenofmu.says", frog.getDisplayName(), line));
    }

    /** "They say the eyeball folk float over the land to the north-east, a few hundred blocks off." */
    private static Component ithkRumor(ServerLevel level, BlockPos from) {
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                        .getTag(ITHK_VILLAGES).orElseThrow(), from, 64, false);
        if (found == null) {
            return null;
        }
        BlockPos at = found.getFirst();
        double dx = at.getX() - from.getX(), dz = at.getZ() - from.getZ();
        int distance = (int) Math.sqrt(dx * dx + dz * dz);
        float angle = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);    // 0 = south, 90 = west, as Minecraft yaw
        int sector = Math.floorMod(Math.round(angle / 45.0F), 8);
        Component direction = Component.translatable("gossip.childrenofmu.dir." + DIRECTIONS[sector]);
        String far = distance < 400 ? "near" : distance < 1500 ? "mid" : "far";
        return Component.translatable("gossip.childrenofmu.ithk." + far, direction, (distance / 50) * 50);
    }

    private FolkGossip() {}
}
