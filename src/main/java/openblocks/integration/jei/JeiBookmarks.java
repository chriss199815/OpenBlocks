package openblocks.integration.jei;

import javax.annotation.Nullable;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IBookmarkManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;

/**
 * JEI bookmarks for other integrations (the manual's bookmark buttons). JEI is optional, so callers check
 * {@link #isAvailable()} first; it doesn't touch JEI classes without JEI.
 */
public class JeiBookmarks {

	public static final String MODID = "jei";

	@Nullable
	private static IJeiRuntime runtime;

	static void setRuntime(@Nullable IJeiRuntime runtime) {
		JeiBookmarks.runtime = runtime;
	}

	public static boolean isAvailable() {
		return ModList.get().isLoaded(MODID) && runtime != null;
	}

	public static boolean isBookmarked(ItemStack stack) {
		return runtime.getIngredientManager().createTypedIngredient(VanillaTypes.ITEM_STACK, stack)
				.map(runtime.getBookmarkManager()::contains)
				.orElse(false);
	}

	public static void toggle(ItemStack stack) {
		final IBookmarkManager bookmarks = runtime.getBookmarkManager();
		runtime.getIngredientManager().createTypedIngredient(VanillaTypes.ITEM_STACK, stack)
				.ifPresent(ingredient -> {
					if (!bookmarks.remove(ingredient)) bookmarks.add(ingredient);
				});
	}

	public static boolean isBookmarked(RecipeHolder<CraftingRecipe> recipe) {
		return runtime.getBookmarkManager().contains(RecipeTypes.CRAFTING, recipe);
	}

	public static void toggle(RecipeHolder<CraftingRecipe> recipe) {
		final IBookmarkManager bookmarks = runtime.getBookmarkManager();
		if (!bookmarks.remove(RecipeTypes.CRAFTING, recipe)) bookmarks.add(RecipeTypes.CRAFTING, recipe);
	}
}
