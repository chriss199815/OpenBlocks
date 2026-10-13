package openblocks.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.neoforge.fluids.FluidStack;
import openblocks.block.entity.TankBlockEntity;

/**
 * The twelve frame edges of a tank. Replaces the legacy {@code tank.json} variant expansions: an edge is hidden where connected tanks continue the
 * surface, so a group of tanks looks like one big tank.
 */
public enum TankEdge {
	BS(Direction.DOWN, Direction.SOUTH, true),
	BN(Direction.DOWN, Direction.NORTH, true),
	TS(Direction.UP, Direction.SOUTH, false),
	TN(Direction.UP, Direction.NORTH, false),
	NE(Direction.NORTH, Direction.EAST, true),
	NW(Direction.NORTH, Direction.WEST, true),
	SE(Direction.SOUTH, Direction.EAST, false),
	SW(Direction.SOUTH, Direction.WEST, false),
	BE(Direction.DOWN, Direction.EAST, true),
	BW(Direction.DOWN, Direction.WEST, true),
	TE(Direction.UP, Direction.EAST, false),
	TW(Direction.UP, Direction.WEST, false);

	public static final int ALL = (1 << values().length) - 1;

	private static final float THICKNESS = 0.1f;

	public final Direction first;
	public final Direction second;

	/**
	 * Edges are shared with the diagonal neighbour, which sees the same edge mirrored (e.g. our BS is its TN).
	 * One side of each pair also hides the edge when only the diagonal neighbour is present, so it isn't drawn twice.
	 */
	private final boolean yieldsToDiagonal;

	TankEdge(Direction first, Direction second, boolean yieldsToDiagonal) {
		this.first = first;
		this.second = second;
		this.yieldsToDiagonal = yieldsToDiagonal;
	}

	public String getName() {
		return name().toLowerCase();
	}

	public Direction.Axis getAxis() {
		for (Direction.Axis axis : Direction.Axis.values())
			if (axis != first.getAxis() && axis != second.getAxis()) return axis;
		throw new AssertionError();
	}

	private boolean isVisible(boolean hasFirst, boolean hasSecond, boolean hasDiagonal) {
		if (yieldsToDiagonal) return hasFirst == hasSecond && !hasDiagonal;
		return (!hasFirst && !hasSecond) || (hasFirst && hasSecond && !hasDiagonal);
	}

	/**
	 * @return model coordinates (0-16) of the edge box, as {@code [fromX, fromY, fromZ, toX, toY, toZ]}
	 */
	public float[] getBounds() {
		final float[] result = new float[6];
		for (Direction.Axis axis : Direction.Axis.values()) {
			final int i = axis.ordinal();
			final Direction side = first.getAxis() == axis? first : second.getAxis() == axis? second : null;
			if (side == null) {
				result[i] = -THICKNESS;
				result[i + 3] = 16 + THICKNESS;
			} else {
				final float center = side.getAxisDirection() == Direction.AxisDirection.POSITIVE? 16 : 0;
				result[i] = center - THICKNESS;
				result[i + 3] = center + THICKNESS;
			}
		}
		return result;
	}

	public static int getVisibleEdges(BlockGetter level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof TankBlockEntity tank)) return ALL;
		final FluidStack fluid = tank.getFluid();

		int result = 0;
		for (TankEdge edge : values()) {
			final BlockPos firstPos = pos.relative(edge.first);
			final boolean visible = edge.isVisible(
					isConnected(level, firstPos, fluid),
					isConnected(level, pos.relative(edge.second), fluid),
					isConnected(level, firstPos.relative(edge.second), fluid));
			if (visible) result |= 1 << edge.ordinal();
		}
		return result;
	}

	private static boolean isConnected(BlockGetter level, BlockPos pos, FluidStack fluid) {
		return level.getBlockEntity(pos) instanceof TankBlockEntity neighbour && neighbour.accepts(fluid);
	}
}
