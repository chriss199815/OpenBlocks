package openblocks.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.SlotItemHandler;
import openblocks.block.entity.ItemDropperBlockEntity;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBMenus;

/**
 * Port of legacy {@code ContainerItemDropper}. Speed and redstone flag are synced via {@link ContainerData}.
 */
public class ItemDropperMenu extends MachineMenu {

	private static final int PLAYER_INVENTORY_Y = 84;

	private static final int BUTTON_TOGGLE_REDSTONE = 0;
	private static final int BUTTON_SPEED = 1;

	private final ItemDropperBlockEntity dropper;
	private final ContainerData data;
	private final ContainerLevelAccess access;

	public ItemDropperMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, (ItemDropperBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(3));
	}

	public ItemDropperMenu(int containerId, Inventory playerInventory, ItemDropperBlockEntity dropper, ContainerData data) {
		super(OBMenus.ITEM_DROPPER.get(), containerId);
		this.dropper = dropper;
		this.data = data;
		this.access = ContainerLevelAccess.create(dropper.getLevel(), dropper.getBlockPos());

		for (int slot = 0; slot < ItemDropperBlockEntity.SLOTS; slot++)
			addSlot(new SlotItemHandler(dropper.getInventory(), slot, 8 + (slot % 3) * 18, 17 + (slot / 3) * 18));
		addPlayerInventory(playerInventory, 8, PLAYER_INVENTORY_Y);
		addDataSlots(data);
	}

	/**
	 * In steps of {@link ItemDropperBlockEntity#SPEED_STEP}.
	 */
	public int getSpeed() {
		return data.get(ItemDropperBlockEntity.DATA_SPEED);
	}

	public int getMaxSpeed() {
		return data.get(ItemDropperBlockEntity.DATA_MAX_SPEED);
	}

	public boolean useRedstoneStrength() {
		return data.get(ItemDropperBlockEntity.DATA_USE_REDSTONE) != 0;
	}

	/**
	 * Button ids for the vanilla container button packet; replace the legacy RPC.
	 */
	public static int toggleRedstoneButtonId() {
		return BUTTON_TOGGLE_REDSTONE;
	}

	public static int speedButtonId(int speed) {
		return BUTTON_SPEED + speed;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == BUTTON_TOGGLE_REDSTONE) dropper.toggleUseRedstoneStrength();
		else if (id >= BUTTON_SPEED) dropper.setSpeed(id - BUTTON_SPEED);
		else return false;
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, OBBlocks.ITEM_DROPPER.get());
	}
}
