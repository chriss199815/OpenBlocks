package openblocks.recipe;

import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
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
 * Port of legacy {@code CrayonMergeRecipe}: two or more pencils, or crayons of the same color, become one with all their
 * uses, set to block mode. Nothing stays in the grid.
 */
public class CrayonMergeRecipe extends CustomRecipe {

	public CrayonMergeRecipe(CraftingBookCategory category) {
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
		Item item = null;
		Integer color = null;
		float uses = 0;
		int count = 0;

		for (int i = 0; i < input.size(); i++) {
			final ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) continue;
			if (!(stack.getItem() instanceof ImaginaryItem)) return ItemStack.EMPTY;

			if (item == null) {
				item = stack.getItem();
				color = ImaginaryItem.getColor(stack);
			} else if (stack.getItem() != item || !Objects.equals(color, ImaginaryItem.getColor(stack))) {
				return ItemStack.EMPTY;
			}

			uses += ImaginaryItem.getUses(stack);
			count++;
		}

		if (count < 2 || uses == 0) return ItemStack.EMPTY;
		return ImaginaryItem.create(color, ImaginaryPlacementMode.BLOCK, uses);
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		return NonNullList.withSize(input.size(), ItemStack.EMPTY);
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return OBRecipeSerializers.CRAYON_MERGE.get();
	}
}
