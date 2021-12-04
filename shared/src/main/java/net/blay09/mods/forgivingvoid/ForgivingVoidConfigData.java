package net.blay09.mods.forgivingvoid;

import me.shedaniel.autoconfig.annotation.Config;
import net.blay09.mods.balm.api.config.BalmConfigData;
import net.blay09.mods.balm.api.config.Comment;

import java.util.ArrayList;
import java.util.List;

@Config(name = ForgivingVoid.MOD_ID)
public class ForgivingVoidConfigData implements BalmConfigData {

    @Comment("The y level at which Forgiving Void should forgive the player and send them towards the sky.")
    public int triggerAtY = -96;

    @Comment("The amount of damage applied to the player when they land.")
    public int damageOnFall = 19;

    @Comment("The height from which the player will be falling after falling through the void.")
    public int fallingHeight = 300;

    @Comment("Prevent death on void fall (limits damage to leave at least 0.5 hearts)")
    public boolean preventDeath = false;

    @Comment("Set to false to make Forgiving Void not trigger in the overworld void (dimension 0).")
    public boolean triggerInOverworld = true;

    @Comment("Set to false to make Forgiving Void not trigger in the nether void (dimension -1).")
    public boolean triggerInNether = true;

    @Comment("Set to false to make Forgiving Void not trigger in the end void (dimension 1).")
    public boolean triggerInEnd = true;

    @Comment("Set to true if players are rubber-banding while falling through the void. If you're hosting a public server, you should only do this if you have proper anti-cheat installed.")
    public boolean disableVanillaAntiCheatWhileFalling = true;

    @Comment("List of dimension ids to be allowed for Forgiving Void. Options triggerInOverworld etc. take priority.")
    public List<String> dimensionAllowList = new ArrayList<>();

    @Comment("List of additional dimension ids to be deny-listed from Forgiving Void. Options triggerInOverworld etc. take priority. Ignored if dimensionAllowList is set.")
    public List<String> dimensionDenyList = new ArrayList<>();

}
