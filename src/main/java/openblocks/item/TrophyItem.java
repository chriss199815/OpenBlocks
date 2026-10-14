package openblocks.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import openblocks.common.TrophyType;
import openblocks.registry.OBComponents;

/**
 * Port of legacy {@code ItemTrophyBlock}: one item for all trophies, the mob is kept in {@link OBComponents#TROPHY}
 * instead of the {@code entity_id} tag and named after it ("Cow Trophy").
 */
public class TrophyItem extends BlockItem {

	public TrophyItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static ItemStack create(ItemLike trophy, TrophyType type) {
		final ItemStack stack = new ItemStack(trophy);
		stack.set(OBComponents.TROPHY, type);
		return stack;
	}

	@Override
	public Component getName(ItemStack stack) {
		final TrophyType type = stack.get(OBComponents.TROPHY);
		if (type == null) return super.getName(stack);
		return Component.translatable(getDescriptionId() + ".entity", type.getEntityType().getDescription());
	}
}
