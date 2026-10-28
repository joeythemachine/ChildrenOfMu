package com.childrenofmu.entity.folk;

import java.util.EnumSet;

import javax.annotation.Nullable;

import com.childrenofmu.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Life on (and under) the streets of RCB City.
 *
 *  - Rats live in the sewers. A rat caught out under the open sky in the city runs, with purpose: home if it lives
 *    indoors, otherwise to the nearest manhole, and down it. Rats keep well clear of cats.
 *  - Cats (the ordinary sort: not the law, not the Gar) hunt rats they catch out in the open. And kill them.
 *  - Bats fly to get about: up to rooftops, decks and stages, and gliding down again. On the ground they walk.
 */
public final class FolkStreets {
    private FolkStreets() {}

    static boolean outdoors(FolkEntity e) {
        return e.level().canSeeSky(e.blockPosition().above()) && !e.isInWater();
    }

    /** A rat out in the open in the city. */
    static boolean exposedRat(FolkEntity e) {
        return e.getRace() == FolkRace.RAT && !e.pitFighter && !e.isTraveller() && e.getTradingPlayer() == null
                && e.homeRadius() > 3 && e.level() instanceof ServerLevel level && outdoors(e)
                && FolkLaw.inCity(level, e.blockPosition());
    }

    /** A cat that hunts: not the law, not the Gar, not in the pit. */
    static boolean hunter(FolkEntity e) {
        return e.getRace() == FolkRace.CAT && !e.isCop() && !FolkGar.isGar(e) && !e.pitFighter && !e.isTraveller()
                && e.getTradingPlayer() == null;
    }

    static void addGoals(FolkEntity e, GoalSelector goals, GoalSelector targets) {
        goals.addGoal(1, new AvoidEntityGoal<>(e, FolkEntity.class, 8.0F, 1.2, 1.6,
                t -> ((FolkEntity) t).getRace() == FolkRace.CAT && hunter((FolkEntity) t)) {
            @Override
            public boolean canUse() {
                return e.getRace() == FolkRace.RAT && !e.pitFighter && outdoors(e) && super.canUse();
            }
        });
        goals.addGoal(2, new ScurryGoal(e));
        goals.addGoal(2, new MeleeAttackGoal(e, 1.35, true) {
            @Override
            public boolean canUse() {
                return hunter(e) && e.getTarget() instanceof FolkEntity f && f.getRace() == FolkRace.RAT && super.canUse();
            }
        });
        targets.addGoal(4, new NearestAttackableTargetGoal<>(e, FolkEntity.class, 60, true, false,
                t -> t instanceof FolkEntity f && exposedRat(f)) {
            @Override
            public boolean canUse() {
                return hunter(e) && super.canUse();
            }
        });
        goals.addGoal(3, new FlyGoal(e));
        e.nestGoal = new NestGoal(e);
        goals.addGoal(3, e.nestGoal);
        goals.addGoal(4, new SurfaceGoal(e));
    }

