package net.blay09.mods.forgivingvoid;

import net.minecraftforge.common.config.Config;

@Config(modid = ForgivingVoid.MOD_ID)
public class ModConfig {
    @Config.Comment("The y level at which Forgiving Void should forgive the player and send them towards the sky.")
    @Config.RangeInt(min = -64, max = 0)
    public static int triggerAtY = -32;

    @Config.Comment("The amount of damage applied to the player when they land.")
    @Config.RangeInt(min = 0)
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

    @Config.Comment("For Game Stages support, set this to the game stage that needs to be unlocked in order for Forgiving Void to be enabled.")
    public static String requiredGameStage = "";
}
