package com.childrenofmu.entity.folk;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The walking folk races and their variants. Each variant is a different look (texture) on the race's shared body;
 * the models and textures come from tools/make_folk_assets.py.
 */
public enum FolkRace {
    /** Rat folk: live in the burrows and sewers beneath the shared Cat/Rat/Bat city. About 1/3 of a player's height. */
    /** They keep the burrows and sewers clean, and trade odd, cheap things. */
    RAT("rat", new String[]{"runner", "scavenger", "gong", "beautiful", "sweeper", "postrat", "accountant", "engineer"}, 0.43F, 7.0F, 0.5F, 0.65F, 10.0, 0.3, false, true, false),
    /** Cat folk: the city's ground floor. Thieves: each sells something different every week. Half a player's height. */
    CAT("cat", new String[]{"rogue", "adventurer", "hunter", "fellmonger", "cop", "guard", "gar", "archer"}, 0.76F, 7.0F, 0.6F, 1.15F, 16.0, 0.28,
            false, true, true),
    /** Bat folk: the roosts above the city. They play jazz all night; the barman sells drinks. Half a player's height. */
    BAT("bat", new String[]{"folk", "flapper", "barman", "jazz", "bookie"}, 0.6F, 7.0F, 0.6F, 0.9F, 12.0, 0.27, false, true, false),
    /**
     * Dog folk: monks, and feudal-Japanese samurai, archers and stablemasters who trade; Dog City adds its pikemen,
     * the trades of its four quarters, the Biters of the kennel, and the bandaged one in the dark tower.
     * 3/4 of a player's height.
     */
    DOG("dog", new String[]{"monk", "husky_monk", "samurai", "archer", "stablemaster", "pikeman", "weaver", "dyer",
            "tailor", "shepherd", "smith", "tanner", "woodcutter", "butcher", "cook", "server", "musician", "teacher",
            "biter", "bandaged", "high_guard"}, 0.9F, 7.0F, 0.7F, 1.35F,
            20.0, 0.25, true, true, false),
    /** Frog folk: couriers, guards and traders of the swamp towns. A little taller than a block (froglets half that). */
    FROG("frog", new String[]{"jumper", "guard", "potioneer", "coppersmith", "chitinier", "fisher", "treasurer",
            "plumber", "elder", "froglet", "cook", "fishmonger", "schoolmarm"}, 0.76F, 6.0F, 0.6F, 1.15F,
            10.0, 0.3, false, true, false),
    /** Goxa: tall, hollow wizards. 1.25 times a player's height. */
    GOXA("goxa", new String[]{"havu", "seminata"}, 1.5F, 12.0F, 0.7F, 2.2F, 30.0, 0.22, true, false, false),
    /** Lilbuns: tiny mushroom-capped folk, 1/12 of a player's height. */
    LILBUN("lilbun", new String[]{"farmer", "friend"}, 0.11F, 4.5F, 0.2F, 0.16F, 4.0, 0.2, false, false, false);

    private final String id;
    private final String[] variants;
    /** How much the 1.5-block design model is shrunk in game. */
    private final float modelScale;
    /** How far below the shoulder the hand is, in design units (for holding tools). */
    private final float handY;
    private final float width, height;
    private final double health, speed;
    /** Wears a long robe: its legs swing less. */
    private final boolean robed;
    /** Trades with players. */
    private final boolean trades;
    /** Draws a fresh set of trades every week instead of keeping them. */
    private final boolean weekly;

    FolkRace(String id, String[] variants, float modelScale, float handY, float width, float height,
             double health, double speed, boolean robed, boolean trades, boolean weekly) {
        this.id = id;
        this.variants = variants;
        this.modelScale = modelScale;
        this.handY = handY;
        this.width = width;
        this.height = height;
        this.health = health;
        this.speed = speed;
        this.robed = robed;
        this.trades = trades;
        this.weekly = weekly;
    }

    public String id() { return id; }
    public String[] variants() { return variants; }
    public float modelScale() { return modelScale; }
    public float handY() { return handY; }
    public float width() { return width; }
    public float height() { return height; }
    public double health() { return health; }
    public double speed() { return speed; }
    public boolean robed() { return robed; }
    public boolean trades() { return trades; }
    public boolean weekly() { return weekly; }

    /** Some variants are smaller than the rest of their race: the froglets. */
    public static float variantScale(String variant) {
        return "froglet".equals(variant) ? 0.5F : "gar".equals(variant) ? 1.9F : 1.0F;
    }

