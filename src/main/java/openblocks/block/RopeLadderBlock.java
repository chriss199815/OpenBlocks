package openblocks.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import openblocks.OBConfig;

/**
 * Port of legacy {@code BlockRopeLadder}: hangs on any block behind it or on the rope ladder above, rolls down to the
 * ground when placed (one item per block) and rolls up again when broken. Climbable through the {@code minecraft:climbable} tag.
 * <p>
 * Like vanilla ladders, {@link #FACING} points away from the supporting block.
 */
public class RopeLadderBlock extends Block {

	public static final MapCodec<RopeLadderBlock> CODEC = simpleCodec(RopeLadderBlock::new);

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 15, 16, 16, 16);
	private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 0, 16, 16, 1);
	private static final VoxelShape WEST_SHAPE = Block.box(15, 0, 0, 16, 16, 16);
	private static final VoxelShape EAST_SHAPE = Block.box(0, 0, 0, 1, 16, 16);

	public RopeLadderBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case SOUTH -> SOUTH_SHAPE;
			case WEST -> WEST_SHAPE;
			case EAST -> EAST_SHAPE;
			default -> NORTH_SHAPE;
		};
	}

	// like legacy, mobs only bump into the ladder while facing it, so it can be walked through from the side
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof LivingEntity entity
				&& entity.getDirection() != state.getValue(FACING).getOpposite())
			return Shapes.empty();
		return getShape(state, level, pos, context);
	}

	private boolean hasSupport(LevelReader level, BlockPos pos, Direction facing) {
		return !level.isEmptyBlock(pos.relative(facing.getOpposite())) || level.getBlockState(pos.above()).is(this);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return hasSupport(level, pos, state.getValue(FACING));
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}

	/**
	 * Clicked side of a block first, then any neighbour in the looking direction, then the ladder above (clicking its bottom).
	 */
	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		final Level level = context.getLevel();
		final BlockPos pos = context.getClickedPos();
		final Direction clicked = context.getClickedFace();
		if (clicked.getAxis().isHorizontal())
			return defaultBlockState().setValue(FACING, clicked);

		for (Direction dir : context.getNearestLookingDirections()) {
			if (dir.getAxis().isHorizontal() && !level.isEmptyBlock(pos.relative(dir)))
				return defaultBlockState().setValue(FACING, dir.getOpposite());
		}

		final BlockState above = level.getBlockState(pos.above());
		return above.is(this)? defaultBlockState().setValue(FACING, above.getValue(FACING)) : null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (level.isClientSide || !(placer instanceof Player player)) return;

		// the placed item is consumed after this call, so one item has to stay in the stack
		final boolean infinite = OBConfig.LADDER.infiniteMode.getAsBoolean() || player.hasInfiniteMaterials();
		BlockPos placePos = pos.below();
		while (placePos.getY() >= level.getMinBuildHeight() && (infinite || stack.getCount() > 1)) {
			if (!level.isEmptyBlock(placePos) || !player.mayUseItemAt(placePos, Direction.DOWN, stack)) return;

			final BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, placePos);
			level.setBlock(placePos, state, Block.UPDATE_ALL);
			if (EventHooks.onBlockPlace(player, snapshot, Direction.DOWN)) {
				snapshot.restore(Block.UPDATE_ALL);
				return;
			}

			if (!infinite) stack.shrink(1);
			placePos = placePos.below();
		}
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		super.onRemove(state, level, pos, newState, movedByPiston);
		if (level.isClientSide || state.is(newState.getBlock())) return;

		// roll up the ladder below; bottom first, so the removed blocks don't recurse into each other
		BlockPos bottom = pos;
		while (level.getBlockState(bottom.below()).is(this))
			bottom = bottom.below();
		for (BlockPos p = bottom; p.getY() < pos.getY(); p = p.above())
			level.destroyBlock(p, true);
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
		return OBConfig.LADDER.infiniteMode.getAsBoolean()? List.of() : super.getDrops(state, params);
	}
}
