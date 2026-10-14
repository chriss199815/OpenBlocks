package openblocks.menu;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.items.SlotItemHandler;
import openblocks.block.entity.VacuumHopperBlockEntity;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBMenus;

/**
 * Port of legacy {@code ContainerVacuumHopper}. The tank and output sides are synced via the block entity update packet.
 */
public class VacuumHopperMenu extends MachineMenu {

	private static final int PLAYER_INVENTORY_Y = 69;

	private final VacuumHopperBlockEntity hopper;
	private final ContainerLevelAccess access;

	public VacuumHopperMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, (VacuumHopperBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
	}

	public VacuumHopperMenu(int containerId, Inventory playerInventory, VacuumHopperBlockEntity hopper) {
		super(OBMenus.VACUUM_HOPPER.get(), containerId);
		this.hopper = hopper;
		this.access = ContainerLevelAccess.create(hopper.getLevel(), hopper.getBlockPos());

		for (int slot = 0; slot < VacuumHopperBlockEntity.SLOTS; slot++)
			addSlot(new SlotItemHandler(hopper.getInventory(), slot, 44 + (slot % 5) * 18, 20 + (slot / 5) * 18));
		addPlayerInventory(playerInventory, 8, PLAYER_INVENTORY_Y);
	}

	public VacuumHopperBlockEntity getHopper() {
		return hopper;
	}

	/**
	 * Button id for toggling a side through the vanilla container button packet; replaces the legacy RPC.
	 */
	public static int toggleButtonId(VacuumHopperBlockEntity.Output output, Direction side) {
		return output.ordinal() * 6 + side.get3DDataValue();
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		final VacuumHopperBlockEntity.Output[] outputs = VacuumHopperBlockEntity.Output.values();
		if (id < 0 || id >= outputs.length * 6) return false;
		hopper.toggleOutput(outputs[id / 6], Direction.from3DDataValue(id % 6));
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, OBBlocks.VACUUM_HOPPER.get());
	}
}
