package com.childrenofmu.entity.folk;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Everyday life in RCB City, below the Cats.
 *
 *  - Rat sweepers keep the streets clean: anything left lying in the street for a couple of minutes gets swept up
 *    and carted off (rats being rats, it isn't seen again).
 *  (Fallen citizens, cops included, are replaced by the census: see FolkCensus.)
 */
public final class FolkCity {
    /** How long an item lies in the street before a sweeper counts it as litter (ticks). */
    private static final int LITTER_AGE = 2400;

    private FolkCity() {}

    static boolean isSweeper(FolkEntity e) {
        return e.getRace() == FolkRace.RAT && "sweeper".equals(e.getVariantName());
    }

    /** A sweeper goes and sweeps up litter. */
    static class SweepGoal extends Goal {
        private final FolkEntity rat;
        @Nullable private ItemEntity litter;
        private int cooldown;
        private int giveUp;

        SweepGoal(FolkEntity rat) {
            this.rat = rat;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!isSweeper(this.rat) || this.rat.getTradingPlayer() != null || --this.cooldown > 0) {
                return false;
            }
            this.cooldown = 60;
            List<ItemEntity> items = this.rat.level().getEntitiesOfClass(ItemEntity.class,
                    this.rat.getBoundingBox().inflate(12.0, 4.0, 12.0), i -> i.isAlive() && i.getAge() > LITTER_AGE);
            this.litter = items.isEmpty() ? null : items.get(this.rat.getRandom().nextInt(items.size()));
            return this.litter != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.litter != null && this.litter.isAlive() && this.giveUp > 0;
        }

        @Override
        public void start() {
            this.giveUp = 300;
            this.rat.getNavigation().moveTo(this.litter, 0.9);
        }

        @Override
        public void tick() {
            if (this.litter == null) {
                return;     // swept up (the goal ends on the next check)
            }
            this.giveUp--;
            if (this.rat.distanceToSqr(this.litter) < 2.25) {
                if (this.rat.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.POOF, this.litter.getX(), this.litter.getY(), this.litter.getZ(),
                            4, 0.2, 0.1, 0.2, 0.01);
                }
                this.rat.playSound(SoundEvents.BRUSH_GENERIC, 0.8F, 1.0F);
                this.rat.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                this.litter.discard();
                this.litter = null;
            } else if (this.rat.getNavigation().isDone()) {
                this.rat.getNavigation().moveTo(this.litter, 0.9);
            }
        }
    }
}
