package com.fallingthrough.event;

import com.fallingthrough.FallingthroughMod;
import com.fallingthrough.config.ConfigurationCache;
import com.fallingthrough.config.DimensionData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Forge event bus handler, ingame events are fired here
 */
public class EventHandler
{
    public static final TicketType<ChunkPos> TELEPORT_TICKET = TicketType.create("fallingthroughTP", Comparator.comparingLong(ChunkPos::toLong), 20 * 60);

    /**
     * Time to tp in 4sec steps
     */
    private static final Integer            TP_TIME      = 12;
    private static       Map<UUID, Integer> playerTpTime = new HashMap<>();

    public static void onPlayerTick(final Player player)
    {
        if (player.level.isClientSide() || player.level.getGameTime() % 80 != 0 || player.isRemoved())
        {
            return;
        }

        if (FallingthroughMod.config.getCommonConfig().enableAboveDimensionTP)
        {
            if (player.getY() >= WorldUtil.getDimensionMaxHeight(player.level.dimensionType()) || player.getY() <= WorldUtil.getDimensionMinHeight(player.level.dimensionType()))
            {
                tryTpPlayer((ServerPlayer) player, player.blockPosition().getY() <= WorldUtil.getDimensionMinHeight(player.level.dimensionType()));
                return;
            }

            final DimensionData above = ConfigurationCache.aboveToNextDim.get(player.level.dimension().location());
            final DimensionData below = ConfigurationCache.belowToNextDim.get(player.level.dimension().location());

            final boolean aboveTP = above != null && above.getLeeWay() != 0 && player.getY() >= (WorldUtil.getDimensionMaxHeight(player.level.dimensionType()) - above.getLeeWay());
            final boolean belowTP = below != null && below.getLeeWay() != 0 && player.getY() <= (WorldUtil.getDimensionMinHeight(player.level.dimensionType()) + below.getLeeWay());

            if (aboveTP || belowTP)
            {
                int time = playerTpTime.computeIfAbsent(player.getUUID(), player2 -> 0);
                time += 1;
                playerTpTime.put(player.getUUID(), time);

                if (time == 1)
                {
                    player.sendSystemMessage(Component.literal("Dimensional forces are starting to affect you, pulling you " + (aboveTP ? "up" : "down") + ", take care!")
                      .withStyle(
                        ChatFormatting.DARK_AQUA));
                }

                if (time == 6)
                {
                    player.sendSystemMessage(Component.literal("Dimensional forces are getting stronger...").withStyle(
                      ChatFormatting.DARK_PURPLE));

                    // Copy from teleporting, adds a previous ticket before accessing
                    DimensionData gotoDim;
                    if (belowTP)
                    {
                        gotoDim = ConfigurationCache.belowToNextDim.get(player.level.dimension().location());
                    }
                    else
                    {
                        gotoDim = ConfigurationCache.aboveToNextDim.get(player.level.dimension().location());
                    }


                    ServerLevel gotoWorld = null;
                    for (final ResourceKey<Level> key : player.level.getServer().levelKeys())
                    {
                        if (key.location().equals(gotoDim.getID()))
                        {
                            gotoWorld = player.level.getServer().getLevel(key);
                            break;
                        }
                    }

                    if (gotoWorld != null)
                    {
                        final ChunkPos dimensionPos = new ChunkPos(gotoDim.translatePosition(player.blockPosition()));
                        ((ServerChunkCache) gotoWorld.getChunkSource()).addRegionTicket(TELEPORT_TICKET, dimensionPos, 2, dimensionPos);
                    }
                }

                player.level.playSound((Player) null,
                  player.getX(),
                  player.getY(),
                  player.getZ(),
                  SoundEvents.PORTAL_AMBIENT,
                  player.getSoundSource(),
                  0.5F,
                  2F + (FallingthroughMod.rand.nextFloat() - FallingthroughMod.rand.nextFloat()) * 0.2F);

                if (time > TP_TIME)
                {
                    tryTpPlayer((ServerPlayer) player, belowTP);
                }
            }
            else
            {
                playerTpTime.remove(player.getUUID());
            }
        }
    }

    public static void onVoidDamageRecv(final Player player, final CallbackInfoReturnable<Boolean> cir, final DamageSource damageSource)
    {
        if (damageSource == DamageSource.OUT_OF_WORLD)
        {
            if (player.level.isClientSide)
            {
                return;
            }

            final ServerPlayer playerEntity = (ServerPlayer) player;
            if (tryTpPlayer(playerEntity, playerEntity.blockPosition().getY() <= WorldUtil.getDimensionMinHeight(playerEntity.level.dimensionType())))
            {
                cir.setReturnValue(false);
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
        playerEntity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, FallingthroughMod.config.getCommonConfig().slowFallDuration));
        return true;
    }
}
