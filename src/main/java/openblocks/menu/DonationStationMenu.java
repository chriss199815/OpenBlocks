package openblocks.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import openblocks.block.entity.DonationStationBlockEntity;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBMenus;

/**
 * Port of legacy {@code ContainerDonationStation}: one slot, with the player inventory 90 pixels down like legacy.
 */
public class DonationStationMenu extends MachineMenu {

	public static final int SLOT_X = 30;
	public static final int SLOT_Y = 30;
	private static final int PLAYER_INVENTORY_Y = 90;

	private final ContainerLevelAccess access;

	public DonationStationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, (DonationStationBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
	}

	public DonationStationMenu(int containerId, Inventory playerInventory, DonationStationBlockEntity station) {
		super(OBMenus.DONATION_STATION.get(), containerId);
		this.access = ContainerLevelAccess.create(station.getLevel(), station.getBlockPos());

		addSlot(new SlotItemHandler(station.getInventory(), 0, SLOT_X, SLOT_Y));
		addPlayerInventory(playerInventory, 8, PLAYER_INVENTORY_Y);
	}

	public ItemStack getDonation() {
		return getSlot(0).getItem();
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, OBBlocks.DONATION_STATION.get());
	}
}
