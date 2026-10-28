package com.childrenofmu.entity.ithk;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import com.childrenofmu.ChildrenOfMu;

/**
 * The Ithk: floating, telepathic eyeball folk. They never fight.
 * Struck once, they phase into an ethereal state. Struck again soon after
 * by the same aggressor, they cast that aggressor out beyond their village.
 * Each has a profession and trades (expensively) like a villager.
 */
public class IthkEntity extends PathfinderMob implements Merchant {

    // ---- Tuning ----------------------------------------------------------
    /** How long the ethereal phase lasts, in ticks (20 ticks = 1 second). */
    public static final int ETHEREAL_DURATION = 100;
    /** If the same aggressor strikes again within this window after a phase, it gets teleported away. */
    private static final int REPEAT_OFFENSE_WINDOW = 600;
    /** Radius used when an Ithk lives outside any village (spawned by egg or command): cast out this far from home. */
    private static final int VILLAGE_RADIUS = 24;
    private static final ResourceKey<Structure> VILLAGE =
            ResourceKey.create(Registries.STRUCTURE, ChildrenOfMu.id("ithk_village"));
    /** Minimum time between right-click replies to the same player (20 seconds). */
    private static final int INTERACT_SPEECH_COOLDOWN = 400;
    /** How long an Ithk holds still and watches you after you right-click it (8 seconds). */
    private static final int ATTEND_DURATION = 160;
    /** Trades restock once per Minecraft day. */
    private static final int RESTOCK_INTERVAL = 24000;
    /** XP needed to reach each level (same as villagers, so the trading screen's progress bar matches). */
    private static final int[] LEVEL_XP = {0, 10, 70, 150, 250};
    public static final int MAX_LEVEL = 5;
    /** Ithk stay within this many blocks of home: where they first appeared (for tower Ithk, the top room). */
    private static final int HOME_RADIUS = 6;
    /** How long an Ithk lingers in one place before drifting somewhere else in the village (1.5 to 4.5 minutes). */
    private static final int MEANDER_MIN_WAIT = 1800, MEANDER_EXTRA_WAIT = 3600;
    /** Each leg of a journey is at most this long, so paths stay short enough to find. */
    private static final int MEANDER_STEP = 14;
    /** A journey is abandoned if it takes longer than this (3 minutes). */
    private static final int MEANDER_TIMEOUT = 3600;
    /** Bump when the trade tables change, so existing Ithk redraw their trades from the new tables. */
    private static final int TRADES_VERSION = 4;

