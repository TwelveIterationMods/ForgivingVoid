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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Set;

public class ForgivingVoid {

    public static final String MOD_ID = "forgivingvoid";

    public static void initialize() {
        ForgivingVoidConfig.initialize();

        Balm.getEvents().onEvent(LivingFallEvent.class, ForgivingVoid::onLivingEntityFall);
        Balm.getEvents().onTickEvent(TickType.ServerPlayer, TickPhase.Start, ForgivingVoid::onPlayerTick);
    }

    public static void onPlayerTick(ServerPlayer player) {
        onEntityTick(player);
    }

    public static void onEntityTick(Entity entity) {
        int triggerAtY = entity.level().getMinBuildHeight() - ForgivingVoidConfig.getActive().triggerAtDistanceBelow;
        boolean isInVoid = entity.getY() < triggerAtY && entity.yo < triggerAtY;
        boolean isTeleporting = entity instanceof ServerPlayer player && ((ServerGamePacketListenerImplAccessor) player.connection).getAwaitingPositionFromClient() != null;
        CompoundTag persistentData = Balm.getHooks().getPersistentData(entity);
        if (isEnabledForDimension(entity.level().dimension()) && isInVoid && !isTeleporting && fireForgivingVoidEvent(entity)) {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 3));
            }
            if (entity.isVehicle()) {
                entity.ejectPassengers();
            }

            entity.stopRiding();

            ((ServerPlayerAccessor) entity).setIsChangingDimension(true);
            entity.teleportTo(entity.getX(), ForgivingVoidConfig.getActive().fallingHeight, entity.getZ());
            persistentData.putBoolean("ForgivingVoidIsFalling", true);
        } else if (persistentData.getBoolean("ForgivingVoidIsFalling")) {
            // LivingFallEvent is not called when the player falls into water or is flying, so reset it manually - and give no damage at all.
            final BlockPos playerPos = entity.blockPosition();
            if (hasLanded(entity) || isOrMayFly(entity)) {
                persistentData.putBoolean("ForgivingVoidIsFalling", false);
                ((ServerPlayerAccessor) entity).setIsChangingDimension(false);
                return;
            }

            if (ForgivingVoidConfig.getActive().disableVanillaAntiCheatWhileFalling) {
                // Vanilla's AntiCheat is triggers on falling and teleports, even in Vanilla.
                // So I'll just disable it until the player lands, so it doesn't look like it's my mod causing the issue.
                ((ServerPlayerAccessor) entity).setIsChangingDimension(true);
            }
        }
    }

    public static final Set<Block> FALL_CATCHING_BLOCKS = Set.of(Blocks.COBWEB);

    private static boolean hasLanded(Entity entity) {
        if (entity.onGround() || entity.isInWater() || entity.isInLava()) {
            return true;
        }

        final var landedOnState = entity.level().getBlockState(entity.blockPosition());
        return FALL_CATCHING_BLOCKS.contains(landedOnState.getBlock());
    }

    private static boolean isOrMayFly(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        return player.getAbilities().flying || player.getAbilities().mayfly;
    }

    public static void onLivingEntityFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer player) {
            CompoundTag persistentData = Balm.getHooks().getPersistentData(player);
            if (persistentData.getBoolean("ForgivingVoidIsFalling")) {
                float damage = ForgivingVoidConfig.getActive().damageOnFall;
                if (ForgivingVoidConfig.getActive().preventDeath && player.getHealth() - damage <= 0) {
                    damage = player.getHealth() - 1f;
                }

                event.setFallDamageOverride(damage);

                ((ServerPlayerAccessor) player).setIsChangingDimension(false);
            }
        }
    }

    private static boolean fireForgivingVoidEvent(Entity entity) {
        ForgivingVoidFallThroughEvent event = new ForgivingVoidFallThroughEvent(entity);
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
