package jp.ryuke.bonemealsieve.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AzaleaBlock;
import net.minecraft.world.level.block.BigDripleafBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.NetherFungusBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SmallDripleafBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Bone meal variants with target-specific low/high-quality behavior. */
public final class FilteredBoneMealItem extends BoneMealItem {
    public enum Mode { GRASS_ONLY, FLOWERS_ONLY }

    private static final int HORIZONTAL_RADIUS = 16;
    private static final int VERTICAL_RADIUS = 8;
    private static final double QUALITY_GROWTH_CHANCE = 0.90;

    private static final Set<Block> DOUBLE_GROWTH_CROPS = Set.of(
            Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
            Blocks.COCOA, Blocks.SWEET_BERRY_BUSH, Blocks.PITCHER_CROP,
            Blocks.TORCHFLOWER_CROP, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM);

    // Excludes wither rose, torchflower, both eyeblossoms and golden dandelion.
    private static final Set<Block> COMMON_SMALL_FLOWERS = Set.of(
            Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM,
            Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.ORANGE_TULIP,
            Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY,
            Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY);

    private final Mode mode;

    public FilteredBoneMealItem(Properties properties, Mode mode) {
        super(properties);
        this.mode = mode;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof GrassBlock grassBlock) {
            return filterVanillaGrowth(context, grassBlock,
                    mode == Mode.FLOWERS_ONLY ? 3 : 1,
                    mode == Mode.FLOWERS_ONLY
                            ? newState -> newState.is(BlockTags.FLOWERS)
                            : newState -> !newState.is(BlockTags.FLOWERS));
        }

        if (state.is(Blocks.CRIMSON_NYLIUM)) {
            return filterVanillaGrowth(context, (BonemealableBlock) state.getBlock(),
                    mode == Mode.FLOWERS_ONLY ? 3 : 1,
                    mode == Mode.FLOWERS_ONLY
                            ? newState -> newState.is(Blocks.CRIMSON_FUNGUS) || newState.is(Blocks.WARPED_FUNGUS)
                            : newState -> newState.is(Blocks.CRIMSON_ROOTS));
        }

        if (state.is(Blocks.WARPED_NYLIUM)) {
            return filterVanillaGrowth(context, (BonemealableBlock) state.getBlock(),
                    mode == Mode.FLOWERS_ONLY ? 3 : 1,
                    mode == Mode.FLOWERS_ONLY
                            ? FilteredBoneMealItem::isQualityWarpedVegetation
                            : newState -> newState.is(Blocks.WARPED_ROOTS));
        }

