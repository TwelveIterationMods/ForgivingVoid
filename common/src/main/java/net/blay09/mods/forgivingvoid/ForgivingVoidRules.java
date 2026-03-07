package net.blay09.mods.forgivingvoid;

import net.blay09.mods.shogi.Shogi;
import net.blay09.mods.shogi.ShogiValue;
import net.minecraft.world.entity.Entity;

import static net.blay09.mods.forgivingvoid.ForgivingVoid.id;

public class ForgivingVoidRules {
    public static final ShogiValue<Entity, Integer> fallingHeight = Shogi.intValue(id("falling_height"), entity -> entity.level().getHeight());
}
