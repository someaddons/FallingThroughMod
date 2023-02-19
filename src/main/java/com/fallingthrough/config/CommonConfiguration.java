package com.fallingthrough.config;

import com.fallingthrough.FallingthroughMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.world.level.Level.*;

public class CommonConfiguration
{
    private final static String DIMENSIONCON = "dimensionconnections";

    public Map<ResourceLocation, List<DimensionData>> dimensionConnections = new HashMap<>();
    private List<DimensionData> dimensionDataList = new ArrayList<>();
    public boolean debuglogging = false;

    protected CommonConfiguration()
    {
        final DimensionData owToNether = new DimensionData(OVERWORLD.location(), Level.NETHER.location(), DimensionData.SPAWNTYPE.AIR);
        owToNether.belowY = -60;
        owToNether.xMult = 1.0 / 8;
        owToNether.zMult = 1.0 / 8;
        owToNether.slowFallDuration = 400;
        owToNether.teleportToYlevel = 125;
        dimensionDataList.add(owToNether);

        final DimensionData endToOw = new DimensionData(END.location(), OVERWORLD.location(), DimensionData.SPAWNTYPE.AIR);
        endToOw.belowY = 0;
        endToOw.slowFallDuration = 400;
        endToOw.teleportToYlevel = 300;
        dimensionDataList.add(endToOw);

        final DimensionData netherToNether = new DimensionData(NETHER.location(), NETHER.location(), DimensionData.SPAWNTYPE.CAVE);
        netherToNether.belowY = 0;
        netherToNether.teleportToYlevel = 4;
        dimensionDataList.add(netherToNether);

        final DimensionData netherToOw = new DimensionData(NETHER.location(), OVERWORLD.location(), DimensionData.SPAWNTYPE.CAVE);
        netherToOw.aboveY = 121;
        netherToOw.xMult = 8;
        netherToOw.zMult = 8;
        netherToOw.teleportToYlevel = -60;
        dimensionDataList.add(netherToOw);

        final DimensionData owToOw = new DimensionData(OVERWORLD.location(), OVERWORLD.location(), DimensionData.SPAWNTYPE.AIR);
        owToOw.aboveY = 364;
        owToOw.teleportToYlevel = 360;
        owToNether.slowFallDuration = 400;
        dimensionDataList.add(owToOw);
    }

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        final JsonObject entry2 = new JsonObject();
        entry2.addProperty("desc:", "Enable debug logging, default:false");
        entry2.addProperty("debuglogging", debuglogging);
        root.add("debuglogging", entry2);

        final JsonArray list1 = new JsonArray();
        for (final DimensionData data : dimensionDataList)
        {
            list1.add(data.serialize());
        }
        root.add(DIMENSIONCON, list1);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        try
        {
            debuglogging = data.get("debuglogging").getAsJsonObject().get("debuglogging").getAsBoolean();

            final JsonArray dimensionData = data.get(DIMENSIONCON).getAsJsonArray();
            dimensionDataList.clear();
            dimensionConnections.clear();
            for (final JsonElement element : dimensionData)
            {
                final DimensionData newData = new DimensionData((JsonObject) element);
                dimensionDataList.add(newData);
                dimensionConnections.computeIfAbsent(newData.from, n -> new ArrayList<>()).add(newData);
            }
        }
        catch (Exception e)
        {
            FallingthroughMod.LOGGER.error("Could not parse config file", e);
            throw e;
        }
    }
}