    private static final EntityDataAccessor<Integer> EYE_COLOR =
            SynchedEntityData.defineId(IthkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ETHEREAL_TICKS =
            SynchedEntityData.defineId(IthkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PROFESSION =
            SynchedEntityData.defineId(IthkEntity.class, EntityDataSerializers.INT);

    // ---- Trading (server-side, saved) -----------------------------------------
    @Nullable private Player tradingPlayer;
    @Nullable private MerchantOffers offers;
    private int merchantXp;
    private int merchantLevel = 1;
    private long lastRestock;

    // ---- Server-only memory (not saved) -----------------------------------
    @Nullable private UUID lastAggressor;
    private long lastPhaseTime = -100000L;
    private final Map<UUID, Long> lastInteractSpeech = new HashMap<>();
    @Nullable private Player attending;
    private int attendTicks;
    /** Where this Ithk lives: it always comes back here between wanderings. */
    @Nullable private BlockPos home;
    @Nullable private BoundingBox villageBounds;
    private boolean villageChecked;
    private long nextMeanderTime;

    // ---- Client-only animation state -------------------------------------
    private int blinkTicks;
    private int nextBlinkIn = 60;
    private boolean wasEthereal;

    public IthkEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.FLYING_SPEED, 0.06)
                .add(Attributes.MOVEMENT_SPEED, 0.06)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(EYE_COLOR, 0);
        builder.define(ETHEREAL_TICKS, 0);
        builder.define(PROFESSION, IthkProfession.NONE.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new AttendPlayerGoal());
        this.goalSelector.addGoal(2, new MeanderGoal());
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    // ---- Identity ----------------------------------------------------------

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
        assignIdentity();
        ensureProfession();
        this.lastRestock = level.getLevel().getGameTime();
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    private void ensureProfession() {
        if (getProfession() == IthkProfession.NONE) {
            setProfession(IthkProfession.random(this.getRandom()));
        }
    }

    public IthkProfession getProfession() {
        return IthkProfession.byOrdinal(this.entityData.get(PROFESSION));
    }

    public void setProfession(IthkProfession profession) {
        this.entityData.set(PROFESSION, profession.ordinal());
    }

    /** Transmuters float higher and carry cones above and below, so their hitbox is much taller. */
    private static final EntityDimensions TRANSMUTER_SIZE = EntityDimensions.scalable(0.9F, 2.6F);

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return getProfession() == IthkProfession.TRANSMUTER ? TRANSMUTER_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PROFESSION.equals(key)) {
            this.refreshDimensions();
        }
    }

    private void assignIdentity() {
        this.entityData.set(EYE_COLOR, this.getRandom().nextInt(IthkEyeColors.COUNT));
        String name = IthkNames.random(this.getRandom());
        this.setCustomName(Component.literal(name).withStyle(ChatFormatting.LIGHT_PURPLE));
        // Name shows when you look at them, not all the time.
        this.setCustomNameVisible(false);
    }

    public int getEyeColorIndex() {
        return Mth.clamp(this.entityData.get(EYE_COLOR), 0, IthkEyeColors.COUNT - 1);
    }

    /** Shown over their heads: "Ouk the Archivist", "Yral the Quiescent". */
    @Override
    public Component getDisplayName() {
        if (getProfession() == IthkProfession.NONE) {
            return super.getDisplayName();
        }
        return Component.translatable("merchant.childrenofmu.ithk_title", getIthkName(),
                Component.translatable("profession.childrenofmu." + getProfession().id()))
                .withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    public String getIthkName() {
        Component name = this.getCustomName();
        return name != null ? name.getString() : "An Ithk";
    }

    // ---- Ticking ------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            clientTick();
        } else {
            serverTick();
        }
    }

    private void serverTick() {
        int ethereal = this.entityData.get(ETHEREAL_TICKS);
        if (ethereal > 0) {
            this.entityData.set(ETHEREAL_TICKS, ethereal - 1);
            if (ethereal == 1) {
                this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.6F);
            }
        }

        if (this.tradingPlayer != null && (!this.tradingPlayer.isAlive() || this.tradingPlayer.distanceTo(this) > 12.0F)) {
            stopTrading();
        }
        if (this.tradingPlayer == null) {
            checkLevelUp();
        }

        if (this.home == null) {
            this.home = this.hasRestriction() ? this.getRestrictCenter() : this.blockPosition();
            this.restrictTo(this.home, HOME_RADIUS);
            this.nextMeanderTime = this.level().getGameTime() + MEANDER_MIN_WAIT / 2 + this.random.nextInt(MEANDER_EXTRA_WAIT);
        }

        if (this.tickCount % 20 == 0) {
            if (!this.hasCustomName()) {
                assignIdentity();
            }
            ensureProfession();

            long now = this.level().getGameTime();
            if (now - this.lastRestock >= RESTOCK_INTERVAL || now < this.lastRestock) {
                restock();
                this.lastRestock = now;
            }
        }
    }

    private void clientTick() {
        boolean ethereal = isEthereal();
        if (ethereal != this.wasEthereal) {
            // Blink as they phase out, and again as they phase back in.
            this.blinkTicks = 6;
            this.wasEthereal = ethereal;
        }
        if (this.blinkTicks > 0) {
            this.blinkTicks--;
        } else if (--this.nextBlinkIn <= 0) {
            this.blinkTicks = 4;
            this.nextBlinkIn = 60 + this.getRandom().nextInt(140);
        }
    }

    public boolean isBlinking() {
        return this.blinkTicks > 0;
    }

    // ---- Telepathy: they only speak when you interact with them directly ----------------

    /** Sends a random telepathic line from the given lang-key group (e.g. telepathy.childrenofmu.greet.0..N-1). */
    private void speak(Player player, String group, int variants) {
        String key = "telepathy.childrenofmu." + group + "." + this.getRandom().nextInt(variants);
        player.displayClientMessage(
                Component.translatable(key, getIthkName()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC),
                false);
    }

    // ---- Right-click ------------------------------------------------------------

    /**
     * Opens the trading screen. The Ithk stops and watches you while you trade, and chimes.
     * It only speaks if it hasn't spoken to you in the last 20 seconds.
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || isEthereal() || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (this.tradingPlayer != null && this.tradingPlayer != player) {
            return InteractionResult.sidedSuccess(this.level().isClientSide); // busy with someone else
        }
        if (!this.level().isClientSide) {
            this.attending = player;
            this.attendTicks = ATTEND_DURATION;
            this.getNavigation().stop();

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 0.7F, 1.2F);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.0F);

            long now = this.level().getGameTime();
            Long last = this.lastInteractSpeech.get(player.getUUID());
            if (last == null || now - last >= INTERACT_SPEECH_COOLDOWN) {
                this.lastInteractSpeech.put(player.getUUID(), now);
                speak(player, "trade." + getProfession().id(), 3);
            }

            if (!getOffers().isEmpty()) {
                setTradingPlayer(player);
                openTradingScreen(player, getDisplayName(), this.merchantLevel);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /** Holds still and keeps its eye on the player who right-clicked it. Blocks wandering and idle looking. */
    /**
     * Day and night, every few minutes, an Ithk drifts off to float somewhere else in its village for a while,
     * then home again (or on to another spot). Ithk that don't live in a village (spawned by egg) stay home.
     * Trading or attending to a player always comes first.
     */
    private class MeanderGoal extends Goal {
        @Nullable private BlockPos target;
        private int ticks;

        MeanderGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (tradingPlayer != null || attending != null || isEthereal() || home == null
                    || level().getGameTime() < nextMeanderTime) {
                return false;
            }
            BoundingBox village = village();
            if (village == null) {
                nextMeanderTime = level().getGameTime() + MEANDER_MIN_WAIT;
                return false;
            }
            boolean atHome = distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) < 12 * 12;
            this.target = atHome || getRandom().nextFloat() < 0.35F ? pickSpot(village) : home;
            return this.target != null;
        }

        @Override
        public void start() {
            this.ticks = 0;
            stepToward();
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.ticks < MEANDER_TIMEOUT && tradingPlayer == null && attending == null
                    && !isEthereal() && distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5) > 2.5 * 2.5;
        }

        @Override
        public void tick() {
            this.ticks++;
            if (getNavigation().isDone() && this.ticks % 10 == 0) {
                stepToward();
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
            // Settle wherever it got to, drifting idly around that spot until the next journey.
            boolean arrived = this.target != null
                    && distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5) < 6 * 6;
            restrictTo(arrived ? this.target : blockPosition(), HOME_RADIUS);
            nextMeanderTime = level().getGameTime() + MEANDER_MIN_WAIT + getRandom().nextInt(MEANDER_EXTRA_WAIT);
            this.target = null;
        }

        /** Moves one short leg toward the target, so each path stays easy to find. */
        private void stepToward() {
            if (this.target == null) {
                return;
            }
            double dx = target.getX() + 0.5 - getX();
            double dy = target.getY() + 0.5 - getY();
            double dz = target.getZ() + 0.5 - getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double x = target.getX() + 0.5, y = target.getY() + 0.5, z = target.getZ() + 0.5;
            if (dist > MEANDER_STEP) {
                double f = MEANDER_STEP / dist;
                x = getX() + dx * f;
                z = getZ() + dz * f;
                // Cruise well above the ground between buildings.
                int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
                y = Math.max(getY() + dy * f, ground + 4);
            }
            if (!getNavigation().moveTo(x, y, z, 1.0)) {
                // No way through from here: try a little higher next time.
                getNavigation().moveTo(getX() + getRandom().nextInt(7) - 3, getY() + 3, getZ() + getRandom().nextInt(7) - 3, 1.0);
            }
        }

        /** A random open spot in the air somewhere in the village. */
        @Nullable
        private BlockPos pickSpot(BoundingBox village) {
            BlockPos c = village.getCenter();
            int r = Math.max(village.getXSpan(), village.getZSpan()) / 2;
            for (int attempt = 0; attempt < 10; attempt++) {
                double angle = getRandom().nextDouble() * Math.PI * 2.0;
                double d = Math.sqrt(getRandom().nextDouble()) * r * 0.75;
                int x = c.getX() + Mth.floor(Math.cos(angle) * d);
                int z = c.getZ() + Mth.floor(Math.sin(angle) * d);
                int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos spot = new BlockPos(x, ground + 4 + getRandom().nextInt(9), z);
                if (level().getBlockState(spot).isAir() && level().getBlockState(spot.above()).isAir()) {
                    return spot;
                }
            }
            return null;
        }
    }

    /** The Ithk village this Ithk lives in (looked up once), or null if none. */
    @Nullable
    private BoundingBox village() {
        if (!this.villageChecked && this.level() instanceof ServerLevel server && this.home != null) {
            this.villageChecked = true;
            Structure structure = server.registryAccess().registryOrThrow(Registries.STRUCTURE).get(VILLAGE);
            if (structure != null) {
                StructureStart start = server.structureManager().getStructureAt(this.home, structure);
                this.villageBounds = start.isValid() ? start.getBoundingBox() : null;
            }
        }
        return this.villageBounds;
    }

    private class AttendPlayerGoal extends Goal {
        AttendPlayerGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (tradingPlayer != null) {
                attending = tradingPlayer;
                attendTicks = Math.max(attendTicks, 20);
            }
            return attendTicks > 0 && attending != null && attending.isAlive()
                    && !attending.isSpectator() && distanceToSqr(attending) < 16.0 * 16.0;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (tradingPlayer == null) {
                attendTicks--;
            }
            if (attending != null) {
                getLookControl().setLookAt(attending, 30.0F, 30.0F);
            }
        }

        @Override
        public void stop() {
            attendTicks = 0;
            attending = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    // ---- Defense: ethereal phase and casting out ------------------------------

    public boolean isEthereal() {
        return this.entityData.get(ETHEREAL_TICKS) > 0;
    }

    public int getEtherealTicks() {
        return this.entityData.get(ETHEREAL_TICKS);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // /kill and the void still work.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (this.level().isClientSide) {
            return false;
        }
        if (isEthereal()) {
            return false;
        }

        Entity aggressor = source.getEntity();
        if (aggressor == null) {
            // Environmental harm (fire, cactus, etc.) behaves normally.
            return super.hurt(source, amount);
        }

        stopTrading();

        long now = this.level().getGameTime();
        boolean repeatOffense = aggressor.getUUID().equals(this.lastAggressor)
                && now - this.lastPhaseTime <= REPEAT_OFFENSE_WINDOW;

        if (repeatOffense) {
            if (aggressor instanceof Player player) {
                speak(player, "cast", 2);
            }
            castOut(aggressor);
        } else if (aggressor instanceof Player player) {
            speak(player, "phase", 3);
        }

        becomeEthereal();
        this.lastAggressor = aggressor.getUUID();
        this.lastPhaseTime = now;
        return false;
    }

    private void becomeEthereal() {
        this.entityData.set(ETHEREAL_TICKS, ETHEREAL_DURATION);
        this.playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.4F);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.5, this.getZ(),
                    12, 0.4, 0.4, 0.4, 0.02);
        }
    }

    /** Teleports the aggressor to a random surface spot just beyond the village's edge. */
    private void castOut(Entity aggressor) {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        // In a village, measure from the village's center out past its farthest building.
        double cx = this.home != null ? this.home.getX() + 0.5 : this.getX();
        double cz = this.home != null ? this.home.getZ() + 0.5 : this.getZ();
        int radius = VILLAGE_RADIUS;
        BoundingBox village = village();
        if (village == null) {
            village = findVillage(server);
        }
        if (village != null) {
            cx = village.getCenter().getX() + 0.5;
            cz = village.getCenter().getZ() + 0.5;
            radius = Math.max(village.getXSpan(), village.getZSpan()) / 2;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
            double distance = radius + 8 + this.getRandom().nextInt(17);
            int x = Mth.floor(cx + Math.cos(angle) * distance);
            int z = Mth.floor(cz + Math.sin(angle) * distance);
            int y = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= server.getMinBuildHeight()) {
                continue; // nothing to stand on here (void); try another direction
            }

            server.sendParticles(ParticleTypes.PORTAL, aggressor.getX(), aggressor.getY() + 1.0, aggressor.getZ(),
                    40, 0.5, 1.0, 0.5, 0.2);
            server.playSound(null, aggressor.getX(), aggressor.getY(), aggressor.getZ(),
                    SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.NEUTRAL, 1.0F, 0.8F);

            if (aggressor.isPassenger()) {
                aggressor.stopRiding();
            }
            aggressor.teleportTo(x + 0.5, y, z + 0.5);
            aggressor.resetFallDistance();
            if (aggressor instanceof Mob mob) {
                mob.setTarget(null);
            }

            server.playSound(null, x + 0.5, y, z + 0.5,
                    SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.NEUTRAL, 1.0F, 0.8F);
            return;
        }
    }

    /** The bounds of the Ithk village this Ithk lives in, or null if it isn't in one. */
    @Nullable
    private BoundingBox findVillage(ServerLevel server) {
        Structure structure = server.registryAccess().registryOrThrow(Registries.STRUCTURE).get(VILLAGE);
        if (structure == null) {
            return null;
        }
        StructureStart start = server.structureManager().getStructureAt(this.blockPosition(), structure);
        return start.isValid() ? start.getBoundingBox() : null;
    }

    @Override
    public boolean isPickable() {
        return !isEthereal() && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return !isEthereal() && super.isPushable();
    }

    @Override
    public boolean isAttackable() {
        return !isEthereal() && super.isAttackable();
    }

    // ---- Floating: no falling, no despawning -----------------------------------

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ---- Saving -------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("EyeColor", this.entityData.get(EYE_COLOR));
        tag.putString("Profession", getProfession().id());
        tag.putInt("MerchantXp", this.merchantXp);
        tag.putInt("MerchantLevel", this.merchantLevel);
        tag.putLong("LastRestock", this.lastRestock);
        tag.putInt("TradesVersion", TRADES_VERSION);
        if (this.home != null) {
            tag.putIntArray("Home", new int[]{this.home.getX(), this.home.getY(), this.home.getZ()});
        }
        if (this.offers != null) {
            MerchantOffers.CODEC.encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.offers)
                    .result().ifPresent(t -> tag.put("Offers", t));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(EYE_COLOR, tag.getInt("EyeColor"));
        if (tag.contains("Profession")) {
            setProfession(IthkProfession.byId(tag.getString("Profession")));
        }
        this.merchantXp = tag.getInt("MerchantXp");
        this.merchantLevel = Mth.clamp(tag.contains("MerchantLevel") ? tag.getInt("MerchantLevel") : 1, 1, MAX_LEVEL);
        this.lastRestock = tag.getLong("LastRestock");
        int[] home = tag.getIntArray("Home");
        if (home.length == 3) {
            this.home = new BlockPos(home[0], home[1], home[2]);
            // If it was saved while away, it stays where it is until its next journey brings it home.
            this.restrictTo(this.distanceToSqr(home[0], home[1], home[2]) > 16 * 16 ? this.blockPosition() : this.home,
                    HOME_RADIUS);
        }
        if (tag.contains("Offers") && tag.getInt("TradesVersion") >= TRADES_VERSION) {
            MerchantOffers.CODEC.parse(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.get("Offers"))
                    .result().ifPresent(o -> this.offers = o);
        }
    }

    // ---- Merchant ---------------------------------------------------------------

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    private void stopTrading() {
        // The trading screen closes itself once its merchant no longer has a trading player.
        this.tradingPlayer = null;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            if (this.level().isClientSide) {
                return new MerchantOffers();
            }
            ensureProfession();
            this.offers = new MerchantOffers();
            for (int lvl = 1; lvl <= this.merchantLevel; lvl++) {
                this.offers.addAll(IthkTrades.forLevel(getProfession(), lvl, this.registryAccess(), this.getRandom()));
            }
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.merchantXp += offer.getXp();
        if (offer.shouldRewardExp() && this.level() instanceof ServerLevel server) {
            int orbs = 3 + this.getRandom().nextInt(4);
            server.addFreshEntity(new ExperienceOrb(server, this.getX(), this.getY() + 0.5, this.getZ(), orbs));
        }
    }

    /** Levels up once the player has finished trading, like villagers do. New trades are there next time. */
    private void checkLevelUp() {
        while (this.merchantLevel < MAX_LEVEL && this.merchantXp >= LEVEL_XP[this.merchantLevel]) {
            levelUp();
        }
    }

    private void levelUp() {
        this.merchantLevel++;
        getOffers().addAll(IthkTrades.forLevel(getProfession(), this.merchantLevel, this.registryAccess(), this.getRandom()));
        this.playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.6F);
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.5F);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 0.5, this.getZ(),
                    30, 0.5, 0.5, 0.5, 0.5);
        }
    }

    private void restock() {
        if (this.offers != null) {
            for (MerchantOffer offer : this.offers) {
                offer.resetUses();
            }
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return this.merchantXp;
    }

    @Override
    public void overrideXp(int xp) {
        this.merchantXp = xp;
    }

    @Override
    public boolean showProgressBar() {
        return true;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }
}
