package com.childrenofmu.entity.folk;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * How the folk find their way: like any walker, but through wooden doors (they open them, and shut them behind
 * them: see the OpenDoorGoal in FolkEntity) and up and down ladders, so upstairs rooms, lofts, towers and the sewers
 * are all part of their world.
 */
public class FolkNavigation extends GroundPathNavigation {
    public FolkNavigation(Mob mob, Level level) {
        super(mob, level);
        this.setCanOpenDoors(true);
        this.setCanPassDoors(true);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new LadderNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanOpenDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    /** Walking, plus straight up a ladder (where there's room overhead) and straight down one. */
    static class LadderNodeEvaluator extends WalkNodeEvaluator {
        private static boolean climbable(BlockGetter level, BlockPos p) {
            return level.getBlockState(p).is(BlockTags.CLIMBABLE);
        }

        /** Room to be in: a ladder, or nothing to bump into. */
        private static boolean room(BlockGetter level, BlockPos p) {
            BlockState s = level.getBlockState(p);
            return s.is(BlockTags.CLIMBABLE) || s.getCollisionShape(level, p).isEmpty();
        }

        @Override
        public int getNeighbors(Node[] out, Node node) {
            int n = super.getNeighbors(out, node);
            BlockGetter level = this.currentContext.level();
            BlockPos here = new BlockPos(node.x, node.y, node.z);
            int tall = Math.max(1, (int) Math.ceil(this.mob.getBbHeight()));
            // Up: on a ladder (or with one starting just overhead), with room for the whole body a block higher.
            if ((climbable(level, here) || climbable(level, here.above())) && n < out.length) {
                boolean fits = true;
                for (int k = 1; k <= tall && fits; k++) {
                    fits = room(level, here.above(k));
                }
                if (fits) {
                    Node up = this.getNode(here.getX(), here.getY() + 1, here.getZ());
                    if (!up.closed) {
                        up.type = PathType.WALKABLE;
                        up.costMalus = Math.max(up.costMalus, 0.0F);
                        out[n++] = up;
                    }
                }
            }
            // Down: a ladder underfoot.
            if (climbable(level, here.below()) && n < out.length) {
                Node down = this.getNode(here.getX(), here.getY() - 1, here.getZ());
                if (!down.closed) {
                    down.type = PathType.WALKABLE;
                    down.costMalus = Math.max(down.costMalus, 0.0F);
                    out[n++] = down;
                }
            }
            return n;
        }
    }
}
