package com.childrenofmu.entity.folk;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A walking folk NPC (Rat, Cat, Bat, Dog and Frog folk, Goxa, Lilbuns). They wander, look at you and run when hurt; some
 * races trade (FolkRace.trades). One class serves every race; the race decides size, look, sounds and trades.
 */
public class FolkEntity extends PathfinderMob implements Merchant, net.minecraft.world.entity.monster.RangedAttackMob {
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(FolkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PLAYING =
            SynchedEntityData.defineId(FolkEntity.class, EntityDataSerializers.BOOLEAN);
    /** True while a frog is torpid on its mat for the night (synced, for the tucked-in pose). */
    private static final EntityDataAccessor<Boolean> RESTING =
            SynchedEntityData.defineId(FolkEntity.class, EntityDataSerializers.BOOLEAN);
    /** A baby, growing into an empty place in the city (synced, for its size). */
    private static final EntityDataAccessor<Boolean> BABY =
            SynchedEntityData.defineId(FolkEntity.class, EntityDataSerializers.BOOLEAN);
    /** This one's own size, times the race's (Dog folk vary from half a player's height to half again; synced). */
    private static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(FolkEntity.class, EntityDataSerializers.FLOAT);
    /** A week of Minecraft days, for the thieving cats' changing stock. */
    private static final long WEEK = 7 * 24000L;
    /** Trades restock once per Minecraft day. */
    private static final int RESTOCK_INTERVAL = 24000;

    private final FolkRace race;
    @Nullable private Player tradingPlayer;
    @Nullable private MerchantOffers offers;
    private long lastRestock;
    private long offersWeek = -1;
    /** How many of a traveller's town maps have been looked for so far (one every few seconds; see tickTravellerMaps). */
    private int mapsDone = Integer.MAX_VALUE;
    /** Like a villager: trading earns experience, and each new level (up to 5, Master) unlocks more trades. */
    private int merchantXp;
    private int merchantLevel = 1;
    private boolean levelUpPending;
    private static final int[] LEVEL_XP = {0, 10, 70, 150, 250};
    /** Dog City guards walking an unwelcome visitor out of town (see DogTrust): who, until when, and which side. */
    @Nullable java.util.UUID escortOf;
    long escortUntil;
    int escortSlot;
    @Nullable net.minecraft.world.phys.Vec3 escortFrom;     // the Keep's middle: the guard keeps between it and them
    /** A Dog City soldier marching with a player (one the monks trust): who. */
    @Nullable java.util.UUID companionOf;
    /** Set when the save (or a template) already gave this one a size. */
    private boolean sizePreset;
    /** Set when a structure or command chose the variant, so spawning doesn't pick another. */
    private boolean variantPreset;
    /** Set from a template ("StayRadius"): on spawning, stay within this many blocks of where it was put. */
    private int stayRadius = -1;
    /** Bats: near a stage (checked now and then), so they play there at night instead of flying about. */
    boolean nearStage;
    /** Rats: where this rat last came up out of the sewers (it makes for a different manhole). */
    @Nullable net.minecraft.core.BlockPos surfacedAt;
    /** Where a homebody lives; it never strays more than a few blocks from here. */
    @Nullable private net.minecraft.core.BlockPos home;
    private static final int HOMEBODY_RADIUS = 3;
    /** Folk placed by a town keep to roughly this many blocks of where they were placed. */
    private static final int TOWNSFOLK_RADIUS = 20;
    private int homeRadius = HOMEBODY_RADIUS;
    private long lastTalk = -100;
    /** Set in registerGoals (which runs before field initializers, so no initializer here). */
    private FolkRest.SleepGoal sleepGoal;
    /** A froglet kept by the Schoolmarm during school hours. */
    boolean atSchool;
    /** An Old Traveller (any race): lives in a little hut, sells maps to the towns. */
    private boolean traveller;

    public boolean isTraveller() {
        return this.traveller;
    }

    private boolean canTrade() {
        return !this.pitFighter && !isBaby() && (this.race.tradesAs(getVariantName()) || this.traveller);
    }
    /** A chef's tickets: dishes the kitchen is making (see FolkKitchen). */
    final java.util.List<FolkKitchen.Order> orders = new java.util.ArrayList<>();
    /** Where a jazz bat's melody last was (see FolkJazz). */
    int melodyIndex;
    /** Dub echoes waiting to play: {game time, volume, pitch} (see FolkJazz). */
    final java.util.ArrayDeque<double[]> echoes = new java.util.ArrayDeque<>();
    /** A Cat cop's current suspect (see FolkLaw). */
    @Nullable java.util.UUID wanted;
    /** Fighting in the pit (see FolkPit): yields instead of dying, and goes home after the bout. */
    boolean pitFighter;
    boolean yielded;
    long pitSeen;
    /** A bookie's book of bouts and bets (see FolkPit). */
    @Nullable private FolkPit.Book book;
    /** Set in registerGoals, like sleepGoal. */
    private FolkRoost.RoostGoal roostGoal;
    /** Set by FolkStreets.addGoals: a sewer rat's nest. */
    FolkStreets.NestGoal nestGoal;
    /** Its place in the city's census (see FolkCensus): which city, which place; -1 for none. */
    long slotCity;
    int slotId = -1;
    /** Checked with the census since it was loaded. */
    private boolean censused;
    /** When a baby grows up (game time). */
    private long growAt;
    /** Left its city to see the world (see FolkCensus): no home, wandering. */
    private boolean adventurer;
    @Nullable net.minecraft.core.BlockPos wanderTarget;
    long wanderUntil;
    /** Set in registerGoals. */
    private FolkWork.BedGoal bedGoal;

    public boolean isAdventurer() {
        return this.adventurer;
    }

    @Nullable
    net.minecraft.core.BlockPos bedTarget() {
        return this.bedGoal == null ? null : this.bedGoal.bed();
    }

    @Override
    public boolean isBaby() {
        return this.entityData != null && this.entityData.get(BABY);
    }

    /** Make this a baby that grows up at the given game time. */
    void setBaby(long growAt) {
        this.growAt = growAt;
        this.entityData.set(BABY, true);
        this.refreshDimensions();
    }

    private double climbY;
    private int climbStuck;

    /**
     * Stuck on a ladder (wedged in a shaft with others, or under a shut manhole), going nowhere for a few seconds: step
     * off it onto the nearest floor.
     */
    private void unjam() {
        if (!onClimbable() || onGround() || this.isResting() || this.isNoGravity()) {
            this.climbStuck = 0;
            return;
        }
        if (Math.abs(getY() - this.climbY) > 0.2) {
            this.climbY = getY();
            this.climbStuck = 0;
            return;
        }
        if (++this.climbStuck < 60) {
            return;
        }
        this.climbStuck = 0;
        net.minecraft.core.BlockPos at = this.blockPosition();
        net.minecraft.core.BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(at.offset(-3, -8, -3), at.offset(3, 1, 3))) {
            net.minecraft.world.level.block.state.BlockState s = this.level().getBlockState(p);
            if (!s.isAir() || !this.level().getBlockState(p.above()).isAir()
                    || !this.level().getBlockState(p.below()).isSolid()
                    || this.level().getBlockState(p.below()).is(net.minecraft.world.level.block.Blocks.LADDER)) {
                continue;
            }
            double d = p.distSqr(at) + this.random.nextDouble();      // a little random, so a crowd spreads out
            if (d < bestD) {
                best = p.immutable();
                bestD = d;
            }
        }
        if (best != null) {
            this.getNavigation().stop();
            this.moveTo(best.getX() + 0.5, best.getY(), best.getZ() + 0.5, this.getYRot(), 0.0F);
        }
    }

