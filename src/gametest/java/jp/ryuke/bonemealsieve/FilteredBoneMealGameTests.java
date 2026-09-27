package jp.ryuke.bonemealsieve;

import jp.ryuke.bonemealsieve.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BigDripleafBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class FilteredBoneMealGameTests {
    private static final BlockPos TARGET = new BlockPos(3, 2, 3);

    private void use(GameTestHelper helper, BlockPos pos, Item item) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        helper.useBlock(pos, player);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "Successful custom growth must consume one bone meal");
    }

    @GameTest
    public void qualityBoneMealAdvancesCropsTwice(GameTestHelper helper) {
        helper.setBlock(TARGET.below(), Blocks.DIRT);
        helper.setBlock(TARGET, Blocks.SWEET_BERRY_BUSH.defaultBlockState()
                .setValue(SweetBerryBushBlock.AGE, 0));

        use(helper, TARGET, ModItems.QUALITY_BONE_MEAL);

        helper.assertBlockProperty(TARGET, SweetBerryBushBlock.AGE, 2);
        helper.succeed();
    }

    @GameTest
    public void qualityBoneMealGrowsSugarCaneAndCactusToMaximumThree(GameTestHelper helper) {
        BlockPos cane = new BlockPos(2, 2, 2);
        helper.setBlock(cane.below(), Blocks.SAND);
        helper.setBlock(cane.below().east(), Blocks.WATER);
        helper.setBlock(cane, Blocks.SUGAR_CANE);
        use(helper, cane, ModItems.QUALITY_BONE_MEAL);
        helper.assertBlockPresent(Blocks.SUGAR_CANE, cane.above());

        helper.setBlock(cane.above(2), Blocks.SUGAR_CANE);
        var canePlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        canePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.QUALITY_BONE_MEAL));
        helper.useBlock(cane, canePlayer);
        helper.assertTrue(canePlayer.getMainHandItem().getCount() == 1,
                "Sugar cane at height three must not consume quality bone meal");

        BlockPos cactus = new BlockPos(6, 2, 6);
        helper.setBlock(cactus.below(), Blocks.SAND);
        helper.setBlock(cactus, Blocks.CACTUS);
        use(helper, cactus, ModItems.QUALITY_BONE_MEAL);
        helper.assertBlockPresent(Blocks.CACTUS, cactus.above());
        helper.succeed();
    }

    @GameTest
    public void qualityBoneMealDuplicatesCommonFlowerOnly(GameTestHelper helper) {
        helper.setBlock(TARGET.below(), Blocks.DIRT);
        helper.setBlock(TARGET, Blocks.DANDELION);
        use(helper, TARGET, ModItems.QUALITY_BONE_MEAL);
        helper.assertItemEntityCountIs(Blocks.DANDELION.asItem(), TARGET, 2, 1);

        BlockPos special = TARGET.east(3);
        helper.setBlock(special.below(), Blocks.SOUL_SAND);
        helper.setBlock(special, Blocks.WITHER_ROSE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.QUALITY_BONE_MEAL));
        helper.useBlock(special, player);
        helper.assertTrue(player.getMainHandItem().getCount() == 1,
                "Special flowers must keep vanilla non-bonemealable behavior");
        helper.assertItemEntityCountIs(Blocks.WITHER_ROSE.asItem(), special, 2, 0);
        helper.succeed();
    }

    @GameTest
    public void qualityBoneMealExtendsVineStraightDown(GameTestHelper helper) {
        BlockPos vine = new BlockPos(4, 4, 4);
        helper.setBlock(vine.east(), Blocks.STONE);
        helper.setBlock(vine.below().east(), Blocks.STONE);
        helper.setBlock(vine, Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true));

        use(helper, vine, ModItems.QUALITY_BONE_MEAL);

        helper.assertBlockPresent(Blocks.VINE, vine.below());
        helper.assertBlockProperty(vine.below(), VineBlock.EAST, true);
        helper.succeed();
    }

    @GameTest
    public void qualityBoneMealSpreadsLilyPadToAdjacentWater(GameTestHelper helper) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.setBlock(TARGET.relative(direction).below(), Blocks.WATER);
        }
        helper.setBlock(TARGET.below(), Blocks.WATER);
        helper.setBlock(TARGET, Blocks.LILY_PAD);

        use(helper, TARGET, ModItems.QUALITY_BONE_MEAL);

        int adjacentPads = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (helper.getBlockState(TARGET.relative(direction)).is(Blocks.LILY_PAD)) adjacentPads++;
        }
        helper.assertTrue(adjacentPads == 1, "Exactly one adjacent lily pad must be placed");
        helper.succeed();
    }

    @GameTest
    public void poorBoneMealDuplicatesSmallDripleafWithoutRemovingPlant(GameTestHelper helper) {
        helper.setBlock(TARGET.below(), Blocks.CLAY);
        BlockState lower = Blocks.SMALL_DRIPLEAF.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
        helper.setBlock(TARGET, lower);
        helper.setBlock(TARGET.above(), upper);

        use(helper, TARGET, ModItems.POOR_BONE_MEAL);

        helper.assertBlockPresent(Blocks.SMALL_DRIPLEAF, TARGET);
        helper.assertBlockPresent(Blocks.SMALL_DRIPLEAF, TARGET.above());
        helper.assertItemEntityCountIs(Blocks.SMALL_DRIPLEAF.asItem(), TARGET, 2, 1);
        helper.succeed();
    }

    @GameTest
    public void poorBoneMealShrinksBigDripleafOneLevel(GameTestHelper helper) {
        helper.setBlock(TARGET.below(), Blocks.CLAY);
        helper.setBlock(TARGET, Blocks.BIG_DRIPLEAF_STEM.defaultBlockState()
                .setValue(BigDripleafBlock.FACING, Direction.NORTH)
                .setValue(BlockStateProperties.WATERLOGGED, false));
        helper.setBlock(TARGET.above(), Blocks.BIG_DRIPLEAF.defaultBlockState()
                .setValue(BigDripleafBlock.FACING, Direction.NORTH)
                .setValue(BlockStateProperties.WATERLOGGED, false));

        use(helper, TARGET.above(), ModItems.POOR_BONE_MEAL);

        helper.assertBlockPresent(Blocks.BIG_DRIPLEAF, TARGET);
        helper.assertBlockNotPresent(Blocks.BIG_DRIPLEAF, TARGET.above());
        helper.assertBlockNotPresent(Blocks.BIG_DRIPLEAF_STEM, TARGET.above());
        helper.succeed();
    }

    @GameTest
    public void poorBoneMealConvertsMinimumBigDripleafToSmall(GameTestHelper helper) {
        helper.setBlock(TARGET.below(), Blocks.CLAY);
        helper.setBlock(TARGET, Blocks.BIG_DRIPLEAF.defaultBlockState()
                .setValue(BigDripleafBlock.FACING, Direction.WEST)
                .setValue(BlockStateProperties.WATERLOGGED, false));

        use(helper, TARGET, ModItems.POOR_BONE_MEAL);

        helper.assertBlockPresent(Blocks.SMALL_DRIPLEAF, TARGET);
        helper.assertBlockPresent(Blocks.SMALL_DRIPLEAF, TARGET.above());
        helper.assertBlockProperty(TARGET, DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
        helper.assertBlockProperty(TARGET.above(), DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
        helper.succeed();
    }

    @GameTest
    public void qualityMossNeverLeavesGrass(GameTestHelper helper) {
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.MOSS_BLOCK);
            }
        }
        use(helper, new BlockPos(4, 2, 4), ModItems.QUALITY_BONE_MEAL);
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                BlockState state = helper.getBlockState(new BlockPos(x, 3, z));
                helper.assertTrue(!state.is(Blocks.SHORT_GRASS) && !state.is(Blocks.TALL_GRASS),
                        "Quality moss growth must remove short and tall grass");
            }
        }
        helper.succeed();
    }

    @GameTest
    public void poorMossNeverLeavesAzaleas(GameTestHelper helper) {
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.MOSS_BLOCK);
            }
        }
        use(helper, new BlockPos(4, 2, 4), ModItems.POOR_BONE_MEAL);
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                BlockState state = helper.getBlockState(new BlockPos(x, 3, z));
                helper.assertTrue(!state.is(Blocks.AZALEA) && !state.is(Blocks.FLOWERING_AZALEA),
                        "Poor moss growth must remove both azalea variants");
            }
        }
        helper.succeed();
    }

    @GameTest
    public void qualityCrimsonNyliumKeepsOnlyFungi(GameTestHelper helper) {
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.CRIMSON_NYLIUM);
            }
        }
        use(helper, new BlockPos(4, 2, 4), ModItems.QUALITY_BONE_MEAL);
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                BlockState state = helper.getBlockState(new BlockPos(x, 3, z));
                helper.assertTrue(!state.is(Blocks.CRIMSON_ROOTS),
                        "Quality crimson nylium must remove crimson roots");
            }
        }
        helper.succeed();
    }
}
