package com.fallingthrough.event;

import net.minecraft.server.level.ServerLevel;

public class WorldUtil
{
    /**
     * Returns a dimensions max height
     *
     * @param dimensionType
     * @return
     */
    public static int getDimensionMaxHeight(final ServerLevel level)
    {
        return Math.min(level.getMaxBuildHeight(), level.getMinBuildHeight() + level.getLogicalHeight());
    }

    /**
     * Returns a dimension min height
     *
     * @param dimensionType
     * @return
     */
    public static int getDimensionMinHeight(final ServerLevel level)
    {
        return level.getMinBuildHeight();
    }
}
