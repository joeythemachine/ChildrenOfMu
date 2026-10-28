package com.childrenofmu.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A sliver of an Ithk focus crystal, attuned to weightlessness.
 * Hold right-click to drift slowly upward. While it's in your hand, you sink gently and take no fall damage.
 * Wears out with use; repair it with amethyst shards in an anvil.
 */
public class LevitationShardItem extends Item {
    /** Strength of the rise: 0 = Levitation I (1 block/s), 1 = Levitation II (2 blocks/s). */
    private static final int RISE_LEVEL = 1;
    /** Loses 1 durability per this many ticks of rising (10 = twice per second). */
    private static final int TICKS_PER_DAMAGE = 10;

    public LevitationShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.5F);
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        // Uses short, hidden vanilla effects, so movement stays smooth and servers don't mistake it for flying.
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 5, RISE_LEVEL, true, false, false));
        }
        entity.resetFallDistance();

        int ticksUsed = getUseDuration(stack, entity) - remainingUseDuration;
        if (level.isClientSide) {
            if (ticksUsed % 3 == 0) {
                level.addParticle(ParticleTypes.END_ROD,
                        entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 0.6, entity.getY(),
                        entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 0.6,
                        0.0, -0.03, 0.0);
            }
        } else if (ticksUsed > 0 && ticksUsed % TICKS_PER_DAMAGE == 0) {
            stack.hurtAndBreak(1, entity, LivingEntity.getSlotForHand(entity.getUsedItemHand()));
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide) {
            entity.removeEffect(MobEffects.LEVITATION);
        }
    }

    /** While it's in either hand (and you're not rising), you sink gently. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || living.isUsingItem()
                || living.onGround() || living.isFallFlying()) {
            return;
        }
        boolean inHand = selected || living.getOffhandItem() == stack;
        if (inHand && living.getDeltaMovement().y < 0) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, true, false, false));
        }
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.AMETHYST_SHARD);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.childrenofmu.levitation_shard.tooltip.0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.childrenofmu.levitation_shard.tooltip.1").withStyle(ChatFormatting.GRAY));
    }
}
