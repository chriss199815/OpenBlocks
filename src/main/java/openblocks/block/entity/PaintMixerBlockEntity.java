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
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.item.PaintCanItem;
import openblocks.menu.PaintMixerMenu;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBBlocks;
import openblocks.util.PaintColors;

/**
 * Port of legacy {@code TileEntityPaintMixer}: mixes paint of the chosen color into a paint can (or a milk bucket, which becomes a can),
 * using cyan, magenta, yellow and black dye. Each dye fills its ink level by one; a color needs its CMYK share of each ink.
 */
public class PaintMixerBlockEntity extends BlockEntity implements MenuProvider {

	public static final int PROGRESS_TICKS = 300;

	public static final int SLOT_PAINT = 0;
	public static final int SLOT_CYAN = 1;
	public static final int SLOT_MAGENTA = 2;
	public static final int SLOT_YELLOW = 3;
	public static final int SLOT_BLACK = 4;
	public static final int SLOTS = 5;

	public static final int INK_CYAN = 0;
	public static final int INK_MAGENTA = 1;
	public static final int INK_YELLOW = 2;
	public static final int INK_BLACK = 3;
	// like legacy, up to two dyes worth of ink, refilled when one or less is left
	public static final float MAX_INK = 2f;

	@SuppressWarnings("unchecked")
	private static final TagKey<Item>[] DYES = new TagKey[] { Tags.Items.DYES_CYAN, Tags.Items.DYES_MAGENTA, Tags.Items.DYES_YELLOW, Tags.Items.DYES_BLACK };

	// ContainerData: progress, color (split, values are sent as shorts), ink levels in hundredths
	public static final int DATA_PROGRESS = 0;
	public static final int DATA_COLOR_HIGH = 1;
	public static final int DATA_COLOR_LOW = 2;
	public static final int DATA_INK = 3;
	public static final int DATA_COUNT = 7;

