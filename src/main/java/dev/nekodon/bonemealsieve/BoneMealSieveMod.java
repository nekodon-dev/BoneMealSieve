package dev.nekodon.bonemealsieve;

import dev.nekodon.bonemealsieve.block.ModBlocks;
import dev.nekodon.bonemealsieve.item.ModItems;
import net.fabricmc.api.ModInitializer;
import dev.nekodon.bonemealsieve.config.SieveConfig;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;

public final class BoneMealSieveMod implements ModInitializer {
    public static final String MOD_ID = "bonemeal_sieve";

    @Override
    public void onInitialize() {
        SieveConfig.load();
        ModBlocks.initialize();
        dev.nekodon.bonemealsieve.block.entity.ModBlockEntities.initialize();
        ModItems.initialize();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(output -> output.accept(ModBlocks.SIEVE));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> {
                    output.accept(ModItems.POOR_BONE_MEAL);
                    output.accept(ModItems.QUALITY_BONE_MEAL);
                });
    }
}
