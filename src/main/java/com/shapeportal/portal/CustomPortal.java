package com.shapeportal.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** A closed flat region (any shape) surrounded by obsidian / crying obsidian. */
public final class CustomPortal {
    public static final int MAX_CELLS = 1024;
    public static final int MIN_CELLS = 2;
    public static final int MAX_SIZE = 40;

    private static final int[] DU = {1, -1, 0, 0};
    private static final int[] DV = {0, 0, 1, -1};

    public final Direction.Axis axis;
    public final int originU, originV, fixed;
    /** interior cells, relative to (originU, originV) */
    public final int[] du, dv;
    /** frame cells (4-neighbours of the interior), relative, with their block */
    public final int[] fdu, fdv;
    public final Block[] fblock;
    public final int width, height;

    private CustomPortal(Direction.Axis axis, int originU, int originV, int fixed,
                         List<Long> cells, Map<Long, Block> frame, int width, int height) {
        this.axis = axis;
        this.originU = originU;
        this.originV = originV;
        this.fixed = fixed;
        this.width = width;
        this.height = height;
        du = new int[cells.size()];
        dv = new int[cells.size()];
        for (int i = 0; i < du.length; i++) {
            long k = cells.get(i);
            du[i] = PortalUtil.uOf(k) - originU;
            dv[i] = PortalUtil.vOf(k) - originV;
        }
        fdu = new int[frame.size()];
        fdv = new int[frame.size()];
        fblock = new Block[frame.size()];
        int i = 0;
        for (Map.Entry<Long, Block> e : frame.entrySet()) {
            fdu[i] = PortalUtil.uOf(e.getKey()) - originU;
            fdv[i] = PortalUtil.vOf(e.getKey()) - originV;
            fblock[i] = e.getValue();
            i++;
        }
    }

    /** Flood-fills the plane from {@code start}. Returns null if the region is not closed by a frame. */
    public static CustomPortal detect(LevelReader level, BlockPos start, Direction.Axis axis) {
        if (axis == Direction.Axis.Y) return null;
        if (!PortalUtil.isEmpty(level.getBlockState(start), axis)) return null;

        int fixed = PortalUtil.fixedOf(axis, start);
        int su = PortalUtil.uOf(axis, start), sv = start.getY();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        List<Long> cells = new ArrayList<>();
        Map<Long, Block> frame = new HashMap<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        long sk = PortalUtil.key(su, sv);
        visited.add(sk);
        queue.add(sk);
        int minU = su, maxU = su, minV = sv, maxV = sv;

        while (!queue.isEmpty()) {
            long k = queue.poll();
            cells.add(k);
            int u = PortalUtil.uOf(k), v = PortalUtil.vOf(k);
            for (int i = 0; i < 4; i++) {
                int nu = u + DU[i], nv = v + DV[i];
                long nk = PortalUtil.key(nu, nv);
                if (visited.contains(nk) || frame.containsKey(nk)) continue;
                PortalUtil.set(m, axis, nu, nv, fixed);
                if (level.isOutsideBuildHeight(m)) return null;
                BlockState s = level.getBlockState(m);
                if (PortalUtil.isEmpty(s, axis)) {
                    if (cells.size() + queue.size() >= MAX_CELLS) return null;
                    visited.add(nk);
                    queue.add(nk);
                    minU = Math.min(minU, nu); maxU = Math.max(maxU, nu);
                    minV = Math.min(minV, nv); maxV = Math.max(maxV, nv);
                    if (maxU - minU + 1 > MAX_SIZE || maxV - minV + 1 > MAX_SIZE) return null;
                } else if (PortalUtil.isFrame(s)) {
                    frame.put(nk, s.getBlock());
                } else {
                    return null; // leak
                }
            }
        }
        if (cells.size() < MIN_CELLS) return null;
        return new CustomPortal(axis, minU, minV, fixed, cells, frame, maxU - minU + 1, maxV - minV + 1);
    }

    /** Fills the interior with portal blocks (used when lighting). */
    public void ignite(Level level) {
        BlockState portal = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, axis);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i < du.length; i++) {
            PortalUtil.set(m, axis, originU + du[i], originV + dv[i], fixed);
            level.setBlock(m, portal, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
