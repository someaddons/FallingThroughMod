package com.fallingthrough.event;

import net.minecraft.world.level.dimension.DimensionType;

public class WorldUtil
{
    /**
     * Returns a dimensions max height
     * @param dimensionType
     * @return
     */
    public static int getDimensionMaxHeight(final DimensionType dimensionType)
    {
        return dimensionType.logicalHeight() + dimensionType.minY();
    }

    /**
     * Returns a dimension min height
     * @param dimensionType
     * @return
     */
    public static int getDimensionMinHeight(final DimensionType dimensionType)
    {
        return dimensionType.minY();
    }
}
