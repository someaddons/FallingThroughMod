package com.fallingthrough.event;

import com.fallingthrough.config.ConfigurationCache;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class ModEventHandler
{
    @SubscribeEvent
    public static void onConfigChanged(ModConfigEvent event)
    {
        ConfigurationCache.parseConfig();
    }
}
