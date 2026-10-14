package openblocks.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import openblocks.item.DevNullItem;
import openblocks.registry.OBMenus;
import openblocks.registry.OBTriggers;

/**
 * Port of legacy {@code ContainerDevNull}: the single slot of the held /dev/null. Its own hotbar slot is hidden and locked.
 */
public class DevNullMenu extends MachineMenu {

	public static final int SLOT_X = 80;
	public static final int SLOT_Y = 22;
	private static final int PLAYER_INVENTORY_Y = 55;

	private final int protectedSlot;
	private final DevNullContainer container;

	public DevNullMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, extraData.readVarInt());
	}

	public DevNullMenu(int containerId, Inventory playerInventory, int protectedSlot) {
		super(OBMenus.DEV_NULL.get(), containerId);
		this.protectedSlot = protectedSlot;
		this.container = new DevNullContainer(playerInventory.player, protectedSlot);

		addSlot(new Slot(container, 0, SLOT_X, SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return DevNullItem.getContents(stack).depth() < DevNullItem.STACK_LIMIT;
			}
		});
		addPlayerInventory(playerInventory, 8, PLAYER_INVENTORY_Y);
	}

	@Override
	protected Slot createPlayerSlot(Inventory inventory, int index, int x, int y) {
		if (index != protectedSlot) return super.createPlayerSlot(inventory, index, x, y);
		return new Slot(inventory, index, x, y) {
			@Override
			public boolean mayPickup(Player player) {
				return false;
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}

			@Override
			public boolean isActive() {
				return false;
			}
		};
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (slotId >= 0 && slotId < slots.size() && !slots.get(slotId).isActive()) return;
		if (clickType == ClickType.SWAP && button == protectedSlot) return;
		super.clicked(slotId, button, clickType, player);
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	/**
	 * Copy of the slot that writes changes back to the /dev/null; replaces OpenModsLib {@code PlayerItemInventory}.
	 */
	private static class DevNullContainer extends SimpleContainer {

		private final Player player;
		private final int slot;
		private final ItemStack containerStack;

		DevNullContainer(Player player, int slot) {
			super(1);
			this.player = player;
			this.slot = slot;
			this.containerStack = player.getInventory().getItem(slot);
			getItems().set(0, DevNullItem.getContained(containerStack));
		}

		@Override
		public void setChanged() {
			super.setChanged();
			DevNullItem.setContained(containerStack, getItem(0));
			if (player instanceof ServerPlayer serverPlayer)
				OBTriggers.DEV_NULL_STACKED.get().trigger(serverPlayer, DevNullItem.getContents(containerStack).depth());
		}

		@Override
		public boolean stillValid(Player player) {
			return player.getInventory().getItem(slot) == containerStack && containerStack.getItem() instanceof DevNullItem;
		}
	}
}
