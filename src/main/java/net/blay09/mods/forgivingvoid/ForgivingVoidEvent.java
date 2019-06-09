package net.blay09.mods.forgivingvoid;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class ForgivingVoidEvent extends Event {

    private final PlayerEntity player;

    public ForgivingVoidEvent(PlayerEntity player) {
        this.player = player;
    }

    public PlayerEntity getPlayer() {
        return player;
    }
}
