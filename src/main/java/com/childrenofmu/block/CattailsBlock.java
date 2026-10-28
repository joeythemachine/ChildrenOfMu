package com.childrenofmu.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A clump of cattails for the edges of boardwalks and docks. Purely decorative; you walk through them. */
public class CattailsBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public CattailsBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
