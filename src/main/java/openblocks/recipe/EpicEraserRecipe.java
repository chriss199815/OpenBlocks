package openblocks.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import openblocks.item.EpicEraserItem;
import openblocks.registry.OBComponents;
import openblocks.registry.OBRecipeSerializers;

/**
 * Port of legacy {@code EpicEraserRecipe}: an epic eraser and an item with lore (silly lore from the lore flim flam or
 * vanilla lore) give the item without both.
 */
public class EpicEraserRecipe extends CustomRecipe {

	public EpicEraserRecipe(CraftingBookCategory category) {
		super(category);
	}

	private static boolean hasLore(ItemStack stack) {
		return stack.has(OBComponents.SILLY_LORE) || stack.has(DataComponents.LORE);
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return !assemble(input).isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		return assemble(input);
	}

	private static ItemStack assemble(CraftingInput input) {
		ItemStack target = ItemStack.EMPTY;
		boolean eraserFound = false;

		for (int i = 0; i < input.size(); i++) {
			final ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) continue;

			if (stack.getItem() instanceof EpicEraserItem && !eraserFound) eraserFound = true;
			else if (hasLore(stack) && target.isEmpty()) target = stack;
			else return ItemStack.EMPTY;
		}

		if (!eraserFound || target.isEmpty()) return ItemStack.EMPTY;

		final ItemStack result = target.copyWithCount(1);
		result.remove(OBComponents.SILLY_LORE);
		result.remove(DataComponents.LORE);
		return result;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return OBRecipeSerializers.EPIC_ERASER.get();
	}
}