        if (state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.PALE_MOSS_BLOCK)) {
            return filterVanillaGrowth(context, (BonemealableBlock) state.getBlock(), 1,
                    mode == Mode.FLOWERS_ONLY
                            ? newState -> !newState.is(Blocks.SHORT_GRASS) && !newState.is(Blocks.TALL_GRASS)
                            : newState -> !newState.is(Blocks.AZALEA) && !newState.is(Blocks.FLOWERING_AZALEA));
        }

        if (mode == Mode.FLOWERS_ONLY && isQualityChanceTarget(state)) {
            return applyQualityChanceGrowth(context, state);
        }

        if (mode == Mode.FLOWERS_ONLY && DOUBLE_GROWTH_CROPS.contains(state.getBlock())) {
            return applyRepeatedGrowth(context, 2);
        }

        if (mode == Mode.GRASS_ONLY && state.is(Blocks.SMALL_DRIPLEAF)) {
            return duplicateSmallDripleaf(context);
        }

        if (mode == Mode.GRASS_ONLY
                && (state.is(Blocks.BIG_DRIPLEAF) || state.is(Blocks.BIG_DRIPLEAF_STEM))) {
            return shrinkBigDripleaf(context);
        }

        if (mode == Mode.FLOWERS_ONLY && (state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS))) {
            return growColumnPlant(context, state.getBlock());
        }

        if (mode == Mode.FLOWERS_ONLY && COMMON_SMALL_FLOWERS.contains(state.getBlock())) {
            return duplicateCommonFlower(context);
        }

        if (mode == Mode.FLOWERS_ONLY && state.is(Blocks.VINE)) {
            return extendVineDown(context);
        }

        if (mode == Mode.FLOWERS_ONLY && state.is(Blocks.LILY_PAD)) {
            return spreadLilyPad(context);
        }

        // Netherrack and every unspecified target retain vanilla behavior.
        return super.useOn(context);
    }

    private InteractionResult filterVanillaGrowth(UseOnContext context, BonemealableBlock target,
                                                   int passes, Predicate<BlockState> keepGeneratedState) {
        Level level = context.getLevel();
        BlockPos center = context.getClickedPos();
        BlockState clickedState = level.getBlockState(center);
        if (!target.isValidBonemealTarget(level, center, clickedState)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        Map<BlockPos, BlockState> before = snapshot(serverLevel, center);
        for (int i = 0; i < passes; i++) {
            target.performBonemeal(serverLevel, serverLevel.getRandom(), center, clickedState);
        }
        restoreRejectedChanges(serverLevel, before, keepGeneratedState);
        finishBonemealUse(context, serverLevel, center, true);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult applyQualityChanceGrowth(UseOnContext context, BlockState initialState) {
        if (!(initialState.getBlock() instanceof BonemealableBlock target)
                || !target.isValidBonemealTarget(context.getLevel(), context.getClickedPos(), initialState)) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        if (serverLevel.getRandom().nextDouble() < QUALITY_GROWTH_CHANCE) {
            target.performBonemeal(serverLevel, serverLevel.getRandom(), context.getClickedPos(), initialState);
        }
        finishBonemealUse(context, serverLevel, context.getClickedPos(), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult applyRepeatedGrowth(UseOnContext context, int passes) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState initial = level.getBlockState(pos);
        if (!(initial.getBlock() instanceof BonemealableBlock initialTarget)
                || !initialTarget.isValidBonemealTarget(level, pos, initial)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        for (int i = 0; i < passes; i++) {
            BlockState current = serverLevel.getBlockState(pos);
            if (!(current.getBlock() instanceof BonemealableBlock target)
                    || !target.isValidBonemealTarget(serverLevel, pos, current)) break;
            target.performBonemeal(serverLevel, serverLevel.getRandom(), pos, current);
        }
        finishBonemealUse(context, serverLevel, pos, true);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult duplicateSmallDripleaf(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos();
        BlockState state = serverLevel.getBlockState(pos);
        if (state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) pos = pos.below();
        Block.popResource(serverLevel, pos, new ItemStack(Blocks.SMALL_DRIPLEAF));
        finishBonemealUse(context, serverLevel, pos, true);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult shrinkBigDripleaf(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos top = findBigDripleafTop(level, clicked);
        if (top == null) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        BlockPos bottom = top;
        while (serverLevel.getBlockState(bottom.below()).is(Blocks.BIG_DRIPLEAF_STEM)) bottom = bottom.below();
        int height = top.getY() - bottom.getY() + 1;
        Direction facing = serverLevel.getBlockState(top).getValue(BigDripleafBlock.FACING);

        if (height > 1) {
            BlockPos newTop = top.below();
            BlockState leaf = Blocks.BIG_DRIPLEAF.defaultBlockState()
                    .setValue(BigDripleafBlock.FACING, facing)
                    .setValue(BlockStateProperties.WATERLOGGED,
                            serverLevel.getFluidState(newTop).getType() == Fluids.WATER);
            serverLevel.setBlock(top, serverLevel.getFluidState(top).createLegacyBlock(),
                    Block.UPDATE_ALL | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            serverLevel.setBlock(newTop, leaf, Block.UPDATE_ALL | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        } else if (!replaceWithSmallDripleaf(serverLevel, bottom, facing)) {
            serverLevel.setBlock(bottom, serverLevel.getFluidState(bottom).createLegacyBlock(),
                    Block.UPDATE_ALL | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            Block.popResource(serverLevel, bottom, new ItemStack(Blocks.SMALL_DRIPLEAF));
        }

        finishBonemealUse(context, serverLevel, bottom, false);
        return InteractionResult.SUCCESS_SERVER;
    }

    private boolean replaceWithSmallDripleaf(ServerLevel level, BlockPos lowerPos, Direction facing) {
        BlockPos upperPos = lowerPos.above();
        if (!level.getBlockState(upperPos).canBeReplaced()) return false;

        BlockState lower = Blocks.SMALL_DRIPLEAF.defaultBlockState()
                .setValue(SmallDripleafBlock.FACING, facing)
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(BlockStateProperties.WATERLOGGED,
                        level.getFluidState(lowerPos).getType() == Fluids.WATER);
        if (!lower.canSurvive(level, lowerPos)) return false;
        DoublePlantBlock.placeAt(level, lower, lowerPos,
                Block.UPDATE_ALL | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        return true;
    }

    private InteractionResult growColumnPlant(UseOnContext context, Block plant) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos bottom = clicked;
        while (level.getBlockState(bottom.below()).is(plant)) bottom = bottom.below();
        BlockPos top = clicked;
        while (level.getBlockState(top.above()).is(plant)) top = top.above();
        int height = top.getY() - bottom.getY() + 1;
        BlockPos growPos = top.above();
        BlockState candidate = plant.defaultBlockState();
        if (height >= 3 || !level.getBlockState(growPos).canBeReplaced()
                || !candidate.canSurvive(level, growPos)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        serverLevel.setBlockAndUpdate(growPos, candidate);
        finishBonemealUse(context, serverLevel, growPos, false);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult duplicateCommonFlower(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos();
        Block.popResource(serverLevel, pos, new ItemStack(serverLevel.getBlockState(pos).getBlock()));
        finishBonemealUse(context, serverLevel, pos, false);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult extendVineDown(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos bottom = context.getClickedPos();
        while (level.getBlockState(bottom.below()).is(Blocks.VINE)) bottom = bottom.below();
        BlockPos growPos = bottom.below();
        BlockState candidate = level.getBlockState(bottom);
        if (!level.getBlockState(growPos).canBeReplaced() || !candidate.canSurvive(level, growPos)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        serverLevel.setBlockAndUpdate(growPos, candidate);
        finishBonemealUse(context, serverLevel, growPos, false);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult spreadLilyPad(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos origin = context.getClickedPos();
        List<BlockPos> candidates = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = origin.relative(direction);
            BlockState lily = Blocks.LILY_PAD.defaultBlockState();
            if (level.getBlockState(pos).canBeReplaced() && lily.canSurvive(level, pos)) candidates.add(pos);
        }
        if (candidates.isEmpty()) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        BlockPos chosen = candidates.get(serverLevel.getRandom().nextInt(candidates.size()));
        serverLevel.setBlockAndUpdate(chosen, Blocks.LILY_PAD.defaultBlockState());
        finishBonemealUse(context, serverLevel, chosen, false);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean isQualityChanceTarget(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof MangrovePropaguleBlock
                && state.getValue(MangrovePropaguleBlock.HANGING)) return false;
        return block instanceof SaplingBlock || block instanceof AzaleaBlock
                || block instanceof MushroomBlock || block instanceof NetherFungusBlock;
    }

    private static boolean isQualityWarpedVegetation(BlockState state) {
        return state.is(Blocks.NETHER_SPROUTS)
                || state.is(Blocks.WARPED_FUNGUS)
                || state.is(Blocks.CRIMSON_FUNGUS)
                || state.is(Blocks.TWISTING_VINES)
                || state.is(Blocks.TWISTING_VINES_PLANT);
    }

    private static BlockPos findBigDripleafTop(Level level, BlockPos start) {
        BlockPos cursor = start;
        if (level.getBlockState(cursor).is(Blocks.BIG_DRIPLEAF)) return cursor;
        if (!level.getBlockState(cursor).is(Blocks.BIG_DRIPLEAF_STEM)) return null;
        while (level.getBlockState(cursor).is(Blocks.BIG_DRIPLEAF_STEM)) cursor = cursor.above();
        return level.getBlockState(cursor).is(Blocks.BIG_DRIPLEAF) ? cursor : null;
    }

    private Map<BlockPos, BlockState> snapshot(ServerLevel level, BlockPos center) {
        int width = HORIZONTAL_RADIUS * 2 + 1;
        int height = VERTICAL_RADIUS * 2 + 1;
        Map<BlockPos, BlockState> states = new HashMap<>(width * width * height);
        for (int dx = -HORIZONTAL_RADIUS; dx <= HORIZONTAL_RADIUS; dx++) {
            for (int dy = -VERTICAL_RADIUS; dy <= VERTICAL_RADIUS; dy++) {
                for (int dz = -HORIZONTAL_RADIUS; dz <= HORIZONTAL_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    states.put(pos, level.getBlockState(pos));
                }
            }
        }
        return states;
    }

    private void restoreRejectedChanges(ServerLevel level, Map<BlockPos, BlockState> before,
                                        Predicate<BlockState> keepGeneratedState) {
        for (Map.Entry<BlockPos, BlockState> entry : before.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState oldState = entry.getValue();
            BlockState newState = level.getBlockState(pos);
            if (!newState.equals(oldState) && !keepGeneratedState.test(newState)) {
                level.setBlock(pos, oldState, Block.UPDATE_ALL | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            }
        }
    }

    private static void finishBonemealUse(UseOnContext context, ServerLevel level,
                                          BlockPos particlePos, boolean vanillaParticleEvent) {
        context.getItemInHand().causeUseVibration(context.getPlayer(), GameEvent.ITEM_INTERACT_FINISH);
        context.getItemInHand().consume(1, context.getPlayer());
        if (vanillaParticleEvent) {
            level.levelEvent(1505, particlePos, 15);
        } else {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    particlePos.getX() + 0.5, particlePos.getY() + 0.7, particlePos.getZ() + 0.5,
                    15, 0.35, 0.35, 0.35, 0.0);
        }
    }
}