    /** A bat's stage: a jukebox or note block within a few blocks. */
    static boolean nearStage(FolkEntity bat) {
        BlockPos at = bat.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-6, -2, -6), at.offset(6, 3, 6))) {
            net.minecraft.world.level.block.state.BlockState s = bat.level().getBlockState(p);
            if (s.is(net.minecraft.world.level.block.Blocks.JUKEBOX) || s.is(net.minecraft.world.level.block.Blocks.NOTE_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    /** Underground: no sky overhead and a roof of something solid not far up (a sewer, a cellar). */
    static boolean underground(FolkEntity e) {
        return !e.level().canSeeSky(e.blockPosition().above());
    }

    /** Rats that live in the sewers (not the clerks in the Justice Hall, not the beautiful ones, not the jailbird). */
    static boolean sewerRat(FolkEntity e) {
        if (e.getRace() != FolkRace.RAT || e.pitFighter || e.isTraveller()) {
            return false;
        }
        String v = e.getVariantName();
        return !"accountant".equals(v) && !"beautiful".equals(v) && e.homeRadius() > 3;
    }

    // ---- Rats sleep in piles --------------------------------------------------------------------

    /**
     * By day (when the cats are about) sewer rats curl up in the nests in the sewer walls (hay, out of the sky): up to
     * three to a nest, piled on top of one another.
     */
    static class NestGoal extends Goal {
        private static final double[][] PILE = {{0.0, 0.0, 0.0}, {0.22, 0.32, -0.15}, {-0.18, 0.62, 0.12}};
        private final FolkEntity rat;
        @Nullable private BlockPos nest;
        private int slot;
        private int cooldown;
        private int giveUp;

        NestGoal(FolkEntity rat) {
            this.rat = rat;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        static boolean sleepyTime(Level level) {
            long t = level.getDayTime() % 24000L;
            return t >= 1000L && t < 11500L;
        }

        @Override
        public boolean canUse() {
            if (!sewerRat(this.rat) || !sleepyTime(this.rat.level()) || this.rat.getTradingPlayer() != null
                    || this.rat.getLastHurtByMob() != null || !underground(this.rat) || --this.cooldown > 0) {
                return false;
            }
            this.cooldown = 160 + this.rat.getRandom().nextInt(160);
            return findNest();
        }

        private boolean findNest() {
            Level level = this.rat.level();
            BlockPos at = this.rat.blockPosition();
            BlockPos best = null;
            int bestSlot = 0;
            double bestD = Double.MAX_VALUE;
            java.util.List<FolkEntity> others = level.getEntitiesOfClass(FolkEntity.class, this.rat.getBoundingBox().inflate(24.0),
                    f -> f != this.rat && f.getRace() == FolkRace.RAT && f.restingMat() != null);
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, -3, -16), at.offset(16, 3, 16))) {
                double d = p.distSqr(at);
                if (d >= bestD || !level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.HAY_BLOCK)
                        || !(level.getBlockState(p.above()).isAir()
                             || level.getBlockState(p.above()).is(net.minecraft.tags.BlockTags.WOOL_CARPETS))
                        || level.canSeeSky(p.above())) {
                    continue;
                }
                int used = 0;
                for (FolkEntity f : others) {
                    if (p.above().equals(f.restingMat())) {
                        used++;
                    }
                }
                if (used < PILE.length) {
                    best = p.above().immutable();
                    bestSlot = used;
                    bestD = d;
                }
            }
            this.nest = best;
            this.slot = bestSlot;
            return best != null;
        }

        @Nullable
        BlockPos nest() {
            return this.nest;
        }

        @Override
        public boolean canContinueToUse() {
            return this.nest != null && sleepyTime(this.rat.level()) && this.rat.hurtTime == 0 && this.giveUp > 0
                    && this.rat.getLastHurtByMob() == null
                    && this.rat.level().getBlockState(this.nest.below()).is(net.minecraft.world.level.block.Blocks.HAY_BLOCK);
        }

        @Override
        public void start() {
            this.giveUp = 400;
            this.rat.getNavigation().moveTo(this.nest.getX() + 0.5, this.nest.getY(), this.nest.getZ() + 0.5, 1.0);
        }

        @Override
        public void stop() {
            if (this.rat.isResting()) {
                this.rat.setResting(false);
                this.rat.setNoGravity(false);
                this.rat.noPhysics = false;
            }
            this.nest = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.nest == null) {
                return;
            }
            double[] off = PILE[this.slot];
            if (this.rat.isResting()) {
                this.rat.setDeltaMovement(Vec3.ZERO);
                this.rat.setPos(this.nest.getX() + 0.5 + off[0], this.nest.getY() + off[1], this.nest.getZ() + 0.5 + off[2]);
                return;
            }
            this.giveUp--;
            double dx = this.rat.getX() - (this.nest.getX() + 0.5), dz = this.rat.getZ() - (this.nest.getZ() + 0.5);
            if (dx * dx + dz * dz < 1.0 && Math.abs(this.rat.getY() - this.nest.getY()) < 1.2) {
                this.rat.getNavigation().stop();
                this.rat.setNoGravity(true);
                this.rat.noPhysics = true;
                this.rat.moveTo(this.nest.getX() + 0.5 + off[0], this.nest.getY() + off[1], this.nest.getZ() + 0.5 + off[2],
                        this.rat.getRandom().nextInt(4) * 90.0F + this.slot * 35.0F, 0.0F);
                this.rat.setResting(true);
                this.giveUp = Integer.MAX_VALUE;
            } else if (this.rat.getNavigation().isDone()) {
                this.rat.getNavigation().moveTo(this.nest.getX() + 0.5, this.nest.getY(), this.nest.getZ() + 0.5, 1.0);
            }
        }
    }

    // ---- Rats on errands up top ------------------------------------------------------------------

    /**
     * Now and then at night a sewer rat standing at the foot of a manhole's ladder pops up into the street, and then
     * (see ScurryGoal) dashes off to a different manhole and down again.
     */
    static class SurfaceGoal extends Goal {
        private final FolkEntity rat;
        private int cooldown = 600;

        SurfaceGoal(FolkEntity rat) {
            this.rat = rat;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!sewerRat(this.rat) || this.rat.isResting() || NestGoal.sleepyTime(this.rat.level()) || !underground(this.rat)
                    || --this.cooldown > 0) {
                return false;
            }
            this.cooldown = 1200 + this.rat.getRandom().nextInt(2400);
            return this.rat.getRandom().nextInt(3) == 0;
        }

        @Override
        public void start() {
            Level level = this.rat.level();
            BlockPos at = this.rat.blockPosition();
            // The nearest ladder within a few blocks, and up it to the manhole.
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-6, -1, -6), at.offset(6, 2, 6))) {
                if (!level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.LADDER)) {
                    continue;
                }
                BlockPos.MutableBlockPos q = p.mutable();
                for (int n = 0; n < 24 && level.getBlockState(q).is(net.minecraft.world.level.block.Blocks.LADDER); n++) {
                    q.move(0, 1, 0);
                }
                if (!level.getBlockState(q).is(ModBlocks.MANHOLE.get())) {
                    continue;
                }
                BlockPos out = q.above();
                if (!level.getBlockState(out).isAir() || !level.getBlockState(out.above()).isAir()) {
                    return;
                }
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.POOF, out.getX() + 0.5, out.getY() + 0.2, out.getZ() + 0.5, 5, 0.2, 0.1, 0.2, 0.01);
                }
                this.rat.playSound(SoundEvents.IRON_TRAPDOOR_OPEN, 0.6F, 1.3F);
                this.rat.moveTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5, this.rat.getYRot(), 0.0F);
                this.rat.surfacedAt = q.immutable();
                this.rat.setHome(out, 48);
                return;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    // ---- Rats run for it ------------------------------------------------------------------------

    /** A rat out in the open makes for home (if home is indoors) or the nearest manhole, and drops down it. */
    static class ScurryGoal extends Goal {
        private final FolkEntity rat;
        @Nullable private BlockPos goal;
        private boolean manhole;
        private int cooldown;
        private int giveUp;
        @Nullable private Vec3 lastPos;

        ScurryGoal(FolkEntity rat) {
            this.rat = rat;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.rat.getRace() != FolkRace.RAT || --this.cooldown > 0) {
                return false;
            }
            this.cooldown = 40;
            if (!exposedRat(this.rat)) {
                return false;
            }
            BlockPos home = this.rat.homePos();
            if (home != null && !this.rat.level().canSeeSky(home.above()) && home.distSqr(this.rat.blockPosition()) < 40 * 40) {
                this.goal = home;
                this.manhole = false;
                return true;
            }
            this.goal = nearestManhole();
            this.manhole = true;
            return this.goal != null;
        }

        @Nullable
        private BlockPos nearestManhole() {
            BlockPos at = this.rat.blockPosition();
            Level level = this.rat.level();
            BlockPos best = null;
            double bestD = Double.MAX_VALUE;
            BlockPos came = this.rat.surfacedAt;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-28, -4, -28), at.offset(28, 4, 28))) {
                double d = p.distSqr(at);
                if (d < bestD && level.getBlockState(p).is(ModBlocks.MANHOLE.get())
                        && (came == null || p.distSqr(came) > 64)) {
                    best = p.immutable();
                    bestD = d;
                }
            }
            return best;
        }

        @Override
        public boolean canContinueToUse() {
            return this.goal != null && this.giveUp > 0 && (this.manhole || outdoors(this.rat));
        }

        @Override
        public void start() {
            this.giveUp = 400;
            this.lastPos = null;
            this.rat.setSprinting(true);
            move();
        }

        @Override
        public void stop() {
            this.rat.setSprinting(false);
            if (this.giveUp <= 0) {
                this.cooldown = 600 + this.rat.getRandom().nextInt(600);   // couldn't get there: calm down a while
            }
            this.goal = null;
        }

        private void move() {
            if (!this.rat.getNavigation().moveTo(this.goal.getX() + 0.5, this.goal.getY(), this.goal.getZ() + 0.5, 1.45)) {
                this.giveUp -= 100;    // no way there from here
            }
        }

        @Override
        public void tick() {
            if (this.goal == null) {
                return;     // already down the manhole (the goal ends on the next check)
            }
            this.giveUp--;
            if (this.rat.tickCount % 20 == 0) {
                // Running on the spot (in a hole, against a wall): give it up sooner.
                if (this.lastPos != null && this.rat.position().distanceToSqr(this.lastPos) < 0.3) {
                    this.giveUp -= 60;
                }
                this.lastPos = this.rat.position();
            }
            double dx = this.rat.getX() - (this.goal.getX() + 0.5), dz = this.rat.getZ() - (this.goal.getZ() + 0.5);
            if (this.manhole && dx * dx + dz * dz < 2.5 && Math.abs(this.rat.getY() - this.goal.getY()) < 2.5) {
                dropIn();
                this.goal = null;
                return;
            }
            if (this.rat.getNavigation().isDone()) {
                move();
            }
        }

        /** Down the manhole: to the sewer floor at the foot of its ladder. It lives down there now. */
        private void dropIn() {
            Level level = this.rat.level();
            BlockPos.MutableBlockPos p = this.goal.mutable().move(0, -1, 0);
            for (int n = 0; n < 24 && level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.LADDER); n++) {
                p.move(0, -1, 0);
            }
            // p: the first block under the ladder. Stand on the floor at the ladder's foot (not inside the floor)...
            BlockPos stand = null;
            if (level.getBlockState(p).isSolid()) {
                stand = p.above();
            } else {
                for (int n = 0; n < 8; n++, p.move(0, -1, 0)) {
                    if (level.getBlockState(p.below()).isSolid()) {
                        stand = p.immutable();
                        break;
                    }
                }
            }
            if (stand == null) {
                return;    // not a proper shaft: never mind
            }
            // ...and step off the ladder into the tunnel, so the shaft stays clear for the next rat.
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos q = stand.relative(d);
                if (level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir()
                        && level.getBlockState(q.below()).isSolid()) {
                    stand = q;
                    break;
                }
            }
            p.set(stand);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.POOF, this.rat.getX(), this.rat.getY() + 0.3, this.rat.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
            }
            this.rat.playSound(SoundEvents.IRON_TRAPDOOR_OPEN, 0.6F, 1.3F);
            this.rat.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, this.rat.getYRot(), 0.0F);
            this.rat.getNavigation().stop();
            this.rat.setHome(p.immutable(), 24);
            this.rat.surfacedAt = null;
        }
    }

    // ---- Bats fly ---------------------------------------------------------------------------------

    /** Now and then a bat flaps up to somewhere high (a roof, a deck, a stage), or glides down from one. */
    static class FlyGoal extends Goal {
        private static final double SPEED = 0.32;
        private final FolkEntity bat;
        @Nullable private Vec3 target;
        private int cooldown = 200;
        private int giveUp;

        FlyGoal(FolkEntity bat) {
            this.bat = bat;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.bat.getRace() != FolkRace.BAT || this.bat.isResting() || this.bat.isPlaying() || this.bat.pitFighter
                    || this.bat.getTradingPlayer() != null || "barman".equals(this.bat.getVariantName())
                    || "bookie".equals(this.bat.getVariantName()) || FolkWork.keepsPost(this.bat) || --this.cooldown > 0) {
                return false;
            }
            this.cooldown = this.bat.level().isNight() ? 80 + this.bat.getRandom().nextInt(160)
                    : 600 + this.bat.getRandom().nextInt(1200);
            this.target = pick();
            return this.target != null;
        }

        /** A spot to land on, well above or below where the bat is: solid underfoot, two blocks of air, open sky. */
        @Nullable
        private Vec3 pick() {
            Level level = this.bat.level();
            BlockPos at = this.bat.blockPosition();
            boolean up = this.bat.onGround() && this.bat.getRandom().nextInt(3) != 0;
            BlockPos home = this.bat.homePos();
            BlockPos base = home != null && home.distSqr(at) > 400 ? home : at;
            for (int i = 0; i < 24; i++) {
                int x = base.getX() + this.bat.getRandom().nextInt(25) - 12;
                int z = base.getZ() + this.bat.getRandom().nextInt(25) - 12;
                int y0 = base.getY() + (up ? 3 + this.bat.getRandom().nextInt(16) : -12 + this.bat.getRandom().nextInt(10));
                for (int y = y0; y > y0 - 6; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (level.getBlockState(p.below()).isSolidRender(level, p.below()) && level.getBlockState(p).isAir()
                            && level.getBlockState(p.above()).isAir() && level.canSeeSky(p)
                            && Math.abs(y - at.getY()) >= 3) {
                        return Vec3.atBottomCenterOf(p);
                    }
                }
            }
            return null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.giveUp > 0 && this.bat.hurtTime == 0 && !this.bat.isResting();
        }

        @Override
        public void start() {
            this.giveUp = 300;
            this.bat.getNavigation().stop();
            this.bat.setNoGravity(true);
            this.bat.noPhysics = true;
            this.bat.playSound(SoundEvents.BAT_TAKEOFF, 0.5F, 1.1F);
        }

        @Override
        public void stop() {
            this.bat.setNoGravity(false);
            this.bat.noPhysics = false;
            this.target = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.target == null) {
                return;     // landed (the goal ends on the next check)
            }
            this.giveUp--;
            Vec3 at = this.bat.position();
            Vec3 to = this.target.subtract(at);
            double d = to.length();
            this.bat.setDeltaMovement(Vec3.ZERO);
            if (d < SPEED) {
                this.bat.moveTo(this.target.x, this.target.y, this.target.z, this.bat.getYRot(), 0.0F);
                this.target = null;
                return;
            }
            // Rise first, then across, then settle: flap up steeply, glide down gently.
            Vec3 step = to.scale(SPEED / d).add(0, Math.sin(this.bat.tickCount * 0.6) * 0.04, 0);
            this.bat.setPos(at.x + step.x, at.y + step.y, at.z + step.z);
            float yaw = (float) (Math.atan2(to.z, to.x) * 180.0 / Math.PI) - 90.0F;
            this.bat.setYRot(yaw);
            this.bat.yBodyRot = yaw;
            this.bat.yHeadRot = yaw;
            if (this.bat.tickCount % 10 == 0) {
                this.bat.playSound(SoundEvents.PHANTOM_FLAP, 0.25F, 1.9F);
            }
        }
    }
}
