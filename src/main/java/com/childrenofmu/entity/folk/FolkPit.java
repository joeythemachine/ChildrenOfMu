package com.childrenofmu.entity.folk;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.childrenofmu.registry.ModEntities;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Pit fights. Every few minutes, when someone's around to watch, the Bat bookie by the fighting pit calls a bout: a Cat
 * against a gang of Rats, Rat against Rat, or Cat against Cat. Bets are open for half a minute (click a stake in chat,
 * or right-click the bookie for the card); then the fighters drop into the sand and go at it until one side taps out.
 * Winning bets pay double. Bats never fight: they take the bets. Nobody dies in the pit (well, hardly anybody):
 * a beaten fighter yields, and the fighters are carried off after the bout.
 */
public final class FolkPit {
    private static final int BETTING_TICKS = 600;
    private static final int MAX_BOUT_TICKS = 1800;
    private static final int[] STAKES = {1, 5, 20};

    /** A bookie's book: the bout being run, its fighters and its bets. */
    static final class Book {
        int state;            // 0 idle, 1 taking bets, 2 fighting, 3 carrying them off
        long until;
        long nextBout;
        int card;
        @Nullable BlockPos pit;
        boolean searched;
        final List<UUID> sideA = new ArrayList<>();
        final List<UUID> sideB = new ArrayList<>();
        final List<Bet> bets = new ArrayList<>();
        Component nameA = Component.empty();
        Component nameB = Component.empty();
    }

    record Bet(UUID player, int side, int amount) {}

    private FolkPit() {}

    static boolean isBookie(FolkEntity e) {
        return e.getRace() == FolkRace.BAT && "bookie".equals(e.getVariantName());
    }

