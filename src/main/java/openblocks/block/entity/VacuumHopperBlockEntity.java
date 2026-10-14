package openblocks.block.entity;

import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.common.FluidXpUtils;
import openblocks.menu.VacuumHopperMenu;
import openblocks.registry.OBBlockEntities;
import openblocks.util.XpUtils;

/**
 * Port of legacy {@code TileEntityVacuumHopper}: pulls in items and XP orbs and pushes them out of the sides configured in the GUI.
 */
public class VacuumHopperBlockEntity extends BlockEntity implements MenuProvider {

	public enum Output {
		ITEMS, XP
	}

	/**
	 * Bit {@code side} for item outputs, bit {@code 6 + side} for XP outputs; read by the client model to draw the nozzles.
	 */
	public static final ModelProperty<Integer> OUTPUTS = new ModelProperty<>();

	/**
	 * Nozzle model variants, indexed by (outputs items ? 1 : 0) + (outputs XP ? 2 : 0) - 1.
	 */
	public static final String[] NOZZLES = { "items", "fluids", "both" };

	public static final int SLOTS = 10;

	private static final double RANGE = 3;
	private static final double COLLECT_DISTANCE = 1.1;
	private static final int OUTPUT_INTERVAL = 10;
	private static final int FLUID_OUTPUT_PER_SIDE = 50;

	private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
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

	private final Set<Direction> itemOutputs = EnumSet.noneOf(Direction.class);
	private final Set<Direction> xpOutputs = EnumSet.noneOf(Direction.class);

	private boolean vacuumDisabled;

