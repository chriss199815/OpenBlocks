package openblocks.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Port of legacy {@code SyncableBlockLayers}: paint on one side of a canvas. A background color (0 for none), stencil shaped layers of paint,
 * and the stencil lying on the side, if any. Painting with a stencil adds a layer, painting without one replaces everything.
 */
public record CanvasSide(int background, List<Layer> layers, Optional<Cover> cover) {

	public static final CanvasSide EMPTY = new CanvasSide(0, List.of(), Optional.empty());

	private static final Codec<Integer> ROTATION = Codec.intRange(0, 3);

	public record Layer(StencilPattern pattern, int rotation, int color) {
		public static final Codec<Layer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				StencilPattern.CODEC.fieldOf("stencil").forGetter(Layer::pattern),
				ROTATION.fieldOf("rotation").forGetter(Layer::rotation),
				Codec.INT.fieldOf("color").forGetter(Layer::color)
		).apply(instance, Layer::new));
	}

	public record Cover(StencilPattern pattern, int rotation) {
		public static final Codec<Cover> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				StencilPattern.CODEC.fieldOf("stencil").forGetter(Cover::pattern),
				ROTATION.fieldOf("rotation").forGetter(Cover::rotation)
		).apply(instance, Cover::new));
	}

	public static final Codec<CanvasSide> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("background", 0).forGetter(CanvasSide::background),
			Layer.CODEC.listOf().optionalFieldOf("layers", List.of()).forGetter(CanvasSide::layers),
			Cover.CODEC.optionalFieldOf("cover").forGetter(CanvasSide::cover)
	).apply(instance, CanvasSide::new));

	public boolean hasPaint() {
		return background != 0 || !layers.isEmpty();
	}

	public boolean isEmpty() {
		return !hasPaint() && cover.isEmpty();
	}

	/**
	 * @param color 24 bit RGB
	 */
	public CanvasSide paint(int color) {
		final int solid = 0xFF000000 | color;
		if (cover.isEmpty()) return new CanvasSide(solid, List.of(), cover);

		final Cover c = cover.get();
		final List<Layer> newLayers = new ArrayList<>(layers);
		// painting over the last layer again only changes its color, like legacy
		if (!newLayers.isEmpty() && newLayers.getLast().pattern() == c.pattern() && newLayers.getLast().rotation() == c.rotation())
			newLayers.removeLast();
		newLayers.add(new Layer(c.pattern(), c.rotation(), solid));
		return new CanvasSide(background, List.copyOf(newLayers), cover);
	}

	@Nullable
	public CanvasSide putStencil(StencilPattern pattern) {
		if (cover.isPresent()) return null;
		return new CanvasSide(background, layers, Optional.of(new Cover(pattern, 0)));
	}

	@Nullable
	public CanvasSide rotateStencil() {
		return cover.map(c -> new CanvasSide(background, layers, Optional.of(new Cover(c.pattern(), (c.rotation() + 1) % 4)))).orElse(null);
	}

	public CanvasSide removeStencil() {
		return new CanvasSide(background, layers, Optional.empty());
	}

	/**
	 * Whether the pixel at (x, y) is cut out when the stencil is turned clockwise by {@code rotation} quarter turns.
	 */
	public static boolean isCutOut(StencilPattern pattern, int rotation, int x, int y) {
		final int max = StencilPattern.SIZE - 1;
		return switch (rotation) {
			case 1 -> pattern.isCutOut(y, max - x);
			case 2 -> pattern.isCutOut(max - x, max - y);
			case 3 -> pattern.isCutOut(max - y, x);
			default -> pattern.isCutOut(x, y);
		};
	}

	/**
	 * ARGB color of the paint at (x, y); 0 where there is none.
	 */
	public int paintAt(int x, int y) {
		int color = background;
		for (Layer layer : layers)
			if (isCutOut(layer.pattern(), layer.rotation(), x, y)) color = layer.color();
		return color;
	}
}
