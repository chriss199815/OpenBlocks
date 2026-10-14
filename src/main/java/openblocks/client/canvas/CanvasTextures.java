package openblocks.client.canvas;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import openblocks.OpenBlocks;
import openblocks.common.CanvasSide;
import openblocks.common.StencilPattern;

/**
 * Replaces the legacy {@code CanvasTextureManager} and its fixed texture pool: one 16x16 texture per distinct paint, kept while used,
 * and one per stencil and rotation for the stencils lying on a canvas.
 */
public final class CanvasTextures {

	private record Paint(int background, List<CanvasSide.Layer> layers) {}

	private record Cover(StencilPattern pattern, int rotation) {}

	private static final ResourceLocation STENCIL = OpenBlocks.location("textures/item/stencil.png");

	private static final AtomicInteger NEXT_ID = new AtomicInteger();

	private static final LoadingCache<Paint, ResourceLocation> PAINT = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.MINUTES)
			.removalListener((RemovalNotification<Paint, ResourceLocation> notification) -> Minecraft.getInstance().getTextureManager().release(notification.getValue()))
			.build(CacheLoader.from(paint -> upload(new CanvasSide(paint.background(), paint.layers(), Optional.empty())::paintAt)));

	private static final LoadingCache<Cover, ResourceLocation> COVERS = CacheBuilder.newBuilder()
			.build(CacheLoader.from(cover -> {
				final int[] stencil = readStencilTexture();
				return upload((x, y) -> CanvasSide.isCutOut(cover.pattern(), cover.rotation(), x, y)? 0 : stencil[y * StencilPattern.SIZE + x]);
			}));

	private CanvasTextures() {}

	public static ResourceLocation paint(CanvasSide side) {
		return PAINT.getUnchecked(new Paint(side.background(), side.layers()));
	}

	public static ResourceLocation cover(CanvasSide.Cover cover) {
		return COVERS.getUnchecked(new Cover(cover.pattern(), cover.rotation()));
	}

	/**
	 * Drops all textures, e.g. when resources are reloaded.
	 */
	public static void clear() {
		PAINT.invalidateAll();
		COVERS.asMap().values().forEach(location -> Minecraft.getInstance().getTextureManager().release(location));
		COVERS.invalidateAll();
	}

	private interface Pixels {
		int argb(int x, int y);
	}

	private static ResourceLocation upload(Pixels pixels) {
		final NativeImage image = new NativeImage(StencilPattern.SIZE, StencilPattern.SIZE, true);
		for (int y = 0; y < StencilPattern.SIZE; y++)
			for (int x = 0; x < StencilPattern.SIZE; x++)
				image.setPixelRGBA(x, y, toAbgr(pixels.argb(x, y)));
		final ResourceLocation location = OpenBlocks.location("canvas/" + NEXT_ID.getAndIncrement());
		Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
		return location;
	}

	// NativeImage stores ABGR
	private static int toAbgr(int argb) {
		return (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
	}

	private static int[] readStencilTexture() {
		final int[] result = new int[StencilPattern.SIZE * StencilPattern.SIZE];
		try (InputStream stream = Minecraft.getInstance().getResourceManager().open(STENCIL); NativeImage image = NativeImage.read(stream)) {
			for (int y = 0; y < StencilPattern.SIZE; y++)
				for (int x = 0; x < StencilPattern.SIZE; x++)
					result[y * StencilPattern.SIZE + x] = toAbgr(image.getPixelRGBA(x % image.getWidth(), y % image.getHeight()));
		} catch (IOException e) {
			OpenBlocks.LOG.warn("Failed to read {}", STENCIL, e);
			Arrays.fill(result, 0xFFE0E0E0);
		}
		return result;
	}
}
