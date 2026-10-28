package com.childrenofmu.entity.folk;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

/**
 * The working day in RCB City. Every citizen has a post: the spot the city put them (a shop counter, a desk, a beat,
 * a stage, a bar). Their day runs in three parts:
 *
 *  - Work: they keep to their post (a shopkeeper at the counter, a cop on the beat round it).
 *  - Off duty: they wander the town round about.
 *  - Sleep: Cats and the town Rats go to bed (any free bed near home; the Gar's accountants only in the green beds
 *    of their dormitory, and nobody else in those). Bats roost (see FolkRoost); the sewer Rats keep their own hours
 *    in their nests (see FolkStreets).
 *
 * Cats and town Rats work by day and sleep at night. Bats work the night (the clubs, the bar, the band) and sleep by
 * day. Half the city guards keep the night watch, and sleep by day instead.
 */
public final class FolkWork {
    enum Phase { WORK, OFF, SLEEP }

    private FolkWork() {}

    static boolean nightShift(FolkEntity e) {
        if (e.getRace() == FolkRace.BAT) {
            return true;
        }
        String v = e.getVariantName();
        return e.getRace() == FolkRace.CAT && ("guard".equals(v) || "archer".equals(v))
                && (e.getUUID().getLeastSignificantBits() & 1L) == 0L;
    }

    static Phase phase(FolkEntity e) {
        return phase(e.level(), nightShift(e));
    }

    static Phase phase(Level level, boolean night) {
        long t = level.getDayTime() % 24000L;
        if (night) {
            if (t >= 13000L && t < 23000L) {
                return Phase.WORK;
            }
            return (t >= 11500L && t < 13000L) || (t >= 23000L && t < 23600L) ? Phase.OFF : Phase.SLEEP;
        }
        if (t >= 1000L && t < 11000L) {
            return Phase.WORK;
        }
        return t >= 13000L && t < 23500L ? Phase.SLEEP : Phase.OFF;
    }

    /** Folk who keep the city's hours: placed citizens with a post (not travellers, fighters, the Gar or sewer rats). */
    static boolean scheduled(FolkEntity e) {
        return FolkLaw.isCityFolk(e) && e.homePos() != null && !e.isTraveller() && !e.pitFighter && !FolkGar.isGar(e)
                && !e.isAdventurer() && !e.isBaby() && !FolkStreets.sewerRat(e);
    }

    /** How far from their post folk go while working. */
    static int workRadius(FolkEntity e) {
        String v = e.getVariantName();
        return switch (e.getRace()) {
            case CAT -> switch (v) {
                case "cop" -> 14;
                case "guard" -> 10;
                case "archer" -> 8;
                case "rogue" -> 4;
                case "fellmonger" -> 2;
                case "hunter" -> 6;
                case "adventurer" -> 12;
                default -> 6;
            };
            case RAT -> switch (v) {
                case "accountant" -> 2;
                case "postrat" -> 3;
                default -> 4;
            };
            case BAT -> switch (v) {
                case "barman", "bookie" -> 3;
                default -> 5;
            };
            default -> 6;
        };
    }

    /** Every so often, on any folk: keep them to their post, or let them off, as the hour says. */
    static void tick(FolkEntity e) {
        if ((e.tickCount + e.getId()) % 40 != 0 || !scheduled(e)) {
            return;
        }
        BlockPos home = e.homePos();
        int hr = e.homeRadius();
        int r = switch (phase(e)) {
            case WORK -> Math.min(hr, workRadius(e));
            case OFF -> hr;
            case SLEEP -> Math.min(hr, 6);
        };
        if (!home.equals(e.getRestrictCenter()) || (int) e.getRestrictRadius() != r) {
            e.restrictTo(home, r);
        }
    }

    /**
     * Folk who keep a post rather than a beat: a shopkeeper or stallholder at the counter, the barman at the bar, the
     * bookie in the booth, a Bat band on its stage. (Cops, guards, sweepers, errand runners and the wandering Cats
     * keep moving.)
     */
    static boolean postKeeper(FolkEntity e) {
        String v = e.getVariantName();
        return switch (e.getRace()) {
            case BAT -> "barman".equals(v) || "bookie".equals(v) || onStage(e);
            case RAT -> e.shop != null || (e.getRace().tradesAs(v) && !"sweeper".equals(v) && !"runner".equals(v)
                    && !"gong".equals(v));
            case CAT -> "fellmonger".equals(v) || "rogue".equals(v);
            default -> false;
        };
    }

