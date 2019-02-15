package net.blay09.mods.forgivingvoid;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class ForgivingVoidEvent extends Event {

    private final EntityPlayer player;

    public ForgivingVoidEvent(EntityPlayer player) {
        this.player = player;
    }

    public EntityPlayer getPlayer() {
        return player;
    }
}
