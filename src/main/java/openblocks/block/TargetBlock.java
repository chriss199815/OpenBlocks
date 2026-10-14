package openblocks.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import openblocks.registry.OBSounds;

/**
 * Port of legacy {@code BlockTarget}: folded flat on the ground, a redstone signal makes it stand up (dropping arrows
 * stuck in it). Arrows hitting a standing target emit a signal for 10 ticks, the stronger the closer to the bullseye.
 * <p>
 * {@link #FACING} is the side the bullseye faces. The signal is kept in {@link #POWER} instead of a block entity, like the
 * vanilla target block.
 */
public class TargetBlock extends Block {

	public static final MapCodec<TargetBlock> CODEC = simpleCodec(TargetBlock::new);

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	/** Standing up, because the block is powered. */
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final IntegerProperty POWER = BlockStateProperties.POWER;

	public static final int SIGNAL_TICKS = 10;

	private static final VoxelShape FOLDED = Block.box(0, 0, 0, 16, 2, 16);
	private static final VoxelShape LEGS = Block.box(0, 0, 0, 16, 1, 16);
	private static final VoxelShape DEPLOYED_NORTH = Shapes.or(LEGS, Block.box(0, 0, 0, 16, 16, 1.6));
	private static final VoxelShape DEPLOYED_SOUTH = Shapes.or(LEGS, Block.box(0, 0, 14.4, 16, 16, 16));
	private static final VoxelShape DEPLOYED_WEST = Shapes.or(LEGS, Block.box(0, 0, 0, 1.6, 16, 16));
	private static final VoxelShape DEPLOYED_EAST = Shapes.or(LEGS, Block.box(14.4, 0, 0, 16, 16, 16));

	public TargetBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(POWER, 0));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED, POWER);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState()
				.setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (!state.getValue(POWERED)) return FOLDED;
		return switch (state.getValue(FACING)) {
			case SOUTH -> DEPLOYED_SOUTH;
			case WEST -> DEPLOYED_WEST;
			case EAST -> DEPLOYED_EAST;
			default -> DEPLOYED_NORTH;
		};
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
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
		if (level.isClientSide) return;

		final boolean powered = level.hasNeighborSignal(pos);
		if (powered == state.getValue(POWERED)) return;

		dropStuckArrows(level, pos);
		level.playSound(null, pos, powered? OBSounds.TARGET_OPEN.get() : OBSounds.TARGET_CLOSE.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
		level.setBlock(pos, state.setValue(POWERED, powered).setValue(POWER, 0), Block.UPDATE_ALL);
		if (state.getValue(POWER) > 0) updateStrongNeighbours(level, pos);
	}

	/**
	 * Like legacy, arrows in the target are handed out as if a player picked them up.
	 */
	private static void dropStuckArrows(Level level, BlockPos pos) {
		for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class, new AABB(pos).inflate(0.2))) {
			switch (arrow.pickup) {
				case ALLOWED -> {
					Block.popResource(level, pos, arrow.getPickupItemStackOrigin());
					arrow.discard();
				}
				case CREATIVE_ONLY -> arrow.discard();
				case DISALLOWED -> {}
			}
		}
	}

	@Override
	protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
		if (level.isClientSide || !state.getValue(POWERED) || !(projectile instanceof AbstractArrow)) return;

		final BlockPos pos = hit.getBlockPos();
		final Direction facing = state.getValue(FACING);
		final Vec3 bullseye = new Vec3(pos.getX() + 0.5 + facing.getStepX() * 0.5, pos.getY() + 0.55, pos.getZ() + 0.5 + facing.getStepZ() * 0.5);
		final double distance = hit.getLocation().distanceTo(bullseye);
		final int power = 15 - Math.min(15, (int)(distance * 32));

		level.setBlock(pos, state.setValue(POWER, power), Block.UPDATE_ALL);
		updateStrongNeighbours(level, pos);
		level.scheduleTick(pos, this, SIGNAL_TICKS);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(POWER) == 0) return;
		level.setBlock(pos, state.setValue(POWER, 0), Block.UPDATE_ALL);
		updateStrongNeighbours(level, pos);
	}

	// the signal is strong on all sides, so blocks around the neighbours have to know too
	private void updateStrongNeighbours(Level level, BlockPos pos) {
		for (Direction direction : Direction.values())
			level.updateNeighborsAt(pos.relative(direction), this);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(POWER);
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(POWER);
	}
}
