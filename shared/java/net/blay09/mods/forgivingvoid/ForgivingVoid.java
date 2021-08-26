package net.blay09.mods.forgivingvoid;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.LivingFallEvent;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.TickType;
import net.blay09.mods.forgivingvoid.mixin.ServerGamePacketListenerImplAccessor;
import net.blay09.mods.forgivingvoid.mixin.ServerPlayerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class ForgivingVoid {

    public static final String MOD_ID = "forgivingvoid";

    public static void initialize() {
        ForgivingVoidConfig.initialize();

        Balm.getEvents().onEvent(LivingFallEvent.class, ForgivingVoid::onPlayerFall);
        Balm.getEvents().onTickEvent(TickType.ServerPlayer, TickPhase.Start, ForgivingVoid::onPlayerTick);
    }

    public static void onPlayerTick(ServerPlayer player) {
        boolean isInVoid = player.getY() < ForgivingVoidConfig.getActive().triggerAtY && player.yo < ForgivingVoidConfig.getActive().triggerAtY;
        boolean isTeleporting = ((ServerGamePacketListenerImplAccessor) player.connection).getAwaitingPositionFromClient() != null;
        CompoundTag persistentData = Balm.getHooks().getPersistentData(player);
        if (isEnabledForDimension(player.level.dimension()) && isInVoid && !isTeleporting && fireForgivingVoidEvent(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 3));
            if (player.isVehicle()) {
                player.ejectPassengers();
            }

            player.stopRiding();

            ((ServerPlayerAccessor) player).setIsChangingDimension(true);
            player.teleportTo(player.getX(), ForgivingVoidConfig.getActive().fallingHeight, player.getZ());
            persistentData.putBoolean("ForgivingVoidNoFallDamage", true);
        } else if (persistentData.getBoolean("ForgivingVoidNoFallDamage")) {
            // LivingFallEvent is not called when the player falls into water or is flying, so reset it manually - and give no damage at all.
            final BlockPos playerPos = player.blockPosition();
            if (player.isInWater() || player.getAbilities().flying || player.getAbilities().mayfly || player.level.getBlockState(playerPos).getBlock() == Blocks.COBWEB) {
                persistentData.putBoolean("ForgivingVoidNoFallDamage", false);
                ((ServerPlayerAccessor) player).setIsChangingDimension(false);
                return;
            }

            if (ForgivingVoidConfig.getActive().disableVanillaAntiCheatWhileFalling) {
                // Vanilla's AntiCheat is triggers on falling and teleports, even in Vanilla.
                // So I'll just disable it until the player lands, so it doesn't look like it's my mod causing the issue.
                ((ServerPlayerAccessor) player).setIsChangingDimension(true);
            }
        }
    }

    public static void onPlayerFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer player) {
            CompoundTag persistentData = Balm.getHooks().getPersistentData(player);
            if (persistentData.getBoolean("ForgivingVoidNoFallDamage")) {
                if (ForgivingVoidConfig.getActive().disableVanillaAntiCheatWhileFalling) {
                    ((ServerPlayerAccessor) player).setIsChangingDimension(false);
                }

                if (!event.isCanceled()) {
                    float damage = ForgivingVoidConfig.getActive().damageOnFall;
                    if (ForgivingVoidConfig.getActive().preventDeath && player.getHealth() - damage <= 0) {
                        damage = player.getHealth() - 1f;
                    }
                    float finalDamage = damage * event.getDamageMultiplier();
                    if (finalDamage > 0f) {
                        entity.hurt(DamageSource.FALL, finalDamage);
                    }
                }

                event.setDamageMultiplier(0f);
                event.setCanceled(true);
                persistentData.putBoolean("ForgivingVoidNoFallDamage", false);
            }
        }
    }

    private static boolean fireForgivingVoidEvent(Player player) {
        ForgivingVoidFallThroughEvent event = new ForgivingVoidFallThroughEvent(player);
        Balm.getEvents().fireEvent(event);
        return !event.isCanceled();
    }

    private static boolean isEnabledForDimension(ResourceKey<Level> dimensionKey) {
        if (dimensionKey == Level.OVERWORLD) {
            return ForgivingVoidConfig.getActive().triggerInOverworld;
        } else if (dimensionKey == Level.END) {
            return ForgivingVoidConfig.getActive().triggerInEnd;
        } else if (dimensionKey == Level.NETHER) {
            return ForgivingVoidConfig.getActive().triggerInNether;
        } else {
            final ResourceLocation dimension = dimensionKey.location();
            List<String> dimensionAllowList = ForgivingVoidConfig.getActive().dimensionAllowList;
            List<String> dimensionDenyList = ForgivingVoidConfig.getActive().dimensionDenyList;
            if (!dimensionAllowList.isEmpty() && !dimensionAllowList.contains(dimension.toString())) {
                return false;
            } else {
                return !dimensionDenyList.contains(dimension.toString());
            }
        }
    }

}
