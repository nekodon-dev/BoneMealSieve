package jp.ryuke.bonemealsieve.block.entity;

import jp.ryuke.bonemealsieve.config.SieveConfig;
import jp.ryuke.bonemealsieve.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Top input and bottom output. A blocked output stops processing without rerolling it. */
public final class SieveBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int PROCESSING_TICKS = 20;
    private static final int INPUT = 0;
    private static final int OUTPUT = 1;
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int progress;

    public SieveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIEVE, pos, state);
    }

    public boolean insertOne(ItemStack stack) {
        if (!canPlaceItem(INPUT, stack)) return false;
        ItemStack input = items.get(INPUT);
        if (!input.isEmpty() && (!ItemStack.isSameItemSameComponents(input, stack)
                || input.getCount() >= input.getMaxStackSize())) return false;
        if (input.isEmpty()) items.set(INPUT, stack.copyWithCount(1));
        else input.grow(1);
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SieveBlockEntity sieve) {
        if (!sieve.items.get(OUTPUT).isEmpty()) {
            sieve.ejectOutput(level, pos);
            if (!sieve.items.get(OUTPUT).isEmpty()) return;
        }
        if (!sieve.items.get(INPUT).is(Items.BONE_MEAL)) {
            if (sieve.progress != 0) {
                sieve.progress = 0;
                sieve.setChanged();
            }
            return;
        }
        sieve.progress++;
        if (sieve.progress >= PROCESSING_TICKS) {
            sieve.items.get(INPUT).shrink(1);
            sieve.items.set(OUTPUT, new ItemStack(level.getRandom().nextDouble() < SieveConfig.qualityBoneMealChance()
                    ? ModItems.QUALITY_BONE_MEAL : ModItems.POOR_BONE_MEAL));
            sieve.progress = 0;
            sieve.ejectOutput(level, pos);
        }
        sieve.setChanged();
    }

    private void ejectOutput(Level level, BlockPos pos) {
        ItemStack output = items.get(OUTPUT);
        // Insert immediately so a hopper's pull cooldown does not extend the 20-tick
        // processing interval. A full or redstone-locked hopper retains the result.
        if (level.getBlockEntity(pos.below()) instanceof HopperBlockEntity hopper) {
            if (hopper.getBlockState().getValue(HopperBlock.ENABLED)) {
                items.set(OUTPUT, HopperBlockEntity.addItem(this, hopper, output, Direction.UP));
                setChanged();
            }
            return;
        }
        // Item height is 0.25 blocks; its top starts below the mesh at y=12.25/16.
        ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.45,
                pos.getZ() + 0.5, output.copy());
        drop.setDeltaMovement(0, -0.05, 0);
        drop.setDefaultPickUpDelay();
        if (level.addFreshEntity(drop)) items.set(OUTPUT, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        progress = Math.clamp(input.getIntOr("Progress", 0), 0, PROCESSING_TICKS - 1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("Progress", progress);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState newState) {
        if (level != null) Containers.dropContents(level, worldPosition, this);
        super.preRemoveSideEffects(pos, newState);
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); progress = 0; setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == INPUT && stack.is(Items.BONE_MEAL); }
    @Override public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? new int[]{INPUT} : side == Direction.DOWN ? new int[]{OUTPUT} : new int[0];
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.UP && canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && slot == OUTPUT;
    }
}
