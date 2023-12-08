package net.blay09.mods.forgivingvoid.fabric;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.forgivingvoid.ForgivingVoid;
import net.fabricmc.api.ModInitializer;

public class FabricForgivingVoid implements ModInitializer {
    @Override
    public void onInitialize() {
        Balm.initialize(ForgivingVoid.MOD_ID, ForgivingVoid::initialize);
    }
}
