package openblocks.util.shapes;

import java.util.List;
import net.minecraft.core.Direction;

/**
 * Port of the shape parts of OpenModsLib {@code GeometryUtils}: lines, planes, ellipses and ellipsoids made of blocks.
 * Shapes may hit a block more than once, consumers that care should deduplicate.
 */
public class ShapeGeometry {

	/**
	 * Signs of one eighth of an ellipsoid; replaces OpenModsLib {@code Octant}.
	 */
	public record Octant(int x, int y, int z) {
		public static final List<Octant> ALL = List.of(
				new Octant(-1, 1, 1), new Octant(1, 1, -1), new Octant(-1, 1, -1), new Octant(1, 1, 1),
				new Octant(-1, -1, 1), new Octant(1, -1, -1), new Octant(-1, -1, -1), new Octant(1, -1, 1));

		public static final List<Octant> SOUTH = ALL.stream().filter(o -> o.z > 0).toList();
	}

	private ShapeGeometry() {}

	/**
	 * Line of {@code length + 1} blocks from the start towards {@code direction}.
	 */
	public static void makeLine(int startX, int startY, int startZ, Direction direction, int length, ShapeConsumer consumer) {
		if (length < 0) return;
		for (int offset = 0; offset <= length; offset++)
			consumer.accept(startX + offset * direction.getStepX(), startY + offset * direction.getStepY(), startZ + offset * direction.getStepZ());
	}

	/**
	 * Plane of {@code (width + 1) × (height + 1)} blocks from the start towards {@code right} and {@code up}.
	 */
	public static void makePlane(int startX, int startY, int startZ, int width, int height, Direction right, Direction up, ShapeConsumer consumer) {
		if (width < 0 || height < 0) return;
		for (int h = 0; h <= height; h++)
			makeLine(startX + h * up.getStepX(), startY + h * up.getStepY(), startZ + h * up.getStepZ(), right, width, consumer);
	}

	/**
	 * Shell of an ellipsoid around 0, only in the given octants.
	 */
	public static void makeEllipsoid(int radiusX, int radiusY, int radiusZ, ShapeConsumer consumer, List<Octant> octants) {
		final double invRadiusX = 1.0 / (radiusX + 0.5);
		final double invRadiusY = 1.0 / (radiusY + 0.5);
		final double invRadiusZ = 1.0 / (radiusZ + 0.5);

		double nextXn = 0;
		forX: for (int x = 0; x <= radiusX; ++x) {
			final double xn = nextXn;
			nextXn += invRadiusX;
			double nextYn = 0;
			forY: for (int y = 0; y <= radiusY; ++y) {
				final double yn = nextYn;
				nextYn += invRadiusY;
				double nextZn = 0;
				for (int z = 0; z <= radiusZ; ++z) {
					final double zn = nextZn;
					nextZn += invRadiusZ;

					if (lengthSq(xn, yn, zn) > 1) {
						if (z == 0) {
							if (y == 0) break forX;
							break forY;
						}
						break;
					}

					// inside, not on the shell
					if (lengthSq(nextXn, yn, zn) <= 1 && lengthSq(xn, nextYn, zn) <= 1 && lengthSq(xn, yn, nextZn) <= 1) continue;

					for (Octant octant : octants)
						consumer.accept(x * octant.x(), y * octant.y(), z * octant.z());
				}
			}
		}
	}

