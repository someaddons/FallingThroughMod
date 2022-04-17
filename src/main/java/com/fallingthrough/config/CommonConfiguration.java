package com.fallingthrough.config;

import com.fallingthrough.FallingthroughMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommonConfiguration
{
    public List<String> belowDimension         = Arrays.asList("minecraft:overworld;minecraft:the_nether;8;8;AIR;4",
      "minecraft:the_end;minecraft:overworld;1;1;AIR;0",
      "minecraft:the_nether;minecraft:the_nether;1;1;CAVE;0");
    public List<String> aboveDimension         = Arrays.asList("minecraft:the_nether;minecraft:overworld;0.125;0.125;CAVE;4",
      "minecraft:overworld;minecraft:overworld;0.8;0.8;AIR;0");
    public boolean      enableAboveDimensionTP = true;
    public int          slowFallDuration       = 400;

    protected CommonConfiguration()
    {

    }

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        final JsonObject entry1 = new JsonObject();
        entry1.addProperty("desc:", "List of dimension connections for players going below a dimension  "
                                      + "Format: FromDimension,ToDimension,XCoordDivider, ZCoordDivider, Y-SpawnType, DimensionBorderTPDistance  "
                                      + "FromDimension and ToDimension are ID's of dimensions, e.g. minecraft:overworld  "
                                      + "X and Z Coord dividers are values by which the original player coordinates are divided by  "
                                      + "Y-SpawnType is one of these: AIR,GROUND,CAVE  "
                                      + "AIR spawns the player as high as possible to the dimensions max height within airblocks.  "
                                      + "GROUND spawns the player on the normal groundlevel,  "
                                      + "CAVE spawns the player in the first open space going from 0 up.  "
                                      + "DimensionBorderTPDistance: Distance from the dimensions border at which the teleport starts  "
                                      + "Use [first,second] to list multiple entries.");
        final JsonArray list1 = new JsonArray();
        for (final String name : belowDimension)
        {
            list1.add(name);
        }
        entry1.add("belowDimension", list1);
        root.add("belowDimension", entry1);

        final JsonObject entry2 = new JsonObject();
        entry2.addProperty("desc:", "Turn on teleporting when going above dimension height");
        entry2.addProperty("enableAboveDimensionTP", enableAboveDimensionTP);
        root.add("enableAboveDimensionTP", entry2);

        final JsonObject entry3 = new JsonObject();
        entry3.addProperty("desc:", "Requires enableAboveDimensionTP to be enabled! " +
                                      "List of dimension connections for players going above a dimension "
                                      + "Format: FromDimension,ToDimension,XCoordDivider, ZCoordDivider, Y-SpawnType, DimensionBorderTPDistance "
                                      + "FromDimension and ToDimension are ID's of dimensions, e.g. minecraft:overworld "
                                      + "X and Z Coord dividers are values by which the original player coordinates are divided by "
                                      + "Y-SpawnType is one of these: AIR,GROUND,CAVE "
                                      + "AIR spawns the player as high as possible to the dimensions max height within airblocks. "
                                      + "GROUND spawns the player on the normal groundlevel, "
                                      + "CAVE spawns the player in the first open space going from 0 up. "
                                      + "DimensionBorderTPDistance: Distance from the dimensions border at which the teleport starts "
                                      + "Use [first,second] to list multiple entries.");
        final JsonArray list3 = new JsonArray();
        for (final String name : aboveDimension)
        {
            list3.add(name);
        }
        entry3.add("aboveDimension", list3);
        root.add("aboveDimension", entry3);

        final JsonObject entry4 = new JsonObject();
        entry4.addProperty("desc:", "Duration of the slowfall potion after teleporting, default: 400 ticks (20 ticks = 1 second)");
        entry4.addProperty("slowFallDuration", slowFallDuration);
        root.add("slowFallDuration", entry4);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        if (data == null)
        {
            FallingthroughMod.LOGGER.error("Config file was empty!");
            return;
        }

        try
        {
            enableAboveDimensionTP = data.get("enableAboveDimensionTP").getAsJsonObject().get("enableAboveDimensionTP").getAsBoolean();
            slowFallDuration = data.get("slowFallDuration").getAsJsonObject().get("slowFallDuration").getAsInt();
            belowDimension = new ArrayList<>();
            for (final JsonElement element : data.get("belowDimension").getAsJsonObject().get("belowDimension").getAsJsonArray())
            {
                belowDimension.add(element.getAsString());
            }

            aboveDimension = new ArrayList<>();
            for (final JsonElement element : data.get("aboveDimension").getAsJsonObject().get("aboveDimension").getAsJsonArray())
            {
                aboveDimension.add(element.getAsString());
            }
        }
        catch (Exception e)
        {
            FallingthroughMod.LOGGER.error("Could not parse config file", e);
        }
    }
}
