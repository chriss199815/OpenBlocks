package openblocks.recipe;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import openblocks.item.ImaginaryItem;
import openblocks.item.ImaginaryPlacementMode;
import openblocks.registry.OBRecipeSerializers;

/**
 * Port of legacy {@code CrayonMixingRecipe}: crayons of different colors with at least one use each make a crayon of
 * their average color, with 0.9 uses per crayon and their most common mode. Like legacy, the crayons stay in the grid
 * with one use less. Crayons of one color are left to {@link CrayonMergeRecipe}, which legacy checked first.
 */
public class CrayonMixingRecipe extends CustomRecipe {

	private static final float USES_PER_CRAYON = 0.9f;

	public CrayonMixingRecipe(CraftingBookCategory category) {
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
		int count = 0;
		int r = 0, g = 0, b = 0;
		Integer firstColor = null;
		boolean mixed = false;
		final Map<ImaginaryPlacementMode, Integer> modes = new EnumMap<>(ImaginaryPlacementMode.class);

		for (int i = 0; i < input.size(); i++) {
			final ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) continue;
			if (!ImaginaryItem.isCrayon(stack)) return ItemStack.EMPTY;

			final int color = ImaginaryItem.getColor(stack);
			if (firstColor == null) firstColor = color;
			else if (firstColor != color) mixed = true;

			if (ImaginaryItem.getUses(stack) < ImaginaryItem.CRAFTING_COST) continue;
			count++;
			r += (color >> 16) & 0xFF;
			g += (color >> 8) & 0xFF;
			b += color & 0xFF;
			modes.merge(ImaginaryItem.getMode(stack), 1, Integer::sum);
		}

		if (count < 2 || !mixed) return ItemStack.EMPTY;

		final int color = ((r / count) << 16) | ((g / count) << 8) | (b / count);
		final ImaginaryPlacementMode mode = modes.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
		return ImaginaryItem.create(color, mode, count * USES_PER_CRAYON);
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return OBRecipeSerializers.CRAYON_MIXING.get();
	}
}
