package net.blay09.mods.forgivingvoid;

import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(ForgivingVoid.MOD_ID)
public class NeoForgeForgivingVoid {

    public NeoForgeForgivingVoid(ModContainer modContainer, IEventBus modEventBus) {
        final var context = new NeoForgeLoadContext(modContainer, modEventBus);
        Balm.initializeMod(ForgivingVoid.MOD_ID, context, ForgivingVoid::initialize);
    }

}
