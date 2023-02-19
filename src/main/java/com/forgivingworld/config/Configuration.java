package com.forgivingworld.config;

import com.forgivingworld.ForgivingWorldMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Configuration
{
    /**
     * Loaded everywhere, not synced
     */
    private final CommonConfiguration commonConfig = new CommonConfiguration();

    /**
     * Loaded clientside, not synced
     */
    // private final ClientConfiguration clientConfig;
    final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Builds configuration tree.
     */
    public Configuration()
    {
    }

    public void load()
    {
        final Path configPath = FMLPaths.CONFIGDIR.get().resolve(ForgivingWorldMod.MODID + ".json");
        final File config = configPath.toFile();

        if (!config.exists())
        {
            ForgivingWorldMod.LOGGER.warn("Config not found, recreating default");
            save();
            load();
        }
        else
        {
            try
            {
                commonConfig.deserialize(gson.fromJson(Files.newBufferedReader(configPath), JsonObject.class));
            }
            catch (Exception e)
            {
                ForgivingWorldMod.LOGGER.error("Could not read config from:" + configPath, e);
                save();
                load();
            }
        }
    }

    public void save()
    {
        final Path configPath = FMLPaths.CONFIGDIR.get().resolve(ForgivingWorldMod.MODID + ".json");
        try
        {
            final BufferedWriter writer = Files.newBufferedWriter(configPath);
            gson.toJson(commonConfig.serialize(), JsonObject.class, writer);
            writer.close();
        }
        catch (Exception e)
        {
            ForgivingWorldMod.LOGGER.error("Could not write config to:" + configPath, e);
        }
    }

    public CommonConfiguration getCommonConfig()
    {
        return commonConfig;
    }
}
