package net.blay09.mods.forgivingvoid;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = ForgivingVoid.MOD_ID, name = "Forgiving Void")
@Mod.EventBusSubscriber
public class ForgivingVoid {

	public static final String MOD_ID = "forgivingvoid";

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		event.player.world.profiler.startSection("ForgivingVoidTick");
		if(event.side == Side.SERVER/* && event.phase == TickEvent.Phase.START*/) {
			if(event.player.posY < ModConfig.triggerAtY) {
				event.player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 3));
				event.player.setPositionAndUpdate(event.player.posX, ModConfig.fallingHeight, event.player.posZ);
				event.player.getEntityData().setBoolean("ForgivingVoidNoFallDamage", true);
			}
		}
		event.player.world.profiler.endSection();
	}

	@SubscribeEvent
	public static void onPlayerFall(LivingFallEvent event) {
		event.getEntity().world.profiler.startSection("ForgivingVoidFall");
		if(event.getEntity() instanceof EntityPlayer) {
			if(event.getEntity().getEntityData().getBoolean("ForgivingVoidNoFallDamage")) {
				float damage = ModConfig.damageOnFall;
				if(ModConfig.preventDeath && event.getEntityLiving().getHealth() - damage <= 0) {
					damage = event.getEntityLiving().getHealth() - 1f;
				}
				event.getEntity().attackEntityFrom(DamageSource.FALL, damage);
				event.setDamageMultiplier(0f);
				event.setCanceled(true);
				event.getEntity().getEntityData().setBoolean("ForgivingVoidNoFallDamage", false);
			}
		}
		event.getEntity().world.profiler.endSection();
	}

	@Config(modid = MOD_ID)
	public static class ModConfig {
		@Config.Comment("The y level at which Forgiving Void should forgive the player and send them towards the sky.")
		@Config.RangeInt(min = -64, max = 0)
		public static int triggerAtY = -32;

		@Config.Comment("The amount of damage applied to the player when they land.")
		@Config.RangeInt(min = 0, max = 20)
		public static int damageOnFall = 19;

		@Config.Comment("Prevent death on void fall (limits damage to leave at least 0.5 hearts)")
		public static boolean preventDeath = false;

		@Config.Comment("The height from which the player will be falling after falling through the void.")
		@Config.RangeInt(min = 256, max = 4096)
		public static int fallingHeight = 300;
	}
}
