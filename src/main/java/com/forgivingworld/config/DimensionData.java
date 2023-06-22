package com.forgivingworld.config;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.function.BiPredicate;

/**
 * Holds target dimension data
 */
public class DimensionData
{
    private final static String FROM_DIM       = "from";
    private final static String TO_DIM         = "to";
    private final static String X_MULT         = "xcoordmultiplier";
    private final static String Z_MULT         = "zcoordmultiplier";
    private final static String TP_TYPE        = "teleporttype";
    private final static String TP_TYPE_YLEVEL = "teleport_to_y";
    private final static String BELOWY         = "belowy";
    private final static String ABOVEY         = "abovey";
    private final static String SLOW_FALL      = "slowfallticks";

    /**
     * Dimension id we go to
     */
    public final ResourceLocation from;
    /**
     * Dimension id we go to
     */
    public final ResourceLocation to;

    /**
     * Coordinate modifiers
     */
    public double xMult  = 1.0d;
    public double zMult  = 1.0d;
    public int    aboveY = Integer.MAX_VALUE;
    public int    belowY = Integer.MIN_VALUE;

    public int slowFallDuration = 0;

    public int teleportToYlevel = 0;

    /**
     * Y-spawn selector
     */
    public final SPAWNTYPE yspawn;

    public DimensionData(final ResourceLocation from, final ResourceLocation to, final SPAWNTYPE yspawn)
    {
        this.from = from;
        this.to = to;
        this.yspawn = yspawn;
    }

    public DimensionData(final JsonObject data)
    {
        from = ResourceLocation.tryParse(data.get(FROM_DIM).getAsString());
        to = ResourceLocation.tryParse(data.get(TO_DIM).getAsString());

        if (data.has(X_MULT))
        {
            xMult = data.get(X_MULT).getAsDouble();
        }
        if (data.has(Z_MULT))
        {
            zMult = data.get(Z_MULT).getAsDouble();
        }

        yspawn = SPAWNTYPE.fromString(data.get(TP_TYPE).getAsJsonObject().get(TP_TYPE).getAsString());
        teleportToYlevel = data.get(TP_TYPE).getAsJsonObject().get(TP_TYPE_YLEVEL).getAsInt();

        if (data.has(BELOWY))
        {
            belowY = data.get(BELOWY).getAsInt();
        }
        if (data.has(ABOVEY))
        {
            aboveY = data.get(ABOVEY).getAsInt();
        }

        if (data.has(SLOW_FALL))
        {
            slowFallDuration = data.get(SLOW_FALL).getAsInt();
        }
    }

    public JsonObject serialize()
    {
        final JsonObject data = new JsonObject();
        data.addProperty(FROM_DIM, from.toString());
        data.addProperty(TO_DIM, to.toString());
        if (xMult != 1.0 || zMult != 1.0)
        {
            data.addProperty(X_MULT, xMult);
            data.addProperty(Z_MULT, zMult);
        }

        final JsonObject spawndata = new JsonObject();
        spawndata.addProperty(TP_TYPE, yspawn.toString());
        spawndata.addProperty(TP_TYPE_YLEVEL, teleportToYlevel);
        data.add(TP_TYPE, spawndata);

        if (belowY != Integer.MIN_VALUE)
        {
            data.addProperty(BELOWY, belowY);
        }

        if (aboveY != Integer.MAX_VALUE)
        {
            data.addProperty(ABOVEY, aboveY);
        }

        if (slowFallDuration != 0)
        {
            data.addProperty(SLOW_FALL, slowFallDuration);
        }

        return data;
    }

    public boolean shouldTP(final double y)
    {
        return y < belowY || y > aboveY;
    }

    /**
     * Y-Spawn types
     */
    public enum SPAWNTYPE
    {
        AIR,
        GROUND,
        CAVE;

        public static SPAWNTYPE fromString(String data)
        {
            if (data.toLowerCase().trim().equals("air"))
            {
                return AIR;
            }
            if (data.toLowerCase().trim().equals("ground"))
            {
                return GROUND;
            }
            if (data.toLowerCase().trim().equals("cave"))
            {
                return CAVE;
            }

            throw new IllegalArgumentException("Unkown TP type: " + data + " expected one of air,ground,cave");
        }
    }

    /**
     * Translates the position for this dimension data
     *
     * @param original
     * @return
     */
    public BlockPos translatePosition(final BlockPos original)
    {
        return new BlockPos((int) (original.getX() * xMult), original.getY(), (int) (original.getZ() * zMult));
    }

    /**
     * Get the spawn pos for the new world, given the original coordinates
     *
     * @param world
     * @param xOriginal
     * @param zOriginal // 115.3x -242.3z
     * @return position to put the player at
     */
    public BlockPos getSpawnPos(final ServerLevel world, double xOriginal, double zOriginal)
    {
        xOriginal = (xOriginal * xMult);
        zOriginal = (zOriginal * zMult);

        switch (yspawn)
        {
            case AIR:
                final BlockPos solidAir =
                  findAround(world, new BlockPos((int) xOriginal, teleportToYlevel, (int) zOriginal), 10, 20, -2, DOUBLE_AIR_GROUND);
                if (solidAir != null)
                {
                    return solidAir;
                }
                return findAround(world, new BlockPos((int) xOriginal, teleportToYlevel, (int) zOriginal), 20, 50, -2, DOUBLE_AIR);
            case GROUND:
                // Load chunk
                final ChunkAccess targetChunk = world.getChunk((int) Math.floor(xOriginal) >> 4, (int) Math.floor(zOriginal) >> 4);
                return findAround(world,
                  new BlockPos((int) xOriginal, targetChunk.getHeight(Heightmap.Types.WORLD_SURFACE, (int) Math.floor(xOriginal), (int) Math.floor(zOriginal)), (int) zOriginal),
                  20,
                  50,
                  2,
                  DOUBLE_AIR);
            case CAVE:
                return findAround(world, new BlockPos((int) xOriginal, teleportToYlevel, (int) zOriginal), 20, 50, 2, DOUBLE_AIR_GROUND);
        }

        return null;
    }

    /**
     * Predicate for pos selection
     */
    final BiPredicate<BlockGetter, BlockPos> DOUBLE_AIR        =
      (world, pos) -> world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir();
    final BiPredicate<BlockGetter, BlockPos> DOUBLE_AIR_GROUND = DOUBLE_AIR.and((world, pos) -> world.getBlockState(pos.below()).isSolid());

    /**
     * Finds a nice position around
     *
     * @param world
     * @param start
     * @param horizontal
     * @param vertical
     * @param yStep
     * @param predicate
     * @return
     */
    public static BlockPos findAround(
      final ServerLevel world,
      final BlockPos start,
      final int vertical,
      final int horizontal,
      final int yStep,
      final BiPredicate<BlockGetter, BlockPos> predicate)
    {
        if (horizontal < 1 && vertical < 1)
        {
            return null;
        }

        BlockPos temp;
        int y = 0;
        int y_offset = yStep;

        for (int i = 0; i < vertical + 2; i++)
        {
            for (int steps = 1; steps <= horizontal; steps++)
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
        }

        return null;
    }
}
