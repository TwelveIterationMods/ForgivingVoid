package net.blay09.mods.forgivingvoid;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ForgivingVoid.MOD_ID)
@Mod.EventBusSubscriber
public class ForgivingVoid {

    public static final String MOD_ID = "forgivingvoid";

    public ForgivingVoid() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // TODO Awaiting Forge config COMMON fix
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ForgivingVoidConfig.commonSpec);
    }

    public void setup(FMLCommonSetupEvent event) {
        ModList.get().getModObjectById("gamestages").ifPresent(it -> {
            // TODO Awaiting Game Stages port, and see if it actually works like this without crashing when not installed
//            new GameStagesCompat()
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.START) {
            if (isEnabledForDimension(event.player.dimension.getId()) && event.player.posY < ForgivingVoidConfig.COMMON.triggerAtY.get() && fireForgivingVoidEvent(event.player)) {
                event.player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 3));
                if (event.player.isBeingRidden()) {
                    event.player.removePassengers();
                }

                if (event.player.getRidingEntity() != null) {
                    event.player.stopRiding();
                }

                ((EntityPlayerMP) event.player).invulnerableDimensionChange = true;
                event.player.setPositionAndUpdate(event.player.posX, ForgivingVoidConfig.COMMON.fallingHeight.get(), event.player.posZ);
                event.player.getEntityData().setBoolean("ForgivingVoidNoFallDamage", true);
            } else if (event.player.getEntityData().getBoolean("ForgivingVoidNoFallDamage")) {
                // LivingFallEvent is not called when the player falls into water, so reset it manually - water means no damage at all.
                if (event.player.isInWater()) {
                    event.player.getEntityData().setBoolean("ForgivingVoidNoFallDamage", false);
                    ((EntityPlayerMP) event.player).invulnerableDimensionChange = false;
                    return;
                }

                if (ForgivingVoidConfig.COMMON.disableVanillaAntiCheatWhileFalling.get()) {
                    // Vanilla's AntiCheat is a dumb, absolutely terrible. Triggers on falling and teleports, even in Vanilla.
                    // So I'll just disable it until the player lands, so it doesn't look like it's my mod causing the issue.
                    ((EntityPlayerMP) event.player).invulnerableDimensionChange = true;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPlayerFall(LivingFallEvent event) {
        if (event.getEntity() instanceof EntityPlayerMP) {
            if (event.getEntity().getEntityData().getBoolean("ForgivingVoidNoFallDamage")) {
                if (ForgivingVoidConfig.COMMON.disableVanillaAntiCheatWhileFalling.get()) {
                    ((EntityPlayerMP) event.getEntity()).invulnerableDimensionChange = false;
                }

                if (!event.isCanceled()) {
                    float damage = ForgivingVoidConfig.COMMON.damageOnFall.get();
                    if (ForgivingVoidConfig.COMMON.preventDeath.get() && event.getEntityLiving().getHealth() - damage <= 0) {
                        damage = event.getEntityLiving().getHealth() - 1f;
                    }
                    float finalDamage = damage * Math.max(1, event.getDamageMultiplier());
                    if (finalDamage > 0f) {
                        event.getEntity().attackEntityFrom(DamageSource.FALL, finalDamage);
                    }
                }

                event.setDamageMultiplier(0f);
                event.setCanceled(true);
                event.getEntity().getEntityData().setBoolean("ForgivingVoidNoFallDamage", false);
            }
        }
    }

    private static boolean fireForgivingVoidEvent(EntityPlayer player) {
        return !MinecraftForge.EVENT_BUS.post(new ForgivingVoidEvent(player));
    }

    private static boolean isEnabledForDimension(int dimension) {
        if (dimension == 0) {
            return ForgivingVoidConfig.COMMON.triggerInOverworld.get();
        } else if (dimension == 1) {
            return ForgivingVoidConfig.COMMON.triggerInEnd.get();
        } else if (dimension == -1) {
            return ForgivingVoidConfig.COMMON.triggerInNether.get();
        } else {
            return ForgivingVoidConfig.COMMON.dimensionBlacklistIsWhitelist.get() == ForgivingVoidConfig.COMMON.dimensionBlacklist.get().contains(dimension);
        }
    }

}
