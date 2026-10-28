package com.childrenofmu.entity.folk;

import javax.annotation.Nullable;

import com.childrenofmu.item.WritItem;
import com.childrenofmu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * The Great Gar: the vast, immovable Cat who is master of an RCB City, enthroned at the top of its Justice Hall. Nobody
 * trades in the city, and no bookie takes a bet, without the Gar's Writ of Trade: a few emeralds buys one, good for
 * nine days. Each city has its own name (worked out from where it stands), and its own Gar.
 */
public final class FolkGar {
    /** What a writ costs, in emeralds. */
    public static final int FEE = 8;
    /** How long a writ is good for: nine days. */
    public static final long TERM = 9L * 24000L;

    private static final String[] FIRST = {"Mog", "Rat", "Purr", "Whisk", "Gutter", "Tom", "Moll", "Scrat", "Grim",
            "Flea", "Old ", "Crook", "Tabby", "Brindle", "Saint ", "Low ", "Mouse", "Bat", "Soot", "Cheap"};
    private static final String[] LAST = {"gate", "wick", "holm", "sprawl", "mire", "bury", "wark", "ditch", "ford",
            "scratch", "moor", "ham", "chester", "stead", "pike", "warren", "kettle", "rookery", "Yowl", "Lantern"};

    static final String PLAIN_TITLE = "The Great Gar";
    /** How long a dead Gar's city holds a grudge, and how long until a new Gar takes the throne: three days. */
    static final long MOURNING = 3L * 24000L;
    /** How far from his cushion the Gar will waddle when he's frightened. */
    static final int ROOM = 6;
    /** How slowly. */
    static final double FLEE_SPEED = 0.07;

    private FolkGar() {}

    /** Now and then, until it knows: a Gar takes the name of its city. */
    static void tickName(FolkEntity gar) {
        if (gar.tickCount % 40 != 7 || !(gar.level() instanceof ServerLevel level)
                || (gar.hasCustomName() && !PLAIN_TITLE.equals(gar.getCustomName().getString()))) {
            return;
        }
        StructureStart city = city(level, gar.blockPosition());
        if (city != null) {
            gar.setCustomName(Component.literal("The Great Gar of " + name(key(city))));
        }
    }

    public static boolean isGar(FolkEntity e) {
        return e.getRace() == FolkRace.CAT && "gar".equals(e.getVariantName());
    }

    /** The RCB City this position is in, if any. */
    @Nullable
    public static StructureStart city(ServerLevel level, BlockPos pos) {
        Structure s = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(FolkLaw.RCB_CITY);
        if (s == null || !level.isLoaded(pos)) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureAt(pos, s);
        return start.isValid() ? start : null;
    }

    public static long key(StructureStart start) {
        return start.getChunkPos().toLong();
    }

    /** The city's name: the same every time for the same city. */
    public static String name(long key) {
        RandomSource r = RandomSource.create(key * 31L + 7L);
        String a = FIRST[r.nextInt(FIRST.length)];
        String b = LAST[r.nextInt(LAST.length)];
        return Character.isUpperCase(b.charAt(0)) || a.endsWith(" ") ? (a.endsWith(" ") ? a + capital(b) : a + " " + b) : a + b;
    }

