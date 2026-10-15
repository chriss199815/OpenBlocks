package openblocks.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.block.ProjectorBlock;
import openblocks.common.HeightMaps;
import openblocks.item.HeightMapItem;
import openblocks.menu.ProjectorMenu;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBItems;

/**
 * Port of legacy {@code TileEntityProjector}: shows the height map in its slot above itself. An empty map put in becomes
 * a new height map. The map can be turned in steps of 90°.
 */
public class ProjectorBlockEntity extends BlockEntity implements MenuProvider {

	private static final String TAG_INVENTORY = "inventory";
	private static final String TAG_ROTATION = "rotation";

	private final ItemStackHandler inventory = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.is(OBItems.HEIGHT_MAP) || stack.is(OBItems.EMPTY_MAP);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		protected void onContentsChanged(int slot) {
			onInventoryChanged();
		}
	};

	private int rotation;

	public ProjectorBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.PROJECTOR.get(), pos, state);
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	private void onInventoryChanged() {
		setChanged();
		if (level == null || level.isClientSide) return;

		final ItemStack stack = inventory.getStackInSlot(0);
		if (stack.is(OBItems.EMPTY_MAP)) {
			// calls this again with the height map
			inventory.setStackInSlot(0, HeightMaps.upgradeToMap(level.getServer(), stack));
			return;
		}

		final BlockState state = getBlockState();
		final boolean active = getMapId() != null;
		if (state.getValue(ProjectorBlock.ACTIVE) != active) level.setBlock(worldPosition, state.setValue(ProjectorBlock.ACTIVE, active), Block.UPDATE_ALL);
		sync();
	}

	@Nullable
	public Integer getMapId() {
		return HeightMapItem.getMapId(inventory.getStackInSlot(0));
	}

	public int getRotation() {
		return rotation;
	}

	public void rotate(int delta) {
		rotation = (rotation + delta) & 3;
		setChanged();
		sync();
	}

	private void sync() {
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new ProjectorMenu(containerId, playerInventory, this);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		inventory.deserializeNBT(registries, tag.getCompound(TAG_INVENTORY));
		rotation = tag.getInt(TAG_ROTATION) & 3;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put(TAG_INVENTORY, inventory.serializeNBT(registries));
		tag.putInt(TAG_ROTATION, rotation);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
