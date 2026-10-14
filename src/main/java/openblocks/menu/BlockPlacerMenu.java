package openblocks.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.items.SlotItemHandler;
import openblocks.block.entity.BlockPlacerBlockEntity;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBMenus;

/**
 * Port of legacy {@code ContainerBlockPlacer}: a 3x3 grid, laid out like the vanilla dispenser.
 */
public class BlockPlacerMenu extends MachineMenu {

	private static final int PLAYER_INVENTORY_Y = 84;

	private final ContainerLevelAccess access;

	public BlockPlacerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, (BlockPlacerBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
	}

	public BlockPlacerMenu(int containerId, Inventory playerInventory, BlockPlacerBlockEntity placer) {
		super(OBMenus.BLOCK_PLACER.get(), containerId);
		this.access = ContainerLevelAccess.create(placer.getLevel(), placer.getBlockPos());

		for (int slot = 0; slot < BlockPlacerBlockEntity.SLOTS; slot++)
			addSlot(new SlotItemHandler(placer.getInventory(), slot, 62 + (slot % 3) * 18, 17 + (slot / 3) * 18));
		addPlayerInventory(playerInventory, 8, PLAYER_INVENTORY_Y);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, OBBlocks.BLOCK_PLACER.get());
	}
}
