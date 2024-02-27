package com.fallingthrough.mixin;

import com.fallingthrough.FallingthroughMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class PlayerPortalCheck
{
    @Shadow
    public abstract void sendSystemMessage(final Component p_215097_);

    @Shadow public abstract void setPortalCooldown();

    @Inject(method = "handleInsidePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;equals(Ljava/lang/Object;)Z"), cancellable = true)
    private void checkDisabled(final BlockPos p_20222_, final CallbackInfo ci)
    {
        if (FallingthroughMod.config.getCommonConfig().disableVanillaPortals && ((Object) this) instanceof ServerPlayer)
        {
            sendSystemMessage(Component.translatable("forgivingworld.disabledportal").withStyle(
              ChatFormatting.LIGHT_PURPLE));
            setPortalCooldown();
            ci.cancel();
        }
    }
}
