package com.fallingthrough.event;

import com.fallingthrough.FallingthroughMod;
import com.fallingthrough.config.DimensionData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

/**
 * Forge event bus handler, ingame events are fired here
 */
public class EventHandler
{
    public static final TicketType<ChunkPos> TELEPORT_TICKET = TicketType.create("fallingthroughTP", Comparator.comparingLong(ChunkPos::toLong), 20 * 60);

    /**
     * Time to tp in 4sec steps
     */
    private static final Integer            TP_TIME      = 10;
    private static final Map<UUID, Integer> playerTpTime = new HashMap<>();
    private static final Map<UUID, Long>    lastTpTime   = new HashMap<>();

    public static void onPlayerTick(final Player player)
    {
        if (player.level.isClientSide() || player.level.getGameTime() % 80 != 0 || player.isRemoved())
        {
            return;
        }

        final Long lastTime = lastTpTime.get(player.getUUID());
        if (lastTime != null && player.level.getGameTime() - lastTime < 20 * 15)
        {
            return;
        }

        final List<DimensionData> dimensionTPs = FallingthroughMod.config.getCommonConfig().dimensionConnections.get(player.level.dimension().location());

        if (dimensionTPs == null || dimensionTPs.isEmpty())
        {
            return;
        }

        DimensionData tp = null;
        for (final DimensionData data : dimensionTPs)
        {
            if (data.shouldTP(player.getY()))
            {
                tp = data;
                break;
            }
        }

        if (tp == null)
        {
            playerTpTime.remove(player.getUUID());
            return;
        }

        if (player.getY() < tp.belowY && Math.abs(player.getY() - tp.belowY) > 15
              || player.getY() > tp.aboveY && Math.abs(player.getY() - tp.aboveY) > 15)
        {
            tryTpPlayer((ServerPlayer) player, tp);
            return;
        }

        int time = playerTpTime.computeIfAbsent(player.getUUID(), player2 -> 0);
        time += 1;
        playerTpTime.put(player.getUUID(), time);

        if (time == 1)
        {
            player.sendSystemMessage(Component.literal("Dimensional forces are starting to affect you, pulling you " + (player.getY() > tp.aboveY ? "up" : "down") + ", take care!")
              .withStyle(
                ChatFormatting.DARK_AQUA));
        }

        if (time == 6)
        {
            player.sendSystemMessage(Component.literal("Dimensional forces are getting stronger...").withStyle(
              ChatFormatting.DARK_PURPLE));


            ServerLevel gotoWorld = null;
            for (final ResourceKey<Level> key : player.level.getServer().levelKeys())
            {
                if (key.location().equals(tp.to))
                {
                    gotoWorld = player.level.getServer().getLevel(key);
                    break;
                }
            }

            if (gotoWorld != null)
            {
                final ChunkPos dimensionPos = new ChunkPos(tp.translatePosition(player.blockPosition()));
                gotoWorld.getChunkSource().addRegionTicket(TELEPORT_TICKET, dimensionPos, 10, dimensionPos);
            }
        }

        player.level.playSound(null,
          player.getX(),
          player.getY(),
          player.getZ(),
          SoundEvents.PORTAL_AMBIENT,
          player.getSoundSource(),
          0.5F,
          2F + (FallingthroughMod.rand.nextFloat() - FallingthroughMod.rand.nextFloat()) * 0.2F);

        if (time > TP_TIME)
        {
            if (tryTpPlayer((ServerPlayer) player, tp))
            {
                playerTpTime.remove(player.getUUID());
            }
        }
    }

    public static void onVoidDamageRecv(final Player player, final CallbackInfoReturnable<Boolean> cir, final DamageSource damageSource)
    {
        if (damageSource.is(DamageTypes.OUT_OF_WORLD))
        {
            if (player.level.isClientSide)
            {
                return;
            }

            final ServerPlayer playerEntity = (ServerPlayer) player;

            final List<DimensionData> dimensions = FallingthroughMod.config.getCommonConfig().dimensionConnections.get(player.level.dimension().location());

            if (dimensions == null || dimensions.isEmpty())
            {
                return;
            }

            for (final DimensionData data : dimensions)
            {
                if (playerEntity.getY() < data.belowY && tryTpPlayer(playerEntity, data))
                {
                    cir.setReturnValue(false);
                    break;
                }
            }
        }
    }

    /**
     * Tries to tp the player
     *
     * @param playerEntity
     * @return
     */
    private static boolean tryTpPlayer(final ServerPlayer playerEntity, DimensionData gotoDim)
    {
        if (playerEntity.isCreative() || playerEntity.isSpectator())
        {
            return false;
        }

        final ServerLevel world = (ServerLevel) playerEntity.level;

        if (gotoDim == null || !gotoDim.shouldTP(playerEntity.getY()))
        {
            return false;
        }

        ServerLevel gotoWorld = null;
        for (final ResourceKey<Level> key : world.getServer().levelKeys())
        {
            if (key.location().equals(gotoDim.to))
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

        final Long lastTime = lastTpTime.get(playerEntity.getUUID());
        if (lastTime != null && world.getGameTime() - lastTime < 20 * 15)
        {
            return false;
        }

        lastTpTime.put(playerEntity.getUUID(), playerEntity.level.getGameTime());

        if (FallingthroughMod.config.getCommonConfig().debuglogging)
        {
            FallingthroughMod.LOGGER.info(
              "Teleporting player " + playerEntity.getDisplayName().getString() + "(" + playerEntity.getId() + ") from " + playerEntity.blockPosition().toShortString() + " in "
                + playerEntity.level.dimension().location()
                + " to: " + tpPos.toShortString() + " in " + gotoWorld.dimension().location() +
                " with TP type:" + gotoDim.yspawn);
        }

        ChunkPos chunkpos = new ChunkPos(tpPos);
        gotoWorld.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkpos, 1, playerEntity.getId());
        playerEntity.stopRiding();
        if (playerEntity.isSleeping())
        {
            playerEntity.stopSleepInBed(true, true);
        }

        playerEntity.teleportTo(gotoWorld, tpPos.getX() + 0.5, tpPos.getY(), tpPos.getZ() + 0.5, playerEntity.getYRot(), playerEntity.getXRot());
        if (gotoDim.slowFallDuration > 0)
        {
            playerEntity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, gotoDim.slowFallDuration));
        }

        playerEntity.fallDistance = 0;

        playerEntity.level.playSound(null,
          playerEntity.getX(),
          playerEntity.getY(),
          playerEntity.getZ(),
          SoundEvents.PORTAL_TRAVEL,
          playerEntity.getSoundSource(),
          1.0F,
          2F + (FallingthroughMod.rand.nextFloat() - FallingthroughMod.rand.nextFloat()) * 0.2F);

        return true;
    }
}
