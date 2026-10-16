package openblocks.common;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The orientations a building guide can have; replaces OpenModsLib {@code Orientation} with the legacy
 * {@code THREE_FOUR_DIRECTIONS} rotation mode. Each one says where the local x and y axes of the guide point; its top
 * (local y) lies along one of the three world axes and can be turned in four steps around it.
 */
public enum GuideOrientation implements StringRepresentable {
	XP_YP(Direction.EAST, Direction.UP),
	XN_YP(Direction.WEST, Direction.UP),
	ZP_YP(Direction.SOUTH, Direction.UP),
	ZN_YP(Direction.NORTH, Direction.UP),

	YP_XN(Direction.UP, Direction.WEST),
	YN_XN(Direction.DOWN, Direction.WEST),
	ZP_XN(Direction.SOUTH, Direction.WEST),
	ZN_XN(Direction.NORTH, Direction.WEST),

	XP_ZN(Direction.EAST, Direction.NORTH),
	XN_ZN(Direction.WEST, Direction.NORTH),
	YP_ZN(Direction.UP, Direction.NORTH),
	YN_ZN(Direction.DOWN, Direction.NORTH);

	public final Direction x;
	public final Direction y;
	public final Direction z;

	GuideOrientation(Direction x, Direction y) {
		this.x = x;
		this.y = y;
		this.z = Direction.fromDelta(
				x.getStepY() * y.getStepZ() - x.getStepZ() * y.getStepY(),
				x.getStepZ() * y.getStepX() - x.getStepX() * y.getStepZ(),
				x.getStepX() * y.getStepY() - x.getStepY() * y.getStepX());
	}

	/**
	 * Legacy {@code BlockPlacementMode.SURFACE}: the top points along the axis of the clicked face.
	 */
	public static GuideOrientation forClickedFace(Direction face) {
		return switch (face.getAxis()) {
			case X -> ZN_XN;
			case Z -> XP_ZN;
			case Y -> XP_YP;
		};
	}

	/**
	 * Turns around the local y axis, clockwise seen from above; legacy {@code rotateAround(POS_Y)}.
	 */
	public GuideOrientation rotateClockwise() {
		return lookup(z, y);
	}

	public GuideOrientation rotateCounterClockwise() {
		return lookup(z.getOpposite(), y);
	}

	/**
	 * Orientation with local axes pointing the given ways; if the top is not along an allowed axis, it is flipped (with x,
	 * so the orientation stays right-handed).
	 */
	public static GuideOrientation lookup(Direction x, Direction y) {
		for (GuideOrientation orientation : values())
			if (orientation.x == x && orientation.y == y) return orientation;
		for (GuideOrientation orientation : values())
			if (orientation.x == x.getOpposite() && orientation.y == y.getOpposite()) return orientation;
		throw new IllegalArgumentException(x + ", " + y);
	}

	public Direction toWorld(Direction local) {
		final Direction positive = switch (local.getAxis()) {
			case X -> x;
			case Y -> y;
			case Z -> z;
		};
		return local.getAxisDirection() == Direction.AxisDirection.POSITIVE? positive : positive.getOpposite();
	}

	public BlockPos toWorld(int localX, int localY, int localZ) {
		return new BlockPos(
				localX * x.getStepX() + localY * y.getStepX() + localZ * z.getStepX(),
				localX * x.getStepY() + localY * y.getStepY() + localZ * z.getStepY(),
				localX * x.getStepZ() + localY * y.getStepZ() + localZ * z.getStepZ());
	}

	/**
	 * Maps a point inside the block (0..1 on every axis) from world to local space.
	 */
	public Vec3 blockToLocal(Vec3 point) {
		final Vec3 centered = point.subtract(0.5, 0.5, 0.5);
		return new Vec3(dot(centered, x), dot(centered, y), dot(centered, z)).add(0.5, 0.5, 0.5);
	}

	/**
	 * Maps a box inside the block (0..1 on every axis) from local to world space.
	 */
	public AABB localToBlock(AABB box) {
		final Vec3 min = localToBlock(new Vec3(box.minX, box.minY, box.minZ));
		final Vec3 max = localToBlock(new Vec3(box.maxX, box.maxY, box.maxZ));
		return new AABB(min, max);
	}

	private Vec3 localToBlock(Vec3 point) {
		final Vec3 centered = point.subtract(0.5, 0.5, 0.5);
		return Vec3.atLowerCornerOf(x.getNormal()).scale(centered.x)
				.add(Vec3.atLowerCornerOf(y.getNormal()).scale(centered.y))
				.add(Vec3.atLowerCornerOf(z.getNormal()).scale(centered.z))
				.add(0.5, 0.5, 0.5);
	}

	private static double dot(Vec3 v, Direction d) {
		return v.x * d.getStepX() + v.y * d.getStepY() + v.z * d.getStepZ();
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
