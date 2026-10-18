package openblocks.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Port of legacy {@code ItemEpicEraser}: crafted with an item, it removes the item's lore (see
 * {@code EpicEraserRecipe}). Stays in the grid with one use less, 15 uses.
 */
public class EpicEraserItem extends Item {

	public static final int USES = 15;

	public EpicEraserItem(Properties properties) {
		super(properties.durability(USES));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public boolean hasCraftingRemainingItem(ItemStack stack) {
		return stack.getDamageValue() < USES - 1;
	}

	@Override
	public ItemStack getCraftingRemainingItem(ItemStack stack) {
		if (!hasCraftingRemainingItem(stack)) return ItemStack.EMPTY;
		final ItemStack result = stack.copyWithCount(1);
		result.setDamageValue(result.getDamageValue() + 1);
		return result;
	}
}
