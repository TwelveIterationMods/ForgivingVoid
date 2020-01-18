package net.blay09.mods.forgivingvoid;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Mod(modid = ForgivingVoid.MOD_ID, name = "Forgiving Void", acceptableRemoteVersions = "*")
@Mod.EventBusSubscriber
public class ForgivingVoid {

    public static final String MOD_ID = "forgivingvoid";
    private static Logger logger = LogManager.getLogger(MOD_ID);

    private static List<Integer> dimensionBlacklist = Lists.newArrayList();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        updateDimensionBlacklist();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        event.buildSoftDependProxy("gamestages", "net.blay09.mods.forgivingvoid.compat.GameStagesCompat");
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == Side.SERVER && event.phase == TickEvent.Phase.START) {
            boolean isInVoid = event.player.posY < ModConfig.triggerAtY && event.player.prevPosY < ModConfig.triggerAtY;
            boolean isTeleporting = ((EntityPlayerMP) event.player).connection.targetPos != null;
            if (isEnabledForDimension(event.player.dimension) && isInVoid && !isTeleporting && fireForgivingVoidEvent(event.player)) {
                event.player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 3));
                if (event.player.isBeingRidden()) {
                    event.player.removePassengers();
                }

                event.player.dismountRidingEntity();
                ((EntityPlayerMP) event.player).connection.setPlayerLocation(event.player.posX, (double) ModConfig.fallingHeight, event.player.posZ, event.player.rotationYaw, event.player.rotationPitch);
                event.player.getEntityData().setBoolean("ForgivingVoidNoFallDamage", true);
            } else if (event.player.getEntityData().getBoolean("ForgivingVoidNoFallDamage")) {
                // LivingFallEvent is not called when the player falls into water, so reset it manually - water means no damage at all.
                if (event.player.isInWater() || event.player.isInWeb || event.player.capabilities.isFlying || event.player.capabilities.allowFlying) {
                    event.player.getEntityData().setBoolean("ForgivingVoidNoFallDamage", false);
                    ((EntityPlayerMP) event.player).invulnerableDimensionChange = false;
                    return;
                }

                if (ModConfig.disableVanillaAntiCheatWhileFalling) {
                    // Vanilla's AntiCheat is dumb, absolutely terrible. Triggers on falling and teleports, even in Vanilla.
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
                if (ModConfig.disableVanillaAntiCheatWhileFalling) {
                    ((EntityPlayerMP) event.getEntity()).invulnerableDimensionChange = false;
                }

                if (!event.isCanceled()) {
                    float damage = ModConfig.damageOnFall;
                    if (ModConfig.preventDeath && event.getEntityLiving().getHealth() - damage <= 0) {
                        damage = event.getEntityLiving().getHealth() - 1f;
                    }
                    float finalDamage = damage * event.getDamageMultiplier();
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

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (MOD_ID.equals(event.getModID())) {
            ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);
            updateDimensionBlacklist();
        }
    }

    private static void updateDimensionBlacklist() {
        dimensionBlacklist.clear();
        for (String dimension : ModConfig.dimensionBlacklist) {
            try {
                dimensionBlacklist.add(Integer.parseInt(dimension));
            } catch (NumberFormatException e) {
                logger.error("Invalid dimension blacklist entry {}, expected numeric id", dimension);
            }
        }
    }

    private static boolean fireForgivingVoidEvent(EntityPlayer player) {
        return !MinecraftForge.EVENT_BUS.post(new ForgivingVoidEvent(player));
    }

    private static boolean isEnabledForDimension(int dimension) {
        if (dimension == 0) {
            return ModConfig.triggerInOverworld;
        } else if (dimension == 1) {
            return ModConfig.triggerInEnd;
        } else if (dimension == -1) {
            return ModConfig.triggerInNether;
        } else {
            return ModConfig.dimensionBlacklistIsWhitelist == dimensionBlacklist.contains(dimension);
        }
    }

}
