package net.blay09.mods.forgivingvoid;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum DamageOnFallMode implements StringRepresentable {
    ABSOLUTE,
    RELATIVE_CURRENT,
    RELATIVE_MAX;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
