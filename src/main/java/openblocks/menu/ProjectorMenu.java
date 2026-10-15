package openblocks.menu;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.items.SlotItemHandler;
import openblocks.block.entity.ProjectorBlockEntity;
import openblocks.item.HeightMapItem;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBMenus;

/**
 * Port of legacy {@code ContainerProjector}: the map slot below the map view. The rotation buttons replace the legacy
 * {@code IRotatable} RPC with menu button clicks.
 */
public class ProjectorMenu extends MachineMenu {

	public static final int BUTTON_ROTATE_LEFT = 0;
	public static final int BUTTON_ROTATE_RIGHT = 1;

	private final ProjectorBlockEntity projector;
	private final ContainerLevelAccess access;

	public ProjectorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, (ProjectorBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
	}

	public ProjectorMenu(int containerId, Inventory playerInventory, ProjectorBlockEntity projector) {
		super(OBMenus.PROJECTOR.get(), containerId);
		this.projector = projector;
		this.access = ContainerLevelAccess.create(projector.getLevel(), projector.getBlockPos());

		addSlot(new SlotItemHandler(projector.getInventory(), 0, 79, 130));
		addPlayerInventory(playerInventory, 8, 152);
	}

	@Nullable
	public Integer getMapId() {
		return HeightMapItem.getMapId(getSlot(0).getItem());
	}

	public int getRotation() {
		return projector.getRotation();
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		switch (id) {
			case BUTTON_ROTATE_LEFT -> projector.rotate(-1);
			case BUTTON_ROTATE_RIGHT -> projector.rotate(+1);
			default -> {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, OBBlocks.PROJECTOR.get());
	}
}