    /** A Bat put on a stage (a note block or jukebox right by where it was put): that's where it plays. */
    static boolean onStage(FolkEntity e) {
        if (e.stagePost == null) {
            BlockPos home = e.homePos();
            boolean stage = false;
            if (home != null) {
                for (BlockPos p : BlockPos.betweenClosed(home.offset(-4, -2, -4), home.offset(4, 2, 4))) {
                    BlockState s = e.level().getBlockState(p);
                    if (s.is(Blocks.NOTE_BLOCK) || s.is(Blocks.JUKEBOX)) {
                        stage = true;
                        break;
                    }
                }
            }
            e.stagePost = stage;
        }
        return e.stagePost;
    }

    /** At work, and the kind that works at a post. */
    static boolean keepsPost(FolkEntity e) {
        return scheduled(e) && (phase(e) == Phase.WORK || FolkPit.isBookie(e)) && postKeeper(e) && e.getTarget() == null
                && !e.isResting()
                && !e.isSleeping();
    }

    /**
     * Back to the post and stay there through the working hours: the shopkeeper behind the counter, the band on the
     * stage. A Bat that can't find a way up (to a roof-deck stage, say) flutters straight there.
     */
    static class PostGoal extends Goal {
        private final FolkEntity folk;
        private int repath;
        private int stuck;

        PostGoal(FolkEntity folk) {
            this.folk = folk;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return keepsPost(this.folk) && this.folk.getTradingPlayer() == null && this.folk.homePos() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            this.repath = 0;
            this.stuck = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return false;
        }

