package com.shapeportal.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Remembers which source portal the currently teleporting entity is using. */
public final class PortalContext {
    private static ServerLevel level;
    private static BlockPos entrance;

    private PortalContext() {}

    public static void begin(Level l, BlockPos pos) {
        if (l instanceof ServerLevel s && pos != null) {
            level = s;
            entrance = pos;
        } else {
            end();
        }
    }

    public static void end() { level = null; entrance = null; }
    public static ServerLevel level() { return level; }
    public static BlockPos entrance() { return entrance; }
}
