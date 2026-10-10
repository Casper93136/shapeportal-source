package com.shapeportal;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ShapePortals.MODID)
public class ShapePortals {
    public static final String MODID = "shapeportal";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ShapePortals() {
        LOGGER.info("Shape Portals loaded");
    }
}
