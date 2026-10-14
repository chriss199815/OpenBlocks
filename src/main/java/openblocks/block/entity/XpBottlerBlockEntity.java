package openblocks.block.entity;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.common.FluidXpUtils;
import openblocks.menu.XpBottlerMenu;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBSounds;
import openblocks.util.SideBits;

/**
 * Port of legacy {@code TileEntityXPBottler}: fills glass bottles with XP from its tank.
 * Each of its {@link AutoSlot}s has a set of sides it is accessible from and an "auto" flag that makes the bottler move things itself.
 */
public class XpBottlerBlockEntity extends BlockEntity implements MenuProvider {

	public enum AutoSlot {
		/** Glass bottles in; auto extract pulls them from neighbours. */
		INPUT,
		/** XP bottles out; auto eject pushes them to neighbours. */
		OUTPUT,
		/** XP fluid in; auto drink pulls it from neighbours. */
		XP
	}

	public static final int SLOT_INPUT = 0;
	public static final int SLOT_OUTPUT = 1;

	public static final int XP_PER_BOTTLE = 8;
	public static final int PROGRESS_TICKS = 40;

	private static final int PULL_PER_TICK = 10;

	private final ItemStackHandler inventory = new ItemStackHandler(2) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return slot == SLOT_INPUT && stack.is(Items.GLASS_BOTTLE);
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}
	};

	private final FluidTank tank = new FluidTank(getTankCapacity(), FluidXpUtils::isXpFluid) {
		@Override
		protected void onContentsChanged() {
			setChanged();
			sendUpdate();
		}
	};

	private final Map<AutoSlot, Set<Direction>> sides = new EnumMap<>(AutoSlot.class);
	private final Set<AutoSlot> autoSlots = EnumSet.noneOf(AutoSlot.class);

	/**
	 * Automation may only insert bottles from input sides and take XP bottles from output sides; index 6 is for the null side.
	 */
	private final IItemHandler[] itemHandlers = new IItemHandler[7];

	/**
	 * Fill-only, like legacy.
	 */
	private final IFluidHandler fluidHandler = new IFluidHandler() {
		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int index) {
			return tank.getFluid();
		}

		@Override
		public int getTankCapacity(int index) {
			return tank.getCapacity();
		}

		@Override
		public boolean isFluidValid(int index, FluidStack stack) {
			return tank.isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return tank.fill(resource, action);
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return FluidStack.EMPTY;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return FluidStack.EMPTY;
		}
	};

	private int progress;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return progress;
		}

		@Override
		public void set(int index, int value) {
			progress = value;
		}

		@Override
		public int getCount() {
			return 1;
		}
	};

	public XpBottlerBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.XP_BOTTLER.get(), pos, state);
		// unlike legacy (no access until configured), new bottlers are open on all sides and drink XP like vanilla machines
		for (AutoSlot slot : AutoSlot.values())
			sides.put(slot, EnumSet.allOf(Direction.class));
		autoSlots.add(AutoSlot.XP);

		for (Direction side : Direction.values())
			itemHandlers[side.get3DDataValue()] = new AutomationHandler(side);
		itemHandlers[6] = new AutomationHandler(null);
	}

	public static int getTankCapacity() {
		return FluidXpUtils.xpToFluid(XP_PER_BOTTLE);
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	public FluidStack getFluid() {
		return tank.getFluid();
	}

	public Set<Direction> getSides(AutoSlot slot) {
		return sides.get(slot);
	}

	public boolean isAuto(AutoSlot slot) {
		return autoSlots.contains(slot);
	}

	@Nullable
	public IItemHandler getItemHandler(@Nullable Direction side) {
		if (side == null) return itemHandlers[6];
		return getSides(AutoSlot.INPUT).contains(side) || getSides(AutoSlot.OUTPUT).contains(side)? itemHandlers[side.get3DDataValue()] : null;
	}

	@Nullable
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return side == null || getSides(AutoSlot.XP).contains(side)? fluidHandler : null;
	}

	public void toggleSide(AutoSlot slot, Direction side) {
		final Set<Direction> slotSides = getSides(slot);
		if (!slotSides.remove(side)) slotSides.add(side);
		setChanged();
		if (level != null) level.invalidateCapabilities(worldPosition);
		sendUpdate();
	}

	public void toggleAuto(AutoSlot slot) {
		if (!autoSlots.remove(slot)) autoSlots.add(slot);
		setChanged();
		sendUpdate();
	}

	private void sendUpdate() {
		if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, XpBottlerBlockEntity bottler) {
		if (bottler.isAuto(AutoSlot.XP)) bottler.pullFluid(level, pos);
		if (bottler.isAuto(AutoSlot.OUTPUT)) bottler.pushOutput(level, pos);
		if (bottler.isAuto(AutoSlot.INPUT)) bottler.pullInput(level, pos);

		if (!bottler.canWork()) {
			bottler.progress = 0;
			return;
		}

		if (++bottler.progress < PROGRESS_TICKS) return;
		bottler.progress = 0;

		bottler.inventory.extractItem(SLOT_INPUT, 1, false);
		bottler.tank.drain(FluidXpUtils.xpToFluid(XP_PER_BOTTLE), IFluidHandler.FluidAction.EXECUTE);
		// not insertItem: isItemValid rejects everything in the output slot, so players and automation can't put items there
		final ItemStack output = bottler.inventory.getStackInSlot(SLOT_OUTPUT);
		bottler.inventory.setStackInSlot(SLOT_OUTPUT, output.isEmpty()? new ItemStack(Items.EXPERIENCE_BOTTLE) : output.copyWithCount(output.getCount() + 1));
		level.playSound(null, pos, OBSounds.BOTTLER_SIGNAL.get(), SoundSource.BLOCKS, 0.5f, 0.8f);
	}

	private void pullFluid(Level level, BlockPos pos) {
		if (tank.getSpace() <= 0) return;
		for (Direction side : getSides(AutoSlot.XP)) {
			final IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(side), side.getOpposite());
			if (source != null && !FluidUtil.tryFluidTransfer(tank, source, PULL_PER_TICK, true).isEmpty()) return;
		}
	}

	/**
	 * Like legacy: one XP bottle per tick into the first output side (in random order) that takes it.
	 */
	private void pushOutput(Level level, BlockPos pos) {
		final ItemStack output = inventory.extractItem(SLOT_OUTPUT, 1, true);
		if (output.isEmpty()) return;
		for (Direction side : shuffledSides(level, AutoSlot.OUTPUT)) {
			final IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(side), side.getOpposite());
			if (target != null && ItemHandlerHelper.insertItem(target, output, false).isEmpty()) {
				inventory.extractItem(SLOT_OUTPUT, 1, false);
				return;
			}
		}
	}

	/**
	 * Like legacy: one glass bottle, only when the input slot is empty, from the first input side (in random order) that has one.
	 */
	private void pullInput(Level level, BlockPos pos) {
		if (!inventory.getStackInSlot(SLOT_INPUT).isEmpty()) return;
		for (Direction side : shuffledSides(level, AutoSlot.INPUT)) {
			final IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(side), side.getOpposite());
			if (source == null) continue;
			for (int slot = 0; slot < source.getSlots(); slot++) {
				if (!source.extractItem(slot, 1, true).is(Items.GLASS_BOTTLE)) continue;
				inventory.insertItem(SLOT_INPUT, source.extractItem(slot, 1, false), false);
				return;
			}
		}
	}

	private List<Direction> shuffledSides(Level level, AutoSlot slot) {
		return Util.shuffledCopy(getSides(slot).toArray(Direction[]::new), level.random);
	}

	private boolean canWork() {
		final ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
		final ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
		final boolean hasSpace = output.isEmpty() || (output.is(Items.EXPERIENCE_BOTTLE) && output.getCount() < output.getMaxStackSize());
		return input.is(Items.GLASS_BOTTLE) && hasSpace && FluidXpUtils.fluidToXp(tank.getFluidAmount()) >= XP_PER_BOTTLE;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("openblocks.gui.xpbottler");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new XpBottlerMenu(containerId, playerInventory, this, data);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		tank.readFromNBT(registries, tag.getCompound("tank"));
		// update packets don't carry the inventory; the open menu uses it on the client, so it must not be cleared
		if (tag.contains("inventory")) inventory.deserializeNBT(registries, tag.getCompound("inventory"));
		progress = tag.getInt("progress");
		for (AutoSlot slot : AutoSlot.values()) {
			final String key = slot.name().toLowerCase(Locale.ROOT);
			if (tag.contains(key + "_sides")) SideBits.read(getSides(slot), tag.getByte(key + "_sides"));
			if (tag.contains(key + "_auto")) {
				if (tag.getBoolean(key + "_auto")) autoSlots.add(slot);
				else autoSlots.remove(slot);
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
		tag.putInt("progress", progress);
		writeUpdateData(tag, registries);
	}

	private void writeUpdateData(CompoundTag tag, HolderLookup.Provider registries) {
		tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		for (AutoSlot slot : AutoSlot.values()) {
			final String key = slot.name().toLowerCase(Locale.ROOT);
			tag.putByte(key + "_sides", SideBits.write(getSides(slot)));
			tag.putBoolean(key + "_auto", isAuto(slot));
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		// the client needs the tank and the side configuration for the GUI
		final CompoundTag tag = new CompoundTag();
		writeUpdateData(tag, registries);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	private class AutomationHandler implements IItemHandler {

		@Nullable
		private final Direction side;

		private AutomationHandler(@Nullable Direction side) {
			this.side = side;
		}

		private boolean isOpen(AutoSlot slot) {
			return side == null || getSides(slot).contains(side);
		}

		@Override
		public int getSlots() {
			return inventory.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return inventory.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return slot == SLOT_INPUT && isOpen(AutoSlot.INPUT)? inventory.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return slot == SLOT_OUTPUT && isOpen(AutoSlot.OUTPUT)? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return inventory.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return slot == SLOT_INPUT && isOpen(AutoSlot.INPUT) && inventory.isItemValid(slot, stack);
		}
	}
}
