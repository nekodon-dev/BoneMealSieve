package jp.ryuke.bonemealsieve;

import jp.ryuke.bonemealsieve.block.ModBlocks;
import jp.ryuke.bonemealsieve.block.SieveBlock;
import jp.ryuke.bonemealsieve.block.entity.SieveBlockEntity;
import jp.ryuke.bonemealsieve.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

public class SieveGameTests {
    private static final BlockPos POS = new BlockPos(1, 2, 1);

    private SieveBlockEntity placeSieve(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.SIEVE);
        return helper.getBlockEntity(POS, SieveBlockEntity.class);
    }

    private void tick(GameTestHelper helper, SieveBlockEntity sieve, int count) {
        for (int i = 0; i < count; i++) {
            SieveBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(POS), sieve.getBlockState(), sieve);
        }
    }

    private int results(HopperBlockEntity hopper) {
        return hopper.countItem(ModItems.POOR_BONE_MEAL) + hopper.countItem(ModItems.QUALITY_BONE_MEAL);
    }

    @GameTest
    public void takesTwentyTicksAndDropsBelowMesh(GameTestHelper helper) {
        SieveBlockEntity sieve = placeSieve(helper);
        sieve.insertOne(new ItemStack(Items.BONE_MEAL));
        tick(helper, sieve, 19);
        helper.assertTrue(sieve.getItem(0).getCount() == 1, "Must not finish before 20 ticks");
        helper.assertEntityNotPresent(EntityType.ITEM);
        tick(helper, sieve, 1);
        helper.assertTrue(sieve.isEmpty(), "Exactly one input must be processed");
        var drops = helper.getEntities(EntityType.ITEM, POS, 1);
        helper.assertTrue(drops.size() == 1, "One result must be dropped");
        var drop = drops.getFirst();
        helper.assertTrue(drop.getBoundingBox().maxY < helper.absolutePos(POS).getY() + 12.25 / 16.0,
                "Result must start entirely below the mesh");
        helper.assertTrue(drop.getDeltaMovement().y < 0, "Result must move downward");
        helper.succeed();
    }

    @GameTest
    public void rightClickQueuesAndConsumesOne(GameTestHelper helper) {
        SieveBlockEntity sieve = placeSieve(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 2));
        helper.useBlock(POS, player);
        helper.assertTrue(sieve.getItem(0).getCount() == 1, "Right click must queue one bone meal");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "Right click must consume one bone meal");
        helper.assertEntityNotPresent(EntityType.ITEM);
        tick(helper, sieve, 20);
        helper.assertEntitiesPresent(EntityType.ITEM, 1);
        helper.succeed();
    }

    @GameTest(maxTicks = 140)
    public void upperAndLowerHoppersTransferWithoutDrops(GameTestHelper helper) {
        placeSieve(helper);
        helper.setBlock(POS.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(POS.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        var upper = helper.getBlockEntity(POS.above(), HopperBlockEntity.class);
        var lower = helper.getBlockEntity(POS.below(), HopperBlockEntity.class);
        upper.setItem(0, new ItemStack(Items.BONE_MEAL, 3));
        upper.setItem(1, new ItemStack(Items.DIRT));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(results(lower) == 3, "All three bone meals must reach the lower hopper");
            helper.assertTrue(upper.getItem(1).is(Items.DIRT), "Other items must remain in upper hopper");
            helper.assertEntityNotPresent(EntityType.ITEM);
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void fullHopperRetainsOutputAndPauses(GameTestHelper helper) {
        SieveBlockEntity sieve = placeSieve(helper);
        helper.setBlock(POS.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        var hopper = helper.getBlockEntity(POS.below(), HopperBlockEntity.class);
        for (int i = 0; i < hopper.getContainerSize(); i++) hopper.setItem(i, new ItemStack(Items.DIRT, 64));
        sieve.setItem(0, new ItemStack(Items.BONE_MEAL, 2));
        tick(helper, sieve, 60);
        helper.assertTrue(sieve.getItem(0).getCount() == 1, "Blocked output must stop further consumption");
        helper.assertTrue(sieve.getItem(1).getCount() == 1, "Blocked result must be retained");
        helper.assertEntityNotPresent(EntityType.ITEM);
        hopper.clearContent();
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(results(hopper) == 2, "Processing must resume after hopper is emptied");
            helper.assertTrue(sieve.isEmpty(), "No input or output may be duplicated");
            helper.assertEntityNotPresent(EntityType.ITEM);
            helper.succeed();
        });
    }

    @GameTest
    public void grassSurvivesAndAxePropertiesMatchPlanks(GameTestHelper helper) {
        helper.setBlock(POS.below(), Blocks.GRASS_BLOCK);
        placeSieve(helper);
        for (int i = 0; i < 100; i++) helper.randomTick(POS.below());
        helper.assertBlockPresent(Blocks.GRASS_BLOCK, POS.below());
        helper.assertBlockTag(BlockTags.MINEABLE_WITH_AXE, POS);
        helper.assertTrue(ModBlocks.SIEVE.defaultDestroyTime() == Blocks.OAK_PLANKS.defaultDestroyTime(),
                "Hardness must match oak planks");
        helper.assertTrue(ModBlocks.SIEVE.getExplosionResistance() == Blocks.OAK_PLANKS.getExplosionResistance(),
                "Blast resistance must match oak planks");
        helper.assertFalse(ModBlocks.SIEVE.defaultBlockState().ignitedByLava(), "Placed sieve must not ignite");
        helper.succeed();
    }

    @GameTest
    public void waterPlacementAndBucketKeepInventory(GameTestHelper helper) {
        helper.setBlock(POS.below(), Blocks.STONE);
        helper.setBlock(POS, Blocks.WATER);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.placeAt(player, new ItemStack(ModBlocks.SIEVE), POS.below(), Direction.UP);
        helper.assertBlockPresent(ModBlocks.SIEVE, POS);
        helper.assertBlockProperty(POS, SieveBlock.WATERLOGGED, true);
        helper.assertTrue(helper.getBlockState(POS).getFluidState().getType() == Fluids.WATER,
                "Underwater placement must preserve water");
        var sieve = helper.getBlockEntity(POS, SieveBlockEntity.class);
        sieve.insertOne(new ItemStack(Items.BONE_MEAL));
        var block = (SieveBlock) ModBlocks.SIEVE;
        block.pickupBlock(player, helper.getLevel(), helper.absolutePos(POS), helper.getBlockState(POS));
        helper.assertBlockProperty(POS, SieveBlock.WATERLOGGED, false);
        helper.assertTrue(helper.getBlockEntity(POS, SieveBlockEntity.class).getItem(0).getCount() == 1,
                "Changing water state must preserve inventory");
        helper.assertEntityNotPresent(EntityType.ITEM);
        helper.succeed();
    }

    @GameTest
    public void breakingReturnsStoredContents(GameTestHelper helper) {
        SieveBlockEntity sieve = placeSieve(helper);
        sieve.setItem(0, new ItemStack(Items.BONE_MEAL, 3));
        sieve.setItem(1, new ItemStack(ModItems.QUALITY_BONE_MEAL));
        helper.destroyBlock(POS);
        helper.assertItemEntityCountIs(Items.BONE_MEAL, POS, 2, 3);
        helper.assertItemEntityCountIs(ModItems.QUALITY_BONE_MEAL, POS, 2, 1);
        helper.succeed();
    }

    @GameTest
    public void hopperOutputKeepsTwentyTickThroughput(GameTestHelper helper) {
        var sieve = placeSieve(helper);
        helper.setBlock(POS.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH));
        var hopper = helper.getBlockEntity(POS.below(), HopperBlockEntity.class);
        sieve.setItem(0, new ItemStack(Items.BONE_MEAL, 3));
        tick(helper, sieve, 59);
        helper.assertTrue(results(hopper) == 2, "Two results after 59 ticks");
        tick(helper, sieve, 1);
        helper.assertTrue(results(hopper) == 3, "Three results after exactly 60 ticks");
        helper.assertEntityNotPresent(EntityType.ITEM);
        helper.succeed();
    }

    @GameTest
    public void savingRestoresInventoryAndProgress(GameTestHelper helper) {
        var sieve = placeSieve(helper);
        sieve.setItem(0, new ItemStack(Items.BONE_MEAL, 2));
        tick(helper, sieve, 12);
        var registries = helper.getLevel().registryAccess();
        var saved = sieve.saveWithoutMetadata(registries);
        var restored = new SieveBlockEntity(helper.absolutePos(POS), sieve.getBlockState());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));
        tick(helper, restored, 7);
        helper.assertTrue(restored.getItem(0).getCount() == 2, "Saved progress must not finish early");
        tick(helper, restored, 1);
        helper.assertTrue(restored.getItem(0).getCount() == 1, "Saved progress must finish after remaining eight ticks");
        helper.assertEntitiesPresent(EntityType.ITEM, 1);
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void droppedSieveIsDestroyedInLava(GameTestHelper helper) {
        helper.setBlock(POS.below(), Blocks.STONE);
        helper.setBlock(POS, Blocks.LAVA);
        var item = helper.spawnItem(ModBlocks.SIEVE.asItem(), 1.5F, 2.2F, 1.5F);
        item.setDeltaMovement(0, 0, 0);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(item.isRemoved(), "Dropped sieve must be destroyed by lava");
            helper.succeed();
        });
    }

    @GameTest
    public void adjacentFireDoesNotBurnSieve(GameTestHelper helper) {
        placeSieve(helper);
        BlockPos firePos = POS.east();
        helper.setBlock(firePos.below(), Blocks.NETHERRACK);
        helper.setBlock(firePos, Blocks.FIRE);
        for (int i = 0; i < 500; i++) helper.tickBlock(firePos);
        helper.assertBlockPresent(ModBlocks.SIEVE, POS);
        helper.succeed();
    }
}
