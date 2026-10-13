package openblocks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import openblocks.registry.OBBlocks;

/**
 * Elevator that turns the arriving player to {@link #FACING}. Like in the legacy mod, it faces the placer.
 */
public class RotatingElevatorBlock extends ElevatorBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public RotatingElevatorBlock(DyeColor color, Properties properties) {
		super(color, properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public String getDescriptionId() {
		return "block.openblocks.elevator_rotating";
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected BlockState recolor(BlockState state, DyeColor dye) {
		return OBBlocks.ROTATING_ELEVATORS.get(dye).get().defaultBlockState().setValue(FACING, state.getValue(FACING));
	}

	@Override
	public Direction getRotation(BlockGetter level, BlockPos pos, BlockState state) {
		return state.getValue(FACING);
	}
}
