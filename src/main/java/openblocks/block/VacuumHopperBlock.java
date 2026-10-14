package openblocks.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.block.entity.VacuumHopperBlockEntity;
import openblocks.registry.OBBlockEntities;

public class VacuumHopperBlock extends BaseEntityBlock {

	public static final MapCodec<VacuumHopperBlock> CODEC = simpleCodec(VacuumHopperBlock::new);

	private static final VoxelShape SHAPE = box(4, 4, 4, 12, 12, 12);
	// almost a full block, like legacy, so items landing on it touch it
	private static final VoxelShape COLLISION_SHAPE = box(0.16, 0.16, 0.16, 15.84, 15.84, 15.84);

	public VacuumHopperBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISION_SHAPE;
	}

	/**
	 * Sneaking with empty hands switches the suction on and off, otherwise opens the GUI.
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof VacuumHopperBlockEntity hopper) {
			if (player.isSecondaryUseActive()) hopper.toggleVacuum();
			else ((ServerPlayer)player).openMenu(hopper, pos);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (level.getBlockEntity(pos) instanceof VacuumHopperBlockEntity hopper) hopper.collect(entity);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof VacuumHopperBlockEntity hopper) {
			final ItemStackHandler inventory = hopper.getInventory();
			for (int slot = 0; slot < inventory.getSlots(); slot++)
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(slot));
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VacuumHopperBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, OBBlockEntities.VACUUM_HOPPER.get(), VacuumHopperBlockEntity::tick);
	}
}
