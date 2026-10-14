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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.block.entity.ItemDropperBlockEntity;

/**
 * Port of legacy {@code BlockItemDropper}: drops one item out of its front on a rising redstone edge.
 */
public class ItemDropperBlock extends BaseEntityBlock {

	public static final MapCodec<ItemDropperBlock> CODEC = simpleCodec(ItemDropperBlock::new);

	public static final DirectionProperty FACING = DirectionalBlock.FACING;
	// replaces the legacy in-memory redstone state, like the vanilla dropper
	public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

	public ItemDropperBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(TRIGGERED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, TRIGGERED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// front faces the player, like the vanilla dropper
		return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof ItemDropperBlockEntity dropper)
			((ServerPlayer)player).openMenu(dropper, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
		if (level.isClientSide) return;
		final int signal = level.getBestNeighborSignal(pos);
		final boolean triggered = state.getValue(TRIGGERED);
		if (signal > 0 && !triggered) {
			level.setBlock(pos, state.setValue(TRIGGERED, true), Block.UPDATE_INVISIBLE);
			if (level.getBlockEntity(pos) instanceof ItemDropperBlockEntity dropper) dropper.dropItem(signal);
		} else if (signal == 0 && triggered) {
			level.setBlock(pos, state.setValue(TRIGGERED, false), Block.UPDATE_INVISIBLE);
		}
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ItemDropperBlockEntity dropper) {
			final ItemStackHandler inventory = dropper.getInventory();
			for (int slot = 0; slot < inventory.getSlots(); slot++)
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(slot));
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ItemDropperBlockEntity(pos, state);
	}
}
