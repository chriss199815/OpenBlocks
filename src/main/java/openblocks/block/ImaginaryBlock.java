package openblocks.block;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import openblocks.OBConfig;
import openblocks.block.entity.ImaginaryBlockEntity;
import openblocks.item.ImaginaryItem;
import openblocks.item.ImaginaryPlacementMode;

/**
 * Port of legacy {@code BlockImaginary}: a block drawn with a pencil or crayon. Whether a player sees, targets or
 * stands on it depends on the glasses they wear, see {@link ImaginaryBlockEntity#is}; other entities pass through.
 * Drawn by {@code ImaginaryRenderer}, which fades it in and out; drops nothing.
 */
public class ImaginaryBlock extends BaseEntityBlock {

	public static final MapCodec<ImaginaryBlock> CODEC = simpleCodec(ImaginaryBlock::new);

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);
	public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);

	private static final double PANEL_HEIGHT = 1.6;

	public enum Type implements StringRepresentable {
		PENCIL, CRAYON;

		@Override
		public String getSerializedName() {
			return this == PENCIL? "pencil" : "crayon";
		}
	}

	/**
	 * Legacy shapes; the stairs have their upper step on the side the block faces.
	 */
	public enum Shape implements StringRepresentable {
		BLOCK("block", Shapes.block(), Shapes.block()),
		PANEL("panel", Block.box(0, 16 - PANEL_HEIGHT, 0, 16, 16, 16), Block.box(0, 16 - PANEL_HEIGHT, 0, 16, 16, 16)),
		HALF_PANEL("half_panel", Block.box(0, 8 - PANEL_HEIGHT, 0, 16, 8, 16), Block.box(0, 8 - PANEL_HEIGHT, 0, 16, 8, 16)),
		// like legacy, targeted as the upper half
		STAIRS("stairs", null, Block.box(0, 8, 0, 16, 16, 16));

		private final String name;
		private final Map<Direction, VoxelShape> collision = new EnumMap<>(Direction.class);
		private final VoxelShape outline;

		Shape(String name, @Nullable VoxelShape collision, VoxelShape outline) {
			this.name = name;
			this.outline = outline;
			for (Direction facing : Direction.Plane.HORIZONTAL)
				this.collision.put(facing, collision != null? collision : stairs(facing));
		}

		private static VoxelShape stairs(Direction facing) {
			return Shapes.or(half(facing.getOpposite(), 8 - PANEL_HEIGHT, 8), half(facing, 16 - PANEL_HEIGHT, 16));
		}

		private static VoxelShape half(Direction side, double minY, double maxY) {
			return switch (side) {
				case NORTH -> Block.box(0, minY, 0, 16, maxY, 8);
				case SOUTH -> Block.box(0, minY, 8, 16, maxY, 16);
				case WEST -> Block.box(0, minY, 0, 8, maxY, 16);
				default -> Block.box(8, minY, 0, 16, maxY, 16);
			};
		}

		public VoxelShape collision(Direction facing) {
			return collision.get(facing);
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public ImaginaryBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, Shape.BLOCK).setValue(TYPE, Type.PENCIL));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SHAPE, TYPE);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return is(ImaginaryBlockEntity.Property.SELECTABLE, level, pos, context)? state.getValue(SHAPE).outline : Shapes.empty();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return is(ImaginaryBlockEntity.Property.SOLID, level, pos, context)? state.getValue(SHAPE).collision(state.getValue(FACING)) : Shapes.empty();
	}

	private static boolean is(ImaginaryBlockEntity.Property property, BlockGetter level, BlockPos pos, CollisionContext context) {
		return context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player
				&& level.getBlockEntity(pos) instanceof ImaginaryBlockEntity imaginary && imaginary.is(property, player);
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		if (!(level.getBlockEntity(pos) instanceof ImaginaryBlockEntity imaginary)) return ItemStack.EMPTY;
		final ImaginaryPlacementMode mode = ImaginaryPlacementMode.of(state.getValue(SHAPE), imaginary.isInverted());
		return ImaginaryItem.create(imaginary.getColor(), mode, OBConfig.IMAGINARY.numberOfUses.get().floatValue());
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ImaginaryBlockEntity(pos, state);
	}
}
