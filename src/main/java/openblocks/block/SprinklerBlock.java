package openblocks.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.block.entity.SprinklerBlockEntity;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code BlockSprinkler}. {@link #AXIS} is the direction of the nozzle, which {@code SprinklerRenderer} swings from side to side.
 */
public class SprinklerBlock extends BaseEntityBlock {

	public static final MapCodec<SprinklerBlock> CODEC = simpleCodec(SprinklerBlock::new);

	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

	private static final VoxelShape SHAPE_Z = box(4.8, 0, 0, 11.2, 4.8, 16);
	private static final VoxelShape SHAPE_X = box(0, 0, 4.8, 16, 4.8, 11.2);

	public SprinklerBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// nozzle across the player's view, so it sprays towards and away from them
		return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AXIS) == Direction.Axis.X? SHAPE_X : SHAPE_Z;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return direction == Direction.DOWN && !canSurvive(state, level, pos)? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof SprinklerBlockEntity sprinkler)
			((ServerPlayer)player).openMenu(sprinkler, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SprinklerBlockEntity sprinkler) {
			final ItemStackHandler inventory = sprinkler.getInventory();
			for (int slot = 0; slot < inventory.getSlots(); slot++)
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(slot));
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SprinklerBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, OBBlockEntities.SPRINKLER.get(),
				level.isClientSide? SprinklerBlockEntity::clientTick : SprinklerBlockEntity::serverTick);
	}
}
