package net.blay09.mods.forgivingvoid;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.LivingFallEvent;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.TickType;
import net.blay09.mods.forgivingvoid.mixin.ServerGamePacketListenerImplAccessor;
import net.blay09.mods.forgivingvoid.mixin.ServerPlayerAccessor;
import net.blay09.mods.forgivingvoid.mixin.ThrownTridentAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

public class ForgivingVoid {

    public static final String MOD_ID = "forgivingvoid";

    public static void initialize() {
        ForgivingVoidConfig.initialize();

        Balm.getEvents().onEvent(LivingFallEvent.class, ForgivingVoid::onLivingEntityFall);
        final var entityAllowList = ForgivingVoidConfig.getActive().entityAllowList;
        final var onlyPlayersExplicitlyAllowed = entityAllowList.isEmpty() || (entityAllowList.size() == 1 && entityAllowList.contains(new ResourceLocation("player")));
        final var otherEntitiesImplicitlyAllowed = ForgivingVoidConfig.getActive().tridentForgiveness;
        if (onlyPlayersExplicitlyAllowed && !otherEntitiesImplicitlyAllowed) {
            Balm.getEvents().onTickEvent(TickType.ServerPlayer, TickPhase.Start, ForgivingVoid::onPlayerTick);
        } else {
            Balm.getEvents().onTickEvent(TickType.Entity, TickPhase.Start, ForgivingVoid::onEntityTick);
        }
    }

    public static void onPlayerTick(ServerPlayer player) {
        onEntityTick(player);
    }

    public static void onEntityTick(Entity entity) {
        if (!isAllowedEntity(entity)) {
            return;
        }

        int triggerAtY = entity.level().getMinBuildHeight() - ForgivingVoidConfig.getActive().triggerAtDistanceBelow;
        boolean isInVoid = entity.getY() < triggerAtY && entity.yo < triggerAtY;
        boolean isTeleporting = entity instanceof ServerPlayer player && ((ServerGamePacketListenerImplAccessor) player.connection).getAwaitingPositionFromClient() != null;
        CompoundTag persistentData = Balm.getHooks().getPersistentData(entity);
        if (isInVoid && !isTeleporting && isEnabledForDimension(entity.level().dimension()) && fireForgivingVoidEvent(entity)) {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 3));
            }
            if (entity.isVehicle()) {
                entity.ejectPassengers();
            }

            entity.stopRiding();

            if (entity instanceof ServerPlayerAccessor player) {
                player.setIsChangingDimension(true);
            }
            entity.teleportTo(entity.getX(), ForgivingVoidConfig.getActive().fallingHeight, entity.getZ());
            persistentData.putBoolean("ForgivingVoidIsFalling", true);
        } else if (persistentData.getBoolean("ForgivingVoidIsFalling")) {
            // LivingFallEvent is not called when the player falls into water or is flying, so reset it manually - and give no damage at all.
            if (hasLanded(entity) || isOrMayFly(entity)) {
                persistentData.putBoolean("ForgivingVoidIsFalling", false);
                if (entity instanceof ServerPlayerAccessor player) {
                    player.setIsChangingDimension(false);
                }
                return;
            }

            if (ForgivingVoidConfig.getActive().disableVanillaAntiCheatWhileFalling && entity instanceof ServerPlayerAccessor player) {
                // Vanilla's AntiCheat is triggers on falling and teleports, even in Vanilla.
                // So I'll just disable it until the player lands, so it doesn't look like it's my mod causing the issue.
                player.setIsChangingDimension(true);
            }
        }
    }

    private static boolean isAllowedEntity(Entity entity) {
        final var entityAllowList = ForgivingVoidConfig.getActive().entityAllowList;
        final var entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityAllowList.isEmpty() && entity instanceof Player) {
            return true;
        }

        if (ForgivingVoidConfig.getActive().tridentForgiveness && entity instanceof ThrownTrident trident) {
            final var loyalty = trident.getEntityData().get(ThrownTridentAccessor.getIdLoyalty());
            //noinspection UnreachableCode
            if (loyalty > 0) {
                return true;
            }
        }

        return entityAllowList.contains(entityId);
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
        if (isAllowedEntity(entity)) {
            CompoundTag persistentData = Balm.getHooks().getPersistentData(entity);
            if (persistentData.getBoolean("ForgivingVoidIsFalling")) {
                float damage = ForgivingVoidConfig.getActive().damageOnFall;
                if (ForgivingVoidConfig.getActive().preventDeath && entity.getHealth() - damage <= 0) {
                    damage = entity.getHealth() - 1f;
                }

                event.setFallDamageOverride(damage);

                if (entity instanceof ServerPlayerAccessor player) {
                    player.setIsChangingDimension(false);
                }
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
            final var dimensionAllowList = ForgivingVoidConfig.getActive().dimensionAllowList;
            final var dimensionDenyList = ForgivingVoidConfig.getActive().dimensionDenyList;
            if (!dimensionAllowList.isEmpty() && !dimensionAllowList.contains(dimension)) {
                return false;
            } else {
                return !dimensionDenyList.contains(dimension);
            }
        }
    }

}
