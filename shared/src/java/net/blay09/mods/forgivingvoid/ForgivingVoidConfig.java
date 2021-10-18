package net.blay09.mods.forgivingvoid;

import net.blay09.mods.balm.api.Balm;

public class ForgivingVoidConfig {
    public static ForgivingVoidConfigData getActive() {
        return Balm.getConfig().getActive(ForgivingVoidConfigData.class);
    }

    public static void initialize() {
        Balm.getConfig().registerConfig(ForgivingVoidConfigData.class, null);
    }

}
