package openblocks.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.util.RandomSource;

/**
 * Port of OpenModsLib {@code openmods.words}: building blocks for random texts. Arguments that are no generators become
 * constant text.
 */
public class Words {

	@FunctionalInterface
	public interface Generator {
		String generate(RandomSource random, Map<String, String> params);
	}

	private static Generator[] convert(Object... args) {
		return Arrays.stream(args).map(arg -> arg instanceof Generator generator? generator : terminal(arg)).toArray(Generator[]::new);
	}

	public static Generator terminal(Object value) {
		final String text = value.toString();
		return (random, params) -> text;
	}

	/**
	 * One of the parts, chosen at random.
	 */
	public static Generator alt(Object... parts) {
		final Generator[] alts = convert(parts);
		return (random, params) -> alts.length == 0? "" : alts[random.nextInt(alts.length)].generate(random, params);
	}

	/**
	 * The parts separated by spaces (empty ones left out).
	 */
	public static Generator seq(Object... parts) {
		final Generator[] generators = convert(parts);
		return (random, params) -> {
			final List<String> results = new ArrayList<>();
			for (Generator part : generators) {
				final String result = part.generate(random, params);
				if (!result.isEmpty()) results.add(result);
			}
			return String.join(" ", results);
		};
	}

	/**
	 * The parts joined without spaces.
	 */
	public static Generator word(Object... parts) {
		final Generator[] generators = convert(parts);
		return (random, params) -> {
			final StringBuilder builder = new StringBuilder();
			for (Generator part : generators)
				builder.append(part.generate(random, params));
			return builder.toString();
		};
	}

	public static Generator opt(float probability, Object part) {
		final Generator generator = convert(part)[0];
		return (random, params) -> random.nextFloat() < probability? generator.generate(random, params) : "";
	}

	/**
	 * A number from {@code start} (inclusive) to {@code end} (exclusive).
	 */
	public static Generator range(int start, int end) {
		return (random, params) -> Integer.toString(start + random.nextInt(end - start));
	}

	/**
	 * The parameter {@code key}, or {@code defaultValue} if it is missing.
	 */
	public static Generator sub(String key, String defaultValue) {
		return (random, params) -> Objects.requireNonNullElse(params.get(key), defaultValue);
	}

	/**
	 * First letter of every word upper case.
	 */
	public static Generator capitalize(Generator generator) {
		return transform(generator, text -> capitalizeWords(text, false));
	}

	/**
	 * First letter of every word upper case, the rest lower case.
	 */
	public static Generator capitalizeFully(Generator generator) {
		return transform(generator, text -> capitalizeWords(text, true));
	}

	private static Generator transform(Generator generator, UnaryOperator<String> transformation) {
		return (random, params) -> transformation.apply(generator.generate(random, params));
	}

	private static String capitalizeWords(String text, boolean fully) {
		final StringBuilder result = new StringBuilder(text.length());
		boolean wordStart = true;
		for (char c : text.toCharArray()) {
			if (Character.isWhitespace(c)) {
				wordStart = true;
				result.append(c);
			} else {
				result.append(wordStart? Character.toTitleCase(c) : fully? Character.toLowerCase(c) : c);
				wordStart = false;
			}
		}
		return result.toString();
	}
}
