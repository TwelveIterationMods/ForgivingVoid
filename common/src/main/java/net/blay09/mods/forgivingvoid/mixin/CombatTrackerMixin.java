package net.blay09.mods.forgivingvoid.mixin;

import net.blay09.mods.balm.api.Balm;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CombatTracker.class)
public class CombatTrackerMixin {

    @Shadow
    @Final
    private LivingEntity mob;

    @Inject(method = "getFallMessage(Lnet/minecraft/world/damagesource/CombatEntry;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    public void getFallMessage(CombatEntry combatEntry, @Nullable Entity entity, CallbackInfoReturnable<Component> callbackInfoReturnable) {
        final var persistentData = Balm.getHooks().getPersistentData(mob);
        if (persistentData.getBoolean("ForgivingVoidIsFalling")) {
            callbackInfoReturnable.setReturnValue(Component.translatable("death.fell.forgivingvoid", mob.getDisplayName()));
        }
    }
}
