package openblocks.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import openblocks.block.entity.VillageHighlighterBlockEntity;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code BlockVillageHighlighter}: while powered, the block entity renderer shows the village around it
 * (see {@link VillageHighlighterBlockEntity}). It emits a full signal while villagers can breed.
 * <p>
 * Unlike legacy, the signal only leaves through the back, and power coming in from the back is ignored: legacy emitted
 * on all sides and listened on all sides, so redstone dust next to it kept it powered by its own signal.
 */
public class VillageHighlighterBlock extends BaseEntityBlock {

	public static final MapCodec<VillageHighlighterBlock> CODEC = simpleCodec(VillageHighlighterBlock::new);

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public VillageHighlighterBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		final BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		return state.setValue(POWERED, isPowered(context.getLevel(), context.getClickedPos(), state));
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	private static Direction back(BlockState state) {
		return state.getValue(FACING).getOpposite();
	}

	private static boolean isPowered(Level level, BlockPos pos, BlockState state) {
		final Direction back = back(state);
		for (Direction direction : Direction.values())
			if (direction != back && level.getSignal(pos.relative(direction), direction) > 0) return true;
		return false;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		// placed without a context (e.g. by commands)
		if (!oldState.is(this)) updatePowerState(state, level, pos);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
		updatePowerState(state, level, pos);
	}

	private static void updatePowerState(BlockState state, Level level, BlockPos pos) {
		if (level.isClientSide) return;
		final boolean powered = isPowered(level, pos, state);
		if (powered != state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		super.onRemove(state, level, pos, newState, movedByPiston);
		if (!state.is(newState.getBlock())) updateOutput(level, pos, state);
	}

	/**
	 * Notifies the block behind it (and that block's neighbors) of a changed signal.
	 */
	public static void updateOutput(Level level, BlockPos pos, BlockState state) {
		final BlockPos target = pos.relative(back(state));
		level.neighborChanged(target, state.getBlock(), pos);
		level.updateNeighborsAtExceptFromFacing(target, state.getBlock(), back(state).getOpposite());
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		// direction points from the neighbor to this block
		if (direction != state.getValue(FACING)) return 0;
		return level.getBlockEntity(pos) instanceof VillageHighlighterBlockEntity highlighter && highlighter.canVillagersBreed()? 15 : 0;
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return getSignal(state, level, pos, direction);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VillageHighlighterBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide? null : createTickerHelper(type, OBBlockEntities.VILLAGE_HIGHLIGHTER.get(), VillageHighlighterBlockEntity::serverTick);
	}
}