	/**
	 * Extract-only, offered on item output sides, like legacy.
	 */
	private final IItemHandler outputHandler = new IItemHandler() {
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
			return stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return inventory.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return inventory.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return false;
		}
	};

	/**
	 * Drain-only, offered on XP output sides, like legacy.
	 */
	private final IFluidHandler drainHandler = new IFluidHandler() {
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
			return false;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return 0;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return tank.drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return tank.drain(maxDrain, action);
		}
	};

	public VacuumHopperBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.VACUUM_HOPPER.get(), pos, state);
	}

	public static int getTankCapacity() {
		return FluidXpUtils.xpToFluid(XpUtils.getExperienceForLevel(5));
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	public FluidStack getFluid() {
		return tank.getFluid();
	}

	public Set<Direction> getOutputs(Output output) {
		return output == Output.ITEMS? itemOutputs : xpOutputs;
	}

	@Nullable
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return side == null || itemOutputs.contains(side)? outputHandler : null;
	}

	@Nullable
	public IFluidHandler getFluidHandler(@Nullable Direction side) {
		return side == null || xpOutputs.contains(side)? drainHandler : null;
	}

	public void toggleOutput(Output output, Direction side) {
		final Set<Direction> sides = getOutputs(output);
		if (!sides.remove(side)) sides.add(side);
		setChanged();
		if (level != null) level.invalidateCapabilities(worldPosition);
		sendUpdate();
	}

	public boolean toggleVacuum() {
		vacuumDisabled = !vacuumDisabled;
		setChanged();
		sendUpdate();
		return vacuumDisabled;
	}

	private void sendUpdate() {
		if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, VacuumHopperBlockEntity hopper) {
		if (hopper.vacuumDisabled) return;

		if (level.isClientSide) {
			level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					level.random.nextDouble() - 0.5, level.random.nextDouble() - 1.0, level.random.nextDouble() - 0.5);
			return;
		}

		// unlike legacy, entities are only moved on the server: the client doesn't know whether the inventory has room
		final Vec3 center = Vec3.atCenterOf(pos);
		for (Entity entity : level.getEntities((Entity)null, new AABB(pos).inflate(RANGE), hopper::canPull)) {
			final Vec3 delta = center.subtract(entity.position());
			final double distance = delta.length();
			if (distance < COLLECT_DISTANCE) {
				hopper.collect(entity);
			} else {
				final double strength = Math.pow(1.0 - distance / 15.0, 2);
				entity.setDeltaMovement(entity.getDeltaMovement().add(delta.x / distance * strength * 0.05, delta.y / distance * strength * 0.2, delta.z / distance * strength * 0.05));
				// makes the server send the new motion right away
				entity.hasImpulse = true;
			}
		}

		if (level.getGameTime() % OUTPUT_INTERVAL == 0) {
			hopper.outputXp(level, pos);
			hopper.outputItems(level, pos);
		}
	}

	private boolean canPull(Entity entity) {
		if (!entity.isAlive()) return false;
		if (entity instanceof ItemEntity item) return ItemHandlerHelper.insertItem(inventory, item.getItem(), true).getCount() < item.getItem().getCount();
		if (entity instanceof ExperienceOrb orb) return tank.fill(FluidXpUtils.xpJuice(getOrbXp(orb)), IFluidHandler.FluidAction.SIMULATE) == FluidXpUtils.xpToFluid(getOrbXp(orb));
		return false;
	}

	/**
	 * Called when an entity touches the hopper or comes close enough.
	 */
	public void collect(Entity entity) {
		if (level == null || level.isClientSide || !entity.isAlive()) return;

		if (entity instanceof ItemEntity item) {
			final ItemStack leftover = ItemHandlerHelper.insertItem(inventory, item.getItem().copy(), false);
			if (leftover.isEmpty()) item.discard();
			else item.setItem(leftover);
		} else if (entity instanceof ExperienceOrb orb) {
			// unlike legacy, orbs are only taken if they fit completely, so no XP is lost
			final FluidStack xp = FluidXpUtils.xpJuice(getOrbXp(orb));
			if (tank.fill(xp, IFluidHandler.FluidAction.SIMULATE) == xp.getAmount()) {
				tank.fill(xp, IFluidHandler.FluidAction.EXECUTE);
				orb.discard();
			}
		}
	}

	private static int getOrbXp(ExperienceOrb orb) {
		return orb.getValue() * orb.count;
	}

	private void outputXp(Level level, BlockPos pos) {
		for (Direction side : xpOutputs) {
			if (tank.isEmpty()) return;
			final IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(side), side.getOpposite());
			if (target != null) FluidUtil.tryFluidTransfer(target, tank, FLUID_OUTPUT_PER_SIDE, true);
		}
	}

	/**
	 * Like legacy: moves the first stack that fits anywhere into one output, trying the sides in random order.
	 */
	private void outputItems(Level level, BlockPos pos) {
		if (itemOutputs.isEmpty()) return;
		for (Direction side : Util.shuffledCopy(itemOutputs.toArray(Direction[]::new), level.random)) {
			final IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(side), side.getOpposite());
			if (target == null) continue;
			for (int slot = 0; slot < inventory.getSlots(); slot++) {
				final ItemStack stack = inventory.extractItem(slot, inventory.getSlotLimit(slot), true);
				if (stack.isEmpty()) continue;
				final int moved = stack.getCount() - ItemHandlerHelper.insertItem(target, stack, false).getCount();
				if (moved > 0) {
					inventory.extractItem(slot, moved, false);
					return;
				}
			}
		}
	}

	@Override
	public ModelData getModelData() {
		int outputs = 0;
		for (Direction side : itemOutputs)
			outputs |= 1 << side.get3DDataValue();
		for (Direction side : xpOutputs)
			outputs |= 1 << (6 + side.get3DDataValue());
		return ModelData.builder().with(OUTPUTS, outputs).build();
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("openblocks.gui.vacuumhopper");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new VacuumHopperMenu(containerId, playerInventory, this);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		tank.readFromNBT(registries, tag.getCompound("tank"));
		// update packets don't carry the inventory; the open menu uses it on the client, so it must not be cleared
		if (tag.contains("inventory")) inventory.deserializeNBT(registries, tag.getCompound("inventory"));
		readSides(itemOutputs, tag.getByte("item_outputs"));
		readSides(xpOutputs, tag.getByte("xp_outputs"));
		vacuumDisabled = tag.getBoolean("vacuum_disabled");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("inventory", inventory.serializeNBT(registries));
		writeUpdateData(tag, registries);
	}

	private void writeUpdateData(CompoundTag tag, HolderLookup.Provider registries) {
		tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
		tag.putByte("item_outputs", writeSides(itemOutputs));
		tag.putByte("xp_outputs", writeSides(xpOutputs));
		tag.putBoolean("vacuum_disabled", vacuumDisabled);
	}

	private static void readSides(Set<Direction> sides, byte bits) {
		sides.clear();
		for (Direction side : Direction.values())
			if ((bits & (1 << side.get3DDataValue())) != 0) sides.add(side);
	}

	private static byte writeSides(Set<Direction> sides) {
		int bits = 0;
		for (Direction side : sides)
			bits |= 1 << side.get3DDataValue();
		return (byte)bits;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		final CompoundTag tag = new CompoundTag();
		writeUpdateData(tag, registries);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
		final int oldOutputs = getModelData().get(OUTPUTS);
		super.onDataPacket(connection, packet, registries);
		// redraw the nozzles, but not on every tank change
		if (level != null && getModelData().get(OUTPUTS) != oldOutputs) {
			requestModelDataUpdate();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
		}
	}
}
