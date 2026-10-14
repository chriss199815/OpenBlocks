package openblocks.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import openblocks.block.BlockManipulatorBlock;
import openblocks.common.OBFakePlayer;

/**
 * Port of legacy {@code TileEntityBlockManipulator}. On a redstone signal it schedules a block event, which does the work at the end of the tick,
 * so breaking or placing doesn't recursively trigger neighbours. While powered, it keeps working, but unlike legacy (up to every tick, limited
 * only by an action count) it waits {@link #getCooldown()} ticks after each action.
 */
public abstract class BlockManipulatorBlockEntity extends BlockEntity {

	public static final int EVENT_ACTIVATE = 3;

	private int cooldown;

	protected BlockManipulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	protected abstract int getCooldown();

	protected abstract boolean canWork(BlockState targetState, BlockPos target, Direction direction);

	protected abstract void doWork(BlockState targetState, BlockPos target, Direction direction);

	protected static FakePlayer getFakePlayer(ServerLevel level) {
		return OBFakePlayer.get(level);
	}

	protected Direction getFacing() {
		return getBlockState().getValue(BlockManipulatorBlock.FACING);
	}

	protected boolean isPowered() {
		return getBlockState().getValue(BlockManipulatorBlock.POWERED);
	}

	public void serverTick() {
		if (cooldown > 0) cooldown--;
		else if (isPowered()) triggerAction();
	}

	public void onNeighborChanged() {
		if (level == null) return;

		final boolean powered = level.hasNeighborSignal(worldPosition);
		if (powered != isPowered()) {
			level.setBlock(worldPosition, getBlockState().setValue(BlockManipulatorBlock.POWERED, powered), Block.UPDATE_CLIENTS);
			level.playSound(null, worldPosition, powered? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.15f + 0.6f);
		}

		if (powered) triggerAction();
	}

	private void triggerAction() {
		if (level == null || cooldown > 0) return;
		final Direction direction = getFacing();
		final BlockPos target = worldPosition.relative(direction);
		if (level.isLoaded(target) && canWork(level.getBlockState(target), target, direction)) {
			level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_ACTIVATE, 0);
			cooldown = getCooldown();
		}
	}

	public void doWork() {
		if (level == null) return;
		final Direction direction = getFacing();
		final BlockPos target = worldPosition.relative(direction);
		if (!level.isLoaded(target)) return;
		final BlockState targetState = level.getBlockState(target);
		if (canWork(targetState, target, direction)) doWork(targetState, target, direction);
	}
}
