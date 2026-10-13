package openblocks.block.entity;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import openblocks.OBConfig;
import openblocks.block.TankBlock;
import openblocks.common.FluidXpUtils;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBComponents;

/**
 * Port of legacy {@code TileEntityTank}. Tanks fill bottom-up as a column and balance their contents with horizontal neighbours.
 */
public class TankBlockEntity extends BlockEntity {

	private static final String TANK_TAG = "tank";

	private static final int SYNC_THRESHOLD = 8;
	private static final int UPDATE_THRESHOLD = 20;

	private final FluidTank tank = new FluidTank(getTankCapacity()) {
		@Override
		protected void onContentsChanged() {
			contentsChanged();
		}
	};

	/**
	 * Exposed as capability; fills and drains the whole column, starting with this tank.
	 */
	private final IFluidHandler columnHandler = new IFluidHandler() {
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
			return accepts(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (resource.isEmpty()) return 0;
			final FluidStack remaining = resource.copy();
			fillColumn(remaining, action);
			return resource.getAmount() - remaining.getAmount();
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			final FluidStack contents = tank.getFluid();
			if (maxDrain <= 0 || contents.isEmpty()) return FluidStack.EMPTY;
			return drain(contents.copyWithAmount(maxDrain), action);
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			if (resource.isEmpty()) return FluidStack.EMPTY;
			final FluidStack needed = resource.copy();
			drainFromColumn(needed, action);
			return resource.copyWithAmount(resource.getAmount() - needed.getAmount());
		}
	};

	private boolean forceUpdate = true;
	private boolean needsSync;
	private boolean needsComparatorUpdate;
	private int ticksSinceLastSync = Math.floorMod(hashCode(), SYNC_THRESHOLD);
	private int ticksSinceLastUpdate = Math.floorMod(hashCode(), UPDATE_THRESHOLD);

