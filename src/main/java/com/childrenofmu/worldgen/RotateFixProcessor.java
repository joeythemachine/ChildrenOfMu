package com.childrenofmu.worldgen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Some modded blocks (Quark's posts, say) don't turn when a structure is placed turned: a post sticking straight out of
 * a wall ends up lying along it. For any block that ignores the rotation, turn its axis and its side connections here,
 * before the template places it (the template then "rotates" it, which does nothing).
 */
public class RotateFixProcessor extends StructureProcessor {
    public static final RotateFixProcessor INSTANCE = new RotateFixProcessor();

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
                                                             StructureTemplate.StructureBlockInfo original,
                                                             StructureTemplate.StructureBlockInfo current,
                                                             StructurePlaceSettings settings) {
        Rotation rot = settings.getRotation();
        BlockState state = current.state();
        if (rot == Rotation.NONE || state.getProperties().isEmpty() || !state.rotate(rot).equals(state)) {
            return current;
        }
        BlockState out = state;
        boolean quarter = rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90;
        for (Property<?> p : state.getProperties()) {
            String name = p.getName();
            if (name.equals("axis") && quarter) {
                out = swapAxis(out, p);
            }
        }
        // Side connections (connect_north, ...): each moves round to the side it now faces.
        BlockState conn = out;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            Property<?> from = state.getBlock().getStateDefinition().getProperty("connect_" + d.getName());
            Property<?> to = state.getBlock().getStateDefinition().getProperty("connect_" + rot.rotate(d).getName());
            if (from != null && to != null) {
                conn = copy(conn, to, state, from);
            }
        }
        out = conn;
        return out == state ? current : new StructureTemplate.StructureBlockInfo(current.pos(), out, current.nbt());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState swapAxis(BlockState s, Property p) {
        Comparable v = s.getValue(p);
        if (v == Direction.Axis.X && p.getPossibleValues().contains(Direction.Axis.Z)) {
            return s.setValue(p, Direction.Axis.Z);
        }
        if (v == Direction.Axis.Z && p.getPossibleValues().contains(Direction.Axis.X)) {
            return s.setValue(p, Direction.Axis.X);
        }
        return s;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copy(BlockState into, Property to, BlockState src, Property from) {
        Comparable v = src.getValue(from);
        return to.getPossibleValues().contains(v) ? into.setValue(to, v) : into;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.NOP;   // never saved: only ever added in code
    }
}