	/**
	 * Shell of an ellipsoid filling the box. Boxes with an even number of blocks along an axis get the middle cut out,
	 * like legacy.
	 */
	public static void makeEllipsoid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, ShapeConsumer consumer, List<Octant> octants) {
		final int centerX = (minX + maxX) / 2;
		final int centerY = (minY + maxY) / 2;
		final int centerZ = (minZ + maxZ) / 2;
		ShapeConsumer shifted = (x, y, z) -> consumer.accept(centerX + x, centerY + y, centerZ + z);

		final int diffX = maxX - minX;
		final int diffY = maxY - minY;
		final int diffZ = maxZ - minZ;
		if ((diffX & 1) != 0) shifted = skipMiddleX(shifted);
		if ((diffY & 1) != 0) shifted = skipMiddleY(shifted);
		if ((diffZ & 1) != 0) shifted = skipMiddleZ(shifted);

		makeEllipsoid(radius(diffX), radius(diffY), radius(diffZ), shifted, octants);
	}

	/**
	 * Outline of an ellipse around 0 at height {@code y}, in all four quadrants.
	 */
	public static void makeEllipse(int radiusX, int radiusZ, int y, ShapeConsumer consumer) {
		final double invRadiusX = 1.0 / (radiusX + 0.5);
		final double invRadiusZ = 1.0 / (radiusZ + 0.5);

		double nextXn = 0;
		forX: for (int x = 0; x <= radiusX; ++x) {
			final double xn = nextXn;
			nextXn += invRadiusX;
			double nextZn = 0;
			for (int z = 0; z <= radiusZ; ++z) {
				final double zn = nextZn;
				nextZn += invRadiusZ;

				if (lengthSq(xn, 0, zn) > 1) {
					if (z == 0) break forX;
					break;
				}

				if (lengthSq(nextXn, 0, zn) <= 1 && lengthSq(xn, 0, nextZn) <= 1) continue;

				consumer.accept(-x, y, z);
				consumer.accept(x, y, -z);
				consumer.accept(-x, y, -z);
				consumer.accept(x, y, z);
			}
		}
	}

	/**
	 * Outline of an ellipse filling the rectangle at height {@code y}; even sizes get the middle cut out, like legacy.
	 */
	public static void makeEllipse(int minX, int minZ, int maxX, int maxZ, int y, ShapeConsumer consumer) {
		final int centerX = (minX + maxX) / 2;
		final int centerZ = (minZ + maxZ) / 2;
		ShapeConsumer shifted = (x, y1, z) -> consumer.accept(centerX + x, y1, centerZ + z);

		final int diffX = maxX - minX;
		final int diffZ = maxZ - minZ;
		if ((diffX & 1) != 0) shifted = skipMiddleX(shifted);
		if ((diffZ & 1) != 0) shifted = skipMiddleZ(shifted);

		makeEllipse(radius(diffX), radius(diffZ), y, shifted);
	}

	/**
	 * Bresenham line from (x0, z0) to (x1, z1) at height {@code y}.
	 */
	public static void line2D(int y, int x0, int z0, int x1, int z1, ShapeConsumer consumer) {
		final int dx = Math.abs(x1 - x0);
		final int sx = x0 < x1? 1 : -1;
		final int dz = -Math.abs(z1 - z0);
		final int sz = z0 < z1? 1 : -1;
		int err = dx + dz;

		while (true) {
			consumer.accept(x0, y, z0);
			if (x0 == x1 && z0 == z1) break;
			final int e2 = 2 * err;
			if (e2 >= dz) {
				err += dz;
				x0 += sx;
			}
			if (e2 <= dx) {
				err += dx;
				z0 += sz;
			}
		}
	}

	/**
	 * 3D Bresenham line, both ends included.
	 */
	public static void line3D(int startX, int startY, int startZ, int endX, int endY, int endZ, ShapeConsumer consumer) {
		final int ax = Math.abs(endX - startX) << 1;
		final int ay = Math.abs(endY - startY) << 1;
		final int az = Math.abs(endZ - startZ) << 1;

		final int signX = Integer.signum(endX - startX);
		final int signY = Integer.signum(endY - startY);
		final int signZ = Integer.signum(endZ - startZ);

		int x = startX;
		int y = startY;
		int z = startZ;

		if (ax >= Math.max(ay, az)) {
			int deltaY = ay - (ax >> 1);
			int deltaZ = az - (ax >> 1);
			while (true) {
				consumer.accept(x, y, z);
				if (x == endX) return;
				if (deltaY >= 0) {
					y += signY;
					deltaY -= ax;
				}
				if (deltaZ >= 0) {
					z += signZ;
					deltaZ -= ax;
				}
				x += signX;
				deltaY += ay;
				deltaZ += az;
			}
		} else if (ay >= Math.max(ax, az)) {
			int deltaX = ax - (ay >> 1);
			int deltaZ = az - (ay >> 1);
			while (true) {
				consumer.accept(x, y, z);
				if (y == endY) return;
				if (deltaX >= 0) {
					x += signX;
					deltaX -= ay;
				}
				if (deltaZ >= 0) {
					z += signZ;
					deltaZ -= ay;
				}
				y += signY;
				deltaX += ax;
				deltaZ += az;
			}
		} else {
			int deltaX = ax - (az >> 1);
			int deltaY = ay - (az >> 1);
			while (true) {
				consumer.accept(x, y, z);
				if (z == endZ) return;
				if (deltaX >= 0) {
					x += signX;
					deltaX -= az;
				}
				if (deltaY >= 0) {
					y += signY;
					deltaY -= az;
				}
				z += signZ;
				deltaX += ax;
				deltaY += ay;
			}
		}
	}

	private static int radius(int diff) {
		return (diff & 1) == 0? diff / 2 : diff / 2 + 1;
	}

	private static double lengthSq(double x, double y, double z) {
		return x * x + y * y + z * z;
	}

	private static ShapeConsumer skipMiddleX(ShapeConsumer consumer) {
		return (x, y, z) -> {
			if (x < 0) consumer.accept(x + 1, y, z);
			else if (x > 0) consumer.accept(x, y, z);
		};
	}

	private static ShapeConsumer skipMiddleY(ShapeConsumer consumer) {
		return (x, y, z) -> {
			if (y < 0) consumer.accept(x, y + 1, z);
			else if (y > 0) consumer.accept(x, y, z);
		};
	}

	private static ShapeConsumer skipMiddleZ(ShapeConsumer consumer) {
		return (x, y, z) -> {
			if (z < 0) consumer.accept(x, y, z + 1);
			else if (z > 0) consumer.accept(x, y, z);
		};
	}
}
