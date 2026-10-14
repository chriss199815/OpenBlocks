package openblocks.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import openblocks.registry.OBBlocks;
import openblocks.registry.OBComponents;
import openblocks.util.PaintColors;

/**
 * Port of legacy {@code ItemPaintCan}: the paint is kept in {@link OBComponents#PAINT}, the bar shows how much is left.
 */
public class PaintCanItem extends BlockItem {

	public static final int FULL_CAN_SIZE = 30;

	public PaintCanItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static ItemStack create(int color) {
		final ItemStack stack = new ItemStack(OBBlocks.PAINT_CAN.get());
		stack.set(OBComponents.PAINT, new Paint(color, FULL_CAN_SIZE));
		return stack;
	}

	public static ItemStack create(DyeColor color) {
		return create(PaintColors.rgb(color));
	}

	public static int getColor(ItemStack stack) {
		final Paint paint = stack.get(OBComponents.PAINT);
		return paint != null? paint.color() : PaintColors.WHITE;
	}

	private static int getAmount(ItemStack stack) {
		final Paint paint = stack.get(OBComponents.PAINT);
		return paint != null? paint.amount() : 0;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return getAmount(stack) < FULL_CAN_SIZE;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13f * getAmount(stack) / FULL_CAN_SIZE);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return Mth.hsvToRgb(Math.max(0f, (float)getAmount(stack) / FULL_CAN_SIZE) / 3f, 1f, 1f);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(PaintColors.hex(getColor(stack))).withStyle(ChatFormatting.GRAY));
	}
}
