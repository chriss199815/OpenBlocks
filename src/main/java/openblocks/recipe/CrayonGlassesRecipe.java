package openblocks.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import openblocks.item.ImaginaryItem;
import openblocks.item.ImaginationGlassesItem;
import openblocks.registry.OBRecipeSerializers;

/**
 * Port of legacy {@code CrayonGlassesRecipe}: a crayon and a paper make glasses of the crayon's color. The crayon stays
 * in the grid with one use less, see {@link ImaginaryItem#getCraftingRemainingItem}.
 */
public class CrayonGlassesRecipe extends CustomRecipe {

	public CrayonGlassesRecipe(CraftingBookCategory category) {
		super(category);
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
		ItemStack crayon = ItemStack.EMPTY;
		int paper = 0;

		for (int i = 0; i < input.size(); i++) {
			final ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) continue;

			if (ImaginaryItem.isCrayon(stack) && crayon.isEmpty()) crayon = stack;
			else if (stack.is(Items.PAPER)) paper++;
			else return ItemStack.EMPTY;
		}

		if (crayon.isEmpty() || paper != 1) return ItemStack.EMPTY;
		return ImaginationGlassesItem.createCrayonGlasses(ImaginaryItem.getColor(crayon));
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return OBRecipeSerializers.CRAYON_GLASSES.get();
	}
}
