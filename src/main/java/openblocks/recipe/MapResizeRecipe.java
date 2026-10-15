package openblocks.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import openblocks.common.HeightMapData;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;
import openblocks.registry.OBRecipeSerializers;

/**
 * Port of legacy {@code MapResizeRecipe}: an empty map between two map memory modules in a row doubles its scale (up to
 * 1:16). Unlike legacy, other items in the grid do not match (they were eaten).
 */
public class MapResizeRecipe extends CustomRecipe {

	public MapResizeRecipe(CraftingBookCategory category) {
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

	// the input is trimmed to the filled area, so the three items fill it exactly
	private static ItemStack assemble(CraftingInput input) {
		if (input.width() != 3 || input.height() != 1) return ItemStack.EMPTY;
		if (!input.getItem(0).is(OBItems.MAP_MEMORY) || !input.getItem(2).is(OBItems.MAP_MEMORY)) return ItemStack.EMPTY;

		final ItemStack map = input.getItem(1);
		if (!map.is(OBItems.EMPTY_MAP)) return ItemStack.EMPTY;
		final int scale = map.getOrDefault(OBComponents.MAP_SCALE, 0);
		if (scale >= HeightMapData.MAX_SCALE) return ItemStack.EMPTY;

		final ItemStack result = map.copyWithCount(1);
		result.set(OBComponents.MAP_SCALE, scale + 1);
		return result;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= 3;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return OBRecipeSerializers.MAP_RESIZE.get();
	}
}
