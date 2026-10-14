package openblocks.menu;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Base for menus with machine slots followed by the player inventory; replaces OpenModsLib {@code ContainerBase}.
 * Subclasses add their machine slots first, then call {@link #addPlayerInventory}.
 */
public abstract class MachineMenu extends AbstractContainerMenu {

	private static final int PLAYER_SLOTS = 36;

	private int machineSlots = -1;

	protected MachineMenu(@Nullable MenuType<?> menuType, int containerId) {
		super(menuType, containerId);
	}

	/**
	 * Adds the 27 inventory slots and, 58 pixels below their top, the hotbar.
	 */
	protected void addPlayerInventory(Inventory inventory, int x, int y) {
		machineSlots = slots.size();

		for (int row = 0; row < 3; row++)
			for (int column = 0; column < 9; column++)
				addSlot(createPlayerSlot(inventory, column + row * 9 + 9, x + column * 18, y + row * 18));

		for (int column = 0; column < 9; column++)
			addSlot(createPlayerSlot(inventory, column, x + column * 18, y + 58));
	}

	protected Slot createPlayerSlot(Inventory inventory, int index, int x, int y) {
		return new Slot(inventory, index, x, y);
	}

	/**
	 * Items from the machine go to the player, items from the player go into the first machine slot that accepts them.
	 */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		final Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;

		final ItemStack stack = slot.getItem();
		final ItemStack original = stack.copy();

		if (index < machineSlots) {
			if (!moveItemStackTo(stack, machineSlots, machineSlots + PLAYER_SLOTS, true)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, machineSlots, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();

		if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
		slot.onTake(player, stack);
		return original;
	}
}
