package openblocks.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;

/**
 * Port of legacy {@code ItemEmptyMap}: becomes a new height map with its scale when put into a projector or cartographer.
 */
public class EmptyMapItem extends Item {

	public EmptyMapItem(Properties properties) {
		super(properties);
	}

	public static ItemStack create(int scale) {
		final ItemStack stack = new ItemStack(OBItems.EMPTY_MAP.get());
		stack.set(OBComponents.MAP_SCALE, scale);
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(scaleTooltip(stack));
	}

	static Component scaleTooltip(ItemStack stack) {
		return Component.translatable("openblocks.misc.map_scale", 1 << stack.getOrDefault(OBComponents.MAP_SCALE, 0)).withStyle(ChatFormatting.GRAY);
	}
}
