package openblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.menu.BigButtonMenu;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code TileEntityBigButton}: any items in its 8 slots set how long the button stays pressed, one tick per item.
 */
public class BigButtonBlockEntity extends BlockEntity implements MenuProvider {

	public static final int SLOTS = 8;

	private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};

	public BigButtonBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.BIG_BUTTON.get(), pos, state);
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	public int getTickTime() {
		int result = 0;
		for (int slot = 0; slot < inventory.getSlots(); slot++)
			result += inventory.getStackInSlot(slot).getCount();
		return Math.max(result, 1);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("openblocks.gui.bigbutton");
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new BigButtonMenu(containerId, playerInventory, this);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("inventory")) inventory.deserializeNBT(registries, tag.getCompound("inventory"));
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
	}
}
