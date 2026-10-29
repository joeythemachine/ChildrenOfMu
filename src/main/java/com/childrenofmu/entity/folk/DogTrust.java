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
        // Meat, bone, and the things of their crafts (never bread, cake or fruit: Dog folk eat only meat).
        if (stack.is(Items.BONE) || stack.is(Items.CHICKEN) || stack.is(Items.RABBIT) || stack.is(ItemTags.WOOL) || stack.is(Items.STRING)
                || stack.is(Items.LEATHER) || stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.MUTTON)) {
            return 1;
        }
        if (stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.COOKED_CHICKEN) || stack.is(Items.COOKED_RABBIT)
                || stack.is(Items.EMERALD) || stack.is(Items.BOOK) || stack.is(Items.LAPIS_LAZULI)
                || stack.is(Items.IRON_INGOT) || stack.getItem() instanceof net.minecraft.world.item.DyeItem) {
            return 2;
        }
        if (stack.is(Items.GOLD_INGOT) || stack.is(Items.DIAMOND) || stack.is(Items.ENCHANTED_BOOK)) {
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
        // The soldiers: they don't trade, but one the monks trust may ask them to march with them.
        if ("samurai".equals(v) || "pikeman".equals(v) || "archer".equals(v)) {
            if (!client && player instanceof ServerPlayer sp) {
                recruit(sp, dog);
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

    // ---------------------------------------------------------------- soldiers who march with you

    static final int COMPANIONS = 2;

    private static void recruit(ServerPlayer player, FolkEntity soldier) {
        String name = soldier.getDisplayName().getString();
        if (player.getUUID().equals(soldier.companionOf)) {
            if (player.isSecondaryUseActive()) {
                soldier.joinCompanion(null);
                player.sendSystemMessage(Component.literal(name + " salutes. \"Then I return to my post. Call on me "
                        + "again, brother.\""));
            } else {
                player.sendSystemMessage(Component.literal(name + ": \"I am with you.\" (Sneak and speak to send me "
                        + "home.)").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        if (soldier.companionOf != null) {
            player.sendSystemMessage(Component.literal(name + " is sworn to march with another."));
            return;
        }
        StructureStart start = city(player.serverLevel(), soldier.blockPosition());
        int[] s = start == null ? null : get(player.serverLevel()).of(id(start), player.getUUID());
        if (s == null || s[0] < KEEP || s[1] < QUESTIONS.length) {
            player.sendSystemMessage(Component.literal(name + " stands at ease and does not trade. \"I serve the city. "
                    + "Should the monks ever call you brother, I would march with you.\""));
            return;
        }
        int have = 0;
        for (FolkEntity f : player.serverLevel().getEntitiesOfClass(FolkEntity.class, player.getBoundingBox().inflate(160))) {
            if (player.getUUID().equals(f.companionOf)) {
                have++;
            }
        }
        if (have >= COMPANIONS) {
            player.sendSystemMessage(Component.literal(name + " looks at the soldiers already at your side. \"You "
                    + "have company enough. The city cannot spare more.\""));
            return;
        }
        soldier.joinCompanion(player);
        soldier.setTarget(null);
        soldier.escortOf = null;
        player.sendSystemMessage(Component.literal(name + " bows. \"The monks call you brother. Where you go, I go.\"")
                .withStyle(ChatFormatting.GOLD));
        soldier.playSound(SoundEvents.WOLF_AMBIENT, 0.8F, 0.9F);
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
    /** Each player's last attempt on the Keep, and how many in a row (a second, unheeded, gets them escorted out). */
    private static final Map<UUID, long[]> BREACHES = new HashMap<>();
    /** Players being walked out of a city: the city, and the Keep's middle. */
    private record Escort(long city, Vec3 keep) {}
    private static final Map<UUID, Escort> ESCORTS = new HashMap<>();
    /** A city's alarm: who broke into the courtyard, until when, where the bell hangs. */
    private static final class Alarm {
        UUID target;
        long until, started, lastRing;
        @Nullable BlockPos bell;
        Vec3 keep;
    }
    private static final Map<Long, Alarm> ALARMS = new HashMap<>();

    private static final String[] ESCORT_LINES = {
            "\"This way, stranger. The gate is behind you.\"",
            "\"Keep walking. Out of the city.\"",
            "\"You were warned. We will see you to the bridge.\"",
            "\"No farther in. Only out.\"",
            "\"Walk. We are in no hurry, and neither are you.\""};

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        StructureStart start = city(level, player.blockPosition());
        tickEscort(level, player, start, now);
        BoundingBox keep = start == null ? null : keep(start);
        int zone = keep == null ? 0 : zone(keep, player.position());
        if (zone == 0) {
            ALLOWED.put(player.getUUID(), player.position());
            return;
        }
        int[] s = get(level).of(id(start), player.getUUID());
        Vec3 middle = new Vec3(keep.minX() + 24.5, keep.minY() + 5, keep.minZ() + 24.5);
        if (zone == 2 && !(s[0] >= KEEP && s[1] >= QUESTIONS.length)) {
            // Into the courtyard without the monks' trust: the High Guard falls on them, and the monks ring the alarm.
            alarm(level, id(start), player, middle, keep, now);
            return;
        }
        if (zone == 2 || s[0] >= KEEP) {
            ALLOWED.put(player.getUUID(), player.position());
            return;
        }
        // Into the Keep's halls a stranger: turned back. The first time a warning; the next, an escort out of town.
        Vec3 back = ALLOWED.get(player.getUUID());
        if (back == null || zone(keep, back) >= zone) {
            Vec3 out = player.position().subtract(middle).multiply(1, 0, 1);
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, -1) : out.normalize();
            back = middle.add(out.scale(21.5));
        }
        player.teleportTo(back.x, back.y, back.z);
        player.setDeltaMovement(Vec3.ZERO);
        if (now - WARNED.getOrDefault(player.getUUID(), -1000L) < 60) {
            return;                                                       // (one turning-back at a time)
        }
        WARNED.put(player.getUUID(), now);
        long[] b = BREACHES.computeIfAbsent(player.getUUID(), k -> new long[2]);
        b[0] = now - b[1] > 6000 ? 1 : b[0] + 1;                          // a fresh start after five minutes
        b[1] = now;
        level.playSound(null, player.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.NEUTRAL, 0.8F, 0.9F);
        if (b[0] <= 1) {
            player.sendSystemMessage(Component.literal("A guard steps into your path. \"The Keep is not for strangers. "
                    + "Be known among the folk first. Do not try it again.\" (" + Math.max(0, s[0]) + "/" + KEEP + ")")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            ESCORTS.put(player.getUUID(), new Escort(id(start), middle));
            player.sendSystemMessage(Component.literal("\"You were told.\" The guards close in around you. \"Now you "
                    + "leave the city, and we walk with you.\"").withStyle(ChatFormatting.GOLD));
        }
    }

    /** The guards walking someone out: picked, kept on the Keep's side of them, talking, until they're out of town. */
    private static void tickEscort(ServerLevel level, ServerPlayer player, @Nullable StructureStart start, long now) {
        Escort e = ESCORTS.get(player.getUUID());
        if (e == null) {
            return;
        }
        if (start == null || id(start) != e.city()) {
            ESCORTS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("The guards stop at the edge of the city and watch you go. "
                    + "\"Come back when the folk know your name.\"").withStyle(ChatFormatting.GRAY));
            return;
        }
        java.util.List<FolkEntity> mine = new java.util.ArrayList<>(), free = new java.util.ArrayList<>();
        for (FolkEntity f : level.getEntitiesOfClass(FolkEntity.class, player.getBoundingBox().inflate(64))) {
            if (!FolkCombat.isDogSoldier(f) || "high_guard".equals(f.getVariantName()) || f.getTarget() != null
                    || f.companionOf != null) {
                continue;
            }
            (player.getUUID().equals(f.escortOf) && now <= f.escortUntil + 40 ? mine : free).add(f);
        }
        free.sort(java.util.Comparator.comparingDouble(f -> f.distanceToSqr(player)));
        for (FolkEntity f : free) {
            if (mine.size() >= 3) {
                break;
            }
            f.escortSlot = mine.size();
            mine.add(f);
        }
        FolkEntity speaker = null;
        for (FolkEntity f : mine) {
            f.escortOf = player.getUUID();
            f.escortUntil = now + 60;
            f.escortFrom = e.keep();
            if (speaker == null || f.distanceToSqr(player) < speaker.distanceToSqr(player)) {
                speaker = f;
            }
        }
        if (speaker != null && now % 140 < 5 && speaker.distanceToSqr(player) < 20 * 20) {
            player.sendSystemMessage(Component.literal(speaker.getDisplayName().getString() + ": "
                    + ESCORT_LINES[(int) (now / 140 % ESCORT_LINES.length)]).withStyle(ChatFormatting.YELLOW));
        }
    }

    /** The courtyard's alarm: the High Guard attacks; a monk runs for the bell; the soldiers come from everywhere. */
    private static void alarm(ServerLevel level, long city, ServerPlayer player, Vec3 middle, BoundingBox keep, long now) {
        Alarm a = ALARMS.get(city);
        boolean fresh = a == null || now > a.until;
        if (fresh) {
            a = new Alarm();
            a.started = now;
            a.keep = middle;
            a.bell = findBell(level, keep);
            ALARMS.put(city, a);
            player.sendSystemMessage(Component.literal("\"Intruder in the courtyard!\" A monk's cry goes up, and the "
                    + "High Guard turn as one.").withStyle(ChatFormatting.RED));
            level.playSound(null, player.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 1.5F, 0.6F);
        }
        a.target = player.getUUID();
        a.until = now + 400;
        ESCORTS.remove(player.getUUID());
    }

    @Nullable
    private static BlockPos findBell(ServerLevel level, BoundingBox keep) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = keep.minY() + 5; y <= keep.maxY(); y++) {
            for (int x = keep.minX(); x <= keep.maxX(); x++) {
                for (int z = keep.minZ(); z <= keep.maxZ(); z++) {
                    if (level.getBlockState(p.set(x, y, z)).is(net.minecraft.world.level.block.Blocks.BELL)) {
                        return p.immutable();
                    }
                }
            }
        }
        return null;
    }

    /** Every second while a city's alarm is up (and once more when it's over, to stand everyone down). */
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (ALARMS.isEmpty() || event.getServer().getTickCount() % 10 != 0) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        long now = level.getGameTime();
        java.util.Iterator<Map.Entry<Long, Alarm>> it = ALARMS.entrySet().iterator();
        while (it.hasNext()) {
            Alarm a = it.next().getValue();
            ServerPlayer target = level.getServer().getPlayerList().getPlayer(a.target);
            boolean over = now > a.until || target == null || !target.isAlive() || target.isCreative()
                    || target.level() != level;
            net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(a.keep, a.keep).inflate(110, 40, 110);
            FolkEntity ringer = null;
            for (FolkEntity f : level.getEntitiesOfClass(FolkEntity.class, area)) {
                if (FolkCombat.isDogSoldier(f) && f.companionOf == null) {
                    if (over) {
                        if (f.getTarget() != null && f.getTarget().getUUID().equals(a.target)) {
                            f.setTarget(null);                            // stand down
                        }
                    } else if (f.getTarget() == null && ("high_guard".equals(f.getVariantName())
                            || now - a.started > 60)) {
                        f.setTarget(target);                              // the High Guard at once, the rest as they hear
                    }
                } else if (!over && a.bell != null && ("monk".equals(f.getVariantName())
                        || "husky_monk".equals(f.getVariantName()))
                        && (ringer == null || f.distanceToSqr(a.bell.getCenter()) < ringer.distanceToSqr(a.bell.getCenter()))) {
                    ringer = f;
                }
            }
            if (over) {
                it.remove();
                continue;
            }
            if (a.bell != null) {
                // The nearest monk runs for the bell; it rings once he's there (or anyway, after a few seconds).
                boolean there = ringer != null && ringer.distanceToSqr(a.bell.getCenter()) < 5 * 5;
                if (ringer != null && !there) {
                    ringer.getNavigation().moveTo(a.bell.getX() + 0.5, a.bell.getY() - 2, a.bell.getZ() + 0.5, 1.4);
                }
                if ((there || now - a.started > 100) && now - a.lastRing >= 30) {
                    a.lastRing = now;
                    net.minecraft.world.level.block.state.BlockState bell = level.getBlockState(a.bell);
                    if (bell.getBlock() instanceof net.minecraft.world.level.block.BellBlock bb) {
                        bb.attemptToRing(ringer, level, a.bell, null);
                    }
                    level.playSound(null, a.bell, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 3.0F, 0.8F);
                }
            }
        }
    }
}
