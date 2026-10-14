package openblocks.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import openblocks.OBConfig;

/**
 * Port of legacy {@code BlockSponge}: removes all liquid blocks in a cube around it when placed, when a neighbour changes
 * and every 5 seconds. Catches fire when it touches lava.
 * <p>
 * Unless {@code sponge.blockUpdate} is set, the removal doesn't notify neighbours, so liquids at the edge stay in place
 * until the sponge is removed.
 */
public class SpongeBlock extends Block {

	public static final MapCodec<SpongeBlock> CODEC = simpleCodec(SpongeBlock::new);

	private static final int TICK_RATE = 20 * 5;

	private static final int EVENT_BURN = 123;

	public SpongeBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (level.isClientSide || oldState.is(this)) return;
		clearLiquids(level, pos);
		scheduleTick(level, pos);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		clearLiquids(level, pos);
		scheduleTick(level, pos);
	}

	private void scheduleTick(Level level, BlockPos pos) {
		level.scheduleTick(pos, this, TICK_RATE + level.random.nextInt(5));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
		if (!level.isClientSide) clearLiquids(level, pos);
	}

	private static boolean isLiquid(BlockState state) {
		return state.getBlock() instanceof LiquidBlock;
	}

	private void clearLiquids(Level level, BlockPos pos) {
		// without neighbour and shape updates, liquids at the edge don't start flowing into the cleared area
		final int flags = OBConfig.SPONGE.blockUpdate.getAsBoolean()? Block.UPDATE_ALL : Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
		final int range = OBConfig.SPONGE.range.getAsInt();
		boolean hitLava = false;
		for (BlockPos workPos : BlockPos.betweenClosed(pos.offset(-range, -range, -range), pos.offset(range, range, range))) {
			if (!level.isLoaded(workPos)) continue;
			final BlockState state = level.getBlockState(workPos);
			if (isLiquid(state)) {
				hitLava |= state.getFluidState().is(FluidTags.LAVA);
				level.setBlock(workPos, Blocks.AIR.defaultBlockState(), flags);
			}
		}
		if (hitLava) level.blockEvent(pos, this, EVENT_BURN, 0);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		super.onRemove(state, level, pos, newState, movedByPiston);
		if (level.isClientSide || state.is(newState.getBlock()) || OBConfig.SPONGE.blockUpdate.getAsBoolean()) return;

		// unfreeze liquids on the border of the cleared area
		final int range = OBConfig.SPONGE.range.getAsInt() + 1;
		for (BlockPos workPos : BlockPos.betweenClosed(pos.offset(-range, -range, -range), pos.offset(range, range, range))) {
			if (!level.isLoaded(workPos)) continue;
			final BlockState liquid = level.getBlockState(workPos);
			if (isLiquid(liquid)) {
				final FluidState fluid = liquid.getFluidState();
				level.scheduleTick(workPos, fluid.getType(), fluid.getType().getTickDelay(level));
			}
		}
	}

	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		if (id != EVENT_BURN) return super.triggerEvent(state, level, pos, id, param);

		if (level.isClientSide) {
			for (int i = 0; i < 20; i++) {
				final double px = pos.getX() + level.random.nextDouble() * 0.1;
				final double py = pos.getY() + 1.0 + level.random.nextDouble();
				final double pz = pos.getZ() + level.random.nextDouble();
				level.addParticle(ParticleTypes.LARGE_SMOKE, px, py, pz, 0.0, 0.0, 0.0);
			}
		} else {
			level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
		}
		return true;
	}
}
