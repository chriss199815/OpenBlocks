package openblocks.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import openblocks.block.FlagBlock;
import openblocks.registry.OBBlocks;

/**
 * Port of legacy {@code ItemFlagBlock}: right-click in the air, or sneaking on a block, switches to the next color.
 */
public class FlagItem extends BlockItem {

	public FlagItem(FlagBlock block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.isSecondaryUseActive()) return InteractionResult.PASS;
		return super.useOn(context);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		final ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);

		final DyeColor next = DyeColor.byId((((FlagBlock)getBlock()).getColor().getId() + 1) % DyeColor.values().length);
		return InteractionResultHolder.sidedSuccess(stack.transmuteCopy(OBBlocks.FLAGS.get(next).get(), stack.getCount()), level.isClientSide);
	}
}
