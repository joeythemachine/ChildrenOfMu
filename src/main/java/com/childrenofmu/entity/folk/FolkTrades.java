package com.childrenofmu.entity.folk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Trades for the folk who trade. Each one draws a few trades at random from its pool (by race and variant, or by
 * race alone when every variant shares one).
 *
 *  - Rats keep the burrows and sewers clean and deal in odd, cheap things for a single emerald.
 *  - Cats are thieves: each draws two random stolen goods, one or two of each, and draws again every week.
 *  - The bat barman sells drinks (Brewin' and Chewin' drinks when that mod is installed).
 *  - The feudal dogs: the samurai sells swords, the archer bows and arrows, the stablemaster horses and leather.
 *  - Frog traders: potions, copper, and insect goods.
 *
 * Items are named by id, so items from other mods can be listed: they're skipped when that mod isn't installed.
 * "potion:minecraft:leaping" means a potion of that kind.
 */
public final class FolkTrades {
    /** How many trades each trader draws from its pool (thieving cats draw fewer). */
    public static final int TRADES_EACH = 4;
    public static final int THIEF_TRADES = 2;

    private record Trade(String give, int giveMin, int giveMax, String get, int getMin, int getMax, int uses) {}

    private static final Map<String, List<Trade>> POOLS = new HashMap<>();

    private static final String EMERALD = "minecraft:emerald";

    /** Pay one emerald, get min-max of an item. */
    private static Trade sells(String item, int min, int max) {
        return new Trade(EMERALD, 1, 1, item, min, max, 12);
    }

    /** Pay min-max emeralds for a number of an item. */
    private static Trade costs(String item, int count, int emeraldsMin, int emeraldsMax) {
        return new Trade(EMERALD, emeraldsMin, emeraldsMax, item, count, count, 6);
    }

    /** A stolen good: there are only one or two to sell. */
    private static Trade stolen(String item, int count, int emeraldsMin, int emeraldsMax) {
        return new Trade(EMERALD, emeraldsMin, emeraldsMax, item, count, count, 2);
    }

    /** Give min-max of an item, get one emerald. */
    private static Trade buys(String item, int min, int max) {
        return new Trade(item, min, max, EMERALD, 1, 1, 12);
    }

