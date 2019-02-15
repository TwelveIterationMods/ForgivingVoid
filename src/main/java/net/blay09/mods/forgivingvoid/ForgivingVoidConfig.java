package net.blay09.mods.forgivingvoid;

import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class ForgivingVoidConfig {

    public static class Common {

        public final ForgeConfigSpec.IntValue triggerAtY;
        public final ForgeConfigSpec.IntValue damageOnFall;
        public final ForgeConfigSpec.IntValue fallingHeight;
        public final ForgeConfigSpec.BooleanValue preventDeath;
        public final ForgeConfigSpec.BooleanValue triggerInOverworld;
        public final ForgeConfigSpec.BooleanValue triggerInNether;
        public final ForgeConfigSpec.BooleanValue triggerInEnd;
        public final ForgeConfigSpec.BooleanValue dimensionBlacklistIsWhitelist;
        public final ForgeConfigSpec.BooleanValue disableVanillaAntiCheatWhileFalling;
        public final ForgeConfigSpec.ConfigValue<List<Integer>> dimensionBlacklist;
        public final ForgeConfigSpec.ConfigValue<String> requiredGameStage;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Configuration for Forgiving Void").push("common");

            triggerAtY = builder
                    .comment("The y level at which Forgiving Void should forgive the player and send them towards the sky.")
                    .translation("forgivingvoid.config.triggerAtY")
                    .defineInRange("triggerAtY", -32, -64, 0);

            damageOnFall = builder
                    .comment("The amount of damage applied to the player when they land.")
                    .translation("forgivingvoid.config.damageOnFall")
                    .defineInRange("damageOnFall", 19, 0, 20);

            fallingHeight = builder
                    .comment("The height from which the player will be falling after falling through the void.")
                    .translation("forgivingvoid.config.fallingHeight")
                    .defineInRange("fallingHeight", 300, 256, 4096);

            preventDeath = builder
                    .comment("Prevent death on void fall (limits damage to leave at least 0.5 hearts)")
                    .translation("forgivingvoid.config.preventDeath")
                    .define("preventDeath", false);

            triggerInOverworld = builder
                    .comment("Set to false to make Forgiving Void not trigger in the overworld void (dimension 0).")
                    .translation("forgivingvoid.config.triggerInOverworld")
                    .define("triggerInOverworld", true);

            triggerInNether = builder
                    .comment("Set to false to make Forgiving Void not trigger in the nether void (dimension -1).")
                    .translation("forgivingvoid.config.triggerInNether")
                    .define("triggerInNether", true);

            triggerInEnd = builder
                    .comment("Set to false to make Forgiving Void not trigger in the end void (dimension 1).")
                    .translation("forgivingvoid.config.triggerInEnd")
                    .define("triggerInEnd", true);

            dimensionBlacklistIsWhitelist = builder
                    .comment("Set to true if you want the dimensionBlacklist to be treated as a whitelist instead. Options triggerInOverworld etc. still take priority.")
                    .translation("forgivingvoid.config.dimensionBlacklistIsWhitelist")
                    .define("dimensionBlacklistIsWhitelist", false);

            disableVanillaAntiCheatWhileFalling = builder
                    .comment("Set to true if players are rubber-banding while falling through the void. If you're hosting a public server, you should only do this if you have proper anti-cheat installed.")
                    .translation("forgivingvoid.config.disableVanillaAntiCheatWhileFalling")
                    .define("disableVanillaAntiCheatWhileFalling", true);

            dimensionBlacklist = builder
                    .comment("List of additional dimension ids to be blacklisted from Forgiving Void. Options triggerInOverworld etc. take priority.")
                    .translation("forgivingvoid.config.dimensionBlacklist")
                    .define("dimensionBlacklist", Lists.newArrayList());

            requiredGameStage = builder
                    .comment("For integration with the Game Stages mod, set this to the game stage that needs to be unlocked in order for Forgiving Void to be enabled.")
                    .translation("forgivingvoid.config.requiredGameStage")
                    .define("requiredGameStage", "");
        }

    }

    static final ForgeConfigSpec commonSpec;
    public static final Common COMMON;
    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }

}
