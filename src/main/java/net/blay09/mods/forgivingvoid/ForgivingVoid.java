package net.blay09.mods.forgivingvoid;

import net.blay09.mods.forgivingvoid.compat.GameStagesCompat;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ForgivingVoid.MOD_ID)
@Mod.EventBusSubscriber
public class ForgivingVoid {

    public static final String MOD_ID = "forgivingvoid";

    public ForgivingVoid() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgivingVoidConfig.commonSpec);
    }

    public void setup(FMLCommonSetupEvent event) {
        ModList.get().getModObjectById("gamestages").ifPresent(it -> new GameStagesCompat());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.START) {
            boolean isInVoid = event.player.getPosY() < ForgivingVoidConfig.COMMON.triggerAtY.get() && event.player.prevPosY < ForgivingVoidConfig.COMMON.triggerAtY.get();
            boolean isTeleporting = ((ServerPlayerEntity) event.player).connection.targetPos != null;
            if (isEnabledForDimension(event.player.world.func_234923_W_()) && isInVoid && !isTeleporting && fireForgivingVoidEvent(event.player)) {
                event.player.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 60, 3));
                if (event.player.isBeingRidden()) {
                    event.player.removePassengers();
                }

                event.player.stopRiding();

                ((ServerPlayerEntity) event.player).invulnerableDimensionChange = true;
                event.player.setPositionAndUpdate(event.player.getPosX(), ForgivingVoidConfig.COMMON.fallingHeight.get(), event.player.getPosZ());
                event.player.getPersistentData().putBoolean("ForgivingVoidNoFallDamage", true);
            } else if (event.player.getPersistentData().getBoolean("ForgivingVoidNoFallDamage")) {
                // LivingFallEvent is not called when the player falls into water or is flying, so reset it manually - and give no damage at all.
                final BlockPos playerPos = event.player.func_233580_cy_();
                if (event.player.isInWater() || event.player.abilities.isFlying || event.player.abilities.allowFlying || event.player.world.getBlockState(playerPos).getBlock() == Blocks.COBWEB) {
                    event.player.getPersistentData().putBoolean("ForgivingVoidNoFallDamage", false);
                    ((ServerPlayerEntity) event.player).invulnerableDimensionChange = false;
                    return;
                }

                if (ForgivingVoidConfig.COMMON.disableVanillaAntiCheatWhileFalling.get()) {
                    // Vanilla's AntiCheat is dumb, absolutely terrible. Triggers on falling and teleports, even in Vanilla.
                    // So I'll just disable it until the player lands, so it doesn't look like it's my mod causing the issue.
                    ((ServerPlayerEntity) event.player).invulnerableDimensionChange = true;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPlayerFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            if (event.getEntity().getPersistentData().getBoolean("ForgivingVoidNoFallDamage")) {
                if (ForgivingVoidConfig.COMMON.disableVanillaAntiCheatWhileFalling.get()) {
                    ((ServerPlayerEntity) event.getEntity()).invulnerableDimensionChange = false;
                }

                if (!event.isCanceled()) {
                    float damage = ForgivingVoidConfig.COMMON.damageOnFall.get();
                    if (ForgivingVoidConfig.COMMON.preventDeath.get() && event.getEntityLiving().getHealth() - damage <= 0) {
                        damage = event.getEntityLiving().getHealth() - 1f;
                    }
                    float finalDamage = damage * event.getDamageMultiplier();
                    if (finalDamage > 0f) {
                        event.getEntity().attackEntityFrom(DamageSource.FALL, finalDamage);
                    }
                }

                event.setDamageMultiplier(0f);
                event.setCanceled(true);
                event.getEntity().getPersistentData().putBoolean("ForgivingVoidNoFallDamage", false);
            }
        }
    }

    private static boolean fireForgivingVoidEvent(PlayerEntity player) {
        return !MinecraftForge.EVENT_BUS.post(new ForgivingVoidEvent(player));
    }

    private static boolean isEnabledForDimension(RegistryKey<World> dimension) {
        if (dimension == World.field_234918_g_) {
            return ForgivingVoidConfig.COMMON.triggerInOverworld.get();
        } else if (dimension == World.field_234920_i_) {
            return ForgivingVoidConfig.COMMON.triggerInEnd.get();
        } else if (dimension == World.field_234919_h_) {
            return ForgivingVoidConfig.COMMON.triggerInNether.get();
        } else {
            final ResourceLocation resourceLocation = dimension.func_240901_a_(); // getResourceLocation()
            return ForgivingVoidConfig.COMMON.dimensionBlacklistIsWhitelist.get() == ForgivingVoidConfig.COMMON.dimensionBlacklist.get().contains(resourceLocation.toString());
        }
    }

}
