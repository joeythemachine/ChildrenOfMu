package com.childrenofmu.entity.ithk;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.childrenofmu.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Every Ithk trade. Each profession has a pool of possible trades per level (1 = Novice ... 5 = Master);
 * each Ithk draws two at random per level, with randomized amounts, so no two are quite alike.
 *
 * The Ithk trade rare things for equally rare things. Guidelines used here:
 *  - Buying from the Ithk is expensive, but what you buy is hard or impossible to get otherwise.
 *  - Selling a true rarity to the Ithk (a dragon head, a heavy core, a rare disc) earns something of equal
 *    standing in return, never a token handful of diamonds.
 *  - Bulk common materials only convert at steep ratios, so farms can't trivially mint treasure.
 *
 * How to read a line:
 *     offer(cost(Items.COAL_BLOCK, 24, 32), out(Items.DIAMOND, 1, 1), 4)
 * means: give 24-32 coal blocks (rolled once per Ithk), get 1 diamond, up to 4 times before it restocks.
 * offer(costA, costB, result, uses) takes two things. "Any(...)" picks one of a list, also once per Ithk.
 * Changes only affect Ithk that reach a level after the change.
 */
public final class IthkTrades {

    /** Produces one concrete offer, rolling its random parts. */
    @FunctionalInterface
    public interface Option {
        MerchantOffer create(RandomSource random, RegistryAccess registries);
    }

    private static final Map<IthkProfession, List<List<Option>>> POOLS = new EnumMap<>(IthkProfession.class);

    private static final Option LEVITATION_SHARD = (r, reg) -> new MerchantOffer(
            new ItemCost(Items.PHANTOM_MEMBRANE, between(r, 4, 6)),
            Optional.of(new ItemCost(Items.DIAMOND, between(r, 3, 5))),
            new ItemStack(ModItems.LEVITATION_SHARD.get()), 2, xpFor(1), 0.0F);

    public static final int TRADES_PER_LEVEL = 2;

    /** Draws this level's trades for one Ithk. */
    public static List<MerchantOffer> forLevel(IthkProfession profession, int level, RegistryAccess registries,
                                               RandomSource random) {
        List<MerchantOffer> out = new ArrayList<>();
        List<List<Option>> levels = POOLS.get(profession);
        if (levels == null || level < 1 || level > levels.size()) {
            return out;
        }
        if (profession == IthkProfession.GATEKEEPER && level == 1) {
            // Every Gatekeeper offers the Levitation Shard, so the top of the tower is always reachable.
            out.add(LEVITATION_SHARD.create(random, registries));
        }
        List<Option> pool = new ArrayList<>(levels.get(level - 1));
        for (int i = 0; i < TRADES_PER_LEVEL && !pool.isEmpty(); i++) {
            Option pick = pool.remove(random.nextInt(pool.size()));
            out.add(pick.create(random, registries));
        }
        return out;
    }

    // =====================================================================================
    // Item groups
    // =====================================================================================

    private static final List<Item> COMMON_DISCS = List.of(
            Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_CHIRP,
            Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL, Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL,
            Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD, Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT);

    private static final List<Item> RARE_DISCS = List.of(
            Items.MUSIC_DISC_OTHERSIDE, Items.MUSIC_DISC_PIGSTEP, Items.MUSIC_DISC_RELIC,
            Items.MUSIC_DISC_CREATOR, Items.MUSIC_DISC_CREATOR_MUSIC_BOX, Items.MUSIC_DISC_PRECIPICE);

    private static final List<Item> SHERDS = List.of(
            Items.ANGLER_POTTERY_SHERD, Items.ARCHER_POTTERY_SHERD, Items.ARMS_UP_POTTERY_SHERD,
            Items.BLADE_POTTERY_SHERD, Items.BREWER_POTTERY_SHERD, Items.BURN_POTTERY_SHERD,
            Items.DANGER_POTTERY_SHERD, Items.EXPLORER_POTTERY_SHERD, Items.FLOW_POTTERY_SHERD,
            Items.FRIEND_POTTERY_SHERD, Items.GUSTER_POTTERY_SHERD, Items.HEART_POTTERY_SHERD,
            Items.HEARTBREAK_POTTERY_SHERD, Items.HOWL_POTTERY_SHERD, Items.MINER_POTTERY_SHERD,
            Items.MOURNER_POTTERY_SHERD, Items.PLENTY_POTTERY_SHERD, Items.PRIZE_POTTERY_SHERD,
            Items.SCRAPE_POTTERY_SHERD, Items.SHEAF_POTTERY_SHERD, Items.SHELTER_POTTERY_SHERD,
            Items.SKULL_POTTERY_SHERD, Items.SNORT_POTTERY_SHERD);

