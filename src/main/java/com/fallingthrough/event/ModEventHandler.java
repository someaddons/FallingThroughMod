package com.fallingthrough.event;

import com.fallingthrough.config.ConfigurationCache;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;

public class ModEventHandler
{
    @SubscribeEvent
    public static void onConfigChanged(ModConfig.ModConfigEvent event)
    {
        ConfigurationCache.parseConfig();
    }
}
