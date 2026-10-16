package openblocks.util.shapes;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import openblocks.util.shapes.ShapeGeometry.Octant;

/**
 * Port of the OpenModsLib {@code Shape*Generator} classes.
 */
public class ShapeGenerators {

	private ShapeGenerators() {}

	/**
	 * Legacy {@code ShapeSphereGenerator}: shell of an ellipsoid, only in the given octants.
	 */
	public static ShapeGenerator sphere(List<Octant> octants) {
		return (minX, minY, minZ, maxX, maxY, maxZ, consumer) -> ShapeGeometry.makeEllipsoid(minX, minY, minZ, maxX, maxY, maxZ, consumer, octants);
	}

	/**
	 * Legacy {@code ShapeCylinderGenerator}: an ellipse in every layer.
	 */
	public static ShapeGenerator cylinder() {
		return (minX, minY, minZ, maxX, maxY, maxZ, consumer) -> ShapeGeometry.makeEllipse(minX, minZ, maxX, maxZ, 0, column(minY, maxY, consumer));
	}

	/**
	 * Legacy {@code ShapeCuboidGenerator}: corners and edges of the box, with {@code walls} also its faces.
	 */
	public static ShapeGenerator cuboid(boolean walls) {
		return (minX, minY, minZ, maxX, maxY, maxZ, consumer) -> {
			final int dx = maxX - minX - 2;
			final int dy = maxY - minY - 2;
			final int dz = maxZ - minZ - 2;

			consumer.accept(maxX, maxY, maxZ);
			consumer.accept(maxX, maxY, minZ);
			consumer.accept(maxX, minY, maxZ);
			consumer.accept(maxX, minY, minZ);
			consumer.accept(minX, maxY, maxZ);
			consumer.accept(minX, maxY, minZ);
			consumer.accept(minX, minY, maxZ);
			consumer.accept(minX, minY, minZ);

			ShapeGeometry.makeLine(minX, minY + 1, minZ, Direction.UP, dy, consumer);
			ShapeGeometry.makeLine(minX, minY + 1, maxZ, Direction.UP, dy, consumer);
			ShapeGeometry.makeLine(maxX, minY + 1, maxZ, Direction.UP, dy, consumer);
			ShapeGeometry.makeLine(maxX, minY + 1, minZ, Direction.UP, dy, consumer);

			ShapeGeometry.makeLine(minX + 1, minY, minZ, Direction.EAST, dx, consumer);
			ShapeGeometry.makeLine(minX + 1, minY, maxZ, Direction.EAST, dx, consumer);
			ShapeGeometry.makeLine(minX + 1, maxY, maxZ, Direction.EAST, dx, consumer);
			ShapeGeometry.makeLine(minX + 1, maxY, minZ, Direction.EAST, dx, consumer);

			ShapeGeometry.makeLine(minX, minY, minZ + 1, Direction.SOUTH, dz, consumer);
			ShapeGeometry.makeLine(minX, maxY, minZ + 1, Direction.SOUTH, dz, consumer);
			ShapeGeometry.makeLine(maxX, maxY, minZ + 1, Direction.SOUTH, dz, consumer);
			ShapeGeometry.makeLine(maxX, minY, minZ + 1, Direction.SOUTH, dz, consumer);

			if (walls) {
				ShapeGeometry.makePlane(minX + 1, minY + 1, minZ, dx, dy, Direction.EAST, Direction.UP, consumer);
				ShapeGeometry.makePlane(minX + 1, minY + 1, maxZ, dx, dy, Direction.EAST, Direction.UP, consumer);

				ShapeGeometry.makePlane(minX + 1, minY, minZ + 1, dx, dz, Direction.EAST, Direction.SOUTH, consumer);
				ShapeGeometry.makePlane(minX + 1, maxY, minZ + 1, dx, dz, Direction.EAST, Direction.SOUTH, consumer);

				ShapeGeometry.makePlane(minX, minY + 1, minZ + 1, dy, dz, Direction.UP, Direction.SOUTH, consumer);
				ShapeGeometry.makePlane(maxX, minY + 1, minZ + 1, dy, dz, Direction.UP, Direction.SOUTH, consumer);
			}
		};
	}

	/**
	 * Legacy {@code ShapeAxesGenerator}: three lines through 0.
	 */
	public static ShapeGenerator axes() {
		return (minX, minY, minZ, maxX, maxY, maxZ, consumer) -> {
			for (int x = minX; x <= maxX; x++)
				consumer.accept(x, 0, 0);
			for (int y = minY; y <= maxY; y++)
				consumer.accept(0, y, 0);
			for (int z = minZ; z <= maxZ; z++)
				consumer.accept(0, 0, z);
		};
	}

