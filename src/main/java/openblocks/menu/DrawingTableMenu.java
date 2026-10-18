package openblocks.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import openblocks.block.entity.DrawingTableBlockEntity;
import openblocks.common.StencilPattern;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBMenus;

/**
 * Port of legacy {@code ContainerDrawingTable}: input and output slot, buttons to switch the mode and pick the pattern
 * or character; printing text comes as a {@code DrawingTablePrintPayload}.
 * Laid out in the vanilla size instead of the legacy 204 pixel high window.
 */
public class DrawingTableMenu extends MachineMenu {

	public static final int BUTTON_PREVIOUS = 0;
	public static final int BUTTON_NEXT = 1;
	public static final int BUTTON_MODE = 2;

	public static final int INPUT_X = 34;
	public static final int OUTPUT_X = 105;
	public static final int SLOT_Y = 35;
	private static final int PLAYER_INVENTORY_Y = 84;

	private final DrawingTableBlockEntity table;
	private final ContainerLevelAccess access;

	public DrawingTableMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, (DrawingTableBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
	}

	public DrawingTableMenu(int containerId, Inventory playerInventory, DrawingTableBlockEntity table) {
		super(OBMenus.DRAWING_TABLE.get(), containerId);
		this.table = table;
		this.access = ContainerLevelAccess.create(table.getLevel(), table.getBlockPos());

		addSlot(new SlotItemHandler(table.getInventory(), DrawingTableBlockEntity.SLOT_INPUT, INPUT_X, SLOT_Y));
		addSlot(new SlotItemHandler(table.getInventory(), DrawingTableBlockEntity.SLOT_OUTPUT, OUTPUT_X, SLOT_Y));
		addPlayerInventory(playerInventory, 8, PLAYER_INVENTORY_Y);
	}

	public StencilPattern getPattern() {
		return table.getPattern();
	}

	public DrawingTableBlockEntity.Mode getMode() {
		return table.getMode();
	}

	public char getGlyph() {
		return table.getGlyph();
	}

	public int getInputCount() {
		return table.getInput().getCount();
	}

	public void print(String text) {
		table.print(text);
		broadcastChanges();
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		switch (id) {
			case BUTTON_PREVIOUS -> table.cycleSelection(-1);
			case BUTTON_NEXT -> table.cycleSelection(1);
			case BUTTON_MODE -> table.toggleMode();
			default -> {
				return false;
			}
		}
		return true;
	}

	/**
	 * The output slot only mirrors the input, so a moved stencil must be taken out of it explicitly.
	 * Moves one stencil; vanilla repeats shift-clicks while the slot still holds the same item, like for the stonecutter.
	 */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index != DrawingTableBlockEntity.SLOT_OUTPUT) return super.quickMoveStack(player, index);

		final Slot slot = slots.get(index);
		final ItemStack stack = slot.getItem();
		if (stack.isEmpty() || !moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;

		final ItemStack taken = slot.remove(1);
		slot.onTake(player, taken);
		return taken;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, OBBlocks.DRAWING_TABLE.get());
	}
}
