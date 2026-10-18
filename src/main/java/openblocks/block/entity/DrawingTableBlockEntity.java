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
import net.minecraft.world.entity.item.ItemEntity;
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
import openblocks.item.GlyphItem;
import openblocks.item.StencilItem;
import openblocks.menu.DrawingTableMenu;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBItems;

/**
 * Port of legacy {@code TileEntityDrawingTable}: turns unprepared stencils into stencils of the selected pattern or,
 * in glyph mode, into glyphs of the selected character; text can be printed as glyphs, dropped on top of the table.
 * The output slot only mirrors the input: like the vanilla stonecutter, it shows one stencil, and each one taken uses up one input item.
 * Unlike legacy, finished stencils and glyphs are accepted as input too, so they can be redrawn.
 */
public class DrawingTableBlockEntity extends BlockEntity implements MenuProvider {

	public static final int SLOT_INPUT = 0;
	public static final int SLOT_OUTPUT = 1;
	public static final int MAX_PRINT_SIZE = 32;

	public enum Mode {
		STENCILS("openblocks.gui.drawingtable.stencils"),
		GLYPHS("openblocks.gui.drawingtable.glyphs");

		public final String translationKey;

		Mode(String translationKey) {
			this.translationKey = translationKey;
		}
	}

	private final ItemStackHandler input = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.is(OBItems.UNPREPARED_STENCIL.get()) || stack.getItem() instanceof StencilItem || stack.is(OBItems.GLYPH.get());
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};

	private final IItemHandlerModifiable inventory = new TableInventory();

	private StencilPattern pattern = StencilPattern.CREEPER_FACE;
	private Mode mode = Mode.STENCILS;
	private char glyph = 'A';

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

	public Mode getMode() {
		return mode;
	}

	public char getGlyph() {
		return glyph;
	}

	public void toggleMode() {
		mode = mode == Mode.STENCILS? Mode.GLYPHS : Mode.STENCILS;
		sync();
	}

	/**
	 * Selects the next or previous pattern or character, depending on the mode.
	 */
	public void cycleSelection(int delta) {
		if (mode == Mode.GLYPHS) {
			glyph = GlyphItem.cycle(glyph, delta);
		} else {
			final StencilPattern[] patterns = StencilPattern.values();
			pattern = patterns[Math.floorMod(pattern.ordinal() + delta, patterns.length)];
		}
		sync();
	}

	/**
	 * How many characters from the start of the text {@link #print} turns into glyphs: up to the first one without a
	 * glyph, at most {@link #MAX_PRINT_SIZE} and one per input item.
	 */
	public static int printableCount(String text, int inputCount) {
		final int max = Math.min(MAX_PRINT_SIZE, Math.min(inputCount, text.length()));
		int count = 0;
		while (count < max && GlyphItem.isValid(text.charAt(count))) count++;
		return count;
	}

	/**
	 * Drops a glyph for each printable character on top of the table, using up one input item each; only in glyph mode.
	 */
	public int print(String text) {
		if (mode != Mode.GLYPHS || level == null) return 0;
		final int count = printableCount(text, getInput().getCount());
		for (int i = 0; i < count; i++) {
			final ItemEntity drop = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, GlyphItem.create(text.charAt(i), 1));
			drop.setDefaultPickUpDelay();
			level.addFreshEntity(drop);
		}
		input.extractItem(0, count, false);
		return count;
	}

	private void sync() {
		setChanged();
		// the client needs the selection to show the output slot
		if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	private ItemStack createOutput(int count) {
		if (count <= 0) return ItemStack.EMPTY;
		return mode == Mode.GLYPHS? GlyphItem.create(glyph, count) : new ItemStack(OBItems.STENCILS.get(pattern).get(), count);
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
		readSelection(tag);
	}

	private void readSelection(CompoundTag tag) {
		pattern = StencilPattern.CODEC.byName(tag.getString("pattern"), StencilPattern.CREEPER_FACE);
		mode = tag.getBoolean("glyphs")? Mode.GLYPHS : Mode.STENCILS;
		glyph = tag.contains("glyph") && GlyphItem.isValid(tag.getInt("glyph"))? (char)tag.getInt("glyph") : 'A';
	}

	private CompoundTag writeSelection(CompoundTag tag) {
		tag.putString("pattern", pattern.getSerializedName());
		tag.putBoolean("glyphs", mode == Mode.GLYPHS);
		tag.putInt("glyph", glyph);
		return tag;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", input.serializeNBT(registries));
		writeSelection(tag);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return writeSelection(new CompoundTag());
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		readSelection(tag);
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