    /**
     * A Dog's height as a fraction of a player's (Dog folk vary a great deal: half a player to half again as tall).
     * The race's model is 3/4 of a player, so the folk's own size factor is this over 0.75. Workshop folk stay at or
     * under 1.1 (a two-high door); soldiers go taller (their towers have three-high archways); the High Guard is 1.5.
     */
    public static float dogHeight(String variant, net.minecraft.util.RandomSource r) {
        return switch (variant) {
            case "high_guard" -> 1.5F;
            case "bandaged" -> 0.75F;
            case "samurai", "archer", "pikeman" -> 0.85F + r.nextFloat() * 0.5F;
            case "biter" -> 0.8F + r.nextFloat() * 0.5F;
            default -> 0.5F + (r.nextFloat() + r.nextFloat()) * 0.3F;     // 0.5 to 1.1, most about 0.8
        };
    }

    /** Variants that never leave home (the rats' Beautiful Ones, who only groom themselves). */
    public static boolean isHomebody(String variant) {
        return "beautiful".equals(variant);
    }

    /** Which of a race's variants a spawn egg or a stray spawn may pick: city officials only come from the city. */
    public boolean randomVariant(String variant) {
        return !("cop".equals(variant) || "bookie".equals(variant) || "gar".equals(variant) || "biter".equals(variant)
                || "bandaged".equals(variant) || "high_guard".equals(variant) || (this == CAT && "archer".equals(variant))
                || (this == CAT && "guard".equals(variant)));
    }

    /** Folk who keep the law or run the bets rather than keep a shop. */
    public boolean tradesAs(String variant) {
        return switch (variant) {
            case "cop", "bookie", "gar", "biter", "bandaged", "pikeman", "high_guard" -> false;
            case "archer" -> this != CAT && this.trades;
            case "guard" -> this != CAT && this.trades;
            default -> this.trades;
        };
    }

    public String variant(int index) {
        return variants[Math.floorMod(index, variants.length)];
    }

    /** What a variant carries in its hand. */
    public ItemStack heldItem(String variant) {
        return switch (variant) {
            case "gong" -> new ItemStack(Items.WOODEN_SHOVEL);   // gong farmers dig out the city's cesspits
            case "farmer" -> new ItemStack(Items.WOODEN_HOE);
            case "samurai" -> new ItemStack(Items.IRON_SWORD);
            case "high_guard" -> new ItemStack(Items.NETHERITE_SWORD);
            case "archer" -> new ItemStack(Items.BOW);   // dog archers and the cats' city archers alike
            case "stablemaster" -> new ItemStack(Items.LEAD);
            case "fisher" -> new ItemStack(Items.FISHING_ROD);
            case "treasurer" -> new ItemStack(Items.GOLD_INGOT);
            case "plumber" -> new ItemStack(Items.BUCKET);
            case "guard" -> new ItemStack(this == CAT ? Items.IRON_SWORD : Items.BAMBOO);   // frogs: a blowpipe
            case "cook" -> new ItemStack(this == DOG ? Items.IRON_SHOVEL : Items.BOWL);   // dogs: a ladle (of sorts)
            case "pikeman" -> new ItemStack(Items.TRIDENT);        // a pike
            case "smith" -> new ItemStack(Items.IRON_PICKAXE);
            case "woodcutter" -> new ItemStack(Items.IRON_AXE);
            case "butcher" -> new ItemStack(Items.IRON_AXE);       // a cleaver
            case "shepherd" -> new ItemStack(Items.SHEARS);
            case "weaver", "tailor" -> new ItemStack(Items.STRING);
            case "dyer" -> new ItemStack(Items.BLUE_DYE);
            case "tanner" -> new ItemStack(Items.LEATHER);
            case "server" -> new ItemStack(Items.BREAD);
            case "musician" -> new ItemStack(Items.GOAT_HORN);
            case "teacher" -> new ItemStack(Items.WRITABLE_BOOK);
            case "fishmonger" -> new ItemStack(Items.COD);
            case "gar" -> new ItemStack(Items.COOKED_SALMON);       // a snack, always
            case "cop" -> new ItemStack(Items.STICK);              // a truncheon
            case "sweeper" -> new ItemStack(Items.BRUSH);
            case "postrat", "bookie" -> new ItemStack(Items.PAPER);
            case "accountant" -> new ItemStack(Items.BOOK);
            case "engineer" -> new ItemStack(Items.REDSTONE_TORCH);
            default -> ItemStack.EMPTY;
        };
    }
}
