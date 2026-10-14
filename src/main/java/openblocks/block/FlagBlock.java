package openblocks.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import openblocks.item.FlagItem;

/**
 * Port of legacy {@code BlockFlag}, one block per color. {@link #FACING} points away from the supporting block: up for flags on the ground,
 * which can turn in 10° steps ({@link #ROTATION}), or horizontal for flags on walls.
 */
public class FlagBlock extends Block {

	public static final MapCodec<FlagBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			DyeColor.CODEC.fieldOf("color").forGetter(FlagBlock::getColor),
			propertiesCodec()
	).apply(instance, FlagBlock::new));

	public static final DirectionProperty FACING = DirectionProperty.create("facing", direction -> direction != Direction.DOWN);
	public static final int ROTATIONS = 36;
	public static final int ROTATION_STEP = 360 / ROTATIONS;
	public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, ROTATIONS - 1);

	private static final VoxelShape GROUND_SHAPE = box(7, 0, 7, 9, 16, 9);
	private static final VoxelShape NORTH_SOUTH_SHAPE = box(7, 0, 3, 9, 16, 13);
	private static final VoxelShape EAST_WEST_SHAPE = box(3, 0, 7, 13, 16, 9);

	private final DyeColor color;

	public FlagBlock(DyeColor color, Properties properties) {
		super(properties);
		this.color = color;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(ROTATION, 0));
	}

	public DyeColor getColor() {
		return color;
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	public String getDescriptionId() {
		// all colors share one name, as in the legacy mod
		return "block.openblocks.flag";
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ROTATION);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		final Direction facing = context.getClickedFace();
		if (facing == Direction.DOWN) return null;

		// the flag points where the player looks
		final int rotation = Mth.floor(context.getRotation() / ROTATION_STEP);
		final BlockState state = defaultBlockState().setValue(FACING, facing).setValue(ROTATION, Math.floorMod(rotation, ROTATIONS));
		return state.canSurvive(context.getLevel(), context.getClickedPos())? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		final Direction facing = state.getValue(FACING);
		final BlockPos supportPos = pos.relative(facing.getOpposite());
		final BlockState support = level.getBlockState(supportPos);
		if (facing != Direction.UP) return support.isFaceSturdy(level, supportPos, facing);
		// fences and walls hold flags, and flags on the ground can be stacked
		return canSupportCenter(level, supportPos, Direction.UP) || support.getBlock() instanceof FlagBlock && support.getValue(FACING) == Direction.UP;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case NORTH, SOUTH -> NORTH_SOUTH_SHAPE;
			case EAST, WEST -> EAST_WEST_SHAPE;
			default -> GROUND_SHAPE;
		};
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		// so flags can be stacked
		return stack.getItem() instanceof FlagItem? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (state.getValue(FACING) != Direction.UP) return InteractionResult.PASS;

		if (!level.isClientSide) {
			final int step = player.isShiftKeyDown()? -1 : 1;
			level.setBlock(pos, state.setValue(ROTATION, Math.floorMod(state.getValue(ROTATION) + step, ROTATIONS)), Block.UPDATE_ALL);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}
