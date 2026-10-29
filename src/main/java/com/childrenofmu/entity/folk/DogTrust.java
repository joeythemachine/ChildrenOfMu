package com.childrenofmu.entity.folk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.childrenofmu.ChildrenOfMu;
import com.childrenofmu.worldgen.IthkBuildingPiece;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * How far a Dog City trusts each player (each city judges for itself).
 *
 *  - The folk's confidence: earned by fair trade, by gifts they value, and by killing hostile things inside the walls;
 *    lost (badly) by killing one of them. Enough of it and the Keep's guards let you in.
 *  - The monks' confidence: they put five moral questions, one at a time, whose answers only make sense to someone who
 *    has read the five fables in the Keep's library. All five answered rightly, and the courtyard is open to you.
 *
 * The Keep's guards turn away anyone who hasn't earned it (they're never attacked for trying; that is the dark
 * tower's business).
 */
public class DogTrust extends SavedData {
    private static final String NAME = "childrenofmu_dog_trust";
    public static final int KEEP = 30;          // confidence that opens the Keep
    public static final int MAX = 100;
    private static final ResourceKey<Structure> DOG_CITY =
            ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(ChildrenOfMu.MODID, "dog_city"));

    /** city|player -> {confidence, monks' questions answered (5 = trusted)}. */
    private final Map<String, int[]> standing = new HashMap<>();

    public static DogTrust get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DogTrust::new, DogTrust::load, null), NAME);
    }

    private static DogTrust load(CompoundTag tag, HolderLookup.Provider registries) {
        DogTrust t = new DogTrust();
        CompoundTag all = tag.getCompound("Standing");
        for (String k : all.getAllKeys()) {
            int[] v = all.getIntArray(k);
            if (v.length == 2) {
                t.standing.put(k, v);
            }
        }
        return t;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        this.standing.forEach(all::putIntArray);
        tag.put("Standing", all);
        return tag;
    }

    private int[] of(long city, UUID player) {
        return this.standing.computeIfAbsent(city + "|" + player, k -> new int[2]);
    }

    // ---------------------------------------------------------------- where

    /** The Dog City this spot is in, or null. */
    @Nullable
    static StructureStart city(ServerLevel level, BlockPos pos) {
        Structure s = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(DOG_CITY);
        if (s == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureAt(pos, s);
        return start.isValid() ? start : null;
    }

    private static long id(StructureStart start) {
        return start.getChunkPos().toLong();
    }

    /** The Keep's building piece in a city. */
    @Nullable
    private static BoundingBox keep(StructureStart start) {
        for (StructurePiece p : start.getPieces()) {
            if (p instanceof IthkBuildingPiece b && b.templateId().endsWith("dc_keep")) {
                return b.getBoundingBox();
            }
        }
        return null;
    }

    /** 0 outside the Keep, 1 in its halls, 2 in the courtyard (its walls and floor: see make_dog_city.py keep()). */
    private static int zone(BoundingBox keep, Vec3 at) {
        double cx = keep.minX() + 24.5, cz = keep.minZ() + 24.5;
        double d = Math.max(Math.abs(at.x - cx), Math.abs(at.z - cz));
        if (at.y < keep.minY() + 4.5 || at.y > keep.minY() + 17) {
            return 0;
        }
        return d < 12.0 ? 2 : d < 19.8 ? 1 : 0;
    }

    // ---------------------------------------------------------------- the folk's confidence

    /** Raise (or lower) a player's standing with the city at this spot. */
    public static void raise(ServerPlayer player, BlockPos where, int amount) {
        StructureStart start = city(player.serverLevel(), where);
        if (start == null) {
            return;
        }
        DogTrust t = get(player.serverLevel());
        int[] s = t.of(id(start), player.getUUID());
        int before = s[0];
        s[0] = Math.max(-MAX, Math.min(MAX, s[0] + amount));
        t.setDirty();
        if (before < KEEP && s[0] >= KEEP) {
            player.sendSystemMessage(Component.literal("The dog folk of this city have come to trust you. "
                    + "The Keep's guards will let you pass.").withStyle(ChatFormatting.GOLD));
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                    0.6F, 1.2F);
        } else if (amount > 0) {
            player.displayClientMessage(Component.literal("The dogs' confidence in you grows ("
                    + Math.max(0, s[0]) + "/" + KEEP + ")").withStyle(ChatFormatting.GRAY), true);
        } else if (amount < 0) {
            player.displayClientMessage(Component.literal("The dogs will not soon forget that.")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    /** What a gift is worth to a dog (0: not a gift). */
    private static int giftValue(ItemStack stack) {
        if (stack.is(Items.BONE) || stack.is(Items.BREAD) || stack.is(ItemTags.WOOL) || stack.is(Items.STRING)
                || stack.is(Items.LEATHER) || stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.MUTTON)) {
            return 1;
        }
        if (stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.EMERALD) || stack.is(Items.BOOK) || stack.is(Items.LAPIS_LAZULI)
                || stack.is(Items.IRON_INGOT) || stack.getItem() instanceof net.minecraft.world.item.DyeItem) {
            return 2;
        }
        if (stack.is(Items.RABBIT_STEW) || stack.is(Items.GOLD_INGOT) || stack.is(Items.GOLDEN_CARROT)
                || stack.is(Items.CAKE) || stack.is(Items.DIAMOND) || stack.is(Items.ENCHANTED_BOOK)) {
            return 4;
        }
        return 0;
    }

    /** Each dog takes one gift from each player a day. */
    private static final Map<String, Long> GIFTS = new HashMap<>();
    private static final Map<UUID, Long> WRONG = new HashMap<>();

    /**
     * A player speaks to a dog. Returns a result if this handled it (a gift, a monk's question), or null to carry on
     * as usual (trading).
     */
    @Nullable
    static InteractionResult interact(FolkEntity dog, Player player) {
        String v = dog.getVariantName();
        if ("biter".equals(v) || "bandaged".equals(v)) {
            return null;
        }
        boolean client = dog.level().isClientSide;
        ItemStack held = player.getMainHandItem();
        // A gift: offered sneaking, with something a dog values.
        if (player.isSecondaryUseActive() && giftValue(held) > 0 && !"monk".equals(v) && !"husky_monk".equals(v)) {
            if (!client && player instanceof ServerPlayer sp && city(sp.serverLevel(), dog.blockPosition()) != null) {
                long day = dog.level().getDayTime() / 24000L;
                String key = dog.getUUID() + "|" + player.getUUID();
                if (GIFTS.getOrDefault(key, -1L) == day) {
                    sp.sendSystemMessage(Component.literal(dog.getDisplayName().getString()
                            + " bows, but will take nothing more from you today."));
                } else {
                    GIFTS.put(key, day);
                    int worth = giftValue(held);
                    held.shrink(1);
                    sp.sendSystemMessage(Component.literal(dog.getDisplayName().getString()
                            + " accepts your gift with both paws, and bows."));
                    dog.playSound(SoundEvents.WOLF_AMBIENT, 0.6F, 1.2F);
                    raise(sp, dog.blockPosition(), worth * 2);
                }
            }
            return InteractionResult.sidedSuccess(client);
        }
        // The monks of the Keep: their questions (sneak to trade with them instead).
        if (("monk".equals(v) || "husky_monk".equals(v))) {
            if (client) {
                return InteractionResult.SUCCESS;
            }
            if (!(player instanceof ServerPlayer sp)) {
                return null;
            }
            StructureStart start = city(sp.serverLevel(), dog.blockPosition());
            if (start == null) {
                return null;                                              // a monk out in the world: just trades
            }
            if (sp.isSecondaryUseActive()) {
                dog.openTrades(sp);
                return InteractionResult.SUCCESS;
            }
            ask(sp, dog, start);
            return InteractionResult.SUCCESS;
        }
        return null;
    }

    // ---------------------------------------------------------------- the monks' questions

    /** The questions, their answers, and which answer is right (the fables are in make_dog_city.py, FABLES). */
    static final String[] QUESTIONS = {
            "Three bowls are set before you: gold and full, oak and heaped, clay and empty. Which do you lift?",
            "The storm rises against your banner on the wall. What do you do?",
            "You weigh out meat for a stranger. Where does your thumb rest?",
            "The bell in the back hall has not rung in a hundred years. What is it worth?",
            "In the high room one kneels, wrapped in linen. What must never be done?"};
    static final String[][] ANSWERS = {
            {"The gold, for it is precious", "The oak, for it feeds", "The clay, for it is empty"},
            {"Bind it tighter to the pole", "Untie it and let it fly", "Take it down until the storm is past"},
            {"On the scale, as all do", "Upon my own heart", "Behind my back, away from temptation"},
            {"Its weight in bronze", "Every year it has not rung", "Nothing, until it rings"},
            {"To bring him food", "To loose a turn of his linen", "To kneel beside him"}};
    static final int[] RIGHT = {2, 1, 1, 1, 1};

    private static void ask(ServerPlayer player, FolkEntity monk, StructureStart start) {
        DogTrust t = get(player.serverLevel());
        int[] s = t.of(id(start), player.getUUID());
        String name = monk.getDisplayName().getString();
        if (s[0] < KEEP) {
            player.sendSystemMessage(Component.literal(name + " regards you kindly, and says only: \"Be known among "
                    + "the folk of the city first, stranger.\" (Sneak to trade.)"));
            return;
        }
        if (s[1] >= QUESTIONS.length) {
            player.sendSystemMessage(Component.literal(name + " bows to you as to a brother. \"Walk in the courtyard "
                    + "as you will.\" (Sneak to trade.)"));
            return;
        }
        Long wrong = WRONG.get(player.getUUID());
        if (wrong != null && player.serverLevel().getGameTime() - wrong < 1200) {
            player.sendSystemMessage(Component.literal(name + " shakes his head. \"Go and read again. Come back when "
                    + "the fables have settled in you.\""));
            return;
        }
        MutableComponent card = Component.literal(name + " asks (" + (s[1] + 1) + " of " + QUESTIONS.length + "): \""
                + QUESTIONS[s[1]] + "\"").withStyle(ChatFormatting.WHITE);
        for (int i = 0; i < ANSWERS[s[1]].length; i++) {
            Style style = Style.EMPTY.withColor(ChatFormatting.GOLD).withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/childrenofmu answer " + monk.getId() + " " + s[1] + " " + i))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Give this answer")));
            card.append("\n  ").append(Component.literal("[" + ANSWERS[s[1]][i] + "]").withStyle(style));
        }
        player.sendSystemMessage(card);
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("childrenofmu")
                .then(Commands.literal("answer")
                        .then(Commands.argument("monk", IntegerArgumentType.integer())
                                .then(Commands.argument("question", IntegerArgumentType.integer(0, 9))
                                        .then(Commands.argument("choice", IntegerArgumentType.integer(0, 9))
                                                .executes(DogTrust::answer))))));
    }

    private static int answer(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        Entity e = player.serverLevel().getEntity(IntegerArgumentType.getInteger(ctx, "monk"));
        int q = IntegerArgumentType.getInteger(ctx, "question"), choice = IntegerArgumentType.getInteger(ctx, "choice");
        if (!(e instanceof FolkEntity monk) || monk.distanceTo(player) > 10.0F) {
            player.sendSystemMessage(Component.literal("The monk has gone about his duties."));
            return 0;
        }
        StructureStart start = city(player.serverLevel(), monk.blockPosition());
        if (start == null) {
            return 0;
        }
        DogTrust t = get(player.serverLevel());
        int[] s = t.of(id(start), player.getUUID());
        if (s[0] < KEEP || s[1] != q || q >= QUESTIONS.length) {
            return 0;                                                     // an old question, already answered
        }
        String name = monk.getDisplayName().getString();
        if (choice == RIGHT[q]) {
            s[1]++;
            t.setDirty();
            if (s[1] >= QUESTIONS.length) {
                player.sendSystemMessage(Component.literal(name + " is silent a long while, then bows low. \"You have "
                        + "understood. The courtyard is open to you, brother.\"").withStyle(ChatFormatting.GOLD));
                player.serverLevel().playSound(null, monk.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL,
                        1.0F, 0.7F);
            } else {
                player.sendSystemMessage(Component.literal(name + " nods slowly. \"Yes.\" (Speak to a monk again for "
                        + "the next question.)").withStyle(ChatFormatting.GREEN));
            }
        } else {
            s[1] = 0;
            t.setDirty();
            WRONG.put(player.getUUID(), player.serverLevel().getGameTime());
            player.sendSystemMessage(Component.literal(name + " closes his eyes. \"You have read the words, not the "
                    + "fable. We will begin again, another day.\"").withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    // ---------------------------------------------------------------- deeds, and the guards at the doors

    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getEntity() instanceof Enemy) {
            raise(player, event.getEntity().blockPosition(), 1);          // one less monster at the walls
        } else if (event.getEntity() instanceof FolkEntity f && f.getRace() == FolkRace.DOG) {
            raise(player, f.blockPosition(), -40);
        }
    }

    /** Where each player last stood that they were allowed to be (to send them back to). */
    private static final Map<UUID, Vec3> ALLOWED = new HashMap<>();
    private static final Map<UUID, Long> WARNED = new HashMap<>();

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        StructureStart start = city(level, player.blockPosition());
        BoundingBox keep = start == null ? null : keep(start);
        int zone = keep == null ? 0 : zone(keep, player.position());
        if (zone == 0) {
            ALLOWED.put(player.getUUID(), player.position());
            return;
        }
        int[] s = get(level).of(id(start), player.getUUID());
        boolean ok = zone == 1 ? s[0] >= KEEP : s[0] >= KEEP && s[1] >= QUESTIONS.length;
        if (ok) {
            ALLOWED.put(player.getUUID(), player.position());
            return;
        }
        // Turned back: to where they last stood (or out onto the terrace before the Keep's gate).
        Vec3 back = ALLOWED.get(player.getUUID());
        if (back == null || zone(keep, back) >= zone) {
            back = new Vec3(keep.minX() + 24.5, keep.minY() + 5, keep.minZ() + 24.5);
            Vec3 out = player.position().subtract(back).multiply(1, 0, 1);
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, -1) : out.normalize();
            back = back.add(out.scale(zone == 2 ? 14.0 : 21.5));
        }
        player.teleportTo(back.x, back.y, back.z);
        player.setDeltaMovement(Vec3.ZERO);
        long now = level.getGameTime();
        if (now - WARNED.getOrDefault(player.getUUID(), -1000L) > 60) {
            WARNED.put(player.getUUID(), now);
            String line = zone == 1
                    ? "A guard steps into your path. \"The Keep is not for strangers. Be known among the folk first.\" ("
                    + Math.max(0, s[0]) + "/" + KEEP + ")"
                    : "A monk raises a paw. \"The courtyard is for those who have understood the fables.\" ("
                    + Math.min(s[1], QUESTIONS.length) + "/" + QUESTIONS.length + " answered)";
            player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.YELLOW));
            level.playSound(null, player.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.NEUTRAL, 0.7F, 0.9F);
        }
    }
}
