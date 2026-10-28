package com.childrenofmu.entity.folk;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Fighting in the swamp.
 *
 * Frog Guards carry bamboo blowpipes and spit poison darts at monsters threatening the town (never creepers: they
 * know better), and at anyone who hurts them. The other grown frogs hunt small slimes with their tongues, for the
 * slimeballs the town trades in. Spiders, for their part, hunt frogs.
 */
public final class FolkCombat {
    private FolkCombat() {}

    static boolean isGuard(FolkEntity e) {
        return e.getRace() == FolkRace.FROG && "guard".equals(e.getVariantName());
    }

    /** A Cat city archer: keeps the law like any guard, but from a distance, with a bow. */
    static boolean isArcher(FolkEntity e) {
        return e.getRace() == FolkRace.CAT && "archer".equals(e.getVariantName());
    }

    /** Grown frogs with ordinary jobs hunt small slimes. */
    static boolean huntsSlime(FolkEntity e) {
        if (e.getRace() != FolkRace.FROG || e.isResting()) {
            return false;
        }
        return switch (e.getVariantName()) {
            case "jumper", "fisher", "fishmonger", "plumber", "coppersmith", "potioneer" -> true;
            default -> false;
        };
    }

    static void addGoals(FolkEntity e, net.minecraft.world.entity.ai.goal.GoalSelector goals,
                         net.minecraft.world.entity.ai.goal.GoalSelector targets) {
        goals.addGoal(2, new RangedAttackGoal(e, 1.0, 30, 40, 14.0F) {
            @Override
            public boolean canUse() {
                return isGuard(e) && super.canUse();
            }
        });
        goals.addGoal(2, new RangedAttackGoal(e, 1.0, 25, 35, 20.0F) {
            @Override
            public boolean canUse() {
                return isArcher(e) && super.canUse();
            }
        });
        goals.addGoal(2, new MeleeAttackGoal(e, 1.2, true) {
            @Override
            public boolean canUse() {
                return (huntsSlime(e) || (e.pitFighter && !e.yielded)) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !e.yielded && super.canContinueToUse();
            }
        });
        targets.addGoal(1, new HurtByTargetGoal(e) {
            @Override
            public boolean canUse() {
                return isGuard(e) && super.canUse();
            }
        });
        targets.addGoal(2, new NearestAttackableTargetGoal<>(e, Mob.class, 10, true, false,
                t -> t instanceof Enemy && !(t instanceof Creeper)) {
            @Override
            public boolean canUse() {
                return isGuard(e) && super.canUse();
            }
        });
        targets.addGoal(3, new NearestAttackableTargetGoal<>(e, Slime.class, 20, true, false,
                t -> t.getType() == EntityType.SLIME && ((Slime) t).getSize() <= 2) {
            @Override
            public boolean canUse() {
                return huntsSlime(e) && super.canUse();
            }
        });
    }

    /** A Guard's poison dart, spat through a bamboo blowpipe. */
    static void shootDart(FolkEntity guard, LivingEntity target) {
        if (isArcher(guard)) {
            Arrow arrow = new Arrow(guard.level(), guard, new ItemStack(Items.ARROW), guard.getMainHandItem());
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.setBaseDamage(3.0);
            double dx = target.getX() - guard.getX(), dz = target.getZ() - guard.getZ();
            double dy = target.getY(0.33) - arrow.getY();
            arrow.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 1.6F, 4.0F);
            guard.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (guard.getRandom().nextFloat() * 0.4F + 0.8F));
            guard.level().addFreshEntity(arrow);
            return;
        }
        Arrow dart = new Arrow(guard.level(), guard, new ItemStack(Items.ARROW), null);
        dart.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
        dart.pickup = AbstractArrow.Pickup.DISALLOWED;
        dart.setBaseDamage(1.5);
        double dx = target.getX() - guard.getX();
        double dy = target.getY(0.33) - dart.getY();
        double dz = target.getZ() - guard.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        dart.shoot(dx, dy + flat * 0.15, dz, 1.5F, 6.0F);
        guard.playSound(SoundEvents.LLAMA_SPIT, 1.0F, 1.4F + guard.getRandom().nextFloat() * 0.3F);
        guard.level().addFreshEntity(dart);
    }

    /** Spiders hunt Frog folk, day or night. */
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof net.minecraft.world.entity.monster.AbstractIllager illager) {
            // Illagers and the Cat watch hate each other on sight.
            illager.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(illager, FolkEntity.class, 10, true, false,
                    t -> ((FolkEntity) t).isCop()));
        }
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Spider spider) {
            spider.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(spider, FolkEntity.class, 10, true, false,
                    t -> ((FolkEntity) t).getRace() == FolkRace.FROG));
        }
    }
}
