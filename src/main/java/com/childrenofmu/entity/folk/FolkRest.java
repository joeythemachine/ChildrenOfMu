package com.childrenofmu.entity.folk;

import java.util.EnumSet;

import com.childrenofmu.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

/**
 * How the Frog folk spend their days and nights.
 *
 * Night: every frog but the Guards finds a free Reed Mat (in the Torpor House, or at home), hops onto it, tucks in its
 * head and limbs and goes torpid until morning. Being hurt wakes it.
 *
 * Day: froglets go to school. In school hours they keep close to the Schoolmarm, wherever she is.
 */
public final class FolkRest {
    private FolkRest() {}

    /** Night for frogs: from dusk until just before dawn. */
    static boolean isNight(Level level) {
        long t = level.getDayTime() % 24000L;
        return t >= 12600L && t < 23300L;
    }

    /** School hours: mid-morning to mid-afternoon. */
    static boolean isSchoolTime(Level level) {
        long t = level.getDayTime() % 24000L;
        return t >= 1500L && t < 8500L;
    }

    static boolean sleeps(FolkEntity e) {
        return e.getRace() == FolkRace.FROG && !"guard".equals(e.getVariantName()) && !e.isTraveller();
    }

    /** Every few seconds, on a froglet: during school hours, stay by the Schoolmarm. */
    static void tickSchool(FolkEntity froglet) {
        if (froglet.tickCount % 100 != 0 || froglet.isResting()) {
            return;
        }
        if (isSchoolTime(froglet.level())) {
            FolkEntity teacher = null;
            double best = 64.0 * 64.0;
            for (FolkEntity f : froglet.level().getEntitiesOfClass(FolkEntity.class, froglet.getBoundingBox().inflate(64.0),
                    f -> f.getRace() == FolkRace.FROG && "schoolmarm".equals(f.getVariantName()))) {
                double d = f.distanceToSqr(froglet);
                if (d < best) {
                    best = d;
                    teacher = f;
                }
            }
            if (teacher != null) {
                froglet.restrictTo(teacher.blockPosition(), 5);
                froglet.atSchool = true;
                return;
            }
        }
        if (froglet.atSchool) {
            froglet.atSchool = false;
            froglet.restoreHome();
        }
    }

    /** Find a free Reed Mat nearby, go to it, and rest there till morning. */
    static class SleepGoal extends Goal {
        private final FolkEntity frog;
        private BlockPos mat;
        private int searchCooldown;
        private int giveUp;

        SleepGoal(FolkEntity frog) {
            this.frog = frog;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!sleeps(this.frog) || !isNight(this.frog.level()) || this.frog.getTradingPlayer() != null
                    || this.frog.getLastHurtByMob() != null) {
                return false;
            }
            if (--this.searchCooldown > 0) {
                return false;
            }
            this.searchCooldown = 200 + this.frog.getRandom().nextInt(200);
            this.mat = findMat();
            return this.mat != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.mat != null && isNight(this.frog.level()) && this.frog.hurtTime == 0
                    && this.frog.getLastHurtByMob() == null && this.frog.getTradingPlayer() == null
                    && this.frog.level().getBlockState(this.mat).is(ModBlocks.REED_MAT.get()) && this.giveUp > 0;
        }

        @Override
        public void start() {
            this.giveUp = 600;
            moveToMat();
        }

        @Override
        public void stop() {
            this.frog.setResting(false);
            this.mat = null;
        }

        private void moveToMat() {
            this.frog.getNavigation().moveTo(this.mat.getX() + 0.5, this.mat.getY(), this.mat.getZ() + 0.5, 1.0);
        }

        @Override
        public void tick() {
            if (this.mat == null) {
                return;
            }
            if (this.frog.isResting()) {
                this.frog.getNavigation().stop();
                this.frog.setDeltaMovement(0, this.frog.getDeltaMovement().y, 0);
                return;
            }
            this.giveUp--;
            double dx = this.frog.getX() - (this.mat.getX() + 0.5), dz = this.frog.getZ() - (this.mat.getZ() + 0.5);
            if (dx * dx + dz * dz < 0.8 && Math.abs(this.frog.getY() - this.mat.getY()) < 1.0) {
                // Settle onto the mat: centred, facing a random way, and still.
                this.frog.getNavigation().stop();
                this.frog.moveTo(this.mat.getX() + 0.5, this.mat.getY(), this.mat.getZ() + 0.5,
                        this.frog.getRandom().nextInt(4) * 90.0F, 0.0F);
                this.frog.setResting(true);
                this.giveUp = Integer.MAX_VALUE;
            } else if (this.frog.getNavigation().isDone()) {
                moveToMat();
            }
        }

        /** The nearest Reed Mat with nobody on it or heading for it. */
        private BlockPos findMat() {
            BlockPos at = this.frog.blockPosition();
            java.util.Set<BlockPos> taken = new java.util.HashSet<>();
            for (FolkEntity f : this.frog.level().getEntitiesOfClass(FolkEntity.class, this.frog.getBoundingBox().inflate(56.0),
                    f -> f != this.frog)) {
                taken.add(f.blockPosition());
                if (f.restingMat() != null) {
                    taken.add(f.restingMat());
                }
            }
            BlockPos best = null;
            double bestD = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-28, -3, -28), at.offset(28, 3, 28))) {
                double d = p.distSqr(at);
                if (d < bestD && !taken.contains(p) && this.frog.level().getBlockState(p).is(ModBlocks.REED_MAT.get())) {
                    best = p.immutable();
                    bestD = d;
                }
            }
            return best;
        }

        BlockPos mat() {
            return this.mat;
        }
    }
}