    private net.minecraft.world.phys.Vec3 stuckAt = net.minecraft.world.phys.Vec3.ZERO;
    private int groundStuck;

    /**
     * Stuck in a hole (a pit a block or two deep, too steep to climb out of) while trying to get somewhere: after five
     * seconds of running on the spot, scramble out onto the nearest bit of open ground above.
     */
    private void unpit() {
        boolean trying = !this.getNavigation().isDone() || this.isSprinting();
        if (!trying || this.isResting() || this.isNoGravity() || this.isPassenger() || this.isSleeping()
                || this.tradingPlayer != null || onClimbable()) {
            this.groundStuck = 0;
            return;
        }
        if (this.position().distanceToSqr(this.stuckAt) > 0.25) {
            this.stuckAt = this.position();
            this.groundStuck = 0;
            return;
        }
        if (++this.groundStuck < 100) {
            return;
        }
        this.groundStuck = 0;
        net.minecraft.core.BlockPos at = this.blockPosition();
        int walled = 0;
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            net.minecraft.core.BlockPos q = at.relative(d);
            if (!this.level().getBlockState(q).getCollisionShape(this.level(), q).isEmpty()
                    && !this.level().getBlockState(q.above()).getCollisionShape(this.level(), q.above()).isEmpty()) {
                walled++;
            }
        }
        this.getNavigation().stop();
        this.setSprinting(false);
        if (walled < 3) {
            return;     // not a pit: just give up on wherever that was
        }
        net.minecraft.core.BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(at.offset(-3, 1, -3), at.offset(3, 4, 3))) {
            if (!this.level().getBlockState(p).getCollisionShape(this.level(), p).isEmpty()
                    || !this.level().getBlockState(p.above()).getCollisionShape(this.level(), p.above()).isEmpty()
                    || !this.level().getBlockState(p.below()).isFaceSturdy(this.level(), p.below(), net.minecraft.core.Direction.UP)
                    || this.level().getBlockState(p).getFluidState().isSource()) {
                continue;
            }
            double d = p.distSqr(at) + this.random.nextDouble();
            if (d < bestD) {
                best = p.immutable();
                bestD = d;
            }
        }
        if (best != null) {
            this.moveTo(best.getX() + 0.5, best.getY(), best.getZ() + 0.5, this.getYRot(), 0.0F);
        }
    }

    /** Too many of us in town: off to see the world, with no home to go back to. */
    void becomeAdventurer() {
        this.adventurer = true;
        this.slotId = -1;
        this.home = null;
        this.clearRestriction();
        this.wanted = null;
        if (this.race == FolkRace.CAT) {
            presetVariant("adventurer");
        }
        double a = this.random.nextDouble() * Math.PI * 2.0;
        int d = 200 + this.random.nextInt(200);
        this.wanderTarget = this.blockPosition().offset((int) (Math.cos(a) * d), 0, (int) (Math.sin(a) * d));
        this.wanderUntil = this.level().getGameTime() + 3 * 24000L;
        this.getNavigation().stop();
    }

    /** Someone right-clicked us: stop and hear them out for a moment. */
    @Nullable private Player listeningTo;
    private long listenUntil;

    /** A Cat cop or city guard: the law in RCB City. */
    public boolean isCop() {
        return this.race == FolkRace.CAT && !isBaby() && ("cop".equals(getVariantName()) || "guard".equals(getVariantName())
                || "archer".equals(getVariantName()));
    }

    /** A shopkeeper's shop (from the sign over its door, e.g. "HATS"): it sells what the sign says. */
    @Nullable String shop;
    /** A Bat whose post is on a stage (worked out once, from where it was put). */
    @Nullable Boolean stagePost;

    /** Which trade pool this folk sells from: its shop's, or its variant's. */
    String tradeKey() {
        return this.shop != null ? "shop:" + this.shop : getVariantName();
    }

    @Nullable
    net.minecraft.core.BlockPos homePos() {
        return this.home;
    }

    FolkPit.Book book() {
        if (this.book == null) {
            this.book = new FolkPit.Book();
        }
        return this.book;
    }

    /** Choose the variant before spawning, so finalizeSpawn keeps it. */
    public void presetVariant(String name) {
        String[] names = this.race.variants();
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                setVariant(i);
                this.variantPreset = true;
            }
        }
    }

    /** The law is tougher than the folk it polices. */
    private void applyVariantStats() {
        if (FolkGar.isGar(this)) {
            // Vast, and going nowhere.
            setBase(Attributes.MAX_HEALTH, 150.0);
            setBase(Attributes.MOVEMENT_SPEED, 0.0);
            setBase(Attributes.KNOCKBACK_RESISTANCE, 1.0);
            setBase(Attributes.ARMOR, 10.0);
            this.setPersistenceRequired();
        }
        if (this.race == FolkRace.RAT && "accountant".equals(getVariantName())) {
            setBase(Attributes.FOLLOW_RANGE, 96.0);           // a long way up to bed, at the top of the Justice Hall
        }
        if (this.race == FolkRace.DOG && "high_guard".equals(getVariantName())) {
            // The High Guard of the dark tower: huge, armoured, and not to be moved.
            setBase(Attributes.MAX_HEALTH, 160.0);
            setBase(Attributes.ATTACK_DAMAGE, 16.0);
            setBase(Attributes.ARMOR, 20.0);
            setBase(Attributes.ARMOR_TOUGHNESS, 8.0);
            setBase(Attributes.KNOCKBACK_RESISTANCE, 1.0);
            setBase(Attributes.MOVEMENT_SPEED, 0.3);
            setBase(Attributes.FOLLOW_RANGE, 32.0);
            setBase(Attributes.STEP_HEIGHT, 1.1);
            this.setPersistenceRequired();
        }
        if (FolkCombat.isDogSoldier(this) && !"high_guard".equals(getVariantName())) {
            setBase(Attributes.MAX_HEALTH, 30.0);
            setBase(Attributes.ATTACK_DAMAGE, "pikeman".equals(getVariantName()) ? 7.0 : 6.0);
            setBase(Attributes.ARMOR, 6.0);
            setBase(Attributes.FOLLOW_RANGE, 64.0);           // the alarm brings them from across the city
        }
        if (isCop()) {
            boolean guard = !"cop".equals(getVariantName());
            setBase(Attributes.MAX_HEALTH, guard ? 36.0 : 30.0);
            setBase(Attributes.ATTACK_DAMAGE, guard ? 7.0 : 6.0);
            setBase(Attributes.ARMOR, guard ? 8.0 : 4.0);
            setBase(Attributes.MOVEMENT_SPEED, 0.33);
            setBase(Attributes.KNOCKBACK_RESISTANCE, 0.5);
            setBase(Attributes.FOLLOW_RANGE, 32.0);           // they spot trouble (and illagers) from afar
        }
    }

    private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance a = this.getAttribute(attribute);
        if (a != null) {
            a.setBaseValue(value);
        }
    }

    public FolkEntity(EntityType<? extends PathfinderMob> type, Level level, FolkRace race) {
        super(type, level);
        this.race = race;
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes(FolkRace race) {
        AttributeSupplier.Builder builder = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, race.health())
                .add(Attributes.MOVEMENT_SPEED, race.speed())
                .add(Attributes.ATTACK_DAMAGE, switch (race) {
                    case CAT -> 3.0;
                    case DOG -> 4.0;
                    case RAT, BAT -> 1.5;
                    case LILBUN -> 0.5;
                    default -> 2.0;
                });
        if (race == FolkRace.FROG) {
            // Frogs jump about twice as high as anyone else and land softly.
            builder.add(Attributes.JUMP_STRENGTH, 0.62).add(Attributes.SAFE_FALL_DISTANCE, 8.0);
        }
        return builder;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(PLAYING, false);
        builder.define(RESTING, false);
        builder.define(BABY, false);
        builder.define(SIZE, 1.0F);
    }

    public float getSize() {
        return this.entityData.get(SIZE);
    }

    /** Dogs: pick this one's size from its job (once; it's kept in the save). */
    private void pickSize() {
        if (this.race == FolkRace.DOG) {
            this.entityData.set(SIZE, FolkRace.dogHeight(getVariantName(), this.random) / 0.75F);
            this.refreshDimensions();
        }
    }

    /** Folk open doors and climb ladders (see FolkNavigation). */
    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new FolkNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));   // and shut it after
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal());
        this.goalSelector.addGoal(1, new ListenGoal());
        this.goalSelector.addGoal(1, new EscortGoal());
        this.goalSelector.addGoal(1, new FollowCompanionGoal());
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.5) {
            @Override
            public boolean canUse() {
                return !pitFighter && !isCop() && !FolkGar.isGar(FolkEntity.this)
                        && !FolkCombat.isDogSoldier(FolkEntity.this) && super.canUse();   // fighters and cops stand their ground; the Gar waddles (see FolkGar)
            }
        });
        FolkLaw.addGoals(this, this.goalSelector, this.targetSelector);
        this.goalSelector.addGoal(2, new FolkKitchen.CookGoal(this));
        this.sleepGoal = new FolkRest.SleepGoal(this);
        this.goalSelector.addGoal(3, this.sleepGoal);
        this.roostGoal = new FolkRoost.RoostGoal(this);
        this.goalSelector.addGoal(3, this.roostGoal);
        this.bedGoal = new FolkWork.BedGoal(this);
        this.goalSelector.addGoal(3, this.bedGoal);
        this.goalSelector.addGoal(4, new FolkWork.LeaveGoal(this));
        this.goalSelector.addGoal(3, new FolkCity.SweepGoal(this));
        FolkCombat.addGoals(this, this.goalSelector, this.targetSelector);
        FolkStreets.addGoals(this, this.goalSelector, this.targetSelector);
        this.goalSelector.addGoal(3, new PlayMusicGoal());
        this.goalSelector.addGoal(3, new FolkWork.PostGoal(this));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
        if (!this.variantPreset) {
            int pick;
            do {
                pick = this.random.nextInt(this.race.variants().length);
            } while (!this.race.randomVariant(this.race.variant(pick)));
            setVariant(pick);
        }
        if (!this.sizePreset) {
            pickSize();
        }
        applyVariantStats();
        this.setHealth(this.getMaxHealth());
        this.lastRestock = level.getLevel().getGameTime();
        if (spawnType == MobSpawnType.STRUCTURE && this.home == null && this.stayRadius >= 0) {
            this.home = this.blockPosition();
            this.homeRadius = Math.max(1, this.stayRadius);
            this.restrictTo(this.home, this.homeRadius);
        }
        if (spawnType == MobSpawnType.STRUCTURE && this.home == null) {
            // Townsfolk: stay around the town instead of wandering off into the swamp.
            this.home = this.blockPosition();
            this.homeRadius = FolkRace.isHomebody(getVariantName()) ? HOMEBODY_RADIUS
                    : this.traveller ? 8 : TOWNSFOLK_RADIUS;
            this.restrictTo(this.home, this.homeRadius);
        }
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    /** The trading window's title: the folk's short name (no title), or its race; the window adds the level. */
    private net.minecraft.network.chat.Component tradeTitle() {
        if (!this.hasCustomName()) {
            return getDisplayName();
        }
        return net.minecraft.network.chat.Component.literal(FolkNames.shortName(getCustomName().getString()));
    }

    /** Folk are townspeople, not wildlife: they never despawn. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** For DogTrust: open this folk's trades for a player (the monks trade when spoken to sneaking). */
    void openTrades(Player player) {
        if (canTrade() && !getOffers().isEmpty() && (this.tradingPlayer == null || this.tradingPlayer == player)) {
            this.getNavigation().stop();
            setTradingPlayer(player);
            openTradingScreen(player, tradeTitle(), this.race.weekly() || this.traveller ? 0 : this.merchantLevel);
        }
    }

    public FolkRace getRace() {
        return this.race;
    }

    public boolean isResting() {
        return this.entityData.get(RESTING);
    }

    void setResting(boolean resting) {
        this.entityData.set(RESTING, resting);
    }

    /** The mat this frog is sleeping on or heading for, if any. */
    @Nullable
    net.minecraft.core.BlockPos restingMat() {
        if (this.roostGoal != null && this.roostGoal.post() != null) {
            return this.roostGoal.post();
        }
        if (this.nestGoal != null && this.nestGoal.nest() != null) {
            return this.nestGoal.nest();
        }
        return this.sleepGoal == null ? null : this.sleepGoal.mat();
    }

    /** Back to wandering around its own home (after school, say). */
    int homeRadius() {
        return this.home == null ? Integer.MAX_VALUE : this.homeRadius;
    }

    void setHome(net.minecraft.core.BlockPos pos, int radius) {
        this.home = pos;
        this.homeRadius = radius;
        this.restrictTo(pos, radius);
    }

    void restoreHome() {
        if (this.home != null) {
            this.restrictTo(this.home, this.homeRadius);
        } else {
            this.clearRestriction();
        }
    }

    @Override
    public void performRangedAttack(net.minecraft.world.entity.LivingEntity target, float power) {
        FolkCombat.shootDart(this, target);
    }

    /** True while a bat is playing jazz (synced, for the playing animation). */
    public boolean isPlaying() {
        return this.entityData.get(PLAYING);
    }

    public String getVariantName() {
        return this.race.variant(this.entityData.get(VARIANT));
    }

    public void setVariant(int index) {
        this.entityData.set(VARIANT, Math.floorMod(index, this.race.variants().length));
        this.setItemSlot(EquipmentSlot.MAINHAND, this.race.heldItem(getVariantName()));
        this.offers = null;
        this.refreshDimensions();
        applyVariantStats();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (SIZE.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
        if ((VARIANT.equals(key) || BABY.equals(key)) && this.race != null) {
            this.refreshDimensions();
        }
    }

    @Override
    protected net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        if (this.race == null) {
            return super.getDefaultDimensions(pose);    // still being constructed
        }
        net.minecraft.world.entity.EntityDimensions d = super.getDefaultDimensions(pose).scale(FolkRace.variantScale(getVariantName()));
        float size = getSize();
        // Taller and broader with size, but never wider than a doorway.
        return size == 1.0F ? d : d.scale(Math.min(size, 0.9F / Math.max(0.1F, d.width())), size);
    }

    /** A beaten pit fighter yields rather than dies. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.pitFighter && !this.level().isClientSide
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (this.yielded) {
                return false;
            }
            if (this.getHealth() - amount < 2.0F) {
                this.setHealth(1.0F);
                this.yielded = true;
                this.setTarget(null);
                this.setAggressive(false);
                this.getNavigation().stop();
                this.playSound(getHurtSound(source), 1.0F, 0.8F);
                FolkPit.yielded(this);
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Climbing: on a ladder with the path leading on up it, go up (walking into a ladder only climbs when pushing
        // against something, and a ladder shaft goes straight up).
        if (!this.level().isClientSide && onClimbable() && !this.isNoGravity()) {
            net.minecraft.world.level.pathfinder.Path path = this.getNavigation().getPath();
            if (path != null && !path.isDone()) {
                net.minecraft.world.level.pathfinder.Node next = path.getNextNode();
                if (next.y > this.blockPosition().getY()
                        || (next.y == this.blockPosition().getY() && this.getY() - next.y < 0.0)) {
                    double cx = next.x + 0.5 - this.getX(), cz = next.z + 0.5 - this.getZ();
                    this.setDeltaMovement(cx * 0.25, 0.2, cz * 0.25);
                }
            }
        }
        // Frogs get about by hopping: froglets everywhere, grown-ups now and then as they go.
        if (!this.level().isClientSide && this.race == FolkRace.FROG && this.onGround() && !this.isInWater() && !isResting()) {
            boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4;
            boolean froglet = "froglet".equals(getVariantName());
            if ((moving && this.random.nextInt(froglet ? 6 : 14) == 0) || (froglet && this.random.nextInt(160) == 0)) {
                this.jumpFromGround();
            }
        }
    }

    // ---- Trading ----------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide && !isResting() && !this.pitFighter) {
            // Whatever else happens, stop what you're doing and look at whoever it is for a few seconds.
            this.listeningTo = player;
            this.listenUntil = this.level().getGameTime() + 70;
            this.getNavigation().stop();
        }
        if (hand == InteractionHand.MAIN_HAND && this.race == FolkRace.FROG && !player.isSecondaryUseActive()) {
            String v = getVariantName();
            if ("jumper".equals(v) || "guard".equals(v) || "elder".equals(v) || "chitinier".equals(v) || "cook".equals(v)) {
                if (!this.level().isClientSide) {
                    this.getNavigation().stop();
                    this.getLookControl().setLookAt(player);
                    long now = this.level().getGameTime();
                    if (now - this.lastTalk >= 30) {     // no more than one line every second and a half
                        this.lastTalk = now;
                        if ("jumper".equals(v)) {
                            com.childrenofmu.post.FolkPost.offer(this, player);
                        } else if ("chitinier".equals(v)) {
                            FolkKitchen.offer(this, player);
                        } else if ("cook".equals(v)) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                                    "kitchen.childrenofmu.cook." + this.random.nextInt(4), getDisplayName()));
                        } else {
                            FolkGossip.speak(this, player);
                        }
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        if (hand == InteractionHand.MAIN_HAND && FolkGar.isGar(this)) {
            if (!this.level().isClientSide) {
                this.getLookControl().setLookAt(player);
                long now = this.level().getGameTime();
                if (now - this.lastTalk >= 20) {
                    this.lastTalk = now;
                    FolkGar.audience(this, player);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (hand == InteractionHand.MAIN_HAND && !player.isSecondaryUseActive() && !this.pitFighter
                && (isCop() || FolkPit.isBookie(this))) {
            if (!this.level().isClientSide) {
                this.getLookControl().setLookAt(player);
                long now = this.level().getGameTime();
                if (now - this.lastTalk >= 30) {
                    this.lastTalk = now;
                    if (isCop()) {
                        FolkLaw.talk(this, player);
                    } else if (FolkGar.mayTrade(this, player)) {
                        FolkPit.offer(this, player);
                    }
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (hand == InteractionHand.MAIN_HAND && this.race == FolkRace.DOG) {
            InteractionResult r = DogTrust.interact(this, player);
            if (r != null) {
                return r;
            }
        }
        if (!canTrade() || hand != InteractionHand.MAIN_HAND || player.isSecondaryUseActive()) {
            return super.mobInteract(player, hand);
        }
        if (this.tradingPlayer != null && this.tradingPlayer != player) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);   // busy with someone else
        }
        if (!this.level().isClientSide && !this.traveller && FolkLaw.isCityFolk(this) && !FolkGar.mayTrade(this, player)) {
            return InteractionResult.sidedSuccess(false);
        }
        if (!this.level().isClientSide && !getOffers().isEmpty()) {
            this.getNavigation().stop();
            setTradingPlayer(player);
            openTradingScreen(player, tradeTitle(), this.race.weekly() || this.traveller ? 0 : this.merchantLevel);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (FolkKitchen.isChef(this)) {
            FolkKitchen.tickChef(this);
        }
        if (this.pitFighter) {
            FolkPit.tickFighter(this);
        } else if (FolkLaw.isCityFolk(this)) {
            if (isCop()) {
                FolkLaw.tickCop(this);
            } else if (FolkPit.isBookie(this)) {
                FolkPit.tickBookie(this);
            }
        }
        if (this.race == FolkRace.FROG && "froglet".equals(getVariantName())) {
            FolkRest.tickSchool(this);
        }
        if (this.companionOf != null && getTarget() == null && (this.tickCount & 7) == 0) {
            // A companion takes up its player's fights: whoever hurts them, and whatever they strike.
            Player p = companion();
            if (p != null) {
                net.minecraft.world.entity.LivingEntity foe = p.getLastHurtByMob();
                if (foe == null || this.tickCount - p.getLastHurtByMobTimestamp() > 200) {
                    foe = this.tickCount - p.getLastHurtMobTimestamp() < 100 ? p.getLastHurtMob() : null;
                }
                if (foe != null && foe.isAlive() && foe != this && !(foe instanceof FolkEntity)
                        && !(foe instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame())
                        && !(foe instanceof Player) && foe.distanceToSqr(this) < 24 * 24) {
                    setTarget(foe);
                }
            }
        }
        if (this.level() instanceof net.minecraft.server.level.ServerLevel server) {
            if (isBaby() && server.getGameTime() >= this.growAt) {
                // Grown up, and into the job.
                this.entityData.set(BABY, false);
                this.refreshDimensions();
                applyVariantStats();
                this.setHealth(this.getMaxHealth());
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight(),
                        getZ(), 6, 0.3, 0.3, 0.3, 0.0);
            }
            if (!this.censused && (this.tickCount + getId()) % 20 == 0) {
                this.censused = true;
                if (this.slotId < 0 && !this.adventurer && !isBaby() && this.home != null) {
                    FolkCensus.register(server, this);
                }
            }
        }
        unjam();
        unpit();
        if (this.isSleeping() && (this.bedGoal == null || this.bedGoal.bed() == null)) {
            this.stopSleeping();          // loaded asleep, or woken: up you get
        }
        FolkWork.tick(this);
        if (FolkCombat.isGuard(this) && this.getMainHandItem().isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, this.race.heldItem("guard"));   // guards from before blowpipes
        }
        if (this.traveller && !this.hasCustomName()) {
            this.setCustomName(FolkNames.travellerName(this.random));
            this.setItemSlot(EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPASS));
        }
        if (!this.hasCustomName()) {
            net.minecraft.network.chat.Component name = FolkNames.name(this.race, getVariantName(), this.random);
            if (name != null) {
                this.setCustomName(name);
            }
        }
        if (FolkGar.isGar(this)) {
            FolkGar.tickName(this);
            FolkGar.tick(this);
        }
        if (this.race == FolkRace.FROG) {
            this.setAirSupply(this.getMaxAirSupply());   // frog folk live in the swamp and never drown
        }
        if (this.tradingPlayer != null && (!this.tradingPlayer.isAlive() || this.tradingPlayer.distanceTo(this) > 8.0F)) {
            this.tradingPlayer = null;
        }
        if (FolkRace.isHomebody(getVariantName()) && this.home == null) {
            this.home = this.blockPosition();
            this.homeRadius = HOMEBODY_RADIUS;
            this.restrictTo(this.home, HOMEBODY_RADIUS);
        }
        if (this.race == FolkRace.BAT) {
            // Bat folk play jazz all night. The barman keeps the bar.
            if ((this.tickCount + getId()) % 100 == 0) {
                this.nearStage = FolkStreets.nearStage(this);
            }
            // Bat folk play jazz all night, on (or by) a stage; the rest fly about town. The barman keeps the bar.
            boolean play = FolkWork.phase(this) == FolkWork.Phase.WORK && !"barman".equals(getVariantName())
                    && !"bookie".equals(getVariantName()) && this.tradingPlayer == null
                    && this.hurtTime == 0 && this.nearStage && !isResting();
            if (play != isPlaying()) {
                this.entityData.set(PLAYING, play);
            }
            if (play && this.level() instanceof net.minecraft.server.level.ServerLevel server) {
                FolkJazz.tick(this, server);
            }
        }
        if (this.traveller) {
            tickTravellerMaps();
        }
        if (this.offers != null && this.race.weekly() && this.level().getGameTime() / WEEK != this.offersWeek) {
            this.offers = null;   // a new week: the cat has stolen something else
        }
        if (this.offers != null && !this.race.weekly() && this.tickCount % 100 == 0) {   // stolen goods don't restock
            long now = this.level().getGameTime();
            if (now - this.lastRestock >= RESTOCK_INTERVAL || now < this.lastRestock) {
                this.offers.forEach(MerchantOffer::resetUses);
                this.lastRestock = now;
            }
        }
    }

    /**
     * A traveller works out its town maps one at a time, a few seconds apart, from the moment it's first loaded, so no
     * single tick stalls the server (finding the nearest town can be slow). Maps go at the top of the list.
     */
    private void tickTravellerMaps() {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel server) || this.tickCount % 60 != 17) {
            return;
        }
        if (this.offers == null) {
            getOffers();
        }
        if (this.offers == null || this.mapsDone >= FolkTrades.townMapCount() || this.tradingPlayer != null) {
            return;
        }
        int i = this.mapsDone++;
        FolkTrades.townMapOffer(this.race, server, this.blockPosition(), i, this.random)
                .ifPresent(o -> this.offers.add(Math.min(i, this.offers.size()), o));
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        boolean closing = this.tradingPlayer != null && player == null;
        this.tradingPlayer = player;
        if (closing && this.levelUpPending) {
            this.levelUpPending = false;
            levelUp();
        }
    }

    private boolean canLevel() {
        return !this.race.weekly() && !this.traveller && this.merchantLevel < 5;
    }

    private void levelUp() {
        while (canLevel() && this.merchantXp >= LEVEL_XP[this.merchantLevel]) {
            this.merchantLevel++;
            if (this.offers != null) {
                FolkTrades.levelUp(this.race, tradeKey(), this.offers, this.merchantLevel, this.random);
            }
        }
        if (this.level() instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight() + 0.2,
                    getZ(), 8, 0.4, 0.3, 0.4, 0.0);
        }
        this.playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.6F);
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            if (this.level().isClientSide || !canTrade()) {
                return new MerchantOffers();
            }
            this.offers = this.traveller && this.level() instanceof net.minecraft.server.level.ServerLevel server
                    ? FolkTrades.traveller(this.race, server, this.blockPosition(), this.random)
                    : FolkTrades.forVariant(this.race, tradeKey(), this.random);
            if (this.traveller) {
                this.mapsDone = 0;
            }
            this.offersWeek = this.level().getGameTime() / WEEK;
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
        if (canLevel() && this.merchantXp >= LEVEL_XP[this.merchantLevel]) {
            this.levelUpPending = true;    // like a villager: they level up once you close the window
        }
        this.playSound(getHurtSound(this.damageSources().generic()), 0.4F, 1.6F);
        if (this.race == FolkRace.DOG && this.tradingPlayer instanceof net.minecraft.server.level.ServerPlayer sp) {
            DogTrust.raise(sp, this.blockPosition(), 2);                  // fair dealing earns the folk's confidence
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
        return !this.race.weekly() && !this.traveller;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.RABBIT_AMBIENT;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    /** Stands in place while playing music. */
    private class PlayMusicGoal extends Goal {
        PlayMusicGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isPlaying();
        }

        @Override
        public void start() {
            getNavigation().stop();
        }
    }

    /** Stops and faces whoever right-clicked it, for a few seconds. */
    private class ListenGoal extends Goal {
        ListenGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return listeningTo != null && tradingPlayer == null && level().getGameTime() < listenUntil
                    && listeningTo.isAlive() && listeningTo.distanceToSqr(FolkEntity.this) < 100.0;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void stop() {
            listeningTo = null;
        }

        @Override
        public void tick() {
            getNavigation().stop();
            if (listeningTo != null) {
                getLookControl().setLookAt(listeningTo, 30.0F, 30.0F);
            }
        }
    }

    /** Stands still and faces the player while trading. */
    private class TradeWithPlayerGoal extends Goal {
        TradeWithPlayerGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return tradingPlayer != null;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (tradingPlayer != null) {
                getLookControl().setLookAt(tradingPlayer, 30.0F, 30.0F);
            }
        }
    }

    // ---- Saving -----------------------------------------------------------------

    /** Join a player on their adventure (no more keeping to the city), or leave them and go home. */
    void joinCompanion(@Nullable Player player) {
        this.companionOf = player == null ? null : player.getUUID();
        if (player != null) {
            this.clearRestriction();
            this.setPersistenceRequired();
        } else if (this.home != null) {
            this.restrictTo(this.home, this.homeRadius);
        }
    }

    @Nullable
    Player companion() {
        return this.companionOf == null ? null : this.level().getPlayerByUUID(this.companionOf);
    }

    /**
     * A companion keeps near its player like a sworn retainer: walks after them, catches up (by any means) when left far
     * behind, and stands by them when close. It holds the folk's movement, so work, beds and posts don't call it away.
     */
    class FollowCompanionGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private int repath;

        FollowCompanionGoal() {
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player p = companion();
            return p != null && p.level() == level() && getTarget() == null && !p.isSpectator();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            Player p = companion();
            if (p == null) {
                return;
            }
            double d = distanceToSqr(p);
            if (d < 36) {
                getLookControl().setLookAt(p, 20.0F, 20.0F);
            }
            if (d > 32 * 32) {
                // Left far behind: catch up at the player's side, on solid ground.
                for (int i = 0; i < 12; i++) {
                    net.minecraft.core.BlockPos at = p.blockPosition().offset(random.nextInt(7) - 3, random.nextInt(3) - 1,
                            random.nextInt(7) - 3);
                    if (level().getBlockState(at.below()).isSolidRender(level(), at.below())
                            && level().getBlockState(at).isAir() && level().getBlockState(at.above()).isAir()) {
                        teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                        getNavigation().stop();
                        return;
                    }
                }
            }
            if (--this.repath > 0) {
                return;
            }
            this.repath = 10;
            if (d > 5 * 5) {
                getNavigation().moveTo(p, d > 12 * 12 ? 1.35 : 1.1);
            } else {
                getNavigation().stop();
            }
        }
    }

    /**
     * A Dog City guard escorting someone out: stays a couple of blocks off them on the Keep's side (so they can only go
     * the other way), facing them, keeping pace as they go.
     */
    class EscortGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private int repath;

        EscortGoal() {
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private Player who() {
            if (escortOf == null || escortFrom == null || level().getGameTime() > escortUntil) {
                return null;
            }
            Player p = level().getPlayerByUUID(escortOf);
            return p != null && p.isAlive() && p.distanceToSqr(FolkEntity.this) < 96 * 96 ? p : null;
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && who() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            Player p = who();
            if (p == null) {
                return;
            }
            getLookControl().setLookAt(p, 30.0F, 30.0F);
            if (--this.repath > 0) {
                return;
            }
            this.repath = 8;
            net.minecraft.world.phys.Vec3 toKeep = escortFrom.subtract(p.position()).multiply(1, 0, 1);
            toKeep = toKeep.lengthSqr() < 1.0E-4 ? new net.minecraft.world.phys.Vec3(0, 0, 1) : toKeep.normalize();
            net.minecraft.world.phys.Vec3 side = new net.minecraft.world.phys.Vec3(-toKeep.z, 0, toKeep.x);
            net.minecraft.world.phys.Vec3 spot = p.position().add(toKeep.scale(2.5))
                    .add(side.scale((escortSlot - 1) * 1.6));
            double d = distanceToSqr(spot);
            if (d > 1.5) {
                getNavigation().moveTo(spot.x, spot.y, spot.z, d > 64 ? 1.3 : 1.0);
            } else {
                getNavigation().stop();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Variant", getVariantName());
        tag.putFloat("Size", getSize());
        if (this.companionOf != null) {
            tag.putUUID("CompanionOf", this.companionOf);
        }
        if (this.shop != null) {
            tag.putString("Shop", this.shop);
        }
        tag.putLong("LastRestock", this.lastRestock);
        tag.putLong("OffersWeek", this.offersWeek);
        FolkKitchen.save(this, tag);
        tag.putInt("MerchantXp", this.merchantXp);
        tag.putBoolean("Traveler", this.traveller);
        tag.putBoolean("PitFighter", this.pitFighter);
        tag.putInt("MerchantLevel", this.merchantLevel);
        tag.putLong("SlotCity", this.slotCity);
        tag.putInt("SlotId", this.slotId);
        tag.putBoolean("Adventurer", this.adventurer);
        if (isBaby()) {
            tag.putLong("GrowAt", this.growAt);
        }
        if (this.wanderTarget != null) {
            tag.putLong("WanderTarget", this.wanderTarget.asLong());
            tag.putLong("WanderUntil", this.wanderUntil);
        }
        if (this.home != null) {
            tag.putIntArray("Home", new int[]{this.home.getX(), this.home.getY(), this.home.getZ()});
            tag.putInt("HomeRadius", this.homeRadius);
        }
        tag.putInt("MapsDone", Math.min(this.mapsDone, 99));
        if (this.offers != null) {
            MerchantOffers.CODEC.encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.offers)
                    .result().ifPresent(t -> tag.put("Offers", t));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) {
            String name = tag.getString("Variant");
            String[] names = this.race.variants();
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(name)) {
                    this.entityData.set(VARIANT, i);
                    this.variantPreset = true;
                }
            }
            if (this.getMainHandItem().isEmpty()) {
                // Summoned with a chosen variant: hand it that variant's tool.
                this.setItemSlot(EquipmentSlot.MAINHAND, this.race.heldItem(getVariantName()));
            }
        }
        if (tag.hasUUID("CompanionOf")) {
            this.companionOf = tag.getUUID("CompanionOf");
        }
        if (tag.contains("Size")) {
            this.entityData.set(SIZE, tag.getFloat("Size"));
            this.sizePreset = true;
            this.refreshDimensions();
        } else if (this.race == FolkRace.DOG && tag.contains("Health")) {
            pickSize();                                                   // a dog from before sizes: give it one
            this.sizePreset = true;
        }
        this.lastRestock = tag.getLong("LastRestock");
        if (tag.contains("Shop")) {
            this.shop = tag.getString("Shop");
        }
        if (tag.contains("StayRadius")) {
            this.stayRadius = tag.getInt("StayRadius");
        }
        this.offersWeek = tag.contains("OffersWeek") ? tag.getLong("OffersWeek") : -1;
        FolkKitchen.load(this, tag);
        this.merchantXp = tag.getInt("MerchantXp");
        this.traveller = tag.getBoolean("Traveler");
        this.pitFighter = tag.getBoolean("PitFighter");
        applyVariantStats();
        if (this.race == FolkRace.BAT || this.race == FolkRace.RAT) {
            this.setNoGravity(false);   // saved while hanging from a roost (or asleep in a pile of rats): let go
        }
        this.merchantLevel = Math.max(1, tag.getInt("MerchantLevel"));
        this.slotCity = tag.getLong("SlotCity");
        this.slotId = tag.contains("SlotId") ? tag.getInt("SlotId") : -1;
        this.adventurer = tag.getBoolean("Adventurer");
        if (tag.contains("GrowAt")) {
            this.growAt = tag.getLong("GrowAt");
            this.entityData.set(BABY, true);
        }
        if (tag.contains("WanderTarget")) {
            this.wanderTarget = net.minecraft.core.BlockPos.of(tag.getLong("WanderTarget"));
            this.wanderUntil = tag.getLong("WanderUntil");
        }
        int[] home = tag.getIntArray("Home");
        if (home.length == 3) {
            this.home = new net.minecraft.core.BlockPos(home[0], home[1], home[2]);
            this.homeRadius = tag.contains("HomeRadius") ? tag.getInt("HomeRadius") : HOMEBODY_RADIUS;
            this.restrictTo(this.home, this.homeRadius);
        }
        if (this.companionOf != null) {
            this.clearRestriction();                                      // off adventuring with someone
        }
        if (tag.contains("Offers")) {
            MerchantOffers.CODEC.parse(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.get("Offers"))
                    .result().ifPresent(o -> this.offers = o);
        }
        this.mapsDone = tag.contains("MapsDone") ? tag.getInt("MapsDone") : Integer.MAX_VALUE;
    }

    // ---- Sounds -----------------------------------------------------------------

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (this.random.nextInt(4) != 0) {
            return null;
        }
        return switch (this.race) {
            case CAT -> SoundEvents.CAT_AMBIENT;
            case DOG -> SoundEvents.WOLF_AMBIENT;
            case BAT -> isPlaying() ? null : SoundEvents.BAT_AMBIENT;
            case FROG -> SoundEvents.FROG_AMBIENT;
            default -> null;
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return switch (this.race) {
            case CAT -> SoundEvents.CAT_HURT;
            case DOG -> SoundEvents.WOLF_HURT;
            case BAT -> SoundEvents.BAT_HURT;
            case FROG -> SoundEvents.FROG_HURT;
            case GOXA -> SoundEvents.ALLAY_HURT;
            default -> SoundEvents.RABBIT_HURT;
        };
    }

    @Override
    protected SoundEvent getDeathSound() {
        return switch (this.race) {
            case CAT -> SoundEvents.CAT_DEATH;
            case DOG -> SoundEvents.WOLF_DEATH;
            case BAT -> SoundEvents.BAT_DEATH;
            case FROG -> SoundEvents.FROG_DEATH;
            case GOXA -> SoundEvents.ALLAY_DEATH;
            default -> SoundEvents.RABBIT_DEATH;
        };
    }

    @Override
    public float getVoicePitch() {
        // Small folk have small voices; Lilbuns smallest of all.
        float pitch = switch (this.race) {
            case DOG -> 1.0F;
            case FROG -> "froglet".equals(getVariantName()) ? 1.8F : 1.0F;
            case GOXA -> 0.6F;
            case CAT, BAT -> 1.1F;
            case LILBUN -> 1.9F;
            default -> 1.4F;
        };
        return super.getVoicePitch() * pitch * (isBaby() ? 1.3F : 1.0F);
    }
}
