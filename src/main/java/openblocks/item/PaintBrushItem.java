package openblocks.item;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import openblocks.OBConfig;
import openblocks.api.IPaintableBlock;
import openblocks.block.CanvasBlock;
import openblocks.common.DyedBlocks;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;
import openblocks.util.PaintColors;

/**
 * Port of legacy {@code ItemPaintBrush}: paints the clicked side (all sides when sneaking) of paintable blocks in any color,
 * dyed vanilla blocks in the dye color closest to the paint, and turns other blocks into a canvas to paint them. Unlike legacy, which only recolored them if the paint was within
 * a small tolerance of a dye color (so even #000000 left wool white), the closest dye color is always used. Dipped into a paint can, it holds {@link #MAX_USES} uses.
 */
public class PaintBrushItem extends Item {

	public static final int MAX_USES = 24;

	public PaintBrushItem(Properties properties) {
		super(properties);
	}

	public static ItemStack create(int color) {
		final ItemStack stack = new ItemStack(OBItems.PAINT_BRUSH.get());
		stack.set(OBComponents.PAINT, new Paint(color, MAX_USES));
		return stack;
	}

	public static ItemStack create(DyeColor color) {
		return create(PaintColors.rgb(color));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

		final ItemStack stack = context.getItemInHand();
		final Paint paint = stack.get(OBComponents.PAINT);
		if (paint == null || paint.amount() <= 0) return InteractionResult.FAIL;

		final Level level = context.getLevel();
		final Player player = context.getPlayer();
		final Direction[] sides = player != null && player.isSecondaryUseActive()? Direction.values() : new Direction[] { context.getClickedFace() };
		if (!recolorBlock(level, context.getClickedPos(), paint.color(), sides)) return InteractionResult.FAIL;

		if (!level.isClientSide) {
			level.playSound(null, context.getClickedPos(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.1f, 0.8f);
			if (player == null || !player.getAbilities().instabuild) {
				final int left = paint.amount() - 1;
				// like legacy, a used up brush is clean again
				if (left <= 0) stack.remove(OBComponents.PAINT);
				else stack.set(OBComponents.PAINT, paint.withAmount(left));
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private static boolean recolorBlock(Level level, BlockPos pos, int color, Direction... sides) {
		if (tryRecolorBlock(level, pos, color, sides)) return true;
		// like legacy, other blocks become a canvas, which takes any color
		if (!OBConfig.CANVAS.paintbrushReplacesBlocks.get() || !CanvasBlock.canReplace(level.getBlockState(pos))) return false;
		if (level.isClientSide) return true;
		return CanvasBlock.replaceBlock(level, pos) && tryRecolorBlock(level, pos, color, sides);
	}

	private static boolean tryRecolorBlock(Level level, BlockPos pos, int color, Direction... sides) {
		final BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof IPaintableBlock paintable) {
			if (level.isClientSide) return true;
			boolean changed = false;
			for (Direction side : sides)
				changed |= paintable.recolorBlock(level, pos, side, color);
			return changed;
		}

		final Optional<BlockState> recolored = DyedBlocks.recolor(state, PaintColors.findNearest(color));
		if (recolored.isEmpty()) return false;
		if (!level.isClientSide) level.setBlock(pos, recolored.get(), Block.UPDATE_ALL);
		return true;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		final Paint paint = stack.get(OBComponents.PAINT);
		return paint != null && paint.amount() < MAX_USES;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		final Paint paint = stack.get(OBComponents.PAINT);
		return paint != null? Math.round(13f * paint.amount() / MAX_USES) : 0;
	}

	@Override
	public int getBarColor(ItemStack stack) {
		final Paint paint = stack.get(OBComponents.PAINT);
		return Mth.hsvToRgb(paint != null? (float)paint.amount() / MAX_USES / 3f : 0f, 1f, 1f);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		final Paint paint = stack.get(OBComponents.PAINT);
		if (paint != null) tooltip.add(Component.literal(PaintColors.hex(paint.color())).withStyle(ChatFormatting.GRAY));
	}
}
