package openblocks.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.SlotItemHandler;
import openblocks.block.entity.XpBottlerBlockEntity;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBMenus;

/**
 * Port of legacy {@code ContainerXPBottler}. Progress is synced via {@link ContainerData}, the tank and side configuration via the block entity update packet.
 */
public class XpBottlerMenu extends MachineMenu {

	private static final int PLAYER_INVENTORY_Y = 69;

	private final XpBottlerBlockEntity bottler;
	private final ContainerData data;
	private final ContainerLevelAccess access;

	public XpBottlerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, (XpBottlerBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(1));
	}

	public XpBottlerMenu(int containerId, Inventory playerInventory, XpBottlerBlockEntity bottler, ContainerData data) {
		super(OBMenus.XP_BOTTLER.get(), containerId);
		this.bottler = bottler;
		this.data = data;
		this.access = ContainerLevelAccess.create(bottler.getLevel(), bottler.getBlockPos());

		addSlot(new SlotItemHandler(bottler.getInventory(), XpBottlerBlockEntity.SLOT_INPUT, 48, 30));
		addSlot(new SlotItemHandler(bottler.getInventory(), XpBottlerBlockEntity.SLOT_OUTPUT, 110, 30));
		addPlayerInventory(playerInventory, 8, PLAYER_INVENTORY_Y);
		addDataSlots(data);
	}

	public XpBottlerBlockEntity getBottler() {
		return bottler;
	}

	public int getProgress() {
		return data.get(0);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		return bottler.getConfig().handleButton(id);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, OBBlocks.XP_BOTTLER.get());
	}
}
