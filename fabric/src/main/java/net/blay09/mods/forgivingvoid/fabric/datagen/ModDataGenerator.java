package net.blay09.mods.forgivingvoid.fabric.datagen;

import net.blay09.mods.balm.client.platform.util.I18nExport;
import net.blay09.mods.forgivingvoid.ForgivingVoid;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

import java.io.File;

public class ModDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        I18nExport.writeStaticI18nKeys(ForgivingVoid.MOD_ID, new File("i18n.export.json"));
    }
}
