package net.blay09.mods.forgivingvoid.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ThrownTrident.class)
public interface ThrownTridentAccessor {

    @Accessor("ID_LOYALTY")
    static EntityDataAccessor<Byte> getIdLoyalty() {
        throw new AssertionError();
    }
}
