package openblocks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.MapColor;
import openblocks.common.HeightMapData;

/**
 * Port of legacy {@code HeightMapRenderer}: draws a height map in a 1×1 square, each pixel as a flat square at its
 * height (1/64 per block, so y 0 is at 0 and the build limit about 5 above). Legacy drew a texture per height level;
 * here neighbouring pixels of the same row, height and color are merged into one quad. The quads are rebuilt whenever
 * the client receives new data for the map.
 */
public class HeightMapRenderer {

	public static final RenderType RENDER_TYPE = RenderType.debugQuads();

	private static final float PIXEL = 1.0f / HeightMapData.SIZE;
	private static final float BLOCK_HEIGHT = 1.0f / 64;

	// x0, z0, x1, z1, y, color per quad
	private record Mesh(float[] quads, int[] colors) {}

	private static final Map<HeightMapData, Mesh> meshes = new WeakHashMap<>();

	public static void render(HeightMapData data, PoseStack.Pose pose, VertexConsumer consumer) {
		final Mesh mesh = meshes.computeIfAbsent(data, HeightMapRenderer::buildMesh);
		final float[] quads = mesh.quads();
		for (int i = 0; i < mesh.colors().length; i++) {
			final int base = i * 5;
			final float x0 = quads[base];
			final float z0 = quads[base + 1];
			final float x1 = quads[base + 2];
			final float z1 = quads[base + 3];
			final float y = quads[base + 4];
			final int color = mesh.colors()[i];
			consumer.addVertex(pose, x0, y, z0).setColor(color);
			consumer.addVertex(pose, x0, y, z1).setColor(color);
			consumer.addVertex(pose, x1, y, z1).setColor(color);
			consumer.addVertex(pose, x1, y, z0).setColor(color);
		}
	}

	private static Mesh buildMesh(HeightMapData data) {
		final FloatArrayList quads = new FloatArrayList();
		final IntArrayList colors = new IntArrayList();

		for (HeightMapData.Layer layer : data.getLayers())
			for (int row = 0; row < HeightMapData.SIZE; row++) {
				int column = 0;
				while (column < HeightMapData.SIZE) {
					final int index = row * HeightMapData.SIZE + column;
					final int colorId = layer.colors()[index] & 0xFF;
					final int height = layer.heights()[index];
					int end = column + 1;
					while (end < HeightMapData.SIZE && (layer.colors()[index + end - column] & 0xFF) == colorId && layer.heights()[index + end - column] == height)
						end++;

					if (colorId != 0) {
						quads.addElements(quads.size(), new float[] { column * PIXEL, row * PIXEL, end * PIXEL, (row + 1) * PIXEL, height * BLOCK_HEIGHT });
						colors.add(layer.alpha() << 24 | MapColor.byId(colorId).col);
					}
					column = end;
				}
			}

		return new Mesh(quads.toFloatArray(), colors.toIntArray());
	}
}