    static {
        // ---- Rats
        POOLS.put("rat_gong", List.of(
                sells("minecraft:dirt", 16, 24), sells("minecraft:coarse_dirt", 8, 12),
                sells("minecraft:rooted_dirt", 6, 10), sells("minecraft:bone_meal", 9, 15),
                sells("minecraft:mud", 8, 12), sells("minecraft:podzol", 4, 6),
                sells("farmersdelight:rich_soil", 2, 4), sells("farmersdelight:organic_compost", 1, 2),
                buys("minecraft:rotten_flesh", 24, 32)));
        POOLS.put("rat_runner", List.of(
                sells("minecraft:potato", 8, 12), sells("minecraft:baked_potato", 4, 6),
                sells("minecraft:poisonous_potato", 6, 10), sells("minecraft:carrot", 6, 10),
                sells("minecraft:beetroot", 6, 10), sells("minecraft:wheat_seeds", 16, 24),
                sells("minecraft:bread", 3, 4), sells("minecraft:apple", 3, 5)));
        POOLS.put("rat_scavenger", List.of(
                sells("minecraft:stick", 16, 24), sells("minecraft:string", 4, 8),
                sells("minecraft:bowl", 6, 10), sells("minecraft:bone", 4, 8),
                sells("minecraft:flint", 4, 6), sells("minecraft:feather", 6, 10),
                sells("minecraft:glass_bottle", 3, 5), sells("minecraft:leather", 1, 2),
                buys("minecraft:spider_eye", 8, 12)));

        // City rats with proper jobs (well, jobs).
        POOLS.put("rat_sweeper", List.of(
                sells("minecraft:brush", 1, 1), sells("minecraft:stick", 16, 24), sells("minecraft:string", 6, 10),
                sells("minecraft:bone", 6, 10), sells("minecraft:gunpowder", 2, 3), sells("minecraft:arrow", 6, 10),
                sells("minecraft:rotten_flesh", 8, 12), sells("minecraft:leather", 2, 3), sells("minecraft:iron_nugget", 6, 9),
                sells("minecraft:gold_nugget", 3, 5), costs("minecraft:music_disc_13", 1, 8, 12)));
        POOLS.put("rat_postrat", List.of(
                sells("minecraft:paper", 12, 18), costs("minecraft:map", 1, 2, 3), costs("minecraft:book", 1, 2, 3),
                costs("minecraft:writable_book", 1, 3, 4), sells("minecraft:feather", 8, 12), sells("minecraft:ink_sac", 3, 5),
                costs("minecraft:compass", 1, 4, 6), costs("minecraft:name_tag", 1, 10, 14),
                buys("minecraft:paper", 20, 28)));
        POOLS.put("rat_accountant", List.of(
                buys("minecraft:gold_ingot", 3, 4), sells("minecraft:gold_nugget", 12, 16), costs("minecraft:clock", 1, 4, 6),
                costs("minecraft:writable_book", 1, 2, 3), costs("minecraft:emerald_block", 1, 10, 10),
                new Trade("minecraft:emerald_block", 1, 1, EMERALD, 8, 9, 6), costs("minecraft:lectern", 1, 5, 7),
                buys("minecraft:diamond", 1, 1)));
        // Engineers: mutated and electrically modified for tunnels, trains and clever machines.
        POOLS.put("rat_engineer", List.of(
                sells("minecraft:redstone", 8, 12), costs("minecraft:repeater", 2, 1, 2), costs("minecraft:comparator", 1, 2, 3),
                costs("minecraft:piston", 1, 2, 3), costs("minecraft:observer", 1, 2, 3), costs("minecraft:rail", 16, 2, 3),
                costs("minecraft:powered_rail", 6, 4, 6), costs("minecraft:minecart", 1, 3, 4), costs("minecraft:daylight_detector", 1, 3, 4),
                costs("create:cogwheel", 4, 1, 2), costs("create:shaft", 8, 1, 2), costs("create:andesite_alloy", 6, 2, 3),
                costs("minecraft:crafter", 1, 6, 8), buys("minecraft:copper_ingot", 10, 14)));


        // ---- The shops of RCB City: a shopkeeper sells what the sign over the door says (see rcb_build.SHOPS).
        POOLS.put("shop:HATS", List.of(
                costs("minecraft:leather_helmet", 1, 2, 3), costs("minecraft:chainmail_helmet", 1, 5, 7),
                costs("minecraft:iron_helmet", 1, 6, 8), costs("minecraft:golden_helmet", 1, 5, 7),
                costs("minecraft:turtle_helmet", 1, 12, 16), costs("minecraft:carved_pumpkin", 1, 1, 2),
                costs("minecraft:diamond_helmet", 1, 18, 24), buys("minecraft:leather", 6, 9),
                buys("minecraft:rabbit_hide", 8, 12), costs("supplementaries:candle_holder", 1, 1, 2)));
        POOLS.put("shop:FRESH", List.of(
                sells("minecraft:rabbit", 2, 3), sells("minecraft:cooked_rabbit", 2, 3), costs("minecraft:rabbit_stew", 1, 1, 2),
                sells("minecraft:cod", 3, 5), sells("minecraft:chicken", 2, 3), buys("minecraft:rabbit", 4, 6)));
        POOLS.put("shop:CHEESE", List.of(
                costs("brewinandchewin:flaxen_cheese_wedge", 2, 1, 2), costs("brewinandchewin:scarlet_cheese_wedge", 2, 1, 2),
                costs("brewinandchewin:unripe_flaxen_cheese_wheel", 1, 4, 6), sells("minecraft:milk_bucket", 1, 1),
                sells("minecraft:rabbit", 2, 3), sells("minecraft:cooked_rabbit", 2, 3), sells("minecraft:bread", 3, 4),
                buys("minecraft:milk_bucket", 1, 1)));
        POOLS.put("shop:PAWN", List.of(
                costs("minecraft:clock", 1, 3, 5), costs("minecraft:compass", 1, 3, 4), costs("minecraft:spyglass", 1, 4, 6),
                costs("minecraft:saddle", 1, 5, 7), costs("minecraft:name_tag", 1, 6, 9), costs("minecraft:lead", 2, 2, 3),
                costs("minecraft:golden_apple", 1, 7, 10), costs("minecraft:music_disc_mellohi", 1, 8, 12),
                buys("minecraft:gold_ingot", 2, 3), buys("minecraft:diamond", 1, 1), buys("minecraft:amethyst_shard", 6, 8)));
        POOLS.put("shop:KNIVES", List.of(
                costs("farmersdelight:flint_knife", 1, 1, 2), costs("farmersdelight:iron_knife", 1, 3, 4),
                costs("farmersdelight:golden_knife", 1, 4, 5), costs("farmersdelight:diamond_knife", 1, 12, 16),
                costs("minecraft:iron_sword", 1, 5, 7), costs("minecraft:shears", 1, 2, 3),
                costs("minecraft:grindstone", 1, 3, 4), buys("minecraft:iron_ingot", 4, 6), buys("minecraft:flint", 10, 14)));
        POOLS.put("shop:CANDLES", List.of(
                sells("minecraft:candle", 2, 3), sells("minecraft:white_candle", 2, 3), sells("minecraft:red_candle", 2, 3),
                sells("minecraft:yellow_candle", 2, 3), sells("minecraft:black_candle", 2, 3), sells("minecraft:string", 6, 10),
                sells("minecraft:honeycomb", 2, 3), buys("minecraft:honeycomb", 6, 9)));
        POOLS.put("shop:TAILOR", List.of(
                sells("minecraft:white_wool", 4, 6), sells("minecraft:black_wool", 4, 6), sells("minecraft:red_wool", 4, 6),
                sells("minecraft:blue_wool", 4, 6), sells("minecraft:string", 8, 12), costs("minecraft:leather_chestplate", 1, 3, 5),
                costs("minecraft:leather_leggings", 1, 3, 4), costs("minecraft:white_banner", 1, 2, 3),
                buys("minecraft:white_wool", 14, 18)));
        POOLS.put("shop:APOTHECARY", List.of(
                costs("potion:minecraft:healing", 1, 3, 5), costs("potion:minecraft:regeneration", 1, 4, 6),
                costs("potion:minecraft:fire_resistance", 1, 4, 6), costs("potion:minecraft:water_breathing", 1, 4, 6),
                sells("minecraft:glistering_melon_slice", 1, 2), sells("minecraft:fermented_spider_eye", 1, 2),
                sells("minecraft:nether_wart", 3, 5), sells("minecraft:milk_bucket", 1, 1), buys("minecraft:spider_eye", 8, 12)));
        POOLS.put("shop:MAPS", List.of(
                costs("minecraft:map", 1, 2, 3), costs("minecraft:compass", 1, 3, 5), costs("minecraft:spyglass", 1, 5, 7),
                sells("minecraft:paper", 12, 16), costs("minecraft:cartography_table", 1, 3, 4),
                costs("minecraft:writable_book", 1, 2, 3), buys("minecraft:paper", 20, 26)));
        POOLS.put("shop:SECOND", List.of(
                costs("minecraft:bucket", 1, 1, 2), costs("minecraft:shears", 1, 1, 2), costs("minecraft:fishing_rod", 1, 1, 2),
                costs("minecraft:flint_and_steel", 1, 1, 2), costs("minecraft:leather_boots", 1, 1, 2),
                costs("minecraft:lantern", 1, 1, 2), sells("minecraft:bowl", 6, 8), costs("minecraft:chainmail_chestplate", 1, 6, 9),
                buys("minecraft:rotten_flesh", 20, 28)));
        POOLS.put("shop:FISHMONGER", List.of(
                sells("minecraft:cod", 5, 8), sells("minecraft:salmon", 4, 6), sells("minecraft:tropical_fish", 2, 3),
                sells("minecraft:cooked_cod", 3, 5), sells("childrenofmu:fermented_tuna", 1, 2),
                costs("minecraft:pufferfish", 1, 1, 2), buys("minecraft:cod", 12, 16), buys("minecraft:salmon", 10, 14)));
        POOLS.put("shop:CALF", List.of(
                sells("childrenofmu:calf", 2, 3), sells("childrenofmu:calf_bite", 1, 2), sells("minecraft:milk_bucket", 1, 1),
                sells("minecraft:cookie", 6, 8), sells("minecraft:honey_bottle", 2, 3)));
        POOLS.put("shop:BAKERY", List.of(
                sells("minecraft:bread", 4, 6), sells("minecraft:cookie", 8, 12), costs("minecraft:cake", 1, 1, 2),
                sells("minecraft:pumpkin_pie", 2, 3), costs("farmersdelight:apple_pie", 1, 2, 3),
                costs("farmersdelight:shepherds_pie", 1, 2, 3), buys("minecraft:wheat", 18, 24)));
        POOLS.put("shop:CLOCKS", List.of(
                costs("minecraft:clock", 1, 3, 4), costs("minecraft:compass", 1, 3, 4), sells("minecraft:gold_nugget", 8, 12),
                sells("minecraft:redstone", 6, 10), costs("minecraft:repeater", 2, 1, 2), costs("minecraft:daylight_detector", 1, 2, 3),
                buys("minecraft:gold_ingot", 3, 4)));
        POOLS.put("shop:ROPE", List.of(
                sells("minecraft:string", 10, 16), costs("minecraft:lead", 2, 2, 3), sells("farmersdelight:rope", 8, 12),
                sells("supplementaries:rope", 8, 12), costs("minecraft:fishing_rod", 1, 1, 2), sells("minecraft:ladder", 6, 8),
                buys("minecraft:string", 14, 18)));
        POOLS.put("shop:LAMPS", List.of(
                costs("minecraft:lantern", 1, 1, 2), costs("minecraft:soul_lantern", 1, 2, 3), sells("minecraft:torch", 12, 16),
                sells("minecraft:candle", 2, 3), sells("minecraft:coal", 6, 10), sells("minecraft:glowstone_dust", 4, 6),
                costs("minecraft:sea_lantern", 1, 3, 4), buys("minecraft:coal", 14, 18)));
        POOLS.put("shop:BOOKS", List.of(
                costs("minecraft:book", 1, 1, 2), costs("minecraft:writable_book", 1, 2, 3), costs("minecraft:bookshelf", 1, 3, 4),
                sells("minecraft:paper", 10, 14), costs("minecraft:lectern", 1, 4, 5), sells("minecraft:ink_sac", 3, 5),
                buys("minecraft:book", 3, 4)));
        POOLS.put("shop:CATNIP", List.of(
                sells("minecraft:sweet_berries", 6, 10), sells("minecraft:glow_berries", 4, 6), sells("minecraft:cod", 4, 6),
                sells("childrenofmu:fermented_tuna", 1, 2), sells("minecraft:string", 6, 8), costs("minecraft:fern", 4, 1, 1),
                buys("minecraft:sweet_berries", 16, 20)));
        POOLS.put("shop:GLOVES", List.of(
                sells("minecraft:leather", 2, 3), sells("minecraft:rabbit_hide", 3, 5), costs("minecraft:leather_chestplate", 1, 3, 4),
                costs("minecraft:leather_boots", 1, 2, 3), costs("minecraft:leather_helmet", 1, 2, 3),
                buys("minecraft:rabbit_hide", 8, 12), buys("minecraft:leather", 6, 8)));
        POOLS.put("shop:BRASS", List.of(
                sells("minecraft:copper_ingot", 4, 6), costs("minecraft:copper_block", 1, 2, 3), costs("minecraft:lightning_rod", 1, 1, 2),
                costs("minecraft:spyglass", 1, 4, 5), sells("create:brass_ingot", 1, 2), sells("create:copper_sheet", 2, 3),
                sells("create:brass_sheet", 1, 2), costs("create:brass_hand", 1, 2, 3), buys("minecraft:copper_ingot", 10, 14)));
        POOLS.put("shop:TEA", List.of(
                sells("minecraft:honey_bottle", 2, 3), sells("minecraft:cookie", 6, 8), costs("minecraft:cake", 1, 1, 2),
                sells("minecraft:sweet_berries", 6, 8), costs("farmersdelight:hot_cocoa", 1, 1, 2),
                costs("farmersdelight:apple_cider", 1, 1, 2), costs("minecraft:flower_pot", 2, 1, 1)));
        POOLS.put("shop:COBBLER", List.of(
                costs("minecraft:leather_boots", 1, 1, 2), costs("minecraft:chainmail_boots", 1, 4, 5),
                costs("minecraft:iron_boots", 1, 5, 7), costs("minecraft:golden_boots", 1, 4, 6),
                costs("minecraft:diamond_boots", 1, 16, 20), sells("minecraft:leather", 2, 3), buys("minecraft:leather", 6, 8)));

        // ---- Cats: whatever they've stolen this week
        POOLS.put("cat", List.of(
                stolen("minecraft:diamond", 1, 8, 12), stolen("minecraft:golden_apple", 1, 6, 9),
                stolen("minecraft:ender_pearl", 2, 4, 6), stolen("minecraft:name_tag", 1, 5, 8),
                stolen("minecraft:saddle", 1, 5, 7), stolen("minecraft:compass", 1, 3, 5),
                stolen("minecraft:clock", 1, 4, 6), stolen("minecraft:spyglass", 1, 4, 6),
                stolen("minecraft:music_disc_cat", 1, 6, 10), stolen("minecraft:gold_ingot", 3, 4, 6),
                stolen("minecraft:iron_ingot", 6, 3, 5), stolen("minecraft:lapis_lazuli", 8, 2, 3),
                stolen("minecraft:amethyst_shard", 6, 2, 3), stolen("minecraft:nautilus_shell", 1, 5, 8),
                stolen("minecraft:experience_bottle", 3, 4, 6), stolen("minecraft:rabbit_foot", 1, 3, 5),
                stolen("minecraft:cake", 1, 2, 3), stolen("minecraft:honey_bottle", 2, 2, 3),
                stolen("minecraft:trial_key", 1, 6, 9), stolen("minecraft:heart_of_the_sea", 1, 20, 28),
                stolen("minecraft:emerald_block", 1, 7, 8), stolen("minecraft:firework_rocket", 4, 2, 3),
                stolen("minecraft:lantern", 2, 2, 3), stolen("minecraft:leather_boots", 1, 2, 3),
                stolen("childrenofmu:fermented_tuna", 3, 1, 2), stolen("childrenofmu:calf", 2, 1, 2)));

        // ---- The bat barman's drinks
        POOLS.put("bat_barman", List.of(
                sells("childrenofmu:calf", 2, 3), sells("childrenofmu:calf_bite", 1, 2),
                sells("childrenofmu:fermented_tuna", 2, 3),
                sells("brewinandchewin:beer", 1, 1), sells("brewinandchewin:mead", 1, 1),
                sells("brewinandchewin:rice_wine", 1, 1), sells("brewinandchewin:vodka", 1, 1),
                sells("brewinandchewin:egg_grog", 1, 1), sells("brewinandchewin:strongroot_ale", 1, 1),
                sells("brewinandchewin:saccharine_rum", 1, 1), sells("brewinandchewin:pale_jane", 1, 1),
                sells("brewinandchewin:glittering_grenadine", 1, 1), sells("brewinandchewin:bloody_mary", 1, 1),
                sells("brewinandchewin:red_rum", 1, 1), sells("minecraft:honey_bottle", 2, 3),
                sells("minecraft:milk_bucket", 1, 1), costs("potion:minecraft:swiftness", 1, 2, 3),
                costs("potion:minecraft:night_vision", 1, 2, 3)));

        // ---- The feudal dogs
        POOLS.put("dog_samurai", List.of(
                costs("minecraft:iron_sword", 1, 6, 9), costs("minecraft:diamond_sword", 1, 18, 24),
                costs("minecraft:stone_sword", 1, 2, 3), costs("minecraft:golden_sword", 1, 4, 6),
                costs("minecraft:shield", 1, 5, 7), costs("minecraft:iron_chestplate", 1, 10, 14),
                buys("minecraft:iron_ingot", 6, 8), buys("minecraft:coal", 15, 20)));
        POOLS.put("dog_archer", List.of(
                costs("minecraft:bow", 1, 3, 4), costs("minecraft:crossbow", 1, 5, 6),
                sells("minecraft:arrow", 12, 16), costs("minecraft:spectral_arrow", 6, 2, 3),
                costs("minecraft:target", 1, 2, 3), buys("minecraft:stick", 24, 32),
                buys("minecraft:feather", 20, 24), buys("minecraft:string", 14, 18)));
        POOLS.put("dog_stablemaster", List.of(
                costs("minecraft:horse_spawn_egg", 1, 22, 28), costs("minecraft:saddle", 1, 6, 8),
                costs("minecraft:leather", 4, 2, 3), costs("minecraft:leather_horse_armor", 1, 4, 6),
                costs("minecraft:iron_horse_armor", 1, 8, 11), costs("minecraft:lead", 2, 2, 3),
                sells("minecraft:hay_block", 2, 3), costs("minecraft:golden_carrot", 3, 2, 3),
                buys("minecraft:wheat", 18, 24)));

        // ---- Frog traders
        POOLS.put("frog_potioneer", List.of(
                costs("potion:minecraft:leaping", 1, 2, 4), costs("potion:minecraft:swiftness", 1, 3, 4),
                costs("potion:minecraft:water_breathing", 1, 3, 5), costs("potion:minecraft:healing", 1, 3, 5),
                costs("potion:minecraft:night_vision", 1, 2, 4), costs("potion:minecraft:fire_resistance", 1, 4, 6),
                costs("potion:minecraft:slow_falling", 1, 3, 5), buys("minecraft:nether_wart", 12, 16),
                buys("minecraft:glass_bottle", 8, 12), buys("minecraft:slime_ball", 6, 10),
                costs("minecraft:slime_block", 1, 4, 6), costs("potion:minecraft:poison", 1, 2, 3)));
        POOLS.put("frog_coppersmith", List.of(
                sells("minecraft:copper_ingot", 4, 6), costs("minecraft:copper_block", 1, 2, 3),
                costs("minecraft:lightning_rod", 1, 1, 2), costs("minecraft:spyglass", 1, 3, 4),
                costs("minecraft:brush", 1, 2, 3), costs("minecraft:oxidized_copper", 2, 2, 3),
                costs("create:copper_sheet", 4, 1, 2), buys("minecraft:raw_copper", 10, 14)));
        POOLS.put("frog_fisher", List.of(
                sells("minecraft:cod", 4, 6), sells("minecraft:salmon", 3, 5),
                sells("minecraft:tropical_fish", 2, 3), costs("minecraft:pufferfish", 1, 2, 3),
                costs("minecraft:fishing_rod", 1, 2, 3), sells("minecraft:lily_pad", 6, 8),
                sells("minecraft:sea_pickle", 3, 5), sells("minecraft:kelp", 10, 14),
                buys("minecraft:string", 14, 18), sells("minecraft:slime_ball", 2, 3)));
        // The treasury changes gold for emeralds and back (at a little spread, of course).
        POOLS.put("frog_treasurer", List.of(
                buys("minecraft:gold_ingot", 3, 4), sells("minecraft:gold_ingot", 2, 2),
                buys("minecraft:gold_nugget", 28, 34), sells("minecraft:gold_nugget", 14, 18),
                costs("minecraft:gold_block", 1, 9, 11), new Trade("minecraft:diamond", 1, 1, EMERALD, 9, 11, 6),
                new Trade("minecraft:emerald_block", 1, 1, "minecraft:gold_block", 1, 1, 4),
                buys("minecraft:slime_block", 1, 2)));
        POOLS.put("frog_plumber", List.of(
                costs("minecraft:bucket", 1, 2, 3), costs("minecraft:water_bucket", 1, 3, 4),
                costs("minecraft:cauldron", 1, 3, 5), costs("minecraft:copper_grate", 2, 1, 2),
                costs("minecraft:copper_bulb", 1, 2, 3), costs("minecraft:sponge", 1, 5, 7),
                costs("create:fluid_pipe", 4, 1, 2), costs("create:mechanical_pump", 1, 4, 6),
                costs("create:fluid_tank", 1, 3, 4), buys("minecraft:raw_copper", 10, 14),
                costs("minecraft:sticky_piston", 1, 3, 4), buys("minecraft:slime_ball", 8, 12)));
        // The Schoolmarm: slates, books and quills.
        POOLS.put("frog_schoolmarm", List.of(
                sells("minecraft:paper", 12, 16), costs("minecraft:book", 1, 2, 3), costs("minecraft:writable_book", 1, 3, 4),
                costs("minecraft:bookshelf", 1, 4, 6), costs("minecraft:lectern", 1, 5, 7), costs("minecraft:map", 1, 3, 4),
                costs("minecraft:bell", 1, 10, 14), buys("minecraft:feather", 10, 14), buys("minecraft:ink_sac", 6, 10),
                costs("minecraft:name_tag", 1, 12, 16)));
        // The fish trader: the Fisher's catch, cleaned, cooked and sold on (Farmer's Delight cuts when installed).
        POOLS.put("frog_fishmonger", List.of(
                sells("minecraft:cooked_cod", 3, 5), sells("minecraft:cooked_salmon", 2, 4),
                sells("minecraft:dried_kelp", 10, 14), costs("minecraft:pufferfish", 1, 3, 4),
                costs("minecraft:tropical_fish_bucket", 1, 6, 8), costs("minecraft:axolotl_bucket", 1, 14, 18),
                sells("farmersdelight:cod_slice", 4, 6), sells("farmersdelight:salmon_slice", 4, 6),
                costs("farmersdelight:fish_stew", 1, 2, 3), costs("farmersdelight:grilled_salmon", 1, 2, 3),
                buys("minecraft:cod", 10, 14), buys("minecraft:salmon", 8, 12)));
        POOLS.put("frog_chitinier", List.of(
                sells("minecraft:honeycomb", 3, 4), sells("minecraft:honey_bottle", 2, 3),
                costs("minecraft:cobweb", 2, 2, 3), costs("minecraft:turtle_scute", 1, 4, 6),
                costs("minecraft:armadillo_scute", 2, 3, 4), sells("minecraft:spider_eye", 3, 4),
                costs("minecraft:bee_nest", 1, 10, 14), buys("minecraft:fermented_spider_eye", 2, 3)));
    }

