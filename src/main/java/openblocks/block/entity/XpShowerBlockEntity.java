package openblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import openblocks.block.XpShowerBlock;
import openblocks.common.FluidXpUtils;
import openblocks.entity.NoFlyXpOrb;
import openblocks.particle.LiquidSprayOptions;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code TileEntityXPShower}: while powered, pulls XP fluid from the block behind it and drops it as orbs.
 */
public class XpShowerBlockEntity extends BlockEntity {

	private static final int DRAIN_PER_CYCLE = 100;
	private static final int ORB_SPAWN_FREQUENCY = 3;
	private static final int PARTICLE_DURATION = 10;

	private static final int EVENT_ORB_SPAWNED = 0;

	private final FluidTank buffer = new FluidTank(FluidType.BUCKET_VOLUME, FluidXpUtils::isXpFluid) {
		@Override
		protected void onContentsChanged() {
			setChanged();
		}
	};

	private int particleTimer;

	public XpShowerBlockEntity(BlockPos pos, BlockState state) {
		super(OBBlockEntities.XP_SHOWER.get(), pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, XpShowerBlockEntity shower) {
		if (level.getGameTime() % ORB_SPAWN_FREQUENCY != 0 || !state.getValue(XpShowerBlock.POWERED)) return;

		final Direction facing = state.getValue(XpShowerBlock.FACING);
		final IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(facing.getOpposite()), facing);
		if (source != null) FluidUtil.tryFluidTransfer(shower.buffer, source, DRAIN_PER_CYCLE, true);

		final FluidStack contents = shower.buffer.getFluid();
		if (contents.isEmpty()) return;

		// never returns 0, so check below that the buffer holds enough
		final int xpInOrb = ExperienceOrb.getExperienceValue(FluidXpUtils.fluidToXp(contents.getAmount()));
		final int toDrain = FluidXpUtils.xpToFluid(xpInOrb);
		if (toDrain > contents.getAmount()) return;

		shower.buffer.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
		level.addFreshEntity(new NoFlyXpOrb(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, xpInOrb));
		level.blockEvent(pos, state.getBlock(), EVENT_ORB_SPAWNED, 0);
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, XpShowerBlockEntity shower) {
		if (shower.particleTimer <= 0) return;
		shower.particleTimer--;

		final double vx = (level.random.nextDouble() - 0.5) * 0.05;
		final double vz = (level.random.nextDouble() - 0.5) * 0.05;
		// the particle engine thins out particles according to the client's particle setting
		level.addParticle(new LiquidSprayOptions(FluidXpUtils.xpJuice(1), 0.4f, 0.7f), pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, vx, 0, vz);
	}

	@Override
	public boolean triggerEvent(int id, int param) {
		if (id == EVENT_ORB_SPAWNED) {
			particleTimer = PARTICLE_DURATION;
			return true;
		}
		return super.triggerEvent(id, param);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		buffer.readFromNBT(registries, tag);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		buffer.writeToNBT(registries, tag);
	}
}
