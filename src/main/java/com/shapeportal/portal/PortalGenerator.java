package com.shapeportal.portal;

import com.shapeportal.ShapePortals;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Builds a copy of the source portal in the destination dimension.
 * Shape and axis are always kept; instead of deforming the shape, the generator searches
 * (within RADIUS of the scaled coordinates) for the spot where the fewest blocks have to
 * be removed, and carves the terrain there.
 */
public final class PortalGenerator {
    private static final int RADIUS = 16;
    private static final double INF = Double.POSITIVE_INFINITY;

    private PortalGenerator() {}

    public static Optional<BlockUtil.FoundRectangle> generate(ServerLevel src, BlockPos entrance,
                                                              ServerLevel dst, BlockPos target,
                                                              Direction.Axis axis) {
        if (!src.getBlockState(entrance).is(Blocks.NETHER_PORTAL)) return Optional.empty();
        CustomPortal shape = CustomPortal.detect(src, entrance, axis);
        if (shape == null) return Optional.empty();

        int floorY = dst.getMinBuildHeight() + 1;
        int ceilY = Math.min(dst.getMaxBuildHeight(), dst.getMinBuildHeight() + dst.dimensionType().logicalHeight()) - 2;
        if (shape.height + 2 > ceilY - floorY) return Optional.empty(); // does not fit vertically

        // anchor: bottom-row cell closest to the horizontal centre
        double avg = 0;
        for (int d : shape.du) avg += d;
        avg /= shape.du.length;
        int baseDu = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < shape.du.length; i++) {
            if (shape.dv[i] != 0) continue;
            double d = Math.abs(shape.du[i] - avg);
            if (d < bestD) { bestD = d; baseDu = shape.du[i]; }
        }

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        double bestScore = Double.MAX_VALUE;
        int bestOu = 0, bestOy = 0, bestFixed = 0;
        boolean found = false;

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > RADIUS || dist * 0.3 >= bestScore) continue;
                int cx = target.getX() + dx, cz = target.getZ() + dz;
                if (!dst.getWorldBorder().isWithinBounds(cx, cz)) continue;

                for (int y = ceilY; y > floorY; y--) {
                    if (!dst.getBlockState(m.set(cx, y, cz)).isAir()) continue;
                    BlockState below = dst.getBlockState(m.set(cx, y - 1, cz));
                    if (!below.blocksMotion() || !below.getFluidState().isEmpty()) continue;

                    int uCoord = axis == Direction.Axis.X ? cx : cz;
                    int fixed = axis == Direction.Axis.X ? cz : cx;
                    int ou = uCoord - baseDu;
                    double extra = dist * 0.3 + Math.abs(y - target.getY()) * 0.05;
                    double limit = bestScore == Double.MAX_VALUE ? Double.MAX_VALUE : (bestScore - extra) / 2.0;
                    double cost = evaluate(dst, shape, ou, y, fixed, floorY, ceilY, limit);
                    if (cost == INF) continue;
                    double score = cost * 2.0 + extra;
                    if (score < bestScore) {
                        bestScore = score; bestOu = ou; bestOy = y; bestFixed = fixed; found = true;
                    }
                }
            }
        }
        if (!found) return Optional.empty();

        place(dst, shape, bestOu, bestOy, bestFixed);
        ShapePortals.LOGGER.debug("Generated {}x{} portal ({} cells) near {}", shape.width, shape.height, shape.du.length, target);

        BlockPos center = PortalUtil.set(new BlockPos.MutableBlockPos(), axis, bestOu + baseDu, bestOy, bestFixed).immutable();
        return Optional.of(BlockUtil.getLargestRectangleAround(center, axis, 21, Direction.Axis.Y, 21,
            p -> dst.getBlockState(p).is(Blocks.NETHER_PORTAL)));
    }

    /** -1 = unbreakable (position invalid). */
    private static double blockCost(ServerLevel l, BlockPos.MutableBlockPos m) {
        BlockState s = l.getBlockState(m);
        if (s.isAir()) return 0;
        if (s.getDestroySpeed(l, m) < 0) return -1;
        if (PortalUtil.isFrame(s)) return 0;
        if (s.hasBlockEntity()) return 6;
        if (!s.getFluidState().isEmpty()) return 0.5;
        if (s.canBeReplaced()) return 0.1;
        return 1;
    }

    private static double evaluate(ServerLevel l, CustomPortal s, int ou, int oy, int fixed,
                                   int floorY, int ceilY, double limit) {
        double cost = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i < s.du.length; i++) {
            int y = oy + s.dv[i];
            if (y < floorY || y > ceilY) return INF;
            for (int n = -1; n <= 1; n++) {
                PortalUtil.set(m, s.axis, ou + s.du[i], y, fixed + n);
                double c = blockCost(l, m);
                if (c < 0) return INF;
                cost += n == 0 ? c : c * 0.7;
                if (cost > limit) return INF;
            }
        }
        for (int i = 0; i < s.fdu.length; i++) {
            int y = oy + s.fdv[i];
            if (y < floorY || y > ceilY) return INF;
            PortalUtil.set(m, s.axis, ou + s.fdu[i], y, fixed);
            double c = blockCost(l, m);
            if (c < 0) return INF;
            cost += c;
            if (cost > limit) return INF;
        }
        return cost;
    }

    private static void place(ServerLevel l, CustomPortal s, int ou, int oy, int fixed) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();

        // clear space in front of / behind the portal + obsidian platform
        for (int i = 0; i < s.du.length; i++) {
            int y = oy + s.dv[i];
            for (int n = -1; n <= 1; n += 2) {
                PortalUtil.set(m, s.axis, ou + s.du[i], y, fixed + n);
                if (!l.getBlockState(m).isAir()) l.setBlock(m, air, Block.UPDATE_ALL);
                if (s.dv[i] == 0) {
                    BlockPos below = m.below();
                    BlockState bs = l.getBlockState(below);
                    if (!bs.blocksMotion() || !bs.getFluidState().isEmpty()) {
                        l.setBlock(below, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
        // frame (copies obsidian / crying obsidian from the source)
        for (int i = 0; i < s.fdu.length; i++) {
            PortalUtil.set(m, s.axis, ou + s.fdu[i], oy + s.fdv[i], fixed);
            l.setBlock(m, s.fblock[i].defaultBlockState(), Block.UPDATE_ALL);
        }
        // interior
        BlockState portal = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, s.axis);
        for (int i = 0; i < s.du.length; i++) {
            PortalUtil.set(m, s.axis, ou + s.du[i], oy + s.dv[i], fixed);
            l.setBlock(m, portal, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
