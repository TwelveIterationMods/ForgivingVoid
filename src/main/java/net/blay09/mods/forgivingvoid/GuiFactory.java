package net.blay09.mods.forgivingvoid;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;

import java.util.Collections;
import java.util.Set;

public class GuiFactory implements IModGuiFactory {
	@Override
	public void initialize(Minecraft minecraftInstance) {
	}

	@Override
	public boolean hasConfigGui() {
		return true;
	}

	@Override
	public GuiScreen createConfigGui(GuiScreen parentScreen) {
		return new Config(parentScreen);
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return Collections.emptySet();
	}

	public static class Config extends GuiConfig {
		public Config(GuiScreen parentScreen) {
			super(parentScreen, ConfigElement.from(ForgivingVoid.ModConfig.class).getChildElements(), ForgivingVoid.MOD_ID, "config", false, false, "Forgiving Void");
		}
	}
}
