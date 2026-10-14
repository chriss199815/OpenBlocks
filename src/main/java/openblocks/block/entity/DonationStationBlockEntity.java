package openblocks.block.entity;

import javax.annotation.Nullable;
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
import openblocks.menu.DonationStationMenu;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code TileEntityDonationStation}: holds one item; the screen shows which mod it comes from, see {@link openblocks.common.DonationInfo}.
 */
public class DonationStationBlockEntity extends BlockEntity implements MenuProvider {

	private final ItemStackHandler inventory = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};

	public DonationStationBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.DONATION_STATION.get(), pos, state);
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("openblocks.gui.donationstation");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new DonationStationMenu(containerId, playerInventory, this);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		inventory.deserializeNBT(registries, tag.getCompound("inventory"));
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
	}
}
