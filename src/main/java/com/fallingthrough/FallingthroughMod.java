package com.fallingthrough;

import com.fallingthrough.config.Configuration;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
public class FallingthroughMod implements ModInitializer
{
    public static final String        MODID  = "forgivingworld";
    public static final Logger        LOGGER = LogManager.getLogger();
    public static       Configuration config = new Configuration();
    public static       Random        rand   = new Random();

    public FallingthroughMod()
    {

    }

    @Override
    public void onInitialize()
    {
        config.load();
    }
}
