package com.fallingthrough.event;

import com.fallingthrough.FallingthroughMod;
import com.fallingthrough.config.ConfigurationCache;
import com.fallingthrough.config.DimensionData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge event bus handler, ingame events are fired here
 */
public class EventHandler
{
    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event)
    {
        if (event.player.level.isClientSide() || event.player.level.getGameTime() % 80 != 0)
        {
            return;
        }

        if (FallingthroughMod.config.getCommonConfig().enableAboveDimensionTP.get())
        {
            if (event.player.getY() >= event.player.level.getLogicalHeight() || event.player.getY() <= event.player.level.dimensionType().minY())
            {
                tryTpPlayer((ServerPlayer) event.player);
            }
        }
    }

    @SubscribeEvent
    public static void onVoidDamageRecv(final LivingHurtEvent event)
    {
        if (event.getSource() == DamageSource.OUT_OF_WORLD)
        {
            if (!(event.getEntity() instanceof Player) || event.getEntity().level.isClientSide)
            {
                return;
            }

            final ServerPlayer playerEntity = (ServerPlayer) event.getEntity();
            if (tryTpPlayer(playerEntity))
            {
                event.setAmount(0f);
            }
        }
    }

    /**
     * Tries to tp the player
     *
     * @param playerEntity
     * @return
     */
    private static boolean tryTpPlayer(final ServerPlayer playerEntity)
    {
        if (playerEntity.isCreative() || playerEntity.isSpectator())
        {
            return false;
        }

        final ServerLevel world = (ServerLevel) playerEntity.level;

        DimensionData gotoDim;
        if (playerEntity.blockPosition().getY() <= playerEntity.level.dimensionType().minY())
        {
            gotoDim = ConfigurationCache.belowToNextDim.get(world.dimension().location());
        }
        else
        {
            gotoDim = ConfigurationCache.aboveToNextDim.get(world.dimension().location());
        }

        if (gotoDim == null)
        {
            return false;
        }

        ServerLevel gotoWorld = null;
        for (final ResourceKey<Level> key : world.getServer().levelKeys())
        {
            if (key.location().equals(gotoDim.getID()))
            {
                gotoWorld = world.getServer().getLevel(key);
                break;
            }
        }

        if (gotoWorld == null)
        {
            // Use same world?
            return false;
        }

        BlockPos tpPos = gotoDim.getSpawnPos(gotoWorld, playerEntity.getX(), playerEntity.getZ());
        if (tpPos == null)
        {
            return false;
        }

        // Config if should give effect, could give some other effects aswell
        playerEntity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300));
        playerEntity.teleportTo(gotoWorld, tpPos.getX() + 0.5, tpPos.getY(), tpPos.getZ() + 0.5, playerEntity.getYRot(), playerEntity.getXRot());
        return true;
    }
}
