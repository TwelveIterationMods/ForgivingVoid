package net.blay09.mods.forgivingvoid;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.LivingFallEvent;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.TickType;
import net.blay09.mods.balm.common.config.ConfigLocalization;
import net.blay09.mods.forgivingvoid.mixin.ServerGamePacketListenerImplAccessor;
import net.blay09.mods.forgivingvoid.mixin.ServerPlayerAccessor;
import net.blay09.mods.forgivingvoid.mixin.ThrownTridentAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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

import java.util.ArrayList;
import java.util.Set;

public class ForgivingVoid {

    public static final String MOD_ID = "forgivingvoid";

    public static void initialize() {
        ConfigLocalization.enableModernTranslationKeys(MOD_ID);
        ForgivingVoidConfig.initialize();

        Balm.getEvents().onEvent(LivingFallEvent.class, ForgivingVoid::onLivingEntityFall);
        Balm.getEvents().onTickEvent(TickType.Entity, TickPhase.Start, ForgivingVoid::onEntityTick);
    }

    public static void onEntityTick(Entity entity) {
        if (!isAllowedEntity(entity)) {
            return;
        }

        int triggerAtY = entity.level().getMinBuildHeight() - ForgivingVoidConfig.getActive().triggerAtDistanceBelow;
        boolean isInVoid = entity.getY() < triggerAtY && entity.yo < triggerAtY;
        boolean isTeleporting = entity instanceof ServerPlayer player && ((ServerGamePacketListenerImplAccessor) player.connection).getAwaitingPositionFromClient() != null;
        CompoundTag persistentData = Balm.getHooks().getPersistentData(entity);
        if (entity.onGround()) {
            persistentData.putLong("LastGroundedPos", entity.blockPosition().asLong());
        }

        if (isInVoid && !isTeleporting && isEnabledForDimension(entity.level().dimension()) && fireForgivingVoidEvent(entity)) {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 3));
            }

            final var entitiesToTeleport = new ArrayList<Entity>();
            entitiesToTeleport.add(entity);
            if (entity.isVehicle()) {
                entitiesToTeleport.addAll(entity.getPassengers());
                entity.ejectPassengers();
            }

            final var vehicle = entity.getVehicle();
            if (vehicle != null) {
                entitiesToTeleport.add(vehicle);
                entity.stopRiding();
            }

            entitiesToTeleport.forEach(teleportedEntity -> {
                if (isAllowedEntity(teleportedEntity)) {
                    if (teleportedEntity instanceof ServerPlayerAccessor player) {
                        player.setIsChangingDimension(true);
                    }
                    final var entityPersistentData = Balm.getHooks().getPersistentData(teleportedEntity);
                    teleportedEntity.teleportTo(teleportedEntity.getX(), ForgivingVoidConfig.getActive().fallingHeight, teleportedEntity.getZ());
                    final var returnToGrounded = ForgivingVoidConfig.getActive().returnToLastGrounded;
                    final var lastGroundedPos = entityPersistentData.contains("LastGroundedPos") ? BlockPos.of(entityPersistentData.getLong("LastGroundedPos")) : entity.blockPosition();
                    final var x = returnToGrounded ? lastGroundedPos.getX() + 0.5f : entity.getX();
                    final var y = ForgivingVoidConfig.getActive().fallingHeight;
                    final var z = returnToGrounded ? lastGroundedPos.getZ() + 0.5f : entity.getZ();
                    entity.teleportTo(x, y, z);
                    entityPersistentData.putBoolean("ForgivingVoidIsFalling", true);
                }
            });

            if (vehicle != null) {
                entity.startRiding(vehicle);
            }
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
        if (entity.level().isClientSide) {
            return false;
        }

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
        if (entity instanceof LivingEntity livingEntity && livingEntity.isFallFlying()) {
            return true;
        }

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
                final var config = ForgivingVoidConfig.getActive();
                final var damage = calculateFallDamage(config, entity);
                event.setFallDamageOverride(damage);

                if (entity instanceof ServerPlayerAccessor player) {
                    player.setIsChangingDimension(false);
                }
            }
        }
    }

    private static float calculateFallDamage(ForgivingVoidConfigData config, LivingEntity entity) {
        float damage = config.damageOnFall;
        // We normalize percentages if the user accidentally set a value out of 100.
        if (config.damageOnFallMode != DamageOnFallMode.ABSOLUTE && damage > 1) {
            damage = damage / 100f;
        }
        if (config.damageOnFallMode == DamageOnFallMode.RELATIVE_CURRENT) {
            damage = entity.getHealth() * damage;
        } else if (config.damageOnFallMode == DamageOnFallMode.RELATIVE_MAX) {
            damage = entity.getMaxHealth() * damage;
        }
        if (config.preventDeath && entity.getHealth() - damage <= 0) {
            damage = entity.getHealth() - 1f;
        }
        return damage;
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
