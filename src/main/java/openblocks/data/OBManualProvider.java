package openblocks.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import openblocks.OpenBlocks;
import openblocks.integration.patchouli.CraftingComponent;
import openblocks.integration.patchouli.ItemBookmarkComponent;
import openblocks.integration.patchouli.OBPatchouli;
import openblocks.registry.OBItems;

/**
 * Generates the Patchouli book that replaces the legacy {@code GuiInfoBook}: like legacy, a page for every block and item with
 * a description, plus the miscellanea pages. Texts come from the lang files and are split into pages here, per language
 * (Patchouli does not paginate and falls back to {@code en_us} per file); names stay lang keys ({@code i18n}).
 */
public class OBManualProvider implements DataProvider {

	// lang files in src/main/resources; the generator can't list them
	private static final List<String> LANGUAGES = List.of("en_us", "de_de", "es_ar", "es_es", "es_mx", "es_uy", "es_ve", "et_ee",
			"fr_fr", "it_it", "ko_kr", "nl_nl", "pl_pl", "pt_br", "ru_ru", "zh_cn");

	private static final String DESCRIPTION = ".description";

	private static final String BLOCKS = "blocks";
	private static final String ITEMS = "items";
	private static final String MISC = "misc";

	/**
	 * Legacy miscellanea pages that still apply (the config page described the removed {@code om_config} commands), with icons.
	 */
	private static final Map<String, String> MISC_PAGES = linkedMap(
			"credits", "openblocks:info_book",
			"restore_inv", "minecraft:chest",
			"bkey", "openblocks:tasty_clay",
			"unstable", "minecraft:enchanted_book",
			"laststand", "minecraft:enchanted_book",
			"flimflam", "minecraft:enchanted_book");

	// Patchouli pages are 116 x 156 with 9 pixel lines; text starts lower on the first page and below a spotlight item
	private static final int PAGE_WIDTH = 116;
	private static final int LINES_SPOTLIGHT = (156 - 40) / 9;
	private static final int LINES_FIRST_PAGE = (156 - 22) / 9;
	private static final int LINES_PAGE = (156 + 4) / 9;

	private static final int MAX_RECIPES = 2;

	// templates instead of patchouli:spotlight and patchouli:crafting, for the JEI bookmark buttons
	private static final String SPOTLIGHT = "spotlight";
	private static final String CRAFTING = "crafting";
	private static final String CRAFTING_TEXTURE = "patchouli:textures/gui/crafting.png";

	private final PackOutput.PathProvider bookPath;
	private final PackOutput.PathProvider contentPath;
	private final CompletableFuture<HolderLookup.Provider> registries;
	private final ExistingFileHelper existingFileHelper;
	private final PackOutput output;

