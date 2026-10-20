package openblocks.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Character widths of Minecraft's default font, read from the vanilla font definitions like the client's {@code BitmapProvider}
 * does, so {@link OBManualProvider} can split texts into book pages without a running client. Characters only in the unifont
 * fallback get an estimate.
 */
class BookFont {

	private static final int UNIFONT_WIDE = 9;
	private static final int UNIFONT_NARROW = 6;

	private final Int2IntMap advances = new Int2IntOpenHashMap();

	BookFont(ExistingFileHelper existingFileHelper) {
		final JsonObject spaces = read(existingFileHelper, ResourceLocation.withDefaultNamespace("font/include/space.json"));
		for (JsonElement provider : GsonHelper.getAsJsonArray(spaces, "providers")) {
			final JsonObject advanceMap = GsonHelper.getAsJsonObject(provider.getAsJsonObject(), "advances");
			for (String ch : advanceMap.keySet())
				advances.put(ch.codePointAt(0), advanceMap.get(ch).getAsInt());
		}

		final JsonObject font = read(existingFileHelper, ResourceLocation.withDefaultNamespace("font/include/default.json"));
		for (JsonElement element : GsonHelper.getAsJsonArray(font, "providers")) {
			final JsonObject provider = element.getAsJsonObject();
			if (GsonHelper.getAsString(provider, "type").equals("bitmap")) addBitmap(existingFileHelper, provider);
		}
	}

	private void addBitmap(ExistingFileHelper existingFileHelper, JsonObject provider) {
		final ResourceLocation file = ResourceLocation.parse(GsonHelper.getAsString(provider, "file")).withPrefix("textures/");
		final int height = GsonHelper.getAsInt(provider, "height", 8);
		final List<int[]> rows = GsonHelper.getAsJsonArray(provider, "chars").asList().stream()
				.map(row -> row.getAsString().codePoints().toArray())
				.toList();

		final BufferedImage image = readImage(existingFileHelper, file);
		final int cellWidth = image.getWidth() / rows.getFirst().length;
		final int cellHeight = image.getHeight() / rows.size();
		final float scale = (float)height / cellHeight;

		for (int y = 0; y < rows.size(); y++) {
			final int[] row = rows.get(y);
			for (int x = 0; x < row.length; x++) {
				if (row[x] == 0 || advances.containsKey(row[x])) continue;
				advances.put(row[x], (int)(0.5 + glyphWidth(image, cellWidth, cellHeight, x, y) * scale) + 1);
			}
		}
	}

	private static int glyphWidth(BufferedImage image, int cellWidth, int cellHeight, int cellX, int cellY) {
		// like NativeImage.getLuminanceOrAlpha
		final int mask = image.getColorModel().hasAlpha()? 0xFF000000 : 0x00FFFFFF;
		for (int x = cellWidth - 1; x >= 0; x--)
			for (int y = 0; y < cellHeight; y++)
				if ((image.getRGB(cellX * cellWidth + x, cellY * cellHeight + y) & mask) != 0) return x + 1;
		return 0;
	}

	int width(int codePoint, boolean bold) {
		final int advance = advances.containsKey(codePoint)? advances.get(codePoint) : unifontWidth(codePoint);
		return bold? advance + 1 : advance;
	}

	private static int unifontWidth(int codePoint) {
		return codePoint >= 0x1100? UNIFONT_WIDE : UNIFONT_NARROW;
	}

	private static JsonObject read(ExistingFileHelper existingFileHelper, ResourceLocation location) {
		try (InputStream stream = existingFileHelper.getResource(location, PackType.CLIENT_RESOURCES).open()) {
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static BufferedImage readImage(ExistingFileHelper existingFileHelper, ResourceLocation location) {
		try (InputStream stream = existingFileHelper.getResource(location, PackType.CLIENT_RESOURCES).open()) {
			return ImageIO.read(stream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
