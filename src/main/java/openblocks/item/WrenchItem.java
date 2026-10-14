package openblocks.item;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import openblocks.block.BigButtonBlock;

/**
 * Port of legacy {@code ItemWrench} ("Big Metal Bar"): rotates the clicked block around the axis of the clicked side,
 * before the block itself reacts. Levers and buttons (and chests, which would open) are only rotated while sneaking.
 * <p>
 * Replaces the 1.12 {@code Block.rotateBlock}: blocks facing in all 6 directions turn around the clicked axis, blocks facing
 * horizontally turn when clicked on top or bottom (through {@link BlockState#rotate}), pillars take the clicked axis.
 * Multi-block parts (beds, doors, double chests) and extended pistons are left alone, and the result has to be able to stay.
 */
public class WrenchItem extends Item {

	public WrenchItem(Properties properties) {
		super(properties);
	}

	private static boolean requiresSneaking(Block block) {
		return block instanceof LeverBlock || block instanceof ButtonBlock || block instanceof BigButtonBlock || block instanceof ChestBlock;
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
		final Level level = context.getLevel();
		final BlockPos pos = context.getClickedPos();
		final Player player = context.getPlayer();
		final BlockState state = level.getBlockState(pos);

		// like legacy, the wrench never lets the block itself react
		if (requiresSneaking(state.getBlock()) && (player == null || !player.isSecondaryUseActive())) return InteractionResult.FAIL;

		final Optional<BlockState> rotated = rotate(state, level, pos, context.getClickedFace());
		if (rotated.isEmpty()) return InteractionResult.FAIL;

		if (!level.isClientSide) {
			level.setBlockAndUpdate(pos, rotated.get());
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	public static Optional<BlockState> rotate(BlockState state, Level level, BlockPos pos, Direction side) {
		if (isMultiblockPart(state)) return Optional.empty();

		final Direction.Axis axis = side.getAxis();
		BlockState result = state;
		if (state.hasProperty(BlockStateProperties.FACING)) {
			final Direction facing = state.getValue(BlockStateProperties.FACING);
			if (facing.getAxis() != axis) result = state.setValue(BlockStateProperties.FACING, facing.getClockWise(axis));
		} else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) || state.hasProperty(BlockStateProperties.ROTATION_16)) {
			if (axis == Direction.Axis.Y) result = state.rotate(level, pos, Rotation.CLOCKWISE_90);
		} else if (state.hasProperty(BlockStateProperties.AXIS)) {
			if (state.getValue(BlockStateProperties.AXIS) != axis) result = state.setValue(BlockStateProperties.AXIS, axis);
		} else if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
			if (axis == Direction.Axis.Y) result = state.rotate(level, pos, Rotation.CLOCKWISE_90);
		}

		if (result == state || !result.canSurvive(level, pos)) return Optional.empty();
		return Optional.of(result);
	}

	private static boolean isMultiblockPart(BlockState state) {
		return state.hasProperty(BlockStateProperties.BED_PART)
				|| state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
				|| (state.hasProperty(BlockStateProperties.CHEST_TYPE) && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE)
				|| (state.hasProperty(BlockStateProperties.EXTENDED) && state.getValue(BlockStateProperties.EXTENDED))
				|| state.hasProperty(BlockStateProperties.PISTON_TYPE);
	}
}
