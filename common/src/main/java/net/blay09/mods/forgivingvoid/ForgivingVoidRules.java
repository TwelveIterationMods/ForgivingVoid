package net.blay09.mods.forgivingvoid;

import net.blay09.mods.shogi.Shogi;
import net.blay09.mods.shogi.ShogiValue;
import net.blay09.mods.shogi.context.MutableShogiContext;
import net.blay09.mods.shogi.context.ShogiContext;
import net.minecraft.world.entity.Entity;

import static net.blay09.mods.forgivingvoid.ForgivingVoid.id;

public class ForgivingVoidRules {
    public static final ShogiValue<ShogiContext, Integer> fallingHeight = Shogi.intValue(id("falling_height"), context -> context.requireLevel().getHeight());

    public static ShogiContext makeContext(Entity entity) {
        return makeContext(entity, ForgivingVoid.getLoops(entity));
    }

    public static ShogiContext makeContext(Entity entity, int loops) {
        return MutableShogiContext.of(entity)
                .withVariable("loops", loops);
    }
}
