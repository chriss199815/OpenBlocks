package openblocks.data;

import com.google.common.hash.Hashing;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import openblocks.OpenBlocks;
import openblocks.common.StencilPattern;
import openblocks.item.StencilItem;

/**
 * Generates the stencil item textures: {@code textures/item/stencil.png} with the pattern cut out.
 * Replaces the legacy {@code StencilTextureManager}, which did this at runtime.
 */
public class OBStencilTextureProvider implements DataProvider {

	public static final ResourceLocation BACKGROUND = OpenBlocks.location("textures/item/stencil.png");

	private final PackOutput.PathProvider pathProvider;
	private final ExistingFileHelper existingFileHelper;

	public OBStencilTextureProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "textures/item");
		this.existingFileHelper = existingFileHelper;
		// so the item models can refer to them
		for (StencilPattern pattern : StencilPattern.values())
			existingFileHelper.trackGenerated(OpenBlocks.location("item/" + StencilItem.itemId(pattern)), PackType.CLIENT_RESOURCES, ".png", "textures");
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		return CompletableFuture.runAsync(() -> {
			final BufferedImage background = readBackground();
			for (StencilPattern pattern : StencilPattern.values())
				write(cache, StencilItem.itemId(pattern), cutOut(background, pattern));
		}, Util.backgroundExecutor());
	}

	private BufferedImage readBackground() {
		try (InputStream stream = existingFileHelper.getResource(BACKGROUND, PackType.CLIENT_RESOURCES).open()) {
			final BufferedImage image = ImageIO.read(stream);
			if (image.getWidth() != StencilPattern.SIZE || image.getHeight() != StencilPattern.SIZE)
				throw new IllegalStateException(BACKGROUND + " must be " + StencilPattern.SIZE + "x" + StencilPattern.SIZE);
			return image;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static BufferedImage cutOut(BufferedImage background, StencilPattern pattern) {
		final BufferedImage result = new BufferedImage(StencilPattern.SIZE, StencilPattern.SIZE, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < StencilPattern.SIZE; y++)
			for (int x = 0; x < StencilPattern.SIZE; x++)
				result.setRGB(x, y, pattern.isCutOut(x, y)? 0 : background.getRGB(x, y));
		return result;
	}

	private void write(CachedOutput cache, String name, BufferedImage image) {
		try {
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			ImageIO.write(image, "png", bytes);
			final byte[] data = bytes.toByteArray();
			cache.writeIfNeeded(pathProvider.file(OpenBlocks.location(name), "png"), data, Hashing.sha1().hashBytes(data));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public String getName() {
		return "OpenBlocks stencil textures";
	}
}
