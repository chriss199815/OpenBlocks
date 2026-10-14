package openblocks.common;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * Automation view of a machine inventory from one side; replaces OpenModsLib {@code SidedItemHandlerAdapter}.
 * Input slots only accept items, output slots only give them out, and only if their auto slot is open on this side.
 * The null side (no side, e.g. other mods' tools) has access to all slots.
 */
public class SidedItemHandler<E extends Enum<E>> implements IItemHandler {

	private final IItemHandlerModifiable inventory;
	private final AutoSlotConfig<E> config;
	private final Map<Integer, E> inputs;
	private final Map<Integer, E> outputs;
	@Nullable
	private final Direction side;

	public SidedItemHandler(IItemHandlerModifiable inventory, AutoSlotConfig<E> config, Map<Integer, E> inputs, Map<Integer, E> outputs, @Nullable Direction side) {
		this.inventory = inventory;
		this.config = config;
		this.inputs = inputs;
		this.outputs = outputs;
		this.side = side;
	}

	private boolean isOpen(Map<Integer, E> slots, int slot) {
		final E autoSlot = slots.get(slot);
		return autoSlot != null && (side == null || config.isOpen(autoSlot, side));
	}

	@Override
	public int getSlots() {
		return inventory.getSlots();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return inventory.getStackInSlot(slot);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		return isOpen(inputs, slot)? inventory.insertItem(slot, stack, simulate) : stack;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return isOpen(outputs, slot)? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int slot) {
		return inventory.getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return isOpen(inputs, slot) && inventory.isItemValid(slot, stack);
	}
}
