package openblocks.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import openblocks.block.CanvasBlock;
import openblocks.block.entity.CanvasBlockEntity;
import openblocks.common.StencilPattern;

/**
 * Port of legacy {@code ItemStencil}: one item per pattern instead of metadata. All are named "Stencil", the pattern is shown in the tooltip,
 * like vanilla banner patterns. Used on a block, it is put on that side, turning the block into a canvas first.
 */
public class StencilItem extends Item {

	private final StencilPattern pattern;

	public StencilItem(StencilPattern pattern, Properties properties) {
		super(properties);
		this.pattern = pattern;
	}

	public StencilPattern getPattern() {
		return pattern;
	}

	public static String itemId(StencilPattern pattern) {
		return pattern.getSerializedName() + "_stencil";
	}

	public static MutableComponent patternName(StencilPattern pattern) {
		return Component.translatable("openblocks.stencil." + pattern.getSerializedName());
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		final Level level = context.getLevel();
		final BlockPos pos = context.getClickedPos();
		if (!(level.getBlockState(pos).getBlock() instanceof CanvasBlock) && !CanvasBlock.canReplace(level.getBlockState(pos))) return InteractionResult.PASS;
		if (level.isClientSide) return InteractionResult.SUCCESS;

		if (CanvasBlock.replaceBlock(level, pos) && level.getBlockEntity(pos) instanceof CanvasBlockEntity canvas && canvas.putStencil(context.getClickedFace(), context.getItemInHand())) {
			context.getItemInHand().consume(1, context.getPlayer());
			return InteractionResult.CONSUME;
		}
		return InteractionResult.FAIL;
	}

	@Override
	public String getDescriptionId() {
		return "item.openblocks.stencil";
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(patternName(pattern).withStyle(ChatFormatting.GRAY));
	}
}