    /** The middle of the fighting pit: the sand floor nearest the bookie. */
    @Nullable
    private static BlockPos findPit(ServerLevel level, BlockPos from) {
        long sx = 0, sy = 0, sz = 0;
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(from.offset(-14, -8, -14), from.offset(14, 2, 14))) {
            if (level.getBlockState(p).is(Blocks.SAND) && level.getBlockState(p.above()).isAir()) {
                sx += p.getX();
                sy += p.getY();
                sz += p.getZ();
                n++;
            }
        }
        return n < 9 ? null : new BlockPos((int) Math.floorDiv(sx, n), (int) Math.floorDiv(sy, n) + 1, (int) Math.floorDiv(sz, n));
    }

    private static List<ServerPlayer> crowd(ServerLevel level, BlockPos pit) {
        return level.getPlayers(p -> p.blockPosition().distSqr(pit) < 32 * 32);
    }

    private static void announce(ServerLevel level, BlockPos pit, Component message) {
        for (ServerPlayer p : crowd(level, pit)) {
            p.sendSystemMessage(message);
        }
    }

    /** Every tick on the bookie (it only does anything once a second). */
    static void tickBookie(FolkEntity bookie) {
        if (!(bookie.level() instanceof ServerLevel level) || bookie.tickCount % 20 != 0) {
            return;
        }
        Book book = bookie.book();
        if (!book.searched || (book.pit == null && bookie.tickCount % 1200 == 0)) {
            book.searched = true;     // (and look again now and then, if there was no pit to be found)
            book.pit = findPit(level, bookie.blockPosition());
            book.nextBout = level.getGameTime() + 400;
        }
        if (book.pit == null) {
            return;
        }
        long now = level.getGameTime();
        clearPit(level, book);
        switch (book.state) {
            case 0 -> {
                if (now >= book.nextBout && !crowd(level, book.pit).isEmpty()) {
                    open(level, bookie, book);
                }
            }
            case 1 -> {
                if (now >= book.until) {
                    startFight(level, bookie, book);
                }
            }
            case 2 -> fight(level, bookie, book, now);
            default -> {
                touch(level, book, now);
                if (now >= book.until) {
                    for (UUID id : concat(book)) {
                        Entity e = level.getEntity(id);
                        if (e != null) {
                            FolkLaw.spawnSmoke(level, e);
                            e.discard();
                        }
                    }
                    book.sideA.clear();
                    book.sideB.clear();
                    book.bets.clear();
                    book.state = 0;
                    book.nextBout = now + 2400 + bookie.getRandom().nextInt(1800);
                }
            }
        }
    }

    /**
     * Nobody but the fighters belongs on the sand: anyone else who's wandered or fallen in (a cop on his beat, a
     * curious rat) is hauled out onto the nearest seat in the stands.
     */
    private static void clearPit(ServerLevel level, Book book) {
        BlockPos pit = book.pit;
        net.minecraft.world.phys.AABB sand = new net.minecraft.world.phys.AABB(pit).inflate(5.0, 1.0, 5.0).expandTowards(0, 1.5, 0);
        for (FolkEntity f : level.getEntitiesOfClass(FolkEntity.class, sand,
                f -> !f.pitFighter && !isBookie(f) && f.isAlive() && !f.isPassenger())) {
            BlockPos out = null;
            double best = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(pit.offset(-9, 1, -9), pit.offset(9, 6, 9))) {
                int d = Math.max(Math.abs(p.getX() - pit.getX()), Math.abs(p.getZ() - pit.getZ()));
                if (d < 6 || !level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                        || !level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                        || !level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)
                           && !level.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.STAIRS)) {
                    continue;
                }
                double dd = p.distToCenterSqr(f.position());
                if (dd < best) {
                    best = dd;
                    out = p.immutable();
                }
            }
            if (out != null) {
                f.getNavigation().stop();
                f.moveTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5, f.getYRot(), 0.0F);
            }
        }
    }

    private static List<UUID> concat(Book book) {
        List<UUID> all = new ArrayList<>(book.sideA);
        all.addAll(book.sideB);
        return all;
    }

    private static void open(ServerLevel level, FolkEntity bookie, Book book) {
        RandomSource r = bookie.getRandom();
        book.card = r.nextInt(3);
        book.nameA = FolkNames.fighterName(book.card == 1 ? FolkRace.RAT : FolkRace.CAT, r);
        book.nameB = book.card == 0 ? Component.translatable("pit.childrenofmu.the_rats")
                : FolkNames.fighterName(book.card == 2 ? FolkRace.CAT : FolkRace.RAT, r);
        book.state = 1;
        book.until = level.getGameTime() + BETTING_TICKS;
        level.playSound(null, bookie.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1.2F, 0.8F);
        for (ServerPlayer p : crowd(level, book.pit)) {
            offer(bookie, p);
        }
    }

    /** The card and the stakes, clickable, in chat. */
    static void offer(FolkEntity bookie, Player player) {
        Book book = bookie.book();
        if (book.pit == null) {
            player.sendSystemMessage(Component.translatable("pit.childrenofmu.no_pit", bookie.getDisplayName()));
            return;
        }
        if (book.state != 1) {
            player.sendSystemMessage(Component.translatable(book.state == 2 ? "pit.childrenofmu.closed"
                    : "pit.childrenofmu.later." + bookie.getRandom().nextInt(3), bookie.getDisplayName()));
            return;
        }
        MutableComponent card = Component.translatable("pit.childrenofmu.card." + book.card, bookie.getDisplayName(),
                book.nameA, book.nameB);
        for (int side = 0; side < 2; side++) {
            card.append("\n ").append(Component.translatable("pit.childrenofmu.on", side == 0 ? book.nameA : book.nameB));
            for (int stake : STAKES) {
                Style style = Style.EMPTY.withColor(ChatFormatting.GOLD).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/childrenofmu bet " + bookie.getId() + " " + side + " " + stake))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("pit.childrenofmu.hover", stake, stake * 2)));
                card.append(" ").append(Component.literal("[" + stake + "]").withStyle(style));
            }
        }
        player.sendSystemMessage(card);
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("childrenofmu")
                .then(Commands.literal("bet")
                        .then(Commands.argument("bookie", IntegerArgumentType.integer())
                                .then(Commands.argument("side", IntegerArgumentType.integer(0, 1))
                                        .then(Commands.argument("stake", IntegerArgumentType.integer(1, 64))
                                                .executes(FolkPit::bet))))));
    }

    private static int bet(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        Entity e = player.serverLevel().getEntity(IntegerArgumentType.getInteger(ctx, "bookie"));
        int side = IntegerArgumentType.getInteger(ctx, "side");
        int stake = IntegerArgumentType.getInteger(ctx, "stake");
        if (!(e instanceof FolkEntity bookie) || !isBookie(bookie) || bookie.distanceTo(player) > 40.0F) {
            player.sendSystemMessage(Component.translatable("pit.childrenofmu.gone"));
            return 0;
        }
        Book book = bookie.book();
        if (book.state != 1) {
            player.sendSystemMessage(Component.translatable("pit.childrenofmu.closed", bookie.getDisplayName()));
            return 0;
        }
        for (Bet b : book.bets) {
            if (b.player().equals(player.getUUID()) && b.side() != side) {
                player.sendSystemMessage(Component.translatable("pit.childrenofmu.one_side", bookie.getDisplayName()));
                return 0;
            }
        }
        if (!FolkGar.mayTrade(bookie, player)) {
            return 0;
        }
        if (player.getInventory().countItem(Items.EMERALD) < stake) {
            player.sendSystemMessage(Component.translatable("pit.childrenofmu.poor", bookie.getDisplayName()));
            return 0;
        }
        player.getInventory().clearOrCountMatchingItems(s -> s.is(Items.EMERALD), stake, player.inventoryMenu.getCraftSlots());
        book.bets.add(new Bet(player.getUUID(), side, stake));
        player.sendSystemMessage(Component.translatable("pit.childrenofmu.taken", bookie.getDisplayName(), stake,
                side == 0 ? book.nameA : book.nameB));
        bookie.playSound(SoundEvents.BAT_AMBIENT, 0.6F, 1.4F);
        return 1;
    }

    // ---- The bout --------------------------------------------------------------------------

    private static void startFight(ServerLevel level, FolkEntity bookie, Book book) {
        RandomSource r = bookie.getRandom();
        BlockPos pit = book.pit;
        switch (book.card) {
            case 0 -> {
                book.sideA.add(fighter(level, FolkRace.CAT, pit.offset(-3, 0, 0), 30, 4.0, book.nameA, r));
                for (int i = 0; i < 3; i++) {
                    book.sideB.add(fighter(level, FolkRace.RAT, pit.offset(3, 0, i - 1), 9, 2.0, null, r));
                }
            }
            case 1 -> {
                book.sideA.add(fighter(level, FolkRace.RAT, pit.offset(-3, 0, 0), 14, 2.0, book.nameA, r));
                book.sideB.add(fighter(level, FolkRace.RAT, pit.offset(3, 0, 0), 14, 2.0, book.nameB, r));
            }
            default -> {
                book.sideA.add(fighter(level, FolkRace.CAT, pit.offset(-3, 0, 0), 24, 3.5, book.nameA, r));
                book.sideB.add(fighter(level, FolkRace.CAT, pit.offset(3, 0, 0), 24, 3.5, book.nameB, r));
            }
        }
        book.sideA.removeIf(u -> u == null);
        book.sideB.removeIf(u -> u == null);
        book.state = 2;
        book.until = level.getGameTime() + MAX_BOUT_TICKS;
        level.playSound(null, pit, SoundEvents.RAID_HORN.value(), SoundSource.NEUTRAL, 0.6F, 1.6F);
        announce(level, pit, Component.translatable("pit.childrenofmu.fight", bookie.getDisplayName()));
    }

    @Nullable
    private static UUID fighter(ServerLevel level, FolkRace race, BlockPos at, double health, double damage,
                                @Nullable Component name, RandomSource r) {
        EntityType<FolkEntity> type = (race == FolkRace.CAT ? ModEntities.CAT_FOLK : ModEntities.RAT_FOLK).get();
        FolkEntity f = type.create(level);
        if (f == null) {
            return null;
        }
        String[] looks = race == FolkRace.CAT ? new String[]{"rogue", "adventurer", "hunter"}
                : new String[]{"runner", "scavenger", "gong"};
        f.presetVariant(looks[r.nextInt(looks.length)]);
        f.pitFighter = true;
        f.pitSeen = level.getGameTime();
        f.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360.0F, 0.0F);
        f.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        f.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);   // bare paws
        f.setCustomName(name != null ? name : FolkNames.fighterName(race, r));
        f.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health * (0.8 + r.nextDouble() * 0.4));
        f.setHealth(f.getMaxHealth());
        f.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage * (0.8 + r.nextDouble() * 0.4));
        f.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(race.speed() * 1.15);
        f.setPersistenceRequired();
        f.restrictTo(BlockPos.containing(at.getX(), at.getY(), at.getZ()), 6);
        level.addFreshEntity(f);
        return f.getUUID();
    }

    @Nullable
    private static FolkEntity active(ServerLevel level, UUID id) {
        return level.getEntity(id) instanceof FolkEntity f && f.isAlive() && !f.yielded ? f : null;
    }

    private static void touch(ServerLevel level, Book book, long now) {
        for (UUID id : concat(book)) {
            if (level.getEntity(id) instanceof FolkEntity f) {
                f.pitSeen = now;
            }
        }
    }

    private static void fight(ServerLevel level, FolkEntity bookie, Book book, long now) {
        touch(level, book, now);
        List<FolkEntity> a = new ArrayList<>(), b = new ArrayList<>();
        book.sideA.forEach(id -> { FolkEntity f = active(level, id); if (f != null) a.add(f); });
        book.sideB.forEach(id -> { FolkEntity f = active(level, id); if (f != null) b.add(f); });
        if (a.isEmpty() || b.isEmpty() || now >= book.until) {
            int winner = !a.isEmpty() && b.isEmpty() ? 0 : a.isEmpty() && !b.isEmpty() ? 1 : -1;
            settle(level, bookie, book, winner);
            book.state = 3;
            book.until = now + 100;
            return;
        }
        pair(level, a, b, book.pit);
        pair(level, b, a, book.pit);
        if (bookie.getRandom().nextInt(6) == 0) {
            // The crowd goes wild.
            level.playSound(null, book.pit, bookie.getRandom().nextBoolean() ? SoundEvents.BAT_AMBIENT : SoundEvents.CAT_AMBIENT,
                    SoundSource.NEUTRAL, 1.0F, 0.8F + bookie.getRandom().nextFloat() * 0.6F);
        }
    }

    /** Every fighter goes for the nearest opponent still standing; nobody leaves the pit. */
    private static void pair(ServerLevel level, List<FolkEntity> us, List<FolkEntity> them, BlockPos pit) {
        for (FolkEntity f : us) {
            if (f.blockPosition().distSqr(pit) > 9 * 9) {
                f.teleportTo(pit.getX() + 0.5, pit.getY(), pit.getZ() + 0.5);
            }
            if (f.getTarget() instanceof FolkEntity t && them.contains(t)) {
                continue;
            }
            FolkEntity best = null;
            for (FolkEntity t : them) {
                if (best == null || f.distanceToSqr(t) < f.distanceToSqr(best)) {
                    best = t;
                }
            }
            f.setTarget(best);
        }
    }

    /** A fighter has had enough. */
    static void yielded(FolkEntity f) {
        if (f.level() instanceof ServerLevel level) {
            announce(level, f.blockPosition(), Component.translatable("pit.childrenofmu.yield", f.getDisplayName()));
        }
    }

    private static void settle(ServerLevel level, FolkEntity bookie, Book book, int winner) {
        if (winner < 0) {
            announce(level, book.pit, Component.translatable("pit.childrenofmu.draw", bookie.getDisplayName()));
        } else {
            announce(level, book.pit, Component.translatable("pit.childrenofmu.winner", bookie.getDisplayName(),
                    winner == 0 ? book.nameA : book.nameB));
            level.playSound(null, book.pit, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8F, 1.2F);
        }
        for (Bet bet : book.bets) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(bet.player());
            if (p == null) {
                continue;   // not here to collect: the house thanks you
            }
            int pay = winner < 0 ? bet.amount() : bet.side() == winner ? bet.amount() * 2 : 0;
            if (pay > 0) {
                ItemStack winnings = new ItemStack(Items.EMERALD, pay);
                if (!p.getInventory().add(winnings)) {
                    p.drop(winnings, false);
                }
                p.sendSystemMessage(Component.translatable(winner < 0 ? "pit.childrenofmu.refund" : "pit.childrenofmu.won",
                        bookie.getDisplayName(), pay));
            } else {
                p.sendSystemMessage(Component.translatable("pit.childrenofmu.lost", bookie.getDisplayName()));
            }
        }
        book.bets.clear();
    }

    /** A fighter left over from a bout nobody is running any more (after a reload, say) goes home. */
    static void tickFighter(FolkEntity f) {
        if (f.level() instanceof ServerLevel level && f.tickCount % 20 == 0 && level.getGameTime() - f.pitSeen > 400) {
            FolkLaw.spawnSmoke(level, f);
            f.discard();
        }
    }
}
