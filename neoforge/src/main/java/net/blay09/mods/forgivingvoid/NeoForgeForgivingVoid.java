package net.blay09.mods.forgivingvoid;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.neoforged.bus.EventBus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;

@Mod(ForgivingVoid.MOD_ID)
public class NeoForgeForgivingVoid {

    public NeoForgeForgivingVoid(IEventBus modEventBus) {
        final var context = new NeoForgeLoadContext(modEventBus);
        Balm.initialize(ForgivingVoid.MOD_ID, context, ForgivingVoid::initialize);
    }

}
