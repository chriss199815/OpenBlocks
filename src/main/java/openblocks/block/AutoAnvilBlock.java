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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.block.entity.AutoAnvilBlockEntity;
import openblocks.registry.OBBlockEntities;

/**
 * Port of legacy {@code BlockAutoAnvil}. Like the vanilla anvil, the long side faces the player.
 */
public class AutoAnvilBlock extends BaseEntityBlock {

	public static final MapCodec<AutoAnvilBlock> CODEC = simpleCodec(AutoAnvilBlock::new);

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	// parts of the model, top along the z axis
	private static final VoxelShape SHAPE_Z = Shapes.or(
			box(2, 0, 2, 14, 4, 14),
			box(4, 4, 3, 12, 5, 13),
			box(6, 5, 4, 10, 10, 12),
			box(3, 10, 0, 13, 16, 16));
	private static final VoxelShape SHAPE_X = Shapes.or(
			box(2, 0, 2, 14, 4, 14),
			box(3, 4, 4, 13, 5, 12),
			box(4, 5, 6, 12, 10, 10),
			box(0, 10, 3, 16, 16, 13));

	public AutoAnvilBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		// the top runs across the player's view
		return state.getValue(FACING).getAxis() == Direction.Axis.Z? SHAPE_X : SHAPE_Z;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof AutoAnvilBlockEntity anvil)
			((ServerPlayer)player).openMenu(anvil, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AutoAnvilBlockEntity anvil) {
			final ItemStackHandler inventory = anvil.getInventory();
			for (int slot = 0; slot < inventory.getSlots(); slot++)
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(slot));
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AutoAnvilBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide? null : createTickerHelper(type, OBBlockEntities.AUTO_ANVIL.get(), AutoAnvilBlockEntity::serverTick);
	}
}
