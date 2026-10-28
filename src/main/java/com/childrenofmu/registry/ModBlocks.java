package com.childrenofmu.registry;

import com.childrenofmu.ChildrenOfMu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ChildrenOfMu.MODID);

    // Vanilla has no amethyst stairs or slabs; the Ithk towers use these to taper their crystal.
    public static final DeferredBlock<StairBlock> AMETHYST_STAIRS = BLOCKS.register("amethyst_stairs",
            () -> new StairBlock(Blocks.AMETHYST_BLOCK.defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)));

    public static final DeferredBlock<SlabBlock> AMETHYST_SLAB = BLOCKS.register("amethyst_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)));

    // Decorative look-alikes used in Ithk buildings. They drop only themselves, so a building can't be mined
    // for a fortune in emeralds or gold.
    public static final DeferredBlock<Block> INLAID_EMERALD = BLOCKS.registerSimpleBlock("inlaid_emerald",
            BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_BLOCK));
    public static final DeferredBlock<Block> INLAID_GOLD = BLOCKS.registerSimpleBlock("inlaid_gold",
            BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK));
    public static final DeferredBlock<Block> INLAID_LAPIS = BLOCKS.registerSimpleBlock("inlaid_lapis",
            BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_BLOCK));

    /** A Frog town's post box: what the Jumper couriers deliver ends up in here. */
    public static final DeferredBlock<com.childrenofmu.post.PostBoxBlock> POST_BOX = BLOCKS.register("post_box",
            () -> new com.childrenofmu.post.PostBoxBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)));

    // ---- Frog folk decor -----------------------------------------------------------
    /** Bundled swamp reeds: the Frog folk's roofing. Greener and shaggier than straw. */
    public static final DeferredBlock<Block> REED_THATCH = BLOCKS.registerSimpleBlock("reed_thatch",
            BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).sound(net.minecraft.world.level.block.SoundType.GRASS));
    public static final DeferredBlock<StairBlock> REED_THATCH_STAIRS = BLOCKS.register("reed_thatch_stairs",
            () -> new StairBlock(REED_THATCH.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(REED_THATCH.get())));
    public static final DeferredBlock<SlabBlock> REED_THATCH_SLAB = BLOCKS.register("reed_thatch_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(REED_THATCH.get())));
    /** A woven reed lattice, used like a window or a railing. Connects like glass panes. */
    public static final DeferredBlock<net.minecraft.world.level.block.IronBarsBlock> REED_SCREEN = BLOCKS.register("reed_screen",
            () -> new net.minecraft.world.level.block.IronBarsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BAMBOO_PLANKS)
                    .strength(0.5F).noOcclusion().sound(net.minecraft.world.level.block.SoundType.BAMBOO_WOOD)));
    /** A glowing bulb under a lily pad: stands on things or hangs like a lantern. */
    public static final DeferredBlock<net.minecraft.world.level.block.LanternBlock> LILY_LANTERN = BLOCKS.register("lily_lantern",
            () -> new net.minecraft.world.level.block.LanternBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
                    .sound(net.minecraft.world.level.block.SoundType.LILY_PAD)));
    public static final DeferredBlock<com.childrenofmu.block.FrogIdolBlock> FROG_IDOL = BLOCKS.register("frog_idol",
            () -> new com.childrenofmu.block.FrogIdolBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_COBBLESTONE).noOcclusion()));
    public static final DeferredBlock<com.childrenofmu.block.FishingNetBlock> FISHING_NET = BLOCKS.register("fishing_net",
            () -> new com.childrenofmu.block.FishingNetBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)
                    .strength(0.3F).noCollission().noOcclusion()));
    public static final DeferredBlock<com.childrenofmu.block.CattailsBlock> CATTAILS = BLOCKS.register("cattails",
            () -> new com.childrenofmu.block.CattailsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS)
                    .noCollission().noOcclusion().instabreak()));

    /** Fish and bugs strung up to dry: hang them under a fence, rope or beam. Walk-through. */
    public static final DeferredBlock<com.childrenofmu.block.CattailsBlock> STRUNG_FISH = BLOCKS.register("strung_fish",
            () -> new com.childrenofmu.block.CattailsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)
                    .strength(0.2F).noCollission().noOcclusion()));
    public static final DeferredBlock<com.childrenofmu.block.CattailsBlock> STRUNG_BUGS = BLOCKS.register("strung_bugs",
            () -> new com.childrenofmu.block.CattailsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)
                    .strength(0.2F).noCollission().noOcclusion()));

    /** A woven reed sleeping mat: Frog folk curl up on these for the night. */
    public static final DeferredBlock<net.minecraft.world.level.block.CarpetBlock> REED_MAT = BLOCKS.register("reed_mat",
            () -> new net.minecraft.world.level.block.CarpetBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_CARPET)
                    .sound(net.minecraft.world.level.block.SoundType.GRASS)));

    /**
     * A round stone manhole cover for the city's streets: a trapdoor that opens by hand, solid when shut (an iron
     * trapdoor needs redstone and you can see through it).
     */
    public static final net.minecraft.world.level.block.state.properties.BlockSetType MANHOLE_SET =
            net.minecraft.world.level.block.state.properties.BlockSetType.register(
                    new net.minecraft.world.level.block.state.properties.BlockSetType("childrenofmu_manhole", true, true,
                            false, net.minecraft.world.level.block.state.properties.BlockSetType.PressurePlateSensitivity.MOBS,
                            net.minecraft.world.level.block.SoundType.STONE,
                            net.minecraft.sounds.SoundEvents.IRON_DOOR_CLOSE, net.minecraft.sounds.SoundEvents.IRON_DOOR_OPEN,
                            net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE, net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN,
                            net.minecraft.sounds.SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF,
                            net.minecraft.sounds.SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON,
                            net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_OFF,
                            net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON));
    public static final DeferredBlock<net.minecraft.world.level.block.TrapDoorBlock> MANHOLE = BLOCKS.register("manhole",
            () -> new net.minecraft.world.level.block.TrapDoorBlock(MANHOLE_SET,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(2.0F, 6.0F).noOcclusion()
                            .isValidSpawn((st, l, p, t) -> false)));

    /** A banker's lamp (brass and green glass), for desks. */
    public static final DeferredBlock<com.childrenofmu.block.BankersLampBlock> BANKERS_LAMP = BLOCKS.register("bankers_lamp",
            () -> new com.childrenofmu.block.BankersLampBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
                    .lightLevel(s -> 12).noOcclusion()));

    private ModBlocks() {}
}