    private static final List<Item> TRIMS = List.of(
            Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);

    private static final List<Item> MOB_HEADS = List.of(
            Items.CREEPER_HEAD, Items.ZOMBIE_HEAD, Items.SKELETON_SKULL, Items.PIGLIN_HEAD);

    // =====================================================================================
    // Pools
    // =====================================================================================

    static {
        // ---- Transmuter: matter rearranged. Bulk common materials in, rarer matter out. ----
        pool(IthkProfession.TRANSMUTER,
                List.of( // 1
                        offer(cost(Items.COAL_BLOCK, 24, 32), out(Items.DIAMOND, 1, 1), 4),
                        offer(cost(Items.COPPER_BLOCK, 32, 48), out(Items.DIAMOND, 1, 1), 4),
                        offer(cost(Items.REDSTONE_BLOCK, 16, 24), out(Items.DIAMOND, 1, 1), 4),
                        offer(cost(Items.LAPIS_BLOCK, 16, 24), cost(Items.AMETHYST_SHARD, 8, 16), out(Items.EMERALD_BLOCK, 1, 1), 4),
                        offer(cost(Items.QUARTZ_BLOCK, 24, 32), out(Items.PRISMARINE_CRYSTALS, 6, 10), 4)),
                List.of( // 2
                        offer(cost(Items.GOLD_BLOCK, 24, 32), out(Items.ANCIENT_DEBRIS, 1, 1), 3),
                        offer(cost(Items.OBSIDIAN, 16, 24), cost(Items.DIAMOND, 6, 10), out(Items.CRYING_OBSIDIAN, 12, 16), 3),
                        offer(cost(Items.AMETHYST_SHARD, 32, 48), cost(Items.SCULK, 16, 32), out(Items.ECHO_SHARD, 1, 1), 3),
                        offer(cost(Items.CLAY, 32, 48), cost(Items.DIAMOND, 2, 4), out(Items.SNIFFER_EGG, 1, 1), 1)),
                List.of( // 3
                        offer(cost(Items.DIAMOND, 8, 12), cost(Items.ECHO_SHARD, 1, 2), out(Items.NETHERITE_SCRAP, 1, 1), 2),
                        offer(cost(Items.AMETHYST_SHARD, 48, 64), cost(Items.DIAMOND_BLOCK, 1, 2), out(Items.BUDDING_AMETHYST, 1, 1), 2),
                        offer(cost(Items.BONE_BLOCK, 32, 48), cost(Items.DIAMOND, 10, 16), out(Items.WITHER_SKELETON_SKULL, 1, 1), 1),
                        offer(cost(Items.PRISMARINE_SHARD, 48, 64), cost(Items.DIAMOND, 8, 12), out(Items.HEART_OF_THE_SEA, 1, 1), 1)),
                List.of( // 4
                        offer(cost(Items.NETHER_STAR, 1, 1), cost(Items.DIAMOND, 12, 20), out(Items.NETHERITE_INGOT, 1, 1), 2),
                        offer(cost(Items.IRON_BLOCK, 32, 48), cost(Items.DIAMOND_BLOCK, 6, 10), out(Items.HEAVY_CORE, 1, 1), 1),
                        offer(cost(Items.NETHERITE_SCRAP, 2, 3), cost(Items.DIAMOND, 12, 16), out(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, 1), 1)),
                List.of( // 5
                        offer(cost(Items.GOLD_BLOCK, 48, 64), cost(Items.DIAMOND, 4, 8), out(Items.ENCHANTED_GOLDEN_APPLE, 1, 1), 1),
                        offer(cost(Items.DIAMOND_BLOCK, 16, 24), cost(Items.DRAGON_BREATH, 2, 4), out(Items.NETHER_STAR, 1, 1), 1),
                        offer(cost(Items.DIAMOND_BLOCK, 8, 12), cost(Items.ECHO_SHARD, 4, 6), out(Items.NETHERITE_INGOT, 2, 2), 1)));

        // ---- Archivist: keepers of knowledge. Lore and records in; rare books and templates out. ----
        pool(IthkProfession.ARCHIVIST,
                List.of( // 1
                        offer(cost(Items.WRITTEN_BOOK, 3, 5), out(Items.EXPERIENCE_BOTTLE, 16, 24), 4),
                        offer(costAny(SHERDS, 1, 1), cost(Items.DIAMOND, 2, 4), out(Items.EXPERIENCE_BOTTLE, 16, 24), 4),
                        offer(cost(Items.BOOK, 24, 32), cost(Items.LAPIS_BLOCK, 6, 10),
                                bookAny(Enchantments.EFFICIENCY, Enchantments.POWER, Enchantments.PROTECTION, Enchantments.SHARPNESS), 2),
                        offer(cost(Items.DISC_FRAGMENT_5, 2, 3), cost(Items.DIAMOND, 4, 8), book(Enchantments.UNBREAKING), 2)),
                List.of( // 2
                        offer(costAny(COMMON_DISCS, 1, 1), cost(Items.DIAMOND, 8, 12),
                                bookAny(Enchantments.FORTUNE, Enchantments.LOOTING, Enchantments.SILK_TOUCH), 2),
                        offer(costAny(SHERDS, 2, 3), cost(Items.DIAMOND, 4, 8),
                                bookAny(Enchantments.FEATHER_FALLING, Enchantments.DEPTH_STRIDER, Enchantments.RESPIRATION), 2),
                        offer(cost(Items.RECOVERY_COMPASS, 1, 1), cost(Items.DIAMOND, 4, 8), book(Enchantments.FROST_WALKER), 1),
                        offer(cost(Items.SPYGLASS, 1, 1), cost(Items.CLOCK, 1, 1), out(Items.EXPERIENCE_BOTTLE, 32, 48), 2)),
                List.of( // 3
                        offer(costAny(TRIMS, 1, 1), cost(Items.DIAMOND, 6, 10), outAny(TRIMS, 1, 1), 2),
                        offer(cost(Items.ECHO_SHARD, 6, 10), cost(Items.DIAMOND, 4, 8), book(Enchantments.SWIFT_SNEAK), 1),
                        offer(costAny(COMMON_DISCS, 2, 3), cost(Items.DIAMOND, 10, 16), book(Enchantments.SOUL_SPEED), 1),
                        offer(cost(Items.TRIAL_KEY, 6, 10), cost(Items.DIAMOND, 6, 10), book(Enchantments.DENSITY), 1)),
                List.of( // 4
                        offer(costAny(RARE_DISCS, 1, 1), cost(Items.DIAMOND, 6, 10), book(Enchantments.MENDING), 1),
                        offer(cost(Items.OMINOUS_TRIAL_KEY, 1, 2), cost(Items.DIAMOND, 12, 16),
                                bookAny(Enchantments.WIND_BURST, Enchantments.BREACH), 1),
                        offer(costAny(TRIMS, 2, 3), cost(Items.ECHO_SHARD, 2, 4), out(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, 1, 1), 1)),
                List.of( // 5
                        offer(costAny(RARE_DISCS, 2, 2), cost(Items.DIAMOND, 6, 10), out(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 1, 1), 1),
                        offer(cost(Items.NETHER_STAR, 1, 1), cost(Items.BOOK, 32, 64), out(Items.EXPERIENCE_BOTTLE, 64, 64), 1),
                        offer(cost(Items.ECHO_SHARD, 12, 16), cost(Items.DIAMOND_BLOCK, 6, 10), book(Enchantments.MENDING), 1)));

        // ---- Gatekeeper: knowledge of dimensional travel. ----
        pool(IthkProfession.GATEKEEPER,
                List.of( // 1
                        offer(cost(Items.ENDER_PEARL, 8, 12), cost(Items.BLAZE_ROD, 6, 8), out(Items.ENDER_EYE, 2, 2), 4),
                        offer(cost(Items.OBSIDIAN, 16, 24), cost(Items.DIAMOND, 4, 6), out(Items.ENDER_EYE, 2, 2), 4),
                        offer(cost(Items.CRYING_OBSIDIAN, 8, 12), cost(Items.DIAMOND, 4, 8), out(Items.LODESTONE, 1, 1), 2)),
                List.of( // 2
                        offer(cost(Items.GHAST_TEAR, 3, 5), cost(Items.DIAMOND, 6, 10), out(Items.ENDER_EYE, 3, 4), 3),
                        offer(cost(Items.GHAST_TEAR, 4, 6), cost(Items.POPPED_CHORUS_FRUIT, 16, 24), out(Items.SHULKER_SHELL, 1, 1), 3),
                        offer(cost(Items.HEART_OF_THE_SEA, 1, 1), cost(Items.DIAMOND, 4, 6), out(Items.RECOVERY_COMPASS, 1, 1), 1)),
                List.of( // 3
                        offer(cost(Items.DRAGON_BREATH, 1, 2), cost(Items.DIAMOND, 8, 12), out(Items.END_CRYSTAL, 2, 4), 2),
                        offer(cost(Items.ECHO_SHARD, 3, 5), cost(Items.ENDER_EYE, 2, 4), out(Items.RECOVERY_COMPASS, 1, 1), 1),
                        offer(cost(Items.CHORUS_FLOWER, 8, 16), cost(Items.DIAMOND, 6, 10), out(Items.SHULKER_SHELL, 2, 2), 2)),
                List.of( // 4
                        offer(cost(Items.NETHER_STAR, 1, 1), cost(Items.SHULKER_SHELL, 2, 4), out(Items.ENDER_EYE, 12, 12), 1),
                        offer(cost(Items.DRAGON_BREATH, 4, 6), cost(Items.DIAMOND_BLOCK, 4, 6), out(Items.SHULKER_SHELL, 4, 4), 1),
                        offer(cost(Items.ECHO_SHARD, 6, 8), cost(Items.DIAMOND_BLOCK, 3, 5), out(Items.HEART_OF_THE_SEA, 1, 1), 1)),
                List.of( // 5
                        offer(cost(Items.NETHER_STAR, 1, 2), cost(Items.DIAMOND_BLOCK, 12, 16), out(Items.ELYTRA, 1, 1), 1),
                        offer(cost(Items.NETHER_STAR, 2, 2), cost(Items.DRAGON_HEAD, 1, 1), out(Items.DRAGON_EGG, 1, 1), 1),
                        offer(cost(Items.NETHER_STAR, 1, 1), cost(Items.ENDER_EYE, 8, 12), out(Items.END_PORTAL_FRAME, 1, 1), 1)));

        // ---- Curator: collects rarities and pays for them in rarities of equal standing. ----
        pool(IthkProfession.CURATOR,
                List.of( // 1
                        offer(cost(Items.TOTEM_OF_UNDYING, 1, 1), out(Items.DIAMOND, 3, 5), 3),
                        offer(cost(Items.NAUTILUS_SHELL, 2, 3), out(Items.DIAMOND, 4, 6), 3),
                        offer(cost(Items.GOAT_HORN, 1, 1), out(Items.DIAMOND, 3, 5), 3),
                        offer(cost(Items.TURTLE_SCUTE, 4, 6), out(Items.NAUTILUS_SHELL, 2, 3), 3)),
                List.of( // 2
                        offer(costAny(MOB_HEADS, 1, 1), out(Items.DIAMOND, 6, 10), 2),
                        offer(cost(Items.WITHER_SKELETON_SKULL, 1, 1), out(Items.DIAMOND_BLOCK, 1, 2), 2),
                        offer(costAny(SHERDS, 1, 1), outAny(SHERDS, 1, 1), 3),
                        offer(cost(Items.SNIFFER_EGG, 1, 1), out(Items.DIAMOND_BLOCK, 2, 3), 1)),
                List.of( // 3
                        offer(cost(Items.TRIAL_KEY, 3, 5), outAny(TRIMS, 1, 1), 2),
                        offer(costAny(COMMON_DISCS, 2, 2), outAny(RARE_DISCS, 1, 1), 1),
                        offer(cost(Items.ENCHANTED_GOLDEN_APPLE, 1, 1), out(Items.DIAMOND_BLOCK, 3, 4), 1),
                        offer(cost(Items.HEART_OF_THE_SEA, 1, 1), out(Items.ECHO_SHARD, 6, 8), 1)),
                List.of( // 4
                        offer(cost(Items.HEAVY_CORE, 1, 1), out(Items.NETHERITE_INGOT, 1, 1), 1),
                        offer(cost(Items.OMINOUS_TRIAL_KEY, 1, 1), out(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, 1), 1),
                        offer(costAny(RARE_DISCS, 1, 1), out(Items.DIAMOND_BLOCK, 3, 4), 1)),
                List.of( // 5
                        offer(cost(Items.DRAGON_HEAD, 1, 1), out(Items.ELYTRA, 1, 1), 1),
                        offer(cost(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 1, 1), out(Items.NETHERITE_INGOT, 2, 3), 1),
                        offer(cost(Items.DRAGON_EGG, 1, 1), out(Items.NETHER_STAR, 3, 4), 1)));
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private interface CostRoll { ItemCost roll(RandomSource r); }
    private interface ResultRoll { ItemStack roll(RandomSource r, RegistryAccess reg); }

    private static int between(RandomSource r, int min, int max) {
        return min + (max > min ? r.nextInt(max - min + 1) : 0);
    }

    private static CostRoll cost(Item item, int min, int max) {
        return r -> new ItemCost(item, between(r, min, max));
    }

    private static CostRoll costAny(List<Item> items, int min, int max) {
        return r -> new ItemCost(items.get(r.nextInt(items.size())), between(r, min, max));
    }

    private static ResultRoll out(Item item, int min, int max) {
        return (r, reg) -> new ItemStack(item, between(r, min, max));
    }

    private static ResultRoll outAny(List<Item> items, int min, int max) {
        return (r, reg) -> new ItemStack(items.get(r.nextInt(items.size())), between(r, min, max));
    }

    /** An enchanted book at that enchantment's maximum level. */
    private static ResultRoll book(ResourceKey<Enchantment> key) {
        return (r, reg) -> {
            Holder<Enchantment> holder = reg.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
            return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, holder.value().getMaxLevel()));
        };
    }

    @SafeVarargs
    private static ResultRoll bookAny(ResourceKey<Enchantment>... keys) {
        return (r, reg) -> book(keys[r.nextInt(keys.length)]).roll(r, reg);
    }

    /** XP the Ithk gains per trade, by level, matching villagers so leveling paces the same. */
    private static int xpFor(int level) {
        return switch (level) {
            case 1 -> 2;
            case 2 -> 10;
            case 3 -> 20;
            default -> 30;
        };
    }

    // Level is filled in by pool(), so lines above don't need to repeat it.
    private interface LeveledOption { MerchantOffer create(RandomSource r, RegistryAccess reg, int level); }

    private static LeveledOption offer(CostRoll a, ResultRoll result, int maxUses) {
        return (r, reg, level) -> new MerchantOffer(a.roll(r), Optional.empty(), result.roll(r, reg),
                maxUses, xpFor(level), 0.0F);
    }

    private static LeveledOption offer(CostRoll a, CostRoll b, ResultRoll result, int maxUses) {
        return (r, reg, level) -> new MerchantOffer(a.roll(r), Optional.of(b.roll(r)), result.roll(r, reg),
                maxUses, xpFor(level), 0.0F);
    }

    @SafeVarargs
    private static void pool(IthkProfession profession, List<LeveledOption>... levels) {
        List<List<Option>> out = new ArrayList<>();
        for (int i = 0; i < levels.length; i++) {
            int level = i + 1;
            List<Option> options = new ArrayList<>();
            for (LeveledOption o : levels[i]) {
                options.add((r, reg) -> o.create(r, reg, level));
            }
            out.add(options);
        }
        POOLS.put(profession, out);
    }

    private IthkTrades() {}
}
