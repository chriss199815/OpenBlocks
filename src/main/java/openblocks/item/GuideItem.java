package openblocks.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import openblocks.common.GuideSettings;
import openblocks.registry.OBComponents;

/**
 * Port of legacy {@code ItemGuide}: a broken guide keeps size, shape and color in {@link OBComponents#GUIDE} and shows
 * them in the tooltip.
 */
public class GuideItem extends BlockItem {

	public GuideItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		final GuideSettings settings = stack.get(OBComponents.GUIDE);
		if (settings != null) settings.addToTooltip(tooltip::add);
	}
}
