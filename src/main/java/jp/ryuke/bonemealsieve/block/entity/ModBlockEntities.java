package jp.ryuke.bonemealsieve.block.entity;

import jp.ryuke.bonemealsieve.BoneMealSieveMod;
import jp.ryuke.bonemealsieve.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<SieveBlockEntity> SIEVE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(BoneMealSieveMod.MOD_ID, "sieve"),
            FabricBlockEntityTypeBuilder.create(SieveBlockEntity::new, ModBlocks.SIEVE).build());

    private ModBlockEntities() {}

    public static void initialize() {}
}