	public OBManualProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper) {
		this.output = output;
		this.bookPath = output.createPathProvider(PackOutput.Target.DATA_PACK, "patchouli_books");
		this.contentPath = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "patchouli_books");
		this.registries = registries;
		this.existingFileHelper = existingFileHelper;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		return registries.thenCompose(lookup -> {
			final BookFont font = new BookFont(existingFileHelper);
			final Map<Item, List<ResourceLocation>> recipes = collectRecipes(lookup);
			final Map<String, String> english = readLang("en_us");

			final List<CompletableFuture<?>> writes = new ArrayList<>();
			writes.add(DataProvider.saveStable(cache, book(), bookPath.json(OBPatchouli.BOOK.withSuffix("/book"))));
			writes.add(saveContent(cache, "en_us", "categories/" + BLOCKS, category("openblocks.gui.blocks", "openblocks:white_elevator", 0)));
			writes.add(saveContent(cache, "en_us", "categories/" + ITEMS, category("openblocks.gui.items", "openblocks:hang_glider", 1)));
			writes.add(saveContent(cache, "en_us", "categories/" + MISC, category("openblocks.gui.misc", "openblocks:info_book", 2)));
			writes.add(saveContent(cache, "en_us", "templates/" + SPOTLIGHT, spotlightTemplate()));
			writes.add(saveContent(cache, "en_us", "templates/" + CRAFTING, craftingTemplate()));

			for (String language : LANGUAGES) {
				final Map<String, String> lang = language.equals("en_us")? english : readLang(language);
				final Paginator paginator = new Paginator(font, Locale.forLanguageTag(language.replace('_', '-')));

				for (String key : english.keySet()) {
					if (!key.endsWith(DESCRIPTION) || !lang.containsKey(key)) continue;
					final String nameKey = key.substring(0, key.length() - DESCRIPTION.length());
					final String id = nameKey.substring(nameKey.lastIndexOf('.') + 1);
					final List<Item> items = findItems(id);
					if (items.isEmpty()) continue;

					final String category = nameKey.startsWith("block.")? BLOCKS : ITEMS;
					final JsonObject entry = itemEntry(nameKey, category, items, paginator.split(lang.get(key), LINES_SPOTLIGHT), recipes);
					writes.add(saveContent(cache, language, "entries/" + category + "/" + id, entry));
				}

				MISC_PAGES.forEach((name, icon) -> {
					final String contentKey = "openblocks.gui." + name + ".content";
					if (!lang.containsKey(contentKey)) return;
					final JsonObject entry = entry("openblocks.gui." + name + ".title", MISC, icon);
					final JsonArray pages = new JsonArray();
					for (String text : paginator.split(lang.get(contentKey), LINES_FIRST_PAGE))
						pages.add(page("patchouli:text", text));
					entry.add("pages", pages);
					writes.add(saveContent(cache, language, "entries/" + MISC + "/" + name, entry));
				});
			}

			return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
		});
	}

	private static JsonObject book() {
		final JsonObject book = new JsonObject();
		book.addProperty("name", OBItems.INFO_BOOK.get().getDescriptionId());
		book.addProperty("landing_text", "openblocks.gui.welcome.content");
		book.addProperty("i18n", true);
		book.addProperty("use_resource_pack", true);
		book.addProperty("dont_generate_book", true);
		book.addProperty("custom_book_item", BuiltInRegistries.ITEM.getKey(OBItems.INFO_BOOK.get()).toString());
		// the pages are measured with the vanilla font; shrinking only saves texts the measurement got wrong
		book.addProperty("use_blocky_font", true);
		book.addProperty("text_overflow_mode", "resize");
		book.addProperty("show_progress", false);
		return book;
	}

	private static JsonObject category(String name, String icon, int sortnum) {
		final JsonObject category = new JsonObject();
		category.addProperty("name", name);
		category.addProperty("description", "");
		category.addProperty("icon", icon);
		category.addProperty("sortnum", sortnum);
		return category;
	}

	private static JsonObject entry(String name, String category, String icon) {
		final JsonObject entry = new JsonObject();
		entry.addProperty("name", name);
		entry.addProperty("category", OpenBlocks.location(category).toString());
		entry.addProperty("icon", icon);
		return entry;
	}

	/**
	 * Spotlight on the item (cycling through its colors) with the start of the description, the rest on text pages, then
	 * crafting recipes; with colors, each recipe slot cycles through the recipes of all colors along with the spotlight.
	 */
	private static JsonObject itemEntry(String nameKey, String category, List<Item> items, List<String> texts, Map<Item, List<ResourceLocation>> recipes) {
		final JsonObject entry = entry(nameKey, category, itemId(items.getFirst()));
		final JsonArray pages = new JsonArray();

		final JsonObject spotlight = page(OpenBlocks.location(SPOTLIGHT).toString(), texts.getFirst());
		spotlight.addProperty("item", items.stream().map(OBManualProvider::itemId).collect(Collectors.joining(",")));
		spotlight.addProperty("title", nameKey);
		pages.add(spotlight);

		for (String text : texts.subList(1, texts.size()))
			pages.add(page("patchouli:text", text));

		final List<String> slots = new ArrayList<>();
		for (int slot = 0; slot < MAX_RECIPES; slot++) {
			final int index = slot;
			final String cycle = items.stream()
					.map(item -> recipes.getOrDefault(item, List.of()))
					.filter(itemRecipes -> itemRecipes.size() > index)
					.map(itemRecipes -> itemRecipes.get(index).toString())
					.collect(Collectors.joining(","));
			if (!cycle.isEmpty()) slots.add(cycle);
		}
		if (!slots.isEmpty()) {
			final JsonObject crafting = new JsonObject();
			crafting.addProperty("type", OpenBlocks.location(CRAFTING).toString());
			crafting.addProperty("recipe", slots.get(0));
			crafting.addProperty("recipe2", slots.size() > 1? slots.get(1) : "");
			pages.add(crafting);
		}

		entry.add("pages", pages);
		return entry;
	}

	/**
	 * Layout of {@code patchouli:spotlight} (title, framed item, text below) plus the bookmark button right of the frame.
	 */
	private static JsonObject spotlightTemplate() {
		final JsonObject frame = component("patchouli:image", 25, 10);
		frame.addProperty("image", CRAFTING_TEXTURE);
		frame.addProperty("v", 102);
		frame.addProperty("width", 66);
		frame.addProperty("height", 26);
		frame.addProperty("texture_width", 128);
		frame.addProperty("texture_height", 256);

		final JsonObject title = component("patchouli:header", 58, 0);
		title.addProperty("text", "#title#");

		final JsonObject item = component("patchouli:item", 50, 15);
		item.addProperty("item", "#item#");
		item.addProperty("link_recipe", true);

		final JsonObject text = component("patchouli:text", 0, 40);
		text.addProperty("text", "#text#");

		final JsonObject bookmark = component("patchouli:custom", 96, 18);
		bookmark.addProperty("class", ItemBookmarkComponent.class.getName());
		bookmark.addProperty("item", "#item#");

		return template(frame, title, item, text, bookmark);
	}

	private static JsonObject craftingTemplate() {
		final JsonObject crafting = component("patchouli:custom", 9, 4);
		crafting.addProperty("class", CraftingComponent.class.getName());
		crafting.addProperty("recipe", "#recipe#");
		crafting.addProperty("recipe2", "#recipe2#");
		return template(crafting);
	}

	private static JsonObject component(String type, int x, int y) {
		final JsonObject component = new JsonObject();
		component.addProperty("type", type);
		component.addProperty("x", x);
		component.addProperty("y", y);
		return component;
	}

	private static JsonObject template(JsonObject... components) {
		final JsonArray array = new JsonArray();
		for (JsonObject component : components)
			array.add(component);
		final JsonObject template = new JsonObject();
		template.add("components", array);
		return template;
	}

	private static JsonObject page(String type, String text) {
		final JsonObject page = new JsonObject();
		page.addProperty("type", type);
		page.addProperty("text", text);
		return page;
	}

	/**
	 * The item of that id, or its colored variants ({@code white_elevator}, ...).
	 */
	private static List<Item> findItems(String id) {
		final ResourceLocation location = OpenBlocks.location(id);
		if (BuiltInRegistries.ITEM.containsKey(location)) return List.of(BuiltInRegistries.ITEM.get(location));
		return OBItems.ITEMS.getEntries().stream()
				.filter(item -> item.getId().getPath().endsWith("_" + id))
				.map(item -> (Item)item.get())
				.toList();
	}

	private static String itemId(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	/**
	 * Unconditional shaped and shapeless recipes of {@link OBRecipeProvider} by result.
	 */
	private Map<Item, List<ResourceLocation>> collectRecipes(HolderLookup.Provider lookup) {
		final Map<Item, List<ResourceLocation>> result = new LinkedHashMap<>();
		new OBRecipeProvider(output, registries).buildRecipes(new RecipeOutput() {
			@Override
			public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
				if (conditions.length == 0 && (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe))
					result.computeIfAbsent(recipe.getResultItem(lookup).getItem(), item -> new ArrayList<>()).add(id);
			}

			@Override
			public Advancement.Builder advancement() {
				return Advancement.Builder.recipeAdvancement();
			}
		});
		return result;
	}

	private Map<String, String> readLang(String language) {
		final ResourceLocation location = OpenBlocks.location("lang/" + language + ".json");
		try (InputStream stream = existingFileHelper.getResource(location, PackType.CLIENT_RESOURCES).open()) {
			final JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			final Map<String, String> result = new LinkedHashMap<>();
			json.entrySet().forEach(e -> result.put(e.getKey(), e.getValue().getAsString()));
			return result;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private CompletableFuture<?> saveContent(CachedOutput cache, String language, String path, JsonObject json) {
		return DataProvider.saveStable(cache, json, contentPath.json(OBPatchouli.BOOK.withSuffix("/" + language + "/" + path)));
	}

	private static Map<String, String> linkedMap(String... keysAndValues) {
		final Map<String, String> result = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2)
			result.put(keysAndValues[i], keysAndValues[i + 1]);
		return result;
	}

	@Override
	public String getName() {
		return "OpenBlocks manual";
	}

	/**
	 * Wraps legacy texts ({@code \n} line breaks, {@code §L}/{@code §R} bold) like Patchouli's {@code TextLayouter} and cuts
	 * them into pages of Patchouli text ({@code $(br)}, {@code $(l)}).
	 */
	private static class Paginator {

		private final BookFont font;
		private final Locale locale;

		Paginator(BookFont font, Locale locale) {
			this.font = font;
			this.locale = locale;
		}

		/**
		 * @param firstPageLines lines left on the first page, all later pages are plain text pages
		 */
		List<String> split(String text, int firstPageLines) {
			final List<String> pages = new ArrayList<>();
			StringBuilder page = new StringBuilder();
			int lines = 0;
			int capacity = firstPageLines;
			boolean bold = false;
			boolean lineBreakPending = false;

			for (String paragraph : text.split("\n", -1)) {
				final StyledText styled = StyledText.parse(paragraph, bold);
				boolean boldOpen = false;
				final List<int[]> wrapped = wrap(styled);

				if (wrapped.isEmpty()) {
					// empty line, dropped at the top of a page
					if (lines > 0 && lines < capacity) {
						page.append("$(br)");
						lines++;
					}
				} else {
					for (int[] line : wrapped) {
						if (lines == capacity) {
							if (boldOpen) page.append("$()");
							boldOpen = false;
							pages.add(page.toString().strip());
							page = new StringBuilder();
							lines = 0;
							capacity = LINES_PAGE;
						} else if (lineBreakPending) {
							page.append("$(br)");
						}
						boldOpen = styled.append(page, line[0], line[1], boldOpen);
						lineBreakPending = false;
						lines++;
					}
				}

				if (boldOpen) page.append("$()");
				lineBreakPending = lines > 0;
				bold = styled.boldAtEnd;
			}

			if (!page.isEmpty() || pages.isEmpty()) pages.add(page.toString().strip());
			return pages;
		}

		/**
		 * Lines as {@code [start, end)} in the plain text; breaks where {@link BreakIterator} allows, long words anywhere.
		 */
		private List<int[]> wrap(StyledText text) {
			final List<int[]> lines = new ArrayList<>();
			final BreakIterator breaks = BreakIterator.getLineInstance(locale);
			breaks.setText(text.plain);

			int lineStart = 0;
			int width = 0;
			for (int start = breaks.first(), end = breaks.next(); end != BreakIterator.DONE; start = end, end = breaks.next()) {
				final int trimmedEnd = trimEnd(text.plain, start, end);
				final int segment = text.width(font, start, trimmedEnd);

				if (width + segment > PAGE_WIDTH && start > lineStart) {
					lines.add(new int[] { lineStart, start });
					lineStart = start;
					width = 0;
				}

				// a word wider than the page is cut
				int from = start;
				while (width + text.width(font, from, trimmedEnd) > PAGE_WIDTH) {
					int cut = from;
					while (cut < trimmedEnd && width + text.width(font, from, cut + 1) <= PAGE_WIDTH)
						cut++;
					if (cut == from) cut++;
					lines.add(new int[] { lineStart, cut });
					lineStart = cut;
					from = cut;
					width = 0;
				}

				width += text.width(font, from, end);
			}

			if (lineStart < text.plain.length()) lines.add(new int[] { lineStart, text.plain.length() });
			return lines;
		}

		private static int trimEnd(String text, int start, int end) {
			while (end > start && Character.isWhitespace(text.charAt(end - 1)))
				end--;
			return end;
		}
	}

	/**
	 * A paragraph without the {@code §} codes, remembering which characters are bold.
	 */
	private record StyledText(String plain, boolean[] bold, boolean boldAtEnd) {

		static StyledText parse(String text, boolean bold) {
			final StringBuilder plain = new StringBuilder();
			final List<Boolean> styles = new ArrayList<>();
			for (int i = 0; i < text.length(); i++) {
				final char ch = text.charAt(i);
				if (ch == '§' && i + 1 < text.length()) {
					final char code = Character.toLowerCase(text.charAt(++i));
					if (code == 'l') bold = true;
					else if (code == 'r') bold = false;
					continue;
				}
				plain.append(ch);
				styles.add(bold);
			}

			final boolean[] flags = new boolean[styles.size()];
			for (int i = 0; i < flags.length; i++)
				flags[i] = styles.get(i);
			return new StyledText(plain.toString(), flags, bold);
		}

		int width(BookFont font, int start, int end) {
			int width = 0;
			for (int i = start; i < end; i = plain.offsetByCodePoints(i, 1))
				width += font.width(plain.codePointAt(i), bold[i]);
			return width;
		}

		/**
		 * Appends a line as Patchouli text and returns whether bold is still on. With {@code i18n} the text goes through
		 * {@code String.format}, so {@code %} is doubled.
		 */
		boolean append(StringBuilder out, int start, int end, boolean current) {
			for (int i = start; i < end; i++) {
				if (bold[i] != current) {
					out.append(bold[i]? "$(l)" : "$()");
					current = bold[i];
				}
				final char ch = plain.charAt(i);
				out.append(ch == '%'? "%%" : String.valueOf(ch));
			}
			return current;
		}
	}
}
