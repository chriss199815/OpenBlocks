package openblocks.block.entity;

import javax.annotation.Nullable;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.common.FluidXpUtils;
import openblocks.menu.XpBottlerMenu;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBSounds;

/**
 * Port of legacy {@code TileEntityXPBottler}: fills glass bottles with XP from its tank, which pulls XP fluid from adjacent blocks.
 */
public class XpBottlerBlockEntity extends BlockEntity implements MenuProvider {

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
			if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	};

	/**
	 * Automation may only insert bottles and take XP bottles (legacy default: input slot insert-only, output slot extract-only).
	 */
	private final IItemHandler automationHandler = new IItemHandler() {
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
			return slot == SLOT_INPUT? inventory.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return slot == SLOT_OUTPUT? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return inventory.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return slot == SLOT_INPUT && inventory.isItemValid(slot, stack);
		}
	};

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
	}

	public static int getTankCapacity() {
		return FluidXpUtils.xpToFluid(XP_PER_BOTTLE);
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	public IItemHandler getAutomationHandler() {
		return automationHandler;
	}

	public IFluidHandler getFluidHandler() {
		return fluidHandler;
	}

	public FluidStack getFluid() {
		return tank.getFluid();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, XpBottlerBlockEntity bottler) {
		bottler.pullFluid(level, pos);

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

	/**
	 * Replaces the legacy "auto drink" option: always pulls from all sides, since vanilla has no blocks that push fluids.
	 */
	private void pullFluid(Level level, BlockPos pos) {
		if (tank.getSpace() <= 0) return;
		for (Direction side : Direction.values()) {
			final IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(side), side.getOpposite());
			if (source != null && !FluidUtil.tryFluidTransfer(tank, source, PULL_PER_TICK, true).isEmpty()) return;
		}
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
		// update packets only carry the tank; the open menu uses this inventory on the client, so it must not be cleared
		if (tag.contains("inventory")) inventory.deserializeNBT(registries, tag.getCompound("inventory"));
		progress = tag.getInt("progress");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
		tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		tag.putInt("progress", progress);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		// the client only needs the tank for the GUI
		final CompoundTag tag = new CompoundTag();
		tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
