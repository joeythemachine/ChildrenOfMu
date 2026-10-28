package com.childrenofmu.registry;

import com.childrenofmu.ChildrenOfMu;
import com.childrenofmu.item.LevitationShardItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ChildrenOfMu.MODID);

    // Sclera white with a violet accent.
    public static final DeferredItem<DeferredSpawnEggItem> ITHK_SPAWN_EGG = ITEMS.register("ithk_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ITHK, 0xEDE6DA, 0x8E44AD, new Item.Properties()));

    // Spawn eggs for the walking folk (each spawns a random variant of its race).
    public static final DeferredItem<DeferredSpawnEggItem> RAT_FOLK_SPAWN_EGG = ITEMS.register("rat_folk_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.RAT_FOLK, 0x705846, 0xD88E86, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CAT_FOLK_SPAWN_EGG = ITEMS.register("cat_folk_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CAT_FOLK, 0x989693, 0x789250, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BAT_FOLK_SPAWN_EGG = ITEMS.register("bat_folk_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BAT_FOLK, 0x524038, 0xBA7870, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> DOG_FOLK_SPAWN_EGG = ITEMS.register("dog_folk_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.DOG_FOLK, 0x7A5234, 0xE2AC38, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FROG_FOLK_SPAWN_EGG = ITEMS.register("frog_folk_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FROG_FOLK, 0x42A034, 0xE8C424, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOXA_SPAWN_EGG = ITEMS.register("goxa_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GOXA, 0x4E80A6, 0xCE3422, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> LILBUN_SPAWN_EGG = ITEMS.register("lilbun_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.LILBUN, 0xCE3636, 0xF2ECE2, new Item.Properties()));

    /** Hold to float upward; sink gently while held. Repair with amethyst shards. */
    public static final DeferredItem<LevitationShardItem> LEVITATION_SHARD = ITEMS.register("levitation_shard",
            () -> new LevitationShardItem(new Item.Properties().durability(400).rarity(Rarity.RARE)));

    public static final DeferredItem<BlockItem> AMETHYST_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.AMETHYST_STAIRS);
    public static final DeferredItem<BlockItem> AMETHYST_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.AMETHYST_SLAB);

    public static final DeferredItem<BlockItem> INLAID_EMERALD = ITEMS.registerSimpleBlockItem(ModBlocks.INLAID_EMERALD);
    public static final DeferredItem<BlockItem> INLAID_GOLD = ITEMS.registerSimpleBlockItem(ModBlocks.INLAID_GOLD);
    public static final DeferredItem<BlockItem> POST_BOX = ITEMS.registerSimpleBlockItem(ModBlocks.POST_BOX);
    public static final DeferredItem<BlockItem> INLAID_LAPIS = ITEMS.registerSimpleBlockItem(ModBlocks.INLAID_LAPIS);

    public static final DeferredItem<BlockItem> REED_THATCH = ITEMS.registerSimpleBlockItem(ModBlocks.REED_THATCH);
    public static final DeferredItem<BlockItem> REED_THATCH_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.REED_THATCH_STAIRS);
    public static final DeferredItem<BlockItem> REED_THATCH_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.REED_THATCH_SLAB);
    public static final DeferredItem<BlockItem> REED_SCREEN = ITEMS.registerSimpleBlockItem(ModBlocks.REED_SCREEN);
    public static final DeferredItem<BlockItem> LILY_LANTERN = ITEMS.registerSimpleBlockItem(ModBlocks.LILY_LANTERN);
    public static final DeferredItem<BlockItem> FROG_IDOL = ITEMS.registerSimpleBlockItem(ModBlocks.FROG_IDOL);
    public static final DeferredItem<BlockItem> FISHING_NET = ITEMS.registerSimpleBlockItem(ModBlocks.FISHING_NET);
    public static final DeferredItem<BlockItem> CATTAILS = ITEMS.registerSimpleBlockItem(ModBlocks.CATTAILS);

    // ---- The Chitinier's menu: fancy insect cuisine, cooked fresh to order (see entity/folk/FolkKitchen.java) ----
    public static final DeferredItem<Item> HONEY_GLAZED_BEETLE = ITEMS.register("honey_glazed_beetle",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build())));
    public static final DeferredItem<Item> CRICKET_CRUMBLE = ITEMS.register("cricket_crumble",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.6F)
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 1200), 1.0F).build())));
    public static final DeferredItem<Item> FIREFLY_FLAMBE = ITEMS.register("firefly_flambe",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(7).saturationModifier(0.9F)
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 600), 1.0F)
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 1200), 1.0F).build())));
    public static final DeferredItem<Item> DRAGONFLY_TARTARE = ITEMS.register("dragonfly_tartare",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(7).saturationModifier(0.8F)
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 900), 1.0F).build())));
    public static final DeferredItem<Item> MOTH_SOUFFLE = ITEMS.register("moth_souffle",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationModifier(1.0F)
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 600), 1.0F).build())));
    public static final DeferredItem<Item> BOG_SNAIL_ESCARGOT = ITEMS.register("bog_snail_escargot",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(8).saturationModifier(1.0F)
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 600), 1.0F).build())));
    public static final DeferredItem<Item> MOSQUITO_CONSOMME = ITEMS.register("mosquito_consomme",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1).food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationModifier(1.2F)
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200), 1.0F).usingConvertsTo(net.minecraft.world.item.Items.BOWL).build())));
    public static final DeferredItem<BlockItem> REED_MAT = ITEMS.registerSimpleBlockItem(ModBlocks.REED_MAT);
    /** Pink Slime: rare, strange, and the start of something (see the design notes). */
    public static final DeferredItem<Item> PINK_SLIME = ITEMS.register("pink_slime",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> STRUNG_FISH = ITEMS.registerSimpleBlockItem(ModBlocks.STRUNG_FISH);
    public static final DeferredItem<BlockItem> STRUNG_BUGS = ITEMS.registerSimpleBlockItem(ModBlocks.STRUNG_BUGS);
    public static final DeferredItem<BlockItem> MANHOLE = ITEMS.registerSimpleBlockItem(ModBlocks.MANHOLE);
    public static final DeferredItem<BlockItem> BANKERS_LAMP = ITEMS.registerSimpleBlockItem(ModBlocks.BANKERS_LAMP);
    /** The Great Gar's Writ of Trade: lets you trade and bet in one RCB City for nine days. */
    public static final DeferredItem<com.childrenofmu.item.WritItem> WRIT_OF_TRADE = ITEMS.register("writ_of_trade",
            () -> new com.childrenofmu.item.WritItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // ---- RCB City: what the Cats and Bats drink ----
    /** Calf: the Cats' alcoholic milk. Warming, and makes the room spin a bit. */
    public static final DeferredItem<Item> CALF = ITEMS.register("calf",
            () -> new com.childrenofmu.item.DrinkItem(new Item.Properties().stacksTo(16).food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(3).saturationModifier(0.6F).alwaysEdible()
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100), 1.0F)
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 200), 1.0F)
                    .usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE).build())));
    /** Calf Bite: Calf with chicken blood thrown in, bitter. The Bats' favourite. */
    public static final DeferredItem<Item> CALF_BITE = ITEMS.register("calf_bite",
            () -> new com.childrenofmu.item.DrinkItem(new Item.Properties().stacksTo(16).food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.5F).alwaysEdible()
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 600), 1.0F)
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 160), 1.0F)
                    .usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE).build())));
    /** Fermented tuna: the Cats get drunk on this too. Pungent. */
    public static final DeferredItem<Item> FERMENTED_TUNA = ITEMS.register("fermented_tuna",
            () -> new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(6).saturationModifier(0.5F)
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 120), 0.5F)
                    .effect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 600), 1.0F).build())));

    private ModItems() {}
}
