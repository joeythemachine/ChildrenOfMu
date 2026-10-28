package com.childrenofmu.post;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Frog town's post box. Opening it shows what's been delivered here, and adds this town to the places the Jumper
 * couriers will carry your parcels to.
 */
public class PostBoxBlock extends Block {
    public PostBoxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            PostOffice office = PostOffice.get(server.getServer());
            PostOffice.Box box = office.boxAt(level.dimension().location(), pos, level.getRandom());
            boolean first = !office.visitedBy(player.getUUID(), box.dimension, pos).contains(box);
            office.visit(player.getUUID(), box);
            if (first) {
                player.sendSystemMessage(Component.translatable("post.childrenofmu.registered", box.town));
            }
            level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.6F, 1.3F);
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, box.contents),
                    Component.translatable("container.childrenofmu.post_box", box.town)));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
