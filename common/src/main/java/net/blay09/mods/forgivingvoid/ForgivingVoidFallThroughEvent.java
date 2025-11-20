package net.blay09.mods.forgivingvoid;

import net.blay09.mods.balm.platform.event.BidirectionalEventMapper;
import net.blay09.mods.balm.platform.event.EventMapper;
import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

public class ForgivingVoidFallThroughEvent {

    public static final BidirectionalEventMapper<Consumer<ForgivingVoidFallThroughEvent>> EVENT = EventMapper.createBound(ForgivingVoidFallThroughEvent.class);

    private final Entity entity;
    private boolean canceled;

    public ForgivingVoidFallThroughEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
