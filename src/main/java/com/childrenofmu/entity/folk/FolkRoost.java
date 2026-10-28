package com.childrenofmu.entity.folk;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Bat folk sleep the day away in roosts. The city's towers have posts sticking out of their sides (fences), some
 * under little awnings for shade; at dawn every bat (the barman and the bookie too) flutters up to a free post and hangs
 * from it upside down, wings wrapped round, until dusk. Then they drop off and glide down for a night of Bazz.
 */
public final class FolkRoost {
    private FolkRoost() {}

    /** Day for bats: from just after dawn until dusk. */
    static boolean isDay(Level level) {
        long t = level.getDayTime() % 24000L;
        return t >= 23500L || t < 12300L;
    }

    /** Every bat roosts by day, the barman and the bookie too (the bar and the pit open at dusk). */
    static boolean roosts(FolkEntity e) {
        return e.getRace() == FolkRace.BAT && !e.isTraveller() && !e.pitFighter && !e.isAdventurer()
                && !FolkPit.isBookie(e);     // the bookie never leaves his booth: the book's always open
    }

    /** Roosting hours: the bat's sleep (see FolkWork), or plain daytime for a bat with no post. */
    static boolean roostTime(FolkEntity bat) {
        return bat.homePos() != null ? FolkWork.phase(bat) == FolkWork.Phase.SLEEP : isDay(bat.level());
    }

    static class RoostGoal extends Goal {
        private static final double FLIGHT_SPEED = 0.3;
        private final FolkEntity bat;
        @Nullable private BlockPos post;
        private int searchCooldown;
        private int giveUp;

        RoostGoal(FolkEntity bat) {
            this.bat = bat;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Nullable
        BlockPos post() {
            return this.post;
        }

        @Override
        public boolean canUse() {
            if (!roosts(this.bat) || !roostTime(this.bat) || this.bat.getTradingPlayer() != null
                    || this.bat.getLastHurtByMob() != null || this.bat.isPlaying()) {
                return false;
            }
            if (--this.searchCooldown > 0) {
                return false;
            }
            this.searchCooldown = 200 + this.bat.getRandom().nextInt(200);
            this.post = findPost();
            return this.post != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.post != null && roostTime(this.bat) && this.bat.hurtTime == 0
                    && this.bat.getTradingPlayer() == null && this.giveUp > 0
                    && this.bat.level().getBlockState(this.post).is(BlockTags.FENCES)
                    && this.bat.level().getBlockState(this.post.below()).isAir();
        }

        @Override
        public void start() {
            this.giveUp = 400;
            this.bat.getNavigation().stop();
            this.bat.setNoGravity(true);
            this.bat.noPhysics = true;
            this.bat.playSound(SoundEvents.BAT_TAKEOFF, 0.5F, 1.1F);
        }

        @Override
        public void stop() {
            boolean wasHanging = this.bat.isResting();
            this.bat.setResting(false);
            this.bat.setNoGravity(false);
            this.bat.noPhysics = false;
            this.post = null;
            // Drop off the post and glide down.
            this.bat.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
            if (wasHanging) {
                this.bat.playSound(SoundEvents.BAT_TAKEOFF, 0.5F, 0.9F);
            }
        }

        /** Where a bat hangs: its feet just under the post. */
        private Vec3 hangAt() {
            return new Vec3(this.post.getX() + 0.5, this.post.getY() - this.bat.getBbHeight() - 0.02, this.post.getZ() + 0.5);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.post == null) {
                return;
            }
            Vec3 target = hangAt();
            this.bat.setDeltaMovement(Vec3.ZERO);
            if (this.bat.isResting()) {
                this.bat.setPos(target.x, target.y, target.z);
                return;
            }
            this.giveUp--;
            Vec3 at = this.bat.position();
            Vec3 to = target.subtract(at);
            double d = to.length();
            if (d < FLIGHT_SPEED) {
                this.bat.moveTo(target.x, target.y, target.z, this.bat.getRandom().nextInt(4) * 90.0F, 0.0F);
                this.bat.setResting(true);
                this.giveUp = Integer.MAX_VALUE;
                return;
            }
            // Flutter straight there, with a little bob.
            Vec3 step = to.scale(FLIGHT_SPEED / d).add(0, Math.sin(this.bat.tickCount * 0.6) * 0.05, 0);
            this.bat.setPos(at.x + step.x, at.y + step.y, at.z + step.z);
            float yaw = (float) (Math.atan2(to.z, to.x) * 180.0 / Math.PI) - 90.0F;
            this.bat.setYRot(yaw);
            this.bat.yBodyRot = yaw;
            this.bat.yHeadRot = yaw;
            if (this.bat.tickCount % 4 == 0 && this.bat.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.5, at.z, 1, 0.1, 0.1, 0.1, 0.0);
                if (this.bat.tickCount % 12 == 0) {
                    this.bat.playSound(SoundEvents.PHANTOM_FLAP, 0.3F, 1.8F);
                }
            }
        }

        /**
         * Inside a building (a hall, a house): walls close by on every side and a ceiling overhead. Bats roost out of
         * doors, on the outsides of towers and under awnings, never on the railings and chandeliers indoors.
         */
        private static boolean indoors(Level level, BlockPos p) {
            boolean roof = false;
            for (int up = 1; up <= 6 && !roof; up++) {
                roof = level.getBlockState(p.above(up)).isSolidRender(level, p.above(up));
            }
            if (!roof) {
                return false;
            }
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                boolean closed = false;
                for (int n = 1; n <= 10 && !closed; n++) {
                    BlockPos q = p.relative(d, n);
                    closed = level.getBlockState(q).isSolidRender(level, q);
                }
                if (!closed) {
                    return false;      // open on this side: outdoors
                }
            }
            return true;
        }

        /** The nearest free roost post: a fence with open air beneath it, sticking out of a wall. */
        @Nullable
        private BlockPos findPost() {
            BlockPos at = this.bat.blockPosition();
            Level level = this.bat.level();
            Set<BlockPos> taken = new HashSet<>();
            for (FolkEntity f : level.getEntitiesOfClass(FolkEntity.class, this.bat.getBoundingBox().inflate(48.0),
                    f -> f != this.bat && f.getRace() == FolkRace.BAT)) {
                if (f.restingMat() != null) {
                    taken.add(f.restingMat());
                }
            }
            BlockPos best = null;
            double bestD = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-18, -2, -18), at.offset(18, 30, 18))) {
                double dd = p.distSqr(at);
                if (dd >= bestD || taken.contains(p) || !level.getBlockState(p).is(BlockTags.FENCES)
                        || !level.getBlockState(p.below()).isAir() || !level.getBlockState(p.below(2)).isAir()) {
                    continue;
                }
                // A post, not a fence on the ground: it must hang from a beam above, or stick out of a wall.
                boolean wall = level.getBlockState(p.above()).isSolidRender(level, p.above());
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    if (wall) {
                        break;
                    }
                    BlockPos side = p.relative(dir);
                    if (level.getBlockState(side).isSolidRender(level, side)) {
                        wall = true;
                        break;
                    }
                }
                if (wall && !indoors(level, p)) {
                    best = p.immutable();
                    bestD = dd;
                }
            }
            return best;
        }
    }
}