	/**
	 * Legacy {@code ShapePlanesGenerator}: three planes through 0.
	 */
	public static ShapeGenerator planes() {
		return (minX, minY, minZ, maxX, maxY, maxZ, consumer) -> {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++)
					consumer.accept(0, y, z);
				for (int x = minX; x <= maxX; x++)
					consumer.accept(x, y, 0);
			}

			for (int x = minX; x <= maxX; x++)
				for (int z = minZ; z <= maxZ; z++)
					consumer.accept(x, 0, z);
		};
	}

	/**
	 * Legacy {@code ShapeEquilateral2dGenerator}: a regular polygon in every layer. The corners are computed for one
	 * symmetric part of the polygon and the edges mirrored.
	 */
	public static ShapeGenerator polygon(int sides) {
		final Symmetry symmetry = sides % 4 == 0? Symmetry.EIGHT_FOLD : sides % 2 == 0? Symmetry.FOUR_FOLD : Symmetry.TWO_FOLD;

		final List<double[]> angles = new ArrayList<>();
		for (int i = 0; i < sides; i++) {
			final double angle = 2 * Math.PI * i / sides;
			if (angle > symmetry.angleLimit) break;
			angles.add(new double[] { Math.cos(angle), Math.sin(angle) });
		}

		return (minX, minY, minZ, maxX, maxY, maxZ, consumer) -> {
			final double middleX = (maxX + minX) / 2.0;
			final double radiusX = (maxX - minX) / 2.0;
			final double middleZ = (maxZ + minZ) / 2.0;
			final double radiusZ = (maxZ - minZ) / 2.0;

			final ShapeConsumer mirrored = symmetry.mirror(column(minY, maxY, consumer));

			int prevX = (int)Math.round(middleX + radiusX * angles.get(0)[0]);
			int prevZ = (int)Math.round(middleZ + radiusZ * angles.get(0)[1]);
			for (int i = 1; i < angles.size(); i++) {
				final int x = (int)Math.round(middleX + radiusX * angles.get(i)[0]);
				final int z = (int)Math.round(middleZ + radiusZ * angles.get(i)[1]);
				ShapeGeometry.line2D(0, prevX, prevZ, x, z, mirrored);
				prevX = x;
				prevZ = z;
			}

			final int[] last = symmetry.mirrorLastPoint(prevX, prevZ);
			ShapeGeometry.line2D(0, prevX, prevZ, last[0], last[1], mirrored);
		};
	}

	private enum Symmetry {
		TWO_FOLD(Math.PI) {
			@Override
			ShapeConsumer mirror(ShapeConsumer consumer) {
				return (x, y, z) -> {
					if (z >= 0) {
						consumer.accept(x, y, +z);
						consumer.accept(x, y, -z);
					}
				};
			}

			@Override
			int[] mirrorLastPoint(int x, int z) {
				return new int[] { x, -z };
			}
		},
		FOUR_FOLD(Math.PI / 2) {
			@Override
			ShapeConsumer mirror(ShapeConsumer consumer) {
				return (x, y, z) -> {
					if (x >= 0 && z >= 0) {
						consumer.accept(+x, y, -z);
						consumer.accept(-x, y, -z);
						consumer.accept(-x, y, +z);
						consumer.accept(+x, y, +z);
					}
				};
			}

			@Override
			int[] mirrorLastPoint(int x, int z) {
				return new int[] { -x, z };
			}
		},
		EIGHT_FOLD(Math.PI / 4) {
			@Override
			ShapeConsumer mirror(ShapeConsumer consumer) {
				return (x, y, z) -> {
					if (x >= z) {
						consumer.accept(+x, y, -z);
						consumer.accept(-x, y, -z);
						consumer.accept(-x, y, +z);
						consumer.accept(+x, y, +z);

						consumer.accept(+z, y, -x);
						consumer.accept(-z, y, -x);
						consumer.accept(-z, y, +x);
						consumer.accept(+z, y, +x);
					}
				};
			}

			@Override
			int[] mirrorLastPoint(int x, int z) {
				return new int[] { z, x };
			}
		};

		final double angleLimit;

		Symmetry(double angleLimit) {
			this.angleLimit = angleLimit;
		}

		abstract ShapeConsumer mirror(ShapeConsumer consumer);

		abstract int[] mirrorLastPoint(int x, int z);
	}

	// repeats a flat shape (at y 0) in every layer
	private static ShapeConsumer column(int minY, int maxY, ShapeConsumer consumer) {
		return (x, ignored, z) -> {
			for (int y = minY; y <= maxY; y++)
				consumer.accept(x, y, z);
		};
	}
}
