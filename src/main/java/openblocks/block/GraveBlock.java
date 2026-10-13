package openblocks.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;
import openblocks.OpenBlocks;
import openblocks.block.entity.GraveBlockEntity;
import openblocks.registry.OBBlockEntities;
import openblocks.registry.OBBlocks;

/**
 * Port of legacy {@code BlockGrave}. {@link #HAS_BASE} shows a dirt mound when the grave stands on dirt or grass.
 * Breaking the grave releases its items; it never drops itself.
 */
public class GraveBlock extends BaseEntityBlock {

	public static final MapCodec<GraveBlock> CODEC = simpleCodec(GraveBlock::new);

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty HAS_BASE = BooleanProperty.create("base");

	/**
	 * Low collision box from legacy, so players can walk over graves.
	 */
	private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 3.2, 16);
	private static final VoxelShape OUTLINE_BASE = Block.box(0, 0, 0, 16, 1, 16);

	public GraveBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HAS_BASE, true));
	}

	public static BlockState stateFor(LevelAccessor level, BlockPos pos) {
		return OBBlocks.GRAVE.get().defaultBlockState().setValue(HAS_BASE, hasBase(level, pos));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HAS_BASE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState()
				.setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(HAS_BASE, hasBase(context.getLevel(), context.getClickedPos()));
	}

	private static boolean hasBase(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos.below()).is(BlockTags.DIRT);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (direction == Direction.DOWN) return state.setValue(HAS_BASE, hasBase(level, pos));
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.or(state.getValue(HAS_BASE)? OUTLINE_BASE : Shapes.empty(), stoneShape(state.getValue(FACING), state.getValue(HAS_BASE)));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISION;
	}

	private static VoxelShape stoneShape(Direction facing, boolean hasBase) {
		final double bottom = hasBase? 1 : 0;
		final double top = hasBase? 16 : 15;
		// the stone stands at the back, the inscription faces FACING
		return switch (facing) {
			case SOUTH -> Block.box(2, bottom, 0, 14, top, 2);
			case EAST -> Block.box(0, bottom, 2, 2, top, 14);
			case WEST -> Block.box(14, bottom, 2, 16, top, 14);
			default -> Block.box(2, bottom, 14, 14, top, 16);
		};
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide && placer != null && level.getBlockEntity(pos) instanceof GraveBlockEntity grave) grave.onPlacedBy(placer, stack);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.canPerformAction(ItemAbilities.SHOVEL_DIG)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof GraveBlockEntity grave && grave.rob(player))
			stack.hurtAndBreak(2, player, LivingEntity.getSlotForHand(hand));
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof GraveBlockEntity grave) grave.showDeathMessage(player);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
			grave.dropLoot();
			OpenBlocks.LOG.debug("Grave @ ({}) in {} destroyed", pos, level.dimension().location());
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GraveBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide? null : createTickerHelper(type, OBBlockEntities.GRAVE.get(), GraveBlockEntity::serverTick);
	}
}
