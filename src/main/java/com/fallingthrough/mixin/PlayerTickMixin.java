package com.fallingthrough.mixin;

import com.fallingthrough.event.EventHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerTickMixin
{
    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(final CallbackInfo ci)
    {
        EventHandler.onPlayerTick((Player) (Object) this);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void onhurt(final DamageSource damageSource, final float f, final CallbackInfoReturnable<Boolean> cir)
    {
        EventHandler.onVoidDamageRecv((Player) (Object) this, cir, damageSource);
    }
}
