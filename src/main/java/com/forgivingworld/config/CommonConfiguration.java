package com.forgivingworld.config;

import com.cupboard.config.ICommonConfig;
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

public class CommonConfiguration implements ICommonConfig
{
    private final static String DIMENSIONCON = "dimensionconnections";

    public  Map<ResourceLocation, List<DimensionData>> dimensionConnections = new HashMap<>();
    private List<DimensionData>                        dimensionDataList    = new ArrayList<>();
    public  boolean                                    disableVanillaPortals         = false;
    public  boolean                                    teleportLeashed         = true;
    public  boolean                                    teleportedRidden         = true;
    public  boolean                                    debuglogging         = false;

    public CommonConfiguration()
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
        entry2.addProperty("desc:", "Disables vanilla portals, default:false");
        entry2.addProperty("disableVanillaPortals", disableVanillaPortals);
        root.add("disableVanillaPortals", entry2);

        final JsonObject entry4 = new JsonObject();
        entry4.addProperty("desc:", "Teleport ridden entities too, default:true");
        entry4.addProperty("teleportedRidden", teleportedRidden);
        root.add("teleportedRidden", entry4);

        final JsonObject entry5 = new JsonObject();
        entry5.addProperty("desc:", "Teleport leashed entities too, default:true");
        entry5.addProperty("teleportLeashed", teleportLeashed);
        root.add("teleportLeashed", entry5);

        final JsonObject entry3 = new JsonObject();
        entry3.addProperty("desc:", "Enable debug logging, default:false");
        entry3.addProperty("debuglogging", debuglogging);
        root.add("debuglogging", entry3);

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
        debuglogging = data.get("debuglogging").getAsJsonObject().get("debuglogging").getAsBoolean();
        disableVanillaPortals = data.get("disableVanillaPortals").getAsJsonObject().get("disableVanillaPortals").getAsBoolean();
        teleportedRidden = data.get("teleportedRidden").getAsJsonObject().get("teleportedRidden").getAsBoolean();
        teleportLeashed = data.get("teleportLeashed").getAsJsonObject().get("teleportLeashed").getAsBoolean();

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
}
