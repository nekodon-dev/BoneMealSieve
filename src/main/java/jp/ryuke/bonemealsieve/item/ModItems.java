package jp.ryuke.bonemealsieve.item;

import jp.ryuke.bonemealsieve.BoneMealSieveMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public final class ModItems {
    public static final FilteredBoneMealItem POOR_BONE_MEAL = register(
            "poor_bone_meal",
            properties -> new FilteredBoneMealItem(properties, FilteredBoneMealItem.Mode.GRASS_ONLY)
    );

    public static final FilteredBoneMealItem QUALITY_BONE_MEAL = register(
            "quality_bone_meal",
            properties -> new FilteredBoneMealItem(properties, FilteredBoneMealItem.Mode.FLOWERS_ONLY)
    );

    private ModItems() {
    }

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(BoneMealSieveMod.MOD_ID, name)
        );
        T item = factory.apply(new Item.Properties().setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    public static void initialize() {
        // Static field initialization performs registration.
    }
}
