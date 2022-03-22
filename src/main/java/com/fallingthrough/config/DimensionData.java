package com.fallingthrough.config;

import com.fallingthrough.FallingthroughMod;
import com.fallingthrough.event.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Material;

import java.util.function.BiPredicate;

/**
 * Holds target dimension data
 */
public class DimensionData
{
    /**
     * Dimension id we go to
     */
    private final ResourceLocation id;

    /**
     * Coordinate modifiers
     */
    private double xDivider = 1.0d;
    private double zDivider = 1.0d;
    private int    leeWay   = 0;

    /**
     * Y-spawn selector
     */
    private YSPAWN yspawn = YSPAWN.AIR;

    public DimensionData(final ResourceLocation id)
    {
        this.id = id;
    }

    /**
     * Get Dimension id
     *
     * @return
     */
    public ResourceLocation getID()
    {
        return id;
    }

    /**
     * Y-Spawn types
     */
    private enum YSPAWN
    {
        AIR,
        GROUND,
        CAVE;
    }

    /**
     * Get the spawn pos for the new world, given the original coordinates
     *
     * @param world
     * @param xOriginal
     * @param zOriginal // 115.3x -242.3z
     * @return position to put the player at
     */
    public BlockPos getSpawnPos(final LevelAccessor world, double xOriginal, double zOriginal)
    {
        xOriginal = (xOriginal / xDivider);
        zOriginal = (zOriginal / zDivider);

        switch (yspawn)
        {
            case AIR:
                final BlockPos solidAir = findAround(world, new BlockPos(xOriginal, WorldUtil.getDimensionMaxHeight(world.dimensionType()) - (4 + leeWay), zOriginal), 10, 20, -2, DOUBLE_AIR_GROUND);
                if (solidAir != null)
                {
                    return solidAir;
                }
                return findAround(world, new BlockPos(xOriginal,  WorldUtil.getDimensionMaxHeight(world.dimensionType()) - (4 + leeWay), zOriginal), 4, 50, -2, DOUBLE_AIR);
            case GROUND:
                // Load chunk
                final ChunkAccess targetChunk = world.getChunk((int) Math.floor(xOriginal) >> 4, (int) Math.floor(zOriginal) >> 4);
                return findAround(world,
                  new BlockPos(xOriginal, targetChunk.getHeight(Heightmap.Types.WORLD_SURFACE, (int) Math.floor(xOriginal), (int) Math.floor(zOriginal)), zOriginal),
                  20,
                  50,
                  2,
                  DOUBLE_AIR);
            case CAVE:
                return findAround(world, new BlockPos(xOriginal, WorldUtil.getDimensionMinHeight(world.dimensionType()) + 6 + leeWay, zOriginal), 12, 50, 2, DOUBLE_AIR_GROUND);
        }

        return null;
    }

    /**
     * Predicate for pos selection
     */
    final BiPredicate<BlockGetter, BlockPos> DOUBLE_AIR        =
      (world, pos) -> world.getBlockState(pos).getMaterial() == Material.AIR && world.getBlockState(pos.above()).getMaterial() == Material.AIR;
    final BiPredicate<BlockGetter, BlockPos> DOUBLE_AIR_GROUND = DOUBLE_AIR.and((world, pos) -> world.getBlockState(pos.below()).getMaterial().isSolid());

    /**
     * Finds a nice position around
     *
     * @param world
     * @param start
     * @param vRange
     * @param hRange
     * @param yStep
     * @param predicate
     * @return
     */
    public static BlockPos findAround(
      final LevelAccessor world,
      final BlockPos start,
      final int vRange,
      final int hRange,
      final int yStep,
      final BiPredicate<BlockGetter, BlockPos> predicate)
    {
        if (vRange < 1 && hRange < 1)
        {
            return null;
        }

        BlockPos temp;
        int y = 0;
        int y_offset = yStep;

        for (int i = 0; i < hRange + 2; i++)
        {
            for (int steps = 1; steps <= vRange; steps++)
            {
                // Start topleft of middle point
                temp = start.offset(-steps, y, -steps);

                // X ->
                for (int x = 0; x <= steps; x++)
                {
                    temp = temp.offset(1, 0, 0);
                    if (predicate.test(world, temp))
                    {
                        return temp;
                    }
                }

                // X
                // |
                // v
                for (int z = 0; z <= steps; z++)
                {
                    temp = temp.offset(0, 0, 1);
                    if (predicate.test(world, temp))
                    {
                        return temp;
                    }
                }

                // < - X
                for (int x = 0; x <= steps; x++)
                {
                    temp = temp.offset(-1, 0, 0);
                    if (predicate.test(world, temp))
                    {
                        return temp;
                    }
                }

                // ^
                // |
                // X
                for (int z = 0; z <= steps; z++)
                {
                    temp = temp.offset(0, 0, -1);
                    if (predicate.test(world, temp))
                    {
                        return temp;
                    }
                }
            }

            y += y_offset;

            if (WorldUtil.getDimensionMaxHeight(world.dimensionType()) <= start.getY() + y || start.getY() + y >= WorldUtil.getDimensionMinHeight(world.dimensionType()))
            {
                return null;
            }
        }

        return null;
    }

    public static DimensionData parse(final String[] splitData)
    {
        final ResourceLocation id = ResourceLocation.tryParse(splitData[1]);
        if (id == null)
        {
            FallingthroughMod.LOGGER.warn("Error parsing config second dimension: " + splitData[1]);
            return null;
        }

        final DimensionData data = new DimensionData(id);
        try
        {
            data.xDivider = Double.parseDouble(splitData[2]);
        }
        catch (Exception e)
        {
            FallingthroughMod.LOGGER.warn("Error parsing config xDivider: " + splitData[2]);
        }

        try
        {
            data.zDivider = Double.parseDouble(splitData[3]);
        }
        catch (Exception e)
        {
            FallingthroughMod.LOGGER.warn("Error parsing config second zDivider: " + splitData[3]);
        }

        try
        {
            data.yspawn = YSPAWN.valueOf(splitData[4]);
        }
        catch (Exception e)
        {
            FallingthroughMod.LOGGER.warn("Error parsing config second y spawn positions setting, should be one of :AIR,GROUND,CAVE: " + splitData[4]);
        }

        try
        {
            data.leeWay = Integer.parseInt(splitData[5]);
        }
        catch (Exception e)
        {
            FallingthroughMod.LOGGER.warn("Error parsing config for dimension tp distance , should be a number: " + splitData[5]);
        }

        return data;
    }

    public int getLeeWay()
    {
        return leeWay;
    }
}