    /** Experience a trade gives at each merchant level (novice .. master), like a villager's. */
    private static final int[] XP = {0, 2, 6, 10, 15, 30};
    /** Trades a merchant starts with, and how many more each new level unlocks. */
    public static final int START_TRADES = 2, PER_LEVEL = 2;

    /** A new merchant's first trades (a cat's whole stolen stock: they don't level up). */
    public static MerchantOffers forVariant(FolkRace race, String variant, RandomSource random) {
        MerchantOffers offers = new MerchantOffers();
        if (race == FolkRace.BAT && "barman".equals(variant)) {
            // The house drink is always on: Calf, and Calf Bite.
            stack("childrenofmu:calf", 2).ifPresent(g -> offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1),
                    Optional.empty(), g, 16, XP[1], 0.05F)));
            stack("childrenofmu:calf_bite", 1).ifPresent(g -> offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1),
                    Optional.empty(), g, 16, XP[1], 0.05F)));
        }
        addTrades(race, variant, offers, race.weekly() ? THIEF_TRADES : START_TRADES, 1, random);
        return offers;
    }

    /** A merchant reached a new level: unlock more trades, and at Master, something special. */
    public static void levelUp(FolkRace race, String variant, MerchantOffers offers, int level, RandomSource random) {
        addTrades(race, variant, offers, PER_LEVEL, level, random);
        if (level >= 5 && "potioneer".equals(variant) && race == FolkRace.FROG && random.nextFloat() < 0.6F) {
            // Pink Slime: the strangest thing in the swamp. (See the design notes: it will matter a great deal.)
            offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 32 + random.nextInt(16)),
                    Optional.of(new ItemCost(Items.SLIME_BALL, 8)),
                    new ItemStack(com.childrenofmu.registry.ModItems.PINK_SLIME.get()), 2, XP[5], 0.05F));
        }
    }

    private static void addTrades(FolkRace race, String variant, MerchantOffers offers, int howMany, int level,
                                  RandomSource random) {
        List<Trade> pool = new ArrayList<>(variant.startsWith("shop:")
                ? POOLS.getOrDefault(variant, POOLS.getOrDefault(variant.split(" ")[0], List.of()))
                : POOLS.getOrDefault(race.id() + "_" + variant, POOLS.getOrDefault(race.id(), List.of())));
        Collections.shuffle(pool, new java.util.Random(random.nextLong()));
        int added = 0;
        for (Trade t : pool) {
            if (added >= howMany) {
                break;
            }
            Optional<ItemStack> give = stack(t.give(), 1);
            Optional<ItemStack> get = stack(t.get(), t.getMin() + random.nextInt(t.getMax() - t.getMin() + 1));
            if (give.isEmpty() || get.isEmpty()) {
                continue;   // from a mod that isn't installed
            }
            if (offers.stream().anyMatch(o -> ItemStack.isSameItemSameComponents(o.getResult(), get.get())
                    && o.getBaseCostA().is(give.get().getItem()))) {
                continue;   // already offered
            }
            int giveCount = t.giveMin() + random.nextInt(t.giveMax() - t.giveMin() + 1);
            offers.add(new MerchantOffer(new ItemCost(give.get().getItem(), giveCount), Optional.empty(),
                    get.get(), t.uses(), XP[Math.min(5, level)], 0.05F));
            added++;
        }
    }

    // ---- The Old Traveller ---------------------------------------------------------------

    /** Towns the Old Traveller can sell a map to: structure tag, and the map's name. */
    private static final String[][] TOWN_MAPS = {
            {"childrenofmu:frog_towns", "map.childrenofmu.frog_town"},
            {"childrenofmu:ithk_villages", "map.childrenofmu.ithk_village"},
            {"childrenofmu:rcb_cities", "map.childrenofmu.rcb_city"}};

    /** What each race's traveller wants alongside the emeralds for a map (race-appropriate, or just odd). */
    private static String travellerWants(FolkRace race) {
        return switch (race) {
            case RAT -> "minecraft:bone";
            case CAT -> "minecraft:cod";
            case BAT -> "minecraft:spider_eye";
            case DOG -> "minecraft:iron_ingot";
            case FROG -> "minecraft:slime_ball";
            case GOXA -> "minecraft:amethyst_shard";
            default -> "minecraft:wheat_seeds";
        };
    }

    static {
        // Odds and ends picked up on the road.
        POOLS.put("traveler", List.of(
                costs("minecraft:compass", 1, 4, 6), costs("minecraft:spyglass", 1, 6, 8),
                costs("minecraft:recovery_compass", 1, 14, 18), costs("minecraft:nautilus_shell", 1, 6, 9),
                costs("minecraft:echo_shard", 1, 16, 22), costs("minecraft:turtle_scute", 1, 5, 7),
                costs("minecraft:music_disc_mellohi", 1, 10, 14), costs("minecraft:sniffer_egg", 1, 24, 32),
                costs("minecraft:disc_fragment_5", 1, 6, 8), costs("minecraft:glow_ink_sac", 3, 2, 3),
                costs("minecraft:name_tag", 1, 8, 12), costs("minecraft:lead", 2, 2, 3)));
    }

    /**
     * The Old Traveller's stock: a treasure map to the nearest of each kind of town they know of (paid for in
     * emeralds plus something their race fancies), and a few odd things from the road.
     */
    public static MerchantOffers traveller(FolkRace race, net.minecraft.server.level.ServerLevel level,
                                          net.minecraft.core.BlockPos at, RandomSource random) {
        // The town maps are added one at a time afterwards (see townMapOffer): finding a town is slow, and doing them
        // all at once froze the game for the player standing at the door.
        MerchantOffers offers = new MerchantOffers();
        List<Trade> pool = new ArrayList<>(POOLS.get("traveler"));
        Collections.shuffle(pool, new java.util.Random(random.nextLong()));
        for (Trade t : pool.subList(0, Math.min(3, pool.size()))) {
            Optional<ItemStack> get = stack(t.get(), t.getMin());
            get.ifPresent(g -> offers.add(new MerchantOffer(new ItemCost(Items.EMERALD,
                    t.giveMin() + random.nextInt(t.giveMax() - t.giveMin() + 1)), Optional.empty(), g, 2, 0, 0.05F)));
        }
        return offers;
    }

    /** How many kinds of town a traveller can sell a map to. */
    public static int townMapCount() {
        return TOWN_MAPS.length;
    }

    /** The traveller's map to the nearest town of the i-th kind, if there is one within reach. */
    public static Optional<MerchantOffer> townMapOffer(FolkRace race, net.minecraft.server.level.ServerLevel level,
                                                       net.minecraft.core.BlockPos at, int i, RandomSource random) {
        Item want = BuiltInRegistries.ITEM.get(ResourceLocation.parse(travellerWants(race)));
        ItemStack map = townMap(level, at, TOWN_MAPS[i][0], TOWN_MAPS[i][1]);
        if (map.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MerchantOffer(new ItemCost(Items.EMERALD, 10 + random.nextInt(7)),
                Optional.of(new ItemCost(want, 4 + random.nextInt(5))), map, 1, 0, 0.0F));
    }

    /** A treasure map, marked with a red X, to the nearest town of this kind not yet found. */
    private static ItemStack townMap(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos at,
                                     String tagId, String nameKey) {
        net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> tag =
                net.minecraft.tags.TagKey.create(Registries.STRUCTURE, ResourceLocation.parse(tagId));
        net.minecraft.core.BlockPos target = level.findNearestMapStructure(tag, at, 64, false);
        if (target == null) {
            return ItemStack.EMPTY;
        }
        ItemStack map = net.minecraft.world.item.MapItem.create(level, target.getX(), target.getZ(), (byte) 2, true, true);
        net.minecraft.world.item.MapItem.renderBiomePreviewMap(level, map);
        net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(map, target, "+",
                net.minecraft.world.level.saveddata.maps.MapDecorationTypes.RED_X);
        map.set(net.minecraft.core.component.DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.translatable(nameKey));
        return map;
    }

    private static Optional<ItemStack> stack(String id, int count) {
        if (id.startsWith("potion:")) {
            ResourceKey<Potion> key = ResourceKey.create(Registries.POTION, ResourceLocation.parse(id.substring(7)));
            Optional<Holder.Reference<Potion>> potion = BuiltInRegistries.POTION.getHolder(key);
            return potion.map(p -> PotionContents.createItemStack(Items.POTION, p));
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id));
        return item.map(i -> new ItemStack(i, count));
    }

    private FolkTrades() {}
}
