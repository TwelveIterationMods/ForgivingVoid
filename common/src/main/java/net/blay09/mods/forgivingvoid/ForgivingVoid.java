package net.blay09.mods.forgivingvoid;

import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.core.BalmRegistrars;
import net.blay09.mods.balm.platform.event.callback.LivingEntityCallback;
import net.blay09.mods.balm.platform.event.callback.ServerTickCallback;
import net.blay09.mods.forgivingvoid.mixin.ServerGamePacketListenerImplAccessor;
import net.blay09.mods.forgivingvoid.mixin.ServerPlayerAccessor;
import net.blay09.mods.forgivingvoid.mixin.ThrownTridentAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;

public class ForgivingVoid {
    public static final String TAG_LAST_GROUNDED_POS = "LastGroundedPos";
    public static final String TAG_LOOPS = "Loops";
    public static final String TAG_IS_FALLING = "ForgivingVoidIsFalling";

    public static final String MOD_ID = "forgivingvoid";

    public static final Logger logger = LoggerFactory.getLogger(ForgivingVoid.class);

    public static void initialize(BalmRegistrars registrars) {
        ForgivingVoidConfig.initialize();

        ServerTickCallback.ServerEntityTick.BEFORE.register(ForgivingVoid::onEntityTick);
        LivingEntityCallback.Fall.Before.EVENT.register(ForgivingVoid::onLivingEntityFall);
    }

    public static void onEntityTick(Entity entity) {
        if (!isAllowedEntity(entity)) {
            return;
        }

        int triggerAtY = entity.level().getMinY() - ForgivingVoidConfig.getActive().triggerAtDistanceBelow;
        boolean isInVoid = entity.getY() < triggerAtY && entity.yo < triggerAtY;
        boolean isTeleporting = entity instanceof ServerPlayer player && ((ServerGamePacketListenerImplAccessor) player.connection).getAwaitingPositionFromClient() != null;
        CompoundTag persistentData = Balm.hooks().getPersistentData(entity);
        if (entity.onGround()) {
            persistentData.putLong(TAG_LAST_GROUNDED_POS, entity.blockPosition().asLong());
            persistentData.putInt(TAG_LOOPS, 0);
        }

        if (isInVoid && !isTeleporting && isEnabledForDimension(entity.level().dimension()) && fireForgivingVoidEvent(entity)) {
            if (entity instanceof LivingEntity livingEntity) {
                applyFallThroughVoidEffects(livingEntity);
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
                    final var teleportedEntityData = Balm.hooks().getPersistentData(teleportedEntity);
                    final var loops = teleportedEntityData.getIntOr(TAG_LOOPS, 0) + 1;
                    teleportedEntityData.putInt(TAG_LOOPS, loops);
                    final var returnToGrounded = ForgivingVoidConfig.getActive().returnToLastGrounded;
                    final var lastGroundedPos = teleportedEntityData.getLong(TAG_LAST_GROUNDED_POS).map(BlockPos::of).orElseGet(teleportedEntity::blockPosition);
                    final var x = returnToGrounded ? lastGroundedPos.getX() + 0.5f : teleportedEntity.getX();
                    final var y = ForgivingVoidRules.fallingHeight.getOrDefault(ForgivingVoidRules.makeContext(teleportedEntity, loops));
                    final var z = returnToGrounded ? lastGroundedPos.getZ() + 0.5f : teleportedEntity.getZ();
                    teleportedEntity.teleportTo(x, y, z);
                    teleportedEntityData.putBoolean(TAG_IS_FALLING, true);
                }
            });

            if (vehicle != null) {
                entity.startRiding(vehicle);
            }
        } else if (persistentData.getBooleanOr(TAG_IS_FALLING, false)) {
            // LivingFallEvent is not called when the player falls into water or is flying, so reset it manually - and give no damage at all.
            if (hasLanded(entity) || isOrMayFly(entity)) {
                persistentData.putInt(TAG_LOOPS, 0);
                persistentData.putBoolean(TAG_IS_FALLING, false);
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

    private static void applyFallThroughVoidEffects(LivingEntity entity) {
        for (String effectString : ForgivingVoidConfig.getActive().fallThroughVoidEffects) {
            String[] parts = effectString.split("\\|");
            Identifier registryName = Identifier.tryParse(parts[0]);
            if (registryName != null) {
                final var holder = BuiltInRegistries.MOB_EFFECT.get(registryName);
                if (holder.isPresent()) {
                    int duration = tryParseInt(parts.length >= 2 ? parts[1] : null, 600);
                    int amplifier = tryParseInt(parts.length >= 3 ? parts[2] : null, 0);
                    entity.addEffect(new MobEffectInstance(holder.get(), duration, amplifier));
                } else {
                    ForgivingVoid.logger.info("Invalid fall through void effect '{}'", parts[0]);
                }
            } else {
                ForgivingVoid.logger.info("Invalid fall through void effect '{}'", parts[0]);
            }
        }
    }

    private static int tryParseInt(@Nullable String text, int defaultVal) {
        if (text != null) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return defaultVal;
            }
        }
        return defaultVal;
    }

    private static boolean isAllowedEntity(Entity entity) {
        if (entity.level().isClientSide()) {
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

    public static float onLivingEntityFall(LivingEntity entity, float originalDamage) {
        if (isAllowedEntity(entity) && originalDamage > 0) {
            CompoundTag persistentData = Balm.hooks().getPersistentData(entity);
            if (persistentData.getBooleanOr(TAG_IS_FALLING, false)) {
                final var config = ForgivingVoidConfig.getActive();
                final var newDamage = calculateFallDamage(config, entity, originalDamage);

                if (entity instanceof ServerPlayerAccessor player) {
                    player.setIsChangingDimension(false);
                }

                return newDamage;
            }
        }

        return originalDamage;
    }

    private static float calculateFallDamage(ForgivingVoidConfig config, LivingEntity entity, float originalDamage) {
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
        return Math.min(damage, originalDamage);
    }

    private static boolean fireForgivingVoidEvent(Entity entity) {
        ForgivingVoidFallThroughEvent event = new ForgivingVoidFallThroughEvent(entity);
        ForgivingVoidFallThroughEvent.EVENT.invoker().accept(event);
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
            final Identifier dimension = dimensionKey.identifier();
            final var dimensionAllowList = ForgivingVoidConfig.getActive().dimensionAllowList;
            final var dimensionDenyList = ForgivingVoidConfig.getActive().dimensionDenyList;
            if (!dimensionAllowList.isEmpty() && !dimensionAllowList.contains(dimension)) {
                return false;
            } else {
                return !dimensionDenyList.contains(dimension);
            }
        }
    }

    public static int getLoops(Entity entity) {
        return Balm.hooks().getPersistentData(entity).getIntOr(TAG_LOOPS, 0);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
