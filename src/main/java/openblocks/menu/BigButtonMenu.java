package openblocks.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.SlotItemHandler;
import openblocks.block.BigButtonBlock;
import openblocks.block.entity.BigButtonBlockEntity;
import openblocks.registry.OBMenus;

/**
 * Port of legacy {@code ContainerBigButton}: 4x2 slots, the resulting press time is sent through {@link ContainerData}.
 */
public class BigButtonMenu extends MachineMenu {

	public static final int PLAYER_INVENTORY_Y = 100;

	private final ContainerLevelAccess access;
	private final Block block;
	private final ContainerData data;

	public BigButtonMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, playerInventory, (BigButtonBlockEntity)playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
	}

	public BigButtonMenu(int containerId, Inventory playerInventory, BigButtonBlockEntity button) {
		super(OBMenus.BIG_BUTTON.get(), containerId);
		this.access = ContainerLevelAccess.create(button.getLevel(), button.getBlockPos());
		this.block = button.getBlockState().getBlock();
		this.data = button.getLevel().isClientSide? new SimpleContainerData(1) : new ContainerData() {
			@Override
			public int get(int index) {
				return button.getTickTime();
			}

			@Override
			public void set(int index, int value) {}

			@Override
			public int getCount() {
				return 1;
			}
		};

		for (int slot = 0; slot < BigButtonBlockEntity.SLOTS; slot++)
			addSlot(new SlotItemHandler(button.getInventory(), slot, 53 + (slot % 4) * 18, 30 + (slot / 4) * 18));
		addPlayerInventory(playerInventory, 8, PLAYER_INVENTORY_Y);
		addDataSlots(data);
	}

	public int getTickTime() {
		return data.get(0);
	}

	@Override
	public boolean stillValid(Player player) {
		return block instanceof BigButtonBlock && stillValid(access, player, block);
	}
}
