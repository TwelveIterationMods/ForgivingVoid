package net.blay09.mods.forgivingvoid;

import net.blay09.mods.balm.api.Balm;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;

@Mod(ForgivingVoid.MOD_ID)
public class NeoForgeForgivingVoid {

    public NeoForgeForgivingVoid() {
        Balm.initialize(ForgivingVoid.MOD_ID, ForgivingVoid::initialize);

        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (a, b) -> true));
    }

}
