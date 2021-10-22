package com.fallingthrough.config;

import com.fallingthrough.FallingthroughMod;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationCache
{
    public static Map<ResourceLocation, DimensionData> belowToNextDim = new HashMap<>();
    public static Map<ResourceLocation, DimensionData> aboveToNextDim = new HashMap<>();

    public static void parseConfig()
    {
        belowToNextDim = new HashMap<>();
        aboveToNextDim = new HashMap<>();

        for (final String data : FallingthroughMod.config.getCommonConfig().belowDimension.get())
        {
            parse(data, belowToNextDim);
        }

        for (final String data : FallingthroughMod.config.getCommonConfig().aboveDimension.get())
        {
            parse(data, aboveToNextDim);
        }
    }

    private static void parse(final String data, final Map<ResourceLocation, DimensionData> resultStorage)
    {
        final String[] splitData = data.split(";");

        if (splitData.length != 5)
        {
            FallingthroughMod.LOGGER.warn("Error parsing config entry, need 5 seperated entries with /; : " + data);
            return;
        }

        final ResourceLocation from = ResourceLocation.tryParse(splitData[0]);
        if (from == null)
        {
            FallingthroughMod.LOGGER.warn("Error parsing config first dimension: " + splitData[0]);
            return;
        }

        final DimensionData dimensionData = DimensionData.parse(splitData);
        if (dimensionData == null)
        {
            return;
        }

        resultStorage.put(from, dimensionData);
    }
}