	private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			if (slot == SLOT_PAINT) return isPaintInput(stack);
			return stack.is(DYES[slot - SLOT_CYAN]);
		}

		@Override
		public int getSlotLimit(int slot) {
			return slot == SLOT_PAINT? 1 : super.getSlotLimit(slot);
		}

		@Override
		protected void onContentsChanged(int slot) {
			if (slot == SLOT_PAINT) onPaintChanged();
			setChanged();
		}
	};

	private final float[] ink = new float[4];

	// target color; follows the inserted can while idle, like legacy
	private int color = 0xFF0000;
	private int progress;

	// what the renderer shows: the can, its current color, and whether it is being mixed
	private boolean hasPaint;
	private int canColor = PaintColors.WHITE;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case DATA_PROGRESS -> progress;
				case DATA_COLOR_HIGH -> (color >> 12) & 0xFFF;
				case DATA_COLOR_LOW -> color & 0xFFF;
				default -> Math.round(ink[index - DATA_INK] * 100);
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public PaintMixerBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.PAINT_MIXER.get(), pos, state);
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	public ContainerData getData() {
		return data;
	}

	public boolean hasPaint() {
		return hasPaint;
	}

	public boolean isWorking() {
		return progress > 0;
	}

	/**
	 * The color shown on the can: fading from the old to the new color while mixing.
	 */
	public int getDisplayColor(float partialTick) {
		if (progress <= 0) return canColor;
		return PaintColors.interpolate(canColor, color, Math.min(1f, (progress + partialTick) / PROGRESS_TICKS));
	}

	public float getInk(int ink) {
		return this.ink[ink];
	}

	private static boolean isPaintInput(ItemStack stack) {
		return stack.is(OBBlocks.PAINT_CAN.asItem()) || stack.is(Items.MILK_BUCKET);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, PaintMixerBlockEntity mixer) {
		mixer.tick();
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, PaintMixerBlockEntity mixer) {
		// the server only syncs start and end; the renderer needs the progress in between
		if (mixer.progress > 0 && mixer.progress < PROGRESS_TICKS) mixer.progress++;
	}

	private void tick() {
		if (progress > 0) {
			if (!isPaintInput(inventory.getStackInSlot(SLOT_PAINT)) || !hasSufficientInk()) {
				setProgress(0);
			} else if (++progress >= PROGRESS_TICKS) {
				consumeInk();
				progress = 0;
				canColor = color;
				inventory.setStackInSlot(SLOT_PAINT, PaintCanItem.create(color));
				sync();
			}
			setChanged();
		}

		refillInk();
	}

	/**
	 * Starts mixing the given color; like legacy, asking for the color that is already being mixed does nothing.
	 */
	public void startMixing(int newColor) {
		newColor &= PaintColors.WHITE;
		if (progress > 0 && newColor == color) return;
		color = newColor;
		setProgress(isPaintInput(inventory.getStackInSlot(SLOT_PAINT))? 1 : 0);
	}

	private void setProgress(int newProgress) {
		progress = newProgress;
		setChanged();
		sync();
	}

	private void refillInk() {
		for (int i = 0; i < ink.length; i++)
			if (ink[i] <= MAX_INK - 1 && useDye(i)) ink[i] += 1;
	}

	private boolean useDye(int inkIndex) {
		final int slot = SLOT_CYAN + inkIndex;
		if (!inventory.getStackInSlot(slot).is(DYES[inkIndex])) return false;
		return !inventory.extractItem(slot, 1, false).isEmpty();
	}

	private float[] inkNeeded() {
		final PaintColors.Cmyk cmyk = PaintColors.Cmyk.of(color);
		return new float[] { cmyk.cyan(), cmyk.magenta(), cmyk.yellow(), cmyk.key() };
	}

	private boolean hasSufficientInk() {
		final float[] needed = inkNeeded();
		for (int i = 0; i < ink.length; i++) {
			if (needed[i] > ink[i]) {
				if (useDye(i)) ink[i] += 1;
				else return false;
			}
		}
		return true;
	}

	private void consumeInk() {
		final float[] needed = inkNeeded();
		for (int i = 0; i < ink.length; i++)
			ink[i] -= needed[i];
	}

	private void onPaintChanged() {
		final ItemStack stack = inventory.getStackInSlot(SLOT_PAINT);
		hasPaint = isPaintInput(stack);
		if (stack.is(OBBlocks.PAINT_CAN.asItem())) {
			canColor = PaintCanItem.getColor(stack);
			if (progress == 0) color = canColor;
		} else if (hasPaint) {
			canColor = PaintColors.WHITE;
		}
		sync();
	}

	private void sync() {
		if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("openblocks.gui.paintmixer");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new PaintMixerMenu(containerId, playerInventory, this, data);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		if (tag.contains("inventory")) inventory.deserializeNBT(registries, tag.getCompound("inventory"));
		loadState(tag);
		ink[INK_CYAN] = tag.getFloat("cyan");
		ink[INK_MAGENTA] = tag.getFloat("magenta");
		ink[INK_YELLOW] = tag.getFloat("yellow");
		ink[INK_BLACK] = tag.getFloat("black");
	}

	private void loadState(CompoundTag tag) {
		color = tag.getInt("color");
		progress = tag.getInt("progress");
		hasPaint = tag.getBoolean("has_paint");
		canColor = tag.getInt("can_color");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
		saveState(tag);
		tag.putFloat("cyan", ink[INK_CYAN]);
		tag.putFloat("magenta", ink[INK_MAGENTA]);
		tag.putFloat("yellow", ink[INK_YELLOW]);
		tag.putFloat("black", ink[INK_BLACK]);
	}

	private void saveState(CompoundTag tag) {
		tag.putInt("color", color);
		tag.putInt("progress", progress);
		tag.putBoolean("has_paint", hasPaint);
		tag.putInt("can_color", canColor);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		final CompoundTag tag = new CompoundTag();
		saveState(tag);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		loadState(tag);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
		loadState(packet.getTag());
	}
}
