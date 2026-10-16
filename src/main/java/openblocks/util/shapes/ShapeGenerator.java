package openblocks.util.shapes;

/**
 * Port of OpenModsLib {@code IShapeGenerator}: generates a shape that fills the box from min to max (both inclusive).
 */
@FunctionalInterface
public interface ShapeGenerator {
	void generate(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, ShapeConsumer consumer);
}
