package net.blay09.mods.forgivingvoid.compat;

import com.google.common.base.Strings;
import net.blay09.mods.forgivingvoid.ForgivingVoidEvent;
import net.blay09.mods.forgivingvoid.ForgivingVoidConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class GameStagesCompat {

    public GameStagesCompat() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onForgivingVoid(ForgivingVoidEvent event) {
        String requiredStage = ForgivingVoidConfig.COMMON.requiredGameStage.get();
        if (Strings.isNullOrEmpty(requiredStage)) {
            return;
        }

        PlayerEntity player = event.getPlayer();
        // TODO Re-enable when GameStages is available
//        if (!GameStageHelper.hasStage(player, requiredStage)) {
//            event.setCanceled(true);
//        }
    }
}
