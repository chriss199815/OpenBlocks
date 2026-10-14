package openblocks.util;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.DyeColor;

/**
 * Port of the OpenModsLib color helpers ({@code ColorMeta}, {@code ColorUtils}, {@code RGB}, {@code CYMK}) the paint feature needs.
 * Colors are 24 bit RGB ints.
 */
public final class PaintColors {

	// legacy ColorMeta values, so paint mixed from the palette matches legacy
	private static final Map<DyeColor, Integer> DYE_COLORS = new EnumMap<>(DyeColor.class);

	static {
		DYE_COLORS.put(DyeColor.WHITE, 0xF0F0F0);
		DYE_COLORS.put(DyeColor.ORANGE, 0xEB8844);
		DYE_COLORS.put(DyeColor.MAGENTA, 0xC354CD);
		DYE_COLORS.put(DyeColor.LIGHT_BLUE, 0x6689D3);
		DYE_COLORS.put(DyeColor.YELLOW, 0xDECF2A);
		DYE_COLORS.put(DyeColor.LIME, 0x41CD34);
		DYE_COLORS.put(DyeColor.PINK, 0xD88198);
		DYE_COLORS.put(DyeColor.GRAY, 0x434343);
		DYE_COLORS.put(DyeColor.LIGHT_GRAY, 0xABABAB);
		DYE_COLORS.put(DyeColor.CYAN, 0x287697);
		DYE_COLORS.put(DyeColor.PURPLE, 0x7B2FBE);
		DYE_COLORS.put(DyeColor.BLUE, 0x253192);
		DYE_COLORS.put(DyeColor.BROWN, 0x51301A);
		DYE_COLORS.put(DyeColor.GREEN, 0x3B511A);
		DYE_COLORS.put(DyeColor.RED, 0xB3312C);
		DYE_COLORS.put(DyeColor.BLACK, 0x1E1B1B);
	}

	public static final int WHITE = 0xFFFFFF;

	private PaintColors() {}

	public static int rgb(DyeColor color) {
		return DYE_COLORS.get(color);
	}

	/**
	 * The dye color closest to the given one, like legacy {@code ColorUtils.findNearestColor}, but without its tolerance.
	 */
	public static DyeColor findNearest(int rgb) {
		DyeColor result = DyeColor.WHITE;
		int bestDistance = Integer.MAX_VALUE;
		for (Map.Entry<DyeColor, Integer> e : DYE_COLORS.entrySet()) {
			final int distance = distance(e.getValue(), rgb);
			if (distance < bestDistance) {
				result = e.getKey();
				bestDistance = distance;
			}
		}
		return result;
	}

	/**
	 * Weighted color distance from http://www.compuphase.com/cmetric.htm, like legacy {@code RGB.distance}.
	 */
	public static int distance(int a, int b) {
		final int r1 = (a >> 16) & 0xFF, g1 = (a >> 8) & 0xFF, b1 = a & 0xFF;
		final int r2 = (b >> 16) & 0xFF, g2 = (b >> 8) & 0xFF, b2 = b & 0xFF;
		final int meanR = (r1 + r2) >>> 1;
		final int dR = r1 - r2, dG = g1 - g2, dB = b1 - b2;
		return Math.abs((((512 + meanR) * dR * dR) >> 8) + ((dG * dG) << 2) + (((767 - meanR) * dB * dB) >> 8));
	}

	public static int interpolate(int from, int to, float amount) {
		final int r = (int)(((from >> 16) & 0xFF) * (1 - amount) + ((to >> 16) & 0xFF) * amount);
		final int g = (int)(((from >> 8) & 0xFF) * (1 - amount) + ((to >> 8) & 0xFF) * amount);
		final int b = (int)((from & 0xFF) * (1 - amount) + (to & 0xFF) * amount);
		return (r << 16) | (g << 8) | b;
	}

	public static String hex(int rgb) {
		return String.format("#%06X", rgb & WHITE);
	}

	/**
	 * Amount of each ink needed for one color, 0 to 1; like legacy {@code RGB.toCYMK}.
	 */
	public record Cmyk(float cyan, float magenta, float yellow, float key) {

		public static Cmyk of(int rgb) {
			float cyan = 1f - ((rgb >> 16) & 0xFF) / 255f;
			float magenta = 1f - ((rgb >> 8) & 0xFF) / 255f;
			float yellow = 1f - (rgb & 0xFF) / 255f;
			final float key = Math.min(1, Math.min(cyan, Math.min(magenta, yellow)));
			if (key == 1) return new Cmyk(0, 0, 0, 1);
			cyan = (cyan - key) / (1f - key);
			magenta = (magenta - key) / (1f - key);
			yellow = (yellow - key) / (1f - key);
			return new Cmyk(cyan, magenta, yellow, key);
		}
	}
}
