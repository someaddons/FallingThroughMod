package com.fallingthrough.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

public class CommonConfiguration
{
    public final ForgeConfigSpec                                     ForgeConfigSpecBuilder;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> belowDimension;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> aboveDimension;
    public final ForgeConfigSpec.ConfigValue<Boolean>                enableAboveDimensionTP;

    protected CommonConfiguration(final ForgeConfigSpec.Builder builder)
    {
        builder.push("Dimension Config");

        builder.comment(
          "List of dimension connections for players going below a dimension"
            + "Format: FromDimension,ToDimension,XCoordDivider, ZCoordDivider, Y-SpawnType, DimensionBorderTPDistance"
            + "FromDimension and ToDimension are ID's of dimensions, e.g. minecraft:overworld"
            + "X and Z Coord dividers are values by which the original player coordinates are divided by"
            + "Y-SpawnType is one of these: AIR,GROUND,CAVE "
            + "AIR spawns the player as high as possible to the dimensions max height within airblocks."
            + "GROUND spawns the player on the normal groundlevel,"
            + "CAVE spawns the player in the first open space going from 0 up."
            + "DimensionBorderTPDistance: Distance from the dimensions border at which the teleport starts"
            + "Use [first,second] to list multiple entries.");
        belowDimension = builder.defineList("belowDimension",
          Arrays.asList("minecraft:overworld;minecraft:the_nether;8;8;AIR;0",
            "minecraft:the_end;minecraft:overworld;1;1;AIR;0",
            "minecraft:the_nether;minecraft:the_nether;1;1;CAVE;0")
          , e -> e instanceof String);

        builder.comment("Turn on teleporting when going above dimension height");
        enableAboveDimensionTP = builder.define("enableAboveDimensionTP", true);

        builder.comment(
          "Requires enableAboveDimensionTP to be enabled! " +
            "List of dimension connections for players going above a dimension"
            + "Format: FromDimension,ToDimension,XCoordDivider, ZCoordDivider, Y-SpawnType, DimensionBorderTPDistance"
            + "FromDimension and ToDimension are ID's of dimensions, e.g. minecraft:overworld"
            + "X and Z Coord dividers are values by which the original player coordinates are divided by"
            + "Y-SpawnType is one of these: AIR,GROUND,CAVE "
            + "AIR spawns the player as high as possible to the dimensions max height within airblocks."
            + "GROUND spawns the player on the normal groundlevel,"
            + "CAVE spawns the player in the first open space going from 0 up."
            + "DimensionBorderTPDistance: Distance from the dimensions border at which the teleport starts"
            + "Use [first,second] to list multiple entries.");
        aboveDimension = builder.defineList("aboveDimension",
          Arrays.asList("minecraft:the_nether;minecraft:overworld;0.125;0.125;CAVE;0",
            "minecraft:overworld;minecraft:overworld;0.8;0.8;AIR;0")
          , e -> e instanceof String);

        // Escapes the current category level
        builder.pop();
        ForgeConfigSpecBuilder = builder.build();
    }
}
