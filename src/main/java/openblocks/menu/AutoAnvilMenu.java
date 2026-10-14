package openblocks.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.items.SlotItemHandler;
import openblocks.block.entity.AutoAnvilBlockEntity;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBMenus;

/**
 * Port of legacy {@code ContainerAutoAnvil}, laid out like the vanilla anvil. The tank and side configuration are synced via the block entity update packet.
 */
public class AutoAnvilMenu extends MachineMenu {

	private static final int PLAYER_INVENTORY_Y = 84;

	private final AutoAnvilBlockEntity anvil;
	private final ContainerLevelAccess access;

	public AutoAnvilMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, (AutoAnvilBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
	}

	public AutoAnvilMenu(int containerId, Inventory playerInventory, AutoAnvilBlockEntity anvil) {
		super(OBMenus.AUTO_ANVIL.get(), containerId);
		this.anvil = anvil;
		this.access = ContainerLevelAccess.create(anvil.getLevel(), anvil.getBlockPos());

		addSlot(new SlotItemHandler(anvil.getInventory(), AutoAnvilBlockEntity.SLOT_TOOL, 8, 47));
		addSlot(new SlotItemHandler(anvil.getInventory(), AutoAnvilBlockEntity.SLOT_MODIFIER, 57, 47));
		addSlot(new SlotItemHandler(anvil.getInventory(), AutoAnvilBlockEntity.SLOT_OUTPUT, 115, 47));
		addPlayerInventory(playerInventory, 8, PLAYER_INVENTORY_Y);
	}

	public AutoAnvilBlockEntity getAnvil() {
		return anvil;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		return anvil.getConfig().handleButton(id);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, OBBlocks.AUTO_ANVIL.get());
	}
}
