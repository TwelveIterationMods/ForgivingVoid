package net.blay09.mods.forgivingvoid;

import net.blay09.mods.balm.api.event.BalmEvent;
import net.minecraft.world.entity.player.Player;

public class ForgivingVoidFallThroughEvent extends BalmEvent {

    private final Player player;

    public ForgivingVoidFallThroughEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

}
