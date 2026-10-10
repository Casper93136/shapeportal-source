package com.shapeportal.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Helpers: a portal lives in a 2D plane (u = coordinate along the axis, v = Y). */
public final class PortalUtil {
    private PortalUtil() {}

    public static boolean isFrame(BlockState s) {
        return s.is(Blocks.OBSIDIAN) || s.is(Blocks.CRYING_OBSIDIAN);
    }

    public static boolean isEmpty(BlockState s, Direction.Axis axis) {
        return s.isAir() || s.is(BlockTags.FIRE)
            || (s.is(Blocks.NETHER_PORTAL) && s.getValue(NetherPortalBlock.AXIS) == axis);
    }

    public static long key(int u, int v) {
        return ((long) u << 32) | (v & 0xFFFFFFFFL);
    }

    public static int uOf(long k) { return (int) (k >> 32); }
    public static int vOf(long k) { return (int) k; }

    public static BlockPos.MutableBlockPos set(BlockPos.MutableBlockPos m, Direction.Axis axis, int u, int v, int fixed) {
        return axis == Direction.Axis.X ? m.set(u, v, fixed) : m.set(fixed, v, u);
    }

    public static int uOf(Direction.Axis axis, BlockPos p) {
        return axis == Direction.Axis.X ? p.getX() : p.getZ();
    }

    public static int fixedOf(Direction.Axis axis, BlockPos p) {
        return axis == Direction.Axis.X ? p.getZ() : p.getX();
    }
}
