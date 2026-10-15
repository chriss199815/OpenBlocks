package openblocks.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import openblocks.OBConfig;
import openblocks.block.entity.ProjectorBlockEntity;

/**
 * Port of legacy {@code BlockProjector}: a slab that is active (and lit) while it holds a height map. The spinner, the
 * holographic cone and the map are drawn by {@code ProjectorRenderer}. The legacy orientation is gone, the model looks
 * the same from all sides.
 */
public class ProjectorBlock extends BaseEntityBlock {

	public static final MapCodec<ProjectorBlock> CODEC = simpleCodec(ProjectorBlock::new);

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

	public ProjectorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	// from the config, so not in the block properties
	@Override
	public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
		return state.getValue(ACTIVE) && OBConfig.PROJECTOR.lightUpWhenWorking.getAsBoolean()? OBConfig.PROJECTOR.lightLevel.getAsInt() : 0;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof ProjectorBlockEntity projector)
			((ServerPlayer)player).openMenu(projector, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ProjectorBlockEntity projector)
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), projector.getInventory().getStackInSlot(0));
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ProjectorBlockEntity(pos, state);
	}
}
