package openblocks.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;

/**
 * Port of legacy {@code ItemHeightMap}: shows a height map in a projector. The map id replaces the legacy item damage;
 * the map center is added to the tooltip on the client, see {@code HeightMapCache}.
 */
public class HeightMapItem extends Item {

	public HeightMapItem(Properties properties) {
		super(properties);
	}

	public static ItemStack create(int mapId, int scale) {
		final ItemStack stack = new ItemStack(OBItems.HEIGHT_MAP.get());
		stack.set(OBComponents.HEIGHT_MAP_ID, mapId);
		stack.set(OBComponents.MAP_SCALE, scale);
		return stack;
	}

	@Nullable
	public static Integer getMapId(ItemStack stack) {
		return stack.is(OBItems.HEIGHT_MAP)? stack.get(OBComponents.HEIGHT_MAP_ID) : null;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(EmptyMapItem.scaleTooltip(stack));
	}
}
