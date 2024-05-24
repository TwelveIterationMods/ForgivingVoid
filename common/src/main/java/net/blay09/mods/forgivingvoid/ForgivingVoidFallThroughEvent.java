package net.blay09.mods.forgivingvoid;

import net.blay09.mods.balm.api.event.BalmEvent;
import net.minecraft.world.entity.Entity;

public class ForgivingVoidFallThroughEvent extends BalmEvent {

    private final Entity entity;

    public ForgivingVoidFallThroughEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

}
