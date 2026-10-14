package openblocks.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemStackHandler;
import openblocks.block.entity.BigButtonBlockEntity;

/**
 * Port of legacy {@code BlockBigButton} and {@code BlockBigButtonWood}: a 14x14 button on any side of a block. It stays
 * pressed for as many ticks as items are in its inventory (sneak + right click with an empty hand opens it).
 * Wooden buttons (block set types that allow it) are also pressed by arrows and stay pressed while arrows stick in them.
 */
public class BigButtonBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {

	public static final MapCodec<BigButtonBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BlockSetType.CODEC.fieldOf("block_set_type").forGetter(block -> block.type),
			propertiesCodec()).apply(instance, BigButtonBlock::new));

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	private static final VoxelShape FLOOR = Block.box(1, 0, 1, 15, 2, 15);
	private static final VoxelShape CEILING = Block.box(1, 14, 1, 15, 16, 15);
	private static final VoxelShape NORTH = Block.box(1, 1, 14, 15, 15, 16);
	private static final VoxelShape SOUTH = Block.box(1, 1, 0, 15, 15, 2);
	private static final VoxelShape WEST = Block.box(14, 1, 1, 16, 15, 15);
	private static final VoxelShape EAST = Block.box(0, 1, 1, 2, 15, 15);

	private static final VoxelShape PRESSED_FLOOR = Block.box(1, 0, 1, 15, 1, 15);
	private static final VoxelShape PRESSED_CEILING = Block.box(1, 15, 1, 15, 16, 15);
	private static final VoxelShape PRESSED_NORTH = Block.box(1, 1, 15, 15, 15, 16);
	private static final VoxelShape PRESSED_SOUTH = Block.box(1, 1, 0, 15, 15, 1);
	private static final VoxelShape PRESSED_WEST = Block.box(15, 1, 1, 16, 15, 15);
	private static final VoxelShape PRESSED_EAST = Block.box(0, 1, 1, 1, 15, 15);

	private final BlockSetType type;

	public BigButtonBlock(BlockSetType type, Properties properties) {
		super(properties.sound(type.soundType()));
		this.type = type;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(FACE, AttachFace.WALL));
	}

	@Override
	protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED, FACE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getShape(state, state.getValue(POWERED));
	}

	private static VoxelShape getShape(BlockState state, boolean pressed) {
		return switch (state.getValue(FACE)) {
			case FLOOR -> pressed? PRESSED_FLOOR : FLOOR;
			case CEILING -> pressed? PRESSED_CEILING : CEILING;
			case WALL -> switch (state.getValue(FACING)) {
				case SOUTH -> pressed? PRESSED_SOUTH : SOUTH;
				case WEST -> pressed? PRESSED_WEST : WEST;
				case EAST -> pressed? PRESSED_EAST : EAST;
				default -> pressed? PRESSED_NORTH : NORTH;
			};
		};
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (player.isSecondaryUseActive()) {
			if (!level.isClientSide && level.getBlockEntity(pos) instanceof BigButtonBlockEntity button)
				((ServerPlayer)player).openMenu(button, pos);
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		if (state.getValue(POWERED)) return InteractionResult.CONSUME;
		if (!level.isClientSide) press(state, level, pos, player);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	public void press(BlockState state, Level level, BlockPos pos, @Nullable Entity cause) {
		setPowered(state, level, pos, true);
		scheduleRelease(level, pos);
		level.gameEvent(cause, GameEvent.BLOCK_ACTIVATE, pos);
	}

	private void setPowered(BlockState state, Level level, BlockPos pos, boolean powered) {
		level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
		updateNeighbours(state, level, pos);
		level.playSound(null, pos, powered? type.buttonClickOn() : type.buttonClickOff(), SoundSource.BLOCKS);
	}

	private void scheduleRelease(Level level, BlockPos pos) {
		final int ticks = level.getBlockEntity(pos) instanceof BigButtonBlockEntity button? button.getTickTime() : 1;
		level.scheduleTick(pos, this, ticks);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(POWERED)) return;

		if (hasStuckArrows(state, level, pos)) {
			// check again later
			scheduleRelease(level, pos);
		} else {
			setPowered(state, level, pos, false);
			level.gameEvent(null, GameEvent.BLOCK_DEACTIVATE, pos);
		}
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (!level.isClientSide && !state.getValue(POWERED) && hasStuckArrows(state, level, pos))
			press(state, level, pos, entity);
	}

	private boolean hasStuckArrows(BlockState state, Level level, BlockPos pos) {
		// stuck arrows end up a bit in front of the surface they hit
		return type.canButtonBeActivatedByArrows() && !level.getEntitiesOfClass(AbstractArrow.class, getShape(state, false).bounds().inflate(0.1).move(pos)).isEmpty();
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(POWERED)? 15 : 0;
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(POWERED) && getConnectedDirection(state) == direction? 15 : 0;
	}

	private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
		level.updateNeighborsAt(pos, this);
		level.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), this);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock())) {
			if (level.getBlockEntity(pos) instanceof BigButtonBlockEntity button) {
				final ItemStackHandler inventory = button.getInventory();
				for (int slot = 0; slot < inventory.getSlots(); slot++)
					Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(slot));
			}
			if (!movedByPiston && state.getValue(POWERED)) updateNeighbours(state, level, pos);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BigButtonBlockEntity(pos, state);
	}
}
