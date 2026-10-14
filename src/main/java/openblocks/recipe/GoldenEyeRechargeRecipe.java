package openblocks.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import openblocks.item.GoldenEyeItem;
import openblocks.registry.OBRecipeSerializers;

/**
 * Port of legacy {@code GoldenEyeRechargeRecipe}: each ender pearl crafted with a golden eye gives back 10 charges; like
 * legacy, it does not match when the pearls would overcharge it. Keeps the locked structure.
 */
public class GoldenEyeRechargeRecipe extends CustomRecipe {

	public static final int PEARL_RECHARGE = 10;

	public GoldenEyeRechargeRecipe(CraftingBookCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		final ItemStack result = assemble(input);
		return !result.isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		return assemble(input);
	}

	private static ItemStack assemble(CraftingInput input) {
		ItemStack eye = ItemStack.EMPTY;
		int pearls = 0;

		for (int i = 0; i < input.size(); i++) {
			final ItemStack stack = input.getItem(i);
			if (stack.isEmpty()) continue;

			if (stack.getItem() instanceof GoldenEyeItem && eye.isEmpty()) eye = stack;
			else if (stack.is(Tags.Items.ENDER_PEARLS)) pearls++;
			else return ItemStack.EMPTY;
		}

		if (eye.isEmpty() || pearls == 0) return ItemStack.EMPTY;
		final int damage = eye.getDamageValue() - pearls * PEARL_RECHARGE;
		if (damage < 0) return ItemStack.EMPTY;

		final ItemStack result = eye.copyWithCount(1);
		result.setDamageValue(damage);
		return result;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return OBRecipeSerializers.GOLDEN_EYE_RECHARGE.get();
	}
}
