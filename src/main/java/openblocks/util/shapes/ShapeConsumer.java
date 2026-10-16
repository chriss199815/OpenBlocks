package openblocks.util.shapes;

/**
 * Port of OpenModsLib {@code IShapeable}: receives the blocks of a generated shape.
 */
@FunctionalInterface
public interface ShapeConsumer {
	void accept(int x, int y, int z);
}