    private static String capital(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** The writ for this city in the player's inventory that runs out last, if any is still good. */
    @Nullable
    private static ItemStack writ(Player player, long city, long now) {
        int epoch = player.level() instanceof ServerLevel sl ? epoch(sl, city) : 0;
        ItemStack best = null;
        long bestUntil = now;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.is(ModItems.WRIT_OF_TRADE.get())) {
                continue;
            }
            CompoundTag t = WritItem.data(s);
            if (t.getLong("City") == city && t.getLong("Expires") > bestUntil && t.getInt("Epoch") == epoch) {
                best = s;
                bestUntil = t.getLong("Expires");
            }
        }
        return best;
    }

    /**
     * May this player trade (or bet) with city folk here? Outside a city, always. Inside, only with the city's writ;
     * if not, the folk say so and point them to the Gar.
     */
    public static boolean mayTrade(FolkEntity folk, Player player) {
        if (!(folk.level() instanceof ServerLevel level) || player.isCreative()) {
            return true;
        }
        StructureStart city = city(level, folk.blockPosition());
        if (city == null) {
            return true;
        }
        long key = key(city);
        if (writ(player, key, level.getGameTime()) != null) {
            return true;
        }
        FolkGarData.City c = FolkGarData.get(level).peek(key);
        if (c != null && c.deadSince >= 0) {
            player.sendSystemMessage(Component.translatable("gar.childrenofmu.mourning", folk.getDisplayName(), name(key)));
            return false;
        }
        player.sendSystemMessage(Component.translatable("gar.childrenofmu.no_writ." + folk.getRandom().nextInt(3),
                folk.getDisplayName(), name(key)));
        return false;
    }

    /** Someone approaches the throne. */
    static void audience(FolkEntity gar, Player player) {
        if (!(gar.level() instanceof ServerLevel level)) {
            return;
        }
        StructureStart city = city(level, gar.blockPosition());
        long key = city != null ? key(city) : gar.blockPosition().asLong();
        String name = name(key);
        long now = level.getGameTime();
        ItemStack held = writ(player, key, now);
        if (held != null) {
            long days = (WritItem.data(held).getLong("Expires") - now + 23999L) / 24000L;
            player.sendSystemMessage(Component.translatable("gar.childrenofmu.still_good", gar.getDisplayName(), name, days));
            return;
        }
        if (player.getInventory().countItem(Items.EMERALD) < FEE) {
            player.sendSystemMessage(Component.translatable("gar.childrenofmu.demand", gar.getDisplayName(), name, FEE));
            gar.playSound(SoundEvents.CAT_HISS, 0.8F, 0.6F);
            return;
        }
        player.getInventory().clearOrCountMatchingItems(s -> s.is(Items.EMERALD), FEE, player.inventoryMenu.getCraftSlots());
        ItemStack writ = WritItem.make(ModItems.WRIT_OF_TRADE.get(), key, name, now + TERM, epoch(level, key));
        if (!player.getInventory().add(writ)) {
            player.drop(writ, false);
        }
        player.sendSystemMessage(Component.translatable("gar.childrenofmu.granted", gar.getDisplayName(), name));
        gar.playSound(SoundEvents.CAT_PURREOW, 1.0F, 0.5F);
        gar.playSound(SoundEvents.BOOK_PUT, 1.0F, 1.0F);
    }

    static int epoch(ServerLevel level, long city) {
        FolkGarData.City c = FolkGarData.get(level).peek(city);
        return c == null ? 0 : c.epoch;
    }

    // ---- On the cushion, and off it -----------------------------------------------------------

    /** Every tick, on the Gar: sit tight on the cushion; if attacked, waddle away (never out of the room). */
    static void tick(FolkEntity gar) {
        if (!(gar.level() instanceof ServerLevel level)) {
            return;
        }
        if (gar.homePos() == null) {
            gar.setHome(gar.blockPosition(), ROOM);
        }
        boolean alarmed = gar.getLastHurtByMob() != null && gar.tickCount - gar.getLastHurtByMobTimestamp() < 600;
        BlockPos throne = gar.homePos();
        if (alarmed) {
            gar.setResting(false);
            setSpeed(gar, FLEE_SPEED);
            if (gar.tickCount % 20 == 0) {
                net.minecraft.world.entity.LivingEntity foe = gar.getLastHurtByMob();
                net.minecraft.world.phys.Vec3 away = gar.position().subtract(foe.position()).normalize().scale(4.0);
                BlockPos to = BlockPos.containing(gar.position().add(away));
                if (to.distSqr(throne) > ROOM * ROOM) {
                    to = throne.offset(gar.getRandom().nextInt(ROOM * 2 + 1) - ROOM, 0, gar.getRandom().nextInt(ROOM * 2 + 1) - ROOM);
                }
                gar.getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 1.0);
                if (gar.getRandom().nextInt(3) == 0) {
                    gar.playSound(SoundEvents.CAT_HISS, 1.0F, 0.5F);
                }
            }
            return;
        }
        double dx = gar.getX() - (throne.getX() + 0.5), dz = gar.getZ() - (throne.getZ() + 0.5);
        if (dx * dx + dz * dz > 0.6) {
            gar.setResting(false);
            setSpeed(gar, FLEE_SPEED);
            if (gar.getNavigation().isDone()) {
                gar.getNavigation().moveTo(throne.getX() + 0.5, throne.getY(), throne.getZ() + 0.5, 1.0);
            }
        } else if (!gar.isResting()) {
            // Settle back onto the cushion, facing the room.
            gar.getNavigation().stop();
            setSpeed(gar, 0.0);
            gar.moveTo(throne.getX() + 0.5, gar.getY(), throne.getZ() + 0.5, gar.getYRot(), 0.0F);
            gar.setResting(true);
        }
    }

    private static void setSpeed(FolkEntity gar, double speed) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance a =
                gar.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (a != null && a.getBaseValue() != speed) {
            a.setBaseValue(speed);
        }
    }

    /** Someone has struck the Great Gar. Every cop and guard in the city comes for them, and there's no paying off. */
    static void attacked(ServerLevel level, net.minecraft.server.level.ServerPlayer player, FolkEntity gar) {
        FolkLaw.uprising(level, player, gar.blockPosition(), 64.0);
    }

    /** The Gar is dead. The city mourns (and remembers who did it); his writs are void; a new Gar comes in time. */
    static void died(ServerLevel level, FolkEntity gar, @Nullable net.minecraft.server.level.ServerPlayer killer) {
        StructureStart city = city(level, gar.blockPosition());
        if (city == null) {
            return;
        }
        long key = key(city);
        FolkGarData data = FolkGarData.get(level);
        FolkGarData.City c = data.city(key);
        long now = level.getGameTime();
        c.deadSince = now;
        c.throne = gar.homePos() != null ? gar.homePos() : gar.blockPosition();
        c.epoch++;
        if (killer != null) {
            c.enemies.put(killer.getUUID(), now + MOURNING);
        }
        data.setDirty();
        Component msg = Component.translatable("gar.childrenofmu.dead", name(key));
        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            if (p.distanceToSqr(gar) < 160 * 160) {
                p.sendSystemMessage(msg);
            }
        }
        level.playSound(null, gar.blockPosition(), SoundEvents.BELL_BLOCK, net.minecraft.sounds.SoundSource.NEUTRAL, 3.0F, 0.5F);
    }

    /**
     * Now and then, on a cop: if this city's Gar has been dead long enough, a new one is enthroned; and anyone the city
     * holds a grudge against is set upon.
     */
    static void tickCity(FolkEntity cop) {
        if (cop.tickCount % 100 != 13 || !(cop.level() instanceof ServerLevel level)) {
            return;
        }
        StructureStart city = city(level, cop.blockPosition());
        if (city == null) {
            return;
        }
        long key = key(city);
        FolkGarData data = FolkGarData.get(level);
        FolkGarData.City c = data.peek(key);
        if (c == null) {
            return;
        }
        long now = level.getGameTime();
        c.enemies.values().removeIf(until -> until < now);
        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            if (c.enemies.containsKey(p.getUUID()) && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(cop) < 24 * 24) {
                FolkLaw.uprising(level, p, cop.blockPosition(), 32.0);
            }
        }
        if (c.deadSince >= 0 && now - c.deadSince >= MOURNING && c.throne != null && level.isLoaded(c.throne)) {
            FolkEntity gar = com.childrenofmu.registry.ModEntities.CAT_FOLK.get().create(level);
            if (gar != null) {
                gar.presetVariant("gar");
                gar.moveTo(c.throne.getX() + 0.5, c.throne.getY(), c.throne.getZ() + 0.5, 180.0F, 0.0F);
                gar.finalizeSpawn(level, level.getCurrentDifficultyAt(c.throne),
                        net.minecraft.world.entity.MobSpawnType.STRUCTURE, null);
                level.addFreshEntity(gar);
                c.deadSince = -1;
                data.setDirty();
                Component msg = Component.translatable("gar.childrenofmu.new", name(key));
                for (net.minecraft.server.level.ServerPlayer p : level.players()) {
                    if (p.distanceToSqr(cop) < 160 * 160) {
                        p.sendSystemMessage(msg);
                    }
                }
            }
        }
        data.setDirty();
    }

    /** The Gar's full title. */
    public static Component title(long key) {
        return Component.translatable("gar.childrenofmu.title", name(key));
    }
}