        @Override
        public void tick() {
            BlockPos home = this.folk.homePos();
            Vec3 c = Vec3.atBottomCenterOf(home);
            double dx = this.folk.getX() - c.x, dz = this.folk.getZ() - c.z, dy = this.folk.getY() - c.y;
            if (dx * dx + dz * dz < 1.5 && Math.abs(dy) < 1.5) {
                this.folk.getNavigation().stop();
                this.stuck = 0;
                return;
            }
            if (--this.repath > 0 && !this.folk.getNavigation().isDone()) {
                return;
            }
            this.repath = 20;
            boolean ok = this.folk.getNavigation().moveTo(c.x, c.y, c.z, 0.8);
            if (!ok || this.folk.getNavigation().isDone()) {
                this.stuck++;
            }
            if (this.stuck >= 8 && this.folk.getRace() == FolkRace.BAT) {
                // Fly up to it.
                if (this.folk.level() instanceof net.minecraft.server.level.ServerLevel server) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, this.folk.getX(),
                            this.folk.getY() + 0.5, this.folk.getZ(), 4, 0.2, 0.2, 0.2, 0.01);
                }
                this.folk.getNavigation().stop();
                this.folk.moveTo(c.x, c.y, c.z, this.folk.getYRot(), 0.0F);
                this.stuck = 0;
            }
        }
    }

    static boolean sleepsInBed(FolkEntity e) {
        return scheduled(e) && e.getRace() != FolkRace.BAT;
    }

    static boolean accountant(FolkEntity e) {
        return e.getRace() == FolkRace.RAT && "accountant".equals(e.getVariantName());
    }

    /** Off to bed: the nearest free bed to home that this folk may use, and sleep in it till the hour comes round. */
    static class BedGoal extends Goal {
        private final FolkEntity folk;
        @Nullable private BlockPos bed;
        private int cooldown;
        private int giveUp;
        private int repath;

        BedGoal(FolkEntity folk) {
            this.folk = folk;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Nullable
        BlockPos bed() {
            return this.bed;
        }

        @Override
        public boolean canUse() {
            if (!sleepsInBed(this.folk) || phase(this.folk) != Phase.SLEEP || this.folk.getTradingPlayer() != null
                    || this.folk.getLastHurtByMob() != null || this.folk.getTarget() != null || --this.cooldown > 0) {
                return false;
            }
            this.cooldown = 200 + this.folk.getRandom().nextInt(200);
            this.bed = findBed();
            return this.bed != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.bed == null || this.giveUp <= 0 || this.folk.hurtTime > 0 || phase(this.folk) != Phase.SLEEP
                    || this.folk.getTarget() != null) {
                return false;
            }
            BlockState s = this.folk.level().getBlockState(this.bed);
            return s.getBlock() instanceof BedBlock && (this.folk.isSleeping() || !s.getValue(BedBlock.OCCUPIED));
        }

        @Override
        public void start() {
            this.giveUp = 900;
            this.repath = 0;
        }

        @Override
        public void stop() {
            if (this.folk.isSleeping()) {
                this.folk.stopSleeping();
            }
            this.bed = null;
        }

        @Override
        public void tick() {
            if (this.bed == null) {
                return;
            }
            if (this.folk.isSleeping()) {
                this.folk.getNavigation().stop();
                return;
            }
            this.giveUp--;
            Vec3 c = Vec3.atBottomCenterOf(this.bed);
            double dx = this.folk.getX() - c.x, dz = this.folk.getZ() - c.z;
            if (dx * dx + dz * dz < 3.0 && Math.abs(this.folk.getY() - c.y) < 2.6) {     // (an upper bunk too)
                this.folk.getNavigation().stop();
                this.folk.startSleeping(this.bed);
                this.giveUp = Integer.MAX_VALUE;
                return;
            }
            if (--this.repath <= 0 || this.folk.getNavigation().isDone()) {
                this.repath = 40;
                if (!this.folk.getNavigation().moveTo(c.x, c.y, c.z, 0.9)) {
                    // Too far to plan the whole way: head that way and try again.
                    Vec3 step = DefaultRandomPos.getPosTowards(this.folk, 12, 7, c, Math.PI / 2);
                    if (step != null) {
                        this.folk.getNavigation().moveTo(step.x, step.y, step.z, 0.9);
                    }
                }
            }
        }

        /** The nearest free bed to home this folk may sleep in (green beds are the accountants', and theirs only). */
        @Nullable
        private BlockPos findBed() {
            Level level = this.folk.level();
            BlockPos home = this.folk.homePos();
            BlockPos at = home != null ? home : this.folk.blockPosition();
            Set<BlockPos> taken = new HashSet<>();
            for (FolkEntity f : level.getEntitiesOfClass(FolkEntity.class, this.folk.getBoundingBox().inflate(48.0),
                    f -> f != this.folk && f.bedTarget() != null)) {
                taken.add(f.bedTarget());
            }
            boolean green = accountant(this.folk);
            int up = green ? 34 : 10;
            BlockPos best = null;
            double bestD = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, -8, -16), at.offset(16, up, 16))) {
                BlockState s = level.getBlockState(p);
                if (!(s.getBlock() instanceof BedBlock) || s.getValue(BedBlock.PART) != BedPart.HEAD
                        || s.getValue(BedBlock.OCCUPIED) || s.is(Blocks.GREEN_BED) != green || taken.contains(p)) {
                    continue;
                }
                double d = p.distSqr(at);
                if (d < bestD) {
                    best = p.immutable();
                    bestD = d;
                }
            }
            return best;
        }
    }

    /**
     * An adventurer leaving town: the city had more of its kind than it could keep, so off it goes into the world, a
     * long walk out, and then wherever it likes.
     */
    static class LeaveGoal extends Goal {
        private final FolkEntity folk;
        private int repath;

        LeaveGoal(FolkEntity folk) {
            this.folk = folk;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.folk.isAdventurer() && this.folk.wanderTarget != null && this.folk.getTarget() == null
                    && this.folk.getTradingPlayer() == null;
        }

        @Override
        public void tick() {
            BlockPos to = this.folk.wanderTarget;
            if (to == null) {
                return;
            }
            if (this.folk.blockPosition().distSqr(to) < 24 * 24
                    || this.folk.level().getGameTime() > this.folk.wanderUntil) {
                this.folk.wanderTarget = null;     // far enough: the world is its own now
                return;
            }
            if (--this.repath <= 0 || this.folk.getNavigation().isDone()) {
                this.repath = 60;
                Vec3 step = DefaultRandomPos.getPosTowards(this.folk, 16, 7, Vec3.atBottomCenterOf(to), Math.PI / 2);
                if (step != null) {
                    this.folk.getNavigation().moveTo(step.x, step.y, step.z, 0.9);
                }
            }
        }
    }
}
