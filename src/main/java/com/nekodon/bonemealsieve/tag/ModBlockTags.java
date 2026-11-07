package com.nekodon.bonemealsieve.tag;

import com.nekodon.bonemealsieve.BoneMealSieveMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    public static final TagKey<Block> QUALITY_CHANCE_TARGETS = create("quality_chance_targets");
    public static final TagKey<Block> DOUBLE_GROWTH_CROPS = create("double_growth_crops");
    public static final TagKey<Block> COMMON_SMALL_FLOWERS = create("common_small_flowers");

    private ModBlockTags() {
    }

    private static TagKey<Block> create(String path) {
        return TagKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(BoneMealSieveMod.MOD_ID, path));
    }
}
