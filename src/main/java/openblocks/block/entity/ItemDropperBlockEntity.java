package openblocks.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.OBConfig;
import openblocks.block.ItemDropperBlock;
import openblocks.menu.ItemDropperMenu;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code TileEntityItemDropper}: drops items straight out of its front, with a speed set in the GUI.
 */
public class ItemDropperBlockEntity extends BlockEntity implements MenuProvider {

	public static final int SLOTS = 9;

	public static final int DATA_SPEED = 0;
	public static final int DATA_USE_REDSTONE = 1;
	public static final int DATA_MAX_SPEED = 2;

	/**
	 * Speeds are stored in steps of 0.1 blocks per tick, like the legacy slider.
	 */
	public static final float SPEED_STEP = 0.1f;

	private static final int PICKUP_DELAY = 40;

	private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};

	private int speed;
	private boolean useRedstoneStrength;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case DATA_SPEED -> speed;
				case DATA_USE_REDSTONE -> useRedstoneStrength? 1 : 0;
				// the config is not synced, so the client gets the limit from here
				default -> getMaxSpeed();
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return 3;
		}
	};

	public ItemDropperBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.ITEM_DROPPER.get(), pos, state);
	}

	public static int getMaxSpeed() {
		return (int)Math.round(OBConfig.ITEM_DROPPER.maxItemDropSpeed.get() / SPEED_STEP);
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	public void setSpeed(int speed) {
		this.speed = Math.clamp(speed, 0, getMaxSpeed());
		setChanged();
	}

	public void toggleUseRedstoneStrength() {
		useRedstoneStrength = !useRedstoneStrength;
		setChanged();
	}

	/**
	 * Drops one item from the first non-empty slot into the block in front.
	 */
	public void dropItem(int redstoneSignal) {
		if (level == null || level.isClientSide) return;

		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			final ItemStack dropped = inventory.extractItem(slot, 1, false);
			if (dropped.isEmpty()) continue;

			final Direction facing = getBlockState().getValue(ItemDropperBlock.FACING);
			final float multiplier = Math.min(speed, getMaxSpeed()) * SPEED_STEP * (useRedstoneStrength? redstoneSignal / 15.0f : 1.0f);
			final Vec3 velocity = Vec3.atLowerCornerOf(facing.getNormal()).scale(multiplier);
			// centered in the block in front, like legacy; lowered so the item's center is there
			final Vec3 pos = Vec3.atCenterOf(worldPosition.relative(facing)).subtract(0, EntityType.ITEM.getHeight() / 2, 0);

			// unlike legacy no fake player and ItemTossEvent: the event is meant for items thrown by players
			final ItemEntity item = new ItemEntity(level, pos.x, pos.y, pos.z, dropped, velocity.x, velocity.y, velocity.z);
			item.setPickUpDelay(PICKUP_DELAY);
			level.addFreshEntity(item);
			return;
		}
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("openblocks.gui.itemdropper");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new ItemDropperMenu(containerId, playerInventory, this, data);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		inventory.deserializeNBT(registries, tag.getCompound("inventory"));
		speed = tag.getInt("speed");
		useRedstoneStrength = tag.getBoolean("use_redstone_strength");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
		tag.putInt("speed", speed);
		tag.putBoolean("use_redstone_strength", useRedstoneStrength);
	}
}
