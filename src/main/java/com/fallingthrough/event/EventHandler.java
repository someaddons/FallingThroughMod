package com.fallingthrough.event;

import com.fallingthrough.FallingthroughMod;
import com.fallingthrough.config.ConfigurationCache;
import com.fallingthrough.config.DimensionData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Forge event bus handler, ingame events are fired here
 */
public class EventHandler
{
    /**
     * Time to tp in 4sec steps
     */
    private static final Integer            TP_TIME      = 12;
    private static       Map<UUID, Integer> playerTpTime = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event)
    {
        if (event.player.level.isClientSide() || event.player.level.getGameTime() % 80 != 0)
        {
            return;
        }

        if (FallingthroughMod.config.getCommonConfig().enableAboveDimensionTP.get())
        {
            if (event.player.getY() >= WorldUtil.getDimensionMaxHeight(event.player.level.dimensionType()) || event.player.getY() <= WorldUtil.getDimensionMinHeight(event.player.level.dimensionType()))
            {
                tryTpPlayer((ServerPlayer) event.player, event.player.blockPosition().getY() <= WorldUtil.getDimensionMinHeight(event.player.level.dimensionType()));
                return;
            }

            final DimensionData above = ConfigurationCache.aboveToNextDim.get(event.player.level.dimension().location());
            final DimensionData below = ConfigurationCache.belowToNextDim.get(event.player.level.dimension().location());

            final boolean aboveTP = above != null && above.getLeeWay() != 0 && event.player.getY() >= (WorldUtil.getDimensionMaxHeight(event.player.level.dimensionType()) - above.getLeeWay());
            final boolean belowTP = below != null && below.getLeeWay() != 0 && event.player.getY() <= (WorldUtil.getDimensionMinHeight(event.player.level.dimensionType()) + below.getLeeWay());

            if (aboveTP || belowTP)
            {
                Integer time = playerTpTime.computeIfAbsent(event.player.getUUID(), player -> 0);
                time += 1;
                playerTpTime.put(event.player.getUUID(), time);

                if (time == 1)
                {
                    event.player.sendMessage(new TextComponent("Dimensional forces are starting to affect you, pulling you " + (aboveTP ? "up" : "down") + ", take care!").withStyle(
                      ChatFormatting.DARK_AQUA), event.player.getUUID());
                }

                if (time == 6)
                {
                    event.player.sendMessage(new TextComponent("Dimensional forces are getting stronger...").withStyle(
                      ChatFormatting.DARK_PURPLE), event.player.getUUID());
                }

                event.player.level.playSound((Player) null,
                  event.player.getX(),
                  event.player.getY(),
                  event.player.getZ(),
                  SoundEvents.PORTAL_AMBIENT,
                  event.player.getSoundSource(),
                  0.5F,
                  2F + (FallingthroughMod.rand.nextFloat() - FallingthroughMod.rand.nextFloat()) * 0.2F);

                if (time > TP_TIME)
                {
                    tryTpPlayer((ServerPlayer) event.player, belowTP);
                }
            }
            else
            {
                playerTpTime.remove(event.player.getUUID());
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
            if (tryTpPlayer(playerEntity, playerEntity.blockPosition().getY() <= WorldUtil.getDimensionMinHeight(playerEntity.level.dimensionType())))
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
    private static boolean tryTpPlayer(final ServerPlayer playerEntity, final boolean below)
    {
        if (playerEntity.isCreative() || playerEntity.isSpectator())
        {
            return false;
        }

        final ServerLevel world = (ServerLevel) playerEntity.level;

        DimensionData gotoDim;
        if (below)
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

        playerEntity.level.playSound((Player) null,
          playerEntity.getX(),
          playerEntity.getY(),
          playerEntity.getZ(),
          SoundEvents.PORTAL_TRAVEL,
          playerEntity.getSoundSource(),
          1.0F,
          2F + (FallingthroughMod.rand.nextFloat() - FallingthroughMod.rand.nextFloat()) * 0.2F);

        playerEntity.teleportTo(gotoWorld, tpPos.getX() + 0.5, tpPos.getY(), tpPos.getZ() + 0.5, playerEntity.getYRot(), playerEntity.getXRot());
        playerEntity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, FallingthroughMod.config.getCommonConfig().slowFallDuration.get()));
        return true;
    }
}
