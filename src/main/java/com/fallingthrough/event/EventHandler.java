package com.fallingthrough.event;

import com.fallingthrough.FallingthroughMod;
import com.fallingthrough.config.ConfigurationCache;
import com.fallingthrough.config.DimensionData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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
            if (event.player.getY() >= event.player.level.getHeight() || event.player.getY() <= 0)
            {
                tryTpPlayer((ServerPlayerEntity) event.player);
            }
        }
    }

    @SubscribeEvent
    public static void onVoidDamageRecv(final LivingHurtEvent event)
    {
        if (event.getSource() == DamageSource.OUT_OF_WORLD)
        {
            if (!(event.getEntity() instanceof PlayerEntity) || event.getEntity().level.isClientSide)
            {
                return;
            }

            final ServerPlayerEntity playerEntity = (ServerPlayerEntity) event.getEntity();
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
    private static boolean tryTpPlayer(final ServerPlayerEntity playerEntity)
    {
        if (playerEntity.isCreative() || playerEntity.isSpectator())
        {
            return false;
        }

        final ServerWorld world = (ServerWorld) playerEntity.level;

        DimensionData gotoDim;
        if (playerEntity.blockPosition().getY() <= 0)
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

        ServerWorld gotoWorld = null;
        for (final RegistryKey<World> key : world.getServer().levelKeys())
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
        playerEntity.addEffect(new EffectInstance(Effects.SLOW_FALLING, 300));
        playerEntity.teleportTo(gotoWorld, tpPos.getX() + 0.5, tpPos.getY(), tpPos.getZ() + 0.5, playerEntity.yRot, playerEntity.xRot);
        return true;
    }
}
