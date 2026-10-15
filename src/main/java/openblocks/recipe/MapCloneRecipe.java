package openblocks.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;
import openblocks.registry.OBRecipeSerializers;

/**
 * Port of legacy {@code MapCloneRecipe}: a height map and an empty map of the same scale give two copies of the height map.
 */
public class MapCloneRecipe extends CustomRecipe {

	public MapCloneRecipe(CraftingBookCategory category) {
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
		ItemStack emptyMap = ItemStack.EMPTY;
		ItemStack heightMap = ItemStack.EMPTY;

		for (int i = 0; i < input.size(); i++) {
			final ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) continue;

			if (stack.is(OBItems.EMPTY_MAP) && emptyMap.isEmpty()) emptyMap = stack;
			else if (stack.is(OBItems.HEIGHT_MAP) && heightMap.isEmpty() && stack.has(OBComponents.HEIGHT_MAP_ID)) heightMap = stack;
			else return ItemStack.EMPTY;
		}

		if (emptyMap.isEmpty() || heightMap.isEmpty()) return ItemStack.EMPTY;
		if (!emptyMap.getOrDefault(OBComponents.MAP_SCALE, 0).equals(heightMap.getOrDefault(OBComponents.MAP_SCALE, 0))) return ItemStack.EMPTY;
		return heightMap.copyWithCount(2);
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return OBRecipeSerializers.MAP_CLONE.get();
	}
}
