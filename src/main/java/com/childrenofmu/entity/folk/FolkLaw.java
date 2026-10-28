package com.childrenofmu.entity.folk;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.childrenofmu.ChildrenOfMu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * The law in RCB City. The Cat cops (and the city guards) do the job iron golems do in a village: they see off
 * monsters. But they also keep order, after a fashion.
 *
 * Crimes: breaking the city's blocks, stealing (taking things out of the city's barrels and sacks), assault and
 * murder. Only cops within {@link #VICINITY} blocks of a crime take any notice. They don't arrest anyone and there
 * are no dungeons: they surround you and circle, naming a fine. Throw them enough emeralds or raw fish (an emerald
 * counts for two fish) and they leave you alone. Don't pay before they lose patience, or hit one of them, and they
 * simply kill you.
 */
public final class FolkLaw {
    public static final ResourceKey<Structure> RCB_CITY = ResourceKey.create(Registries.STRUCTURE, ChildrenOfMu.id("rcb_city"));
    /** How near a crime a cop must be to notice it. */
    static final double VICINITY = 24.0;
    /** How long the cops circle (in ticks) before they lose patience. */
    private static final int PATIENCE = 300;
    private static final int PATIENCE_MURDER = 200;
    /** Cops give up a chase this far from home (you'll be picked up again if you come back owing). */
    private static final double LEASH = 64.0;
    private static final double CIRCLE_RADIUS = 3.5;

    enum Crime {
        VANDALISM(4), THEFT(6), ASSAULT(12), MURDER(40), RESISTING(10);

        /** The fine, in fish (an emerald is worth two). */
        final int fine;

        Crime(int fine) {
            this.fine = fine;
        }

        String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** What a player owes the law, and how patient the cops still are. */
    static final class Warrant {
        int owed;
        long deadline;
        boolean hostile;
        boolean resisting;
        long lastAssault = -1000;
        long lastSpoke = -1000;
        boolean toldTimeUp;
    }

    private static final Map<UUID, Warrant> WARRANTS = new HashMap<>();
    /** A container a player opened in the city, and how many items were in it then. */
    private record Opened(BlockPos pos, int count) {}
    private static final Map<UUID, Opened> OPEN = new HashMap<>();

    private FolkLaw() {}

    // ---- Where the law runs ------------------------------------------------------------------

    @Nullable
    private static Structure city(ServerLevel level) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(RCB_CITY);
    }

    /** Anywhere within an RCB City's bounds. */
    public static boolean inCity(ServerLevel level, BlockPos pos) {
        Structure s = city(level);
        return s != null && level.isLoaded(pos) && level.structureManager().getStructureAt(pos, s).isValid();
    }

    /** Inside one of the city's buildings or streets (its blocks belong to the city). */
    static boolean cityBlock(ServerLevel level, BlockPos pos) {
        Structure s = city(level);
        return s != null && level.structureManager().getStructureWithPieceAt(pos, s).isValid();
    }

    /**
     * Inside the town wall itself (not merely within the city's bounds, which take in the land round its corners, where
     * other people's camps may stand). A city without a wall: anywhere within its bounds.
     */
    public static boolean insideWalls(ServerLevel level, BlockPos pos) {
        Structure s = city(level);
        if (s == null || !level.isLoaded(pos)) {
            return false;
        }
        net.minecraft.world.level.levelgen.structure.StructureStart start = level.structureManager().getStructureAt(pos, s);
        if (!start.isValid()) {
            return false;
        }
        for (net.minecraft.world.level.levelgen.structure.StructurePiece p : start.getPieces()) {
            if (p instanceof com.childrenofmu.worldgen.CityTerrainPiece t && t.ring().length >= 6) {
                return com.childrenofmu.worldgen.CityRing.inside(com.childrenofmu.worldgen.CityRing.edges(t.ring()),
                        pos.getX() + 0.5, pos.getZ() + 0.5);
            }
        }
        return true;
    }

    static boolean isCityFolk(FolkEntity e) {
        return e.getRace() == FolkRace.RAT || e.getRace() == FolkRace.CAT || e.getRace() == FolkRace.BAT;
    }

    @Nullable
    static Warrant warrant(Player player) {
        return WARRANTS.get(player.getUUID());
    }

    // ---- Crimes ------------------------------------------------------------------------------

    /** A crime was committed: the cops nearby (if any) step in. */
    static void report(ServerLevel level, ServerPlayer player, BlockPos where, Crime crime, int extraFine) {
        if (player.isSpectator()) {
            return;
        }
        List<FolkEntity> cops = level.getEntitiesOfClass(FolkEntity.class, new AABB(where).inflate(VICINITY),
                f -> f.isCop() && f.isAlive() && !f.pitFighter);
        if (cops.isEmpty()) {
            return;   // nobody saw a thing
        }
        long now = level.getGameTime();
        Warrant w = WARRANTS.computeIfAbsent(player.getUUID(), u -> new Warrant());
        if (crime == Crime.ASSAULT || crime == Crime.RESISTING) {
            if (now - w.lastAssault < 60) {
                return;   // one flurry of blows is one assault
            }
            w.lastAssault = now;
        }
        w.owed += crime.fine + extraFine;
        if (crime == Crime.RESISTING) {
            w.hostile = true;
            w.resisting = true;
        } else if (!w.hostile) {
            w.deadline = now + (crime == Crime.MURDER ? PATIENCE_MURDER : PATIENCE);
            w.toldTimeUp = false;
        }
        FolkEntity speaker = null;
        for (FolkEntity cop : cops) {
            cop.wanted = player.getUUID();
            if (w.hostile) {
                cop.setTarget(player);
            }
            if (speaker == null || cop.distanceToSqr(player) < speaker.distanceToSqr(player)) {
                speaker = cop;
            }
        }
        if (speaker != null && now - w.lastSpoke >= 40) {
            w.lastSpoke = now;
            speaker.playSound(SoundEvents.CAT_HISS, 1.0F, 1.0F);
            if (w.hostile) {
                say(player, speaker, "law.childrenofmu.resist");
            } else {
                say(player, speaker, "law.childrenofmu.demand." + crime.key(), emeralds(w.owed), w.owed);
            }
        }
    }

    /**
     * An uprising: every cop and guard within range turns on this player at once, with no chance to pay (striking the
     * Great Gar, or coming back after killing him).
     */
    static void uprising(ServerLevel level, ServerPlayer player, BlockPos where, double range) {
        if (player.isSpectator() || player.isCreative()) {
            return;
        }
        Warrant w = WARRANTS.computeIfAbsent(player.getUUID(), u -> new Warrant());
        boolean fresh = !w.resisting;
        w.hostile = true;
        w.resisting = true;
        w.owed = Math.max(w.owed, 999);
        FolkEntity speaker = null;
        for (FolkEntity cop : level.getEntitiesOfClass(FolkEntity.class, new AABB(where).inflate(range),
                f -> f.isCop() && f.isAlive() && !f.pitFighter)) {
            cop.wanted = player.getUUID();
            cop.setTarget(player);
            if (speaker == null || cop.distanceToSqr(player) < speaker.distanceToSqr(player)) {
                speaker = cop;
            }
        }
        if (fresh && speaker != null) {
            speaker.playSound(SoundEvents.CAT_HISS, 1.0F, 0.8F);
            say(player, speaker, "law.childrenofmu.treason");
        }
    }

    private static int emeralds(int fish) {
        return (fish + 1) / 2;
    }

    private static void say(Player player, FolkEntity cop, String key, Object... args) {
        Object[] all = new Object[args.length + 1];
        all[0] = cop.getDisplayName();
        System.arraycopy(args, 0, all, 1, args.length);
        player.sendSystemMessage(Component.translatable(key, all));
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getState();
        if (state.getDestroySpeed(level, event.getPos()) == 0.0F || !cityBlock(level, event.getPos())
                || !insideWalls(level, event.getPos())) {
            return;   // flowers and grass don't count, nor anything outside the walls
        }
        report(level, player, event.getPos(), Crime.VANDALISM, 0);
    }

    public static void onOpen(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(event.getPos());
        if (be instanceof Container container && inCity(level, event.getPos()) && insideWalls(level, event.getPos())) {
            OPEN.put(player.getUUID(), new Opened(event.getPos().immutable(), count(container)));
        }
    }

    public static void onClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Opened o = OPEN.remove(player.getUUID());
        if (o == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.getBlockEntity(o.pos()) instanceof Container container) {
            int taken = o.count() - count(container);
            if (taken > 0) {
                report(level, player, o.pos(), Crime.THEFT, Math.min(26, taken * 2));
            }
        }
    }

    private static int count(Container c) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            n += c.getItem(i).getCount();
        }
        return n;
    }

    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof FolkEntity victim) || !(victim.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof ServerPlayer player) || !isCityFolk(victim)
                || !inCity(level, victim.blockPosition())) {
            return;
        }
        if (FolkGar.isGar(victim)) {
            FolkGar.attacked(level, player, victim);
            return;
        }
        report(level, player, victim.blockPosition(), victim.isCop() ? Crime.RESISTING : Crime.ASSAULT, 0);
    }

    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (WARRANTS.remove(player.getUUID()) != null) {
                standDown(player.serverLevel(), player.getUUID());   // case closed
            }
            return;
        }
        if (event.getEntity() instanceof FolkEntity dead && dead.level() instanceof ServerLevel dl) {
            FolkCensus.died(dl, dead);
        }
        if (event.getEntity() instanceof FolkEntity gar && FolkGar.isGar(gar) && gar.level() instanceof ServerLevel gl) {
            FolkGar.died(gl, gar, event.getSource().getEntity() instanceof ServerPlayer k ? k : null);
        }
        if (event.getEntity() instanceof FolkEntity rat && rat.getRace() == FolkRace.RAT
                && event.getSource().getEntity() instanceof FolkEntity cat && cat.getRace() == FolkRace.CAT
                && rat.level() instanceof ServerLevel rl) {
            // Caught. Eaten. Nothing left but a tail.
            cat.playSound(SoundEvents.CAT_EAT, 1.0F, 0.9F);
            cat.playSound(SoundEvents.GENERIC_EAT, 0.8F, 0.8F);
            rl.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ROTTEN_FLESH)),
                    rat.getX(), rat.getY() + 0.3, rat.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
            cat.setTarget(null);
        }
        if (event.getEntity() instanceof FolkEntity victim && victim.level() instanceof ServerLevel level
                && event.getSource().getEntity() instanceof ServerPlayer player && isCityFolk(victim)
                && inCity(level, victim.blockPosition())) {
            report(level, player, victim.blockPosition(), Crime.MURDER, 0);
            if (victim.isCop()) {
                report(level, player, victim.blockPosition(), Crime.RESISTING, 0);
            }
        }
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        WARRANTS.clear();
        OPEN.clear();
    }

    // ---- Paying up ---------------------------------------------------------------------------

    /** What an item is worth to a cop, in fish. */
    static int value(ItemStack stack) {
        if (stack.is(Items.EMERALD)) {
            return 2;
        }
        if (stack.is(Items.EMERALD_BLOCK)) {
            return 18;
        }
        if (stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH)) {
            return 1;
        }
        return 0;
    }

    /** Every few ticks: a cop snaps up any emeralds or raw fish thrown at its feet. */
    private static void collect(ServerLevel level, FolkEntity cop) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, cop.getBoundingBox().inflate(1.5),
                i -> i.isAlive() && value(i.getItem()) > 0)) {
            int worth = value(item.getItem()) * item.getItem().getCount();
            Player payer = item.getOwner() instanceof Player p && warrant(p) != null ? p : null;
            if (payer == null) {
                payer = level.getNearestPlayer(cop.getX(), cop.getY(), cop.getZ(), 10.0, p -> warrant((Player) p) != null);
            }
            item.discard();
            cop.playSound(SoundEvents.CAT_EAT, 0.8F, 1.0F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cop.getX(), cop.getY() + cop.getBbHeight(), cop.getZ(),
                    3, 0.2, 0.2, 0.2, 0.0);
            if (payer == null) {
                continue;   // nobody owes anything: the cat keeps the fish anyway, thank you very much
            }
            Warrant w = warrant(payer);
            if (w.resisting) {
                say(payer, cop, "law.childrenofmu.too_late");
                continue;
            }
            w.owed -= worth;
            if (w.owed <= 0) {
                WARRANTS.remove(payer.getUUID());
                say(payer, cop, "law.childrenofmu.paid");
                standDown(level, payer.getUUID());
            } else {
                say(payer, cop, "law.childrenofmu.more", emeralds(w.owed), w.owed);
            }
        }
    }

    /** Every cop after this player goes back to its beat. */
    static void standDown(ServerLevel level, UUID player) {
        for (Entity e : level.getAllEntities()) {
            if (e instanceof FolkEntity cop && player.equals(cop.wanted)) {
                cop.wanted = null;
                cop.setTarget(null);
                cop.setAggressive(false);
                cop.getNavigation().stop();
            }
        }
    }

    // ---- The cops at work --------------------------------------------------------------------

    /** Every tick, on a cop. */
    static void tickCop(FolkEntity cop) {
        if (!(cop.level() instanceof ServerLevel level)) {
            return;
        }
        FolkGar.tickCity(cop);
        if (cop.tickCount % 5 == 0) {
            collect(level, cop);
        }
        long now = level.getGameTime();
        if (cop.wanted != null) {
            Player player = level.getPlayerByUUID(cop.wanted);
            Warrant w = player == null ? null : warrant(player);
            if (player == null || w == null || !player.isAlive()) {
                cop.wanted = null;
                cop.setTarget(null);
                return;
            }
            BlockPos home = cop.homePos();
            if ((home != null && player.blockPosition().distSqr(home) > LEASH * LEASH) || cop.distanceToSqr(player) > 48 * 48) {
                // Out of our patch. We'll see you again.
                cop.wanted = null;
                cop.setTarget(null);
                cop.restoreHome();
                return;
            }
            if (!w.hostile && now >= w.deadline) {
                w.hostile = true;
                if (!w.toldTimeUp) {
                    w.toldTimeUp = true;
                    say(player, cop, "law.childrenofmu.time_up");
                }
            }
            if (w.hostile && cop.getTarget() != player && !player.isCreative()) {
                cop.setTarget(player);
            }
        } else if (cop.tickCount % 40 == 0) {
            // Anyone with an open warrant who wanders past gets picked up again.
            Player p = level.getNearestPlayer(cop.getX(), cop.getY(), cop.getZ(), 16.0,
                    e -> warrant((Player) e) != null && !((Player) e).isSpectator());
            if (p != null) {
                cop.wanted = p.getUUID();
                Warrant w = warrant(p);
                if (!w.hostile && w.deadline < now + 100) {
                    w.deadline = now + 200;   // "You again. Pay up."
                    if (now - w.lastSpoke >= 40) {
                        w.lastSpoke = now;
                        say(p, cop, "law.childrenofmu.again", emeralds(w.owed), w.owed);
                    }
                }
            }
        }
    }

    /** A player right-clicked a cop. */
    static void talk(FolkEntity cop, Player player) {
        Warrant w = warrant(player);
        if (w != null && !w.resisting) {
            say(player, cop, "law.childrenofmu.owe", emeralds(w.owed), w.owed);
        } else if (w != null) {
            say(player, cop, "law.childrenofmu.too_late");
        } else {
            say(player, cop, "law.childrenofmu.chat." + cop.getRandom().nextInt(8));
        }
    }

    static void addGoals(FolkEntity e, GoalSelector goals, GoalSelector targets) {
        goals.addGoal(1, new CircleGoal(e));
        goals.addGoal(2, new MeleeAttackGoal(e, 1.25, true) {
            @Override
            public boolean canUse() {
                return e.isCop() && !FolkCombat.isArcher(e) && super.canUse();
            }
        });
        // Illagers are attacked on sight, wherever they turn up.
        targets.addGoal(2, new NearestAttackableTargetGoal<>(e, net.minecraft.world.entity.monster.AbstractIllager.class,
                10, true, false, t -> true) {
            @Override
            public boolean canUse() {
                return e.isCop() && e.wanted == null && super.canUse();
            }
        });
        // Golem duty: monsters in the city get short shrift (but nobody wants to tangle with a creeper).
        targets.addGoal(3, new NearestAttackableTargetGoal<>(e, Mob.class, 10, true, false,
                t -> t instanceof Enemy && !(t instanceof Creeper)) {
            @Override
            public boolean canUse() {
                return e.isCop() && e.wanted == null && super.canUse();
            }
        });
    }

    /**
     * Surround the suspect and circle, waiting to be paid. A cop who sees money or fish land nearby goes and gets it.
     */
    static class CircleGoal extends Goal {
        private final FolkEntity cop;
        private double angle;
        private int repath;

        CircleGoal(FolkEntity cop) {
            this.cop = cop;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Nullable
        private Player suspect() {
            if (this.cop.wanted == null) {
                return null;
            }
            Player p = this.cop.level().getPlayerByUUID(this.cop.wanted);
            Warrant w = p == null ? null : warrant(p);
            return w == null || w.hostile ? null : p;
        }

        @Override
        public boolean canUse() {
            return this.cop.isCop() && suspect() != null;
        }

        @Override
        public void start() {
            Player p = suspect();
            if (p != null) {
                this.angle = Math.atan2(this.cop.getZ() - p.getZ(), this.cop.getX() - p.getX());
            }
            this.repath = 0;
        }

        @Override
        public void stop() {
            this.cop.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player p = suspect();
            if (p == null) {
                return;
            }
            this.cop.getLookControl().setLookAt(p, 30.0F, 30.0F);
            if (--this.repath > 0) {
                return;
            }
            this.repath = 8;
            // Money on the ground? Go and get it.
            List<ItemEntity> loot = this.cop.level().getEntitiesOfClass(ItemEntity.class,
                    this.cop.getBoundingBox().inflate(6.0), i -> value(i.getItem()) > 0);
            if (!loot.isEmpty()) {
                ItemEntity item = loot.get(0);
                this.cop.getNavigation().moveTo(item, 1.2);
                return;
            }
            this.angle += 0.55;   // keep walking round
            Vec3 at = p.position().add(Math.cos(this.angle) * CIRCLE_RADIUS, 0, Math.sin(this.angle) * CIRCLE_RADIUS);
            this.cop.getNavigation().moveTo(at.x, at.y, at.z, 1.0);
        }
    }

    static void spawnSmoke(ServerLevel level, Entity e) {
        level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.4, e.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, e.blockPosition(), SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.5F, 1.2F);
    }
}
