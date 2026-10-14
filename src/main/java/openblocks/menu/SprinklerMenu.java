package openblocks.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.items.SlotItemHandler;
import openblocks.block.entity.SprinklerBlockEntity;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBMenus;

/**
 * Port of legacy {@code ContainerSprinkler}: a 3x3 grid for bone meal, laid out like the vanilla dispenser.
 */
public class SprinklerMenu extends MachineMenu {

	private static final int PLAYER_INVENTORY_Y = 84;

	private final ContainerLevelAccess access;

	public SprinklerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, (SprinklerBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
	}

	public SprinklerMenu(int containerId, Inventory playerInventory, SprinklerBlockEntity sprinkler) {
		super(OBMenus.SPRINKLER.get(), containerId);
		this.access = ContainerLevelAccess.create(sprinkler.getLevel(), sprinkler.getBlockPos());

		for (int slot = 0; slot < SprinklerBlockEntity.SLOTS; slot++)
			addSlot(new SlotItemHandler(sprinkler.getInventory(), slot, 62 + (slot % 3) * 18, 17 + (slot / 3) * 18));
		addPlayerInventory(playerInventory, 8, PLAYER_INVENTORY_Y);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, OBBlocks.SPRINKLER.get());
	}
}
