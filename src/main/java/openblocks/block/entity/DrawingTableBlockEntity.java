package openblocks.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
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
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.common.StencilPattern;
import openblocks.item.StencilItem;
import openblocks.menu.DrawingTableMenu;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBItems;

/**
 * Port of legacy {@code TileEntityDrawingTable}: turns unprepared stencils into stencils of the selected pattern.
 * The output slot only mirrors the input: like the vanilla stonecutter, it shows one stencil, and each one taken uses up one input item.
 * Unlike legacy, finished stencils are accepted as input too, so they can be redrawn with another pattern.
 * The glyph mode comes with the glyphs.
 */
public class DrawingTableBlockEntity extends BlockEntity implements MenuProvider {

	public static final int SLOT_INPUT = 0;
	public static final int SLOT_OUTPUT = 1;

	private final ItemStackHandler input = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.is(OBItems.UNPREPARED_STENCIL.get()) || stack.getItem() instanceof StencilItem;
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};

	private final IItemHandlerModifiable inventory = new TableInventory();

	private StencilPattern pattern = StencilPattern.CREEPER_FACE;

	public DrawingTableBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.DRAWING_TABLE.get(), pos, state);
	}

	/**
	 * Both slots: unprepared (or other) stencils go into {@link #SLOT_INPUT}, stencils come out of {@link #SLOT_OUTPUT}.
	 */
	public IItemHandlerModifiable getInventory() {
		return inventory;
	}

	public ItemStack getInput() {
		return input.getStackInSlot(0);
	}

	public StencilPattern getPattern() {
		return pattern;
	}

	public void cyclePattern(int delta) {
		final StencilPattern[] patterns = StencilPattern.values();
		pattern = patterns[Math.floorMod(pattern.ordinal() + delta, patterns.length)];
		setChanged();
		// the client needs the pattern to show the output slot
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	private ItemStack createOutput(int count) {
		return count > 0? new ItemStack(OBItems.STENCILS.get(pattern).get(), count) : ItemStack.EMPTY;
	}

	private ItemStack takeOutput(boolean simulate) {
		return createOutput(input.extractItem(0, 1, simulate).getCount());
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("openblocks.gui.drawingtable");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new DrawingTableMenu(containerId, playerInventory, this);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		input.deserializeNBT(registries, tag.getCompound("inventory"));
		pattern = StencilPattern.CODEC.byName(tag.getString("pattern"), StencilPattern.CREEPER_FACE);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", input.serializeNBT(registries));
		tag.putString("pattern", pattern.getSerializedName());
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		final CompoundTag tag = new CompoundTag();
		tag.putString("pattern", pattern.getSerializedName());
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		pattern = StencilPattern.CODEC.byName(tag.getString("pattern"), StencilPattern.CREEPER_FACE);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
		handleUpdateTag(packet.getTag(), registries);
	}

	private class TableInventory implements IItemHandlerModifiable {

		@Override
		public int getSlots() {
			return 2;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return slot == SLOT_INPUT? getInput() : createOutput(Math.min(getInput().getCount(), 1));
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return slot == SLOT_INPUT? input.insertItem(0, stack, simulate) : stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (slot == SLOT_INPUT) return input.extractItem(0, amount, simulate);
			return amount > 0? takeOutput(simulate) : ItemStack.EMPTY;
		}

		/**
		 * The output is computed from the input, so setting it (e.g. when the client menu receives it) does nothing.
		 */
		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			if (slot == SLOT_INPUT) input.setStackInSlot(0, stack);
		}

		@Override
		public int getSlotLimit(int slot) {
			return input.getSlotLimit(0);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return slot == SLOT_INPUT && input.isItemValid(0, stack);
		}
	}
}
