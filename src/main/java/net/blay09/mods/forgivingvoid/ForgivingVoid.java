package net.blay09.mods.forgivingvoid;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == Side.SERVER && event.phase == TickEvent.Phase.START) {
            if (isEnabledForDimension(event.player.dimension) && event.player.posY < ModConfig.triggerAtY) {
                event.player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 3));
                if (event.player.isBeingRidden()) {
                    event.player.removePassengers();
                }
                if (event.player.isRiding()) {
                    event.player.dismountRidingEntity();
                }
                ((EntityPlayerMP) event.player).invulnerableDimensionChange = true;
                event.player.setPositionAndUpdate(event.player.posX, ModConfig.fallingHeight, event.player.posZ);
                event.player.getEntityData().setBoolean("ForgivingVoidNoFallDamage", true);
            } else if (event.player.getEntityData().getBoolean("ForgivingVoidNoFallDamage")) {
                // LivingFallEvent is not called when the player falls into water, so reset it manually - water means no damage at all.
                if (event.player.isInWater()) {
                    event.player.getEntityData().setBoolean("ForgivingVoidNoFallDamage", false);
                    ((EntityPlayerMP) event.player).invulnerableDimensionChange = false;
                    return;
                }

                if (ModConfig.disableVanillaAntiCheatWhileFalling) {
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
                if (ModConfig.disableVanillaAntiCheatWhileFalling) {
                    ((EntityPlayerMP) event.getEntity()).invulnerableDimensionChange = false;
                }

                if (!event.isCanceled()) {
                    float damage = ModConfig.damageOnFall;
                    if (ModConfig.preventDeath && event.getEntityLiving().getHealth() - damage <= 0) {
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

    @Config(modid = MOD_ID)
    public static class ModConfig {
        @Config.Comment("The y level at which Forgiving Void should forgive the player and send them towards the sky.")
        @Config.RangeInt(min = -64, max = 0)
        public static int triggerAtY = -32;

        @Config.Comment("The amount of damage applied to the player when they land.")
        @Config.RangeInt(min = 0, max = 20)
        public static int damageOnFall = 19;

        @Config.Comment("Prevent death on void fall (limits damage to leave at least 0.5 hearts)")
        public static boolean preventDeath = false;

        @Config.Comment("The height from which the player will be falling after falling through the void.")
        @Config.RangeInt(min = 256, max = 4096)
        public static int fallingHeight = 300;

        @Config.Comment("Set to false to make Forgiving Void not trigger in the overworld void (dimension 0).")
        public static boolean triggerInOverworld = true;

        @Config.Comment("Set to false to make Forgiving Void not trigger in the nether void (dimension -1).")
        public static boolean triggerInNether = true;

        @Config.Comment("Set to false to make Forgiving Void not trigger in the end void (dimension 1).")
        public static boolean triggerInEnd = true;

        @Config.Comment("List of additional dimension ids to be blacklisted from Forgiving Void. Options triggerInOverworld etc. take priority.")
        public static String[] dimensionBlacklist = new String[]{};

        @Config.Comment("Set to true if you want the dimensionBlacklist to be treated as a whitelist instead. Options triggerInOverworld etc. still take priority.")
        public static boolean dimensionBlacklistIsWhitelist = false;

        @Config.Comment("Set to true if players are rubber-banding while falling through the void. If you're hosting a public server, you should only do this if you have proper anti-cheat installed.")
        public static boolean disableVanillaAntiCheatWhileFalling = true;
    }
}