	public TankBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.TANK.get(), pos, state);
	}

	public static int getTankCapacity() {
		return FluidType.BUCKET_VOLUME * OBConfig.TANKS.bucketsPerTank.getAsInt();
	}

	public FluidStack getFluid() {
		return tank.getFluid();
	}

	public int getCapacity() {
		return tank.getCapacity();
	}

	public double getFluidRatio() {
		return (double)tank.getFluidAmount() / tank.getCapacity();
	}

	public IFluidHandler getFluidHandler() {
		return columnHandler;
	}

	public boolean accepts(FluidStack liquid) {
		if (liquid.isEmpty()) return true;
		final FluidStack ownFluid = tank.getFluid();
		return ownFluid.isEmpty() || FluidStack.isSameFluidSameComponents(ownFluid, liquid);
	}

	private boolean containsFluid(FluidStack liquid) {
		final FluidStack ownFluid = tank.getFluid();
		return !ownFluid.isEmpty() && FluidStack.isSameFluidSameComponents(ownFluid, liquid);
	}

	private void contentsChanged() {
		forceUpdate = true;
		needsSync = true;
		needsComparatorUpdate = true;
		setChanged();
	}

	public void onNeighborChanged() {
		forceUpdate = true;
	}

	public boolean tryDrainXp(Player player) {
		if (!FluidXpUtils.isXpFluid(tank.getFluid())) return false;

		final int requiredXp = Mth.ceil(player.getXpNeededForNextLevel() * (1 - player.experienceProgress));
		final FluidStack drained = columnHandler.drain(FluidXpUtils.xpToFluid(requiredXp), IFluidHandler.FluidAction.SIMULATE);
		final int xp = FluidXpUtils.fluidToXp(drained.getAmount());
		if (xp <= 0) return false;

		columnHandler.drain(FluidXpUtils.xpToFluid(xp), IFluidHandler.FluidAction.EXECUTE);
		player.giveExperiencePoints(xp);
		return true;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, TankBlockEntity tank) {
		tank.tick(level, pos, state);
	}

	private void tick(Level level, BlockPos pos, BlockState state) {
		ticksSinceLastSync++;
		ticksSinceLastUpdate++;

		if (forceUpdate) {
			forceUpdate = false;

			if (OBConfig.TANKS.shouldTanksUpdate.getAsBoolean()) {
				if (!tank.isEmpty() && pos.getY() > level.getMinBuildHeight()) tryFillBottomTank(level, pos);
				if (!tank.isEmpty()) tryBalanceNeighbors(level, pos);
			}

			updateLight(level, pos, state);
		}

		if (needsSync && ticksSinceLastSync > SYNC_THRESHOLD) {
			needsSync = false;
			ticksSinceLastSync = 0;
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
		}

		if (needsComparatorUpdate && ticksSinceLastUpdate > UPDATE_THRESHOLD) {
			needsComparatorUpdate = false;
			ticksSinceLastUpdate = 0;
			level.updateNeighbourForOutputSignal(pos, state.getBlock());
		}
	}

	private void updateLight(Level level, BlockPos pos, BlockState state) {
		final FluidStack fluid = tank.getFluid();
		final int light = OBConfig.TANKS.emitLight.getAsBoolean() && !fluid.isEmpty()? fluid.getFluidType().getLightLevel(fluid) : 0;
		if (state.getValue(TankBlock.LIGHT) != light) level.setBlock(pos, state.setValue(TankBlock.LIGHT, light), Block.UPDATE_ALL);
	}

	@Nullable
	private static TankBlockEntity getTank(Level level, BlockPos pos) {
		if (!level.isLoaded(pos)) return null;
		return level.getBlockEntity(pos) instanceof TankBlockEntity tank && !tank.isRemoved()? tank : null;
	}

	private void tryFillBottomTank(Level level, BlockPos pos) {
		final TankBlockEntity below = getTank(level, pos.below());
		if (below == null || !below.accepts(tank.getFluid())) return;

		final int amount = below.tank.fill(tank.getFluid(), IFluidHandler.FluidAction.EXECUTE);
		if (amount > 0) tank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
	}

	private void tryBalanceNeighbors(Level level, BlockPos pos) {
		final FluidStack contents = tank.getFluid();
		final List<TankBlockEntity> neighbors = new ArrayList<>(4);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			final TankBlockEntity neighbor = getTank(level, pos.relative(side));
			if (neighbor != null && neighbor.accepts(contents)) neighbors.add(neighbor);
		}

		if (neighbors.isEmpty()) return;

		int sum = contents.getAmount();
		for (TankBlockEntity n : neighbors)
			sum += n.tank.getFluidAmount();

		final int suggestedAmount = sum / (neighbors.size() + 1);
		// don't balance small amounts to reduce server load
		if (Math.abs(suggestedAmount - contents.getAmount()) < OBConfig.TANKS.fluidDifferenceUpdateThreshold.getAsInt()) return;

		for (TankBlockEntity n : neighbors) {
			final int amount = n.tank.getFluidAmount();
			if (Math.abs(amount - suggestedAmount) > 1) {
				// setFluid doesn't fire onContentsChanged
				n.tank.setFluid(contents.copyWithAmount(suggestedAmount));
				n.contentsChanged();
				sum -= suggestedAmount;
			} else {
				sum -= amount;
			}
		}

		if (sum != tank.getFluidAmount()) {
			tank.setFluid(contents.copyWithAmount(sum));
			contentsChanged();
		}
	}

	private void fillColumn(FluidStack resource, IFluidHandler.FluidAction action) {
		if (!accepts(resource) || resource.isEmpty()) return;

		resource.shrink(tank.fill(resource, action));

		if (!resource.isEmpty()) {
			final TankBlockEntity above = getTank(level, worldPosition.above());
			if (above != null) above.fillColumn(resource, action);
		}
	}

	private void drainFromColumn(FluidStack needed, IFluidHandler.FluidAction action) {
		if (!containsFluid(needed) || needed.isEmpty()) return;

		// take from the top first, so the column empties from above
		final TankBlockEntity above = getTank(level, worldPosition.above());
		if (above != null) above.drainFromColumn(needed, action);

		if (needed.isEmpty()) return;

		needed.shrink(tank.drain(needed.getAmount(), action).getAmount());
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		final FluidStack previous = tank.getFluid();
		tank.readFromNBT(registries, tag.getCompound(TANK_TAG));

		// frame and fluid walls of neighbouring tanks depend on which fluid this one holds
		if (level != null && level.isClientSide && !FluidStack.isSameFluidSameComponents(previous, tank.getFluid()))
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put(TANK_TAG, tank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput componentInput) {
		super.applyImplicitComponents(componentInput);
		tank.setFluid(componentInput.getOrDefault(OBComponents.TANK_CONTENTS, SimpleFluidContent.EMPTY).copy());
		contentsChanged();
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (!tank.isEmpty()) components.set(OBComponents.TANK_CONTENTS, SimpleFluidContent.copyOf(tank.getFluid()));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void removeComponentsFromTag(CompoundTag tag) {
		tag.remove(TANK_TAG);
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
